/*
 * @author Stefan Hepp
 * @date 12.12.2005
 * @version $Id: NodePreferences.java,v 1.4.10.1 2011-02-04 13:17:26 norbert Exp $
 */
package com.gentics.contentnode.etc;

import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;

/**
 * This interface provides preferences, feature-settings and property-values.
 * The preferences are usually retrieved by {@link NodeConfig}, where you can
 * either get the default preferences or the preferences for a given user.
 * Preferences can be readonly, or changeable, depending on the implementation.
 * Changeable preferences may be stored back to the configuration resource.
 *
 * Usually, preferences can be given default-preferences during initialization.
 * If a property or feature is not set, the default-preferences should be used to get its value.
 *
 * Feature- and property-keynames must be mutually exclusive to ensure compatiblity
 * to flat property-files.
 */
public interface NodePreferences {
	/**
	 * Build the feature name, like used in the properties
	 * @param name name
	 * @return full feature name
	 */
	static String buildFeatureName(String name) {
		// Features in do=24 are prefixed with 'contentnode.feature.'
		return "contentnode.feature." + name;
	}

	/**
	 * Check, if a feature is activated. Feature names should be .node 36
	 * compatible, for new features, a dot-separated syntax should be used to
	 * group features.
	 *
	 * @param name the name of the feature.
	 * @return true if the feature is set and activated, else false.
	 */
	default boolean getFeature(String name) {
		return ObjectTransformer.getBoolean(getProperty(buildFeatureName(name)), false);
	}

	/**
	 * Check if a feature is activated generally or for the given node
	 * @param name name of the feature
	 * @param node node
	 * @return true if the feature is activated, false if not
	 */
	default boolean getFeature(String name, Node node) {
		String configKey = buildFeatureName(name);
		// get the value of the feature
		Object featureValue = getProperty(configKey);
		Map<String, String> valueMap = getPropertyMap(configKey);

		if (ObjectTransformer.getBoolean(featureValue, false)) {
			// if the value is Boolean.TRUE, the feature is turned on generally
			return true;
		} else if (valueMap != null && node != null) {
			// the feature is configured per node, check whether it is turned on for the given node
			return ObjectTransformer.getBoolean(valueMap.get(ObjectTransformer.getString(node.getId(), null)), false);
		} else {
			// feature not found, or set to false or configured per node, but no
			// node given
			return false;
		}
	}

	/**
	 * Check whether a feature is activated (generally).
	 * @param feature feature to check
	 * @return true if the feature is activated, false if not
	 */
	default boolean isFeature(Feature feature) {
		// the features in the enum must have the same names (only uppercase)
		return getFeature(feature.toString().toLowerCase());
	}

	/**
	 * Check whether a feature is activated generally or for the given node
	 * @param feature feature to check
	 * @param node node for which the feature shall be checked
	 * @return true if the feature is activated generally or for the node, false if not
	 */
	default boolean isFeature(Feature feature, Node node) {
		if (feature.isPerNode() && node != null) {
			// when the feature can be activated for the node with the new mechanism, we check for the node
			try {
				return node.getFeatures().contains(feature);
			} catch (NodeException e) {
				return false;
			}
		} else {
			// the features in the enum must have the same names (only uppercase)
			return getFeature(feature.toString().toLowerCase(), node);
		}
	}

	/**
	 * Get a string-value of a property. If the property contains a list of
	 * values, only the first value is returned.
	 *
	 * @param name the name of the property.
	 * @return the value of the property, or null if not set.
	 */
	String getProperty(String name);

	/**
	 * Get a list of values for a property. If the property is only a single-
	 * value property, a list with only one entry will be returned. The
	 * order of the values depends on the implementation of this interface
	 * and should be configurable in the underlying property-resource.
	 *
	 * @param name the name of the property.
	 * @return the values of the property.
	 */
	String[] getProperties(String name);

	/**
	 * Set the value of a feature. If the feature is not changable, nothing is done.
	 *
	 * @param name the name of a feature
	 * @param value the new value for this feature.
	 */
	default void setFeature(String name, boolean value) {
		setProperty(buildFeatureName(name), Boolean.toString(value));
	}

	/**
	 * Activate/deactivate the feature
	 * @param feature feature
	 * @param value true to activate, false to deactivate
	 */
	default void setFeature(Feature feature, boolean value) {
		if (feature != null) {
			setFeature(feature.getName(), value);
		}
	}

	/**
	 * Set the new value of a property.
	 *
	 * @param name
	 * @param value
	 */
	void setProperty(String name, String value);

	/**
	 * Set new values for a property
	 * @param name property name
	 * @param values new values
	 */
	void setProperty(String name, String[] values);

	/**
	 * Unset a feature and use its defaultvalue if it has any.
	 * @param name name of the feature to unset.
	 */
	default void unsetFeature(String name) {
		unsetProperty(buildFeatureName(name));
	}

	/**
	 * Unset a property and use its defaultvalue if it has any.
	 * @param name name of the feature to unset.
	 */
	void unsetProperty(String name);

	/**
	 * If a property with this name exists and is a correct array,
	 * returns a property as map 
	 * @param string
	 */
	<T> Map<String, T> getPropertyMap(String name);

	/**
	 * Set the given map as property
	 * @param name name of the property
	 * @param map property map
	 * @throws NodeException 
	 */
	void setPropertyMap(String name, Map<String, String> map) throws NodeException;

	/**
	 * Get the property with this name as object, depending on the original
	 * type. This supports Strings (for single properties), Lists (for
	 * properties that are arrays) and Maps (for associative arrays) and
	 * embedded objects.
	 * @param name name of the property
	 * @return property object (either String, List or Map), List and Map values
	 *         may also be Strings, Lists or Maps
	 */
	<T> T getPropertyObject(String name);

	/**
	 * Get the node-specific property with this name as object.
	 * The property with given name is supposed to be a map of properties, where the keys are one of
	 * <ul>
	 * <li>local node ID</li>
	 * <li>global node ID</li>
	 * <li>node name</li>
	 * </ul>
	 * 
	 * @param node node for which to get the property
	 * @param name property name
	 * @return value
	 * @throws NodeException
	 */
	<T> T getPropertyObject(Node node, String name) throws NodeException;

	/**
	 * Return the configuration as nested maps (if supported)
	 * @return config as nested maps
	 */
	Map<String, Object> toMap();
}
