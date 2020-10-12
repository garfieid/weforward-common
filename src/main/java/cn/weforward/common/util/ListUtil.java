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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.weforward.common.ResultPage;

/**
 * List工具类
 * 
 * @author zhangpengji
 * 
 */
public final class ListUtil {

	public static boolean isEmpty(List<?> list) {
		return null == list || 0 == list.size();
	}

	/**
	 * 反转list
	 * 
	 * @param list
	 * @return
	 */
	public static <E> List<E> reverse(List<E> list) {
		if (list.size() < 2) {
			return list;
		}
		return new ReverseList<E>(list);
	}

	public static <E> List<E> toList(List<E> list) {
		if (null == list) {
			return Collections.emptyList();
		}
		return list;
	}

	/**
	 * 遍历ResultPage
	 * 
	 * @param rp
	 * @return
	 */
	public static <E> List<E> toList(ResultPage<E> rp) {
		if (0 == rp.getCount()) {
			return Collections.emptyList();
		}
		if (1 == rp.getCount()) {
			rp.gotoPage(1);
			return Collections.singletonList(rp.next());
		}
		return new ResultPageList<E>(rp);
	}

	/**
	 * 当list不包含obj时添加
	 * 
	 * @param list
	 * @param obj
	 */
	public static <E> void add(List<E> list, E obj) {
		if (!list.contains(obj)) {
			list.add(obj);
		}
	}

	/**
	 * 将list2添加到list1中（当list1不包含该项时）
	 * 
	 * @param list1
	 * @param list2
	 */
	public static <E> void addAll(List<E> list1, List<E> list2) {
		for (E obj : list2) {
			if (!list1.contains(obj)) {
				list1.add(obj);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> fill(T obj, int count) {
		if (0 == count) {
			return Collections.emptyList();
		}
		if (1 == count) {
			return Collections.singletonList(obj);
		}
		Object[] objs = new Object[count];
		Arrays.fill(objs, obj);
		return (List<T>) Arrays.asList(objs);
	}

	public static int indexOf(int[] arr, int val) {
		if (null == arr || 0 == arr.length) {
			return -1;
		}
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == val) {
				return i;
			}
		}
		return -1;
	}

	public static boolean contains(int[] arr, int val) {
		return -1 != indexOf(arr, val);
	}

	public static <E> List<E> union(final List<List<E>> lists) {
		int size = 0;
		for (List<E> list : lists) {
			size += list.size();
		}
		final int _s = size;
		return new AbstractList<E>() {

			@Override
			public E get(int index) {
				int i = 0;
				for (; i < lists.size(); i++) {
					if (index < lists.get(i).size()) {
						break;
					}
					index -= lists.get(i).size();
				}
				return lists.get(i).get(index);
			}

			@Override
			public int size() {
				return _s;
			}
		};
	}

	public static <E> List<E> union(@SuppressWarnings("unchecked") List<E>... lists) {
		return union(Arrays.asList(lists));
	}

	private static class ReverseList<E> extends AbstractList<E> {

		List<E> m_Orginal;

		ReverseList(List<E> list) {
			m_Orginal = list;
		}

		@Override
		public E get(int index) {
			return m_Orginal.get(m_Orginal.size() - index - 1);
		}

		@Override
		public int size() {
			return m_Orginal.size();
		}
	}

	/**
	 * 比较list。null和empty list相同
	 * 
	 * @param l1
	 * @param l2
	 * @return
	 */
	public static boolean eq(List<?> l1, List<?> l2) {
		return l1 == l2 || (isEmpty(l1) && isEmpty(l2)) || (null != l1 && l1.equals(l2));
	}

	private static class ResultPageList<E> extends AbstractList<E> {

		ResultPage<E> m_Original;

		ResultPageList(ResultPage<E> rp) {
			m_Original = rp;
		}

		@Override
		public E get(int index) {
			int pageSize = m_Original.getPageSize();
			int page = (index / pageSize) + 1;
			int pos = index % pageSize;
			if (m_Original.getPage() != page) {
				m_Original.gotoPage(page);
			}
			return m_Original.move(pos);
		}

		@Override
		public int size() {
			return m_Original.getCount();
		}

	}

	/**
	 * 对有序的list进行二分查找
	 * 
	 * @param <E>
	 * @param <K>
	 * @param list
	 * @param key
	 * @param c
	 * @return 当找到<code>key</code>时，返回对应的下标；否则返回 <tt>(-(<i>插入点</i>) - 1)</tt>
	 */
	public static <E, K> int binarySearch(List<? extends E> list, K key, ComparatorExt<? super E, K> c) {
		int low = 0;
		int high = list.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			E midVal = list.get(mid);
			int cmp = c.compareTo(midVal, key);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found
	}
}
