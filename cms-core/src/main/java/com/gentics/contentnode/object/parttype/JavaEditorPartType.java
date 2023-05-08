/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: JavaEditorPartType.java,v 1.2 2007-02-07 12:14:05 clemens Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 26 - Java Editor
 */
public class JavaEditorPartType extends TextPartType {

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public JavaEditorPartType(Value value) throws NodeException {
		super(value, TextPartType.REPLACENL_EXTENDEDNL2BR);
	}

	@Override
	public Type getPropertyType() {
		return Type.RICHTEXT;
	}
}
