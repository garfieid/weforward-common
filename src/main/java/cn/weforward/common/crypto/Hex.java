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

import java.io.IOException;

import cn.weforward.common.util.StringUtil;

/**
 * Hex格式编/解码工具。
 * 
 * @author zhangpengji
 *
 */
public class Hex {
	/** 用来将字节转换成 16 进制表示的字符表 */
	public static final char _hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
			'b', 'c', 'd', 'e', 'f' };

	/** 用来将16 进制表示的字符转换成字节表（以ASCII表的0-9,a-f,A-F部分表示） */
	static final int _hexTable0_f[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1,
			-1, -1, -1, -1, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0x0A, 0x0B, 0x0C,
			0x0D, 0x0E, 0x0F };

	public static String encode(byte[] data) {
		return encode(data, 0, data.length);
	}

	public static String encode(byte[] data, int offset, int len) {
		if (null == data) {
			return null;
		}
		char[] hex = new char[len * 2];
		int k = 0;
		for (int i = 0; i < len; i++) {
			// 转换成 16 进制字符的转换
			byte bt = data[i + offset]; // 取第 i 个字节
			hex[k++] = _hexDigits[(bt >> 4) & 0x0F]; // 取字节中高 4 位的数字转换
			hex[k++] = _hexDigits[bt & 0x0F]; // 取字节中低 4 位的数字转换
		}
		return new String(hex);
	}

	/**
	 * HEX解码
	 * 
	 * @param hex
	 *            HEX编码串
	 * @return 解码后的字节数组
	 */
	public static byte[] decode(String hex) {
		if (null == hex || 0 == hex.length()) {
			return null;
		}
		if (1 == (1 & hex.length())) {
			throw new IllegalArgumentException("HEX格式错误：" + StringUtil.limit(hex, 50));
		}
		byte[] data = new byte[hex.length() / 2];
		int k = 0;
		for (int i = 0; i < hex.length(); i++) {
			char ch = hex.charAt(i++);
			if (ch < 0 || ch >= _hexTable0_f.length || -1 == _hexTable0_f[ch]) {
				throw new IllegalArgumentException("HEX格式错误，在" + i + "位置");
			}
			int bit8 = _hexTable0_f[ch];
			ch = hex.charAt(i);
			if (ch < 0 || ch >= _hexTable0_f.length || -1 == _hexTable0_f[ch]) {
				throw new IllegalArgumentException("HEX格式错误，在" + i + "位置");
			}
			bit8 = (bit8 << 4) & 0xF0;
			bit8 |= (0x0F & _hexTable0_f[ch]);
			data[k++] = (byte) bit8;
		}
		return data;
	}

	/**
	 * HEX解码一个字节
	 * 
	 * @param hex
	 *            HEX字串
	 * @param offset
	 *            开始位置
	 * @return 解码后的字节
	 */
	public static byte decodeByte(CharSequence hex, int offset) {
		char ch = hex.charAt(offset++);
		if (ch < 0 || ch >= _hexTable0_f.length || -1 == _hexTable0_f[ch]) {
			throw new IllegalArgumentException("HEX格式错误，在" + offset + "位置");
		}
		int bit8 = _hexTable0_f[ch];
		ch = hex.charAt(offset);
		if (ch < 0 || ch >= _hexTable0_f.length || -1 == _hexTable0_f[ch]) {
			throw new IllegalArgumentException("HEX格式错误，在" + offset + "位置");
		}
		bit8 = (bit8 << 4) & 0xF0;
		bit8 |= (0x0F & _hexTable0_f[ch]);
		return (byte) bit8;
	}

	/**
	 * 32位整数HEX字串，不足8个字符前端补0
	 * 
	 * @param val
	 *            32位数字
	 * @param sb
	 *            字串缓冲区，若为null自动创建新的
	 * @return 8字符的HEX编码串
	 */
	public static StringBuilder toHexFixed(int val, StringBuilder sb) {
		if (null == sb) {
			sb = new StringBuilder(8);
		}
		if (val < 0 || val >= 0x10000000) {
			sb.append(_hexDigits[(val >> 28) & 0xF]);
			sb.append(_hexDigits[(val >> 24) & 0xF]);
			sb.append(_hexDigits[(val >> 20) & 0xF]);
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x01000000) {
			sb.append('0');
			sb.append(_hexDigits[(val >> 24) & 0xF]);
			sb.append(_hexDigits[(val >> 20) & 0xF]);
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00100000) {
			sb.append("00");
			sb.append(_hexDigits[(val >> 20) & 0xF]);
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00010000) {
			sb.append("000");
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00001000) {
			sb.append("0000");
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00000100) {
			sb.append("00000");
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00000010) {
			sb.append("000000");
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00000001) {
			sb.append("0000000");
			sb.append(_hexDigits[(val) & 0xF]);
		} else {
			sb.append("00000000");
			return sb;
		}
		return sb;
	}

	/**
	 * 24位整数HEX字串，不足6个字符前端补0
	 * 
	 * @param i24
	 *            24位数值
	 * @param builder
	 *            字串缓冲区
	 * @throws NumberFormatException
	 *             当输入的数值大于0x00FFFFFF时
	 */
	public static void toHexFixed24(int i24, StringBuilder builder) {
		if (i24 >= 0x01000000) {
			throw new NumberFormatException("i24(" + i24 + ")>0x00FFFFFF");
		}
		if (i24 >= 0x00100000) {
			builder.append(_hexDigits[(i24 >> 20) & 0xF]);
			builder.append(_hexDigits[(i24 >> 16) & 0xF]);
			builder.append(_hexDigits[(i24 >> 12) & 0xF]);
			builder.append(_hexDigits[(i24 >> 8) & 0xF]);
			builder.append(_hexDigits[(i24 >> 4) & 0xF]);
			builder.append(_hexDigits[(i24) & 0xF]);
		} else if (i24 >= 0x00010000) {
			builder.append("0");
			builder.append(_hexDigits[(i24 >> 16) & 0xF]);
			builder.append(_hexDigits[(i24 >> 12) & 0xF]);
			builder.append(_hexDigits[(i24 >> 8) & 0xF]);
			builder.append(_hexDigits[(i24 >> 4) & 0xF]);
			builder.append(_hexDigits[(i24) & 0xF]);
		} else if (i24 >= 0x00001000) {
			builder.append("00");
			builder.append(_hexDigits[(i24 >> 12) & 0xF]);
			builder.append(_hexDigits[(i24 >> 8) & 0xF]);
			builder.append(_hexDigits[(i24 >> 4) & 0xF]);
			builder.append(_hexDigits[(i24) & 0xF]);
		} else if (i24 >= 0x00000100) {
			builder.append("000");
			builder.append(_hexDigits[(i24 >> 8) & 0xF]);
			builder.append(_hexDigits[(i24 >> 4) & 0xF]);
			builder.append(_hexDigits[(i24) & 0xF]);
		} else if (i24 >= 0x00000010) {
			builder.append("0000");
			builder.append(_hexDigits[(i24 >> 4) & 0xF]);
			builder.append(_hexDigits[(i24) & 0xF]);
		} else if (i24 >= 0x00000001) {
			builder.append("00000");
			builder.append(_hexDigits[(i24) & 0xF]);
		} else {
			builder.append("000000");
		}
	}

	/**
	 * 64位整数HEX字串，不足16个字符前端补0
	 * 
	 * @param val
	 *            64位数值
	 * @param sb
	 *            字串缓冲区，若为null自动创建新的
	 * @return 16字符的HEX编码串
	 */
	public static StringBuilder toHexFixed(long val, StringBuilder sb) {
		if (null == sb) {
			sb = new StringBuilder(16);
		}
		// 高32位
		int i32 = (int) ((val >> 32) & 0xFFFFFFFF);
		toHexFixed(i32, sb);
		// 低32位
		i32 = (int) (val & 0xFFFFFFFF);
		toHexFixed(i32, sb);
		return sb;
	}

	/**
	 * 定长的16位整数HEX字串，不足4个字符前端补0
	 * 
	 * @param val
	 *            要转换的数值
	 * @param sb
	 *            转换输出字串缓冲区，若为null则自动内部创建
	 * @return 传入或内部创建的缓存区
	 */
	public static StringBuilder toHexFixed(short val, StringBuilder sb) {
		if (null == sb) {
			sb = new StringBuilder(4);
		}
		if (val < 0 || val >= 0x1000) {
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[val & 0x0F]);
		} else if (val >= 0x0100) {
			sb.append('0');
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[val & 0x0F]);
		} else if (val >= 0x0010) {
			sb.append("00");
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[val & 0x0F]);
		} else if (val >= 0x0001) {
			sb.append("000");
			sb.append(_hexDigits[val & 0x0F]);
		} else {
			sb.append("0000");
		}
		return sb;
	}

	/**
	 * 8位HEX字串，不足2个字符前端补0
	 * 
	 * @param val
	 *            字节
	 * @param sb
	 *            字串输出缓存区
	 * @return 输出hex字串后的sb
	 */
	public static StringBuilder toHexFixed(byte val, StringBuilder sb) {
		sb.append(_hexDigits[(val >> 4) & 0xF]);
		sb.append(_hexDigits[(val) & 0xF]);
		return sb;
	}

	/**
	 * 64位整数HEX字串，不足16个字符前端补0
	 * 
	 * @param val
	 *            整数
	 * @return hex格式串
	 */
	public static String toHex64(long val) {
		if (0 == val) {
			return "0000000000000000";
		}
		return toHexFixed(val, new StringBuilder(16)).toString();
	}

	/**
	 * 32位整数HEX字串，不足8个字符前端补0
	 * 
	 * @param val
	 *            整数
	 * @return hex格式串
	 */
	public static String toHex32(int val) {
		if (0 == val) {
			return "00000000";
		}
		return toHexFixed(val, new StringBuilder(8)).toString();
	}

	/**
	 * 16位整数HEX字串，不足4个字符前端补0
	 * 
	 * @param val
	 * @return
	 */
	public static String toHex16(short val) {
		if (0 == val) {
			return "0000";
		}
		return toHexFixed(val, new StringBuilder(4)).toString();
	}

	/**
	 * 转为 HEX字串
	 * 
	 * @param val
	 *            32位数值
	 * @param sb
	 *            转换HEX后的追加字串缓冲区
	 * @return 追加后的字串缓冲区
	 */
	public static StringBuilder toHex(int val, StringBuilder sb) {
		if (val < 0 || val >= 0x10000000) {
			sb.append(_hexDigits[(val >> 28) & 0xF]);
			sb.append(_hexDigits[(val >> 24) & 0xF]);
			sb.append(_hexDigits[(val >> 20) & 0xF]);
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x01000000) {
			sb.append(_hexDigits[(val >> 24) & 0xF]);
			sb.append(_hexDigits[(val >> 20) & 0xF]);
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00100000) {
			sb.append(_hexDigits[(val >> 20) & 0xF]);
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00010000) {
			sb.append(_hexDigits[(val >> 16) & 0xF]);
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00001000) {
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00000100) {
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00000010) {
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[(val) & 0xF]);
		} else if (val >= 0x00000001) {
			sb.append(_hexDigits[(val) & 0xF]);
		} else {
			sb.append("0");
			return sb;
		}
		return sb;
	}

	/**
	 * 转换为16进制格式的字串
	 * 
	 * @param val
	 *            32位数值
	 * @return 16进制格式串（如：a1f）
	 */
	public static String toHex(int val) {
		return toHex(val, new StringBuilder(8)).toString();
	}

	/**
	 * 转为 HEX字串
	 * 
	 * @param val
	 *            64位数值
	 * @param sb
	 *            转换HEX后的追加字串缓冲区
	 * @return 追加后的字串缓冲区
	 */
	public static StringBuilder toHex(long val, StringBuilder sb) {
		// 高32位
		int i32 = (int) ((val >> 32) & 0xFFFFFFFF);
		if (0 != i32) {
			toHex(i32, sb);
			// 低32位
			i32 = (int) (val & 0xFFFFFFFF);
			toHexFixed(i32, sb);
			return sb;
		}
		// 只有低32位
		i32 = (int) (val & 0xFFFFFFFF);
		toHex(i32, sb);
		return sb;
	}

	/**
	 * 转换为16进制格式的字串
	 * 
	 * @param val
	 *            64位数值
	 * @return 16进制格式串（如：a1f）
	 */
	public static String toHex(long val) {
		return toHex(val, new StringBuilder(16)).toString();
	}

	/**
	 * 定长的16位整数HEX字串，不足4个字符前端补0
	 * 
	 * @param val
	 *            要转换的数值
	 * @param sb
	 *            转换输出字串缓冲区，若为null则自动内部创建
	 * @return 传入或内部创建的缓存区
	 * @throws IOException
	 */
	public static Appendable toHexFixed(short val, Appendable sb) throws IOException {
		if (val < 0 || val >= 0x1000) {
			sb.append(_hexDigits[(val >> 12) & 0xF]);
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[val & 0x0F]);
		} else if (val >= 0x0100) {
			sb.append('0');
			sb.append(_hexDigits[(val >> 8) & 0xF]);
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[val & 0x0F]);
		} else if (val >= 0x0010) {
			sb.append("00");
			sb.append(_hexDigits[(val >> 4) & 0xF]);
			sb.append(_hexDigits[val & 0x0F]);
		} else if (val >= 0x0001) {
			sb.append("000");
			sb.append(_hexDigits[val & 0x0F]);
		} else {
			sb.append("0000");
		}
		return sb;
	}

	/**
	 * 转换HEX字串
	 * 
	 * @param hex
	 *            HEX字串
	 * @param offset
	 *            HEX字串开始位置（包含）
	 * @param end
	 *            HEX字串结束位置（不包含），-1则为全字串长
	 * @param invaild
	 *            若格式无效，返回此值
	 * @return 返回解析后的数值，若格式无效返回传入的invaild
	 */
	public static int parseHex(CharSequence hex, int offset, int end, int invaild) {
		if (end > hex.length()) {
			return invaild;
		}
		if (-1 == end) {
			end = hex.length();
		}
		if (0 == end || end - offset > 8) {
			return invaild;
		}

		// 检查字串是否有效，若包含无效部分，返回invaild
		int val = 0;
		for (int i = offset; i < end; i++) {
			char ch = hex.charAt(i);
			if (ch < 0 || ch >= _hexTable0_f.length || -1 == _hexTable0_f[ch]) {
				return invaild;
			}
			int bit4 = _hexTable0_f[ch];
			if (i > 0) {
				val <<= 4;
			}
			val |= bit4;
		}
		return val;
	}

	/**
	 * 转换HEX字串
	 * 
	 * @param hex
	 *            HEX字串
	 * @param len
	 *            HEX字串长度的部分，-1则为全字串长
	 * @param invaild
	 *            若格式无效，返回此值
	 * @return 返回解析后的数值，若格式无效返回传入的invaild
	 */
	public static int parseHex(CharSequence hex, int len, int invaild) {
		return parseHex(hex, 0, len, invaild);
	}

}
