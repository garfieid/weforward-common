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

import java.util.Iterator;

import cn.weforward.common.ResultPage;

/**
 * 封装ResultPage[S]为ResultPage[T]的抽象类（供需要的子类继承）
 * 
 * @param T 分页结果目标对象
 * @param S 分页结果源对象
 * @version V1.0
 * @author daibo
 */
public abstract class TransResultPage<T, S> implements ResultPage<T> {
	/** 源分页结果对象 */
	final protected ResultPage<S> m_Reference;

	/**
	 * 由ResultPage<S>构造
	 * 
	 * @param rp
	 */
	public TransResultPage(ResultPage<S> rp) {
		m_Reference = rp;
	}

	/**
	 * 子类实现此抽象方法提供S->T的封装功能
	 * 
	 * @param src 源条目
	 * @return 目标条目
	 */
	protected abstract T trans(S src);

	public int getCount() {
		return m_Reference.getCount();
	}

	public int getPage() {
		return m_Reference.getPage();
	}

	public int getPageCount() {
		return m_Reference.getPageCount();
	}

	public int getPageSize() {
		return m_Reference.getPageSize();
	}

	public boolean gotoPage(int page) {
		return m_Reference.gotoPage(page);
	}

	public boolean hasPrev() {
		return m_Reference.hasPrev();
	}

	public T move(int pos) {
		return trans(m_Reference.move(pos));
	}

	public T prev() {
		return trans(m_Reference.prev());
	}

	public void setPageSize(int size) {
		m_Reference.setPageSize(size);
	}

	public boolean hasNext() {
		return m_Reference.hasNext();
	}

	public T next() {
		return trans(m_Reference.next());
	}

	public void remove() {
		m_Reference.remove();
	}

	public Iterator<T> iterator() {
		return this;
	}

	@Override
	public void setPage(int page) {
		m_Reference.setPage(page);
	}

	@Override
	public String toString() {
		return String.valueOf(m_Reference);
	}

	/**
	 * 转换接口
	 * 
	 * @author daibo
	 *
	 * @param <E>
	 */
	public static interface Trans<T, S> {
		/** 转换 */
		public T trans(S item);

	}

	/**
	 * 构造list
	 * 
	 * @param <E>
	 * @param list
	 * @param trans
	 * @return
	 */
	public static <T, S> ResultPage<T> valueOf(ResultPage<S> rp, final TransResultPage.Trans<T, S> trans) {
		return new TransResultPage<T, S>(rp) {

			@Override
			protected T trans(S item) {
				return trans.trans(item);
			}
		};
	}
}
