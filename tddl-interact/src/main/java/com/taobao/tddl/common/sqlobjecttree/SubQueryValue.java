package com.taobao.tddl.common.sqlobjecttree;

import java.util.Map;

/**
 * Ϊ�˽������һ�����⣺
 * �������治��֧��һ��=��comparative�а������Or�Ĺ���ƥ�䡣
 * ������Щ�������Ƕ��ֱ�����ֵ��subSelect��̳�����ӿڣ���
 * Comparative����һ��hook��ר�Ŵ�������֮�µ�����
 * @author shenxun
 *
 */
public interface SubQueryValue extends Value {
	public void setAliasMap(Map<String, SQLFragment>  map);
	
}
