package com.gentics.contentnode.rest.util;

import java.lang.reflect.Method;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Filter implementation to match a property against a reference Integer
 */
public class IntFilter extends AbstractNodeObjectFilter {

	/**
	 * Reference result.
	 */
	protected Integer referenceValue;

	/**
	 * Getter method to get the value from the NodeObject
	 */
	protected Method getter;

	/**
	 * Create an instance of the IntFilter
	 * @param referenceValue reference value
	 * @param getter getter
	 */
	public IntFilter(Integer referenceValue, Method getter) {
		this.referenceValue = referenceValue;
		this.getter = getter;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		try {
			return ObjectTransformer.getInteger(getter.invoke(object), 0).equals(ObjectTransformer.getInteger(referenceValue, 0));
		} catch (Exception e) {
			throw new NodeException("Error while matching {" + object + "}", e);
		}
	}
}
