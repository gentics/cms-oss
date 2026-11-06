package com.gentics.contentnode.tests.rest.formtype;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterFormTypeConfigurationsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link FormResource#listFormTypeConfigurations(FilterFormTypeConfigurationsParameterBean, SortParameterBean, PagingParameterBean)}.
 */
@GCNFeature(set = { Feature.FORMS })
@ConfigBy(FormResourceListTypesTest.class)
public class FormResourceListTypesTest extends AbstractListSortAndFilterTest<FormTypeConfigurationModel> implements ConfigModifier {
	private static Node firstNode;

	private static Node secondNode;

	private static Node thirdNode;

	private final static FormTypeConfigurationMap formTypeConfigurationMap;

	private final static Map<String, Set<Integer>> nodeIdsPerType = new HashMap<>();

	static {
		formTypeConfigurationMap = new FormTypeConfigurationMap();
		IntStream.range(0, NUM_ITEMS).forEach(i -> {
			String type = randomLowercaseStringGenerator.generate(5, 10);

			FormTypeConfiguration config = new FormTypeConfiguration(
					Map.of("de", randomStringGenerator.generate(10, 20), "en", randomStringGenerator.generate(10, 20)),
					randomLowercaseStringGenerator.generate(5, 10));

			config.descriptionI18n = Map.of("de", randomStringGenerator.generate(10, 20), "en",
					randomStringGenerator.generate(10, 20));
			config.external = random.nextBoolean();
			config.submitOnce = random.nextBoolean();

			formTypeConfigurationMap.put(type, config);
		});
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		firstNode = supply(() -> createNode(Feature.FORMS));
		secondNode = supply(() -> createNode(Feature.FORMS));
		thirdNode = supply(() -> createNode(Feature.FORMS));

		// add the types randomly to the nodes, but make sure that each type is added to at
		// least one node
		for (String type : formTypeConfigurationMap.keySet()) {
			boolean added = false;
			if (random.nextBoolean()) {
				addTypeToNode(type, firstNode);
				added = true;
			}
			if (random.nextBoolean()) {
				addTypeToNode(type, secondNode);
				added = true;
			}
			if (!added || random.nextBoolean()) {
				addTypeToNode(type, thirdNode);
			}
		}
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<FormTypeConfigurationModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("type", item -> item.type),
				Pair.of("name", item -> item.nameI18n.get("de")),
				Pair.of("description", item -> item.descriptionI18n.get("de")),
				Pair.of("external", item -> Boolean.toString(item.external)),
				Pair.of("submitOnce", item -> Boolean.toString(item.submitOnce))
		);
		List<Pair<String, Function<FormTypeConfigurationModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("type", item -> item.type),
				Pair.of("name", item -> item.nameI18n.get("de")),
				Pair.of("description", item -> item.descriptionI18n.get("de")),
				Pair.of("external", item -> Boolean.toString(item.external)),
				Pair.of("submitOnce", item -> Boolean.toString(item.submitOnce)),
				// filtering by nodeId is a bit of a hack, because the returned items do not have the attribute "nodeId".
				// what we do is to "extract" the requested nodeId from the item, if the item is assigned to the node, and null if not
				Pair.of("nodeId", item -> nodeIdsPerType.getOrDefault(item.type, Collections.emptySet()).contains(firstNode.getId()) ? Integer.toString(firstNode.getId()) : null)
		);
		return data(sortAttributes, filterAttributes);
	}

	/**
	 * Add the type to the given node
	 * @param type type
	 * @param node node
	 * @throws NodeException
	 */
	protected static void addTypeToNode(String type, Node node) throws NodeException {
		Trx.consume((t, n) -> FormFactory.addFormTypeToNode(type, n.getId()), type, node);
		nodeIdsPerType.computeIfAbsent(type, key -> new HashSet<>()).add(node.getId());
	}

	@Before
	@Override
	public void setup() throws NodeException {
		super.setup();

		// when filtering by nodeId, we get the id of the first node
		if (Strings.CI.equals(filterBy, "nodeId")) {
			query = Integer.toString(firstNode.getId());
		}
	}

	@Override
	protected void fillItemsList(List<? super FormTypeConfigurationModel> items) throws NodeException {
		for (Entry<String, FormTypeConfiguration> entry : formTypeConfigurationMap.entrySet()) {
			items.add(FormConfigProvider.TRANSFORM2REST.apply(entry));
		}
	}

	@Override
	protected FormTypeConfigurationModel createItem() throws NodeException {
		return null;
	}

	@Override
	protected AbstractListResponse<FormTypeConfigurationModel> getResult(SortParameterBean sort,
			FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		FilterFormTypeConfigurationsParameterBean formTypeFilter = new FilterFormTypeConfigurationsParameterBean();
		if (filterBy != null) {
			switch (filterBy) {
			case "external":
				formTypeFilter.external = Boolean.valueOf(filter.query);
				break;
			case "submitOnce":
				formTypeFilter.submitOnce = Boolean.valueOf(filter.query);
				break;
			case "nodeId":
				formTypeFilter.nodeId = filter.query;
				break;
			default:
				formTypeFilter.query = filter.query;
				break;
			}
		}
		return new FormResourceImpl().listFormTypeConfigurations(formTypeFilter, sort, paging);
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfigurationMap);
	}
}
