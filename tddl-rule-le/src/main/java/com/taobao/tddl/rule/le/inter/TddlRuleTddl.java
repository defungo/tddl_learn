//Copyright(c) Taobao.com
package com.taobao.tddl.rule.le.inter;

import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.rule.VirtualTableRoot;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.rule.le.exception.ResultCompareDiffException;

/**
 * @description
 * @author <a href="junyu@taobao.com">junyu</a> 
 * @version 1.1
 * @since 1.6
 * @date 2011-5-4����06:54:24
 * 
 * ���������ӿڣ�����֧��Join�Ż���broadcast.
 * @author Whisper
 */
public interface TddlRuleTddl {
	/**
	 * �����ǰ����һ���㲥��/�㲥Index��
	 * �㲥�����������ͣ�
	 * 	һ����С��ȫ���Ƶ��������ݽڵ㡣
	 * 	һ����������������һ��ά�����˸��ơ�
	 * 
	 * ����������£�������߼�Ӧ�����ƣ�
	 * ���������ж��Ƿ��Ǹ���һ���Զ���
	 * ����ǣ���ô���ղ���join��������/index���������ݵķֲ���
	 * 
	 * @return true ����Ǹ���ҪbroadCast���߼�����߼�������
	 */
	boolean isBroadCast(String logicName);
	
	/**
	 * �����ж�������Ƿ�ʹ������ͬ���зֹ���
	 * 
	 * @param leftLogicName
	 * @param rightLogicName
	 * @return
	 */
	boolean isInTheSameJoinGroup(String leftLogicName,String rightLogicName);
	
	/**
	 * �����ж�������Ƿ�ʹ������ͬ���зֹ���
	 * 
	 * @param logicNames
	 * @return
	 */
	boolean isInTheSameJoinGroup(List<String> logicNames);
	
	/**
	 * �򵥵��׹���֧��(TDDLʹ��)
	 * @param vtab
	 * @param condition
	 * @return
	 */
	public MatcherResult route(String vtab,ComparativeMapChoicer choicer,List<Object> args,boolean needSourceKey);
	
	/**
	 * ���׹���֧��(TDDLʹ��)
	 * @param vtab
	 * @param condition
	 * @return
	 */
	public Map<String, MatcherResult> routeMVer(
			String vtab,
			ComparativeMapChoicer choicer,List<Object> args,boolean needSourceKey);
	
	/**
	 * ָ��һ�׹������
	 * @param vtab
	 * @param condition
	 * @return
	 */
	public MatcherResult route(String vtab,ComparativeMapChoicer choicer,List<Object> args,boolean needSourceKey,VirtualTableRoot specifyVtr);
	
	/**
	 * �¾ɹ�����㲢�Ƚ�,����Ŀ����ж�
	 * 
	 * @param vtab
	 * @param conditionStr
	 * @return
	 */
	public MatcherResult routeMVerAndCompare(SqlType sqlType,
			String vtab, ComparativeMapChoicer choicer,List<Object> args,boolean needSourceKey)throws ResultCompareDiffException;
	
	/**
	 * �¾ɹ�����㲢�Ƚ�,��Ŀ����ж�
	 * 
	 * @param vtab
	 * @param conditionStr
	 * @return
	 */
	public MatcherResult routeMVerAndCompare(SqlType sqlType,
			String vtab,  ComparativeMapChoicer choicer,List<Object> args,boolean needSourceKey,String oriDb,String oriTable)throws ResultCompareDiffException; 
}
