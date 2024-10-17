package com.gentics.contentnode.events;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.devtools.ChangeWatchService;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.InstantPublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.datasource.mccr.UnknownChannelException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * TODO comment this
 * @author norbert
 *
 */
public final class Events {
	/**
	 * Log
	 */
	private final static NodeLogger log = NodeLogger.getNodeLogger(Events.class);

	public final static int ALL = 0x7FFFFFFF;

	public final static int CREATE = 0x1;

	public final static int UPDATE = 0x2;

	public final static int DELETE = 0x4;

	public final static int MOVE = 0x8;

	public final static int CHILD = 0x10;

	public final static int PARENT = 0x20;

	public final static int NOTIFY = 0x40;

	public final static int MAINTENANCE_PUBLISH = 0x80;

	public final static int MAINTENANCE_DELAY = 0x100;

	public final static int MAINTENANCE_REPUBLISH = 0x200;

	public final static int MAINTENANCE_MARKPUBLISHED = 0x400;

	public final static int LOGGING_START = 0x800;

	public final static int LOGGING_END = 0x1000;

	public final static int DATACHECK_CR = 0x4000;

	public final static int RESERVED12 = 0x8000;
	public final static int DIRT = RESERVED12;

	public final static int USER_1 = 0x10000;
	public final static int EVENT_CN_CONTENT = USER_1;

	public final static int USER_2 = 0x20000;
	public final static int EVENT_CN_PAGESTATUS = USER_2;

	public final static int USER_3 = 0x40000;
	public final static int EVENT_OBJTAG_SYNC = USER_3;

	public final static int MAINTENANCE_MIGRATE2PUBLISHQUEUE = 0x80000;

	public final static int HIDE = 0x100000;

	public final static int REVEAL = 0x200000;

	public final static int WASTEBIN = 0x400000;

	public final static int USER_8 = 0x800000;

	public final static int USER_9 = 0x1000000;

	public final static int USER_10 = 0x2000000;

	public final static int USER_11 = 0x4000000;

	public final static int USER_12 = 0x8000000;

	public final static int USER_13 = 0x10000000;

	public final static int USER_14 = 0x20000000;

	public final static int USER_15 = 0x40000000;

	public final static String[] EVENTNAMES = new String[] {
		"ALL", "CREATE", "UPDATE", "DELETE", "MOVE", "CHILD", "PARENT", "NOTIFY", "MAINTENANCE_PUBLISH",
		"MAINTENANCE_DELAY", "MAINTENANCE_REPUBLISH", "MAINTENANCE_MARKPUBLISHED", "LOGGING_START", "LOGGING_END", "CLEAN_CR", "DATACLEAN_CR", "DIRT",
		"CN_CONTENT", "CN_PAGESTATUS", "USER_3", "USER_4", "HIDE", "REVEAL", "WASTEBIN", "USER_8", "USER_9", "USER_10", "USER_11", "USER_12", "USER_13",
		"USER_14", "USER_15"};

	/**
	 * Loader for {@link EventsService}s
	 */
	protected final static ServiceLoaderUtil<EventsService> eventsServiceLoader = ServiceLoaderUtil.load(EventsService.class);

	/**
	 * Check whether the given mask contains the given event
	 * @param mask bitmask
	 * @param event event (might be the mask of some events)
	 * @return true when the bitmask contains the given event, false if not
	 */
	public final static boolean isEvent(int mask, int event) {
		return mask >= 0 && (mask & event) != 0;
	}

	/**
	 * Convert the given eventMask into a readable string
	 * @param eventMask eventmask
	 * @return readable string
	 */
	public final static String toString(int eventMask) {
		StringBuffer string = new StringBuffer();

		if (eventMask == ALL) {
			string.append(EVENTNAMES[0]);
		} else {
			int bit = 1;

			for (int i = 1; i < EVENTNAMES.length; i++) {
				if ((eventMask & bit) > 0) {
					if (string.length() > 0) {
						string.append(" | ");
					}
					string.append(EVENTNAMES[i]);
				}
				bit <<= 1;
			}
		}

		return string.toString();
	}

	/**
	 * Trigger the given event for the object. This method assumes that a
	 * transaction has already been started and is set as current transaction
	 * @param object object for that the event occurred
	 * @param properties string array of affected properties, may be null
	 * @param eventMask eventmask of the triggered event
	 * @throws NodeException
	 */
	public static void trigger(NodeObject object, String[] properties, int eventMask) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		long timestamp = t.getTimestamp();

