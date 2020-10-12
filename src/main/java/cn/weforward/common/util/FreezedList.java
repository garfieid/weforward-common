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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

/**
 * 冻结列表项的只读列表
 * 
 * @author liangyi
 * 
 * @param <E> 列表项类型
 */
public class FreezedList<E> extends AbstractList<E> implements RandomAccess, Serializable {
	private static final long serialVersionUID = 1L;
	/** 列表项 */
	final protected E[] m_Elements;
	/** 项数 */
	final protected int m_Size;
	/** 开始位置（下标） */
	final protected int m_Offset;

	/**
	 * 封装为只读的列表
	 * 
	 * @param <E>  列表项类型
	 * @param list 要封装的列表
	 * @return 封装后的列表
	 */
	public static <E> List<E> freezed(List<E> list) {
		if (list instanceof FreezedList<?>) {
			return (List<E>) list;
		}
		if (null == list || list.isEmpty()) {
			return Collections.emptyList();
		}
		if (list.size() == 1) {
			return Collections.singletonList((E) list.get(0));
		}
		return new FreezedList<E>(list);
	}

	/**
	 * 封装为只读的列表
	 * 
	 * @param <E>      列表项类型
	 * @param list     要封装的列表
	 * @param trimNull 是否去除空项
	 * @return 封装后的列表
	 */
	public static <E> List<E> freezed(List<E> list, boolean trimNull) {
		if (list instanceof FreezedList<?>) {
			return (List<E>) list;
		}
		if (null == list || list.isEmpty()) {
			return Collections.emptyList();
		}
		if (list.size() == 1) {
			E element = list.get(0);
			if (null == element) {
				return Collections.emptyList();
			}
			return Collections.singletonList(element);
		}
		return new FreezedList<E>(list, trimNull);
	}

