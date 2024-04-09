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
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.model.devtools.ConstructInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PackageResource#listConstructs(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class PackageResourceListConstructsTest extends AbstractListSortAndFilterTest<ConstructInPackage> {
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
		List<Pair<String, Function<ConstructInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("keyword", ConstructInPackage::getKeyword),
				Pair.of("name", ConstructInPackage::getName),
				Pair.of("description", ConstructInPackage::getDescription)
		);
		List<Pair<String, Function<ConstructInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("keyword", ConstructInPackage::getKeyword),
				Pair.of("name", ConstructInPackage::getName),
				Pair.of("description", ConstructInPackage::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ConstructInPackage createItem() throws NodeException {
		Construct construct = Builder.create(com.gentics.contentnode.object.Construct.class, c -> {
			c.setName(randomStringGenerator.generate(5, 10), 1);
			c.setName(randomStringGenerator.generate(5, 10), 2);
			c.setKeyword(randomStringGenerator.generate(5, 10));
			c.setDescription(randomStringGenerator.generate(10, 20), 1);
			c.setDescription(randomStringGenerator.generate(10, 20), 2);
		}).build();

		consume(c -> Synchronizer.getPackage(packageName).synchronize(c, true), construct);

		return execute(c -> {
			ConstructInPackage cip = new ConstructInPackage();
			cip.setPackageName(packageName);
			Construct.NODE2REST.apply(c, cip);
			return cip;
		}, construct);
	}

	@Override
	protected AbstractListResponse<ConstructInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().listConstructs(packageName, filter, sort, paging, new PermsParameterBean(), new EmbedParameterBean());
	}
}
