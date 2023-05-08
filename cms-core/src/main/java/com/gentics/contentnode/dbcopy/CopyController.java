/*
 * @author norbert
 * @date 28.01.2008
 * @version $Id: CopyController.java,v 1.7 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.contentnode.dbcopy;

import java.util.List;
import java.util.Map;

import com.gentics.lib.etc.IWorkPhase;

/**
 * Interface for CopyControllers to be used by {@link StructureCopy}.<br/>
 * Specific implementations of this interface define, what exactly is copied
 * into what. Use Cases are:
 * <ul>
 * <li>NodeCopy</li>
 * <li>Export</li>
 * <li>Import</li>
 * </ul>
 */
public interface CopyController {

	/**
	 * constant for the return value of
	 * {@link #copyObject(StructureCopy, DBObject, boolean)} (object was
	 * created)
	 */
	public final static int OBJECT_CREATED = 0;

	/**
	 * constant for the return value of
	 * {@link #copyObject(StructureCopy, DBObject, boolean)} (object was
	 * updated)
	 */
	public final static int OBJECT_UPDATED = 1;

	/**
	 * constant for the return value of
	 * {@link #copyObject(StructureCopy, DBObject, boolean)} (object was
	 * removed)
	 */
	public final static int OBJECT_REMOVED = 2;

	/**
	 * constant for the return value of
	 * {@link #copyObject(StructureCopy, DBObject, boolean)} (object was
	 * ignored)
	 */
	public final static int OBJECT_IGNORED = 3;

	/**
	 * constant for the return value of method
	 * {@link #isExcluded(StructureCopy, Table, Object)}, which means: not excluded
	 */
	public final static int EXCLUSION_NO = 0;

	/**
	 * constant for the return value of method
	 * {@link #isExcluded(StructureCopy, Table, Object)}, which means: excluded (return null)
	 */
	public final static int EXCLUSION_NULL = 1;
    
	/**
	 * constant for the return value of method
	 * {@link #isExcluded(StructureCopy, Table, Object)}, which means: excluded (return an instance of {@link ExcludedObject} instead).
	 */
	public final static int EXCLUSION_NOTED = 2;

	/**
	 * Set the root workphase
	 * @param rootWorkPhase root work phase
	 */
	public void setRootWorkPhase(IWorkPhase rootWorkPhase);

	/**
	 * Start the copying process
	 * @param copy copy configuration
	 * @throws StructureCopyException
	 */
	public void startCopy(StructureCopy copy) throws StructureCopyException;

	/**
	 * finish the copying process
	 * @param copy copy configuration
	 * @throws StructureCopyException
	 */
	public void finishCopy(StructureCopy copy) throws StructureCopyException;

	/**
	 * Called in case of an (unrecoverable) error
	 * @param copy copy configuration
	 * @param e exception containing the error information
	 * @throws StructureCopyException
	 */
	public void handleErrors(StructureCopy copy, Exception e) throws StructureCopyException;

	/**
	 * Get the object structure as Map {@link StructureCopy.ObjectKey} -&gt;
	 * {@link DBObject}. The objects will hold references to other copied
	 * objects.
	 * @param copy copy configuration
	 * @param allObjects map of all objects
	 * @param mainObjects map of the main objects
	 * @throws StructureCopyException in case of errors
	 */
	public void getObjectStructure(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> allObjects, Map<StructureCopy.ObjectKey, DBObject> mainObjects) throws StructureCopyException;

	/**
	 * Get objects from the specified table, that are filtered by the given
	 * restriction. The fetched objects will be put into the map and also
	 * returned as list.
	 * @param copy copy configuration
	 * @param table table from which to fetch the objects
	 * @param restriction restriction string (might be null or empty for no
	 *        further restriction)
	 * @param params parameters to be used in the restriction
	 * @param allObjects map of all already fetched objects
	 * @param referenceName name of the reference which caused the objects to be
	 *        fetched (fetched objects will have the reference with this name
	 *        set to the given object)
	 * @param referencedObject referenced object which caused the objects to be
	 *        fetched
	 * @return list of fetched objects
	 * @throws StructureCopyException in case of errors
	 */
	public List<DBObject> getObjects(StructureCopy copy, Table table, String fromClause, String restriction,
			Object[] params, Map<StructureCopy.ObjectKey, DBObject> allObjects, String referenceName, DBObject referencedObject) throws StructureCopyException;

