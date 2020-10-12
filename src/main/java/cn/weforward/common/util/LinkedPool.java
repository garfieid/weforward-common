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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.execption.AbortException;
import cn.weforward.common.sys.StackTracer;

/**
 * 使用链表结构的资源池管理器
 * 
 * @param <E>
 *            放入池中的资源类型
 * @author liangyi
 * 
 */
public class LinkedPool<E> {
	/** 状态 - 空闲的 */
	public final static int STATE_IDLE = 0;
	/** 状态 - 使用中 */
	public final static int STATE_INUSE = 1;
	/** 状态 - 空的 */
	public final static int STATE_EMPTY = 2;
	/** 状态 - 空闲后未释放的项，还一直空闲 */
	public final static int STATE_OLD = 3;
	/** 状态 - 检查到使用时间有点长（1/2的时间过长值） */
	public final static int STATE_DELAY = 10;
	/** 状态 - 占用太长 */
	public final static int STATE_LONGTIME = 20;

	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(LinkedPool.class);

	/** 用于检查处理队列的定时器 */
	protected final static Timer _Timer = new Timer("LinkedPool-Timer", true);

	/** 最大等待（进入队列）的时间（毫秒） */
	protected int m_QueueTimeout;
	/** 最大等待数（不包含当前并发处理中） */
	protected int m_QueueLengthMax;
	/** 池的最大资源项数 */
	protected int m_MaxSize;

	/** 可使用的资源表（链表首项） */
	protected volatile Element<E> m_ResourceChain;
	/** 使用中的资源表（链表首项） */
	protected volatile Element<E> m_InUseChain;
	/** 当前正在使用（链表中）的项数 */
	protected volatile int m_InUseCount;

	/** 在等待及处理中的数 */
	protected final AtomicInteger m_InQueue = new AtomicInteger();
	/** 名称 */
	protected final String m_Name;
	/** 创建计数 */
	protected volatile int m_CreateTimes;

	/** 定时检查可用表空闲项 */
	protected IdleChecker m_IdleChecker;
	/** 定时检查使用表占用过长项 */
	protected LongtimeChecker m_LongtimeChecker;

	/**
	 * 构造池
	 * 
	 * @param maxSize
	 *            池的最大项
	 * @param name
	 *            指定池的名称
	 */
	public LinkedPool(int maxSize, String name) {
		m_Name = (null == name) ? "" : name;
		m_QueueTimeout = 0;
		m_MaxSize = maxSize;
		m_QueueLengthMax = maxSize;
	}

	/**
	 * 名称
	 */
	public String getName() {
		return m_Name;
	}

	// /**
	// * 以指定的资源列表创建池
	// *
	// * @param resources
	// * 置入池的资源列表
	// */
	// @SuppressWarnings("unchecked")
	// synchronized public void create(Collection<E> resources) {
	// if (null == resources || resources.size() == 0) {
	// throw new IllegalArgumentException("项数不能小于1");
	// }
	//
	// int queueMax = resources.size();
	// if (m_QueueLengthMax < queueMax) {
	// m_QueueLengthMax = queueMax * 3;
	// }
	// // boolean reinit = (null != m_Queue);
	// // 创建换乘器队列
	// m_Queue = new ArrayBlockingQueue<Element<E>>(queueMax);
	// m_InProcess = new Element[queueMax];
	// for (E tr : resources) {
	// if (null == tr) {
	// throw new IllegalArgumentException("置入池的列表有null项，于" + m_Queue.size());
	// }
	// m_Queue.offer(new Element<E>(tr));
	// }
	// // if (!reinit) {
	// // // 注册新的定时器（每10秒检查一下队列）
	// // _Timer.schedule(m_TimeoutChecker, 10 * 1000, 10 * 1000);
	// // }
	// }

	/**
	 * 关闭池
	 */
	synchronized public void close() {
		// 中断使用资源
		abort();
		// 关闭可用资源
		for (Element<E> n = m_ResourceChain; null != n; n = n.next) {
			onCloseElement(n);
		}
		m_ResourceChain = null;
	}

	/**
	 * 关闭在使用中的资源（异步）
	 * 
	 * @return 还在使用中的项数
	 */
	synchronized public int abort() {
		// 关闭使用中的资源
		for (Element<E> n = m_InUseChain; null != n; n = n.next) {
			onCloseElement(n);
		}
		return getInUseCount();
	}

	/**
	 * 关停池，不允许继续分配资源
	 */
	public void shutdown() {
		// FIXME 最简单粗暴的方法就是直接置零queueLengthMax
		m_QueueLengthMax = 0;
	}

