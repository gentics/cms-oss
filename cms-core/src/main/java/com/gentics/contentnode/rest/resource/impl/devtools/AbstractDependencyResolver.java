package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import java.util.List;

public abstract class AbstractDependencyResolver {

  public static AbstractDependencyResolver getResolver(
      Class<? extends SynchronizableNodeObject> clazz) throws NodeException {
    if (clazz.equals(Construct.class)) {
      return new ConstructResolver();
    }

    throw new NodeException("Could not find appropriate resolver for class: " + clazz);
  }

  public abstract List<PackageDependency> resolve(
      PackageSynchronizer packageSynchronizer) throws NodeException;


}
