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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.util.StringUtil;

/**
 * ID生成器
 * 
 * @author liangyi
 *
 */
public abstract class IdGenerator {
	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(IdGenerator.class);
	// /** 用于IdGenerator.Simple生成器的时间戳刷新定时器 */
	// final static Timer _Timer = new Timer("IdGenerator-Timer", true);

	/** 服务器标识 */
	protected String m_ServerId;

	protected IdGenerator(String serverId) {
		m_ServerId = StringUtil.toString(serverId);
	}

	/**
	 * ID包含的服务器标识如x000a
	 */
	public String getServerId() {
		return m_ServerId;
	}

	/**
	 * 产生新ID
	 * 
	 * @param prefix
	 *            前缀，可为null
	 * @return 新的ID
	 */
	public abstract String genId(String prefix);

	/**
	 * 由联合标识的序号尝试分析服务器标识（ID），格式类似”xxxx-xxxx“的最后的xxxx
	 * 
	 * @param ordinal
	 *            （ID）序号
	 * @return 服务器标识没有则返回null
	 */
	public static String getServerId(String ordinal) {
		if (null == ordinal || ordinal.length() < 5) {
			return null;
		}
		int idx = ordinal.lastIndexOf('-');
		if (-1 != idx) {
			return ordinal.substring(idx + 1);
		}
		return null;
	}

	/**
	 * 56位整数HEX字串，不足14个字符前端补0
	 * 
	 * @param val
	 *            56位数值
	 * @param builder
	 *            字串缓冲区
	 * @return 16字符的HEX编码串
	 */
	public static StringBuilder toHex(long val, StringBuilder builder) {
		// 高24位
		int i32 = (int) ((val >> 32) & 0xFFFFFFFF);
		Hex.toHexFixed24(i32, builder);
		// 低32位
		i32 = (int) (val & 0xFFFFFFFF);
		Hex.toHexFixed(i32, builder);
		return builder;
	}

	/**
	 * 基于一定节奏毫秒单位的时间戳ID生成器，（56位）长度为14个十六进制字符
	 * 
	 * @author liangyi
	 *
	 */
	public static class Tick extends IdGenerator {
		static final Timestamp _Timestamp = Timestamp.getInstance(Timestamp.POLICY_DEFAULT);

		public Tick(String serverId) {
			super(serverId);
		}

		@Override
		public String genId(String prefix) {
			long id = _Timestamp.next(0);
			// 去掉最后的8位服务器标识
			id >>= 8;
			StringBuilder sb = new StringBuilder(
					16 + (null == m_ServerId ? 0 : m_ServerId.length()) + (null == prefix ? 0 : prefix.length()));
			if (!StringUtil.isEmpty(prefix)) {
				sb.append(prefix);
			}
			toHex(id, sb);
			if (!StringUtil.isEmpty(m_ServerId)) {
				sb.append('-');
				sb.append(m_ServerId);
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(32);
			sb.append("{server:");
			sb.append(getServerId());
			sb.append(",ts:").append(_Timestamp);
			sb.append("}");
			return sb.toString();
		}
	}
}
