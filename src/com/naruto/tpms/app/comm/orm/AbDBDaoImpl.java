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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.naruto.tpms.app.comm.orm.annotation.ActionType;
import com.naruto.tpms.app.comm.orm.annotation.Column;
import com.naruto.tpms.app.comm.orm.annotation.Id;
import com.naruto.tpms.app.comm.orm.annotation.Relations;
import com.naruto.tpms.app.comm.orm.annotation.RelationsType;
import com.naruto.tpms.app.comm.orm.annotation.Table;
import com.naruto.tpms.app.comm.util.AbStrUtil;

// TODO: Auto-generated Javadoc
/**
 * The Class AbDBDaoImpl.
 * 
 * @param <T>
 *            the generic type
 */
public class AbDBDaoImpl<T> extends AbBasicDBDao implements AbDBDao<T> {

	/** The tag. */
	private String TAG = "AbDBDaoImpl";

	/** The db helper. */
	private SQLiteOpenHelper dbHelper;

	/** д�� */
	private final static byte[] writeLock = new byte[0];

	/** The table name. */
	private String tableName;

	/** The id column. */
	private String idColumn;

	/** The clazz. */
	private Class<T> clazz;

	/** The all fields. */
	private List<Field> allFields;

	/** The Constant METHOD_INSERT. */
	private static final int METHOD_INSERT = 0;

	/** The Constant METHOD_UPDATE. */
	private static final int METHOD_UPDATE = 1;

	/** The Constant TYPE_NOT_INCREMENT. */
	private static final int TYPE_NOT_INCREMENT = 0;

	/** The Constant TYPE_INCREMENT. */
	private static final int TYPE_INCREMENT = 1;

	/** ���Dao�����ݿ���� */
	private SQLiteDatabase db = null;

	/**
	 * ��һ������ʵ���ʼ��������ݿ����ʵ����.
	 * 
	 * @param dbHelper
	 *            ���ݿ����ʵ����
	 * @param clazz
	 *            ӳ�����ʵ��
	 */
	public AbDBDaoImpl(SQLiteOpenHelper dbHelper, Class<T> clazz) {
		this.dbHelper = dbHelper;
		if (clazz == null) {
			this.clazz = ((Class<T>) ((java.lang.reflect.ParameterizedType) super.getClass().getGenericSuperclass())
					.getActualTypeArguments()[0]);
		} else {
			this.clazz = clazz;
		}

		if (this.clazz.isAnnotationPresent(Table.class)) {
			Table table = this.clazz.getAnnotation(Table.class);
			this.tableName = table.name();
		}

		// ���������ֶ�
		this.allFields = AbTableHelper.joinFields(this.clazz.getDeclaredFields(), this.clazz.getSuperclass().getDeclaredFields());

		// �ҵ�����
		for (Field field : this.allFields) {
			if (field.isAnnotationPresent(Id.class)) {
				Column column = field.getAnnotation(Column.class);
				this.idColumn = column.name();
				break;
			}
		}

		Log.d(TAG, "clazz:" + this.clazz + " tableName:" + this.tableName + " idColumn:" + this.idColumn);
	}

	/**
	 * ��ʼ��������ݿ����ʵ����.
	 * 
	 * @param dbHelper
	 *            ���ݿ����ʵ����
	 */
	public AbDBDaoImpl(SQLiteOpenHelper dbHelper) {
		this(dbHelper, null);
	}

	/**
	 * ������TODO.
	 * 
	 * @return the db helper
	 * @see com.ab.db.orm.dao.AbDBDao#getDbHelper()
	 */
	@Override
	public SQLiteOpenHelper getDbHelper() {
		return dbHelper;
	}

	/**
	 * ��������ѯһ��.
	 * 
	 * @param id
	 *            the id
	 * @return the t
	 * @see com.ab.db.orm.dao.AbDBDao#queryOne(int)
	 */
	@Override
	public T queryOne(int id) {
		synchronized (writeLock) {
			String selection = this.idColumn + " = ?";
			String[] selectionArgs = { Integer.toString(id) };
			Log.d(TAG, "[get]: select * from " + this.tableName + " where " + this.idColumn + " = '" + id + "'");
			List<T> list = queryList(null, selection, selectionArgs, null, null, null, null);
			if ((list != null) && (list.size() > 0)) {
				return list.get(0);
			}
			return null;
		}
	}

