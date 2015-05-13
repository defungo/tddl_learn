package com.taobao.tddl.sqlobjecttree.oracle.function;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.taobao.tddl.common.exception.runtime.NotSupportException;
import com.taobao.tddl.sqlobjecttree.Utils;
import com.taobao.tddl.sqlobjecttree.common.value.OperationBeforTwoArgsFunction;

public class ToDate extends OperationBeforTwoArgsFunction {
	private static final String[] dateNotSupport=new String[]{
		"SSSSS",
		"SYYY",
		"IYYY", 
		"RR", 	
		"YEAR", 
		"SYEAR", 	
		"BC", 	
		"Q",  
		//"D", 	
		"DAY", 	
		"FMDAY",	
		"DY", 	
		"J", };
	private volatile DateFormat toDateFormat;
	@Override
	public String getFuncName() {
		return "TO_DATE";
	}
	@Override
	public Comparable<?> getVal(List<Object> args) {
			Object obj=null;
			DateFormat df;
			Date returnDate;
			String timeArgument;
			try {
				obj=Utils.getVal(args, arg2);
				String temp=(String)obj;
				df = getDF(temp);
				obj=Utils.getVal(args, arg1);
				timeArgument=(String)obj;
			} catch (ClassCastException e) {
				throw new IllegalArgumentException("�����ת��������"+obj+"�ò�������ΪString");
			}
			try {
				returnDate=df.parse(timeArgument);
			} catch (ParseException e) {
				throw new IllegalArgumentException("�����ʱ�亯������ǰʱ�亯��StringΪ"+timeArgument,e);
			}
			return returnDate;
	}
	protected DateFormat getDF(String source){
		String temp;
		if(toDateFormat==null){
			synchronized (this) {
				if(toDateFormat==null){
					temp=replaceToJavaFormat(source);
					toDateFormat=new SimpleDateFormat(temp);
				}else{
					//do nothing
				}
			}
		}
		return toDateFormat;
	}
	/**
	 * �ṩ��oracle��format��ʽת��Ϊjava format��ʽ��ת������
	 * @param source
	 * @return
	 */
	protected static final String replaceToJavaFormat(String source){
		if(source==null){
			return null;
		}else{
			source=source.toUpperCase();
			for(String s:dateNotSupport){
				if(source.contains(s))
				{
					throw new NotSupportException("Oracleת��������֧��ʹ�õ�ǰ����:"+s+"���������:"+source);
				}
			}
			source=source.replaceAll("YYYY", "yyyy");
			source=source.replaceAll("YY", "yy");
			source=source.replaceAll("MONTH", "MMMMM");
			source=source.replaceAll("MON", "MMM");
			source=source.replaceAll("WW", "www");
			source=source.replaceAll("DD", "dd");
			source=source.replaceAll("HH24", "kk");
			source=source.replaceAll("HH12", "hh");
			source=source.replaceAll("HH", "hh");
			source=source.replaceAll("AM", "aaa");
			source=source.replaceAll("PM", "aaa");
			source=source.replaceAll("MI", "mm");
			source=source.replaceAll("SS", "ss");
			return source;
		}
		
	}

}
