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
package cn.weforward.common;

import java.util.Iterator;

/**
 * 用于支持查询结果以分页方式提取的结果集接口
 * 
 * @param E
 *            结果项对象类型
 * 
 * @version V1.0
 * @author liangyi
 */
public interface ResultPage<E> extends Iterator<E>, Iterable<E> {

	/**
	 * 总页数
	 * 
	 * @return
	 */
	public int getPageCount();

	/**
	 * 总项数
	 * 
	 * @return
	 */
	public int getCount();

	/**
	 * 取每页的项数
	 * 
	 * @return
	 */
	public int getPageSize();

	/**
	 * 设置每页项数
	 * 
	 * @param size
	 *            每页项数
	 */
	public void setPageSize(int size);

	/**
	 * 转到指定页（1-getPageCount()）
	 * 
	 * @param page
	 *            第N页
	 */
	void setPage(int page);

	/**
	 * 转到指定页（1-getPageCount()）
	 * 
	 * @param page
	 *            第N页
	 * @return 返回false表示超出可到的页面范围
	 */
	public boolean gotoPage(int page);

	/**
	 * 上一项（为支持上一项/下一项功能作扩展）
	 * 
	 * @return 上一项，返回null表示已经到达最前一项
	 */
	public E prev();

	/**
	 * 是否有前一项
	 * 
	 * @return true表示有前一项，prev()有返回结果
	 */
	public boolean hasPrev();

	/**
	 * 移动到指定位置（相对于当前页）
	 * 
	 * @param pos
	 *            在当前页的位置（0~getPageSize()-1）
	 * @return 当前项，null表示位置不对
	 */
	public E move(int pos);

	/**
	 * 当前所处的结果页
	 * 
	 * @return 当前页号
	 */
	public int getPage();

}
