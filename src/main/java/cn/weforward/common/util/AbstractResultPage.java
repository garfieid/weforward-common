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

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.weforward.common.ResultPage;

/**
 * 结果分页基础类，子类只需实现get抽象方法即可
 * 
 * @version V1.0
 * 
 * @author liangyi
 */
public abstract class AbstractResultPage<E> implements ResultPage<E> {
	/** 每页项数 */
	protected int m_PageSize = 50;
	/** 总页数 */
	protected int m_PageCount = -1;
	/** 当前页 */
	protected int m_CurrentPage;
	/** 当前项位置 */
	protected int m_CurrentPos;
	/** 当前页第一项的位置 */
	protected int m_CurrentPageEnd;
	/** 当前页最后一项的位置 */
	protected int m_CurrentPageBegin;
	/** 标记next的计数 */
	protected int m_NextCount = -1;

	/**
	 * 由下标取得对象（需要子类实现）
	 * 
	 * @param idx
	 *            在结果中的第N项（0~getCount()-1）
	 * @return 结果项
	 */
	protected abstract E get(int idx);

	/**
	 * 若nextCount>=在每次next时调用其，且直至返回flase
	 * 
	 * @param nextCount
	 *            最新next调用次数
	 * @return 返回true则保持每次调用，返回false则结束不再调用
	 */
	protected boolean onCountNext(int nextCount) {
		return false;
	}

	protected AbstractResultPage() {
	}

	protected AbstractResultPage(int nextCount) {
		m_NextCount = nextCount;
	}

	public int getPageCount() {
		if (-1 == m_PageCount) {
			reinit();
		}
		return m_PageCount;
	}

	public int getPageSize() {
		return m_PageSize;
	}

	public void setPage(int page) {
		if (!gotoPage(page)) {
			throw new IndexOutOfBoundsException("over page " + page + " at 1~" + getPageCount());
		}
	}

	public boolean gotoPage(int page) {
		if (page < 1 || page > getPageCount()) {
			return false;
		}

		// 计算页开始位置
		m_CurrentPageBegin = m_CurrentPos = (page - 1) * getPageSize();
		if (page == m_CurrentPage) {
			return true;
		}

		m_CurrentPage = page;
		// 计算页结束位置
		m_CurrentPageEnd = m_CurrentPos + getPageSize();
		if (m_CurrentPageEnd > getCount()) {
			m_CurrentPageEnd = getCount();
		}

		return true;
	}

	/**
	 * 检查是否超出页的最大项数
	 * 
	 * @param size
	 *            每页项数
	 */
	protected void checkPageSize(int size) {
		// 子类重载
	}

	public void setPageSize(int size) {
		checkPageSize(size);
		if (m_PageSize != size && size > 0) {
			m_PageSize = ((-1 == size) ? getCount() : size);
			reinit();
		}
	}

	public boolean hasNext() {
		return (m_CurrentPos < m_CurrentPageEnd);
	}

	public E next() {
		if (hasNext()) {
			if (m_NextCount >= 0 && !onCountNext(++m_NextCount)) {
				// 若onCountNext返回false，把nextCount置为-2，且不再调用onCountNext
				m_NextCount = -2;
			}
			return this.get(m_CurrentPos++);
		}
		throw new NoSuchElementException("访问越界：" + m_CurrentPos + "/" + getCount());
		// return (hasNext() ? this.get(m_CurrentPos++) : null);
	}

	public void remove() {
		throw new UnsupportedOperationException("此功能不支持");
	}

	public boolean hasPrev() {
		return (m_CurrentPos > m_CurrentPageBegin);
	}

	public E prev() {
		return (hasPrev() ? this.get(--m_CurrentPos) : null);
	}

	public E move(int pos) {
		if (pos >= 0 && pos < (m_CurrentPageEnd - m_CurrentPageBegin)) {
			m_CurrentPos = m_CurrentPageBegin + pos;
			return get(m_CurrentPos);
		}
		return null;
	}

	public int getPage() {
		return m_CurrentPage;
	}

	public Iterator<E> iterator() {
		return this;
	}

	public void sort(Comparator<E> c, int limit) {
		// throw UNSUPPORTED;
		throw new UnsupportedOperationException("此功能不支持：" + c);
	}

	/**
	 * （重新初始化且）计算页数
	 */
	protected void reinit() {
		// 计算总页数
		m_PageCount = getCount() / m_PageSize;
		if ((getCount() % m_PageSize) > 0) {
			++m_PageCount;
		}
		m_CurrentPage = 0;
		m_CurrentPos = 0;
		m_CurrentPageBegin = 0;
		m_CurrentPageEnd = 0;
	}

	@Override
	public String toString() {
		return "{c:" + getCount() + ",ps:" + m_PageSize + ",pc:" + m_PageCount + ",p:"
				+ m_CurrentPage + ",idx:" + m_CurrentPos + "}";
	}
}
