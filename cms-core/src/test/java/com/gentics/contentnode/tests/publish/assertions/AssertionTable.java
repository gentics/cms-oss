/*
 * @author johannes2
 * @date 20.08.2008
 * @version $Id: AssertionTable.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.publish.assertions;

import java.util.ArrayList;
import java.util.Iterator;

import com.gentics.contentnode.tests.publish.CRComparator;
import com.gentics.lib.log.NodeLogger;

import junit.framework.Assert;

public class AssertionTable {

	static NodeLogger logger = NodeLogger.getNodeLogger(AssertionExtension.class.getClass());

	public AssertionTable() {}

	/**
	 * Generic comparison method
	 * @param tableName
	 * @param columnName
	 * @param nRow
	 * @param reference
	 * @param target
	 */
	public static void assertEquals(String tableName, String columnName, int nRow,
			Object reference, Object target) {
		Assert.assertEquals("Comparison failed for: " + tableName + "/" + columnName + " - " + " Row: " + nRow, String.valueOf(reference),
				String.valueOf(target));
	}

	public static void assertEqualsWithNull(String tableName, String columnName, int nRow,
			Object reference, Object target) {
		if (isNull(reference, target)) {
			logger.debug("Assertion - " + tableName + " - " + columnName + " - Row: " + nRow + " - Both objects are null");
		} else {
			assertEquals(tableName, columnName, nRow, reference, target);
		}

	}

	/**
	 * Compare reference object with target object. reference object might be an
	 * empty string which should be equal to null.
	 * @param reference
	 * @param target
	 * @return
	 */
	public static boolean isNull(Object reference, Object target) {
		// "" == null
		if (String.valueOf(reference).equals("") && (target == null)) {
			return true;
			// null == ""
		} else if ((String.valueOf(target).equals("") && (reference == null))) {
			return true;
			// 0 == null
		} else if (String.valueOf(reference).equals("0") && (target == null)) {
			return true;
			// null == null
		} else if ((target == null) && (reference == null)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Read the given key and return an array containing all values separated by
	 * ','
	 * @param keyName
	 * @return
	 */
	public static ArrayList getColumnsFromSetting(String keyName) {
		ArrayList ar = new ArrayList();
		String param = CRComparator.compareSettings.getProperty(keyName);
		String[] columns = param.split(",");
		int i = 0;

		while (i < columns.length) {
			ar.add(columns[i]);
			i++;
		}

		return ar;
	}

	public static boolean isValueInKey(String key, String value) {
		ArrayList columns = getColumnsFromSetting(key);
		Iterator it = columns.iterator();

		while (it.hasNext()) {
			if (value.equals(it.next())) {
				return true;
			}

		}
		return false;
	}
}
