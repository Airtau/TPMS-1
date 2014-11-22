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

import java.lang.reflect.Field;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.naruto.tpms.app.comm.orm.annotation.Column;
import com.naruto.tpms.app.comm.orm.annotation.Id;
import com.naruto.tpms.app.comm.orm.annotation.Relations;
import com.naruto.tpms.app.comm.orm.annotation.Table;
import com.naruto.tpms.app.comm.util.AbStrUtil;

// TODO: Auto-generated Javadoc
/**
 * The Class AbTableHelper.
 */
public class AbTableHelper {

	/** ��־���. */
	private static final String TAG = "AbTableHelper";

	/**
	 * ����ӳ��Ķ��󴴽���.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param db
	 *            ���ݿ����
	 * @param clazzs
	 *            ����ӳ��
	 */
	public static <T> void createTablesByClasses(SQLiteDatabase db, Class<?>[] clazzs) {
		for (Class<?> clazz : clazzs) {
			createTable(db, clazz);
		}
	}

	/**
	 * ����ӳ��Ķ���ɾ����.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param db
	 *            ���ݿ����
	 * @param clazzs
	 *            ����ӳ��
	 */
	public static <T> void dropTablesByClasses(SQLiteDatabase db, Class<?>[] clazzs) {
		for (Class<?> clazz : clazzs) {
			dropTable(db, clazz);
		}
	}

	/**
	 * ������.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param db
	 *            ����ӳ��Ķ��󴴽���.
	 * @param clazz
	 *            ����ӳ��
	 */
	public static <T> void createTable(SQLiteDatabase db, Class<T> clazz) {
		String tableName = "";
		if (clazz.isAnnotationPresent(Table.class)) {
			Table table = clazz.getAnnotation(Table.class);
			tableName = table.name();
		}
		if (AbStrUtil.isEmpty(tableName)) {
			Log.d(TAG, "��Ҫӳ���ʵ��[" + clazz.getName() + "],δע��@Table(name=\"?\"),������");
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ").append(tableName).append(" (");

		List<Field> allFields = AbTableHelper.joinFieldsOnlyColumn(clazz.getDeclaredFields(), clazz.getSuperclass().getDeclaredFields());
		for (Field field : allFields) {
			if (!field.isAnnotationPresent(Column.class)) {
				continue;
			}

			Column column = field.getAnnotation(Column.class);

			String columnType = "";
			if (column.type().equals(""))
				columnType = getColumnType(field.getType());
			else {
				columnType = column.type();
			}

			sb.append(column.name() + " " + columnType);

			if (column.length() != 0) {
				sb.append("(" + column.length() + ")");
			}
			// ʵ���ඨ��ΪInteger���ͺ�������Id�쳣
			if ((field.isAnnotationPresent(Id.class)) && ((field.getType() == Integer.TYPE) || (field.getType() == Integer.class)))
				sb.append(" primary key autoincrement");
			else if (field.isAnnotationPresent(Id.class)) {
				sb.append(" primary key");
			}

			sb.append(", ");
		}

		sb.delete(sb.length() - 2, sb.length() - 1);
		sb.append(")");

		String sql = sb.toString();

		Log.d(TAG, "crate table [" + tableName + "]: " + sql);

		db.execSQL(sql);
	}

	/**
	 * ɾ����.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param db
	 *            ����ӳ��Ķ��󴴽���.
	 * @param clazz
	 *            ����ӳ��
	 */
	public static <T> void dropTable(SQLiteDatabase db, Class<T> clazz) {
		String tableName = "";
		if (clazz.isAnnotationPresent(Table.class)) {
			Table table = clazz.getAnnotation(Table.class);
			tableName = table.name();
		}
		String sql = "DROP TABLE IF EXISTS " + tableName;
		Log.d(TAG, "dropTable[" + tableName + "]:" + sql);
		db.execSQL(sql);
	}

	/**
	 * ��ȡ������.
	 * 
	 * @param fieldType
	 *            the field type
	 * @return ������
	 */
	private static String getColumnType(Class<?> fieldType) {
		if (String.class == fieldType) {
			return "TEXT";
		}
		if ((Integer.TYPE == fieldType) || (Integer.class == fieldType)) {
			return "INTEGER";
		}
		if ((Long.TYPE == fieldType) || (Long.class == fieldType)) {
			return "BIGINT";
		}
		if ((Float.TYPE == fieldType) || (Float.class == fieldType)) {
			return "FLOAT";
		}
		if ((Short.TYPE == fieldType) || (Short.class == fieldType)) {
			return "INT";
		}
		if ((Double.TYPE == fieldType) || (Double.class == fieldType)) {
			return "DOUBLE";
		}
		if (Blob.class == fieldType) {
			return "BLOB";
		}

		return "TEXT";
	}

	/**
	 * �ϲ�Field���鲢ȥ��,��ʵ�ֹ��˵���Column�ֶ�,��ʵ��Id�������ֶ�λ�ù���.
	 * 
	 * @param fields1
	 *            ��������1
	 * @param fields2
	 *            ��������2
	 * @return ���Ե��б�
	 */
	public static List<Field> joinFieldsOnlyColumn(Field[] fields1, Field[] fields2) {
		Map<String, Field> map = new LinkedHashMap<String, Field>();
		for (Field field : fields1) {
			// ���˵���Column������ֶ�
			if (!field.isAnnotationPresent(Column.class)) {
				continue;
			}
			Column column = field.getAnnotation(Column.class);
			map.put(column.name(), field);
		}
		for (Field field : fields2) {
			// ���˵���Column������ֶ�
			if (!field.isAnnotationPresent(Column.class)) {
				continue;
			}
			Column column = field.getAnnotation(Column.class);
			if (!map.containsKey(column.name())) {
				map.put(column.name(), field);
			}
		}
		List<Field> list = new ArrayList<Field>();
		for (String key : map.keySet()) {
			Field tempField = map.get(key);
			// �����Id�������λ��.
			if (tempField.isAnnotationPresent(Id.class)) {
				list.add(0, tempField);
			} else {
				list.add(tempField);
			}
		}
		return list;
	}

	/**
	 * �ϲ�Field���鲢ȥ��.
	 * 
	 * @param fields1
	 *            ��������1
	 * @param fields2
	 *            ��������2
	 * @return ���Ե��б�
	 */
	public static List<Field> joinFields(Field[] fields1, Field[] fields2) {
		Map<String, Field> map = new LinkedHashMap<String, Field>();
		for (Field field : fields1) {
			// ���˵���Column��Relations������ֶ�
			if (field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);
				map.put(column.name(), field);
			} else if (field.isAnnotationPresent(Relations.class)) {
				Relations relations = field.getAnnotation(Relations.class);
				map.put(relations.name(), field);
			}

		}
		for (Field field : fields2) {
			// ���˵���Column��Relations������ֶ�
			if (field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);
				if (!map.containsKey(column.name())) {
					map.put(column.name(), field);
				}
			} else if (field.isAnnotationPresent(Relations.class)) {
				Relations relations = field.getAnnotation(Relations.class);
				if (!map.containsKey(relations.name())) {
					map.put(relations.name(), field);
				}
			}
		}
		List<Field> list = new ArrayList<Field>();
		for (String key : map.keySet()) {
			Field tempField = map.get(key);
			// �����Id�������λ��.
			if (tempField.isAnnotationPresent(Id.class)) {
				list.add(0, tempField);
			} else {
				list.add(tempField);
			}
		}
		return list;
	}
}