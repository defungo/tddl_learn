package com.taobao.tddl.rule.ruleengine.entities.abstractentities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * һ�Զ�ڵ����
 * 
 * ����һ��һ�Զ��ӳ��map,���������ı��ڵ������еĹ�������
 * �Լ���������������Щ�ڵ��Ƿ���Ҫ��Ȼ����㲢����
 * 
 * @author shenxun
 *
 */
public abstract class ListSharedElement extends SharedElement {
	
	Log log = LogFactory.getLog(ListSharedElement.class);
	
	public enum DEFAULT_LIST_RESULT_STRAGETY {
		/**
		 * Ĭ��ȫ�Ӽ���ѯ
		 */
		FULL_TABLE_SCAN,

		/**
		 * Ĭ��һ������ѡ��
		 */
		NONE
	}

	/**
	 * Ĭ���ӽڵ��ѡ��ʽ��ȫ��
	 */
	public DEFAULT_LIST_RESULT_STRAGETY defaultListResultStragety;

	/**
	 * ���û���ҵ�ָ����ListResult��ʱ��Ӧ��Ӧ�÷��صĽڵ����Ϣ
	 */
	protected List<String> defaultListResult;

	/**
	 * ���Ϊһ��list ����ļ���ʽ����һ�����������Ե͵��ߣ��������ȼ����ν���
	 */
	//TODO:������
	protected RuleChain listResultRule;

	/**
	 * �ӽڵ�Map
	 * 		���ֱ����map��ʽָ����key����init�����б����õ��ӽڵ���Ϊ�ӽڵ��id
	 * 
	 * 		�����list��ʽָ�������±���Ϊ���map��key,����init�����б����õ��ӽڵ���Ϊid.
	 */
	protected Map<String, ? extends SharedElement> subSharedElement = Collections.emptyMap();
	


	/**
	 * ��LogicMapע���Լ��ĺ�����
	 * 
	 * ��ƽ��������������LogicTable����,������matcher�оͿ���֪�����й�������Ҫ�Ĳ����Ƿ񶼾߱��ˡ�
	 * 
	 * ��νmatcher���Ǹ��ݹ�������Ĺ�������Ҫ�Ĳ�����ȥsql����Ѱ����Ҫ�������ƥ�������+������
	 *
	 * ����һ��context���ڼ���ʱ���õ�
	 * 
	 * @param ruleSet
	 */
	public void registeRule(Set<RuleChain> ruleSet){
		
		registeSubSharedElement(ruleSet);
		
		registeCurrentSharedElement(ruleSet);
	}


	/** ע�ᵱǰ�ڵ�
	 * @param ruleSet
	 */
	private void registeCurrentSharedElement(Set<RuleChain> ruleSet) {
		if(this.listResultRule!= null){
			ruleSet.add(this.listResultRule);
		}
	}


	/**
	 * ������ӽڵ���ע���ӽڵ�
	 * 
	 * @param ruleSet
	 */
	private void registeSubSharedElement(Set<RuleChain> ruleSet) {
		for(SharedElement sharedElement:subSharedElement.values()){
			if(sharedElement instanceof ListSharedElement){
				((ListSharedElement) sharedElement).registeRule(ruleSet);
			}
		}
	}


	public void setSubSharedElement(
			Map<String, ? extends SharedElement> subSharedElement) {
		this.subSharedElement = subSharedElement;
	}



	/* (non-Javadoc)
	 * @see com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement#init()
	 * �����databaseMapProvider =>��ʼ��databaseMap
	 * 
	 */
	public void init() {
		init(true);
	}
	/**
	 * �����Է�������Ϊ��ǰTableRule��¶��init�������ⲿʹ�á�
	 * @param invokeBySpring
	 */
	public void init(boolean invokeBySpring){
		Map<String ,? extends SharedElement> subSharedElements = fillNullSubSharedElementWithEmptyList();

		initDefaultSubSharedElementsListRule();

		setChildIdByUsingMapKey(subSharedElements,invokeBySpring);
		
		super.init(invokeBySpring);
	}



