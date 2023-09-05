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
import java.util.stream.Stream;

public class ConstructResolver extends AbstractDependencyResolver {

	private static final Class<Construct> CLAZZ = Construct.class;

	/**
	 * Utility method to map datasourceId to the uuid of a datasource
	 *
	 * @param datasourceId numeric id that should be mapped to an uuid
	 * @return the mapped uuid
	 * @throws NodeException
	 */
	private static String resolveUuid(int datasourceId) throws NodeException {
		Transaction transaction = TransactionManager.getCurrentTransaction();
		return transaction.getGlobalId(Datasource.class, datasourceId);
	}

	@Override
	public List<PackageDependency> resolve()
			throws NodeException {
		List<PackageObject<Construct>> packageObjects = synchronizer.getObjects(CLAZZ);
		List<PackageDependency> resolvedDependencyList = new ArrayList<>();

		for (PackageObject<Construct> packageObject : packageObjects) {
			Construct construct = packageObject.getObject();

			List<PackageDependency> references = Stream.concat(
					resolveReferences(construct, Arrays.asList(Part.SELECTSINGLE, Part.SELECTMULTIPLE),
							Type.DATASOURCE).stream(),
					resolveReferences(construct,
							Arrays.asList(Part.TEXT, Part.TEXTHMTL, Part.HTML, Part.URLFILE, Part.URLFOLDER,
									Part.URLPAGE, Part.HTMLLONG, Part.CHECKBOX, Part.VELOCITY),
							Type.CONSTRUCT).stream()
			).collect(Collectors.toList());

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

	private List<PackageDependency> resolveReferences(Construct construct, List<Integer> dependencies,
			Type dependencyType) throws NodeException {
		List<Part> referencedParts = construct.getParts().stream().filter(
						part -> dependencies.stream()
								.anyMatch(type -> type == part.getPartTypeId()))
				.collect(Collectors.toList());

		List<PackageDependency> referencedDependencies = new ArrayList<>();

		for (Part part : referencedParts) {
			PackageDependency referencedDependency = new PackageDependency.Builder()
					.withGlobalId(part.getGlobalId().toString())
					.withKeyword(part.getKeyname())
					.withName(part.getName().toString())
					.withIsInPackage(isInPackage(part, dependencyType))
					.withType(dependencyType)
					.build();

			referencedDependencies.add(referencedDependency);
		}

		return referencedDependencies;
	}

	private boolean isInPackage(Part part, Type dependencyType) throws NodeException {
		if (Type.DATASOURCE == dependencyType) {
			return isInPackage(Datasource.class, resolveUuid(part.getInfoInt()));
		}
		return isInPackage(Construct.class, part.getConstruct().getGlobalId().toString());
	}

}
