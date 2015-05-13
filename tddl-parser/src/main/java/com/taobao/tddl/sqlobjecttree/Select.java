package com.taobao.tddl.sqlobjecttree;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.common.exception.runtime.NotSupportException;
import com.taobao.tddl.common.sqlobjecttree.Column;
import com.taobao.tddl.common.sqlobjecttree.SQLFragment;
import com.taobao.tddl.common.sqlobjecttree.SubQueryValue;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.sqlobjecttree.common.TableNameFunctionImp;
import com.taobao.tddl.sqlobjecttree.common.expression.ExpressionGroup;

public class Select extends DMLCommon implements SubQueryValue {
	String tempWhereStr = null;
	GroupFunctionType type = GroupFunctionType.NORMAL;
	boolean isSubSelect = false;
	protected SelectUpdate forUpdate;

	public boolean isSubSelect() {
		return isSubSelect;
	}

	public void setSubSelect(boolean isSubSelect) {
		this.isSubSelect = isSubSelect;
	}

	// protected TableNameImp tbName = new TableNameImp();
	protected WhereCondition where = null;;
	protected Columns columns = new Columns();

	// add by junyu
	protected HavingCondition having = null;

	protected WhereCondition getWhereCondition() {
		return new WhereCondition();
	}

	protected HavingCondition getHavingCondition() {
		return new HavingCondition();
	}

	public Select() {
		where = getWhereCondition();
		where.setHolder(holder);

		having = getHavingCondition();
		// FIXME:����ô��⣬���Ǹ���walker����Ҫ����
		having.setHolder(holder);
	}

	public Select(BindIndexHolder holder) {
		super.holder = holder;
		where = getWhereCondition();
		where.setHolder(holder);

		having = getHavingCondition();
		having.setHolder(holder);
	}

	public WhereCondition getWhere() {
		return where;
	}

	public HavingCondition getHaving() {
		return having;
	}

	public Columns getColumns() {
		return columns;
	}

	public void setColumns(Columns columns) {
		this.columns = columns;
	}

	public void addColumn(String table, String column, String alias) {
		columns.addColumn(table, column, alias);
	}

	public void addColumn(Column col) {
		columns.addColumn(col);
	}

	public void addAndWhereExpressionGroup(ExpressionGroup exp) {
		where.addAndExpression(exp);
	}



	public TableNameFunctionImp getTableNameFunction() {
		TableNameFunctionImp snap = null;
		for (TableName name : tbNames) {
			if (name instanceof TableNameFunctionImp) {
				TableNameFunctionImp tbNameFunction = (TableNameFunctionImp) name;
				if (snap != null) {
					throw new IllegalStateException("������һ��sql���г��ֶ����������");
				} else {
					snap = tbNameFunction;
				}
			}
		}
		return snap;
	}

	public void appendSQL(StringBuilder sb) {
		buildSelectString(sb);
	}

	protected void buildSelectString(StringBuilder sb) {
		appendSelect(sb);
		columns.appendSQL(sb);
		if (hasTable()) {
			sb.append(" FROM ");
		}
		super.appendSQL(sb);
		where.appendSQL(sb);
		having.appendSQL(sb);
		if (forUpdate != null) {
			forUpdate.appendSQL(sb);
		}
	}



	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendSelect(sb);
		columns.appendSQL(sb);
		if (hasTable()) {
			sb.append(" FROM ");
		}
		sb.append(super.toString());
		sb.append(where.toString());
		sb.append(having.toString());
		if (forUpdate != null) {
			forUpdate.appendSQL(sb);
		}
		return sb.toString();
	}

	/**
	 * FIXME:����having��ʱ����eval()
	 */
	@Override
	public Map<String, Comparative> getSubColumnsMap() {
		return where.eval();
	}

	public List<OrderByEle> nestGetOrderByList() {
		return where.getOrderByColumns();
	}

	public List<OrderByEle> nestGetGroupByList() {
		return where.getGroupByColumns();
	}

	@Override
	public StringBuilder regTableModifiable(Set<String> oraTabName,
			List<Object> list, StringBuilder sb) {
		sb = buildSqlStringWithModifiable(oraTabName, list, sb);
		return sb;
	}

	protected StringBuilder buildSqlStringWithModifiable(
			Set<String> oraTabName, List<Object> list, StringBuilder sb) {
		appendSelect(sb);
		return appendSelectBody(oraTabName, list, sb);
	}

	protected StringBuilder appendSelectBody(Set<String> oraTabName,
			List<Object> list, StringBuilder sb) {
		sb = columns.regTableModifiable(oraTabName, list, sb);
		if (hasTable()) {
			sb.append(" FROM ");
		}
		sb = super.regTableModifiable(oraTabName, list, sb);
		sb = where.regTableModifiable(oraTabName, list, sb);
		sb = having.regTableModifiable(oraTabName, list, sb);
		if (forUpdate != null) {
			sb = forUpdate.regTableModifiable(oraTabName, list, sb);
		}
		return sb;
	}

	protected void appendSelect(StringBuilder sb) {
		sb.append("SELECT ");
	}

	public List<Comparable<?>> getNesteSelectTable(List<Object> arguments) {
		throw new IllegalArgumentException("��֧����������Ƕselect�ҵ�����������");
	}

	public Comparable<?> eval() {
		return this;
	}

	public Comparable<?> getVal(List<Object> args) {
		TableNameFunctionImp tabFunc = getTableNameFunction();
		Function func = null;
		if (tabFunc != null) {
			func = tabFunc.getFunction();
		}
		if (func == null) {
			throw new IllegalArgumentException("���ܴ���select:" + this
					+ "���ҵ���ֱ�ӻ�õĸ�ֵ����");
		}

		return func.getVal(args);
	}

	public void init() {
		super.init();

		getSelectSqlType();

	}

	/**
	 * �鿴sqltype��ʲô��count,min,max,avg�ȣ������������Ĭ��Ϊnormal
	 */
	protected void getSelectSqlType() {
		type = columns.getSelectType();
	}

	public int compareTo(Object arg0) {
		throw new NotSupportException("should not be here");
	}

	public void setAliasMap(Map<String, SQLFragment> map) {
		this.aliasToSQLFragementMap = map;
	}

	public void buildAliasToTableAndColumnMapping(
			Map<String, SQLFragment> sqlAliasMap) {
		super.buildAliasToTableAndColumnMapping(sqlAliasMap);
		columns.appendAliasToColumnMap(sqlAliasMap);
	}

	public void addForUpdate(SelectUpdate forUpdate) {
		this.forUpdate = forUpdate;
	}

	@Override
	public GroupFunctionType getGroupFuncType() {
		return type;
	}

	@Override
	public WhereCondition getSubWhereCondition() {
		return where;
	}
}
