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

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;

import cn.weforward.common.execption.Unexpected;
import cn.weforward.common.util.StringBuilderPool;

/**
 * 用来代替System.currentTimeMillis()获取秒单位以上的时间点（基于性能及时间点总是增长的需求）的生成器<br/>
 * 注：若是按每秒单位生成的时间点在2068年后溢出
 * 
 * @author liangyi
 * 
 */
public class Timepoint {
	/** 日志记录器 */
	final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(Timepoint.class);

	/** 2000-1-1的GMT毫秒时间点（用于作时间点的起点值） */
	public static final long BASE_TIME = 946721219000L;

	/** 校正基准时间的节奏（N个时间点单位后） */
	static final int DATUM_OFFSET = 10;

	/**
	 * 取得共享的时间点发生器
	 * 
	 * @param unit
	 *            时间点精度/单位（秒）
	 * @return 时间点发生器
	 */
	public static Timepoint getInstance(int unit) {
		if (1 == unit) {
			return Instances._C01;
		}
		if (2 == unit) {
			return Instances._C02;
		}
		if (10 == unit) {
			return Instances._C10;
		}
		return new Timepoint(unit);
	}

	/** 计时器 */
	final Tick m_ClockTick;

	/**
	 * 构建计时间点发生器
	 * 
	 * @param unit
	 *            时间点单位（秒）
	 */
	public Timepoint(int unit) {
		m_ClockTick = new Tick(unit);
		m_ClockTick.datum();
	}

	/**
	 * 时间点
	 */
	public int getTimepoint() {
		return m_ClockTick.getTicker();
	}

	/**
	 * 时间点单位（秒）
	 */
	public int getUnit() {
		return m_ClockTick.getPeriod();
	}

	/**
	 * 未对时的步数
	 */
	public int getSteps() {
		return m_ClockTick.m_Steps;
	}

	@Override
	public String toString() {
		// return String.valueOf(getUnit());
		int t = getTimepoint();
		StringBuilder sb = StringBuilderPool._128.poll();
		try {
			sb.append(t).append("(");
			formatTimestamp(getTime(t), sb);
			sb.append(")/").append(getUnit()).append("秒");
			return (sb.toString());
		} finally {
			StringBuilderPool._128.offer(sb);
		}
	}

	/**
	 * 由GMT毫秒时间计算时间点
	 * 
	 * @param time
	 *            相对对GMT1970基准点的毫秒时间
	 * @return 相应的时间点值（单位秒）
	 */
	public int getTimepoint(long time) {
		int t = (int) ((time - BASE_TIME) / (getUnit() * 1000));
		return t;
	}

	/**
	 * 取得自1970年1月1日的GMT毫秒时间(Date.getTime())
	 * 
	 * @param timepoint
	 *            时间点
	 * @return 相对对GMT1970基准点的毫秒时间
	 */
	public long getTime(int timepoint) {
		long t = (1000L * getUnit() * timepoint) + BASE_TIME;
		return t;
	}

	/**
	 * 继承计时器产生时间点
	 * 
	 * @author liangyi
	 * 
	 */
	class Tick extends ClockTick {
		/** 距离最后一次对时的步数 */
		int m_Steps;

		public Tick(int period) {
			super(period, 0);
		}

		@Override
		public void run() {
			++m_Steps;
			if (m_Steps >= DATUM_OFFSET) {
				m_Steps = 0;
				datum();
				return;
			}
			++m_Ticker;
		}

		/**
		 * 使用System.currentTimeMillis()校正基准时间
		 * 
		 * @return 新的基准时间
		 */
		protected int datum() {
			long now = System.currentTimeMillis();
			int t = (int) ((now - BASE_TIME) / (getUnit() * 1000));
			int k = getTicker();
			if (t < k) {
				// 要校正的时间点小于当前时间点，忽略它继续使用旧的值保证时间是增大的
				StringBuilder sb = new StringBuilder("当前系统时间早于上个时间点(" + getUnit() + "):");
				sb.append(k).append("/").append(t).append("(");
				formatTimestamp(getTime(k), sb);
				sb.append("/");
				formatTimestamp(now, sb);
				sb.append(")");
				_Logger.error(sb.toString());
				return k;
			}
			// if (t == k) {
			// // 没变化，什么也不做
			// return t;
			// }

			m_Ticker = t;
			if (_Logger.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("校正时间点(" + getUnit() + "):");
				sb.append(k).append("/").append(t).append("(");
				formatTimestamp(getTime(k), sb);
				sb.append("/");
				formatTimestamp(now, sb);
				sb.append(")");
				_Logger.debug(sb.toString());
			}
			return t;
		}
	}

	static Calendar GMT_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

	/**
	 * 格式化为紧凑的GMT时区的时间戳，如：20140311T181509000
	 * 
	 * @param timestamp
	 *            时间戳（Date.getTime()）
	 * @param appender
	 *            把格式串追加至其
	 * @return 格式化输出
	 */
	static public final Appendable formatTimestamp(long timestamp, Appendable appender) {
		synchronized (GMT_CALENDAR) {
			Calendar c = GMT_CALENDAR;
			try {
				c.setTimeInMillis(timestamp);
				append4(appender, c.get(Calendar.YEAR));
				append2(appender, 1 + c.get(Calendar.MONTH));
				append2(appender, c.get(Calendar.DAY_OF_MONTH));
				appender.append('T');
				append2(appender, c.get(Calendar.HOUR_OF_DAY));
				append2(appender, c.get(Calendar.MINUTE));
				append2(appender, c.get(Calendar.SECOND));
				append3(appender, c.get(Calendar.MILLISECOND));
			} catch (IOException e) {
				throw new Unexpected(e);
			}
		}
		return appender;
	}

	/** 十进制数字（0~9） */
	private final static char[] _TenDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	static private final void append4(Appendable appender, int val) throws IOException {
		if (val >= 10) {
			if (val >= 100) {
				if (val >= 1000) {
					appender.append(_TenDigits[(int) (val / 1000)]);
					val = val % 1000;
				} else {
					appender.append('0');
				}
				appender.append(_TenDigits[(int) (val / 100)]);
				val = val % 100;
			} else {
				appender.append('0');
				appender.append('0');
			}
			appender.append(_TenDigits[(int) (val / 10)]);
			val = val % 10;
		} else {
			appender.append('0');
			appender.append('0');
			appender.append('0');
		}
		appender.append(_TenDigits[val]);
	}

	static private final void append3(Appendable appender, int val) throws IOException {
		if (val >= 10) {
			if (val >= 100) {
				appender.append(_TenDigits[(int) (val / 100)]);
				val = val % 100;
			} else {
				appender.append('0');
			}
			appender.append(_TenDigits[(int) (val / 10)]);
			val = val % 10;
		} else {
			appender.append('0');
			appender.append('0');
		}
		appender.append(_TenDigits[val]);
	}

	static private final void append2(Appendable appender, int val) throws IOException {
		if (val >= 10) {
			appender.append(_TenDigits[(int) (val / 10)]);
			val = val % 10;
		} else {
			appender.append('0');
		}
		appender.append(_TenDigits[val]);
	}

	/**
	 * 使用内部类来解决延迟初始化单例及避免DCL（双重检查锁定的问题）
	 * 
	 * @author liangyi
	 * 
	 */
	private static class Instances {
		/** 共享、1秒的 */
		private static Timepoint _C01 = new Timepoint(1);
		/** 共享、2秒的 */
		private static Timepoint _C02 = new Timepoint(2);
		/** 共享、10秒的 */
		private static Timepoint _C10 = new Timepoint(10);
	}
}
