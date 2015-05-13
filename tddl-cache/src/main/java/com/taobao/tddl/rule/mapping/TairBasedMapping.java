package com.taobao.tddl.rule.mapping;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.common.tair.DataEntry;
import com.taobao.common.tair.Result;
import com.taobao.common.tair.ResultCode;
import com.taobao.common.tair.TairManager;
import com.taobao.common.tair.impl.DefaultTairManager;
import com.taobao.tddl.common.Monitor;


/**
 * tairӳ�����
 * ��database����Ļ����ϡ�
 * ��Ϊtair���غ���Ҫ��������޸ġ�
 * 
 * ��Ϊ�����ص㣺
 * 1.tair Ҫ��ʹ�ü򵥶�����˲�����ʹ��java bean������������ݡ���ֻ����ʹ�û��������String��
 * 2.Ϊ�˼�С�����ԣ����Ա�֤���ݴ���tair�����Զ���޸ġ�
 * ����Ϊ��ʵ��Ӧ������key->(value1,value2��,������ӳ���ϵ������������˵
 * value1��value2���п�����null,��˱��뱣֤��value1,value2����Ϊ�յ�����£���дtair.
 * 3.����tair���������� value1|value2|...��String���󡣵������ֻ��һ��valueҪ����tair,��ô������String���а�װ
 * 4.����ָ����index ��ȡvalue1String,Ȼ�����Ԥ���趨�õ�typeHandler����typeת������
 * 5.columns�������� col1|int,col2|long
 *  
 * @author shenxun
 *
 */
public class TairBasedMapping extends DatabaseBasedMapping{
	private final Log log = LogFactory.getLog(TairBasedMapping.class);
	private static final int DEFAULT_VALUE = -1;
	/**
	 * �ڲ���ʱʹ�õ�null holder,Ϊ�˷�ֹ�����ݿ��в������value��ĺܲ��ҵ���"null"����ֶΡ�
	 * ������ȥΪnull�Ͳ����ˡ���
	 */
	private static final String NULL_PLACEHOLDER = "TDDL_NULL_PLACE_HOLDER";
	public static final int DEFAULT_NAMESPACE = -1000;
	
	/*for tair */
	private List<String> tairConfigServers = new ArrayList<String>(2);
	private String groupName;
	private String charSet;
	private int compressionThreshold = DEFAULT_VALUE;
	private int maxWaitThread = DEFAULT_VALUE;
	private int timeout = DEFAULT_VALUE;
	private TairManager tairManager;
	
	private int namespace = DEFAULT_NAMESPACE;
	
//	 Map<String/*target key*/, TypeHandlerEntry> typeHandlerMap;

	@Override
	public void initInternal() {
			if(this.tairManager == null){
				if(namespace == DEFAULT_NAMESPACE){
					throw new IllegalArgumentException("δָ��namespace");
				}
				log.warn("tddl init tair manager , tairConfigServers is "+ tairConfigServers
						+" group name is "+groupName);
				DefaultTairManager tairManager = new DefaultTairManager();
				tairManager.setConfigServerList(tairConfigServers);
				tairManager.setGroupName(groupName);
				if(charSet != null)
					tairManager.setCharset(charSet);
				if(compressionThreshold != DEFAULT_VALUE)
					tairManager.setCompressionThreshold(compressionThreshold);
				if(maxWaitThread != DEFAULT_VALUE)
					tairManager.setMaxWaitThread(maxWaitThread);
				if(timeout != DEFAULT_VALUE)
					tairManager.setTimeout(timeout);
				
				tairManager.init();
				log.warn("inited");
				this.tairManager = tairManager;
			}

		super.initInternal();
	}
	
	
	/**
	 * ����Ϊtair׼����һ���ַ��������ȡ����������null����hasNullValueΪtrue;
	 * �� map : key1 -> val1 ,key2 -> null;
	 * ��ôhasNullValue = true,tokenForTair = val1
	 * @author shenxun
	 *
	 */
	static class TokenForTairResult{
		/**
		 * �Ƿ��п�ֵ
		 */
		boolean hasNullVale;
		/**
		 * �����ǻ������ͻ�String,����������ͻ�Stringƴװ���ֶ���û��nullֵ
		 * ���д�뵽tair�У�����ֻ�����ں���ӳ�䴦��
		 */
		Serializable tokenForTair;
	}
	
	protected Object get(String targetKey, String sourceKey, Object sourceValue) {
		
		Object cacheres = getTargetValueFromTair(sourceValue);
		
		if(cacheres == null){
			Monitor.add(Monitor.KEY1, sourceKey, Monitor.KEY3_TAIR_HIT_RATING,0,1);
			log.debug("tair doesn't have spec value,get from database;");
			//���ûȡ�����ݣ���ô�����ݿ���ȡ����
			Map<String, Object>  map = getResultMap(sourceKey, sourceValue,targetKey);
			log.debug("value from database is :"+map);
			TokenForTairResult tokenForTair = getValueForTairCache(map);
			putIntoTairCache(sourceValue, tokenForTair);
			cacheres = tokenForTair.tokenForTair;
		}else{
			Monitor.add(Monitor.KEY1, sourceKey, Monitor.KEY3_TAIR_HIT_RATING,1,1);
		}
		return translate(targetKey, cacheres);
	}

