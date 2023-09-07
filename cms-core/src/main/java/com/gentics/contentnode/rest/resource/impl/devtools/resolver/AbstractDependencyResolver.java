package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.devtools.dependency.PackageDependency;
import java.util.List;

/**
 * Factory class to create concrete dependency resolver instances
 */
public abstract class AbstractDependencyResolver {

	/**
	 * The synchronizer that will look for objects in the Filesystem
	 */
	protected PackageSynchronizer synchronizer;


	/**
	 * Resolves all objects and gather all dependencies of the class
	 *
	 * @return List of packages dependencies and their references
	 * @throws NodeException
	 */
	public abstract List<PackageDependency> resolve() throws NodeException;


	/**
	 * Checks if an entity is part of the synchronized package.
	 *
	 * @param clazz A class that is synchronizeable. The synchronizer will look exclusively for this
	 *              class.
	 * @param id    The object id the synchonizer will look for.
	 * @return true if the object of the given class and id is in the package (i.e.: synced as package
	 * object)
	 * @throws NodeException
	 */
	<T extends SynchronizableNodeObject> boolean isInPackage(Class<T> clazz, String id)
			throws NodeException {
		return this.synchronizer.getObjects(clazz).stream().anyMatch(d ->
				id.equals(ObjectTransformer.getString(d.getObject().getId(), null))
						|| id.equals(ObjectTransformer.getString(d.getObject().getGlobalId(), null)));
	}


	/**
	 * Builder to create concrete dependency resolver instances
	 */
	public static class Builder {

		AbstractDependencyResolver resolver;

		/**
		 * The builder constructor takes a child class of a SynchronizableNodeObject to create an
		 * appropriate resolver instance
		 *
		 * @param clazz child of SynchronizableNodeObject
		 * @throws NodeException throws if no resolver implementation for the given class can be instantiated
		 * @see SynchronizableNodeObject
		 */
		public Builder(
				Class<? extends SynchronizableNodeObject> clazz) throws NodeException {
			if (clazz.equals(Construct.class)) {
				this.resolver = new ConstructResolver();
			} else if (clazz.equals(Template.class)) {
				this.resolver = new TemplateResolver();
			} else if (clazz.equals(ObjectTagDefinition.class)) {
				this.resolver = new ObjectPropertyResolver();
			} else if (clazz.equals(Datasource.class)) {
				this.resolver = new DatasourceResolver();
			} else {
				throw new NodeException("Could not find appropriate resolver for class: " + clazz);
			}
		}

		/**
		 * Sets the PackageSynchronizer
		 * @param synchronizer the PackageSynchronizer
		 * @return fluent API
		 */
		public Builder withSynchronizer(PackageSynchronizer synchronizer) {
			this.resolver.synchronizer = synchronizer;
			return this;
		}

		/**
		 * Build the resolver
		 * @return the actual resolver instance
		 */
		public AbstractDependencyResolver build() {
			return this.resolver;
		}

	}

}

