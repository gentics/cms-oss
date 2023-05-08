/*
 * @author raoul
 * @date 17.03.2006
 * @version $Id: DBCon.java,v 1.1 2010-02-04 14:25:03 norbert Exp $
 */
package com.gentics.node.tests.crsync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBCon {

	Connection con;

	Statement stmt;

	String type = "";

	public DBCon(String dbDriver, String dbUrl, String dbUser, String dbPass) throws ClassNotFoundException, SQLException {
		int sp1 = dbUrl.indexOf(":");
		int sp2 = dbUrl.indexOf(":", sp1 + 1);
		
		type = dbUrl.substring(sp1 + 1, sp2);

		Class.forName(dbDriver);
		con = DriverManager.getConnection(dbUrl, dbUser, dbPass);
		createStatement();
	}
	
	private void createStatement() throws SQLException {
		this.stmt = this.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
	}
	
	private void closeStatement() throws SQLException {
		if (this.stmt != null) {
			this.stmt.close();
		}
	}
	
	public void resetStatement() throws SQLException {
		closeStatement();
		createStatement();
	}

	public String getType() {
		if (type == null) {
			return "";
		}
		if ("sqlserver".equals(type)) {
			return "mssql";
		}
		
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public ResultSet runSQL(String sql) {
		try {
			resetStatement();
			ResultSet rs = this.stmt.executeQuery(sql);

			return rs;
		} catch (SQLException ex) {}
		return null;
	}

	public void updateSQL(String sql) {
		updateSQL(sql, true);
	}

	public void updateSQL(String sql, boolean stopOnError) {
		try {
			resetStatement();
			this.stmt.executeUpdate(sql);
		} catch (SQLException ex) {
			if (stopOnError) {
				ex.printStackTrace();
			}
		}
	}

	public void closeCON() {
		try {
			if (this.stmt != null) {
				this.stmt.close();
			}
			this.con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the product name of the database
	 * @return product name
	 * @throws SQLException
	 */
	public String getDatabaseProductName() throws SQLException {
		return con.getMetaData().getDatabaseProductName();
	}
}
