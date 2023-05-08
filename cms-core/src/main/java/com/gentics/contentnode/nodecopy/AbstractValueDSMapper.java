/*
 * @author norbert
 * @date 25.02.2008
 * @version $Id: AbstractValueDSMapper.java,v 1.1 2008-02-26 10:39:33 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Vector;

import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType;
import com.gentics.lib.db.DB;

/**
 * Abstract base class for two mappers value -&gt; datasource/datasource_value
 * for sharing common methods
 */
public abstract class AbstractValueDSMapper extends AbstractReferenceDescriptor {

	/**
	 * list of datasource parts
	 */
	protected List<Object> datasourceParts = new Vector<Object>();

	/**
	 * Create an instance of this reference descriptor
	 * @param sourceTable
	 * @param tables
	 * @param reference
	 */
	public AbstractValueDSMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#init(java.sql.Connection,
	 *      com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType[])
	 */
	public void init(Connection conn, ParameterType[] parameter) throws StructureCopyException {
		Statement st = null;
		ResultSet res = null;

		try {
			// get all datasource parts
			st = conn.createStatement();
			res = st.executeQuery("SELECT id FROM part WHERE type_id IN (29, 30, 32)");
			while (res.next()) {
				datasourceParts.add(res.getObject("id"));
			}
		} catch (SQLException e) {
			throw new StructureCopyException(e);
		} finally {
			DB.close(res);
			DB.close(st);
		}
	}

	/**
	 * Check whether the given object is a datasource value
	 * @param object given dbobject
	 * @return true for datasource values, false for other values
	 */
	protected boolean isDatasourceValue(DBObject object) {
		return datasourceParts.contains(object.getColValue("part_id"));
	}
}
