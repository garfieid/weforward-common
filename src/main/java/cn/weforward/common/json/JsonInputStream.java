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
package cn.weforward.common.json;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

import cn.weforward.common.io.InputStreamNio;

/**
 * 封装UTF-8字节流的InputStream为JsonInput
 * 
 * @author liangyi
 *
 */
public class JsonInputStream implements JsonInput {
	protected InputStream m_Source;
	protected CharsetDecoder m_Decoder;
	protected ByteBuffer m_InBuffer;
	protected CharBuffer m_OutBuffer;
	protected int m_Position;

	public JsonInputStream(InputStream source) {
		this(source, "UTF-8");
	}

	public JsonInputStream(InputStream source, String charset) {
		m_Source = source;
		Charset cs = Charset.forName(charset);
		m_Decoder = cs.newDecoder();
	}

	protected boolean decode() throws IOException {
		if (null == m_Source) {
			throw new IOException("closed.");
		}
		if (null == m_OutBuffer) {
			m_InBuffer = ByteBuffer.allocate(3 * 8 * (int) m_Decoder.averageCharsPerByte());
			m_InBuffer.rewind();
			m_InBuffer.limit(0);
			m_OutBuffer = CharBuffer.allocate(m_InBuffer.capacity());
			m_OutBuffer.rewind();
			m_OutBuffer.limit(0);
		} else if (m_OutBuffer.hasRemaining()) {
			// 缓冲区还有解码后的数据
			return true;
		}
		// 由源流读取输入数据
		int pos;
		if (0 == m_InBuffer.remaining()) {
			m_InBuffer.clear();
			pos = 0;
		} else if (m_InBuffer.limit() == m_InBuffer.capacity()) {
			// 要把数据移动到前端:(
			byte[] bytes = m_InBuffer.array();
			pos = m_InBuffer.remaining();
			System.arraycopy(bytes, m_InBuffer.arrayOffset() + m_InBuffer.position(), bytes, 0, pos);
			m_InBuffer.position(pos);
			m_InBuffer.limit(m_InBuffer.capacity());
			pos = 0;
		} else {
			pos = m_InBuffer.position();
			m_InBuffer.position(m_InBuffer.limit());
			m_InBuffer.limit(m_InBuffer.capacity());
		}
		if (m_Source instanceof InputStreamNio) {
			if (((InputStreamNio) m_Source).read(m_InBuffer) < 0) {
				return false;
			}
			m_InBuffer.flip();
		} else {
			byte[] bytes = m_InBuffer.array();
			int len = m_Source.read(bytes, m_InBuffer.arrayOffset() + m_InBuffer.position(), m_InBuffer.remaining());
			if (len < 0) {
				// // 重置解码输出缓冲区
				// m_OutBuffer.clear();
				// CoderResult cr = m_Decoder.flush(m_OutBuffer);
				// if (!cr.isUnderflow()) {
				// cr.throwException();
				// }
				// m_OutBuffer.flip();
				// if (m_OutBuffer.remaining() > 0) {
				// return true;
				// }
				return false;
			}
			m_InBuffer.limit(len + m_InBuffer.position());
		}
		m_InBuffer.position(pos);
		// 重置解码输出缓冲区
		m_OutBuffer.clear();
		// 解码
		CoderResult cr = m_Decoder.decode(m_InBuffer, m_OutBuffer, false);
		if (!cr.isUnderflow()) {
			cr.throwException();
		}
		m_OutBuffer.flip();
		return true;
	}

	@Override
	public int available() throws IOException {
		if (null != m_OutBuffer && m_OutBuffer.remaining() > 0) {
			return m_OutBuffer.remaining();
		}
		return m_Source.available();
	}

	@Override
	public char readChar() throws IOException {
		if (!decode()) {
			throw new EOFException();
		}
		char ch = m_OutBuffer.get();
		if (StandardCharsets.UTF_8 == m_Decoder.charset()) {
			// utf16转utf8的字节数计算，参考：https://zh.m.wikipedia.org/zh-hans/UTF-16；https://zh.m.wikipedia.org/zh-hans/UTF-8
			if (ch >= 0 && ch <= 0x7F) {
				m_Position += 1;
			} else if (ch >= 0x80 && ch <= 0x7FF) {
				m_Position += 2;
			} else if (ch >= 0x800 && ch <= 0xD7FF) {
				m_Position += 3;
			} else if (ch >= 0xD800 && ch <= 0xDFFF) {
				// Unicode辅助平面（U+10000到U+10FFFF），utf16在java中为双字符（4字节），utf8为4字节
				m_Position += 2;
			} else {
				m_Position += 3;
			}
		} else {
			// FIXME 如果字符不在（U+0000到U+007F）范围内就不准
			++m_Position;
		}
		return ch;
	}

	@Override
	public int position() {
		return m_Position;
	}

	@Override
	public void close() throws IOException {
		m_Decoder = null;
		m_InBuffer = null;
		m_OutBuffer = null;
		m_Source.close();
		m_Source = null;
	}
}
