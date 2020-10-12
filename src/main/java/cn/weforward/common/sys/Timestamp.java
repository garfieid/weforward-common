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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import cn.weforward.common.execption.OverflowException;

/**
 * 时间戳生成器，由：0~7位的服务器标识，8~15位的补充序数及16~59位（自1970后的）毫秒数（有500多年的时间段应该足够用了）
 * 
 * @author liangyi
 * 
 */
public abstract class Timestamp {
	/**
	 * 0~7位为服务器ID（1~255），0保留，250～255为测试服务器ID
	 */
	public static int MASK_SERVER = 0xFF;
	/**
	 * 8~15位为补充序数（1~255），当时间戳生成间隔短于1毫秒时使用，即能支持每秒255000个时间戳
	 */
	public static int MASK_ORDINAL = 0xFF00;
	/** 16~59位（自1970后的）毫秒数的基准时间 */
	public static long MASK_TIMESTAMP = 0x0FFFFFFFFFFF0000L;
	/** 标记为删除（最高位） */
	public static long MARK_REMOVED = 0x8000000000000000L;
	/** 标记位（60~63） */
	public static long MASK_MARK = 0xF000000000000000L;
	/** 基准时间及补充序数位 */
	public static long MASK_TIME_AND_ORDINAL = 0x0FFFFFFFFFFFFF00L;

	/** 生成器技术策略-默认 */
	public static int POLICY_DEFAULT = 0;
	/** 生成器技术策略-不安全的，不提供线程安全保护，要使用者自己确保在线程安全 */
	public static int POLICY_UNSAFE = 1;
	/** 生成器技术策略-使用CAS(Compare And Set)来保证线程安全 */
	public static int POLICY_CAS = 2;
	/** 生成器技术策略-使用（重入）锁来保证线程安全 */
	public static int POLICY_LOCK = 3;
	/** 生成器技术策略-使用同步块来保证线程安全 */
	public static int POLICY_SYNC = 4;

	/** 计时器 */
	static final ClockTick _Tick = ClockTick.getInstance(1);

	/** 校正基准时间的节奏（N秒） */
	static final int TIME_TICKS = 5;
	/** 在每个基准时间内的自动生成的间隔（次数） */
	static final int MAX_INTERVAL = TIME_TICKS * 1000 * 255;

	/** 基准时间（秒） */
	volatile long m_Datum;
	/** 最后计时值（秒） */
	volatile int m_LastTick;

	/**
	 * 取（全局的）时间戳生成器
	 */
	public static Timestamp getInstance(int policy) {
		if (POLICY_CAS == policy) {
			return new CasTimestamp();
		}
		if (POLICY_LOCK == policy) {
			return new LockTimestamp();
		}
		if (POLICY_UNSAFE == policy) {
			return new UnsafeTimestamp();
		}
		if (POLICY_DEFAULT == policy || POLICY_SYNC == policy) {
			return new SyncTimestamp();
		}
		throw new IllegalArgumentException("参数无效：" + policy);
	}

	/**
	 * 取得（自1970后的）毫秒数的基准时间，如 new Date(Timestamp.getTimestamp(ts))
	 * 
	 * @param v
	 *            时间戳
	 * @return 毫秒为单位的时间
	 */
	final static public long getTime(long v) {
		return (MASK_TIMESTAMP & v) >> 16;
	}

	/**
	 * 取基准时间及补充序号位
	 * 
	 * @param v
	 *            时间戳
	 * @return 基准时间及补充序号所占位
	 */
	final static public long getTimeBits(long v) {
		return (MASK_TIME_AND_ORDINAL & v);
	}

	/**
	 * 取得补充序数
	 * 
	 * @param v
	 *            时间戳
	 * @return 补充序数
	 */
	final static public int getOrdinal(long v) {
		return ((int) (0xFF00 & v)) >> 8;
	}

	/**
	 * 取得服务器ID
	 * 
	 * @param v
	 *            时间戳
	 * @return 服务器ID
	 */
	final static public int getServerId(long v) {
		return ((int) (0xFF & v));
	}

	/**
	 * 检查标记位
	 * 
	 * @param v
	 *            时间戳
	 * @param mark
	 *            要检查的标记位 MARK_xxx
	 * @return 是则返回true
	 */
	final static public boolean isMark(long v, long mark) {
		return mark == (v & mark);
	}

	/**
	 * 合成时间戳
	 * 
	 * @param t
	 *            毫秒为单位的时间
	 * @param ordinal
	 *            补充序数（0～255）
	 * @param serverId
	 *            服务器ID（0～255）
	 * @return 时间戳
	 */
	final static public long getTimestamp(long t, int ordinal, int serverId) {
		t <<= 16;
		t |= (0xFF00 & (ordinal << 8));
		t |= (0xFF & serverId);
		return t;
	}

