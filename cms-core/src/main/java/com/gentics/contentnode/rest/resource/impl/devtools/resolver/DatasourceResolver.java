package com.gentics.contentnode.rest.resource.impl.devtools.resolver;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.List;

public class DatasourceResolver extends AbstractDependencyResolver {

	private static final Class<Datasource> CLAZZ = Datasource.class;

	@Override
	public List<PackageDependency> resolve() throws NodeException {
		List<PackageObject<Datasource>> datasources = synchronizer.getObjects(CLAZZ);

		List<PackageDependency> resolvedDependencyList = new ArrayList<>();

		for (PackageObject<Datasource> datasource : datasources) {
			Datasource ds = datasource.getObject();
			PackageDependency dependency = new PackageDependency.Builder()
					.withGlobalId(datasource.getGlobalId().toString())
					.withName(ds.getName())
					.withType(Type.DATASOURCE)
					.build();

			resolvedDependencyList.add(dependency);
		}

		return resolvedDependencyList;
	}
}
