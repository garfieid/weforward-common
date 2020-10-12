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

/**
 * 指示在GC时可清理的接口
 * 
 * @author liangyi
 * 
 */
public interface GcCleanable {
	/** 去除空闲的缓存 */
	static final int POLICY_IDLE = 0x01;
	/** 可以执行保养/维护的时候啦 */
	static final int POLICY_MAINTAIN = 0x03;
	/** 可以执行优化操作 */
	static final int POLICY_OPTIMIZE = 0x04;
	/** 内存低，尽量去除缓存 */
	static final int POLICY_LOW = 0x0e;
	/** 内存严重不足，能扔的都扔了吧 */
	static final int POLICY_CRITICAL = 0x10;

	/** 策略位 */
	static final int POLICY_MASK = 0xFF;

	/**
	 * 处理GC触发的清理事件，根据优先级以不同策略处理
	 * 
	 * @param policy POLICY_xxx
	 */
	void onGcCleanup(int policy);
}
