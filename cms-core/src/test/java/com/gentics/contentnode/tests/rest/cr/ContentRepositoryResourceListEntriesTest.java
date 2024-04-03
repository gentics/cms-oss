package com.gentics.contentnode.tests.rest.cr;

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
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.object.cr.CrFragmentEntryWrapper;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ContentRepositoryResource#listEntries(String, boolean, FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class ContentRepositoryResourceListEntriesTest extends AbstractListSortAndFilterTest<TagmapEntryModel> {
	public static int CR_ID;

	public static List<CrFragment> FRAGMENTS = new ArrayList<>();

	public static List<Integer> OBJ_TYPES = Arrays.asList(Page.TYPE_PAGE, Folder.TYPE_FOLDER, ImageFile.TYPE_IMAGE,
			File.TYPE_FILE);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		for (int i = 0; i < 3; i++) {
			FRAGMENTS.add(Builder.create(CrFragment.class, fragment -> {
				fragment.setName(randomStringGenerator.generate(5, 10));
			}).build());
		}

		CR_ID = Builder.create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setName(randomStringGenerator.generate(5, 10));
			cr.getAssignedFragments().addAll(FRAGMENTS);
		}).build().getId();
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<TagmapEntryModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getId())),
				Pair.of("globalId", TagmapEntryModel::getGlobalId),
				Pair.of("tagname", TagmapEntryModel::getTagname),
				Pair.of("mapname", TagmapEntryModel::getMapname),
				Pair.of("object", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getObject())),
				Pair.of("attributeType", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getAttributeType())),
				Pair.of("targetType", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getTargetType())),
				Pair.of("multivalue", entry -> Boolean.toString(entry.getMultivalue())),
				Pair.of("optimized", entry -> Boolean.toString(entry.getOptimized())),
				Pair.of("filesystem", entry -> Boolean.toString(entry.getFilesystem())),
				Pair.of("foreignlinkAttribute", TagmapEntryModel::getForeignlinkAttribute),
				Pair.of("foreignlinkAttributeRule", TagmapEntryModel::getForeignlinkAttributeRule),
				Pair.of("category", TagmapEntryModel::getCategory),
				Pair.of("segmentfield", entry -> Boolean.toString(entry.getSegmentfield())),
				Pair.of("displayfield", entry -> Boolean.toString(entry.getDisplayfield())),
				Pair.of("urlfield", entry -> Boolean.toString(entry.getUrlfield())),
				Pair.of("fragmentName", TagmapEntryModel::getFragmentName)
		);

		List<Pair<String, Function<TagmapEntryModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getId())),
				Pair.of("globalId", TagmapEntryModel::getGlobalId),
				Pair.of("tagname", TagmapEntryModel::getTagname),
				Pair.of("mapname", TagmapEntryModel::getMapname),
				Pair.of("foreignlinkAttribute", TagmapEntryModel::getForeignlinkAttribute),
				Pair.of("foreignlinkAttributeRule", TagmapEntryModel::getForeignlinkAttributeRule),
				Pair.of("category", TagmapEntryModel::getCategory),
				Pair.of("fragmentName", TagmapEntryModel::getFragmentName)
		);

		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected TagmapEntryModel createItem() throws NodeException {
		boolean addToFragment = random.nextBoolean();

		if (addToFragment) {
			int fragmentId = getRandomEntry(FRAGMENTS).getId();

			return supply(() -> TagmapEntry.TRANSFORM2REST.apply(new CrFragmentEntryWrapper(null, Builder.create(CrFragmentEntry.class, entry -> {
				entry.setCrFragmentId(fragmentId);
				entry.setTagname(randomStringGenerator.generate(5, 10));
				entry.setMapname(randomStringGenerator.generate(5, 10));
				entry.setObjType(getRandomEntry(OBJ_TYPES));
				entry.setAttributeTypeId(getRandomEntry(TagmapEntry.AttributeType.values()).getType());
				entry.setTargetType(getRandomEntry(OBJ_TYPES));
				entry.setMultivalue(random.nextBoolean());
				entry.setOptimized(random.nextBoolean());
				entry.setFilesystem(random.nextBoolean());
				entry.setForeignlinkAttribute(randomStringGenerator.generate(5, 10));
				entry.setForeignlinkAttributeRule(randomStringGenerator.generate(5, 10));
				entry.setCategory(randomStringGenerator.generate(5, 10));
				entry.setSegmentfield(random.nextBoolean());
				entry.setDisplayfield(random.nextBoolean());
				entry.setUrlfield(random.nextBoolean());
			}).build())));
		} else {
			return supply(() -> TagmapEntry.TRANSFORM2REST.apply(Builder.create(TagmapEntry.class, entry -> {
				entry.setContentRepositoryId(CR_ID);
				entry.setTagname(randomStringGenerator.generate(5, 10));
				entry.setMapname(randomStringGenerator.generate(5, 10));
				entry.setObject(getRandomEntry(OBJ_TYPES));
				entry.setAttributeTypeId(getRandomEntry(TagmapEntry.AttributeType.values()).getType());
				entry.setTargetType(getRandomEntry(OBJ_TYPES));
				entry.setMultivalue(random.nextBoolean());
				entry.setOptimized(random.nextBoolean());
				entry.setFilesystem(random.nextBoolean());
				entry.setForeignlinkAttribute(randomStringGenerator.generate(5, 10));
				entry.setForeignlinkAttributeRule(randomStringGenerator.generate(5, 10));
				entry.setCategory(randomStringGenerator.generate(5, 10));
				entry.setSegmentfield(random.nextBoolean());
				entry.setDisplayfield(random.nextBoolean());
				entry.setUrlfield(random.nextBoolean());
			}).build()));
		}
	}

	@Override
	protected AbstractListResponse<TagmapEntryModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ContentRepositoryResourceImpl().listEntries(Integer.toString(CR_ID), true, filter, sort, paging);
	}
}
