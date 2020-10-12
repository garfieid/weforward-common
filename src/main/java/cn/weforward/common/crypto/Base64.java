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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import cn.weforward.common.execption.InvalidFormatException;
import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.StringBuilderPool;

/**
 * BASE64
 * 
 * @author liangyi
 * 
 */
public class Base64 {
	// static private final int BASE_LENGTH = 128;
	// static private final int LOOKUP_LENGTH = 64;
	// static private final int FOUR_BYTE = 4;

	static private final int BASE_BIT = 6;
	static private final int EIGHT_BIT = 8;
	static private final int SIXTEEN_BIT = 16;
	static private final int TWENTY_FOUR_BIT = 24;
	static private final int EIGHT_MASK = 0xFF;

	static private final int SIGN = -128;
	static private final char PAD = '=';

	static final private int[] _Base64DecodeTable = new int[EIGHT_MASK];
	static final private char[] _Base64EncodeTable = { //
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', // 0
			'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', // 1
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', // 2
			'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', // 3
			'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', // 4
			'o', 'p', 'q', 'r', 's', 't', 'u', 'v', // 5
			'w', 'x', 'y', 'z', '0', '1', '2', '3', // 6
			'4', '5', '6', '7', '8', '9', '+', '/' // 7
	};

	/**
	 * 初始化反编码表
	 */
	static {
		for (int i = 0; i < _Base64DecodeTable.length; i++) {
			_Base64DecodeTable[i] = -2; // Illegal digit
		}
		for (int i = 0; i < _Base64EncodeTable.length; i++) {
			_Base64DecodeTable[_Base64EncodeTable[i]] = i;
		}
		_Base64DecodeTable[PAD] = -1;
	}

	/**
	 * BASE64编码
	 * 
	 * @param bytes 要编码的数据（字节数组）
	 * @return BASE64编码串
	 */
	public static String encode(byte[] bytes) {
		return encode(bytes, 0, bytes.length);
	}

	/**
	 * BASE64编码
	 * 
	 * @param bytes  要编码的数据（字节数组）
	 * @param offset 开始位置
	 * @param len    长度
	 * @return BASE64编码串
	 */
	public static String encode(byte[] bytes, int offset, int len) {
		StringBuilder builder = StringBuilderPool._8k.poll();
		try {
			return encode(builder, bytes, offset, len).toString();
		} catch (IOException e) {
			// 略过
			return null;
		} finally {
			StringBuilderPool._8k.offer(builder);
		}
	}

	/**
	 * BASE64编码
	 * 
	 * @param appender 编码结果输出
	 * @param bytes    要编码的数据（字节数组）
	 * @param offset   开始位置
	 * @param len      长度
	 * @return appender
	 * @throws IOException
	 */
	public static Appendable encode(Appendable appender, byte[] bytes, int offset, int len) throws IOException {
		if (null == bytes) {
			return null;
		}
		if (offset < 0 || len < 0 || offset + len > bytes.length) {
			throw new IllegalArgumentException("offset[" + offset + "]+len[" + len + "] over{0~" + bytes.length + "}");
		}
		if (0 == len) {
			return appender;
		}

		int dataBits = len * EIGHT_BIT; // 数据的总共位数
		int groups = dataBits / TWENTY_FOUR_BIT; // 按24位（6与8最小公倍数）后的分组数
		int fewer = dataBits % TWENTY_FOUR_BIT; // 分组后剩余位数
		// int count = 4 * ((fewer != 0) ? (groups + 1) : groups); // 编码后产生的字串长度
		// char result[] = new char[count];

		int bytesIdx = offset;
		byte k = 0, l = 0, b1 = 0, b2 = 0, b3 = 0;
		for (int i = 0; i < groups; i++) {
			b1 = bytes[bytesIdx++];
			b2 = bytes[bytesIdx++];
			b3 = bytes[bytesIdx++];

			l = (byte) (b2 & 0x0f);
			k = (byte) (b1 & 0x03);

			byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
			byte val2 = ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);
			byte val3 = ((b3 & SIGN) == 0) ? (byte) (b3 >> 6) : (byte) ((b3) >> 6 ^ 0xfc);

			appender.append(_Base64EncodeTable[val1]);
			appender.append(_Base64EncodeTable[val2 | (k << 4)]);
			appender.append(_Base64EncodeTable[(l << 2) | val3]);
			appender.append(_Base64EncodeTable[b3 & 0x3f]);
		}

