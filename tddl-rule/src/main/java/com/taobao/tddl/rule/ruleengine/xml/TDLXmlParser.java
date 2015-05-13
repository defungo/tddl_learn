package com.taobao.tddl.rule.ruleengine.xml;


import static com.taobao.tddl.rule.ruleengine.util.StringUtils.trim;
import static com.taobao.tddl.rule.ruleengine.util.StringUtils.validAndTrim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.taobao.tddl.common.exception.runtime.CantfindConfigFileByPathException;
import com.taobao.tddl.common.exception.runtime.TDLRunTimeException;
import com.taobao.tddl.rule.ruleengine.TableRuleType;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.DBRule;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.LogicTabMatrix;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.TabRule;

public class TDLXmlParser {
	private static final Log log = LogFactory.getLog(TDLXmlParser.class);
	/**
	 * ���������path��ַ��ȡ��������dbrule��tableRule��Map. ֻ�ڳ�ʼ����ʱ�����һ��.
	 * 
	 * 
	 * @param path
	 *            ·����ϵͳ�ڲ��� getClass().getResourceAsStream(path);�ķ�ʽ��ȡ��Ӧ��stream��Դ
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized void initLogicTabMarticMap(String path,
			Map<String, LogicTabMatrix> map) {
			Document doc = getDocument(path);
			Element ele = doc.getRootElement();
			String dbType= trim(ele.attributeValue("dbType"));
			Iterator dbTabItr = ele.elementIterator("table");
			int i = 1;
			while (dbTabItr.hasNext()) {
				Element aVtab = (Element) dbTabItr.next();
				LogicTabMatrix aVtabMatrix = new LogicTabMatrix();
				// globalTableRule,������򴢴�����
				Element globeRule = aVtab.element("globalTableRule");
				if (globeRule != null) {
					TabRule globTabRule = this.getTabRule(globeRule);
					aVtabMatrix.setGlobalTableRule(globTabRule);
				}
				String logicName = validAndTrim(aVtab
						.attributeValue("logicName"), "�޷��ҵ���" + i
						+ "��������logicName");
				//martrix�����TableName�Ƿ�����ǰ�����ڱ����滻�ģ����Ҫ����ԭ��
				aVtabMatrix.setTableName(logicName);
				String needRowCopy = trim(aVtab.attributeValue("rowCopy"));
				if(needRowCopy != null&&!needRowCopy.equals("")){
					aVtabMatrix.setNeedRowCopy(Boolean.valueOf(needRowCopy));
				}
				String reverseOutput = trim(aVtab.attributeValue("reverseOutput"));
				if(reverseOutput!=null&&!reverseOutput.equals("")){
					aVtabMatrix.setAllowReverseOutput(Boolean.valueOf(reverseOutput));
				}
				aVtabMatrix.setDBType(dbType);
				String tabFactor = trim(aVtab.elementText("tableFactor"));
				aVtabMatrix.setTableFactor(tabFactor);
				Element dbRules = (Element) aVtab.element("dbRules");
				Iterator rulesItr = dbRules.elementIterator("dbRule");
				Map<String,DBRule> ruleMap = getRuleList(rulesItr,aVtabMatrix);
				//��һ��Rule������expression��ʱ�����depositedRule��allRule
				//���û��expressionString������ֻ����allRule��ѡ
				Map<String,DBRule> depositedRules = getDepositedRule(ruleMap);
				aVtabMatrix.setAllRules(ruleMap);
				aVtabMatrix.setDepositedRules(depositedRules);
				List<DBRule> defaultRuleList = getDefaultRuleList(aVtab,
						aVtabMatrix);
				aVtabMatrix.setDefaultRules(defaultRuleList);

			
				map.put(logicName.toLowerCase(), aVtabMatrix);
				i++;
		}
	}

	private Map<String,DBRule> getDepositedRule(Map<String,DBRule> ruleMap){
		Map<String,DBRule> retMap=new HashMap<String, DBRule>(ruleMap.size());
		for(Entry<String, DBRule> ent:ruleMap.entrySet()){
			if(!ent.getValue().getExpression().equals("")){
				if(ent.getValue().getParameters().equals("")){
					throw new IllegalArgumentException("��depositedRule�б�������parameters������");
				}
				retMap.put(ent.getKey(), ent.getValue());
			}
			if(!ent.getValue().getPrimaryKeyExp().equals("")){
				if(ent.getValue().getPrimaryKey().equals("")){
					throw new IllegalArgumentException("��depositedRule�б�������primary key������");
				}
				retMap.put(ent.getKey(), ent.getValue());
			}
		}
		return retMap;
	}
	/**
	 * ����DefaultPool�ֶΣ������á�,���ָ�)����ȡDBRule�к���ָ��д���DBRule
	 * 
	 * @param aVtab
	 * @param aVtabMatrix
	 * @param tempSet
	 * @return
	 */
	private List<DBRule> getDefaultRuleList(Element aVtab,
			LogicTabMatrix aVtabMatrix) {
		List<DBRule> defaultList = new ArrayList<DBRule>();
		String defaultPools = trim(aVtab.attributeValue("defaultWritePool"));
		if (defaultPools != null) {
			String[] defaultPoolsStrArray = defaultPools.split(",");
			Map<String, DBRule> map=aVtabMatrix.getAllRules();
			for (String str : defaultPoolsStrArray) {
				DBRule dbrule=map.get(str.trim());
				if(dbrule!=null){
				defaultList.add(dbrule);
				}else{
					throw new IllegalArgumentException("defaultRule��idΪ:"+str+" ���ֶβ����ҵ�" +
							"һ����Ӧ�Ĺ�����ȷ�ϸ�id��Ӧһ��dbRule��id����");
				}
			}
		}
		return defaultList;
	}

