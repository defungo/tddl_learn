package com.taobao.tddl.client.databus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.client.controller.DatabaseExecutionContext;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.handler.AbstractHandler.FlowType;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.rule.VirtualTableRoot;
import com.taobao.tddl.parser.SQLParser;
import com.taobao.tddl.rule.bean.TDDLRoot;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * ���һ�β�ѯ����һ�����ݸ��¹����е�����
 * 
 * @author junyu
 *
 */
public class PipelineRuntimeInfo {
	public static final String INFO_NAME="PipelineRuntimeInfo";
	/**
	 * ��ʼ��Ϣ��ԭʼsql,������
	 */
	private StartInfo startInfo;
	
	/**
	 * Sql���������RouteConditionҲ��ģ�����һ����
	 */
    private SqlParserResult sqlParserResult;
    
    /**
     * �Ƿ񾭹���Sql��������RouteCondition����false��
     */
    private boolean isSqlParsed;
    
    /**
     * �߼����������Դ�sqlParserResult�еõ�
     */
    private Set<String> logicTableNames;
    
    /**
     * �Ƿ����������
     */
    private boolean isAllowReverseOutput;
   
    /**
     * ��������Ҫִ�е�sql�����ݿ�
     */
	private DispatcherResult metaData;
	
	/**
	 * ����ƥ����ƥ����Ľ��
	 */
	private MatcherResult matcherResult ;
	
	/**
	 * ִ�мƻ������ս������ݲ�ѯ�������ݸ��´�����������
	 */
	private ExecutionPlan executionPlan;
	
	/**
	 * ����һ�׹����ڳ�ʼ��ʱ�ӹ������ѡ�У�Ҳ�п���ָ��������һ�׹���
	 */
	private SqlDispatcher sqlDispatcher;
	
	/**
	 * ��ת����
	 */
	private FlowType flowType;
	
	private boolean needRowCopy=false;
	
	private List<String> uniqueColumns;
    
	/**
	 * ��join���������
	 */
	private List<String> virtualJoinTableNames= new ArrayList<String>();
	
	List<List<TargetDB>> targetDBList;
	
	List<DatabaseExecutionContext> dataBaseExecutionContext;
	
	private Map<String, String> alias ;
	
	/**
	 * id in�����ʶ
	 */
	private boolean needIdInGroup;
	
	/**
	 * �����distinct��ʶ
	 */
	private boolean completeDistinct;
	
	/**
	 * sql�д���groupHint,��Ҫ��ͨ��sql������
	 * ֮ǰȥ��,�ڽ�������ӻ�,��ʱ��֧�ֲ���
	 * ռλ
	 */
	private String groupHintStr;
	
	public SqlParserResult getSqlParserResult() {
		return sqlParserResult;
	}
	
	public void setSqlParserResult(SqlParserResult sqlParserResult) {
		this.sqlParserResult = sqlParserResult;
	}
	
	public boolean getIsSqlParsed() {
		return isSqlParsed;
	}
	
	public void setIsSqlParsed(boolean isSqlParsed) {
		this.isSqlParsed = isSqlParsed;
	}
	
	public Set<String> getLogicTableNames() {
		return logicTableNames;
	}
	
	public void setLogicTableNames(Set<String> logicTableNames) {
		this.logicTableNames = logicTableNames;
	}
	
	public boolean isAllowReverseOutput() {
		return isAllowReverseOutput;
	}
	
	public void setAllowReverseOutput(boolean isAllowReverseOutput) {
		this.isAllowReverseOutput = isAllowReverseOutput;
	}
	
	public SqlDispatcher getSqlDispatcher() {
		return sqlDispatcher;
	}
	
	public void setSqlDispatcher(SqlDispatcher sqlDispatcher) {
		this.sqlDispatcher = sqlDispatcher;
	}
	
	public DispatcherResult getMetaData() {
		return metaData;
	}
	
	public void setMetaData(DispatcherResult metaData) {
		this.metaData = metaData;
	}
	
	public MatcherResult getMatcherResult() {
		return matcherResult;
	}
	
	public void setMatcherResult(MatcherResult matcherResult) {
		this.matcherResult = matcherResult;
	}
	
	public ExecutionPlan getExecutionPlan() {
		return executionPlan;
	}
	
	public void setExecutionPlan(ExecutionPlan executionPlan) {
		this.executionPlan = executionPlan;
	}
	
	public TDDLRoot getTDDLRoot(){
		return this.sqlDispatcher.getRoot();
	}
	
	public VirtualTableRoot getVirtualTableRoot(){
		return this.sqlDispatcher.getVtabroot();
	}
	
	public SQLParser getSQLParser(){
		return this.sqlDispatcher.getParser();
	}
	
	public StartInfo getStartInfo() {
		return startInfo;
	}
	
	public void setStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
	}

	public List<String> getVirtualJoinTableNames() {
		return virtualJoinTableNames;
	}

	public void setVirtualJoinTableNames(List<String> virtualJoinTableNames) {
		this.virtualJoinTableNames = virtualJoinTableNames;
	}

	public FlowType getFlowType() {
		return flowType;
	}

	public void setFlowType(FlowType flowType) {
		this.flowType = flowType;
	}

	public List<List<TargetDB>> getTargetDBList() {
		return targetDBList;
	}

	public void setTargetDBList(List<List<TargetDB>> targetDBList) {
		this.targetDBList = targetDBList;
	}

	public List<DatabaseExecutionContext> getDataBaseExecutionContext() {
		return dataBaseExecutionContext;
	}

	public void setDataBaseExecutionContext(
			List<DatabaseExecutionContext> dataBaseExecutionContext) {
		this.dataBaseExecutionContext = dataBaseExecutionContext;
	}

	public Map<String, String> getAlias() {
		return alias;
	}

	public void setAlias(Map<String, String> alias) {
		this.alias = alias;
	}

	public boolean isNeedRowCopy() {
		return needRowCopy;
	}

	public void setNeedRowCopy(boolean needRowCopy) {
		this.needRowCopy = needRowCopy;
	}

	public List<String> getUniqueColumns() {
		return uniqueColumns;
	}

	public void setUniqueColumns(List<String> uniqueColumns) {
		this.uniqueColumns = uniqueColumns;
	}

	public boolean isNeedIdInGroup() {
		return needIdInGroup;
	}

	public void setNeedIdInGroup(boolean needIdInGroup) {
		this.needIdInGroup = needIdInGroup;
	}

	public boolean isCompleteDistinct() {
		return completeDistinct;
	}

	public void setCompleteDistinct(boolean completeDistinct) {
		this.completeDistinct = completeDistinct;
	}

	public String getGroupHintStr() {
		return groupHintStr;
	}

	public void setGroupHintStr(String groupHintStr) {
		this.groupHintStr = groupHintStr;
	}
}