	/**
	 * ������һ�ָ����ķ�ʽ��ѯ����֧�ֶ������������д������sql.
	 * 
	 * @param sql
	 *            ������sql�磺select * from a ,b where a.id=b.id and a.id = ?
	 * @param selectionArgs
	 *            �󶨱���ֵ
	 * @param clazz
	 *            ���صĶ�������
	 * @return the list
	 * @see com.ab.db.orm.dao.AbDBDao#rawQuery(java.lang.String,
	 *      java.lang.String[])
	 */
	@Override
	public List<T> rawQuery(String sql, String[] selectionArgs, Class<T> clazz) {

		synchronized (writeLock) {
			Log.d(TAG, "[rawQuery]: " + getLogSql(sql, selectionArgs));

			List<T> list = new ArrayList<T>();
			Cursor cursor = null;
			try {
				cursor = db.rawQuery(sql, selectionArgs);
				getListFromCursor(clazz, list, cursor);
			} catch (Exception e) {
				Log.e(this.TAG, "[rawQuery] from DB Exception.");
				e.printStackTrace();
			} finally {
				closeCursor(cursor);
			}

			return list;
		}
	}

	/**
	 * �������Ƿ����.
	 * 
	 * @param sql
	 *            the sql
	 * @param selectionArgs
	 *            the selection args
	 * @return true, if is exist
	 * @see com.ab.db.orm.dao.AbDBDao#isExist(java.lang.String,
	 *      java.lang.String[])
	 */
	@Override
	public boolean isExist(String sql, String[] selectionArgs) {
		synchronized (writeLock) {
			Log.d(TAG, "[isExist]: " + getLogSql(sql, selectionArgs));
			Cursor cursor = null;
			try {
				cursor = db.rawQuery(sql, selectionArgs);
				if (cursor.getCount() > 0) {
					return true;
				}
			} catch (Exception e) {
				Log.e(this.TAG, "[isExist] from DB Exception.");
				e.printStackTrace();
			} finally {
				closeCursor(cursor);
			}
			return false;
		}
	}

	/**
	 * ��������ѯ��������.
	 * 
	 * @return the list
	 * @see com.ab.db.orm.dao.AbDBDao#queryList()
	 */
	@Override
	public List<T> queryList() {
		return queryList(null, null, null, null, null, null, null);
	}

