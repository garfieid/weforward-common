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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import cn.weforward.common.util.ClassUtil;

/***
 * 编译java文件
 * 
 * @author daibo
 *
 */
public class CompilerJavaUtil {
	/** 编译器 */
	private static JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

	/**
	 * 编译
	 * 
	 * @param srcs        源文件
	 * @param outs        输文件
	 * @param diagnostics
	 * @return
	 * @throws IOException
	 */
	public static boolean complier(File src, File out, DiagnosticListener<? super JavaFileObject> diagnostics)
			throws IOException {
		URLClassLoader loader = new URLClassLoader(new URL[] {}, ClassUtil.getDefaultClassLoader());
		return complier(loader, src, out, diagnostics);
	}

	/**
	 * 编译
	 * 
	 * @param srcs        源文件
	 * @param outs        输文件
	 * @param diagnostics 诊断程序监听
	 * @return
	 * @throws IOException
	 */
	public static boolean complier(URLClassLoader loader, File src, File out,
			DiagnosticListener<? super JavaFileObject> diagnostics) throws IOException {
		List<File> outfiles = Collections.singletonList(out);
		JavaFileManager fileManager = null;
		try {
			StandardJavaFileManager standardJavaFileManager = COMPILER.getStandardFileManager(diagnostics, null,
					Charset.forName("UTF-8"));
			standardJavaFileManager.setLocation(StandardLocation.CLASS_OUTPUT, outfiles);
			Iterable<? extends JavaFileObject> compilationUnits = standardJavaFileManager.getJavaFileObjects(src);
			fileManager = new CustomClassloaderJavaFileManager(loader, standardJavaFileManager);
			JavaCompiler.CompilationTask task = COMPILER.getTask(null, fileManager, diagnostics, null, null,
					compilationUnits);
			return task.call();
		} finally {
			if (null != fileManager) {
				fileManager.close();
			}
		}

	}

	/** 清理文件夹 */
	public static File clean(File outfile) throws IOException {
		if (outfile.exists()) {
			delete(outfile);
		}
		if (!outfile.mkdirs()) {
			throw new IOException("无法创建[" + outfile + "]");
		}
		return outfile;
	}

	/** 删除文件 */
	public static void delete(File file) throws IOException {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delete(f);
			}
			boolean isSuccess = file.delete();
			if (!isSuccess) {
				throw new IOException("删除" + file.getAbsolutePath() + "失败");
			}
		} else {
			boolean isSuccess = file.delete();
			if (!isSuccess) {
				throw new IOException("删除" + file.getAbsolutePath() + "失败");
			}
		}
	}

}
