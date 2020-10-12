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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.LoggerFactory;

import cn.weforward.common.DestroyableExt;
import cn.weforward.common.crypto.Hex;
import cn.weforward.common.execption.AbortException;
import cn.weforward.common.execption.OverflowException;
import cn.weforward.common.sys.ClockTick;
import cn.weforward.common.sys.StackTracer;

/**
 * 延时（最小间隔1秒）/异步地执行任务的辅助基类
 * 
 * @author liangyi
 * 
 */
public abstract class DelayRunner<E> implements DestroyableExt {
	/** 日志记录器 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(DelayRunner.class);

	/** 用于优化检查输出日志debug、trace、info是否允许代码 */
	public static final boolean _DebugEnabled = _Logger.isDebugEnabled();
	public static final boolean _TraceEnabled = _Logger.isTraceEnabled();
	public static final boolean _InfoEnabled = _Logger.isInfoEnabled();

	/** 任务已关闭 */
	protected static final int STATE_CLOSED = 0x1000;
	/** 停止任务 */
	protected static final int STATE_STOP = 0x0100;
	/** 任务线程是否已启动 */
	protected static final int STATE_READY = 0x0010;
	/** 重启任务线程 */
	protected static final int STATE_RESTART = 0x2000;
	/** 标记任务列表有变化 */
	protected static final int STATE_QUEUE_CHANGED = 0x4000;

	/** 计时器 */
	protected static final ClockTick _Tick = ClockTick.getInstance(1);

	/** 延时执行任务项链表 */
	final protected SinglyLinked<E> m_Tasks;
	/** 锁 */
	final protected ReentrantLock m_Lock = new ReentrantLock();
	/** 等待任务条件 */
	final protected Condition m_WaitForTask = m_Lock.newCondition();

	/** 每次执行任务的最小间隔（秒），若为0不作间隔 */
	protected int m_Interval;
	/** 延时执行任务的最大累积项数（0表示不控制） */
	protected int m_MaxSuspend;

	/** 执行任务的后台工作线程 */
	protected Thread m_Thread;
	/** 状态 STATE_xxx */
	protected volatile int m_State;

	/** 最后执行时间戳（tick） */
	protected int m_LastExecute;
	/** 已完成任务数 */
	protected volatile int m_Completes;
	/** 执行中的任务数 */
	protected volatile int m_Pending;
	/** 执行中的任务 */
	protected volatile SinglyLinked.Node<E> m_PendingTask;
	/** 连续失败数 */
	protected volatile int m_Fails;
	/** 总处理任务数（已完成/失败） */
	protected volatile int m_Total;
	/** 名称 */
	protected String m_Name;

	/**
	 * 子类提供具体的任务执行
	 * 
	 * @param task
	 *            要执行的任务
	 */
	protected abstract void execute(E task) throws Exception;

	/**
	 * 后台任务线程启动时，供子类覆盖进行初始化处理（如：可以在这加载持久化任务）
	 */
	protected void onInit() {
	}

	/**
	 * 后台任务线程终止时，供子类覆盖控制是否完成队列中的任务，调用<code>complete()</code>能完成队列中的任务，或持久化任务
	 */
	protected void onFinally() {
	}

	/**
	 * 执行（批量）任务开始时，供子类覆盖加入事务控制
	 * 
	 * @param size
	 *            这次执行的批量任务数
	 */
	protected void onBegin(int size) {
	}

	/**
	 * 执行（批量）任务结束时，供子类覆盖加入事务控制
	 */
	protected void onEnd() {
	}

	/**
	 * 执行（批量）任务结束时，供子类覆盖加入事务控制
	 * 
	 * @param completes
	 *            批次完成的任务数
	 * @param count
	 *            批次总任务数
	 */
	protected void onEnd(int completes, int count) {
		onEnd();
		m_State |= STATE_QUEUE_CHANGED;
	}

	/**
	 * 标记任务队列的变化已清理过（如：完成持久化）
	 */
	protected void cleanQueue() {
		m_State &= (~STATE_QUEUE_CHANGED);
	}

