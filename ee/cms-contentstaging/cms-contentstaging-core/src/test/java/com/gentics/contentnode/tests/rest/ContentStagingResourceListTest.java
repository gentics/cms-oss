package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.ContentStagingResource;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link ContentStagingResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
@GCNFeature(set = { Feature.CONTENT_STAGING })
public class ContentStagingResourceListTest extends AbstractListSortAndFilterTest<StagedContentPackage> {
	private static List<Node> nodes;
	private static List<Page> pages;
	private static List<File> files;
	private static List<ImageFile> images;
	private static List<Folder> folders;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		nodes = supply(t -> {
			return t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDLIST));
		});

		pages = supply(t -> {
			return t.getObjects(Page.class, DBUtils.select("SELECT id FROM page", DBUtils.IDS));
		});

		files = supply(t -> {
			return t.getObjects(File.class, DBUtils.select("SELECT id FROM contentfile WHERE filetype NOT LIKE 'image/%'", DBUtils.IDS));
		});

		images = supply(t -> {
			return t.getObjects(ImageFile.class, DBUtils.select("SELECT id FROM contentfile WHERE filetype LIKE 'image/%'", DBUtils.IDS));
		});

		folders = supply(t -> {
			return t.getObjects(Folder.class, DBUtils.select("SELECT id FROM folder", DBUtils.IDS));
		});
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<StagedContentPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", StagedContentPackage::getName),
				Pair.of("description", StagedContentPackage::getDescription),
				Pair.of("nodes", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getNodes())),
				Pair.of("pages", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getPages())),
				Pair.of("folders", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getFolders())),
				Pair.of("files", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getFiles())),
				Pair.of("images", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getImages()))
		);
		List<Pair<String, Function<StagedContentPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", StagedContentPackage::getName),
				Pair.of("description", StagedContentPackage::getDescription));

		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected StagedContentPackage createItem() throws NodeException {
		StagedContentPackage contentPackage = supply(() -> {
			StagedContentPackage create = new StagedContentPackage();
			create.setName(randomStringGenerator.generate(5, 10));
			create.setDescription(randomStringGenerator.generate(10, 20));
			return new ContentStagingResourceImpl().create(create).getContentPackage();
		});

		for (Node node : nodes) {
			if (random.nextInt(nodes.size()) == 0) {
				contentPackage = execute(name -> {
					return new ContentStagingResourceImpl().add(name, "node", node.getGlobalId().toString(), false).getContentPackage();
				}, contentPackage.getName());
			}
		}

		for (Page page : pages) {
			if (random.nextInt(pages.size()) == 0) {
				contentPackage = execute(name -> {
					return new ContentStagingResourceImpl().add(name, "page", page.getGlobalId().toString(), false).getContentPackage();
				}, contentPackage.getName());
			}
		}

		for (File file : files) {
			if (random.nextInt(files.size()) == 0) {
				contentPackage = execute(name -> {
					return new ContentStagingResourceImpl().add(name, "file", file.getGlobalId().toString(), false).getContentPackage();
				}, contentPackage.getName());
			}
		}

		for (ImageFile image : images) {
			if (random.nextInt(images.size()) == 0) {
				contentPackage = execute(name -> {
					return new ContentStagingResourceImpl().add(name, "image", image.getGlobalId().toString(), false).getContentPackage();
				}, contentPackage.getName());
			}
		}

		for (Folder folder : folders) {
			if (random.nextInt(folders.size()) == 0) {
				contentPackage = execute(name -> {
					return new ContentStagingResourceImpl().add(name, "folder", folder.getGlobalId().toString(), false).getContentPackage();
				}, contentPackage.getName());
			}
		}

		return contentPackage;
	}

	@Override
	protected AbstractListResponse<StagedContentPackage> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new ContentStagingResourceImpl().list(filter, sort, paging);
	}
}
