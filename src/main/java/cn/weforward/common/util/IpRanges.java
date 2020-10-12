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

/**
 * （IPv4）地址段
 * 
 * @author liangyi
 * 
 */
public class IpRanges {
	/** IP段列表 */
	protected List<Range> m_RangeList = Collections.emptyList();

	public IpRanges() {
	}

	/**
	 * 构造地址（段）表，每IP（段）项以分号分隔，如：“127.0.0.1;192.168.0.0-192.168.0.100”
	 * 
	 * @param ipList 地址（段）表
	 */
	public IpRanges(String ipList) {
		if (StringUtil.isEmpty(ipList)) {
			return;
		}
		String ss[] = ipList.split("\\;");
		if (ss.length > 0) {
			List<Range> ls = new ArrayList<Range>(ss.length);
			for (String s : ss) {
				if (!StringUtil.isEmpty(s)) {
					ls.add(new Range(s));
				}
			}
			m_RangeList = ls;
		}
	}

	/**
	 * 取得地址所处的地址段
	 * 
	 * @param address 地址
	 * @return 找到则返回所处地址段，否则返回null
	 */
	public Range find(String address) {
		return find(ipv4(address));
	}

	/**
	 * 取得地址所处的地址段
	 * 
	 * @param address 地址
	 * @return 找到则返回所处地址段，否则返回null
	 */
	public Range find(byte[] address) {
		int ip = ipv4(address);
		if (0 == ip) {
			return null;
		}
		return find(ip);
	}

	/**
	 * 取得地址所处的地址段
	 * 
	 * @param ip 32位IP地址
	 * @return 找到则返回所处地址段，否则返回null
	 */
	public Range find(int ip) {
		// 先检查访问是否在允许ip表中
		for (Range ipr : m_RangeList) {
			if (ipr.match(ip)) {
				// 找到了
				return ipr;
			}
		}
		return null;
	}

	/**
	 * 设置IP列表，每项的格式为“192.168.0.1-192.168.0.4”或“192.168.0.1”
	 * 
	 * @param ipList 地址列表
	 */
	public void set(List<String> ipList) {
		m_RangeList = new ArrayList<Range>(ipList.size());
		for (String s : ipList) {
			m_RangeList.add(new Range(s));
		}
	}

	/**
	 * 转为32位的ip数值
	 * 
	 * @param addr IP地址
	 * @return 32位数值
	 */
	static public int ipv4(byte[] addr) {
		if (null == addr || addr.length != 4) {
			return 0;
		}
		int ip = addr[3] & 0xFF;
		ip |= ((addr[2] << 8) & 0xFF00);
		ip |= ((addr[1] << 16) & 0xFF0000);
		ip |= ((addr[0] << 24) & 0xFF000000);
		return ip;
	}

	/**
	 * 转为32位的ip数值
	 * 
	 * @param ip IP地址串
	 * @return 32位数值
	 */
	static public int ipv4(String ip) {
		int v4 = 0;
		int idx;
		int begin = 0;
		idx = ip.indexOf('.', begin);
		if (-1 == idx) {
			// 无效地址
			return 0;
		}
		v4 = NumberUtil.toInt(ip.substring(begin, idx)) << 24;
		begin = idx + 1;
		idx = ip.indexOf('.', begin);
		if (-1 == idx) {
			// 无效地址
			return 0;
		}
		v4 |= NumberUtil.toInt(ip.substring(begin, idx)) << 16;
		begin = idx + 1;
		idx = ip.indexOf('.', begin);
		if (-1 == idx) {
			// 无效地址
			return 0;
		}
		v4 |= NumberUtil.toInt(ip.substring(begin, idx)) << 8;
		v4 |= NumberUtil.toInt(ip.substring(idx + 1));
		return v4;
	}

	/**
	 * 地址段项
	 * 
	 * @author liangyi
	 * 
	 */
	public static class Range {
		public final int start;
		public final int end;

		/**
		 * 构造地址段
		 * 
		 * @param ips 地址，如“192.168.0.1-192.168.0.4”，“192.168.0.1”
		 */
		public Range(String ips) {
			int idx = ips.indexOf('-');
			if (-1 == idx) {
				this.end = this.start = ipv4(ips);
			} else {
				this.start = ipv4(ips.substring(0, idx));
				this.end = ipv4(ips.substring(idx + 1));
			}
		}

		public Range(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean match(int ip) {
			// if (this.end < 0 && ip >= 0 && this.start > 0) {
			// // 当end为负数时（即大于或等于128.0.0.0）且要匹配的ip大于0，只需要检查是否大于start即可
			// return (ip >= this.start);
			// }
			// XXX 不能无符号比较，累啊:(
			if (this.end < 0) {
				if (this.start >= 0) {
					// 当end为负数时（即大于或等于128.0.0.0）
					if (ip >= 0) {
						// 要匹配的ip不小于0，只需要检查是否大于start即可
						return (ip >= this.start);
					}
					return (ip <= this.end);
				}
			}
			// 都是正数
			return (ip >= this.start && ip <= this.end);
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

	}
}
