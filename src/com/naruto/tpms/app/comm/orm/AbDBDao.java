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

import java.util.List;
import java.util.Map;

import android.database.sqlite.SQLiteOpenHelper;

// TODO: Auto-generated Javadoc
/**
 * The Interface AbDBDao.
 *
 * @param <T> the generic type
 */
public interface AbDBDao<T> {

	/**
	 * ��ȡ���ݿ�.
	 *
	 * @return the db helper
	 */
	public SQLiteOpenHelper getDbHelper();

	/**
	 * ����ʵ����,Ĭ����������,����insert(T,true);.
	 *
	 * @param entity ӳ��ʵ��
	 * @return ����ɹ�������ID
	 */
	public abstract long insert(T entity);

	/**
	 * ����ʵ����.
	 *
	 * @param entity ӳ��ʵ��
	 * @param flag flagΪtrue���Զ���������,flagΪfalseʱ���ֹ�ָ��������ֵ.
	 * @return ����ɹ��������к�
	 */
	public abstract long insert(T entity, boolean flag);
	
	/**
	 * ����ʵ�����б�Ĭ����������,����insertList(List<T>,true);.
	 *
	 * @param entityList ӳ��ʵ���б�
	 * @return ����ɹ��������кŵĺ�
	 */
	public abstract long insertList(List<T> entityList);
	
	/**
	 * ����ʵ�����б�.
	 *
	 * @param entityList ӳ��ʵ���б�
	 * @param flag flagΪtrue���Զ���������,flagΪfalseʱ���ֹ�ָ��������ֵ
	 * @return ����ɹ��������кŵĺ�
	 */
	public abstract long insertList(List<T> entityList, boolean flag);
	
	

	/**
	 * ����IDɾ������.
	 *
	 * @param id ����ID����
	 */
	public abstract long delete(int id);

	/**
	 * ����IDɾ�����ݣ������.
	 *
	 * @param ids ����ID����
	 */
	public abstract long delete(Integer... ids);
	
	/**
	 * ����whereɾ������.
	 * @param whereClause where���
	 * @param whereArgs  where����
	 */
	public abstract long delete(String whereClause, String[] whereArgs);

	/**
	 * ɾ����������.
	 */
	public abstract long deleteAll();
	
	/**
	 * ��������.
	 *
	 * @param entity ����,ID����
	 * @return �޸ĳɹ��������к�
	 */
	public abstract long update(T entity);
	
	/**
	 * ��������.
	 *
	 * @param entityList �����б�,ID����
	 * @return �޸ĳɹ��������кź�
	 */
	public abstract long updateList(List<T> entityList);

	/**
	 * ���ݻ�ȡһ������.
	 *
	 * @param id ����ID����
	 * @return һ������ӳ��ʵ��
	 */
	public abstract T queryOne(int id);

	/**
	 * ִ�в�ѯ���.
	 *
	 * @param sql sql���
	 * @param selectionArgs �󶨱����Ĳ���ֵ
	 * @param clazz  ���صĶ�������
	 * @return ӳ��ʵ���б�
	 */
	public abstract List<T> rawQuery(String sql, String[] selectionArgs,Class<T> clazz);

	/**
	 * ��ѯ�б�.
	 *
	 * @return ӳ��ʵ���б�
	 */
	public abstract List<T> queryList();

	/**
	 * ӳ��ʵ���б�.
	 *
	 * @param columns ��ѯ����
	 * @param selection where����sql
	 * @param selectionArgs where����sql�İ󶨱����Ĳ���
	 * @param groupBy �������
	 * @param having �����Ĺ������
	 * @param orderBy ����
	 * @param limit limit���
	 * @return ӳ��ʵ���б�
	 */
	public abstract List<T> queryList(String[] columns, String selection,
			String[] selectionArgs, String groupBy, String having,
			String orderBy, String limit);
	
	/**
	 * ӳ��ʵ���б�.
	 * @param selection where����sql
	 * @param selectionArgs where����sql�İ󶨱����Ĳ���
	 * @return ӳ��ʵ���б�
	 */
	public abstract List<T> queryList(String selection,String[] selectionArgs);

	/**
	 * ����Ƿ��������.
	 *
	 * @param sql sql���
	 * @param selectionArgs �󶨱����Ĳ���ֵ
	 * @return ������ڷ���true, ������Ϊfalse
	 */
	public abstract boolean isExist(String sql, String[] selectionArgs);

	/**
	 * ����ѯ�Ľ������Ϊ��ֵ��map.
	 * 
	 * @param sql ��ѯsql
	 * @param selectionArgs �󶨱����Ĳ���ֵ
	 * @return ���ص�Map�е�keyȫ����Сд��ʽ.
	 */
	public List<Map<String, String>> queryMapList(String sql,String[] selectionArgs);
	
	/**
	 * ����һ����ѯ�Ľ������.
	 * @param sql ��ѯsql
	 * @param selectionArgs �󶨱����Ĳ���ֵ
	 * @return ������.
	 */
	public int queryCount(String sql,String[] selectionArgs);

	/**
	 * ��װִ��sql����.
	 *
	 * @param sql sql���
	 * @param selectionArgs �󶨱����Ĳ���ֵ
	 */
	public void execSql(String sql, Object[] selectionArgs);

}