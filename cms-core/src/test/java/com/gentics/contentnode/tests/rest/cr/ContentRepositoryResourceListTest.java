package com.gentics.contentnode.tests.rest.cr;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ContentRepositoryResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class ContentRepositoryResourceListTest extends AbstractListSortAndFilterTest<ContentRepositoryModel> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ContentRepositoryModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("globalId", ContentRepositoryModel::getGlobalId),
				Pair.of("name", ContentRepositoryModel::getName),
				Pair.of("crType", item -> item.getCrType().name()),
				Pair.of("dbType", item -> item.getDbType()),
				Pair.of("username", ContentRepositoryModel::getUsername),
				Pair.of("url", ContentRepositoryModel::getUrl),
				Pair.of("basepath", ContentRepositoryModel::getBasepath),
				Pair.of("instantPublishing", item -> Boolean.toString(item.getInstantPublishing())),
				Pair.of("languageInformation", item -> Boolean.toString(item.getLanguageInformation())),
				Pair.of("permissionInformation", item -> Boolean.toString(item.getPermissionInformation())),
				Pair.of("diffDelete", item -> Boolean.toString(item.getDiffDelete())),
				Pair.of("checkDate", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getCheckDate())),
				Pair.of("checkStatus", item -> addLeadingOrder(item.getCheckStatus())),
				Pair.of("statusDate", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getStatusDate())),
				Pair.of("dataStatus", item -> addLeadingOrder(item.getDataStatus()))
		);

		List<Pair<String, Function<ContentRepositoryModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("globalId", ContentRepositoryModel::getGlobalId),
				Pair.of("name", ContentRepositoryModel::getName),
				Pair.of("crType", item -> item.getCrType().name()),
				Pair.of("dbType", item -> item.getDbType()),
				Pair.of("username", ContentRepositoryModel::getUsername),
				Pair.of("url", ContentRepositoryModel::getUrl),
				Pair.of("basepath", ContentRepositoryModel::getBasepath)
		);

		return data(sortAttributes, filterAttributes);
	}

	protected static String addLeadingOrder(Status status) {
		return (status.code() + 1) + "_" + status.name();
	}

	@Override
	protected ContentRepositoryModel createItem() throws NodeException {
		ContentRepositoryModel model = supply(() -> ContentRepository.TRANSFORM2REST.apply(Builder.create(ContentRepository.class, cr -> {
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
		}).build()));

		operate(() -> {
			DBUtils.update(
					"UPDATE contentrepository SET checkstatus = ?, checkdate = ?, datastatus = ?, statusdate = ? WHERE id = ?",
					getRandomEntry(Status.values()).code(), random.nextInt(1000), getRandomEntry(Status.values()).code(),
					random.nextInt(1000), model.getId());
		});

		return model;
	}

	@Override
	protected AbstractListResponse<ContentRepositoryModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ContentRepositoryResourceImpl().list(filter, sort, paging, new PermsParameterBean());
	}
}
