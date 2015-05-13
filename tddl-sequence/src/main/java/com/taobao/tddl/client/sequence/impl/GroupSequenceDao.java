package com.taobao.tddl.client.sequence.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.client.sequence.SequenceDao;
import com.taobao.tddl.client.sequence.SequenceRange;
import com.taobao.tddl.client.sequence.exception.SequenceException;
import com.taobao.tddl.client.sequence.util.RandomSequence;
import com.taobao.tddl.client.util.DataSourceType;
import com.taobao.tddl.common.GroupDataSourceRouteHelper;
import com.taobao.tddl.jdbc.group.TGroupDataSource;

public class GroupSequenceDao implements SequenceDao {

	private static final Log log = LogFactory.getLog(GroupSequenceDao.class);

	private static final int MIN_STEP = 1;
	private static final int MAX_STEP = 100000;

	private static final int DEFAULT_INNER_STEP = 1000;

	private static final int DEFAULT_RETRY_TIMES = 2;

	private static final String DEFAULT_TABLE_NAME = "sequence";
	private static final String DEFAULT_NAME_COLUMN_NAME = "name";
	private static final String DEFAULT_VALUE_COLUMN_NAME = "value";
	private static final String DEFAULT_GMT_MODIFIED_COLUMN_NAME = "gmt_modified";

	private static final int DEFAULT_DSCOUNT = 2;// Ĭ��
	private static final Boolean DEFAULT_ADJUST = false;

	private static final long DELTA = 100000000L;
	// /**
	// * ����Դ����
	// */
	// private DataSourceMatrixCreator dataSourceMatrixCreator;

	/**
	 * Ӧ����
	 */
	private String appName;

	/**
	 * group����
	 */
	private List<String> dbGroupKeys;
	
	/**
	 * groupDsʹ�õ�����Դ����
	 */
	private DataSourceType dataSourceType = DataSourceType.TbDataSource;
	
	/**
	 * ����Դ
	 */
	private Map<String, DataSource> dataSourceMap;

	/**
	 * ����Ӧ����
	 */
	private boolean adjust = DEFAULT_ADJUST;
	/**
	 * ���Դ���
	 */
	private int retryTimes = DEFAULT_RETRY_TIMES;

	/**
	 * ����Դ����
	 */
	private int dscount = DEFAULT_DSCOUNT;

	/**
	 * �ڲ���
	 */
	private int innerStep = DEFAULT_INNER_STEP;

	/**
	 * �ⲽ��
	 */
	private int outStep = DEFAULT_INNER_STEP;

	/**
	 * �������ڵı���
	 */
	private String tableName = DEFAULT_TABLE_NAME;

	/**
	 * �洢�������Ƶ�����
	 */
	private String nameColumnName = DEFAULT_NAME_COLUMN_NAME;

	/**
	 * �洢����ֵ������
	 */
	private String valueColumnName = DEFAULT_VALUE_COLUMN_NAME;

	/**
	 * �洢����������ʱ�������
	 */
	private String gmtModifiedColumnName = DEFAULT_GMT_MODIFIED_COLUMN_NAME;

	private volatile String selectSql;
	private volatile String updateSql;
	private volatile String insertSql;

