/*
 * @date 02.08.2004
 * @user erwin
 * @version $Id: PropertySetter.java,v 1.17 2008-08-20 14:01:18 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.lib.expressionparser.AssignmentException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * PropertySetter is an extension of {@link PropertyResolver} that also can
 * change properties by paths based on a given {@link Resolvable}. The setting
 * process resolves the given property path up to the next to last path part.
 * When the result of this is a {@link Changeable} or a Collection of
 * Changeables, the objects are modified by setting the property given by the
 * last part of the property path.<br> Example: let the base object be a
 * {@link Resolvable} representing a user. The call to
 * {@link #setProperty(String, Object)} with ("organisation.employees.email",
 * "a.b@foo.com") would first resolve "organisation.employees" to a
 * {@link Collection} of {@link Changeable}s representing all employees of the
 * user's organisation and would then change the email addresses by calling
 * {@link Changeable#setProperty(String, Object)} with ("email", "a.b@foo.com")
 * for each.
 */
public class PropertySetter extends PropertyResolver {

	/**
	 * Create a propertySetter that resolves and sets properties based on the given Resolvable.
	 * @param baseObject base object
	 */
	public PropertySetter(Resolvable baseObject) {
		super(baseObject);
	}

	/**
	 * Set the property (properties) given by the path to the given value
	 * @param path path to a property/properties of an object/objects
	 * @param value new value to set
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 * @throws InsufficientPrivilegesException when setting the value is not allowed
	 */
	public void setProperty(String path, Object value) throws UnknownPropertyException,
				InsufficientPrivilegesException {
		if (path == null) {
			throw new UnknownPropertyException("No null-values allowed in Path");
		}
		int lastIdx = path.lastIndexOf('.');
		String objPath = path.substring(0, lastIdx);
		String propertyName = path.substring(lastIdx + 1);
		Stack resolvableStack = new Stack();

		try {
			List objects = resolvePath(m_startObject, objPath, false, PropertyModificationListener.class, false, resolvableStack);
			Object obj = objects != null && objects.size() > 0 ? ((PropertyPathEntry) objects.get(objects.size() - 1)).getEntry() : null;

			if (obj == null) {
				StringBuffer resolved = new StringBuffer();

				for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
					PropertyPathEntry pathEntry = (PropertyPathEntry) iterator.next();

					resolved.append("\t- ").append(pathEntry.getPath()).append(": ");
					Object entry = pathEntry.getEntry();

					if (entry == null) {
						resolved.append("[null]");
					} else {
						resolved.append(entry.getClass().getName());
					}
					resolved.append("\n");
				}
				throw new UnknownPropertyException(
						"Cannot set Property {" + propertyName + "} for null-object resolved from {" + objPath + "}. Resolved so far:\n" + resolved.toString());
			}

			if (obj instanceof Changeable) {
				// the path resolved to a single changeable, so set the property
				// for it
				((Changeable) obj).setProperty(propertyName, value);
				notifyListeners(objects, propertyName, value);
			} else if (obj instanceof Collection) {
				// the path resolved to a collection of objects, change the
				// property
				// for all Changeables within it
				for (Iterator iter = ((Collection) obj).iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					if (element instanceof Changeable) {
						((Changeable) element).setProperty(propertyName, value);
						notifyListeners(objects, propertyName, value);
					} else if (element != null) {
						// one of the objects in the collection is not
						// Changeable,
						// create a warning
						NodeLogger.getLogger(getClass()).warn(
								"one of the objects in the collection resolved by '" + objPath + "' was not Changeable, but of class '" + element.getClass().getName()
								+ "' instead. Property '" + propertyName + "' was not set for this object");
					}
				}
			} else {
				throw new UnknownPropertyException("Cannot set Property {" + propertyName + "} for non-Changeable object (" + objPath + ")");
			}
		} finally {
			while (!resolvableStack.isEmpty()) {
				ResolverContextHandler.pop(resolvableStack.pop());
			}
		}
	}

	/**
	 * Add the given property to the properties resolved by the given path.
	 * Duplicate values will not be allowed (i.e. the value will not be added if
	 * it already is present in the property)
	 * @param path property path
	 * @param value value to add
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 * @throws InsufficientPrivilegesException when setting the property is not allowed
	 */
	public void addToProperty(String path, Object value) throws UnknownPropertyException,
				InsufficientPrivilegesException {
		addToProperty(path, value, false);
	}

	/**
	 * Add the given property to the properties resolved by the given path.
	 * @param path property path
	 * @param value value to add
	 * @param allowDuplicates true when duplicates are allowed, false if not
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 * @throws InsufficientPrivilegesException when setting the property is not allowed
	 */
	public void addToProperty(String path, Object value, boolean allowDuplicates) throws UnknownPropertyException, InsufficientPrivilegesException {
		if (path == null) {
			throw new UnknownPropertyException("No null-values allowed in Path");
		}

		int lastIdx = path.lastIndexOf('.');
		String objPath = lastIdx >= 0 ? path.substring(0, lastIdx) : "";
		String propertyName = lastIdx >= 0 ? path.substring(lastIdx + 1) : path;

		Stack resolvableStack = new Stack();

		try {
			List objects = resolvePath(m_startObject, objPath, false, PropertyModificationListener.class, false, resolvableStack);
			Object obj = objects != null && objects.size() > 0 ? ((PropertyPathEntry) objects.get(objects.size() - 1)).getEntry() : null;

			if (obj == null) {
				throw new UnknownPropertyException("Cannot modify Property for null-object");
			}
			if (obj instanceof Changeable) {
				// the path resolved to a single changeable, so modify the property for it
				Changeable changeable = (Changeable) obj;

				try {
					ResolverContextHandler.push(changeable);
					Object newValues = addValue(changeable.getProperty(propertyName), value, allowDuplicates);

					changeable.setProperty(propertyName, newValues);
					notifyListeners(objects, propertyName, newValues);
				} finally {
					ResolverContextHandler.pop(obj);
				}
			} else if (obj instanceof Collection) {
				// the path resolved to a collection of objects, change the property
				// for all Changeables within it
				for (Iterator iter = ((Collection) obj).iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					if (element instanceof Changeable) {
						Changeable changeable = (Changeable) element;

						try {
							ResolverContextHandler.push(changeable);
							Object newValues = addValue(changeable.getProperty(propertyName), value, allowDuplicates);

							changeable.setProperty(propertyName, newValues);
							notifyListeners(objects, propertyName, newValues);
						} finally {
							ResolverContextHandler.pop(obj);
						}
					} else if (element != null) {
						// one of the objects in the collection is not Changeable,
						// create a warning
						NodeLogger.getLogger(getClass()).warn(
								"one of the objects in the collection resolved by '" + objPath + "' was not Changeable, but of class '" + element.getClass().getName()
								+ "' instead. Property '" + propertyName + "' was not modified for this object");
					}
				}
			} else {
				throw new UnknownPropertyException("Cannot set Property for non-Changeable object (" + path + ")");
			}
		} finally {
			while (!resolvableStack.isEmpty()) {
				ResolverContextHandler.pop(resolvableStack.pop());
			}
		}
	}

	/**
	 * Remove the given value from the properties resolved by the given path.
	 * @param path path of the property
	 * @param value value to remove
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 * @throws InsufficientPrivilegesException when changing the property is not allowed
	 */
	public void removeFromProperty(String path, Object value) throws UnknownPropertyException, InsufficientPrivilegesException {
		if (path == null) {
			throw new UnknownPropertyException("No null-values allowed in Path");
		}

		int lastIdx = path.lastIndexOf('.');
		String objPath = path.substring(0, lastIdx);
		String propertyName = path.substring(lastIdx + 1);
		Stack resolvableStack = new Stack();

		try {
			List objects = resolvePath(m_startObject, objPath, false, PropertyModificationListener.class, false, resolvableStack);
			Object obj = objects != null && objects.size() > 0 ? ((PropertyPathEntry) objects.get(objects.size() - 1)).getEntry() : null;

			if (obj == null) {
				throw new UnknownPropertyException("Cannot modify Property for null-object");
			}
			if (obj instanceof Changeable) {
				// the path resolved to a single changeable, so modify the property for it
				Changeable changeable = (Changeable) obj;

				try {
					ResolverContextHandler.push(changeable);
					Object newValues = removeValue(changeable.getProperty(propertyName), value);

					changeable.setProperty(propertyName, newValues);
					notifyListeners(objects, propertyName, newValues);
				} finally {
					ResolverContextHandler.pop(obj);
				}
			} else if (obj instanceof Collection) {
				// the path resolved to a collection of objects, change the property
				// for all Changeables within it
				for (Iterator iter = ((Collection) obj).iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					if (element instanceof Changeable) {
						Changeable changeable = (Changeable) element;

						try {
							ResolverContextHandler.push(changeable);
							Object newValues = removeValue(changeable.getProperty(propertyName), value);

							changeable.setProperty(propertyName, newValues);
							notifyListeners(objects, propertyName, newValues);
						} finally {
							ResolverContextHandler.pop(obj);
						}
					} else if (element != null) {
						// one of the objects in the collection is not Changeable,
						// create a warning
						NodeLogger.getLogger(getClass()).warn(
								"one of the objects in the collection resolved by '" + objPath + "' was not Changeable, but of class '" + element.getClass().getName()
								+ "' instead. Property '" + propertyName + "' was not modified for this object");
					}
				}
			} else {
				throw new UnknownPropertyException("Cannot set Property for non-Changeable object (" + path + ")");
			}
		} finally {
			while (!resolvableStack.isEmpty()) {
				ResolverContextHandler.pop(resolvableStack.pop());
			}
		}
	}

	/**
	 * Add the new value to the old values and return a collection
	 * @param oldValues old values as map, collection or single value
	 * @param newValue new value
	 * @param allowDuplicates true when duplicates are allowed in the value collection/map, false if not
	 * @return a collection/map of new values
	 */
	private Object addValue(Object oldValues, Object newValue, boolean allowDuplicates) {
		// add the new value to the values only when it is not yet set
		if (oldValues instanceof Collection) {
			Collection oldValuesCol = new Vector((Collection) oldValues);

			// add the new value to the collection
			if (newValue instanceof Collection) {
				if (!allowDuplicates) {
					oldValuesCol.removeAll((Collection) newValue);
				}
				oldValuesCol.addAll((Collection) newValue);
			} else if (newValue instanceof Map) {
				if (!allowDuplicates) {
					oldValuesCol.removeAll(((Map) newValue).keySet());
				}
				oldValuesCol.addAll(((Map) newValue).keySet());
			} else if (newValue != null) {
				if (!allowDuplicates) {
					// duplicates are not allowed, so remove any existing equal
					// object from the collection first
					oldValuesCol.remove(newValue);
				}
				// add the new value
				oldValuesCol.add(newValue);
			}
			return oldValuesCol;
		} else if (oldValues instanceof Map) {
			Map oldValuesMap = new HashMap((Map) oldValues);

			// add the new value to the map
			if (newValue instanceof Map) {
				// add a map to a map
				oldValuesMap.putAll((Map) newValue);
			} else if (newValue instanceof Collection) {
				// add the collection to the map (all entries as keys and values)
				for (Iterator iter = ((Collection) newValue).iterator(); iter.hasNext();) {
					Object entry = (Object) iter.next();

					oldValuesMap.put(entry, entry);
				}
			} else if (newValue != null) {
				oldValuesMap.put(newValue, newValue);
			}
			return oldValuesMap;
		} else {
			// create a collection holding the old and new value
			Collection newValueCollection = new Vector();

			if (oldValues != null) {
				if (allowDuplicates || (newValue instanceof Collection && !((Collection) newValue).contains(oldValues)) || (!oldValues.equals(newValue))) {
					// preserve the old value when duplicates are allowed or the
					// objects are different from each other
					newValueCollection.add(oldValues);
				}
			}
			if (newValue instanceof Collection) {
				newValueCollection.addAll((Collection) newValue);
			} else if (newValue != null) {
				// add the new value
				newValueCollection.add(newValue);
			}
			return newValueCollection;
		}
	}

	/**
	 * Remove the given value from the collection of old values
	 * @param oldValues old values (might be a collection or a single value)
	 * @param toRemove value to remove
	 * @return collection of objects after toRemove has been removed
	 */
	private Object removeValue(Object oldValues, Object toRemove) {
		if (oldValues instanceof Collection) {
			// always generate a new collection, holding all values of the existing
			Collection oldValuesCol = new Vector((Collection) oldValues);

			if (toRemove instanceof Collection) {
				removeAll(oldValuesCol, (Collection) toRemove);
			} else if (toRemove instanceof Map) {
				removeAll(oldValuesCol, ((Map) toRemove).keySet());
			} else {
				remove(oldValuesCol, toRemove);
			}
			return oldValuesCol;
		} else if (oldValues instanceof Map) {
			Map oldValuesMap = new LinkedHashMap((Map) oldValues);

			if (toRemove instanceof Map) {
				for (Iterator iter = ((Map) toRemove).keySet().iterator(); iter.hasNext();) {
					Object key = (Object) iter.next();

					oldValuesMap.remove(key);
				}
			} else if (toRemove instanceof Collection) {
				for (Iterator iter = ((Collection) toRemove).iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					oldValuesMap.remove(element);
				}
			} else if (toRemove != null) {
				oldValuesMap.remove(toRemove);
			}
			return oldValuesMap;
		} else if (oldValues != null) {
			if (toRemove instanceof Collection) {
				if (((Collection) toRemove).contains(oldValues)) {
					return new Vector();
				} else {
					Vector newCol = new Vector();

					newCol.add(oldValues);
					return newCol;
				}
			} else if (toRemove instanceof Map) {
				if (((Map) toRemove).keySet().contains(oldValues)) {
					return new Vector();
				} else {
					Vector newCol = new Vector();

					newCol.add(oldValues);
					return newCol;
				}
			} else {
				if (oldValues.equals(toRemove)) {
					return new Vector();
				} else {
					Vector newCol = new Vector();

					newCol.add(oldValues);
					return newCol;
				}
			}
		} else {
			return new Vector();
		}
	}

	/**
	 * Perform the given assignment expression
	 * @param expression assignment expression
	 * @throws ExpressionParserException when assignment fails
	 */
	public void performAssignment(Expression expression) throws ExpressionParserException {
		if (!(expression instanceof EvaluableExpression)) {
			throw new AssignmentException("The given expression must be an assignment");
		} else {
			try {
				RuntimeProfiler.beginMark(ComponentsConstants.EXPRESSIONPARSER_ASSIGNMENT);
				Object result = ((EvaluableExpression) expression).evaluate(new ExpressionQueryRequest(this, null), ExpressionEvaluator.OBJECTTYPE_ASSIGNMENT);

				// when the result object is not ASSIGNMENT, no assignment was done
				if (result != ExpressionParser.ASSIGNMENT) {
					NodeLogger.getNodeLogger(getClass()).warn("No assignment performed for expression {" + expression.getExpressionString() + "}");
				}
			} finally {
				RuntimeProfiler.endMark(ComponentsConstants.EXPRESSIONPARSER_ASSIGNMENT);
			}
		}
	}

	/**
	 * Notify all {@link PropertyModificationListener} about modified properties
	 * @param listeners list of PropertyModificationListeners
	 * @param propertyName name of the modified property
	 * @param value new value
	 */
	private void notifyListeners(List listeners, String propertyName, Object value) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			PropertyPathEntry element = (PropertyPathEntry) iter.next();
			Object entry = element.getEntry();

			if (entry instanceof PropertyModificationListener) {
				((PropertyModificationListener) entry).propertyModified(element.getRemainingPath(), propertyName, value);
			}
		}
	}

	/**
	 * Remove the given object from the given collection. Do compares using the
	 * equals() method in both directions.
	 * @param toModify collection to modify
	 * @param toRemove object to remove
	 */
	protected void remove(Collection toModify, Object toRemove) {
		if (toRemove == null || toModify == null || toModify.isEmpty()) {
			return;
		}

		for (Iterator iterator = toModify.iterator(); iterator.hasNext();) {
			Object element = (Object) iterator.next();

			if (element != null && (element.equals(toRemove) || toRemove.equals(element))) {
				iterator.remove();
			}
		}
	}

	/**
	 * Remove all objects in the collection from the given collection. Do
	 * compares using the equals() method in both directions.
	 * @param toModify collection to modify
	 * @param toRemove collection of objects to remove
	 */
	protected void removeAll(Collection toModify, Collection toRemove) {
		if (toModify == null || toRemove == null || toModify.isEmpty() || toRemove.isEmpty()) {
			return;
		}

		for (Iterator iterator = toRemove.iterator(); iterator.hasNext();) {
			Object objectToRemove = (Object) iterator.next();

			remove(toModify, objectToRemove);
		}
	}
}
