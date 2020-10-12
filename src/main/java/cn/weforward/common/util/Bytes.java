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
package cn.weforward.common.util;

import java.util.Arrays;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.sys.GcCleaner;

/**
 * 简单的字节数组描述，byte[],offset,size三个要素
 * 
 * @author liangyi
 * 
 */
public class Bytes {
	/** 存储内容的字节数组 */
	final protected byte[] m_Bytes;
	/** 内容在数组中的开始位置 */
	protected int m_Offset;
	/** 内容的字节数 */
	protected int m_Size;

	public Bytes(byte[] bytes, int offset, int size) {
		if (offset < 0 || offset + size > bytes.length) {
			throw new IllegalArgumentException(
					"offset[" + offset + "]+size[" + size + "] over{0~" + bytes.length + "}");
		}
		if (size < 0 || size > offset + bytes.length) {
			throw new IllegalArgumentException(
					"size[" + size + "+" + offset + "] over{0~" + bytes.length + "}");
		}
		this.m_Bytes = bytes;
		this.m_Offset = offset;
		this.m_Size = size;
	}

	public Bytes(byte[] bytes) {
		this.m_Bytes = bytes;
		this.m_Offset = 0;
		this.m_Size = bytes.length;
	}

	public byte[] getBytes() {
		return m_Bytes;
	}

	public int getOffset() {
		return m_Offset;
	}

	public int getSize() {
		return m_Size;
	}

	/**
	 * 取得刚好合适内容的字节数组
	 */
	public byte[] fit() {
		if (0 == m_Offset && m_Size == m_Bytes.length) {
			// 很好，直接合适
			return m_Bytes;
		}
		return Arrays.copyOfRange(m_Bytes, m_Offset, m_Size + m_Offset);
	}

	/**
	 * 剩余空间（数组长度-size）
	 */
	public int getFree() {
		return (this.m_Bytes.length - this.m_Size);
	}

	public void setSize(int size) {
		if (size < 0 || size + m_Offset > m_Bytes.length) {
			throw new IndexOutOfBoundsException("size[" + size + "]越界 " + this);
		}
		this.m_Size = size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);
		// return "{bytes:" + Arrays.toString(bytes) + ",offset:" + offset +
		// ",size:" + size + "}";
		sb.append("{offset:").append(m_Offset).append(",size:").append(m_Size).append(",free:")
				.append(getFree()).append(",bytes:");
		toString(sb, this.m_Bytes, 50);
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 字串数组格式串
	 * 
	 * @param sb
	 *            输出格式串的StringBuilder，若为null则内部创建
	 * @param bytes
	 *            字节数组
	 * @param limit
	 *            限制节字数（-1为不限制）
	 * @return 格式串 [1,2,8...]
	 */
	public static StringBuilder toString(StringBuilder sb, byte[] bytes, int limit) {
		if (bytes == null) {
			return null;
		}
		int count = bytes.length - 1;
		if (count == -1) {
			if (null == sb) {
				sb = new StringBuilder(2);
			}
			sb.append("[]");
			return sb;
		}
		if (limit > 0 && limit < count) {
			count = limit;
		}
		if (null == sb) {
			sb = new StringBuilder(3 * count);
		}
		sb.append('[');
		for (int i = 0;; i++) {
			sb.append(bytes[i]);
			if (i == count) {
				sb.append(']').toString();
				break;
			}
			sb.append(',');
		}
		return sb;
	}

	public static final byte[] _nilBytes = {};
	static final Bytes _nil = new Bytes(_nilBytes);

	/**
	 * 0长度字节数组
	 */
	public static Bytes empty() {
		return _nil;
	}

	/**
	 * 比较两个字节数组
	 * 
	 * @param b1
	 *            字节数组1
	 * @param offset1
	 *            字节数组1开始位置
	 * @param b2
	 *            字节数组2
	 * @param offset2
	 *            字节数组2开始位置
	 * @param length
	 *            长度
	 * @return 0为相等，>0则b1>b2，<0则b1<b2
	 */
	public static int compare(byte[] b1, int offset1, byte[] b2, int offset2, int length) {
		int ret = 0;
		while (length-- > 0 && 0 == ret) {
			ret = b1[offset1++] - b2[offset2++];
		}
		return ret;
	}

	// public int position() {
	// return m_Offset;
	// }
	//
	// public void position(int newPosition) {
	// if (newPosition < 0 || newPosition >= limit()) {
	// throw new IndexOutOfBoundsException("position[" + newPosition + "]越界 " +
	// this);
	// }
	// m_Offset = newPosition;
	// }
	//
	// public int limit() {
	// return m_Size;
	// }
	//
	// /**
	// * limit()-position()
	// */
	// public int remaining() {
	// return m_Size - m_Offset;
	// }
	//
	// public byte get(int index) {
	// if (index < 0 || index >= limit()) {
	// throw new IndexOutOfBoundsException("index[" + index + "]越界 " + this);
	// }
	// return m_Bytes[index];
	// }

	public static final long UNIT_TB = 1024L * 1024 * 1024 * 1024;
	public static final int UNIT_GB = 1024 * 1024 * 1024;
	public static final int UNIT_MB = 1024 * 1024;
	public static final int UNIT_KB = 1024;
	public static final int REMAIN_XB = 1024 / 10;

	/**
	 * 友好的存储容量格式化，按T、G、K及保留小数点两位，如：4.5T,3.4G,2.3M,1.2K
	 * 
	 * @param bytes
	 *            字节数
	 * @return 格式化后的容量信息
	 */
	public static String formatHumanReadable(long bytes) {
		return formatHumanReadable(new StringBuilder(8), bytes).toString();
	}

