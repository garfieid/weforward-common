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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import cn.weforward.common.ResultPage;
import cn.weforward.common.execption.OverloadException;
import cn.weforward.common.execption.UnsupportedException;

/**
 * 把多个分页结果集合并封装在一起
 * 
 * @author liangyi
 * 
 * @param <E>
 */
public class UnionResultPage<E> implements ResultPage<E> {
	/** 多个分页结果集 */
	protected List<ResultPage<E>> m_Pages;
	/** 用于比较结果项来确定返回位置 */
	protected Comparator<E> m_Comparator;
	/** 总项数 */
	protected int m_Count;
	/** 每页项数 */
	protected int m_PageSize;
	/** 页数 */
	protected int m_PageCount;
	/** 当前页 */
	protected int m_CurrentPage;

	/** 当前页的集合 */
	protected E[] m_Elements;
	/** 当前页的项数 */
	protected int m_Size;
	/** next位置 */
	protected int m_Position;
	/** 最大的连续翻页数（默认10000，0为不限，超过抛出OverloadException异常） */
	static int _max_turning_pages = NumberUtil
			.toInt(System.getProperty("cn.weforward.common.util.UnionResultPage.max_turning_pages", null), 10000);

	/**
	 * 合并多个分页结果集合
	 * 
	 * @param <E>        集合项类型
	 * @param pages      分页集
	 * @param comparator 与页结果排序一致的比较器，为null则只是简单遍历每个页结果
	 * @return 合并后的页结果
	 */
	static public <E> ResultPage<E> union(List<ResultPage<E>> pages, Comparator<E> comparator) {
		if (0 == pages.size()) {
			return ResultPageHelper.empty();
		}
		if (1 == pages.size()) {
			ResultPage<E> one = pages.get(0);
			if (null == one) {
				return ResultPageHelper.empty();
			}
			return one;
		}
		List<ResultPage<E>> fix = new ArrayList<ResultPage<E>>(pages.size());
		int count = 0;
		for (int i = pages.size() - 1; i >= 0; i--) {
			ResultPage<E> rp = pages.get(i);
			if (0 == rp.getCount()) {
				continue; // 略过没有结果的集合
			}

			fix.add(rp);
			count += rp.getCount();
			// rp.gotoPage(1);
		}
		if (0 == fix.size()) {
			return ResultPageHelper.empty();
		}
		if (1 == fix.size()) {
			return fix.get(0);
		}
		return new UnionResultPage<E>(fix, count, comparator);
	}

	protected UnionResultPage(List<ResultPage<E>> pages, int count, Comparator<E> comparator) {
		m_Pages = pages;
		m_Count = count;
		m_Comparator = comparator;
		setPageSize(20);
	}

	public int getCount() {
		return m_Count;
	}

	public int getPage() {
		return m_CurrentPage;
	}

	public int getPageCount() {
		return m_PageCount;
	}

	public int getPageSize() {
		return m_PageSize;
	}

	@SuppressWarnings("unchecked")
	public boolean gotoPage(int page) {
		if (page < 1 || page > m_PageCount || null == m_Pages) {
			return false;
		}
		m_Position = 0;
		m_Size = 0;
		if (null == m_Elements) {
			m_Elements = (E[]) new Object[m_PageSize];
		}
		if (page > m_CurrentPage) {
			if (_max_turning_pages > 0 && page >= m_CurrentPage + _max_turning_pages) {
				// 限制连续翻1W页，很容易弄死系统
				throw new OverloadException("并集结果页连续翻页过多[" + m_CurrentPage + "->" + page + "]");
			}
			// 往下翻页
			while (m_CurrentPage < page) {
				nextPage();
			}
			return true;
		}
		if (1 == page) {
			// 全部页集合先转到第一页
			for (ResultPage<E> e : m_Pages) {
				e.gotoPage(1);
			}
			m_CurrentPage = 0;
			nextPage();
			return true;
		}
		if (page < m_CurrentPage) {
			// 往上翻页，只能先回到第一页，再往下
			gotoPage(1);
			gotoPage(page);
			return true;
		}
		return false;
	}

	/**
	 * 一页页往下翻
	 */
	private void nextPage() {
		// 每页集合去找最合适的项，填充下一页
		m_Size = 0;
		int endOfList = 0;
		while (m_Size < m_Elements.length) {
			E last = null;
			boolean onLast = false;
			ResultPage<E> rpLast = null;
			for (int i = m_Pages.size() - 1; i >= endOfList; i--) {
				ResultPage<E> rp = m_Pages.get(i);
				if (!rp.hasNext() && !rp.gotoPage(rp.getPage() + 1)) {
					// rp已经没结果，把它移到列表最前
					if (i > endOfList) {
						ResultPage<E> swap = m_Pages.get(endOfList);
						m_Pages.set(endOfList, rp);
						m_Pages.set(i, swap);
					}
					++endOfList;
					++i;
					continue;
				}
				if (!rp.hasNext()) {
					continue;
				}
				E e = rp.next();
				// if (null == e) {
				// continue;
				// }
				// 若没指定比较器，直接遍历各集合
				if (null == m_Comparator) {
					last = e;
					rpLast = rp;
					onLast = true;
					break;
				}
				// 指定比较器时，要略过空项
				if (null == last) {
					last = e;
					rpLast = rp;
					onLast = true;
					continue;
				}

				// 看与最后的结果项比较看是否更合适
				if (m_Comparator.compare(e, last) < 0) {
					if (null != rpLast) {
						// 把上个属于稍后的结果项回退
						rpLast.prev();
						// 把它们的位置交换一下（能加速吗？）
						// results.set(i, rpLast);
						// results.set(i + 1, rp);
					}
					last = e;
					rpLast = rp;
					continue;
				}
				// 这个项条件不合适，回退
				rp.prev();
			}
			if (!onLast) {
				break;
			}
			m_Elements[m_Size++] = last;
		}
		++m_CurrentPage;
	}

	public boolean hasPrev() {
		return (m_Position > 0);
	}

	public E move(int pos) {
		if (null != m_Elements && pos >= 0 && pos < m_Size) {
			m_Position = pos;
			return m_Elements[m_Position];
		}
		return null;
	}

	public E prev() {
		return (hasPrev() ? m_Elements[--m_Position] : null);
	}

	public void setPageSize(int size) {
		if (m_PageSize == size) {
			return;
		}
		m_PageSize = size;
		m_Elements = null;
		m_CurrentPage = 0;
		m_Position = 0;
		size = 0;

		// 计算总页面数
		m_PageCount = m_Count / m_PageSize;
		if ((m_Count % m_PageSize) > 0) {
			++m_PageCount;
		}
	}

	public boolean hasNext() {
		return (null != m_Elements && m_Position < m_Size);
	}

	public E next() {
		return (hasNext() ? m_Elements[m_Position++] : null);
	}

	public void remove() {
		throw new UnsupportedException("此功能不支持");
	}

	public Iterator<E> iterator() {
		return this;
	}

	public void sort(Comparator<E> c, int limit) {
		throw new UnsupportedException("此功能不支持：" + c);
	}

	public void setPage(int page) {
		gotoPage(page);
	}

	@Override
	public String toString() {
		return "{c:" + getCount() + ",ps:" + m_PageSize + ",pc:" + m_PageCount + ",p:" + m_CurrentPage + ",pos:"
				+ m_Position + ",unions:" + (null == m_Pages ? 0 : m_Pages.size()) + "}";
	}
}
