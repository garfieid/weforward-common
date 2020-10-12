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
 * 当前情况（条件下）不支持所请求的方法或功能
 * 
 * @author liangyi
 * 
 */
public class UnsupportedException extends Unexpected {
	private static final long serialVersionUID = -5296884460063197004L;

	public static final UnsupportedException INSTANCE = new UnsupportedException("此功能不支持");

	public UnsupportedException(String message) {
		super(message);
	}

	public UnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedException(Throwable cause) {
		super(cause);
	}

	public UnsupportedException() {
		super("Unsupported");
	}
}
