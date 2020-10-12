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

/**
 * 将S迭代器转换成T迭代器
 * 
 * @author daibo
 *
 */
public abstract class TransIterator<T, S> implements Iterator<T> {

	protected Iterator<S> m_Originals;

	public TransIterator(Iterator<S> originals) {
		m_Originals = originals;
	}

	@Override
	public boolean hasNext() {
		return m_Originals.hasNext();
	}

	@Override
	public T next() {
		return trans(m_Originals.next());
	}

	/**
	 * 子类实现此抽象方法提供S->T的封装功能
	 * 
	 * @param src 源条目
	 * @return 目标条目
	 */
	protected abstract T trans(S src);

	@Override
	public void remove() {
		m_Originals.remove();
	}

	@Override
	public String toString() {
		return m_Originals.toString();
	}
}
