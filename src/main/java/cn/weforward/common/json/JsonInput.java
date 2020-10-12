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
package cn.weforward.common.json;

import java.io.IOException;

/**
 * 输入JSON格式字串流
 * 
 * @author liangyi
 *
 */
public interface JsonInput {
	/**
	 * 返回当前可读的字符数
	 * 
	 * @return 返回0表示未知，返回-1表示已经结束，其它>0的值为当前可读
	 */
	int available() throws IOException;

	/**
	 * 读取一个字符
	 * 
	 * @throws IOException
	 */
	char readChar() throws IOException;

	/**
	 * 已读取到的位置
	 */
	int position();

	/**
	 * 关闭
	 * 
	 * @throws IOException
	 */
	void close() throws IOException;
}
