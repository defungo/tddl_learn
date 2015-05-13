package com.taobao.tddl.util.IDAndDateCondition.routeCondImp;

import java.util.Map;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * �߹���������������ʽ
 * @author shenxun
 *
 */
public interface RuleRouteCondition extends RouteCondition{
	/**
	 * ������ʵ��
	 * @return
	 */
	public Map<String, Comparative> getParameters() ;

	public SqlParserResult getSqlParserResult();
}
