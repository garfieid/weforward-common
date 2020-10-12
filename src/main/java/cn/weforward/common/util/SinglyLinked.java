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

import cn.weforward.common.execption.UnsupportedException;

/**
 * 简单的单向链表，能使用Lock加锁来保证链表的一致性
 * 
 * @param <E> 链表项的类型
 * 
 * @author liangyi
 * 
 */
public class SinglyLinked<E> implements Iterable<E> {
	/**
	 * 链表节点
	 * 
	 * @author liangyi
	 * 
	 * @param <E> 链表项的类型
	 */
	public static abstract class SinglyLinkedNode<E> {
		// /** 下一项 */
		// volatile public SinglyLinkedNode<E> next;
		/** 项的值 */
		public final E value;

		// public SinglyLinkedNode(SinglyLinkedNode<E> next, E value) {
		// this.next = next;
		// this.value = value;
		// }

		public SinglyLinkedNode(E value) {
			// this.next = next;
			this.value = value;
		}

		abstract public SinglyLinkedNode<E> getNext();

		// abstract protected void setNext(SinglyLinkedNode<E> next);

		public E getValue() {
			return this.value;
		}
	}

	/**
	 * 链表节点
	 * 
	 * @author liangyi
	 * 
	 * @param <E> 链表项的类型
	 */
	public static class Node<E> extends SinglyLinkedNode<E> {
		/** 下一项 */
		volatile public Node<E> next;

		// /** 项的值 */
		// public final E value;

		public Node(Node<E> next, E value) {
			super(value);
			this.next = next;
			// this.value = value;
		}

		public Node<E> getNext() {
			return this.next;
		}

		// @Override
		// protected void setNext(SinglyLinkedNode<E> next) {
		// this.next = next;
		// }

		// public E getValue() {
		// return this.value;
		// }
	}

	/** 链表首项 */
	volatile protected Node<E> m_Head;
	/** 链表末项 */
	volatile protected Node<E> m_Tail;
	/** 链表项数 */
	volatile protected int m_Size;

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

