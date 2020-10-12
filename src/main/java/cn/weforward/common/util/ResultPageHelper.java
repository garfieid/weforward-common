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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import cn.weforward.common.ResultPage;

/**
 * ResultPage相关的辅助方法
 * 
 * @author liangyi、zhangpengji
 */
public final class ResultPageHelper {

	/** 整页遍历的最大项数 */
	public static final int MAX_SIZE_ON_FOREACH = 1000;

	/**
	 * 快速封装（构造）单结果项ResultPage<E>
	 * 
	 * @param <E>
	 *            项类型
	 * @param element
	 *            结果项
	 * @return 封装后的ResultPage
	 */
	static public <E> ResultPage<E> singleton(E element) {
		return new Singleton<E>(element);
	}

	/**
	 * 快速封装（构造）基于List<E>的ResultPage<E>
	 * 
	 * @param <E>项类型
	 * @param ls
	 *            结果集（List）
	 * @return 封装后的ResultPage
	 */
	@SuppressWarnings("unchecked")
	static public <E> ResultPage<E> toResultPage(List<? super E> ls) {
		if (0 == ls.size()) {
			return empty();
		}
		if (1 == ls.size()) {
			return singleton((E) ls.get(0));
		}
		return new OnList<E>(ls);
	}

	@SuppressWarnings("unchecked")
	/**
	 * 没结果项的ResultPage
	 */
	public static <E> ResultPage<E> empty() {
		return (ResultPage<E>) _nil;
	}

	/**
	 * 当结果只有一项时，能使用其作简单快捷的封装
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 *            结果对象类型
	 */
	public static class Singleton<E> implements ResultPage<E> {
		protected E element;
		protected int pos;
		/** 结果集标识 */
		protected String resultId;

		public Singleton(E element) {
			this.element = element;
			this.pos = -1;
		}

		/**
		 * @see ResultPages
		 */
		public int getCount() {
			return 1;
		}

		/**
		 * @see ResultPages
		 */
		public int getPage() {
			return (this.pos < 0) ? 0 : 1;
		}

		/**
		 * @see ResultPages
		 */
		public int getPageCount() {
			return 1;
		}

		/**
		 * @see ResultPages
		 */
		public int getPageSize() {
			return 1;
		}

		/**
		 * @see ResultPages
		 */
		public boolean gotoPage(int page) {
			if (1 == page) {
				pos = 0;
				return true;
			}
			return false;
		}

		/**
		 * @see ResultPages
		 */
		public boolean hasPrev() {
			return false;
		}

		/**
		 * @see ResultPages
		 */
		public E move(int pos) {
			if (1 == pos) {
				this.pos = 1;
				return element;
			}
			return null;
		}

		/**
		 * @see ResultPages
		 */
		public E prev() {
			if (1 == pos) {
				pos = 0;
				return element;
			}
			return null;
		}

		/**
		 * @see ResultPages
		 */
		public void setPageSize(int size) {
		}

		/**
		 * @see ResultPages
		 */
		public boolean hasNext() {
			return (0 == pos);
		}

		/**
		 * @see ResultPages
		 */
		public E next() {
			if (0 == pos) {
				++pos;
				return element;
			}
			return null;
		}

		/**
		 * @see ResultPages
		 */
		public void remove() {
		}

		/**
		 * @see ResultPages
		 */
		public Iterator<E> iterator() {
			return this;
		}

		/**
		 * @see ResultPages
		 */
		public void sort(Comparator<E> c, int limit) {
		}

		public void setPage(int page) {
			if (!gotoPage(page)) {
				throw new IndexOutOfBoundsException("over page " + page + " at 1~" + getPageCount());
			}
		}
	}

	/**
	 * 直接由List封装为ResultPage
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 */
	public static final class OnList<E> extends AbstractResultPage<E> {
		private List<E> m_Elements;

		@SuppressWarnings("unchecked")
		private OnList(List<? super E> ls) {
			m_Elements = (List<E>) ls;
		}

		@Override
		protected E get(int idx) {
			return m_Elements.get(idx);
		}

		public int getCount() {
			return m_Elements.size();
		}
	}

	static final ResultPage<?> _nil = new Empty<Object>();

	/**
	 * 没结果项的ResultPage快速封装
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 */
	public static class Empty<E> implements ResultPage<E> {

		public int getCount() {
			return 0;
		}

		public int getPage() {
			return 0;
		}

		public int getPageCount() {
			return 0;
		}

		public int getPageSize() {
			return 0;
		}

		public boolean gotoPage(int page) {
			return false;
		}

		public boolean hasPrev() {
			return false;
		}

		public E move(int pos) {
			return null;
		}

		public E prev() {
			return null;
		}

		public void setPageSize(int size) {
		}

		public boolean hasNext() {
			return false;
		}

		public E next() {
			return null;
		}

		public void remove() {
		}

		public Iterator<E> iterator() {
			return this;
		}

		public void sort(Comparator<E> c, int limit) {
		}

		public void setPage(int page) {
			throw new IndexOutOfBoundsException("over page " + page + " at empty");
		}
	}

	/**
	 * 反转ResultPage
	 * 
	 * @param <E>
	 *            项类型
	 * @param rp
	 *            反转前的页结果
	 * @return 反转后的页结果
	 */
	static public <E> ResultPage<E> reverseResultPage(ResultPage<E> rp) {
		if (rp.getCount() < 2) {
			// 小于2项的，反不反转都一个样
			return rp;
		}
		return new Reverse<E>(rp);
	}