	/**
	 * 执行（批量）任务错误/异常时，供子类覆盖加入流程/事务控制
	 * 
	 * @param chain
	 *            执行中的任务链
	 * @param e
	 *            出错的异常
	 * @return 返回true表示忽略当前出错的任务继续，false表示中止且丢掉链里的所有任务
	 */
	protected boolean onFail(SinglyLinked.Node<E> chain, Throwable e) {
		_Logger.error("忽略异常继续[" + chain.value + "]" + this, e);
		return true;
	}

	/**
	 * 创建任务链表
	 */
	protected SinglyLinked<E> createTasks() {
		return new SinglyLinked<E>();
	}

	/**
	 * 构造执行间隔为1秒的
	 */
	public DelayRunner() {
		this(1);
	}

	/**
	 * 构造
	 * 
	 * @param interval
	 *            两批任务之间的最小执行间隔（秒），若0为则没有间隔
	 */
	public DelayRunner(int interval) {
		// m_Interval = interval;
		// m_Tasks = createTasks();
		this("", interval);
	}

	/**
	 * 构造
	 * 
	 * @param name
	 *            名称
	 * @param interval
	 *            两批任务之间的最小执行间隔（秒），若0为则没有间隔
	 */
	public DelayRunner(String name, int interval) {
		m_Name = name;
		m_Interval = interval;
		m_Tasks = createTasks();
	}

	/**
	 * 每次执行任务的最小间隔（秒）
	 * 
	 * @param seconds
	 *            控制最小间隔（秒），若0为则没有控制，有任务马上通知执行
	 */
	public void setInterval(int seconds) {
		m_Interval = seconds;
	}

	/**
	 * 每次执行任务的最小间隔（秒）
	 */
	public int getInterval() {
		return m_Interval;
	}

	/**
	 * 延时执行任务的最大累积项数
	 * 
	 * @param limit
	 *            要控制的项数，0表示不控制
	 */
	public void setMaxSuspend(int limit) {
		m_MaxSuspend = limit;
	}

	/**
	 * 延时执行任务的最大累积项数
	 */
	public int getMaxSuspend() {
		return m_MaxSuspend;
	}

	/**
	 * 已完成任务量
	 */
	public int getCompletes() {
		return m_Completes;
	}

	/**
	 * 在执行中的任务量
	 */
	public int getPending() {
		return m_Pending;
	}

	/**
	 * 当前任务执行连续失败数
	 */
	public int getFails() {
		return m_Fails;
	}

	/**
	 * 总处理任务数据（完成及失败）
	 */
	public int getTotal() {
		return m_Total;
	}

	/**
	 * 把任务提交到队列中
	 * 
	 * @param task
	 *            要提交的任务
	 * @return 成功则返回true
	 */
	public boolean submit(E task) {
		return submit(task, false);
	}

	/**
	 * 把任务提交到队列中
	 * 
	 * @param task
	 *            要提交的任务
	 * @param absent
	 *            若为true则不重复的才加入队列
	 * @return 成功则返回true
	 */
	public boolean submit(E task, boolean absent) {
		return null != submitInternal(task, absent);
	}

	/**
	 * 取得任务（还在等待的）
	 */
	public Iterator<E> getQueueTasks() {
		return new SinglyLinked.LinkedIterator<E>(m_Tasks.getHead());
	}

	/**
	 * 取得正在处理的任务
	 */
	public Iterator<E> getPendingTasks() {
		SinglyLinked.Node<E> node = m_PendingTask;
		if (null == node) {
			// return Collections.emptyList().iterator();
			List<E> ls = Collections.emptyList();
			return ls.iterator();
		}
		return new SinglyLinked.LinkedIterator<E>(node);
	}

	/**
	 * 批量增加到任务列表
	 * 
	 * @param it
	 *            要加入的任务
	 * @return 总共加入的项数
	 * @throws OverflowException
	 *             若要加入的项超过100W则抛出
	 */
	protected int addAll(Iterator<E> it) {
		int count = m_Tasks.size();
		m_Lock.lock();
		try {
			for (int i = 0; it.hasNext() && i < 1000000; i++) {
				E t = it.next();
				if (null != t) {
					m_Tasks.addTail(t);
				}
			}
			m_State |= STATE_QUEUE_CHANGED;
		} finally {
			m_Lock.unlock();
		}
		count = m_Tasks.size() - count;
		if (it.hasNext()) {
			throw new OverflowException("要加入的任务太多（>" + count + "）");
		}
		return count;
	}

