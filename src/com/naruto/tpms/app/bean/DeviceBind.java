package com.naruto.tpms.app.bean;

/**
 * �Ҿӿ�����λ�ø������豸�󶨹�ϵ
 * 
 * @author Thinkman 415071574@qq.com
 */

import com.naruto.tpms.app.comm.orm.annotation.Column;
import com.naruto.tpms.app.comm.orm.annotation.Id;
import com.naruto.tpms.app.comm.orm.annotation.Table;

@Table(name = "device_bind")
public class DeviceBind {

	/**
	 * ��Ƭ��������ID
	 */
	@Id
	@Column(name = "id")
	public int deviceId;
	/**
	 * �Ѱ�������mac��ַ
	 */
	@Column(name = "bt_mac", length = 17)
	public String btMac;
	/**
	 * �Ѱ��������豸����
	 */
	@Column(name = "bt_name")
	public String btName;
}
