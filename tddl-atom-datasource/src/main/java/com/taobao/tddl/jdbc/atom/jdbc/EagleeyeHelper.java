package com.taobao.tddl.jdbc.atom.jdbc;

import java.lang.reflect.Field;

import com.taobao.eagleeye.EagleEye;
import com.taobao.tddl.jdbc.atom.config.object.TAtomDsConfDO;

/**
 * @author jiechen.qzm
 * Eagleeye�����࣬Э����¼��ѯʱ��
 */
public class EagleeyeHelper {

	/**
	 * ��ȡ����execute������Դ��Ϣ
	 * @param datasourceWrapper
	 * @param sqlType
	 * @throws Exception
	 */
	public static void startRpc(TAtomDsConfDO runTimeConf, String sqlType){
		String ip = runTimeConf.getIp();
		String port = runTimeConf.getPort();
		String dbName = runTimeConf.getDbName();
		String serviceName = "TDDL-" + dbName + "-" + ip + "-" + port;
		String methodName = sqlType.toString();
		EagleEye.startRpc(serviceName, methodName);
	}

	/**
	 * execute֮ǰд��־
	 */
	public static void annotateRpcBeforeExecute(){
		EagleEye.annotateRpc(EagleEye.TAG_CLIENT_SEND);
	}

	/**
	 * execute֮��д��־
	 */
	public static void annotateRpcAfterExecute(){
		EagleEye.annotateRpc(EagleEye.TAG_CLIENT_RECV);
	}
}
