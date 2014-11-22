/*
 * Copyright (C) 2013 www.418log.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.naruto.tpms.app.comm.orm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

// TODO: Auto-generated Javadoc
/**
 * 
 * Copyright (c) 2012 All rights reserved
 * ���ƣ�AbDBHelper.java 
 * �������ֻ�data/data��������ݿ�
 * @author zhaoqp
 * @date��2013-7-23 ����9:47:10
 * @version v1.0
 */
public class AbDBHelper extends SQLiteOpenHelper{
	
	/** The model classes. */
	private Class<?>[] modelClasses;
	
	
	/**
	 * ��ʼ��һ��AbSDDBHelper.
	 *
	 * @param context Ӧ��context
	 * @param name ���ݿ���
	 * @param factory ���ݿ��ѯ���α깤��
	 * @param version ���ݿ���°汾��
	 * @param modelClasses Ҫ��ʼ���ı�Ķ���
	 */
	public AbDBHelper(Context context, String name,
			CursorFactory factory, int version,Class<?>[] modelClasses) {
		super(context, name, factory, version);
		this.modelClasses = modelClasses;
	}
	
	
	/**
     * ��������Ĵ���.
     *
     * @param db ���ݿ����
     * @see com.ab.db.orm.AbSDSQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    public void onCreate(SQLiteDatabase db) {
		AbTableHelper.createTablesByClasses(db, this.modelClasses);
	}

	/**
	 * ����������ؽ�.
	 *
	 * @param db ���ݿ����
	 * @param oldVersion �ɰ汾��
	 * @param newVersion �°汾��
	 * @see com.ab.db.orm.AbSDSQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		AbTableHelper.dropTablesByClasses(db, this.modelClasses);
		onCreate(db);
	}
}
