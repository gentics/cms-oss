/*
 * @author norbert
 * @date 22.01.2007
 * @version $Id: DependencyObject.java,v 1.4.8.1 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.events;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.DummyObject;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;

/**
 * A dependency object may be one of
 * <ul>
 * <li>objectclass</li>
 * <li>objectclass and elementclass</li>
 * <li>objectclass and element instance</li>
 * <li>object instance</li>
 * <li>object instance and elementclass</li>
 * <li>object instance and element instance</li>
 * </ul>
 */
public class DependencyObject implements Comparable<DependencyObject> {

	/**
	 * Object id
	 */
	protected Integer objectId;

	/**
	 * Object class
	 */
	protected Class<? extends NodeObject> objectClass;

	/**
	 * Element id
	 */
	protected Integer elementId;

	/**
	 * element class
	 */
	protected Class<? extends NodeObject> elementClass;

	/**
	 * Get the element or null if none set
	 * @return element object (or null)
	 * @throws NodeException
	 */
	public NodeObject getElement() throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();
		NodeObject element = null;
		if (elementClass != null && elementId != null) {
			element = t.getObject(elementClass, elementId, -1, false);

			// if the object was not found, this means that it was probably removed.
			// but we still want to trigger events on that object, so we create a dummy
			if (element == null) {
				element = new DummyObject(elementId, t.createObjectInfo(elementClass));
			}
		}

