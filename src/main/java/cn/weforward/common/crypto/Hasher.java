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
package cn.weforward.common.crypto;

/**
 * 散列计算工具类
 * 
 * @author liangyi
 *
 */
public class Hasher {

	/**
	 * java String 的散列计算
	 * 
	 * @param ch
	 *            要计算的字符
	 * @param hash
	 *            上次的值（初始值为0）
	 * @return 计算后的值
	 */
	public static int stringHash(char ch, int hash) {
		hash = 31 * hash + ch;
		return hash;
	}

	/**
	 * java String 的散列计算
	 * 
	 * @param str
	 *            要计算的字串
	 * @param hash
	 *            上次的值（初始值为0）
	 * @return 计算后的值
	 */
	public static int stringHash(String str, int hash) {
		if (null == str) {
			return hash;
		}
		int len = str.length();
		for (int i = 0; i < len; i++) {
			hash = 31 * hash + str.charAt(i);
		}
		return hash;
	}

	public static int stringHash(CharSequence str, int beginIndex, int endIndex, int hash) {
		for (; beginIndex < endIndex; beginIndex++) {
			hash = 31 * hash + str.charAt(beginIndex);
		}
		return hash;
	}

	/**
	 * 简易hash算法
	 * 
	 * @param str
	 *            要HASH的字串
	 * @return 对字串的UTF16内码进行HASH计算后的32位整数结果
	 */
	public static final String simpleHash(String str) {
		int h = hashInt32(str, 0);
		return String.valueOf((h < 0) ? ((0x100000000L) + h) : h);
	}

	/**
	 * 超简单的32位整数hash算法
	 * 
	 * @param str
	 *            源字串
	 * @param sign
	 *            基础hash值
	 * @return 在基础值上hash源字串后的最终值
	 */
	public static int hashInt32(String str, int sign) {
		if (null == str || 0 == str.length()) {
			return sign;
		}
		int sum;
		int h;
		int i = 0;
		int a;
		int len = str.length();
		sum = ((sign >> 16) & 0xFFFF);
		h = (sign & 0xFFFF);
		while (i < len) {
			a = str.charAt(i++);
			if (a < 256) {
				a = a << 8;
				if (i < len) {
					a |= str.charAt(i);
					a *= (i++);
				} else {
					a *= i;
				}
			} else {
				a *= i;
			}
			sum += sum ^ a;
			h += a;
			h &= 0xFFFF;
			sum &= 0xFFFF;
			// _Logger.trace("sum="+sum+" h="+h+" i="+i);
		}
		if (sum > 0x7FFF) {
			long fix = sum;
			fix <<= 16;
			fix |= h;
			return (int) (fix - 0x100000000L);
		}
		return ((sum << 16) | h);
	}

	/**
	 * 64位整数hash算法
	 * 
	 * @param str
	 * @param sign
	 * @return
	 */
	public static long hashInt64(String str, int sign) {
		if (null == str || 0 == str.length()) {
			return sign;
		}
		long hash = hashInt32(str, sign);
		hash = (hash << 32) >>> 32;
		// hash |= (long) hashCode(str) << 32;
		hash |= (long) Hasher.stringHash(str, 0) << 32;
		return hash;
	}

}
