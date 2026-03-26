package com.gentics.contentnode.tests.rest.node;

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
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link NodeResource#features(String, FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
@GCNFeature(set = { Feature.CONTENTFILE_AUTO_OFFLINE, Feature.ALWAYS_LOCALIZE, Feature.DISABLE_INSTANT_DELETE,
		Feature.PUBLISH_FOLDER_STARTPAGE, Feature.LIVE_URLS_PER_NODE, Feature.LINK_CHECKER,
		Feature.FORMS, Feature.ASSET_MANAGEMENT })
public class NodeResourceFeaturesTest extends AbstractListSortAndFilterTest<NodeFeature> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
		node = Builder.update(node, update -> {
			for (Feature feature : Feature.values()) {
				if (feature.isPerNode()) {
					update.activateFeature(feature);
				}
			}
		}).build();
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<NodeFeature, String>>> attributes = Arrays.asList(
				Pair.of("name", NodeFeature::name)
		);
		return data(attributes, attributes);
	}

	@Override
	protected NodeFeature createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super NodeFeature> items) throws NodeException {
		for (Feature feature : Feature.values()) {
			if (feature.isPerNode()) {
				items.add(ModelBuilder.getFeature(feature));
			}
		}
	}

	@Override
	protected AbstractListResponse<NodeFeature> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new NodeResourceImpl().features(Integer.toString(node.getId()), filter, sort, paging);
	}
}