	private void setChildIdByUsingMapKey(
			Map<String, ? extends SharedElement> subSharedElements,boolean invokeBySpring) {
		for(Entry<String, ? extends SharedElement> sharedElement:subSharedElements.entrySet()){
			//��map�е�key��Ϊid���ø��ӽڵ�
			sharedElement.getValue().setId(sharedElement.getKey());
			 initOneSubSharedElement(sharedElement.getValue(),invokeBySpring);
		}
	}


	protected Map<String, ? extends SharedElement> fillNullSubSharedElementWithEmptyList() {
		if (this.subSharedElement== null) {
			this.subSharedElement = Collections.emptyMap();
		}
		return subSharedElement;
	}
	protected void initOneSubSharedElement(SharedElement sharedElement,boolean invokeBySpring){
		sharedElement.init(invokeBySpring);
	}

	/**
	 * ��ʼ��Ĭ�Ϲ���key���б�
	 * 
	 * �ᰴ�ռȶ����Գ�ʼ��������������������в��ܻ��target �ӹ���Ļ�
	 * 
	 * �ͻ�ʹ��Ĭ�Ϲ���
	 * 
	 */
	protected void initDefaultSubSharedElementsListRule() {
		if (defaultListResultStragety == null) {
			log.debug("default stragety is null ,use none stragety .");
			defaultListResultStragety = DEFAULT_LIST_RESULT_STRAGETY.NONE;
		}
		
		switch (defaultListResultStragety) {
		case FULL_TABLE_SCAN:
			buildFullTableKeysList();
			break;
		case NONE:
			//fix by shen: ��Ҫע����ǿձ����͵�ʱ��Ӧ�÷���Ψһ�Ľڵ�����ǿսڵ㡣
			if(listResultRule == null||listResultRule.getListResultRule() == null||
					listResultRule.getListResultRule().isEmpty()){
				if(subSharedElement.size() == 1){
					log.warn("NONE stragety ,current element has only one SubElement,use " +
							"full table stragety! subElement is "+ subSharedElement);
					buildFullTableKeysList();
				}else{
					log.warn("NONE stragety ,current element has more than one SubElement,use empty" +
							" default stragety! subElement is "+ subSharedElement);
					defaultListResult = Collections.emptyList();
				}
				
			}else{
				defaultListResult = Collections.emptyList();
			}
			break;
		default:
			throw new IllegalArgumentException("���ܴ��������");
		}
	}


	protected void buildFullTableKeysList() {
		int subSharedElementSize = 0;
		if (subSharedElement == null) {
			subSharedElement = Collections.emptyMap();
		}
		subSharedElementSize = subSharedElement.size();
		defaultListResult = new ArrayList<String>(subSharedElementSize);
		log.debug("use full table stragety, default keys are :");
		StringBuilder sb = new StringBuilder();
		for (String key : subSharedElement.keySet()) {
			sb.append(key).append("|");
			defaultListResult.add(key);
		}
		log.debug(sb.toString());
	}


	public Map<String, ? extends SharedElement> getSubSharedElements() {
		return subSharedElement;
	}


	public DEFAULT_LIST_RESULT_STRAGETY getDefaultListResultStragety() {
		return defaultListResultStragety;
	}


	public void setDefaultListResultStragety(
			DEFAULT_LIST_RESULT_STRAGETY defaultListResultStragety) {
		this.defaultListResultStragety = defaultListResultStragety;
	}


	@Override
	public String toString() {
		return "ListSharedElement [defaultListResult=" + defaultListResult
				+ ", defaultListResultStragety=" + defaultListResultStragety
				+ ", listResultRule=" + listResultRule + ", subSharedElement="
				+ subSharedElement + "]";
	}

}
