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
package cn.weforward.common;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import cn.weforward.common.execption.UnsupportedException;

/**
 * 按对照ID以List集合组织的对照表，get访问的不是下标，是NameItem的id
 * 
 * @author liangyi
 * 
 */
public class NameItems extends AbstractCollection<NameItem> implements List<NameItem> {
	/** 按ID比较 */
	final static Comparator<NameItem> _CompById = new Comparator<NameItem>() {
		@Override
		public int compare(NameItem o1, NameItem o2) {
			return o1.getId() - o2.getId();
		}
	};
	protected final NameItem[] m_Items;

	/**
	 * 封装为只读的按ID排序集合
	 * 
	 * @param items 名称/ID对照表
	 * @return 集合封装
	 */
	static public NameItems valueOf(NameItem... items) {
		return new NameItems(items);
	}

	/**
	 * 创建对照表
	 * 
	 * @param items 对照项列表
	 */
	protected NameItems(NameItem... items) {
		m_Items = items;
		Arrays.sort(m_Items, _CompById);
	}

	/**
	 * 由对照ID获取对照项
	 * 
	 * @param id 对照ID
	 * @return 相应的对照项，没有则返回unknown()的值
	 */
	@Override
	public NameItem get(int id) {
		int low = 0;
		int high = m_Items.length - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			NameItem midVal = m_Items[mid];
			int cmp = midVal.id - id;
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				return midVal; // key found
			}
		}
		// return -(low + 1); // key not found.
		return unknown(id);
	}

	/**
	 * 子类可覆盖返回定义的未知项
	 * 
	 * @param id ID
	 * @return 所未知id的项
	 */
	protected NameItem unknown(int id) {
		return NameItem.valueOf("未知[" + id + "]", id);
	}

	@Override
	public int size() {
		return m_Items.length;
	}

	@Override
	public void add(int index, NameItem element) {
		throw new UnsupportedException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends NameItem> c) {
		throw new UnsupportedException();
	}

	@Override
	public int indexOf(Object o) {
		for (int i = 0; i < m_Items.length; i++) {
			if (m_Items[i].equals(o)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Iterator<NameItem> iterator() {
		return listIterator(0);
	}

	@Override
	public int lastIndexOf(Object o) {
		for (int i = m_Items.length - 1; i >= 0; i--) {
			if (m_Items[i].equals(o)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public ListIterator<NameItem> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<NameItem> listIterator(int index) {
		return new ListItr(index);
	}

	@Override
	public NameItem remove(int index) {
		throw new UnsupportedException();
	}

	@Override
	public NameItem set(int index, NameItem element) {
		throw new UnsupportedException();
	}

	@Override
	public List<NameItem> subList(int fromIndex, int toIndex) {
		if (fromIndex < 0 || fromIndex >= size() || toIndex < 0 || toIndex >= size() || fromIndex > toIndex) {
			throw new IndexOutOfBoundsException("fromIndex=" + fromIndex + ",toIndex=" + toIndex + ",size=" + size());
		}
		NameItem[] sub = Arrays.copyOfRange(m_Items, fromIndex, toIndex);
		return new NameItems(sub);
	}

	/**
	 * 迭代器
	 * 
	 * @author liangyi
	 * 
	 */
	private class ListItr implements ListIterator<NameItem> {
		int cursor = 0;

		ListItr(int index) {
			cursor = index;
		}

		public boolean hasNext() {
			return cursor != size();
		}

		public NameItem next() {
			int i = cursor;
			if (i < 0 || i >= size()) {
				throw new NoSuchElementException();
			}
			NameItem next = m_Items[i];
			// lastRet = i;
			cursor = i + 1;
			return next;
		}

		public void remove() {
			throw new UnsupportedException();
		}

		public boolean hasPrevious() {
			return cursor != 0;
		}

		public NameItem previous() {
			int i = cursor - 1;
			if (i < 0 || i >= size()) {
				throw new NoSuchElementException();
			}
			NameItem previous = get(i);
			// lastRet = cursor = i;
			return previous;
		}

		public int nextIndex() {
			return cursor;
		}

		public int previousIndex() {
			return cursor - 1;
		}

		public void set(NameItem e) {
			throw new UnsupportedException();
		}

		public void add(NameItem e) {
			throw new UnsupportedException();
		}
	}
}