		return element;
	}

	/**
	 * Get the element id
	 * @return element id
	 */
	public Object getElementId() {
		return elementId;
	}

	/**
	 * Get the element class
	 * @return element class
	 */
	public Class<? extends NodeObject> getElementClass() {
		return elementClass;
	}

	/**
	 * Get the object or null if none set
	 * @return object or null
	 * @throws NodeException
	 */
	public NodeObject getObject() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodeObject obj = null;
		if (objectClass != null && objectId != null) {
			obj = t.getObject(objectClass, objectId, -1, false);

			// if the object was not found, this means that it was probably removed.
			// but we still want to trigger events on that object, so we create a dummy
			if (obj == null) {
				obj = new DummyObject(objectId, t.createObjectInfo(objectClass));
			}
		}

		return obj;
	}

	/**
	 * Get the object id
	 * @return object id
	 */
	public Integer getObjectId() {
		return objectId;
	}

	/**
	 * Get the object class for the dependency object. This may be null, if the object does not exist
	 * @return object class (may be null)
	 */
	public Class<? extends NodeObject> getObjectClass() {
		return objectClass;
	}

	/**
	 * Create an instance for the given object class
	 * @param objectClass object class
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass) {
		this(objectClass, (Class<? extends NodeObject>) null);
	}

	/**
	 * Create an instance for the given object
	 * @param object object
	 */
	public DependencyObject(NodeObject object) {
		this(object, (NodeObject) null);
	}

	/**
	 * Create an instance of the given object class and element class
	 * @param objectClass object class
	 * @param elementClass element class
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass, Class<? extends NodeObject> elementClass) {
		setObject(objectClass, null);
		setElement(elementClass, null);
	}

	/**
	 * Create an instance for the given object class and element
	 * @param objectClass object class
	 * @param element element
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass, NodeObject element) {
		setObject(objectClass, null);
		setElement(element);
	}

	/**
	 * Create an instance for the given object class and element (by class and id)
	 * @param objectClass object class
	 * @param elementClass element class
	 * @param elementId element id
	 * @throws NodeException
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass, Class<? extends NodeObject> elementClass, Integer elementId) throws NodeException {
		setObject(objectClass, null);
		setElement(elementClass, elementId);
	}

	/**
	 * Create an instance for the given object and element
	 * @param object object
	 * @param element element
	 */
	public DependencyObject(NodeObject object, NodeObject element) {
		setObject(object);
		setElement(element);
	}

	/**
	 * Create an instance of the given object (by class and id) and element (by class and id)
	 * @param objectClass object class
	 * @param objectId object id
	 * @param elementClass element class
	 * @param elementId element id
	 * @throws NodeException
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass, Integer objectId, Class<? extends NodeObject> elementClass,
			Integer elementId) throws NodeException {
		setObject(objectClass, objectId);
		setElement(elementClass, elementId);
	}

	/**
	 * Create an instance for the given object and element class
	 * @param object object
	 * @param elementClass element class
	 */
	public DependencyObject(NodeObject object, Class<? extends NodeObject> elementClass) {
		setObject(object);
		setElement(elementClass, null);
	}

	/**
	 * Create an instance for the given object (by class and id) and element class
	 * @param objectClass object class
	 * @param objectId object id
	 * @param elementClass element class
	 * @throws NodeException
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass, Integer objectId, Class<? extends NodeObject> elementClass) throws NodeException {
		setObject(objectClass, objectId);
		setElement(elementClass, null);
	}

	/**
	 * Create an instance for the given object (by class and id) and element
	 * @param objectClass object class
	 * @param objectId object id
	 * @param element element
	 * @throws NodeException
	 */
	public DependencyObject(Class<? extends NodeObject> objectClass, Integer objectId, NodeObject element) throws NodeException {
		setObject(objectClass, objectId);
		setElement(element);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof DependencyObject) {
			DependencyObject depObj = (DependencyObject) obj;

			return compareTo(depObj) == 0;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		// TODO maybe optimize this
		int hashCode = 0;

		hashCode += getObjectClass() != null ? getObjectClass().hashCode() : 0;
		hashCode += getObjectId() != null ? getObjectId().hashCode() : 0;
		hashCode -= getElementClass() != null ? getElementClass().hashCode() : 0;
		hashCode -= getElementId() != null ? getElementId().hashCode() : 0;
		return hashCode;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();

		if (objectClass != null) {
			str.append(objectClass.getSimpleName());
			if (objectId != null) {
				str.append(" (").append(objectId).append(")");
			}
		} else {
			str.append("no object");
		}
		if (elementClass != null) {
			str.append("/").append(elementClass.getSimpleName());
			if (elementId != null) {
				str.append(" (").append(elementId).append(")");
			}
		}
		return str.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(DependencyObject o) {
		// first compare the object data
		int result = compareClassId(getObjectClass(), getObjectId(), o.getObjectClass(), o.getObjectId());

		// object data is identical -> compare element data
		if (result == 0) {
			result = compareClassId(getElementClass(), getElementId(), o.getElementClass(), o.getElementId());
		}

		return result;
	}

	/**
	 * Static helper method to compare class/id combinations
	 * @param clazz first class
	 * @param id first id
	 * @param foreignClazz second class
	 * @param foreignId second id
	 * @return -1 when the first combination is "smaller", 1 when the second
	 *         combination is "smaller", 0 when they are "equal"
	 */
	protected static int compareClassId(Class<? extends NodeObject> clazz, Object id, Class<? extends NodeObject> foreignClazz,
			Object foreignId) {
		// compare the classes first
		if (clazz == null) {
			return foreignClazz == null ? 0 : -1;
		} else if (foreignClazz == null) {
			return 1;
		} else {
			if (clazz.equals(foreignClazz)) {
				// compare obj ids
				int idInt = ObjectTransformer.getInt(id, -1);
				int foreignIdInt = ObjectTransformer.getInt(foreignId, -1);

				return idInt - foreignIdInt;
			} else {
				return clazz.getName().compareTo(foreignClazz.getName());
			}
		}
	}

	/**
	 * Set the object
	 * @param object object (may be null)
	 */
	protected void setObject(NodeObject object) {
		if (object != null) {
			setObject(object.getObjectInfo().getObjectClass(), object.getId());
		} else {
			setObject(null, null);
		}
	}

	/**
	 * Set the object by class and id
	 * @param objectClass object class
	 * @param objectId object id
	 */
	protected void setObject(Class<? extends NodeObject> objectClass, Integer objectId) {
		this.objectClass = objectClass;
		// normalize object class, since dependencies do not distinguish between images and files
		if (this.objectClass == ImageFile.class) {
			this.objectClass = File.class;
		}
		this.objectId = objectId;
	}

	/**
	 * Set the element
	 * @param element element (may be null)
	 */
	protected void setElement(NodeObject element) {
		if (element != null) {
			setElement(element.getObjectInfo().getObjectClass(), element.getId());
		} else {
			setElement(null, null);
		}
	}

	/**
	 * Set the element by class and id
	 * @param elementClass element class
	 * @param elementId element id
	 */
	protected void setElement(Class<? extends NodeObject> elementClass, Integer elementId) {
		this.elementClass = elementClass;
		// normalize element class, since dependencies do not distinguish between images and files
		if (this.elementClass == ImageFile.class) {
			this.elementClass = File.class;
		}
		this.elementId = elementId;
	}
}
