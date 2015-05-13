package com.taobao.tddl.common.coder;

import java.util.List;

import com.taobao.tddl.common.SyncCommand;

/**
 * @author huali
 *
 * ���ݿ��������������
 * ������ݿ���������б��ַ����ı���ͽ������
 */
public interface Coder {
	List<SyncCommand> decode(String content);
	
	String encode(List<SyncCommand> commands);
	
	String getId();
}
