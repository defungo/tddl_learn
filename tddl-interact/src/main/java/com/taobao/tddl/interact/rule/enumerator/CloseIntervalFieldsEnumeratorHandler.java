package com.taobao.tddl.interact.rule.enumerator;

import java.util.Set;

import com.taobao.tddl.interact.sqljep.Comparative;

public interface CloseIntervalFieldsEnumeratorHandler {
	 /**
	 * @param source
	 * @param retValue
	 * @param cumulativeTimes
	 * @param atomIncrValue
	 */
	void processAllPassableFields(Comparative source ,Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue);
		/**
		 * ��ٳ���from��to�е�����ֵ����������value
		 * 
		 * @param from
		 * @param to
		 */
	abstract void mergeFeildOfDefinitionInCloseInterval(
				Comparative from, Comparative to, Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue);

	
}
