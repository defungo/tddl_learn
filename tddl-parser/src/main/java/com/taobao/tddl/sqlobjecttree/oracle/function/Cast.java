package com.taobao.tddl.sqlobjecttree.oracle.function;

import java.util.List;
import java.util.Set;

import com.taobao.tddl.common.sqlobjecttree.Column;
import com.taobao.tddl.sqlobjecttree.Function;
import com.taobao.tddl.sqlobjecttree.Utils;


/**
 * ��������Ƚ����⣬��Ϊ������������Ƚ����⣬��д���������ļ�����ġ�
 * @author shenxun
 *
 */
public  class Cast implements Function{
	private String type;
	
	protected Object arg1;
	public void setValue(List<Object> values) {
		if(values.size()!=1){
			throw new IllegalArgumentException("������Ϊһ��");
		}
		arg1=values.get(0);
	}

	public final Comparable<?> eval() {
		return this;
	}

	public String getNestedColName() {
		String ret=null;
		
		if(arg1 instanceof Column){
			ret=((Column)arg1).getColumn();
		}
		return ret;
	}

	public   String getFuncName(){
		return "CAST";
	}
	
	public void appendSQL(StringBuilder sb) {
		sb.append(getFuncName());
		sb.append("(");
		Utils.appendSQLList(arg1, sb);
		sb.append(" AS ");
		sb.append(type);
		sb.append(")");
	}
	public StringBuilder regTableModifiable(Set<String> oraTabName, List<Object> list,
			StringBuilder sb) {
		sb.append(getFuncName());
		sb.append("(");
		sb=Utils.appendSQLListWithList(oraTabName, arg1, sb, list);
		sb.append(" AS ");
		sb.append(type);
		sb.append(")");
		return sb;
		
	}
	public Comparable<?> getVal(List<Object> args) {
		return Utils.getVal(args, arg1);
	}
	public int compareTo(Object o) {
		throw new IllegalStateException("should not be here");
	}

	public void setType(String type) {
		this.type = type;
	}

}
