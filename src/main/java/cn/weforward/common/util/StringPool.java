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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.crypto.Hasher;

/**
 * 字串共享池（一般用于解析格式串中复用字串减少内存消耗）
 * 
 * @author liangyi
 *
 */
public class StringPool implements GcCleanable {
	static final Logger _Logger = LoggerFactory.getLogger(StringPool.class);

	static final Entry[] EMPTY_TABLE = new Entry[0];

	/** hash表容量 */
	int m_MaxCapacity;
	/** 进入池的最大字串长度 */
	int m_InternMaxLength = 100;
	/** 池中字串项数 */
	transient int m_Size;
	/** hash表 */
	transient Entry[] m_Table = EMPTY_TABLE;
	/** 命中计数 */
	long m_Hits;
	/** 不命中计数 */
	long m_Miss;
	/** 太长字串计数 */
	long m_TooLong;

	/**
	 * hash表项
	 * 
	 * @author liangyi
	 *
	 */
	static class Entry {
		final String string;
		final Entry next;

		public Entry(String str, Entry entry) {
			this.string = str;
			this.next = entry;
		}
	}

	public StringPool() {
		m_MaxCapacity = 1000;
	}

	public StringPool(int maxCapacity) {
		m_MaxCapacity = maxCapacity;
	}

	public void setMaxLength(int maxLength) {
		m_InternMaxLength = maxLength;
	}

	public void setMaxCapacity(int maxCapacity) {
		m_MaxCapacity = maxCapacity;
	}

	private int hash(CharSequence str, int beginIndex, int endIndex) {
		return Hasher.stringHash(str, beginIndex, endIndex, 0);
	}

	private int indexFor(int hash, int len) {
		return hash & (len - 1);
	}

	private boolean eq(String entry, CharSequence str, int beginIndex, int endIndex) {
		if (entry.length() != endIndex - beginIndex) {
			return false;
		}
		for (int i = 0; beginIndex < endIndex; i++, beginIndex++) {
			if (entry.charAt(i) != str.charAt(beginIndex)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 把字串合并到字串池中
	 * 
	 * @param str
	 *            要合并到字串池
	 * @return 合并后的字串实例
	 */
	public String intern(CharSequence str) {
		if (null == str) {
			return null;
		}
		if (0 == str.length()) {
			return "";
		}
		return intern(str, 0, str.length());
	}

	public String intern(CharSequence chars, int beginIndex, int endIndex) {
		if (endIndex - beginIndex > m_InternMaxLength) {
			++m_TooLong;
			return chars.subSequence(beginIndex, endIndex).toString();
		}
		int hash = hash(chars, beginIndex, endIndex);
		String str;
		synchronized (this) {
			if (m_Table == EMPTY_TABLE) {
				// 16表项起点
				m_Table = new Entry[16];
			}
			int idx = indexFor(hash, m_Table.length);
			for (Entry e = m_Table[idx]; null != e; e = e.next) {
				if (eq(e.string, chars, beginIndex, endIndex)) {
					// yeah!命中缓冲
					// System.out
					// .println("hit:" + str.substring(beginIndex, endIndex) +
					// "," + e.string);
					// XXX 测试统计用
					++m_Hits;
					return e.string;
				}
			}
			str = chars.subSequence(beginIndex, endIndex).toString();
			addEntry(str, idx);
		}
		++m_Miss;
		return str;
	}

	private void addEntry(String str, int idx) {
		if ((m_Size >= m_Table.length) && (null != m_Table[idx]) && m_Size < m_MaxCapacity) {
			// 缓冲项数大于表容量2倍，重建hash表（尝试缩短链表提高性能）
			resize(Math.min(2 * m_Table.length, m_MaxCapacity));
			// 重新计算idx
			int hash = hash(str, 0, str.length());
			idx = indexFor(hash, m_Table.length);
		}
		Entry e = m_Table[idx];
		m_Table[idx] = new Entry(str, e);
		m_Size++;
	}

	private void resize(int size) {
		if (_Logger.isTraceEnabled()) {
			_Logger.trace("重建字串缓冲池[" + size + "]" + this);
		}
		Entry[] newTable = new Entry[size];
		Entry e;
		int idx;
		for (int i = m_Table.length - 1; i >= 0; i--) {
			for (e = m_Table[i]; null != e; e = e.next) {
				idx = indexFor(hash(e.string, 0, e.string.length()), size);
				newTable[idx] = new Entry(e.string, newTable[idx]);
			}
		}
		m_Table = newTable;
	}

	synchronized public void clear() {
		if (null == m_Table || 0 == m_Table.length) {
			return;
		}
		_Logger.info("清空字串缓冲池" + this);
		for (int i = m_Table.length - 1; i >= 0; i--) {
			m_Table[i] = null;
		}
		m_Size = 0;
	}

	public int size() {
		return m_Size;
	}

	@Override
	public void onGcCleanup(int policy) {
		if (GcCleanable.POLICY_LOW == (GcCleanable.POLICY_MASK & policy)) {
			// m_Table = EMPTY_TABLE;
			// 清空缓存项（但保留表）
			clear();
		}
	}

	@Override
	public String toString() {
		return "{max-c:" + m_MaxCapacity + ",c:" + m_Table.length + ",max-l:" + m_InternMaxLength
				+ ",size:" + m_Size + ",hits:" + m_Hits + ",miss:" + m_Miss + ",long:" + m_TooLong
				+ "}";
	}
}
