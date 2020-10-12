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
package cn.weforward.common.restful;

import java.io.IOException;

/**
 * RESTful API支持
 * 
 * @author liangyi
 * 
 */
public interface RestfulService {

	/**
	 * （可选的）预检查请求是否可以进行
	 * 
	 * @param request
	 *            请求
	 * @param response
	 *            响应
	 * @throws IOException
	 *             若抛出则会直接关闭连接（且尽量响应500）
	 */
	void precheck(RestfulRequest request, RestfulResponse response) throws IOException;

	/**
	 * 处理请求
	 * 
	 * @param request
	 *            请求
	 * @param response
	 *            响应
	 * @throws IOException
	 *             若抛出则会直接关闭连接（且尽量响应500）
	 */
	void service(RestfulRequest request, RestfulResponse response) throws IOException;

	/**
	 * 处理请求超时未响应，可以响应业务层的超时，若不处理会响应HTTP 202
	 * 
	 * @param request
	 *            请求
	 * @param response
	 *            响应
	 * @throws IOException
	 */
	void timeout(RestfulRequest request, RestfulResponse response) throws IOException;
}
