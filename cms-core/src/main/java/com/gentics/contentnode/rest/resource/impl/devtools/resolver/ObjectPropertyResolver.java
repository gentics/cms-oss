
package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ObjectPropertyResolver extends AbstractDependencyResolver {

  private static final Class<ObjectTagDefinition> CLAZZ = ObjectTagDefinition.class;

  private static final Pattern UUID_REGEX =
      Pattern.compile(
          "^[0-9a-fA-F]{4}.[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");


  @Override
  public List<PackageDependency> resolve()
      throws NodeException {
    List<PackageObject<ObjectTagDefinition>> objectTagDefinitions = synchronizer.getObjects(CLAZZ);
    List<PackageDependency> resolvedDependencyList = new ArrayList<>();

    for (PackageObject<ObjectTagDefinition> packageObject : objectTagDefinitions) {
      ObjectTagDefinition objectTagDefinition = packageObject.getObject();
      List<PackageDependency> references = resolveReferences(objectTagDefinition.getObjectTags());

      PackageDependency dependency = new PackageDependency.Builder()
          .withGlobalId(objectTagDefinition.getGlobalId().toString())
          .withName(objectTagDefinition.getName())
          .withType(Type.OBJECT_TAG_DEFINITION)
          .withKeyword(objectTagDefinition.getObjectTag().getTypeKeyword())
          .withDependencies(references)
          .build();

      resolvedDependencyList.add(dependency);
    }

    return resolvedDependencyList;
  }


  private List<PackageDependency> resolveReferences(
      List<ObjectTag> objectTags) throws NodeException {
    List<PackageDependency> referencedDependencies = new ArrayList<>();

    for (ObjectTag tag : objectTags) {
      String constructId = tag.getConstruct().getGlobalId().toString();

      if (!UUID_REGEX.matcher(constructId).matches()) {
        // skip part types and dependencies with invalid uuids
        continue;
      }

      PackageDependency dependency = new PackageDependency.Builder()
          .withType(Type.CONSTRUCT)
          .withGlobalId(tag.getGlobalId().toString())
          .withName(tag.getName())
          .withIsInPackage(isInPackage(Construct.class, constructId))
          .build();

      referencedDependencies.add(dependency);
    }

    return referencedDependencies;
  }


}