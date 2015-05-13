package com.taobao.tddl.rule.bean;

import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;
import com.taobao.tddl.rule.mapping.DatabaseBasedMapping;
import com.taobao.tddl.rule.ruleengine.rule.AbstractMappingRule;
import com.taobao.tddl.rule.ruleengine.rule.ResultAndMappingKey;

public class MappingRule extends AbstractMappingRule {
	DatabaseBasedMapping mappingHandler ;
	//private String mappingRuleBeanId ;
	
	public DatabaseBasedMapping getMappingHandler() {
		return mappingHandler;
	}

	public void setMappingHandler(DatabaseBasedMapping mappingRule) {
		this.mappingHandler = mappingRule;
	}
	
	@Override
	protected void initInternal() {
		if(mappingHandler == null){
			throw new IllegalArgumentException("mapping rule is null");
		}
		mappingHandler.initInternal();
		super.initInternal();
	}
	/* (non-Javadoc)
	 * @see com.taobao.tddl.rule.ruleengine.rule.AbstractMappingRule#get(java.lang.String, java.lang.String, java.lang.Object)
	 */
	protected Object get(String targetKey, String sourceKey, Object sourceValue) {
		/*modified by shenxun:������ǰ��������mappingRule�����key�ģ���������ΪmappingRule��
		 * DatabaseBasedMappingRule�ֿ��ˣ���˻����MappingRule��TairBasedMappingRUle������ͬ�����⡣
		 * ��������һ������������ݿ��е�������ҵ���ڲ�ͬ��������������ǲ�ͬ�Ĵ�sql��ѡ������һ�����ݣ���Ҫӳ�䵽ӳ����ڵ�����һ��������ȥ��
		 * ��auction_num_id ��auction_auctions ,image ,spu �������ֲ�ͬ��������
		 * ����Ӧ�����ݿ������ȴ��Ψһ�ġ������Ҫ��һ��ӳ�������ֱ��ʹ��parameter����е�������
		 * 
		 */
		return mappingHandler.get(targetKey, sourceValue);
	}

	@Override
	public ResultAndMappingKey evalueateSimpleColumAndValue(String column,
			Object value, ExtraParameterContext extraParameterContext) {
		return null;
	};
}
