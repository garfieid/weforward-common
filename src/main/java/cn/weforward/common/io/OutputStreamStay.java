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

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * 暂留在缓存中的输出流，主要用于支撑在数据完成后再flush的场景
 * 
 * @author liangyi
 *
 */
public interface OutputStreamStay extends Flushable {
	/**
	 * 启动暂留
	 * 
	 * @throws StayException
	 */
	void stay() throws StayException;

	/**
	 * 把OutputStream封装为支持在缓冲暂留
	 * 
	 * @author liangyi
	 *
	 */
	public static class Wrap extends OutputStream implements OutputStreamStay {
		protected OutputStream m_Out;
		protected byte m_Buffer[];
		protected int m_Count;

		/**
		 * 封装为支持在缓冲暂留
		 * 
		 * @param out
		 *            输出流
		 * @return 支持OutputStreamStay的流
		 */
		static public OutputStream wrap(OutputStream out) {
			if (out instanceof OutputStreamStay) {
				return out;
			}
			return new Wrap(out);
		}

		public Wrap(OutputStream out) {
			m_Out = out;
		}

		private void ensureCapacity(int minCapacity) {
			int oldCapacity = 0;
			if (null != m_Buffer) {
				oldCapacity = m_Buffer.length;
			}
			if (minCapacity > oldCapacity) {
				int newCapacity = (oldCapacity > 8192) ? 4096 : (oldCapacity * 2);
				if (newCapacity < minCapacity) {
					newCapacity = minCapacity;
				}
				if (null == m_Buffer) {
					m_Buffer = new byte[newCapacity];
				} else {
					m_Buffer = Arrays.copyOf(m_Buffer, newCapacity);
				}
			}
		}

		private boolean isFlushed() {
			return m_Count < 0;
		}

		protected void flushBuffer() throws IOException {
			if (m_Count > 0) {
				m_Out.write(m_Buffer, 0, m_Count);
			}
			m_Count = -1;
		}

		@Override
		public void stay() throws StayException {
			if (isFlushed()) {
				throw new StayException("已刷写过");
			}
		}

		@Override
		public void write(int b) throws IOException {
			if (isFlushed()) {
				write(b);
				return;
			}
			ensureCapacity(m_Count + 1);
			m_Buffer[m_Count] = (byte) b;
			m_Count += 1;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (isFlushed()) {
				write(b, off, len);
				return;
			}
			if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) - b.length > 0)) {
				throw new IndexOutOfBoundsException();
			}
			ensureCapacity(m_Count + len);
			System.arraycopy(b, off, m_Buffer, m_Count, len);
			m_Count += len;
		}

		@Override
		public void flush() throws IOException {
			flushBuffer();
			super.flush();
		}

		@Override
		public void close() throws IOException {
			flushBuffer();
			m_Out.close();
		}
	}
}
