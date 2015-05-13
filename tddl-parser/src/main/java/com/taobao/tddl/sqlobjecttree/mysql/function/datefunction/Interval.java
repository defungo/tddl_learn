package com.taobao.tddl.sqlobjecttree.mysql.function.datefunction;

import java.util.List;
import java.util.Set;

import com.taobao.tddl.sqlobjecttree.Function;
import com.taobao.tddl.sqlobjecttree.Utils;

/**
 * @author junyu
 *
 */
public class Interval implements Function {
	protected Object expr;
	protected Object dateUnit;
    
	@Override
	public String getNestedColName() {
		return null;
	}

	@Override
	public void setValue(List<Object> values) {
		if(values.size()!=2){
			throw new IllegalArgumentException("������������");
		}
		expr=values.get(0);
		dateUnit= values.get(1);
	}

	@Override
	public Comparable<?> eval() {
		return this;
	}

	@Override
	public void appendSQL(StringBuilder sb) {
		sb.append(" INTERVAL ");
//		Utils.appendSQL(expr, sb);
		//fixed by mazhidan.pt
		//��Ϊexpr����Ϊ-1����1��������ʽ����ǰ�Ĵ��룬Ϊ-1ʱ�����ص���StringBuilder������������
		//�����ص�Ϊ1ʱ������ΪString����ʹ��Utils.appened��  �������'1'����ԭ����˼������
		sb.append(expr.toString());
		Utils.appendSQL(dateUnit,sb);
	}

	@Override
	public StringBuilder regTableModifiable(Set<String> logicTableNames,
			List<Object> list, StringBuilder sb) {
		sb.append(" INTERVAL ");
		Utils.appendSQLWithList(logicTableNames, expr, sb, list);
		Utils.appendSQLWithList(logicTableNames, dateUnit, sb,list);
		return sb;
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}
	
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append(" INTERVAL ");
		Utils.appendSQL(expr, sb);
		Utils.appendSQL(dateUnit, sb);
		return sb.toString();
	}

	@Override
	public Comparable<?> getVal(List<Object> args) {
		return null;
	}
}
