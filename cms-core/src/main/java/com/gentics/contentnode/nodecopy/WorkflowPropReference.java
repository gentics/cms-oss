/*
 * @author norbert
 * @date 17.10.2006
 * @version $Id: WorkflowPropReference.java,v 1.7 2008-02-26 10:39:33 norbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.ReferenceDescriptor;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.dbcopy.Tables;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType.Parameter;
import com.gentics.lib.db.DB;

/**
 * Reference Descriptor for references from tables reactionprop and eventprop
 */
public class WorkflowPropReference extends AbstractReferenceDescriptor implements
		ReferenceDescriptor {

	/**
	 * possible targets
	 */
	protected Table[] possibleTargets;

	/**
	 * mapping of keywords to target tables
	 */
	protected Map<String, Table> keywordMapping;

	/**
	 * Create instance of the reference descriptor
	 * @param sourceTable source table
	 * @param tables all tables
	 * @param reference reference
	 */
	public WorkflowPropReference(Table sourceTable, Tables tables, Reference reference) {
		super(sourceTable, tables, reference);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#getLinkColumn()
	 */
	public String getLinkColumn() {
		return "value";
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getLinkingObjects(com.gentics.contentnode.dbcopy.StructureCopy,
	 *      java.sql.Connection, com.gentics.contentnode.dbcopy.DBObject,
	 *      java.util.Map)
	 */
	public List<DBObject> getLinkingObjects(StructureCopy copy, Connection conn, DBObject object,
			Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException {
		// we don't need this
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.AbstractReferenceDescriptor#getTargetTable(com.gentics.contentnode.dbcopy.DBObject)
	 */
	public Table getTargetTable(StructureCopy copy, DBObject object) throws StructureCopyException {
		Table targetTable = getReferencedObjectTable(object);

		if (targetTable != null) {
			return targetTable;
		}
		String keyWord = ObjectTransformer.getString(object.getColValue("keyword"), null);
		Connection conn = null;

		if ("o_id".equals(keyWord)) {
			// special case: keyword is o_id -> we have to fetch the record
			// with
			// same triggerevent_id/reaction_id and keyword "o_type", which
			// tells us
			// the target type

			PreparedStatement st = null;
			ResultSet res = null;

			conn = copy.getConnection();
			try {
				st = conn.prepareStatement("SELECT value FROM `" + sourceTable.getName() + "` WHERE keyword = 'o_type' AND " + getParentObjectLink() + " = ?");
				st.setObject(1, object.getColValue(getParentObjectLink()));
				res = st.executeQuery();
				if (res.next()) {
					targetTable = tables.getTable(ObjectHelper.typeToTable(new Integer(res.getString(getLinkColumn()))));
				}
			} catch (SQLException e) {
				throw new StructureCopyException(e);
			} finally {
				DB.close(res);
				DB.close(st);
			}
		} else if (keywordMapping.containsKey(keyWord)) {
			targetTable = (Table) keywordMapping.get(keyWord);
		}
		return targetTable;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#init(java.sql.Connection,
	 *      com.gentics.contentnode.dbcopy.jaxb.JAXBreferenceType.ParameterType[])
	 */
	public void init(Connection conn, Parameter[] parameter) throws StructureCopyException {
		List<Table> targets = new Vector<Table>();

		targets.add(tables.getTable("folder"));
		targets.add(tables.getTable("template"));
		targets.add(tables.getTable("page"));
		targets.add(tables.getTable("contentfile"));

		keywordMapping = new HashMap<String, Table>();
		for (int i = 0; i < parameter.length; i++) {
			Table targetTable = tables.getTable(parameter[i].getValue());

			if (targetTable != null) {
				keywordMapping.put(parameter[i].getId(), targetTable);
				if (!targets.contains(targetTable)) {
					targets.add(targetTable);
				}
			}
		}

		possibleTargets = (Table[]) targets.toArray(new Table[targets.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getPossibleTargets()
	 */
	public Table[] getPossibleTargets() {
		return possibleTargets;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.ReferenceDescriptor#getReferenceColumns()
	 */
	public String[] getReferenceColumns() {
		return new String[] { "keyword", "value", getParentObjectLink()};
	}

	/**
	 * Get the link name to the parent object
	 * @return link name
	 */
	protected String getParentObjectLink() {
		if ("reactionprop".equals(sourceTable.getName())) {
			return "reaction_id";
		} else if ("eventprop".equals(sourceTable.getName())) {
			return "triggerevent_id";
		} else {
			return null;
		}
	}
}
