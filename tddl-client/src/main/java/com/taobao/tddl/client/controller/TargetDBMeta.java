package com.taobao.tddl.client.controller;

import java.util.ArrayList;
import java.util.List;

import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.EXECUTE_PLAN;
import com.taobao.tddl.client.dispatcher.LogicTableName;
import com.taobao.tddl.client.dispatcher.SingleLogicTableName;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.rule.ruleengine.entities.retvalue.TargetDBMetaData;
import com.taobao.tddl.sqlobjecttree.GroupFunctionType;

/**
 * �ֿ�ֱ�Դ��Ϣ ��ƽ����������Ժ��ع�controller��ʱ����ȥ��������Ϊ�˱�֤������ʵ�ֲ��ҸĶ�����С���ȷ����øĶ�
 * 
 * @author shenxun
 * 
 */
public class TargetDBMeta implements DispatcherResult {
	private final TargetDBMetaData dbMeta;

	/**
	 * maxֵ�����sql�и�����limit m,n,��rownum<xx ��maxֵ����֮�仯ΪӦ���е�ֵ
	 * ��Ҫע����ǣ�max������limitTo�ĺ��壬�����ʵ��󶼻��Ϊ xxx<max����������
	 * <p>
	 * ����oracle: rownum<=n max=n+1
	 * </p>
	 * <p>
	 * ����mysql: limit m,n max=m+n
	 * </p>
	 */
	private final int max;

	/**
	 * skipֵ�����sql�и�����limit m,n,��rownum>xx ��skipֵ����֮�仯ΪӦ���е�ֵ
	 * ��Ҫע����ǣ�skip������limitFrom�ĺ��壬�����ʵ��󶼻��Ϊ xxx>=m����������
	 * <p>
	 * ����oracle: rownum>n skip=n+1
	 * </p>
	 * <p>
	 * ����mysql: limit m,n skip=m
	 * </p>
	 */
	private final int skip;

	/**
	 * sql �е�order by ��Ϣ
	 */
	private final OrderByMessages orderByMessages;

	/**
	 * ��sql�������Ƕ�׵�select�е�columns�����group function��Ϣ�� ���ô���group
	 * function,��parser���������жϣ�ȷ��ֻ��һ��group function��û�������С���������׳��쳣 ����������group
	 * function����û�������д��ڣ���᷵�ظ�group function��Ӧ��Type ���û��group
	 * function�������������͵�sql(insert update��)���򷵻�normal.
	 */
	private final GroupFunctionType groupFunctionType;

	/**
	 * ��������Ϊ�ֿ�������ǲ��������ģ������Ǹ�ColumnMetaData����.���������xml�������˱�����е�parameters��
	 * ����ÿһ����','�ָ�����Ŀ����Ӧlist�е�һ�ColumnMetaData�е�key��Ӧ��parameters��ÿһ����','�ָ�����Ŀ
	 * ��value��Ӧ�Ѿ�ͨ�����㲢�Ұ��˱����Ժ��ֵ�����ֵ����Ϊnull,Ϊnull���ʾ�û�û����sql�и�����Ӧ �Ĳ�����
	 */
	private ColumnMetaData primaryKey;

	/**
	 * �ֿ���б���Ϊ�ֿ���������������ģ������Ǹ�list.���������xml��������parameters���ÿһ��
	 * ��','�ָ�����Ŀ����Ӧlist�е�һ�ColumnMetaData�е�key��Ӧ��parameters��ÿһ����','�ָ�����Ŀ
	 * ��value��Ӧ�Ѿ�ͨ�����㲢�Ұ��˱����Ժ��ֵ�����ֵ����Ϊnull,Ϊnull���ʾ�û�û����sql�и�����Ӧ �Ĳ�����
	 */
	private final List<ColumnMetaData> splitDB = new ArrayList<ColumnMetaData>();

	/**
	 * �ֱ������Ϊ�ֿ�������ǲ��������ģ������Ǹ�ColumnMetaData����.���������xml�������˱�����е�parameters��
	 * ����ÿһ����','�ָ�����Ŀ����Ӧlist�е�һ�ColumnMetaData�е�key��Ӧ��parameters��ÿһ����','�ָ�����Ŀ
	 * ��value��Ӧ�Ѿ�ͨ�����㲢�Ұ��˱����Ժ��ֵ�����ֵ����Ϊnull,Ϊnull���ʾ�û�û����sql�и�����Ӧ �Ĳ�����
	 */
	private final List<ColumnMetaData> splitTab = new ArrayList<ColumnMetaData>();

	/** ��join��������� */
	private final List<String> virtualJoinTableNames = new ArrayList<String>(0);

	public TargetDBMeta(TargetDBMetaData dbMeta, int skip, int max,
			OrderByMessages orderByMessages, GroupFunctionType groupFunctionType) {
		this.dbMeta = dbMeta;
		this.skip = skip;
		this.max = max;
		this.orderByMessages = orderByMessages;
		this.groupFunctionType = groupFunctionType;
	}

	public int getMax() {
		return max;
	}

	public int getSkip() {
		return skip;
	}

	public OrderByMessages getOrderByMessages() {
		return orderByMessages;
	}

	public LogicTableName getVirtualTableName() {
		return new SingleLogicTableName(dbMeta.getVirtualTableName());
	}

	public boolean needRowCopy() {
		return dbMeta.needRowCopy();
	}

	public ColumnMetaData getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(ColumnMetaData primaryKey) {
		this.primaryKey = primaryKey;
	}

	public List<ColumnMetaData> getSplitDB() {
		return splitDB;
	}

	public void addSplitDB(ColumnMetaData splitDB) {
		this.splitDB.add(splitDB);
	}

	public List<ColumnMetaData> getSplitTab() {
		return this.splitTab;
	}

	public void addSplitTab(ColumnMetaData splitTab) {
		this.splitTab.add(splitTab);
	}

	public boolean allowReverseOutput() {
		return dbMeta.allowReverseOutput();
	}

	public void needAllowReverseOutput(boolean reverse) {
		dbMeta.needAllowReverseOutput(reverse);
	}

	public GroupFunctionType getGroupFunctionType() {
		return groupFunctionType;
	}

	public List<TargetDB> getTarget() {
		return dbMeta.getTarget();
	}

	public EXECUTE_PLAN getDatabaseExecutePlan() {
		throw new IllegalStateException("not support yet");
	}

	public EXECUTE_PLAN getTableExecutePlan() {
		throw new IllegalStateException("not support yet");
	}

	public void setDatabaseExecutePlan(EXECUTE_PLAN executePlan) {
		throw new IllegalStateException("not support yet");
	}

	public void setTableExecutePlan(EXECUTE_PLAN executePlan) {
		throw new IllegalStateException("not support yet");
	}

	public boolean mappingRuleReturnNullValue() {
		return false;
	}

	public List<String> getVirtualJoinTableNames() {
		return virtualJoinTableNames;
	}

	public void setVirtualJoinTableNames(List<String> virtualJoinTableNames) {
		this.virtualJoinTableNames.addAll(virtualJoinTableNames);
	}

	public List<DatabaseExecutionContext> getDataBaseExecutionContexts() {
		return null;
	}

	@Override
	public List<String> getDistinctColumns() {

		return null;
	}

}
