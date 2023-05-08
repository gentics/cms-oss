/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: CheckboxPartType.java,v 1.13 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Objects;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 31 - Checkbox
 * <p>
 * This is a simple parttype implementation which renders checkboxes with the values '0' or '1' if checked.
 * </p> 
 */
public class CheckboxPartType extends AbstractPartType implements PartType {

	/**
	 * Serial version uid
	 */
	private static final long serialVersionUID = 6368429370721365947L;

	/**
	 * flag to mark whether the box is checked
	 */
	private boolean checked;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public CheckboxPartType(Value value) throws NodeException {
		super(value);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#setValue(com.gentics.contentnode.object.Value)
	 */
	public void setValue(Value value) throws NodeException {
		super.setValue(value);
		String text = value.getValueText();

		this.checked = ObjectTransformer.getInt(text, 0) == 1;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/**
	 * Returns true if the checkbox is required and not filled in
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}
		return !checked;
	}
    
	/**
	 * Check whether the checkbox is checked or not
	 * @return true when the checkbox is checked, false if not
	 */
	public boolean isChecked() {
		return checked;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		return checked ? "1" : "0";
	}
    
	public Object get(String key) {
		if ("checked".equals(key)) {
			return new Boolean(isChecked());
		}
		return null;
	}

	/**
	 * Set checked status
	 * @param checked checked status
	 * @throws NodeException
	 */
	public void setChecked(boolean checked) throws NodeException {
		Value value = getValueObject();
		value.setValueText(checked ? "1" : "0");
		this.checked = checked;
	}

	@Override
	public Type getPropertyType() {
		return Type.BOOLEAN;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setBooleanValue(isChecked());
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Boolean booleanValue = property.getBooleanValue();
		if (booleanValue == null) {
			return;
		}
		setChecked(booleanValue);
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof CheckboxPartType) {
			return Objects.equals(isChecked(), ((CheckboxPartType) other).isChecked());
		} else {
			return false;
		}
	}
}
