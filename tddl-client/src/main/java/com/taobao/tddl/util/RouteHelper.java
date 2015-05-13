/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.taobao.tddl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.client.RouteCondition.ROUTE_TYPE;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeAND;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.AdvanceCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.AdvancedDirectlyRouteCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.SimpleCondition;

/**
 * ����ҵ�����ֱ�ӽӿڵķ���
 * s
 * @author shenxun
 */
public class RouteHelper {
    public static final int EQ = Comparative.Equivalent;
    public static final int GT = Comparative.GreaterThan;
    public static final int LT = Comparative.LessThan;
    public static final int GTE = Comparative.GreaterThanOrEqual;
    public static final int LTE = Comparative.LessThanOrEqual;


    /**
     * ֱ����ĳ������,ִ��һ��sql
     * ��ʱ��TDDLֻ���������飬��һ����Э���ж��Ƿ�������״̬��
     * �ڶ��������ǣ����б������滻��
     *
     * @param dbIndex dbIndex�б�
     * @param logicTable �߼�����
     * @param table ʵ�ʱ���
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     */
    public static void executeByDBAndTab(
            String dbIndex,String logicTable,
            ROUTE_TYPE routeType,String... tables) {
        DirectlyRouteCondition condition = new DirectlyRouteCondition();
        if(tables == null){
        	throw new IllegalArgumentException("tables is null");
        }
        for(String table : tables){
        	 condition.addATable(table);
        }

        condition.setVirtualTableName(logicTable);
        condition.setDBId(dbIndex);
        condition.setRouteType(routeType);
        ThreadLocalMap.put(ThreadLocalString.DB_SELECTOR, condition);
    }
    /**
     * ֱ����ĳ������,ִ��һ��sql
     * ��ʱ��TDDLֻ���������飬��һ����Э���ж��Ƿ�������״̬��
     * �ڶ��������ǣ����б������滻��
     *
     * @param dbIndex dbIndex�б�
     * @param logicTable �߼�����
     * @param table ʵ�ʱ���
     */
    public static void executeByDBAndTab( String dbIndex,String logicTable,
            String... table){
    	executeByDBAndTab(dbIndex, logicTable,ROUTE_TYPE.FLUSH_ON_EXECUTE,table);
    }

