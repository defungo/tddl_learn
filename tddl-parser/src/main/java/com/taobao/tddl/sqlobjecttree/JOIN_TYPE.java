package com.taobao.tddl.sqlobjecttree;
public enum JOIN_TYPE{
		/**
		*������
		*/
		INNER
		,/**
		* ��������
		*/
		LEFT,
		LEFT_OUTER,
		/**
		 * ��������
		 */
		RIGHT,
		RIGHT_OUTER,
		
		/**
		 * ȫ����
		 */
		
		FULL,
		FULL_OUTER,
		/**
		 * �����ʱû�ù�
		 */
		UNION,
		/**
		 * ��������
		 */
		CROSS;
	}