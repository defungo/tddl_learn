package com.taobao.tddl.rule.groovy;

import groovy.lang.GroovyClassLoader;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;

import com.taobao.tddl.interact.rule.bean.AdvancedParameter;
import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;
import com.taobao.tddl.interact.rule.bean.SamplingField;
import com.taobao.tddl.rule.ruleengine.rule.CartesianProductBasedListResultRule;
import com.taobao.tddl.rule.ruleengine.rule.ResultAndMappingKey;

public class GroovyListRuleEngine extends CartesianProductBasedListResultRule {
	private static final Log logger = LogFactory
			.getLog(GroovyListRuleEngine.class);
	private Object ruleObj;
	private Method m_routingRuleMap;
	private static final String IMPORT_STATIC_METHOD = "import static com.taobao.tddl.rule.groovy.staticmethod.GroovyStaticMethod.*;";
	// Ӧ������������ģ���������evel��groovy�ű���
	private static final String IMPORT_EXTRA_PARAMETER_CONTEXT = "import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;";

	private Map<String, Object> context;

	protected void initInternal() {
		if (expression == null) {
			throw new IllegalArgumentException("δָ�� expression");
		}
		GroovyClassLoader loader = new GroovyClassLoader(
				GroovyListRuleEngine.class.getClassLoader());
		String groovyRule = getGroovyRule(expression,extraPackagesStr);
		Class<?> c_groovy;
		try {
			c_groovy = loader.parseClass(groovyRule);
		} catch (CompilationFailedException e) {
			throw new IllegalArgumentException(groovyRule, e);
		}

		try {
			// �½���ʵ��
			ruleObj = c_groovy.newInstance();
			// ��ȡ����
			m_routingRuleMap = getMethod(c_groovy, "eval", Map.class,
					ExtraParameterContext.class);
			if (m_routingRuleMap == null) {
				throw new IllegalArgumentException("���򷽷�û�ҵ�");
			}
			m_routingRuleMap.setAccessible(true);

		} catch (Throwable t) {
			throw new IllegalArgumentException("ʵ�����������ʧ��", t);
		}
	}

	private static final Pattern RETURN_WHOLE_WORD_PATTERN = Pattern.compile(
			"\\breturn\\b", Pattern.CASE_INSENSITIVE);// ȫ��ƥ��
	private static final Pattern DOLLER_PATTERN = Pattern.compile("#.*?#");
    
	protected String getGroovyRule(String expression){
		return this.getGroovyRule(expression, null);
	}
	
	/**
	 * ex:Integer.valueOf(#userIdStr#.substring(0,1),16).intdiv(8)
	 */
	protected String getGroovyRule(String expression,String extraPackagesStr) {
		StringBuffer sb = new StringBuffer();
		//����û��Զ���package,�Ѿ�����
		if(extraPackagesStr!=null){
		    sb.append(extraPackagesStr);
		}
		sb.append(IMPORT_STATIC_METHOD);
		sb.append(IMPORT_EXTRA_PARAMETER_CONTEXT);
		Set<AdvancedParameter> params = new HashSet<AdvancedParameter>();
		Matcher matcher = DOLLER_PATTERN.matcher(expression);
		sb.append("public class RULE ").append("{");
		sb.append("public Object eval(Map map,ExtraParameterContext extraParameterContext){");

		// �滻����װadvancedParameter
		int start = 0;

		Matcher returnMarcher = RETURN_WHOLE_WORD_PATTERN.matcher(expression);
		if (!returnMarcher.find()) {
			sb.append("return ");
		}

		while (matcher.find(start)) {
			String realParam = matcher.group();
			realParam = realParam.substring(1, realParam.length() - 1);
			AdvancedParameter advancedParameter = AdvancedParameter.getAdvancedParamByParamTokenNew(realParam,false);
			//modify by junyu
			if (isNeedAdd(params, advancedParameter)) {
				params.add(advancedParameter);
			}
			sb.append(expression.substring(start, matcher.start()));
			sb.append("(map.get(\"");
			// �滻��(map.get("key"));
			sb.append(advancedParameter.key);
			sb.append("\"))");

			start = matcher.end();
		}
		// ������Ҫ�õ��Ĳ���
		setAdvancedParameter(params);
		sb.append(expression.substring(start));
		sb.append(";");
		sb.append("};");
		sb.append("}");
		logger.debug(sb.toString());
		return sb.toString();
	}

