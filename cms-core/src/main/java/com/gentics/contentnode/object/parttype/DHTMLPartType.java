/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: DHTMLPartType.java,v 1.3 2007-03-19 07:57:35 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 27 - DHTML Editor
 */
public class DHTMLPartType extends TextPartType {

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public DHTMLPartType(Value value) throws NodeException {
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
