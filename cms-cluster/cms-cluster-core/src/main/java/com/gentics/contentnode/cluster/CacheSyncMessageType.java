package com.gentics.contentnode.cluster;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Trx;

/**
 * Types of {@link CacheSyncMessage}s
 */
public enum CacheSyncMessageType {
	/**
	 * Dirt the cache for an object
	 */
	dirt(msg -> {
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		Trx.operate(() -> {
			factory.getFactory().dirtObjectCacheLocal(msg.getClazz(), msg.getId());
		});
	}),

	/**
	 * Clear the cache for a single object
	 */
	clearobject(msg -> {
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		factory.getFactory().clearLocal(msg.getClazz(), msg.getId());
	}),

	/**
	 * Clear the cache for multiple objects
	 */
	clearobjects(msg -> {
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		factory.getFactory().clearLocal(msg.getClazz(), msg.getIds());
	}),

	/**
	 * Clear the cache for all objects of a type
	 */
	cleartype(msg -> {
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		factory.getFactory().clearLocal(msg.getClazz());
	}),

	/**
	 * Clear the cache for all objects
	 */
	clearall(msg -> {
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		factory.getFactory().clearLocal();
	});

	/**
	 * Message handler
	 */
	private Consumer<CacheSyncMessage> handler;

	/**
	 * Create instance
	 * @param handler message handler
	 */
	private CacheSyncMessageType(Consumer<CacheSyncMessage> handler) {
		this.handler = handler;
	}

	/**
	 * Execute the handler on the given message
	 * @param msg message
	 */
	void execute(CacheSyncMessage msg) {
		try {
			handler.accept(msg);
			if (ClusterSupport.logger.isDebugEnabled()) {
				ClusterSupport.logger.debug(String.format("Handled %s", msg));
			}
		} catch (NodeException e) {
			ClusterSupport.logger.error(String.format("Error while handling cache sync message %s", msg), e);
		}
	}
}