	public ResultAndMappingKey evalueateSamplingField(
			SamplingField samplingField) {
		List<String> columns = samplingField.getColumns();
		List<Object> values = samplingField.getEnumFields();

		int size = columns.size();
		Map<String, Object> argumentMap = new HashMap<String, Object>(size);
		for (int i = 0; i < size; i++) {
			argumentMap.put(columns.get(i), values.get(i));
		}
		// ����Ӧ���Զ����ֶ�
		if (this.context != null) {
			for (Map.Entry<String, Object> entry : this.context.entrySet()) {
				argumentMap.put(entry.getKey(), entry.getValue());
			}
		}

		Object[] args = new Object[] { argumentMap };
		String result = imvokeMethod(args);
		if (result != null) {
			return new ResultAndMappingKey(result);
		} else {
			throw new IllegalArgumentException("��������Ľ������Ϊnull");
		}
	}

	public ResultAndMappingKey evalueateSamplingField(
			SamplingField samplingField,
			ExtraParameterContext extraParameterContext) {
		List<String> columns = samplingField.getColumns();
		List<Object> values = samplingField.getEnumFields();

		int size = columns.size();
		Map<String, Object> argumentMap = new HashMap<String, Object>(size);
		for (int i = 0; i < size; i++) {
			argumentMap.put(columns.get(i), values.get(i));
		}
		// ����Ӧ���Զ����ֶ�
		if (this.context != null) {
			for (Map.Entry<String, Object> entry : this.context.entrySet()) {
				argumentMap.put(entry.getKey(), entry.getValue());
			}
		}

		Object[] args = new Object[] { argumentMap, extraParameterContext };
		String result = imvokeMethod(args);
		if (result != null) {
			return new ResultAndMappingKey(result);
		} else {
			throw new IllegalArgumentException("��������Ľ������Ϊnull");
		}
	}
	
	/**
	 * ��Ե�column�͵���value
	 * @param column
	 * @param value
	 * @param extraParameterContext
	 * @return
	 */
	public ResultAndMappingKey evalueateSimpleColumAndValue(
			String column,Object value,
			ExtraParameterContext extraParameterContext) {
		Map<String, Object> argumentMap = new HashMap<String, Object>(1);
		argumentMap.put(column, value);
		// ����Ӧ���Զ����ֶ�
		if (this.context != null) {
			for (Map.Entry<String, Object> entry : this.context.entrySet()) {
				argumentMap.put(entry.getKey(), entry.getValue());
			}
		}

		Object[] args = new Object[] { argumentMap, extraParameterContext };
		String result = imvokeMethod(args);
		if (result != null) {
			return new ResultAndMappingKey(result);
		} else {
			throw new IllegalArgumentException("��������Ľ������Ϊnull");
		}
	}

	/**
	 * ����Ŀ�귽��
	 * 
	 * @param args
	 * @return
	 */
	public String imvokeMethod(Object[] args) {
		Object value = invoke(ruleObj, m_routingRuleMap, args);
		String retString = null;
		if (value == null) {
			return null;
		} else {
			retString = String.valueOf(value);
			return retString;
		}
	}

	private static Method getMethod(Class<?> c, String name,
			Class<?>... parameterTypes) {
		try {
			return c.getMethod(name, parameterTypes);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("ʵ�����������ʧ��", e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("û���������" + name, e);
		}
	}

	private static Object invoke(Object obj, Method m, Object... args) {
		try {
			return m.invoke(obj, args);
		} catch (Throwable t) {
			// logger.warn("���÷�����" + m + "ʧ��", t);
			// return null;
			throw new IllegalArgumentException("���÷���ʧ��: " + m + t.getCause(), t);
		}
	}

	/**
	 * ���һ�������������ظ�����parameter,��ô�Ե�һ��Ϊ׼
	 * �˴�û�н���paramSet�Ƿ�Ϊnull�ļ�飬���ⲿ���ϡ�
	 * 
	 * @param paramSet
	 * @param param
	 * @return
	 */
	private boolean isNeedAdd(Set<AdvancedParameter> paramSet,
			AdvancedParameter param) {
		for (AdvancedParameter ap : paramSet) {
			if (param.key.equals(ap.key)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "GroovyListRuleEngine [expression=" + expression
				+ ", parameters=" + parameters + "]";
	}

}