	/**
	 * ��������ѯ�б�.
	 * 
	 * @param columns
	 *            the columns
	 * @param selection
	 *            the selection
	 * @param selectionArgs
	 *            the selection args
	 * @param groupBy
	 *            the group by
	 * @param having
	 *            the having
	 * @param orderBy
	 *            the order by
	 * @param limit
	 *            the limit
	 * @return the list
	 * @see com.ab.db.orm.dao.AbDBDao#queryList(java.lang.String[],
	 *      java.lang.String, java.lang.String[], java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<T> queryList(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy,
			String limit) {
		synchronized (writeLock) {
			Log.d(TAG, "[queryList]");

			List<T> list = new ArrayList<T>();
			Cursor cursor = null;
			try {
				cursor = db.query(this.tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);

				getListFromCursor(this.clazz, list, cursor);

				closeCursor(cursor);

				// ��ȡ������Ĳ������ͺ͹�ϵ����
				String foreignKey = null;
				String type = null;
				String action = null;
				// ��Ҫ�ж��Ƿ��й�����
				for (Field relationsField : allFields) {
					if (!relationsField.isAnnotationPresent(Relations.class)) {
						continue;
					}

					Relations relations = relationsField.getAnnotation(Relations.class);
					// ��ȡ�������
					foreignKey = relations.foreignKey();
					// ��������
					type = relations.type();
					// ��������
					action = relations.action();
					// ���ÿɷ���
					relationsField.setAccessible(true);

					if (!(action.indexOf(ActionType.query) != -1)) {
						return list;
					}

					// �õ�������ı�����ѯ
					for (T entity : list) {

						if (RelationsType.one2one.equals(type)) {
							// һ��һ��ϵ
							// ��ȡ���ʵ��ı���
							String relationsTableName = "";
							if (relationsField.getType().isAnnotationPresent(Table.class)) {
								Table table = relationsField.getType().getAnnotation(Table.class);
								relationsTableName = table.name();
							}

							List<T> relationsList = new ArrayList<T>();
							Field[] relationsEntityFields = relationsField.getType().getDeclaredFields();
							for (Field relationsEntityField : relationsEntityFields) {
								Column relationsEntityColumn = relationsEntityField.getAnnotation(Column.class);
								// ��ȡ�����ֵ��Ϊ������Ĳ�ѯ����
								if (relationsEntityColumn.name().equals(foreignKey)) {

									// ��������ڹ������foreignKeyֵ
									String value = "-1";
									for (Field entityField : allFields) {
										// ���ÿɷ���
										entityField.setAccessible(true);
										Column entityForeignKeyColumn = entityField.getAnnotation(Column.class);
										if (entityForeignKeyColumn == null) {
											continue;
										}
										if (entityForeignKeyColumn.name().equals(foreignKey)) {
											value = String.valueOf(entityField.get(entity));
											break;
										}
									}
									// ��ѯ�������ø������
									cursor = db.query(relationsTableName, null, foreignKey + " = ?", new String[] { value }, null, null,
											null, null);
									getListFromCursor(relationsField.getType(), relationsList, cursor);
									if (relationsList.size() > 0) {
										// ��ȡ������Ķ�������ֵ
										relationsField.set(entity, relationsList.get(0));
									}

									break;
								}
							}

						} else if (RelationsType.one2many.equals(type) || RelationsType.many2many.equals(type)) {
							// һ�Զ��ϵ

							// �õ��������class���Ͷ���
							Class listEntityClazz = null;
							Class<?> fieldClass = relationsField.getType();
							if (fieldClass.isAssignableFrom(List.class)) {
								Type fc = relationsField.getGenericType();
								if (fc == null)
									continue;
								if (fc instanceof ParameterizedType) {
									ParameterizedType pt = (ParameterizedType) fc;
									listEntityClazz = (Class) pt.getActualTypeArguments()[0];
								}

							}

							if (listEntityClazz == null) {
								Log.e(TAG, "����ģ����Ҫ����List�ķ���");
								return null;
							}

							// �õ�����
							String relationsTableName = "";
							if (listEntityClazz.isAnnotationPresent(Table.class)) {
								Table table = (Table) listEntityClazz.getAnnotation(Table.class);
								relationsTableName = table.name();
							}

							List<T> relationsList = new ArrayList<T>();
							Field[] relationsEntityFields = listEntityClazz.getDeclaredFields();
							for (Field relationsEntityField : relationsEntityFields) {
								Column relationsEntityColumn = relationsEntityField.getAnnotation(Column.class);
								// ��ȡ�����ֵ��Ϊ������Ĳ�ѯ����
								if (relationsEntityColumn.name().equals(foreignKey)) {

									// ��������ڹ������foreignKeyֵ
									String value = "-1";
									for (Field entityField : allFields) {
										// ���ÿɷ���
										entityField.setAccessible(true);
										Column entityForeignKeyColumn = entityField.getAnnotation(Column.class);
										if (entityForeignKeyColumn.name().equals(foreignKey)) {
											value = String.valueOf(entityField.get(entity));
											break;
										}
									}
									// ��ѯ�������ø������
									cursor = db.query(relationsTableName, null, foreignKey + " = ?", new String[] { value }, null, null,
											null, null);
									getListFromCursor(listEntityClazz, relationsList, cursor);
									if (relationsList.size() > 0) {
										// ��ȡ������Ķ�������ֵ
										relationsField.set(entity, relationsList);
									}

									break;
								}
							}

						}
					}
				}

			} catch (Exception e) {
				Log.e(this.TAG, "[queryList] from DB Exception");
				e.printStackTrace();
			} finally {
				closeCursor(cursor);
			}

			return list;
		}
	}

	/**
	 * ��������һЩ�Ĳ�ѯ.
	 * 
	 * @param selection
	 *            the selection
	 * @param selectionArgs
	 *            the selection args
	 * @return the list
	 * @see com.ab.db.orm.dao.AbDBDao#queryList(java.lang.String,
	 *      java.lang.String[])
	 * @author: zhaoqp
	 */
	@Override
	public List<T> queryList(String selection, String[] selectionArgs) {
		return queryList(null, selection, selectionArgs, null, null, null, null);
	}

	/**
	 * ���α��л��ӳ������б�.
	 * 
	 * @param list
	 *            ���ص�ӳ������б�
	 * @param cursor
	 *            ��ǰ�α�
	 * @return the list from cursor
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InstantiationException
	 *             the instantiation exception
	 */
	private void getListFromCursor(Class<?> clazz, List<T> list, Cursor cursor) throws IllegalAccessException, InstantiationException {
		while (cursor.moveToNext()) {
			Object entity = clazz.newInstance();
			// ���������ֶ�
			List<Field> allFields = AbTableHelper.joinFields(entity.getClass().getDeclaredFields(), entity.getClass().getSuperclass()
					.getDeclaredFields());

			for (Field field : allFields) {
				Column column = null;
				if (field.isAnnotationPresent(Column.class)) {
					column = field.getAnnotation(Column.class);

					field.setAccessible(true);
					Class<?> fieldType = field.getType();

					int c = cursor.getColumnIndex(column.name());
					if (c < 0) {
						continue; // ���������ѭ���¸�����ֵ
					} else if ((Integer.TYPE == fieldType) || (Integer.class == fieldType)) {
						field.set(entity, cursor.getInt(c));
					} else if (String.class == fieldType) {
						field.set(entity, cursor.getString(c));
					} else if ((Long.TYPE == fieldType) || (Long.class == fieldType)) {
						field.set(entity, Long.valueOf(cursor.getLong(c)));
					} else if ((Float.TYPE == fieldType) || (Float.class == fieldType)) {
						field.set(entity, Float.valueOf(cursor.getFloat(c)));
					} else if ((Short.TYPE == fieldType) || (Short.class == fieldType)) {
						field.set(entity, Short.valueOf(cursor.getShort(c)));
					} else if ((Double.TYPE == fieldType) || (Double.class == fieldType)) {
						field.set(entity, Double.valueOf(cursor.getDouble(c)));
					} else if (Date.class == fieldType) {// ����java.util.Date����,update2012-06-10
						Date date = new Date();
						date.setTime(cursor.getLong(c));
						field.set(entity, date);
					} else if (Blob.class == fieldType) {
						field.set(entity, cursor.getBlob(c));
					} else if (Character.TYPE == fieldType) {
						String fieldValue = cursor.getString(c);
						if ((fieldValue != null) && (fieldValue.length() > 0)) {
							field.set(entity, Character.valueOf(fieldValue.charAt(0)));
						}
					} else if ((Boolean.TYPE == fieldType) || (Boolean.class == fieldType)) {
						String temp = cursor.getString(c);
						if ("true".equals(temp) || "1".equals(temp)) {
							field.set(entity, true);
						} else {
							field.set(entity, false);
						}
					}

				}
			}

			list.add((T) entity);
		}
	}

