package com.taobao.tddl.sqlobjecttree.common.expression;

import java.util.List;
import java.util.Set;

import com.taobao.tddl.common.sqlobjecttree.Column;
import com.taobao.tddl.common.sqlobjecttree.Value;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.sqlobjecttree.Expression;
import com.taobao.tddl.sqlobjecttree.Function;
import com.taobao.tddl.sqlobjecttree.RowJepVisitor;
import com.taobao.tddl.sqlobjecttree.Utils;
import com.taobao.tddl.sqlobjecttree.common.ComparableElement;
import com.taobao.tddl.sqlobjecttree.common.value.UnknowValueObject;



/**
 * ��ʾin�Ĺ�ϵ
 * 
 * col in (������������������
 * 
 * @author shenxun
 *
 */
public  class InExpression implements Expression{
	protected Object left;
	protected Object right;
	protected  int getComparativeOperation(){
		return Comparative.Equivalent;
	}
	protected  String getRelationString(){
		return " in ";
	}
	public void appendSQL(StringBuilder sb) {
		Utils.appendSQLList(left, sb);
		
		sb.append(getRelationString());
		Utils.appendSQLList(right, sb);
	}
	@SuppressWarnings("rawtypes")
	public void eval(RowJepVisitor visitor, boolean inAnd) {
		String colName=null;
		if (left instanceof Column) {
			colName = ((Column) left).getColumn();
		}else if(left instanceof Function){
			//ifnull(col,0); nvl(col,0)
			colName = ((Function)left).getNestedColName();
		}else{
			throw new IllegalArgumentException("�����ҵ���������ȷ�������ڵ�ʽ����");
		}
		
		int operator=getComparativeOperation();
		
		ComparativeOR or = null;
		
		if (right instanceof List) {

			List li = (List) right;
			or = new ComparativeOR(li.size());
			for (Object obj : li) {
				this.buildOneEqComparative(or, obj, colName, false, operator);
			}
		} else if (right instanceof Value) {
			or = new ComparativeOR();
			this.buildOneEqComparative(or, right, colName, inAnd, operator);
		} else {
			throw new IllegalStateException(
					"��֧�ַ�list��select�����������������ڲ���in���������");
		}
		
		visitor.put(colName.toUpperCase(), new ComparableElement(or,inAnd,operator));
		 
	}
	private void buildOneEqComparative(ComparativeOR comparativeOR,Object right,String colName,boolean inAnd,int operator){
		if (right instanceof Value) {
			Value val = (Value) right;
			Comparable<?> temp =val.eval();
			comparativeOR.addComparative(new Comparative(operator,temp));
		}  else {
			//�������Value����
			if(colName==null){
				throw new IllegalArgumentException("sqlԪ�أ�"+left+"|"+getRelationString()+"|"+right+"���ҵ�ָ��������");
			}
			if(right instanceof Comparable){
				comparativeOR.addComparative(new Comparative(operator,(Comparable<?>) right));
			} else {
				comparativeOR.addComparative(new Comparative(operator,UnknowValueObject.getUnknowValueObject()));
			}
			
		}
	}
	public Object getLeft() {
		return left;
	}
	public void setLeft(Object left) {
		this.left = left;
	}
	public Object getRight() {
		return right;
	}
	public void setRight(Object right) {
		this.right = right;
	}
	public StringBuilder regTableModifiable(Set<String> oraTabName, List<Object> list,
			StringBuilder sb) {
			sb=Utils.appendSQLListWithList(oraTabName, left, sb, list);
			sb.append(getRelationString());
			sb=Utils.appendSQLListWithList(oraTabName, right, sb, list);
			return sb;
		
	}
	
}
