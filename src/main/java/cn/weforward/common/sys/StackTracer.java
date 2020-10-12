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
package cn.weforward.common.sys;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import cn.weforward.common.execption.Unexpected;

/**
 * 稍优化Stack Trace
 * 
 * @author liangyi
 *
 */
public class StackTracer {
	/**
	 * 简化方式打印指定线程的调用堆栈
	 * 
	 * @param thread
	 *            要dump的线程，若null则为当前线程
	 * @param builder
	 *            要输出的Appendable
	 * @return 打印后的Appendable
	 */
	public static final Appendable printStackTrace(Thread thread, Appendable builder) {
		try {
			if (null == thread || thread == Thread.currentThread()) {
				// Thread.getStackTrace()性能不如Throwable.getStackTrace()，不过jdk1.5已调整
				printStackTrace(new Throwable().getStackTrace(), 1, 0, builder);
			} else {
				printStackTrace(thread.getStackTrace(), 0, 0, builder);
			}
		} catch (IOException ee) {
			throw new Unexpected(ee);
		}
		return builder;
	}

	/**
	 * 简化方式打印指定异常的调用堆栈
	 * 
	 * @param e
	 *            要dump的异常
	 * @param builder
	 *            要输出的Appendable
	 * @return 打印后的Appendable
	 */
	public static final Appendable printStackTrace(Throwable e, Appendable builder) {
		// if (null == e) {
		// return ((null == builder) ? (new StringBuilder()) : builder);
		// }
		if (null == e) {
			return builder;
		}
		Throwable cause = e.getCause();
		StackTraceElement[] stacks;
		try {
			if (null != cause && e != cause) {
				// 使用cause中的调用堆栈，e的只使用前三行
				stacks = e.getStackTrace();
				if (null == builder) {
					builder = new StringBuilder(stacks.length * 64);
				}
				builder.append(e.toString()).append('\n');
				String jarLast = null;
				for (int j = 0; j < stacks.length; j++) {
					StackTraceElement ste = stacks[j];
					String jar = getJarVersion(ste.getClassName());
					builder.append("\tat ").append(ste.toString());
					if (null != jar && !jar.equals(jarLast)) {
						builder.append('[').append(jar).append(']');
						jarLast = jar;
					}
					builder.append('\n');
					if (j >= 4) {
						// 到此为止
						builder.append("\t...\nCaused by:");
						break;
					}
				}
				e = cause;
			}
			stacks = e.getStackTrace();
			if (null == builder) {
				builder = new StringBuilder(stacks.length * 64);
			}
			if (Throwable.class != e.getClass()) {
				// 若是Throwable对象，估计只是为了打印调用堆栈，不输出错误信息
				builder.append(e.toString());
			}
			builder.append('\n');
			printStackTrace(stacks, 0, 0, builder);
		} catch (IOException ee) {
			throw new Unexpected(ee);
		}
		return builder;
	}

	/**
	 * 把堆栈输出到StringBuilder
	 * 
	 * @param stacks
	 *            要输出的堆栈表
	 * @param offset
	 *            开始层（下标）
	 * @param limit
	 *            限制到层（下标）
	 * @param appender
	 *            把堆栈表输出到其后（可null）
	 * @return 输出堆栈表后的Appendable
	 */
	private static final Appendable printStackTrace(StackTraceElement[] stacks, int offset,
			int limit, Appendable appender) throws IOException {
		String jarLast = null;
		if (limit <= 0 || limit > stacks.length) {
			limit = stacks.length;
		}
		if (null == appender) {
			appender = new StringBuilder(limit * 64);
		}
		int j;
		if (offset >= limit) {
			j = (limit > 3) ? (limit - 3) : 0;
		} else {
			j = offset;
		}
		for (; j < limit; j++) {
			StackTraceElement ste = stacks[j];
			String className = ste.getClassName();
			if ("java.lang.reflect.Method".equals(className)
					|| "sun.reflect.NativeConstructorAccessorImpl".equals(className)) {
				// 只到java.lang.reflect.Method.*及sun.reflect.NativeConstructorAccessorImpl.*这部分，去除由反映调用的堆栈部分
				appender.append("\t...\n");
				break;
			}
			appender.append("\tat ").append(ste.toString());// .append('\n');
			if ("_jspService".equals(ste.getMethodName())) {
				// 若是JSP部分，也到此为止
				appender.append("\t...\n");
				break;
			}
			String jar = getJarVersion(ste.getClassName());
			if (null != jar && !jar.equals(jarLast)) {
				appender.append('[').append(jar).append(']');
				jarLast = jar;
			}
			appender.append('\n');
		}
		if (limit > offset && j > limit) {
			appender.append("\t...\n");
		}
		return appender;
	}

