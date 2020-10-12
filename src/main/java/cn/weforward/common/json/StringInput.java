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

import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;

/**
 * 字串输入
 * 
 * @author liangyi
 *
 */
public class StringInput implements JsonInput {
	String m_String;
	int m_Position;
	int m_End;

	public StringInput(String string) {
		m_String = string;
		m_Position = 0;
		m_End = string.length();
	}

	public StringInput(String str, int pos, int length) {
		m_End = length + pos;
		if (m_End > str.length()) {
			throw new IndexOutOfBoundsException("[" + length + "+" + pos + ">" + str.length() + "]"
					+ StringUtil.limit(str, 100));
		}
		m_String = str;
		m_Position = pos;
	}

	public String getString() {
		return m_String;
	}

	@Override
	public int available() {
		if (m_End <= m_Position) {
			return -1;
		}
		return m_End - m_Position;
	}

	@Override
	public char readChar() throws IOException {
		if (m_Position >= m_End) {
			throw new EOFException();
		}
		return m_String.charAt(m_Position++);
	}

	@Override
	public int position() {
		return m_Position;
	}

	@Override
	public void close() throws IOException {
		m_String = "";
	}

	public String toString() {
		if (null == m_String || 0 == m_End) {
			return "closed";
		}
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append('[').append(m_Position).append('/').append(m_End).append(']');
			int remain = m_End - m_Position;
			if (remain > 50) {
				remain = 50;
				builder.append(m_String, m_Position, m_Position + remain).append("...");
			} else {
				builder.append(m_String, m_Position, m_End);
			}
			return builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}
}