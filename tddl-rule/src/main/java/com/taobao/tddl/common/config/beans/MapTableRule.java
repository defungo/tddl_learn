package com.taobao.tddl.common.config.beans;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.taobao.tddl.rule.bean.Database;
import com.taobao.tddl.rule.bean.LogicTable;
import com.taobao.tddl.rule.bean.Table;
import com.taobao.tddl.rule.groovy.GroovyListRuleEngine;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.RuleChain;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement;
import com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker.DatabaseMapProvider;
import com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker.TableMapProvider;
import com.taobao.tddl.rule.ruleengine.util.RuleUtils;

/**
 * �����Լ�ָ�����зֿ�ͷֱ� ���������ͳһ��Ҳ���Ǳ�������Ϳ������ȫ��Ψһ
 * 
 * @author shenxun
 */
public class MapTableRule extends LogicTable {
	public MapTableRule() {
	}

	/**
	 * ���map.key���Ǽ��������ȡ��key,value����
	 */
	private Map<String, String> dbMap = null;
	private Map<String, String> tabMap = null;

	List<Object> dbShardingRule = new LinkedList<Object>();
	List<Object> tabShardingRule = new LinkedList<Object>();
    
	@Override
	public void init() {
		init(true);
	}

	@Override
	public void init(boolean invokeBySpring) {
		final Map<String, String> tempDBMap = dbMap;
		DatabaseMapProvider mapProvider = new DatabaseMapProvider() {
			public Map<String, Database> getDatabaseMap() {
				Map<String, Database> retMap = new HashMap<String, Database>(
						tempDBMap.size());
				for (Entry<String, String> entry : tempDBMap.entrySet()) {
					Database db = new Database();
					db.setDataSourceKey(entry.getValue());
					retMap.put(entry.getKey(), db);
				}
				return retMap;
			}
		};
		// ����dbMap
		setDatabaseMapProvider(mapProvider);

		// ����tabMap
		final Map<String, String> tempTabMap = tabMap;
		TableMapProvider tabMap = new TableMapProvider() {

			public void setParentID(String parentID) {
				// needn't do anything.
			}

			public void setLogicTable(String logicTable) {
				// needn't do anything
			}

			public Map<String, SharedElement> getTablesMap() {
				Map<String, SharedElement> retMap = new HashMap<String, SharedElement>(
						tempTabMap.size());
				for (Entry<String, String> entry : tempTabMap.entrySet()) {
					Table table = new Table();
					table.setTableName(entry.getValue());
					retMap.put(entry.getKey(), table);
				}
				return retMap;
			}
		};
		setTableMapProvider(tabMap);

		// ��ʼ������
		boolean isDatabase = true;
		RuleChain rc = RuleUtils.getRuleChainByRuleStringList(dbShardingRule,
				GroovyListRuleEngine.class, isDatabase,extraPackagesStr);
		super.listResultRule = rc;

		rc = RuleUtils.getRuleChainByRuleStringList(tabShardingRule,
				GroovyListRuleEngine.class, !isDatabase,extraPackagesStr);
		setTableRuleChain(rc);
		super.init(invokeBySpring);
	}

	public List<Object> getDbShardingRule() {
		return dbShardingRule;
	}

	public void setDbShardingRule(List<Object> dbShardingRule) {
		this.dbShardingRule = dbShardingRule;
	}

	public List<Object> getTabShardingRule() {
		return tabShardingRule;
	}

	public void setTabShardingRule(List<Object> tabShardingRule) {
		this.tabShardingRule = tabShardingRule;
	}
}
