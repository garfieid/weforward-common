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

import cn.weforward.common.util.NumberUtil;

/**
 * 简单地提供一个name/value|id对照的项
 * 
 * 通常用于定义状态等值，通过findById/getNameById/findByValue/getNameByValue等取得对照
 * 
 * @version V1.0
 * 
 * @author liangyi
 */
public class NameItem {
	/** 项名 */
	public final String name;
	/** 项值 */
	public final Object value;
	/** 项ID */
	public final int id;
	/** 未知（-,0） */
	final static public NameItem _unknown_0 = new NameItem("-", 0, "-");
	/** 未知（-,-1） */
	final static public NameItem _unknown_01 = new NameItem("-", -1, "-");

	final static NameItem[] _cache = { _unknown_0, _unknown_01 };

	/**
	 * 取得name/id的对照
	 * 
	 * @param name 对照名
	 * @param id   对照id
	 * @return 对照项
	 */
	static public NameItem valueOf(String name, int id) {
		for (NameItem ni : _cache) {
			if (ni.id == id && ni.name.equals(name)) {
				return ni;
			}
		}
		return new NameItem(name, id, String.valueOf(id));
	}

	/**
	 * 取得name/id的对照
	 * 
	 * @param name 对照名
	 * @param id   对照id
	 * @return 对照项
	 */
	static public NameItem valueOf(String name, int id, Object value) {
		return new NameItem(name, id, value);
	}

	/**
	 * 取得name/value的对照
	 * 
	 * @param name  对照名
	 * @param value 对照值
	 * @return 对照项
	 */
	static public NameItem valueOf(String name, String value) {
		return new NameItem(name, value);
	}

	/**
	 * 由ID查询在items中的项
	 * 
	 */
	static public NameItem findById(int id, Iterable<NameItem> items) {
		for (NameItem item : items) {
			if (item.id == id)
				return item;
		}
		return null;
	}

	/**
	 * 由ID查询在items中的项
	 * 
	 */
	static public NameItem findById(int id, NameItem[] items) {
		for (NameItem item : items) {
			if (item.id == id)
				return item;
		}
		return null;
	}

	/**
	 * 由ID查询到相应项的名称，没找到则返回null
	 * 
	 */
	static public String getNameById(int id, NameItem[] items) {
		NameItem ni = findById(id, items);
		return (null == ni) ? null : ni.name;
	}

	/**
	 * 由ID查询到相应项的名称，没找到则返回null
	 * 
	 */
	static public String getNameById(int id, Iterable<NameItem> items) {
		NameItem ni = findById(id, items);
		return (null == ni) ? null : ni.name;
	}

	/**
	 * 由value查询在items中的项
	 * 
	 */
	static public NameItem findByValue(String value, Iterable<NameItem> items) {
		for (NameItem item : items) {
			if (item.value.equals(value))
				return item;
		}
		return null;
	}

	/**
	 * 由value查询在items中的项
	 * 
	 */
	static public NameItem findByValue(String value, NameItem[] items) {
		for (NameItem item : items) {
			if (item.value.equals(value))
				return item;
		}
		return null;
	}

	/**
	 * 由value查询到相应项的名称，没找到则返回null
	 * 
	 */
	static public String getNameByValue(String value, NameItem[] items) {
		NameItem ni = findByValue(value, items);
		return (null == ni) ? null : ni.name;
	}

	/**
	 * 由value查询到相应项的名称，没找到则返回null
	 * 
	 */
	static public String getNameByValue(String value, Iterable<NameItem> items) {
		NameItem ni = findByValue(value, items);
		return (null == ni) ? null : ni.name;
	}

	/**
	 * 由name查询在items中的项
	 * 
	 */
	static public NameItem findByName(String name, Iterable<NameItem> items) {
		for (NameItem item : items) {
			if (item.name.equalsIgnoreCase(name))
				return item;
		}
		return null;
	}

	/**
	 * 由name查询在items中的项
	 * 
	 */
	static public NameItem findByName(String name, NameItem[] items) {
		for (NameItem item : items) {
			if (item.name.equalsIgnoreCase(name))
				return item;
		}
		return null;
	}

	/**
	 * 由name查询到相应项的value，没找到则返回null
	 * 
	 */
	static public Object getValueByName(String name, NameItem[] items) {
		NameItem ni = findByName(name, items);
		return (null == ni) ? null : ni.value;
	}

	/**
	 * 由name查询到相应项的value，没找到则返回null
	 * 
	 */
	static public Object getValueByName(String name, Iterable<NameItem> items) {
		NameItem ni = findByName(name, items);
		return (null == ni) ? null : ni.value;
	}

	/**
	 * 
	 * @param name  对照名
	 * @param value 对照值
	 */
	protected NameItem(String name, String value) {
		int id = Integer.MIN_VALUE;
		if (null != value && value.length() > 0) {
			char ch = value.charAt(0);
			if (ch >= '0' && ch <= '9') {
				try {
					id = NumberUtil.toInt(value);
				} catch (NumberFormatException e) {
				}
			}
		}
		this.id = id;
		this.name = name;
		this.value = value;
	}

	/**
	 * 
	 * @param name  对照名
	 * @param value 对照值
	 */
	protected NameItem(String name, Object value) {
		this.name = name;
		this.value = value;
		if (value instanceof Integer) {
			this.id = (Integer) value;
		} else {
			this.id = 0;
		}
	}

	/**
	 * 构造
	 * 
	 * @param name  对照名
	 * @param id    对照id
	 * @param value 对照值
	 */
	public NameItem(String name, int id, Object value) {
		this.name = name;
		this.value = value;
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return (String) value;
	}

	/**
	 * 更改名称
	 * 
	 * @param name 要变更的名称
	 * @return 变更后的对照项
	 */
	public NameItem changeName(String name) {
		return new NameItem(name, this.id, this.value);
	}

	/**
	 * 更改ID
	 * 
	 * @param id 要变更的ID
	 * @return 变更后的对照项
	 */
	public NameItem changeId(int id) {
		return new NameItem(this.name, id, this.value);
	}

	/**
	 * 更改值
	 * 
	 * @param value 要变更的值
	 * @return 变更后的对照项
	 */
	public NameItem changeValue(Object value) {
		return new NameItem(this.name, this.id, value);
	}

	@Override
	public String toString() {
		if (null == value) {
			return name + "[" + id + "]";
		}
		return name + "[" + id + "]=" + value;
	}

}
