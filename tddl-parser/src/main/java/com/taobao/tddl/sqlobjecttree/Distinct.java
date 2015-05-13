package com.taobao.tddl.sqlobjecttree;

import java.util.List;
import java.util.Set;

import com.taobao.tddl.common.sqlobjecttree.Column;

/**
 * @author junyu 
 *@author mazhidan.pt
 */
public class Distinct implements Function {
	protected Columns columns = new Columns();
//	private List<Column> columns = new ArrayList<Column>();
	
	public void addColumn(String table, String column, String alias) {
		columns.addColumn(table, column, alias);
	}

	public void addColumn(Column col) {
		columns.addColumn(col);
	}
	
	@Override
	public StringBuilder regTableModifiable(Set<String> logicTableNames,
			List<Object> list, StringBuilder sb) {
	    sb.append(" DISTINCT ");
		return sb;
	}

	@Override
	public String toString() {
		return " DISTINCT ";
	}

	
	public String getFuncName() {
		// TODO Auto-generated method stub
		return "distinct";
	}
	
	//�˴���Ϊdistinct����ͨ��ֻ��һ�������ĺ�����ͬ��������д����Ĵ˷���
	//��ͬ��1��distinct�������ϸ������ϵĺ�����distinct �ؼ��ֺ����ÿո����� ����ʾ��Ҳ����ͨ������������
	//��ͬ��2��distinct �������Ϊһ�У�Ҳ����Ϊ���У�������Column�ӿڣ�
	@Override
	public void appendSQL(StringBuilder sb) {
		sb.append(getFuncName());
		sb.append(" ");
		columns.appendSQL(sb);
		sb.append(" ");
	}

	@Override
	public Comparable<?> eval() {
		return null;
	}

	@Override
	public Comparable<?> getVal(List<Object> args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setValue(List<Object> values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getNestedColName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public Columns getColumns() {
		return columns;
	}

	public void setColumns(Columns columns) {
		this.columns = columns;
	}


}
