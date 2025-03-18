/*
 * @author norbert
 * @date 10.10.2006
 * @version $Id: PermObjectMapper.java,v 1.4 2008-05-26 15:05:55 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;

import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;

/**
 * special implementation of the objectmapper for table perm (where o_
 */
public class PermObjectMapper extends ObjectMapper {

	/**
	 * @param sourceTable
	 * @param tables
	 * @param reference
	 */
	public PermObjectMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
	}

	public void init(Connection conn, Parameter[] parameter) throws StructureCopyException {
		super.init(conn, parameter);

		// // folder
		// Table folderTable = tables.getTable(ObjectHelper.typeToTable(ObjectHelper.T_FOLDER));
		// if (folderTable != null) {
		// targets.put(ObjectHelper.T_NODE, folderTable);
		// }
	}
}
