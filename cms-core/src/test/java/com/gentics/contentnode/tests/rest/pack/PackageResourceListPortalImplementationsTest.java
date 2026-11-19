package com.gentics.contentnode.tests.rest.pack;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PortalImplementationSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.devtools.PortalImplementationInPackage;
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
 * Sorting and filtering tests for {@link PackageResource#listPortalImplementations(String, FilterParameterBean, SortParameterBean, PagingParameterBean, EmbedParameterBean, PermsParameterBean)}
 */
public class PackageResourceListPortalImplementationsTest extends AbstractListSortAndFilterTest<PortalImplementationInPackage> {
	protected static String packageName;

	/**
	 * Counter to make keywords and names unique
	 */
	protected static AtomicInteger counter = new AtomicInteger();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();
		Synchronizer.start();

		packageName = randomStringGenerator.generate(5, 10);
		operate(() -> Synchronizer.addPackage(packageName));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<PortalImplementationInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("keyword", PortalImplementationInPackage::getKeyword),
				Pair.of("name", PortalImplementationInPackage::getName)
		);
		List<Pair<String, Function<PortalImplementationInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("keyword", PortalImplementationInPackage::getKeyword),
				Pair.of("name", PortalImplementationInPackage::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected PortalImplementationInPackage createItem() throws NodeException {
		int index = counter.incrementAndGet();
		PortalImplementation item = Builder.create(PortalImplementation.class, impl -> {
			impl.setKeyword(randomStringGenerator.generate(5, 10) + index);
			impl.setName(randomStringGenerator.generate(5, 10) + index);
			impl.setType(PortalImplementationModel.Type.Java);
		}).build();

		consume(i -> Synchronizer.getPackage(packageName).synchronize(i, true), item);

		return execute(i -> {
			return PortalImplementationSynchronizer.TRANSFORM2REST.apply(new PackageObject<PortalImplementation>(i, packageName));
		}, item);
	}

	@Override
	protected AbstractListResponse<PortalImplementationInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().listPortalImplementations(packageName, filter, sort, paging, new EmbedParameterBean(),
				new PermsParameterBean());
	}
}
