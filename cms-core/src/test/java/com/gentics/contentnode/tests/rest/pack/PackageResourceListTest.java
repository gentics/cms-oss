package com.gentics.contentnode.tests.rest.pack;

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
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PackageResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class PackageResourceListTest extends AbstractListSortAndFilterTest<Package> {
	private static List<Integer> TYPES = Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE, Template.TYPE_TEMPLATE);

	public final static int NUM_ITEMS = 10;

	protected final static List<Construct> constructs = new ArrayList<>();

	protected final static List<ContentRepository> contentRepositories = new ArrayList<>();

	protected final static List<Datasource> datasources = new ArrayList<>();

	protected final static List<ObjectTagDefinition> objectProperties = new ArrayList<>();

	protected final static List<CrFragment> crFragments = new ArrayList<>();

	protected final static List<Template> templates = new ArrayList<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();
		Synchronizer.start();

		for (int i = 0; i < NUM_ITEMS; i++) {
			constructs.add(Builder.create(Construct.class, c -> {
				c.setAutoEnable(true);
				c.setKeyword(randomStringGenerator.generate(5, 10));
				c.setName(randomStringGenerator.generate(5, 10), 1);
				c.setName(randomStringGenerator.generate(5, 10), 2);
			}).build());
			contentRepositories.add(Builder.create(ContentRepository.class, cr -> {
				cr.setName(randomStringGenerator.generate(5, 10));
				cr.setCrType(getRandomEntry(Type.values()));
				cr.setDbType(randomStringGenerator.generate(5, 10));
				cr.setUsername(randomStringGenerator.generate(5, 10));
				cr.setUrl(randomStringGenerator.generate(10, 20));
				cr.setBasepath(randomStringGenerator.generate(5, 10));
				cr.setInstantPublishing(random.nextBoolean());
				cr.setLanguageInformation(random.nextBoolean());
				cr.setPermissionInformation(random.nextBoolean());
				cr.setDiffDelete(random.nextBoolean());
			}).build());
			datasources.add(Builder.create(Datasource.class, ds -> {
				ds.setName(randomStringGenerator.generate(5, 10));
				ds.setSourceType(getRandomEntry(SourceType.values()));
			}).build());
			objectProperties.add(Builder.create(ObjectTagDefinition.class, objProp -> {
				ObjectTag tag = objProp.getObjectTag();
				tag.setConstructId(getRandomEntry(constructs).getId());
				tag.setName("object." + randomStringGenerator.generate(5, 10));
				tag.setObjType(getRandomEntry(TYPES));
				objProp.setName(randomStringGenerator.generate(5, 10), 1);
				objProp.setName(randomStringGenerator.generate(5, 10), 2);
				objProp.setDescription(randomStringGenerator.generate(10, 20), 1);
				objProp.setDescription(randomStringGenerator.generate(10, 20), 2);
			}).build());
			crFragments.add(Builder.create(CrFragment.class, fr -> {
				fr.setName(randomStringGenerator.generate(5, 10));
			}).build());
			templates.add(Builder.create(Template.class, t -> {
				t.setName(randomStringGenerator.generate(5, 10));
				t.setDescription(randomStringGenerator.generate(10, 20));
			}).build());
		}
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Package, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", Package::getName),
				Pair.of("constructs", item -> addLeadingZeros(item.getConstructs())),
				Pair.of("datasources", item -> addLeadingZeros(item.getDatasources())),
				Pair.of("templates", item -> addLeadingZeros(item.getTemplates())),
				Pair.of("objectProperties", item -> addLeadingZeros(item.getObjectProperties())),
				Pair.of("crFragments", item -> addLeadingZeros(item.getCrFragments())),
				Pair.of("contentRepositories", item -> addLeadingZeros(item.getContentRepositories()))
		);
		List<Pair<String, Function<Package, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", Package::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Package createItem() throws NodeException {
		return supply(() -> {
			String name = randomStringGenerator.generate(5, 10);
			Synchronizer.addPackage(name);

			MainPackageSynchronizer pack = Synchronizer.getPackage(name);
			addItems(pack, constructs);
			addItems(pack, contentRepositories);
			addItems(pack, datasources);
			addItems(pack, objectProperties);
			addItems(pack, crFragments);
			addItems(pack, templates);

			return MainPackageSynchronizer.TRANSFORM2REST.apply(pack);
		});
	}

	protected <T extends SynchronizableNodeObject> void addItems(MainPackageSynchronizer pack, List<T> items)
			throws NodeException {
		int numItems = random.nextInt(items.size() + 1);
		for (int i = 0; i < numItems; i++) {
			pack.synchronize(items.get(i), true);
		}
	}

	@Override
	protected AbstractListResponse<Package> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().list(filter, sort, paging);
	}
}
