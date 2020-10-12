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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.DestroyableExt;
import cn.weforward.common.Nameable;
import cn.weforward.common.Quotas;
import cn.weforward.common.Quotas.Governable;
import cn.weforward.common.execption.AbortException;
import cn.weforward.common.sys.StackTracer;
import cn.weforward.common.util.LinkedPool.Element;

/**
 * 简单线程池
 * 
 * @author liangyi
 *
 */
public class ThreadPool implements Executor, DestroyableExt, Governable, Nameable {
	final public static Logger _Logger = LoggerFactory.getLogger(ThreadPool.class);
	/** 池 */
	protected LinkedPool<ThreadExt> m_Pool;
	/** 名称 */
	protected String m_Name;
	/** 使用配额控制 */
	protected Quotas m_Quotas;
	/** 并发数（若为MIN_VALUE则不计算并发数且不受配额限制） */
	protected AtomicInteger m_Concurrent = new AtomicInteger();
	// protected volatile int m_Concurrent;
	/** 是否后台线程模式 */
	protected boolean m_Daemon;

	/**
	 * 使用配额共用的线程池
	 * 
	 * @param group
	 *            共用的池
	 */
	public ThreadPool(ThreadPool group) {
		if (null == group.m_Quotas) {
			group.setQuotas(new Quotas.SimpleQuotas(group.m_Pool.getMaxSize()));
		}
		m_Quotas = group.m_Quotas;
		m_Pool = group.m_Pool;
		m_Name = group.m_Name;
	}

	/**
	 * 创建线程池
	 * 
	 * @param maxThreads
	 *            最大线程数
	 * @param name
	 *            名称
	 */
	public ThreadPool(int maxThreads, String name) {
		m_Name = name;
		m_Pool = new LinkedPool<ThreadExt>(maxThreads, name) {
			@Override
			protected ThreadExt onCreateElement() {
				// 创建（后台）线程
				ThreadExt thread = new ThreadExt(m_Pool.getName() + "-" + getCreateTimes());
				thread.setDaemon(m_Daemon);
				thread.start();
				_Logger.info("[" + thread.getName() + "]created.");
				return thread;
			}

			@Override
			protected void onCloseElement(Element<ThreadExt> element) {
				ThreadExt t = element.resource;
				if (null != t && t.isAlive()) {
					t.interrupt();
					_Logger.info("close " + element);
				}
			}

			@Override
			protected void onIdle(Element<ThreadExt> element, int idle) {
				// super.onIdle(element, idle);
				// 释放掉空闲的线程
				ThreadExt thread = element.getResource();
				if (null != thread && thread.isAlive()) {
					_Logger.info("idel " + element);
					element.clear();
					thread.interrupt();
				}
			}

			@Override
			protected void onLongtime(Element<ThreadExt> element, int useup) {
				// 输出状态
				StringBuilder sb = new StringBuilder(128);
				Thread t = element.resource;
				if (null == t) {
					t = element.thread;
				}
				sb.append("{占资源时间太长(ms):").append(useup);
				if (null == t) {
					sb.append(",n:").append(getName());
					sb.append(",e:").append(element);
					sb.append("}");
				} else {
					sb.append(",t:").append(t).append("}\n");
					// 输出其调用堆栈
					StackTracer.printStackTrace(t, sb);
				}
				_Logger.warn(sb.toString());
			}
		};
		// m_Pool.setQueueLengthMax(m_Pool.getMaxSize());
		// m_Pool.setQueueTimeout(0);
		// Shutdown.register(this);

		// 默认线程空闲30分钟则结束
		setIdle(30 * 60);
	}

	/**
	 * 启用独立配额限制并发线程数
	 */
	public ThreadPool byQuota() {
		return new ThreadPool(this);
	}

	public void setName(String name) {
		m_Name = name;
	}

	public String getName() {
		return m_Name;
	}

	/**
	 * 配置是否后台线程模式
	 */
	public void setDaemon(boolean daemon) {
		m_Daemon = daemon;
	}

	/**
	 * 设置线程空闲回收时间
	 * 
	 * @param seconds
	 *            空闲时间（单位为秒）
	 */
	public void setIdle(int seconds) {
		m_Pool.setIdle(seconds);
	}

	/**
	 * 使用池中项的（检查）超时值
	 *
	 * @param seconds
	 *            超时值（单位为秒）
	 */
	public void setLongtime(int seconds) {
		m_Pool.setLongtime(seconds);
	}

	public void setQuotas(Quotas quotas) {
		m_Quotas = (quotas);
	}

