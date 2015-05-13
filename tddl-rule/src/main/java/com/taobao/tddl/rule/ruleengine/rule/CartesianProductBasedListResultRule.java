package com.taobao.tddl.rule.ruleengine.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;
import com.taobao.tddl.interact.rule.bean.SamplingField;
import com.taobao.tddl.interact.rule.enumerator.Enumerator;
import com.taobao.tddl.interact.rule.enumerator.EnumeratorImp;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.rule.ruleengine.cartesianproductcalculator.CartesianProductCalculator;
import com.taobao.tddl.rule.ruleengine.util.RuleUtils;

/**
 * �������һ�����Ĺ���
 * 
 * @author shenxun
 * 
 */
public abstract class CartesianProductBasedListResultRule extends
		ListAbstractResultRule {

	private final Log log = LogFactory
			.getLog(CartesianProductBasedListResultRule.class);
	Enumerator enumerator = new EnumeratorImp();

	/**
	 * ��ǰΪ����Ԥ���������Ѿ��������������
	 */
	private boolean isDebug;

	/**
	 * �˷����Ѿ�δʹ�ã��滻�ķ������棬Ϊ�˵�Ԫ���Լ�����
	 * �Ƿ���Ҫ�Խ����ڵ�����ȡ������
	 */
	// protected boolean needMergeValueInCloseInterval = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.taobao.tddl.rule.ruleengine.rule.ListAbstractResultRule#eval(java
	 * .util.Map)
	 * 
	 * @Tested
	 */

