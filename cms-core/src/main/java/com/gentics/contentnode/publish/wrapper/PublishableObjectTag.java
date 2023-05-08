package com.gentics.contentnode.publish.wrapper;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.rest.model.Tag;

/**
 * Wrapper for the REST Model of an object tag
 * Instances of this class will be used when versioned publishing is active and multithreaded publishing is used.
 * See {@link PublishablePage} for details.
 */
public class PublishableObjectTag extends ObjectTag {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9194520665139074979L;

	/**
	 * Wrapped tag
	 */
	protected Tag wrappedTag;

	/**
	 * Container object
	 */
	protected NodeObject container;

	/**
	 * List of the values of this tag
	 */
	protected PublishableValueList valueList;

	/**
	 * Create an instance wrapping the given tag
	 * @param tag wrapped tag
	 * @param container container
	 */
	protected PublishableObjectTag(Tag tag, NodeObject container) throws NodeException {
		super(tag.getId(), null);
		this.wrappedTag = tag;
		this.container = container;
		this.valueList = new PublishableValueList(tag, this);
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		if (info == null) {
			info = container.getObjectInfo().getSubInfo(ObjectTag.class);
		}
		return info;
	}

	public NodeObject copy() throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public boolean isIntag() {
		return false;
	}

	@Override
	public boolean isInheritable() {
		return false;
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public int getObjType() {
		return ObjectTransformer.getInt(container.getTType(), 0);
	}

	@Override
	public NodeObject getNodeObject() throws NodeException {
		return container;
	}

	@Override
	public ObjectTagDefinition getDefinition() throws NodeException {
		// TODO this is currently only used for import/export (not for publishing)
		return null;
	}

	@Override
	protected void performDelete() throws NodeException {
		failReadOnly();
	}

	@Override
	public ValueList getValues() throws NodeException {
		valueList.checkDeletedParts();
		return valueList;
	}

	@Override
	public Construct getConstruct() throws NodeException {
		Construct construct = TransactionManager.getCurrentTransaction().getObject(Construct.class, wrappedTag.getConstructId());
		assertNodeObjectNotNull(construct, wrappedTag.getConstructId(), "Construct");
		return construct;
	}

	@Override
	public Integer getConstructId() throws NodeException {
		return wrappedTag.getConstructId();
	}

	@Override
	public String getName() {
		return wrappedTag.getName();
	}

	@Override
	public boolean isEnabled() {
		return wrappedTag.getActive();
	}

	@Override
	public int getEnabledValue() {
		return wrappedTag.getActive() ? 1 : 0;
	}

	@Override
	public String toString() {
		return "PublishableObjectTag {" + getName() + ", " + getId() + "}";
	}

	@Override
	public ObjectTag getInTagObject() throws NodeException {
		return null;
	}

	@Override
	public Integer getInTagId() {
		return null;
	}

	@Override
	public void sync() throws NodeException {
		failReadOnly();
	}

	@Override
	public Set<Integer> checkSync() throws NodeException {
		return Collections.emptySet();
	}

	@Override
	public Set<Pair<NodeObject, ObjectTag>> getSyncVariants() throws NodeException {
		return Collections.emptySet();
	}

	@Override
	public boolean hasSameContent(ObjectTag other) throws NodeException {
		// TODO Auto-generated method stub
		return false;
	}
}
