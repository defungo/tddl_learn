package com.taobao.tddl.sqlobjecttree.traversalAction;

import java.util.LinkedList;
import java.util.List;

import com.taobao.tddl.sqlobjecttree.OrderByEle;

public class GroupByTraversalAction implements TraversalSQLAction {
	private List<OrderByEle> orderByEles = new LinkedList<OrderByEle>();

	public void actionProformed(TraversalSQLEvent event) {

		List<OrderByEle> temp = event.getCurrStatement().nestGetGroupByList();
		if (orderByEles.size()==0) {
			if (temp != null && temp.size() != 0){
				orderByEles = temp;
			}
		} else {
			if (temp != null&&!temp.isEmpty()){
				//TODO:����ط�Ҫ���һ�����ԣ����ڲ���Ƕ�ײ�ѯ�У���Ƕ�ײ�ѯ��group by ��order by������£��Ƿ񲻻��׳�����쳣
				throw new IllegalArgumentException("��������Ƕ��sql�г��ֶ��group by����");
			}
		}
	}
	public List<OrderByEle> getGroupByEles(){
		return orderByEles;
	}

}
