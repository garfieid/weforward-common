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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cn.weforward.common.sys.IdGenerator;
import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.UriMatcher;

public class TestUtils {

	@Test
	public void benchmarkAntPathPattern() {
		AntPathPattern ap1 = AntPathPattern.valueOf("/**/*.jsp");
		AntPathPattern ap2 = AntPathPattern.valueOf("/**/a*b?.*");
		AntPathPattern ap3 = AntPathPattern.valueOf("/**/a*b?.*");
		AntPathPattern ap4 = AntPathPattern.valueOf("/app/**/dir/file.*");
		AntPathPattern ap5 = AntPathPattern.valueOf("/app/**/dir/file.*");
		AntPathPattern ap6 = AntPathPattern.valueOf("/**abc.*");
		AntPathPattern ap7 = AntPathPattern.valueOf("/**/*");
		AntPathPattern ap8 = AntPathPattern.valueOf("/abc/*");
		AntPathPattern ap9 = AntPathPattern.valueOf("/abc/*");
		AntPathPattern ap10 = AntPathPattern.valueOf("/**/?");
		AntPathPattern ap11 = AntPathPattern.valueOf("/**/?");
		AntPathPattern ap12 = AntPathPattern.valueOf("abc/*.jsp");
		AntPathPattern ap13 = AntPathPattern.valueOf("/abc////def.*");
		AntPathPattern ap14 = AntPathPattern.valueOf("/abc/def.jsp/**/**");
		for (int i = 0; i < 1000000; i++) {
			ap1.match("/bs/ts/aa.jsp");
			ap2.match("/bs/ts/aab.jsp");
			ap3.match("/bs/ts/abc.jsp");
			ap4.match("/app/foo/dir/file.html");
			ap5.match("/app/foo/bar/dir/file.pdf");
			ap6.match("/app/foo/abc.pdf");
			ap7.match("/app/foo/abc/");
			ap8.match("/abc/a");
			ap9.match("/abc/a/");
			ap10.match("/app/foo/abc/");
			ap11.match("/app/f");
			ap12.match("bb/aa.jsp");
			ap13.match("/abc/def.jsp");
			ap14.match("/abc/def.jsp");
		}
	}

	// @Test
	public void testBytes() {
		String readable = "2000m";
		long v = Bytes.parseHumanReadable(readable);
		System.out.println(v);
		System.out.println(v >= Bytes.UNIT_GB);
	}

	// @Test
	public void testTimestamp() {
		IdGenerator.Tick tick = new IdGenerator.Tick("x000a");
		System.out.println(tick.genId(""));
		System.out.println(tick.genId(""));
	}

	// @Test
	public void testUriMatcher() {
		List<String> uris = Arrays.asList("/abc/(**)", "/ab-(**)", "/ticket/*.jspx", "/ticket/**",
				"/ticket/abc/*.jspx", "/wx-*", "/wx-**", "**");
		UriMatcher matcher = new UriMatcher(uris);
		System.out.println(matcher.match("/abc/b.jspx"));
		System.out.println(matcher.match("/ab-b.jspx"));
		System.out.println(matcher.match("/ticket/b.jspx"));
		System.out.println(matcher.match("/ticket/abc/a.jspx"));
		System.out.println(matcher.match("/ticket/abc/b/a.jspx"));
		System.out.println(matcher.match("/wx-a001b"));
		System.out.println(matcher.match("/wx-a001b/a.jspx"));
		System.out.println(matcher.match("/dir/file"));
		System.out.println(matcher.match("/2dir/3-file"));
	}
}