/*	public Map<String �����ֵ����db��index��table��index , Field> eval(
	Map<String, Comparative> argumentsMap) {
		Map<String, Set<Object>> enumeratedMap = prepareEnumeratedMap(argumentsMap);// ������㼯��
		if (log.isDebugEnabled()) {
			log.debug("Sampling filed message : " + enumeratedMap);
		}
		Map<String, Field> map = evalElement(enumeratedMap);
		decideWhetherOrNotToThrowSpecEmptySetRuntimeException(map);// �����Ƿ��׳�runtimeException
		return map;
	}*/
	
	public Map<String/* �����ֵ����db��index��table��index */, Field> eval(
			Map<String, Comparative> argumentsMap,ExtraParameterContext extraParameterContext) {
		Map<String, Set<Object>> enumeratedMap = prepareEnumeratedMap(argumentsMap);// ������㼯��
		if (log.isDebugEnabled()) {
			log.debug("Sampling filed message : " + enumeratedMap);
		}
		Map<String, Field> map = evalElement(enumeratedMap,extraParameterContext);
		decideWhetherOrNotToThrowSpecEmptySetRuntimeException(map);// �����Ƿ��׳�runtimeException
		return map;
	}

	// /* (non-Javadoc)
	// * @see
	// com.taobao.tddl.rule.ruleengine.rule.ListAbstractResultRule#evalWithoutSourceTrace(java.util.Map)
	// *
	// *
	// */
	// public Set<String> evalWithoutSourceTrace(Map<String, Set<Object>>
	// enumeratedMap){
	// if (enumeratedMap.size() == 1) {
	// return evalOneArgumentExpression(enumeratedMap);
	//
	// } else {
	// return evalMutiargumentsExpression(enumeratedMap);
	// }
	// }

	/**
	 * �����Ƿ��׳�runtimeException
	 * 
	 * @param map
	 */
	private void decideWhetherOrNotToThrowSpecEmptySetRuntimeException(
			Map<String, Field> map) {
		if ((map == null || map.isEmpty())
				&& ruleRequireThrowRuntimeExceptionWhenSetIsEmpty()) {
			throw new EmptySetRuntimeException();
		}
	}

	/**
	 * TODO:���Ҫ�ᵽ���෽����
	 * 
	 * @param argumentsMap
	 * @return
	 */
	public Map<String, Set<Object>> prepareEnumeratedMap(
			Map<String, Comparative> argumentsMap) {
		if (log.isDebugEnabled()) {
			log.debug("eval at CartesianProductRule ,param is " + argumentsMap);
		}

		Map<String/* column */, Set<Object>/* ��� */> enumeratedMap = RuleUtils
				.getSamplingField(argumentsMap, parameters);
		return enumeratedMap;
	}

	// private Set<String> evalMutiargumentsExpression(
	// Map<String, Set<Object>> enumeratedMap) {
	// Set<String> set;
	//
	// // TODO:�õ����ֵ��ͬ�����ֿ��ֱ��ʱ����Ҫreview
	// // ����һ��ֵ����Ҫ���еѿ�����
	// CartesianProductCalculator cartiesianProductCalculator = new
	// CartesianProductCalculator(
	// enumeratedMap);
	// /*
	// * ȷʵ����ȷ��set�Ĵ�С����һ����˵�ֿ���16������������Ͷ�16����ʱ������һ�ֿ��ܵĿ����ǽ�
	// * capacity����Ϊ�����ܳ��ֵĽ����
	// */
	// set = new HashSet<String>(16);
	// for (SamplingField samplingField : cartiesianProductCalculator) {
	// evalOnceAndAddToReturnSet(set, samplingField,16);
	// }
	//
	// return set;
	// }

	// private Set<String> evalOneArgumentExpression(
	// Map<String, Set<Object>> enumeratedMap) {
	// Set<String> set;
	// // ����һ��ֵ����Ҫ���еѿ�����
	// List<String> columns = new ArrayList<String>(1);
	// Set<Object> enumeratedValues = null;
	// for (Entry<String, Set<Object>> entry : enumeratedMap.entrySet()) {
	// columns.add(entry.getKey());
	// enumeratedValues = entry.getValue();
	// }
	//
	// SamplingField samplingField = new SamplingField(columns, 1);
	//
	//
	// // ����ֵ���Ҳ�����뺯����x�ĸ������Ӧ
	// set = new HashSet<String>(enumeratedValues.size());
	// evalNormal(set, enumeratedValues, samplingField);
	//
	// if ((set == null || set.isEmpty())
	// && ruleRequireThrowRuntimeExceptionWhenSetIsEmpty()) {
	// throw new EmptySetRuntimeException();
	// }
	// return set;
	// }

	// private void evalNormal(Set<String> set, Set<Object> enumeratedValues,
	// SamplingField samplingField) {
	// for (Object value : enumeratedValues) {
	// samplingField.clear();
	// samplingField.add(0, value);
	// evalOnceAndAddToReturnSet(set, samplingField,enumeratedValues.size());
	// }
	// }

	/**
	 * �˷����Ѿ�δʹ�ã��滻�ķ������棬Ϊ�˵�Ԫ���Լ�����
	 * 
	 * �����ļ�����̣�����->���������������м��㣬��ȡ���ս����
	 * 
	 * @param enumeratedMap
	 * @return ���ص�map����Ϊnull,���п���Ϊ�յ�map�����map��Ϊ�գ����ڲ�����map�ض���Ϊ�ա����ٻ���һ��ֵ
	 */
	/*public Map<String �����ֵ , Field> evalElement(
			Map<String, Set<Object>> enumeratedMap) {
		Map<String �����ֵ , Field> map;
		if (enumeratedMap.size() == 1) {
			// �и�������һ��ֵ����Ҫ���еѿ�����
			List<String> columns = new ArrayList<String>(1);
			Set<Object> enumeratedValues = null;
			for (Entry<String, Set<Object>> entry : enumeratedMap.entrySet()) {
				columns.add(entry.getKey());
				enumeratedValues = entry.getValue();
			}

			SamplingField samplingField = new SamplingField(columns, 1);
			// ����ֵ���Ҳ�����뺯����x�ĸ������Ӧ
			map = new HashMap<String, Field>(enumeratedValues.size());
			// Ϊ�����и��������ֶ�
			for (Object value : enumeratedValues) {
				samplingField.clear();
				samplingField.add(0, value);
				evalOnceAndAddToReturnMap(map, samplingField,
						enumeratedValues.size());
			}

			return map;

		} else {
			// ����һ��ֵ����Ҫ���еѿ�����
			CartesianProductCalculator cartiesianProductCalculator = new CartesianProductCalculator(
					enumeratedMap);
			
			 * ȷʵ����ȷ��set�Ĵ�С����һ����˵�ֿ���16������������Ͷ�16����ʱ������һ�ֿ��ܵĿ����ǽ�
			 * capacity����Ϊ�����ܳ��ֵĽ����
			 
			map = new HashMap<String, Field>(16);
			for (SamplingField samplingField : cartiesianProductCalculator) {
				evalOnceAndAddToReturnMap(map, samplingField, 16);
			}

			return map;
		}
	}
	*/
	/**
	 * 
	 * �����ļ�����̣�����->���������������м��㣬��ȡ���ս����
	 * 
	 * @param enumeratedMap
	 * @return ���ص�map����Ϊnull,���п���Ϊ�յ�map�����map��Ϊ�գ����ڲ�����map�ض���Ϊ�ա����ٻ���һ��ֵ
	 */
	public Map<String/* �����ֵ */, Field> evalElement(
			Map<String, Set<Object>> enumeratedMap,ExtraParameterContext extraParameterContext) {
		Map<String/* �����ֵ */, Field> map;
		if (enumeratedMap.size() == 1) {
			// �и�������һ��ֵ����Ҫ���еѿ�����
			List<String> columns = new ArrayList<String>(1);
			Set<Object> enumeratedValues = null;
			for (Entry<String, Set<Object>> entry : enumeratedMap.entrySet()) {
				columns.add(entry.getKey());
				enumeratedValues = entry.getValue();
			}
			
			SamplingField samplingField = new SamplingField(columns, 1);
			// ����ֵ���Ҳ�����뺯����x�ĸ������Ӧ
			map = new HashMap<String, Field>(enumeratedValues.size());
			// Ϊ�����и��������ֶ�
			for (Object value : enumeratedValues) {
				samplingField.clear();
				samplingField.add(0, value);
				evalOnceAndAddToReturnMap(map, samplingField,
						enumeratedValues.size(),extraParameterContext);
			}
			
			return map;
			
		} else {
			// ����һ��ֵ����Ҫ���еѿ�����
			CartesianProductCalculator cartiesianProductCalculator = new CartesianProductCalculator(
					enumeratedMap);
			/*
			 * ȷʵ����ȷ��set�Ĵ�С����һ����˵�ֿ���16������������Ͷ�16����ʱ������һ�ֿ��ܵĿ����ǽ�
			 * capacity����Ϊ�����ܳ��ֵĽ����
			 */
			map = new HashMap<String, Field>(16);
			for (SamplingField samplingField : cartiesianProductCalculator) {
				evalOnceAndAddToReturnMap(map, samplingField, 16,extraParameterContext);
			}
			
			return map;
		}
	}

	/**
	 * ����ӹ�����Ҫ�ڷ���ֵΪnull��Ϊ��collectionsʱ�׳��쳣����̳д����false��Ϊtrue����
	 * 
	 * @return
	 */
	protected boolean ruleRequireThrowRuntimeExceptionWhenSetIsEmpty() {
		return false;
	}

	void evalOnceAndAddToReturnSet(Set<String> set,
			SamplingField samplingField, int valueSetSize) {
		ResultAndMappingKey resultAndMappingKey = evalueateSamplingField(samplingField,null);
		String targetIndex = resultAndMappingKey.result;
		// ODOT:�ظ��ж�
		if (targetIndex != null) {
			String[] targets = StringUtil.split(targetIndex, "\\|");
			for (String str : targets) {
				set.add(str);
			}
		} else {
			throw new IllegalArgumentException("��������Ľ������Ϊnull");
		}
	}

	/**
	 * �÷����Ѿ�δʹ�ã�Ϊ�˼��ݵ�Ԫ����
	 * ��һ�����ݽ��м���
	 * 
	 * ֻ�������ݼ����ȡ��ֵ��ʱ��ŻὫ��Ӧ��ֵ��ȡ���кͶ������ڵ�ֵ����map�С�
	 * 
	 * @param map
	 * @param samplingField
	 * @param valueSetSize
	 * @Test ���������TairBasedMappingRule�ļ��ɲ��Ժ͵�Ԫ�����ﶼ��
	 */
	/*void evalOnceAndAddToReturnMap(Map<String �����ֵ , Field> map,
			SamplingField samplingField, int valueSetSize) {
		ResultAndMappingKey returnAndMappingKey = evalueateSamplingField(samplingField);
		if (returnAndMappingKey != null) {
			String[] targets = StringUtils.split(returnAndMappingKey.result,
					"\\|");
			for (String target : targets) {
				List<String> lists = samplingField.getColumns();
				List<Object> values = samplingField.getEnumFields();

				Field colMap = prepareColumnMap(map, samplingField, target);

				int index = 0;
				for (String column : lists) {
					Object value = values.get(index);
					Set<Object> set = prepareEnumeratedSet(valueSetSize,
							colMap, column);
					set.add(value);
					index++;
				}
			}

		}
	}*/
	/**
	 * ��һ�����ݽ��м���
	 * 
	 * ֻ�������ݼ����ȡ��ֵ��ʱ��ŻὫ��Ӧ��ֵ��ȡ���кͶ������ڵ�ֵ����map�С�
	 * 
	 * @param map
	 * @param samplingField
	 * @param valueSetSize
	 * @Test ���������TairBasedMappingRule�ļ��ɲ��Ժ͵�Ԫ�����ﶼ��
	 */
	void evalOnceAndAddToReturnMap(Map<String/* �����ֵ */, Field> map,
			SamplingField samplingField, int valueSetSize, ExtraParameterContext extraParameterContext) {
		ResultAndMappingKey returnAndMappingKey = evalueateSamplingField(samplingField,extraParameterContext);
		if (returnAndMappingKey != null) {
			String[] targets = StringUtils.split(returnAndMappingKey.result,
			"\\|");
			for (String target : targets) {
				List<String> lists = samplingField.getColumns();
				List<Object> values = samplingField.getEnumFields();
				
				Field colMap = prepareColumnMap(map, samplingField, target,returnAndMappingKey);
				
				int index = 0;
				for (String column : lists) {
					Object value = values.get(index);
					Set<Object> set = prepareEnumeratedSet(valueSetSize,
							colMap, column);
					set.add(value);
					index++;
				}
			}
			
		}
	}

	private Set<Object> prepareEnumeratedSet(int valueSetSize, Field colMap,
			String column) {
		// sourcekey ��ʼ���Ժ���ڲ���set��һֱ����
		Set<Object> set = colMap.sourceKeys.get(column);
		if (set == null) {
			set = new HashSet<Object>(valueSetSize);
			colMap.sourceKeys.put(column, set);
		}
		return set;
	}

	private Field prepareColumnMap(Map<String, Field> map,
			SamplingField samplingField, String targetIndex,ResultAndMappingKey returnAndMappingKey) {
		Field colMap = map.get(targetIndex);
		if (colMap == null) {
			int size = samplingField.getColumns().size();
			colMap = new Field(size);
			map.put(targetIndex, colMap);
		}

		if (returnAndMappingKey.mappingTargetColumn != null&&colMap.mappingTargetColumn == null) {
			colMap.mappingTargetColumn = returnAndMappingKey.mappingTargetColumn;
		}
		if (returnAndMappingKey.mappingKey != null) {
			if(colMap.mappingKeys == null){
				colMap.mappingKeys = new HashSet<Object>();
			}
			colMap.mappingKeys.add(returnAndMappingKey.mappingKey);
		}

		return colMap;
	}

	// public Map<String, Set<Object>/* ����������key��ֵ��pair */> getSamplingField(
	// Map<String, SharedValueElement> sharedValueElementMap) {
	// // TODO:��ϸע��,����ѿ�����
	// // ö���Ժ��columns�����ǵ����֮��Ķ�Ӧ��ϵ
	// Map<String, Set<Object>> enumeratedMap = new HashMap<String,
	// Set<Object>>(
	// sharedValueElementMap.size());
	// for (Entry<String, SharedValueElement> entry : sharedValueElementMap
	// .entrySet()) {
	// SharedValueElement sharedValueElement = entry.getValue();
	// String key = entry.getKey();
	// // ��ǰenumerator��ָ����ǰ�����Ƿ���Ҫ���������⡣
	// // enumerator.setNeedMergeValueInCloseInterval();
	//
	// try {
	// Set<Object> samplingField = enumerator.getEnumeratedValue(
	// sharedValueElement.comp,
	// sharedValueElement.cumulativeTimes,
	// sharedValueElement.atomicIncreaseValue,
	// sharedValueElement.needMergeValueInCloseInterval);
	// enumeratedMap.put(key, samplingField);
	// } catch (UnsupportedOperationException e) {
	// throw new UnsupportedOperationException("��ǰ�зֿ�ֱ���ִ��󣬳��ִ����������:"
	// + entry.getKey(), e);
	// }
	//
	// }
	// return enumeratedMap;
	// }

	/**
	 * ����һ������������һ�����
	 * 
	 * @return ͨ������Ľ�����������������Ϊnull: ӳ�����ԭ������ڣ���ӳ����Ŀ�겻���ڣ��᷵��null�� ����ʱ�̣������쳣
	 * 
	 */
/*	public abstract ResultAndMappingKey evalueateSamplingField(
			SamplingField samplingField);*/
	
	public abstract ResultAndMappingKey evalueateSamplingField(
			SamplingField samplingField,ExtraParameterContext extraParameterContext);

	

	public boolean isDebug() {
		return isDebug;
	}

	public void setDebug(boolean isDebug) {
		this.isDebug = isDebug;
	}

}
