package cn.weforward.common;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.SimpleUtf8Encoder;

public class Utf8EncoderTest {

	// @Test
	public void pressure() throws IOException {
		ByteArrayOutputStream output = new BytesOutputStream();
		Bytes.Pool._8k.poll();
		long u1 = 0;
		long u2 = 0;
		for (int l = 0; l < 100000; l++) {
			StringBuilder sb = new StringBuilder(64 * 1024);
			Random rd = new Random();
			for (int i = 0; i < 100; i++) {
				int ch = rd.nextInt(0x7f);
				if (ch >= 0xD800 && ch <= 0xDFFF) {
					continue;
				}
				sb.append((char) ch);
			}
			String str = sb.toString();

			output.reset();
			SimpleUtf8Encoder encoder = new SimpleUtf8Encoder(output);
			long start = System.nanoTime();
			encoder.encode(str);
			long u11 = System.nanoTime() - start;
			u1 += u11;
			byte[] d1 = output.toByteArray();

			start = System.nanoTime();
			byte[] d2 = str.getBytes("utf-8");
			long u22 = System.nanoTime() - start;
			u2 += u22;
			System.out.println(u11 + "," + u22 + "," + (u11 - u22) + "," + (double) u11 / u22);
			// for (int i = 0; i < str.length(); i++) {
			// System.out.print("\\" + "u" + Hex.toHex16((short) str.charAt(i)));
			// }
			// System.out.println();
			// System.out.println(Arrays.toString(d1));
			// System.out.println(Arrays.toString(d2));
			assertTrue(Arrays.equals(d1, d2));
		}
		System.out.println(u1 + "," + u2 + "," + (u1 - u2) + "," + (double) u1 / u2);

		output.close();
	}

	@Test
	public void pressure2() throws IOException, NoSuchAlgorithmException, InterruptedException {
		Bytes.Pool._8k.poll();
		long u1 = 0;
		long u2 = 0;
		for (int l = 0; l < 1000000; l++) {
			if(l+1%100000 == 0) {
				Thread.sleep(10*1000);
			}
			
			StringBuilder sb = new StringBuilder(64 * 1024);
			Random rd = new Random();
			for (int i = 0; i < 100; i++) {
				int ch = rd.nextInt(0x10000);
				if (ch >= 0xD800 && ch <= 0xDFFF) {
					continue;
				}
				sb.append((char) ch);
			}
			String str = sb.toString();

			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			SimpleUtf8Encoder utf8Encoder = new SimpleUtf8Encoder(new OutputStream() {

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					md.update(b, off, len);
				}

				@Override
				public void write(int b) throws IOException {
					md.update((byte) b);
				}
			});
			//char[] cs = str.toCharArray();
			long start = System.nanoTime();
			utf8Encoder.encode(str);
			md.digest();
			long u11 = System.nanoTime() - start;
			u1 += u11;
		}

		for (int l = 0; l < 1000000; l++) {
			if(l+1%100000 == 0) {
				Thread.sleep(10*1000);
			}
			
			StringBuilder sb = new StringBuilder(64 * 1024);
			Random rd = new Random();
			for (int i = 0; i < 100; i++) {
				int ch = rd.nextInt(0x10000);
				if (ch >= 0xD800 && ch <= 0xDFFF) {
					continue;
				}
				sb.append((char) ch);
			}
			String str = sb.toString();
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			long start = System.nanoTime();
			md.update(str.getBytes("utf-8"));
			md.digest();
			long u22 = System.nanoTime() - start;
			u2 += u22;
		}
		System.out.println(u1 + "," + u2 + "," + (u1 - u2) + "," + (double) u1 / u2);
	}
}
