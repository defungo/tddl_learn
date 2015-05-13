package com.taobao.tddl.interact.bean;

import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * ƥ��Ľ�����󣬹�����Controller���з��ض����ƴװ
 * 
 * 
 * ��Щ�Ǵ���Ĵ�ƥ���п��Ի�õ����� ��Ҫ��Ӧ������Щ����Щ���Ƿ���������ֿ�ֱ����
 * 
 * @author shenxun
 *
 */
public interface MatcherResult {
	/**
	 * ��������Ľ������
	 * @return
	 */
	List<TargetDB> getCalculationResult();
	
	/**
	 * ƥ��Ŀ������ʲô,�������Nullֵ
	 * @return
	 */
	Map<String, Comparative> getDatabaseComparativeMap();
	
	/**
	 * ƥ��ı������ʲô,�������nullֵ
	 * @return
	 */
	Map<String,Comparative> getTableComparativeMap();
}
