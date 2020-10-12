package cn.weforward.common.util;

/**
 * crc16计算工具
 * 
 * @author zhangpengji
 *
 */
public class CRC16 {

	public enum CRC16Mode {

		/**
		 * CRC16_CCITT_FALSE：多项式x16+x12+x5+1（0x1021），初始值0xFFFF，低位在后，高位在前，结果与0x0000异或
		 */
		CCITT_FALSE(0xFFFF, 0x1021, 0x0000, false),
		/**
		 * CRC16_CCITT_FALSE：多项式x16+x12+x5+1（0x1021），初始值0xFFFF，低位在后，高位在前，结果与0x0000异或
		 */
		// 0x8408为0x1021倒转
		CCITT(0x0000, 0x8408, 0x0000, true),
		/**
		 * CRC16_XMODEM：多项式x16+x12+x5+1（0x1021），初始值0x0000，低位在后，高位在前，结果与0x0000异或
		 */
		XMODEM(0x0000, 0x1021, 0x0000, false),
		/**
		 * CRC16_X25：多项式x16+x12+x5+1（0x1021），初始值0xFFFF，低位在前，高位在后，结果与0xFFFF异或
		 */
		X25(0xFFFF, 0x8408, 0xFFFF, true),
		/**
		 * CRC16_MODBUS：多项式x16+x15+x2+1（0x8005），初始值0xFFFF，低位在前，高位在后，结果与0x0000异或
		 */
		// 0xa001为0x8005倒转
		MODBUS(0xFFFF, 0xa001, 0x0000, true),
		/**
		 * CRC16_MAXIM：多项式x16+x15+x2+1（0x8005），初始值0x0000，低位在前，高位在后，结果与0xFFFF异或
		 */
		MAXIM(0x0000, 0xa001, 0xFFFF, true),
		/**
		 * CRC16_IBM/ARC：多项式x16+x15+x2+1（0x8005），初始值0x0000，低位在前，高位在后，结果与0x0000异或
		 */
		IBM(0x0000, 0xa001, 0x0000, true),
		/**
		 * CRC16_USB：多项式x16+x15+x2+1（0x8005），初始值0xFFFF，低位在前，高位在后，结果与0xFFFF异或
		 */
		USB(0xFFFF, 0xa001, 0xFFFF, true);

		public final int init;
		public final int polynomial;
		public final int finalXor;
		public final boolean reverse;

		CRC16Mode(int init, int polynomial, int finalXor, boolean reverse) {
			this.init = init;
			this.polynomial = polynomial;
			this.finalXor = finalXor;
			this.reverse = reverse;
		}
	}

	CRC16Mode m_Mode;
	int m_Digest;

	public CRC16(CRC16Mode mode) {
		m_Mode = mode;
		m_Digest = mode.init;
	}

	public void update(byte[] data) {
		if (m_Mode.reverse) {
			m_Digest = calculateReverse(m_Digest, m_Mode.polynomial, data);
		} else {
			m_Digest = calculate(m_Digest, m_Mode.polynomial, data);
		}
	}

	public short digest() {
		return (short) (m_Digest ^ m_Mode.finalXor);
	}

	/**
	 * 计算crc16
	 * 
	 * @param mode
	 * @param data
	 * @return 不管那种标准，返回值统一为高位在前短整型
	 */
	public static short calculate(CRC16Mode mode, byte[] data) {
		int value;
		if (mode.reverse) {
			value = calculateReverse(mode.init, mode.polynomial, data);
		} else {
			value = calculate(mode.init, mode.polynomial, data);
		}
		return (short) (value ^ mode.finalXor);
	}

	/* 高位在前的计算方式 */
	private static int calculate(int init, int polynomial, byte[] data) {
		for (int i = 0; i < data.length; i++) {
			// 取被校验串的一个字节与 16 位寄存器进行“异或”运算
			init ^= ((data[i] & 0xff) << 8);

			for (int j = 0; j < 8; j++) {
				int flag = init & 0x8000;
				// 把这个 16 寄存器向左移一位
				init = init << 1;
				if (0 != flag) {// 若向左(标记位)移出的数位是 1
					init ^= polynomial;
				}
			}
		}

		return init;
	}

	/* 低位在前的计算方式 */
	private static int calculateReverse(int init, int polynomial, byte[] data) {
		for (int i = 0; i < data.length; i++) {
			// 取被校验串的一个字节与 16 位寄存器进行“异或”运算
			init ^= (data[i] & 0xff);

			for (int j = 0; j < 8; j++) {
				int flag = init & 0x0001;
				init = init >> 1;
				// 若向右(标记位)移出的数位是 1
				if (flag == 1)
					init ^= polynomial;
			}
		}

		return init;
	}

}
