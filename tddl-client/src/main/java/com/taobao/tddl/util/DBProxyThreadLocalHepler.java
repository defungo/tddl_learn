package com.taobao.tddl.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.RouteCondition.ROUTE_TYPE;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.common.util.TStringUtil;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeAND;
import com.taobao.tddl.interact.sqljep.ComparativeBaseList;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.util.HintParser.RouteMethod;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.AdvanceCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.AdvancedDirectlyRouteCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.SimpleCondition;

/**
 * Description: ����������Ҫ�ṩ��TDDLʹ�õ�THREAD_LOCAL�Ķ�������String,�ͽ������Stringת��
 * �ɶ�Ӧ��THREAD_LOCAL�Ķ��󣬷���DPS��THREAD_LOCAL��ֵ�������紫�ݡ�
 *
 * @author: qihao
 * @version: 1.0 Filename: DBProxyThreadLocalHepler.java Create at: Nov 8, 2010
 *           10:39:15 AM
 *
 *           Copyright: Copyright (c)2010 Company: TaoBao
 *
 *           Modification History: Date Author Version Description
 *           ------------------------------------------------------------------
 *           Nov 8, 2010 qihao 1.0 1.0 Version
 */
public class DBProxyThreadLocalHepler {
	/**
	 * THREAD_LOCAL��key��Ӧ�����ͳ���
	 */
	private static final int INTEGER_TYPE = 1;
	private static final int BOOLEAN_TYPE = 2;
	private static final int ROUTE_CONDITION_TYPE = 3;
	/**
	 * RouteCondition ��ʵ�����ͳ���
	 */
	private static final int ADVANCED_DIRECTLY_CLASS_TYPE = 1;
	private static final int DIRECTLY_CLASS_TYPE = 2;
	private static final int ADVANCE_CONDITION_CLASS_TYPE = 3;
	private static final int SIMPLE_CONDITION_CLASS_TYPE = 4;
	/**
	 * ROUTE_TYPE �ĳ���(DBProxy��hint)
	 */
	private static final int ROUTE_TYPE_FLUSH_ON_CLOSECONNECTION = 1;
	private static final int ROUTE_TYPE_FLUSH_ON_EXECUTE = 2;

	/**
	 * ROUTE_TYPE �ĳ���(�û���Sql hint)
	 */
	private static final String CONNECTION = "connection";
	private static final String EXECUTE = "execute";

	/**
	 * ��threadLocal�������ֵdump��Map<String, String>
	 * ע���������������FLUSH_ON_EXECUTE�Ļ�������dump ��������threadLocal�ж�Ӧ��ֵ
	 *
	 * @return
	 */
	public static Map<String, String> dumpThreadLocal2StrMap() {
		ThreadLocalKey[] keyEnums = ThreadLocalKey.values();
		Map<String, String> threadLocals = new HashMap<String, String>(
				keyEnums.length);
		for (ThreadLocalKey keyEnum : keyEnums) {
			String threadLocalKey = keyEnum.getKey();
			String strData = encodeThreadLocal(threadLocalKey);
			if (StringUtil.isNotBlank(strData)) {
				threadLocals.put(threadLocalKey, strData);
				Object object = ThreadLocalMap.get(threadLocalKey);
				// ���RouteCondition��ROUTE_TYPE ��FLUSH_ON_EXECUTE
				// dump��ɺ�����threadLocal�ж�Ӧ��key
				if (object instanceof RouteCondition) {
					RouteCondition routeCondition = (RouteCondition) object;
					if (ROUTE_TYPE.FLUSH_ON_EXECUTE == routeCondition
							.getRouteType()) {
						ThreadLocalMap.put(threadLocalKey, null);
					}
				}
			}
		}
		return threadLocals;
	}

	/**
	 * �÷��������ӹر�ʱ���ã���Ҫ�������������FLUSH_ON_CLOSECONNECTION ��ThreadLocal����
	 */
	public static void cleanOnCloseConnectionThreadLocal() {
		ThreadLocalKey[] keyEnums = ThreadLocalKey.values();
		for (ThreadLocalKey keyEnum : keyEnums) {
			String threadLocalKey = keyEnum.getKey();
			Object object = ThreadLocalMap.get(threadLocalKey);
			if (null != object) {
				// �����RouteCondition���͵���Ҫ�ж���ROUTE_TYPE
				// ���ROUTE_TYPE��FLUSH_ON_CLOSECONNECTION �����
				if (object instanceof RouteCondition) {
					RouteCondition routeCondition = (RouteCondition) object;
					if (ROUTE_TYPE.FLUSH_ON_CLOSECONNECTION == routeCondition
							.getRouteType()) {
						ThreadLocalMap.put(threadLocalKey, null);
					}
				} else {
					// ����RouteCondition���͵������
					ThreadLocalMap.put(threadLocalKey, null);
				}
			}
		}
	}

