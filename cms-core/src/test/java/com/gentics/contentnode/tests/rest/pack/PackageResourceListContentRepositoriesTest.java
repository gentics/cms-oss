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
import com.gentics.contentnode.devtools.ContentRepositorySynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.devtools.ContentRepositoryInPackage;
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
 * Sorting and filtering tests for {@link PackageResource#listContentRepositories(String, FilterParameterBean, SortParameterBean, PagingParameterBean, EmbedParameterBean, PermsParameterBean)}
 */
public class PackageResourceListContentRepositoriesTest extends AbstractListSortAndFilterTest<ContentRepositoryInPackage> {
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
		List<Pair<String, Function<ContentRepositoryInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", ContentRepositoryInPackage::getName),
				Pair.of("crType", item -> item.getCrType().name())
		);
		List<Pair<String, Function<ContentRepositoryInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", ContentRepositoryInPackage::getName),
				Pair.of("crType", item -> item.getCrType().name())
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ContentRepositoryInPackage createItem() throws NodeException {
		ContentRepository item = Builder.create(ContentRepository.class, cr -> {
			cr.setName(randomStringGenerator.generate(5, 10));
			cr.setCrType(getRandomEntry(Type.values()));
			cr.setDbType(randomStringGenerator.generate(5, 10));
			cr.setUsername(randomStringGenerator.generate(5, 10));
			cr.setUrl(randomStringGenerator.generate(10, 20));
			cr.setBasepath(randomStringGenerator.generate(5, 10));
			cr.setInstantPublishing(random.nextBoolean());
			cr.setLanguageInformation(random.nextBoolean());
			cr.setPermissionInformation(random.nextBoolean());
			cr.setDiffDelete(random.nextBoolean());
		}).build();

		consume(i -> Synchronizer.getPackage(packageName).synchronize(i, true), item);

		return execute(c -> {
			return ContentRepositorySynchronizer.TRANSFORM2REST
					.apply(new PackageObject<ContentRepository>(item, packageName));
		}, item);
	}

	@Override
	protected AbstractListResponse<ContentRepositoryInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().listContentRepositories(packageName, filter, sort, paging, new EmbedParameterBean(), new PermsParameterBean());
	}
}
