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
import java.util.NoSuchElementException;

/**
 * 能用在并发锁Lock下的单向后进先出（LIFO）链表，只支持在链表头增加项及全部清除项
 * 
 * @param <E> 链表项的类型
 * 
 * @author liangyi
 * 
 */
public class SinglyLinkedLifo<E> implements Iterable<E> {
	/**
	 * 链表节点
	 * 
	 * @author liangyi
	 * 
	 * @param <E> 链表项的类型
	 */
	public static class Node<E> extends SinglyLinked.SinglyLinkedNode<E> {
		/** 下一项 */
		public Node<E> next;

		// /** 项的值 */
		// public final E value;

		public Node(Node<E> next, E value) {
			super(value);
			this.next = next;
			// this.value = value;
			// super(next, value);
		}

		public Node<E> getNext() {
			return (Node<E>) this.next;
		}

		// public E getValue() {
		// return this.value;
		// }
	}

	/** 链表首项 */
	protected volatile Node<E> m_Head;
	/** 链表项数 */
	protected volatile int m_Size;

	/**
	 * 链表项数
	 */
	public int size() {
		return m_Size;
	}

	/**
	 * 链表是否空
	 * 
	 * @return 是则返回true
	 */
	public boolean isEmpty() {
		return (null == m_Head);
	}

	/**
	 * 取得链表首
	 * 
	 * @return 链表不为空返回首项，否则null
	 */
	public Node<E> getHead() {
		return m_Head;
	}

	/**
	 * 在链表首增加项
	 * 
	 * @param value 加入的项
	 * @return 增加后的链表项
	 */
	public Node<E> addHead(E value) {
		final Node<E> node = createNode(m_Head, value);
		++m_Size;
		return m_Head = node;
	}

	// /**
	// * CAS（无锁）方式在链表首增加项
	// *
	// * @param value
	// * 加入的项
	// * @return 增加后的链表项
	// */
	// public Node<E> casAddHead(E value) {
	// Node<E> head = m_Head;
	// final Node<E> node = createNode(head, value);
	// m_Head = node;
	// ++m_Size;
	// return node;
	// }

	/**
	 * 在链表首增加未有的项
	 * 
	 * @param value 加入的项
	 * @return 新增则返回true
	 */
	public boolean addIfAbsent(E value) {
		if (null != find(value)) {
			return false;
		}
		// 创建新的项
		Node<E> n = createNode(m_Head, value);
		++m_Size;
		m_Head = n;
		return true;
	}

	/**
	 * 删除项
	 * 
	 * @param value 待删除的项
	 * @return 链表中有待删除的项在删除后返回true
	 */
	public boolean remove(Object value) {
		Node<E> n = m_Head;
		// 未有任何项
		if (null == n) {
			return false;
		}

		// （链表中）寻找
		while (null != n) {
			if (value.equals(n.value)) {
				// 找到则删除（在此点重建链表）
				Node<E> newly = n.getNext();
				n = m_Head;
				value = n.value;
				while (null != n && n.value != value) {
					newly = createNode(newly, n.value);
					n = n.getNext();
				}
				m_Head = newly;
				--m_Size;
				return true;
			}
			n = n.getNext();
		}
		return false;
	}

	/**
	 * 在链表中查找项
	 * 
	 * @param value 要查找的项
	 * @return 找到则返回其节点，否则返回null
	 */
	public Node<E> find(Object value) {
		if (null == value) {
			return null;
		}
		Node<E> n = m_Head;
		while (null != n) {
			if (value.equals(n.value)) {
				// 已有项
				return n;
			}
			n = n.getNext();
		}
		return null;
	}

	/**
	 * 分离链表（由首项返回且清除链表）
	 * 
	 * @return 链表首
	 */
	public Node<E> detach() {
		final Node<E> first = m_Head;
		m_Head = null;
		m_Size = 0;
		return first;
	}

	/**
	 * 直接把另一个链表插入到首（比较危险的功能）
	 * 
	 * @param linked 插入到链表首的链表
	 * @return 增加前的链表首
	 */
	protected Node<E> insertHead(Node<E> linked) {
		if (null == linked) {
			return m_Head;
		}
		final Node<E> node = m_Head;
		// 遍历linked找到最后项
		Node<E> next;
		for (next = linked; null != next.getNext(); next = next.getNext()) {
			++m_Size;
		}
		next.next = node;
		m_Head = linked;
		return node;
	}

	/**
	 * 分离链表（由首项返回且清除链表）
	 * 
	 * @return 链表以迭代器方式返回
	 */
	public LinkedIterator<E> detachIt() {
		return new LinkedIterator<E>(detach());
	}

	@Override
	public Iterator<E> iterator() {
		return new LinkedIterator<E>(m_Head);
	}

	/**
	 * 把以迭代器方式分离的链表插入到首
	 * 
	 * @param it 以迭代器方式分离的链表
	 */
	public void insertHead(LinkedIterator<E> it) {
		if (null == it) {
			return;
		}
		insertHead((Node<E>) it.detach());
	}

	/**
	 * 迭代器包装的链表
	 *
	 * @author liangyi
	 *
	 * @param <E>
	 */
	static public class LinkedIterator<E> implements Iterator<E> {
		protected Node<E> m_Head;
		protected Node<E> m_Next;
		protected Node<E> m_Previous;

		public LinkedIterator(Node<E> head) {
			m_Head = head;
			m_Next = head;
			m_Previous = null;
		}

		@Override
		public boolean hasNext() {
			return null != m_Next;
		}

		public Node<E> detach() {
			Node<E> head = m_Head;
			m_Head = null;
			m_Next = null;
			m_Previous = null;
			return head;
		}

		@Override
		public E next() {
			Node<E> p = m_Next;
			if (null == p) {
				throw new NoSuchElementException("没有啦");
			}
			m_Next = p.getNext();
			if (p == m_Head) {
				m_Previous = null;
			} else if (null == m_Previous) {
				m_Previous = m_Head;
			} else if (m_Previous.next != p) {
				m_Previous = m_Previous.getNext();
			}
			return p.value;
		}

		@Override
		public void remove() {
			if (m_Head == m_Next || (null != m_Previous && m_Previous == m_Next)) {
				throw new NoSuchElementException("未执行过next吧");
			}
			if (null == m_Previous) {
				// 首项？
				m_Head = m_Next;
			} else {
				m_Previous.next = m_Next;
			}
		}
	}

	/**
	 * 清除链表所有项
	 */
	public void clear() {
		m_Head = null;
		m_Size = 0;
	}

	/**
	 * 创建链表节点
	 * 
	 * @param next  下个节点
	 * @param value 节点值
	 * @return 创建的节点
	 */
	protected Node<E> createNode(Node<E> next, E value) {
		return new Node<E>(next, value);
	}

	@Override
	public String toString() {
		return "{class:\"SLL\",size:" + m_Size + "}";
	}

}
