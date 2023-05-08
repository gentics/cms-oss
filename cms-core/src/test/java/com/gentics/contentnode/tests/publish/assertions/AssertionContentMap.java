/*
 * @author johannes2
 * @date 20.08.2008
 * @version $Id: AssertionContentMap.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.publish.assertions;

public class AssertionContentMap extends AssertionTable {

	private final static String TABLE_NAME = "contentmap";

	public static void assertEquals(String columnName, int nRow, Object reference, Object target) {
		if (isValueInKey("ContentmapAssertEqualsWithNull", columnName)) {
			assertEqualsWithNull(TABLE_NAME, columnName, nRow, reference, target);
		} else {
			assertEquals(TABLE_NAME, columnName, nRow, reference, target);
		}

	}

}
