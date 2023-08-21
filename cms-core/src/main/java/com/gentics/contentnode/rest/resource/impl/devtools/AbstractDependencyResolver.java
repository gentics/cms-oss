package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import java.util.List;

/**
 * Factory for the respective DependencyResolver implementations
 */
public abstract class AbstractDependencyResolver {

  /**
   * Factory to create the appropriate resolver implementation.
   * @param clazz The class for which the dependencies should be resolved.
   * @return The dependency resolver implementation
   * @throws NodeException Exception if no appropriate resolver can be instantiated.
   */
  public static AbstractDependencyResolver getResolver(
      Class<? extends SynchronizableNodeObject> clazz) throws NodeException {
    if (clazz.equals(Construct.class)) {
      return new ConstructResolver();
    } else if (clazz.equals(Template.class)) {
      return new TemplateResolver();
    }

    throw new NodeException("Could not find appropriate resolver for class: " + clazz);
  }

  /**
   * Method to resolve the package dependencies
   * @param packageSynchronizer The package synchronizer
   * @return The list of package dependencies
   * @throws NodeException Exception if dependencies cannot be resolved
   */
  public abstract List<PackageDependency> resolve(
      PackageSynchronizer packageSynchronizer) throws NodeException;


  /**
   * Checks if an entity is part of the synchronized package.
   * @param synchronizer The synchronizer that will look for objects in the Filesystem
   * @param clazz A class that is synchronizeable. The synchronizer will look exclusively for this class.
   * @param id The object id the synchonizer will look for.
   * @return true if the object of the given class and id is in the package (i.e.: synced as package object)
   * @throws NodeException Exception if assertion cannot be performed
   */
   <T extends SynchronizableNodeObject> boolean isInPackage(PackageSynchronizer synchronizer, Class<T> clazz, String id)
      throws NodeException {
    return synchronizer.getObjects(clazz).stream().anyMatch(d ->
        id.equals(ObjectTransformer.getString(d.getObject().getId(), null))
            || id.equals(ObjectTransformer.getString(d.getObject().getGlobalId(), null)));
  }

}