	/**
	 * 子类覆盖此方法动态创建池的资源项
	 *
	 * @return 新建的项
	 */
	protected E onCreateElement() {
		return null;
	}

	/**
	 * 关闭资源项
	 *
	 * @param element
	 *            要关闭的项
	 */
	protected void onCloseElement(Element<E> element) {
	}

	/**
	 * 项空闲
	 *
	 * @param element
	 *            空闲的项
	 * @param idle
	 *            空闲值
	 */
	protected void onIdle(Element<E> element, int idle) {
		if (_Logger.isWarnEnabled()) {
			// 输出状态
			StringBuilder sb = new StringBuilder(128);
			sb.append('[').append(m_Name).append("]空闲(").append(idle).append("ms)");
			_Logger.warn(sb.toString());
		}
		element.state = STATE_OLD;
	}

	// /**
	// * 项使用将超时（1/2超时值）
	// *
	// * @param element
	// * 将超时的项
	// * @param useup
	// * 使用时间（毫秒）
	// */
	// protected void onDelay(Element<E> element, int useup) {
	// if (_Logger.isWarnEnabled()) {
	// // 输出状态
	// Thread thread = element.thread;
	// StringBuilder sb = new StringBuilder(128);
	// sb.append(m_Name).append('/').append(thread).append("占资源时间稍长(").append(useup)
	// .append("ms)---->\n");
	// if (null != thread) {
	// // 输出其调用堆栈
	// sb.append("\ttrace stack--->\n");
	// Misc.printStackTrace(thread, sb);
	// }
	// // element.routing.dump(sb);
	// _Logger.warn(sb.toString());
	// }
	// }

	/**
	 * 资源项占用时间过长
	 *
	 * @param element
	 *            耗时的资源项
	 * @param useup
	 *            使用时间（毫秒）
	 */
	protected void onLongtime(Element<E> element, int useup) {
		if (_Logger.isWarnEnabled()) {
			// 输出状态
			Thread thread = element.thread;
			StringBuilder sb = new StringBuilder(128);
			sb.append('[').append(m_Name).append(']').append(thread).append("占资源时间太长(")
					.append(useup).append("ms)--->\n");
			// element.routing.dump(sb);
			if (null != thread) {
				// 输出其调用堆栈
				sb.append("\ttrace stack--->\n");
				StackTracer.printStackTrace(thread, sb);
			}
			_Logger.warn(sb.toString());
		}
	}

	/**
	 * 池的最大项数
	 */
	public int getMaxSize() {
		return m_MaxSize;
	}

	/**
	 * 设置最大排队项数（=池的最大项数+等待的项数）
	 *
	 * @param size
	 *            最大排队数，必须不小于QueueMax
	 */
	synchronized public void setQueueLengthMax(int size) {
		if (size < getMaxSize()) {
			throw new IllegalArgumentException(
					"size必须大于或等于maxSize(" + getMaxSize() + "), 但它=" + size);
		}
		m_QueueLengthMax = size;
	}

	public int getQueueLengthMax() {
		return m_QueueLengthMax;
	}

	/**
	 * 在池中取得空闲项的排队超时值
	 *
	 * @param mills
	 *            超时值（单位为毫秒），若=0为不等待
	 */
	synchronized public void setQueueTimeout(int mills) {
		if (mills < 0) {
			throw new IllegalArgumentException("参数必须不小于0");
		}
		m_QueueTimeout = mills;
	}

	public int getQueueTimeout() {
		return m_QueueTimeout;
	}

	/**
	 * 设置池中项空闲时间检查
	 *
	 * @param seconds
	 *            空闲时间（单位为秒），若=0为不检查空闲
	 */
	synchronized public void setIdle(int seconds) {
		if (seconds < 0) {
			throw new IllegalArgumentException("参数必须不小于0");
		}
		if (null != m_IdleChecker) {
			m_IdleChecker.cancel();
			m_IdleChecker = null;
		}
		if (seconds > 0) {
			m_IdleChecker = new IdleChecker(seconds);
		}
	}

	public int getIdle() {
		return (null == m_IdleChecker) ? 0 : m_IdleChecker.m_Idle;
	}

