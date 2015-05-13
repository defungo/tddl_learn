package com.taobao.tddl.interact.rule.ruleimpl;

import groovy.lang.GroovyClassLoader;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;

import com.taobao.tddl.interact.rule.ShardingFunction;

public class GroovyRule<T> extends EnumerativeRule<T> {
	private static final Log logger = LogFactory.getLog(GroovyRule.class);
	// 应用置入的上下文，可以用在evel的groovy脚本里
	private static final String IMPORT_EXTRA_PARAMETER_CONTEXT = "import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;";
	private static final String IMPORT_STATIC_METHOD = "import static com.taobao.tddl.interact.rule.groovy.GroovyStaticMethod.*;";
	private static final Pattern RETURN_WHOLE_WORD_PATTERN = Pattern.compile("\\breturn\\b", Pattern.CASE_INSENSITIVE);// 全字匹配

	private String extraPackagesStr;
	private ShardingFunction shardingFunction ;

	public GroovyRule(String expression) {
		this(expression,null);
	}
	
	public GroovyRule(String expression,String extraPackagesStr) {
		super(expression);
		if(extraPackagesStr==null){
			this.extraPackagesStr="";
		}else{
			this.extraPackagesStr=extraPackagesStr;
		}
		initGroovy();
	}

	private void initGroovy() {
		if (expression == null) {
			throw new IllegalArgumentException("未指定 expression");
		}
		GroovyClassLoader loader = new GroovyClassLoader(GroovyRule.class.getClassLoader());
		String groovyRule = getGroovyRule(expression,extraPackagesStr);
		Class<?> c_groovy;
		try {
			c_groovy = loader.parseClass(groovyRule);
		} catch (CompilationFailedException e) {
			throw new IllegalArgumentException(groovyRule, e);
		}

		try {
			// 新建类实例
			Object ruleObj = c_groovy.newInstance();
			if(ruleObj instanceof ShardingFunction)
			{
				shardingFunction = (ShardingFunction) ruleObj;
			}
			else
			{
				throw new IllegalArgumentException("should not be here");
			}
			// 获取方法
			
		} catch (Throwable t) {
			throw new IllegalArgumentException("实例化规则对象失败", t);
		}
	}

	protected static String getGroovyRule(String expression,String extraPackagesStr) {
		StringBuffer sb = new StringBuffer();
		sb.append(extraPackagesStr);
		sb.append(IMPORT_STATIC_METHOD);
		sb.append(IMPORT_EXTRA_PARAMETER_CONTEXT);
		sb.append("public class RULE implements com.taobao.tddl.interact.rule.ShardingFunction").append("{");
		sb.append("public Object eval(Map map, Object outerCtx) {");
		Matcher returnMarcher = RETURN_WHOLE_WORD_PATTERN.matcher(expression);
		if (!returnMarcher.find()) {
			sb.append("return ");
			sb.append(expression);
			sb.append("+\"\";};}");
		}else{
			sb.append(expression);
			sb.append(";};}");
		}
		logger.debug(sb.toString());
		return sb.toString();
	}

	/**
	 * 替换成(map.get("name"));以在运算时通过列名取得参数值（描点值）
	 */
	@Override
	protected String replace(com.taobao.tddl.interact.rule.Rule.RuleColumn ruleColumn) {
		return new StringBuilder("(map.get(\"").append(ruleColumn.key).append("\"))").toString();
	}

	/**
	 * 调用groovy的方法：public Object eval(Map map,Map ctx){...}");
	 */
	@SuppressWarnings("unchecked")
	public T eval(Map<String, Object> columnValues, Object outerCtx) {
		try {
			T value = (T) shardingFunction.eval(columnValues, outerCtx);
			if (value == null) {
				throw new IllegalArgumentException("rule eval resulte is null! rule:" + this.expression);
			}
			return value;
		} catch (Throwable t) {
			throw new IllegalArgumentException("调用方法失败: " + expression, t);
		}
	}


	@Override
	public String toString() {
		return new StringBuilder("GroovyRule{expression=").append(expression).append(", parameters=")
				.append(parameters).append("}").toString();
	}
}
