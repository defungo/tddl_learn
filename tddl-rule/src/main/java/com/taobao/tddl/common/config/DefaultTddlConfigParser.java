package com.taobao.tddl.common.config;

/**
 * Ĭ�Ͻ����ı�������
 * 
 * @author linxuan
 *
 */
public class DefaultTddlConfigParser<T> implements TddlConfigParser<T> {
	private static final String prefix_spring = "<!-- tddlconfig -->";
	private static final String prefix_properties = "#tddlconfig";

	private SpringXmlConfigParser<T> springParser = new SpringXmlConfigParser<T>();
	private PropertiesConfigParser<T> propertiesParser = new PropertiesConfigParser<T>();
	public T parseCongfig(String txt) {
		if (txt.startsWith(prefix_spring)){
			//return springParser.parseCongfig(txt.substring(prefix_spring.length()));
			return springParser.parseCongfig(txt); //����Ҫȥ��ͷ��
		}else if(txt.startsWith(prefix_properties)){
			//return propertiesParser.parseCongfig(txt.substring(prefix_properties.length()));
			return propertiesParser.parseCongfig(txt); //����Ҫȥ��ͷ��
		}else{
			//����ǰ׺Ĭ��Ϊspring xml��ʽ������ֱ�ӽ������ı��������ļ��У���spring IDE���
			return springParser.parseCongfig(txt);
		}
	}
}
