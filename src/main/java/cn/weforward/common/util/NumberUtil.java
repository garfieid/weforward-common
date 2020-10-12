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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数字工具类
 * 
 * @author daibo
 *
 */
public class NumberUtil {
	/** 日志记录器 */
	public final static Logger _Logger = LoggerFactory.getLogger(NumberUtil.class);
	/** 0项的int数组 */
	public static final int[] _nilInts = new int[0];
	/** 0项的long数组 */
	public static final long[] _nilLongs = new long[0];
	/** 0项的double数组 */
	public static final double[] _nilDoubles = new double[0];

	/**
	 * 转换字串为32位整数
	 * 
	 * @param str 字串
	 * @return 相应的数值
	 * @throws NumberFormatException 格式无效则异常
	 */
	public static final int toInt(String str) throws NumberFormatException {
		// 如果字串为空时，返回defaultValue
		if (null == str || 0 == str.length()) {
			return 0;
		}

		char first = str.charAt(0);
		if ('0' == first && str.length() > 2) {
			// 如果是以0x开首表示是十六进制
			first = str.charAt(1);
			if ('x' == first || 'X' == first) {
				return Integer.parseInt(str.substring(2), 16);
			}
		} else if (str.length() > 1 && ('x' == first || 'X' == first)) {
			// x开首以十六进制
			return Integer.parseInt(str.substring(1), 16);
		}
		return Integer.parseInt(str);
	}

	/**
	 * 转换字串为int
	 * 
	 * @param str          字串
	 * @param defaultValue 格式无效则使用此值
	 * @return 相应的数值，格式无效则返回 defaultValue
	 */
	public static int toInt(String str, int defaultValue) {
		// 如果字串为空时，返回defaultValue
		if (null == str || 0 == str.length()) {
			return defaultValue;
		}

		try {
			char first = str.charAt(0);
			if ('0' == first && str.length() > 2) {
				// 如果是以0x开首表示是十六进制
				first = str.charAt(1);
				if ('x' == first || 'X' == first) {
					return Integer.parseInt(str.substring(2), 16);
				}
			} else if (str.length() > 1 && ('x' == first || 'X' == first)) {
				// x开首以十六进制
				return Integer.parseInt(str.substring(1), 16);
			}
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			if (_Logger.isDebugEnabled()) {
				_Logger.debug("解析" + str + "异常", e);
			}
		}
		return defaultValue;
	}

	/**
	 * 转换字串为double
	 * 
	 * @param str          （带小数）数值
	 * @param defaultValue 格式无效则使用此值
	 * @return 相应的数值，格式无效则返回 defaultValue
	 */
	public static double toDouble(String str, double defaultValue) {
		// 如果字串为空时，返回defaultValue
		if (null == str || 0 == str.length()) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException e) {
			if (_Logger.isDebugEnabled()) {
				_Logger.debug("解析" + str + "异常", e);
			}
		}
		return defaultValue;
	}

	/**
	 * 是否数字
	 * 
	 * @param ch 字符
	 * @return 是则返回true
	 */
	public static final boolean isNumber(char ch) {
		return (ch > 0x30 - 1 && ch < 0x39 + 1);
	}

	/**
	 * 是否纯（半角）数字
	 * 
	 * @param str 要检查的字串
	 * @return 是则返回true
	 */
	public static final boolean isNumber(String str) {
		if (null == str || 0 == str.length()) {
			return false;
		}
		for (int i = str.length() - 1; i >= 0; i--) {
			if (!isNumber(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
