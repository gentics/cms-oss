package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import java.util.List;

public abstract class AbstractDependencyResolver {

  public static AbstractDependencyResolver getResolver(
      Class<? extends SynchronizableNodeObject> clazz) throws NodeException {
    if (clazz.equals(Construct.class)) {
      return new ConstructResolver();
    } else if (clazz.equals(Template.class)) {
      return new TemplateResolver();
    }

    throw new NodeException("Could not find appropriate resolver for class: " + clazz);
  }

  public abstract List<PackageDependency> resolve(
      PackageSynchronizer packageSynchronizer) throws NodeException;


  /**
   * Checks if an entity is part of the synchronized package.
   * @param synchronizer  The synchronizer that will look for objects in the Filesystem
   * @param clazz A class that is synchronizeable. The synchronizer will look exclusively for this class.
   * @param id The id the synchonizer will look for.
   * @return
   * @param <T>
   * @throws NodeException
   */
   <T extends SynchronizableNodeObject> boolean isInPackage(PackageSynchronizer synchronizer, Class<T> clazz, String id)
      throws NodeException {
    return synchronizer.getObjects(clazz).stream().anyMatch(d ->
        id.equals(ObjectTransformer.getString(d.getObject().getId(), null))
            || id.equals(ObjectTransformer.getString(d.getObject().getGlobalId(), null)));
  }

}
