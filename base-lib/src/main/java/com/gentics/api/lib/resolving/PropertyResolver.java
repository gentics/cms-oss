/*
 * @date 16.06.2003
 * @version $Id: PropertyResolver.java,v 1.12 2009-12-16 16:12:07 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * Instances of class PropertyResolver can resolve properties by paths based on
 * a single Resolvable object. The resolving process is done iteratively for
 * each part of the property path as long as each property is a Resolvable
 * itself.<br>Example: Let the base object be a Resolvable representing a
 * User. A call to {@link #resolve(String)} with "organisation.name" would first
 * call {@link Resolvable#get(String)} with "organisation" on the base object.
 * If and only if the return value is again a Resolvable (representing the
 * organisation of the user) the resolving process would continue by calling
 * {@link Resolvable#get(String)} with "name" on the organisation object. The
 * return value of this call would be the final value, since "name" is the last
 * part of the property path.<br><br>The resolving process is also capable
 * of resolving multivalue properties:<br> Let's expand the example above in
 * the following way: The base object again is the user, and the call to
 * {@link #resolve(String)} is done with "organisation.employees.email".
 * Resolving of "organisation" gives a single {@link Resolvable} representing
 * the user's organisation. The property "employees" shall be a
 * {@link Collection} of {@link Resolvable}s representing all users of the
 * organisation. The resolving process would now continue by iterating over the
 * {@link Collection} and get the property "email" for each {@link Resolvable}.
 * So the final return value would be a {@link Collection} of all email
 * addresses of all users of the base user's organisation.<br><br> The
 * PropertyResolver has also static variants of the resolve methods that get another
 * base object to start resolving. If one of those methods is used, the default
 * base object is ignored and the given startObject is used instead.
 */
public class PropertyResolver implements Resolvable {
	protected Resolvable m_startObject;

	private static NodeLogger logger = NodeLogger.getNodeLogger(PropertyResolver.class);

	private PropertyResolver() {}

	/**
	 * create an instance of a PropertyResolver based on the given Resolvable
	 * @param startObject base resolvable object
	 */
	public PropertyResolver(Resolvable startObject) {
		this.m_startObject = startObject;
	}

	/**
	 * Resolve the given property path to a value (which also might be a
	 * Collection of values) or null.
	 * @param propertyPath property path
	 * @return the value of the property
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 */
	public Object resolve(String propertyPath) throws UnknownPropertyException {
		return resolve(propertyPath, false);
	}

	/**
	 * Resolve the given property path to a value (which also might be a
	 * Collection of values) or null, starting with the given startObject
	 * @param startObject start object
	 * @param propertyPath property path
	 * @return the value of the property
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 */
	public static Object resolve(Resolvable startObject, String propertyPath) throws UnknownPropertyException {
		return resolve(startObject, propertyPath, false);
	}

	/**
	 * Resolve the given propertypath into a list of instances of
	 * {@link PropertyPathEntry}.
	 * @param propertyPath property path to resolve
	 * @return list of PropertyPathEntries
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 */
	public List resolvePath(String propertyPath) throws UnknownPropertyException {
		return resolvePath(m_startObject, propertyPath, false, Object.class, true, null);
	}

	/**
	 * Resolve the given propertypath into a list of instances of
	 * {@link PropertyPathEntry}, starting with the given Resolvable
	 * @param startObject resolvable to start resolving
	 * @param propertyPath property path to resolve
	 * @return list of PropertyPathEntries
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 */
	public static List resolvePath(Resolvable startObject, String propertyPath) throws UnknownPropertyException {
		return resolvePath(startObject, propertyPath, false, Object.class, true, null);
	}

	/**
	 * Resolve the given path into a list of objects. The list will always
	 * contain the last part of the resolved objects and all other that
	 * implement the given class. <b>Important implementation notice</b>: When
	 * the flag cleanResolvableStack is set to false, an (empty) resolvableStack
	 * must be passed to this method and the caller must afterwards clean the
	 * resolvable stack by calling {@link ResolverContextHandler#pop(Object)}
	 * for all objects that were put onto this stack (in the order they are popped() from this stack).
	 * @param startObject start object
	 * @param propertyPath property path to resolve
	 * @param failIfUnresolvablePath true when the process shall fail if one
	 *        part of the path could not be resolved
	 * @param implementingClass interface or class all returned objects have to
	 *        implement, null for only returning the last object
	 * @param cleanResolvableStack flag to mark whether the resolvable context
	 *        shall be cleaned
	 * @param resolvableStack stack for resolvable objects
	 * @return list of objects resolved from the path. the last object in the
	 *         list is always the last part in the path
	 * @throws UnknownPropertyException and logs a warning
	 */
	protected static List resolvePath(Resolvable startObject, String propertyPath,
			boolean failIfUnresolvablePath, Class implementingClass, boolean cleanResolvableStack, Stack resolvableStack) throws UnknownPropertyException {
		if (resolvableStack == null) {
			resolvableStack = new Stack();
		}
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.PROPERTY_RESOLVER_RESOLVE, propertyPath);