	/**
	 * ����������ʵ��.
	 * 
	 * @param entity
	 *            the entity
	 * @return the long
	 * @see com.ab.db.orm.dao.AbDBDao#insert(java.lang.Object)
	 */
	@Override
	public long insert(T entity) {
		return insert(entity, true);
	}

	/**
	 * ����������ʵ��.
	 * 
	 * @param entity
	 *            the entity
	 * @param flag
	 *            the flag
	 * @return the long
	 * @see com.ab.db.orm.dao.AbDBDao#insert(java.lang.Object, boolean)
	 */
	@Override
	public long insert(T entity, boolean flag) {
		synchronized (writeLock) {
			String sql = null;
			long row = 0L;
			try {
				ContentValues cv = new ContentValues();
				if (flag) {
					// id����
					sql = setContentValues(entity, cv, TYPE_INCREMENT, METHOD_INSERT);
				} else {
					// id��ָ��
					sql = setContentValues(entity, cv, TYPE_NOT_INCREMENT, METHOD_INSERT);
				}
				Log.d(TAG, "[insert]: insert into " + this.tableName + " " + sql);
				row = db.insert(this.tableName, null, cv);

				// ��ȡ������Ĳ������ͺ͹�ϵ����
				String foreignKey = null;
				String type = null;
				String action = null;
				// ��Ҫ�ж��Ƿ��й�����
				for (Field relationsField : allFields) {
					if (!relationsField.isAnnotationPresent(Relations.class)) {
						continue;
					}

					Relations relations = relationsField.getAnnotation(Relations.class);
					// ��ȡ�������
					foreignKey = relations.foreignKey();
					// ��������
					type = relations.type();
					// ��������
					action = relations.action();
					// ���ÿɷ���
					relationsField.setAccessible(true);

					if (!(action.indexOf(ActionType.insert) != -1)) {
						return row;
					}

					if (RelationsType.one2one.equals(type)) {
						// һ��һ��ϵ
						// ��ȡ������Ķ���
						T relationsEntity = (T) relationsField.get(entity);
						if (relationsEntity != null) {
							ContentValues relationsCv = new ContentValues();
							if (flag) {
								// id����
								sql = setContentValues(relationsEntity, relationsCv, TYPE_INCREMENT, METHOD_INSERT);
							} else {
								// id��ָ��
								sql = setContentValues(relationsEntity, relationsCv, TYPE_NOT_INCREMENT, METHOD_INSERT);
							}
							String relationsTableName = "";
							if (relationsEntity.getClass().isAnnotationPresent(Table.class)) {
								Table table = relationsEntity.getClass().getAnnotation(Table.class);
								relationsTableName = table.name();
							}

							Log.d(TAG, "[insert]: insert into " + relationsTableName + " " + sql);
							row += db.insert(relationsTableName, null, relationsCv);
						}

					} else if (RelationsType.one2many.equals(type) || RelationsType.many2many.equals(type)) {
						// һ�Զ��ϵ
						// ��ȡ������Ķ���
						List<T> list = (List<T>) relationsField.get(entity);

						if (list != null && list.size() > 0) {
							for (T relationsEntity : list) {
								ContentValues relationsCv = new ContentValues();
								if (flag) {
									// id����
									sql = setContentValues(relationsEntity, relationsCv, TYPE_INCREMENT, METHOD_INSERT);
								} else {
									// id��ָ��
									sql = setContentValues(relationsEntity, relationsCv, TYPE_NOT_INCREMENT, METHOD_INSERT);
								}
								String relationsTableName = "";
								if (relationsEntity.getClass().isAnnotationPresent(Table.class)) {
									Table table = relationsEntity.getClass().getAnnotation(Table.class);
									relationsTableName = table.name();
								}

								Log.d(TAG, "[insert]: insert into " + relationsTableName + " " + sql);
								row += db.insert(relationsTableName, null, relationsCv);
							}
						}

					}
				}

			} catch (Exception e) {
				Log.d(this.TAG, "[insert] into DB Exception.");
				e.printStackTrace();
				row = -1;
			} finally {
			}
			return row;
		}
	}

