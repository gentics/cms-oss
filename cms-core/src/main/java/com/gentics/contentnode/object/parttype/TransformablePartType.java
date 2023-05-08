package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * Interface for PartType implementations, that can be transformed to/from REST Models
 */
public interface TransformablePartType {
	/**
	 * Get the property type for the partType
	 * @return property type
	 */
	Type getPropertyType();

	/**
	 * Transform the value to the REST Model
	 * @return Property
	 * @throws NodeException
	 */
	Property toProperty() throws NodeException;

	/**
	 * Fill the value with data from the given REST Model
	 * @param property property
	 * @throws NodeException
	 */
	void fromProperty(Property property) throws NodeException;
}
