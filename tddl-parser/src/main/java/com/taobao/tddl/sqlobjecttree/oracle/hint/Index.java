package com.taobao.tddl.sqlobjecttree.oracle.hint;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.sqlobjecttree.HintSetter;
import com.taobao.tddl.sqlobjecttree.IndexWrapper;

public class Index implements HintSetter {
	List<String> args=Collections.emptyList();
	public List<String> getArguments() {
		return args;
	}

	public void appendSQL(StringBuilder sb) {
		sb.append("INDEX").append("(");
		boolean firstElement=true;
		for(String str:args){
			if(firstElement){
				firstElement=false;
			}else{
				sb.append(",");
			}
			sb.append(str);
		}
		sb.append(")");
	} 

	public StringBuilder regTableModifiable(Set<String> oraTabName,
			List<Object> list, StringBuilder sb) {
		//����ʵ�ֱȽϹ��죬��ΪregTableModifiable��������漰��̫������Ҫ�䶯
		//Ϊ����һ��index�䶯ȥ�޸������ܹ��ò���ʧ��������ʱ����һ����ʱ�ķ�ʽ�������������
		//ֱ����ChangeMethodCommon���������Ҫ�滻��index������
		sb.append("INDEX").append("(");
//		boolean firstElement=true;
		if(args.size()!=2){
			throw new IllegalArgumentException("index hint��ʱֻ֧����������");
		}

		if(oraTabName.contains(StringUtil.trim(args.get(0)))){
			list.add(sb.toString());
			IndexWrapper wa=new IndexWrapper();
			wa.setOriginalTableName(args.get(0));
			list.add(wa);
			sb = new StringBuilder();
		}else{
			sb.append(args.get(0));
		}
		sb.append(",");
		//�ڶ��������ڲ����ڿ��ܻ������Ҫ�滻�ı��������Ҳ�����ȫƥ��,����Ҫ������ƥ��
		String dbIndex = args.get(1);
		int position = 0;
		for(String oraTable: oraTabName){
			int lastIndex = StringUtil.lastIndexOf(dbIndex, oraTable);
			if(-1 != lastIndex){
				//
				if(0 != lastIndex ){
					sb.append(StringUtil.substring(dbIndex, 0,lastIndex));
					position += lastIndex; 
				}
				//��ʾmatch��,����position�͵���lastIndex��λ��
				//position += dbIndex.length();
				position += oraTable.length();
				
				list.add(sb.toString());
				IndexWrapper wa=new IndexWrapper();
				wa.setOriginalTableName(oraTable);
				list.add(wa);
				sb = new StringBuilder();
				sb.append(StringUtil.substring(dbIndex, position));
				break;
			}
		}
//		list.add(sb.toString());
//		sb=new StringBuilder();
		sb.append(")");
		return sb;
	}

	public void addHint(List<String> args) {
		this.args=args;
	}

}
