package com.taobao.tddl.rule.ruleengine.cartesianproductcalculator;

import java.util.List;

import com.taobao.tddl.interact.rule.bean.SamplingField;

/**
 * �����ӳ�丽�����ֶΣ�
 * 
 * @author shenxun
 *
 */
public abstract class MappingSamplingField extends SamplingField{

	public MappingSamplingField(List<String> columns,int capacity) {
		super(columns,capacity);
	}

}
