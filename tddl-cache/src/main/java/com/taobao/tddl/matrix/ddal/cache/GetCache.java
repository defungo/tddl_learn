package com.taobao.tddl.matrix.ddal.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ��ȡcache
 * @author hu.weih
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetCache {
	/**
	 * @return cache��ַ
	 */
	int cacheArea() default 0;
	
	/**
	 * ��λ ��
	 * ʧЧʱ�� 0 ���� -1 ��Զ��ʧЧ 
	 * <br> Ĭ��1��
	 * @return
	 */
	int expire() default 86400;
	
	/**
	 * �Ƿ������null��cache�С�
	 * <br>���� key value�����ݽṹ�Ϻ��ʺ�
	 * @return
	 */
	boolean canCacheNull() default false;
}
