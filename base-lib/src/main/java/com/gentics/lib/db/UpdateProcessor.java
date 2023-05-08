package com.gentics.lib.db;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 27.04.2004
 */
public interface UpdateProcessor {
	void process(Statement stmt) throws SQLException;
}