	/**
	 * ������������滻ʵ��.
	 * 
	 * @param entity
	 *            the entity
	 * @param flag
	 *            the flag
	 * @return the long
	 * @see com.ab.db.orm.dao.AbDBDao#insert(java.lang.Object, boolean)
	 */
	public long replace(T entity, boolean flag) {
		synchronized (writeLock) {
			String sql = null;
			long row = 0L;
			try {
				ContentValues cv = new ContentValues();
				if (flag) {
					// id����
					sql = setContentValues(entity, cv, TYPE_INCREMENT, METHOD_INSERT);
				} else {
					// id��ָ��
					sql = setContentValues(entity, cv, TYPE_NOT_INCREMENT, METHOD_INSERT);
				}
				Log.d(TAG, "[replace]: replace into " + this.tableName + " " + sql);
				row = db.replace(this.tableName, null, cv);

				// ��ȡ������Ĳ������ͺ͹�ϵ����
				String foreignKey = null;
				String type = null;
				String action = null;
				// ��Ҫ�ж��Ƿ��й�����
				for (Field relationsField : allFields) {
					if (!relationsField.isAnnotationPresent(Relations.class)) {
						continue;
					}

					Relations relations = relationsField.getAnnotation(Relations.class);
					// ��ȡ�������
					foreignKey = relations.foreignKey();
					// ��������
					type = relations.type();
					// ��������
					action = relations.action();
					// ���ÿɷ���
					relationsField.setAccessible(true);

					if (!(action.indexOf(ActionType.insert) != -1)) {
						return row;
					}

					if (RelationsType.one2one.equals(type)) {
						// һ��һ��ϵ
						// ��ȡ������Ķ���
						T relationsEntity = (T) relationsField.get(entity);
						if (relationsEntity != null) {
							ContentValues relationsCv = new ContentValues();
							if (flag) {
								// id����
								sql = setContentValues(relationsEntity, relationsCv, TYPE_INCREMENT, METHOD_INSERT);
							} else {
								// id��ָ��
								sql = setContentValues(relationsEntity, relationsCv, TYPE_NOT_INCREMENT, METHOD_INSERT);
							}
							String relationsTableName = "";
							if (relationsEntity.getClass().isAnnotationPresent(Table.class)) {
								Table table = relationsEntity.getClass().getAnnotation(Table.class);
								relationsTableName = table.name();
							}

							Log.d(TAG, "[insert]: replace into " + relationsTableName + " " + sql);
							row += db.insert(relationsTableName, null, relationsCv);
						}

					} else if (RelationsType.one2many.equals(type) || RelationsType.many2many.equals(type)) {
						// һ�Զ��ϵ
						// ��ȡ������Ķ���
						List<T> list = (List<T>) relationsField.get(entity);

						if (list != null && list.size() > 0) {
							for (T relationsEntity : list) {
								ContentValues relationsCv = new ContentValues();
								if (flag) {
									// id����
									sql = setContentValues(relationsEntity, relationsCv, TYPE_INCREMENT, METHOD_INSERT);
								} else {
									// id��ָ��
									sql = setContentValues(relationsEntity, relationsCv, TYPE_NOT_INCREMENT, METHOD_INSERT);
								}
								String relationsTableName = "";
								if (relationsEntity.getClass().isAnnotationPresent(Table.class)) {
									Table table = relationsEntity.getClass().getAnnotation(Table.class);
									relationsTableName = table.name();
								}

								Log.d(TAG, "[insert]: insert into " + relationsTableName + " " + sql);
								row += db.insert(relationsTableName, null, relationsCv);
							}
						}

					}
				}

			} catch (Exception e) {
				Log.d(this.TAG, "[insert] into DB Exception.");
				e.printStackTrace();
				row = -1;
			} finally {
			}
			return row;
		}
	}

	/**
	 * �����������б�
	 * 
	 * @see com.ab.db.orm.dao.AbDBDao#insertList(java.util.List)
	 */
	@Override
	public long insertList(List<T> entityList) {
		return insertList(entityList, true);
	}

