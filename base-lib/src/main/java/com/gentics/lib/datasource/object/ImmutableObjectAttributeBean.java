package com.gentics.lib.datasource.object;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;

/**
 * Immutable version of {@link ObjectAttributeBean}.
 */
public class ImmutableObjectAttributeBean extends ObjectAttributeBean {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2935261749548138567L;

	@Override
	public void setAttributetype(int attributeType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttributetype(String attributetype) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setExcludeVersioning(boolean excludeVersioning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFilesystem(boolean filesystem) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setExcludeVersioning(String excludeVersioning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setForeignlinkattribute(String foreignLinkAttributeType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setForeignlinkattributerule(String foreignlinkattributerule) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLinkedobjecttype(int linkedObjectType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLinkedobjecttype(String linkedObjectType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMultivalue(boolean multivalue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMultivalue(String multivalue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setObjecttype(int objectType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setObjecttype(String objectType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOldname(String oldName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOptimized(boolean optimized) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOptimized(String optimized) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setQuickname(String quickName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVersioning(String versioning) {
		throw new UnsupportedOperationException();
	}
}
