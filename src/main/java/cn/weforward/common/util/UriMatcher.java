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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 简单的包含通配符URI匹配器
 * 
 * <pre>
 * URI样例：
 * /abc/*.jspx
 * /abc/**
 * /abc/def/*.jspx
 * /ab-*
 * /ab-**
 * </pre>
 * 
 * @author liangyi
 * 
 */
public class UriMatcher {
	/** 匹配任何路径（优先级最低） */
	public static final String ANY = "**";

	/** 要匹配的列表 */
	protected List<String> m_Uris;
	/** 匹配任何“**” */
	protected String m_Any;

	static final Comparator<String> _comp_string = new Comparator<String>() {
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
	};

	public UriMatcher() {
		m_Uris = Collections.emptyList();
	}

	public UriMatcher(Collection<String> uris) {
		setUris(uris);
	}

	public void setUris(Collection<String> uris) {
		if (null == uris || 0 == uris.size()) {
			m_Uris = Collections.emptyList();
			return;
		}
		String array[] = new String[uris.size()];
		array = uris.toArray(array);
		Arrays.sort(array, _comp_string);
		if (ANY.equals(array[0])) {
			// 有匹配任意
			m_Any = ANY;
			m_Uris = Arrays.asList(Arrays.copyOfRange(array, 1, array.length));
		} else {
			m_Uris = Arrays.asList(array);
		}
	}

	/**
	 * 确定URI是否在列表中
	 * 
	 * @param uri
	 *            访问路径
	 * @return 若有匹配项，返回匹配项，否则null
	 */
	public String match(String uri) {
		String ret = match(m_Uris, uri);
		if (null == ret) {
			return m_Any;
		}
		return ret;
	}

	/**
	 * 确定URI是否在列表中
	 * 
	 * @param uris
	 *            路径表
	 * @param uri
	 *            访问路径
	 * @return 若有匹配项，返回匹配项，否则null
	 */
	static public String match(List<String> uris, String uri) {
		if (null == uris || uris.size() == 0) {
			return null;
		}
		// 最泛的匹配
		String startsWith = null;
		// 在验证列表中找，若有，则需要检查
		int i;
		// 找到最后字符为“*”或完全匹配的项
		for (i = 0; i < uris.size(); i++) {
			String name = uris.get(i);
			int c = name.compareTo(uri);
			if (0 == c) {
				// 找到了:)
				return name;
			}
			// 看看是否有“*”号
			int split = name.indexOf('*');
			if (split >= 0) {
				String prefix = name.substring(0, split);
				if (split + 2 == name.length() && '*' == name.charAt(split + 1)
						&& uri.startsWith(prefix)) {
					// 是 xxx** 的格式，好泛啊
					return name;
				}
				// 分为前后缀进行比较
				String suffix = name.substring(split + 1);
				if ((prefix.length() == 0 || uri.startsWith(prefix))
						&& (0 == suffix.length() || uri.endsWith(suffix))) {
					// 前、后缀都匹配到啦，取得匹配的部分
					String match = uri.substring(prefix.length(), uri.length() - suffix.length());
					// 匹配到的部分不能包含“/”，防止类似“/*.jspx”，“/abc/*.jspx”匹配到类似“/abc/c/d.jspx”，“/abc/c/d/xxx.jspx”的问题
					if (-1 == match.indexOf('/')) {
						return name;
					}
				}
			}
			if (c > 0) {
				// 后面的项是不可能合适的啦 :(
				break;
			}
		}
		// 没有精确的匹配允许泛的
		return startsWith;
	}
}
