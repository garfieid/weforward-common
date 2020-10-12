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
package cn.weforward.common.sys;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.DestroyableExt;
import cn.weforward.common.GcCleanable;
import cn.weforward.common.NameItem;
import cn.weforward.common.crypto.Hex;
import cn.weforward.common.execption.AbortException;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.SinglyLinked;
import cn.weforward.common.util.SinglyLinkedLifo;

/**
 * 使用SoftReference在GC时清除缓存
 * 
 * @author liangyi
 * 
 */
public class GcCleaner implements DestroyableExt {
	/** 内存严重不足到快挂掉了 */
	static final int POLICY_SUSPEND = Memory.MEMORY_SUSPEND.id;

	/** 日志记录器 */
	final static Logger _Logger = LoggerFactory.getLogger(GcCleaner.class);
	/** 默认全局Cleaner */
	public static final GcCleaner _Cleaner = new GcCleaner();

	/** 是否在内存紧张时指示挂起耗内存操作 */
	static protected int _critical_suspend = NumberUtil
			.toInt(System.getProperty("cn.weforward.common.sys.GcCleaner.critical_suspend"), 1);

	/**
	 * 注册到全局的Gc控制器
	 * 
	 * @param cleanable
	 *            要注册的清理项
	 * @return 加入列表则返回true，返回false表示其已在列表
	 */
	public static boolean register(GcCleanable cleanable) {
		return _Cleaner.add(cleanable);
	}

	/**
	 * 移除在全局的Gc控制器的项
	 * 
	 * @param cleanable
	 *            要取消的清理项
	 * @return 有此项则清除且返回true，否则false
	 */
	public static boolean unregister(GcCleanable cleanable) {
		return _Cleaner.remove(cleanable);
	}

