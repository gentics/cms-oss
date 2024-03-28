package com.gentics.contentnode.tests.rest.pack;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.rest.model.devtools.DatasourceInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PackageResource#listDatasources(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class PackageResourceListDatasourcesTest extends AbstractListSortAndFilterTest<DatasourceInPackage> {
	protected static String packageName;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();
		Synchronizer.start();

		packageName = randomStringGenerator.generate(5, 10);
		operate(() -> Synchronizer.addPackage(packageName));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<DatasourceInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", DatasourceInPackage::getName)
		);
		List<Pair<String, Function<DatasourceInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", DatasourceInPackage::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected DatasourceInPackage createItem() throws NodeException {
		Datasource item = Builder.create(Datasource.class, ds -> {
			ds.setName(randomStringGenerator.generate(5, 10));
			ds.setSourceType(getRandomEntry(SourceType.values()));
		}).build();

		consume(i -> Synchronizer.getPackage(packageName).synchronize(i, true), item);

		return execute(c -> {
			DatasourceInPackage ip = new DatasourceInPackage();
			ip.setPackageName(packageName);
			Datasource.NODE2REST.apply(item, ip);
			return ip;
		}, item);
	}

	@Override
	protected AbstractListResponse<DatasourceInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().listDatasources(packageName, filter, sort, paging, new PermsParameterBean());
	}
}
