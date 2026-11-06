package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.NodeFeatureModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link NodeResource#availableFeatures(FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
@GCNFeature(set = { Feature.CONTENTFILE_AUTO_OFFLINE, Feature.ALWAYS_LOCALIZE, Feature.DISABLE_INSTANT_DELETE,
		Feature.PUBLISH_FOLDER_STARTPAGE, Feature.LIVE_URLS_PER_NODE, Feature.LINK_CHECKER,
		Feature.FORMS, Feature.ASSET_MANAGEMENT })
public class NodeResourceAvailableFeaturesTest extends AbstractListSortAndFilterTest<NodeFeatureModel> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// "id", "name", "description"
		List<Pair<String, Function<NodeFeatureModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId().ordinal())),
				Pair.of("name", NodeFeatureModel::getName),
				Pair.of("description", NodeFeatureModel::getDescription)
		);
		List<Pair<String, Function<NodeFeatureModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> item.getId().name()),
				Pair.of("name", NodeFeatureModel::getName),
				Pair.of("description", NodeFeatureModel::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected NodeFeatureModel createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super NodeFeatureModel> items) throws NodeException {
		operate(() -> {
			for (Feature feature : Feature.values()) {
				if (feature.isPerNode()) {
					NodeFeature f = ModelBuilder.getFeature(feature);
					items.add(new NodeFeatureModel().setId(f).setName(I18NHelper.get("feature." + f.name()))
							.setDescription(I18NHelper.get("feature." + f.name() + ".help")));
				}
			}
		});
	}

	@Override
	protected AbstractListResponse<NodeFeatureModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new NodeResourceImpl().availableFeatures(filter, sort, paging);
	}
}
