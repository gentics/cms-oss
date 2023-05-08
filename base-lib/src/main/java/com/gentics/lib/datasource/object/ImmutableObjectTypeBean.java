package com.gentics.lib.datasource.object;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;

/**
 * Immutable Version of {@link ObjectTypeBean}
 */
public class ImmutableObjectTypeBean extends ObjectTypeBean {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2944618282786185332L;

	@Override
	public void setExcludeVersioning(boolean excludeVersioning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setExcludeVersioning(String excludeVersioning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOldType(Integer oldType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOldType(String oldType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setType(Integer type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setType(String type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVersioning(String versioning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAttributeType(ObjectAttributeBean attributeType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearAttributeTypes() {
		throw new UnsupportedOperationException();
	}
}