	/**
	 * �����������б�
	 * 
	 * @see com.ab.db.orm.dao.AbDBDao#insertList(java.util.List, boolean)
	 */
	@Override
	public long insertList(List<T> entityList, boolean flag) {
		synchronized (writeLock) {
			String sql = null;
			long rows = 0;
			try {
				for (T entity : entityList) {
					ContentValues cv = new ContentValues();
					if (flag) {
						// id����
						sql = setContentValues(entity, cv, TYPE_INCREMENT, METHOD_INSERT);
					} else {
						// id��ָ��
						sql = setContentValues(entity, cv, TYPE_NOT_INCREMENT, METHOD_INSERT);
					}

					Log.d(TAG, "[insertList]: insert into " + this.tableName + " " + sql);
					rows += db.insert(this.tableName, null, cv);

					// ��ȡ������Ĳ������ͺ͹�ϵ����
					String foreignKey = null;
					String type = null;
					String action = null;
					Field field = null;
					// ��Ҫ�ж��Ƿ��й�����
					for (Field relationsField : allFields) {
						if (!relationsField.isAnnotationPresent(Relations.class)) {
							continue;
						}

						Relations relations = relationsField.getAnnotation(Relations.class);
						// ��ȡ�������
						foreignKey = relations.foreignKey();
						// ��������
						type = relations.type();
						// ��������
						action = relations.action();
						// ���ÿɷ���
						relationsField.setAccessible(true);
						field = relationsField;
					}

					if (field == null) {
						continue;
					}

					if (!(action.indexOf(ActionType.insert) != -1)) {
						continue;
					}

					if (RelationsType.one2one.equals(type)) {
						// һ��һ��ϵ
						// ��ȡ������Ķ���
						T relationsEntity = (T) field.get(entity);
						if (relationsEntity != null) {
							ContentValues relationsCv = new ContentValues();
							if (flag) {
								// id����
								sql = setContentValues(relationsEntity, relationsCv, TYPE_INCREMENT, METHOD_INSERT);
							} else {
								// id��ָ��
								sql = setContentValues(relationsEntity, relationsCv, TYPE_NOT_INCREMENT, METHOD_INSERT);
							}
							String relationsTableName = "";
							if (relationsEntity.getClass().isAnnotationPresent(Table.class)) {
								Table table = relationsEntity.getClass().getAnnotation(Table.class);
								relationsTableName = table.name();
							}

							Log.d(TAG, "[insertList]: insert into " + relationsTableName + " " + sql);
							rows += db.insert(relationsTableName, null, relationsCv);
						}

					} else if (RelationsType.one2many.equals(type) || RelationsType.many2many.equals(type)) {
						// һ�Զ��ϵ
						// ��ȡ������Ķ���
						List<T> list = (List<T>) field.get(entity);
						if (list != null && list.size() > 0) {
							for (T relationsEntity : list) {
								ContentValues relationsCv = new ContentValues();
								if (flag) {
									// id����
									sql = setContentValues(relationsEntity, relationsCv, TYPE_INCREMENT, METHOD_INSERT);
								} else {
									// id��ָ��
									sql = setContentValues(relationsEntity, relationsCv, TYPE_NOT_INCREMENT, METHOD_INSERT);
								}
								String relationsTableName = "";
								if (relationsEntity.getClass().isAnnotationPresent(Table.class)) {
									Table table = relationsEntity.getClass().getAnnotation(Table.class);
									relationsTableName = table.name();
								}

								Log.d(TAG, "[insertList]: insert into " + relationsTableName + " " + sql);
								rows += db.insert(relationsTableName, null, relationsCv);
							}
						}

					}
				}
			} catch (Exception e) {
				Log.d(this.TAG, "[insertList] into DB Exception.");
				e.printStackTrace();
			} finally {
			}

			return rows;
		}
	}

	/**
	 * ��������idɾ��.
	 * 
	 * @param id
	 *            the id
	 * @see com.ab.db.orm.dao.AbDBDao#delete(int)
	 */
	@Override
	public long delete(int id) {
		synchronized (writeLock) {
			String where = this.idColumn + " = ?";
			String[] whereValue = { Integer.toString(id) };
			Log.d(TAG, "[delete]: delelte from " + this.tableName + " where " + where.replace("?", String.valueOf(id)));
			long rows = db.delete(this.tableName, where, whereValue);
			return rows;
		}
	}

	/**
	 * ��������idɾ��.
	 * 
	 * @param ids
	 *            the ids
	 * @see com.ab.db.orm.dao.AbDBDao#delete(java.lang.Integer[])
	 */
	@Override
	public long delete(Integer... ids) {
		long rows = -1;
		if (ids.length > 0) {
			for (int i = 0; i < ids.length; i++) {
				rows += delete(ids[i]);
			}
		}
		return rows;
	}

	/**
	 * ������������ɾ������
	 * 
	 * @see com.ab.db.orm.dao.AbDBDao#delete(java.lang.String,
	 *      java.lang.String[])
	 */
	@Override
	public long delete(String whereClause, String[] whereArgs) {
		synchronized (writeLock) {
			String mLogSql = getLogSql(whereClause, whereArgs);
			if (!AbStrUtil.isEmpty(mLogSql)) {
				mLogSql += " where ";
			}
			Log.d(TAG, "[delete]: delete from " + this.tableName + mLogSql);
			long rows = db.delete(this.tableName, whereClause, whereArgs);
			return rows;
		}

	}

