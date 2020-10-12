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
package cn.weforward.common.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.weforward.common.util.StringBuilderPool;

/**
 * <code>JsonArray</code>实现
 * 
 * @author liangyi、zhangpengji
 *
 */
public class SimpleJsonArray implements JsonArray.Appendable {
	List<Object> m_Items;

	public SimpleJsonArray() {
		m_Items = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public SimpleJsonArray(List<? extends Object> items) {
		m_Items = (List<Object>) items;
	}

	public int size() {
		return m_Items.size();
	}

	public Object item(int index) {
		return m_Items.get(index);
	}

	// @Override
	public void add(Object obj) {
		m_Items.add(obj);
	}

	@Override
	public String toString() {
		StringBuilder builder = StringBuilderPool._8k.poll();
		try {
			builder.append('[');
			for (int i = 0; i < size(); i++) {
				Object p = item(i);
				if (null == p) {
					builder.append("null,");
				} else {
					builder.append(p).append(',');
				}
			}
			builder.append(']');
			return builder.toString();
		} finally {
			StringBuilderPool._8k.offer(builder);
		}
	}

	@SuppressWarnings("unchecked")
	static SimpleJsonArray _empty = new SimpleJsonArray(Collections.EMPTY_LIST);

	public static SimpleJsonArray empty() {
		return _empty;
	}
}
