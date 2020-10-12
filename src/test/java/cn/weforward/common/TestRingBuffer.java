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

import java.io.FileInputStream;

import org.junit.Test;

import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.sys.GcCleaner;
import cn.weforward.common.util.Bytes;

public class TestRingBuffer {

	@Test
	public void test() {
		// final RingBuffer<String> buffer = new RingBuffer<String>(100);
		long ts = System.currentTimeMillis();
		int i;
		ThreadGroup tg = new ThreadGroup("test");
		for (i = 0; i < 100; i++) {
			// final int ii = i * 10000;
			Thread t = new Thread(tg, "t-" + i) {
				@Override
				public void run() {
					for (int i = 0; i < 10000; i++) {
						// String v = String.valueOf(ii + i);
						// buffer.offer(v);
						try {
							testBytesOutputStream();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			t.start();
		}
		// for (i = 0; i < 20; i++) {
		// Thread t = new Thread() {
		// @Override
		// public void run() {
		// for (int i = 0; i < 100; i++) {
		// buffer.poll();
		// }
		// }
		// };
		// t.start();
		// }
		while (tg.activeCount() > 0) {
			try {
				synchronized (tg) {
					tg.wait(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// System.out.println(buffer);
		System.out.println((System.currentTimeMillis() - ts) + "(ms)");
		System.out.println(Bytes.Pool._8k);
		GcCleaner._Cleaner.cleanup(GcCleanable.POLICY_CRITICAL);
		System.out.println(Bytes.Pool._8k);
		System.out.println("done.");
	}

	// @Test
	public void testBytesOutputStream() throws Exception {
		FileInputStream in;
		in = new FileInputStream("bin/weforward-common-source.jar");
		BytesOutputStream out = new BytesOutputStream(in);
		out.close();
		System.out.println(out.getBytes());
	}
}
