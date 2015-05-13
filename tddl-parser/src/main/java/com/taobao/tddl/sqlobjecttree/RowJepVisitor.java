package com.taobao.tddl.sqlobjecttree;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeAND;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.sqlobjecttree.common.ComparableElement;

/**
 * Visitor����Ҫ������һ��ComparativeMap.
 * 
 * ���ͼ�{@link Expression}��eval����
 * 
 * @author shenxun
 *
 */
public class RowJepVisitor{
	private Map<String, Comparative> comparable=new HashMap<String,Comparative>();

//	private List<Object> args=Collections.emptyList();
	/**
	 * ��expression �� key�Ͷ�Ӧ��value�ŵ�Comparative�С�
	 * Ҫ�����һ�������ǣ���Ϊ�ڱ���Expression�Ĺ����У������Ĺ�����Expression���ĺ�����������Ա����Ǵ��ӽڵ���
	 * ���ڵ�һ��һ��Ľ��з����ģ������ͻ����һ�����⣬������һ���ط��������е�and�ڵ��or�ڵ���Ϣ������ȫ��
	 * ƴ�뵽�������вŲ����������
	 *
	 * �����expression��eval�����������һ��booleanֵ����ʶ���ڱ������ĳ���ӽڵ�ĸ��ڵ㵽����һ��and�ڵ㻹��һ��or
	 * �Ľڵ㣬��RowJepVisitor�������and�ڵ��or�ڵ�Ĳ�ͬ�������˲�ͬ��ComparativeAnd��comparativeOr����֮��Ӧ��
	 *
	 * 
	 * @param key
	 * @param ele
	 */
	public void put(String key,ComparableElement ele){
		Comparative val=comparable.get(key);
		if(val==null){
			if(ele.comp instanceof ComparativeOR){
				comparable.put(key, (ComparativeOR)ele.comp);
			}else{
				comparable.put(key, new Comparative(ele.operator,ele.comp));
			}
		}else{
			if(ele.isAnd){
				ComparativeAND and=new ComparativeAND();
				and.addComparative(val);
				if(ele.comp instanceof ComparativeOR){
				and.addComparative((ComparativeOR)ele.comp);	
				}else{
				and.addComparative(new Comparative(ele.operator,ele.comp));
				}
				comparable.put(key, and);
			}else{
				//ʵ�ʵ�OR�ڵ㲢δʹ�����ѡ��֧
				//��ѡ��ֻ֧����in������Ҫ��OR����ת���Expressionʹ��
				ComparativeOR or=new ComparativeOR();

				or.addComparative(val);
				or.addComparative(new Comparative(ele.operator,ele.comp));
				comparable.put(key, or);
			}
		}
	}
	public Comparative get(String key){
		return comparable.get(StringUtil.toUpperCase(key));
	}
	public Map<String, Comparative> getComparable() {
		return comparable;
	}
	public void setComparable(Map<String, Comparative> comparable) {
		this.comparable = comparable;
	}
//	public List<Object> getArgs() {
//		return args;
//	}
//	public void setArgs(List<Object> args) {
//		this.args = args;
//	}
}
