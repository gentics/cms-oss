package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.devtools.dependency.PackageDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.ReferenceDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.Type;
import java.util.ArrayList;
import java.util.List;

public class TemplateResolver extends AbstractDependencyResolver {

	private static final Class<Template> CLAZZ = Template.class;

	@Override
	public List<PackageDependency> resolve() throws NodeException {
		List<PackageObject<Template>> packageTemplates = synchronizer.getObjects(CLAZZ);
		List<PackageDependency> resolvedDependencyList = new ArrayList<>();

		for (PackageObject<Template> packageObject : packageTemplates) {
			Template template = packageObject.getObject();
			List<ReferenceDependency> references = resolveReferences(template);

			PackageDependency dependency = new PackageDependency.Builder<>(
					PackageDependency.class).withGlobalId(
							template.getGlobalId().toString()).withName(template.getName()).withType(Type.TEMPLATE)
					.build().withReferenceDependencies(references);

			resolvedDependencyList.add(dependency);
		}

		return resolvedDependencyList;
	}


	private List<ReferenceDependency> resolveReferences(Template template) throws NodeException {
		List<TemplateTag> templateTags = new ArrayList<>(template.getTemplateTags().values());
		List<ObjectTag> objectTags = new ArrayList<>(template.getObjectTags().values());

		List<ReferenceDependency> referencedDependencies = new ArrayList<>();
		referencedDependencies.addAll(resolveTemplateTags(templateTags));
		referencedDependencies.addAll(resolveObjectTags(objectTags));

		return referencedDependencies;
	}

	private List<ReferenceDependency> resolveObjectTags(List<ObjectTag> objectTags)
			throws NodeException {
		List<ReferenceDependency> referencedDependencies = new ArrayList<>();

		for (ObjectTag tag : objectTags) {
			ObjectTagDefinition objectTagDefinition = tag.getDefinition();

			ReferenceDependency dependency = new PackageDependency.Builder<>(
					ReferenceDependency.class).withGlobalId(
							objectTagDefinition.getGlobalId().toString())
					.withName(objectTagDefinition.getName())
					.withType(Type.OBJECT_TAG_DEFINITION)
					.build().withIsInOtherPackage(
							isInPackage(ObjectTagDefinition.class, objectTagDefinition.getGlobalId().toString()));

			referencedDependencies.add(dependency);
		}

		return referencedDependencies;
	}

	private List<ReferenceDependency> resolveTemplateTags(List<TemplateTag> templateTags)
			throws NodeException {
		List<ReferenceDependency> referencedDependencies = new ArrayList<>();

		for (TemplateTag tag : templateTags) {
			Construct construct = tag.getConstruct();

			ReferenceDependency dependency = new PackageDependency.Builder<>(
					ReferenceDependency.class).withGlobalId(
							construct.getGlobalId().toString()).withName(construct.getName().toString())
					.withType(Type.CONSTRUCT)
					.build()
					.withIsInPackage(isInPackage(Construct.class, construct.getGlobalId().toString()));

			referencedDependencies.add(dependency);
		}

		return referencedDependencies;
	}

}