	/**
	 * ���Ի�
	 * 
	 * @throws SequenceException
	 */
	public void init() throws SequenceException {
		// ���Ӧ����Ϊ�գ�ֱ���׳�
		if (StringUtils.isEmpty(appName)) {
			SequenceException sequenceException = new SequenceException(
					"appName is Null ");
			log.error("û������appName", sequenceException);
			throw sequenceException;
		}
		if (dbGroupKeys == null || dbGroupKeys.size() == 0) {
			log.error("û������dbgroupKeys");
			throw new SequenceException("dbgroupKeysΪ�գ�");
		}

		dataSourceMap = new HashMap<String, DataSource>();
		for (String dbGroupKey : dbGroupKeys) {
			if (dbGroupKey.toUpperCase().endsWith("-OFF")) {
				continue;
			}
			TGroupDataSource tGroupDataSource = new TGroupDataSource(
					dbGroupKey, appName, dataSourceType);
			tGroupDataSource.init();
			dataSourceMap.put(dbGroupKey, tGroupDataSource);
		}
		if (dbGroupKeys.size() >= dscount) {
			dscount = dbGroupKeys.size();
		} else {
			for (int ii = dbGroupKeys.size(); ii < dscount; ii++) {
				dbGroupKeys.add(dscount + "-OFF");
			}
		}
		outStep = innerStep * dscount;// �����ⲽ��

		StringBuilder sb = new StringBuilder();
		sb.append("GroupSequenceDao��ʼ����ɣ�\r\n ");
		sb.append("appName:").append(appName).append("\r\n");
		sb.append("innerStep:").append(this.innerStep).append("\r\n");
		sb.append("dataSource:").append(dscount).append("��:");
		for (String str : dbGroupKeys) {
			sb.append("[").append(str).append("]��");
		}
		sb.append("\r\n");
		sb.append("adjust��").append(adjust).append("\r\n");
		sb.append("retryTimes:").append(retryTimes).append("\r\n");
		sb.append("tableName:").append(tableName).append("\r\n");
		sb.append("nameColumnName:").append(nameColumnName).append("\r\n");
		sb.append("valueColumnName:").append(valueColumnName).append("\r\n");
		sb.append("gmtModifiedColumnName:").append(gmtModifiedColumnName)
				.append("\r\n");
		log.info(sb.toString());
	}

	/**
	 * 
	 * @param index
	 *            gourp�ڵ���ţ���0��ʼ
	 * @param value
	 *            ��ǰȡ��ֵ
	 * @return
	 */
	private boolean check(int index, long value) {
		return (value % outStep) == (index * innerStep);
	}

