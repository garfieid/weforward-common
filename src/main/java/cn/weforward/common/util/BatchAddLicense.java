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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * 批量添加License
 * 
 * @author daibo
 *
 */
public class BatchAddLicense {
	/**
	 * 入口
	 * 
	 * @param args 传入项目路径，如里没有就用user.dir
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String dir;
		if (null != args && args.length > 0) {
			dir = args[0];
		} else {
			dir = System.getProperty("user.dir");
		}
		String licensePath = dir + File.separator + "LICENSE";
		String projectPath = dir + File.separator + "src";

		File license = new File(licensePath);
		if (!license.exists() || !license.isFile()) {
			System.out.println("licensePath不存在");
			return;
		}
		File project = new File(projectPath);
		if (!project.exists()) {
			System.out.println("projectPath不存在");
			return;
		}
		String lineSeparator = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("/**");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(license)))) {
			String line;
			while (null != (line = in.readLine())) {
				sb.append(lineSeparator);
				sb.append(" * ");
				sb.append(line);
			}
		}
		sb.append(lineSeparator);
		sb.append(" * ");
		sb.append(lineSeparator);
		sb.append(" */");
		sb.append(lineSeparator);
		byte[] licenseContent = sb.toString().getBytes();
		paddingLicense(project, licenseContent);
	}

	private static void paddingLicense(File project, byte[] licenseContent) throws IOException {
		if (project.isDirectory()) {
			for (File child : project.listFiles()) {
				paddingLicense(child, licenseContent);
			}
		} else if (project.getName().endsWith(".java")) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			boolean first = true;
			try (InputStream in = new FileInputStream(project)) {
				byte[] bs = new byte[1024];
				int l = 0;
				while (-1 != (l = in.read(bs))) {
					if (first) {
						if (new String(bs, 0, l).toString().startsWith("/**")) {
							return;
						}
						first = false;
					}
					bytes.write(bs, 0, l);
				}
			}
			try (OutputStream out = new FileOutputStream(project)) {
				out.write(licenseContent);
				out.write(bytes.toByteArray());
			}
		}

	}
}
