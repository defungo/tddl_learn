package com.taobao.tddl.client.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.Result;
import com.taobao.tddl.client.dispatcher.SingleLogicTableName;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.dispatcher.impl.DatabaseAndTablesDispatcherResultImp;
import com.taobao.tddl.client.pipeline.PipelineFactory;
import com.taobao.tddl.client.pipeline.bootstrap.Bootstrap;
import com.taobao.tddl.client.pipeline.bootstrap.PipelineBootstrap;
import com.taobao.tddl.interact.rule.VirtualTable;
import com.taobao.tddl.interact.rule.VirtualTableRoot;
import com.taobao.tddl.parser.SQLParser;
import com.taobao.tddl.rule.bean.LogicTable;
import com.taobao.tddl.rule.bean.TDDLRoot;

/**
 * ��Ҫ����root���õ���Ҫ����Ϣ��Ȼ����matcher����ƥ�䡣
 * 
 * ��󷵻���Ҫ�Ľ��
 * 
 * @author shenxun
 * 
 */
public class SpringBasedDispatcherImpl implements SqlDispatcher {
	static final Log logger = LogFactory
			.getLog(SpringBasedDispatcherImpl.class);
	/**
	 * ��Ҫע���sql ����������
	 */
	private SQLParser parser = null;

	/**
	 * TDDL�ĸ��ڵ�
	 */
	TDDLRoot root;

	/**
	 * �¹�����ڵ�
	 */
	VirtualTableRoot vtabroot;

	/**
	 * ͨ��TDataSource��ʼ��ʱע���pipelineFactory; ��Ҫ������ʹ�ã���ȻҲ���Զ���ʹ��
	 */
	private PipelineFactory pipelineFactory;

	private Bootstrap bootstrap;

	/**
	 * �����Է���
	 */
	public DispatcherResult getDBAndTables(RouteCondition rc) {
		if (null == bootstrap) {
			bootstrap = new PipelineBootstrap(null, pipelineFactory);
		}
		try {
			return bootstrap.bootstrapForGetDBAndTabs(rc, this);
		} catch (SQLException e) {
			// ������費Ӧ�ò����κ��쳣
			return null;
		}
	}

	/**
	 * �����Է���
	 */
	public DispatcherResult getDBAndTables(String sql, List<Object> args) {
		if (null == bootstrap) {
			bootstrap = new PipelineBootstrap(null, pipelineFactory);
		}
		try {
			return bootstrap.bootstrapForGetDBAndTabs(sql, args, this);
		} catch (SQLException e) {
			// ������費Ӧ�ò����κ��쳣
			return null;
		}
	}

	public Result getAllDatabasesAndTables(String logicTableName) {
		if(root==null){
			throw new RuntimeException("the root is null,may be use new rule,use getDbTopology may work!");
		}
		LogicTable logicTable = root.getLogicTable(StringUtil
				.toLowerCase(logicTableName));
		if (logicTable == null) {
			throw new IllegalArgumentException("�߼�����δ�ҵ�");
		}
		SingleLogicTableName log = new SingleLogicTableName(logicTableName);
		return new DatabaseAndTablesDispatcherResultImp(
				logicTable.getAllTargetDBList(), log);
	}

	public Map<String, Set<String>> getDbTopology(String logicTableName) {
		if(vtabroot==null){
			throw new RuntimeException("the vtabroot is null,may be use old rule, use getAllDatabasesAndTables may work!");
		}
		
		VirtualTable logicTable = vtabroot.getTableRules().get(logicTableName);
		return logicTable.getActualTopology();
	}
	
	/**
	 * ���߼���getter/setter
	 */
	public SQLParser getParser() {
		return parser;
	}

	public void setParser(SQLParser parser) {
		this.parser = parser;
	}

	public TDDLRoot getRoot() {
		return root;
	}

	public void setRoot(TDDLRoot root) {
		this.root = root;
	}

	public VirtualTableRoot getVtabroot() {
		return vtabroot;
	}

	public void setVtabroot(VirtualTableRoot vtabroot) {
		this.vtabroot = vtabroot;
	}

	public void setPipelineFactory(PipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}
}
