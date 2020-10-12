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
package cn.weforward.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 增强的输出流（支持ByteBuffer）
 * 
 * @author liangyi
 *
 */
public interface OutputStreamNio {
	/**
	 * 写入
	 * 
	 * @param src
	 *            要写入的输入流
	 * @return 写入的字节数
	 * @throws IOException
	 */
	int write(InputStream src) throws IOException;

	/**
	 * 写入
	 * 
	 * @param src
	 *            要写入的数据
	 * @return 写入的字节数
	 * @throws IOException
	 */
	int write(ByteBuffer src) throws IOException;

	/**
	 * 取消且关闭
	 * 
	 * @throws IOException
	 */
	void cancel() throws IOException;

	/**
	 * 写入
	 * 
	 * @param data
	 *            要写入的数据
	 * @param off
	 *            开始位置
	 * @param len
	 *            数据长度
	 * @throws IOException
	 */
	void write(byte data[], int off, int len) throws IOException;

	/**
	 * （写入结束）关闭流
	 * 
	 * @throws IOException
	 */
	void close() throws IOException;

}
