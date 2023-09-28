package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.devtools.dependency.AbstractDependencyModel;
import com.gentics.contentnode.rest.model.devtools.dependency.PackageDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.ReferenceDependency;
import com.gentics.contentnode.rest.model.devtools.dependency.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructResolver extends AbstractDependencyResolver {

	private static final Class<Construct> CLAZZ = Construct.class;

	private static Datasource getDatasourceObject(int datasourceId) throws NodeException {
		Transaction transaction = TransactionManager.getCurrentTransaction();
		return transaction.getObject(Datasource.class, datasourceId);
	}

	@Override
	public List<PackageDependency> resolve()
			throws NodeException {
		List<PackageObject<Construct>> packageObjects = synchronizer.getObjects(CLAZZ);
		List<PackageDependency> resolvedDependencyList = new ArrayList<>();

		for (PackageObject<Construct> packageObject : packageObjects) {
			Construct construct = packageObject.getObject();

			List<ReferenceDependency> references = resolveReferences(construct,
					Arrays.asList(Part.SELECTSINGLE, Part.SELECTMULTIPLE));

			PackageDependency dependency = new PackageDependency.Builder<>(
					PackageDependency.class)
					.withGlobalId(construct.getGlobalId().toString())
					.withName(construct.getName().toString())
					.withKeyword(construct.getKeyword())
					.withType(Type.CONSTRUCT)
					.build();

			dependency.withReferenceDependencies(references);

			resolvedDependencyList.add(dependency);
		}

		return resolvedDependencyList;
	}

	private List<ReferenceDependency> resolveReferences(Construct construct,
			List<Integer> dependencies)
			throws NodeException {
		List<Part> referencedParts = construct.getParts().stream().filter(
						part -> dependencies.stream()
								.anyMatch(type -> type == part.getPartTypeId()))
				.collect(Collectors.toList());

		List<ReferenceDependency> referencedDependencies = new ArrayList<>();

		for (Part part : referencedParts) {
			Datasource datasource = getDatasourceObject(part.getInfoInt());

			ReferenceDependency referencedDependency = new AbstractDependencyModel.Builder<>(
					ReferenceDependency.class)
					.withGlobalId(datasource.getGlobalId().toString())
					.withName(datasource.getName())
					.withType(Type.DATASOURCE)
					.build()
					.withIsInPackage(isInPackage(Datasource.class, datasource.getId().toString()));

			referencedDependencies.add(referencedDependency);
		}

		return referencedDependencies;
	}

}
