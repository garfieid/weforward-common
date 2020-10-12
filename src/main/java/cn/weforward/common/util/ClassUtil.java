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
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class处理类
 * 
 * @author daibo
 *
 */
public class ClassUtil {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(ClassUtil.class);

	/** 使用Map来缓存，查表加速 */
	private static volatile Map<Class<?>, String> _SimpleNameCache = Collections.emptyMap();

	/**
	 * 根据名称加载类
	 * 
	 * @param clazz
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<?> forName(String clazz) throws ClassNotFoundException {
		return Class.forName(clazz, true, getDefaultClassLoader());
	}

	/**
	 * 获取默认加载类
	 * 
	 * @return
	 */
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
		}
		if (cl == null) {
			cl = ClassUtil.class.getClassLoader();
			if (cl == null) {
				try {
					cl = ClassLoader.getSystemClassLoader();
				} catch (Throwable ex) {
				}
			}
		}
		return cl;
	}

	/**
	 * 取得某个接口下所有实现这个接口的类
	 * 
	 * @throws IOException
	 */
	public static List<Class<?>> getAllClassByInterface(Class<?> c) throws IOException {
		List<Class<?>> returnClassList = null;
		if (c.isInterface()) {
			// 获取当前的包名
			String packageName = c.getPackage().getName();
			// 获取当前包下以及子包下所以的类
			List<Class<?>> allClass = getClasses(packageName);
			if (allClass != null) {
				returnClassList = new ArrayList<>();
				for (Class<?> classes : allClass) {
					// 判断是否是同一个接口
					if (c.isAssignableFrom(classes)) {
						// 本身不加入进去
						if (!c.equals(classes)) {
							returnClassList.add(classes);
						}
					}
				}
			}
		}
		return returnClassList;
	}

	/**
	 * 取得某一类所在包的所有类名 不含迭代
	 */
	public static String[] getPackageAllClassName(String classLocation, String packageName) {
		// 将packageName分解
		String[] packagePathSplit = packageName.split("[.]");
		String realClassLocation = classLocation;
		int packageLength = packagePathSplit.length;
		for (int i = 0; i < packageLength; i++) {
			realClassLocation = realClassLocation + File.separator + packagePathSplit[i];
		}
		File packeageDir = new File(realClassLocation);
		if (packeageDir.isDirectory()) {
			String[] allClassName = packeageDir.list();
			return allClassName;
		}
		return null;
	}

	/**
	 * 从包package中获取所有的Class
	 * 
	 * @param pack
	 * @return
	 * @throws IOException
	 */
	public static List<Class<?>> getClasses(String packageName) {
		// 第一个class类的集合
		List<Class<?>> classes = new ArrayList<Class<?>>();
		// 是否循环迭代
		boolean recursive = true;
		// 获取包的名字 并进行替换
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
		} catch (IOException e) {
			_Logger.warn("忽略加载异常", e);
			return classes;
		}
		// 循环迭代下去
		while (dirs.hasMoreElements()) {
			// 获取下一个元素
			URL url = dirs.nextElement();
			// 得到协议的名称
			String protocol = url.getProtocol();
			// 如果是以文件的形式保存在服务器上
			if ("file".equals(protocol)) {
				// 获取包的物理路径
				String filePath;
				try {
					filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				} catch (IOException e) {
					_Logger.warn("忽略加载异常", e);
					continue;
				}
				// 以文件的方式扫描整个包下的文件 并添加到集合中
				findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
			} else if ("jar".equals(protocol)) {
				// 如果是jar包文件
				// 定义一个JarFile
				JarFile jar;
				// 获取jar
				try {
					jar = ((JarURLConnection) url.openConnection()).getJarFile();
				} catch (IOException e) {
					_Logger.warn("忽略加载异常", e);
					continue;
				}
				// 从此jar包 得到一个枚举类
				Enumeration<JarEntry> entries = jar.entries();
				// 同样的进行循环迭代
				while (entries.hasMoreElements()) {
					// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
					JarEntry entry = entries.nextElement();
					String name = entry.getName();
					// 如果是以/开头的
					if (name.charAt(0) == '/') {
						// 获取后面的字符串
						name = name.substring(1);
					}
					// 如果前半部分和定义的包名相同
					if (name.startsWith(packageDirName)) {
						int idx = name.lastIndexOf('/');
						// 如果以"/"结尾 是一个包
						if (idx != -1) {
							// 获取包名 把"/"替换成"."
							packageName = name.substring(0, idx).replace('/', '.');
						}
						// 如果可以迭代下去 并且是一个包
						if ((idx != -1) || recursive) {
							// 如果是一个.class文件 而且不是目录
							if (name.endsWith(".class") && !entry.isDirectory()) {
								// 去掉后面的".class" 获取真正的类名
								String className = name.substring(packageName.length() + 1, name.length() - 6);
								String cname = packageName + '.' + className;
								try {
									// 添加到classes
									classes.add(Class.forName(cname));
								} catch (ClassNotFoundException e) {
									_Logger.warn("忽略加载类异常", e);
								}
							}
						}
					}
				}
			}
		}
		return classes;
	}

	/**
	 * 以文件的形式来获取包下的所有Class
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive,
			List<Class<?>> classes) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		File[] dirfiles = dir.listFiles(new FileFilter() {
			// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
			public boolean accept(File file) {
				return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
			}
		});
		// 循环所有文件
		for (File file : dirfiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
						classes);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0, file.getName().length() - 6);
				try {
					// 添加到集合中去
					classes.add(Class.forName(packageName + '.' + className));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 查找类
	 * 
	 * @param type
	 * @param index
	 * @return
	 */
	public static Class<?> find(Type type, int index) {
		if (type instanceof ParameterizedType) {
			Type[] types = ((ParameterizedType) type).getActualTypeArguments();
			if (null != types && types.length > index) {
				Type t = types[index];
				if (t instanceof Class) {
					return (Class<?>) t;
				} else {
					return find(t, index);
				}
			}
			return null;
		} else {
			return null;
		}
	}

	/**
	 * 取得类用于标识/映射对象的短名（主要代替Class.getSimpleName，性能高于其将近10倍）
	 * 
	 * @param clazz 类，如 com.ourlinc.tern.Metatype
	 * @return 短名，如 Metatype
	 */
	public static final String getSimpleName(Class<?> clazz) {
		String name;
		Map<Class<?>, String> cache = _SimpleNameCache;
		name = cache.get(clazz);
		if (null == name) {
			// synchronized (Metatype.class) {
			name = clazz.getSimpleName();
			cache = new HashMap<Class<?>, String>(cache);
			cache.put(clazz, name);
			_SimpleNameCache = cache;
			// }
		}
		return name;
	}

	/**
	 * 查找类的指定泛型
	 * 
	 * @param clazz                   具体实现类
	 * @param parameterizedSuperclass 类实现的泛型接口
	 * @param typeParamName           泛型名
	 * @return
	 */
	public static Class<?> find(Class<?> clazz, Class<?> parameterizedSuperclass, String typeParamName) {
		final Class<?> thisClass = clazz;
		Class<?> currentClass = thisClass;
		for (;;) {
			if (currentClass.getSuperclass() == parameterizedSuperclass) {
				int typeParamIndex = -1;
				TypeVariable<?>[] typeParams = currentClass.getSuperclass().getTypeParameters();
				for (int i = 0; i < typeParams.length; i++) {
					if (typeParamName.equals(typeParams[i].getName())) {
						typeParamIndex = i;
						break;
					}
				}
				if (typeParamIndex < 0) {
					throw new UnsupportedOperationException("找不到" + thisClass + "对应的泛型" + typeParamName);
				}
				Type genericSuperType = currentClass.getGenericSuperclass();
				if (!(genericSuperType instanceof ParameterizedType)) {
					return Object.class;
				}
				Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();
				Type actualTypeParam = actualTypeParams[typeParamIndex];
				if (actualTypeParam instanceof ParameterizedType) {
					actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
				}
				if (actualTypeParam instanceof Class) {
					return (Class<?>) actualTypeParam;
				}
				if (actualTypeParam instanceof GenericArrayType) {
					Type componentType = ((GenericArrayType) actualTypeParam).getGenericComponentType();
					if (componentType instanceof ParameterizedType) {
						componentType = ((ParameterizedType) componentType).getRawType();
					}
					if (componentType instanceof Class) {
						return Array.newInstance((Class<?>) componentType, 0).getClass();
					}
				}
				if (actualTypeParam instanceof TypeVariable) {
					TypeVariable<?> v = (TypeVariable<?>) actualTypeParam;
					currentClass = thisClass;
					if (!(v.getGenericDeclaration() instanceof Class)) {
						return Object.class;
					}

					parameterizedSuperclass = (Class<?>) v.getGenericDeclaration();
					typeParamName = v.getName();
					if (parameterizedSuperclass.isAssignableFrom(thisClass)) {
						continue;
					} else {
						return Object.class;
					}
				}
				throw new UnsupportedOperationException("找不到" + thisClass + "对应的泛型" + typeParamName);
			}
			currentClass = currentClass.getSuperclass();
			if (currentClass == null) {
				throw new UnsupportedOperationException("找不到" + thisClass + "对应的泛型" + typeParamName);
			}
		}
	}

}