	/**
	 * ��ThreadLocal������ֵ����Ҫ�ǲ��뽫ThreadLocalMap ����౩©���ⲿϵͳ��ȥ��
	 *
	 * @param key
	 * @param value
	 */
	public static void setThreadLocal(String key, Object value) {
		ThreadLocalMap.put(key, value);
	}

	/**
	 * ��ָ����ThreadLocal��key��Ӧ�Ķ��󣬱�����ַ���
	 *
	 * @param keyThreadLocal��key
	 * @return
	 */
	public static String encodeThreadLocal(String key) {
		String strValue = null;
		ThreadLocalKey enumKey = ThreadLocalKey.getThreadLocalKey(key);
		Object objValue = ThreadLocalMap.get(key);
		// �����ȡ������Ӧ��ö�ٶ���˵��key�ǲ���֧�ֵģ�����value��null
		if (null == enumKey || null == objValue) {
			return strValue;
		}
		// ����key�����Ͷ�Ӧ��ϵ����valueת����String
		switch (enumKey.getType()) {
		case INTEGER_TYPE:
			strValue = Integer.toString((Integer) objValue);
			break;
		case BOOLEAN_TYPE:
			strValue = Boolean.toString((Boolean) objValue);
			break;
		case ROUTE_CONDITION_TYPE:
			strValue = encodeRouteCondition(objValue);
		}
		return strValue;
	}

	/**
	 * ����ָ����ThreadLocal��key���ͱ�������ַ�������� ����ǰ�Ķ���
	 *
	 * @param key
	 *            ThreadLocal��key
	 * @param strData
	 *            ��Ӧ�ľ���������ַ�������
	 * @return
	 */
	public static Object decodeThreadLocal(String key, String strData) {
		Object object = null;
		ThreadLocalKey enumKey = ThreadLocalKey.getThreadLocalKey(key);
		// �����ȡ������Ӧ��ö�ٶ���˵��key�ǲ���֧�ֵ�,�����ǲ��Ϸ����ַ���
		if (null == enumKey || StringUtil.isBlank(strData)) {
			return object;
		}
		// ����key�����Ͷ�Ӧ��ϵ����valueת����String
		switch (enumKey.getType()) {
		case INTEGER_TYPE:
			object = Integer.valueOf(strData);
			break;
		case BOOLEAN_TYPE:
			object = Boolean.valueOf(strData);
			break;
		case ROUTE_CONDITION_TYPE:
			object = decodeRouteCondition(strData);
		}
		return object;
	}

	/*
	 * ��routeCondition ��������String
	 *
	 * @param routeCondition
	 *
	 * @return
	 */
	private static String encodeRouteCondition(Object routeCondition) {
		String strCondition = null;
		JSONObject jsonObject = new JSONObject();
		try {
			if (routeCondition instanceof DirectlyRouteCondition) {
				// ���ù�����������
				DirectlyRouteCondition directlyRouteCondition = (DirectlyRouteCondition) routeCondition;
				jsonObject.put("dbId", directlyRouteCondition.getDbRuleID());
				if (ROUTE_TYPE.FLUSH_ON_CLOSECONNECTION.toString().equals(
						directlyRouteCondition.getRouteType().toString())) {
					jsonObject.put("routeType",
							ROUTE_TYPE_FLUSH_ON_CLOSECONNECTION);
				} else if (ROUTE_TYPE.FLUSH_ON_EXECUTE.toString().equals(
						directlyRouteCondition.getRouteType().toString())) {
					jsonObject.put("routeType", ROUTE_TYPE_FLUSH_ON_EXECUTE);
				}
				jsonObject.put("virtualTableName",
						directlyRouteCondition.getVirtualTableName());
				// ���ø�������
				if (routeCondition.getClass() == DirectlyRouteCondition.class) {
					// ����RouteCondition��class����ΪDirectlyRouteCondition
					jsonObject.put("classType", DIRECTLY_CLASS_TYPE);
					Set<String> tableSet = directlyRouteCondition.getTables();
					if (null != tableSet && !tableSet.isEmpty()) {
						JSONArray jsonTables = new JSONArray();
						for (String table : tableSet) {
							jsonTables.put(table);
						}
						jsonObject.put("tables", jsonTables);
					}
				} else if (routeCondition.getClass() == AdvancedDirectlyRouteCondition.class) {
					// ����RouteCondition��class����ΪAdvancedDirectlyRouteCondition
					jsonObject.put("classType", ADVANCED_DIRECTLY_CLASS_TYPE);
					AdvancedDirectlyRouteCondition advancedDirectlyRouteCondition = (AdvancedDirectlyRouteCondition) routeCondition;
					Map<String, List<Map<String, String>>> shardTableMap = advancedDirectlyRouteCondition
							.getShardTableMap();
					JSONObject jsonShardTableMap = new JSONObject();
					for (Map.Entry<String, List<Map<String, String>>> entry : shardTableMap
							.entrySet()) {
						jsonShardTableMap.put(entry.getKey(), entry.getValue());
					}
					jsonObject.put("shardTableMap", jsonShardTableMap);
				} else {
					throw new RuntimeException(
							"encodeRouteCondition Error not support RouteCondition type: "
									+ routeCondition.getClass().getName());
				}
				strCondition = jsonObject.toString();
			} else if (routeCondition instanceof SimpleCondition) {
				// ��������������
				SimpleCondition simpleCondition = (SimpleCondition) routeCondition;
				if (ROUTE_TYPE.FLUSH_ON_CLOSECONNECTION.toString().equals(
						simpleCondition.getRouteType().toString())) {
					jsonObject.put("routeType",
							ROUTE_TYPE_FLUSH_ON_CLOSECONNECTION);
				} else if (ROUTE_TYPE.FLUSH_ON_EXECUTE.toString().equals(
						simpleCondition.getRouteType().toString())) {
					jsonObject.put("routeType", ROUTE_TYPE_FLUSH_ON_EXECUTE);
				}
				jsonObject.put("virtualTableName",
						simpleCondition.getVirtualTableName());
				// ����parameters
				Map<String, Comparative> parameters = simpleCondition
						.getParameters();
				if (null != parameters && !parameters.isEmpty()) {
					JSONObject jsonParameters = new JSONObject();
					for (Map.Entry<String, Comparative> entry : parameters
							.entrySet()) {
						String paramKey = entry.getKey();
						Comparative praramValue = entry.getValue();
						// ��������Ϸ�����Comparative���б����String
						if (StringUtil.isNotBlank(paramKey)
								&& null != praramValue) {
							String encdeComparativeStr = encodeComparative(praramValue);
							if (StringUtil.isNotBlank(encdeComparativeStr)) {
								jsonParameters.put(paramKey,
										encdeComparativeStr);
							}
						}
					}
					jsonObject.put("parameters", jsonParameters);
				}
				// ����classType
				if (routeCondition.getClass() == SimpleCondition.class) {
					jsonObject.put("classType", SIMPLE_CONDITION_CLASS_TYPE);
				} else if (routeCondition.getClass() == AdvanceCondition.class) {
					jsonObject.put("classType", ADVANCE_CONDITION_CLASS_TYPE);
				} else {
					throw new RuntimeException(
							"encodeRouteCondition Error not support RouteCondition type: "
									+ routeCondition.getClass().getName());
				}
				strCondition = jsonObject.toString();
			} else {
				throw new RuntimeException(
						"encodeRouteCondition Error not support RouteCondition type: "
								+ routeCondition.getClass().getName());
			}
		} catch (JSONException e) {
			throw new RuntimeException("encodeRouteCondition Error !", e);
		}
		return strCondition;
	}

