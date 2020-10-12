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
import java.io.OutputStream;

/**
 * RESTful响应返回接口
 * 
 * @author liangyi
 * 
 */
public interface RestfulResponse {
	/**
	 * 200 OK
	 */
	public static final int STATUS_OK = 200;

	/**
	 * 203 Non-Authoritative Information (since HTTP/1.1)
	 */
	public static final int STATUS_NON_AUTHORITATIVE_INFORMATION = 203;

	/**
	 * 204 No Content
	 */
	public static final int STATUS_NO_CONTENT = 204;

	/**
	 * 206 Partial Content
	 */
	public static final int STATUS_PARTIAL_CONTENT = 206;

	/**
	 * 300 Multiple Choices
	 */
	public static final int STATUS_MULTIPLE_CHOICES = 300;

	/**
	 * 301 Moved Permanently
	 */
	public static final int STATUS_MOVED_PERMANENTLY = 301;

	/**
	 * 302 Moved Temporarily
	 */
	public static final int STATUS_MOVED_TEMPORARILY = 302;

	/**
	 * 303 See Other (since HTTP/1.1)
	 */
	public static final int STATUS_SEE_OTHER = 303;

	/**
	 * 304 Not Modified
	 */
	public static final int STATUS_NOT_MODIFIED = 304;

	/**
	 * 307 Temporary Redirect (since HTTP/1.1)
	 */
	public static final int STATUS_TEMPORARY_REDIRECT = 307;

	/**
	 * 308 Permanent Redirect (RFC7538)
	 */
	public static final int STATUS_PERMANENT_REDIRECT = 308;

	/**
	 * 400 Bad Request
	 */
	public static final int STATUS_BAD_REQUEST = 400;

	/**
	 * 401 Unauthorized
	 */
	public static final int STATUS_UNAUTHORIZED = 401;

	/**
	 * 402 Payment Required
	 */
	public static final int STATUS_PAYMENT_REQUIRED = 402;

	/**
	 * 403 Forbidden
	 */
	public static final int STATUS_FORBIDDEN = 403;

	/**
	 * 404 Not Found
	 */
	public static final int STATUS_NOT_FOUND = 404;

	/**
	 * 405 Method Not Allowed
	 */
	public static final int STATUS_METHOD_NOT_ALLOWED = 405;

	/**
	 * 406 Not Acceptable
	 */
	public static final int STATUS_NOT_ACCEPTABLE = 406;

	/**
	 * 408 Request Timeout
	 */
	public static final int STATUS_REQUEST_TIMEOUT = 408;

	/**
	 * 409 Conflict
	 */
	public static final int STATUS_CONFLICT = 409;

	/**
	 * 410 Gone
	 */
	public static final int STATUS_GONE = 410;

	// /**
	// * 411 Length Required
	// */
	// public static final int LENGTH_REQUIRED = 411;
	//
	// /**
	// * 412 Precondition Failed
	// */
	// public static final int PRECONDITION_FAILED = 412;

	/**
	 * 413 Request Entity Too Large
	 */
	public static final int STATUS_REQUEST_ENTITY_TOO_LARGE = 413;

	/**
	 * 414 Request-URI Too Long
	 */
	public static final int STATUS_REQUEST_URI_TOO_LONG = 414;

	/**
	 * 415 Unsupported Media Type
	 */
	public static final int STATUS_UNSUPPORTED_MEDIA_TYPE = 415;

	// /**
	// * 416 Requested Range Not Satisfiable
	// */
	// public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

	/**
	 * 417 Expectation Failed
	 */
	public static final int STATUS_EXPECTATION_FAILED = 417;

	/**
	 * 421 Misdirected Request
	 */
	public static final int STATUS_MISDIRECTED_REQUEST = 421;

	/**
	 * 429 Too Many Requests (RFC6585)
	 */
	public static final int STATUS_TOO_MANY_REQUESTS = 429;

	/**
	 * 431 Request Header Fields Too Large (RFC6585)
	 */
	public static final int STATUS_REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

	/**
	 * 500 Internal Server Error
	 */
	public static final int STATUS_INTERNAL_SERVER_ERROR = 500;

	/**
	 * 501 Not Implemented
	 */
	public static final int STATUS_NOT_IMPLEMENTED = 501;

	/**
	 * 502 Bad Gateway
	 */
	public static final int STATUS_BAD_GATEWAY = 502;

	/**
	 * 503 Service Unavailable
	 */
	public static final int STATUS_SERVICE_UNAVAILABLE = 503;

	/**
	 * 504 Gateway Timeout
	 */
	public static final int STATUS_GATEWAY_TIMEOUT = 504;

	/**
	 * 505 HTTP Version Not Supported
	 */
	public static final int STATUS_HTTP_VERSION_NOT_SUPPORTED = 505;

	/**
	 * 指定响应超时值，若指定则在超时未响应时自动向请求端返回STATUS_MISDIRECTED_REQUEST(421)
	 * 
	 * @param timeout
	 *            超时值（毫秒），0则不超时
	 */
	void setResponse(int timeout) throws IOException;

	/**
	 * 设置响应的头信息
	 * 
	 * @param name
	 *            名称
	 * @param value
	 *            值
	 */
	void setHeader(String name, String value) throws IOException;

	/**
	 * 设置响应状态
	 * 
	 * @param status
	 *            HTTP状态码（参考STATUS_xxx）
	 */
	void setStatus(int status) throws IOException;

	/**
	 * 打开响应输出流
	 * 
	 * @return 输出流
	 * @throws IOException
	 */
	OutputStream openOutput() throws IOException;

	/**
	 * （不响应）关闭输出/连接
	 */
	void close();

	/**
	 * 是否已响应
	 */
	boolean isRespond();
}
