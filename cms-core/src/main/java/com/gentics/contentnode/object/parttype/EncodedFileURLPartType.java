/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: EncodedFileURLPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 14 - URL (file, encoded)
 */
public class EncodedFileURLPartType extends UrlPartType {

	/**
	 * Create instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public EncodedFileURLPartType(Value value) throws NodeException {
		super(value, UrlPartType.TARGET_FILE, true);
	}

	@Override
	public Type getPropertyType() {
		return Type.UNKNOWN;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}
}
