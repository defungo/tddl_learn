package com.taobao.tddl.rule.ruleengine;

import java.util.Map;
import java.util.Set;

import com.taobao.tddl.common.exception.checked.TDLCheckedExcption;
import com.taobao.tddl.common.sequence.Config;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.TabRule;

public interface TableRuleProvider {

	/**
	 * @deprecated ֻ���ڲ�������
	 * ��ȡ�����б� 
	 * Ŀǰ��֧�ֵ�������£�
	 * and��ϵֻ֧�����������һ��Ŀǰ��֧�ֵĹ�ϵ������4�ࣺ
	 * <p>1.���and������ɵ�����</p>
	 * <p>�磺����һ��columnΪid.
	 * id > x and id < y and id > z ��������������id��ɵ���������</p>
	 * <p>return:���׳��쳣</p>
	 * <p>2.��ͬ��column��ɵ����� </p>
	 * <p>return:������֧�֡�</p>
	 * <p>3.or�����ȼ���������ϡ�</p>
	 * <p>�磺����һ��columnΪid.
	 * id > x and ( id = y or id =z )</p>
	 * <p>return :�׳��쳣��</p>
	 * <p>4.and��ϵ����������ϵ�������������Ĺ�ϵ��
	 * id > x and id=x.</p>
	 * <p>return:�׳��쳣����and����������������һ������Ϊ���ڡ���</p>
	 * 
	 * �����������������ȷʶ��
	 * 
	 * @param row Comparable��
	 * @param position ���в�����Ӧ��λ��
	 * @param tab ����ı�������Bean.
	 * @param vTabName �������
	 * @return
	 * @throws TDLCheckedExcption
	 * 
	 */
	@Deprecated 
	public Set<String> getTables(Comparable<?>[] row,
			Map<String, Integer> position, TabRule tab, String vTabName)
			throws TDLCheckedExcption;
	/**
	 * ��ȡ�����б� 
	 * Ŀǰ��֧�ֵ�������£�
	 * and��ϵֻ֧�����������һ��Ŀǰ��֧�ֵĹ�ϵ������4�ࣺ
	 * <p>1.���and������ɵ�����</p>
	 * <p>�磺����һ��columnΪid.
	 * id > x and id < y and id > z ��������������id��ɵ���������</p>
	 * <p>return:���׳��쳣</p>
	 * <p>2.��ͬ��column��ɵ����� </p>
	 * <p>return:������֧�֡�</p>
	 * <p>3.or�����ȼ���������ϡ�</p>
	 * <p>�磺����һ��columnΪid.
	 * id > x and ( id = y or id =z )</p>
	 * <p>return :�׳��쳣��</p>
	 * <p>4.and��ϵ����������ϵ�������������Ĺ�ϵ��
	 * id > x and id=x.</p>
	 * <p>return:�׳��쳣����and����������������һ������Ϊ���ڡ���</p>
	 * @param map ����key value��
	 * @param tab �����
	 * @param vTabName �������
	 * @param config pk�����ļ�
	 * @return
	 * @throws TDLCheckedExcption
	 */
	public Set<String> getTables(Map<String, Comparative> map, TabRule tab, String vTabName, Config config)
			throws TDLCheckedExcption;
}