package com.taobao.tddl.rule.ruleengine;

import java.util.HashMap;
import java.util.Map;

import com.taobao.tddl.rule.ruleengine.impl.DayOfMonthCalTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.DayOfWeekCalTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.DayOfWeekSunIs7CalTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.DayOfYearCalTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.ModTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.MonthCalTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.NoneTableProvider;
import com.taobao.tddl.rule.ruleengine.impl.PrimaryKeyRuleProvider;
import com.taobao.tddl.rule.ruleengine.impl.RightConvertModTabProvider;
import com.taobao.tddl.rule.ruleengine.impl.TwoColumnTabProvider;

public class TableRuleProviderRegister {
	private static final Map<String, TableRuleProvider> reg = new HashMap<String, TableRuleProvider>();

	static {
		reg.put("dayofmonth", new DayOfMonthCalTabProvider());
		reg.put("dayofweek", new DayOfWeekCalTabProvider());
		reg.put("dayofyear", new DayOfYearCalTabProvider());
		reg.put("month", new MonthCalTabProvider());
		reg.put("none", new NoneTableProvider());
		reg.put("mod", new ModTabProvider());
		reg.put("primarykey", new PrimaryKeyRuleProvider());
		reg.put("dayofweek-sun-is-7",new DayOfWeekSunIs7CalTabProvider());
		reg.put("rightconvertmod", new RightConvertModTabProvider());
		reg.put("towcolumn", new TwoColumnTabProvider());
	}

	public static TableRuleProvider getTableRuleProviderByKey(String key) {
		TableRuleProvider provider;
		if (key == null) {
			return null;
		}
		// 6_12 �ع��޸���getTableRule�ķ�����ʹ�����ͳһ
		if (key.startsWith(ModTabProvider.FUNCTION_NAME_MOD)) {
			provider = reg.get(ModTabProvider.FUNCTION_NAME_MOD);
		} else {
			
			String[] keyToken = key.split("_");
			provider = reg.get(keyToken[0]);
		}
		//TODO: provider ==null��ʱ����õ���ʾ
		// if (provider != null) {
		return provider;
		// ��Ϊ���ڵ��߼��Ѿ��������ˣ������޷�����tableExpression�ҵ�
		// ��������
		// }
		// else {
		// throw new IllegalArgumentException("key = " + key +
		// ", �����ҵ�ָ����tableRuleProvider�����");
		// }
	}
}


