package com.gentics.contentnode.publish.wrapper;

import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * NodeObjectInfo instances used by Publishable Objects
 */
public class PublishableNodeObjectInfo implements NodeObjectInfo {
	/**
	 * Object class
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * Version Timestamp
	 */
	protected int versionTimestamp;

	/**
	 * Hash key
	 */
	protected String hashKey;

	/**
	 * Create an instance
	 * @param clazz class
	 * @param versionTimestamp version timestamp
	 */
	public PublishableNodeObjectInfo(Class<? extends NodeObject> clazz, int versionTimestamp) {
		this.clazz = clazz;
		this.versionTimestamp = versionTimestamp;
		this.hashKey = clazz.getName() + ";false;" + versionTimestamp;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#getObjectClass()
	 */
	public Class<? extends NodeObject> getObjectClass() {
		return clazz;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#getEditUserId()
	 */
	public int getEditUserId() {
		return 0;
	}

	@Override
	public String getHashKey() {
		return hashKey;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#getFactory()
	 */
	public NodeFactory getFactory() {
		return ContentNodeFactory.getInstance().getFactory();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#getConfiguration()
	 */
	public NodeConfig getConfiguration() {
		try {
			return TransactionManager.getCurrentTransaction().getNodeConfig();
		} catch (TransactionException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#getVersionTimestamp()
	 */
	public int getVersionTimestamp() {
		return versionTimestamp;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#isCurrentVersion()
	 */
	public boolean isCurrentVersion() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObjectInfo#getSubInfo(java.lang.Class)
	 */
	public NodeObjectInfo getSubInfo(Class<? extends NodeObject> clazz) {
		return new PublishableNodeObjectInfo(clazz, versionTimestamp);
	}
}
