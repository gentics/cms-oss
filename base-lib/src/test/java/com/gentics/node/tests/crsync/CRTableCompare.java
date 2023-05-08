/*
 * @author raoul
 * @date 17.03.2006
 * @version $Id: CRTableCompare.java,v 1.1 2010-02-04 14:25:03 norbert Exp $
 */
package com.gentics.node.tests.crsync;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.base.NodeIllegalArgumentException;

public class CRTableCompare {

	DBCon sdb1, tdb1, sdb2, tdb2 = null;
	
	public final static String[] CRTABLES = { "contentmap", "contentattributetype", "contentattribute", "contentobject"/* , "contentstatus"*/ };

	/**
	 * compare 2 crs, print readable result to stdout. exit status 0 for no
	 * changes, -1 for diff.
	 * @param args 2 filepaths to propertyfiles, source first, then target
	 */
	public static void main(String[] args) {
		try {
			if (args[0] == null || args[1] == null) {
				throw new NodeIllegalArgumentException("error, invalid arguments. 2 parameters to property files required. first source-path then target-path.");
			}
			Properties source = new Properties();

			source.load(new FileInputStream(args[0]));
			Properties target = new Properties();

			target.load(new FileInputStream(args[1]));

			// source
			DBCon s1 = new DBCon(source.getProperty("driverClass"), source.getProperty("url"), source.getProperty("username"), source.getProperty("passwd"));
			DBCon s2 = new DBCon(source.getProperty("driverClass"), source.getProperty("url"), source.getProperty("username"), source.getProperty("passwd"));
			// target
			DBCon t1 = new DBCon(target.getProperty("driverClass"), target.getProperty("url"), target.getProperty("username"), target.getProperty("passwd"));
			DBCon t2 = new DBCon(target.getProperty("driverClass"), target.getProperty("url"), target.getProperty("username"), target.getProperty("passwd"));
			CRTableCompare comp = new CRTableCompare(s1, t1, s2, t2);

			for (int i = 0; i < CRTABLES.length; i++) {
				comp.compareTable(CRTABLES[i]);
			}

			System.out.print("no diffs found.");

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
    
	public CRTableCompare(DBCon sdb1, DBCon tdb1, DBCon sdb2, DBCon tdb2) {
		// assign
		this.sdb1 = sdb1;
		this.tdb1 = tdb1;
		this.sdb2 = sdb2;
		this.tdb2 = tdb2;
	}
	
	public void compareTable(String table) throws SQLException, CompareDataException, CompareException, TableDoesNotExistException, CompareDataException {
		// recreate statements;
		sdb1.resetStatement();
		sdb2.resetStatement();
		tdb1.resetStatement();
		tdb2.resetStatement();
		
		// statement for sql structure
		String sql_structure = "SELECT * FROM " + table;
 		
		// result metadata sdb und tdb
		ResultSetMetaData rsmd1 = null;
		ResultSetMetaData rsmd2 = null;
	
		// resultsets sdb und tdb
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		
		// store columns
		StringBuffer sb1 = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();

		try {
			rsmd1 = this.sdb1.runSQL(sql_structure).getMetaData();
			rsmd2 = this.tdb1.runSQL(sql_structure).getMetaData();
		} catch (NullPointerException npe) {
			if (rsmd1 == null) {
				throw new TableDoesNotExistException("table " + table + " does not exist in db1");
			}
			if (rsmd2 == null) {
				throw new TableDoesNotExistException("table " + table + " does not exist in db2");
			}
		}
		
		if (rsmd1.getColumnCount() != rsmd2.getColumnCount()) {
			throw new CompareException("column count does not match");
			// throw new CompareTableException();
		}
		
		// compare table structure - start
		// compare table structure - get relevant column names - start
		
		// get columns for table one
		TreeSet ts1 = new TreeSet();

		for (int i = 1; i <= rsmd1.getColumnCount(); i++) {
			// exclude updatetimestamp, id and id_counter columns
			if (!"updatetimestamp".equalsIgnoreCase(rsmd1.getColumnName(i)) && !"id".equalsIgnoreCase(rsmd1.getColumnName(i))
					&& !"id_counter".equalsIgnoreCase(rsmd1.getColumnName(i))) {
				ts1.add(rsmd1.getColumnName(i).toLowerCase());
			}
			// System.out.println(rsmd1.getPrecision(i) + " " + rsmd1.getColumnTypeName(i) + " " + rsmd1.isNullable(i));
		}
		
		// store columns into stringbuffer
		Iterator i1 = ts1.iterator();

		while (i1.hasNext()) {
			sb1.append((String) i1.next());
		}
		
		// get columns for table two
		TreeSet ts2 = new TreeSet();

		for (int i = 1; i <= rsmd2.getColumnCount(); i++) {
			// exclude updatetimestamp, id and id_counter columns
			if (!"updatetimestamp".equalsIgnoreCase(rsmd2.getColumnName(i)) && !"id".equalsIgnoreCase(rsmd2.getColumnName(i))
					&& !"id_counter".equalsIgnoreCase(rsmd2.getColumnName(i))) {
				ts2.add(rsmd2.getColumnName(i).toLowerCase());
			}
			// ts2.add(rsmd2.getColumnName(i));
		}
		
		// store columns into stringbuffer
		Iterator i2 = ts2.iterator();

		while (i2.hasNext()) {
			sb2.append((String) i2.next());
		}
		if (!sb1.toString().equals(sb2.toString())) {
			throw new CompareException("column count and/or name do not match: \n" + sb1.toString() + " vs.\n" + sb2.toString());
			// throw new CompareTableException();
		}
		
		// compare table structure - get relevant column names - end

		if (!sdb1.getDatabaseProductName().equals(tdb1.getDatabaseProductName())) {
			// cycle column definition table one
			StringBuffer sb8 = new StringBuffer();
			TreeSet ts8 = new TreeSet();

			for (int i = 1; i <= rsmd1.getColumnCount(); i++) {
				if (!"updatetimestamp".equalsIgnoreCase(rsmd1.getColumnName(i)) && !"id".equalsIgnoreCase(rsmd1.getColumnName(i))
						&& !"id_counter".equalsIgnoreCase(rsmd1.getColumnName(i))) {
					// ts8.add(rsmd1.getColumnName(i) + " " +
					// rsmd1.getPrecision(i) + " " + rsmd1.getColumnTypeName(i)
					// + " " + rsmd1.isNullable(i) + "::");
					ts8.add(rsmd1.getColumnName(i) + " " + " " + rsmd1.getColumnTypeName(i) + " " + rsmd1.isNullable(i));
				}
			}
			Iterator i8 = ts8.iterator();

			while (i8.hasNext()) {
				sb8.append((String) i8.next());
			}

			// cycle column definition table two
			StringBuffer sb9 = new StringBuffer();
			TreeSet ts9 = new TreeSet();

			for (int i = 1; i <= rsmd2.getColumnCount(); i++) {
				if (!"updatetimestamp".equals(rsmd2.getColumnName(i)) && !"id".equals(rsmd2.getColumnName(i)) && !"id_counter".equals(rsmd2.getColumnName(i))) {
					// ts9.add(rsmd2.getColumnName(i) + " " +
					// rsmd2.getPrecision(i) + " " + rsmd2.getColumnTypeName(i)
					// + " " + rsmd2.isNullable(i) + "::");
					ts9.add(rsmd2.getColumnName(i) + " " + " " + rsmd2.getColumnTypeName(i) + " " + rsmd2.isNullable(i));
				}
			}
			Iterator i9 = ts9.iterator();

			while (i9.hasNext()) {
				sb9.append((String) i9.next());
			}

			// now compare the datatypes of table one and table two
			if (sdb1.getType().equals(tdb1.getType()) && !sb8.toString().equals(sb9.toString())) {
				throw new CompareException("table structure (column definition) does not match.");
			}
		}
		
		// compare table structure - end
		
		// compare table data - start
		String sql_data = "SELECT * FROM " + table;

		try {
			rs1 = this.sdb2.runSQL(sql_data);
			rs2 = this.tdb2.runSQL(sql_data);
		} catch (NullPointerException npe) {
			throw new TableDoesNotExistException("table " + table + " does not exist.");
		}

		// cycle all data from table one
		rs1.beforeFirst();
		while (rs1.next()) {
			i1 = ts1.iterator();
			StringBuffer sb3 = new StringBuffer();

			while (i1.hasNext()) {
				// sb3.append(rs1.getString((String)i1.next())+"::");
				sb3.append(getStringFromColumn(rs1, (String) i1.next()) + "::");
			}
			if (!isStringinDb(rs2, sb3.toString(), ts2)) {
				throw new CompareDataException("error finding db1 string in db2: " + sb3.toString());
			}
		}
		
		// cycle all data from table two
		rs2.beforeFirst();
		while (rs2.next()) {
			i2 = ts2.iterator();
			StringBuffer sb5 = new StringBuffer();

			while (i2.hasNext()) {
				// sb5.append(rs2.getString((String)i2.next())+"::");
				sb5.append(getStringFromColumn(rs2, (String) i2.next()) + "::");
			}
			if (!isStringinDb(rs1, sb5.toString(), ts1)) {
				throw new CompareDataException("error finding db2 string in db1: " + sb5.toString());
			}
		}

		// count the records in both tables
		int recordCount1 = 0;
		int recordCount2 = 0;

		rs1.beforeFirst();
		rs2.beforeFirst();
		while (rs1.next()) {
			recordCount1++;
		}
		while (rs2.next()) {
			recordCount2++;
		}

		// check for identical record counts
		if (recordCount1 != recordCount2) {
			throw new CompareDataException(
					"Difference in record count found: db1 contained " + recordCount1 + " records, db2 contained " + recordCount2 + " records");
		}

		// compare table data - end
	}
	
	/**
	 * check if string compare is in rs using ts helper
	 * @param rs
	 * @param compare
	 * @param ts
	 * @return true if data exists
	 */
	private boolean isStringinDb(ResultSet rs, String compare, TreeSet ts) {
		// int countsame = 0;
		try {
			rs.beforeFirst();
			while (rs.next()) {
				StringBuffer sb = new StringBuffer();
				Iterator i = ts.iterator();

				while (i.hasNext()) {
					// String s = (String) i.next();
					// Object o = rs.getObject(s);
					//
					// if (o instanceof byte[]) {
					// byte[] b = (byte[]) o;
					// sb.append(new String(b) + "::");
					// } else {
					// sb.append(rs.getString(s) +"::");
					// }
					
					sb.append(getStringFromColumn(rs, (String) i.next()) + "::");
				}

				if (sb.toString().equals(compare)) {
					return true;
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return false;
	
	}
	
	private String getStringFromColumn(ResultSet rs, String key) throws SQLException {
		try {
			Object o = rs.getObject(key);
	
			if (o instanceof byte[]) {
				byte[] b = (byte[]) o;

				return new String(b);
			} else if (o instanceof Blob) {
				Blob blob = rs.getBlob(key);
				Reader in = new BufferedReader(new InputStreamReader(blob.getBinaryStream()));
				StringBuffer buffer = new StringBuffer();
				int ch = 0;

				try {
					while ((ch = in.read()) > -1) {
						buffer.append((char) ch);
					}
					in.close();
				} catch (IOException ioe) {// ignore
				}
				return buffer.toString();
			} else {
				return ObjectTransformer.getString(rs.getObject(key), ""); // rs.getString(key);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	} 
}

