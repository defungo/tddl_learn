package com.taobao.tddl.interact.bean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * �����ɸѡ��
 * 
 * @author shenxun
 *
 */
public interface ComparativeMapChoicer {

	/**
	 * ����PartinationSet ��ȡ����������Ӧֵ��map.
	 * @param arguments
	 * @param partnationSet
	 * @return
	 */
	Map<String, Comparative> getColumnsMap(List<Object> arguments, Set<String> partnationSet);
	
	Comparative getColumnComparative(List<Object> arguments, String colName);
}
