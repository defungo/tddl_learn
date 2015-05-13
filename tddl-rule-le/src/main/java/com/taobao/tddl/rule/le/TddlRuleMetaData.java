//Copyright(c) Taobao.com
package com.taobao.tddl.rule.le;

import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.rule.VirtualTableRoot;

/**
 * @description
 * @author <a href="junyu@taobao.com">junyu</a> 
 * @version 1.0
 * @since 1.6
 * @date 2011-8-17����05:04:06
 */
public interface TddlRuleMetaData {
	/**
	 * �õ�ĳ���汾�����ĳ���߼�������˽ṹ
	 * 
	 * @param vtab
	 * @param version
	 * @return
	 */
	public Map<String,Set<String>> getTopologyByVersion(String vtab,String version);
	
	/**
	 * �õ���ǰ���а汾����
	 * @return
	 */
	public Map<String/*version*/,VirtualTableRoot> getAllVersionedRule();
	
	
    /**
     * get local rule setted by user,no version
     * 
     * @return
     */
	public VirtualTableRoot getLocalRule();
	
	/**
	 * get the actual db table topology of one specified logic table
	 * which belongs the rule setted by user, no version
	 * 
	 * @param vtab
	 * @return
	 */
	public Map<String,Set<String>> getLocalRuleTopology(String vtab);
}
