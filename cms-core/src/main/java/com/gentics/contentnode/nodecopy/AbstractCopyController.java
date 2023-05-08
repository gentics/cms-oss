/*
 * @author norbert
 * @date 25.02.2008
 * @version $Id: AbstractCopyController.java,v 1.4 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.nodecopy;

import java.sql.Connection;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.dbcopy.CopyController;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.ReferenceRestrictor;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.StructureCopyException;
import com.gentics.contentnode.dbcopy.StructureCopyInterruptedException;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * @author norbert
 */
public abstract class AbstractCopyController implements CopyController {
	public void copyObjectStructure(StructureCopy copy, Map<StructureCopy.ObjectKey, DBObject> objectStructure) throws StructureCopyException {
		Connection conn = copy.getConnection();

		// do the copying in three steps
		// 1st copy the (normal) objects, generate new ids
		for (DBObject object : objectStructure.values()) {
			// do the first run copy, the copy controller might omit cross
			// tables here
			RuntimeProfiler.beginMark(ComponentsConstants.COPYCONTROLLER_FIRSTRUN);
			object.getSourceTable().copyObject(copy, conn, object, true);
			RuntimeProfiler.endMark(ComponentsConstants.COPYCONTROLLER_FIRSTRUN);
		}

		// now all normal objects (having id's) are copied and new id's have
		// been generated, so we can now update the object links

		// 2nd update the object links
		for (DBObject object : objectStructure.values()) {
			// again omit cross tables
			RuntimeProfiler.beginMark(ComponentsConstants.COPYCONTROLLER_SECONDRUN);
			if (!object.getSourceTable().isCrossTable()) {
				object.getSourceTable().updateObjectLinks(copy, conn, object);
			}
			RuntimeProfiler.endMark(ComponentsConstants.COPYCONTROLLER_SECONDRUN);
		}

		// cross table objects do not have id's, so they are copied and updated
		// in one step. this has to be done in the last run.
		// 3rd copy the cross table objects
		for (DBObject object : objectStructure.values()) {
			// do the second run copy, the copy controller might only copy
			// crosstables here
			RuntimeProfiler.beginMark(ComponentsConstants.COPYCONTROLLER_THIRDRUN);
			object.getSourceTable().copyObject(copy, conn, object, false);
			RuntimeProfiler.endMark(ComponentsConstants.COPYCONTROLLER_THIRDRUN);
		}
	}

	/**
	 * Check interrupted status of thread (set when user cancels operation).
	 * @throws StructureCopyInterruptedException if thread has been interrupted
	 */
	protected static void checkInterrupted() throws StructureCopyInterruptedException {
		if (Thread.interrupted()) {
			throw new StructureCopyInterruptedException();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#isExcluded(com.gentics.contentnode.dbcopy.Table, java.lang.Object)
	 */
	public int isExcluded(StructureCopy copy, Table table, Object id) {
		// the default implementation does not exclude anything
		return EXCLUSION_NO;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.dbcopy.CopyController#getCurrentReferenceRestrictor()
	 */
	public ReferenceRestrictor getCurrentReferenceRestrictor() {
		// the default implementation does not support reference descriptors
		return null;
	}

	/**
	 * Create a version of the page
	 * @param copy copy configuration
	 * @param object page object
	 * @throws StructureCopyException
	 */
	protected void createPageVersion(StructureCopy copy, DBObject object) throws StructureCopyException {
		Integer pageId = ObjectTransformer.getInteger(object.getNewId(), null);
		Transaction t = copy.getTransaction();

		if (t == null) {
			throw new StructureCopyException("Error while creating page versions: Could not get the transaction");
		}

		try {
			Page page = (Page) t.getObject(Page.class, pageId);

			if (page == null) {
				throw new StructureCopyException("Error while creating page versions for page {" + pageId + "}: Could not find page.");
			}

			PageFactory.createPageVersion(page, page.isOnline() && !page.isModified(), null);
		} catch (NodeException e) {
			throw new StructureCopyException("Error while creating page versions", e);
		}
	}
}
