package com.gentics.contentnode.publish;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.lib.log.NodeLogger;

/**
 * Static helper class for handling automatic disabling of instant publishing due to errors
 */
public class InstantCRPublishing {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(InstantCRPublishing.class);

	/**
	 * Error counts of content maps, keys are the IDs
	 */
	protected static Map<Integer, Integer> errorCounts = Collections.synchronizedMap(new HashMap<Integer, Integer>());

	/**
	 * Retry timestamps for content maps, that are temporarily disabled. Keys are the IDs
	 */
	protected static Map<Integer, Long> retry = Collections.synchronizedMap(new HashMap<Integer, Long>());

	/**
	 * Maximum allowed error count
	 */
	protected static int maxErrorCount = 0;

	/**
	 * Retry after (in milliseconds)
	 */
	protected static long retryAfter = 0;

	/**
	 * Private Constructor to avoid instantiation
	 */
	private InstantCRPublishing() {
	}

	/**
	 * Determine whether instant publishing for the given contentrepository is temporarily disabled due to subsequent errors
	 * @param contentRepository contentrepository to check
	 * @return true if instant publishing is currently temporarily disabled
	 */
	public static boolean isTemporarilyDisabled(ContentRepository contentRepository) {
		return isTemporarilyDisabled(contentRepository.getId());
	}

	/**
	 * Determine whether instant publishing for the given content map is temporarily disabled due to subsequent errors
	 * @param contentMap content map to check
	 * @return true if instant publishing is currently temporarily disabled
	 */
	public static boolean isTemporarilyDisabled(ContentMap contentMap) {
		return isTemporarilyDisabled(contentMap.getId());
	}

	/**
	 * Determine whether instant publishing for the given contentrepository ID is temporarily disabled due to subsequent errors
	 * @param id contentrepository ID
	 * @return true if instant publishing is currently temporarily disabled
	 */
	public static boolean isTemporarilyDisabled(Integer id) {
		if (!isAutomaticDisabling()) {
			return false;
		}
		long nextRetry = ObjectTransformer.getLong(retry.get(id), 0);
		if (nextRetry <= 0) {
			return false;
		}

		boolean doRetry = nextRetry < System.currentTimeMillis();
		if (doRetry && logger.isInfoEnabled()) {
			logger.info("Retrying temporarily disabled instant publishing for CR " + id);
		}
		return !doRetry;
	}

	/**
	 * Increase the error count for the given content map. This might temporarily disable instant publishing
	 * @param contentMap content map
	 */
	public static synchronized void increaseErrorCount(ContentMap contentMap) {
		if (!isAutomaticDisabling()) {
			return;
		}

		Integer id = contentMap.getId();
		int count = ObjectTransformer.getInt(errorCounts.get(id), 0) + 1;
		errorCounts.put(id, count);
		if (count > maxErrorCount) {
			long retryValue = System.currentTimeMillis() + retryAfter;
			retry.put(id, retryValue);
			logger.error("Disabling instant publishing for " + contentMap + " after " + count + " errors. Will retry @" + (retryValue/1000));
		}
	}

	/**
	 * Reset the error count. If instant publishing is temporarily disabled, it will be enabled again
	 * @param id content map id
	 * @return true if error count was set before, false if not
	 */
	public static synchronized boolean resetErrorCount(Integer id) {
		if (!isAutomaticDisabling()) {
			return false;
		}
		boolean hadErrorCount = errorCounts.containsKey(id);
		errorCounts.remove(id);
		retry.remove(id);
		return hadErrorCount;
	}

	/**
	 * Reset the error count. If instant publishing is temporarily disabled, it will be enabled again
	 * @param contentMap
	 */
	public static void resetErrorCount(ContentMap contentMap) {
		boolean hadErrorCount = resetErrorCount(contentMap.getId());
		if (hadErrorCount && logger.isInfoEnabled()) {
			logger.info("Reset error count for " + contentMap);
		}
	}

	/**
	 * Set the configuration values
	 * @param newMaxErrorCount max error count
	 * @param newRetryAfter retry after (in seconds)
	 */
	public static void set(int newMaxErrorCount, int newRetryAfter) {
		maxErrorCount = newMaxErrorCount;
		retryAfter = newRetryAfter * 1000L;
		if (logger.isInfoEnabled()) {
			logger.info("Setting configuration for automatic disabling of instant publishing: maxErrorCount: " + newMaxErrorCount + ", retryAfter: " + newRetryAfter
					+ ". Automatic disabling will be " + (isAutomaticDisabling() ? "on" : "off"));
		}
	}

	/**
	 * Check whether the automatic disabling is on
	 * @return true if automatic disabling is on, false if not
	 */
	public static boolean isAutomaticDisabling() {
		return maxErrorCount > 0 && retryAfter > 0;
	}

	/**
	 * Get the IDs of content maps, that are temporarily disabled
	 * @return Set of IDs
	 */
	public static Set<Integer> getDisabled() {
		return retry.keySet();
	}
}