		// form integral number of 6-bit groups
		if (fewer == EIGHT_BIT) {
			b1 = bytes[bytesIdx];
			k = (byte) (b1 & 0x03);
			byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
			appender.append(_Base64EncodeTable[val1]);
			appender.append(_Base64EncodeTable[k << 4]);
			appender.append(PAD);
			appender.append(PAD);
		} else if (fewer == SIXTEEN_BIT) {
			b1 = bytes[bytesIdx];
			b2 = bytes[bytesIdx + 1];
			l = (byte) (b2 & 0x0f);
			k = (byte) (b1 & 0x03);

			byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2) : (byte) ((b1) >> 2 ^ 0xc0);
			byte val2 = ((b2 & SIGN) == 0) ? (byte) (b2 >> 4) : (byte) ((b2) >> 4 ^ 0xf0);

			appender.append(_Base64EncodeTable[val1]);
			appender.append(_Base64EncodeTable[val2 | (k << 4)]);
			appender.append(_Base64EncodeTable[l << 2]);
			appender.append(PAD);
		}
		return appender;
	}

	/**
	 * BASE64输入（字节流、字串流...）
	 * 
	 * @author liangyi
	 *
	 */
	public static interface Base64Input {
		/**
		 * 可读取量
		 * 
		 * @return 可读取的长度，0表示结束
		 * 
		 * @throws IOException
		 */
		int available() throws IOException;

		/**
		 * 读取一个字节/字符
		 * 
		 * @return 返回0表示已读取完
		 * @throws IOException
		 */
		byte read() throws IOException;
	}

	public static class WrapString implements Base64Input {
		protected final String m_Base64;
		protected int m_Pos;

		public WrapString(String base64) {
			m_Base64 = base64;
		}

		@Override
		public int available() throws IOException {
			return m_Base64.length() - m_Pos;
		}

		@Override
		public byte read() throws IOException {
			if (m_Base64.length() == m_Pos) {
				m_Pos++;
				return 0;
			}
			if (m_Pos > m_Base64.length()) {
				throw new EOFException("无更多字符内容[" + m_Pos + "]");
			}
			return (byte) m_Base64.charAt(m_Pos++);
		}

		@Override
		public String toString() {
			return m_Base64;
		}
	}

	public static class WrapInputStream implements Base64Input {
		protected InputStream m_Base64;

		public WrapInputStream(InputStream base64) {
			m_Base64 = base64;
		}

		@Override
		public int available() throws IOException {
			if (null == m_Base64) {
				return 0;
			}
			return m_Base64.available();
		}

		@Override
		public byte read() throws IOException {
			if (null == m_Base64) {
				// return 0;
				throw new EOFException("已读取完关闭");
			}
			int ret = m_Base64.read();
			if (ret < 0) {
				m_Base64.close();
				m_Base64 = null;
				return 0;
			}
			return (byte) ret;
		}

		@Override
		protected void finalize() throws Throwable {
			if (null != m_Base64) {
				m_Base64.close();
				m_Base64 = null;
			}
		}
	}

	public static class WrapBytes implements Base64Input {
		final byte[] m_Base64;
		int m_Offset;
		final int m_Len;

		public WrapBytes(byte[] base64, int offset, int len) {
			m_Base64 = base64;
			m_Offset = offset;
			m_Len = len + m_Offset;
		}

		@Override
		public int available() throws IOException {
			return m_Len - m_Offset;
		}

		@Override
		public byte read() throws IOException {
			if (m_Offset == m_Len) {
				m_Offset++;
				return 0;
			}
			if (m_Offset > m_Len) {
				throw new EOFException("无更多内容[" + m_Offset + "]");
			}
			return m_Base64[m_Offset++];
		}
	}

	/**
	 * 由字节数组解码
	 * 
	 * @param base64 字节数组的BASE64编码串
	 * @return 解码后的内容
	 * @throws IOException
	 */
	public static byte[] decode(byte[] base64) throws IOException {
		if (null == base64) {
			return null;
		}
		return decode(base64, 0, base64.length);
	}

	/**
	 * 由字节数组解码
	 * 
	 * @param base64 字节数组的BASE64编码串
	 * @param offset 开始位置
	 * @param len    长度
	 * @return 解码后的内容
	 * @throws IOException
	 */
	public static byte[] decode(byte[] base64, int offset, int len) throws IOException {
		if (null == base64) {
			return null;
		}
		return decode(new WrapBytes(base64, offset, len)).getBytes();
	}

	// public static Base64Input wrap(String base64) {
	// if (Misc.isEmpty(base64)) {
	// return null;
	// }
	// return new WrapString(base64);
	// }

	/**
	 * 由字节输入流解码
	 * 
	 * @param base64 输入流
	 * @return 解码后的内容
	 * @throws IOException
	 */
	public static byte[] decode(InputStream base64) throws IOException {
		if (null == base64) {
			return null;
		}
		return decode(new WrapInputStream(base64)).getBytes();
	}

	/**
	 * BASE64解码
	 * 
	 * @param base64 BASE64编码串
	 * @return 解码后的数据（字节数组）
	 */
	public static Bytes decode(Base64Input base64) throws IOException {
		if (base64 == null) {
			return Bytes.empty();
		}
		BytesOutputStream result;
		{
			int size = base64.available();
			if (size < 1) {
				return Bytes.empty();
			}
			size = (size * 3) / 4;
			result = new BytesOutputStream(size < 16 ? 16 : size);
		}
		// byte[] result = new byte[size];

		int bits = 0; // 解码得到的位数据（不应该超过16位）
		int remaining = 0; // 已解码的数据位数
		// int nNumBits = 6;
		int digit;
		// int resultIdx = 0;
		int pos = 0;
		for (;; pos++) {
			byte ch = base64.read();
			if (0 == ch) {
				break;
			}
			// 忽略空格、换行，制表符
			if (' ' == ch || '\t' == ch || '\r' == ch || '\n' == ch) {
				continue;
			}

			digit = _Base64DecodeTable[ch & 0x7F];
			if (digit < -1) {
				result.close();
				throw new InvalidFormatException(base64.toString(), pos - 1);
			}
			if (-1 == digit) {
				// PAD
				break;
			}

			// int nBits = digit & 0x3F;
			digit &= 0x3F;
			bits = (bits << BASE_BIT) | digit;
			remaining += BASE_BIT;
			if (remaining >= EIGHT_BIT) {
				// 有完整的8位啦，转换
				remaining -= EIGHT_BIT;
				int scratch = bits >> (remaining);
				// result[resultIdx++] = (byte) (scratch & 0xFF);
				result.write((scratch & 0xFF));
			}
		}
		result.close();
		if (remaining > 0) {
			// 碰到PAD或异常的结束，检查是否合法
			bits &= ((1 << remaining) - 1);
			if (2 != remaining && 4 != remaining && 0 != bits) {
				throw new InvalidFormatException(base64.toString(), pos - 1);
			}
		}
		return result.getBytes();
	}

	/**
	 * BASE64解码
	 * 
	 * @param base64 BASE64编码串
	 * @return 解码后的数据（字节数组）
	 */
	public static byte[] decode(String base64) {
		if (base64 == null || base64.length() == 0) {
			return null;
		}

		// int len = base64.length();
		// int size = (len * 3) / 4;
		// byte[] result = new byte[size];
		//
		// int bits = 0; // 解码得到的位数据（不应该超过16位）
		// int remaining = 0; // 已解码的数据位数
		// // int nNumBits = 6;
		// int digit;
		// int resultIdx = 0;
		// int pos = 0;
		// for (; pos < len; pos++) {
		// char ch = base64.charAt(pos);
		// // 忽略空格、换行，制表符
		// if (' ' == ch || '\t' == ch || '\r' == ch || '\n' == ch) {
		// continue;
		// }
		//
		// digit = _Base64DecodeTable[ch & 0x7F];
		// if (digit < -1) {
		// throw new InvalidFormatException(base64, pos);
		// }
		// if (-1 == digit) {
		// // PAD
		// break;
		// }
		//
		// // int nBits = digit & 0x3F;
		// digit &= 0x3F;
		// bits = (bits << BASE_BIT) | digit;
		// remaining += BASE_BIT;
		// if (remaining >= EIGHT_BIT) {
		// // 有完整的8位啦，转换
		// remaining -= EIGHT_BIT;
		// int scratch = bits >> (remaining);
		// result[resultIdx++] = (byte) (scratch & 0xFF);
		// }
		// }
		// if (remaining > 0) {
		// // 碰到PAD或异常的结束，检查是否合法
		// bits &= ((1 << remaining) - 1);
		// if (2 != remaining && 4 != remaining && 0 != bits) {
		// throw new InvalidFormatException(base64, pos);
		// }
		// }
		//
		// if (result.length > resultIdx) {
		// // 有得多，需要修剪
		// result = Arrays.copyOf(result, resultIdx);
		// // } else {
		// // // result的数据刚刚合适
		// // return result;
		// }
		// return result;

		Bytes bytes;
		try {
			bytes = decode(new WrapString(base64));
		} catch (IOException e) {
			throw new InvalidFormatException(base64);
		}
		if (bytes.getFree() > 0) {
			return Arrays.copyOfRange(bytes.getBytes(), bytes.getOffset(), bytes.getSize());
		}
		return bytes.getBytes();
	}

}
