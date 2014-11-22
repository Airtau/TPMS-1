package com.naruto.tpms.app.comm.orm;

import android.content.Context;

import com.naruto.tpms.app.bean.DeviceBind;

/**
 * 
 * Copyright (c) 2012 All rights reserved ���ƣ�DBInsideHelper.java
 * �������ֻ�data/data��������ݿ�
 * 
 * @author zhaoqp
 * @date��2013-7-31 ����3:50:18
 * @version v1.0
 */
public class DBInsideHelper extends AbDBHelper {
	// ���ݿ���
	private static final String DBNAME = "lightControl.db";

	// ��ǰ���ݿ�İ汾
	private static final int DBVERSION = 1;
	// TODO Ҫ��ʼ���ı�
	private static final Class<?>[] clazz = { DeviceBind.class };

	public DBInsideHelper(Context context) {
		super(context, DBNAME, null, DBVERSION, clazz);
	}

}
