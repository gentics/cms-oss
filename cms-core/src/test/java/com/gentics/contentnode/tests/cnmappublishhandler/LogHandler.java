/*
 * @author norbert
 * @date 12.11.2009
 * @version $Id: LogHandler.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.cnmappublishhandler;

import java.util.Map;
import java.util.TreeMap;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of the publish handler, that just logs what it is doing
 */
public class LogHandler implements CnMapPublishHandler {

	/**
	 * logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#close()
	 */
	public void close() {
		logger.info("close()");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#commit()
	 */
	public void commit() {
		logger.info("commit()");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#createObject(com.gentics.api.lib.resolving.Resolvable)
	 */
	public void createObject(Resolvable object) throws CnMapPublishException {
		logger.info("createObject(" + object + ")");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#deleteObject(com.gentics.api.lib.resolving.Resolvable)
	 */
	public void deleteObject(Resolvable object) throws CnMapPublishException {
		logger.info("deleteObject(" + object + ")");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#destroy()
	 */
	public void destroy() {
		logger.info("destroy()");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#init(java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public void init(@SuppressWarnings("rawtypes") Map parameters) throws CnMapPublishException {
		logger.info("init(" + new TreeMap<>(parameters) + ")");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#open(long)
	 */
	public void open(long timestamp) throws CnMapPublishException {
		logger.info("open(timestamp)");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#rollback()
	 */
	public void rollback() {
		logger.info("rollback()");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.publish.CnMapPublishHandler#updateObject(com.gentics.api.lib.resolving.Resolvable)
	 */
	public void updateObject(Resolvable object) throws CnMapPublishException {
		logger.info("updateObject(" + object + ")");
	}
}
