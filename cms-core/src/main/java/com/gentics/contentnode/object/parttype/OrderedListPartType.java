/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: OrderedListPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 17 - List (ordered)
 */
public class OrderedListPartType extends ListPartType {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -6131876322873608971L;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public OrderedListPartType(Value value) throws NodeException {
		super(value, ListPartType.TYPE_ORDERED);
	}

	@Override
	public Type getPropertyType() {
		return Type.ORDEREDLIST;
	}
}
