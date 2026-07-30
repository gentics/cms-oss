package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * Replacement {@link PartType} implementation for optional parttypes, which are not available
 */
public class UnavailablePartType extends AbstractPartType implements PartType {

	private static final long serialVersionUID = -493274421361002723L;

	protected String originalClass;

	public UnavailablePartType(Value value, String originalClass) throws NodeException {
		super(value);
		this.originalClass = originalClass;
	}

	@Override
	public Type getPropertyType() {
		return Type.UNKNOWN;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}

	@Override
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	@Override
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		return false;
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		return false;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
	}

	@Override
	public String render(RenderResult renderResult, String template) throws NodeException {
		logger.error("PartType implementation %s is not available".formatted(originalClass));
		return null;
	}
}
