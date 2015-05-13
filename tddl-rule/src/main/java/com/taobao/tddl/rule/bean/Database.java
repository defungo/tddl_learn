package com.taobao.tddl.rule.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;
import com.taobao.tddl.interact.rule.bean.SamplingField;
import com.taobao.tddl.rule.ruleengine.cartesianproductcalculator.CartesianProductCalculator;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.ListSharedElement;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.OneToManyEntry;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.RuleChain;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.TablePropertiesSetter;
import com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker.TableMapProvider;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.CalculationContextInternal;
import com.taobao.tddl.rule.ruleengine.rule.ListAbstractResultRule;
import com.taobao.tddl.rule.ruleengine.rule.ResultAndMappingKey;
import com.taobao.tddl.rule.ruleengine.util.RuleUtils;

/**
 * һ�����ݿ�ĳ���
 * 
 * @author shenxun
 * 
 */
public class Database extends ListSharedElement implements
		TablePropertiesSetter {
	Log log = LogFactory.getLog(ListSharedElement.class);
	// implements TableContainer,TableListResultRuleContainer
	private String dataSourceKey;

	/**
	 * �߼�����
	 */
	private String logicTableName;
	/**
	 * ����List������initʱ���ruleChain
	 */
	private List<ListAbstractResultRule> tableRuleList;
	/**
	 * 1�Զ�ָ��������Entry
	 */
	private OneToManyEntry oneToManyEntry;

	private TableMapProvider tableMapProvider;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.taobao.tddl.rule.ruleengine.entities.abstractentities.ListSharedElement
	 * #init() ������Ҫ���ȳ�ʼ������subTable��Ȼ���������Ӧ�Ķ�����
	 */
	public void init() {
		init(true);
	}

	public void init(boolean invokeBySpring) {
		initTableRuleChain();
		initLogicTableName();
		initTableMapProvider();
		initDefaultListResultStragety();

		// �����tableRuleProvider ��ô��ʼ���ӱ�tableMapProvier��transmit����������ʱ��������
		if (tableMapProvider != null) {
			if (invokeBySpring) {
				log.warn("��ɾ��"
						+ "��tddl�����У�bean class Ϊcom.taobao.tddl.common.config.beans.TableRule"
						+ " �Ĺ����bean �е�init-method������ " + "Ŀǰ�ɼ��ݣ������ɾ��");
			} else {
				Map<String, SharedElement> beConstructedMap = getTableMapByTableMapProvider();
				putAutoConstructedMapIntoCurrentTagMap(beConstructedMap);
			}
		}

		super.init(invokeBySpring);
	}

	private void initDefaultListResultStragety() {
		if (defaultListResultStragety == null) {
			defaultListResultStragety = oneToManyEntry
					.getDefaultListResultStragety();
		}
	}

	/*
	*//**
	 * �˷����Ѿ�δʹ�ã��滻�ķ������棬Ϊ�˵�Ԫ���Լ���
	 * 
	 * ��������ķ��� 1. �����Database����û�й��򣬼ȶ�Ӧ�Ŀ�û�зֱ����,
	 * ������Ȼ�й��򣬵��ǵ�ǰ��sql������û��ƥ�䵽��Щ���򣬷���������������� a
	 * ���Database����ֻ��һ������ô����Ĭ�Ϲ���Ϊ�ζ�Ӧ��ȡ���ñ����������Ҫ�����ڵ��⣨��,�ࣩ���򣨵����ࣩ�� ���������¡� b
	 * ���Database�ж������ôӦ��ʹ��Ĭ��ѡ����ԣ�DEFAULT_LIST_RESULT_STRAGETY����
	 * ���������������ΪDEFAULT_LIST_RESULT_STRAGETY.NONE�Ƚϰ�ȫ;
	 * 
	 * 2. ��������˹���
	 * 2.1�����˹���Database��û�д��ݶ�λ����databaseʱ�����õ�����->��㼯�ϵ�map.��ô��ʾǰ�����ݺ͵�ǰ�����޹�
	 * ��ֱ��ʹ�õ�ǰ����+sql��ƥ���ֵ���ɡ� 2.2�������˶�λ����databaseʱ������->���ļ���
	 * 2.2.1�����ݵ���㼯���е�key�����˴�sql�л�ȡ��key����ô��ʾdatabase��table�������ǻ��໥����Ӱ��ġ�
	 * ���ʱ������ʹ��database�д���� ����->�������㡣
	 * 2.2.2�����ݵ���㼯���е�key��������sql�л�ȡ��key,��ô��ʾdatabase��table֮��������޹�
	 * ��ֱ��ʹ��table�����ݽ��м��㡣
	 * 
	 * @param targetDB
	 *            Ŀ����������������ڱ���װ
	 * @param sourceTrace
	 *            �������ǰ���ݿ��Դ����׷�١�
	 * @param map
	 *            ����͹����Ӧ�������ġ�
	 */
	/*
	 * public void calculateTable(TargetDB targetDB, Field sourceTrace,
	 * Map<RuleChain, CalculationContextInternal> map) {
	 * CalculationContextInternal calculationContext =
	 * map.get(this.listResultRule); Map<String,Field> resultSet = null;
	 * 
	 * if (calculationContext == null) { // ��ʾû��ָ�����򣬰�����ǰ����Ϊnull��û��ƥ�䵽���ݣ���Ĭ�� if
	 * (subSharedElement != null && subSharedElement.size() == 1) { // 1��
	 * resultSet = builSingleTable(); } else { //2) resultSet =
	 * buildDefaultTable(); } } else if (sourceTrace ==
	 * null||sourceTrace.sourceKeys.isEmpty()) {
	 * //2.1��Database��û�д��ݶ�λ����databaseʱ�����õ�����->��㼯�ϵ�map
	 * 
	 * ListAbstractResultRule rule =
	 * calculationContext.ruleChain.getRuleByIndex(calculationContext.index);
	 * Map<String, Set<Object>> argsFromSQL = getEnumeratedSqlArgsMap(
	 * calculationContext, rule); resultSet = rule.evalElement(argsFromSQL); }
	 * else { //2.2�������˶�λ����databaseʱ������->���ļ��� ListAbstractResultRule rule =
	 * calculationContext.ruleChain.getRuleByIndex(calculationContext.index);
	 * Map<String, Set<Object>> argsFromSQL = getEnumeratedSqlArgsMap(
	 * calculationContext, rule);
	 * //�й�ϵ������Ӧ�����ȡ�putAll���ô����sourceKeys���Ա�database��Ч����㼯������ȫ��sql��Ϣ�ļ�����
	 * Map<String ���� , Set<Object> �õ��ý�������ֵ�� > sourceKeys =
	 * sourceTrace.sourceKeys; for(Entry<String, Set<Object>>
	 * entry:sourceKeys.entrySet()){
	 * if(argsFromSQL.containsKey(entry.getKey())){
	 * argsFromSQL.put(entry.getKey(), entry.getValue());
	 * log.debug("put entry: "+entry +" to args"); } // else{ //
	 * //�ӷֿ���������û�зֱ�����Ҫ�����ݵ�ʱ�򣬲��Ž�ȥ // } } resultSet =
	 * rule.evalElement(argsFromSQL); }
	 * 
	 * buildTableNameSet(targetDB, resultSet); }
	 */
	/**
	 * ��������ķ��� 1. �����Database����û�й��򣬼ȶ�Ӧ�Ŀ�û�зֱ����,
	 * ������Ȼ�й��򣬵��ǵ�ǰ��sql������û��ƥ�䵽��Щ���򣬷���������������� a
	 * ���Database����ֻ��һ������ô����Ĭ�Ϲ���Ϊ�ζ�Ӧ��ȡ���ñ����������Ҫ�����ڵ��⣨��,�ࣩ���򣨵����ࣩ�� ���������¡� b
	 * ���Database�ж������ôӦ��ʹ��Ĭ��ѡ����ԣ�DEFAULT_LIST_RESULT_STRAGETY����
	 * ���������������ΪDEFAULT_LIST_RESULT_STRAGETY.NONE�Ƚϰ�ȫ;
	 * 
	 * 2. ��������˹���
	 * 2.1�����˹���Database��û�д��ݶ�λ����databaseʱ�����õ�����->��㼯�ϵ�map.��ô��ʾǰ�����ݺ͵�ǰ�����޹�
	 * ��ֱ��ʹ�õ�ǰ����+sql��ƥ���ֵ���ɡ� 2.2�������˶�λ����databaseʱ������->���ļ���
	 * 2.2.1�����ݵ���㼯���е�key�����˴�sql�л�ȡ��key����ô��ʾdatabase��table�������ǻ��໥����Ӱ��ġ�
	 * ���ʱ������ʹ��database�д���� ����->�������㡣
	 * 2.2.2�����ݵ���㼯���е�key��������sql�л�ȡ��key,��ô��ʾdatabase��table֮��������޹�
	 * ��ֱ��ʹ��table�����ݽ��м��㡣
	 * 
	 * @param targetDB
	 *            Ŀ����������������ڱ���װ
	 * @param sourceTrace
	 *            �������ǰ���ݿ��Դ����׷�١�
	 * @param map
	 *            ����͹����Ӧ�������ġ�
	 */
	public void calculateTable(TargetDB targetDB, Field sourceTrace,
			Map<RuleChain, CalculationContextInternal> map,
			ExtraParameterContext extraParameterContext) {
		CalculationContextInternal calculationContext = map
				.get(this.listResultRule);
		Map<String, Field> resultSet = null;

		if (calculationContext == null) {
			// ��ʾû��ָ�����򣬰�����ǰ����Ϊnull��û��ƥ�䵽���ݣ���Ĭ��
			if (subSharedElement != null && subSharedElement.size() == 1) {
				// 1��
				resultSet = builSingleTable();
			} else {
				// 2)
				resultSet = buildDefaultTable();
			}
		} else if (sourceTrace == null || sourceTrace.sourceKeys.isEmpty()) {
			// 2.1��Database��û�д��ݶ�λ����databaseʱ�����õ�����->��㼯�ϵ�map

			// ListAbstractResultRule rule =
			// calculationContext.ruleChain.getRuleByIndex(calculationContext.index);
			ListAbstractResultRule rule = calculationContext.rule;
			Map<String, Set<Object>> argsFromSQL = getEnumeratedSqlArgsMap(
					calculationContext, rule);
			resultSet = rule.evalElement(argsFromSQL, extraParameterContext);
		} else {
			// 2.2�������˶�λ����databaseʱ������->���ļ���
			// ListAbstractResultRule rule =
			// calculationContext.ruleChain.getRuleByIndex(calculationContext.index);
			ListAbstractResultRule rule = calculationContext.rule;
			Map<String, Set<Object>> argsFromSQL = getEnumeratedSqlArgsMap(
					calculationContext, rule);
			// �й�ϵ������Ӧ�����ȡ�putAll���ô����sourceKeys���Ա�database��Ч����㼯������ȫ��sql��Ϣ�ļ�����
			Map<String/* ���� */, Set<Object>/* �õ��ý�������ֵ�� */> sourceKeys = sourceTrace.sourceKeys;
			for (Entry<String, Set<Object>> entry : sourceKeys.entrySet()) {
				if (argsFromSQL.containsKey(entry.getKey())) {
					argsFromSQL.put(entry.getKey(), entry.getValue());
					log.debug("put entry: " + entry + " to args");
				}
				// else{
				// //�ӷֿ���������û�зֱ�����Ҫ�����ݵ�ʱ�򣬲��Ž�ȥ
				// }
			}
			resultSet = rule.evalElement(argsFromSQL, extraParameterContext);
		}
		buildTableNameSet(targetDB, resultSet);
	}

	// //////////////////////////////////////////////////////////////////////////////

	public void calculateTableWithNoDuplicateKey(TargetDB targetDB,
			Map<String/* ���� */, Object/* ����ֵ */> sourceMap,
			ExtraParameterContext extraParameterContext, RuleContext ruleContext) {
		CalculationContextInternal calculationContext = ruleContext.calContextMap
				.get(this.listResultRule);
		Map<String, Field> resultSet = null;
		if (calculationContext == null) {
			if (ruleContext.firstTableCalculate) {
				firstWithOutCalculateContext(ruleContext);
			}
			resultSet = ruleContext.tabSourceWithNoRule;
		} else {
			if (ruleContext.firstTableCalculate) {
				firstWithCalculateContext(sourceMap, calculationContext,
						ruleContext);
				//�����NPE,˵��ruleContext.dbAndTabWithSameColumnδ��ʼ��,
				//����ڿ�����ʱ���Ӧ�ù��
				if (ruleContext.dbAndTabWithSameColumn.size() == 0) {
					resultSet = evalElement(calculationContext,
							ruleContext.tabArgsMap, extraParameterContext,
							ruleContext);
					ruleContext.tabResultSet=resultSet;
				}
			}
            
			//�����ķֿ�ֱ���н�����ʱ��,������Ҫ�滻��������key,ÿ�μ����
			if (ruleContext.dbAndTabWithSameColumn.size() != 0) {
				// db������ÿһ��sourceKey�����ܲ���ͬ,������Ҫ�滻
				for (String key : ruleContext.dbAndTabWithSameColumn) {
					Set<Object> set = new HashSet<Object>(1);
					set.add(sourceMap.get(key));
					ruleContext.tabArgsMap.put(key, set);
				}
				
				resultSet = evalElement(calculationContext,
						ruleContext.tabArgsMap, extraParameterContext,
						ruleContext);
			}else{
				//�����ķֿ�ֱ��û�н�����ʱ��,���ڱ�ļ���,����ֻ��Ҫ����һ��
				resultSet=ruleContext.tabResultSet;
			}
		}

		buildOrAddTable(targetDB, resultSet);
	}

	/**
	 * 1.ö�ٱ�����ŵ�ruleContext��,һ��sql������,���������ǲ����б仯��
	 * 2.�����������,�������зŵ�ruleContext��,һ��sql������,��������ǲ����б仯��
	 * 
	 * ����2��������һ��sql������ִֻ��һ��.
	 * 
	 * @param sourceMap
	 * @param calculationContext
	 * @param ruleContext
	 */
	private void firstWithCalculateContext(
			Map<String/* ���� */, Object/* ����ֵ */> sourceMap,
			CalculationContextInternal calculationContext,
			RuleContext ruleContext) {
		Map<String, Set<Object>> argsFromSQL = getEnumeratedSqlArgsMap(
				calculationContext, calculationContext.rule);
		ruleContext.tabArgsMap = argsFromSQL;

		ruleContext.dbAndTabWithSameColumn = new ArrayList<String>(2);
		for (String key : sourceMap.keySet()) {
			if (argsFromSQL.containsKey(key)) {
				ruleContext.dbAndTabWithSameColumn.add(key);
			}
		}
		ruleContext.firstTableCalculate = false;
	}

	/**
	 * û�б����������,��һ�μ�����Ҫ�õ����map,�����õ�ruleContext�� ֮�󽫲���ֱ��ʹ��ruleContext�е�map����
	 * �������������һ��sql������ִֻ��һ��.
	 * 
	 * @param ruleContext
	 */
	private void firstWithOutCalculateContext(RuleContext ruleContext) {
		if (subSharedElement != null && subSharedElement.size() == 1) {
			ruleContext.tabSourceWithNoRule = builSingleTable();
		} else {
			ruleContext.tabSourceWithNoRule = buildDefaultTable();
		}
		ruleContext.firstTableCalculate = false;
	}

	/**
	 * ����ĳһ������ı��׺,������ͼ���������Ŀ��targetDB,���ǰ��һ�� �������Ѿ�������,��ôֻ��sourceKey
	 * 
	 * @param calculationContext
	 * @param enumeratedMap
	 * @param extraParameterContext
	 * @param ruleContext
	 * @return
	 */
	private Map<String/* �����ֵ */, Field> evalElement(
			CalculationContextInternal calculationContext,
			Map<String, Set<Object>> enumeratedMap,
			ExtraParameterContext extraParameterContext, RuleContext ruleContext) {
		Map<String/* �����ֵ */, Field> map;
		if (enumeratedMap.size() == 1) {
			String column = null;
			Set<Object> enumeratedValues = null;
			for (Entry<String, Set<Object>> entry : enumeratedMap.entrySet()) {
				column = entry.getKey();
				enumeratedValues = entry.getValue();
			}

			// ����ֵ���Ҳ�����뺯����x�ĸ������Ӧ
			map = new HashMap<String, Field>(enumeratedValues.size());
			for (Object value : enumeratedValues) {
				evalSimple(column, value, calculationContext,
						extraParameterContext, map, ruleContext);
			}
		} else {
			CartesianProductCalculator cartiesianProductCalculator = new CartesianProductCalculator(
					enumeratedMap);
			map = new HashMap<String, Field>(16);
			for (SamplingField samplingField : cartiesianProductCalculator) {
				eval(samplingField, calculationContext, extraParameterContext,
						map, ruleContext);
			}
		}
		return map;
	}

	/**
	 * ���ռ���һ�����,����sourceKey
	 * 
	 * @param samplingField
	 * @param calculationContext
	 * @param extraParameterContext
	 * @param map
	 * @param ruleContext
	 */
	private void eval(SamplingField samplingField,
			CalculationContextInternal calculationContext,
			ExtraParameterContext extraParameterContext,
			Map<String, Field> map, RuleContext ruleContext) {
		ResultAndMappingKey returnAndMappingKey = calculationContext.rule
				.evalueateSamplingField(samplingField, extraParameterContext);
		if (returnAndMappingKey != null) {
			String[] targets = StringUtils.split(returnAndMappingKey.result,
					"\\|");
			List<String> columns = samplingField.getColumns();
			List<Object> values = samplingField.getEnumFields();
			for (String target : targets) {
				Field colMap = map.get(target);
				if (colMap == null) {
					colMap = new Field(columns.size());
					map.put(target, colMap);
				}

				// ֻ������ҪsourceKey������²�ȥ��
				if (ruleContext.needSourceKey) {
					int index = 0;
					for (String column : columns) {
						Object value = values.get(index);
						Set<Object> set = colMap.sourceKeys.get(column);
						if (set == null) {
							set = new HashSet<Object>(1);
							colMap.sourceKeys.put(column, set);
						}
						set.add(value);
						index++;
					}
				}
			}
		}
	}

	/**
	 * ��Ե�column,��value�Ĺ������
	 * 
	 * @param column
	 * @param value
	 * @param calculationContext
	 * @param extraParameterContext
	 * @param map
	 * @param ruleContext
	 */
	private void evalSimple(String column, Object value,
			CalculationContextInternal calculationContext,
			ExtraParameterContext extraParameterContext,
			Map<String, Field> map, RuleContext ruleContext) {
		ResultAndMappingKey returnAndMappingKey = calculationContext.rule
				.evalueateSimpleColumAndValue(column, value,
						extraParameterContext);
		if (returnAndMappingKey != null) {
			String[] targets = StringUtils.split(returnAndMappingKey.result,
					"\\|");
			for (String target : targets) {
				Field colMap = map.get(target);
				if (colMap == null) {
					colMap = new Field(1);
					map.put(target, colMap);
				}

				// ֻ������ҪsourceKey������²�ȥ��
				if (ruleContext.needSourceKey) {
					Set<Object> set = colMap.sourceKeys.get(column);
					if (set == null) {
						set = new HashSet<Object>(1);
						colMap.sourceKeys.put(column, set);
					}
					set.add(value);
				}
			}
		}
	}

	/**
	 * ��ͼ��table����Ŀ��targeDB
	 * 
	 * @param targetDB
	 * @param resultSet
	 */
	private void buildOrAddTable(TargetDB targetDB, Map<String, Field> resultSet) {
		for (Entry<String, Field> entry : resultSet.entrySet()) {
			Table table = (Table) subSharedElement.get(entry.getKey());
			if (table == null) {
				throw new IllegalArgumentException(
						"cant find table by target index :" + entry
								+ " current sub tables is " + subSharedElement);
			}
			targetDB.addOneTableWithSameTable(table.getTableName(),
					entry.getValue());
		}
	}

	// ////////////////////////////////////////////////////////////////////
	private void buildTableNameSet(TargetDB targetDB,
			Map<String, Field> resultSet) {
		for (Entry<String, Field> entry : resultSet.entrySet()) {
			Table table = (Table) subSharedElement.get(entry.getKey());
			if (table == null) {
				throw new IllegalArgumentException(
						"cant find table by target index :" + entry
								+ " current sub tables is " + subSharedElement);
			}
			targetDB.addOneTable(table.getTableName(), entry.getValue());
		}
	}

	private Map<String, Set<Object>> getEnumeratedSqlArgsMap(
			CalculationContextInternal calculationContext,
			ListAbstractResultRule rule) {
		if (rule == null) {
			throw new IllegalStateException("should not be here");
		}
		// ǿת ��Ҫ�õ�����ķ���
		Map<String, Set<Object>> argsFromSQL = RuleUtils.getSamplingField(
				calculationContext.sqlArgs, rule.getParameters());
		return argsFromSQL;
	}

	private Map<String, Field> buildDefaultTable() {
		Map<String, Field> resultMap;
		resultMap = new HashMap<String, Field>();
		for (String defaultIndex : defaultListResult) {
			resultMap.put(defaultIndex, Field.EMPTY_FIELD);
		}
		return resultMap;
	}

	private Map<String, Field> builSingleTable() {
		Map<String, Field> resultMap = new HashMap<String, Field>(2);
		for (String key : subSharedElement.keySet()) {
			resultMap.put(key, Field.EMPTY_FIELD);
		}
		return resultMap;
	}

	/**
	 * �����ǰ�ڵ�û��tableMapProvider����ôʹ��1�Զำ����tableMapProvider.
	 */
	void initTableMapProvider() {
		if (this.tableMapProvider == null) {
			this.tableMapProvider = oneToManyEntry.getTableMapProvider();
		}
	}

	/**
	 * �����ǰ�ڵ�û��logicTableName,��ôʹ��1�Զำ���logictable.
	 */
	void initLogicTableName() {
		String logicTable = oneToManyEntry.getLogicTableName();
		if (logicTableName == null || logicTableName.length() == 0) {
			this.logicTableName = logicTable;
		}
	}

	/**
	 * ��ʼ��tableRuleChain,�����ǰ�ڵ��ListRule��Ϊ��,RuleChainΪ����ʹ��listRule�½�һ��ruleChain.
	 * ���listRuleΪ�գ�RuleChainҲΪ�գ���ôʹ��1�Զำ���ruleChain. ���ruleChain��Ϊ�գ����ʼ��֮
	 */
	void initTableRuleChain() {
		RuleChain ruleChain = oneToManyEntry.getTableRuleChain();
		// ���tableRuleList ��Ϊ�գ�����ruleChain == �ա���tableRuleList
		// ������ʵ��
		if (this.tableRuleList != null) {
			if (listResultRule != null) {
				throw new IllegalArgumentException(
						"��tableRuleList����ָ����ruleChain");
			} else {
				listResultRule = OneToManyEntry.getRuleChain(tableRuleList);
			}
		}
		// �����ʵ��δָ������ǰdatabase���������ʹ�ô��ݺ�Ĺ���
		if (listResultRule == null) {
			listResultRule = ruleChain;
		}
		// ��ǰdatabase�Ѿ��й��������£���ʼ����ǰ������Ϊrulechain�����ظ���ʼ��������ֻ���ʼ��һ�Ρ�
		if (ruleChain != null) {
			listResultRule.init();
		} else {
			log.warn("rule chain size is 0");
		}
	}

	protected Map<String, SharedElement> getTableMapByTableMapProvider() {
		TableMapProvider provider = getTableMapProvider();
		provider.setParentID(this.getId());
		provider.setLogicTable(getLogicTableName());
		Map<String, SharedElement> beConstructedMap = provider.getTablesMap();
		return beConstructedMap;
	}

	/**
	 * �����ñ�ݷ������ɵ���Ԫ��map���õ���ǰ�ڵ����Ԫ��map�����С�
	 * 
	 * �����ǰ�ӽڵ����Ԫ��map����Ϊ����ֱ������
	 * 
	 * �����ǰ�ӽڵ㲻Ϊ��,���ʾҵ��ͨ��spring�ķ�ʽset��һ�����е�map����
	 * 
	 * ���map�����ȼ�Ҫ���Զ����ɵ�map�����ȼ�Ҫ�ߡ�
	 * 
	 * @param beingConstructedMap
	 */
	protected void putAutoConstructedMapIntoCurrentTagMap(
			Map<String, SharedElement> beingConstructedMap) {
		if (this.subSharedElement == null) {
			subSharedElement = beingConstructedMap;
		} else {
			if (beingConstructedMap != null) {
				// ���Զ����table��ͬʱ����ͳһ������ô�����ϲ����Զ�����򸲸�ͨ��ͳһ����
				beingConstructedMap.putAll(subSharedElement);
				subSharedElement = beingConstructedMap;
			}
			// else
			// û���Զ����ɹ���ֻ���Զ��������ôʲô���鶼����
		}
	}

	/**
	 * 1��databse��Ӧһ��tables
	 * 
	 * @param tables
	 */
	public void setTables(Map<String, SharedElement> tablesMap) {
		super.subSharedElement = tablesMap;
	}

	@SuppressWarnings("unchecked")
	public Map<String, SharedElement> getTables() {
		return (Map<String, SharedElement>) super.subSharedElement;
	}

	private Map<String, SharedElement> getTablesMapByStringList(
			List<String> tablesString) {

		List<Table> tables = null;
		tables = new ArrayList<Table>(tablesString.size());

		for (String tabName : tablesString) {
			Table tab = new Table();
			tab.setTableName(tabName);
			tables.add(tab);
		}
		Map<String, SharedElement> returnMap = RuleUtils
				.getSharedElemenetMapBySharedElementList(tables);
		return returnMap;
	}

	/**
	 * ����ҵ��ֱ��ͨ��string->string�ķ�ʽ��ָ������
	 * 
	 * @param tablesMapString
	 */
	protected void setTablesMapString(
			Map<String/* ���index */, String/* ���ʵ�ʱ��� */> tablesMapString) {
		Map<String, SharedElement> beingConstructedMap = new HashMap<String, SharedElement>(
				tablesMapString.size());

		for (Entry<String, String> entry : tablesMapString.entrySet()) {
			Table table = new Table();
			table.setTableName(entry.getValue());
			beingConstructedMap.put(entry.getKey(), table);
		}
		putAutoConstructedMapIntoCurrentTagMap(beingConstructedMap);
	}

	@SuppressWarnings("unchecked")
	public void setTablesMapSimple(Object obj) {
		if (obj instanceof Map) {
			setTablesMapString((Map<String/* ���index */, String/* ���ʵ�ʱ��� */>) obj);
		} else if (obj instanceof List) {
			setTablesList((List) obj);
		}
	}

	/**
	 * ����ҵ��ʹ��table1,table2,table3�ķ�ʽ��ָ��������key=�����±ꡣ ͬʱҲ����ҵ��ʹ��list.add("table1");
	 * list.add("table2");... �ķ�ʽ��ָ����Key=�����±�
	 * 
	 * @param tablesString
	 */
	protected void setTablesList(List<String> tablesString) {
		// û����tablesStringlist��not null��飬��Ϊ����spring
		if (tablesString.size() == 1) {
			String[] tokens = tablesString.get(0).split(",");
			tablesString = new ArrayList<String>();
			tablesString.addAll(Arrays.asList(tokens));
			putAutoConstructedMapIntoCurrentTagMap(getTablesMapByStringList(tablesString));
		} else {
			putAutoConstructedMapIntoCurrentTagMap(getTablesMapByStringList(tablesString));
		}
	}

	public String getDataSourceKey() {
		return dataSourceKey;
	}

	public void setDataSourceKey(String dataSourceKey) {
		this.dataSourceKey = dataSourceKey;
	}

	@Override
	public String toString() {
		return "Database [dataSourceKey=" + dataSourceKey
				+ ", defaultListResult=" + defaultListResult
				+ ", defaultListResultStragety=" + defaultListResultStragety
				+ ", listResultRule=" + listResultRule + ", subSharedElement="
				+ subSharedElement + "]";
	}

	public void setLogicTableName(String logicTable) {
		this.logicTableName = logicTable;
	}

	public void setTableMapProvider(TableMapProvider tableMapProvider) {
		this.tableMapProvider = tableMapProvider;
	}

	public void setTableRule(List<ListAbstractResultRule> tableRule) {
		this.tableRuleList = tableRule;
	}

	public String getLogicTableName() {
		return logicTableName;
	}

	public TableMapProvider getTableMapProvider() {
		return tableMapProvider;
	}

	public RuleChain getRuleChain() {
		return super.listResultRule;
	}

	public void setTableRuleChain(RuleChain ruleChain) {
		super.listResultRule = ruleChain;
	}

	@Override
	public void put(OneToManyEntry oneToManyEntry) {
		this.oneToManyEntry = oneToManyEntry;
	}
}