	/**
	 * 使用池中项的（检查）超时值
	 *
	 * @param seconds
	 *            超时值（单位为秒），若为0则不检查
	 */
	synchronized public void setLongtime(int seconds) {
		if (seconds < 0) {
			throw new IllegalArgumentException("参数必须不小于0");
		}
		if (null != m_LongtimeChecker && m_LongtimeChecker.getLongtime() == seconds) {
			// 没变
			return;
		}
		if (null != m_LongtimeChecker) {
			m_LongtimeChecker.cancel();
			m_LongtimeChecker = null;
		}
		if (seconds < 1) {
			// 关闭检查
			return;
		}
		m_LongtimeChecker = new LongtimeChecker(seconds);
	}

	public int getLongtime() {
		return (null == m_LongtimeChecker) ? 0 : m_LongtimeChecker.m_Longtime;
	}

	/**
	 * 在等待及处理中的项数
	 */
	public int getInQueue() {
		return m_InQueue.get();
	}

	public int getCreateTimes() {
		return m_CreateTimes;
	}

	/**
	 * 使用中的项数
	 */
	public int getInUseCount() {
		return m_InUseCount;
	}

	/**
	 * 由池取得队列项，通常使用的代码为：
	 *
	 * <pre>
	 * E handler=null;
	 * try {
	 * handler=queue.poll();
	 * if(null==handler){
	 * throw BusyException("服务器忙");
	 * }
	 * 对handler的业务调用...
	 * }finally {
	 * queue.offer(handler);
	 * }
	 * </pre>
	 *
	 * @return 空闲的项，否则返回null
	 */
	public E poll() {
		Element<E> element = null;
		E resource = null;
		// 控制总并发
		int count = m_InQueue.incrementAndGet();
		try {
			if (count > m_QueueLengthMax) {
				if (m_QueueLengthMax > 0) {
					_Logger.warn("等待数过多 {n:" + m_Name + ",q-in:" + count + ",q-len:"
							+ getQueueLengthMax() + ",thread:" + Thread.currentThread() + "}");
				} else {
					_Logger.warn("池已关停 {n:" + m_Name + ",q-in:" + count + ",thread:"
							+ Thread.currentThread() + "}");
				}
				return null;
			}

			// 等待池资源
			synchronized (this) {
				element = m_ResourceChain;
				if (null == element) {
					// 可用表空
					if (m_InUseCount < m_MaxSize) {
						// 动态创建项
						element = new Element<E>();
					} else if (m_QueueTimeout <= 0) {
						_Logger.warn("无空闲项 {n:" + m_Name + ",c:" + count + ",thread:"
								+ Thread.currentThread() + "}");
						return null;
					} else {
						// 只好等了
						long ts = System.currentTimeMillis();
						for (; null == element; element = m_ResourceChain) {
							// 等待
							long v = (System.currentTimeMillis() - ts - m_QueueTimeout);
							if (v <= 0) {
								_Logger.warn("等待超时 {n:" + m_Name + ",c:" + count + ",thread:"
										+ Thread.currentThread() + "}");
								return null;
							}
							this.wait(v);
						}
					}
				}
				m_ResourceChain = element.next;
				// （由可用资源链表）移到正在处理链表
				use(element);
				resource = element.resource;
				++m_InUseCount;
			}
			if (null == resource) {
				synchronized (element) {
					resource = element.resource;
					if (null == resource) {
						// 若项未创建，调用onCreateElement创建
						++m_CreateTimes;
						resource = onCreateElement();
						element.resource = resource;
					}
				}
				if (_Logger.isTraceEnabled()) {
					_Logger.trace(m_Name + " 创建项：" + element.resource);
				}
			}
			element = null;
			return resource;
		} catch (InterruptedException e) {
			_Logger.error(StackTracer.printStackTrace(e,
					(new StringBuilder(m_Name)).append("进入失败：").append(Thread.currentThread()))
					.toString());
			// Thread.currentThread().interrupt();
			throw new AbortException("等待进入时中断" + this);
		} finally {
			if (null == resource) {
				// 未能成功获取池资源，inQueue计数减一
				m_InQueue.decrementAndGet();
			}
			if (null != element) {
				// 由正在处理列表移除
				free(element);
			}
		}
	}

	/**
	 * 把用完的资源放回池中
	 *
	 * @param resource
	 *            用完的资源项
	 */
	public void offer(E resource) {
		offer(resource, false);
	}

