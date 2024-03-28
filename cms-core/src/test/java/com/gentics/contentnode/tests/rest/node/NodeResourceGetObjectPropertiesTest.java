package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link NodeResource#getObjectProperties(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class NodeResourceGetObjectPropertiesTest extends AbstractListSortAndFilterTest<ObjectProperty> {
	private static Node node;
	private static Integer constructId;

	private static List<Integer> TYPES = Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE, Template.TYPE_TEMPLATE);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
		constructId = supply(() -> createConstruct(node, HTMLPartType.class, "construct", "part"));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// keyword, name, description
		List<Pair<String, Function<ObjectProperty, String>>> attributes = Arrays.asList(
				Pair.of("keyword", ObjectProperty::getKeyword),
				Pair.of("name", ObjectProperty::getName),
				Pair.of("description", ObjectProperty::getDescription)
		);
		return data(attributes, attributes);
	}

	@Override
	protected ObjectProperty createItem() throws NodeException {
		return supply(() -> {
			ObjectTagDefinition objectTagDefinition = Builder.create(ObjectTagDefinition.class, o -> {
				ObjectTag tag = o.getObjectTag();
				tag.setConstructId(constructId);
				tag.setName("object." + randomStringGenerator.generate(5, 10));
				tag.setObjType(getRandomEntry(TYPES));
				o.setName(randomStringGenerator.generate(5, 10), 1);
				o.setName(randomStringGenerator.generate(5, 10), 2);
				o.setDescription(randomStringGenerator.generate(10, 20), 1);
				o.setDescription(randomStringGenerator.generate(10, 20), 2);
			}).build();
			node = Builder.update(node, n -> {
				n.addObjectTagDefinition(objectTagDefinition);
			}).build();
			return ObjectTagDefinition.TRANSFORM2REST.apply(objectTagDefinition);
		});
	}

	@Override
	protected AbstractListResponse<ObjectProperty> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new NodeResourceImpl().getObjectProperties(Integer.toString(node.getId()), filter, sort, paging, new PermsParameterBean());
	}
}
