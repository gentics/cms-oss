package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;

/**
 * Sorting and filtering tests for {@link NodeResource#availableLanguages(String, FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class NodeResourceAvailableLanguagesTest extends AbstractListSortAndFilterTest<ContentLanguage> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ContentLanguage, String>>> attributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("globalId", ContentLanguage::getGlobalId),
				Pair.of("name", ContentLanguage::getName),
				Pair.of("code", ContentLanguage::getCode)
		);
		return data(attributes, attributes);
	}

	@Override
	protected ContentLanguage createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super Object> items) throws NodeException {
		operate(() ->  {
			for (com.gentics.contentnode.object.ContentLanguage lang : LanguageFactory.languagesPerCode().values()) {
				items.add(com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(lang));
			}
		});
	}

	@Override
	protected AbstractListResponse<ContentLanguage> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new NodeResourceImpl().availableLanguages(Integer.toString(node.getId()), filter, sort, paging);
	}
}