			// path will hold all resolved entries that implement the given
			// class and the last entry
			Vector path = null;

			// currently resolved object
			Object ret = null;

			// mother of currently resolved object
			Object mother = null;

			/* If the Property is not null, try to validate the property */
			if (propertyPath != null) {
				// initialise values
				StringTokenizer parts = new StringTokenizer(propertyPath, ".", false);

				ret = startObject;
				path = new Vector(parts.countTokens());
				String nextToken = "";
				StringBuffer resolvedSoFar = new StringBuffer(propertyPath.length());

				if (implementingClass != null && ret != null && implementingClass.isAssignableFrom(ret.getClass())) {
					path.add(new PropertyPathEntry("", propertyPath, ret, mother));
				}

				// as long as there are more elements
				while (parts.hasMoreElements() && ret != null) {
					// get the next token
					nextToken = parts.nextToken();

					mother = ret;

					// if the current object is a resolvable object - starting
					// with
					// the first object
					if (ret instanceof Resolvable) {

						// cast to resolvable
						Resolvable r = (Resolvable) ret;

						// push the object onto the resolvercontexts
						ResolverContextHandler.push(r);
						resolvableStack.push(r);

						// check if this is really resolvable?
						if (r.canResolve()) {
							// resolve the next token
							ret = r.getProperty(nextToken);
							if (ret != null) {
								if (resolvedSoFar.length() > 0) {
									resolvedSoFar.append(".");
								}
								resolvedSoFar.append(nextToken);

								// add the current ret value to the token
								if (implementingClass != null && implementingClass.isAssignableFrom(ret.getClass()) && parts.hasMoreElements()) {
									path.add(new PropertyPathEntry(resolvedSoFar.toString(), propertyPath.substring(resolvedSoFar.length() + 1), ret, mother));
								}
							}
						} else {
							// set the ret to null
							ret = null;
						}
					} else if (ret instanceof Collection) {
						// the return value is a collection, continue resolving
						// with
						// every resolvable object
						Collection coll = (Collection) ret;
						Collection newCol = new NestedCollection();

						// loop through all objects in the collection
						for (Iterator i = coll.iterator(); i.hasNext();) {
							Object retPart = i.next();

							if (retPart instanceof Resolvable) {
								Resolvable r = (Resolvable) retPart;

								// push the object onto the resolvercontexts
								ResolverContextHandler.push(r);
								resolvableStack.push(r);

								Object prop = r.getProperty(nextToken);

								if (prop instanceof Collection) {
									newCol.addAll((Collection) prop);
								} else if (prop != null) {
									newCol.add(prop);
								}
							}
						}

						ret = newCol;
						if (ret != null) {
							if (resolvedSoFar.length() > 0) {
								resolvedSoFar.append(".");
							}
							resolvedSoFar.append(nextToken);

							// add the current ret value to the token
							if (implementingClass != null && implementingClass.isAssignableFrom(ret.getClass()) && parts.hasMoreElements()) {
								path.add(new PropertyPathEntry(resolvedSoFar.toString(), propertyPath.substring(resolvedSoFar.length() + 1), ret, mother));
							}
						}

					} else {
						// this is now not a valid resolvable objecct --> return
						// null, not an exception
						ret = null;
						UnknownPropertyException e = new UnknownPropertyException(
								"Failed to resolve {" + propertyPath + "}: could not resolve Property {" + nextToken + "} for object {" + resolvedSoFar.toString()
								+ "}");

						if (failIfUnresolvablePath) {
							throw e;
						} else {
							logger.debug(e);
						}
					}
				}

				if (parts.hasMoreElements() && ret == null) {
					UnknownPropertyException e = new UnknownPropertyException(
							"Failed to resolve {" + propertyPath + "}: could not resolve Property {" + nextToken + "} for object {" + resolvedSoFar.toString() + "}");

					if (failIfUnresolvablePath) {
						throw e;
					} else {
						logger.debug(e);
					}
				}
				path.add(new PropertyPathEntry(propertyPath, "", ret, mother));
			}
			return path;
		} finally {
			// don't forget to remove all objects from the ResolverContexts that were pushed
			while (cleanResolvableStack && !resolvableStack.isEmpty()) {
				ResolverContextHandler.pop(resolvableStack.pop());
			}

			RuntimeProfiler.endMark(ComponentsConstants.PROPERTY_RESOLVER_RESOLVE, propertyPath);
		}
	}

	/**
	 * Resolve the given property path to a value (which also might be a
	 * Collection of values) or null.
	 * @param propertyPath property path
	 * @param failIfUnresolvablePath true when the resolving shall fail with an
	 *        UnknownPropertyException, when a part of the property path (not
	 *        the last) could not be resolved. otherwise, the method would just
	 *        return null in such a case
	 * @return the value of the property
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 */
	public Object resolve(String propertyPath, boolean failIfUnresolvablePath) throws UnknownPropertyException {
		return resolve(m_startObject, propertyPath, failIfUnresolvablePath);
	}

	/**
	 * Resolve the given property path to a value (which also might be a
	 * Collection of values) or null, starting with the given startObject.
	 * @param startObject start object
	 * @param propertyPath property path
	 * @param failIfUnresolvablePath true when the resolving shall fail with an
	 *        UnknownPropertyException, when a part of the property path (not
	 *        the last) could not be resolved. otherwise, the method would just
	 *        return null in such a case
	 * @return the value of the property
	 * @throws UnknownPropertyException when the property path cannot be fully resolved
	 */
	public static Object resolve(Resolvable startObject, String propertyPath,
			boolean failIfUnresolvablePath) throws UnknownPropertyException {
		List path = resolvePath(startObject, propertyPath, failIfUnresolvablePath, null, true, null);

		return path != null && path.size() > 0 ? ((PropertyPathEntry) path.get(path.size() - 1)).getEntry() : null;
	}

	/**
	 * Get a property from the base object.
	 * @param key property key
	 * @return property of the start object
	 */
	public Object get(String key) {
		return m_startObject != null ? m_startObject.getProperty(key) : null;
	}

	/**
	 * Determine whether this resolver (the base object) can resolve right now
	 * or not
	 * @return true when resolving is possible, false if not
	 */
	public boolean canResolve() {
		return m_startObject != null ? m_startObject.canResolve() : false;
	}

	/**
	 * Get a property from the start object.
	 * @param key property key
	 * @return property from the start object
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/**
	 * Inner class to return property path entries
	 */
	public static class PropertyPathEntry {

		/**
		 * The part of the property path that resolved to this entry
		 */
		private String path;

		/**
		 * Remaining part of the path
		 */
		private String remainingPath;

		/**
		 * name of the resolved property
		 */
		private String propertyName;

		/**
		 * Entry object
		 */
		private Object entry;

		/**
		 * mother object
		 */
		private Object mother;

		/**
		 * Get mother object(s)
		 * @return mother object, may be null, a Resolvable or a Collection of
		 *         Resolvables
		 */
		public Object getMother() {
			return mother;
		}

		/**
		 * Get property name of this entry
		 * @return property name
		 */
		public String getPropertyName() {
			return propertyName;
		}

		/**
		 * Create an instance of a PropertyPathEntry
		 * @param path path that resolved to this entry
		 * @param remainingPath remaining part of the path
		 * @param entry resolved entry
		 * @param mother mother object (may be null)
		 */
		public PropertyPathEntry(String path, String remainingPath, Object entry, Object mother) {
			this.path = path;
			this.remainingPath = remainingPath;
			this.entry = entry;
			this.mother = mother;
			if (StringUtils.isEmpty(path)) {
				propertyName = "";
			} else {
				propertyName = path.substring(path.lastIndexOf('.') + 1);
			}
		}

		/**
		 * Get the entry
		 * @return entry object
		 */
		public Object getEntry() {
			return entry;
		}

		/**
		 * Get the path that resolved to this property
		 * @return path to this property
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Get the remaining part of the path
		 * @return remaining part
		 */
		public String getRemainingPath() {
			return remainingPath;
		}

		/**
		 * Check whether this entry is the last entry (the actual resolved
		 * property)
		 * @return true for the last entry, false for other resolved entries
		 */
		public boolean isLastEntry() {
			return StringUtils.isEmpty(remainingPath);
		}
	}
}