	/** JAR版本号缓存 */
	static final Map<String, String> _JarVersions = new HashMap<String, String>();

	/**
	 * 取得指定类所在.jar中MANIFEST.MF标示的版本号
	 * 
	 * @param className
	 *            类名
	 * @return 所在JAR的版本号
	 */
	public static final String getJarVersion(String className) {
		if (className.startsWith("java.") || className.startsWith("sun.")) {
			// 忽略java.xxx及sun.xxx的类
			return "";
		}
		synchronized (_JarVersions) {
			String ver = _JarVersions.get(className);
			if (null != ver) {
				return ver;
			}
			try {
				Class<?> clazz = Class.forName(className);
				ver = getJarVersion0(clazz);
				// } catch (ClassNotFoundException e) {
				// return "";
			} catch (Throwable e) {
				// 置为长度0的字串，下次不再尝试获取
				ver = "";
			}
			_JarVersions.put(className, ver);
			return ver;
		}
	}

	/**
	 * 取得指定类所在.jar中MANIFEST.MF标示的版本号
	 * 
	 * @param clazz
	 *            类
	 * @return 所在JAR的版本号
	 */
	public static final String getJarVersion(Class<?> clazz) throws IOException {
		String className = clazz.getName();
		synchronized (_JarVersions) {
			String ver = _JarVersions.get(className);
			if (null != ver) {
				return ver;
			}
			ver = getJarVersion0(clazz);
			_JarVersions.put(className, ver);
			return ver;
		}
	}

	private static final String getJarVersion0(Class<?> clazz) throws UnsupportedEncodingException {
		if (null == clazz) {
			return "";
		}
		String jar = null;
		try {
			CodeSource cs = clazz.getProtectionDomain().getCodeSource();
			if (null != cs) {
				URL url = cs.getLocation();
				if (null != url) {
					jar = decodeUrl(url.getFile());
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

		String ver = _JarVersions.get(jar);
		if (null != ver) {
			return ver;
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
				// _Logger.warn(clazz + " Not in JAR file: " + jar);
				return "";
			}
			name = name.substring(0, idx);

			JarFile jf = new JarFile(jar);
			Manifest mf = jf.getManifest();
			Attributes attributes = mf.getMainAttributes();
			ver = attributes.getValue("Implementation-Version");
			if (null == ver) {
				ver = "";
			}
			ver = name + ' ' + ver;
			jf.close();
			_JarVersions.put(jar, ver);
			return ver;
		} catch (IOException e) {
			// _Logger.warn("getJarVersion failed. " + clazz + " for " + jar,
			// e);
		}
		return "";
	}

	public static final String getJarManifestVersion(Class<?> clazz) {
		if (null == clazz) {
			return "";
		}
		String jar = null;
		try {
			CodeSource cs = clazz.getProtectionDomain().getCodeSource();
			if (null != cs) {
				URL url = cs.getLocation();
				if (null != url) {
					jar = url.getFile();
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
			JarFile jf = new JarFile(jar);
			Manifest mf = jf.getManifest();
			Attributes attributes = mf.getMainAttributes();
			String ver = attributes.getValue("Implementation-Version");
			if (null == ver) {
				ver = "";
			}
			jf.close();
			return ver;
		} catch (IOException e) {
			// _Logger.warn("getJarManifestVersion failed[" + clazz + "]" +
			// jar);
		}
		return "";
	}

	/**
	 * URL编码解码
	 * 
	 * @param url
	 *            URL串
	 * @return 解码后的字串
	 */
	public static String decodeUrl(String url) throws UnsupportedEncodingException {
		if (null == url || 0 == url.length()) {
			return url;
		}
		return java.net.URLDecoder.decode(url, "UTF-8");
	}
}
