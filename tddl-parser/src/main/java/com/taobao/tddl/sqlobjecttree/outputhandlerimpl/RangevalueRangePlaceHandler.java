//package com.taobao.tddl.sqlobjecttree.outputhandlerimpl;
//
//import java.util.Map;
//
//import com.taobao.tddl.sqlobjecttree.PageWrapperCommon;
//
///**
// * ��Ӧ limit m , n �е�n ��Ҫ�滻�����
// * @author shenxun
// *
// */
//public class RangevalueRangePlaceHandler extends RangePlaceHandler{
//
//
//	private long getSubLong(Number skip, Number max) {
//		return (max.longValue() - skip.longValue());
//	}
//
//	private int getSubInt(Number skip, Number max) {
//		return (max.intValue() - skip.intValue());
//	}
//	
//	/**
//	 * ��ֵ����Ӧ�ڰ󶨱��������m,n�����
//	 * @param index
//	 * @param limitFrom
//	 * @param limitTo
//	 * @param modifiedMap
//	 */
//	protected void modifyParam(int index ,Number skip, Number max,Map<Integer,Object> modifiedMap) {
//		if (skip instanceof Long || max instanceof Long) {
//			modifiedMap.put(index, getSubLong(skip, max));
//		} else if (skip instanceof Integer && max instanceof Integer) {
//			modifiedMap.put(index, getSubInt(skip, max));
//		} else {
//			throw new IllegalArgumentException("ֻ֧��int long�����");
//		}
//	}
//
//	protected String getSqlReturn(Number skip, Number max) {
//		if (skip instanceof Long || max instanceof Long) {
//			return String.valueOf(getSubLong(skip, max));
//		} else if (skip instanceof Integer && max instanceof Integer) {
//			return String.valueOf(getSubInt(skip, max));
//		} else {
//			throw new IllegalArgumentException("ֻ֧��int long�����");
//		}
//
//	}
//
//}
