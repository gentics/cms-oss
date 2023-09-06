package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
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
			List<PackageDependency> references = resolveReferences(template);

			PackageDependency dependency = new PackageDependency.Builder().withGlobalId(
							template.getGlobalId().toString()).withName(template.getName()).withType(Type.TEMPLATE)
					.withDependencies(references).build();

			resolvedDependencyList.add(dependency);
		}

		return resolvedDependencyList;
	}


	private List<PackageDependency> resolveReferences(Template template) throws NodeException {
		List<TemplateTag> templateTags = new ArrayList<>(template.getTemplateTags().values());
		List<ObjectTag> objectTags = new ArrayList<>(template.getObjectTags().values());

		List<PackageDependency> referencedDependencies = new ArrayList<>();
		referencedDependencies.addAll(resolveTemplateTags(templateTags));
		referencedDependencies.addAll(resolveObjectTags(objectTags));

		return referencedDependencies;
	}

	private List<PackageDependency> resolveObjectTags(List<ObjectTag> objectTags)
			throws NodeException {
		List<PackageDependency> referencedDependencies = new ArrayList<>();

		for (ObjectTag tag : objectTags) {
			ObjectTagDefinition objectTagDefinition = tag.getDefinition();

			PackageDependency dependency = new PackageDependency.Builder().withGlobalId(
							objectTagDefinition.getGlobalId().toString()).withName(objectTagDefinition.getName())
					.withType(Type.OBJECT_TAG_DEFINITION).withIsInPackage(
							isInPackage(ObjectTagDefinition.class, objectTagDefinition.getGlobalId().toString()))
					.build();

			referencedDependencies.add(dependency);
		}

		return referencedDependencies;
	}

	private List<PackageDependency> resolveTemplateTags(List<TemplateTag> templateTags)
			throws NodeException {
		List<PackageDependency> referencedDependencies = new ArrayList<>();

		for (TemplateTag tag : templateTags) {
			Construct construct = tag.getConstruct();

			PackageDependency dependency = new PackageDependency.Builder().withGlobalId(
							construct.getGlobalId().toString()).withName(construct.getName().toString())
					.withType(Type.CONSTRUCT)
					.withIsInPackage(isInPackage(Construct.class, construct.getGlobalId().toString()))
					.build();

			referencedDependencies.add(dependency);
		}

		return referencedDependencies;
	}

}
