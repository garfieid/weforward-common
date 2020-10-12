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
package cn.weforward.common.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 扩展ByteArrayInputStream，实现InputStreamNio接口
 * 
 * @author zhangpengji
 *
 */
public class BytesInputStream extends ByteArrayInputStream implements InputStreamNio {

	public BytesInputStream(byte[] buf) {
		super(buf);
	}

	public BytesInputStream(byte[] buf, int offset, int length) {
		super(buf, offset, length);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int count = 0;
		while (dst.hasRemaining()) {
			dst.put((byte) read());
			count++;
		}
		return count;
	}

	@Override
	public InputStreamNio duplicate() throws IOException {
		return new BytesInputStream(buf, pos, count - pos);
	}

	/**
	 * 无内容流
	 */
	static public InputStream empty() {
		return _empty;
	}

	static private InputStream _empty = new InputStream() {
		@Override
		public int read() throws IOException {
			return -1;
		}
	};
}
