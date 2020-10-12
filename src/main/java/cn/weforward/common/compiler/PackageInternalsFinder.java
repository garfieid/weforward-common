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
package cn.weforward.common.compiler;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;

import javax.tools.JavaFileObject;

/**
 * 包查找器
 * 
 * @author daibo
 *
 */
class PackageInternalsFinder {

	private static final String CLASS_FILE_EXTENSION = ".class";

	private ClassLoader m_ClassLoader;

	public PackageInternalsFinder(ClassLoader classLoader) {
		m_ClassLoader = classLoader;
	}

	public List<JavaFileObject> find(String packageName) throws IOException {
		String javaPackageName = packageName.replaceAll("\\.", "/");
		List<JavaFileObject> result = new ArrayList<JavaFileObject>();
		Enumeration<URL> urlEnumeration = m_ClassLoader.getResources(javaPackageName);
		while (urlEnumeration.hasMoreElements()) {
			URL packageFolderURL = urlEnumeration.nextElement();
			result.addAll(listUnder(packageName, packageFolderURL));
		}
		return result;
	}

	private Collection<JavaFileObject> listUnder(String packageName, URL packageFolderURL) {
		File directory = new File(packageFolderURL.getFile());
		if (directory.isDirectory()) {
			return processDir(packageName, directory);
		} else {
			return processJar(packageFolderURL);
		}
	}

	private List<JavaFileObject> processJar(URL packageFolderURL) {
		List<JavaFileObject> result = new ArrayList<JavaFileObject>();
		try {
			String e = packageFolderURL.toExternalForm();
			int index = e.lastIndexOf("!");
			String jarUri = e.substring(0, index);
			JarURLConnection jarConn = (JarURLConnection) packageFolderURL.openConnection();
			String rootEntryName = jarConn.getEntryName();
			int rootEnd = rootEntryName.length() + 1;
			Enumeration<JarEntry> entryEnum = jarConn.getJarFile().entries();
			while (entryEnum.hasMoreElements()) {
				JarEntry jarEntry = entryEnum.nextElement();
				String name = jarEntry.getName();
				if (name.startsWith(rootEntryName) && name.indexOf('/', rootEnd) == -1) {
					URI uri = URI.create(jarUri + "!/" + name);
					if (name.endsWith(CLASS_FILE_EXTENSION)) {
						String binaryName = name.replaceAll("/", ".");
						binaryName = binaryName.replaceAll(CLASS_FILE_EXTENSION + "$", "");
						result.add(new CustomJavaFileObject(binaryName, uri));
					} else if (name.endsWith(".jar")) {
						processJar(uri.toURL());
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("处理包异常:" + e.getMessage(), e);
		}
		return result;
	}

	private List<JavaFileObject> processDir(String packageName, File directory) {
		List<JavaFileObject> result = new ArrayList<JavaFileObject>();
		File[] childFiles = directory.listFiles();
		for (File childFile : childFiles) {
			if (childFile.isFile()) {
				if (childFile.getName().endsWith(CLASS_FILE_EXTENSION)) {
					String binaryName = packageName + "." + childFile.getName();
					binaryName = binaryName.replaceAll(CLASS_FILE_EXTENSION + "$", "");
					result.add(new CustomJavaFileObject(binaryName, childFile.toURI()));
				}
			}
		}

		return result;
	}
}