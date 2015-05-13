/**
 * 
 */
package com.taobao.tddl.matrix.ddal.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ʧЧ
 * @author hu.weih
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InvalidCache{
	/**
	 * @return cache��ַ
	 */
	int cacheArea() ;
	
	/**
	 * ����������,����
	 * @return
	 */
	String idfield();	
	
}