	/**
	 * 把用完的资源放回池
	 *
	 * @param resource
	 *            用完的资源项
	 * @param empty
	 *            是否在池中清空此项
	 */
	public void offer(E resource, boolean empty) {
		if (null == resource) {
			return;
		}

		Element<E> processer = null;
		// 由正在处理链表查找并移回资源链表
		synchronized (this) {
			Element<E> f = m_InUseChain;
			for (Element<E> n = f; null != n; n = n.next) {
				if (resource == n.resource) {
					// 找到了，先在使用表移除
					if (n == m_InUseChain) {
						// 就是首项
						m_InUseChain = n.next;
					} else {
						f.next = n.next;
					}
					processer = n;
					// 标记为空闲
					processer.idle();
					if (empty) {
						// 清掉项
						processer.clear();
					}
					// 然后放回资源表
					processer.next = m_ResourceChain;
					m_ResourceChain = processer;
					--m_InUseCount;
					m_InQueue.decrementAndGet();
					// 通知poll
					this.notify();
					return;
				}
				f = n;
			}
		}
		_Logger.warn("offer mismatch:" + resource);
	}

	/**
	 * 放入正在使用表
	 *
	 * @param element
	 *            准备使用的资源项
	 */
	synchronized private void use(Element<E> element) {
		element.inUse();
		element.next = m_InUseChain;
		m_InUseChain = element;
	}

