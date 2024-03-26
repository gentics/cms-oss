package com.gentics.contentnode.tests.rest.ml;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.rest.model.MarkupLanguage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.MarkupLanguageResource;
import com.gentics.contentnode.rest.resource.impl.MarkupLanguageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link MarkupLanguageResource#list(SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
@GCNFeature(set = {Feature.FORMS})
public class MarkupLanguageResourceListTest extends AbstractListSortAndFilterTest<MarkupLanguage> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// "id", "name", "extension", "contenttype", "feature", "excludeFromPublishing"
		List<Pair<String, Function<MarkupLanguage, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", MarkupLanguage::getName),
				Pair.of("extension", MarkupLanguage::getExtension),
				Pair.of("contentType", MarkupLanguage::getContentType),
				Pair.of("feature", MarkupLanguage::getFeature),
				Pair.of("excludeFromPublishing", item -> Boolean.toString(item.isExcludeFromPublishing()))
		);

		// "id", "name", "extension", "contenttype", "feature"
		List<Pair<String, Function<MarkupLanguage, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", MarkupLanguage::getName),
				Pair.of("extension", MarkupLanguage::getExtension),
				Pair.of("contentType", MarkupLanguage::getContentType),
				Pair.of("feature", MarkupLanguage::getFeature)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected MarkupLanguage createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super Object> items) throws NodeException {
		operate(t ->  {
			List<com.gentics.contentnode.object.MarkupLanguage> mls = t.getObjects(
					com.gentics.contentnode.object.MarkupLanguage.class,
					DBUtils.select("SELECT id FROM ml", DBUtils.IDS));
			for (com.gentics.contentnode.object.MarkupLanguage ml : mls) {
				items.add(com.gentics.contentnode.object.MarkupLanguage.TRANSFORM2REST.apply(ml));
			}
		});
	}

	@Override
	protected AbstractListResponse<MarkupLanguage> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new MarkupLanguageResourceImpl().list(sort, filter, paging);
	}
}