    /**
     * ֱ����ĳ������,ִ��һ��sql����������ж���������滻����ҪĿ����join��sql
     *
     * tddl should do :
     * 1. �ж������Ƿ��ִ�С�
     * 2. ��������������滻��
     * @param dbIndex dbIndex�б�
     * @param tableMap Դ����->Ŀ�������map
     */
    public static void executeByDBAndTab(String dbIndex,
            Map<String/*original table*/,String/*advanced table*/> tableMap) {
    	executeByDBAndTab(dbIndex, tableMap, ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }

    /**
     * ֱ����ĳ������,ִ��һ��sql����������ж���������滻����ҪĿ����join��sql
     *
     * @param dbIndex dbIndex�б�
     * @param tableMap Դ����->Ŀ�������map
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     */
    public static void executeByDBAndTab(String dbIndex,
            Map<String/*original table*/,String/*advanced table*/> tableMap,ROUTE_TYPE routeType){
    	AdvancedDirectlyRouteCondition condition = new AdvancedDirectlyRouteCondition();
    	condition.setDBId(dbIndex);
    	Map<String, List<Map<String, String>>> directlyShardTableMap
    		= new HashMap<String, List<Map<String,String>>>(2);
    	List<Map<String,String>> tables2BReplaced = new ArrayList<Map<String,String>>(1);
    	tables2BReplaced.add(tableMap);
    	directlyShardTableMap.put(dbIndex, tables2BReplaced);
    	condition.setShardTableMap(directlyShardTableMap);
    	condition.setRouteType(routeType);
        ThreadLocalMap.put(ThreadLocalString.DB_SELECTOR, condition);
    }


    /**
     * ֱ����ĳ������,ִ��һ��sql����������ж���������滻����ҪĿ����join��sql
     *
     * @param dbIndex dbIndex�б�
     * @param tableMap Դ����->Ŀ�������map
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     */
    public static void executeByDBAndTab(String dbIndex,
            List<Map<String/*original table*/,String/*advanced table*/>> tableMap,ROUTE_TYPE routeType){
    	AdvancedDirectlyRouteCondition condition = new AdvancedDirectlyRouteCondition();
    	condition.setDBId(dbIndex);

    	Map<String, List<Map<String, String>>> directlyShardTableMap
		= new HashMap<String, List<Map<String,String>>>(2);
    	directlyShardTableMap.put(dbIndex, tableMap);
    	condition.setShardTableMap(directlyShardTableMap);
    	condition.setRouteType(routeType);
        ThreadLocalMap.put(ThreadLocalString.DB_SELECTOR, condition);
    }

    /**
     * ֱ����ĳ������,ִ��һ��sql����������ж���������滻����ҪĿ����join��sql
     *
     * @param dbIndex dbIndex�б�
     * @param tableMap Դ����->Ŀ�������map
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     */
    public static void executeByDBAndTab(String dbIndex,
            List<Map<String/*original table*/,String/*advanced table*/>> tableMap){
    	executeByDBAndTab(dbIndex, tableMap, ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }
    /**
     * ֱ����ĳ������,ִ��һ��sql����������ж���������滻����ҪĿ����join��sql
     *
     * @param dbIndex dbIndex�б�
     * @param tableMap Դ����->Ŀ�������map
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     */
    public static void executeByDBAndTab(Map<String, List<Map<String, String>>> tableMap,ROUTE_TYPE routeType){
    	AdvancedDirectlyRouteCondition condition = new AdvancedDirectlyRouteCondition();
    	condition.setShardTableMap(tableMap);
    	condition.setRouteType(routeType);
        ThreadLocalMap.put(ThreadLocalString.DB_SELECTOR, condition);
    }
    public static void executeByDBAndTab(Map<String, List<Map<String, String>>> tableMap){
    	executeByDBAndTab(tableMap, ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }
    /**
     * ����db index ִ��һ��sql. sql������ͨ��Ibatis�����sql.
     *
     * ֻ��һ�����飬����Э���ж��Ƿ���Ҫ��������
     *
     * @param dbIndex dbIndex�б�
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     */
    public static void executeByDB(String dbIndex,ROUTE_TYPE routeType){
    	DirectlyRouteCondition condition = new DirectlyRouteCondition();
    	condition.setDBId(dbIndex);
    	condition.setRouteType(routeType);
    	ThreadLocalMap.put(ThreadLocalString.DB_SELECTOR, condition);
    }

    /**
     * ����db index ִ��һ��sql. sql������ͨ��Ibatis�����sql.
     *
     * ֻ��һ�����飬����Э���ж��Ƿ���Ҫ��������
     *
     * @param dbIndex dbIndex�б�
     */
    public static void executeByDB(String dbIndex){
    	executeByDB(dbIndex,ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }

    /**
     * ѡ��һ������
     *
     * Ĭ��������ж�д����ĳ����У�MASTER��Ӧ���⣬SLAVE��Ӧ���⡣
     *
     * @param selector db index key
     */
    public static void selectKey(String selector){
    	//������һ���������dsMap�г��֣���ô����dsMap�е�key����Ӧ��ds
    	//���û��dsMap,��ô��鿴����map���Ƿ��ж�Ӧ�Ĺ���
    	executeByRule(selector,ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }

    /**
     * ѡ��һ������
     *
     * @param ruleKey
     */
    public static void executeByRule(String ruleKey,ROUTE_TYPE routeType){
    	DirectlyRouteCondition condition = new DirectlyRouteCondition();
    	condition.setDBId(ruleKey);
    	condition.setRouteType(routeType);
    	ThreadLocalMap.put(ThreadLocalString.RULE_SELECTOR, condition);
    }

    /**
     * ѡ��һ��������Ҫע����ǹ����key���������ݿ��е�����Դ������
     * ��������ѡ������Դ
     *
     * Ĭ��������ж�д����ĳ����У�MASTER��Ӧ���⣬SLAVE��Ӧ���⡣
     *
     * @param selector db index key
     * @param routeType �������hint�������ӹرյ�ʱ����գ�������ִ��ʱ�����
     *
    */
    public static void selectKey(String selector,ROUTE_TYPE routeType){
    	executeByRule(selector,routeType);
    }

//    /**
//     * ִ��һ��sql,�м����suffix.
//     * ����@suffix@
//     * ����@suffix,key@
//     *
//     * @param dbIndex
//     * @param suffix
//     */
//    public static void executeByDBAndTabSuffix(String dbIndex,
//    		String suffix) {
//    	AdvancedDirectlyRouteCondition condition = new AdvancedDirectlyRouteCondition();
//    	condition.setSuffixModel(true);
//    	condition.setSuffix(suffix);
//        ThreadLocalMap.put(ThreadLocalString.DB_SELECTOR, condition);
//    }

    /**
     * ��������ѡ�����ݿ����ִ��sql
     *
     * @param logicTable
     * @param key
     * @param comp
     */
    public static void executeByCondition(
            String logicTable, String key, Comparable<?> comp) {
       executeByCondition(logicTable, key, comp,ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }

    public static void executeByCondition(
            String logicTable, String key, Comparable<?> comp,ROUTE_TYPE routeType){
    	 SimpleCondition simpleCondition = new SimpleCondition();
         simpleCondition.setVirtualTableName(logicTable);
         simpleCondition.put(key, comp);
         simpleCondition.setRouteType(routeType);
         ThreadLocalMap.put(ThreadLocalString.ROUTE_CONDITION, simpleCondition);
    }

    public static void executeWithParallel(boolean useParallel){
    	ThreadLocalMap.put(ThreadLocalString.PARALLEL_EXECUTE,useParallel);
    }

    public static void executeByCondition(
            String logicTable, String key, Comparable<?> comp,String ruleSelector) {
    	executeByCondition(logicTable, key, comp);
    	selectKey(ruleSelector);
    }

    public static void executeByCondition(
            String logicTable, String key, Comparable<?> comp,String ruleSelector
            ,ROUTE_TYPE routeType) {
    	executeByCondition(logicTable, key, comp,routeType);
    	selectKey(ruleSelector,routeType);
    }

    public static void executeByAdvancedCondition(
            String logicTable, Map<String, Comparable<?>> param
            ,ROUTE_TYPE routeType) {
    	AdvanceCondition condition = new AdvanceCondition();
        condition.setVirtualTableName(logicTable);
        for (Map.Entry<String, Comparable<?>> entry : param.entrySet()) {
            condition.put(entry.getKey(), entry.getValue());
        }
        condition.setRouteType(routeType);
        ThreadLocalMap.put(ThreadLocalString.ROUTE_CONDITION, condition);
    }

    public static void executeByAdvancedCondition(
            String logicTable, Map<String, Comparable<?>> param) {
        executeByAdvancedCondition(logicTable, param, ROUTE_TYPE.FLUSH_ON_EXECUTE);
    }

    public static void executeByAdvancedCondition(
            String logicTable, Map<String, Comparable<?>> param,String ruleSelector) {
    	executeByAdvancedCondition(logicTable, param);
    	selectKey(ruleSelector);
    }

    public static void executeByAdvancedCondition(
            String logicTable, Map<String, Comparable<?>> param,String ruleSelector
            ,ROUTE_TYPE routeType){
    	executeByAdvancedCondition(logicTable, param,routeType);
		selectKey(ruleSelector,routeType);
    }

    public static Comparative or(Comparative parent, Comparative target) {
        if (parent == null) {
            ComparativeOR or = new ComparativeOR();
            or.addComparative(target);
            return or;
        } else {
            if (parent instanceof ComparativeOR) {
                ((ComparativeOR) parent).addComparative(target);
                return parent;
            } else {
                ComparativeOR or = new ComparativeOR();
                or.addComparative(parent);
                or.addComparative(target);
                return or;
            }
        }
    }

    public static Comparative and(Comparative parent, Comparative target) {
        if (parent == null) {
            ComparativeAND and = new ComparativeAND();
            and.addComparative(target);
            return and;
        } else {
            if (parent instanceof ComparativeAND) {

                ComparativeAND and = ((ComparativeAND) parent);
                if (and.getList().size() == 1) {
                    and.addComparative(target);
                    return and;
                } else {
                    ComparativeAND andNew = new ComparativeAND();
                    andNew.addComparative(and);
                    andNew.addComparative(target);
                    return andNew;
                }

            } else {
                ComparativeAND and = new ComparativeAND();
                and.addComparative(parent);
                and.addComparative(target);
                return and;
            }
        }
    }
}
