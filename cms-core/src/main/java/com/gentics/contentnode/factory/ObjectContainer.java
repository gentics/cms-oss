/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ObjectContainer.java,v 1.8 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * This class implements the business logic of storing different versions of the
 * same NodeObject into the cache.
 */
public class ObjectContainer implements Serializable {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 5290340828166254718L;

	/**
	 * map holding all currently cached versions of the NodeObject
	 */
	protected Map<Long, ObjectWithExpiryDate> objMap;

	/**
	 * id of the cached NodeObject
	 */
	private Integer id;

	/**
	 * class of the cached NodeObject
	 */
	private Class<? extends NodeObject> objClass;

	/**
	 * Constructor for a new container.
	 * @param clazz class of the objects to be contained.
	 * @param id id of the objects to be contained.
	 */
	public ObjectContainer(Class<? extends NodeObject> clazz, Integer id) {
		this.id = id;
		this.objClass = clazz;
		this.objMap = new HashMap<Long, ObjectContainer.ObjectWithExpiryDate>(2);
	}

	/**
	 * get the id of the objects stored in this container.
	 * @return if of the stored objects.
	 */
	public Integer getObjectId() {
		return id;
	}

	/**
	 * get the class of the objects stored in this container.
	 * @return class of the stored objects.
	 */
	public Class<? extends NodeObject> getObjectClass() {
		return objClass;
	}

	/**
	 * get a stored version of a nodeobject.
	 * @param info an objectinfo describing the type of object to get.
	 * @return the stored object, or null if the object is not stored.
	 */
	public NodeObject getObject(NodeObjectInfo info) throws NodeException {
		// get the latest version (with transaction id <= current transaction
		// id), ignoring all transactions that were running when the current
		// transaction started
		Transaction t = TransactionManager.getCurrentTransaction();
		Long lastValidVersion = null;
		ObjectWithExpiryDate cachedObject = null;

		synchronized (objMap) {
			for (Iterator<Entry<Long, ObjectWithExpiryDate>> iter = objMap.entrySet().iterator(); iter.hasNext();) {
				Entry<Long, ObjectWithExpiryDate> entry = iter.next();
				Long id = entry.getKey();

				// ignore parallel running transactions
				if (t.isParallelOpen(id.longValue())) {
					continue;
				}

				// find the latest valid transaction which is not younger than the
				// current transaction
				if (id.longValue() <= t.getId() && (lastValidVersion == null || lastValidVersion.longValue() < id.longValue())
						&& entry.getValue().isValidFor(t.getId())) {
					lastValidVersion = id;
				}
			}
			cachedObject = lastValidVersion != null ? objMap.get(lastValidVersion) : null;
		}

		return cachedObject != null ? cachedObject.getObject() : null;
	}

	/**
	 * set a new object into the container.
	 * @param obj object to store.
	 * @return the previously stored object of the same type, or null if none
	 *         was set.
	 */
	public NodeObject setObject(NodeObject obj) throws NodeException {
		Long transactionId = new Long(TransactionManager.getCurrentTransaction().getId());
		Object oldObject = null;

		synchronized (objMap) {
			oldObject = objMap.put(transactionId, new ObjectWithExpiryDate(obj));
		}
		return oldObject instanceof NodeObject ? (NodeObject) oldObject : null;
	}

	/**
	 * Dirt the object (remove caches for older, not running transactions)
	 * @throws NodeException
	 */
	public void dirtObject() throws NodeException {
		// remove all versions of the object when transaction of object is not
		// running and transaction id < current transaction id
		long currentTransactionId = TransactionManager.getCurrentTransaction().getId();
		long lastTransactionId = TransactionManager.getLastTransactionID();

		synchronized (objMap) {
			for (Iterator<Entry<Long, ObjectWithExpiryDate>> iter = objMap.entrySet().iterator(); iter.hasNext();) {
				Entry<Long, ObjectWithExpiryDate> entry = iter.next();
				Long id = entry.getKey();

				if ((id.longValue() < currentTransactionId && !TransactionManager.isTransactionRunning(id.longValue()))
						|| id.longValue() == currentTransactionId) {
					// remove the key (which removes the entry)
					iter.remove();
				} else {
					// set a new expiry date for the object
					entry.getValue().setExpiryDate(lastTransactionId);
				}
			}
		}
	}

	/**
	 * Purge old versions (which cannot be used any more)
	 */
	public void purgeOldVersions() {
		// remove all versions of the object (but the last) where the
		// transaction is not running any more
		Long lastValidVersion = null;
		Collection<Long> toRemove = new Vector<Long>();

		synchronized (objMap) {
			for (Iterator<Long> iter = objMap.keySet().iterator(); iter.hasNext();) {
				Long id = iter.next();

				if (!TransactionManager.isTransactionRunning(id.longValue())) {
					if (lastValidVersion != null) {
						// a valid version was found before
						if (lastValidVersion.longValue() < id.longValue()) {
							// mark the former last valid version for removal
							if (lastValidVersion != null) {
								toRemove.add(lastValidVersion);
							}
							lastValidVersion = id;
						} else {
							// this version is older than the currently youngest, so
							// remove it
							iter.remove();
						}
					} else {
						// this is the first valid version found
						lastValidVersion = id;
					}
				}
			}

			objMap.keySet().removeAll(toRemove);
		}
	}

	/**
	 * Helper class for wrapping objects with an expiry date
	 */
	protected final static class ObjectWithExpiryDate implements Serializable {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2080993720283963328L;

		/**
		 * Stored object
		 */
		protected NodeObject object;

		/**
		 * ID of the transaction, which is the newest this object is valid for
		 */
		protected Long validUntil;

		/**
		 * Create an instance with the given object
		 * @param object object
		 */
		public ObjectWithExpiryDate(NodeObject object) {
			this.object = object;
		}

		/**
		 * Get the object
		 * @return object
		 */
		public NodeObject getObject() {
			return object;
		}

		/**
		 * Check whether the object is valid for the transaction with given id
		 * @param id id of the transaction
		 * @return true if the object is valid, false if not
		 */
		public boolean isValidFor(Long id) {
			if (id == null) {
				return false;
			} else if (validUntil == null) {
				return true;
			} else {
				return id <= validUntil;
			}
		}

		/**
		 * Set a new expiry date
		 * @param id new expiry date
		 */
		public void setExpiryDate(Long id) {
			if (id == null) {
				return;
			} else if (validUntil == null) {
				validUntil = id;
			} else if (id > validUntil) {
				validUntil = id;
			}
		}
	}
}
