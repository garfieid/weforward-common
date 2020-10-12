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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

import cn.weforward.common.util.NumberUtil;

/**
 * 
 * @author liangyi
 *
 */
public class VmStat {

	/** 内存使用状态 */
	protected Memory m_Memory;
	/** 系统上一分钟时的负载情况百分比，(0~100)*CPU数，如：单CPU下，100表系统满载，500表示系统快忙崩，最好是在70以下 */
	protected int m_LoadAverage;
	/** 当前JVM进程CPU负载百分比 */
	protected int m_ProcessCpuLoad;
	/** 当前JVM线程数 */
	protected int m_ThreadCount;
	/** JVM进程ID */
	protected int m_Pid;

	/** CPU数量 */
	static public int _cpus = Runtime.getRuntime().availableProcessors();
	public final static VmStat _VmStat = new VmStat();

	static public Memory getMemory() {
		return _VmStat.m_Memory;
	}

	/**
	 * 系统负载（0%～100%）
	 */
	static public int getLoadAverage() {
		return _VmStat.m_LoadAverage;
	}

	/**
	 * 当前JVM进程CPU负载（0%～100%）
	 */
	static public int getProcessCpuLoad() {
		return _VmStat.m_ProcessCpuLoad;
	}

	public static int getThreadCount() {
		return _VmStat.m_ThreadCount;
	}

	static public int getPid() {
		return _VmStat.m_Pid;
	}

	static public void refresh() {
		_VmStat.refresh0();
	}

	private VmStat() {
		m_Memory = new Memory();
		m_ProcessCpuLoad = -1;
		String name = ManagementFactory.getRuntimeMXBean().getName();
		int idx = name.indexOf('@');
		if (idx > 0) {
			name = name.substring(0, idx);
			m_Pid = NumberUtil.toInt(name, 0);
		}
	}

	@SuppressWarnings("restriction")
	public void refresh0() {
		m_Memory.refresh();
		synchronized (this) {
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			if (null != os) {
				m_LoadAverage = (int) (os.getSystemLoadAverage() * 100);
				if (os instanceof com.sun.management.OperatingSystemMXBean) {
					m_ProcessCpuLoad = (int) (100
							* ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad());
				}
			}
			ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
			if (null != tmx) {
				m_ThreadCount = tmx.getThreadCount();
			}
			// this.notifyAll();
		}
	}

	@Override
	public String toString() {
		return "{pid:" + m_Pid + ",cpus:" + _cpus + ",cpu-load:" + m_ProcessCpuLoad + ",sys-load:"
				+ m_LoadAverage + ",mem:" + m_Memory + "}";
	}
}
