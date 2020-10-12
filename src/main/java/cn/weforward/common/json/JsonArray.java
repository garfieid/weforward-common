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

/**
 * JSON数组
 * 
 * @author zhangpengji
 *
 */
public interface JsonArray extends JsonNode {
	/**
	 * 项数
	 */
	int size();

	/**
	 * 指定下标的项
	 * 
	 * @param index
	 *            下标
	 * @return 相应项
	 */
	Object item(int index);

	/**
	 * （在解析过程）可追加项的
	 * 
	 * @author liangyi
	 *
	 */
	public interface Appendable extends JsonArray {
		/**
		 * 追加项
		 * 
		 * @param value
		 *            追加的项
		 */
		void add(Object value);
	}
}
