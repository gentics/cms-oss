/*
 * @author johannes2
 * @date 20.08.2008
 * @version $Id: AssertionExtension.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.publish.assertions;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.SortedTable;

import com.gentics.lib.log.NodeLogger;

import junit.framework.Assert;

public final class AssertionExtension {

	private AssertionExtension() {}

	private static NodeLogger logger = NodeLogger.getNodeLogger(AssertionExtension.class.getClass());

	/**
	 * Compare two ITables
	 * @param reference
	 * @param target
	 * @throws DataSetException
	 */
	public static void assertEqualsWithNull(ITable reference, ITable target) throws DataSetException {
		Column[] colsRef = reference.getTableMetaData().getColumns();
		Column[] colsSrc = target.getTableMetaData().getColumns();

		int i = 0;

		while (i < colsRef.length) {
			Assert.assertEquals(colsRef[i].getColumnName(), colsSrc[i].getColumnName());
			logger.info("CRComparator - Comparing " + reference.getTableMetaData().getTableName() + " - " + colsRef[i].getColumnName());
			int u = 0;

			if (target.getRowCount() != 0) {
				while (u < target.getRowCount()) {

					Object objRef = reference.getValue(u, colsRef[i].getColumnName());
					Object objSrc = target.getValue(u, colsSrc[i].getColumnName());

					Assert.assertEquals(
							"Comparison failed for: " + reference.getTableMetaData().getTableName() + " - " + colsRef[i].getColumnName() + " Row: " + u,
							String.valueOf(objRef), String.valueOf(objSrc));

					u++;

				}
				logger.info("CRComparator - Comparision of " + u + " values finished.");
			} else {
				logger.info("CRComparator - No Values for comparison were found");
			}

			i++;
		}
	}

	/**
	 * Compare two ITables but handle value_blob, value_clob, quick_public,
	 * quick_publisher, quickname, quick_navsortorder, foreignlinkattributerule
	 * in different ways
	 * @param reference
	 * @param target
	 * @throws DataSetException
	 */
	public static void assertEqualsHandleTables(ITable reference, ITable target) throws DataSetException {
		Column[] colsRef = reference.getTableMetaData().getColumns();
		Column[] colsTrg = target.getTableMetaData().getColumns();

		int i = 0;

		while (i < colsRef.length) {
			Assert.assertEquals(colsRef[i].getColumnName(), colsTrg[i].getColumnName());
			logger.info("Assertion - Comparing " + reference.getTableMetaData().getTableName() + " - " + colsRef[i].getColumnName());

			int u = 0;

			if (target.getRowCount() != 0) {
				while (u < target.getRowCount()) {

					Object objRef = reference.getValue(u, colsRef[i].getColumnName());
					Object objTrg = target.getValue(u, colsTrg[i].getColumnName());

					if (reference.getTableMetaData().getTableName().equalsIgnoreCase("contentattribute:reference")) {
						String contentid = (String) target.getValue(u, "contentid");
						String name = (String) target.getValue(u, "name");
                        
						AssertionContentAttribute.assertEquals(colsRef[i].getColumnName(), u, contentid, name, objRef, objTrg);
					} else if (reference.getTableMetaData().getTableName().equalsIgnoreCase("contentmap:reference")) {
						AssertionContentMap.assertEquals(colsRef[i].getColumnName(), u, objRef, objTrg);
					} else if (reference.getTableMetaData().getTableName().equalsIgnoreCase("contentattributetype:reference")) {
						AssertionContentAttributeType.assertEquals(colsRef[i].getColumnName(), u, objRef, objTrg);
					} else {
						Assert.fail("Unknown table name: '" + reference.getTableMetaData().getTableName() + "'");
					}

					u++;
				}
			}

			logger.info("CRComparator - Comparing of " + target.getRowCount() + " elements was successful");
			i++;
		}
	}

	/**
	 * Compare two datasets and ignore those column which are defined in
	 * excludedColumns for all tables.
	 * @param reference
	 * @param source
	 * @param excludedColumns
	 * @throws Exception
	 */
	public static void assertEqualsWithColumnFilter(IDataSet reference, IDataSet source) throws Exception {
		ITableIterator itRef = reference.iterator();
		ITableIterator itSrc = source.iterator();

		while (itRef.next() && itSrc.next()) {

			// skip contentstatus table
			if (itSrc.getTable().getTableMetaData().getTableName().equalsIgnoreCase("contentstatus")) {
				continue;
			}

			ITable tableSrc = itSrc.getTable();
			ITable tableRef = itRef.getTable();

			if (itSrc.getTable().getTableMetaData().getTableName().equalsIgnoreCase("contentattribute")) {
				tableSrc = new SortedTable(tableSrc, new String[] { "name", "contentid"});
				tableRef = new SortedTable(tableRef, new String[] { "name", "contentid"});
			}

			Assert.assertEquals(tableSrc.getTableMetaData().getTableName(), tableRef.getTableMetaData().getTableName());

			Column[] colsRef = tableRef.getTableMetaData().getColumns();
			Column[] colsSrc = tableSrc.getTableMetaData().getColumns();

			int i = 0;

			while (i < colsRef.length) {
				Assert.assertEquals(colsRef[i].getColumnName(), colsSrc[i].getColumnName());
				logger.info("CRComparator - Comparing " + tableRef.getTableMetaData().getTableName() + " - " + colsRef[i].getColumnName());

				// skipping column 'id'
				if (colsRef[i].getColumnName().equalsIgnoreCase("id")) {
					i++;
					continue;
				}

				// skipping column 'updatetimestamp'
				if (colsRef[i].getColumnName().equalsIgnoreCase("updatetimestamp")) {
					i++;
					continue;
				}

				int u = 0;

				if (tableSrc.getRowCount() != 0) {
					while (u < tableSrc.getRowCount()) {
						Object objRef = tableRef.getValue(u, colsRef[i].getColumnName());
						Object objSrc = tableSrc.getValue(u, colsSrc[i].getColumnName());

						Assert.assertEquals(
								"Comparison failed for: " + tableRef.getTableMetaData().getTableName() + " - " + colsRef[i].getColumnName() + " Row: " + u,
								String.valueOf(objRef), String.valueOf(objSrc));
						u++;
					}
				}

				logger.info("CRComparator - Comparing of " + tableSrc.getRowCount() + " elements was successful");
				i++;
			}

		}
	}

}
