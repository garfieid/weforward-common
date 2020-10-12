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
import java.io.OutputStream;

import cn.weforward.common.util.SimpleUtf8Encoder;

/**
 * 封装OutputStream为JSON格式输出流（UTF-8）
 * 
 * @author liangyi
 *
 */
public class JsonOutputStream implements JsonOutput {
	static final int INVAILD_CHAR = Integer.MAX_VALUE;

	protected OutputStream m_Output;
	protected SimpleUtf8Encoder m_Encoder;
	// CharsetEncoder m_Encoder;

	public JsonOutputStream(OutputStream stream) {
		m_Output = stream;
		m_Encoder = new SimpleUtf8Encoder(m_Output);
		// Charset cs = Charset.forName(charset);
		// m_Encoder = cs.newEncoder();
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		// return append(csq, 0, csq.length());
		m_Encoder.encode(csq);
		return this;
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		// for (; start < end; start++) {
		// append(csq.charAt(start));
		// }
		m_Encoder.encode(csq, start, end);
		return this;
	}

	@Override
	public Appendable append(char ch) throws IOException {
		m_Encoder.encode(ch);
		return this;
	}

	@Override
	public void close() throws IOException {
		m_Output.close();
		m_Output = null;
	}

}
