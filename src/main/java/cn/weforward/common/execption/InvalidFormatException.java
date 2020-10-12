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
 * 格式无效
 * 
 * @author liangyi,zhangpengji
 * 
 */
public class InvalidFormatException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	static String message(String format, int position) {
		if (null == format || 0 == format.length() || position < 0 || position >= format.length()) {
			// Illegal Argument
			StringBuilder sb = new StringBuilder(100);
			sb.append("格式错误：");
			if (null == format || format.length() < 50) {
				sb.append(format);
			} else {
				sb.append(format.subSequence(0, 50));
			}
			return sb.toString();
		}

		int idx = (position > 15) ? (position - 10) : position;
		int end = idx + 50;
		StringBuilder sb = new StringBuilder(100);
		sb.append("格式错误，发生在").append(position - idx).append("位置的：");
		if (idx > 0) {
			sb.append("...");
		}
		if (end > format.length()) {
			sb.append(format.substring(idx));
		} else {
			sb.append(format.substring(idx, end)).append("...");
		}
		return sb.toString();
	}

	/**
	 * 创建格式错误异常
	 * 
	 * @param format
	 *            格式串
	 * @param position
	 *            发生错误的位置
	 */
	public InvalidFormatException(String format, int position) {
		super(message(format, position));
	}

	public InvalidFormatException(String s) {
		super(s);
	}

	public InvalidFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
