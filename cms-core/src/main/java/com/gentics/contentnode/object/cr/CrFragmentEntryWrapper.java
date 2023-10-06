package com.gentics.contentnode.object.cr;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.TagmapEntry;

/**
 * Wrapper for instances of {@link CrFragmentEntry}
 */
public class CrFragmentEntryWrapper extends TagmapEntry {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 329337365163660144L;

	/**
	 * Wrapped fragment entry
	 */
	protected CrFragmentEntry wrapped;

	/**
	 * ContentRepository
	 */
	protected ContentRepository cr;

	/**
	 * Create an instance
	 * @param cr ContentRepository
	 * @param wrapped wrapped entry
	 */
	public CrFragmentEntryWrapper(ContentRepository cr, CrFragmentEntry wrapped) {
		super(null, null);
		this.cr = cr;
		this.wrapped = wrapped;
	}

	@Override
	public NodeObject copy() throws NodeException {
		throw new NodeException("Not implemented");
	}

	@Override
	public String getTagname() {
		return wrapped.getTagname();
	}

	@Override
	public String getMapname() {
		return wrapped.getMapname();
	}

	@Override
	public int getObject() {
		return wrapped.getObjType();
	}

	@Override
	public int getTargetType() {
		return wrapped.getTargetType();
	}

	@Override
	public AttributeType getAttributetype() {
		return AttributeType.getForType(wrapped.getAttributeTypeId());
	}

	@Override
	public int getAttributeTypeId() {
		return wrapped.getAttributeTypeId();
	}

	@Override
	public boolean isMultivalue() {
		return wrapped.isMultivalue();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public boolean isOptimized() {
		return wrapped.isOptimized();
	}

	@Override
	public boolean isFilesystem() {
		return wrapped.isFilesystem();
	}

	@Override
	public String getForeignlinkAttribute() {
		return wrapped.getForeignlinkAttribute();
	}

	@Override
	public String getForeignlinkAttributeRule() {
		return wrapped.getForeignlinkAttributeRule();
	}

	@Override
	public ContentRepository getContentRepository() throws NodeException {
		return cr;
	}

	@Override
	public CrFragment getContentRepositoryFragment() throws NodeException {
		return TransactionManager.getCurrentTransaction().getObject(CrFragment.class, wrapped.getCrFragmentId());
	}

	@Override
	public String getCategory() {
		return wrapped.getCategory();
	}

	@Override
	public boolean isSegmentfield() {
		return wrapped.isSegmentfield();
	}

	@Override
	public boolean isDisplayfield() {
		return wrapped.isDisplayfield();
	}

	@Override
	public boolean isUrlfield() {
		return wrapped.isUrlfield();
	}

	@Override
	public String getElasticsearch() {
		return wrapped.getElasticsearch();
	}

	@Override
	public String getMicronodeFilter() {
		return wrapped.getMicronodeFilter();
	}

	@Override
	public Integer getId() {
		return wrapped.getId();
	}

	@Override
	public GlobalId getGlobalId() {
		return wrapped.getGlobalId();
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		return wrapped.getObjectInfo();
	}

	@Override
	public boolean isNoIndex() {
		return wrapped.isNoIndex();
	}
}
