package com.taobao.tddl.client.databus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.common.jdbc.ParameterMethod;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;

/**
 * ���һ�β�ѯ����һ�����ݸ��¿�ʼʱ�ⲿ���������
 * 
 * @author junyu
 * 
 */
public class StartInfo {
	/**
	 * ԭʼsql
	 */
	private String sql;

	/**
	 * ������
	 */
	private Map<Integer, ParameterContext> sqlParam;

	/**
	 * sql����
	 */
	private SqlType sqlType;

	/**
	 * �Զ��ύ
	 */
	private boolean autoCommit;
	
	/**
	 * ���ݿ�����
	 */
	private DBType dbType;

	/**
	 * �Ƿ����в���������ִ��
	 */
	private boolean isParameterBatch = false;

	/**
	 * Ŀ��sql�Ͳ���������keyΪԭʼsql,value�е�key��Ŀ��sql,ListΪ�����б�
	 * batchʹ��
	 */
	private Map<String, Map<String, List<List<ParameterContext>>>> targetSqls;

	/**
	 * ����������sql�б�batchʹ��
	 */
	private Map<String, List<String>> targetSqlsNoParameter;

	/**
	 * sql����
	 */
	private List<Object> sqlArgs;
	
	/**
	 * ��������
	 */
	private RouteCondition rc ;
	
	/**
	 * ֱ��ָ��Rule����ָ������Դ�ƿ�sql������·�ɼ���
	 */
	private DirectlyRouteCondition directlyRouteCondition;

	/**
	 * ������ͨ��List����ʽ����ҵ��
	 * 
	 * @return
	 */
	public List<Object> getSqlParameters() {
		if (sqlParam != null) {
			List<Object> parameters = new ArrayList<Object>();
			for (ParameterContext context : sqlParam.values()) {
				if (context.getParameterMethod() != ParameterMethod.setNull1
						&& context.getParameterMethod() != ParameterMethod.setNull2) {
					parameters.add(context.getArgs()[1]);
				} else {
					parameters.add(null);
				}

			}
			return parameters;
		} else if (sqlArgs != null) {
			return sqlArgs; // getDBAndTables���Ի��õ�
		} else {
			return Collections.emptyList();
		}
	}

	public boolean isParameterBatch() {
		return isParameterBatch;
	}

	public void setParameterBatch(boolean isParameterBatch) {
		this.isParameterBatch = isParameterBatch;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public Map<Integer, ParameterContext> getSqlParam() {
		return sqlParam;
	}

	public void setSqlParam(Map<Integer, ParameterContext> sqlParam) {
		this.sqlParam = sqlParam;
	}

	public SqlType getSqlType() {
		return sqlType;
	}

	public void setSqlType(SqlType sqlType) {
		this.sqlType = sqlType;
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}
	
	public Map<String, Map<String, List<List<ParameterContext>>>> getTargetSqls() {
		return targetSqls;
	}

	public void setTargetSqls(
			Map<String, Map<String, List<List<ParameterContext>>>> targetSqls) {
		this.targetSqls = targetSqls;
	}

	public Map<String, List<String>> getTargetSqlsNoParameter() {
		return targetSqlsNoParameter;
	}

	public void setTargetSqlsNoParameter(
			Map<String, List<String>> targetSqlsNoParameter) {
		this.targetSqlsNoParameter = targetSqlsNoParameter;
	}

	public void setSqlArgs(List<Object> sqlArgs) {
		this.sqlArgs = sqlArgs;
	}

	public RouteCondition getRc() {
		return rc;
	}

	public void setRc(RouteCondition rc) {
		this.rc = rc;
	}

	public DirectlyRouteCondition getDirectlyRouteCondition() {
		return directlyRouteCondition;
	}

	public void setDirectlyRouteCondition(
			DirectlyRouteCondition directlyRouteCondition) {
		this.directlyRouteCondition = directlyRouteCondition;
	}

	public DBType getDbType() {
		return dbType;
	}

	public void setDbType(DBType dbType) {
		this.dbType = dbType;
	}
}
