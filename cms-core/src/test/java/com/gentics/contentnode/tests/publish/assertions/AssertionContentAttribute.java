/*
 * @author johannes2
 * @date 20.08.2008
 * @version $Id: AssertionContentAttribute.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.publish.assertions;

import junit.framework.Assert;

public final class AssertionContentAttribute extends AssertionTable {

	private final static String TABLE_NAME = "contentattribute";

	public static void assertEquals(String columnName, int nRow, String contentID,
			String attributeName, Object reference, Object target) {

		if (isValueInKey("ContentAttributeAssertEqualsWithNull", columnName)) {
			assertEqualsWithNull(TABLE_NAME, columnName, nRow, reference, target);
		} else if (columnName.equals("value_blob")) {

			if (isNull(reference, target)) {
				logger.debug("Assertion - Both are null");
			} else {
				// TODO Implement comparison of blobs
				logger.debug("Assertion - Skipping blobs");
			}

		} else if (columnName.equals("value_clob")) {
			if (isNull(reference, target)) {
				logger.debug("Assertion - " + TABLE_NAME + " - " + columnName + " - Row: " + nRow + " - Both objects are null");
			} else if (isValueInKey("excludeCLOBForPageContents", contentID) && attributeName.equalsIgnoreCase("content")) {
				logger.debug("Assertion - Skipping value_clob - contentattribute for page: " + contentID);
			} else {
				assertEqualsClob(columnName, nRow, contentID, reference, target);
			}

		} else {
			assertEquals(TABLE_NAME, columnName, nRow, reference, target);
		}

	}

	/**
	 * Compare both clob values with this method. It will replace some tags
	 * because php and java publisher use different ways of rendering
	 * @param columnName
	 * @param nRow
	 * @param contentID
	 * @param reference
	 * @param target
	 */
	public static void assertEqualsClob(String columnName, int nRow, String contentID,
			Object reference, Object target) {
		String referenceStr = (String) reference;
		String targetStr = (String) target;

		// remove brs because php and java publisher render them differently
		referenceStr = removeBR(referenceStr);
		targetStr = removeBR(targetStr);

		Assert.assertEquals("Comparison failed for: " + TABLE_NAME + "/" + columnName + " - " + " Row: " + nRow + " - ContentID: " + contentID, referenceStr,
				targetStr);

	}

	public static String removeBR(String html) {
		html = html.replaceAll("<br>", "<REPLACEDBR>");
		html = html.replaceAll("<br />", "<REPLACEDBR>");
		html = html.replaceAll("<BR />", "<REPLACEDBR>");
		html = html.replaceAll("<BR>", "<REPLACEDBR>");
		return html;
	}
}
