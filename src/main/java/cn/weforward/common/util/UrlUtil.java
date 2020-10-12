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

import java.io.UnsupportedEncodingException;

/**
 * url工具类
 * 
 * @author daibo
 *
 */
public class UrlUtil {
	/** 用来将字节转换成 16 进制表示的字符表 */
	private static final char _HEXDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F' };

	/**
	 * 编码字符
	 * 
	 * @param b
	 * @param sb
	 */
	public static final void encodeUrl(int b, StringBuilder sb) {
		sb.append('%');
		sb.append(_HEXDigits[(b >> 4) & 0xF]);
		sb.append(_HEXDigits[(b) & 0xF]);
	}

	/**
	 * 按UTF-8字符集对字串进行URL编码
	 * 
	 * @param str 要编码的字串
	 * @param sb  编码输出至StringBuilder
	 * @return 编码输出后的StringBuilder
	 */
	public static final StringBuilder encodeUrl(String str, StringBuilder sb) {
		return encodeUrl(str, ".-_*", sb);
	}

	/**
	 * 按UTF-8字符集对字串进行URL编码
	 * 
	 * @param str     要编码的字串
	 * @param symbols 允许不编码的符号表
	 * @param sb      编码输出至StringBuilder
	 * @return 编码输出后的StringBuilder
	 */
	public static final StringBuilder encodeUrl(String str, String symbols, StringBuilder sb) {
		if (null == str) {
			return ((null == sb) ? new StringBuilder(0) : sb);
		}
		if (null == sb) {
			sb = new StringBuilder(str.length() * 3);
		}
		int len = str.length();
		char ch;
		for (int i = 0; i < len; i++) {
			ch = str.charAt(i);
			if (symbols.indexOf(ch) >= 0) {
				// 是指定不用编码的符号或字符
				sb.append(ch);
			} else if (ch < 0 || ch > 0x7F) {
				/*
				 * UNICODE编码规则： 0x00000000 - 0x0000007F 0xxxxxxx ANSI码（英文,数字符号）占一个byte
				 * 0x00000080 - 0x000007FF 110xxxxx 10xxxxxx 0x00000800 - 0x0000FFFF 1110xxxx
				 * 10xxxxxx 10xxxxxx 中日韩文等编码占三个byte 0x00010000 - 0x001FFFFF 11110xxx 10xxxxxx
				 * 10xxxxxx 10xxxxxx 0x00200000 - 0x03FFFFFF 111110xx 10xxxxxx 10xxxxxx 10xxxxxx
				 */
				int utf16 = (ch < 0) ? (0x10000 + ch) : ch;
				if (utf16 >= 0x80 && utf16 < 0x07ff) {
					encodeUrl(0xC0 | (0x1F & (utf16 >> 6)), sb);
					encodeUrl(0x80 | (0x3F & utf16), sb);
				} else if (utf16 >= 0x0800) {
					encodeUrl(0xE0 | (0x0F & (utf16 >> 12)), sb);
					encodeUrl(0x80 | (0x3F & (utf16 >> 6)), sb);
					encodeUrl(0x80 | (0x3F & utf16), sb);
					// } else {
					// // 不会吧，还有这种错误的情况？
					// encodeUrl('?', sb);
				}
			} else {
				// if ('.' == ch || '-' == ch || '_' == ch || '*' == ch || (ch
				// >= '0' && ch <= '9')
				if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
					// 数字及字母不用编码
					sb.append(ch);
				} else {
					// 按%xx编码
					encodeUrl(ch, sb);
				}
			}
		}
		return sb;
	}

	/**
	 * 按UTF-8字符集对字串进行URL编码
	 * 
	 * @param str 要编码的字串
	 * @return 编码后字串
	 */
	public static final String encodeUrl(String str) {
		if (null == str || 0 == str.length()) {
			return "";
		}
		return encodeUrl(str, null).toString();
	}

	/**
	 * URL编码解码
	 * 
	 * @param url URL串
	 * @return 解码后的字串
	 */
	public static String decodeUrl(String url) {
		if (StringUtil.isEmpty(url)) {
			return url;
		}
		try {
			return java.net.URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(e);
		}
	}
}
