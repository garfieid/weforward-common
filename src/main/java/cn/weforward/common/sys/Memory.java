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

import java.io.InterruptedIOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.weforward.common.NameItem;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.TimeUtil;

/**
 * 内存状态
 * 
 * @author liangyi
 * 
 */
public class Memory {
	/** 可用内存很多 */
	public static final NameItem MEMORY_IDEL = NameItem.valueOf("空闲", 0x01);
	/** 内存使用情况正常 */
	public static final NameItem MEMORY_NORMAL = NameItem.valueOf("正常", 0x0a);
	/** 内存低，尽量去除缓存 */
	public static final NameItem MEMORY_LOW = NameItem.valueOf("低", 0x0e);
	/** 内存严重不足，能扔的都扔了吧 */
	public static final NameItem MEMORY_CRITICAL = NameItem.valueOf("紧张", 0x10);
	/** 内存严重不足，能扔的都扔了吧 */
	public static final NameItem MEMORY_SUSPEND = NameItem.valueOf("将挂掉", 0x1e);

	/**
	 * 默认内存使用状态刷新周期（毫秒）
	 */
	public static int getRefreshPeriod() {
		int period = NumberUtil.toInt(System.getProperty("cn.weforward.common.sys.period"),
				8 * 60 * 1000);
		return period;
	}

	/** 上限 */
	public long max;
	/** （最大）可用的 */
	public long usable;
	/** 已分配的 */
	public long alloc;
	/** full-GC次数 */
	public int gcCount;
	/** full-GC消耗时间（秒） */
	public int gcTime;

	/** 最后计算的内存状态 */
	protected NameItem m_Level = MEMORY_NORMAL;
	/** 等待锁 */
	final protected Lock m_Lock;
	/** 状态事件 */
	final protected Condition m_LevelEvent;
	/** 内存空间严重不足界线 */
	protected long m_Critical;
	/** full-GC相关的收集器（通常是旧生代） */
	protected GarbageCollectorMXBean m_gc;

