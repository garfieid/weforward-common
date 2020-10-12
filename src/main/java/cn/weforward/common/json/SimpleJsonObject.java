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

import java.util.Collections;
import java.util.List;

import cn.weforward.common.util.StringBuilderPool;

/**
 * Json对象
 * 
 * @author liangyi
 *
 */
public class SimpleJsonObject extends SimpleJsonArray implements JsonObject.Appendable {

	@SuppressWarnings("unchecked")
	public SimpleJsonObject(List<JsonPair> items) {
		super((List<Object>) (List<?>) items);
	}

	public SimpleJsonObject() {
		super();
	}

	public JsonPair item(int index) {
		return (JsonPair) super.item(index);
	}

	public JsonPair property(String name) {
		JsonPair item;
		for (int i = size() - 1; i >= 0; i--) {
			item = item(i);
			if (name.equalsIgnoreCase(item.getKey())) {
				return item;
			}
		}
		return null;
	}

	public void add(String name, Object value) {
		JsonPair pair = new JsonPair(name, value);
		m_Items.add(pair);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<JsonPair> items() {
		return (Iterable<JsonPair>) (Iterable<?>) m_Items;
	}

	@Override
	public String toString() {
		StringBuilder builder = StringBuilderPool._8k.poll();
		try {
			builder.append('{');
			for (int i = 0; i < size(); i++) {
				JsonPair p = item(i);
				if (null == p) {
					builder.append("null,");
				} else {
					builder.append(p.getKey()).append(':').append(p.getValue()).append(',');
				}
			}
			builder.append('}');
			return builder.toString();
		} finally {
			StringBuilderPool._8k.offer(builder);
		}
	}

	public static SimpleJsonObject empty() {
		return _empty;
	}

	@SuppressWarnings("unchecked")
	static SimpleJsonObject _empty = new SimpleJsonObject((List<JsonPair>) Collections.EMPTY_LIST);
}
