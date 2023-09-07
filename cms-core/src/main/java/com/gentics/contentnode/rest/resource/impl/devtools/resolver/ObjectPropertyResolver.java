package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.rest.model.devtools.dependency.PackageDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.ReferenceDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.Type;
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
			List<ReferenceDependency> references = Collections.singletonList(
					getObjectTagDefinitionAsDependency(objectTagDefinition.getObjectTag()));

			PackageDependency dependency = new PackageDependency.Builder<>(PackageDependency.class)
					.withGlobalId(objectTagDefinition.getGlobalId().toString())
					.withName(objectTagDefinition.getName())
					.withType(Type.OBJECT_TAG_DEFINITION)
					.withKeyword(objectTagDefinition.getObjectTag().getTypeKeyword())
					.build();

			dependency.withReferenceDependencies(references);

			resolvedDependencyList.add(dependency);
		}

		return resolvedDependencyList;
	}


	private ReferenceDependency getObjectTagDefinitionAsDependency(
			ObjectTag objectTagDefinition) throws NodeException {
		Construct construct = objectTagDefinition.getConstruct();
		String constructId = construct.getGlobalId().toString();

		return new PackageDependency.Builder<>(ReferenceDependency.class)
				.withType(Type.CONSTRUCT)
				.withGlobalId(constructId)
				.withName(construct.getName().toString())
				.build()
				.withIsInPackage(
						isInPackage(Construct.class, constructId));

	}

}
