package com.taobao.tddl.rule.bean;

import java.util.List;

import com.taobao.tddl.interact.bean.TargetDB;

/**
 * ƥ��Ľ��
 * 
 * @author shenxun
 *
 */
public interface CalculationResult {
	/**
	 * ���ݵ�ǰ���򣬷���һ��TargetDB���б�
	 * @return
	 */
	public List<TargetDB> getTargetDBList();
	
}
