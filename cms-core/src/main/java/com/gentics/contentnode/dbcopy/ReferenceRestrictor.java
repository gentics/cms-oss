/*
 * @author norbert
 * @date 30.07.2009
 * @version $Id: ReferenceRestrictor.java,v 1.2 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.contentnode.dbcopy;

/**
 * Interface for implementations that restrict references when recursively finding references via the method {@link Table#getObjectLinks(StructureCopy, java.sql.Connection, java.util.Map, DBObject, boolean)}
 */
public interface ReferenceRestrictor {

	/**
	 * Check whether the reference is restricted or not
	 * @param copy copy configuration
	 * @param referenceDescriptor reference descriptor to check
	 * @param object object holding the references
	 * @param foreignReference true when the reference is a foreign reference to the given object, false if not (direct reference from the object)
	 * @return true when the reference is restricted, false if not
	 * @throws StructureCopyException
	 */
	public boolean isReferenceRestricted(StructureCopy copy,
			ReferenceDescriptor referenceDescriptor, DBObject object, boolean foreignReference) throws StructureCopyException;
}
