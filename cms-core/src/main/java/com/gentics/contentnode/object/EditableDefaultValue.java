package com.gentics.contentnode.object;

/**
 * Interface for editable default values, which are embedded into editable variants of parts.
 * The only purpose of this interface is to allow the editable part to connect itself with the editable default value
 */
public interface EditableDefaultValue {
	/**
	 * Set the editable part (stored in the instance of the EditableDefaultValue) to the given part, which should also be editable.
	 * @param part editable part
	 */
	void setEditablePart(Part part);
}
