package com.gentics.lib.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) <p/>Date: 11.12.2003
 */
public interface ResultProcessor {
	void process(ResultSet rs) throws SQLException;

	void takeOver(ResultProcessor p);
}
