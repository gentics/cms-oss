package com.gentics.contentnode.object.parttype;

/**
 * Interface for additional resolvers, which are added via {@link CMSResolverService} implementations
 */
public interface ProvidedResolver {
	/**
	 * Clean resources, which were occupied by the resolver
	 */
	void clean();
}
