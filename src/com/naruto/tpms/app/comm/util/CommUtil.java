package com.naruto.tpms.app.comm.util;

import java.text.DecimalFormat;

import com.naruto.tpms.app.comm.AppContext;
import com.naruto.tpms.app.comm.Constants;
import com.naruto.tpms.app.weight.TipsToast;

import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * ����ͨ�ù���
 * 
 * @author Thinkman
 * 
 */
public class CommUtil {

	/**
	 * ����У��λ
	 * 
	 * @param bytes
	 *            �ֽ�������
	 * @param startPosition
	 *            ����У��������ʼλ��
	 * @param endPosition
	 *            ����У���������λ��
	 * @return
	 */
	public static byte getCheckByte(byte[] bytes, int startPosition, int endPosition) {
		byte result = 0x00;
		for (int i = startPosition; i <= endPosition; i++) {
			result ^= bytes[i];
		}
		return result;
	}

	/**
	 * �ֽ�ת�������ַ���
	 * 
	 * @param in
	 * @return
	 */
	public static String byteToBinaryString(byte in) {
		String result = Integer.toBinaryString(in);
		if (result.length() > 8) {
			result = result.substring(result.length() - 8);
		} else if (result.length() < 8) {
			result = "00000000".substring(result.length()) + result;
		}
		return result;
	}

	static TipsToast tipsToast;

	/**
	 * ��icon��Toast��ʾ
	 * 
	 * @param iconResId
	 * @param msgResId
	 */
	public static void showTips(int iconResId, int msgResId) {
		if (tipsToast != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				tipsToast.cancel();
			}
		} else {
			tipsToast = TipsToast.makeText(AppContext.getInstance().getBaseContext(), msgResId, TipsToast.LENGTH_LONG);
		}
		tipsToast.show();
		tipsToast.setIcon(iconResId);
		tipsToast.setText(msgResId);
	}

	public static void showTips(int iconResId, String message) {
		if (tipsToast != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				tipsToast.cancel();
			}
		} else {
			tipsToast = TipsToast.makeText(AppContext.getInstance().getBaseContext(), message, TipsToast.LENGTH_LONG);
		}
		tipsToast.show();
		tipsToast.setIcon(iconResId);
		tipsToast.setText(message);
	}

	// ������ʾ��ѹ�����¶ȵ�λ
	public static final String[] pressureUnits = new String[] { "Psi", "KPa", "Bar" };
	public static final String[] temperatureUnits = new String[] { "��C", "��F" };
	static DecimalFormat pressureFormater = new DecimalFormat("0.0");
	static DecimalFormat pressurekPaFormater = new DecimalFormat("0");
	static DecimalFormat tempratureFormater = new DecimalFormat("0");

	/**
	 * 
	 * @param pressureVlue
	 *            ѹ��ֵ����λΪkPa
	 * @param pressureUnitIndex
	 *            ��ǰ��λindex
	 * @return
	 */
	public static String getPressure(double pressureVlue, int pressureUnitIndex) {
		double num = 0.0d;
		switch (pressureUnitIndex) {
		case 0:
			num = 0.145037d * pressureVlue;
			break;
		case 1:
			num = pressureVlue;
			return pressurekPaFormater.format(num);
		case 2:
			num = 0.01 * pressureVlue;
			break;
		}
		String str = pressureFormater.format(num);
		return str;// +
											// pressureUnits[pressureUnitIndex];
	}

	public static String getPressureWithUnit(double pressureVlue, int pressureUnitIndex) {
		double num = 0.0d;
		switch (pressureUnitIndex) {
		case 0:
			num = 0.145037d * pressureVlue;
			break;
		case 1:
			num = pressureVlue;
			break;
		case 2:
			num = 0.01 * pressureVlue;
			break;
		}
		return pressureFormater.format(num) + pressureUnits[pressureUnitIndex];
	}

	/**
	 * @param temperatureValue
	 *            ԭʼ�¶�ֵ,��λ��C
	 * @param temperatureUnitIndex
	 *            ��λ���
	 * @return
	 */
	public static String getTemperatureWithUnit(double temperatureValue, int temperatureUnitIndex) {
		double num = 0.0d;
		switch (temperatureUnitIndex) {
		case 0:
			num = (int)temperatureValue;
			break;
		case 1:
			num = (int)temperatureValue * 9 / 5 + 32;
			break;
		}
		String str = tempratureFormater.format(num) + temperatureUnits[temperatureUnitIndex];
		return str;
	}

	public static String byte2HexStr(byte[] b) {
		String hs = "";
		String stmp = "";
		for (int n = 0; n < b.length; n++) {
			stmp = (Integer.toHexString(b[n] & 0XFF));
			if (stmp.length() == 1)
				hs = hs + "0" + stmp;
			else
				hs = hs + stmp;
			// if (n<b.length-1) hs=hs+":";
		}
		return hs.toUpperCase();
	}

	/**
	 * ������̥λ�ñ�Ż����̥λ��index
	 * 
	 * @param tireNum
	 * @return
	 */
	public static int getTireIndexByTireNum(byte tireNum) {
		for (int i = Constants.TIRE_NUMS.length - 1; i >= 0; i--) {
			if (tireNum == Constants.TIRE_NUMS[i]) {
				return i;
			}
		}
		return -1;
	}

}
