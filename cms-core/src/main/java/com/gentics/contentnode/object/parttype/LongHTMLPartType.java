/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: LongHTMLPartType.java,v 1.3 2010-04-30 08:39:39 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 21 - HTML (long)
 */
public class LongHTMLPartType extends TextPartType {

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public LongHTMLPartType(Value value) throws NodeException {
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
