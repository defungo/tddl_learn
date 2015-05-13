package com.taobao.tddl.matrix.ddal.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 
 * ��� @CacheKeyʹ��
 * @author hu.weih
 * @see com.taobao.matrix.ddal.cache.CacheKey
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InvalidCacheByKey {
	/**
	 * @return cache��ַ
	 */
	int cacheArea()  default 0;
}
