package com.gentics.contentnode.factory;

import java.util.Collection;
import java.util.Iterator;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Enumeration for handling deleted objects (objects put into the wastebin)
 */
public enum Wastebin {
	/**
	 * Exclude objects from wastebin
	 */
	EXCLUDE,

	/**
	 * Include objects from wastebin
	 */
	INCLUDE,

	/**
	 * Only handle objects from wastebin
	 */
	ONLY;

	/**
	 * Get the default wastebin
	 * @return EXCLUDE
	 */
	public static Wastebin getDefault() {
		return EXCLUDE;
	}

	/**
	 * Set this Wastebin setting to the current transaction
	 * @return WastebinFilter as AutoClosable
	 * @throws NodeException
	 */
	public WastebinFilter set() throws NodeException {
		return new WastebinFilter(this);
	}

	/**
	 * Filter the given collection of objects by their wastebin status
	 * @param objects collection of objects, that is modified
	 * @throws NodeException
	 */
	public void filter(Collection<? extends NodeObject> objects) throws NodeException {
		if (objects.isEmpty()) {
			return;
		}

		for (Iterator<? extends NodeObject> i = objects.iterator(); i.hasNext(); ) {
			NodeObject o = i.next();
			if (!isFiltered(o)) {
				i.remove();
			}
		}
	}

	/**
	 * Filter the single object by its wastebin status
	 * @param object object
	 * @return the object or null
	 * @throws NodeException
	 */
	public <T extends NodeObject> T filter(T object) throws NodeException {
		return isFiltered(object) ? object : null;
	}

	/**
	 * Return an SQL query clause to filter for this wastebin filter.
	 * If a non-empty filter clause is returned, it begins with " AND"
	 * @param tablePrefix table prefix (must not be empty)
	 * @return filter clause (may be empty, but never null)
	 */
	public String filterClause(String tablePrefix) {
		StringBuilder clause = new StringBuilder();
		switch (this) {
		case ONLY:
			clause.append(" AND ").append(tablePrefix).append(".deleted != 0");
			break;
		case EXCLUDE:
			clause.append(" AND ").append(tablePrefix).append(".deleted = 0");
			break;
		default:
			break;
		}
		return clause.toString();
	}

	/**
	 * Filter the given object. This will consider, whether
	 * <ol>
	 * <li>The object can be recycled</li>
	 * <li>The feature is activated for the object's node</li>
	 * <li>The user is allowed to view wastebin objects for the object's node</li>
	 * <li>The object is deleted</li>
	 * <li>The Wastebin status</li>
	 * </ol>
	 * @param object object
	 * @return true if the object is allowed, false if not
	 * @throws NodeException
	 */
	protected boolean isFiltered(NodeObject object) throws NodeException {
		if (object == null) {
			return false;
		}

		if (!object.isRecyclable()) {
			return true;
		}

		switch (this) {
		case INCLUDE:
			return true;
		case EXCLUDE:
			return !object.isDeleted();
		case ONLY:
			return object.isDeleted();
		default:
			return false;
		}
	}
}
