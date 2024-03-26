package com.gentics.contentnode.tests.rest.pack;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.ObjectTagDefinitionSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.devtools.ObjectPropertyInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PackageResource#listObjectProperties(String, FilterParameterBean, SortParameterBean, PagingParameterBean, EmbedParameterBean, PermsParameterBean)}
 */
public class PackageResourceListObjectPropertiesTest extends AbstractListSortAndFilterTest<ObjectPropertyInPackage> {
	private static List<Integer> TYPES = Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE, Template.TYPE_TEMPLATE);

	protected static String packageName;

	private static Construct construct;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();
		Synchronizer.start();

		packageName = randomStringGenerator.generate(5, 10);
		operate(() -> Synchronizer.addPackage(packageName));
		construct = Builder.create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setIconName("icon");
			c.setKeyword(randomStringGenerator.generate(5, 10));
			c.setName(randomStringGenerator.generate(5, 10), 1);
			c.setName(randomStringGenerator.generate(5, 10), 2);
		}).build();
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ObjectPropertyInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", ObjectPropertyInPackage::getName),
				Pair.of("description", ObjectPropertyInPackage::getDescription),
				Pair.of("keyword", ObjectPropertyInPackage::getKeyword),
				Pair.of("type", item -> addLeadingZeros(item.getType())),
				Pair.of("required", item -> Boolean.toString(item.getRequired())),
				Pair.of("inheritable", item -> Boolean.toString(item.getInheritable())),
				Pair.of("construct.name", item -> item.getConstruct().getName())
		);
		List<Pair<String, Function<ObjectPropertyInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", ObjectPropertyInPackage::getName),
				Pair.of("description", ObjectPropertyInPackage::getDescription),
				Pair.of("keyword", ObjectPropertyInPackage::getKeyword)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ObjectPropertyInPackage createItem() throws NodeException {
		ObjectTagDefinition item = Builder.create(ObjectTagDefinition.class, objProp -> {
			ObjectTag tag = objProp.getObjectTag();
			tag.setConstructId(construct.getId());
			tag.setName("object." + randomStringGenerator.generate(5, 10));
			tag.setObjType(getRandomEntry(TYPES));
			objProp.setName(randomStringGenerator.generate(5, 10), 1);
			objProp.setName(randomStringGenerator.generate(5, 10), 2);
			objProp.setDescription(randomStringGenerator.generate(10, 20), 1);
			objProp.setDescription(randomStringGenerator.generate(10, 20), 2);
		}).build();

		consume(i -> Synchronizer.getPackage(packageName).synchronize(i, true), item);

		return execute(c -> {
			ObjectPropertyInPackage ip = new ObjectPropertyInPackage();
			ip.setPackageName(packageName);
			ObjectTagDefinition.NODE2REST.apply(item, ip);
			ObjectTagDefinitionSynchronizer.EMBED_CONSTRUCT.accept(ip);
			return ip;
		}, item);
	}

	@Override
	protected AbstractListResponse<ObjectPropertyInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		EmbedParameterBean embed = new EmbedParameterBean();
		embed.embed = "construct";
		return new PackageResourceImpl().listObjectProperties(packageName, filter, sort, paging, embed, new PermsParameterBean());
	}
}
