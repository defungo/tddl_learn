package com.taobao.tddl.jdbc.group;

import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.jdbc.group.dbselector.DBSelector;

/**
 * @author yangzhu
 *
 */
public class ThreadLocalDataSourceIndex {
	public static boolean existsIndex() {
		return getIndexAsObject() != null;
	}

	public static Integer getIndexAsObject() {
		Integer indexObject = null;
		try {
			indexObject = (Integer) ThreadLocalMap.get(ThreadLocalString.DATASOURCE_INDEX);
			if (indexObject == null)
				return null;

			return indexObject;
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(indexObject));
		}
	}

	public static int getIndex() {
		Integer indexObject = null;
		try {
			indexObject = (Integer) ThreadLocalMap.get(ThreadLocalString.DATASOURCE_INDEX);
			//����������ʱ����-1������������ֻҪ֪������ֵ��-1�ͻ���Ϊҵ���û�����ù�����
			if (indexObject == null)
				return DBSelector.NOT_EXIST_USER_SPECIFIED_INDEX;

			int index = indexObject.intValue();
			//���ҵ�������������������ʱ��������Ϊ��ֵ
			if (index < 0)
				throw new IllegalArgumentException(msg(indexObject));

			return index;
		} catch (Exception e) {
			throw new IllegalArgumentException(msg(indexObject));
		}
	}
	
	public static void clearIndex() {
		ThreadLocalMap.remove(ThreadLocalString.DATASOURCE_INDEX);
		ThreadLocalMap.remove(ThreadLocalString.RETRY_IF_SET_DS_INDEX);
	}
	
	private static String msg(Integer indexObject) {
		return indexObject + " ����һ����Ч������Դ����������ֻ���Ǵ���0������";
	}
}