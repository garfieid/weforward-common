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
 * Json对象
 * 
 * @author zhangpengji
 *
 */
public interface JsonObject extends JsonNode {
	/**
	 * 属性项数
	 */
	int size();

	/**
	 * 对象属性
	 * 
	 * @param name
	 *            属性名
	 * @return 属性
	 */
	JsonPair property(String name);

	/**
	 * 所有属性
	 */
	Iterable<JsonPair> items();

	/**
	 * （在解析过程）可追加项的
	 * 
	 * @author liangyi
	 *
	 */
	public interface Appendable extends JsonObject {
		/**
		 * 追加属性项
		 * 
		 * @param name
		 *            属性名
		 * @param value
		 *            属性值
		 */
		void add(String name, Object value);
	}
}
