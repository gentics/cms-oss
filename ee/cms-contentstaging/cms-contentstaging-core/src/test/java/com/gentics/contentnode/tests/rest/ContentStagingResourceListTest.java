package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.ContentStagingResource;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ContentPackageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

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

		List<Folder> potentialMotherFolders = new ArrayList<>();

		// 20 nodes
		nodes = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			Node node = supply(() -> ContentNodeTestDataUtils.createNode());
			nodes.add(node);
			potentialMotherFolders.add(execute(Node::getFolder, node));
		}

		Template template = supply(() -> ContentNodeTestDataUtils.createTemplate(nodes.get(0).getFolder(), "Template"));

		// 40 folders
		folders = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			Folder mother = potentialMotherFolders.get(random.nextInt(potentialMotherFolders.size()));
			Folder folder = supply(() -> ContentNodeTestDataUtils.createFolder(mother, randomStringGenerator.generate(5, 10)));
			folders.add(folder);
			potentialMotherFolders.add(folder);
		}

		// 100 pages
		pages = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			Folder mother = potentialMotherFolders.get(random.nextInt(potentialMotherFolders.size()));
			pages.add(supply(() -> ContentNodeTestDataUtils.createPage(mother, template,
					randomStringGenerator.generate(5, 10))));
		}

		// 20 files
		files = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			Folder mother = potentialMotherFolders.get(random.nextInt(potentialMotherFolders.size()));
			files.add(supply(() -> ContentNodeTestDataUtils.createFile(mother, randomStringGenerator.generate(5, 10),
					randomStringGenerator.generate(20, 40).getBytes())));
		}

		// 20 images
		images = new ArrayList<>();
		try {
			byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
			for (int i = 0; i < 20; i++) {
				Folder mother = potentialMotherFolders.get(random.nextInt(potentialMotherFolders.size()));
				images.add(supply(() -> (ImageFile) ContentNodeTestDataUtils.createImage(mother,
						randomStringGenerator.generate(5, 10), imageData)));
			}
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<StagedContentPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", StagedContentPackage::getName),
				Pair.of("description", StagedContentPackage::getDescription),
				Pair.of("nodes", item -> AbstractListSortAndFilterTest.addLeadingZeros(ObjectTransformer.getInt(item.getNodes(), 0))),
				Pair.of("pages", item -> AbstractListSortAndFilterTest.addLeadingZeros(ObjectTransformer.getInt(item.getPages(), 0))),
				Pair.of("folders", item -> AbstractListSortAndFilterTest.addLeadingZeros(ObjectTransformer.getInt(item.getFolders(), 0))),
				Pair.of("files", item -> AbstractListSortAndFilterTest.addLeadingZeros(ObjectTransformer.getInt(item.getFiles(), 0))),
				Pair.of("images", item -> AbstractListSortAndFilterTest.addLeadingZeros(ObjectTransformer.getInt(item.getImages(), 0)))
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
		return new ContentStagingResourceImpl().list(filter, sort, paging, new ContentPackageListParameterBean());
	}
}