	/**
	 * 友好的存储容量格式化，按T、G、K及保留小数点两位，如：4.5T,3.4G,2.3M,1.2K
	 * 
	 * @param sb
	 *            字串缓冲区
	 * @param bytes
	 *            字节数
	 * @return 输出格式化后的容量信息
	 */
	public static StringBuilder formatHumanReadable(StringBuilder sb, long bytes) {
		if (bytes > UNIT_TB) {
			sb.append((bytes / UNIT_TB));
			int remain = (int) ((bytes % UNIT_TB) / UNIT_GB);
			if (remain > REMAIN_XB) {
				remain /= REMAIN_XB;
				sb.append('.').append(remain);
			}
			sb.append("T");
		} else if (bytes > UNIT_GB) {
			sb.append((bytes / UNIT_GB));
			int remain = (int) ((bytes % UNIT_GB) / UNIT_MB);
			if (remain > REMAIN_XB) {
				remain /= REMAIN_XB;
				sb.append('.').append(remain);
			}
			sb.append("G");
		} else if (bytes > UNIT_MB) {
			sb.append((bytes / UNIT_MB));
			int remain = (int) ((bytes % UNIT_MB) / UNIT_KB);
			if (remain > REMAIN_XB) {
				remain /= REMAIN_XB;
				sb.append('.').append(remain);
			}
			sb.append("M");
		} else if (bytes > UNIT_KB) {
			sb.append((bytes / UNIT_KB));
			int remain = (int) (bytes % UNIT_KB);
			if (remain > REMAIN_XB) {
				remain /= REMAIN_XB;
				sb.append('.').append(remain);
			}
			sb.append("K");
		} else {
			sb.append(bytes);
		}
		return sb;
	}

	/**
	 * 从格式化友好的存储容量（如1.5G,200M）中解析出实际字节数大小
	 * 
	 * @param readable
	 *            友好的存储容量
	 * @return 字节数大小
	 * @throws NumberFormatException
	 *             字符串格式错误
	 */
	public static long parseHumanReadable(String readable) throws NumberFormatException {
		if (null == readable || 0 == readable.length()) {
			return 0;
		}
		char last = readable.charAt(readable.length() - 1);
		if (NumberUtil.isNumber(last)) {
			return Long.parseLong(readable);
		}
		long unit;
		switch (last) {
		case 'b':
		case 'B':
			unit = 1;
			break;
		case 'k':
		case 'K':
			unit = UNIT_KB;
			break;
		case 'm':
		case 'M':
			unit = UNIT_MB;
			break;
		case 'g':
		case 'G':
			unit = UNIT_GB;
			break;
		case 't':
		case 'T':
			unit = UNIT_TB;
			break;
		default:
			throw new NumberFormatException("无法解析:" + readable);
		}
		readable = readable.substring(0, readable.length() - 1);
		if (readable.contains(".")) {
			// 可能会有精度问题不管
			return (long) (Double.parseDouble(readable) * unit);
		} else {
			return (Long.parseLong(readable) * unit);
		}
	}

	public static int POOL_8K_SIZE = NumberUtil.toInt(System.getProperty("Bytes.POOL_8K_SIZE"),
			512);
	public static int POOL_1K_SIZE = NumberUtil.toInt(System.getProperty("Bytes.POOL_1K_SIZE"),
			1024);
	public static int POOL_512_SIZE = NumberUtil.toInt(System.getProperty("Bytes.POOL_512_SIZE"),
			2048);

	/**
	 * 找一个容量合适的池
	 * 
	 * @param capacity
	 *            期望的容量
	 * @return 没有合适的池则返回null
	 */
	public static Pool pool(int capacity) {
		if (capacity <= Pool._512.capacity()) {
			return Pool._512;
		}
		if (capacity <= Pool._1k.capacity()) {
			return Pool._1k;
		}
		if (capacity <= Pool._8k.capacity()) {
			return Pool._8k;
		}
		return null;
	}

	/**
	 * 
	 * 字节数组池化
	 * 
	 * @author liangyi
	 */
	public static class Pool extends RingBuffer<byte[]> {
		protected int m_Capacity;

		/**
		 * 构建池
		 * 
		 * @param size
		 *            池大小
		 * @param capacity
		 *            字节数组容量
		 */
		public Pool(int size, int capacity) {
			super(size);
			m_Capacity = capacity;
		}

		public int capacity() {
			return m_Capacity;
		}

		// @Override
		// protected void onInit() {
		// GcCleaner.register(this);
		// }

		@Override
		protected byte[] onEmpty() {
			return new byte[capacity()];
		}

		@Override
		public boolean offer(byte[] item) {
			if (null == item || item.length != capacity()) {
				return false;
			}
			return super.offer(item);
		}

		/** 8k字节数组池 */
		public static Pool _8k = new Pool(POOL_8K_SIZE, 8 * 1024);
		/** 1k字节数组池 */
		public static Pool _1k = new Pool(POOL_1K_SIZE, 1024);
		/** 512字节数组池 */
		public static Pool _512 = new Pool(POOL_512_SIZE, 512);

		static Cleaner _Cleaner = new Cleaner();

		static class Cleaner implements GcCleanable {
			Cleaner() {
				GcCleaner.register(this);
			}

			@Override
			public void onGcCleanup(int policy) {
				_8k.onGcCleanup(policy);
				_1k.onGcCleanup(policy);
				_512.onGcCleanup(policy);
			}
		};
	}
}
