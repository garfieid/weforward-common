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

/**
 * 文件工具类
 * 
 * @author daibo
 *
 */
public class FileUtil {
	/**
	 * 取得绝对路径
	 * 
	 * @param path 相对路径，支持”../“
	 * @param root 给相对路径作对照的绝对根路径，或为null则使用System.getProperty("user.dir")
	 * @return 转换后的绝对路径
	 */
	static public String getAbsolutePath(String path, String root) {
		// 使用user.dir作为相对目录的根目录
		path = StringUtil.toString(path);
		char ch = (path.length() > 0) ? path.charAt(0) : ' ';
		// 要注意windows的路径 c:\xxx 之类
		if ('/' != ch && '\\' != ch && (path.length() < 2 || path.charAt(1) != ':')) {
			if (null == root) {
				root = System.getProperty("user.dir");
			}
			String sp = System.getProperty("file.separator");
			if (null == sp || 0 == sp.length()) {
				sp = "/";
			}
			ch = root.charAt(root.length() - 1);
			if ('/' == ch || '\\' == ch || sp.charAt(0) == ch) {
				root = root.substring(0, root.length() - 1);
			}
			// 转换上层路径“../”
			while (path.startsWith("../")) {
				int idx = root.lastIndexOf(sp);
				if (-1 == idx) {
					break;
				}
				root = root.substring(0, idx);
				path = path.substring(3, path.length());
			}
			path = root + sp + path;
		}
		return path;
	}
}
