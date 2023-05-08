package com.gentics.contentnode.devtools;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.jmx.ChangeEventListener;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.lib.log.NodeLogger;

/**
 * Watch Service that will send events to registered listeners
 */
public class ChangeWatchService {
	/**
	 * Registry
	 */
	protected static Map<UUID, ChangeListener> registry = new ConcurrentHashMap<>();

	/**
	 * Loger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ChangeWatchService.class);

	/**
	 * Check whether the automatic cleanup job was registered
	 */
	protected static boolean cleanRegistered = false;

	/**
	 * Register a listener for the given page
	 * @param nodeId node ID
	 * @param pageId page ID
	 * @return UUID of the listener
	 */
	public static synchronized UUID register(String nodeId, String pageId) {
		registerCleanUp();
		UUID uuid = UUID.randomUUID();
		registry.put(uuid, new ChangeListener(nodeId, pageId));
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registered listener %s. Listener size is now %d", uuid, getRegistryEntries()));
		}
		return uuid;
	}

	/**
	 * Unregister the listener with given UUID
	 * @param uuid uuid
	 */
	public static synchronized void unregister(UUID uuid) {
		if (registry.containsKey(uuid)) {
			ChangeListener listener = registry.remove(uuid);
			try {
				listener.getEventOutput().close();
			} catch (IOException e) {
			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Unregistered listener %s. Listener size is now %d", uuid, getRegistryEntries()));
			}
		}
	}

	/**
	 * Get the Event Output for the given UUID
	 * @param uuid UUID
	 * @return Event Output
	 */
	public static EventOutput getEventOutput(UUID uuid) {
		ChangeListener changeListener = registry.get(uuid);
		return changeListener != null ? changeListener.getEventOutput() : new EventOutput();
	}

	/**
	 * Render the page, registered with the given UUID
	 * @param uuid UUID
	 * @return rendered page
	 * @throws NodeException
	 */
	public static String render(UUID uuid) throws NodeException {
		ChangeListener changeListener = registry.get(uuid);
		return changeListener != null ? changeListener.render() : "";
	}

	/**
	 * Trigger a change on an object. Inform all watchers
	 * @param object changed object
	 * @param properties changed properties
	 * @param eventMask event mask for events
	 */
	public static void trigger(NodeObject object, String[] properties, int eventMask) {
		if (object == null) {
			return;
		}
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.DEVTOOLS)) {
				return;
			}

			try (LangTrx lTrx = new LangTrx(t.getObject(UserLanguage.class, 1))) {
				if (logger.isDebugEnabled()) {
					logger.debug("Trigger " + Events.toString(eventMask) + " on " + object);
				}
				Set<EventOutput> listeners = get(object.getTType(), object.getId());
				if (listeners.isEmpty()) {
					return;
				}

				OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
				eventBuilder.data(String.class, object.toString());
				OutboundEvent event = eventBuilder.build();

				for (EventOutput listener : listeners) {
					try {
						if (!listener.isClosed()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Send event to listener");
							}
							listener.write(event);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Omitting closed listener");
							}
						}
					} catch (IOException e) {
						logger.error("Error while sending event to listener", e);
					}
				}
			}
		} catch (NodeException e) {
			logger.error("Error while triggering event", e);
			return;
		}
	}

	/**
	 * Get the listeners on the given object
	 * @param objectType object type
	 * @param objectId object id
	 * @return set of listeners
	 */
	protected static synchronized Set<EventOutput> get(int objectType, int objectId) {
		return registry.values().stream().map(listener -> listener.listen(objectType, objectId)).filter(eventOutput -> eventOutput != null)
				.collect(Collectors.toSet());
	}

	/**
	 * Register a periodic cleanup job (if not done before)
	 */
	protected static void registerCleanUp() {
		if (cleanRegistered) {
			return;
		}

		MBeanRegistry.registerMBean(new ChangeEventListener(), "System", "ChangeEventListener");
		if (logger.isInfoEnabled()) {
			logger.info("Register cleanup job");
		}
		Operator.getScheduledExecutor().scheduleAtFixedRate(() -> {
			if (logger.isInfoEnabled()) {
				logger.info("Cleanup job running");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Entries before cleanup: " + getRegistryEntries());
			}

			// send the ping to every listener (but only once)
			OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
			eventBuilder.name("ping");
			eventBuilder.data(String.class, "ping");
			final OutboundEvent ping = eventBuilder.build();

			registry.values().stream().map(listener -> listener.getEventOutput()).forEach(output -> {
				try {
					if (!output.isClosed()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Sending ping");
						}
						output.write(ping);
					}
				} catch (Exception e) {
				}
			});

			registry.values().removeIf(listener -> listener.getEventOutput().isClosed());

			if (logger.isInfoEnabled()) {
				logger.info("Cleanup job finished");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Entries after cleanup: " + getRegistryEntries());
			}
		}, 1, 1, TimeUnit.MINUTES);
		cleanRegistered = true;
	}

	/**
	 * Get the registry count (not distinct)
	 * @return registry count
	 */
	public static int getRegistryEntries() {
		return registry.size();
	}
}
