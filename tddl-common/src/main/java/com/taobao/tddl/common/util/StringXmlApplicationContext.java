package com.taobao.tddl.common.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * �ַ�����ʽ��Spring ApplicationContextʵ�֡�
 * ֧�ֶ�̬����spring���õĴ���
 * 
 * @author linxuan
 *
 */
public class StringXmlApplicationContext extends AbstractXmlApplicationContext {
	private Resource[] configResources;

	public StringXmlApplicationContext(String stringXml) {
		this(new String[] { stringXml }, null);
	}

	public StringXmlApplicationContext(String[] stringXmls) {
		this(stringXmls, null);
	}

	public StringXmlApplicationContext(String[] stringXmls, ApplicationContext parent) {
		super(parent);
		this.configResources = new Resource[stringXmls.length];
		for (int i = 0; i < stringXmls.length; i++) {
			this.configResources[i] = new ByteArrayResource(stringXmls[i].getBytes());
		}
		refresh();
	}

	protected Resource[] getConfigResources() {
		return this.configResources;
	}
}
