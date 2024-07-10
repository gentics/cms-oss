package com.gentics.contentnode.tests.rest.datasource;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.DatasourceResource;
import com.gentics.contentnode.rest.resource.impl.DatasourceResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;

/**
 * Sorting and filtering tests for {@link DatasourceResource#constructs(String, SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
public class DatasourceResourceConstructsTest extends AbstractListSortAndFilterTest<Construct> {
	public static Datasource datasource;

	public static List<ConstructCategory> categories = new ArrayList<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		datasource = Builder.create(Datasource.class, ds -> {
			ds.setName(randomStringGenerator.generate(5, 10));
			ds.setSourceType(SourceType.staticDS);
		}).build();

		for (int i = 0; i < 5; i++) {
			categories.add(Builder.create(ConstructCategory.class, cat -> {
				cat.setName(randomStringGenerator.generate(5, 10), 1);
				cat.setName(randomStringGenerator.generate(5, 10), 2);
			}).build());
		}
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Construct, String>>> attributes = Arrays.asList(
				Pair.of("id", construct -> AbstractListSortAndFilterTest.addLeadingZeros(construct.getId())),
				Pair.of("globalId", Construct::getGlobalId),
				Pair.of("keyword", Construct::getKeyword),
				Pair.of("name", Construct::getName),
				Pair.of("description", Construct::getDescription)
				// TODO
//				Pair.of("category", Construct::getCategory)
		);
		return data(attributes, attributes);
	}

	@Override
	protected Construct createItem() throws NodeException {
		return supply(() -> {
			return com.gentics.contentnode.object.Construct.TRANSFORM2REST
					.apply(Builder.create(com.gentics.contentnode.object.Construct.class, c -> {
						c.setName(randomStringGenerator.generate(5, 10), 1);
						c.setName(randomStringGenerator.generate(5, 10), 2);
						c.setKeyword(randomStringGenerator.generate(5, 10));
						c.setDescription(randomStringGenerator.generate(10, 20), 1);
						c.setDescription(randomStringGenerator.generate(10, 20), 2);

						c.getParts().add(Builder.create(Part.class, p -> {
							p.setKeyname(randomStringGenerator.generate(5, 10));
							p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(SingleSelectPartType.class));
							p.setInfoInt(datasource.getId());
						}).doNotSave().build());

						ConstructCategory category = null;
						if (random.nextBoolean()) {
							category = getRandomEntry(categories);
							c.setConstructCategoryId(category.getId());
						}
					}).build());
		});
	}

	@Override
	protected AbstractListResponse<Construct> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new DatasourceResourceImpl().constructs(Integer.toString(datasource.getId()), sort, filter, paging, new EmbedParameterBean());
	}
}
