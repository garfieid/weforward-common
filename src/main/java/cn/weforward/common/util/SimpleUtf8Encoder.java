package cn.weforward.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import cn.weforward.common.crypto.Hex;

/**
 * 简单的UTF-8编码器。<br/>
 * 根据编码表，直接将UTF-16转为UTF-8，参考：<a href=
 * "https://zh.wikipedia.org/wiki/UTF-16">UTF-16</a>，<a href=
 * "https://zh.wikipedia.org/wiki/UTF-8">UTF-8</a>，<a href=
 * "https://zh.wikipedia.org/wiki/Unicode">Unicode</a>
 * 
 * @author zhangpengji
 *
 */
public class SimpleUtf8Encoder {
	static final int INVAILD_CHAR = Integer.MAX_VALUE;

	final OutputStream m_Output;
	// UTF-16对Unicode辅助平面的代理对的高位码元
	int m_HighSurrogateChar;

	public SimpleUtf8Encoder(OutputStream output) {
		m_Output = output;
		m_HighSurrogateChar = INVAILD_CHAR;
	}

	public void encode(CharSequence csq) throws IOException {
		encode(csq, 0, csq.length());
	}

	public void encode(CharSequence csq, int start, int end) throws IOException {
		byte[] buf;
		buf = Bytes.Pool._1k.poll();
		int bufPos = 0;
		int hs = m_HighSurrogateChar;
		try {
			for (; start < end; start++) {
				char ch = csq.charAt(start);
				if (ch < 0 || ch > 0xFFFF) {
					// 这是什么字符？:(
					throw new UnsupportedEncodingException("不在字符集内\\X" + Hex.toHex16((short) ch));
				}
				/*
				 UTF-8编码规则：
				 1字节 0x00000000 - 0x0000007F 0xxxxxxx   ANSI码（英文,数字符号）
				 2字节 0x00000080 - 0x000007FF 110xxxxx 10xxxxxx 
				 3字节  0x00000800 - 0x0000FFFF 1110xxxx 10xxxxxx 10xxxxxx   中日韩文等编码
				 4字节  0x00010000 - 0x001FFFFF 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				 5字节  0x00200000 - 0x03FFFFFF 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
				 6字节   1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
				 */
				if (ch <= 0x7F) {
					// ANSI码
					buf[bufPos++] = (byte) ch;
				} else {
					int utf16 = ch; // (ch < 0) ? (0x10000 + ch) : ch;
					if (utf16 >= 0x80 && utf16 <= 0x07FF) {
						// 2字节
						buf[bufPos++] = (byte) (0xC0 | (0x1F & (utf16 >> 6)));
						buf[bufPos++] = (byte) (0x80 | (0x3F & utf16));
					} else if (utf16 <= 0xD7FF || utf16 >= 0xE000) {
						// 3字节
						buf[bufPos++] = (byte) (0xE0 | (0x0F & (utf16 >> 12)));
						buf[bufPos++] = (byte) (0x80 | (0x3F & (utf16 >> 6)));
						buf[bufPos++] = (byte) (0x80 | (0x3F & utf16));
					} else {
						// 4字节
						if (INVAILD_CHAR == hs) {
							if (ch >= 0xDC00) {
								throw new UnsupportedEncodingException(
										"辅助平面的代理对未按顺序出现(high)：" + Hex.toHex16((short) ch));
							}
							// 记录高位码元，等下一个低位码元
							hs = ch;
						} else {
							if (ch < 0xDC00) {
								throw new UnsupportedEncodingException(
										"辅助平面的代理对未按顺序出现(low)：" + Hex.toHex16((short) ch));
							}
							/*
							 * UTF16对U+10000到U+10FFFF的处理过程，以U+10437编码（𐐷）为例:
							 * 0x10437 减去 0x10000，结果为0x00437，二进制为 0000 0000 0100 0011 0111
							 * 分割它的上10位值和下10位值（使用二进制）：0000 0000 01 和 00 0011 0111
							 * 添加 0xD800 到上值，以形成高位：0xD800 + 0x0001 = 0xD801
							 * 添加 0xDC00 到下值，以形成低位：0xDC00 + 0x0037 = 0xDC37
							 */
							// 先把UTF16转回Unicode
							int u32 = ((0x3FF & ((~0xD800) & hs)) << 10);
							u32 |= (0x3FF & ((~0xDC00) & ch));
							u32 += 0x10000;
							// 再把Unicode转UTF8
							buf[bufPos++] = (byte) (0xF0 | (0x7 & (u32 >> 18)));
							buf[bufPos++] = (byte) (0x80 | (0x3F & (u32 >> 12)));
							buf[bufPos++] = (byte) (0x80 | (0x3F & (u32 >> 6)));
							buf[bufPos++] = (byte) (0x80 | (0x3F & u32));
							hs = INVAILD_CHAR;
						}
					}
				}
				if (bufPos + 4 > buf.length) {
					m_Output.write(buf, 0, bufPos);
					bufPos = 0;
				}
			}
			m_HighSurrogateChar = hs;
			if (bufPos > 0) {
				m_Output.write(buf, 0, bufPos);
			}
		} finally {
			Bytes.Pool._1k.offer(buf);
		}
	}

