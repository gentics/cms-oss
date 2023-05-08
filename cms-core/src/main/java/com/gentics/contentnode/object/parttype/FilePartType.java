/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: FilePartType.java,v 1.11 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Objects;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 22 - File (localpath)
 * <p>
 * The file parttype creates a static url to a local file.
 * </p>
 */
public class FilePartType extends AbstractPartType {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 5296989856436391971L;

	/**
	 * local url
	 */
	private String url;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public FilePartType(Value value) throws NodeException {
		super(value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isEmpty()
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}        
		if (url == null) {
			return true;
		} else {
			return "".equals(url);
		}
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#setValue(com.gentics.contentnode.object.Value)
	 */
	public void setValue(Value value) throws NodeException {
		super.setValue(value);
		this.url = generateUrl(value.getValueText());
	}

	/**
	 * Generate the url
	 * @param text
	 * @return generated url
	 */
	private String generateUrl(String text) {
		if (text != null && text.length() > 2) {
			return "file://" + text.substring(2).replace('\\', '/');
		} else {
			return "";
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
		return url;
	}

	@Override
	public Type getPropertyType() {
		return Type.LOCALFILE;
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
		if (other instanceof FilePartType) {
			return Objects.deepEquals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}
}
