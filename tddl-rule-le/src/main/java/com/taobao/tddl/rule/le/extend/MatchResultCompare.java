//Copyright(c) Taobao.com
package com.taobao.tddl.rule.le.extend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.bean.TargetDB;

/**
 * @description
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-4-22����12:49:53
 */
public class MatchResultCompare {
	/**
	 * �¾�matcherResult�Ա�
	 * @param resultNew
	 * @param resultOld
	 * @return ��������ȫ��ͬ����true,���򷵻�false
	 */
	public static boolean matchResultCompare(MatcherResult resultNew,
			MatcherResult resultOld) {
		return matchResultCompare(resultNew,resultOld,null,null);
	}

	/**
	 * �¾�MatchResult�Ա�,�����ͬ,�Աȵ�ǰ����Ƿ���¹��������һ��
	 * @param resultNew
	 * @param resultOld
	 * @param oriDb
	 * @param oriTable
	 * @return
	 */
	public static boolean matchResultCompare(
			MatcherResult resultNew, MatcherResult resultOld, String oriDb,String oriTable) {
		List<TargetDB> targetNew = resultNew.getCalculationResult();
		List<TargetDB> targetOld = resultOld.getCalculationResult();
		return innerCompare(targetNew,targetOld,oriDb,oriTable);
	}

	/**
	 * ȷ��ȡ���ݵĿ���Ƿ��ڹ������Ľ��֮��.
	 * @param resultNew
	 * @param oriDb
	 * @param oriTable
	 * @return false ������, true ��ʾ�����������ȫ����ȡ���ݵĿ��
	 */
	public static boolean oriDbTabCompareWithMatchResult(MatcherResult resultNew,String oriDb,String oriTable){
		List<TargetDB> targetNew = resultNew.getCalculationResult();
		Map<String, Map<String, String>> dbTabMap = getTargetMap(targetNew);
		Map<String,String> tables=dbTabMap.get(oriDb);
		if(tables==null){
			return false;
		}else if(tables.get(oriTable)==null){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * �¾�TargetDB�Ա�
	 * @param targetNew
	 * @param targetOld
	 * @return ��������ȫ��ͬ����true,���򷵻�false
	 */
	public static boolean targetDbCompare(List<TargetDB> targetNew,
			List<TargetDB> targetOld) {
		return targetDbCompare(targetNew,targetOld,null,null);
	}

	/**
	 * �¾�TargetDB�Ա�,�����ͬ,�Աȵ�ǰ����Ƿ���¹��������һ��
	 * @param targetNew
	 * @param targetOld
	 * @param oriDb
	 * @param oriTable
	 * @return
	 */
	public static boolean targetDbCompare(List<TargetDB> targetNew,
			List<TargetDB> targetOld, String oriDb,String oriTable) {
		return innerCompare(targetNew,targetOld,oriDb,oriTable);
	}

	/**
	 * ȷ��ȡ���ݵĿ���Ƿ��ڹ������Ľ��֮��.
	 * @param resultNew
	 * @param oriDb
	 * @param oriTable
	 * @return false ������, true ��ʾ�����������ȫ����ȡ���ݵĿ��
	 */
	public static boolean oriDbTabCompareWithTargetDb(List<TargetDB> targetNew,String oriDb,String oriTable){
		Map<String, Map<String, String>> dbTabMap = getTargetMap(targetNew);
		Map<String,String> tables=dbTabMap.get(oriDb);
		if(tables==null){
			return false;
		}else if(tables.get(oriTable)==null){
			return false;
		}else{
			return true;
		}
	}

	private static boolean innerCompare(List<TargetDB> targetNew,List<TargetDB> targetOld,String oriDb,String oriTable){
		Map<String, Map<String, String>> newOne = getTargetMap(targetNew);
		Map<String, Map<String, String>> oldOne = getTargetMap(targetOld);
		boolean dbDiff=false;
		boolean tbDiff=false;
		//����Ƚ�
		for(Map.Entry<String,Map<String,String>> entry:newOne.entrySet()){
			Map<String,String> oldTables=oldOne.get(entry.getKey());
			if(oldTables!=null){
				//����Ƚϱ�
				for(Map.Entry<String, String> newTbEntry:entry.getValue().entrySet()){
					String tb=oldTables.get(newTbEntry.getKey());
					if(tb==null){
						tbDiff=true;
					}
				}
				//����Ƚϱ�
				for(Map.Entry<String, String> oldTbEntry:oldTables.entrySet()){
					String tb=entry.getValue().get(oldTbEntry.getKey());
					if(tb==null){
						tbDiff=true;
					}
				}
			}else{
				dbDiff=true;
			}
		}

		//����ֻ����������Ƿ���ͬ,��Ϊ��������Ƚ�����Ϳ��Եõ�
		for(Map.Entry<String,Map<String,String>> entry:oldOne.entrySet()){
			Map<String,String> newTables=newOne.get(entry.getKey());
			if(newTables==null){
				dbDiff=true;
			}
		}

		return compareResultAnalyse(newOne,oriDb,oriTable,dbDiff,tbDiff);
	}

	private static boolean compareResultAnalyse(Map<String, Map<String, String>> newResult,String oriDb,String oriTable,boolean dbDiff,boolean tbDiff){
		//������������ͬ,���������ͬ
		if(dbDiff&&oriDb!=null){
			Map<String,String> tables=newResult.get(oriDb);
			if(tables!=null){
				//����������Ŀ���
			    return compareResutlAnalyseTable(tables,oriTable,tbDiff);
			}else{
				//������ⲻ��Ŀ���,��ô˵����ҪǨ��.
				return false;
			}
		}else if(dbDiff&&oriDb==null){
			return false;
		}else{
			//�����������Ҳ��ͬ,���������
			return compareResutlAnalyseTable(newResult.get(oriDb),oriTable,tbDiff);
		}
	}

	private static boolean compareResutlAnalyseTable(Map<String,String> tables,String oriTable,boolean tbDiff){
		if(tbDiff&&oriTable!=null){
			if(tables.get(oriTable)!=null){
				return true;
			}else{
				return false;
			}
		}else if(tbDiff&&oriTable==null){
			return false;
		}else{
			return true;
		}
	}

	private static Map<String, Map<String, String>> getTargetMap(
			List<TargetDB> targetDbs) {
		Map<String, Map<String, String>> reMap = new HashMap<String, Map<String, String>>();
		for (TargetDB db : targetDbs) {
			Map<String, String> tableMap = new HashMap<String, String>();
			Set<String> tables = db.getTableNames();
			for (String table : tables) {
				tableMap.put(table, table);
			}
			reMap.put(db.getDbIndex(), tableMap);
		}

		return reMap;
	}
}
