package com.taobao.tddl.jdbc.druid;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.jdbc.druid.common.DruidConstants;
import com.taobao.tddl.jdbc.druid.config.object.DruidDbStatusEnum;
import com.taobao.tddl.jdbc.druid.config.object.DruidDbTypeEnum;
import com.taobao.tddl.jdbc.druid.exception.DruidAlreadyInitException;
import com.taobao.tddl.jdbc.druid.listener.DruidDbStatusListener;

/**
 * ��̬����Դ��֧������Դ������̬�޸�
 * 
 * @author qihao
 * 
 */
public class TDruidDataSource extends AbstractTDruidDataSource {

	protected static Log logger = LogFactory.getLog(TDruidDataSource.class);

	private static Map<String, DruidDsConfHandle> cacheConfHandleMap = new HashMap<String, DruidDsConfHandle>();

	private volatile DruidDsConfHandle dsConfHandle = new DruidDsConfHandle();

	public void init() throws Exception {
		String dbName = DruidConstants.getDbNameStr(this.getAppName(), this.getDbKey());
		synchronized (cacheConfHandleMap) {
			DruidDsConfHandle cacheConfHandle = cacheConfHandleMap.get(dbName);
			if (null == cacheConfHandle) {
				//��ʼ��config�Ĺ�����
				this.dsConfHandle.init();
				cacheConfHandleMap.put(dbName, dsConfHandle);
				logger.info("create new TAtomDsConfHandle dbName : " + dbName);
			} else {
				dsConfHandle = cacheConfHandle;
				logger.info("use the cache TAtomDsConfHandle dbName : " + dbName);
			}
		}
	}

	/**
	 * �������������Դ
	 */
	public static void cleanAllDataSource() {
		synchronized (cacheConfHandleMap) {
			for (DruidDsConfHandle handles : cacheConfHandleMap.values()) {
				try {
					handles.destroyDataSource();
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			cacheConfHandleMap.clear();
		}
	}

	/**
	 * ˢ������Դ
	 */
	public void flushDataSource() {
		this.dsConfHandle.flushDataSource();
	}

	/**��������Դ������
	 * @throws Exception 
	 */
	public void destroyDataSource() throws Exception {
		String dbName = DruidConstants.getDbNameStr(this.getAppName(), this.getDbKey());
		synchronized (cacheConfHandleMap) {
			this.dsConfHandle.destroyDataSource();
			cacheConfHandleMap.remove(dbName);
		}
	}

	public String getAppName() {
		return this.dsConfHandle.getAppName();
	}

	public String getDbKey() {
		return this.dsConfHandle.getDbKey();
	}

	public void setAppName(String appName) throws DruidAlreadyInitException {
		this.dsConfHandle.setAppName(StringUtil.trim(appName));
	}

	public void setDbKey(String dbKey) throws DruidAlreadyInitException {
		this.dsConfHandle.setDbKey(StringUtil.trim(dbKey));
	}

	public DruidDbStatusEnum getDbStatus() {
		return this.dsConfHandle.getStatus();
	}

	public void setDbStatusListeners(List<DruidDbStatusListener> dbStatusListeners) {
		this.dsConfHandle.setDbStatusListeners(dbStatusListeners);
	}

	public void setSingleInGroup(boolean isSingleInGroup) {
		this.dsConfHandle.setSingleInGroup(isSingleInGroup);
	}

	/**=======���������ñ������ȵ��������ԣ���������˻�������͵����ö�ʹ�ñ��ص�����=======*/
	public void setPasswd(String passwd) throws DruidAlreadyInitException {
		this.dsConfHandle.setLocalPasswd(passwd);
	}

	public void setDriverClass(String driverClass) throws DruidAlreadyInitException {
		this.dsConfHandle.setLocalDriverClass(driverClass);
	}

	public DruidDbTypeEnum getDbType() {
		return this.dsConfHandle.getDbType();
	}

	public void setSorterClass(String sorterClass) throws DruidAlreadyInitException {
		this.dsConfHandle.setLocalSorterClass(sorterClass);
	}

	public void setConnectionProperties(Map<String, String> map) throws DruidAlreadyInitException {
		this.dsConfHandle.setLocalConnectionProperties(map);
	}

	protected DataSource getDataSource() throws SQLException {
		return this.dsConfHandle.getDataSource();
	}
}
