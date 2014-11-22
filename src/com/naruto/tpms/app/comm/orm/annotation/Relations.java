package com.naruto.tpms.app.comm.orm.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface Relations.
 * ��ʾ������
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { java.lang.annotation.ElementType.FIELD })
public @interface Relations {
	
	/**
	 * ������,������Ψһ����.
	 *
	 * @return the string
	 */
	public abstract String name();
	
	/**
	 * ���.
	 *
	 * @return the string
	 */
	public abstract String foreignKey();
	
	/**
	 * ��������.
	 *
	 * @return the string  one2one  one2many many2many
	 */
	public abstract String type();
	
	/**
	 * ��������.
	 *
	 * @return the string  query insert query_insert
	 */
	public abstract String action() default "query_insert";
}