	/**
	 * 把任务提交到队列中
	 * 
	 * @param task
	 *            要提交的任务
	 * @param absent
	 *            若为true则不重复的才加入队列
	 * @return 成功则返回任务项
	 */
	public SinglyLinked.Node<E> submitInternal(E task, boolean absent) {
		m_Lock.lock();
		try {
			if (!startThread(0)) {
				// 执行器已是停止的
				StringBuilder sb = new StringBuilder(128);
				sb.append("已关闭，忽略此项[").append(task).append("]");
				toString(sb).append('\n');
				_Logger.warn(StackTracer.printStackTrace(Thread.currentThread(), sb).toString());
				return null;
			}
			// 加到队列中
			SinglyLinked.Node<E> ret;
			if (absent) {
				// 不加入重复项
				ret = m_Tasks.find(task);
				if (null != ret) {
					// 队列中已有此项，直接返回
					return ret;
				}
				ret = m_Tasks.addTail(task);
			} else {
				ret = m_Tasks.addTail(task);
			}
			m_State |= STATE_QUEUE_CHANGED;
			if (0 == m_LastExecute) {
				// 首先进入的项，给最后刷写时间一个值
				m_LastExecute = _Tick.getTicker();
			}
			if (m_Tasks.size() > 1 && !condition()) {
				// 未到条件不用通知处理线程（任务数要大于1才能不通知，否则可能会死等了）
				return ret;
			}
			// 给等待的线程一个讯号
			m_WaitForTask.signal();
			return ret;
		} finally {
			m_Lock.unlock();
		}
	}

	/**
	 * 是否达到可执行任务的条件
	 */
	private boolean condition() {
		if (m_MaxSuspend > 0 && m_Tasks.size() >= m_MaxSuspend) {
			// 达到积压条件
			return true;
		}
		if (0 == m_Interval || _Tick.getTicker() >= (m_Interval + m_LastExecute)) {
			// 达到最小间隔条件
			return true;
		}
		return false;
	}

	public boolean isStop() {
		return isStop(m_State);
	}

	private static boolean isStop(int state) {
		return (STATE_CLOSED == (STATE_CLOSED & state) || STATE_STOP == (STATE_STOP & state));
	}

	/**
	 * 任务列表有变化
	 */
	public boolean isQueueChanged() {
		return STATE_QUEUE_CHANGED == (m_State & STATE_QUEUE_CHANGED);
	}

	/**
	 * 启动任务线程
	 * 
	 * @param delay
	 *            延迟时间（毫秒）
	 * @return
	 */
	private boolean startThread(final int delay) {
		if (isStop()) {
			return false;
		}
		if (STATE_READY == (STATE_READY & m_State)) {
			return true;
		}

		Thread thread = m_Thread;
		if (null != thread && thread.isAlive()) {
			// throw new IllegalStateException("上个进程还");
			// 不会吧，线程还在活动中啊？
			StringBuilder sb = new StringBuilder(128);
			sb.append("线程工作中，但与状态不相配");
			toString(sb).append('\n');
			_Logger.error(StackTracer.printStackTrace(thread, sb).toString());
			return true;
		}

		thread = new Thread() {
			@Override
			public void run() {
				if (delay > 0) {
					try {
						sleep(delay);
					} catch (InterruptedException e) {
						// 被中断了
						return;
					}
				}
				if (StringUtil.isEmpty(m_Name)) {
					setName("DR-" + Hex.toHex(DelayRunner.this.hashCode()) + "-"
							+ Hex.toHex(hashCode()));
				} else {
					setName("DR-" + m_Name);
				}
				m_Thread = this;
				// _Logger.info(this + " running.");
				m_Fails = 0;
				try {
					daemon();
				} finally {
					boolean restart = (STATE_RESTART == (STATE_RESTART & m_State));
					m_Lock.lock();
					try {
						m_Thread = null;
						if (restart) {
							// 要重启任务线程哦
							m_State &= ~(STATE_CLOSED | STATE_READY | STATE_STOP | STATE_RESTART);
							// 重启任务线程
							startThread(0);
						} else {
							m_State |= STATE_CLOSED;
						}
					} finally {
						m_Lock.unlock();
					}
					if (restart) {
						_Logger.warn("重启任务线程 [" + this + "]" + DelayRunner.this);
					}
				}
				// _Logger.info(this + " done.");
			}
		};
		thread.setDaemon(true);
		thread.start();

		// 只要线程start就加上标识
		m_State |= STATE_READY;
		return true;
	}

