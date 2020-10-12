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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import cn.weforward.common.util.Bytes;

/**
 * 扩展ByteArrayOutputStream用于直接获取到内部的byte[]
 * 
 * @author liangyi
 * 
 */
public class BytesOutputStream extends ByteArrayOutputStream implements OutputStreamNio {
	public BytesOutputStream() {
		super();
	}

	public BytesOutputStream(int size) {
		super(size);
	}

	private BytesOutputStream(CachedInputStream.AtBuffers buffers) {
		super(buffers.size());
		this.buf = buffers.toArray(this.buf, 0);
		this.count = buffers.size();
	}

	/**
	 * 由输入流创建
	 * 
	 * @param in
	 *            内容输入流
	 * @throws IOException
	 */
	public BytesOutputStream(InputStream in) throws IOException {
		this(new CachedInputStream.AtBuffers(in, 0));
		in.close();
	}

	/**
	 * 分离缓冲区内部数据
	 */
	synchronized public byte[] detach() {
		byte[] bs;
		if (this.count < this.buf.length) {
			bs = Arrays.copyOf(this.buf, this.count);
		} else {
			bs = this.buf;
			this.buf = _emptyBytes;
		}
		this.count = 0;
		return bs;
	}

	public Bytes getBytes() {
		if (0 == this.count) {
			return Bytes.empty();
		}
		return new Bytes(this.buf, 0, this.count);
	}

	/**
	 * 重置最后写入位置
	 * 
	 * @param length
	 *            位置
	 */
	synchronized public void setSize(int length) throws IOException {
		if (length > this.count || length >= this.buf.length) {
			throw new EOFException("length越界{" + this.count + ";" + this.buf.length + "}");
		}
		this.count = length;
	}

	@Override
	synchronized public int write(InputStream src) throws IOException {
		int len, ret;
		do {
			len = src.available();
			if (len < 2) {
				len = 512;
			}
			ensureWritableBytes(len);
			ret = src.read(this.buf, this.count, len);
			if (ret > 0) {
				this.count += ret;
			}
		} while (-1 != ret);
		return 0;
	}

	@Override
	synchronized public int write(ByteBuffer src) throws IOException {
		int len = src.remaining();
		ensureWritableBytes(len);
		src.get(this.buf, this.count, len);
		this.count += len;
		return len;
	}

	@Override
	public void cancel() throws IOException {
		reset();
	}

	/**
	 * 确保还有可写入的空间
	 * 
	 * @param minSize
	 *            至少可写空间（字节数）
	 */
	protected void ensureWritableBytes(int minSize) {
		if (minSize > this.buf.length - this.count) {
			int grow = this.buf.length << 1;
			if (grow < minSize + this.count) {
				grow = minSize + this.count;
			}
			byte[] copy = new byte[grow];
			System.arraycopy(this.buf, 0, copy, 0, this.count);
			this.buf = copy;
		}
	}

	/** 空节数组 */
	public static final byte[] _emptyBytes = new byte[0];

	/**
	 * 把输入流的内容传送到输出流，完成后不会关闭它们
	 * 
	 * @param in
	 *            输入流
	 * @param out
	 *            输出流
	 * @param limit
	 *            若>0则限制读取的字节数
	 * @return 传送的总字节数
	 * @throws IOException
	 */
	static public int transfer(InputStream in, OutputStream out, int limit) throws IOException {
		if (limit <= 0) {
			if (in instanceof InputStreamNio && out instanceof OutputStreamNio) {
				OutputStreamNio outNio = (OutputStreamNio) out;
				InputStreamNio inNio = (InputStreamNio) in;
				ByteBuffer buf = ByteBuffer.allocateDirect(512);
				int ret;
				int total = 0;
				for (;;) {
					buf.clear();
					ret = inNio.read(buf);
					if (ret < 0) {
						break;
					}
					buf.flip();
					if (ret != buf.remaining()) {
						throw new IOException("ret!=remaining ??");
					}
					outNio.write(buf);
					total += ret;
				}
				return total;
			}
			if (in instanceof FileInputStream && out instanceof WritableByteChannel) {
				// NIO transfer
				FileChannel channel = ((FileInputStream) in).getChannel();
				long size = channel.size();
				channel.transferTo(0, size, (WritableByteChannel) out);
				return (int) (Integer.MAX_VALUE & size);
			}
		}

		int total = 0;
		int len;
		byte[] buf;
		buf = Bytes.Pool._8k.poll();
		try {
			if (limit <= 0) {
				// 传送全部
				do {
					len = in.read(buf);
					if (len > 0) {
						out.write(buf, 0, len);
						total += len;
					}
				} while (len >= 0);
				return total;
			}

			// 传送限定的部分字节数
			do {
				len = limit - total;
				if (len > buf.length) {
					len = buf.length;
				}
				len = in.read(buf, 0, len);
				if (len > 0) {
					out.write(buf, 0, len);
					total += len;
				}
			} while (len >= 0 && total < limit);
			return total;
		} finally {
			Bytes.Pool._8k.offer(buf);
		}
	}

	/**
	 * 把ByteBuffer的内容传送到输出流，完成后不会关闭输出流
	 * 
	 * @param buffer
	 *            字节缓冲区的内容
	 * @param out
	 *            输出流
	 * @param limit
	 *            若>0则限制读取的字节数
	 * @return 传送的总字节数
	 * @return
	 * @throws IOException
	 */
	static public int transfer(ByteBuffer buffer, OutputStream out, int limit) throws IOException {
		int total = 0;
		if (out instanceof OutputStreamNio) {
			OutputStreamNio outNio = (OutputStreamNio) out;
			total = buffer.remaining();
			if (limit > 0 && limit < total) {
				// 控制读取量
				total = limit;
				// 保存limit用于写入后恢复
				limit = buffer.limit();
				buffer.limit(total + buffer.position());
				outNio.write(buffer);
				// 恢复limit
				buffer.limit(limit);
			} else {
				outNio.write(buffer);
			}
			return total;
		}

		int len;
		byte[] buf;
		buf = Bytes.Pool._8k.poll();
		try {
			if (limit <= 0) {
				// 传送全部
				int remain = buffer.remaining();
				while (remain > 0) {
					len = (buf.length > remain) ? remain : buf.length;
					buffer.get(buf, 0, len);
					out.write(buf, 0, len);
					total += len;
					remain = buffer.remaining();
				}
				return total;
			}

			// 传送限定的部分字节数
			int remain = buffer.remaining();
			while (remain > 0 && limit < total) {
				len = limit - total;
				if (len > buf.length) {
					len = buf.length;
				}
				if (len > remain) {
					len = remain;
				}
				buffer.get(buf, 0, len);
				out.write(buf, 0, len);
				total += len;
				remain = buffer.remaining();
			}
			return total;
		} finally {
			Bytes.Pool._8k.offer(buf);
		}
	}
}
