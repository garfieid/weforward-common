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

import java.io.IOException;

import cn.weforward.common.util.StringBuilderPool;

/**
 * 输出为字串
 * 
 * @author liangyi
 *
 */
public class StringOutput implements JsonOutput {
	StringBuilder m_Builder;

	private void sureOpen() throws IOException {
		if (null == m_Builder) {
			throw new IOException("closed");
		}
	}

	public StringOutput() {
		// m_Builder = new StringBuilder();
		m_Builder = StringBuilderPool._8k.poll();
	}

	public StringOutput(int capacity) {
		m_Builder = new StringBuilder(capacity);
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		return append(csq, 0, csq.length());
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		sureOpen();
		return m_Builder.append(csq, start, end);
	}

	@Override
	public Appendable append(char c) throws IOException {
		sureOpen();
		return m_Builder.append(c);
	}

	@Override
	public void close() throws IOException {
		if (null != m_Builder
				&& StringBuilderPool._8k.getInitialCapacity() == m_Builder.capacity()) {
			StringBuilderPool._8k.offer(m_Builder);
		}
		m_Builder = null;
	}

	@Override
	public String toString() {
		if (null != m_Builder) {
			return m_Builder.toString();
		}
		return "";
	}
}