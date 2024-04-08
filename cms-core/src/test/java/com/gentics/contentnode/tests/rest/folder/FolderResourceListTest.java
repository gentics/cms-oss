package com.gentics.contentnode.tests.rest.folder;

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
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link FolderResource#list(InFolderParameterBean, FolderListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)}
 */
public class FolderResourceListTest extends AbstractListSortAndFilterTest<Folder> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Folder, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Folder::getName),
				Pair.of("description", Folder::getDescription),
				Pair.of("publishDir", Folder::getPublishDir)
		);
		List<Pair<String, Function<Folder, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Folder::getName),
				Pair.of("description", Folder::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Folder createItem() throws NodeException {
		return supply(() -> ModelBuilder.getFolder(Builder.create(com.gentics.contentnode.object.Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName(randomStringGenerator.generate(5, 10));
			f.setDescription(randomStringGenerator.generate(10, 20));
			f.setPublishDir(randomStringGenerator.generate(5, 10));
		}).build()));
	}

	@Override
	protected AbstractListResponse<Folder> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new FolderResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())),
				new FolderListParameterBean(), filter, sort, paging, new EditableParameterBean(),
				new WastebinParameterBean());
	}
}