	/**
	 * 指定是否不受配额限制
	 * 
	 * @param nolimit
	 *            是否不受配额限制
	 */
	public void setNolimit(boolean nolimit) {
		if (nolimit) {
			// m_Concurrent = Integer.MIN_VALUE;
			m_Concurrent = null;
		} else if (null == m_Concurrent) {
			// m_Concurrent = 0;
			m_Concurrent = new AtomicInteger();
		}
	}

	/**
	 * 设置最大排队项数（=池的最大项数+等待的项数）
	 *
	 * @param size
	 *            最大排队数，必须不小于QueueMax
	 */
	public void setQueueLengthMax(int size) {
		m_Pool.setQueueLengthMax(size);
	}

	/**
	 * 在池中取得空闲项的排队超时值
	 *
	 * @param mills
	 *            超时值（单位为毫秒），若=0为不等待
	 */
	public void setQueueTimeout(int mills) {
		m_Pool.setQueueTimeout(mills);
	}

	public int getThreads() {
		LinkedPool<ThreadExt> pool = m_Pool;
		return null == pool ? 0 : pool.getInUseCount();
	}

	public int getMaxThreads() {
		LinkedPool<ThreadExt> pool = m_Pool;
		return null == pool ? 0 : pool.getMaxSize();
	}

	public int getInQueue() {
		LinkedPool<ThreadExt> pool = m_Pool;
		return null == pool ? 0 : pool.getInQueue();
	}

	@Override
	public void execute(Runnable handler) {
		LinkedPool<ThreadExt> pool = m_Pool;
		if (null == pool) {
			// 线程池已关了
			throw new AbortException("[" + getName() + "] is shutdown." + handler);
		}
		ThreadExt thread = null;
		Quotas quotas = m_Quotas;
		if (null != quotas) {
			AtomicInteger c = m_Concurrent;
			quotas.use(this, null == c ? 0 : c.get());
		}

		try {
			thread = pool.poll();
		} finally {
			if (null == thread && null != quotas) {
				// 没能获取线程，归还配额
				quotas.refund(this);
			}
		}
		if (null == thread) {
			throw new RejectedExecutionException(toString("线程池忙"));
		}
		// 执行command
		thread.submit(handler, this);
		thread = null;
	}

	/**
	 * 任务准备开始执行
	 * 
	 * @param thread
	 *            执行的线程
	 */
	private void begin(ThreadExt thread) {
		// m_Concurrent.incrementAndGet();
		// if (Integer.MIN_VALUE != m_Concurrent) {
		// // 独立限额才计算并发数
		// ++m_Concurrent;
		AtomicInteger c = m_Concurrent;
		if (null != c) {
			// 独立限额才计算并发数
			c.incrementAndGet();
		}
	}

	/**
	 * 任务执行完成，释放回池
	 * 
	 * @param thread
	 *            线程
	 * @param running
	 *            是否继续运行
	 */
	protected void end(ThreadExt thread, boolean running) {
		// m_Concurrent.decrementAndGet();
		// if (Integer.MIN_VALUE != m_Concurrent) {
		// // 独立限额才计算并发数
		// --m_Concurrent;
		AtomicInteger c = m_Concurrent;
		if (null != c) {
			// 独立限额才计算并发数
			c.decrementAndGet();
		}
		Quotas quotas = m_Quotas;
		LinkedPool<ThreadExt> pool = m_Pool;
		if (null != quotas) {
			quotas.refund(this);
		}
		if (null != pool) {
			pool.offer(thread, !running);
		}
	}

	public void shutdown() {
		LinkedPool<ThreadExt> pool = m_Pool;
		if (null != pool) {
			m_Pool = null;
			_Logger.info("shutdown " + pool);
			pool.close();
		}
	}

	@Override
	public void destroy() {
		shutdown();
	}

	@Override
	public boolean destroySignal() {
		// if (m_Pool.getInQueue() <= 0) {
		// return false;
		// }
		LinkedPool<ThreadExt> pool = m_Pool;
		if (null == pool) {
			// 已关闭
			m_Pool = null;
			return false;
		}

		if (pool.abort() > 0) {
			// shutdown池，不允许继续分配资源
			pool.shutdown();
			return true;
		}
		// 已经没有正在执行的线程，关闭池
		shutdown();
		return false;
	}

	@Override
	public String toString() {
		return toString(null);
	}

	public String toString(String msg) {
		StringBuilder sb;
		if (null == msg) {
			sb = new StringBuilder(32);
		} else {
			sb = new StringBuilder(msg.length() + 32);
		}
		sb.append("{n:").append(getName());
		// if (Integer.MIN_VALUE != m_Concurrent) {
		if (null != m_Concurrent) {
			sb.append(",c:").append(m_Concurrent);
		}
		if (null != m_Pool) {
			sb.append(",pool:").append(m_Pool);
		}
		if (null != m_Quotas) {
			sb.append(",quotas:").append(m_Quotas);
		}
		sb.append("}");
		return sb.toString();
	}

