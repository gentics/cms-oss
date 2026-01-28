package com.gentics.contentnode.cache;

import java.util.Set;

import com.gentics.contentnode.object.NodeObject;

/**
 * Interface for implementations that handle cache clear operations
 */
public interface CacheService {
	/**
	 * Called when the complete cache is cleared
	 */
	void clear();

	/**
	 * Called when the cache of a class of objects is cleared
	 * @param clazz object class
	 */
	void clear(Class<? extends NodeObject> clazz);

	/**
	 * Called when the cache of an object is cleared
	 * @param clazz object class
	 * @param id object ID
	 */
	void clear(Class<? extends NodeObject> clazz, Integer id);

	/**
	 * Called when the cache of an object is cleared
	 * @param clazz object class
	 * @param id object ID
	 */
	void clear(Class<? extends NodeObject> clazz, Set<Integer> ids);

	/**
	 * Called when the cache of an object is dirted
	 * @param clazz object class
	 * @param id object ID
	 */
	void dirt(Class<? extends NodeObject> clazz, Integer id);
}
