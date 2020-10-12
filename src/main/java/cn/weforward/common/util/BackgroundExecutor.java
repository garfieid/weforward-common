/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.common.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.Destroyable;
import cn.weforward.common.DestroyableExt;
import cn.weforward.common.Nameable;
import cn.weforward.common.crypto.Hex;
import cn.weforward.common.execption.AbortTaskExecute;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.sys.Timepoint;

/**
 * 基于定时任务及线程池的后台任务执行器，用于管理进行后台操作的任务
 * 
 * @author liangyi
 * 
 */
public class BackgroundExecutor implements TaskExecutor, DestroyableExt {
	/** 日志记录器 */
	public final static Logger _Logger = LoggerFactory.getLogger(TaskExecutor.class);

	/** AbortPolicy */
	final static RejectedExecutionHandler _ExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	/** 共享的Timer */
	final static Timer _Timer = new Timer("bgTimer-share", true);
	/** 用于标识停止的定时器 */
	final static Timer _Dead = new Timer("bgTimer-Dead", true);

	/** 任务定时器 */
	protected Timer m_Timer;
	/** 用于标识后台运行的任务 */
	protected final ThreadLocal<Object> m_Mark;
	/** 等待（网络可用/空闲/登录后）执行的任务 */
	protected final ArrayList<TaskImpl> m_Tasks;
	/** 线程池 */
	protected Executor m_Executor;
	/** 名称，仅为作标识 */
	protected String m_Name;
	/** 创建线程次数/计数 */
	protected volatile int m_CreateTimes;

	/**
	 * 指定上层执行器来构建
	 * 
	 * @param executor
	 *            上层执行器
	 */
	public BackgroundExecutor(Executor executor) {
		m_Mark = new ThreadLocal<Object>();
		m_Tasks = new ArrayList<TaskImpl>();
		// m_Timer = new Timer("bgTimer-" + Misc.toHex(this.hashCode()), true);
		m_Executor = executor;
		m_Timer = _Timer;
		Shutdown.register(this);
		if (executor instanceof Nameable) {
			m_Name = ((Nameable) executor).getName();
		}
	}

	/**
	 * 创建执行器
	 * 
	 * @param maxThreads
	 *            最大线程数
	 * @param name
	 *            名称
	 */
	public BackgroundExecutor(int maxThreads, String name) {
		this(new ThreadPool(maxThreads, name));
		// 线程空闲30分钟则结束
		ThreadPool tp = (ThreadPool) m_Executor;
		tp.setDaemon(true);
		tp.setIdle(30 * 60);
	}