	/**
	 * 替换列表项且封装为只读
	 * 
	 * @param <E>   列表项类型
	 * @param list  要替换且封装的列表
	 * @param index 替换项的位置
	 * @param value 新的项值
	 * @return 封装后的列表
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> replaceToFreezed(List<? extends E> list, int index, E value) {
		// if (list instanceof FreezedList<?>) {
		// return (List<E>) list;
		// }
		if (index < 0 || index >= list.size()) {
			throw new ArrayIndexOutOfBoundsException(index + " over 0~" + list.size());
		}
		if (1 == list.size()) {
			return Collections.singletonList(value);
		}
		E[] elements = (E[]) list.toArray();
		elements[index] = value;
		return new FreezedList<E>(elements, elements.length, true);
	}

	/**
	 * 删除列表项且封装为只读
	 * 
	 * @param <E>   列表项类型
	 * @param list  要删除且封装的列表
	 * @param index 删除的项位置
	 * @param count 删除的项数
	 * @return 封装后的列表
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> removeToFreezed(List<? extends E> list, int index, int count) {
		// if (list instanceof FreezedList<?>) {
		// return (List<E>) list;
		// }
		if (index < 0 || count < 0 || index + count > list.size()) {
			throw new ArrayIndexOutOfBoundsException(index + "/" + count + " over 0~" + list.size());
		}
		if (0 == index && list.size() == count) {
			return Collections.emptyList();
		}
		/*
		 * if (list.size() == 1 + count) { return Collections.singletonList((E)
		 * list.get(0)); }
		 */
		E[] elements = (E[]) new Object[list.size() - count];
		int i = 0;
		for (; i < index; i++) {
			elements[i] = list.get(i);
		}
		for (index += count; index < list.size(); index++) {
			elements[i++] = list.get(index);
		}
		return new FreezedList<E>(elements, elements.length, true);
	}

	/**
	 * 插入列表项且封装为只读
	 * 
	 * @param <E>   列表项类型
	 * @param list  要插入且封装的列表
	 * @param index 插入项的位置
	 * @param value 要插入的项
	 * @return 封装后的列表
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> addToFreezed(List<? extends E> list, int index, E value) {
		// if (list instanceof FreezedList<?>) {
		// return (List<E>) list;
		// }
		if (index < 0 || index > list.size()) {
			throw new ArrayIndexOutOfBoundsException(index + " over 0~" + list.size());
		}
		if (0 == list.size()) {
			return Collections.singletonList(value);
		}
		E[] elements = (E[]) new Object[list.size() + 1];
		int i = 0;
		for (; i < index; i++) {
			elements[i] = list.get(i);
		}
		elements[index++] = value;
		for (; i < list.size(); i++) {
			elements[index++] = list.get(i);
		}
		return new FreezedList<E>(elements, elements.length, true);
	}

	/**
	 * 封装为只读的列表
	 * 
	 * @param <E>  列表项类型
	 * @param coll 要封装的列表
	 * @return 封装后的列表
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> freezed(Collection<E> coll) {
		if (coll instanceof FreezedList<?>) {
			return (List<E>) coll;
		}
		if (null == coll || coll.isEmpty()) {
			return Collections.emptyList();
		}
		if (coll.size() == 1) {
			E[] a = coll.toArray((E[]) new Object[1]);
			return Collections.singletonList(a[0]);
		}
		return new FreezedList<E>(coll);
	}

	/**
	 * 封装为只读的列表
	 * 
	 * @param <E>  列表项类型
	 * @param coll 要封装的列表
	 * @param sort 若指定比较器则排序后封装
	 * @return 封装后的列表
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> freezed(Collection<E> coll, Comparator<? super E> sort) {
		if (null == sort) {
			return freezed(coll);
		}
		// if (coll instanceof FreezedList<?>) {
		// return (List<E>) coll;
		// }
		if (null == coll || coll.isEmpty()) {
			return Collections.emptyList();
		}
		if (coll.size() == 1) {
			E[] a = coll.toArray((E[]) new Object[1]);
			return Collections.singletonList(a[0]);
		}
		// (E[]) coll.toArray(new Object[coll.size()]);
		E[] elements = (E[]) coll.toArray();
		Arrays.sort(elements, sort);
		return new FreezedList<E>(elements, elements.length, true);
	}

	/**
	 * 封装为只读的列表
	 * 
	 * @param <E>      列表项类型
	 * @param elements 要封装的数组
	 * @param sort     若指定比较器则排序后封装
	 * @return 封装后的列表
	 */
	public static <E> List<E> freezed(E[] elements, Comparator<? super E> sort) {
		if (0 == elements.length) {
			return Collections.emptyList();
		}
		if (1 == elements.length) {
			return Collections.singletonList(elements[0]);
		}
		elements = Arrays.copyOf(elements, elements.length);
		if (null != sort) {
			Arrays.sort(elements, sort);
		}
		return new FreezedList<E>(elements, elements.length, true);
	}

	/**
	 * 封装为只读排序（且去除空项）的字串列表
	 * 
	 * @param list 要封装的字串列表
	 * @return 封装后的字串列表
	 */
	public static List<String> freezedStrings(List<String> list) {
		if (list instanceof StringList) {
			return list;
		}
		if (null == list || list.isEmpty()) {
			return Collections.emptyList();
		}
		if (list.size() == 1) {
			return Collections.singletonList(list.get(0));
		}
		String[] elements = (String[]) list.toArray();
		Arrays.sort(elements, _compString);
		int i = elements.length - 1;
		for (; i >= 0; i--) {
			if (null != elements[i] && 0 != elements[i].length()) {
				break;
			}
		}
		if (i < 0) {
			return Collections.emptyList();
		}
		return new StringList(elements, 0, i + 1);
	}

	public FreezedList(E[] elements, int size, boolean attach) {
		this(elements, 0, size, attach);
	}

	@SuppressWarnings("unchecked")
	public FreezedList(E[] elements, int offset, int size, boolean isAttach) {
		m_Size = size;
		if (isAttach) {
			m_Elements = elements;
			m_Offset = offset;
		} else {
			m_Elements = (E[]) new Object[m_Size];
			System.arraycopy(elements, offset, m_Elements, 0, m_Size);
			m_Offset = 0;
		}
	}

	protected FreezedList(E[] elements) {
		m_Elements = elements;
		m_Size = (null == elements) ? 0 : elements.length;
		m_Offset = 0;
	}

	public FreezedList(Collection<? extends E> elements) {
		this(elements, false);
	}

	@SuppressWarnings("unchecked")
	public FreezedList(Collection<? extends E> elements, boolean trimNull) {
		E[] ls;
		if (trimNull) {
			ls = (E[]) new Object[elements.size()];
			Iterator<? extends E> it = elements.iterator();
			int count = 0;
			while (it.hasNext()) {
				E o = it.next();
				if (null != o) {
					ls[count++] = o;
				}
			}
			if (count + 4 <= ls.length) {
				ls = Arrays.copyOf(ls, count);
			}
			m_Elements = ls;
			m_Size = count;
		} else {
			// m_Elements = (E[]) elements.toArray(new Object[elements.size()]);
			ls = (E[]) elements.toArray();
			m_Elements = ls;
			m_Size = m_Elements.length;
		}
		m_Offset = 0;
	}

	@Override
	public E get(int index) {
		return m_Elements[index + m_Offset];
	}

	@Override
	public int size() {
		return m_Size;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(m_Elements);
		return result;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if (fromIndex < 0) {
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		}
		if (toIndex > size()) {
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
		}
		if (fromIndex == toIndex) {
			return Collections.emptyList();
		}
		return new FreezedList<E>(m_Elements, fromIndex + m_Offset, toIndex - fromIndex, true);
	}

	@Override
	public Object[] toArray() {
		if (0 == size()) {
			return _nilArray;
		}
		Object[] array = new Object[size()];
		System.arraycopy(m_Elements, m_Offset, array, 0, array.length);
		return array;
	}

	public static final Object[] _nilArray = new Object[0];
	/** 字串顺序排序比较器（但空项或空字串排到最后） */
	public static final Comparator<String> _compString = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if (o1 == o2) {
				return 0;
			}
			if (null == o1 || 0 == o1.length()) {
				return 1;
			}
			if (null == o2 || 0 == o2.length()) {
				return -1;
			}
			return o1.compareTo(o2);
		}
	};

	/**
	 * 排序的字串列表
	 * 
	 * @author liangyi
	 *
	 */
	public static class StringList extends FreezedList<String> {
		private static final long serialVersionUID = 1L;

		protected StringList(String[] elements, int offset, int size) {
			super(elements, offset, size, true);
		}

		@Override
		public boolean contains(Object o) {
			if (o == null) {
				return false;
			}
			String s = o.toString();
			int i = size();
			if (0 == s.length() || i < 1) {
				return false;
			}
			int ret;
			if (i > 1000) {
				// 大于1000项，二分查
				ret = Arrays.binarySearch(m_Elements, m_Offset, m_Offset + size(), s, _compString);
				return (ret >= 0);
			}

			// 由后往前查
			for (--i; i >= 0; i--) {
				ret = s.compareTo(get(i));
				if (0 == ret) {
					return true;
				}
				if (ret > 0) {
					// 后面的项更小，不用查了
					return false;
				}
			}
			return false;
		}
	}

}
