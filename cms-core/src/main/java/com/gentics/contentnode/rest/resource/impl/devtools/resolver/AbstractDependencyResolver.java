
package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import java.util.List;

public abstract class AbstractDependencyResolver {

  /**
   * The synchronizer that will look for objects in the Filesystem
   */
  protected PackageSynchronizer synchronizer;


  /**
   * Resolves all objects and gather all dependencies of the class
   * @return List of packages dependencies and their references
   * @throws NodeException
   */
  public abstract List<PackageDependency> resolve() throws NodeException;


  /**
   * Checks if an entity is part of the synchronized package.
   *
   * @param clazz        A class that is synchronizeable. The synchronizer will look exclusively for
   *                     this class.
   * @param id           The object id the synchonizer will look for.
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


  public static class Builder {

    AbstractDependencyResolver resolver;

    public Builder(
        Class<? extends SynchronizableNodeObject> clazz) throws NodeException {
      if (clazz.equals(Construct.class)) {
        this.resolver = new ConstructResolver();
      } else if (clazz.equals(Template.class)) {
        this.resolver = new TemplateResolver();
      }
      else if (clazz.equals(ObjectTagDefinition.class)) {
        this.resolver = new ObjectPropertyResolver();
      }
      else {
        throw new NodeException("Could not find appropriate resolver for class: " + clazz);
      }
    }

    public Builder withSynchronizer(PackageSynchronizer synchronizer) {
      this.resolver.synchronizer = synchronizer;
      return this;
    }

    public AbstractDependencyResolver build() {
      return this.resolver;
    }

  }

}
