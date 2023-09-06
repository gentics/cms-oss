package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructResolver extends AbstractDependencyResolver {

	private static final Class<Construct> CLAZZ = Construct.class;

	@Override
	public List<PackageDependency> resolve()
			throws NodeException {
		List<PackageObject<Construct>> packageObjects = synchronizer.getObjects(CLAZZ);
		List<PackageDependency> resolvedDependencyList = new ArrayList<>();

		for (PackageObject<Construct> packageObject : packageObjects) {
			Construct construct = packageObject.getObject();

			List<PackageDependency> references = resolveReferences(construct,
					Arrays.asList(Part.SELECTSINGLE, Part.SELECTMULTIPLE));

			PackageDependency dependency = new PackageDependency.Builder()
					.withGlobalId(construct.getGlobalId().toString())
					.withName(construct.getName().toString())
					.withKeyword(construct.getKeyword())
					.withType(Type.CONSTRUCT)
					.withDependencies(references)
					.build();

			resolvedDependencyList.add(dependency);
		}

		return resolvedDependencyList;
	}

	private List<PackageDependency> resolveReferences(Construct construct, List<Integer> dependencies)
			throws NodeException {
		List<Part> referencedParts = construct.getParts().stream().filter(
						part -> dependencies.stream()
								.anyMatch(type -> type == part.getPartTypeId()))
				.collect(Collectors.toList());

		List<PackageDependency> referencedDependencies = new ArrayList<>();

		for (Part part : referencedParts) {
			Datasource datasource = getDatasourceObject(part.getInfoInt());

			PackageDependency referencedDependency = new PackageDependency.Builder()
					.withGlobalId(datasource.getGlobalId().toString())
					.withName(datasource.getName())
					.withIsInPackage(
							isInPackage(Datasource.class, datasource.getId().toString()))
					.withType(Type.DATASOURCE)
					.build();

			referencedDependencies.add(referencedDependency);
		}

		return referencedDependencies;
	}

	private static Datasource getDatasourceObject(int datasourceId) throws NodeException {
		Transaction transaction = TransactionManager.getCurrentTransaction();
		return transaction.getObject(Datasource.class, datasourceId);
	}

}
