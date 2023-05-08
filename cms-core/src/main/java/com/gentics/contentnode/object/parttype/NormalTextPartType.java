/*
 * @author norbert
 * @date 03.01.2007
 * @version $Id: NormalTextPartType.java,v 1.3 2007-03-19 07:57:35 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 1 (for normal texts)
 */
public class NormalTextPartType extends TextPartType {

	/**
	 * Create instance of this parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public NormalTextPartType(Value value) throws NodeException {
		super(value, TextPartType.REPLACENL_NL2BR);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isLiveEditorCapable()
	 */
	public boolean isLiveEditorCapable() throws NodeException {
		return true;
	}

	@Override
	public Type getPropertyType() {
		return Type.STRING;
	}
}
