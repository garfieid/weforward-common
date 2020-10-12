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
package cn.weforward.common;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import cn.weforward.common.execption.OverquotaException;

/**
 * 用于控制资源使用的简单配额控制
 * 
 * <pre>
 * 通常的使用场景为：
 * 1. 设定总配额或按资源指定
 * 2. 资源在使用配额时调用 use
 * 3. 资源使用完全释放（归还）配额时使用 refund
 * </pre>
 * 
 * @author liangyi
 *
 */
public interface Quotas {

	/**
	 * 可配额控制的项
	 * 
	 * @author liangyi
	 *
	 */
	public static interface Governable {
	}

	/**
	 * 最大值（总配额）
	 */
	public int getMax();

	/**
	 * 正使用的（总）配额
	 */
	public int getCount();

	/**
	 * 指定资源的当前可用配额
	 * 
	 * @param governable
	 *            受配额控制的资源项
	 * @param concurrent
	 *            资源的并发数（正在使用数）
	 * @return 相应的配额
	 */
	public int getQuota(Governable governable, int concurrent);

	/**
	 * 使用配额
	 * 
	 * @param governable
	 *            使用配额的资源项
	 * @param concurrent
	 *            资源的并发数（正在使用数）
	 * @return 已使用的（总）配额
	 * @throws OverquotaException
	 *             若超过配额抛出异常
	 */
	public int use(Governable governable, int concurrent) throws OverquotaException;

	/**
	 * 归还配额
	 * 
	 * @param resource
	 *            要归还配额的资源
	 * @return 正使用的（总）配额
	 */
	public int refund(Object resource);

	/**
	 * 超简的配额控制，总按可用额的2/3计算直至0，默认最大配额为100
	 * 
	 * @author liangyi
	 *
	 */
	public static class SimpleQuotas implements Quotas {
		/** 最大限额 */
		protected int m_Max;
		/** 已用配额 */
		protected AtomicInteger m_Count;

		public SimpleQuotas() {
			this(100);
		}

		public SimpleQuotas(int max) {
			m_Max = max;
			m_Count = new AtomicInteger(0);
		}

		public void setMax(int max) {
			m_Max = max;
		}

		@Override
		public int getMax() {
			return m_Max;
		}

		@Override
		public int getCount() {
			return m_Count.get();
		}

		protected int calcQuota(int quota, Governable resource, int concurrent) {
			// int q = m_Max - m_Count.get();
			// if (q <= 0) {
			// // 用尽了:(
			// return 0;
			// }
			// if (q > 1) {
			// // 可用的1/2
			// q = concurrent + (q / 2);
			// } else {
			// q = concurrent + q;
			// }
			// if (q > m_Max) {
			// q = m_Max;
			// }
			// return q;

			if (quota <= 0) {
				// 用尽了:(
				return 0;
			}

			// // 很简单粗暴的每次返回可用的配额的1/2，每资源最大占用配额大概1/3
			// if (quota > 2) {
			// // 可用的1/2
			// return quota / 2;
			// }

			if (quota > 3) {
				// 可用的2/3
				return quota * 2 / 3;
			}

			// 一个可用额度
			return 1;
		}

		@Override
		public int getQuota(Governable resource, int concurrent) {
			return calcQuota(m_Max - m_Count.get(), resource, concurrent);
		}

		@Override
		public int use(Governable resource, int concurrent) throws OverquotaException {
			int count = m_Count.incrementAndGet();
			if (count > m_Max) {
				// 超额了，减一对冲前面的加一，然后抛出超额异常
				m_Count.decrementAndGet();
				throw new OverquotaException("超额{count:" + count + ",concurrent:" + concurrent
						+ ",max:" + m_Max + ",res:" + resource + "}");
			}
			// if (concurrent > 0 && concurrent >= getQuota(resource,
			// concurrent)) {
			if (concurrent > 0) {
				int q = calcQuota(m_Max - count, resource, concurrent);
				if (concurrent >= q) {
					// 当前资源满额了，减一对冲前面的加一，然后抛出满额异常
					m_Count.decrementAndGet();
					throw new RejectedExecutionException(
							"满额{count:" + (count - 1) + ",concurrent:" + concurrent + ",quota:" + q
									+ ",max:" + m_Max + ",res:" + resource + "}");
				}
			}
			return count;
		}

		@Override
		public int refund(Object resource) {
			return m_Count.decrementAndGet();
		}

		/**
		 * 重置（置零）已用配额，特殊情况下使用
		 */
		public void reset() {
			m_Count.set(0);
		}

		@Override
		public String toString() {
			return "{max:" + m_Max + ",count:" + m_Count.get() + "}";
		}
	}

	/** 无限制的配额 */
	public static final Quotas _NoLimit = new Quotas() {
		/** 已用配额 */
		final protected AtomicInteger m_Count = new AtomicInteger(0);

		@Override
		public int getMax() {
			return Integer.MAX_VALUE;
		}

		@Override
		public int getCount() {
			return m_Count.get();
		}

		@Override
		public int getQuota(Governable resource, int concurrent) {
			return Integer.MAX_VALUE;
		}

		@Override
		public int use(Governable resource, int concurrent) throws OverquotaException {
			return m_Count.incrementAndGet();
		}

		@Override
		public int refund(Object resource) {
			return m_Count.decrementAndGet();
		}

		@Override
		public String toString() {
			return "{nolimit:" + m_Count.get() + "}";
		}
	};

}