		String sid = t.getSessionId();

		// when the property array contains just the empty string, we set it
		// to null
		if (properties != null && properties.length == 1 && StringUtils.isEmpty(properties[0])) {
			properties = null;
		}

		// store deleted/created objects here
		NodeObject deletedOrCreatedObject = null;
		// the parent(s) of the deleted/created object
		List<NodeObject> parentObjects = new Vector<NodeObject>();

		// special treatment for DELETE/CREATE events:
		if ((isEvent(eventMask, DELETE) || isEvent(eventMask, CREATE) || isEvent(eventMask, EVENT_CN_PAGESTATUS))
				&& !isEvent(eventMask, CHILD) && !isEvent(eventMask, PARENT)) {
			deletedOrCreatedObject = object;
			if (deletedOrCreatedObject instanceof Page) {
				// check whether the page is now to publish and was not
				// published before or is now taken offline and was
				// online before
				Page page = (Page) deletedOrCreatedObject;

				if (isEvent(eventMask, DELETE) || isEvent(eventMask, CREATE)) {
					// page is deleted
					parentObjects.add(deletedOrCreatedObject.getParentObject());
				} else if (isEvent(eventMask, EVENT_CN_PAGESTATUS)) {
					// status of page was changed
					parentObjects.add(deletedOrCreatedObject.getParentObject());
				}
			} else if (deletedOrCreatedObject instanceof Template) {
				// get all folders the template is linked to
				Template deletedOrCreatedTemplate = ((Template) deletedOrCreatedObject).getMaster();

				parentObjects.addAll(deletedOrCreatedTemplate.getFolders());
			} else if (deletedOrCreatedObject != null) {
				parentObjects.add(deletedOrCreatedObject.getParentObject());
			}
		}

		// clear the cache of the object
		t.dirtObjectCache(object.getObjectInfo().getObjectClass(), object.getId(), true);

		// and the cache of the parent objects
		for (NodeObject parentObject : parentObjects) {
			// omit null objects
			if (parentObject == null) {
				continue;
			}
			t.dirtObjectCache(parentObject.getObjectInfo().getObjectClass(), parentObject.getId(), true);
			// when the parent object is a folder (which most likely will be),
			// we also dirt the object cache for all other folders of the same
			// channelset
			if (parentObject instanceof Folder) {
				Folder parentFolder = (Folder) parentObject;
				Map<Integer, Integer> channelSet = parentFolder.getChannelSet();

				for (Integer channelVariantId : channelSet.values()) {
					t.dirtObjectCache(Folder.class, channelVariantId, true);
				}
			}
		}

		// clear the cache of all language variants of this page
		if (deletedOrCreatedObject instanceof Page) {
			List<Page> languageVariants = ((Page) deletedOrCreatedObject).getLanguageVariants(false);

			for (Iterator<Page> iter = languageVariants.iterator(); iter.hasNext();) {
				Page languageVariant = iter.next();

				t.dirtObjectCache(Page.class, languageVariant.getId(), true);
			}
			Collection<Integer> channelVariants = ((Page) deletedOrCreatedObject).getChannelSet().values();

			for (Integer id : channelVariants) {
				t.dirtObjectCache(Page.class, id, true);
			}
		} else if (deletedOrCreatedObject instanceof Template) {
			Collection<Integer> channelVariants = ((Template) deletedOrCreatedObject).getChannelSet().values();

			for (Integer id : channelVariants) {
				t.dirtObjectCache(Template.class, id, true);
			}
		} else if (deletedOrCreatedObject instanceof com.gentics.contentnode.object.File) {
			Collection<Integer> channelVariants = ((com.gentics.contentnode.object.File) deletedOrCreatedObject).getChannelSet().values();

			for (Integer id : channelVariants) {
				t.dirtObjectCache(deletedOrCreatedObject.getObjectInfo().getObjectClass(), id, true);
			}
		}

		// add the dirtevent into the table
		QueueEntry entry = new QueueEntry((int) (timestamp / 1000), ObjectTransformer.getInt(object.getId(), -1),
				t.getTType(object.getObjectInfo().getObjectClass()), eventMask, properties, 0, sid);

