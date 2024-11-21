package com.gentics.contentnode.tests.rest.objectproperty;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.model.ObjectPropertyCategory;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ObjectPropertyResource;
import com.gentics.contentnode.rest.resource.impl.ObjectPropertyResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ObjectPropertyParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link ObjectPropertyResource#list(SortParameterBean, FilterParameterBean, PagingParameterBean, ObjectPropertyParameterBean, EmbedParameterBean)}
 */
@GCNFeature(set = {Feature.OBJTAG_SYNC})
public class ObjectPropertyResourceListTest extends AbstractListSortAndFilterTest<ObjectProperty> {
	private static List<Integer> TYPES = Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE, Template.TYPE_TEMPLATE);

	private final static int NUM_CONSTRUCTS = 20;

	private static List<Integer> constructIds = new ArrayList<>();

	private final static int NUM_CATEGORIES = 20;

	private static List<Integer> categoryIds = new ArrayList<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		for (int i = 0; i < NUM_CONSTRUCTS; i++) {
			constructIds.add(supply(() -> createConstruct(null, HTMLPartType.class,
					randomStringGenerator.generate(5, 10), randomStringGenerator.generate(5, 10))));
		}

		for (int i = 0; i < NUM_CATEGORIES; i++) {
			final int sortOrder = i;
			categoryIds.add(Builder.create(ObjectTagDefinitionCategory.class, cat -> {
				cat.setName(randomStringGenerator.generate(5, 10), 1);
				cat.setName(randomStringGenerator.generate(5, 10), 2);
				cat.setSortorder(sortOrder);
			}).build().getId());
		}
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// "id", "globalId", "name", "description", "keyword", "type", "required", "inheritable", "syncContentset", "syncChannelset", "syncVariants", "construct.name", "category.name", "category.sortorder"
		List<Pair<String, Function<ObjectProperty, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("globalId", ObjectProperty::getGlobalId),
				Pair.of("name", ObjectProperty::getName),
				Pair.of("description", ObjectProperty::getDescription),
				Pair.of("keyword", ObjectProperty::getKeyword),
				Pair.of("type", item -> addLeadingZeros(item.getType())),
				Pair.of("required", item -> Boolean.toString(item.getRequired())),
				Pair.of("inheritable", item -> Boolean.toString(item.getInheritable())),
				Pair.of("syncContentset", item -> Optional.ofNullable(item.getSyncContentset()).map(b -> Boolean.toString(b)).orElse("false")),
				Pair.of("syncChannelset", item -> Optional.ofNullable(item.getSyncChannelset()).map(b -> Boolean.toString(b)).orElse(null)),
				Pair.of("syncVariants", item -> Optional.ofNullable(item.getSyncVariants()).map(b -> Boolean.toString(b)).orElse("false")),
				Pair.of("restricted", item -> Optional.ofNullable(item.getRestricted()).map(b -> Boolean.toString(b)).orElse("false")),
				Pair.of("construct.name", item -> item.getConstruct().getName()),
				Pair.of("category.name", item -> Optional.ofNullable(item.getCategory()).map(ObjectPropertyCategory::getName).orElse(null))
		);
		// "id", "globalId", "name", "description", "keyword"
		List<Pair<String, Function<ObjectProperty, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("globalId", ObjectProperty::getGlobalId),
				Pair.of("name", ObjectProperty::getName),
				Pair.of("description", ObjectProperty::getDescription),
				Pair.of("keyword", ObjectProperty::getKeyword)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ObjectProperty createItem() throws NodeException {
		return supply(() -> {
			ObjectTagDefinition objectTagDefinition = Builder.create(ObjectTagDefinition.class, o -> {
				ObjectTag tag = o.getObjectTag();
				tag.setConstructId(getRandomEntry(constructIds));
				tag.setName("object." + randomStringGenerator.generate(5, 10));
				tag.setObjType(getRandomEntry(TYPES));
				o.setName(randomStringGenerator.generate(5, 10), 1);
				o.setName(randomStringGenerator.generate(5, 10), 2);
				o.setDescription(randomStringGenerator.generate(10, 20), 1);
				o.setDescription(randomStringGenerator.generate(10, 20), 2);

				tag.setRequired(random.nextBoolean());
				tag.setInheritable(random.nextBoolean());
				o.setSyncChannelset(random.nextBoolean());
				if (tag.getObjType() == Page.TYPE_PAGE) {
					o.setSyncContentset(random.nextBoolean());
					o.setSyncVariants(random.nextBoolean());
				}
				o.setRestricted(random.nextBoolean());

				if (random.nextBoolean()) {
					o.setCategoryId(getRandomEntry(categoryIds));
				}
			}).build();
			return ObjectTagDefinition.TRANSFORM2REST.apply(objectTagDefinition);
		});
	}

	@Override
	protected AbstractListResponse<ObjectProperty> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		EmbedParameterBean embed = new EmbedParameterBean();
		embed.embed = "construct,category";
		return new ObjectPropertyResourceImpl().list(sort, filter, paging, new ObjectPropertyParameterBean(), embed);
	}
}