	/**
	 * 由正使用表释放
	 * 
	 * @param element
	 *            不占用的资源项
	 */
	synchronized private void free(Element<E> element) {
		if (null == m_InUseChain) {
			return;
		}
		if (element == m_InUseChain) {
			// 首项就是
			m_InUseChain = element.next;
			return;
		}
		for (Element<E> n = m_InUseChain; null != n; n = n.next) {
			if (element == n.next) {
				// 找到了
				n.next = element.next;
				break;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		return toString(sb).toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("{n:").append(getName()).append(",c:").append(getInUseCount()).append(",max:")
				.append(getMaxSize()).append(",in-q:").append(m_InQueue.get()).append(",len-q:")
				.append(m_QueueLengthMax).append(",t-o:").append(m_QueueTimeout);
		if (m_CreateTimes > 0) {
			sb.append(",c-t:").append(getCreateTimes());
		}
		// if (null != m_Quotas) {
		// sb.append(",quotas:").append(m_Quotas);
		// }
		sb.append("}");
		return sb;
	}

	public String dump() {
		StringBuilder sb = new StringBuilder(512);
		toString(sb);
		sb.append("{");
		Element<E> n;
		n = m_ResourceChain;
		if (null != n) {
			sb.append("res:[\n");
			for (; null != n; n = n.next) {
				if (n != m_ResourceChain) {
					sb.append(",\n");
				}
				n.toString(sb);
			}
			sb.append("\n]");
		}
		n = m_InUseChain;
		if (null != n) {
			sb.append(",use:[\n");
			for (; null != n; n = n.next) {
				if (n != m_InUseChain) {
					sb.append(",\n");
				}
				n.toString(sb);
			}
			sb.append("\n]");
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 队列项
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 *            池中的项
	 */
	public static class Element<E> {
		/** 下一项 */
		Element<E> next;
		/** 放入池中的资源 */
		volatile E resource;
		/** 开始时间点 */
		volatile long startTime;
		/** 使用次数 */
		volatile int times;
		/** 使用时长（总） */
		volatile long uptime;
		/** 检查状态 */
		volatile int state;
		/** 使用的线程 */
		volatile Thread thread;

		Element() {
		}

		Element(E resource) {
			this.resource = resource;
		}

		/** 使用中 */
		void inUse() {
			this.state = STATE_INUSE;
			this.startTime = System.currentTimeMillis();
			this.thread = Thread.currentThread();
			++this.times;
		}

		/** 空闲 */
		void idle() {
			this.state = STATE_IDLE;
			this.uptime += (System.currentTimeMillis() - this.startTime);
			this.thread = null;
			this.startTime = System.currentTimeMillis();
		}

		/**
		 * 清除项（通常用于空闲时释放资源）
		 */
		public void clear() {
			this.state = STATE_EMPTY;
			this.resource = null;
			this.thread = null;
			StringBuilder sb = new StringBuilder(128);
			sb.append("clear ");
			toString(sb);
			_Logger.info(sb.toString());
		}

		public boolean isIdle() {
			return (STATE_IDLE == this.state);
		}

		public boolean isEmpty() {
			return (STATE_EMPTY == this.state);
		}

		/**
		 * 使用次数
		 */
		public int getTimes() {
			return times;
		}

		/**
		 * 当次使用的时间
		 */
		public int getUseup() {
			if (0 == startTime) {
				return 0;
			}
			return (int) (System.currentTimeMillis() - startTime);
		}

		/**
		 * 使用的线程
		 */
		public Thread getThread() {
			return thread;
		}

		/**
		 * 池中的资源项
		 */
		public E getResource() {
			return resource;
		}

		/**
		 * 状态 STATE_xxx
		 */
		public int getState() {
			return state;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(128);
			return toString(sb).toString();
		}

		public StringBuilder toString(StringBuilder sb) {
			sb.append("{s:");
			if (STATE_INUSE == state) {
				sb.append("inuse");
			} else if (STATE_IDLE == state) {
				sb.append("idle");
			} else if (STATE_EMPTY == state) {
				sb.append("empty");
			} else if (STATE_DELAY == state) {
				sb.append("delay");
			} else if (STATE_LONGTIME == state) {
				sb.append("longtime");
			} else if (STATE_OLD == state) {
				sb.append("old");
			} else {
				sb.append(state);
			}
			if (times > 0) {
				sb.append(",times:").append(times);
			}
			if (this.uptime > 10000) {
				sb.append(",uptime(s):").append(this.uptime / 1000);
			} else if (this.uptime > 0) {
				sb.append(",uptime:").append(this.uptime);
			}
			if (STATE_INUSE == state) {
				int useup = getUseup();
				if (useup > 10000) {
					sb.append(",useup(s):").append(useup / 1000);
				} else {
					sb.append(",useup:").append(useup);
				}
			}
			if (null != resource) {
				sb.append(",res:").append(resource);
			}
			if (null != thread) {
				sb.append(",thread:").append(thread);
			}
			sb.append("}");
			return sb;
		}
	}

	/**
	 * 检查占用时间过长项
	 *
	 * @author liangyi
	 *
	 */
	class LongtimeChecker extends TimerTask {
		/** 使用超时值（秒） */
		int m_Longtime;

		LongtimeChecker(int seconds) {
			m_Longtime = seconds;
			// 注册定时器（每1/5 seconds检查一下队列）
			long t = (1000L * seconds) / 5;
			if (t < 1000) {
				// 控制在1秒/次
				t = 1000;
			}
			_Timer.schedule(this, t, t);
		}

		public int getLongtime() {
			return m_Longtime;
		}

		@Override
		public void run() {
			Element<E> f = m_InUseChain;
			if (null == f) {
				return;
			}
			int timeout = m_Longtime * 1000;
			// int delayTime = timeout / 2;
			// FIXME 优化把超时项复制到临时列表，在同步块外部执行onLongtime
			synchronized (LinkedPool.this) {
				long now = System.currentTimeMillis();
				for (Element<E> n = f; null != n; f = n, n = n.next) {
					if (n.startTime <= 0) {
						continue;
					}
					int interval = (int) (now - n.startTime);
					// if (delayTime > interval) {
					// continue;
					// }
					try {
						if (interval > timeout && n.state < STATE_LONGTIME) {
							// 把状态标示为STATE_TIMEOUT
							n.state = STATE_LONGTIME;
							onLongtime(n, interval);
							// } else if (n.state < STATE_DELAY) {
							// // 把状态标示为STATE_DELAY
							// n.state = STATE_DELAY;
							// onDelay(n, interval);
						}
					} catch (Throwable e) {
						_Logger.error(e.getMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * 检查池中使用项是否空闲
	 *
	 * @author liangyi
	 *
	 */
	class IdleChecker extends TimerTask {
		/** 空闲时间（秒） */
		int m_Idle;

		IdleChecker(int seconds) {
			m_Idle = seconds;
			// 注册定时器（每1/5 seconds检查一下队列）
			long t = (1000L * seconds) / 5;
			if (t < 1000) {
				// 控制在1秒/次
				t = 1000;
			}
			_Timer.schedule(this, t, t);
		}

		@Override
		public void run() {
			Element<E> f = m_ResourceChain;
			if (null == f) {
				return;
			}
			int idel = m_Idle * 1000;
			// FIXME 优化先把空闲项移到一个临时链表，在同步块外部执行onIdle
			synchronized (LinkedPool.this) {
				long now = System.currentTimeMillis();
				for (Element<E> n = f; null != n; f = n, n = n.next) {
					if (n.isEmpty()) {
						// 由池移除
						if (n == m_ResourceChain) {
							// 首项
							m_ResourceChain = n.next;
						} else {
							f.next = n.next;
						}
					} else if (n.startTime > 0 && n.isIdle()) {
						int interval = (int) (now - n.startTime);
						if (interval >= idel) {
							try {
								onIdle((Element<E>) n, interval);
							} catch (Throwable e) {
								_Logger.error(e.getMessage(), e);
							}
						}
					}
				}
			}
		}
	}
}
