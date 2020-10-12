package cn.weforward.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import cn.weforward.common.io.ByteBufferInputStream;
import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.SimpleUtf8Encoder;
import cn.weforward.common.util.VersionUtil;

public class TestAll {

	@Test
	public void versionCompare() {
		assertTrue(VersionUtil.compareTo("1.0", "1.0") == 0);
		assertTrue(VersionUtil.compareTo("1.23.456", "2.23.456") < 0);
		assertTrue(VersionUtil.compareTo("1.23.456", "2.23") < 0);
		assertTrue(VersionUtil.compareTo("2.34", "2.23.456") > 0);
		assertTrue(VersionUtil.compareTo("1.23.456", "1.23.4567") < 0);
		assertTrue(VersionUtil.compareTo("1.23.567", "1.23.4567") < 0);
		assertTrue(VersionUtil.compareTo("1.23", "1.23.4567") < 0);
	}

	@Test
	public void antPathMatch() {
		assertTrue(AntPathPattern.match("/**/*.jsp", "/bs/ts/aa.jsp"));
		assertFalse(AntPathPattern.match("**/*.jsp", "/bs/ts/aa.jsp"));
		assertFalse(AntPathPattern.match("/**/a*b?.*", "/bs/ts/aab.jsp"));
		assertTrue(AntPathPattern.match("/**/a*b?.*", "/bs/ts/abc.jsp"));
		assertTrue(AntPathPattern.match("/app/**/dir/file.*", "/app/foo/dir/file.html"));
		assertTrue(AntPathPattern.match("/app/**/dir/file.*", "/app/foo/bar/dir/file.pdf"));
		assertFalse(AntPathPattern.match("/**abc.*", "/app/foo/abc.pdf"));
		assertTrue(AntPathPattern.match("/**/*", "/app/foo/abc/"));
		assertTrue(AntPathPattern.match("/abc/*", "/abc/a"));
		assertFalse(AntPathPattern.match("/abc/*", "/abc/a/"));
		assertFalse(AntPathPattern.match("/**/?", "/app/foo/abc/"));
		assertTrue(AntPathPattern.match("/**/?", "/app/f"));
		assertFalse(AntPathPattern.match("abc/*.jsp", "bb/aa.jsp"));
		assertTrue(AntPathPattern.match("/abc/def.jsp/**/**", "/abc/def.jsp"));

		assertTrue(AntPathPattern.matchStart("/abc/a/123/456", "/abc/a/123"));
		assertFalse(AntPathPattern.matchStart("/abc/a/123/456", "/abc/a/1234"));
		assertTrue(AntPathPattern.matchStart("/abc/a/**/abc", "/abc/a/b"));
		// assertTrue(AntPathPattern.match("/abc////def.*", "/abc/def.jsp"));
	}

	@Test
	public void utf8Encoder() throws IOException {
		String str = "";
		str += " !\u007f";// ASCII 0~7f
		str += "¢¥®Ø"; // 拉丁字母-1
		str += "ĀĲ"; // 拉丁字母（扩展A）
		str += "\u01F1"; // 拉丁字母（扩展B）
		str += "\u02A0"; // IPA扩展
		str += "ᴀᵿ"; // 音标扩展
		str += "ӲѼ"; // 西里尔字母
		str += "\u2DE0\uA68F"; // 西里尔字母扩展A区、B区
		str += "\u25A0"; // 几何图形
		str += "表意文字劍漢兌兑"; // 中日韩
		str += "😄𐐷𤭢"; // Uncode辅组平面
		ByteArrayOutputStream output = new BytesOutputStream();
		SimpleUtf8Encoder encoder = new SimpleUtf8Encoder(output);
		encoder.encode(str);
		byte[] d1 = output.toByteArray();
		output.close();
		byte[] d2 = str.getBytes("utf-8");
		assertTrue(Arrays.equals(d1, d2));

		StringBuilder sb = new StringBuilder(64 * 1024);
		Random rd = new Random();
		for (int i = 0; i < 100000; i++) {
			int ch = rd.nextInt(0x10000);
			if (ch >= 0xD800 && ch <= 0xDFFF) {
				continue;
			}
			sb.append((char) ch);
		}
		for (int i = 0; i < 1000; i++) {
			int ch = rd.nextInt(0x400);
			sb.append((char) (0xD800 | ch));
			ch = rd.nextInt(0x1000);
			sb.append((char) (0xDC00 | ch));
		}
		str = sb.toString();
		output = new BytesOutputStream();
		encoder = new SimpleUtf8Encoder(output);
		encoder.encode(str);
		d1 = output.toByteArray();
		output.close();
		d2 = str.getBytes("utf-8");
		// for (int i = 0; i < str.length(); i++) {
		// System.out.print("\\" + "u" + Hex.toHex16((short) str.charAt(i)));
		// }
		// System.out.println();
		// System.out.println(Arrays.toString(d1));
		// System.out.println(Arrays.toString(d2));
		assertTrue(Arrays.equals(d1, d2));
	}

	@Test
	public void byteBufferInputStream() throws IOException {
		byte[] data = new byte[10 * 1024];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		BytesOutputStream out = new BytesOutputStream();
		BytesOutputStream.transfer(buf, out, 100);
		BytesOutputStream.transfer(buf, out, 1234);
		BytesOutputStream.transfer(buf, out, -1);
		byte[] data2 = out.toByteArray();
		assertTrue(Arrays.equals(data, data2));
	}

	@Test
	public void byteBufferInputStream2() throws IOException {
		byte[] data = new byte[16];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		ByteBufferInputStream in = new ByteBufferInputStream(ByteBuffer.wrap(data));
		BytesOutputStream out = new BytesOutputStream();
		ByteBuffer buf = ByteBuffer.allocateDirect(7);
		int len = in.read(buf);
		while (-1 != len) {
			buf.flip();
			while (buf.hasRemaining()) {
				out.write(buf.get());
			}
			buf.clear();
			len = in.read(buf);
		}
		byte[] data2 = out.toByteArray();
		in.close();
		out.close();
		assertTrue(Arrays.equals(data, data2));
	}
}
