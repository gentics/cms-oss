package com.gentics.contentnode.object.parttype;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * Part Type for linking forms
 */
public class FormPartType extends AbstractPartType {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3689154778164901914L;

	/**
	 * Create instance
	 * @param value value
	 * @throws NodeException
	 */
	public FormPartType(Value value) throws NodeException {
		super(value);
	}

	@Override
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	@Override
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		return isRequired() && StringUtils.isBlank(getFormUUID());
	}

	@Override
	public Type getPropertyType() {
		return Type.FORM;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		setFormUUID(property.getStringValue());
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setStringValue(getFormUUID());
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof FormPartType) {
			return Objects.equals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}

	/**
	 * Get the form UUID
	 * @return form UUID
	 */
	public String getFormUUID() {
		return getValueObject().getValueText();
	}

	/**
	 * Set the form UUID
	 * @param formUUID form UUID
	 * @throws ReadOnlyException
	 */
	public void setFormUUID(String formUUID) throws ReadOnlyException {
		getValueObject().setValueText(formUUID);
	}

	@Override
	public String render(RenderResult renderResult, String template) throws NodeException {
		super.render(renderResult, template);
		return getFormUUID();
	}
}
