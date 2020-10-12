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

import java.util.List;
import java.util.Map;

/**
 * 只读的字典接口
 * 
 * @author zhangpengji
 *
 * @param <K>
 * @param <V>
 */
public interface Dictionary<K, V> {

	/**
	 * 取集合中指定键的项
	 * 
	 * @param key
	 *            项的标识键
	 * @return 相应的项
	 */
	V get(K key);

	/**
	 * Util
	 */
	public final static class Util {
		@SuppressWarnings("unchecked")
		static public <K, V> Dictionary<K, V> empty() {
			return (Dictionary<K, V>) _Empty;
		}

		static public <K, V> Dictionary<K, V> valueOf(Map<K, V> map) {
			if (null == map) {
				throw new NullPointerException("map is <null>");
			}
			return new WrapMap<K, V>(map);

		}

		static public <K, V> Dictionary<K, V> valueOf(KvPair<K, V>[] items) {
			if (null == items || items.length == 0) {
				return empty();
			}
			return new ByArray<K, V>(items);
		}

		@SuppressWarnings("unchecked")
		public static <K, V> Dictionary<K, V> valueOf(List<KvPair<K, V>> list) {
			if (null == list || list.size() == 0) {
				return empty();
			}
			return new ByArray<K, V>((KvPair<K, V>[]) list.toArray());
		}
	}

	/**
	 * 无内容
	 */
	public final static Dictionary<?, ?> _Empty = new Dictionary<Object, Object>() {
		@Override
		public Object get(Object key) {
			return null;
		}
	};

	/**
	 * 直接封装Map
	 * 
	 * @author liangyi
	 */
	public static class WrapMap<K, V> implements Dictionary<K, V> {
		Map<K, V> map;

		public WrapMap(Map<K, V> map) {
			this.map = map;
		}

		@Override
		public V get(K key) {
			return map.get(key);
		}
	}

	/**
	 * 封装KvPair数组
	 * 
	 * @author liangyi
	 *
	 * @param <K>
	 * @param <V>
	 */
	public static class ByArray<K, V> implements Dictionary<K, V> {
		KvPair<K, V>[] items;

		public ByArray(KvPair<K, V>[] items) {
			this.items = items;
		}

		@Override
		public V get(K key) {
			KvPair<K, V> kv;
			for (int i = items.length - 1; i >= 0; i--) {
				kv = items[i];
				if (key.equals(kv.getKey())) {
					return kv.getValue();
				}
			}
			return null;
		}
	}
}
