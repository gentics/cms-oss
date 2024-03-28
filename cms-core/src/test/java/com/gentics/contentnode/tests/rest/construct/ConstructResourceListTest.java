package com.gentics.contentnode.tests.rest.construct;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ConstructParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ConstructResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, ConstructParameterBean, PermsParameterBean)}
 */
public class ConstructResourceListTest extends AbstractListSortAndFilterTest<Construct> {
	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		operate(t -> {
			for (Node node : t.getObjects(Node.class, DBUtils.select("SELECt id FROM node", DBUtils.IDLIST))) {
				node.delete(true);
			}
		});

		operate(t -> {
			for (ObjectTagDefinition otDef : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag", DBUtils.IDLIST))) {
				otDef.delete(true);
			}
		});

		operate(t -> {
			for (com.gentics.contentnode.object.Construct construct : t.getObjects(com.gentics.contentnode.object.Construct.class, DBUtils.select("SELECT id FROM construct", DBUtils.IDLIST))) {
				construct.delete(true);
			}
		});
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Construct, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", construct -> AbstractListSortAndFilterTest.addLeadingZeros(construct.getId())),
				Pair.of("globalId", Construct::getGlobalId),
				Pair.of("keyword", Construct::getKeyword),
				Pair.of("name", Construct::getName),
				Pair.of("description", Construct::getDescription)
		);
		List<Pair<String, Function<Construct, String>>> filterAttributes = Arrays.asList(
				Pair.of("keyword", Construct::getKeyword),
				Pair.of("name", Construct::getName),
				Pair.of("description", Construct::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Construct createItem() throws NodeException {
		return supply(() -> {
			return com.gentics.contentnode.object.Construct.TRANSFORM2REST
					.apply(Builder.create(com.gentics.contentnode.object.Construct.class, c -> {
						c.setName(randomStringGenerator.generate(5, 10), 1);
						c.setName(randomStringGenerator.generate(5, 10), 2);
						c.setIconName(randomStringGenerator.generate(5));
						c.setKeyword(randomStringGenerator.generate(5, 10));
						c.setDescription(randomStringGenerator.generate(10, 20), 1);
						c.setDescription(randomStringGenerator.generate(10, 20), 2);
					}).build());
		});
	}

	@Override
	protected AbstractListResponse<Construct> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ConstructResourceImpl().list(filter, sort, paging, new ConstructParameterBean(), new PermsParameterBean(), new EmbedParameterBean());
	}
}
