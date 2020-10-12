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

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时加一的计时发布器（通常用于作为运行时的时间戳）
 * 
 * @author liangyi
 * 
 */
public class ClockTick extends TimerTask {
	/** 用于发布时间的定时器 */
	protected final static Timer _Timer = new Timer("ClockTick-Timer", true);

	/** 计时的初始值 */
	protected final static int TICKER_INIT = 3600;

	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(ClockTick.class);

	/**
	 * 使用内部类来解决延迟初始化单例及避免DCL（双重检查锁定的问题）
	 * 
	 * @author liangyi
	 * 
	 */
	private static class Instances {
		/** 共享、0.01秒的 */
		private static ClockTick _C001 = new ClockTickMills(10, TICKER_INIT * 100);
		/** 共享、0.1秒的 */
		private static ClockTick _C01 = new ClockTickMills(100, TICKER_INIT * 10);
		/** 共享、1秒的 */
		private static ClockTick _C1 = new ClockTick(1, TICKER_INIT);
		/** 共享、10秒的 */
		private static ClockTick _C10 = new ClockTick(10, TICKER_INIT / 10);
	}

	/**
	 * 取得可能共享的计时发布器实例
	 * 
	 * @param period
	 *            计时周期（秒）
	 * @return 可能共享的实例
	 */
	public static ClockTick getInstance(int period) {
		if (1 == period) {
			return Instances._C1;
		}
		if (10 == period) {
			return Instances._C10;
		}
		return new ClockTick(period, TICKER_INIT / period);
	}

	/**
	 * 取得可能共享的计时发布器实例
	 * 
	 * @param period
	 *            计时周期（秒）
	 * @return 可能共享的实例
	 */
	public static ClockTick getInstance(double period) {
		if (0.1 == period) {
			return Instances._C01;
		}
		if (0.01 == period) {
			return Instances._C001;
		}
		int v = (int) (period * 1000);
		if ((v / 1000) == period) {
			return getInstance((int) period);
		}
		return new ClockTickMills(v, TICKER_INIT);
	}

	/** 计数值 */
	protected volatile long m_Ticker;
	/** 计数周期（秒） */
	protected final int m_Period;

	/**
	 * 构建计时发布器
	 * 
	 * @param period
	 *            计时周期（秒）
	 * @param initTicker
	 *            初始值
	 */
	public ClockTick(int period, int initTicker) {
		m_Ticker = initTicker;
		m_Period = period;
		_Timer.schedule(this, 0, period * 1000);
	}

	protected ClockTick(int period) {
		m_Period = period;
	}

	/**
	 * 计数值
	 */
	public int getTicker() {
		return (int) m_Ticker;
	}

	/**
	 * 计数值（长整数）
	 */
	public long getTickerLong() {
		return m_Ticker;
	}

	/**
	 * 毫秒数
	 */
	public long getMills() {
		return m_Ticker * m_Period * 1000;
	}

	/**
	 * 计数周期（秒）
	 */
	public int getPeriod() {
		return m_Period;
	}

	/**
	 * 计数周期（毫秒）
	 */
	public int getPeriodMills() {
		return m_Period * 1000;
	}

	@Override
	public String toString() {
		return "{ticker:" + m_Ticker + ",period:" + m_Period + "}";
	}

	@Override
	public void run() {
		++m_Ticker;
		if (m_Ticker >= Integer.MAX_VALUE) {
			// 溢出后归1
			_Logger.error("时间计数器溢出 " + this);
			m_Ticker = 1;
		}
	}

	/**
	 * 毫秒的计时器
	 * 
	 * @author liangyi
	 * 
	 */
	public static class ClockTickMills extends ClockTick {

		/**
		 * 创建计数器
		 * 
		 * @param period
		 *            计时周期（毫秒）
		 * @param initTicker
		 *            初始值（毫秒）
		 */
		protected ClockTickMills(int period, int initTicker) {
			super(period);
			m_Ticker = initTicker;
			_Timer.schedule(this, 0, period);
		}

		@Override
		public void run() {
			++m_Ticker;
		}

		@Override
		public int getPeriod() {
			return m_Period / 1000;
		}

		@Override
		public int getPeriodMills() {
			return m_Period;
		}

		@Override
		public int getTicker() {
			if (m_Ticker > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("计数值溢出[" + m_Ticker + "]");
			}
			return (int) m_Ticker;
		}

		@Override
		public long getMills() {
			return m_Ticker * m_Period;
		}

	}
}