	/**
	 * 尝试等待GC完成
	 * 
	 * @param timeout
	 *            等待超时值（毫秒）
	 */
	public static void waitFor(long timeout) {
		Thread thread = _Cleaner.m_Thread;
		try {
			if (null == thread) {
				// throw new IllegalStateException("GcCleaner is down.");
				synchronized (_Cleaner) {
					_Cleaner.wait(timeout);
				}
			} else {
				synchronized (thread) {
					thread.wait(timeout);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * GC cleanup后是否还是内存严重不足，要挂起或中止消耗内存的数据加载操作
	 * 
	 * @param gc
	 *            严重不足时是否触发GC
	 */
	public static boolean isCriticalSuspend(boolean gc) {
		NameItem state = getMemory().getLevel();
		if (1 == _critical_suspend && POLICY_SUSPEND == state.id) {
			if (gc && gc()) {
				// GC后重新计算下内存状态
				_Cleaner.calcPolicy();
			}
			state = getMemory().getLevel();
			return (POLICY_SUSPEND == state.id);
		}
		return false;
	}

	/**
	 * INFO日志输出Cleanable注册的项
	 */
	public static void dumpInfo() {
		_Cleaner.dump();
	}

	/**
	 * 调用System.gc，但加上时间点检查
	 * 
	 * @return 若距上次调用的时间太短（30秒内）不处理直接返回false，否则调用System.gc后返回true
	 */
	public static boolean gc() {
		return gc(false);
	}

	/**
	 * 调用System.gc
	 * 
	 * @param force
	 *            若为true直接执行，否则按时间点检查是否调用时间过短
	 * 
	 * @return 若距上次调用的时间太短（30秒内）不处理直接返回false，否则调用System.gc后返回true
	 */
	public static boolean gc(boolean force) {
		long now = System.currentTimeMillis();
		long interval = now - _Cleaner.m_LastSystemGc;
		if (!force && interval < _Cleaner.m_Interval) {
			// 时间太短了（小于m_Interval）
			return false;
		}
		_Cleaner.m_LastSystemGc = now;
		// XXX G1 GC下，System.gc的调用时间非常长（在20G最大内存，使用10G的情况下长达17秒）
		System.gc();
		_Logger.warn("{gc:\"System.gc()\",memory:" + getMemory() + ",interval:" + interval + "}");
		// // 重新计算下内存状态
		// _Cleaner.calcPolicy(Runtime.getRuntime().freeMemory(),
		// totalMemory());
		return true;
	}

	static public Memory getMemory() {
		// _Cleaner.m_Memory.refresh();
		// 除了刷新内存使用量，还要计算POLICY_xxx
		_Cleaner.calcPolicy();
		return VmStat.getMemory();
	}

	/**
	 * 用于很简单地计算缓存的内存量
	 * 
	 * @param rate
	 *            总可用堆内存的百分比（1~80）
	 * @param max
	 *            上限（字节数）
	 * @return 可用于缓存的内存量（字节数）
	 */
	public static long calcMemForCache(int rate, long max) {
		getMemory().refresh();
		long total = getMemory().max;
		long need = (total * rate) / 100;
		if (need > max) {
			return max;
		}
		return need;
	}

	/** 监听m_gcSignal回收事件 */
	ReferenceQueue<Object> m_ReferenceQueue = new ReferenceQueue<Object>();

	/** 执行cleanup的线程 */
	private Thread m_Thread;
	/** 清理列表项 */
	private final SinglyLinkedLifo<WeakReference<GcCleanable>> m_Cleanables = new SinglyLinkedLifo<WeakReference<GcCleanable>>();
	/** 最后调用System.gc的时间点 */
	volatile long m_LastSystemGc;
	// /** GC cleanup后状态 */
	// volatile int m_LastState;
	/** 主动触发GC的最小间隔（单位毫秒，默认为5分钟） */
	int m_Interval;

	/**
	 * 增加清理项
	 * 
	 * @param c
	 *            清理项
	 * @return 加入列表则返回true，返回false表示其已在列表
	 */
	public boolean add(GcCleanable c) {
		if (null == c) {
			return false;
		}
		synchronized (m_Cleanables) {
			if (m_Cleanables.addIfAbsent(new CleanableReference(c))) {
				_Logger.trace("add cleanable[{}] {}", m_Cleanables.size(), c);
				return true;
			}
		}
		return false;
	}

	/**
	 * 删除清理项
	 * 
	 * @param c
	 *            清理项
	 * @return 有此项则清除且返回true，否则false
	 */
	public boolean remove(GcCleanable c) {
		if (null == c) {
			return false;
		}
		synchronized (m_Cleanables) {
			return m_Cleanables.remove(new CleanableReference(c));
		}
	}

	/**
	 * 扩展WeakReference包装GcCleanable
	 * 
	 * @author liangyi
	 *
	 */
	static class CleanableReference extends WeakReference<GcCleanable> {

		public CleanableReference(GcCleanable referent) {
			super(referent);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			GcCleanable v = get();
			if (null == v) {
				return false;
			}
			if (obj instanceof WeakReference<?>) {
				return v.equals(((WeakReference<?>) obj).get());
			}
			return v.equals(obj);
		}
	}

	/**
	 * 主动触发GC的最小间隔
	 * 
	 * @param ms
	 *            间隔（毫秒）
	 */
	public void setInterval(int ms) {
		m_Interval = ms;
	}

	/**
	 * 日志输出具体的Cleanable
	 */
	public void dump() {
		if (!_Logger.isInfoEnabled()) {
			return;
		}
		// synchronized (m_Cleanables) {
		getMemory().refresh();
		_Logger.info("****dump** [" + m_Cleanables.size() + "]" + getMemory());
		int i = 0;
		for (SinglyLinked.SinglyLinkedNode<WeakReference<GcCleanable>> p = m_Cleanables
				.getHead(); null != p; p = p.getNext(), i++) {
			// if(null==cc) continue;
			GcCleanable c = p.value.get();
			if (null == c) {
				_Logger.info("<null> element at " + i);
				continue;
			}
			_Logger.info("[" + c.getClass() + "]" + c.toString());
		}
		// }
		_Logger.info("************************");
	}

	public Iterator<WeakReference<GcCleanable>> getCleanables() {
		return m_Cleanables.iterator();
	}

	protected GcCleaner() {
		m_Interval = NumberUtil.toInt(
				System.getProperty("cn.weforward.common.util.GcCleaner.interval", null),
				5 * 60 * 1000);
		init();
		Shutdown.register(this);
	}

	protected void init() {
		m_Thread = new Thread(Shutdown.getRootThreadGroup(), "GcSignal.finalize") {
			@Override
			public void run() {
				/** 用于GC时得到通知（软引用，内存不足时释放） */
				SoftReference<Object> gcSignal = null;
				int policy;
				int gcSignalTick = GcSignal.getTicker(); // gcSignal创建的时间点
				// 空闲时的检查周期
				int period = Memory.getRefreshPeriod();
				Reference<?> ret;
				for (;;) {
					Thread thread = m_Thread;
					if (null == thread) {
						break;
					}
					try {
						ret = null;
						if (null == gcSignal) {
							gcSignal = new SoftReference<Object>(new GcSignal(), m_ReferenceQueue);
							gcSignalTick = GcSignal.getTicker();
							if (_Logger.isInfoEnabled()) {
								_Logger.info("{GcSignal-reinit:" + Hex.toHex(gcSignal.hashCode())
										+ ",cleanables:" + m_Cleanables.size() + ",memory:"
										+ getMemory() + "}");
							}
							// 创建后先等30秒
							ret = m_ReferenceQueue.remove(30 * 1000);
							if (null == m_Thread) {
								// 即时结束
								break;
							}
							// // 计算POLICY_xxx
							// policy = calcPolicy();
							// } else if (_Logger.isInfoEnabled()) {
							// _Logger.info("{GcSignal-loop:" +
							// Hex.toHex(gcSignal.hashCode())
							// + ",cleanables:" + m_Cleanables.size() +
							// ",memory:"
							// + getMemory() + "}");
						}
						if (null == ret) {
							ret = m_ReferenceQueue.remove(period);
						}
						if (null == m_Thread) {
							// 即时结束
							break;
						}

						if (null != ret) {
							if (ret == gcSignal) {
								// Full GC（或内存不足）后才会有软信号
								gcSignal = null;
								// gcSignal才创建不到60秒就要释放，内存太紧张了，可能需要主动触发FullGC
								boolean fullGc = (GcSignal.getTicker() < gcSignalTick + 60);
								if (_Logger.isInfoEnabled()) {
									_Logger.info("GcSignal-signaled:" + Hex.toHex(ret.hashCode())
											+ ",tick:" + (GcSignal.getTicker() - gcSignalTick)
											+ "}");
								}

								// 计算POLICY_xxx
								policy = calcPolicy();
								if (fullGc && policy != GcCleanable.POLICY_IDLE) {
									// 需要full GC，按内存紧张释放
									policy = GcCleanable.POLICY_CRITICAL;
									log(policy);
									cleanup(policy);
									gc();
								} else {
									// 执行cleanup（暂时不清除链表中的空项）
									log(policy);
									cleanup(policy);
								}
							} else {
								// 不是当前的gcSignal？
								_Logger.error("{GcSignal-unexpect:" + Hex.toHex(ret.hashCode())
										+ ",ret:" + gcSignal + "}");
								cleanup();
							}
						} else {
							if (_Logger.isInfoEnabled()) {
								_Logger.info("{GcSignal-idel:" + gcSignal + ",cleanables:"
										+ m_Cleanables.size() + "}");
							}
							// 空闲周期清理
							cleanup();
						}
						// getMemory().refresh();
						policy = calcPolicy();
					} catch (InterruptedException e) {
						_Logger.info("{GcSignal-interrupt:" + gcSignal + ",cleanables:"
								+ m_Cleanables.size() + ",memory:" + getMemory() + "}");
						break;
					} catch (Throwable e) {
						// 若是内存不足够，别指望能输出日志添乱啊
						if (!(e instanceof OutOfMemoryError)) {
							_Logger.error(e.getMessage(), e);
						}
					}
				}
				m_Thread = null;
				if (_Logger.isInfoEnabled()) {
					_Logger.info("{GcSignal-destroy:" + gcSignal + ",cleanables:"
							+ m_Cleanables.size() + ",memory:" + getMemory() + "}");
				}
			}
		};
		m_Thread.setDaemon(true);
		// m_Thread.setPriority(Thread.NORM_PRIORITY);
		m_Thread.start();
	}

	/**
	 * 当前清理处理策略（POLICY_xxx）
	 * 
	 * @return POLICY_xxx
	 */
	static public int getPolicy() {
		return _Cleaner.calcPolicy();
	}

	/**
	 * 统计内存使用状态，计算相关的处理策略（POLICY_xxx）
	 * 
	 * @return POLICY_xxx
	 */
	private int calcPolicy() {
		int policy = VmStat.getMemory().refresh();
		if (POLICY_SUSPEND == policy || Memory.MEMORY_CRITICAL.id == policy) {
			// 把POLICY_SUSPEND转为POLICY_CRITICAL（GcCleanable不认识POLICY_SUSPEND）
			policy = GcCleanable.POLICY_CRITICAL;
		} else if (Memory.MEMORY_LOW.id == policy) {
			policy = GcCleanable.POLICY_LOW;
		} else {
			// 默认为IDEL
			policy = GcCleanable.POLICY_IDLE;
		}
		return policy;
	}

	private void log(int policy) {
		if (policy < GcCleanable.POLICY_LOW) {
			if (_Logger.isInfoEnabled()) {
				_Logger.info("{GcSignal-cleanup:" + m_Cleanables.size() + ",state:IDLE" + ",memory:"
						+ getMemory() + "}");
			}
		} else if (_Logger.isWarnEnabled()) {
			_Logger.warn("{GcSignal-cleanup:" + m_Cleanables.size() + ",state:"
					+ ((GcCleanable.POLICY_CRITICAL == policy) ? "CRITICAL"
							: ((GcCleanable.POLICY_LOW == policy) ? "LOW" : "IDLE"))
					+ ",memory:" + getMemory() + "}");
		}
	}

	public void cleanup() {
		// 计算POLICY_xxx
		int policy = calcPolicy();
		log(policy);

		int nilCount = cleanup(policy);
		if (nilCount > 0) {
			// 在链表中清理空项
			int old = m_Cleanables.size();
			synchronized (m_Cleanables) {
				// 清除空的项
				for (SinglyLinked.SinglyLinkedNode<WeakReference<GcCleanable>> p = m_Cleanables
						.detach(); null != p; p = p.getNext()) {
					GcCleanable c = p.value.get();
					if (null == c) {
						continue;
					}
					m_Cleanables.addHead(p.value);
				}
			}
			_Logger.info("Clear <null> elements:" + old + "/" + m_Cleanables.size());
		}
		// calcPolicy();
	}

	/**
	 * 对注册的GcCleanable表执行onGcCleanup
	 * 
	 * @param policy
	 *            传给onGcCleanup的POLICY_xxx
	 * @return 执行后发现的空项数
	 */
	public int cleanup(int policy) {
		int nilCount = 0;
		for (SinglyLinked.SinglyLinkedNode<WeakReference<GcCleanable>> p = m_Cleanables
				.getHead(); null != p; p = p.getNext()) {
			GcCleanable c = p.value.get();
			if (null == c) {
				++nilCount;
				continue;
			}
			try {
				c.onGcCleanup(policy);
				// } catch (AbortException e) {
				// _Logger.error(c.toString() + " onGcCleanup abort!", e);
				// break;
			} catch (AbortException e) {
				// 执行被中止
				_Logger.error(c.toString() + " onGcCleanup abort!", e);
				break;
			} catch (Throwable e) {
				if (Thread.currentThread().isInterrupted()) {
					// 线程被中断
					_Logger.error(c.toString() + " onGcCleanup interrupted!", e);
					break;
				} else if (!(e instanceof OutOfMemoryError)) {
					_Logger.error(c.toString() + " onGcCleanup failed.", e);
				}
			}
		}
		return nilCount;
	}

	@Override
	public boolean destroySignal() {
		return false;
	}

	@Override
	public void destroy() {
		Thread thread = m_Thread;
		if (null != thread) {
			synchronized (thread) {
				if (GcCleaner.this == _Cleaner) {
					// 刷新最后调用gc的时间点（finalize当然是gc所触发的啦）
					_Cleaner.m_LastSystemGc = System.currentTimeMillis();
				}
				m_Thread = null;
				// thread.notifyAll();
				thread.interrupt();
			}
		}
	}

	/**
	 * 定义一个只实现finalize的GC信号对象，以在内存不足时cleanup缓存
	 * 
	 * @author liangyi
	 * 
	 */
	static protected class GcSignal {
		@Override
		public String toString() {
			return Integer.toHexString(hashCode());
		}

		static public int getTicker() {
			return Timepoint.Tick.getInstance(1).getTicker();
		}
	}

	@Override
	public String toString() {
		getMemory().refresh();
		return "{mem:" + getMemory() + ",lastGc:"
				+ ((System.currentTimeMillis() - m_LastSystemGc) / 1000) + ",interval:" + m_Interval
				+ ",cleanables:" + m_Cleanables.size() + ",thread:" + m_Thread + "}";
	}
}