	public void encode(char ch) throws IOException {
		if (ch < 0 || ch > 0xFFFF) {
			// 这是什么字符？:(
			throw new UnsupportedEncodingException("不在字符集内\\X" + Hex.toHex16((short) ch));
		}
		/*
		 UTF-8编码规则：
		 1字节 0x00000000 - 0x0000007F 0xxxxxxx   ANSI码（英文,数字符号）
		 2字节 0x00000080 - 0x000007FF 110xxxxx 10xxxxxx 
		 3字节  0x00000800 - 0x0000FFFF 1110xxxx 10xxxxxx 10xxxxxx   中日韩文等编码
		 4字节  0x00010000 - 0x001FFFFF 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
		 5字节  0x00200000 - 0x03FFFFFF 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
		 6字节   1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
		 */
		if (ch <= 0x7F) {
			// ANSI码
			m_Output.write((byte) ch);
		} else {
			int utf16 = ch; // (ch < 0) ? (0x10000 + ch) : ch;
			if (utf16 >= 0x80 && utf16 <= 0x07FF) {
				// 2字节
				m_Output.write((byte) (0xC0 | (0x1F & (utf16 >> 6))));
				m_Output.write((byte) (0x80 | (0x3F & utf16)));
			} else if (utf16 <= 0xD7FF || utf16 >= 0xE000) {
				// 3字节
				m_Output.write((byte) (0xE0 | (0x0F & (utf16 >> 12))));
				m_Output.write((byte) (0x80 | (0x3F & (utf16 >> 6))));
				m_Output.write((byte) (0x80 | (0x3F & utf16)));
			} else {
				// 4字节
				if (INVAILD_CHAR == m_HighSurrogateChar) {
					if (ch >= 0xDC00) {
						throw new UnsupportedEncodingException("辅助平面的代理对未按顺序出现(high)：" + Hex.toHex16((short) ch));
					}
					// 记录高位码元，等下一个低位码元
					m_HighSurrogateChar = ch;
				} else {
					if (ch < 0xDC00) {
						throw new UnsupportedEncodingException("辅助平面的代理对未按顺序出现(low)：" + Hex.toHex16((short) ch));
					}
					/*
					 * UTF16对U+10000到U+10FFFF的处理过程，以U+10437编码（𐐷）为例:
					 * 0x10437 减去 0x10000，结果为0x00437，二进制为 0000 0000 0100 0011 0111
					 * 分割它的上10位值和下10位值（使用二进制）：0000 0000 01 和 00 0011 0111
					 * 添加 0xD800 到上值，以形成高位：0xD800 + 0x0001 = 0xD801
					 * 添加 0xDC00 到下值，以形成低位：0xDC00 + 0x0037 = 0xDC37
					 */
					// 先把UTF16转回Unicode
					int u32 = ((0x3FF & ((~0xD800) & m_HighSurrogateChar)) << 10);
					u32 |= (0x3FF & ((~0xDC00) & ch));
					u32 += 0x10000;
					// 再把Unicode转UTF8
					m_Output.write((byte) (0xF0 | (0x7 & (u32 >> 18))));
					m_Output.write((byte) (0x80 | (0x3F & (u32 >> 12))));
					m_Output.write((byte) (0x80 | (0x3F & (u32 >> 6))));
					m_Output.write((byte) (0x80 | (0x3F & u32)));
					m_HighSurrogateChar = INVAILD_CHAR;
				}
			}
		}
	}
}
