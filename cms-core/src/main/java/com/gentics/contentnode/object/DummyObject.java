/*
 * @author norbert
 * @date 18.05.2007
 * @version $Id: DummyObject.java,v 1.4 2010-10-08 11:45:32 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Collections;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * The DummyObject class is used to manipulate objects after they were removed
 * from the db (e.g. trigger delete event)
 */
public class DummyObject extends AbstractContentObject implements LocalizableNodeObject<DummyObject> {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1172194187295288681L;

	/**
	 * Node ID
	 */
	protected int nodeId;

	/**
	 * Mesh UUID of the dummy object
	 */
	protected String meshUuid;

	/**
	 * Language of the dummy object in Mesh CR
	 */
	protected String meshLanguage;

	/**
	 * Create an instance of the dummy object
	 * @param id id
	 * @param info object info
	 */
	public DummyObject(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Set the owning node ID
	 * @param nodeId node ID
	 */
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String className = null;

		if (getObjectInfo() != null && getObjectInfo().getObjectClass() != null) {
			className = getObjectInfo().getObjectClass().getName();
			className = className.substring(className.lastIndexOf(".") + 1);
		}
		return "DummyObject for " + className + " {" + getId() + "}";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copy()
	 */
	public NodeObject copy() throws NodeException {
		// dummy implementation
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getObject()
	 */
	public DummyObject getObject() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSet()
	 */
	public Map<Integer, Integer> getChannelSet() throws NodeException {
		return Collections.emptyMap();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSetId(boolean)
	 */
	public Integer getChannelSetId() throws NodeException {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannel()
	 */
	public Node getChannel() throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getOwningNode()
	 */
	public Node getOwningNode() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		return t.getObject(Node.class, nodeId);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#setChannelInfo(java.lang.Integer, java.lang.Integer)
	 */
	public void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException {}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#setChannelInfo(java.lang.Integer, java.lang.Integer, boolean)
	 */
	public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange) throws ReadOnlyException, NodeException {}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#modifyChannelId(java.lang.Integer)
	 */
	public void modifyChannelId(Integer channelId) throws ReadOnlyException, NodeException {}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#isInherited()
	 */
	public boolean isInherited() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#isMaster()
	 */
	public boolean isMaster() throws NodeException {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getMaster()
	 */
	public LocalizableNodeObject<DummyObject> getMaster() throws NodeException {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#pushToMaster(com.gentics.contentnode.object.Node)
	 */
	public LocalizableNodeObject<DummyObject> pushToMaster(Node master) throws ReadOnlyException, NodeException {
		throw new NodeException("Cannot push the dummy object into the master");
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getNextHigherObject()
	 */
	public LocalizableNodeObject<DummyObject> getNextHigherObject() throws NodeException {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String setName(String name) throws ReadOnlyException {
		return null;
	}

	/**
	 * Get the mesh uuid
	 * @return mesh uuid
	 */
	public String getMeshUuid() {
		return meshUuid;
	}

	/**
	 * Set the mesh uuid
	 * @param meshUuid mesh uuid
	 */
	public void setMeshUuid(String meshUuid) {
		this.meshUuid = meshUuid;
	}

	/**
	 * Get the mesh language
	 * @return mesh language
	 */
	public String getMeshLanguage() {
		return meshLanguage;
	}

	/**
	 * Set the mesh language
	 * @param meshLanguage mesh language
	 */
	public void setMeshLanguage(String meshLanguage) {
		this.meshLanguage = meshLanguage;
	}
}