	/**
	 * 去除时间戳中的标记位
	 * 
	 * @param timestamp
	 *            时间戳
	 * @return 不包含标记位的时间戳
	 */
	final static public long unmark(long timestamp) {
		return (timestamp & (~MASK_MARK));
	}

	/**
	 * 生成时间戳
	 * 
	 * @param serverId
	 *            服务器ID（0～255）
	 * @return 时间戳
	 */
	public abstract long next(int serverId);

	/**
	 * 使用System.currentTimeMillis()校正基准时间
	 * 
	 * @return 新的基准时间
	 */
	protected long datum() {
		long t = System.currentTimeMillis() << 8;
		m_Datum = t;
		m_LastTick = _Tick.getTicker();
		return t;
	}

	/**
	 * 基于CAS(Compare And Set)的实现
	 * 
	 * @author liangyi
	 * 
	 */
	static class CasTimestamp extends Timestamp {
		/** 最后时间戳 */
		final AtomicLong m_LastStamp;

		CasTimestamp() {
			m_LastStamp = new AtomicLong(datum());
		}

		public long next(int serverId) {
			long t, v, m;
			int d;
			for (int c = 0; c < 1000; c++) {
				// for (;;) {
				v = m_LastStamp.get();
				d = _Tick.getTicker();
				if (d > m_LastTick + TIME_TICKS) {
					// 超过10秒后校正基准时间，注意：校正后有可能d小于最后值（比如调整了系统时间）
					t = datum();
					if (t > v) {
						// 只认增大的基准值作为新的时间戳，否则只好等下面的逻辑继续用加一操作
						if (m_LastStamp.compareAndSet(v, t)) {
							return (t << 8 | (0xFF & serverId));
						}
						// } else {
						// System.out.println("晕，有没有这么频密啊！" + (v - t));
					}
				}

				// 直接最后时间戳加一
				t = v + 1;
				m = (m_Datum + MAX_INTERVAL);
				if (t > m) {
					throw new OverflowException(
							"在短时（" + TIME_TICKS + "秒）内生成太多时间戳：" + (t - m_Datum));
				}
				if (m_LastStamp.compareAndSet(v, t)) {
					// break;
					return (t << 8 | (0xFF & serverId));
				}
			}
			throw new OverflowException("CAS尝试过多（1000次）");
		}

		@Override
		public String toString() {
			return "{CAS:" + m_LastStamp.get() + "}";
		}
	}

	/**
	 * 不线程安全的实现
	 * 
	 * @author liangyi
	 * 
	 */
	static class UnsafeTimestamp extends Timestamp {
		volatile long m_LastStamp;

		protected UnsafeTimestamp() {
			m_LastStamp = datum();
		}

		@Override
		public long next(int serverId) {
			return unsafeNext(serverId);
		}

		final long unsafeNext(int serverId) {
			long t, v, m;
			int d;
			v = m_LastStamp;
			d = _Tick.getTicker();
			if (d > m_LastTick + TIME_TICKS) {
				// 超过10秒后校正基准时间，注意：校正后有可能d小于最后值（比如调整了系统时间）
				t = datum();
				if (t > v) {
					// 只认增大的基准值作为新的时间戳，否则只好等下面的逻辑继续用加一操作
					m_LastStamp = t;
					return (t << 8 | (0xFF & serverId));
					// } else {
					// System.out.println("晕，有没有这么频密啊！" + (v - t));
				}
			}

			// 直接最后时间戳加一
			t = v + 1;
			m = (m_Datum + MAX_INTERVAL);
			if (t > m) {
				throw new OverflowException("在短时（" + TIME_TICKS + "秒）内生成太多时间戳：" + (t - m_Datum));
			}
			m_LastStamp = t;
			return (t << 8 | (0xFF & serverId));
		}

		@Override
		public String toString() {
			return "{Unsafe:" + m_LastStamp + "}";
		}
	}

	/**
	 * 使用同步块的的实现
	 * 
	 * @author liangyi
	 * 
	 */
	static class SyncTimestamp extends UnsafeTimestamp {
		@Override
		synchronized public long next(int serverId) {
			return unsafeNext(serverId);
		}

		@Override
		public String toString() {
			return "{synchronized:" + m_LastStamp + "}";
		}
	}

	/**
	 * 使用重入锁的的实现
	 * 
	 * @author liangyi
	 * 
	 */
	static class LockTimestamp extends UnsafeTimestamp {
		final ReentrantLock m_Lock = new ReentrantLock();

		@Override
		public long next(int serverId) {
			m_Lock.lock();
			try {
				return unsafeNext(serverId);
			} finally {
				m_Lock.unlock();
			}
		}

		@Override
		public String toString() {
			return "[重入锁]" + m_LastStamp;
		}
	}
}
