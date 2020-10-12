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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.Destroyable;
import cn.weforward.common.DestroyableExt;
import cn.weforward.common.crypto.Hex;

/**
 * 用于（运行结束）退出时执行清理工作
 * 
 * @author liangyi
 * 
 */
public class Shutdown {
	final static Logger _Logger = LoggerFactory.getLogger(Shutdown.class);

	/**
	 * 一个全局 ShutdownHook 在JVM运行结束时执行清理工作
	 */
	static final Shutdown _ShutdownHook;
	static {
		_ShutdownHook = new Shutdown(true);
		_Logger.error("{shutdown-hook-init:" + Hex.toHex(_ShutdownHook.hashCode()) + ",thread:{"
				+ Thread.currentThread() + "}}");
	}

	/**
	 * 注册Destroyable
	 * 
	 * @param destroyable
	 *            实现可销毁接口的对象
	 * @return 若已有此项返回false，否则true
	 */
	public static boolean register(Destroyable destroyable) {
		return _ShutdownHook.add(destroyable);
	}

	/**
	 * 删除Destroyable注册
	 * 
	 * @param destroyable
	 *            实现可销毁接口的对象
	 * @return 若已有此项且删除返回true，否则false
	 */
	public static boolean unregister(Destroyable destroyable) {
		return _ShutdownHook.add(destroyable);
	}

	/**
	 * 模拟系统调用shutdown
	 */
	public static void shutdown() {
		_ShutdownHook.cleanup();
	}

	/**
	 * 顶层的线程组（估计是Main线程的组）
	 */
	public static ThreadGroup getRootThreadGroup() {
		ThreadGroup rg = Thread.currentThread().getThreadGroup();
		while (rg.getParent() != null) {
			rg = rg.getParent();
		}
		return rg;
	}

	/** 要在shutdown时执行destroy的对象 */
	private final List<WeakReference<Destroyable>> m_Destroyables;

	public Shutdown() {
		m_Destroyables = new ArrayList<WeakReference<Destroyable>>();
	}

	private Shutdown(boolean onJVM) {
		this();
		Thread hook = new Thread(getRootThreadGroup(), "shutdown-hook") {
			public void run() {
				_Logger.error("{shutdown-begin:" + Hex.toHex(Shutdown.this.hashCode()) + "}");
				cleanup();
				// 清除列表
				m_Destroyables.clear();
				_Logger.info("{shutdown-end:" + Hex.toHex(Shutdown.this.hashCode()) + "}");
			}
		};
		hook.setDaemon(false);
		// hook.setPriority(Thread.MAX_PRIORITY);
		// 注册在VM终止时执行清理
		Runtime.getRuntime().addShutdownHook(hook);
	}