	/**
	 * �������������
	 * 
	 * @see com.ab.db.orm.dao.AbDBDao#deleteAll()
	 */
	@Override
	public long deleteAll() {
		synchronized (writeLock) {
			Log.d(TAG, "[delete]: delete from " + this.tableName);
			long rows = db.delete(this.tableName, null, null);
			return rows;
		}
	}

	/**
	 * ����������ʵ��.
	 * 
	 * @param entity
	 *            the entity
	 * @return the long
	 * @see com.ab.db.orm.dao.AbDBDao#update(java.lang.Object)
	 */
	@Override
	public long update(T entity) {
		synchronized (writeLock) {
			long row = 0;
			try {
				ContentValues cv = new ContentValues();

				// ע�ⷵ�ص�sql�а���������
				String sql = setContentValues(entity, cv, TYPE_NOT_INCREMENT, METHOD_UPDATE);

				String where = this.idColumn + " = ?";
				int id = Integer.parseInt(cv.get(this.idColumn).toString());
				// set sql�в��ܰ���������
				cv.remove(this.idColumn);

				Log.d(TAG, "[update]: update " + this.tableName + " set " + sql + " where " + where.replace("?", String.valueOf(id)));

				String[] whereValue = { Integer.toString(id) };
				row = db.update(this.tableName, cv, where, whereValue);
			} catch (Exception e) {
				Log.d(this.TAG, "[update] DB Exception.");
				e.printStackTrace();
			} finally {
			}
			return row;
		}
	}

	/**
	 * �����������б�
	 * 
	 * @see com.ab.db.orm.dao.AbDBDao#updateList(java.util.List)
	 */
	@Override
	public long updateList(List<T> entityList) {
		synchronized (writeLock) {
			String sql = null;
			long row = 0;
			try {
				for (T entity : entityList) {
					ContentValues cv = new ContentValues();

					sql = setContentValues(entity, cv, TYPE_NOT_INCREMENT, METHOD_UPDATE);

					String where = this.idColumn + " = ?";
					int id = Integer.parseInt(cv.get(this.idColumn).toString());
					cv.remove(this.idColumn);

					Log.d(TAG, "[update]: update " + this.tableName + " set " + sql + " where " + where.replace("?", String.valueOf(id)));

					String[] whereValue = { Integer.toString(id) };
					db.update(this.tableName, cv, where, whereValue);
				}
			} catch (Exception e) {
				Log.d(this.TAG, "[update] DB Exception.");
				e.printStackTrace();
			} finally {
			}

			return row;
		}
	}

	/**
	 * �������ContentValues.
	 * 
	 * @param entity
	 *            ӳ��ʵ��
	 * @param cv
	 *            the cv
	 * @param type
	 *            id������ͣ��Ƿ�����
	 * @param method
	 *            Ԥִ�еĲ���
	 * @return sql���ַ���
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 */
	private String setContentValues(T entity, ContentValues cv, int type, int method) throws IllegalAccessException {
		StringBuffer strField = new StringBuffer("(");
		StringBuffer strValue = new StringBuffer(" values(");
		StringBuffer strUpdate = new StringBuffer(" ");

		// ���������ֶ�
		List<Field> allFields = AbTableHelper.joinFields(entity.getClass().getDeclaredFields(), entity.getClass().getSuperclass()
				.getDeclaredFields());
		for (Field field : allFields) {
			if (!field.isAnnotationPresent(Column.class)) {
				continue;
			}
			Column column = field.getAnnotation(Column.class);

			field.setAccessible(true);
			Object fieldValue = field.get(entity);
			if (fieldValue == null)
				continue;
			if ((type == TYPE_INCREMENT) && (field.isAnnotationPresent(Id.class))) {
				continue;
			}
			// ����java.util.Date����,update
			if (Date.class == field.getType()) {
				// 2012-06-10
				cv.put(column.name(), ((Date) fieldValue).getTime());
				continue;
			}
			String value = String.valueOf(fieldValue);
			cv.put(column.name(), value);
			if (method == METHOD_INSERT) {
				strField.append(column.name()).append(",");
				strValue.append("'").append(value).append("',");
			} else {
				strUpdate.append(column.name()).append("=").append("'").append(value).append("',");
			}

		}
		if (method == METHOD_INSERT) {
			strField.deleteCharAt(strField.length() - 1).append(")");
			strValue.deleteCharAt(strValue.length() - 1).append(")");
			return strField.toString() + strValue.toString();
		} else {
			return strUpdate.deleteCharAt(strUpdate.length() - 1).append(" ").toString();
		}
	}

