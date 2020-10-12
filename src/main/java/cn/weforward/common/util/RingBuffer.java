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

import org.slf4j.LoggerFactory;

import cn.weforward.common.Destroyable;
import cn.weforward.common.GcCleanable;

/**
 * 环型缓冲区（FIFO），也适合做缓存池
 * 
 * @author liangyi
 *
 * @param <T>
 */
public class RingBuffer<T> implements Destroyable, GcCleanable {
	// protected final static Logger _Logger =
	// LoggerFactory.getLogger(RingBuffer.class);

	protected T[] m_Items;
	protected volatile int m_IndexPut;
	protected volatile int m_IndexGet;

	protected volatile int m_FullCounter;
	protected volatile int m_EmtryCounter;
	protected volatile int m_HitCounter;

	private Object lockPut() {
		return m_Items;
	}

	private Object lockGet() {
		return this;
	}

	/**
	 * 缓冲区满了
	 * 
	 * @param item
	 *            要置入的项
	 */
	protected void onFull(T item) {
	}

	/**
	 * 缓冲区空了
	 * 
	 * @return 当缓冲区为空时返回给它吧
	 */
	protected T onEmpty() {
		return null;
	}

	/**
	 * 释放缓存项
	 * 
	 * @param item
	 *            要释放的缓存项
	 * @param index
	 *            缓存项所在的位置（下标）
	 */
	protected void onDestroy(T item, int index) {
	}

	/**
	 * 初始化
	 */
	protected void onInit() {
	}

	@SuppressWarnings("unchecked")
	public RingBuffer(int size) {
		// 修正缓存项数必须是2的幂
		int v = 1;
		do {
			v <<= 1;
		} while (v < size);
		m_Items = (T[]) new Object[v];
		onInit();
	}

	/**
	 * 置入缓冲区
	 * 
	 * @param item
	 *            要置入缓冲区的项
	 * @return 缓冲区满则失败返回false，成功则返回true
	 * 
	 * @see #remove()
	 * @see #poll()
	 */
	public boolean offer(T item) {
		synchronized (lockPut()) {
			if ((m_IndexGet ^ m_Items.length) != m_IndexPut) {
				m_Items[logicIndex(m_IndexPut)] = item;
				m_IndexPut = virtualIndex(m_IndexPut + 1);
				return true;
			}
		}
		// 缓冲区满了，读写指针相差m_Items.length
		++m_FullCounter;
		onFull(item);
		return false;
	}

	/**
	 * 由缓冲区获取最顶的项（不置空引用），若缓冲区空则调用onEmpty返回值
	 * 
	 * @see #remove()
	 */
	public T poll() {
		synchronized (lockGet()) {
			if (m_IndexGet != m_IndexPut) {
				T item = m_Items[logicIndex(m_IndexGet)];
				m_IndexGet = virtualIndex(m_IndexGet + 1);
				++m_HitCounter;
				return item;
			}
		}
		// 缓冲区是空的
		++m_EmtryCounter;
		return onEmpty();
	}

	/**
	 * 由缓冲区获取最顶的项（置空引用），若缓冲区空则调用onEmpty返回值
	 * 
	 * @see #poll()
	 */
	public T remove() {
		synchronized (lockGet()) {
			if (m_IndexGet != m_IndexPut) {
				int idx = logicIndex(m_IndexGet);
				T item = m_Items[idx];
				m_Items[idx] = null;
				m_IndexGet = virtualIndex(m_IndexGet + 1);
				++m_HitCounter;
				return item;
			}
		}
		// 缓冲区是空的
		++m_EmtryCounter;
		return onEmpty();
	}

	public T item(int index) {
		return m_Items[index];
	}

	/**
	 * 转换为逻辑地址（在缓冲区的实际位置）
	 * 
	 * @param index
	 *            地址
	 * @return 0～size()-1
	 */
	private int logicIndex(int index) {
		return index & (m_Items.length - 1);
	}

	/**
	 * 虚拟地址（用于识别缓冲区空还是满）
	 * 
	 * @param index
	 *            地址
	 * @return 0～size()*2-1
	 */
	private int virtualIndex(int index) {
		return index & (2 * m_Items.length - 1);
	}

	public int size() {
		return (null == m_Items) ? 0 : m_Items.length;
	}

	public int readable() {
		if (m_IndexPut == m_IndexGet) {
			// 没有可读的
			return 0;
		}
		int w = logicIndex(m_IndexPut);
		int r = logicIndex(m_IndexGet);
		return w > r ? w - r : (w + size() - r);
	}

	public int writable() {
		return size() - readable();
	}

	/**
	 * 清空缓冲区
	 */
	public boolean empty() {
		if (0 == m_IndexPut && 0 == m_IndexGet) {
			// 没有缓存项
			return false;
		}

		T item;
		synchronized (lockPut()) {
			synchronized (lockGet()) {
				m_IndexPut = 0;
				m_IndexGet = 0;
				for (int i = m_Items.length - 1; i >= 0; i--) {
					item = m_Items[i];
					m_Items[i] = null;
					if (null != item) {
						onDestroy(item, i);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void destroy() {
		empty();
		m_Items = null;
	}

	@Override
	public void onGcCleanup(int policy) {
		if (POLICY_CRITICAL == policy) {
			if (empty()) {
				LoggerFactory.getLogger(RingBuffer.class).info("memory critical do emtpy." + this);
			}
		}
	}

	@Override
	public String toString() {
		return "{size:" + size() + ",readable:" + readable() + ",ip:" + m_IndexPut + ",ig:"
				+ m_IndexGet + ",empty:" + m_EmtryCounter + ",full:" + m_FullCounter + ",hit:"
				+ m_HitCounter + "}";
	}
}
