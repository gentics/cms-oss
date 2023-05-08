/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: PortalPoolProvider.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool.ObjectPool;

/**
 * The PortalPoolProvider serves as the central point for management and
 * accessing of Portal Pools. TODO implement initialization with a single
 * properties file similar to PortalCache. TODO all pools used in the Portal
 * shall be managed with this class
 */
public abstract class PortalPoolProvider {

	/**
	 * Constant for the region of the document builder pool
	 */
	public final static String DOCUMENT_BUILDER_POOL = "documentBuilder";

	/**
	 * Map of PortalPool implementations. Keys are the pool regions.
	 */
	private final static Map PORTAL_POOLS = new HashMap();

	/**
	 * Map of KeyedPortalPool implementations. Keys are the pool regions.
	 */
	private final static Map KEYED_PORTAL_POOLS = new HashMap();

	static {
		// create the pool for document builders
		// TODO: do this in a generic way
		ObjectPool objectPool = PoolFactory.getObjectPoolFactory(null).createPool();

		objectPool.setFactory(new PortalDocumentBuilderFactory());

		PORTAL_POOLS.put(DOCUMENT_BUILDER_POOL, new CommonsPortalPool(objectPool));
	}

	/**
	 * Hide the constructor for this utility class
	 *
	 */
	private PortalPoolProvider() {}

	/**
	 * Get the {@link PortalPool} implementation for the given region.
	 * @param poolRegion pool region
	 * @return the PortalPool instance
	 * @throws PortalPoolException
	 */
	public final static PortalPool getPortalPool(String poolRegion) throws PortalPoolException {
		if (!PORTAL_POOLS.containsKey(poolRegion)) {
			throw new PortalPoolException("There is no pool defined for region {" + poolRegion + "}");
		} else {
			return (PortalPool) PORTAL_POOLS.get(poolRegion);
		}
	}

	/**
	 * Get the {@link KeyedPortalPool} implementation for the given region
	 * @param poolRegion pool region
	 * @return the KeyedPortalPool instance
	 * @throws PortalPoolException
	 */
	public final static KeyedPortalPool getKeyedPortalPool(String poolRegion) throws PortalPoolException {
		if (!KEYED_PORTAL_POOLS.containsKey(poolRegion)) {
			throw new PortalPoolException("There is no keyed pool defined for region {" + poolRegion + "}");
		} else {
			return (KeyedPortalPool) KEYED_PORTAL_POOLS.get(poolRegion);
		}
	}
}
