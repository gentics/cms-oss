package com.gentics.contentnode.object.parttype;

import java.util.Map;

/**
 * Interface for services that return additional resolvers
 */
public interface CMSResolverService {
	/**
	 * Get a map of resolvers. The map may be empty but must not be null
	 * @return map of resolvers
	 */
	Map<String, ProvidedResolver> getResolvers();
}
