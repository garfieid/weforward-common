package cn.weforward.common.util;

import cn.weforward.common.sys.GcCleaner;

/**
 * StringBuilder池化
 * 
 * @author liangyi
 *
 */
public class StringBuilderPool extends RingBuffer<StringBuilder> {
	protected int m_InitialCapacity;

	/**
	 * 构建池
	 * 
	 * @param size
	 *            池大小
	 * @param initialCapacity
	 *            StringBuilder创建时初始容量
	 */
	public StringBuilderPool(int size, int initialCapacity) {
		super(size);
		m_InitialCapacity = initialCapacity;
	}

	@Override
	protected StringBuilder onEmpty() {
		return new StringBuilder(m_InitialCapacity);
	}

	@Override
	protected void onInit() {
		GcCleaner.register(this);
	}

	@Override
	public StringBuilder poll() {
		StringBuilder v = super.poll();
		v.setLength(0);
		return v;
	}

	@Override
	public StringBuilder remove() {
		StringBuilder v = super.remove();
		v.setLength(0);
		return v;
	}

	@Override
	public boolean offer(StringBuilder item) {
		if (null == item || (item.capacity() << 1) > m_InitialCapacity) {
			// 容量不合适，不归池
			return false;
		}
		return super.offer(item);
	}

	public int getInitialCapacity() {
		return m_InitialCapacity;
	}

	/** 8K初始容量 */
	public static StringBuilderPool _8k = new StringBuilderPool(1024, 8192);
	// /** 1K初始容量 */
	// public static StringBuilderPool _1k = new StringBuilderPool(1024, 1024);
	// /** 512初始容量 */
	// public static StringBuilderPool _512 = new StringBuilderPool(2048, 512);
	/** 128初始容量 */
	public static StringBuilderPool _128 = new StringBuilderPool(2048, 128);
}