	/**
	 * Copy the given object. The specific copy implementation might take into
	 * consideration whether the object's table is a crosstable or not
	 * @param copy copy configuration
	 * @param object object to be copied
	 * @param firstRun true if called for the first run, false for the second
	 * @return one of ({@link #OBJECT_CREATED}, {@link #OBJECT_UPDATED}, {@link #OBJECT_REMOVED}, {@link #OBJECT_IGNORED}).
	 * @throws StructureCopyException in case of errors
	 */
	public int copyObject(StructureCopy copy, DBObject object, boolean firstRun) throws StructureCopyException;

	/**
	 * Update the object links of the given object in the copy target.
	 * @param copy copy configuration
	 * @param object object for which the object links shall be updated
	 * @throws StructureCopyException in case of errors
	 */
	public void updateObjectLinks(StructureCopy copy, DBObject object) throws StructureCopyException;

	/**
	 * Called once before copying the objects starts
	 * @param copy copy configuration
	 * @param allObjects map of all objects
	 * @throws StructureCopyException
	 */
	public void beginCopyObjects(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException;

	/**
	 * Called once after copying the objects ends
	 * @param copy copy configuration
	 * @param allObjects map of all objects
	 * @throws StructureCopyException
	 */
	public void finishCopyObjects(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> allObjects) throws StructureCopyException;

	/**
	 * Handle an unsatisfied link to another object. A link is unsatisfied when the reference has no deepcopy flag set and the referenced object is not in the set of copied objects.
	 * @param copy copy configuration
	 * @param object object holding the unsatisfied link
	 * @param reference reference descriptor
	 * @param table target table
	 * @param id id of the linked object
	 * @throws StructureCopyException
	 */
	public void handleUnsatisfiedLink(StructureCopy copy, DBObject object,
			ReferenceDescriptor reference, Table table, Object id) throws StructureCopyException;

	/**
	 * Get a single object by its id. When the object is excluded from copying
	 * (calling the method {@link #isExcluded(StructureCopy, Table, Object)} for
	 * the object would not return {@link #EXCLUSION_NO}), the exact behaviour
	 * depends on the flag checkForExclusions. If the flag is set, this method
	 * returns null for excluded objects, if the flag is not set, the object is
	 * returned anyway.
	 * @param copy copy configuration
	 * @param table table of the object
	 * @param id object id
	 * @param referenceName name of the reference
	 * @param referencingObject referencing object
	 * @param checkForExclusions whether object exclusions shall be checked or
	 *        not
	 * @return the object or null if not found
	 * @throws StructureCopyException
	 */
	public DBObject getObjectByID(StructureCopy copy, Table table, Object id,
			String referenceName, DBObject referencingObject, boolean checkForExclusions) throws StructureCopyException;

	/**
	 * Copy the object structure
	 * @param copy copy configuration
	 * @param objectStructure object structure
	 * @throws StructureCopyException
	 */
	public void copyObjectStructure(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> objectStructure) throws StructureCopyException;

	/**
	 * Called after copy was finished and transaction was committed.
	 * @param copy copy configuration
	 */
	public void postCommit(StructureCopy copy);

	/**
	 * Check whether the object shall be excluded from copying
	 * @param copy copy configuration
	 * @param table table of the object
	 * @param id id of the object
	 * @return the exclusion status for the object
	 */
	public int isExcluded(StructureCopy copy, Table table, Object id);

	/**
	 * Get the currently set reference restrictor
	 * @return currently set reference restrictor
	 */
	public ReferenceRestrictor getCurrentReferenceRestrictor();
}
