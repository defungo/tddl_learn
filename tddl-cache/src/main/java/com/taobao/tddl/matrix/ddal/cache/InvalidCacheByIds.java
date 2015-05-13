/**
 * 
 */
package com.taobao.tddl.matrix.ddal.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * �ʺ� �ڷ����в�����һ��id��
 * 
 * ���� 1111,2222,3333 
 * �Զ��Ÿ���
 * @author hu.weih
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InvalidCacheByIds {
	
	/**
	 * @return cache��ַ
	 */
	int cacheArea() ;
	

}
