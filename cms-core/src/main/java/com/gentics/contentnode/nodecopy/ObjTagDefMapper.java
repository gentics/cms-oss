/*
 * @author norbert
 * @date 20.02.2008
 * @version $Id: ObjTagDefMapper.java,v 1.2 2008-02-28 15:42:36 herbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;

/**
 * Reference descriptor for referencing from an objecttag to its definition
 */
public class ObjTagDefMapper extends AbstractReferenceDescriptor {

	/**
	 * target table
	 */
	protected Table[] targetTable;

	/**
	 * Create an instance of the reference descriptor
	 * @param sourceTable source table
	 * @param tables tables definition
	 * @param reference reference
	 */
	public ObjTagDefMapper(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
		targetTable = new Table[] { tables.getTable("objtagdef")};
	}

	// /* (non-Javadoc)
	// * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#getLinkedObject(com.gentics.contentnode.dbcopy.StructureCopy, java.sql.Connection, com.gentics.contentnode.dbcopy.DBObject, java.util.Map, boolean)
	// */
	// public DBObject getLinkedObject(StructureCopy copy, Connection conn, DBObject object,
	// Map allObjects, boolean getFromDB) throws StructureCopyException {
	// // no direct link between an objtag and its definition
	// try {
	// PreparedStatement st = conn.prepareStatement("SELECT o2.id FROM objtag as o1 INNER JOIN objtag as o2 ON o1.obj_type = o2.obj_type AND o1.name = o2.name AND o2.obj_id = 0 WHERE o1.id = ?");
	// st.setObject(1, object.getOriginalId());
	// ResultSet rs = st.executeQuery();
	// if(rs.next()) {
	// DBObject obj = tables.getTable("objtagdef").getObjectByID(copy, conn, rs.getObject(1), allObjects, getFromDB, "objtag_objtagref", object);
	// System.out.println("need to export objtag with id: " + rs.getInt(1) + " --- " + obj.toString());
	// return obj;
	// }
	// } catch (SQLException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return null;
	// }

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		// TODO hmmm, there is no real link column
		return "name";
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getLinkingObjects(com.gentics.contentnode.dbcopy.StructureCopy, java.sql.Connection, com.gentics.contentnode.dbcopy.DBObject, java.util.Map)
	 */
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		// TODO get all objtag's with obj_id = 0 and matching obj_type and name
		// there are no foreign links from objtagdef to objtag.
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getPossibleTargets()
	 */
	public Table[] getPossibleTargets() {
		return targetTable;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getReferenceColumns()
	 */
	public String[] getReferenceColumns() {
		// no real reference columns here
		return new String[] { "name", "obj_type" };
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getTargetTable(com.gentics.contentnode.dbcopy.StructureCopy, com.gentics.contentnode.dbcopy.DBObject)
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		return targetTable[0];
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#init(java.sql.Connection, com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType[])
	 */
	public void init(Connection conn, Parameter[] parameter) throws StructureCopyException {// nothing to do here
	}
}
