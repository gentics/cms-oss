/*
 * @author norbert
 * @date 03.10.2006
 * @version $Id: Reference.java,v 1.7 2010-11-09 09:59:00 clemens Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.sql.Connection;
import java.sql.SQLException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.dbcopy.jaxb.JAXBReferenceType;

/**
 * Reference configuration
 */
public class Reference extends JAXBReferenceType {

	/**
	 * reference descriptor 
	 */
	protected ReferenceDescriptor referenceDescriptor;

	/**
	 * source table
	 */
	protected Table sourceTable;

	/**
	 * all tables
	 */
	protected Tables tables;

	/*
	 * (non-Javadoc)
	 * @see com.gentics.jaxb.JAXBreferenceType#getCol()
	 */
	public String getCol() {
		String col = super.getCol();

		return col != null ? col : getTarget() + "_id";
	}

	/**
	 * Check the consistency of the reference
	 * @param conn database connection
	 * @param tables tables
	 * @return true when the reference is consistent, false if not
	 * @throws SQLException
	 */
	public boolean checkConsistency(Connection conn, Table sourceTable, Tables tables) throws SQLException {
		this.sourceTable = sourceTable;
		this.tables = tables;
		if (isSetImplementationClass()) {
			// first get the implementation class
			try {
				Class<?> implClass = Class.forName(getImplementationClass());

				if (!ReferenceDescriptor.class.isAssignableFrom(implClass)) {
					// class not allowed
					System.err.println(
							"Reference of " + sourceTable + " found with implementation class " + getImplementationClass() + " which does not implement "
							+ ReferenceDescriptor.class.getName());
					return false;
				}
				referenceDescriptor = (ReferenceDescriptor) implClass.getConstructor(new Class[] { Table.class, Tables.class, Reference.class}).newInstance(
						new Object[] { sourceTable, tables, this});
				referenceDescriptor.init(conn, getParameter());

				// if (!isSetForeigndeepcopy() || isForeigndeepcopy(true)) {
				// foreigndeepcopy is set to true (or not set at all), so
				// add this
				// reference as foreign reference to the target tables
				Table[] targets = referenceDescriptor.getPossibleTargets();

				for (int i = 0; i < targets.length; i++) {
					targets[i].addForeignReference(this);
				}
				// }
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		} else {
			if (!isSetTarget()) {
				System.err.println("Reference of " + sourceTable + " found with neither implementation class nor target table set");
				return false;
			} else {
				Table targetTable = tables.getTable(getTarget());

				// check whether the target table exists
				if (targetTable == null) {
					System.err.println("Reference of " + sourceTable + " found with target table {" + getTarget() + "} which does not exist");
					return false;
				} else if (true || !isSetForeigndeepcopy() || isForeigndeepcopy(true)) {
					// add this reference as foreign reference to the target
					// table
					targetTable.addForeignReference(this);
				}
			}
		}
		return true;
	}

	public boolean isForeigndeepcopy(boolean treatAskAsTrue) {
		String foreignDeepCopy = getForeigndeepcopy().value();

		return "true".equals(foreignDeepCopy) || (treatAskAsTrue && "ask".equals(foreignDeepCopy));
	}

	/**
	 * Get the reference descriptor of this reference
	 * @param conn database connection
	 * @return reference descriptor
	 */
	public ReferenceDescriptor getReferenceDescriptor(Connection conn) throws StructureCopyException {
		if (referenceDescriptor == null) {
			// need to generate the reference descriptor
			if (!isSetImplementationClass()) {
				// need to generate a normal reference descriptor
				referenceDescriptor = new NormalReference(sourceTable, tables.getTable(getTarget()), getCol(), this);
				referenceDescriptor.init(conn, getParameter());
			} else {// TODO generate and initialize the reference descriptor
			}
		}
		return referenceDescriptor;
	}

	/**
	 * Check whether the table has deepcopy
	 * @return true for deepcopy, false for no deepcopy (or ask)
	 */
	public boolean isDeepcopy() {
		return ObjectTransformer.getBoolean(getDeepcopy(), true);
	}
}
