/*
 * @author floriangutmann
 * @date Nov 18, 2009
 * @version $Id: FileManager.java,v 1.3 2010-10-19 11:20:01 norbert Exp $
 */
package com.gentics.contentnode.io;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Transactional;
import com.gentics.lib.log.NodeLogger;

/**
 * The FileManager allows to manipulate files on the filesystem within a transaction.
 * It has to be attached to a transaction otherwise no action will be performed.
 * The file manipulations are performed after the database transaction is successfully committed.
 * 
 * @author floriangutmann
 */
public class FileManager extends AbstractTransactional {

	/**
	 * A NodeLogger instance
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(FileManager.class);
    
	/**
	 * Holds the files to delete
	 */
	private List deleteList = new Vector();
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		for (Iterator it = deleteList.iterator(); it.hasNext();) {
			File file = (File) it.next();

			if (!file.delete()) {
				logger.info("Could not delete file { " + file.getName() + " }. Either it doesn't exist or it has incorrect permissions.");
			}
		}

		return false;
	}

	/**
	 * Deletes a file from the filesystem
	 * @param file
	 */
	public void deleteFile(File file) {
		deleteList.add(file);
	}

	public void onDBCommit(Transaction t) throws NodeException {}

}