	/**
	 * ��鲢����ĳ��sequence 1�����sequece�����ڣ�����ֵ������ʼ��ֵ 2������Ѿ����ڣ������ص�����������
	 * 3������Ѿ����ڣ������ص���
	 * 
	 * @throws SequenceException
	 */
	public void adjust(String name) throws SequenceException, SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		for (int i = 0; i < dbGroupKeys.size(); i++) {
			if (dbGroupKeys.get(i).toUpperCase().endsWith("-OFF"))// �Ѿ��ص���������
			{
				continue;
			}
			TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
					.get(dbGroupKeys.get(i));
			try {
				conn = tGroupDataSource.getConnection();
				stmt = conn.prepareStatement(getSelectSql());
				stmt.setString(1, name);
				GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
				rs = stmt.executeQuery();
				int item = 0;
				while (rs.next()) {
					item++;
					long val = rs.getLong(this.getValueColumnName());
					if (!check(i, val)) // �����ֵ
					{
						if (this.isAdjust()) {
							this.adjustUpdate(i, val, name);
						} else {
							log.error("���ݿ������õĳ�ֵ���������������ݿ⣬��������adjust����");
							throw new SequenceException(
									"���ݿ������õĳ�ֵ���������������ݿ⣬��������adjust����");
						}
					}
				}
				if (item == 0)// ������,����������¼
				{
					if (this.isAdjust()) {
						this.adjustInsert(i, name);
					} else {
						log.error("���ݿ���δ���ø�sequence���������ݿ��в���sequence��¼����������adjust����");
						throw new SequenceException(
								"���ݿ���δ���ø�sequence���������ݿ��в���sequence��¼����������adjust����");
					}
				}
			} catch (SQLException e) {// �̵�SQL�쳣�������������õĿ����
				log.error("��ֵУ�������Ӧ�����г���.", e);
				throw e;
			} finally {
				closeResultSet(rs);
				rs = null;
				closeStatement(stmt);
				stmt = null;
				closeConnection(conn);
				conn = null;

			}

		}
	}

	/**
	 * ����
	 * 
	 * @param index
	 * @param value
	 * @param name
	 * @throws SequenceException
	 * @throws SQLException
	 */
	private void adjustUpdate(int index, long value, String name)
			throws SequenceException, SQLException {
		long newValue = (value - value % outStep) + outStep + index * innerStep;// ���ó��µĵ���ֵ
		TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
				.get(dbGroupKeys.get(index));
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = tGroupDataSource.getConnection();
			stmt = conn.prepareStatement(getUpdateSql());
			stmt.setLong(1, newValue);
			stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			stmt.setString(3, name);
			stmt.setLong(4, value);
			GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
			int affectedRows = stmt.executeUpdate();
			if (affectedRows == 0) {
				throw new SequenceException(
						"faild to auto adjust init value at  " + name
								+ " update affectedRow =0");
			}
			log.info(dbGroupKeys.get(index) + "���³�ֵ�ɹ�!" + "sequence Name��"
					+ name + "���¹��̣�" + value + "-->" + newValue);
		} catch (SQLException e) { // �Ե�SQL�쳣����Sequence�쳣
			log.error(
					"����SQLException,���³�ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "���¹��̣�" + value + "-->" + newValue, e);
			throw new SequenceException(
					"����SQLException,���³�ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "���¹��̣�" + value + "-->" + newValue, e);
		} finally {
			closeStatement(stmt);
			stmt = null;
			closeConnection(conn);
			conn = null;
		}
	}

	/**
	 * ������ֵ
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws SequenceException
	 * @throws SQLException
	 */
	private void adjustInsert(int index, String name) throws SequenceException,
			SQLException {
		TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
				.get(dbGroupKeys.get(index));
		long newValue = index * innerStep;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = tGroupDataSource.getConnection();
			stmt = conn.prepareStatement(getInsertSql());
			stmt.setString(1, name);
			stmt.setLong(2, newValue);
			stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
			int affectedRows = stmt.executeUpdate();
			if (affectedRows == 0) {
				throw new SequenceException(
						"faild to auto adjust init value at  " + name
								+ " update affectedRow =0");
			}
			log.info(dbGroupKeys.get(index) + "   name:" + name + "�����ֵ:"
					+ name + "value:" + newValue);

		} catch (SQLException e) { // �Ե�SQL�쳣����sequence�쳣
			log.error(
					"����SQLException,�����ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "   value:" + newValue, e);
			throw new SequenceException(
					"����SQLException,�����ֵ����Ӧʧ�ܣ�dbGroupIndex:"
							+ dbGroupKeys.get(index) + "��sequence Name��" + name
							+ "   value:" + newValue, e);
		} finally {
			closeResultSet(rs);
			rs = null;
			closeStatement(stmt);
			stmt = null;
			closeConnection(conn);
			conn = null;
		}
	}

	private ConcurrentHashMap<Integer/* ds index */, AtomicInteger/* �ӹ����� */> excludedKeyCount = new ConcurrentHashMap<Integer, AtomicInteger>(
			dscount);
	//����Թ�������ָ�
    private int maxSkipCount=10;
    //ʹ���������ݿⱣ��
    private boolean useSlowProtect=false;
    //������ʱ��
    private int protectMilliseconds=50;
   
    private ExecutorService exec = Executors.newFixedThreadPool(1);
    
	public SequenceRange nextRange(final String name) throws SequenceException {
		if (name == null) {
			log.error("������Ϊ�գ�");
			throw new IllegalArgumentException("�������Ʋ���Ϊ��");
		}

		long oldValue;
		long newValue;

		boolean readSuccess;
		boolean writeSuccess;

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
        
		int[] randomIntSequence = RandomSequence.randomIntSequence(dscount);
		for (int i = 0; i < retryTimes; i++) {
			for (int j = 0; j < dscount; j++) {
				readSuccess = false;
				writeSuccess = false;
				int index = randomIntSequence[j];
				if (dbGroupKeys.get(index).toUpperCase().endsWith("-OFF")) // �Ѿ��ص���������
				{
					continue;// �ö����Ѿ��ر�
				}
				
				if(excludedKeyCount.get(index)!=null){
					if(excludedKeyCount.get(index).incrementAndGet()>maxSkipCount){
						excludedKeyCount.remove(index);
					    log.error(maxSkipCount+"�����ѹ���indexΪ"+index+"������Դ�������³���ȡ����");
				    }else{
				    	continue;
				    }
				}
				
				final TGroupDataSource tGroupDataSource = (TGroupDataSource) dataSourceMap
						.get(dbGroupKeys.get(index));
				// ��ѯ��ֻ�����������ݿ�ҵ��������������ݿⱣ��
				try {
					//���δʹ�ó�ʱ���������Ѿ�ֻʣ����1������Դ��������ô��ȥ��
					if(!useSlowProtect||excludedKeyCount.size()>=(dscount-1)){
					    conn = tGroupDataSource.getConnection();
					    stmt = conn.prepareStatement(getSelectSql());
					    stmt.setString(1, name);
					    GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
					    rs = stmt.executeQuery();
					    rs.next();
					    oldValue = rs.getLong(1);
					}else{
						FutureTask<Long> future=new FutureTask<Long>(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								//ֱ���׳��쳣����ӣ�����������Ҫֱ�ӹر�����
								Connection fconn=null;
								PreparedStatement fstmt=null;
								ResultSet frs=null;
								try{
								    fconn = tGroupDataSource.getConnection();
								    fstmt = fconn.prepareStatement(getSelectSql());
								    fstmt.setString(1, name);
								    GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
								    frs= fstmt.executeQuery();
								    frs.next();
								    return frs.getLong(1);
								}finally{
									closeResultSet(frs);
									frs = null;
									closeStatement(fstmt);
									fstmt = null;
									closeConnection(fconn);
									fconn = null;
								}
						    }
						});
						
						try {
						    exec.submit(future);
							oldValue=future.get(protectMilliseconds, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							throw new SQLException("[SEQUENCE SLOW-PROTECTED MODE]:InterruptedException",e);
						} catch (ExecutionException e) {
							throw new SQLException("[SEQUENCE SLOW-PROTECTED MODE]:ExecutionException",e);
						} catch (TimeoutException e) {
							throw new SQLException("[SEQUENCE SLOW-PROTECTED MODE]:TimeoutException,��ǰ���ó�ʱʱ��Ϊ"+protectMilliseconds,e);
						}
					}
					
					if (oldValue < 0) {
						StringBuilder message = new StringBuilder();
						message.append(
								"Sequence value cannot be less than zero, value = ")
								.append(oldValue);
						message.append(", please check table ").append(
								getTableName());
						log.info(message);

						continue;
					}
					if (oldValue > Long.MAX_VALUE - DELTA) {
						StringBuilder message = new StringBuilder();
						message.append("Sequence value overflow, value = ")
								.append(oldValue);
						message.append(", please check table ").append(
								getTableName());
						log.info(message);
						continue;
					}
					
					newValue = oldValue + outStep;
					if (!check(index, newValue)) // ���������ֵ������
					{
						if (this.isAdjust()) {
							newValue = (newValue - newValue % outStep)
									+ outStep + index * innerStep;// ���ó��µĵ���ֵ
						} else {
							SequenceException sequenceException = new SequenceException(
									dbGroupKeys.get(index)
											+ ":"
											+ name
											+ "��ֵ�ô��󣬸��ǵ�������Χ���ˣ����޸����ݿ⣬���߿���adjust���أ�");

							log.error(dbGroupKeys.get(index) + ":" + name
									+ "��ֵ�ô��󣬸��ǵ�������Χ���ˣ����޸����ݿ⣬���߿���adjust���أ�",
									sequenceException);
							throw sequenceException;
						}
					}
				} catch (SQLException e) {
					log.error("ȡ��Χ������--��ѯ����" + dbGroupKeys.get(index) + ":"
							+ name, e);
					//�������Դֻʣ�������һ�����Ͳ�Ҫ�ų���
					if(excludedKeyCount.size()<(dscount-1)){
						excludedKeyCount.put(index, new AtomicInteger(0));
					    log.error("��ʱ�߳�indexΪ"+index+"������Դ��"+maxSkipCount+"�κ����³���");
					}
					
					continue;
				} finally {
					closeResultSet(rs);
					rs = null;
					closeStatement(stmt);
					stmt = null;
					closeConnection(conn);
					conn = null;
				}
				readSuccess = true;

				try {
					conn = tGroupDataSource.getConnection();
					stmt = conn.prepareStatement(getUpdateSql());
					stmt.setLong(1, newValue);
					stmt.setTimestamp(2,
							new Timestamp(System.currentTimeMillis()));
					stmt.setString(3, name);
					stmt.setLong(4, oldValue);
					GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
					int affectedRows = stmt.executeUpdate();
					if (affectedRows == 0) {
						continue;
					}

				} catch (SQLException e) {
					log.error("ȡ��Χ������--���³���" + dbGroupKeys.get(index) + ":"
							+ name, e);
					continue;
				} finally {
					closeStatement(stmt);
					stmt = null;
					closeConnection(conn);
					conn = null;
				}
				writeSuccess = true;
				if (readSuccess && writeSuccess)
					return new SequenceRange(newValue + 1, newValue + innerStep);
			}
			//���������һ�����Ի���ʱ,���excludedMap,���������һ�λ���
            if(i==(retryTimes-2)){
            	excludedKeyCount.clear();
            }
		}
		log.error("��������Դ�������ã�������" + this.retryTimes + "�κ���Ȼʧ��!");
		throw new SequenceException("All dataSource faild to get value!");
	}

	public int getDscount() {
		return dscount;
	}

	public void setDscount(int dscount) {
		this.dscount = dscount;
	}

	private String getInsertSql() {
		if (insertSql == null) {
			synchronized (this) {
				if (insertSql == null) {
					StringBuilder buffer = new StringBuilder();
					buffer.append("insert into ").append(getTableName())
							.append("(");
					buffer.append(getNameColumnName()).append(",");
					buffer.append(getValueColumnName()).append(",");
					buffer.append(getGmtModifiedColumnName()).append(
							") values(?,?,?);");
					insertSql = buffer.toString();
				}
			}
		}
		return insertSql;
	}

	private String getSelectSql() {
		if (selectSql == null) {
			synchronized (this) {
				if (selectSql == null) {
					StringBuilder buffer = new StringBuilder();
					buffer.append("select ").append(getValueColumnName());
					buffer.append(" from ").append(getTableName());
					buffer.append(" where ").append(getNameColumnName())
							.append(" = ?");

					selectSql = buffer.toString();
				}
			}
		}

		return selectSql;
	}

	private String getUpdateSql() {
		if (updateSql == null) {
			synchronized (this) {
				if (updateSql == null) {
					StringBuilder buffer = new StringBuilder();
					buffer.append("update ").append(getTableName());
					buffer.append(" set ").append(getValueColumnName())
							.append(" = ?, ");
					buffer.append(getGmtModifiedColumnName()).append(
							" = ? where ");
					buffer.append(getNameColumnName()).append(" = ? and ");
					buffer.append(getValueColumnName()).append(" = ?");

					updateSql = buffer.toString();
				}
			}
		}

		return updateSql;
	}

	private static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				log.debug("Could not close JDBC ResultSet", e);
			} catch (Throwable e) {
				log.debug("Unexpected exception on closing JDBC ResultSet", e);
			}
		}
	}

	private static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				log.debug("Could not close JDBC Statement", e);
			} catch (Throwable e) {
				log.debug("Unexpected exception on closing JDBC Statement", e);
			}
		}
	}

	private static void closeConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.debug("Could not close JDBC Connection", e);
			} catch (Throwable e) {
				log.debug("Unexpected exception on closing JDBC Connection", e);
			}
		}
	}

	public int getRetryTimes() {
		return retryTimes;
	}

	public void setRetryTimes(int retryTimes) {
		this.retryTimes = retryTimes;
	}

	public int getInnerStep() {
		return innerStep;
	}

	public void setInnerStep(int innerStep) {
		this.innerStep = innerStep;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getNameColumnName() {
		return nameColumnName;
	}

	public void setNameColumnName(String nameColumnName) {
		this.nameColumnName = nameColumnName;
	}

	public String getValueColumnName() {
		return valueColumnName;
	}

	public void setValueColumnName(String valueColumnName) {
		this.valueColumnName = valueColumnName;
	}

	public String getGmtModifiedColumnName() {
		return gmtModifiedColumnName;
	}

	public void setGmtModifiedColumnName(String gmtModifiedColumnName) {
		this.gmtModifiedColumnName = gmtModifiedColumnName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setDbGroupKeys(List<String> dbGroupKeys) {
		this.dbGroupKeys = dbGroupKeys;
	}

	public void setDataSourceType(DataSourceType dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	public boolean isAdjust() {
		return adjust;
	}

	public void setAdjust(boolean adjust) {
		this.adjust = adjust;
	}

	public int getMaxSkipCount() {
		return maxSkipCount;
	}

	public void setMaxSkipCount(int maxSkipCount) {
		this.maxSkipCount = maxSkipCount;
	}

	public boolean isUseSlowProtect() {
		return useSlowProtect;
	}

	public void setUseSlowProtect(boolean useSlowProtect) {
		this.useSlowProtect = useSlowProtect;
	}

	public int getProtectMilliseconds() {
		return protectMilliseconds;
	}

	public void setProtectMilliseconds(int protectMilliseconds) {
		this.protectMilliseconds = protectMilliseconds;
	}
}