	public String dump() {
		StringBuilder sb = StringBuilderPool._8k.poll();
		try {
			sb.append("{n:").append(getName());
			if (null != m_Concurrent) {
				sb.append(",c:").append(m_Concurrent);
			}
			if (null != m_Quotas) {
				sb.append(",quotas:").append(m_Quotas);
			}
			if (null != m_Pool) {
				sb.append(",pool:");
				sb.append(m_Pool.dump());
			}
			sb.append("}");
			return sb.toString();
		} finally {
			StringBuilderPool._8k.offer(sb);
		}
	}

	public List<Thread> getRunThreads() {
		LinkedPool<ThreadExt> pool = m_Pool;
		if (null == pool || 0 == pool.getInUseCount()) {
			return Collections.emptyList();
		}
		List<Thread> threads = new ArrayList<Thread>(pool.getInUseCount());
		synchronized (pool) {
			for (Element<ThreadExt> n = pool.m_InUseChain; null != n; n = n.next) {
				threads.add(n.getResource());
			}
		}
		return threads;
	}

	/**
	 * 池中的线程
	 * 
	 * @author liangyi
	 *
	 */
	public static class ThreadExt extends Thread {
		Runnable m_Handler;
		ThreadPool m_ThreadPool;

		public ThreadExt(String name) {
			super(name);
		}

		public void submit(Runnable handler, ThreadPool pool) {
			synchronized (this) {
				// if (null != this.command && command != this.command) {
				if (null != m_Handler) {
					// 有问题了吧？
					throw new IllegalStateException("有bugs!!上个任务还在执行！{name:" + getName() + ",cur:"
							+ m_Handler + ",submit:" + handler + "}");
				}
				if (!isAlive()) {
					// 线程不是活的？
					pool.end(this, false);
					throw new IllegalStateException(
							"有bugs!!死掉的线程？{name:" + getName() + ",handler:" + handler + "}");
				}
				// 放入任务
				m_Handler = handler;
				m_ThreadPool = pool;
				// 唤醒线程执行
				notify();
			}
			String name = getName();
			String poolName = pool.getName();
			int idx = name.lastIndexOf('-');
			if (null != poolName && poolName.length() > 0) {
				if (0 == idx || (idx >= 0 && !name.regionMatches(0, poolName, 0, idx - 1))) {
					// 换掉线程名
					name = poolName + name.substring(idx);
					setName(name);
				}
			}
		}

		@Override
		public void run() {
			Runnable cmd;
			ThreadPool pool;
			boolean running = true;
			_Logger.info("[" + getName() + "]begin " + hashCode());
			do {
				cmd = null;
				pool = null;
				try {
					synchronized (this) {
						// 看看pool及cmd
						cmd = m_Handler;
						pool = m_ThreadPool;
						if (null == cmd || null == pool) {
							// 等待任务（可能会碰到线程中断标志）
							wait();
							// 再看看
							cmd = m_Handler;
							pool = m_ThreadPool;
						}
						// 拿走后复位
						m_Handler = null;
						m_ThreadPool = null;
					}
					if (null != cmd) {
						if (null != pool) {
							// 执行任务（可能产生线程中断标志，而且InterruptedException被catch）
							pool.begin(this);
							cmd.run();
						} else {
							_Logger.error("cmd非空异常[" + getName() + "]" + cmd);
						}
					}
				} catch (AbortException e) {
					_Logger.info("执行中止{n:" + getName() + ",pool:" + m_ThreadPool + ",handler:"
							+ m_Handler + "}", e);
					running = false;
					// break;
				} catch (InterruptedException e) {
					// 可能是结束事件
					if (null != cmd) {
						_Logger.info("异常中断{n:" + getName() + ",pool:" + m_ThreadPool + ",handler:"
								+ m_Handler + "}");
					}
					running = false;
					// break;
				} catch (Exception e) {
					// 其它异常
					_Logger.info("执行异常{n:" + getName() + ",pool:" + m_ThreadPool + ",handler:"
							+ m_Handler + "}", e);
				} finally {
					running = (running && !isInterrupted());
					// 执行完任务，回池！
					if (null != cmd && null != pool) {
						pool.end(this, running);
					}
				}
				// } while (null != this.threadPool || !isInterrupted());
			} while (running);
			cmd = m_Handler;
			pool = m_ThreadPool;
			if (null != pool || null != cmd) {
				_Logger.info("bugs?{n:" + getName() + ",pool:" + pool + ",handler:" + cmd + "}");
				if (null != pool) {
					pool.end(this, false);
				}
			} else {
				_Logger.info("[" + getName() + "]exit " + hashCode());
			}
		}
	}
}
