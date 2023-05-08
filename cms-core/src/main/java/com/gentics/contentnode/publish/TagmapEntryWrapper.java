package com.gentics.contentnode.publish;

import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;

/**
 * Wrapped for {@link TagmapEntry} instances
 */
public class TagmapEntryWrapper implements TagmapEntryRenderer {
	/**
	 * Wrapped entry
	 */
	protected TagmapEntry wrapped;

	/**
	 * Create an instance wrapping an entry
	 * @param wrapped wrapped entry
	 */
	public TagmapEntryWrapper(TagmapEntry wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public String getMapname() {
		return wrapped.getMapname();
	}

	@Override
	public String getTagname() {
		return wrapped.getTagname();
	}

	@Override
	public int getObjectType() {
		return wrapped.getObject();
	}

	@Override
	public int getTargetType() {
		return wrapped.getTargetType();
	}

	@Override
	public int getAttributeType() {
		return wrapped.getAttributeTypeId();
	}

	@Override
	public boolean isMultivalue() {
		return wrapped.isMultivalue();
	}

	@Override
	public boolean canSkip() {
		return true;
	}

	@Override
	public String toString() {
		return wrapped.toString();
	}
}
