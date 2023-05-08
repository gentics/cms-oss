/*
 * @author johannes2
 * @date Sep 22, 2010
 * @version $Id: TransactionalTriggerEvent.java,v 1.3 2010-10-19 11:20:01 norbert Exp $
 */
package com.gentics.contentnode.events;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Transactional;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.log.NodeLogger;

/**
 * This transactional object can be added to transactions. The stored event will
 * be triggered once the transaction will be commited.
 * @author johannes2
 */
public class TransactionalTriggerEvent extends AbstractTransactional {

	/**
	 * Key for the transaction attribute that holds the prevent flag
	 */
	public static final String PREVENT_KEY = "TransactionalTriggerEvent.prevent";

	protected static NodeLogger logger = NodeLogger.getNodeLogger(TransactionalTriggerEvent.class);

	/**
	 * The trigger event prevent implementation. This transactional can be used as a substitute for the {@link TransactionalTriggerEvent} class. The trigger event
	 * preventer will not invoke any events upon onTransactionCommit.
	 */
	private static Transactional triggerEventPreventer = new AbstractTransactional() {

		public boolean onTransactionCommit(Transaction t) {
			return false;
		}

		public void onDBCommit(Transaction t) throws NodeException {
		}
	};

	/**
	 * Object Class
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * Object ID
	 */
	protected Integer id;

	/**
	 * NodeObject, for which the event shall be triggered
	 */
	protected NodeObject object;

	/**
	 * Contains the properties for the trigger event call
	 */
	protected String[] properties;

	/**
	 * Contains the event mask for the event
	 */
	protected int eventMask;

	/**
	 * Create a new TransactionalTriggerEvent object that stores all information
	 * that is needed to trigger an event once the transaction will be committed
	 * @param clazz class
	 * @param id object id
	 * @param properties properties
	 * @param eventMask event mask
	 */
	public TransactionalTriggerEvent(Class<? extends NodeObject> clazz, Integer id, String[] properties, int eventMask) {
		this.clazz = clazz;
		this.id = id;
		this.properties = properties;
		this.eventMask = eventMask;
	}

	/**
	 * Create a new TransactionalTriggerEVent object that will trigger the given event
	 * on the given object
	 * @param object object
	 * @param properties
	 * @param eventMask
	 */
	public TransactionalTriggerEvent(NodeObject object, String[] properties,
			int eventMask) {
		this.object = object;
		this.clazz = object.getObjectInfo().getObjectClass();
		this.properties = properties;
		this.eventMask = eventMask;
	}

	/**
	 * @see Transactional#onDBCommit(Transaction)
	 */
	public void onDBCommit(Transaction t) throws NodeException {}

	/**
	 * @see Transactional#onTransactionCommit(Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		// we need to include objects from the wastebin here, because when putting folder structures into the wastebin, the subfolders
		// need to find their folders (in the wastebin) while triggering the event
		try (WastebinFilter f = new WastebinFilter(Wastebin.INCLUDE)) {
			if (object != null) {
				doTrigger(t, object);
				return true;
			} else if (clazz != null && id != null) {
				NodeObject o = t.getObject(clazz, id);

				if (o != null) {
					doTrigger(t, o);
					return true;
				}
			}
			return false;
		} catch (NodeException e) {
			logger.error("Error while calling triggerEvent within TransactionalTriggerEvent object.", e);
			return false;
		}
	}

	/**
	 * Trigger the event on the object
	 * @param t transaction
	 * @param o object
	 * @throws NodeException
	 */
	private void doTrigger(Transaction t, NodeObject o) throws NodeException {
		// if moving in a multichannelling environment, all channel variants are moved
		if (Events.isEvent(eventMask, Events.MOVE) && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)
				&& o instanceof LocalizableNodeObject<?>) {
			LocalizableNodeObject<?> locO = (LocalizableNodeObject<?>)o;
			for (NodeObject variant : t.getObjects(clazz, locO.getChannelSet().values())) {
				// for the master page, we allow instant publishing, for localized copies, we turn it off
				if (variant.equals(locO)) {
					Events.trigger(variant, properties, eventMask);
				} else {
					try (InstantPublishingTrx noInstantPub = new InstantPublishingTrx(false)) {
						Events.trigger(variant, properties, eventMask);
					}
				}
			}
		} else {
			Events.trigger(o, properties, eventMask);
		}
	}

	/**
	 * Returns the threshold for usage of the generic singleton implementation of the {@link TransactionalTriggerEvent} transactional.
	 */
	public int getThreshold(Transaction t) {
		return isPrevented(t) ? 0 : -1;
	}

	/**
	 * Checks whether the transaction attribute {@link TransactionalTriggerEvent#PREVENT_KEY} was set and the trigger event preventer is enabled
	 * 
	 * @param t
	 * @return
	 */
	private boolean isPrevented(Transaction t) {
		boolean isPrevented = ObjectTransformer.getBoolean(t.getAttributes().get(PREVENT_KEY), false);
		if (isPrevented && logger.isDebugEnabled()) {
			logger.debug("The triggerEventPreventer was enabled since the {" + PREVENT_KEY +"} setting was configured.");
		}
		return isPrevented;
	}

	/**
	 * Returns the trigger event preventer when the transaction attribute {@link TransactionalTriggerEvent#PREVENT_KEY} was set.
	 */
	public Transactional getSingleton(Transaction t) {
		return isPrevented(t) ? triggerEventPreventer : null;
	}
}
