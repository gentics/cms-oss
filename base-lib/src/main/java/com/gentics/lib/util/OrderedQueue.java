package com.gentics.lib.util;

import java.util.Map;

/**
 * User: Stefan Hepp Date: 22.06.2005 Time: 17:38:24
 */
public interface OrderedQueue extends OrderedMap {

	int getQueueSize();

	Map.Entry pushStart(Object key, Object value);

	Map.Entry pushEnd(Object key, Object value);

	Map.Entry popStart();

	Map.Entry popEnd();

	Map.Entry pop(Object key);
}
