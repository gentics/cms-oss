package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.List;

public class TemplateResolver extends AbstractDependencyResolver {

  private static final Class<Template> CLAZZ = Template.class;


  @Override
  public List<PackageDependency> resolve(PackageSynchronizer packageSynchronizer)
      throws NodeException {
    List<PackageObject<Template>> packageTemplates = packageSynchronizer.getObjects(CLAZZ);
    List<PackageDependency> resolvedDependencyList = new ArrayList<>();

    for (PackageObject<Template> packageObject : packageTemplates) {
      Template template = packageObject.getObject();
      List<PackageDependency> references = resolveReferences(packageSynchronizer, template);

      PackageDependency dependency = new PackageDependency.Builder()
          .withGlobalId(template.getGlobalId().toString())
          .withName(template.getName())
          .withType(Type.TEMPLATE)
          .withDependencies(references)
          .build();

      resolvedDependencyList.add(dependency);
    }

    return resolvedDependencyList;
  }


  private List<PackageDependency> resolveReferences(PackageSynchronizer packageSynchronizer,
      Template template) throws NodeException {
    List<TemplateTag> templateTags = new ArrayList<>(template.getTemplateTags().values());
    List<ObjectTag> objectTags = new ArrayList<>(template.getObjectTags().values());

    List<PackageDependency> referencedDependencies = new ArrayList<>();
    referencedDependencies.addAll(resolveTags(packageSynchronizer, templateTags));
    referencedDependencies.addAll(resolveTags(packageSynchronizer, objectTags));

    return referencedDependencies;
  }

  private <T extends Tag> List<PackageDependency> resolveTags(
      PackageSynchronizer packageSynchronizer,
      List<T> tags) throws NodeException {
    List<PackageDependency> referencedDependencies = new ArrayList<>();

    for (T tag : tags) {
      PackageDependency dependency = new PackageDependency.Builder()
          .withType(getSynchronizationType(tag))
          .withGlobalId(tag.getGlobalId().toString())
          .withName(tag.getName())
          .withIsInPackage(
              isInPackage(packageSynchronizer, getSynchronizationClass(tag), resolveUuid(tag)))
          .build();

      referencedDependencies.add(dependency);
    }

    return referencedDependencies;
  }

  private String resolveUuid(Tag tag) throws NodeException {
    if (tag instanceof ObjectTag) {
      // use definition id (as opposed to the instance id)
      ObjectTagDefinition objectTagDefinition = ((ObjectTag) tag).getDefinition();
      return objectTagDefinition.getGlobalId().toString();
    }

    return tag.getConstruct().getGlobalId().toString();
  }

  private Class<? extends SynchronizableNodeObject> getSynchronizationClass(Tag tag)
      throws NodeException {
    if (tag instanceof ObjectTag) {
      return ObjectTagDefinition.class;
    } else if (tag instanceof TemplateTag) {
      return Construct.class;
    }

    throw new NodeException("Could not resolve appropriate synchronization class for tag " + tag.getGlobalId());
  }

  private Type getSynchronizationType(Tag tag) {
    if (tag instanceof ObjectTag) {
      return Type.OBJECT_TAG_DEFINITION;
    } else if (tag instanceof TemplateTag) {
      return Type.TEMPLATE_TAG;
    }

    return Type.UNKNOWN;
  }


}