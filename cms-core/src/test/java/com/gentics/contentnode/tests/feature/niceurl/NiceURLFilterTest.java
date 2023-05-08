package com.gentics.contentnode.tests.feature.niceurl;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test filtering objects by nice URL
 */
@GCNFeature(set = { Feature.NICE_URLS })
@RunWith(value = Parameterized.class)
public class NiceURLFilterTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static Map<TestedType, Set<Integer>> allObjects = new HashMap<>();

	private static Map<TestedType, Set<Integer>> niceObjects = new HashMap<>();

	private static Map<TestedType, Set<Integer>> unniceObjects = new HashMap<>();

	private static Map<TestedType, Map<String, Integer>> objectsPerNiceUrl = new HashMap<>();

	private static Function<? super TestedType, ? extends Set<Integer>> EMPTY_SET = key -> new HashSet<>();

	private static Function<? super TestedType, ? extends Map<String, Integer>> EMPTY_MAP = key -> new HashMap<>();

	@Parameters(name = "{index}: test {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType type : Arrays.asList(TestedType.page, TestedType.file, TestedType.image)) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		createPage("No nice URL 1");
		createPage("No nice URL 2");
		createPage("No nice URL 3");

		createFile("no-nice-url-1.txt");
		createFile("no-nice-url-2.txt");
		createFile("no-nice-url-3.txt");

		createImage("no-nice-url-1.jpg");
		createImage("no-nice-url-2.jpg");
		createImage("no-nice-url-3.jpg");

		createPageWithNiceURL("Nice URL 1", "/page/nice1");
		createPageWithNiceURL("Nice URL 2", "/page/nice2");
		createPageWithNiceURL("Nice URL 3", "/page/nice3");

		createFileWithNiceURL("nice-url-1.txt", "/file/nice1");
		createFileWithNiceURL("nice-url-2.txt", "/file/nice2");
		createFileWithNiceURL("nice-url-3.txt", "/file/nice3");

		createImageWithNiceURL("nice-url-1.jpg", "/image/nice1");
		createImageWithNiceURL("nice-url-2.jpg", "/image/nice2");
		createImageWithNiceURL("nice-url-3.jpg", "/image/nice3");

		createPageWithAlternateURL("Alternate URLs 1", "/page/nice4", "/page/alt1");
		createPageWithAlternateURL("Alternate URLs 2", "/page/nice5", "/page/alt2");
		createPageWithAlternateURL("Alternate URLs 3", "/page/nice6", "/page/alt3");

		createFileWithAlternateURL("alternate-url-1.txt", "/file/nice4", "/file/alt1");
		createFileWithAlternateURL("alternate-url-2.txt", "/file/nice5", "/file/alt2");
		createFileWithAlternateURL("alternate-url-3.txt", "/file/nice6", "/file/alt3");

		createImageWithAlternateURL("alternate-url-1.jpg", "/image/nice4", "/image/alt1");
		createImageWithAlternateURL("alternate-url-2.jpg", "/image/nice5", "/image/alt2");
		createImageWithAlternateURL("alternate-url-3.jpg", "/image/nice6", "/image/alt3");
	}

	/**
	 * Create a page without nice URL
	 * @param name page name
	 * @throws NodeException
	 */
	protected static void createPage(String name) throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName(name);
		});
		allObjects.computeIfAbsent(TestedType.page, EMPTY_SET).add(page.getId());
		unniceObjects.computeIfAbsent(TestedType.page, EMPTY_SET).add(page.getId());
	}

	/**
	 * Create a page with nice URL
	 * @param name page name
	 * @param niceUrl nice URL
	 * @throws NodeException
	 */
	protected static void createPageWithNiceURL(String name, String niceUrl) throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName(name);
			p.setNiceUrl(niceUrl);
		});
		allObjects.computeIfAbsent(TestedType.page, EMPTY_SET).add(page.getId());
		niceObjects.computeIfAbsent(TestedType.page, EMPTY_SET).add(page.getId());
		objectsPerNiceUrl.computeIfAbsent(TestedType.page, EMPTY_MAP).put(niceUrl, page.getId());
	}

	/**
	 * Create a page alternate URLs
	 * @param name page name
	 * @param alternateUrls alternate URLs
	 * @throws NodeException
	 */
	protected static void createPageWithAlternateURL(String name, String...alternateUrls) throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName(name);
			p.setAlternateUrls(alternateUrls);
		});
		allObjects.computeIfAbsent(TestedType.page, EMPTY_SET).add(page.getId());
		niceObjects.computeIfAbsent(TestedType.page, EMPTY_SET).add(page.getId());
		for (String url : alternateUrls) {
			objectsPerNiceUrl.computeIfAbsent(TestedType.page, EMPTY_MAP).put(url, page.getId());
		}
	}

	/**
	 * Create a file without nice URL
	 * @param name file name
	 * @throws NodeException
	 */
	protected static void createFile(String name) throws NodeException {
		com.gentics.contentnode.object.File file = create(com.gentics.contentnode.object.File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(name);
			f.setFileStream(new ByteArrayInputStream("Contents".getBytes()));
		});
		allObjects.computeIfAbsent(TestedType.file, EMPTY_SET).add(file.getId());
		unniceObjects.computeIfAbsent(TestedType.file, EMPTY_SET).add(file.getId());
	}

	/**
	 * Create a file with nice URL
	 * @param name file name
	 * @param niceUrl nice URL
	 * @throws NodeException
	 */
	protected static void createFileWithNiceURL(String name, String niceUrl) throws NodeException {
		com.gentics.contentnode.object.File file = create(com.gentics.contentnode.object.File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(name);
			f.setFileStream(new ByteArrayInputStream("Contents".getBytes()));
			f.setNiceUrl(niceUrl);
		});
		allObjects.computeIfAbsent(TestedType.file, EMPTY_SET).add(file.getId());
		niceObjects.computeIfAbsent(TestedType.file, EMPTY_SET).add(file.getId());
		objectsPerNiceUrl.computeIfAbsent(TestedType.file, EMPTY_MAP).put(niceUrl, file.getId());
	}

	/**
	 * Create a file alternate URLs
	 * @param name file name
	 * @param alternateUrls alternate URLs
	 * @throws NodeException
	 */
	protected static void createFileWithAlternateURL(String name, String...alternateUrls) throws NodeException {
		com.gentics.contentnode.object.File file = create(com.gentics.contentnode.object.File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(name);
			f.setFileStream(new ByteArrayInputStream("Contents".getBytes()));
			f.setAlternateUrls(alternateUrls);
		});
		allObjects.computeIfAbsent(TestedType.file, EMPTY_SET).add(file.getId());
		niceObjects.computeIfAbsent(TestedType.file, EMPTY_SET).add(file.getId());
		for (String url : alternateUrls) {
			objectsPerNiceUrl.computeIfAbsent(TestedType.file, EMPTY_MAP).put(url, file.getId());
		}
	}

	/**
	 * Create an image without nice URL
	 * @param name image name
	 * @throws NodeException
	 */
	protected static void createImage(String name) throws NodeException {
		ImageFile image = create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(name);
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		});
		allObjects.computeIfAbsent(TestedType.image, EMPTY_SET).add(image.getId());
		unniceObjects.computeIfAbsent(TestedType.image, EMPTY_SET).add(image.getId());
	}

	/**
	 * Create an image with nice URL
	 * @param name image name
	 * @param niceUrl nice URL
	 * @throws NodeException
	 */
	protected static void createImageWithNiceURL(String name, String niceUrl) throws NodeException {
		ImageFile image = create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(name);
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			f.setNiceUrl(niceUrl);
		});
		allObjects.computeIfAbsent(TestedType.image, EMPTY_SET).add(image.getId());
		niceObjects.computeIfAbsent(TestedType.image, EMPTY_SET).add(image.getId());
		objectsPerNiceUrl.computeIfAbsent(TestedType.image, EMPTY_MAP).put(niceUrl, image.getId());
	}

	/**
	 * Create an image alternate URLs
	 * @param name image name
	 * @param alternateUrls alternate URLs
	 * @throws NodeException
	 */
	protected static void createImageWithAlternateURL(String name, String...alternateUrls) throws NodeException {
		ImageFile image = create(ImageFile.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName(name);
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			f.setAlternateUrls(alternateUrls);
		});
		allObjects.computeIfAbsent(TestedType.image, EMPTY_SET).add(image.getId());
		niceObjects.computeIfAbsent(TestedType.image, EMPTY_SET).add(image.getId());
		for (String url : alternateUrls) {
			objectsPerNiceUrl.computeIfAbsent(TestedType.image, EMPTY_MAP).put(url, image.getId());
		}
	}

	@Parameter(0)
	public TestedType type;

	/**
	 * Test getting pages without nice URL filter
	 * @throws NodeException
	 */
	@Test
	public void testNoFilter() throws NodeException {
		assertThat(allObjects).as("Map of all objects").containsKey(type);

		assertThat(getObjectIds(null, null))
			.as("Object IDs")
			.containsOnlyElementsOf(allObjects.getOrDefault(type, Collections.emptySet()));

		assertThat(getObjectIdsLegacy(null, null))
			.as("Object IDs (legacy)")
			.containsOnlyElementsOf(allObjects.getOrDefault(type, Collections.emptySet()));
	}

	/**
	 * Test getting pages with general nice URL filter
	 * @throws NodeException
	 */
	@Test
	public void testFilter() throws NodeException {
		assertThat(niceObjects).as("Map of objects with nice URL").containsKey(type);
		String niceUrl = ".+";

		assertThat(getObjectIds(niceUrl, null))
			.as("Object IDs")
			.containsOnlyElementsOf(niceObjects.getOrDefault(type, Collections.emptySet()));

		assertThat(getObjectIdsLegacy(niceUrl, null))
			.as("Object IDs (legacy)")
			.containsOnlyElementsOf(niceObjects.getOrDefault(type, Collections.emptySet()));
	}

	/**
	 * Test getting pages with specific nice URL filter
	 * @throws NodeException
	 */
	@Test
	public void testSpecificFilter() throws NodeException {
		assertThat(objectsPerNiceUrl).as("Map of nice URLs").containsKey(type);

		for (Map.Entry<String, Integer> entry : objectsPerNiceUrl.getOrDefault(type, Collections.emptyMap()).entrySet()) {
			String niceUrl = entry.getKey();
			Integer objectId = entry.getValue();

			assertThat(getObjectIds(niceUrl, null))
				.as("Object IDs for " + niceUrl)
				.containsOnly(objectId);

			assertThat(getObjectIdsLegacy(niceUrl, null))
				.as("Object IDs for " + niceUrl + " (legacy)")
				.containsOnly(objectId);
		}
	}

	/**
	 * Test searching
	 * @throws NodeException
	 */
	@Test
	public void testSearch() throws NodeException {
		assertThat(niceObjects).as("Map of objects with nice URL").containsKey(type);

		String searchQuery = "/nice";

		assertThat(getObjectIds(null, searchQuery))
			.as("Object IDs")
			.containsOnlyElementsOf(niceObjects.getOrDefault(type, Collections.emptySet()));

		assertThat(getObjectIdsLegacy(null, searchQuery))
			.as("Object IDs (legacy)")
			.containsOnlyElementsOf(niceObjects.getOrDefault(type, Collections.emptySet()));
	}

	/**
	 * Get the items via the legacy endpoint of the folder resource.
	 * @param niceUrl Nice URL to search for
	 * @param searchQuery Search query
	 * @return List of found objects
	 * @throws NodeException
	 */
	private Set<Integer> getObjectIdsLegacy(String niceUrl, String searchQuery) throws NodeException {
		return Trx.supply(() -> {
			FolderResource res = ContentNodeRESTUtils.getFolderResource();
			String folderId = node.getFolder().getId().toString();

			switch (type) {
			case file:
			{
				LegacyFileListResponse response = res.getFiles(folderId,
					new InFolderParameterBean().setFolderId(folderId),
					new FileListParameterBean().setNiceUrl(niceUrl),
					new LegacyFilterParameterBean().setSearch(searchQuery),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				ContentNodeRESTUtils.assertResponseOK(response);
				return response.getFiles().stream().map(File::getId).collect(Collectors.toSet());
			}
			case image:
			{
				LegacyFileListResponse response = res.getImages(folderId,
					new InFolderParameterBean().setFolderId(folderId),
					new FileListParameterBean().setNiceUrl(niceUrl),
					new LegacyFilterParameterBean().setSearch(searchQuery),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				ContentNodeRESTUtils.assertResponseOK(response);
				return response.getFiles().stream().map(File::getId).collect(Collectors.toSet());
			}
			case page:
			{
				LegacyPageListResponse response = res.getPages(folderId,
					new InFolderParameterBean().setFolderId(folderId),
					new PageListParameterBean().setNiceUrl(niceUrl),
					new LegacyFilterParameterBean().setSearch(searchQuery),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new PublishableParameterBean(),
					new WastebinParameterBean());

				ContentNodeRESTUtils.assertResponseOK(response);
				return response.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId).collect(Collectors.toSet());
			}
			default:
				fail("Cannot perform test for type " + type);
				return null;
			}
		});
	}

	/**
	 * Get the items via the specific REST resource.
	 *
	 * @param niceUrl Nice URL to search for
	 * @param searchQuery Search query
	 * @return List of found objects
	 * @throws NodeException
	 */
	private Set<Integer> getObjectIds(String niceUrl, String searchQuery) throws NodeException {
		return Trx.supply(() -> {
			String folderId = node.getFolder().getId().toString();

			switch (type) {
			case file:
			{
				FileListResponse response = ContentNodeRESTUtils.getFileResource().list(
					new InFolderParameterBean().setFolderId(folderId),
					new FileListParameterBean().setNiceUrl(niceUrl),
					new FilterParameterBean().setQuery(searchQuery),
					new SortParameterBean(),
					new PagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				ContentNodeRESTUtils.assertResponseOK(response);
				return response.getItems().stream().map(File::getId).collect(Collectors.toSet());
			}
			case image:
			{
				ImageListResponse response = ContentNodeRESTUtils.getImageResource().list(
					new InFolderParameterBean().setFolderId(folderId),
					new FileListParameterBean().setNiceUrl(niceUrl),
					new FilterParameterBean().setQuery(searchQuery),
					new SortParameterBean(),
					new PagingParameterBean(),
					new EditableParameterBean(),
					new WastebinParameterBean());

				ContentNodeRESTUtils.assertResponseOK(response);
				return response.getItems().stream().map(File::getId).collect(Collectors.toSet());
			}
			case page:
			{
				PageListResponse response = ContentNodeRESTUtils.getPageResource().list(
					new InFolderParameterBean().setFolderId(folderId),
					new PageListParameterBean().setNiceUrl(niceUrl),
					new FilterParameterBean().setQuery(searchQuery),
					new SortParameterBean(),
					new PagingParameterBean(),
					new PublishableParameterBean(),
					new WastebinParameterBean());

				ContentNodeRESTUtils.assertResponseOK(response);
				return response.getItems().stream().map(com.gentics.contentnode.rest.model.Page::getId).collect(Collectors.toSet());
			}
			default:
				fail("Cannot perform test for type " + type);
				return null;
			}
		});
	}
}