	/**
	 * ��tair��ȡ�������ݣ���targetKey��ȡ��Ӧ��index�����ݣ�Ȼ��ת��
	 * ����������
	 * @param targetKey
	 * @param cacheres
	 * @return
	 */
	Object translate(String targetKey, Object cacheres) {
		if(cacheres == null){
			//�������У�ֱ�Ӵ��������ʱ��������������null��ֱ�ӷ���null
			return null;
		}
		if(!(cacheres instanceof String)){
			//�����������
			return cacheres;
		}
		//����Ϊnull������е����������ƴװ�ֶκ��123|TDDL_NULL_PLACE_HOLDER
		String str = String.valueOf(cacheres);
		String[] targetValues = str.split("\\|");
		if(targetValues.length != typeHandlerMap.size()){
			log.error("source values do not equal type Handler map size ." +
					",target value in tair is " + str+" map size is" + typeHandlerMap.size());
			return null;
		}
		TypeHandlerEntry typeHandlerEntry = typeHandlerMap.get(targetKey);
		if(typeHandlerEntry == null){
			log.error("cant find type handler by targetKey :" + targetKey+" .type handler map is"+typeHandlerMap);
			return null;
		}
		String targetValue = targetValues[typeHandlerEntry.index];
		if(NULL_PLACEHOLDER.equals(targetValue)){
			return null;
		}else{
			return typeHandlerEntry.typeHandler.process(targetValue);
		}
	}
	
	private void putIntoTairCache(Object sourceValue, TokenForTairResult tokenForTair) {
		ResultCode rs=null;
		try {
			if(!tokenForTair.hasNullVale){
				rs=this.tairManager.put(namespace, sourceValue, tokenForTair.tokenForTair);
				if(!rs.isSuccess()){
					log.error("put key "+sourceValue.toString()+" "+rs.getMessage());
				}
			}else{
				log.info("null value was detected, give up to write to tair cache" +
						", It will not be a  problem until sourceValue repeat to many times ,current sourceValue is"+sourceValue+" token for tair is "+tokenForTair.tokenForTair);
			}
		} catch (Exception e) {
			log.error(sourceValue.toString(), e);
		}
	}
	
	/**
	 * ��ȡ���ݿ��е�������tair���ŵ���ʽ
	 * 
	 * val1|val2|val3
	 * 
	 * @param map
	 * @return
	 * @throws InterruptedException
	 */
	TokenForTairResult getValueForTairCache(Map<String, Object> map){
		TokenForTairResult tokenForTairResult = new TokenForTairResult();
	
	
		if(columns.length > 1){
			StringBuilder sb = new StringBuilder();
			boolean firstElement = true;
			for(String column: super.columns){
				if(firstElement){
					firstElement = false;
				}else{
					sb.append("|");
				}
				
				if(appendColumnStr(map, sb, column)){
					tokenForTairResult.hasNullVale = true;
				}
			}
			
			tokenForTairResult.tokenForTair  = sb.toString();
		}else if(columns.length == 1){
			/*
			 * ����columns == 1 .<1�Ĳ����ܳ��֣�columns.length������С��0������0��initRule���쳣
			 * columns =1ʱ����ǿתΪString����ֱ��ʹ��ԭ�е�Serializable������
			 */
			String column  = columns[0];
			Object value = map.get(column);
			if(value == null){
				tokenForTairResult.hasNullVale = true;
			}else{
				try {
					tokenForTairResult.tokenForTair  = (Serializable) value;
				} catch (ClassCastException e) {
					log.error("target column "+column + " , value is "+value);
				}
			}
			
		}else{
			//������ܵ�û�뵽�����
			throw new IllegalStateException("should not be here");
		}
	
		return tokenForTairResult;
	}
	
	boolean appendColumnStr(Map<String, Object> map, StringBuilder sb,
			String column) {
		String tempColumn = null;
		boolean hasNullValue = false;
		Object tempValue = map.get(column);
		if(tempValue == null){
			hasNullValue = true;
		}
		if(tempValue == null){
			tempColumn = NULL_PLACEHOLDER;
		}else{
			tempColumn = tempValue.toString();
		}
		sb.append(tempColumn);
		
		return hasNullValue;
	}
	 Object getTargetValueFromTair(Object sourceValue) {
		Object cacheres = null;
		try {

			Result<DataEntry> result = this.tairManager.get(namespace,sourceValue);

			if (result!=null&&result.isSuccess()) {

				
				DataEntry dataEntry = result.getValue();
				//��result!=nullʱ�������ܱ�֤dataEntry��Ϊnull,������Ҫ�ж�
				if(dataEntry!=null){
				 cacheres = dataEntry.getValue();						
				}
				log.debug("get from tair success ; cacheres is "+ cacheres);
			}else{
				log.error("get key" + sourceValue + (result == null ? "" : result.getRc().getMessage()));
			}

		} catch (Exception e) {
			log.error(sourceValue, e);
		}
		return cacheres;
	}
	public int getNamespace() {
		return namespace;
	}
	public void setNamespace(int namespace) {
		this.namespace = namespace;
	}

	public TairManager getTairManager() {
		return tairManager;
	}

	public void setTairManager(TairManager tairManager) {
		this.tairManager = tairManager;
	}

	public List<String> getTairConfigServers() {
		return tairConfigServers;
	}

	public void setTairConfigServers(List<String> tairConfigServers) {
		this.tairConfigServers = tairConfigServers;
	}

	public void setCommaTairConfigServers(String commaTairConfigServers) {
		this.tairConfigServers = Arrays.asList(commaTairConfigServers.split(","));
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	public int getCompressionThreshold() {
		return compressionThreshold;
	}

	public void setCompressionThreshold(int compressionThreshold) {
		this.compressionThreshold = compressionThreshold;
	}

	public int getMaxWaitThread() {
		return maxWaitThread;
	}

	public void setMaxWaitThread(int maxWaitThread) {
		this.maxWaitThread = maxWaitThread;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Map<String, TypeHandlerEntry> getTypeHandlerMap() {
		return typeHandlerMap;
	}

	public void setTypeHandlerMap(Map<String, TypeHandlerEntry> typeHandlerMap) {
		this.typeHandlerMap = typeHandlerMap;
	}
	
	
}
