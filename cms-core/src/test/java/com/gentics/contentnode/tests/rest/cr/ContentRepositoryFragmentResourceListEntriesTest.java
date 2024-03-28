package com.gentics.contentnode.tests.rest.cr;

import static com.gentics.contentnode.factory.Trx.supply;

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
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryFragmentResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryFragmentResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ContentRepositoryFragmentResource#listEntries(String, FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class ContentRepositoryFragmentResourceListEntriesTest extends AbstractListSortAndFilterTest<ContentRepositoryFragmentEntryModel> {
	public static int FRAGMENT_ID;

	public static List<Integer> OBJ_TYPES = Arrays.asList(Page.TYPE_PAGE, Folder.TYPE_FOLDER, ImageFile.TYPE_IMAGE,
			File.TYPE_FILE);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		FRAGMENT_ID = Builder.create(CrFragment.class, fragment -> {
			fragment.setName(randomStringGenerator.generate(5, 10));
		}).build().getId();
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ContentRepositoryFragmentEntryModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getId())),
				Pair.of("globalId", ContentRepositoryFragmentEntryModel::getGlobalId),
				Pair.of("tagname", ContentRepositoryFragmentEntryModel::getTagname),
				Pair.of("mapname", ContentRepositoryFragmentEntryModel::getMapname),
				Pair.of("objType", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getObjType())),
				Pair.of("attributeType", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getAttributeType())),
				Pair.of("targetType", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getTargetType())),
				Pair.of("multivalue", entry -> Boolean.toString(entry.getMultivalue())),
				Pair.of("optimized", entry -> Boolean.toString(entry.getOptimized())),
				Pair.of("filesystem", entry -> Boolean.toString(entry.getFilesystem())),
				Pair.of("foreignlinkAttribute", ContentRepositoryFragmentEntryModel::getForeignlinkAttribute),
				Pair.of("foreignlinkAttributeRule", ContentRepositoryFragmentEntryModel::getForeignlinkAttributeRule),
				Pair.of("category", ContentRepositoryFragmentEntryModel::getCategory),
				Pair.of("segmentfield", entry -> Boolean.toString(entry.getSegmentfield())),
				Pair.of("displayfield", entry -> Boolean.toString(entry.getDisplayfield())),
				Pair.of("urlfield", entry -> Boolean.toString(entry.getUrlfield()))
		);

		List<Pair<String, Function<ContentRepositoryFragmentEntryModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", entry -> AbstractListSortAndFilterTest.addLeadingZeros(entry.getId())),
				Pair.of("globalId", ContentRepositoryFragmentEntryModel::getGlobalId),
				Pair.of("tagname", ContentRepositoryFragmentEntryModel::getTagname),
				Pair.of("mapname", ContentRepositoryFragmentEntryModel::getMapname),
				Pair.of("foreignlinkAttribute", ContentRepositoryFragmentEntryModel::getForeignlinkAttribute),
				Pair.of("foreignlinkAttributeRule", ContentRepositoryFragmentEntryModel::getForeignlinkAttributeRule),
				Pair.of("category", ContentRepositoryFragmentEntryModel::getCategory)
		);

		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ContentRepositoryFragmentEntryModel createItem() throws NodeException {
		return supply(() -> Builder.create(CrFragmentEntry.class, entry -> {
			entry.setCrFragmentId(FRAGMENT_ID);
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
		}).build().getModel());
	}

	@Override
	protected AbstractListResponse<ContentRepositoryFragmentEntryModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ContentRepositoryFragmentResourceImpl().listEntries(Integer.toString(FRAGMENT_ID), filter, sort, paging);
	}
}
