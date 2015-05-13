package com.taobao.tddl.sqlobjecttree.mysql.function;


import java.util.HashMap;
import java.util.Map;

import com.taobao.tddl.sqlobjecttree.Function;
import com.taobao.tddl.sqlobjecttree.mysql.function.datefunction.Current_date;
import com.taobao.tddl.sqlobjecttree.mysql.function.datefunction.Sysdate;
import com.taobao.tddl.sqlobjecttree.mysql.function.interval.datetype.Day;
import com.taobao.tddl.sqlobjecttree.mysql.function.interval.datetype.Hour;
import com.taobao.tddl.sqlobjecttree.mysql.function.interval.datetype.IntervalMonth;
import com.taobao.tddl.sqlobjecttree.mysql.function.interval.datetype.IntervalYear;
import com.taobao.tddl.sqlobjecttree.mysql.function.interval.datetype.Minute;
import com.taobao.tddl.sqlobjecttree.mysql.function.interval.datetype.Second;


public class MySQLConsistStringRegister {
	public final static MySQLConsistStringRegister reg=new MySQLConsistStringRegister();
	private  final static Map<String, Class<? extends Function>> consistReg=new HashMap<String, Class<? extends Function>>();
	static{
		consistReg.put("SYSDATE", Sysdate.class);
		consistReg.put("CURRENT_DATE", Current_date.class);
		/**
		 * ADD BY JUNYU
		 */
		consistReg.put("DAY",Day.class);
		consistReg.put("SECOND",Second.class);
		consistReg.put("HOUR",Hour.class);
		consistReg.put("MINUTE", Minute.class);
		
		/**
		 * ����ᵼ�¹����ͻ����ΪYEAR(),MONTH()����
		 * ��INTERVAL��YEAR,MONTH�ı�һ�£�����ANTLR
		 * ��look ahead���Կ��Խ�������ͻ����ΪYEAR
		 * ����������ֵķ�����(,INTERVAL��YEAR�󲻿���
		 * ����������ţ���֮��Ȼ
		 */
		consistReg.put("YEAR", IntervalYear.class);
		consistReg.put("MONTH", IntervalMonth.class);
	}
	public boolean containsKey(String key){
		return  consistReg.containsKey(key);
	}
	public Function get(String key){
		Function cls=null;
		try {
			if(key==null||key.trim().equals("")){
				throw new IllegalArgumentException("group function����Ϊ��");
			}
			cls=consistReg.get(key.toUpperCase()).newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);	
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);	
		}
		return cls;
	}
	
}
