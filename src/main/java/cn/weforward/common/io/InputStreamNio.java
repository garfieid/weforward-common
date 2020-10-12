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
import java.nio.ByteBuffer;

/**
 * 增强的输入流（支持ByteBuffer）
 * 
 * @author liangyi
 *
 */
public interface InputStreamNio {
	/**
	 * 读取
	 * 
	 * @param dst
	 *            读取到的缓冲区
	 * @return 读取到的字节数
	 * @throws IOException
	 */
	int read(ByteBuffer dst) throws IOException;

	/**
	 * 读取
	 * 
	 * @param buffer
	 *            缓冲区
	 * @param off
	 *            缓冲区开始位置
	 * @param len
	 *            缓冲区的空间
	 * @return 读取到的字节数
	 * @throws IOException
	 */
	int read(byte buffer[], int off, int len) throws IOException;

	/**
	 * 当前马上可读取的数据（很可能是0）
	 * 
	 * @throws IOException
	 */
	int available() throws IOException;

	/**
	 * 关闭流
	 * 
	 * @throws IOException
	 */
	void close() throws IOException;

	/**
	 * 产生同样内容的副本流，与主体读取互不影响
	 * 
	 * @return 其副本
	 * @throws IOException
	 */
	InputStreamNio duplicate() throws IOException;
}
