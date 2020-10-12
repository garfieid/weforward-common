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
import java.io.InputStream;

import cn.weforward.common.DictionaryExt;

/**
 * RESTful调用请求
 * 
 * @author liangyi
 * 
 */
public interface RestfulRequest {
	public static final String HEAD = "HEAD";
	public static final String GET = "GET";
	public static final String PUT = "PUT";
	public static final String POST = "POST";
	public static final String DELETE = "DELETE";

	/**
	 * 执行动作，如：GET/POST/PUT/DELETE/HEAD...
	 */
	String getVerb();

	/**
	 * 资源路径，如：/order，/order/payment
	 */
	String getUri();

	/**
	 * 附加参数
	 */
	DictionaryExt<String, String> getParams();

	/**
	 * 头信息（元数据）
	 */
	DictionaryExt<String, String> getHeaders();

	/**
	 * 取得调用请求的内容
	 * 
	 * @return 输入流形式的内容
	 */
	InputStream getContent() throws IOException;

	/**
	 * 调用者IP
	 */
	String getClientIp();
}
