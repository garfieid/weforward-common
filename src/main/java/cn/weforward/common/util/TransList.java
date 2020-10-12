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
import java.util.Collections;
import java.util.List;

/**
 * 将S列表转换成T列表
 * 
 * @author daibo
 * 
 * @param <E>
 * @param <V>
 */
public abstract class TransList<T, S> extends AbstractList<T> {
	/** 原数据 */
	protected List<S> m_Originals;
	/** 缓存数据 */
	protected Object[] m_Caches;
	/** 空数据 */
	protected static Object EMPTY_OBJECT = new Object();

	public TransList(List<S> originals) {
		this(originals, true);
	}

	public TransList(List<S> originals, boolean useCaches) {
		if (null == originals || originals.isEmpty()) {
			m_Originals = Collections.emptyList();
		} else {
			m_Originals = originals;
			if (useCaches) {
				m_Caches = new Object[m_Originals.size()];
			} else {
				m_Caches = null;
			}
		}

	}

	/**
	 * 子类实现此抽象方法提供S->T的封装功能
	 * 
	 * @param src 源条目
	 * @return 目标条目
	 */
	protected abstract T trans(S src);

	@SuppressWarnings("unchecked")
	@Override
	public T get(int index) {
		if (null != m_Caches) {
			Object e = m_Caches[index];
			if (null == e) {
				e = trans(m_Originals.get(index));
				if (null == e) {
					m_Caches[index] = EMPTY_OBJECT;
				} else {
					m_Caches[index] = e;
				}
			}
			return e == EMPTY_OBJECT ? null : (T) e;
		} else {
			return trans(m_Originals.get(index));
		}
	}

	@Override
	public int size() {
		return m_Originals.size();
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
	public static <T, S> List<T> valueOf(List<S> list, final TransList.Trans<T, S> trans) {
		return new TransList<T, S>(list) {

			@Override
			protected T trans(S item) {
				return trans.trans(item);
			}
		};
	}

}
