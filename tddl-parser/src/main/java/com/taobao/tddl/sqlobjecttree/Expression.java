package com.taobao.tddl.sqlobjecttree;

import com.taobao.tddl.common.sqlobjecttree.SQLFragment;


/**
 * ���ʽ
 * 
 * ����
 * column comparasion value
 * ��
 * col = 1;
 * ��col2 > 2;
 * @author shenxun
 *
 */
public interface Expression extends SQLFragment{
	
	/**
	 *	�����ʽcol = 1 or col = 2 ...�����ı��ʽ�顣����Ϊһ��map.
	 *	map��key��col��Ҳ��������
	 *	map��value��ֵ�ļ��ϣ���Ӧ��ʵ��������Comparative.
	 *	���������col = 1 or col = 2;
	 *	������������󣬾ͻᱻ��Ϊcol -> (=1 or =2);
	 *
	 *	
	 * @param visitor
	 * @param inAnd ������ǰ�Ƿ���andExpressionGroup��
	 */
	public void eval(RowJepVisitor visitor, boolean inAnd);
}
