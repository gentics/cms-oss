package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObjectPropertyResolver extends AbstractDependencyResolver {

	private static final Class<ObjectTagDefinition> CLAZZ = ObjectTagDefinition.class;

	@Override
	public List<PackageDependency> resolve()
			throws NodeException {
		List<PackageObject<ObjectTagDefinition>> objectTagDefinitions = synchronizer.getObjects(CLAZZ);
		List<PackageDependency> resolvedDependencyList = new ArrayList<>();

		for (PackageObject<ObjectTagDefinition> packageObject : objectTagDefinitions) {
			ObjectTagDefinition objectTagDefinition = packageObject.getObject();
			List<PackageDependency> references = Collections.singletonList(
					getObjectTagDefinitionAsDependency(objectTagDefinition.getObjectTag()));

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


	private PackageDependency getObjectTagDefinitionAsDependency(
			ObjectTag objectTagDefinition) throws NodeException {
		String constructId = objectTagDefinition.getConstruct().getGlobalId().toString();

		return new PackageDependency.Builder()
				.withType(Type.CONSTRUCT)
				.withGlobalId(constructId)
				.withName(objectTagDefinition.getName())
				.withIsInPackage(isInPackage(Construct.class, constructId))
				.build();
	}

}
