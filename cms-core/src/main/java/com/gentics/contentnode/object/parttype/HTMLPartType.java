/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: HTMLPartType.java,v 1.3 2007-03-19 07:57:35 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 3 (HTML)
 */
public class HTMLPartType extends TextPartType {

	/**
	 * Create instance of this parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public HTMLPartType(Value value) throws NodeException {
		super(value, TextPartType.REPLACENL_NONE);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isLiveEditorCapable()
	 */
	public boolean isLiveEditorCapable() throws NodeException {
		return true;
	}

	@Override
	public Type getPropertyType() {
		return Type.RICHTEXT;
	}
}
