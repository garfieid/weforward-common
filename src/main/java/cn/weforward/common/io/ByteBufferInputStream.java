package cn.weforward.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 封装ByteBuffer的InputStream
 * 
 * @author zhangpengji
 *
 */
public class ByteBufferInputStream extends InputStream implements InputStreamNio {

	ByteBuffer m_Buffer;

	public ByteBufferInputStream(ByteBuffer buffer) {
		m_Buffer = buffer;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int rem = m_Buffer.remaining();
		if (rem <= 0) {
			return -1;
		}
		int drem = dst.remaining();
		if (drem <= 0) {
			return 0;
		}
		int limit = m_Buffer.limit();
		if (rem > drem) {
			m_Buffer.limit(m_Buffer.position() + drem);
			rem = drem;
		}
		dst.put(m_Buffer);
		m_Buffer.limit(limit);
		return rem;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int rem = m_Buffer.remaining();
		if (rem <= 0) {
			return -1;
		}
		if (len > rem) {
			len = rem;
		}
		m_Buffer.get(b, off, len);
		return len;
	}

	@Override
	public int read() throws IOException {
		if (m_Buffer.hasRemaining()) {
			return m_Buffer.get();
		}
		return -1;
	}

	@Override
	public InputStreamNio duplicate() throws IOException {
		throw new IOException("不支持");
	}

}
