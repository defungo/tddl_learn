/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.taobao.tddl.util;

/**
 *  �������
 * 
 * @author shenxun
 */
public interface Rule {
    String getDBIndex(Object param);
    String getTable(Object param);
}
