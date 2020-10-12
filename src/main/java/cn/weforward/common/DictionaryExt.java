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
package cn.weforward.common;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * 扩展Dictionary，提供索引等接口
 * 
 * @author zhangpengji
 *
 */
public interface DictionaryExt<K, V> extends Dictionary<K, V> {

	/**
	 * 总项数
	 * 
	 * @return
	 */
	int size();

	/**
	 * 字典的索引集
	 * 
	 * @return
	 */
	Enumeration<K> keys();

	/**
	 * 无内容
	 */
	public final static DictionaryExt<?, ?> _Empty = new DictionaryExt<Object, Object>() {
		@Override
		public Object get(Object key) {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Enumeration<Object> keys() {
			return Collections.emptyEnumeration();
		}
	};

	/**
	 * 直接封装Map
	 * 
	 * @author liangyi
	 */
	public static class WrapMap<K, V> implements DictionaryExt<K, V> {
		Map<K, V> map;

		public WrapMap(Map<K, V> map) {
			this.map = map;
		}

		@Override
		public V get(K key) {
			return map.get(key);
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public Enumeration<K> keys() {
			return Collections.enumeration(map.keySet());
		}

	}
}
