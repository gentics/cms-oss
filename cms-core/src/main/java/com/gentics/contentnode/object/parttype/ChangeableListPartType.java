/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: ChangeableListPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 15 - List
 */
public class ChangeableListPartType extends ListPartType {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 8812078458060808577L;

	/**
	 * Create an instance of the parttype
	 * @param value of the parttype
	 * @throws NodeException
	 */
	public ChangeableListPartType(Value value) throws NodeException {
		super(value, ListPartType.TYPE_CHANGEABLE);
	}

	/**
	 * Set the list to be ordered
	 * @throws NodeException
	 */
	public void setOrdered() throws NodeException {
		getValueObject().setInfo(1);
	}

	/**
	 * Set the list to be unordered
	 * @throws NodeException
	 */
	public void setUnordered() throws NodeException {
		getValueObject().setInfo(0);
	}

	/**
	 * Check whether the list is ordered
	 * @return true for ordered, false for unordered
	 */
	public boolean isOrdered() {
		return getValueObject().getInfo() > 0;
	}

	@Override
	public Type getPropertyType() {
		return Type.LIST;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		super.fillProperty(property);
		property.setBooleanValue(isOrdered());
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		super.fromProperty(property);
		getValueObject().setInfo(ObjectTransformer.getBoolean(property.getBooleanValue(), false) ? 1 : 0);
	}
}
