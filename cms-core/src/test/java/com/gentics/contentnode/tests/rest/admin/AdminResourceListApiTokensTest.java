package com.gentics.contentnode.tests.rest.admin;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getAdminResource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.auth.ResolvableApiTokenDataModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationRequest;
import com.gentics.contentnode.rest.model.token.ApiTokenDataModel;
import com.gentics.contentnode.rest.resource.AdminResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;

/**
 * Sorting and filtering tests for {@link AdminResource#listAPITokens(FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class AdminResourceListApiTokensTest extends AbstractListSortAndFilterTest<ApiTokenDataModel> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ApiTokenDataModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", ApiTokenDataModel::getName),
				Pair.of("cdate", item -> addLeadingZeros(item.getCdate())),
				Pair.of("expires", item -> addLeadingZeros(item.getExpires())),
				Pair.of("lastUsed", item -> addLeadingZeros(item.getLastUsed())),
				Pair.of("valid", item -> Boolean.toString(item.isValid()))
		);
		List<Pair<String, Function<ApiTokenDataModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", ApiTokenDataModel::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ApiTokenDataModel createItem() throws NodeException {
		return supply(() -> {
			String token = ApiTokenFactory.createToken();
			String name = randomStringGenerator.generate(10, 20);
			int expires = 0;
			if (random.nextBoolean()) {
				long diff = random.nextLong(-3600, 3600);
				expires = (int) Instant.now().plus(diff, ChronoUnit.SECONDS).getEpochSecond();
			}
			ResolvableApiTokenDataModel tokenDataModel = ApiTokenFactory.create(new ApiTokenCreationRequest().setName(name).setExpires(expires), 1, token);

			// fake cdate and last_used
			int cdate = (int) Instant.now().minus(random.nextLong(3600), ChronoUnit.SECONDS).getEpochSecond();
			int lastUsed = (int) Instant.now().minus(random.nextLong(3600), ChronoUnit.SECONDS).getEpochSecond();
			update("UPDATE api_token SET cdate = ?, last_used = ? WHERE id = ?", cdate, lastUsed, tokenDataModel.getId());
			tokenDataModel.setCdate(cdate);
			tokenDataModel.setLastUsed(lastUsed);

			return tokenDataModel;
		});
	}

	@Override
	protected AbstractListResponse<ApiTokenDataModel> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return getAdminResource().listAPITokens(filter, sort, paging);
	}
}
