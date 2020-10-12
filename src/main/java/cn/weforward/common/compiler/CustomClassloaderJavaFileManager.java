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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

/**
 * 自定义类加载java文件管理
 * 
 * @author daibo
 *
 */
class CustomClassloaderJavaFileManager implements JavaFileManager {

	private ClassLoader m_ClassLoader;
	private StandardJavaFileManager m_StandardFileManager;
	private PackageInternalsFinder m_Finder;

	public CustomClassloaderJavaFileManager(ClassLoader classLoader, StandardJavaFileManager standardFileManager) {
		m_ClassLoader = classLoader;
		m_StandardFileManager = standardFileManager;
		m_Finder = new PackageInternalsFinder(classLoader);
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return m_ClassLoader;
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
			boolean recurse) throws IOException {
		if (location == StandardLocation.PLATFORM_CLASS_PATH) {
			return m_StandardFileManager.list(location, packageName, kinds, recurse);
		} else if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
			List<JavaFileObject> result = new ArrayList<>();
			for (JavaFileObject v : m_StandardFileManager.list(location, packageName, kinds, recurse)) {
				result.add(v);
			}
			for (JavaFileObject v : m_Finder.find(packageName)) {
				result.add(v);
			}
			return result;
		}
		return m_StandardFileManager.list(location, packageName, kinds, recurse);

	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof CustomJavaFileObject) {
			return ((CustomJavaFileObject) file).binaryName();
		} else {
			return m_StandardFileManager.inferBinaryName(location, file);
		}
	}

	@Override
	public boolean isSameFile(FileObject a, FileObject b) {
		return m_StandardFileManager.isSameFile(a, b);
	}

	@Override
	public boolean handleOption(String current, Iterator<String> remaining) {
		return handleOption(current, remaining);
	}

	@Override
	public boolean hasLocation(Location location) {
		return m_StandardFileManager.hasLocation(location);
	}

	@Override
	public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind)
			throws IOException {
		return m_StandardFileManager.getJavaFileForInput(location, className, kind);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
			FileObject sibling) throws IOException {
		return m_StandardFileManager.getJavaFileForOutput(location, className, kind, sibling);
	}

	@Override
	public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
		return m_StandardFileManager.getFileForInput(location, packageName, relativeName);
	}

	@Override
	public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling)
			throws IOException {
		return m_StandardFileManager.getFileForOutput(location, packageName, relativeName, sibling);
	}

	@Override
	public void flush() throws IOException {
		m_StandardFileManager.flush();
	}

	@Override
	public void close() throws IOException {
		m_StandardFileManager.close();
	}

	@Override
	public int isSupportedOption(String option) {
		return m_StandardFileManager.isSupportedOption(option);
	}

}