		// In order to store QueueEntries, we must pass it the a ContentNodeFactory so that it can create new transactions.
		ContentNodeFactory factory = ContentNodeFactory.getInstance();

		if (log.isDebugEnabled()) {
			log.debug("Storing event {" + entry + "} into queue.");
		}
		entry.store(factory);

		// possible synchronization
		if (object instanceof SynchronizableNodeObject
				&& (isEvent(eventMask, CREATE) || isEvent(eventMask, UPDATE) || isEvent(eventMask, NOTIFY))) {
			Synchronizer.synchronize((SynchronizableNodeObject) object);
		}
		if (object instanceof Tag && ((Tag) object).getContainer() instanceof SynchronizableNodeObject
				&& (isEvent(eventMask, CREATE) || isEvent(eventMask, UPDATE) || isEvent(eventMask, NOTIFY))) {
			Synchronizer.synchronize((SynchronizableNodeObject)(((Tag) object).getContainer()));
		}
		if (object instanceof SynchronizableNodeObject && (isEvent(eventMask, DELETE))) {
			Synchronizer.remove((SynchronizableNodeObject) object);
		}

		// inform watchers
		ChangeWatchService.trigger(object, properties, eventMask);

		for (EventsService eventsService : eventsServiceLoader) {
			eventsService.trigger(object, eventMask, properties);
		}

		// synchronize object properties
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC) && object instanceof ObjectTag
				&& isEvent(eventMask, EVENT_OBJTAG_SYNC)) {
			((ObjectTag) object).sync();
		}

		// check whether the object is a "published" object and if yes, handle instant publishing for it
		boolean publishCache = t.isPublishCacheEnabled();
		try {
			// disable the publish cache, because insant publishing would fail.
			// the publish cache depends on prepared data, which are not available for instant publishing
			t.setPublishCacheEnabled(false);
			try (WastebinFilter f = new WastebinFilter(Wastebin.EXCLUDE)) {
				InstantPublisher.handleInstantPublishing(object, eventMask, null, properties);
			}
		} catch (NodeException e) {
			log.error("Error while handling instant publishing for " + object, e);
			String suffix = (e instanceof UnknownChannelException) ? "error.unknownchannel" : "error";
			RenderResult renderResult = t.getRenderResult();

			if (renderResult != null) {
				if (object instanceof Page) {
					I18nString msg = new CNI18nString("instantpublishing.page." + suffix);

					msg.setParameter("0", ((Page) object).getName());
					renderResult.addMessage(new DefaultNodeMessage(Level.ERROR, Events.class, msg.toString()), false);
				} else if (object instanceof Folder) {
					I18nString msg = new CNI18nString("instantpublishing.folder." + suffix);

					msg.setParameter("0", ((Folder) object).getName());
					renderResult.addMessage(new DefaultNodeMessage(Level.ERROR, Events.class, msg.toString()), false);
				} else if (object instanceof com.gentics.contentnode.object.File) {
					I18nString msg = new CNI18nString("instantpublishing.file." + suffix);

					msg.setParameter("0", ((com.gentics.contentnode.object.File) object).getName());
					renderResult.addMessage(new DefaultNodeMessage(Level.ERROR, Events.class, msg.toString()), false);
				}
			}
		} finally {
			t.setPublishCacheEnabled(publishCache);
		}
	}

	/**
	 * Triggers a log start or log end event.
	 * @param id Id of the log command that was effected by the user
	 * @param properties If more than one object was effected by the user the number of additional objects effected should be stored in the first property.
	 * @param sid The sessionId for which to trigger the event
	 * @param eventMask Events.LOGGING_START or Events.LOGGING_END
	 */
	public static void triggerLogEvent(Object id, String[] properties, String sid, int eventMask) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		long timestamp = t.getTimestamp();

		// add the dirt event into the table
		QueueEntry entry = new QueueEntry((int) (timestamp / 1000), ObjectTransformer.getInt(id, -1), 0, eventMask, properties, 0, sid);

		if (log.isDebugEnabled()) {
			log.debug("Storing event {" + entry + "} into queue.");
		}

		// In order to store QueueEntries, we must pass it the a ContentNodeFactory so that it can create new transactions.
		ContentNodeFactory factory = ContentNodeFactory.getInstance();

		entry.store(factory);
	}
}