	/**
	 * ��������ѯΪmap�б�.
	 * 
	 * @param sql
	 *            the sql
	 * @param selectionArgs
	 *            the selection args
	 * @return the list
	 * @see com.ab.db.orm.dao.AbDBDao#queryMapList(java.lang.String,
	 *      java.lang.String[])
	 */
	@Override
	public List<Map<String, String>> queryMapList(String sql, String[] selectionArgs) {
		synchronized (writeLock) {
			Log.d(TAG, "[queryMapList]: " + getLogSql(sql, selectionArgs));
			Cursor cursor = null;
			List<Map<String, String>> retList = new ArrayList<Map<String, String>>();
			try {
				cursor = db.rawQuery(sql, selectionArgs);
				while (cursor.moveToNext()) {
					Map<String, String> map = new HashMap<String, String>();
					for (String columnName : cursor.getColumnNames()) {
						int c = cursor.getColumnIndex(columnName);
						if (c < 0) {
							continue; // ���������ѭ���¸�����ֵ
						} else {
							map.put(columnName.toLowerCase(), cursor.getString(c));
						}
					}
					retList.add(map);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "[queryMapList] from DB exception");
			} finally {
				closeCursor(cursor);
			}

			return retList;
		}
	}

	/**
	 * ��������ѯ����.
	 * 
	 * @param sql
	 *            the sql
	 * @param selectionArgs
	 *            the selection args
	 * @return the int
	 * @see com.ab.db.orm.dao.AbDBDao#queryCount(java.lang.String,
	 *      java.lang.String[])
	 */
	@Override
	public int queryCount(String sql, String[] selectionArgs) {
		synchronized (writeLock) {
			Log.d(TAG, "[queryCount]: " + getLogSql(sql, selectionArgs));
			Cursor cursor = null;
			int count = 0;
			try {
				cursor = db.query(this.tableName, null, sql, selectionArgs, null, null, null);
				if (cursor != null) {
					count = cursor.getCount();
				}
			} catch (Exception e) {
				Log.e(TAG, "[queryCount] from DB exception");
				e.printStackTrace();
			} finally {
				closeCursor(cursor);
			}
			return count;
		}
	}

	/**
	 * ������ִ���ض���sql.
	 * 
	 * @param sql
	 *            the sql
	 * @param selectionArgs
	 *            the selection args
	 * @see com.ab.db.orm.dao.AbDBDao#execSql(java.lang.String,
	 *      java.lang.Object[])
	 */
	@Override
	public void execSql(String sql, Object[] selectionArgs) {
		synchronized (writeLock) {
			Log.d(TAG, "[execSql]: " + getLogSql(sql, selectionArgs));
			try {
				if (selectionArgs == null) {
					db.execSQL(sql);
				} else {
					db.execSQL(sql, selectionArgs);
				}
			} catch (Exception e) {
				Log.e(TAG, "[execSql] DB exception.");
				e.printStackTrace();
			} finally {
			}
		}
	}

	/**
	 * 
	 * ��������ȡд���ݿ⣬���ݲ���ǰ�������
	 * 
	 * @param transaction
	 *            �Ƿ�������
	 * @throws
	 */
	public void startWritableDatabase(boolean transaction) {
		synchronized (writeLock) {
			if (db == null || !db.isOpen()) {
				db = this.dbHelper.getWritableDatabase();
			}
			if (db != null && transaction) {
				db.beginTransaction();
			}
		}

	}

	/**
	 * 
	 * ��������ȡ�����ݿ⣬���ݲ���ǰ�������
	 * 
	 * @param transaction
	 *            �Ƿ�������
	 * @throws
	 */
	public void startReadableDatabase(boolean transaction) {
		synchronized (writeLock) {
			if (db == null || !db.isOpen()) {
				db = this.dbHelper.getReadableDatabase();
			}

			if (db != null && transaction) {
				db.beginTransaction();
			}
		}

	}

	/**
	 * 
	 * ������������ɺ���������ɹ�����ܵ���closeDatabase(true);
	 * 
	 * @throws
	 */
	public void setTransactionSuccessful() {
		synchronized (writeLock) {
			if (db != null) {
				db.setTransactionSuccessful();
			}
		}

	}

	/**
	 * 
	 * �������ر����ݿ⣬���ݲ�����������
	 * 
	 * @param transaction
	 *            �ر�����
	 * @throws
	 */
	public void closeDatabase(boolean transaction) {
		synchronized (writeLock) {
			try {
				if (db != null) {
					if (transaction) {
						db.endTransaction();
					}
					if (db.isOpen()) {
						db.close();
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ��ӡ��ǰsql���.
	 * 
	 * @param sql
	 *            sql��䣬����
	 * @param args
	 *            �󶨱���
	 * @return ������sql
	 */
	private String getLogSql(String sql, Object[] args) {
		if (args == null || args.length == 0) {
			return sql;
		}
		for (int i = 0; i < args.length; i++) {
			sql = sql.replaceFirst("\\?", "'" + String.valueOf(args[i]) + "'");
		}
		return sql;
	}
}