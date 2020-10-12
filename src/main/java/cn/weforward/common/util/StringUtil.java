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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cn.weforward.common.crypto.Base64;

/**
 * 各种零散的工具方法集合
 * 
 * @author zhangpengji
 *
 */
public class StringUtil {
	/** UTF-16的最后一个字符 */
	public static final char UNICODE_REPLACEMENT_CHAR = 0xfffd;
	/** UTF-16的最后一个字符 */
	public static final String UNICODE_REPLACEMENT_STRING = String
			.valueOf(UNICODE_REPLACEMENT_CHAR);
	/** 0项的字串数组 */
	public static final String[] _nilStrings = new String[0];

	/**
	 * 是否空字串（null或length为0）
	 * 
	 * @param str
	 *            字串
	 * @return true/false
	 */
	public static final boolean isEmpty(String str) {
		return (str == null || 0 == str.length());
	}

	/**
	 * 比较两字串是否相同
	 * 
	 * @param s1
	 *            字串1
	 * @param s2
	 *            字串2
	 * @return 相同则返回true
	 */
	public static final boolean eq(String s1, String s2) {
		if (s1 == s2) {
			return true;
		}
		return (null != s1 && s1.equals(s2));
	}

	/**
	 * 将6ase64字符串转换成输入流
	 * 
	 * @param base64String
	 * @return
	 */
	public static InputStream fromBase64String(String base64String) {
		int index = base64String.indexOf(',');
		if (index > 0) {
			base64String = base64String.substring(index + 1);
		}
		return new ByteArrayInputStream(Base64.decode(base64String));
	}

	/**
	 * 把字串限制在指定长度内
	 * 
	 * @param str
	 *            原始字串
	 * @param max
	 *            限制的长度
	 * @return 限制长度后的字串
	 */
	public static String limit(String str, int max) {
		if (null == str || 0 == str.length() || str.length() < max) {
			return str;
		}
		str = str.substring(0, max) + "...";
		return str;
	}

	public static String toString(Object obj) {
		if (obj == null) {
			return "";
		}
		return obj.toString().trim();
	}

	/**
	 * 检查输入的字符流是否UTF-8字符
	 * 
	 * @param bytes
	 *            字节数组（字串流）
	 * @param offset
	 *            开始位置（字节）
	 * @param len
	 *            长度（字节）
	 * @return 是则返回true
	 */
	// 0000 0000-0000 007F - 0xxxxxxx (ascii converts to 1 octet!)
	// 0000 0080-0000 07FF - 110xxxxx 10xxxxxx ( 2 octet format)
	// 0000 0800-0000 FFFF - 1110xxxx 10xxxxxx 10xxxxxx (3 octet format)
	public static boolean isUtf8(byte[] bytes, int offset, int len) {
		boolean bAllAscii = true;
		byte cOctets = 0; // octets to go in this UTF-8 encoded character
		short chr;
		for (int i = offset; i < len; i++) {
			chr = bytes[i];
			if (0 != (chr & 0x80)) {
				bAllAscii = false;
				chr += 0x100;
			}
			if (0 == cOctets) {
				if (chr >= 0x80) {
					do {
						chr <<= 1;
						cOctets++;
					} while (0 != (chr & 0x80));
					cOctets--;
					if (0 == cOctets) {
						return false;
					}
				}
			} else {
				if (0x80 != (chr & 0xC0)) {
					return false;
				}
				cOctets--;
			}
		}
		if (cOctets > 0 || bAllAscii) {
			return false;
		}
		return true;
	}

	/**
	 * 比较两字串大小
	 * 
	 * @param s1
	 *            字串1
	 * @param s2
	 *            字串2
	 * @return s1==s2返回0,s1>s2返回大于0，s1<s2返回小于0
	 */
	public static final int compareTo(String s1, String s2) {
		if (s1 == s2) {
			return 0;
		}
		if (null == s1) {
			return -1;
		}
		if (null == s2) {
			return 1;
		}
		return s1.compareTo(s2);
	}

	/**
	 * 子串包装（共享根串）
	 * 
	 * @param str
	 *            串
	 * @param start
	 *            子串开始位置
	 * @param end
	 *            子串结束位置
	 * @return 子串
	 * @throws IndexOutOfBoundsException
	 */
	public static CharSequence subSequence(CharSequence str, int start, int end)
			throws IndexOutOfBoundsException {
		if (start == end) {
			return "";
		}
		if (0 == start && str.length() == end) {
			return str;
		}
		return new CharSequenceSlice(str, start, end);
	}

	/**
	 * 子串包装类（String已经没有共享内存的子串，无奈硬支持）
	 * 
	 * @author liangyi
	 *
	 */
	public static class CharSequenceSlice implements CharSequence {
		/** 字串 */
		final CharSequence root;
		/** 子串位置 */
		int start, end;

		public CharSequenceSlice(CharSequence str, int start, int end)
				throws IndexOutOfBoundsException {
			if (end < start || start < 0 || end > str.length()) {
				throw new IndexOutOfBoundsException("start or end over " + 0 + "~" + str.length());
			}
			this.end = end;
			this.start = start;
			this.root = str;
		}

		private int checkIndex(int index) {
			if (index < start || index > end) {
				throw new IndexOutOfBoundsException(index + " over " + start + "~" + end);
			}
			return index;
		}

		@Override
		public int length() {
			return end - start;
		}

		@Override
		public char charAt(int index) throws IndexOutOfBoundsException {
			return root.charAt(checkIndex(start + index));
		}

		@Override
		public CharSequence subSequence(int start, int end) throws IndexOutOfBoundsException {
			return root.subSequence(checkIndex(this.start + start), checkIndex(this.start + end));
		}

		@Override
		public String toString() {
			return root.subSequence(start, end).toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof CharSequence) {
				CharSequence another = (CharSequence) obj;
				int n = length();
				if (n == another.length()) {
					for (int i = 0; i < n; i++) {
						if (root.charAt(i + start) != another.charAt(i)) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}
	}
}
