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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.KvPair;
import cn.weforward.common.execption.OverquotaException;

/**
 * Hash表+LRU，用于支持缓存管理
 * 
 * @author liangyi
 *
 * @param <K>
 *            键的类型（通常是字串）
 * @param <V>
 *            值的类型
 */
public class LruHashMap<K, V> implements Iterable<KvPair<K, V>>, GcCleanable {
	protected final static Logger _Logger = LoggerFactory.getLogger(LruHashMap.class);

	static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
	static final int MAXIMUM_CAPACITY = 1 << 30;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/** hash表 */
	protected Node<K, V>[] m_Table;
	/** 项数 */
	protected volatile int m_Size;
	/** 变化计数 */
	protected volatile int m_ModCount;
	// /** 有更新的操作并发计数 */
	// protected volatile int m_Concurrent;
	/** 扩容表的阀值 */
	protected int m_Threshold;
	/** 扩容因子 */
	protected final float m_LoadFactor;
	/** 限制最大容量 */
	protected int m_MaxCapacity;

	/** LRU链表头 */
	protected final Node<K, V> m_LruHead;
	/** LRU链表尾 */
	protected final Node<K, V> m_LruTail;
	/** 链表高度 */
	protected int m_Height;

	public LruHashMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}
		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		}
		m_MaxCapacity = MAXIMUM_CAPACITY;
		if (initialCapacity > m_MaxCapacity) {
			initialCapacity = m_MaxCapacity;
		}
		m_LoadFactor = loadFactor;
		m_Threshold = tableSizeFor(initialCapacity);
		m_LruHead = newNode(0, null, null, null);
		m_LruTail = newNode(0, null, null, null);
		m_LruTail.putLru(m_LruHead);
	}

	public LruHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public LruHashMap() {
		this(0, DEFAULT_LOAD_FACTOR);
	}

	public int getMaxCapacity() {
		return m_MaxCapacity;
	}

	/**
	 * 限制最大项数
	 * 
	 * @param maxCapacity
	 *            最大项数，若=0为MAXIMUM_CAPACITY
	 */
	public void setMaxCapacity(int maxCapacity) {
		synchronized (tableLock()) {
			if (maxCapacity <= 0 || maxCapacity > MAXIMUM_CAPACITY) {
				m_MaxCapacity = MAXIMUM_CAPACITY;
			} else {
				m_MaxCapacity = maxCapacity;
			}
			Node<K, V>[] oldTab = m_Table;
			int oldCap = (oldTab == null) ? 0 : oldTab.length;
			if (oldCap >= m_MaxCapacity) {
				m_Threshold = Integer.MAX_VALUE;
			}
		}
	}

	/**
	 * 项数
	 */
	public int size() {
		return m_Size;
	}

	public V get(Object key) {
		Node<K, V> e;
		e = getNode(hash(key), key);
		if (null == e) {
			return null;
		}
		afterNodeAccess(e);
		return e.getValue();
	}

	public V put(K key, V value) {
		V old;
		Node<K, V> node = openNode(hash(key), key);
		// try {
		// ++m_Concurrent;
		old = node.setValue(value);
		afterNodeUpdate(node);
		// } finally {
		// --m_Concurrent;
		// }
		return old;
	}

	public V remove(Object key) {
		Node<K, V> e;
		e = removeNode(hash(key), key, null, false);
		return (e == null) ? null : e.value;
	}

	public void removeAll() {
		clear();
	}

	public void clear() {
		Node<K, V>[] tab;
		synchronized (tableLock()) {
			tab = m_Table;
			if (null == tab) {
				return;
			}
			m_ModCount++;
			m_Size = 0;
			m_Table = null;
			m_Threshold = tableSizeFor(DEFAULT_INITIAL_CAPACITY);
		}
		synchronized (lruLock()) {
			initLru();
		}
	}

	/**
	 * 重新初始化LRU
	 */
	protected void initLru() {
		m_LruHead.removeLru();
		m_LruTail.putLru(m_LruHead);
	}

	/**
	 * 尝试修复LRU
	 */
	protected void fixLru() {
		_Logger.warn("LRU断链！" + this);
		initLru();
	}

	/**
	 * 压紧下hash表
	 */
	protected void pinch() {
		if (0 == size() && null != m_Table) {
			clear();
			return;
		}
		// TODO 暂时关掉
		// synchronized (tableLock()) {
		// Node<K, V>[] tab = m_Table;
		// if (null != tab && (tab.length >> 1) > size()) {
		// // 表容量一半比总项数还要多一倍，压紧下？
		// @SuppressWarnings({ "unchecked" })
		// Node<K, V>[] newTab = (Node<K, V>[]) new Node[size()];
		// _Logger.warn("pinch:" + tab.length + "=>" + newTab.length + this);
		// m_Height = 0;
		// rebuild(tab, tab.length, newTab, newTab.length);
		// m_Table = newTab;
		// m_Threshold = tableSizeFor(newTab.length);
		// }
		// }
	}

	@Override
	public void onGcCleanup(int policy) {
		if (0 == size()) {
			if (null != m_Table) {
				clear();
			}
			return;
		}

		if (POLICY_LOW == policy && size() > 16) {
			// 内存低清除1/4缓存项
			if (trim(size() >> 2) > 0) {
				pinch();
			}
			return;
		}
		if (POLICY_CRITICAL == policy && size() > 16) {
			// 内存紧张清除全部缓存项
			clear();
			return;
		}
	}

	/**
	 * 按LRU表移除N项
	 * 
	 * @param expect
	 *            期望移除的项数
	 * @return 已移除的基数
	 */
	public int trim(int expect) {
		int count = 0;
		Node<K, V> p, node;
		int i = 0;
		synchronized (lruLock()) {
			int over = size();
			p = m_LruTail.lruBefore;
			for (; i < over && p != m_LruHead && count < expect; i++) {
				if (null == p) {
					// FIXME 这什么情况？
					fixLru();
					break;
				}
				node = p;
				p = p.lruBefore;
				removeNode(node.hash, node.key, null, false);
				++count;
			}
		}
		if (count > 0) {
			if (0 == size()) {
				clear();
			}
			_Logger.info("{trim:" + count + ",expect:" + expect + "}" + this);
		}
		return count;
	}

	public V getOrDefault(Object key, V defaultValue) {
		Node<K, V> e;
		e = getNode(hash(key), key);
		if (null == e) {
			return defaultValue;
		}
		afterNodeAccess(e);
		return e.getValue();
	}

	public V putIfAbsent(K key, V value) {
		V old;
		Node<K, V> node = openNode(hash(key), key);
		synchronized (node) {
			old = node.getValue();
			if (null == old) {
				node.setValue(value);
				afterNodeUpdate(node);
			}
		}
		return old;
	}

	public boolean remove(Object key, Object value) {
		return removeNode(hash(key), key, value, true) != null;
	}

	// public boolean replace(K key, V oldValue, V newValue) {
	// Node<K, V> e;
	// V v;
	// if ((e = getNode(hash(key), key)) != null
	// && ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
	// e.value = newValue;
	// afterNodeUpdate(e);
	// return true;
	// }
	// return false;
	// }
	//
	// public V replace(K key, V value) {
	// Node<K, V> e;
	// if ((e = getNode(hash(key), key)) != null) {
	// V oldValue = e.value;
	// e.value = value;
	// afterNodeUpdate(e);
	// return oldValue;
	// }
	// return null;
	// }

	protected Object tableLock() {
		return m_LruTail;
	}

	protected Object lruLock() {
		return m_LruHead;
	}

	protected Node<K, V> getNode(int hash, Object key) {
		Node<K, V>[] tab;
		Node<K, V> first, e;
		int n, i;
		// 先定位到节点
		synchronized (tableLock()) {
			tab = m_Table;
			if (null == tab) {
				return null;
			}
			n = tab.length;
			// if(n<=0) {
			// return null;
			// }
			i = (n - 1) & hash;
			first = tab[i];
			if (first == null) {
				return null;
			}
			// if (first.hash == hash && first.key.equals(key)) {
			// return first;
			// }

			// 然后锁节点遍历
			e = first;
			do {
				if (e.hash == hash && e.key.equals(key)) {
					return e;
				}
				e = e.next;
			} while (null != e);
		}
		return null;
	}

	protected Node<K, V> openNode(int hash, K key) {
		Node<K, V>[] tab;
		Node<K, V> p;
		int n, i;
		for (;;) {
			// 先锁表，定位到表项
			synchronized (tableLock()) {
				tab = m_Table;
				if (null == tab || (n = tab.length) == 0 || m_Size > m_Threshold) {
					// 初始化hash table
					tab = resize();
					n = tab.length;
				}
				i = (n - 1) & hash;
				p = tab[i];
				int height = 1;
				while (null != p) {
					if (p.hash == hash && (p.key == key || p.key.equals(key))) {
						// 已有
						return p;
					}
					p = p.next;
					++height;
				}
				if (height > m_Height) {
					// if(height>8) {
					// // XXX 链表长度超过8，转换为红黑树
					// }
					m_Height = height;
				}
				// 新节点
				if (m_Size < m_MaxCapacity) {
					p = newNode(hash, key, null, tab[i]);
					tab[i] = p;
					++m_ModCount;
					++m_Size;
					break;
				}
			}

			// 超过最大项数，通过LRU移除？
			if (trim(1) < 1) {
				throw new OverquotaException("over max capacity:" + size());
			}
		}
		afterNodeInsertion(p);
		return p;
	}

	protected Node<K, V>[] resize() {
		Node<K, V>[] oldTab = m_Table;
		int oldCap = (oldTab == null) ? 0 : oldTab.length;
		int oldThr = m_Threshold;
		int newCap;
		if (oldThr > 0 && 0 == oldCap) {
			newCap = oldThr;
		} else {
			int newThr = 0;
			if (oldCap > 0) {
				if (oldCap >= m_MaxCapacity) {
					// 不再扩展table
					m_Threshold = Integer.MAX_VALUE;
					return oldTab;
				}
				newCap = oldCap << 1;
				if (newCap < m_MaxCapacity && oldCap >= DEFAULT_INITIAL_CAPACITY) {
					newThr = oldThr << 1; // 下次的扩展值2倍化
				}
			} else {
				// 使用默认初始值
				newCap = DEFAULT_INITIAL_CAPACITY;
				newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
			}
			if (newThr == 0) {
				int ft = (int) (newCap * m_LoadFactor);
				newThr = (newCap < m_MaxCapacity && ft < m_MaxCapacity ? ft : Integer.MAX_VALUE);
			}
			m_Threshold = newThr;
		}
		@SuppressWarnings({ "unchecked" })
		Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
		m_Height = 0;
		if (oldTab != null) {
			rebuild(oldTab, oldCap, newTab, newCap);
		}
		m_Table = newTab;
		return newTab;
	}

	protected void rebuild(Node<K, V>[] oldTab, int oldCap, Node<K, V>[] newTab, int newCap) {
		if (_Logger.isTraceEnabled()) {
			_Logger.trace("rebuild:" + oldCap + "/" + newCap + this);
		}
		if (null == newTab) {
			return;
		}
		Node<K, V> e;
		for (int j = 0; j < oldCap; ++j) {
			e = oldTab[j];
			if (null == e) {
				continue;
			}
			oldTab[j] = null;
			if (e.next == null) {
				newTab[e.hash & (newCap - 1)] = e;
				// } else if (e instanceof TreeNode) {
				// ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
			} else {
				Node<K, V> loHead = null, loTail = null;
				Node<K, V> hiHead = null, hiTail = null;
				Node<K, V> next;
				int height = 1;
				do {
					next = e.next;
					if ((e.hash & oldCap) == 0) {
						if (loTail == null) {
							loHead = e;
						} else {
							loTail.next = e;
						}
						loTail = e;
					} else {
						if (hiTail == null) {
							hiHead = e;
						} else {
							hiTail.next = e;
						}
						hiTail = e;
					}
					height++;
				} while ((e = next) != null);
				if (loTail != null) {
					loTail.next = null;
					newTab[j] = loHead;
				}
				if (hiTail != null) {
					hiTail.next = null;
					newTab[j + oldCap] = hiHead;
				}
				if (height > m_Height) {
					m_Height = height;
				}
			}
		}
	}

	final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue) {
		Node<K, V>[] tab;
		Node<K, V> p, e, node = null;
		int n, index;
		synchronized (tableLock()) {
			tab = m_Table;
			if (null == tab) {
				return null;
			}
			n = tab.length;
			// if (n <= 0) {
			// return null;
			// }
			index = (n - 1) & hash;
			p = tab[index];
			if (null == p) {
				return null;
			}
			if (p.hash == hash && p.key.equals(key)) {
				node = p;
			} else if (null != p.next) {
				e = p.next;
				do {
					if (e.hash == hash && e.key.equals(key)) {
						node = e;
						break;
					}
					p = e;
					e = e.next;
				} while (null != e);
			}
			if (null == node) {
				return null;
			}
			if (!matchValue || value == node.value || (value != null && value.equals(node.value))) {
				// 移除
				if (node == p) {
					tab[index] = node.next;
				} else {
					p.next = node.next;
				}
				++m_ModCount;
				--m_Size;
			} else {
				// key匹配，但值不相同
				return null;
			}
		}
		afterNodeRemoval(node);
		return node;
	}

	final int capacity() {
		return (m_Table != null) ? m_Table.length
				: (m_Threshold > 0) ? m_Threshold : DEFAULT_INITIAL_CAPACITY;
	}

	/**
	 * 更新到LRU链表首
	 * 
	 * @param node
	 *            要更新的节点
	 */
	protected void putLru(Node<K, V> node) {
		synchronized (lruLock()) {
			node.putLru(m_LruHead);
		}
	}

	/**
	 * 由LRU表移除
	 * 
	 * @param node
	 *            要由LRU移除的节点
	 */
	protected void removeLru(Node<K, V> node) {
		synchronized (lruLock()) {
			node.removeLru();
		}
	}

	protected Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
		return new Node<K, V>(hash, key, value, next);
	}

	/**
	 * 节点被访问
	 * 
	 * @param p
	 */
	protected void afterNodeAccess(Node<K, V> p) {
		putLru(p);
	}

	/**
	 * 节点被更新
	 * 
	 * @param p
	 */
	protected void afterNodeUpdate(Node<K, V> p) {
		putLru(p);
	}

	/**
	 * 新插入节点
	 */
	protected void afterNodeInsertion(Node<K, V> p) {
		// putLru(p);
	}

	/**
	 * 节点被删除
	 * 
	 * @param p
	 */
	protected void afterNodeRemoval(Node<K, V> p) {
		removeLru(p);
	}

	@Override
	public String toString() {
		return "{size:" + m_Size + ",capacity:" + capacity() + ",threshold:" + m_Threshold + ",mod:"
				+ m_ModCount + ",height:" + m_Height + "}";
	}

	/**
	 * 遍历所有项（不支持remove）
	 */
	public Iterator<KvPair<K, V>> iterator() {
		return new HashIterator();
	}

	/**
	 * 遍历LRU（不支持remove）
	 */
	public Iterator<KvPair<K, V>> lru() {
		return new LruIterator();
	}

	/**
	 * 遍历LRU
	 * 
	 * @author liangyi
	 */
	class LruIterator implements Iterator<KvPair<K, V>> {
		Node<K, V> next;

		LruIterator() {
			next = m_LruTail.lruBefore;
		}

		@Override
		public boolean hasNext() {
			return null != next && next != m_LruHead;
		}

		@Override
		public KvPair<K, V> next() {
			Node<K, V> p = next;
			next = next.lruBefore;
			return p;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * 遍历所有项（不支持remove）
	 * 
	 * @author liangyi
	 */
	class HashIterator implements Iterator<KvPair<K, V>> {
		Node<K, V> next;
		Node<K, V> current;
		int expectedModCount;
		int index;

		HashIterator() {
			expectedModCount = m_ModCount;
			Node<K, V>[] tab = m_Table;
			current = next = null;
			index = 0;
			if (tab != null && m_Size > 0) {
				do {
				} while (index < tab.length && (next = tab[index++]) == null);
			}
		}

		public final boolean hasNext() {
			return next != null;
		}

		final Node<K, V> nextNode() {
			Node<K, V>[] tab;
			Node<K, V> e = next;
			if (m_ModCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			if (e == null) {
				throw new NoSuchElementException();
			}
			if ((next = (current = e).next) == null && (tab = m_Table) != null) {
				do {
				} while (index < tab.length && (next = tab[index++]) == null);
			}
			return e;
		}

		public final void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Node<K, V> next() {
			return nextNode();
		}
	}

	// /**
	// * 检查LRU表
	// */
	// public void checkLru() {
	// // 检查链表是否有不存在的缓存项还在链表中，或链表项小于缓存项
	// int i = 0;
	// int count = 0;
	// int size = size();
	// int over = size * 2;
	// Node<K, V> e;
	// synchronized (lruLock()) {
	// Node<K, V> p = m_LruTail.lruBefore;
	// for (; i < over && p != m_LruHead; i++, p = p.lruBefore) {
	// if (null == p.key) {
	// continue;
	// }
	// e = getNode(hash(p.key), p.key);
	// if (e != p) {
	// // 还真有:<
	// _Logger.warn("LRU残留：" + p);
	// ++count;
	// // e.lruRemove();
	// }
	// }
	// }
	// if (i != size || count > 0) {
	// _Logger.warn("{LRU-check:" + i + ",remain:" + count + "}" + this);
	// } else if (_Logger.isTraceEnabled()) {
	// _Logger.trace("{LRU-check:" + i + "}" + this);
	// }
	// }

	/**
	 * 计算扩展的阀值
	 * 
	 * @param cap
	 *            表容量
	 */
	protected final int tableSizeFor(int cap) {
		if (0 == cap) {
			return 0;
		}
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= m_MaxCapacity) ? m_MaxCapacity : n + 1;
	}

	/**
	 * hash表项
	 * 
	 * @param <K>
	 *            键的类型（通常是字串）
	 * @param <V>
	 *            值的类型
	 * 
	 * @author liangyi
	 */
	protected static class Node<K, V> implements KvPair<K, V> {
		protected final int hash;
		protected final K key;
		protected V value;
		protected Node<K, V> next;
		protected Node<K, V> lruBefore;
		protected Node<K, V> lruAfter;

		protected Node(int hash, K key, V value, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}

		public V setValue(V v) {
			V old = this.value;
			this.value = v;
			return old;
		}

		public final K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public String toString() {
			return key + ":" + value;
		}

		public final int hashCode() {
			return Objects.hashCode(key) ^ Objects.hashCode(value);
		}

		protected void removeLru() {
			if (null != lruBefore) {
				lruBefore.lruAfter = lruAfter;
			}
			if (null != lruAfter) {
				lruAfter.lruBefore = lruBefore;
			}
			lruBefore = null;
			lruAfter = null;
		}

		protected void putLru(Node<K, V> chain) {
			if (chain == lruBefore) {
				// 没变化
				return;
			}
			removeLru();
			lruAfter = chain.lruAfter;
			if (null != lruAfter) {
				lruAfter.lruBefore = this;
			}
			chain.lruAfter = this;
			lruBefore = chain;
		}
	}

	protected static final int hash(Object key) {
		int h;
		return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
	}
}