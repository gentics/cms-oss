package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.PartTypeFactory;
import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.PartTypeResource;
import com.gentics.contentnode.rest.resource.impl.PartTypeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PartTypeListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

/**
 * Sorting and filtering tests for {@link PartTypeResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PartTypeListParameterBean)}
 */
public class PartTypeResourceListTest extends AbstractListSortAndFilterTest<PartType> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// "id", "name", "javaclass", "deprecated"
		List<Pair<String, Function<PartType, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", PartType::getName),
				Pair.of("javaClass", PartType::getJavaClass),
				Pair.of("deprecated", item -> Boolean.toString(item.isDeprecated()))
		);

		// "name", "description", "javaclass", "deprecated"
		List<Pair<String, Function<PartType, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", PartType::getName),
				Pair.of("description", PartType::getDescription),
				Pair.of("javaClass", PartType::getJavaClass),
				Pair.of("deprecated", item -> Boolean.toString(item.isDeprecated()))
		);
		return data(sortAttributes, filterAttributes);
	}


	@Override
	protected PartType createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super PartType> items) throws NodeException {
		operate(() -> {
			PartTypeFactory factory = PartTypeFactory.getInstance();

			DBUtils.select("SELECT * FROM `type`", rs -> {
				while (rs.next()) {
					PartType partType = new PartType()
						.setId(rs.getInt("id"))
						.setName(rs.getString("name"))
						.setDescription(rs.getString("description"))
						.setJavaClass(rs.getString("javaclass"))
						.setDeprecated(rs.getBoolean("deprecated"));

					if (factory.isAvailable(partType.getId())) {
						items.add(partType);
					}
				}
				return null;
			});
		});
	}

	@Override
	protected AbstractListResponse<PartType> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		PartTypeListParameterBean partTypeFilter = new PartTypeListParameterBean();

		if (filterBy != null) {
			switch (filterBy) {
			case "deprecated":
				partTypeFilter.deprecated = Boolean.valueOf(filter.query);
				filter.query = null;
				break;
			}
		}

		return new PartTypeResourceImpl().list(filter, sort, paging, partTypeFilter);
	}
}