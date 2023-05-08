/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: StaticSelectPartType.java,v 1.12 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * The staticselect parttype renderes simple selections which can be selected from a list of preconfigured values.
 */
public abstract class StaticSelectPartType extends AbstractPartType {

	/**
	 * Values of the select parttype
	 */
	private Map values;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @param values values to select
	 * @throws NodeException
	 */
	public StaticSelectPartType(Value value, Map values) throws NodeException {
		super(value);
		this.values = values == null ? new HashMap() : new HashMap(values);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isEmpty()
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}
        
		String text = getValueObject().getValueText();

		if (text == null) {
			return true;
		} else {
			return "".equals(text);
		}
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		return getValueObject().getValueText();
	}

	@Override
	public Type getPropertyType() {
		return Type.STRING;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setStringValue(getValueObject().getValueText());
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		getValueObject().setValueText(property.getStringValue());
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof StaticSelectPartType) {
			return Objects.equals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}
}
