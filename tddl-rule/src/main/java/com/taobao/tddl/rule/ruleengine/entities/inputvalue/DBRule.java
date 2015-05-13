package com.taobao.tddl.rule.ruleengine.entities.inputvalue;

import java.util.HashMap;
import java.util.Map;


/**
 * ��Ӧxml�����DBRule
 * 
 * @author shenxun
 * 
 */
public class DBRule {

	private Map<String, Integer> posiMap = new HashMap<String, Integer>();
	// ��Ϊ���Խ���sql�ˣ���������� �����������־�ֿ�Ĺؼ��ֶ���ʲô��

	/**
	 * �ֿ����
	 */
	private String parameters="";
	/**
	 * ��Ӧ���ʽ
	 */
	private String expression="";

	/**
	 * ����idΪ��
	 */
	private Map<String, Integer> primaryPosiMap = new HashMap<String, Integer>();
	private String primaryKey="";
	/**
	 * �����ı��ʽ
	 */
	private String primaryKeyExp="";

	/**
	 * д��
	 */
	private String[] writePool;
	/**
	 * ����
	 */
	private String[] readPool;
	/**
	 * �ӱ�������������ӱ���������ô�����globe�����������������DBRule��
	 */

	private TabRule DBSubTabRule;

	public Map<String, Integer> getPosiMap() {
		return posiMap;
	}

	public TabRule getDBSubTabRule() {
		return DBSubTabRule;
	}

	public void setDBSubTabRule(TabRule subTabRule) {
		DBSubTabRule = subTabRule;
	}

	public String getExpression() {
		return expression;
	}

	/**
	 * 
	 * @param expression
	 * @throws ParseException
	 */
	public void setExpression(String expression) {
		if(expression!=null){
			this.expression = expression.toLowerCase();
		}

	}


	public void setParameters(String parameters) {
		if (parameters != null) {
			this.parameters = parameters.toLowerCase();
			String[] paramsTokens = this.parameters.split(",");
			int i = 0;
			for (String str : paramsTokens) {
				posiMap.put(str, i);
				i++;
			}
		} 

	}

	public String getParameters() {
		return parameters;
	}

	public String[] getWritePool() {
		return writePool;
	}

	public void setWritePool(String[] writePool) {
		this.writePool = writePool;
	}

	public String[] getReadPool() {
		return readPool;
	}

	public void setReadPool(String[] readPool) {
		this.readPool = readPool;
	}

	public String getPrimaryKeyExp() {
		return primaryKeyExp;
	}

	public void setPrimaryKeyExp(String primaryKeyExp) {
		if(primaryKeyExp!=null){
			this.primaryKeyExp = primaryKeyExp.toLowerCase();
		}
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(String primaryKey) {
		if (primaryKey != null) {
			if (primaryKey.contains(",")) {
				throw new IllegalArgumentException("������������������");
			}
			this.primaryKey = primaryKey.toLowerCase();
			if (this.primaryKey != null) {
				String[] paramsTokens = this.primaryKey.split(",");
				int i = 0;
				for (String str : paramsTokens) {
					primaryPosiMap.put(str, i);
					i++;
				}
			}
		}
	}

	public Map<String, Integer> getPrimaryPosiMap() {
		return primaryPosiMap;
	}

}