	/**
	 * ����ÿһ��rule
	 * 
	 * @param rulesItr
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String,DBRule> getRuleList(Iterator rulesItr,LogicTabMatrix aVtabMatrix) {
		Map<String,DBRule> rules = new HashMap<String,DBRule>();
	
		while (rulesItr.hasNext()) {
			
			Element ruleEle = (Element) rulesItr.next();
			String id=validAndTrim(ruleEle.attributeValue("id"),
					"����ָ��Rule��id");
			DBRule rule = new DBRule();
			String exp=trim(ruleEle.elementText("expression"));
			rule.setExpression(exp);
			String primaryKey=trim(ruleEle.elementText("primaryKey"));
			rule.setPrimaryKey(primaryKey);
			String primaryKeyExp=trim(ruleEle.elementText("primaryKeyExp"));
			rule.setPrimaryKeyExp(primaryKeyExp);
			String parameters=trim(ruleEle.elementText("parameters"));
			rule.setParameters(parameters);
			String readPoolStr = ruleEle.elementText("readPools");
			String writePoolStr = ruleEle.elementText("writePools");
			if (readPoolStr == null || writePoolStr == null
					|| readPoolStr.trim().equals("")
					|| writePoolStr.trim().equals("")) {
				throw new TDLRunTimeException("readPool��writePool����ͬʱָ��"
						+ "readPool���Ժ�writePoolͬ������writePool����Ϊ�����Ҳ����Ϊ�����");
			}
			String[] readPools = readPoolStr.trim().split(",");
			String[] writePools = writePoolStr.trim().split(",");

			rule.setReadPool(readPools);
			rule.setWritePool(writePools);
			Element subTableRuleElement = ruleEle.element("tableRule");
			if (subTableRuleElement != null) {
				TabRule tabRule = getTabRule(subTableRuleElement);
				
				tabRule.setPrimaryKey(primaryKey);
				rule.setDBSubTabRule(tabRule);
				log.debug("id:"+id+"��DBRule��subTableRule,���ʹ��subTableRule");
			}
			//add by shenxun ���ڵ��߼���ֱ���滻subRuleEle,�Ժ󲻻���
			//ʹ��globalRule�����Ŀ�ˣ���ֻ�������ʱ����ʱ������
			else{
				TabRule tabRule=aVtabMatrix.getGlobalTableRule();
				if(tabRule!=null){
					tabRule.setPrimaryKey(primaryKey);
				}
				rule.setDBSubTabRule(tabRule);
				log.debug("id:"+id+"��DBRuleû��subTableRule,���ʹ��globalTableRule");
			}
			rules.put(id,rule);
		}
		return rules;
	}

	/**
	 * ��ȡ�����
	 * 
	 * @param ele
	 * @return
	 */
	private TabRule getTabRule(Element ele) {
		TabRule tabRule = new TabRule();
		String parameters = trim(ele.elementText("parameters"));
		tabRule.setParameter(parameters);
		String expression = trim(ele.elementText("expression"));
		tabRule.setExpFunction(expression);
		String type = trim(ele.elementText("type"));
		if (type != null && !type.equals("")) {
			tabRule.setTableType(TableRuleType.valueOf(type.toUpperCase()));
		}
		String padding=trim(ele.elementText("padding"));
		tabRule.setPadding(padding);
		String width=trim(ele.elementText("width"));
		tabRule.setWidth(width);
		tabRule.setDefaultTable(trim(ele.elementText("defaultTableRange")));
		String offset = ele.elementText("offset");
		if (offset != null && !offset.trim().equals("")) {
			tabRule.setOffset(Integer.valueOf(offset.trim()).intValue());
		}
		return tabRule;
	}

	/**
	 * ����·����ȡdom4j
	 * 
	 * @param path
	 * @return
	 */
	private Document getDocument(String path) {
		Document doc = null;
		InputStream in = null;
		if (path == null) {
			throw new CantfindConfigFileByPathException(null);
		}
		path = path.trim();
		if (path.equals("")) {
			throw new CantfindConfigFileByPathException(path);
		}
		try {
			in = TDLXmlParser.class.getResourceAsStream(path);
			if (in == null) {
				try {
					in=new FileInputStream(new File(path));
				} catch (FileNotFoundException e) {
					throw new TDLRunTimeException("ָ����mapping�ļ�����ȷ����ָ��·��Ϊ��" + path
							+ "" + "��ʹ��/filename,����path��class��Ŀ¼��ȥ����Դ,һ��ʹ���������"
							+ "��ʹ��filename,����������Ŀ¼ȥѰ����Դ,��������ҵ���Ҳ�᳢��ʹ��File�ķ�ʽ����",e);
			
				}
			}
			Reader read = new InputStreamReader(in);
			SAXReader reader = new SAXReader();
			doc = reader.read(read);
		} catch (DocumentException e) {
			throw new TDLRunTimeException("����ӳ���ļ�ʱ��������,����ӳ���ļ�", e);
		}
		return doc;
	}
}
