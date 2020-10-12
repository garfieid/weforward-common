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
package cn.weforward.common.util;

import java.util.Date;
import java.util.concurrent.Executor;

import cn.weforward.common.Cancelable;

/**
 * 任务执行器，可指定任务执行的条件、事件或延时及周期<br/>
 * 
 * EVENT_*为任务执行触发事件，使用0x10000000~0xFFFF0000位<br/>
 * CONDITION_*为任务执行条件，使用0x00001000~0x0000FFFF位<br/>
 * 
 * @author liangyi
 * 
 */
public interface TaskExecutor extends Executor {
	/**
	 * 无选项指定
	 */
	public static final int OPTION_NONE = 0x00000000;

	/**
	 * 网络可用事件
	 */
	public static final int EVENT_NETWORK_AVAILABLE = 0x10000000;

	/**
	 * 网络空闲事件
	 */
	public static final int EVENT_NETWORK_IDLE = 0x20000000;

	/**
	 * 已登录事件
	 */
	public static final int EVENT_LOGINED = 0x40000000;

	/**
	 * 需要网络可用
	 */
	public static final int CONDITION_NETWORK_AVAILABLE = 0x00001000;

	/**
	 * 需要网络空闲
	 */
	public static final int CONDITION_NETWORK_IDLE = 0x00002000;

	/**
	 * 需要已登录
	 */
	public static final int CONDITION_LOGINED = 0x00004000;

	/**
	 * 增加一个后台任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*或EVENT_*
	 * @param delay
	 *            任务在（毫秒后）执行
	 * @param period
	 *            任务按（毫秒）周期地执行
	 * @return 后台任务
	 */
	public Task execute(Runnable worker, int options, long delay, long period);

	/**
	 * 增加一个后台任务，若firstTime小于当前时间且period为0则不执行任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*或EVENT_*
	 * @param firstTime
	 *            任务首次执行时间
	 * @param period
	 *            任务按（毫秒）周期地执行，若为0表示只执行一次
	 * @return 后台任务，若firstTime小于当前时间且period为0返回null
	 */
	public Task execute(Runnable worker, int options, Date firstTime, long period);

	/**
	 * 增加一个后台任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*或EVENT_*
	 * @param delay
	 *            任务在（毫秒后）执行
	 * @return 后台任务
	 */
	public Task execute(Runnable worker, int options, long delay);

	/**
	 * 增加一个后台任务
	 * 
	 * @param worker
	 *            要执行的任务
	 * @param options
	 *            选项 CONDITION_*或EVENT_*
	 * @return 后台任务
	 */
	public Task execute(Runnable worker, int options);

	/**
	 * 检查当前线程是否后台运行中的任务
	 * 
	 * @return 是则返回true
	 */
	public boolean isBackground();

	/**
	 * 检查指定的条件或事件是否都具备
	 * 
	 * @param options
	 *            选项 CONDITION_*或EVENT_*
	 * @return 具备则返回true
	 */
	public boolean isReady(int options);

	/**
	 * 可运行接口的一个基类，用于在执行工作时取得执行器的具体关联任务，因其在进入TaskExecutor时会调用setTask注入Task
	 * 
	 * @author liangyi
	 * 
	 */
	public static abstract class AbstractWorker implements Runnable {
		private volatile Task m_Task;

		/**
		 * 取得当前的关联任务
		 * 
		 * @return 关联任务
		 */
		public Task getTask() {
			return m_Task;
		}

		/**
		 * 取消任务（若未执行的话）
		 */
		public void cancel() {
			if (null != m_Task) {
				m_Task.cancel();
			}
		}

		protected void setTask(Task task) {
			m_Task = task;
		}
	}

	/**
	 * 待执行的任务
	 * 
	 * @author liangyi
	 * 
	 */
	public static interface Task extends Runnable, Cancelable {
		/**
		 * 是否具备执行的条件了
		 * 
		 * @return 具备则返回true
		 */
		public boolean isReady();

		/**
		 * 任务是否取消了
		 * 
		 * @return 是取消则返回true
		 */
		public boolean isCancel();

		/**
		 * 任务工作是否执行中
		 * 
		 * @return 若任务的工作正在执行中返回true
		 */
		public boolean isRunning();

		/**
		 * 取消任务（若其还是等待执行中），isCancel()将返回true
		 */
		public void cancel();

		/**
		 * 完成任务（若其还是等待执行中），isFinish()将返回true
		 */
		public void finish();

		/**
		 * 执行次数
		 * 
		 * @return 任务总执行次数
		 */
		public int getFrequency();

		/**
		 * 任务是否完成（不会再执行）
		 * 
		 * @return 是完成则返回true
		 */
		public boolean isFinish();

		/**
		 * 任务最后执行完成时间
		 */
		public Date getLastFinish();

	}
}
