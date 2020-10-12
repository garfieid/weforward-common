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
package cn.weforward.common.execption;

/**
 * 运行期间未在意料中的异常，用于包装上游异常的数据访问异常，最主要的用途是覆盖掉fillInStackTrace()提高效率
 * 
 * @author liangyi
 * 
 */
public class Unexpected extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public Unexpected(String message) {
		super(message);
	}

	public Unexpected(Throwable cause) {
		super(cause);
	}

	public Unexpected(String message, Throwable cause) {
		super(message, cause);
	}
//
//	@Override
//	public Throwable fillInStackTrace() {
//		// 不要填堆栈信息
//		Throwable cause = getCause();
//		return (null == cause) ? this : cause;
//	}
//
//	@Override
//	public StackTraceElement[] getStackTrace() {
//		return getCause().getStackTrace();
//	}
}
