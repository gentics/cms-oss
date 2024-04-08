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
import com.gentics.contentnode.devtools.ContentRepositoryFragmentSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.devtools.ContentRepositoryFragmentInPackage;
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
 * Sorting and filtering tests for {@link PackageResource#listCrFragments(String, FilterParameterBean, SortParameterBean, PagingParameterBean, EmbedParameterBean, PermsParameterBean)}
 */
public class PackageResourceListCrFragmentsTest extends AbstractListSortAndFilterTest<ContentRepositoryFragmentInPackage> {
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
		List<Pair<String, Function<ContentRepositoryFragmentInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", ContentRepositoryFragmentInPackage::getName)
		);
		List<Pair<String, Function<ContentRepositoryFragmentInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", ContentRepositoryFragmentInPackage::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ContentRepositoryFragmentInPackage createItem() throws NodeException {
		CrFragment item = Builder.create(CrFragment.class, fr -> {
			fr.setName(randomStringGenerator.generate(5, 10));
		}).build();

		consume(i -> Synchronizer.getPackage(packageName).synchronize(i, true), item);

		return execute(c -> {
			return ContentRepositoryFragmentSynchronizer.TRANSFORM2REST
					.apply(new PackageObject<CrFragment>(c, packageName));
		}, item);
	}

	@Override
	protected AbstractListResponse<ContentRepositoryFragmentInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().listCrFragments(packageName, filter, sort, paging, new EmbedParameterBean(), new PermsParameterBean());
	}
}
