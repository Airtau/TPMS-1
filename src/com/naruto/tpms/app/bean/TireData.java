package com.naruto.tpms.app.bean;

/**
 * ��װ����̥������Ϣ
 * @author Thinkman
 *
 */
public class TireData {
	
	/**
	 * ��̥���,0x00��ʾ��ǰ ��0x01��ʾ��ǰ��0x10��ʾ���0x11��ʾ�Һ�
	 */
	public byte tireNum;
	/**
	 * ��̥ID
	 */
	public byte[]  tireId;
	/**
	 * ѹ��(������Ǿ��������õ���kpa)
	 */
	public double pressure;
	/**
	 * �¶�(�洢���Ǿ��������õ������϶�
	 */
	public int temperature;
	
	/**
	 * �Ƿ�©��
	 */
	public boolean isLeak;
	/**
	 * �Ƿ�͵���
	 */
	public boolean isLowBattery;
	/**
	 * �Ƿ��źŴ���
	 */
	public boolean isSignalError;
	/**
	 * �ϴα���ʱ��
	 */
	public long lastUpdateTime;
	
	/**
	 * �Ƿ���ڸ澯
	 */
	public boolean  haveAlarm;
}
