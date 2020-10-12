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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cn.weforward.common.execption.OverflowException;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.StringUtil;

/**
 * 缓存输入流支持mark/reset对流重读取
 * 
 * @author liangyi
 * 
 */
public class CachedInputStream extends InputStream {
	/** 源输入流 */
	protected InputStream m_Origin;
	/** 缓存中的数据量（字节数） */
	protected int m_Count;
	/** 在缓存中读取到的位置 */
	protected int m_Pos;
	/** mark */
	protected int mark;
	/** 缓存 */
	protected byte[] m_Cache;

	/**
	 * 若输入流不支持markSupported，缓存其支持mark/reset对流重读取
	 * 
	 * @param in
	 *            源输入流
	 * @return 源输入流或有必要时缓存包装后的输入流
	 */
	public static InputStream cached(InputStream in) {
		if (in.markSupported()) {
			// in本身就支持了
			return in;
		}
		return new CachedInputStream(in);
	}

	public CachedInputStream(InputStream in) {
		m_Origin = in;
	}

	// protected CachedInputStream(InputStream in, byte[] cache, int count) {
	// m_Origin = in;
	// m_Cache = cache;
	// m_Count = count;
	// }

	/**
	 * 扩展缓存容量
	 * 
	 * @param size
	 *            期望扩展的大小
	 * @return 实际扩展的大小
	 */
	private int expand(int size) {
		int c;
		if (null == m_Cache) {
			// c = len < 1024 ? 1024 : len;
			c = size;
			m_Cache = new byte[c];
		} else {
			c = m_Cache.length - m_Count;
			if (c <= 0) {
				// 扩展缓存
				// c = len < 1024 ? 1024 : len;
				c = size;
				m_Cache = Arrays.copyOf(m_Cache, c + m_Count);
			}
		}
		return c;
	}

	/**
	 * 扩展足够的缓存容量
	 * 
	 * @param size
	 *            要扩展的大小
	 */
	private void capacity(int size) {
		if (null == m_Cache) {
			m_Cache = new byte[size];
			return;
		}

		int c = m_Cache.length - m_Count;
		if (c < size) {
			// 扩展
			c = size - c;
			m_Cache = Arrays.copyOf(m_Cache, c + m_Count);
		}
	}

	/**
	 * 填充到缓存
	 * 
	 * @param len
	 *            期望填充的字节数
	 * @return 实际填充到缓存的字节数
	 */
	private int cache(int len) throws IOException {
		int c;
		// c = m_Count - m_Pos;
		// if (c <= 0) {
		// 由源输入流读取
		if (null == m_Origin) {
			// throw new EOFException("源已关闭");
			return -1;
		}
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedIOException(String.valueOf(m_Origin));
		}
		int single;
		if (1 == len) {
			// 只读1个字节？先看下是否流结束
			single = m_Origin.read();
			if (-1 == single) {
				// 真是结束
				return -1;
			}
			if (expand(1) < 1) {
				// XXX 不可能吧？
				throw new IOException("扩展缓存容量失败");
			}
			m_Cache[m_Count++] = (byte) single;
			return 1;
		}

