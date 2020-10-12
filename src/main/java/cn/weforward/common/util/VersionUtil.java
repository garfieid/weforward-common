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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 版本工具类
 * 
 * @author daibo
 *
 */
public class VersionUtil {
	/** 日志记录器 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(VersionUtil.class);

	private static ConcurrentMap<String, String> _MainVersion = new ConcurrentHashMap<>();
	private static ConcurrentMap<String, String> _ImplementationVersion = new ConcurrentHashMap<>();

	/**
	 * 取得的class所在.jar文件中的版本信息
	 * 
	 * @param clazzs 类列表，如“cn.weforward.common.util.VersionUtil”
	 */
	public static String getVersion(List<String> clazzs) {
		StringBuilder sb = StringBuilderPool._8k.poll();
		try {
			for (String s : clazzs) {
				try {
					Class<?> classOf = Class.forName(s);
					s = getImplementationVersionByJar(classOf);
					if (s.length() > 0) {
						if (sb.length() > 0) {
							sb.append(';');
						}
						sb.append(s);
					}
				} catch (ClassNotFoundException e) {
					_Logger.warn(e.toString(), e);
				}
			}
			return sb.toString();
		} finally {
			StringBuilderPool._8k.offer(sb);
		}
	}

	/**
	 * 从jar中获取主版本
	 * 
	 * @param clazz
	 * @return
	 */
	public static String getMainVersionByJar(Class<?> clazz) {
		String name = clazz.getName();
		String v = _MainVersion.get(name);
		if (null == v) {
			v = getVersionByJar(clazz, "Main-Version");
			String old = _MainVersion.putIfAbsent(name, v);
			if (null != old) {
				v = old;
			}
		}
		return v;
	}

	/**
	 * 从pom.xml获取主版本
	 * 
	 * @return
	 * @throws IOException
	 */
	public static String getMainVersionByPom() {
		File file = new File(FileUtil.getAbsolutePath("pom.xml", null));
		if (file.exists()) {
			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				NodeList list = doc.getElementsByTagName("properties");
				for (int i = 0; i < list.getLength(); i++) {
					Node node = list.item(i);
					NodeList childlist = node.getChildNodes();
					for (int j = 0; j < childlist.getLength(); j++) {
						Node child = childlist.item(j);
						if (StringUtil.eq(child.getNodeName(), "main.version")) {
							return child.getTextContent();
						}
					}
				}

			} catch (Exception e) {
				_Logger.warn("解析xml异常", e);
			}
		}
		return "";
	}

	/**
	 * 从jar中获取实现版本
	 * 
	 * @param clazz
	 * @return
	 */
	public static String getImplementationVersionByJar(Class<?> clazz) {
		String name = clazz.getName();
		String v = _ImplementationVersion.get(name);
		if (null == v) {
			v = getVersionByJar(clazz, "Implementation-Version");
			String old = _ImplementationVersion.putIfAbsent(name, v);
			if (null != old) {
				v = old;
			}
		}
		return v;
	}

	/**
	 * 从jar中获取实现版本
	 * 
	 * @param className
	 * @return
	 */
	public static String getImplementationVersionByJar(String className) {
		String v = _ImplementationVersion.get(className);
		if (null == v) {
			Class<?> clazz;
			try {
				clazz = Class.forName(className);
				v = getImplementationVersionByJar(clazz);
			} catch (ClassNotFoundException e) {
				String old = _ImplementationVersion.putIfAbsent(className, v);
				if (null != old) {
					v = old;
				}
			}
		}
		return v;
	}

	/**
	 * 从pom.xml获取实现版本
	 * 
	 * @return
	 * @throws IOException
	 */
	public static String getImplementationVersionByPom() {
		File file = new File(FileUtil.getAbsolutePath("pom.xml", null));
		if (file.exists()) {
			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				NodeList list = doc.getElementsByTagName("version");
				if (list.getLength() > 0) {
					Node node = list.item(0);
					return node.getTextContent();
				}

			} catch (Exception e) {
				_Logger.warn("解析xml异常", e);
			}
		}
		return "";
	}

	/**
	 * 从jar中获取对应key的属性
	 * 
	 * @param clazz
	 * @param key
	 * @return
	 */
	public static String getVersionByJar(Class<?> clazz, String key) {
		if (null == clazz) {
			return "";
		}
		String jar = null;
		try {
			CodeSource cs = clazz.getProtectionDomain().getCodeSource();
			if (null != cs) {
				URL url = cs.getLocation();
				if (null != url) {
					jar = UrlUtil.decodeUrl(url.getFile());
				}
			}
		} catch (SecurityException e) {
		}
		if (null == jar) {
			return "";
		}
		// 去除 file:
		if (jar.startsWith("file:/")) {
			jar = jar.substring(5);
		}
		// 去除 !com.xx.xx.xx.class
		int idx = jar.indexOf('!');
		if (-1 != idx) {
			jar = jar.substring(0, idx);
		}
		try {
			// 去除路径及.jar部分
			idx = jar.lastIndexOf('/');
			if (-1 == idx) {
				idx = jar.lastIndexOf('\\');
			}
			String name;
			if (-1 != idx) {
				name = jar.substring(idx + 1);
			} else {
				name = jar;
			}
			idx = name.lastIndexOf('.');
			if (-1 == idx) {
				// 应该不是jar文件吧
				_Logger.warn(clazz + " Not in JAR file: " + jar);
				return "";
			}
			name = name.substring(0, idx);
			JarFile jf = new JarFile(jar);
			Manifest mf = jf.getManifest();
			Attributes attributes = mf.getMainAttributes();
			jf.close();
			return StringUtil.toString(attributes.getValue(key));
		} catch (IOException e) {
			_Logger.warn("getJarVersion failed. " + clazz + " for " + jar, e);
		}
		return "";
	}

	/**
	 * 比较两个版本号的大小
	 * 
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static int compareTo(String v1, String v2) {
		if (v1 == v2) {
			return 0;
		}
		if (null == v1) {
			return -1;
		}
		if (null == v2) {
			return 1;
		}
		if (v1.equals(v2)) {
			return 0;
		}
		// 按“.”分隔每段版本号，逐个比较
		int b1 = 0;
		int e1;
		int b2 = 0;
		int e2;
		while (true) {
			e1 = v1.indexOf('.', b1);
			e2 = v2.indexOf('.', b2);

			int c = compareTo(v1, b1, e1, v2, b2, e2);
			if (0 != c) {
				return c;
			}

			if (-1 == e1 || -1 == e2) {
				break;
			}
			b1 = e1 + 1;
			b2 = e2 + 1;
		}
		if (-1 != e1) {
			// v1段落更多
			return 1;
		}
		if (-1 != e2) {
			// v2段落更多
			return -1;
		}
		return 0;
	}

	private static int compareTo(String v1, int b1, int e1, String v2, int b2, int e2) {
		if (-1 == e1) {
			e1 = v1.length();
		}
		if (-1 == e2) {
			e2 = v2.length();
		}
		int len1 = e1 - b1;
		int len2 = e2 - b2;
		if (len1 != len2) {
			return len1 > len2 ? 1 : -1;
		}
		if (len1 != v1.length()) {
			v1 = v1.substring(b1, e1);
		}
		if (len2 != v2.length()) {
			v2 = v2.substring(b2, e2);
		}
		return v1.compareTo(v2);
	}
}
