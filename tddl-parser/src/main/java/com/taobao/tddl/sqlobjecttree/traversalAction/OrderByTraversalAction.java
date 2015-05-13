package com.taobao.tddl.sqlobjecttree.traversalAction;

import java.util.LinkedList;
import java.util.List;

import com.taobao.tddl.sqlobjecttree.OrderByEle;

public class OrderByTraversalAction implements TraversalSQLAction {
	private List<OrderByEle> orderByEles = new LinkedList<OrderByEle>();

	public void actionProformed(TraversalSQLEvent event) {

		List<OrderByEle> temp = event.getCurrStatement().nestGetOrderByList();
		if (orderByEles.size()==0) {
			if (temp != null && temp.size() != 0){
				orderByEles = temp;
			}
		} else {
			if (temp != null&&!temp.isEmpty()){
				//���list��Ϊnull,�����Ƿǿգ����ʾ������������
				throw new IllegalArgumentException("��������Ƕ��sql�г��ֶ��order by����");
			}
		}
	}
	public List<OrderByEle> getOrderByEles(){
		return orderByEles;
	}

}
