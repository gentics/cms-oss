package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.lib.log.NodeLogger;

/**
 * Property implementation for NodeObject instances
 *
 * @param <T> NodeObject class
 */
public class NodeObjectProperty<T extends NodeObject> extends AbstractProperty {
	/**
	 * Getter function
	 */
	protected BiFunction<T, String, Object> getter;

	/**
	 * Create instance of the property
	 * @param getter getter implementation
	 * @param dependsOn meta attributes, the resolved attribute depends on
	 */
	public NodeObjectProperty(BiFunction<T, String, Object> getter, String...dependsOn) {
		super(dependsOn);
		this.getter = getter;
	}

	/**
	 * Get the property value for the given object
	 * @param object object
	 * @param key property key
	 * @return property value
	 */
	public Object get(T object, String key) {
		if (object == null || key == null) {
			return null;
		}
		try {
			return getter.apply(object, key);
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(object.getClass()).error(String.format("Error while resolving %d for %d", key, object), e);
			return null;
		}
	}
}