	/**
	 * 反转ResultPage结果（相当于倒排序）
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 */
	public static final class Reverse<E> extends AbstractResultPage<E> {
		private ResultPage<E> m_Original;

		private Reverse(ResultPage<E> rp) {
			m_Original = rp;
		}

		@Override
		protected E get(int idx) {
			// 计算反转后的位置
			int ridx = m_Original.getCount() - 1 - idx;
			// 定位到页
			int page = 1 + (ridx / m_Original.getPageSize());
			m_Original.gotoPage(page);
			// 所在页的位置
			int pos = ridx - ((page - 1) * m_Original.getPageSize());
			return m_Original.move(pos);
		}

		@Override
		public int getCount() {
			return m_Original.getCount();
		}

		@Override
		public String toString() {
			return super.toString() + m_Original;
		}
	}

	/**
	 * 转换ResultPage中前部分的项为List
	 * 
	 * @param <E>
	 *            结果项类型
	 * @param rp
	 *            页结果
	 * @param limit
	 *            限制转换的项数
	 * @return 转换得到的列表
	 */
	static public <E> List<E> toList(ResultPage<E> rp, int limit) {
		// return AbstractResultPage.toList(rp, limit);
		return toList(rp, null, limit);
	}

	/**
	 * 转换ResultPage中前部分的项为List，且对其进行排序
	 * 
	 * @param <E>
	 *            结果项类型
	 * @param rp
	 *            页结果
	 * @param c
	 *            排序比较器
	 * @param limit
	 *            限制转换的项数
	 * @return 转换得到的列表
	 */
	@SuppressWarnings("unchecked")
	static public <E> List<E> toList(ResultPage<E> rp, Comparator<E> c, int limit) {
		if (null == rp || 0 == rp.getCount()) {
			return Collections.emptyList();
		}
		if (limit < 1 || limit > rp.getCount()) {
			// 修正limit的修不能小于0或大于总结果数
			limit = rp.getCount();
		}

		if (1 == limit) {
			// 只有一项
			rp.setPageSize(1);
			rp.gotoPage(1);
			return Collections.singletonList(rp.next());
		}

		if (null == c && List.class.isInstance(rp)) {
			// 若rp是List直接返回或subList返回
			List<E> ls = (List<E>) rp;
			return (ls.size() <= limit) ? ls : ls.subList(0, limit);
		}

		List<E> ls;
		if (null == c) {
			// 只好逐项提取填充到List中 FIXME 兼容旧的实现（避免有些旧的不规范代码使用可能会修改list）
			ls = new ArrayList<E>(limit);
			if (limit <= 128) {
				// 限制项在128以内，一页搞掂它
				rp.setPageSize(limit);
				rp.gotoPage(1);
				for (E e : rp) {
					ls.add(e);
				}
			} else {
				// 每页128项的方式提取
				rp.setPageSize(128);
				for (int i = 1; limit > 0 && rp.gotoPage(i); i++) {
					for (E e : rp) {
						ls.add(e);
						if ((--limit) <= 0) {
							break;
						}
					}
				}
			}
		} else {
			ArrayList<E> array = new ArrayList<E>(limit);
			if (limit <= 128) {
				// 限制项在128以内，一页搞掂它
				rp.setPageSize(limit);
				rp.gotoPage(1);
				for (E e : rp) {
					array.add(e);
				}
			} else {
				// 每页128项的方式提取
				rp.setPageSize(128);
				for (int i = 1; limit > 0 && rp.gotoPage(i); i++) {
					for (E e : rp) {
						array.add(e);
						if ((--limit) <= 0) {
							break;
						}
					}
				}
			}
			if (null != c) {
				Collections.sort(array, c);
			}
			ls = array;
		}
		return ls;
	}

	/**
	 * 转为可（foreach）遍历的Iterable
	 * 
	 * @param <E>
	 *            结果项类型
	 * @param rp
	 *            页结果
	 * @return 用于遍历的Iterable
	 */
	static public <E> Iterable<E> toForeach(ResultPage<E> rp) {
		if (rp.getCount() == 0) {
			return rp;
		}
		if (rp.getPage() < 2) {
			if (rp.getCount() <= MAX_SIZE_ON_FOREACH) {
				// 一页搞掂吧
				rp.setPageSize(rp.getCount());
				rp.setPage(1);
				return rp;
			}
			rp.setPage(1);
		}
		if (rp.getPage() == rp.getPageCount()) {
			// 是最后一页了，直接返回
			return rp;
		}
		return new Foreach<E>(rp);
	}

	/**
	 * 遍历ResultPage
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 */
	public static final class Foreach<E> implements Iterable<E>, Iterator<E> {
		final ResultPage<E> m_Result;

		Foreach(ResultPage<E> result) {
			m_Result = result;
		}

		@Override
		public Iterator<E> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			if (m_Result.hasNext()) {
				return true;
			}
			// 转到下一页
			return m_Result.gotoPage(m_Result.getPage() + 1) && m_Result.hasNext();
		}

		@Override
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException("[" + m_Result.getPage() + "," + m_Result.getPageSize() + "/"
						+ m_Result.getCount() + "]" + m_Result);
			}
			return m_Result.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