		c = expand(len);
		c = m_Origin.read(m_Cache, m_Count, c);
		if (c < 0) {
			// 关掉源输入流？
			m_Origin.close();
			m_Origin = null;
		} else {
			m_Count += c;
		}
		// }
		return c;
	}

	/**
	 * 填充指定量（或完整）的数据到缓存
	 * 
	 * @param len
	 *            要填充的字节数
	 * @return 填充到缓存的字节数
	 */
	private int cacheFully(int len) throws IOException {
		if (null == m_Origin) {
			// throw new EOFException("源已关闭");
			return -1;
		}

		capacity(len);
		int total = len;
		while (len > 0) {
			int n = m_Origin.read(m_Cache, m_Count, len);
			if (n <= 0) {
				break;
			}
			m_Count += n;
			len -= n;
		}
		return total - len;
	}

	/**
	 * 当前已缓存的数据量
	 */
	public int count() {
		return m_Count;
	}

	/**
	 * 当前读取到的位置
	 */
	public int position() {
		return m_Pos;
	}

	/**
	 * 重定位读取到的位置
	 * 
	 * @param pos
	 *            要重定位的位置（0～count()）
	 */
	public void position(int pos) {
		if (pos < 0 || pos > m_Count) {
			throw new IndexOutOfBoundsException("pos[" + pos + "]{0~" + m_Count + "}");
		}
		m_Pos = pos;
	}

	@Override
	synchronized public int read() throws IOException {
		if (m_Pos >= m_Count) {
			if (cache(4096) < 1) {
				return -1;
			}
		}
		return m_Cache[m_Pos++];
	}

	// @Override
	// public int read(byte[] b) throws IOException {
	// return read(b, 0, b.length);
	// }

	@Override
	synchronized public int read(byte[] b, int off, int len) throws IOException {
		int total = 0;
		int c;
		while (len > 0) {
			c = m_Count - m_Pos;
			if (c <= 0) {
				// 先缓存
				c = cache(len < 4096 ? 4096 : len);
				if (-1 == c && 0 == total) {
					return -1;
				}
				if (c < 1) {
					return total;
				}
			}
			// 读取
			if (c > len) {
				c = len;
			}
			System.arraycopy(m_Cache, m_Pos, b, off, c);
			len -= c;
			off += c;
			m_Pos += c;
			total += c;
		}
		return total;
	}

	@Override
	synchronized public long skip(long n) throws IOException {
		if (n > Integer.MAX_VALUE) {
			throw new EOFException("没有这么多数据：" + n);
		}
		int nn = (int) (Integer.MAX_VALUE & n);
		int c = m_Count - m_Pos;
		if (c < 1) {
			c = cache(nn);
			if (c < 1) {
				// throw new EOFException("没有这么多数据：" + nn);
				return -1;
			}
		}
		if (c > nn) {
			c = nn;
		}
		m_Pos += c;
		return c;
	}

	@Override
	public int available() throws IOException {
		if (null == m_Origin) {
			return m_Count - m_Pos;
		}
		return m_Origin.available() + (m_Count - m_Pos);
	}

	/**
	 * 源输入流已关闭（或已缓存完整）
	 */
	public boolean isEof() {
		return null == m_Origin;
	}

	@Override
	public void close() throws IOException {
		InputStream origin = m_Origin;
		if (null != origin) {
			m_Origin = null;
			origin.close();
		}
	}

	@Override
	public synchronized void mark(int readlimit) {
		mark = m_Pos;
	}

	@Override
	public synchronized void reset() throws IOException {
		m_Pos = mark;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	/**
	 * 完全缓存输入流数据
	 * 
	 * @throws IOException
	 */
	synchronized public void fullyCached() throws IOException {
		if (null == m_Origin) {
			return;
		}
		// fullyCached(m_Origin.available());
		AtBuffers buffers = new AtBuffers(m_Origin, 0);
		int count = count();
		expand(buffers.size() + count);
		m_Cache = buffers.toArray(m_Cache, count);
		m_Count = buffers.size() + count;
		close();
	}

	public Bytes getBytes() {
		if (m_Count < 1) {
			return Bytes.empty();
		}
		return new Bytes(m_Cache, m_Pos, m_Count);
	}

	/**
	 * 取得刚好合适内容的字节数组
	 */
	public byte[] getFitBytes() {
		if (0 == m_Pos && m_Count == m_Cache.length) {
			// 很好，直接合适
			return m_Cache;
		}
		return Arrays.copyOfRange(m_Cache, m_Pos, m_Count + m_Pos);
	}

	/**
	 * 输入流缓存到Bytes
	 * 
	 * @param in
	 *            输入流
	 * @return 相应的Bytes
	 * @throws IOException
	 */
	public static Bytes toBytes(InputStream in) throws IOException {
		return toBytes(in, 0);
	}

	/**
	 * 输入流缓存到Bytes
	 * 
	 * @param in
	 *            输入流
	 * @param limit
	 *            若>0则为限制缓存字节数
	 * @return 相应的Bytes
	 * @throws IOException
	 */
	public static Bytes toBytes(InputStream in, int limit) throws IOException {
		AtBuffers buffers = new AtBuffers(in, limit);
		Bytes bytes = new Bytes(buffers.toArray());
		in.close();
		return bytes;
	}

	/**
	 * 由输入流读取（预期小于8K字节的）指定字符集编码字串
	 * 
	 * @param in
	 *            输入流
	 * @param expect
	 *            预期的字节数
	 * @param charset
	 *            字符集，如：UTF-8，若指定为null则自动检查是否为utf-8，不是则使用GBK
	 * @return 指定字符集的字串
	 * @throws IOException
	 */
	public static String readString(InputStream in, int expect, String charset) throws IOException {
		return readString(in, expect, 0, charset);
	}

	/**
	 * 由输入流读取（预期小于8K字节的）指定字符集编码字串
	 * 
	 * @param in
	 *            输入流
	 * @param expect
	 *            预期的字节数
	 * @param limit
	 *            限制的字节数（为0则不限制）
	 * @param charset
	 *            字符集，如：UTF-8，若指定为null则自动检查是否为utf-8，不是则使用GBK
	 * @return 指定字符集的字串
	 * @throws IOException
	 */
	public static String readString(InputStream in, int expect, int limit, String charset)
			throws IOException {
		if (null == in) {
			return null;
		}

		byte[] data;
		int offset, count;

		CachedInputStream cached;
		if (in instanceof CachedInputStream) {
			cached = (CachedInputStream) in;
			if (expect > 0) {
				// 先按预期的量读取
				if (cached.cacheFully(expect) >= expect) {
					// 可能还有哦
					if (limit > 0) {
						// 再读取限制的
						if (cached.cacheFully(limit) >= limit) {
							// 数据量太多了，出错吧
							cached.close();
							throw new OverflowException("要读取的数据量超过限制[" + limit + "]");
						}
					}
					// 无限制，只好全部读
					cached.fullyCached();
				}
			} else {
				cached.fullyCached();
			}
			cached.close();
			data = cached.m_Cache;
			offset = cached.m_Pos;
			count = cached.m_Count;
		} else {
			AtBuffers ab = new AtBuffers();
			ab.input(in, limit);
			in.close();
			data = ab.toArray();
			count = data.length;
			offset = 0;
		}
		if (0 == count) {
			// 空串
			return "";
		}
		if (null == charset) {
			charset = StringUtil.isUtf8(data, offset, count) ? "UTF-8" : "GBK";
		}
		String str = new String(data, offset, count, charset);
		return str;
	}

	/**
	 * 检查输入的字符流是否UTF-8字符
	 * 
	 * @param bytes
	 *            字节数组（字串流）
	 * @param len
	 *            长度（字节）
	 * @return 是则返回true
	 */
	static public boolean isUtf8(byte[] bytes, int len) {
		return StringUtil.isUtf8(bytes, 0, len);
	}

	/**
	 * 完整读取需要的数据
	 * 
	 * @param in
	 *            输入流
	 * @param buffer
	 *            读取缓冲区
	 * @param off
	 *            读缓冲区开始位置
	 * @param len
	 *            期望读取的长度
	 * @return 返回已读取的字节数，若小于期望读取的长度表示输入流已结束
	 * @throws IOException
	 */
	public static int readFully(InputStream in, byte[] buffer, int off, int len)
			throws IOException {
		int total = len;
		while (len > 0) {
			int n = in.read(buffer, off, len);
			if (n < 0) {
				break;
			}
			off += n;
			len -= n;
		}
		return total - len;
	}

	/**
	 * 由输入流复制到流出流（完成会关闭输入流）
	 * 
	 * @param in
	 *            输入流
	 * @param out
	 *            输出流
	 * @return 所复制的字节数
	 * @throws IOException
	 */
	public static int copy(InputStream in, OutputStream out) throws IOException {
		int total = 0;
		int ret;
		byte[] buf;
		buf = Bytes.Pool._8k.poll();
		try {
			do {
				ret = in.read(buf);
				if (ret > 0) {
					out.write(buf, 0, ret);
					total += ret;
				}
			} while (ret >= 0);
		} finally {
			Bytes.Pool._8k.offer(buf);
			in.close();
		}
		return total;
	}

	/**
	 * 先使用缓冲区池暂存，再合并到合适的字节数组内
	 * 
	 * @author liangyi
	 *
	 */
	public static class AtBuffers {
		int m_Count;
		List<byte[]> m_Buffers;
		byte[] m_Bytes;

		public AtBuffers() {
			m_Buffers = new LinkedList<>();
		}

		/**
		 * 使用缓冲区池读取输入流的数据进行暂存
		 * 
		 * @param in
		 *            输入流
		 * @param limit
		 *            若>0则为限制读取的字节数
		 * @throws IOException
		 */
		public AtBuffers(InputStream in, int limit) throws IOException {
			this();
			try {
				input(in, limit);
			} catch (Exception e) {
				// 若过程出错，释放缓冲区
				free();
			}
		}

		/**
		 * 送入缓冲池
		 * 
		 * @param in
		 *            输入流
		 * @param limit
		 *            若>0则为限制读取的字节数
		 * @return 当次读取的节字数
		 * @throws IOException
		 */
		public int input(InputStream in, int limit) throws IOException {
			byte[] buf;
			int count;
			int len, ret, offset, remain;
			buf = Bytes.Pool._8k.poll();
			m_Buffers.add(buf);
			len = buf.length;
			offset = 0;
			if (limit <= 0) {
				limit = Integer.MAX_VALUE;
			}
			count = 0;
			do {
				if (len <= 0) {
					// 分配下个缓冲区
					buf = Bytes.Pool._8k.poll();
					m_Buffers.add(buf);
					len = buf.length;
					offset = 0;
				}
				remain = limit - count;
				if (len > remain) {
					len = remain;
				}
				ret = in.read(buf, offset, len);
				if (ret > 0) {
					count += ret;
					offset += ret;
					len -= ret;
				}
			} while (ret >= 0 && count < limit);
			m_Count += count;
			return count;
		}

		public int size() {
			return m_Count;
		}

		public byte[] toArray() {
			if (null != m_Bytes) {
				return m_Bytes;
			}
			m_Bytes = new byte[size()];
			return toArray(m_Bytes, 0);
		}

		public byte[] toArray(byte[] bs, int offset) {
			int size = bs.length - offset;
			if (size < size()) {
				bs = Arrays.copyOf(bs, offset + size());
			}
			int len;
			Iterator<byte[]> it = m_Buffers.iterator();
			byte[] buf;
			while (it.hasNext() && offset < size) {
				buf = it.next();
				len = size - offset;
				if (len > buf.length) {
					len = buf.length;
				}
				System.arraycopy(buf, 0, bs, offset, len);
				offset += len;
			}
			free();
			return bs;
		}

		/**
		 * 释放buffers回池
		 */
		private void free() {
			if (null == this.m_Buffers) {
				return;
			}
			Iterator<byte[]> it = m_Buffers.iterator();
			while (it.hasNext()) {
				Bytes.Pool._8k.offer(it.next());
			}
			this.m_Buffers = null;
		}

		public void close() {
			free();
			m_Bytes = null;
			m_Count = 0;
		}
	}

}