	/**
	 * 增加
	 * 
	 * @param destroyable
	 *            实现可销毁接口的对象
	 * @return 若已有此项返回false，否则true
	 */
	public boolean add(Destroyable destroyable) {
		if (null == destroyable) {
			throw new IllegalArgumentException("destroyable is null!");
		}
		synchronized (this) {
			for (int i = m_Destroyables.size() - 1; i >= 0; i--) {
				Destroyable da = m_Destroyables.get(i).get();
				if (da == destroyable) {
					return false; // 已增加有此项
				}
			}

			m_Destroyables.add(new WeakReference<Destroyable>(destroyable));
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("{add:" + Hex.toHex(hashCode()) + ",size:" + m_Destroyables.size()
						+ ",destroyable:" + destroyable.hashCode() + "}");
			}
			return true;
		}
	}

	/**
	 * 删除
	 * 
	 * @param destroyable
	 *            实现可销毁接口的对象
	 * @return 若已有此项且删除返回true，否则false
	 */
	public boolean remove(Destroyable destroyable) {
		if (null == destroyable) {
			return false;
		}
		synchronized (this) {
			for (int i = m_Destroyables.size() - 1; i >= 0; i--) {
				Destroyable da = m_Destroyables.get(i).get();
				if (da == destroyable) {
					// 找到了
					m_Destroyables.remove(i);
					if (_Logger.isTraceEnabled()) {
						_Logger.trace("{remove:" + Hex.toHex(hashCode()) + ",size:"
								+ m_Destroyables.size() + ",destroyable:" + destroyable.hashCode()
								+ "}");
					}
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * 执行清理
	 */
	private void cleanup() {
		synchronized (m_Destroyables) {
			try {
				_Logger.info("{signal:" + Hex.toHex(hashCode()) + ",size:" + m_Destroyables.size()
						+ "}");
			} catch (Throwable e) {
			}

			// 用于存放执行destroySignal的项，并在最后执行destroy
			ArrayList<DestroyableExt> signals = new ArrayList<DestroyableExt>(
					m_Destroyables.size());
			ArrayList<Destroyable> destroy = new ArrayList<Destroyable>(m_Destroyables.size());

			// 第一遍执行DestroyableExt的destroySignal
			for (int i = m_Destroyables.size() - 1; i >= 0; i--) {
				WeakReference<Destroyable> wr = m_Destroyables.get(i);
				Destroyable o = wr.get();
				if (o == null) {
					if (_Logger.isTraceEnabled()) {
						_Logger.trace("{null-element:" + i + "}");
					}
					continue;
				}
				try {
					if (o instanceof DestroyableExt) {
						// 若是DestroyableExt，先调用destroySignal
						DestroyableExt de = ((DestroyableExt) o);
						de.destroySignal();
						signals.add(de);
					} else {
						destroy.add(o);
					}
				} catch (Throwable e) {
					if (!(e instanceof OutOfMemoryError)) {
						_Logger.error("{signal:" + Hex.toHex(hashCode()) + ",fail:" + o + "}", e);
					}
				}
			}

			// 第二遍执行非DestroyableExt的destroy
			if (destroy.size() > 0) {
				try {
					_Logger.info("{destroy:" + Hex.toHex(hashCode()) + ",count:" + destroy.size()
							+ ",size:" + m_Destroyables.size() + "}");
					// _Logger.info("#" + Thread.currentThread().hashCode() +
					// " destroy general... "
					// + m_Destroyables.size());
				} catch (Throwable e) {
				}
			}
			for (int i = 0; i < destroy.size(); i++) {
				Destroyable o = destroy.get(i);
				if (o == null) {
					continue;
				}
				try {
					if (_Logger.isTraceEnabled()) {
						_Logger.trace("{destroy:" + o + "}");
					}
					o.destroy();
				} catch (Throwable e) {
					if (!(e instanceof OutOfMemoryError)) {
						_Logger.error("{destroy:" + Hex.toHex(hashCode()) + ",fail:" + o + "}", e);
					}
				}
			}
			destroy = null;
			// // 清除列表
			// m_Destroyables.clear();

			if (signals.size() == 0) {
				// 没有DestroyableExt项
				return;
			}

			ArrayList<DestroyableExt> remain = new ArrayList<DestroyableExt>(signals.size());
			ArrayList<DestroyableExt> loop = signals;
			int sleeping = 2 * 1000; // 每轮sleep的时间（毫秒）
			// 周期执行signals/loop的destroySignal，直至清理完毕或超过（大概）4分钟
			long stopTime = System.currentTimeMillis() + (4 * 60 * 1000L);
			for (int t = 0; System.currentTimeMillis() < stopTime && t < 10000; t++) {
				try {
					_Logger.info("{signal:" + Hex.toHex(hashCode()) + ",loop:" + t + ",size:"
							+ loop.size() + ",remain:" + remain.size() + "}");
				} catch (Throwable e) {
				}
				remain.clear();
				for (DestroyableExt de : loop) {
					if (de == null) {
						continue;
					}
					try {
						if (_Logger.isTraceEnabled()) {
							_Logger.trace("{destroy-signal:" + de + "}");
						}
						if (de.destroySignal()) {
							// 还在清理
							remain.add(de);
						}
					} catch (Throwable e) {
						if (!(e instanceof OutOfMemoryError)) {
							// _Logger.error("destroySignal loop failed:" + de,
							// e);
							_Logger.error("{signal:" + Hex.toHex(hashCode()) + ",fail:" + de
									+ ",size:" + loop.size() + "}", e);
						}
					}
				}
				if (remain.isEmpty()) {
					// 没有需清理的项，结束loop destroySignal
					break;
				}
				// // 交换signals与remain
				// loop = remain;
				// signals.clear();
				// remain = signals;
				// signals = loop;
				// 等2~10秒后再下一轮
				try {
					// 先清除中断标记，否则sleep会一直被中断
					Thread.interrupted();
					Thread.sleep(sleeping);
					if (sleeping >= 10 * 1000) {
						sleeping = 10 * 1000;
					} else {
						sleeping *= 2;
					}
				} catch (InterruptedException e) {
					// 恢复中断标记，让其能影响de.destroySignal()里的流程（一般来说不需要，但保持其能用于特殊情况下使用）
					Thread.currentThread().interrupt();
					_Logger.warn("{interrupt:" + Hex.toHex(hashCode()) + "}");
				}
			}

			// 看看是否还有destroySignal没能结束的项
			try {
				if (remain.isEmpty()) {
					_Logger.info("{final:" + Hex.toHex(hashCode()) + "}");
				} else {
					StringBuilder sb = new StringBuilder(256);
					sb.append("{force:").append(Hex.toHex(hashCode())).append(",size:")
							.append(signals.size());
					sb.append(",remain-").append(remain.size()).append(":[\n");
					for (int i = 0; i < remain.size(); i++) {
						if (i > 0) {
							sb.append(",\n");
						}
						sb.append(remain.get(i));
					}
					sb.append("]}");
					_Logger.error(sb.toString());
				}
			} catch (Throwable e) {
			}
			remain = null;
			signals = null;
			loop = null;

			// 最后再全部执行一次destroy
			for (int i = m_Destroyables.size() - 1; i >= 0; i--) {
				WeakReference<Destroyable> wr = m_Destroyables.get(i);
				Destroyable o = wr.get();
				if (o == null) {
					continue;
				}
				try {
					if (_Logger.isTraceEnabled()) {
						_Logger.trace("destroy... " + o);
					}
					o.destroy();
				} catch (Throwable e) {
					if (!(e instanceof OutOfMemoryError)) {
						_Logger.error(
								"{destroy-final:" + Hex.toHex(hashCode()) + ",fail:{" + o + "}}",
								e);
					}
				}
			}
		}
	}

}
