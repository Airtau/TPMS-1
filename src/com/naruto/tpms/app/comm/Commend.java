package com.naruto.tpms.app.comm;

/**
 * ������Ϣ������
 * 
 * @author Thinkman 415071574@qq.com
 */
public final class Commend {
	/**
	 * ���͵�����
	 * 
	 * @author Thinkman 415071574@qq.com
	 */
	public static final class TX {
		/**
		 * ��ѯ������ID(�̶�����)
		 */
		public static final byte[] QUERY_TIRE_ID = new byte[] { (byte) 0x055, (byte) 0xaa, (byte) 0x06, (byte) 0x02, (byte) 0x00, (byte) 0xfb };
		/**
		 * �˳����ѧϰ(�̶�����)
		 */
		public static final byte[] QUIT_STUDY = new byte[] { (byte) 0x055, (byte) 0xaa, (byte) 0x06, (byte) 0x06, (byte) 0x00, (byte) 0xff };
		/**
		 * ��Ϊ����
		 */
		public static final byte[] SET_MUTE_ON = new byte[] { (byte) 0x055, (byte) 0xaa, (byte) 0x06, (byte) 0x04, (byte) 0x00, (byte) 0xff };
		/**
		 * ȡ������
		 */
		public static final byte[] SET_MUTE_OFF = new byte[] { (byte) 0x055, (byte) 0xaa, (byte) 0x06, (byte) 0x04, (byte) 0x01, (byte) 0xff };
	}

	/**
	 * ���յ�����
	 * 
	 * @author Thinkman 415071574@qq.com
	 */
	public static final class RX {

	}

}
