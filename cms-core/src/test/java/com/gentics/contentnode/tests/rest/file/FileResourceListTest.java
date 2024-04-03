package com.gentics.contentnode.tests.rest.file;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link FileResource#list(InFolderParameterBean, FileListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, EditableParameterBean, WastebinParameterBean)}
 */
@GCNFeature(set = { Feature.NICE_URLS })
public class FileResourceListTest extends AbstractListSortAndFilterTest<File> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<File, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", File::getName),
				Pair.of("niceUrl", File::getNiceUrl),
				Pair.of("alternateUrls", item -> item.getAlternateUrls().first()),
				Pair.of("fileSize", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getFileSize())),
				Pair.of("fileType", File::getFileType)
		);
		List<Pair<String, Function<File, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", File::getName),
				Pair.of("description", File::getDescription),
				Pair.of("niceUrl", File::getNiceUrl),
				Pair.of("alternateUrls", item -> item.getAlternateUrls().first())
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected File createItem() throws NodeException {
		return supply(() -> ModelBuilder.getFile(Builder.create(com.gentics.contentnode.object.File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(randomStringGenerator.generate(5, 10));
			f.setDescription(randomStringGenerator.generate(10, 20));
			String contents = randomStringGenerator.generate(10, 20);
			f.setFileStream(new ByteArrayInputStream(contents.getBytes()));
			f.setNiceUrl(randomStringGenerator.generate(5, 10));
			f.setAlternateUrls(randomStringGenerator.generate(5, 10));
			f.setFiletype(randomStringGenerator.generate(5, 10));
		}).build(), Collections.emptyList()));
	}

	@Override
	protected AbstractListResponse<File> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new FileResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())),
				new FileListParameterBean(), filter, sort, paging, new EditableParameterBean(),
				new WastebinParameterBean());
	}
}