	/**
	 * 启动执行器
	 */
	public void start() {
		start(0);
	}

	/**
	 * 启动执行器
	 * 
	 * @param delay
	 *            延时时间（毫米）
	 */
	public void start(int delay) {
		m_Lock.lock();
		try {
			if (isStop()) {
				// 已是停止的状态，重置状态就能再启动
				m_State &= ~(STATE_CLOSED | STATE_READY | STATE_STOP | STATE_RESTART);
				_Logger.info("已重置停止状态：" + this);
			}
		} finally {
			m_Lock.unlock();
		}
		startThread(delay);
	}

	/**
	 * 停止执行器
	 */
	public void stop() {
		Thread thread;
		m_Lock.lock();
		try {
			thread = m_Thread;
			// if (null == thread || !thread.isAlive()) {
			// // 已是结束的
			// return;
			// }
			// m_Thread = null;
			if (STATE_CLOSED == (STATE_CLOSED & m_State) || null == thread || !thread.isAlive()) {
				// 已是结束的
				return;
			}
			m_State |= STATE_STOP;
			m_WaitForTask.signal();
		} finally {
			m_Lock.unlock();
		}

		// 等待后台线程结束(至多等待30秒)
		try {
			thread.join(30 * 1000);
		} catch (InterruptedException e) {
			_Logger.warn(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 重启任务线程
	 */
	public void restart() {
		Thread thread;
		m_Lock.lock();
		try {
			thread = m_Thread;
			if (!(STATE_CLOSED == (STATE_CLOSED & m_State) || null == thread
					|| !thread.isAlive())) {
				// 发送信号停止任务线程（标明要重启线程）
				m_State |= (STATE_STOP | STATE_RESTART);
				m_WaitForTask.signal();
				return;
			}
			// 直接启动就好了
			startThread(0);
		} finally {
			m_Lock.unlock();
		}
	}

	/**
	 * 执行任务
	 * 
	 * @param task
	 *            任务（链表节点）
	 * @throws Exception
	 */
	protected void execute(SinglyLinked.Node<E> task) throws Exception {
		execute(task.value);
	}

	/**
	 * 执行任务
	 */
	public void executeTasks() {
		SinglyLinked.Node<E> first;
		int count;
		m_Lock.lock();
		try {
			count = m_Pending = m_Tasks.size();
			first = m_Tasks.detach();
			if (null == first) {
				// 没有需要更新的项
				return;
			}
			// m_State |= STATE_QUEUE_CHANGED;
		} finally {
			m_Lock.unlock();
		}
		if (_TraceEnabled) {
			_Logger.trace("DelayRunning...");
		}

		// 历遍（链表中）的项执行
		m_PendingTask = first;
		onBegin(count);
		int completes = 0;
		while (null != first) {
			int state = m_State;
			if (STATE_RESTART == (STATE_RESTART & state) && isStop(state)) {
				// 收到信号说要重启任务线程，把任务加回链接，结束处理
				m_Lock.lock();
				try {
					m_Tasks.attachToHead(first);
				} finally {
					m_Lock.unlock();
				}
				if (_TraceEnabled) {
					_Logger.trace("put to chain on restart.");
				}
				break;
			}
			try {
				// 执行
				--m_Pending;
				++m_Total;
				execute(first);
				++completes;
				++m_Completes;
				m_Fails = 0;
			} catch (Throwable e) {
				++m_Fails;
				if (isStop() && e instanceof AbortException) {
					_Logger.error("已停止状态下中止之后所有的任务执行[" + first.value + "]" + this, e);
					break;
				}
				if (!onFail(first, e)) {
					_Logger.error("异常且结束任务队列[" + first.value + "]" + this, e);
					break;
				}
				// _Logger.error("执行任务异常忽略并继续[" + first.value + "]" + this, e);
			}
			first = first.getNext();
			// m_LastExecute = System.currentTimeMillis();
		}
		m_PendingTask = null;
		m_Pending = 0;
		onEnd(completes, count);
		// m_LastExecute = System.currentTimeMillis();
		m_LastExecute = _Tick.getTicker();
	}

	/**
	 * 后台线程方式等待任务执行
	 */
	protected void daemon() {
		_Logger.info("beginning [" + m_Thread + "]");
		onInit();
		_daemon_loop: do {
			m_Lock.lock();
			try {
				// 等待处理条件
				// while (null != m_Thread) {
				while (STATE_STOP != (STATE_STOP & m_State)) {
					if (m_MaxSuspend > 0 && m_Tasks.size() >= m_MaxSuspend) {
						if (_Logger.isInfoEnabled()) {
							_Logger.info("Over suspends: " + m_MaxSuspend + "/" + m_Tasks.size());
						}
						// 到达最大累积项，执行
						break;
					}

					int interval = 0;
					if (!m_Tasks.isEmpty()) {
						// 队列不为空
						if (0 == m_Interval) {
							// 没有间隔控制，执行
							break;
						}

						// 计算上次处理的间隔，控制执行的频率
						// interval = (int) (System.currentTimeMillis() -
						// m_LastExecute);
						interval = _Tick.getTicker() - m_LastExecute;
						if (interval >= m_Interval) {
							if (_DebugEnabled) {
								_Logger.debug("DelayFlush signal." + interval);
							}
							// 上次执行后超过间隔时间，执行
							break;
						}
						// 等待一小会吧
						interval = m_Interval - interval;
					}

					// 没到要处理的条件，等等（唤醒或到时间）吧
					if (_DebugEnabled) {
						_Logger.debug("Waiting... " + interval);
					}
					try {
						if (interval < 1) {
							m_WaitForTask.await();
						} else {
							m_WaitForTask.await(interval, TimeUnit.SECONDS);
						}
					} catch (InterruptedException e) {
						// _Logger.error(Misc.printStackTrace(e));
						// Thread.currentThread().interrupt();
						_Logger.warn(toString(new StringBuilder("执行器被中断")).toString());
						break _daemon_loop;
					}
				}
			} finally {
				m_Lock.unlock();
			}
			try {
				// 执行任务
				executeTasks();
			} catch (Throwable e) {
				_Logger.error("执行任务异常：" + this, e);
			}
			// } while (null != m_Thread);
			// m_Thread = null;
		} while (STATE_STOP != (STATE_STOP & m_State));
		onFinally();
		_Logger.info("end [" + m_Thread + "]");
	}

	@Override
	public boolean destroySignal() {
		m_Lock.lock();
		try {
			if (!m_Tasks.isEmpty()) {
				m_LastExecute = _Tick.getTicker() - (1 + m_Interval);
				m_WaitForTask.signal();
			}
		} finally {
			m_Lock.unlock();
		}
		Thread thread = m_Thread;
		if (null != thread) {
			try {
				thread.interrupt();
			} catch (Throwable e) {
			}
		} else if (isStop()) {
			return false;
		}
		if (!m_Tasks.isEmpty() || null != m_PendingTask) {
			_Logger.info(toString());
			return true;
		}
		return false;
	}

	@Override
	public void destroy() {
		stop();
	}

	/**
	 * 处理完所有任务
	 */
	protected void complete() {
		while (!m_Tasks.isEmpty()) {
			executeTasks();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			return toString(builder).toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("{name:").append(m_Name).append(",state:").append(m_State).append(",queue:")
				.append(m_Tasks.size()).append(",pending:").append(getPending())
				.append(",complete:").append(getCompletes()).append(",fails:").append(getFails())
				.append(",total:").append(getTotal()).append(",interval:").append(m_Interval)
				.append(",max:").append(m_MaxSuspend).append(",thread:").append(m_Thread)
				.append("}");
		return sb;
	}

}