	/*
	 * ��comparativeת��������ַ����������ǵ�һ��ϵ���㣬 ���Ƕ��ϵ����
	 * [������(and/or)]~��ϵ��������1;ֵ����1:ֵ1,��ϵ��������2;ֵ����2:ֵ2........��ϵ��������n;ֵ����n:ֵn
	 *
	 * ע�⣺1.���ϵ���� ֻ֧��һ�㣬��֧��Ƕ�ף�����ϵ����ֵ�ﲻ֧���ٴδ��ڶ��ϵ��comparative
	 * ���磺a>(b<c)�����ǲ�֧�ֵģ�ֻ֧��a>b and <c and <......�����ĸ�ʽ
	 * 2.��ϵ�������ֵĿǰֻ֧��Integer,Long,String,Date���� and~1;i:5,2;l:4
	 *
	 * @param comparative
	 *
	 * @return
	 */
	private static String encodeComparative(Comparative comparative) {
		StringBuilder sb = new StringBuilder();
		if (comparative instanceof ComparativeBaseList) {
			// ������ComparativeOR����comparativeAND
			ComparativeBaseList comparativeBaseList = (ComparativeBaseList) comparative;
			List<Comparative> comparativeList = comparativeBaseList.getList();
			if (null != comparativeList && !comparativeList.isEmpty()) {
				if (comparativeBaseList instanceof ComparativeAND) {
					sb.append("and").append("~");
				} else if (comparativeBaseList instanceof ComparativeOR) {
					sb.append("or").append("~");
				} else {
					throw new RuntimeException(
							"encodeComparative not support ComparativeBaseList!");
				}
				for (Comparative comp : comparativeList) {
					// ��������ǵڶ��㻹��ComparativeBaseList���͵�ֱ���׳��쳣��֧��
					if (comp instanceof ComparativeBaseList) {
						throw new RuntimeException(
								"encodeComparative not support second ComparativeBaseList!");
					}
					String strValue = encodeComparativeValue(comp);
					if (null != strValue) {
						sb.append(strValue).append(",");
					}
				}
				// ����Ƕ��comparative��ȥ���������,��
				String strData = sb.toString();
				if (StringUtil.isNotBlank(strData)
						&& strData.lastIndexOf(",") != -1) {
					return StringUtil.substringBeforeLast(strData, ",");
				}
			}
		} else {
			String strValue = encodeComparativeValue(comparative);
			if (null != strValue) {
				sb.append(strValue);
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	/*
	 * ����һ��ϵ�����Comparative ������ַ��� ��ʽ����ϵ��������;ֵ����:ֵ ���� >=1 ��2;i:4
	 *
	 * @param comp Comparative����
	 *
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static String encodeComparativeValue(Comparative comp) {
		StringBuilder sb = new StringBuilder();
		Comparable compValue = comp.getValue();
		if (null != compValue) {
			if (compValue instanceof Integer) {
				sb.append(comp.getComparison()).append(";").append("i:")
						.append(((Integer) compValue).intValue());
			} else if (compValue instanceof Long) {
				sb.append(comp.getComparison()).append(";").append("l:")
						.append(((Long) compValue).longValue());
			} else if (compValue instanceof String) {
				sb.append(comp.getComparison()).append(";").append("s:")
						.append(((String) compValue));
			} else if (compValue instanceof Date) {
				sb.append(comp.getComparison()).append(";").append("d:")
						.append(((Date) compValue).getTime());
			} else {
				throw new RuntimeException(
						"encodeComparative not support ComparativeValue!");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	/*
	 * ��������ַ�����RouteCondition ���뻹ԭ��RouteCondition����
	 *
	 * @param strData
	 *
	 * @return
	 */
	public static RouteCondition decodeRouteCondition(String strData) {
		RouteCondition routeCondition = null;
		try {
			JSONObject jsonObject = new JSONObject(strData);
			int classType = jsonObject.getInt("classType");
			// ˵����AdvancedDirectlyRouteCondition ����DirectlyRouteCondition
			if (classType == DIRECTLY_CLASS_TYPE
					|| classType == ADVANCED_DIRECTLY_CLASS_TYPE) {
				routeCondition = decodeNoComparativeRouteCondition(jsonObject);
			} else if (classType == SIMPLE_CONDITION_CLASS_TYPE
					|| classType == ADVANCE_CONDITION_CLASS_TYPE) {
				routeCondition = decodeComparativeRouteCondition(jsonObject);
			}
		} catch (JSONException e) {
			throw new RuntimeException("decodeRouteCondition Error !", e);
		}
		return routeCondition;
	}

	/*
	 * ��json�ַ�������ɴ�Comparative��RouteCondition
	 *
	 * @param jsonObject
	 *
	 * @return
	 *
	 * @throws JSONException
	 */
	private static RouteCondition decodeComparativeRouteCondition(
			JSONObject jsonObject) throws JSONException {
		int classType = jsonObject.getInt("classType");
		SimpleCondition comparativeCondition = null;
		if (classType == SIMPLE_CONDITION_CLASS_TYPE) {
			comparativeCondition = new SimpleCondition();
		} else if (classType == ADVANCE_CONDITION_CLASS_TYPE) {
			comparativeCondition = new AdvanceCondition();
		} else {
			throw new RuntimeException(
					"not Sport RouteCondition Type Error ! classType: "
							+ classType);
		}

		decodeVirtualTableName(comparativeCondition, jsonObject);
		decodeRouteType(comparativeCondition, jsonObject);
		decodeParameter(comparativeCondition, jsonObject);
		return comparativeCondition;
	}

	/**
	 * ���û�hint����ʹ�õ�ComparativeRouteCondition����
	 *
	 * @param jsonObject
	 * @return
	 * @throws JSONException
	 */
	public static RouteCondition decodeComparativeRouteCondition4Outer(
			JSONObject jsonObject) throws JSONException {
		SimpleCondition comparativeCondition = new SimpleCondition();
		decodeParameterForOuter(comparativeCondition, jsonObject);
		decodeVirtualTableName(comparativeCondition, jsonObject);
		decodeSpecifyInfo(comparativeCondition,jsonObject);
		return comparativeCondition;
	}

	private static void decodeSpecifyInfo(SimpleCondition condition,
			JSONObject jsonObject) throws JSONException{
		if (jsonContainsKey(jsonObject, "skip")) {
			Integer skip = Integer.valueOf(jsonObject.getString("skip"));
			if (skip!=null) {
				condition.setSkip(skip);
			}
		}

		if (jsonContainsKey(jsonObject, "max")) {
			Integer max = Integer.valueOf(jsonObject.getString("max"));
			if (max!=null) {
				condition.setMax(max);
			}
		}
	}

	/**
	 * ��Ҫ������������ʼ֮ǰʹ�ã������п�����;�ı����� �������Զ�ʧ
	 *
	 * @param condition
	 * @param jsonObject
	 * @throws JSONException
	 */
	private static void decodeParameter(SimpleCondition condition,
			JSONObject jsonObject) throws JSONException {
		if (jsonContainsKey(jsonObject, "parameters")) {
			// ���� ComparativeMap
			JSONObject jsonParameters = jsonObject.getJSONObject("parameters");
			if (null != jsonParameters && jsonParameters.length() != 0) {
				Iterator<String> keys = jsonParameters.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					String value = jsonParameters.getString(key);
					if (StringUtil.isNotBlank(value)) {
						// and~1;i:5,2;l:4
						if (StringUtil.contains(value, "~")) {
							// ˵����ComparativeBaseList
							String compStr = StringUtil.substringAfter(value,
									"~");
							String opStr = StringUtil.substringBefore(value,
									"~");
							if (StringUtil.isNotBlank(compStr)
									&& StringUtil.isNotBlank(opStr)) {
								ComparativeBaseList comparativeBaseList = null;
								if ("or".endsWith(opStr)) {
									comparativeBaseList = new ComparativeOR();
								} else if ("and".endsWith(opStr)) {
									comparativeBaseList = new ComparativeAND();
								} else {
									throw new RuntimeException(
											"decodeComparative not support ComparativeBaseList key: "
													+ key + " str:" + value);
								}
								String[] compValues = StringUtil.split(compStr,
										",");
								for (String compValue : compValues) {
									Comparative comparative = decodeComparative(compValue);
									if (null != comparative) {
										comparativeBaseList
												.addComparative(comparative);
									}
								}
								condition.put(key, comparativeBaseList);
							}
						} else {
							// ˵��ֻ��Comparative
							Comparative comparative = decodeComparative(value);
							if (null != comparative) {
								condition.put(key, comparative);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * ��Ҫ������������ʼ֮ǰʹ�ã������п�����;�ı����� �������Զ�ʧ
	 *
	 * @param condition
	 * @param jsonObject
	 * @throws JSONException
	 */
	public static void decodeParameterForOuter(SimpleCondition condition,
			JSONObject jsonObject) throws JSONException {
		// modified by jiechen.qzm ȷ��һ����parameters�������
		String parametersString = jsonContainsKeyAndValueNotBlank(jsonObject, "parameters");
		if (parametersString == null) {
			throw new RuntimeException(
					"hint contains no property 'parameters'.");
		}

		// ���� ComparativeMap
		JSONArray jsonParameters = new JSONArray(parametersString);
		if (jsonParameters.length() != 0) {
			for (int i = 0; i < jsonParameters.length(); i++) {
				String value = jsonParameters.getString(i).toLowerCase();
				if (StringUtil.isNotBlank(value)) {
					// pk=1;i and pk>=2;i
					boolean containsAnd = StringUtil.contains(value, " and ");
					boolean containsOr = StringUtil.contains(value, " or ");
					if (containsAnd || containsOr) {
						ComparativeBaseList comparativeBaseList = null;
						String op;
						if (containsOr) {
							comparativeBaseList = new ComparativeOR();
							op = " or ";
						} else if (containsAnd) {
							comparativeBaseList = new ComparativeAND();
							op = " and ";
						} else {
							throw new RuntimeException(
									"decodeComparative not support ComparativeBaseList value:"
											+ value);
						}
						String[] compValues = TStringUtil.twoPartSplit(value,
								op);
						String key = null;
						for (String compValue : compValues) {
							Comparative comparative = decodeComparativeForOuter(compValue);
							if (null != comparative) {
								comparativeBaseList.addComparative(comparative);
							}
							String temp = getComparativeKey(compValue).trim();
							if (null == key) {
								key = temp;
							} else if (!temp.equals(key)) {
								throw new RuntimeException(
										"decodeComparative not support ComparativeBaseList value:"
												+ value);
							}
						}
						condition.put(key, comparativeBaseList);
					} else {
						// ˵��ֻ��Comparative
						String key = getComparativeKey(value);
						Comparative comparative = decodeComparativeForOuter(value);
						if (null != comparative) {
							condition.put(key, comparative);
						}
					}
				}
			}
		}
	}

	public static String getComparativeKey(String compValue){
		int value = Comparative.getComparisonByCompleteString(compValue);
		String splitor = Comparative.getComparisonName(value);
		int index = compValue.indexOf(splitor);
		return StringUtil.substring(compValue,0, index);
	}

//	public static void main(String[] args) {
//		try {
//			DBProxyThreadLocalHepler db = new DBProxyThreadLocalHepler();
//			JSONObject jsonObject = new JSONObject("{parameters:[\"pk=1;i and pk>=2;i\",\"id<2;i or id<>3;i\",\"gmt=2011-11-11;d\"]}");
//			SimpleCondition comparativeCondition = new SimpleCondition();
//			db.decodeParameterForOuter(comparativeCondition,jsonObject);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//	}

	/*
	 * ����ʽ����ϵ��������;ֵ����:ֵ ���� >=1 ��2;i:4 ���ַ��������Comparative����
	 *
	 * @param compValue
	 *
	 * @return
	 */
	private static Comparative decodeComparative(String compValue) {
		// 1;i:5
		Comparative comparative = null;
		String strFuction = StringUtil.substringBefore(compValue, ";");
		String[] data = StringUtil.split(
				StringUtil.substringAfter(compValue, ";"), ":");
		if (StringUtil.isNumeric(strFuction) && null != data
				&& data.length == 2) {
			int fuction = Integer.valueOf(StringUtil.substringBefore(compValue,
					";"));
			String dataType = data[0];
			String dataValue = data[1];
			if ("i".equals(dataType)) {
				comparative = new Comparative(fuction,
						Integer.valueOf(dataValue));
			} else if ("l".equals(dataType)) {
				comparative = new Comparative(fuction, Long.valueOf(dataValue));
			} else if ("s".equals(dataType)) {
				comparative = new Comparative(fuction, dataValue);
			} else if ("d".equals(dataType)) {
				comparative = new Comparative(fuction, new Date(
						Long.valueOf(dataValue)));
			} else {
				throw new RuntimeException(
						"decodeComparative Error notSupport Comparative valueType value: "
								+ compValue);
			}
		} else {
			throw new RuntimeException("decodeComparative Error datsStr: "
					+ compValue);
		}
		return comparative;
	}

	private static Comparative decodeComparativeForOuter(String compValue) {
		Comparative comparative = null;
		int value = Comparative.getComparisonByCompleteString(compValue);
		String splitor = Comparative.getComparisonName(value);
		int size = splitor.length();
		int index = compValue.indexOf(splitor);
		String[] valueAndType = StringUtil.split(
				StringUtil.substring(compValue, index + size), ";");

		if (null != valueAndType && valueAndType.length == 2) {
			if ("i".equals(valueAndType[1].trim())) {
				comparative = new Comparative(value,
						Integer.valueOf(valueAndType[0]));
			} else if ("l".equals(valueAndType[1].trim())) {
				comparative = new Comparative(value,
						Long.valueOf(valueAndType[0]));
			} else if ("s".equals(valueAndType[1].trim())) {
				comparative = new Comparative(value, valueAndType[0]);
			} else if ("d".equals(valueAndType[1].trim())) {
				SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd");
				try {
					comparative = new Comparative(value, sdf.parse(valueAndType[0]));
				} catch (ParseException e) {
					throw new RuntimeException(
							"only support 'yyyy-MM-dd',now date string is:"
									+ valueAndType[0]);
				}
			} else if ("int".equals(valueAndType[1].trim())) {
				comparative = new Comparative(value,
						Integer.valueOf(valueAndType[0]));
			} else if ("long".equals(valueAndType[1].trim())) {
				comparative = new Comparative(value,
						Long.valueOf(valueAndType[0]));
			} else if ("string".equals(valueAndType[1].trim())) {
				comparative = new Comparative(value, valueAndType[0]);
			} else if ("date".equals(valueAndType[1].trim())) {
				SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd");
				try {
					comparative = new Comparative(value, sdf.parse(valueAndType[0]));
				} catch (ParseException e) {
					throw new RuntimeException(
							"only support 'yyyy-MM-dd',now date string is:"
									+ valueAndType[0]);
				}
			} else {
				throw new RuntimeException(
						"decodeComparative Error notSupport Comparative valueType value: "
								+ compValue);
			}
		} else {
			throw new RuntimeException(
					"decodeComparative Error notSupport Comparative valueType value: "
							+ compValue);
		}

		return comparative;
	}

	/*
	 * ��ûComparative�����RouteCondition�����String�ַ��������ݽ��� ��ԭ��RouteCondition����
	 * modified by junyu ����߼�����
	 *
	 * @param jsonObject
	 *
	 * @return
	 *
	 * @throws JSONException
	 */
	private static RouteCondition decodeNoComparativeRouteCondition(
			JSONObject jsonObject) throws JSONException {
		RouteCondition routeCondition;
		/*
		 * �ȶ���һ��DirectlyRouteCondition ��Ҫ��������
		 * AdvancedDirectlyRouteCondition��DirectlyRouteCondition �Ĺ�������
		 */
		DirectlyRouteCondition directlyRouteCondition = null;
		int classType = jsonObject.getInt("classType");
		if (DIRECTLY_CLASS_TYPE == classType) {
			// ����DirectlyRouteCondition ����������
			directlyRouteCondition = new DirectlyRouteCondition();
			if (jsonContainsKey(jsonObject, "tables")) {
				directlyRouteCondition = generateDirectlyRouteCondition(jsonObject);
			}
		} else if (ADVANCED_DIRECTLY_CLASS_TYPE == classType) {
			// ����AdvancedDirectlyRouteCondition ����������
			AdvancedDirectlyRouteCondition advancedDirectlyRouteCondition = new AdvancedDirectlyRouteCondition();
			// ����AdvancedDirectlyRouteCondition�� Map<String, List<Map<String,
			// String>>> tableMap����
			if (jsonContainsKey(jsonObject, "shardTableMap")) {
				advancedDirectlyRouteCondition = generateAdvancedDirectlyRouteCondition(jsonObject);
			}
			// ��������Ҫ��֤directlyRouteCondition���ʵ��һ����Ϊnull;
			directlyRouteCondition = advancedDirectlyRouteCondition;
		} else {
			throw new RuntimeException(
					"not Sport RouteCondition Type Error ! classType: "
							+ classType);
		}

		decodeDbId(directlyRouteCondition, jsonObject);
		decodeVirtualTableName(directlyRouteCondition, jsonObject);
		decodeRouteType(directlyRouteCondition, jsonObject);
		routeCondition = directlyRouteCondition;
		return routeCondition;
	}

	/**
	 * �û�ʹ�õ�hint����
	 * modified by jiechen.qzm �����ϸ�ƥ��
	 * @param jsonObject
	 * @return
	 * @throws JSONException
	 */
	public static RouteCondition decodeNoComparativeRouteCondition4Outer(
			JSONObject jsonObject, RouteMethod type) throws JSONException {
		DirectlyRouteCondition directlyRouteCondition = null;

		if(type.equals(RouteMethod.executeByDB)) {
			directlyRouteCondition = new DirectlyRouteCondition();
			decodeDbId(directlyRouteCondition, jsonObject);
		}
		else if(type.equals(RouteMethod.executeByDBAndTab)) {
			// ����DirectlyRouteCondition ����������
			directlyRouteCondition = generateDirectlyRouteCondition(jsonObject);
			decodeDbId(directlyRouteCondition, jsonObject);
			decodeVirtualTableName(directlyRouteCondition, jsonObject);
		}
		else if(type.equals(RouteMethod.executeByDBAndMutiReplace)) {
			// ����AdvancedDirectlyRouteCondition�� Map<String, List<Map<String,
			// String>>> tableMap����
			directlyRouteCondition = generateAdvancedDirectlyRouteCondition(jsonObject);
		}
		return directlyRouteCondition;
	}

	/**
	 * ����DirectlyRouteCondition
	 *
	 * @param jsonObject
	 * @return
	 * @throws JSONException
	 * @author junyu
	 */
	private static DirectlyRouteCondition generateDirectlyRouteCondition(
			JSONObject jsonObject) throws JSONException {
		DirectlyRouteCondition directlyRouteCondition = new DirectlyRouteCondition();

		// modified by jiechen.qzm ȷ��һ����tables�������
		String tableString = jsonContainsKeyAndValueNotBlank(jsonObject, "tables");
		if(tableString == null) {
			throw new RuntimeException("hint contains no property 'tables'.");
		}

		JSONArray jsonTables = new JSONArray(tableString);
		// ����table��Set<String>
		if (jsonTables.length() > 0) {
			Set<String> tables = new HashSet<String>(jsonTables.length());
			for (int i = 0; i < jsonTables.length(); i++) {
				tables.add(jsonTables.getString(i));
			}
			directlyRouteCondition.setTables(tables);
		}
		return directlyRouteCondition;
	}

	/**
	 * ����AdvancedDirectlyRouteCondition
	 *
	 * @param jsonObject
	 * @return
	 * @throws JSONException
	 */
	private static AdvancedDirectlyRouteCondition generateAdvancedDirectlyRouteCondition(
			JSONObject jsonObject) throws JSONException {
		// modified by jiechen.qzm ȷ��һ����shardTableMap�������
		String tableMapString = jsonContainsKeyAndValueNotBlank(jsonObject, "shardTableMap");
		if(tableMapString == null) {
			throw new RuntimeException("hint contains no property 'shardTableMap'.");
		}

		AdvancedDirectlyRouteCondition advancedDirectlyRouteCondition = new AdvancedDirectlyRouteCondition();
		JSONObject jsonShardTableMap = new JSONObject(tableMapString);
		if (jsonShardTableMap.length() > 0) {
			Iterator<String> shardTableMapKeys = jsonShardTableMap.keys();
			Map<String, List<Map<String, String>>> shardTableMap = new HashMap<String, List<Map<String, String>>>(
					jsonShardTableMap.length());
			while (shardTableMapKeys.hasNext()) {
				String key = shardTableMapKeys.next();
				JSONArray jsonTableList = jsonShardTableMap.getJSONArray(key);
				if (null != jsonTableList && jsonTableList.length() > 0) {
					List<Map<String, String>> tableMapList = new ArrayList<Map<String, String>>(
							jsonTableList.length());
					for (int i = 0; i < jsonTableList.length(); i++) {
						String jsonTabStr = jsonTableList.getString(i);
						if (StringUtil.isNotBlank(jsonTabStr)) {
							JSONObject jsonTableMap = new JSONObject(jsonTabStr);
							Map<String, String> tableMap = new HashMap<String, String>(
									jsonTableMap.length());
							Iterator<String> tableMapKey = jsonTableMap.keys();
							while (tableMapKey.hasNext()) {
								String tableKey = tableMapKey.next();
								String tableValue = jsonTableMap
										.getString(tableKey);
								if (StringUtil.isNotBlank(tableValue)) {
									tableMap.put(tableKey, tableValue);
								}
							}
							tableMapList.add(tableMap);
						}
					}
					shardTableMap.put(key, tableMapList);
				}
			}
			advancedDirectlyRouteCondition.setShardTableMap(shardTableMap);
		}
		return advancedDirectlyRouteCondition;
	}

	/**
	 * DbId����
	 *
	 * @param condition
	 * @param jsonObject
	 * @throws JSONException
	 */
	private static void decodeDbId(DirectlyRouteCondition condition,
			JSONObject jsonObject) throws JSONException {
		String dbId = jsonContainsKeyAndValueNotBlank(jsonObject, "dbId");
		if (dbId == null) {
			throw new RuntimeException("hint contains no property 'dbId'.");
		}
		condition.setDBId(dbId);
	}

	/**
	 * virtualTable����
	 *
	 * @param condition
	 * @param jsonObject
	 * @throws JSONException
	 */
	private static void decodeVirtualTableName(RouteCondition condition,
			JSONObject jsonObject) throws JSONException {

		String virtualTableName = jsonContainsKeyAndValueNotBlank(jsonObject, "virtualTableName");
		if (virtualTableName == null) {
			throw new RuntimeException("hint contains no property 'virtualTableName'.");
		}
		condition.setVirtualTableName(virtualTableName);
	}

	/**
	 * DBPROXYʹ�õ�sql hint����
	 *
	 * @param condition
	 * @param jsonObject
	 * @throws JSONException
	 */
	private static void decodeRouteType(RouteCondition condition,
			JSONObject jsonObject) throws JSONException {
		if (jsonContainsKey(jsonObject, "routeType")) {
			int routeType = jsonObject.getInt("routeType");
			if (routeType == ROUTE_TYPE_FLUSH_ON_CLOSECONNECTION) {
				condition.setRouteType(ROUTE_TYPE.FLUSH_ON_CLOSECONNECTION);
			} else if (routeType == ROUTE_TYPE_FLUSH_ON_EXECUTE) {
				condition.setRouteType(ROUTE_TYPE.FLUSH_ON_EXECUTE);
			}
		}
	}



	/**
	 * json�а���ĳ�����ԣ�����ֵ��Ϊ�գ����ظ�ֵ�����򷵻�null
	 * @param jsonObject
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	private static String jsonContainsKeyAndValueNotBlank(JSONObject jsonObject, String key) throws JSONException {
		if(!jsonContainsKey(jsonObject, key)){
			return null;
		}
		String value = jsonObject.getString(key);
		if (StringUtil.isBlank(value)) {
			return null;
		}
		return value;
	}

	/*
	 * �ж�JSON�Ƿ����ָ��key
	 *
	 * @param jsonObject
	 *
	 * @param key
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static boolean jsonContainsKey(JSONObject jsonObject, String key) {
		boolean res = false;
		Iterator<String> it = jsonObject.keys();
		while (it.hasNext()) {
			String itKey = it.next();
			if (StringUtil.equals(itKey, key)) {
				res = true;
				break;
			}
		}
		return res;
	}

	/**
	 * THREAD_LOACL��KEY ��ö�ٶ���������THREAD_LOCAL��KEY��VALUE�� ���Ͷ�Ӧ Description:
	 *
	 * @author: qihao
	 * @version: 1.0 Filename: DBProxyThreadLocalHepler.java Create at: Nov 8,
	 *           2010 4:07:32 PM
	 *
	 *           Copyright: Copyright (c)2010 Company: TaoBao
	 *
	 *           Modification History: Date Author Version Description
	 *           ----------
	 *           -------------------------------------------------------- Nov 8,
	 *           2010 qihao 1.0 1.0 Version
	 */
	private enum ThreadLocalKey {
		DATASOURCE_INDEX(ThreadLocalString.DATASOURCE_INDEX, INTEGER_TYPE), PARALLEL_EXECUTE(
				ThreadLocalString.PARALLEL_EXECUTE, BOOLEAN_TYPE), IS_EXIST_QUITE(
				ThreadLocalString.IS_EXIST_QUITE, BOOLEAN_TYPE), DB_SELECTOR(
				ThreadLocalString.DB_SELECTOR, ROUTE_CONDITION_TYPE), ROUTE_CONDITION(
				ThreadLocalString.ROUTE_CONDITION, ROUTE_CONDITION_TYPE), RULE_SELECTOR(
				ThreadLocalString.RULE_SELECTOR, ROUTE_CONDITION_TYPE);

		private String key;

		private int type;

		/**
		 * @param key
		 * @param type
		 */
		private ThreadLocalKey(String key, int type) {
			this.key = key;
			this.type = type;
		}

		public static ThreadLocalKey getThreadLocalKey(String key) {
			ThreadLocalKey enumKey = null;
			ThreadLocalKey[] ThreadLocalKeys = ThreadLocalKey.values();
			for (ThreadLocalKey threadLocalKey : ThreadLocalKeys) {
				if (threadLocalKey.getKey().equals(key)) {
					enumKey = threadLocalKey;
					break;
				}
			}
			return enumKey;
		}

		public String getKey() {
			return key;
		}

		public int getType() {
			return type;
		}
	}
}