	/**
	 * 构造执行器
	 * 
	 * @param minThreads
	 *            最小维持/核心线程数
	 * @param maxThreads
	 *            最大线程数
	 * @param queueSize
	 *            最大任务等待队列
	 */
	public BackgroundExecutor(int minThreads, int maxThreads, int queueSize) {
		this(null);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(minThreads, maxThreads, 2,
				TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(queueSize), new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						String name = getName();
						StringBuilder sb;
						if (null == name || 0 == name.length()) {
							sb = new StringBuilder(8 + 4);
							sb.append("bgt-");
						} else {
							sb = new StringBuilder(8 + 1 + name.length());
							sb.append(name).append('-');
						}
						// Misc.toHex(r.hashCode(), sb);
						sb.append(++m_CreateTimes);
						name = sb.toString();
						Thread t = new Thread(r, name);
						if (t.isDaemon()) {
							t.setDaemon(false);
						}
						// if (t.getPriority() != Thread.NORM_PRIORITY){
						// t.setPriority(Thread.NORM_PRIORITY);
						// }
						return t;
					}
				}, _ExecutionHandler);
		// 允许core线程空闲2分钟后结束
		threadPool.allowCoreThreadTimeOut(true);
		m_Executor = threadPool;
		// Shutdown.register(this);
	}

	protected Timer getTimer(boolean restart) {
		Timer timer = m_Timer;
		if (_Dead == timer) {
			// 已经是终止的，不能重启
			return timer;
		}
		if (null == timer || restart) {
			// 重建
			_Logger.error("重建Timer：" + this);
			timer = new Timer("bgTimer-" + Hex.toHex(this.hashCode()), true);
			m_Timer = timer;
		}
		return timer;
	}

	/**
	 * 指定一个名称方便标识，没业务用途
	 * 
	 * @param name
	 *            名称
	 */
	public void setName(String name) {
		m_Name = name;
	}

	public String getName() {
		return m_Name;
	}

	@Override
	public String toString() {
		// if (null != m_Name) {
		// return m_Name;
		// }
		// return super.toString();
		return toString(new StringBuilder(64)).toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("{name:");
		if (null == m_Name || 0 == m_Name.length()) {
			Hex.toHex(hashCode(), sb);
		} else {
			sb.append(m_Name);
		}
		sb.append(",tasks:").append(m_Tasks.size());
		if (m_Executor instanceof ExecutorService) {
			ThreadPoolExecutor tpe = (ThreadPoolExecutor) m_Executor;
			sb.append(",core:").append(tpe.getCorePoolSize()).append(",max:")
					.append(tpe.getMaximumPoolSize()).append(",active:")
					.append(tpe.getActiveCount()).append(",done:")
					.append(tpe.getCompletedTaskCount());
		} else {
			sb.append(",pool:").append(m_Executor);
		}
		sb.append(",timer:");// .append(m_Timer);
		if (_Timer == m_Timer) {
			sb.append("share");
		} else if (_Dead == m_Timer) {
			sb.append("dead");
		} else {
			sb.append(m_Timer);
		}
		sb.append("}");
		return sb;
	}

	/**
	 * 增加一个后台任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*|EVENT_*
	 * @param delay
	 *            任务在（毫秒后）执行
	 * @param period
	 *            任务按（毫秒）周期地执行
	 */
	synchronized public Task execute(Runnable worker, int options, long delay, long period) {
		if (isShutdown()) {
			throw new RejectedExecutionException("执行器已停:" + getName());
		}

		TaskImpl tx = new TaskImpl(worker, options);
		if (worker instanceof AbstractWorker) {
			((AbstractWorker) worker).setTask(tx);
		}
		if (period > 0 || delay > 0) {
			// 定时任务
			TimerTask t = new TimerTaskExt(tx);
			if (period > 0) {
				// 周期执行的后台任务
				Timer timer = getTimer(false);
				try {
					timer.schedule(t, (delay < 0 ? 0 : delay), period);
				} catch (IllegalStateException e) {
					_Logger.error("定时器异常：" + BackgroundExecutor.this, e);
					// m_Timer = null;
					getTimer(true).schedule(t, (delay < 0 ? 0 : delay), period);
				}
			} else {
				// 延时执行，只执行一次
				tx.setOnce();
				Timer timer = getTimer(false);
				try {
					timer.schedule(t, delay);
				} catch (IllegalStateException e) {
					_Logger.error("定时器异常：" + BackgroundExecutor.this, e);
					// m_Timer = null;
					getTimer(true).schedule(t, delay);
				}
			}
		} else if (tx.isReady()) {
			// 具备立刻就能执行的条件，现在就执行
			tx.setOnce();
			execute(tx);
		} else {
			// 放到队列中等吧
			tx.setOnce();
			m_Tasks.add(tx);
		}
		return tx;
	}

	public Task execute(Runnable worker, int options, Date firstTime, long period) {
		long delay = firstTime.getTime() - System.currentTimeMillis();
		if (0 == period && delay < 0) {
			// 若firstTime小于当前时间且period为0则不执行任务
			return null;
		}
		return execute(worker, options, delay, period);
	}

	/**
	 * 增加一个后台任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*|EVENT_*
	 * @param delay
	 *            任务在（毫秒后）执行
	 */
	public Task execute(Runnable worker, int options, long delay) {
		return execute(worker, options, delay, 0);
	}

	/**
	 * 增加一个后台任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*|EVENT_*
	 */
	public Task execute(Runnable worker, int options) {
		return execute(worker, options, 0, 0);
	}

	/**
	 * 检查当前线程是否后台运行中的任务
	 * 
	 * @return 是则返回true
	 */
	public boolean isBackground() {
		return (this == m_Mark.get());
	}

	/**
	 * 使用线程池来运行后台任务
	 * 
	 * @param runnable
	 */
	public void execute(Runnable runnable) {
		if (isShutdown()) {
			throw new RejectedExecutionException("执行器已停:" + getName());
		}
		if (runnable instanceof AbstractWorker) {
			TaskImpl tx = new TaskImpl(runnable, OPTION_NONE);
			((AbstractWorker) runnable).setTask(tx);
		} else {
			m_Executor.execute(runnable);
		}
	}

	/**
	 * 运行指定事件下的任务
	 * 
	 * @param event
	 *            事件 EVENT_*
	 * 
	 */
	public void runTasksAtEvent(int event) {
		for (int i = m_Tasks.size() - 1; i >= 0; i--) {
			TaskImpl task = m_Tasks.get(i);
			if (task.isOption(event) && task.isReady()) {
				// 是等待这个状态的任务，执行其并由队列中删除
				m_Tasks.remove(i);
				execute(task);
			}
		}
	}

	/**
	 * 是否已经关停
	 */
	public boolean isShutdown() {
		return _Dead == m_Timer;
	}

	protected void shutdown() {
		Timer timer = m_Timer;
		m_Timer = _Dead;
		if (null != timer && _Timer != timer && _Dead != timer) {
			// 停掉非共享的计时器
			try {
				timer.cancel();
			} catch (Throwable e) {
				_Logger.error(e.toString(), e);
			}
		}
	}

	@Override
	public boolean destroySignal() {
		shutdown();
		if (m_Executor instanceof ExecutorService) {
			ExecutorService es = (ExecutorService) m_Executor;
			if (es.isTerminated()) {
				return false;
			}
			try {
				// m_Executor.shutdown();
				es.shutdownNow();
			} catch (Throwable e) {
				_Logger.error(e.toString(), e);
			}
			return true;
		}
		if (m_Executor instanceof DestroyableExt) {
			return ((DestroyableExt) m_Executor).destroySignal();
		}
		return false;
	}

	public void destroy() {
		shutdown();
		if (m_Executor instanceof ExecutorService) {
			try {
				((ExecutorService) m_Executor).shutdownNow();
			} catch (Throwable e) {
				_Logger.error(e.toString(), e);
			}
		} else if (m_Executor instanceof Destroyable) {
			((Destroyable) m_Executor).destroy();
		}
	}

	/**
	 * 检查指定的条件或事件是否都具备
	 * 
	 * @param options
	 *            选项CONDITION_*或EVENT_*
	 * @return 具备则返回true
	 */
	public boolean isReady(int options) {
		return true;
	}

	/**
	 * 定时器任务
	 * 
	 * @author liangyi
	 * 
	 */
	class TimerTaskExt extends TimerTask {
		final TaskImpl m_Tx;

		public TimerTaskExt(TaskImpl tx) {
			this.m_Tx = tx;
		}

		@Override
		public void run() {
			try {
				try {
					if (!m_Tx.isContinue()) {
						cancel();
						_Logger.info("任务已结束 " + m_Tx);
						return;
					}
					if (m_Tx.isReady()) {
						execute(m_Tx);
					}
				} catch (RejectedExecutionException e) {
					// if (m_Executor instanceof ExecutorService
					// && ((ExecutorService) m_Executor).isShutdown()) {
					if (isShutdown()) {
						// 线程池停掉了
						cancel();
						_Logger.warn("线程池已关（略过执行） " + m_Tx);
						return;
					}
					// 线程池的任务满了，若是只执行一次的任务放回定时器5秒后再试！
					if (m_Tx.isOnce()) {
						try {
							_Logger.error("线程池忙（5秒再试） " + m_Tx, e);
							TimerTaskExt t = new TimerTaskExt(m_Tx);
							getTimer(false).schedule(t, 5 * 1000);
						} catch (IllegalStateException ee) {
							_Logger.error("定时器异常（略过执行） " + m_Tx, ee);
						}
					} else {
						_Logger.error("线程池忙（等下周期） " + m_Tx, e);
					}
				}
			} catch (Throwable e) {
				_Logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 后台运行的任务
	 * 
	 * @author liangyi
	 * 
	 */
	public class TaskImpl implements Task {
		/** 待执行的工作 */
		final Runnable m_Worker;
		/** 选项 TaskExecutor.EVENT_xxx，TaskExecutor.CONDITION_xxx */
		final int m_Options;
		/** 状态 STATE_xxx */
		volatile int m_State;
		/** 任务总执行次数 */
		volatile int m_Frequency;
		/** 最后一次执行完的时间 */
		volatile long m_LastFinish;

		/**
		 * 状态-运行中
		 */
		static final int STATE_RUNNING = 0x01;
		/**
		 * 状态-已取消
		 */
		static final int STATE_CANCEL = 0x02;
		/**
		 * 状态-已完成
		 */
		static final int STATE_FINISH = 0x04;
		/**
		 * 状态-只运行一次
		 */
		static final int STATE_ONCE = 0x10;

		TaskImpl(Runnable task, int options) {
			this.m_Worker = task;
			this.m_Options = options;
			m_LastFinish = -1;
		}

		public void run() {
			// if (!isReady()) {
			// return;
			// }
			synchronized (this) {
				if (STATE_RUNNING == (STATE_RUNNING & m_State)) {
					// 还在运行中，忽略这次的任务:(
					return;
				}
				m_State |= STATE_RUNNING;
			}
			++m_Frequency;
			// 标记这是后台任务
			m_Mark.set(BackgroundExecutor.this);
			try {
				this.m_Worker.run();
			} catch (AbortTaskExecute e) {
				// AbortTaskExecute异常结束任务
				finish();
				_Logger.error("结束运行 " + this, e);
			} catch (Throwable e) {
				_Logger.error(e.getMessage(), e);
			}
			// m_Mark.remove();
			m_LastFinish = System.currentTimeMillis();
			m_Mark.set(null); // 直接置null更合算
			synchronized (this) {
				m_State &= ~STATE_RUNNING;
				if (STATE_ONCE == (m_State & STATE_ONCE)) {
					finish();
				}
			}
		}

		/**
		 * 是否有指定选项
		 * 
		 * @param option
		 *            选项CONDITION_*或EVENT_*
		 * @return 有则返回true
		 */
		public boolean isOption(int option) {
			return (option == (option & this.m_Options));
		}

		/**
		 * 是否具备执行的条件了
		 * 
		 * @return 具备则返回true
		 */
		public boolean isReady() {
			return BackgroundExecutor.this.isReady(this.m_Options);
		}

		/**
		 * 任务是否取消了
		 * 
		 * @return 是取消则返回true
		 */
		public boolean isCancel() {
			return (STATE_CANCEL == (STATE_CANCEL & m_State));
		}

		/**
		 * 任务工作是否执行中
		 * 
		 * @return 若任务的工作正在执行中返回true
		 */
		public boolean isRunning() {
			return (STATE_RUNNING == (STATE_RUNNING & m_State));
		}

		/**
		 * 取消任务（若其还是等待执行中）
		 */
		synchronized public void cancel() {
			m_State |= STATE_CANCEL;
		}

		/**
		 * 执行次数
		 * 
		 * @return 任务总执行次数
		 */
		public int getFrequency() {
			return m_Frequency;
		}

		public Date getLastFinish() {
			return (-1 == m_LastFinish) ? null : new Date(m_LastFinish);
		}

		public boolean isFinish() {
			return (STATE_FINISH == (STATE_FINISH & m_State));
		}

		synchronized public void finish() {
			m_State |= STATE_FINISH;
		}

		/**
		 * 是否继续执行（状态不是结束或取消且执行器未关闭的）
		 */
		public boolean isContinue() {
			return 0 == ((STATE_FINISH | STATE_CANCEL) & m_State) && !isShutdown();
		}

		/**
		 * 设置为只运行一次的状态（运行后置STATE_FINISH）
		 */
		public void setOnce() {
			m_State |= STATE_ONCE;
		}

		/**
		 * 只运行一次的任务
		 */
		public boolean isOnce() {
			return (STATE_ONCE == (m_State & STATE_ONCE));
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(160);
			sb.append("{worker:").append(m_Worker).append(",fre:").append(m_Frequency)
					.append(",last:");
			Timepoint.formatTimestamp(m_LastFinish, sb);
			sb.append(",ops:").append(m_Options).append(",state:").append(m_State).append(",pool:");
			BackgroundExecutor.this.toString(sb);
			sb.append("}");
			return sb.toString();
		}
	}
}