	public Memory() {
		this.m_Lock = new ReentrantLock();
		this.m_LevelEvent = this.m_Lock.newCondition();
		// m_Critical = 512 * 1024 * 1024L;

		List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gc : gcs) {
			String name = gc.getName();
			// PS GC, GMS GC, G1 GC
			if (name.equals("PS MarkSweep") || name.equals("ConcurrentMarkSweep")
					|| name.contains(" Old ")) {
				m_gc = gc;
				break;
			}
		}
	}

	/**
	 * 刷新内存使用状态
	 * 
	 * @return MEMORY_xxx
	 */
	public int refresh() {
		Lock lock = null;
		if (m_Lock.tryLock()) {
			lock = m_Lock;
		}
		try {
			Runtime runtime = Runtime.getRuntime();
			max = runtime.maxMemory();
			alloc = runtime.totalMemory();
			usable = runtime.freeMemory();
			if (Long.MAX_VALUE == max) {
				max = alloc;
			}
			if (max > alloc) {
				// runtime.freeMemory()只是返回已分配内存未使用的部分，所以要加上max与total的差
				usable += max - alloc;
			}
			this.m_Level = calcSate();
			if (null != lock) {
				m_LevelEvent.signalAll();
			}
		} finally {
			if (null != lock) {
				m_Lock.unlock();
			}
		}

		// full-GC计数
		if (null != m_gc) {
			this.gcCount = (int) m_gc.getCollectionCount();
			this.gcTime = (int) m_gc.getCollectionTime() / 1000;
		}
		return this.m_Level.id;
	}

	public long getMax() {
		return max;
	}

	/** （最大）可用的 */
	public long getUsable() {
		return usable;
	}

	/** 已分配的 */
	public long getAlloc() {
		return alloc;
	}

	/** full-GC次数 */
	public int getGcCount() {
		return gcCount;
	}

	/** full-GC消耗时间（秒） */
	public int getGcTime() {
		return gcTime;
	}

	public NameItem getLevel() {
		return this.m_Level;
	}

	/**
	 * 检查内存是否达到指定的状态以上（MEMORY_SUSPEND->MEMORY_CRITICAL->MEMORY_LOW->
	 * MEMORY_NORMAL ->MEMORY_IDEL）
	 * 
	 * @param level
	 *            要检查的内存状态
	 * @return 在状态以上则返回true
	 */
	public boolean checkLevel(NameItem level) {
		return (level.id >= this.m_Level.id);
	}

	/**
	 * 等待内存达到指定的状态以上（MEMORY_SUSPEND->MEMORY_CRITICAL->MEMORY_LOW->MEMORY_NORMAL
	 * ->MEMORY_IDEL）
	 * 
	 * @param level
	 *            要等的内存状态
	 * @param timeoutMs
	 *            等待超时值（毫秒），=0则永远等待
	 * @return 等待时间内等到则返回剩余时间，超时返回-1
	 */
	public int waitFor(NameItem level, int timeoutMs) throws InterruptedIOException {
		if (level.id >= this.m_Level.id) {
			// yeah,不用等
			return timeoutMs;
		}

		long ts = TimeUtil.currentTimeMillis();
		int interval;
		Lock lock = null;
		try {
			m_Lock.lockInterruptibly();
			lock = m_Lock;
			interval = (timeoutMs > 0) ? (timeoutMs - (int) (TimeUtil.currentTimeMillis() - ts))
					: 0;
			while (interval >= 0) {
				// 等啊
				if (0 == timeoutMs) {
					m_LevelEvent.await();
				} else {
					m_LevelEvent.await(interval, TimeUnit.MILLISECONDS);
					interval = timeoutMs - (int) (TimeUtil.currentTimeMillis() - ts);
				}
				if (level.id >= this.m_Level.id) {
					// 等到了:)
					return interval > 0 ? interval : 0;
				}
			}
		} catch (InterruptedException e) {
			// // 恢复回中断状态
			// Thread.currentThread().interrupt();
			throw new InterruptedIOException("等待内存可用[" + level + "]时中断");
		} finally {
			if (null != lock) {
				lock.unlock();
			}
		}
		return interval;
	}

	/**
	 * 设定内存空间严重不足界线
	 * 
	 * @param bytes
	 *            不足界线（单位字）
	 */
	public void setCritical(long bytes) {
		m_Critical = bytes;
	}

	/**
	 * 统计内存使用状态 MEMORY_xxx
	 */
	protected NameItem calcSate() {
		if (m_Critical > 0 && this.usable < m_Critical) {
			// 指定了严重不足界线
			return MEMORY_CRITICAL;
		}
		// 计算状态
		NameItem state = MEMORY_NORMAL;
		// 空闲内存百分率
		int rate = (int) ((this.usable * 100) / this.max);
		if (rate <= 5 && m_Critical <= 0) {
			// <=5% 且没有指定critical值
			state = MEMORY_CRITICAL;
		} else if (rate <= 10 && this.usable < 50 * 1024 * 1024 && m_Critical <= 0) {
			// <=10% 且 <50M
			state = MEMORY_CRITICAL;
		} else if (rate < 15 && this.usable <= 0x7FFFFFFF) {
			// <15% and < 2G
			state = MEMORY_LOW;
			// } else if (rate < 30 && VmStat.getMemory().usable < 10 * 1024 *
			// 1024) {
			// // <30% and <10M
			// state=;
		} else if (rate >= 50) {
			// <50%
			state = MEMORY_IDEL;
			// } else if (rate >= 30) {
			// // >=30%
			// state = MEMORY_NORMAL;
		}
		if (this.usable < (32 * 1024 * 1024)
				|| (rate < 5 && m_Critical > 0 && this.usable < m_Critical)) {
			// <5% or 32M，要挂掉了吧
			state = MEMORY_SUSPEND;
		}
		return state;
	}

	@Override
	public String toString() {
		// return "{used:" + (max - usable) / 1024 + ",usable:" + usable / 1024
		// + ",max:" + max / 1024
		// + ",alloc:" + alloc / 1024 + "}";
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			return toString(builder).toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("{used:");
		Bytes.formatHumanReadable(sb, (max - usable));
		sb.append(",usable:");
		Bytes.formatHumanReadable(sb, usable);
		sb.append(",max:");
		Bytes.formatHumanReadable(sb, max);
		if (max != alloc) {
			sb.append(",alloc:");
			Bytes.formatHumanReadable(sb, alloc);
		}
		if (null != m_Level) {
			sb.append(",level:").append(m_Level.name);
		}
		sb.append(",gc:").append(this.gcCount);
		sb.append(",gc-t:").append(this.gcTime);
		sb.append("}");
		return sb;
	}
}