	@Override
	public Iterator<E> iterator() {
		return new LinkedIterator<E>(m_Head);
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
	 * 由链表首删除
	 * 
	 * @return 返回删除的项，若链表空则返回null
	 */
	public Node<E> removeHead() {
		Node<E> first = m_Head;
		if (null != first) {
			m_Head = first.next;
			if (null == m_Head) {
				m_Tail = null;
			}
			--m_Size;
		}
		return first;
	}

	/**
	 * 由链表尾删除
	 * 
	 * @return 返回删除的项，若链表空则返回null
	 */
	public Node<E> removeTail() {
		Node<E> last = m_Tail;
		if (null != last) {
			if (last == m_Head) {
				m_Head = m_Tail = null;
			} else {
				for (Node<E> p = m_Head; null != p; p = p.next) {
					if (p.next == last) {
						m_Tail = p.next;
						m_Tail.next = null;
						break;
					}
				}
				if (last == m_Tail) {
					// 晕，链表有问题，由首找不到尾？
					throw new IllegalStateException("链表有问题，由首找不回尾！");
				}
				// if (null == m_Last) {
				// m_First = null;
				// }
			}
			--m_Size;
		}
		return last;
	}

	/**
	 * 在链表首增加项
	 * 
	 * @param value 加入的项
	 * @return 增加后的链表项
	 */
	public Node<E> addHead(E value) {
		final Node<E> first = createNode(m_Head, value);
		++m_Size;
		m_Head = first;
		if (null == m_Tail) {
			m_Tail = first;
		}
		return first;
	}

	/**
	 * 在链表末增加项
	 * 
	 * @param value 加入的项
	 * @return 增加后的链表项
	 */
	public Node<E> addTail(E value) {
		final Node<E> last = createNode(null, value);
		++m_Size;
		if (null == m_Tail) {
			m_Head = last;
		} else {
			m_Tail.next = last;
		}
		m_Tail = last;
		return last;

	}

	/**
	 * 在链表末增加未有的项
	 * 
	 * @param value 加入的项
	 * @return 新增则返回true
	 */
	public boolean addIfAbsent(E value) {
		// Node<E> n = m_First;
		// // 未有任何项
		// if (null == n) {
		// // 创建新的项
		// n = new Node<E>(null, value);
		// ++m_Size;
		// m_First = n;
		// return true;
		// }
		// if (isEmpty()) {
		// addTail(value);
		// return true;
		// }
		// Node<E> n = m_Head;
		//
		// // （链表中）寻找是否已有相同项
		// while (null != n) {
		// if (value.equals(n.value)) {
		// // 已有项
		// return false;
		// }
		// n = n.next;
		// }
		if (null != find(value)) {
			return false;
		}
		// // 创建新的项
		// n = new Node<E>(m_First, value);
		// ++m_Size;
		// m_First = n;
		addTail(value);
		return true;
	}

	/**
	 * 查找链表项
	 * 
	 * @param value 要查询的项
	 * @return 相应的链表项，没找到则返回null
	 */
	public Node<E> find(E value) {
		Node<E> n = m_Head;
		// （链表中）寻找是否已有相同项
		while (null != n) {
			if (value.equals(n.value)) {
				// 已有项
				return n;
			}
			n = n.next;
		}
		return null;
	}

	/**
	 * 删除项
	 * 
	 * @param value 待删除的项
	 * @return 链表中有待删除的项在删除后返回true
	 */
	public boolean remove(E value) {
		Node<E> f = m_Head;
		// 未有任何项
		if (null == f) {
			return false;
		}

		// （链表中）寻找
		Node<E> n = f;
		while (null != n) {
			if (value.equals(n.value)) {
				// 找到则删除（在此点重建链表）
				if (n == m_Head) {
					// 晕，就是第一项
					m_Head = n.next;
					if (null == m_Head) {
						m_Tail = null;
					}
				} else {
					f.next = n.next;
					// if(n==m_Last){
					if (null == f.next) {
						// 最后一项
						m_Tail = f;
					}
				}
				--m_Size;
				return true;
			}
			f = n;
			n = n.next;
		}
		return false;
	}

	/**
	 * 分离链表（由首项返回且清除链表）
	 * 
	 * @return 链表首
	 */
	public Node<E> detach() {
		if (null == m_Head) {
			return null;
		}
		final Node<E> first = m_Head;
		m_Head = m_Tail = null;
		m_Size = 0;
		return first;
	}

	/**
	 * 直接把链附加到链表首（要确保要附加的链是安全的）
	 * 
	 * @param top 要附加的链
	 * @return 加入的项数
	 */
	public int attachToHead(Node<E> top) {
		Node<E> p = top;
		Node<E> last = p;
		int count = 0;
		while (null != p) {
			last = p;
			++count;
			p = p.getNext();
		}
		m_Size += count;
		if (null == m_Tail) {
			m_Tail = last;
		} else {
			last.next = m_Head;
		}
		m_Head = top;
		return count;
	}

	/**
	 * 清除链表所有项
	 */
	public void clear() {
		m_Head = m_Tail = null;
		m_Size = 0;
	}

	@Override
	public String toString() {
		return "{class:\"SL\",size:" + m_Size + "}";
	}

	/**
	 * 迭代器包装的链表
	 * 
	 * @author liangyi
	 * 
	 * @param <E>
	 */
	static public class LinkedIterator<E> implements Iterator<E> {
		protected SinglyLinkedNode<E> m_Head;
		protected SinglyLinkedNode<E> m_Next;
		protected SinglyLinkedNode<E> m_Previous;

		public LinkedIterator(SinglyLinkedNode<E> head) {
			m_Head = head;
			m_Next = head;
			m_Previous = null;
		}

		@Override
		public boolean hasNext() {
			return null != m_Next;
		}

		public SinglyLinkedNode<E> getHead() {
			return m_Head;
		}

		@Override
		public E next() {
			SinglyLinkedNode<E> p = m_Next;
			if (null == p) {
				throw new NoSuchElementException("没有啦");
			}
			m_Next = p.getNext();
			if (p == m_Head) {
				m_Previous = null;
			} else if (null == m_Previous) {
				m_Previous = m_Head;
			} else {
				SinglyLinkedNode<E> prev = m_Previous.getNext();
				if (prev != p) {
					m_Previous = prev;
				}
			}
			return p.value;
		}

		public void remove() {
			throw new UnsupportedException("不支持删除链表项");
		}

		// @Override
		// public void remove() {
		// if (m_Head == m_Next || (null != m_Previous && m_Previous == m_Next))
		// {
		// throw new NoSuchElementException("未执行过next吧");
		// }
		// if (null == m_Previous) {
		// // 首项？
		// m_Head = m_Next;
		// } else {
		// m_Previous.setNext(m_Next);
		// }
		// }
	}
}
