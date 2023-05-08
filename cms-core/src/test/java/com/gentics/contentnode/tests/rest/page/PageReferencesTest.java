package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.rest.model.response.ReferencedFilesListResponse;
import com.gentics.contentnode.rest.model.response.ReferencedPagesListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for getting referenced objects for pages
 */
public class PageReferencesTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Integer pageUrlConstructId;
	private static Integer fileUrlConstructId;
	private static Integer imageUrlConstructId;
	private static Template template;
	private static File file1;
	private static File file2;
	private static File file3;
	private static ImageFile image1;
	private static ImageFile image2;
	private static ImageFile image3;
	private static Page page1;
	private static Page page2;
	private static Page page3;
	private static Page using1;
	private static Page using2;
	private static Page using3;
	private static Page using0;

	/**
	 * Setup test data
	 * @throws NodeException
	 * @throws IOException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		node = Trx.supply(() -> createNode());

		pageUrlConstructId = Trx.supply(() -> createConstruct(node, PageURLPartType.class, "page", "page"));
		fileUrlConstructId = Trx.supply(() -> createConstruct(node, FileURLPartType.class, "file", "file"));
		imageUrlConstructId = Trx.supply(() -> createConstruct(node, ImageURLPartType.class, "image", "image"));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		file1 = Trx.supply(() -> createFile(node.getFolder(), "file1.txt", "Contents".getBytes()));
		file2 = Trx.supply(() -> createFile(node.getFolder(), "file2.txt", "Contents".getBytes()));
		file3 = Trx.supply(() -> createFile(node.getFolder(), "file3.txt", "Contents".getBytes()));

		byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		image1 = (ImageFile)Trx.supply(() -> createImage(node.getFolder(), "image1.jpg", imageData));
		image2 = (ImageFile)Trx.supply(() -> createImage(node.getFolder(), "image2.jpg", imageData));
		image3 = (ImageFile)Trx.supply(() -> createImage(node.getFolder(), "image3.jpg", imageData));

		page1 = Trx.supply(() -> createPage(node.getFolder(), template, "Page1"));
		page2 = Trx.supply(() -> createPage(node.getFolder(), template, "Page2"));
		page3 = Trx.supply(() -> createPage(node.getFolder(), template, "Page3"));

		using1 = Trx.supply(() -> update(createPage(node.getFolder(), template, "Using1"), p -> {
			usePage(p, page1);
			useFile(p, file1);
			useImage(p, image1);
		}));

		using2 = Trx.supply(() -> update(createPage(node.getFolder(), template, "Using2"), p -> {
			usePage(p, page1);
			usePage(p, page2);
			useFile(p, file1);
			useFile(p, file2);
			useImage(p, image1);
			useImage(p, image2);
		}));

		using3 = Trx.supply(() -> update(createPage(node.getFolder(), template, "Using3"), p -> {
			usePage(p, page1);
			usePage(p, page2);
			usePage(p, page3);
			useFile(p, file1);
			useFile(p, file2);
			useFile(p, file3);
			useImage(p, image1);
			useImage(p, image2);
			useImage(p, image3);
		}));

		using0 = Trx.supply(() -> createPage(node.getFolder(), template, "Using0"));
	}

	/**
	 * Test page using 0 pages
	 * @throws NodeException
	 */
	@Test
	public void testPage0() throws NodeException {
		doPageTest(using0);
	}

	/**
	 * Test page using 1 page
	 * @throws NodeException
	 */
	@Test
	public void testPage1() throws NodeException {
		doPageTest(using1, page1);
	}

	/**
	 * Test page using 2 pages
	 * @throws NodeException
	 */
	@Test
	public void testPage2() throws NodeException {
		doPageTest(using2, page1, page2);
	}

	/**
	 * Test page using 3 pages
	 * @throws NodeException
	 */
	@Test
	public void testPage3() throws NodeException {
		doPageTest(using3, page1, page2, page3);
	}

	/**
	 * Test all pages
	 * @throws NodeException
	 */
	@Test
	public void testPageAll() throws NodeException {
		doPageTest(Arrays.asList(using0, using1, using2, using3), page1, page2, page3);
	}

	/**
	 * Test page using 0 files
	 * @throws NodeException
	 */
	@Test
	public void testFile0() throws NodeException {
		doFileTest(using0);
	}

	/**
	 * Test page using 1 file
	 * @throws NodeException
	 */
	@Test
	public void testFile1() throws NodeException {
		doFileTest(using1, file1);
	}

	/**
	 * Test page using 2 files
	 * @throws NodeException
	 */
	@Test
	public void testFile2() throws NodeException {
		doFileTest(using2, file1, file2);
	}

	/**
	 * Test page using 3 files
	 * @throws NodeException
	 */
	@Test
	public void testFile3() throws NodeException {
		doFileTest(using3, file1, file2, file3);
	}

	/**
	 * Test all pages
	 * @throws NodeException
	 */
	@Test
	public void testFileAll() throws NodeException {
		doFileTest(Arrays.asList(using0, using1, using2, using3), file1, file2, file3);
	}

	/**
	 * Test page using 0 images
	 * @throws NodeException
	 */
	@Test
	public void testImage0() throws NodeException {
		doImageTest(using0);
	}

	/**
	 * Test page using 1 image
	 * @throws NodeException
	 */
	@Test
	public void testImage1() throws NodeException {
		doImageTest(using1, image1);
	}

	/**
	 * Test page using 2 images
	 * @throws NodeException
	 */
	@Test
	public void testImage2() throws NodeException {
		doImageTest(using2, image1, image2);
	}

	/**
	 * Test page using 3 images
	 * @throws NodeException
	 */
	@Test
	public void testImage3() throws NodeException {
		doImageTest(using3, image1, image2, image3);
	}

	/**
	 * Test all pages
	 * @throws NodeException
	 */
	@Test
	public void testImageAll() throws NodeException {
		doImageTest(Arrays.asList(using0, using1, using2, using3), image1, image2, image3);
	}

	/**
	 * Execute the page test
	 * @param pages list of referencing pages
	 * @param expected expected referenced pages
	 * @throws NodeException
	 */
	protected void doPageTest(Page page, Page...expected) throws NodeException {
		doPageTest(Arrays.asList(page), expected);
	}

	/**
	 * Execute the page test
	 * @param pages list of referencing pages
	 * @param expected expected referenced pages
	 * @throws NodeException
	 */
	protected void doPageTest(List<Page> pages, Page...expected) throws NodeException {
		Trx.operate(() -> {
			PageResource res = new PageResourceImpl();
			List<Integer> pageIds = new ArrayList<>();
			for (Page page : pages) {
				pageIds.add(page.getId());
			}
			ReferencedPagesListResponse response = res.getLinkedPages(0, -1, null, null, pageIds, null);
			assertThat(response).as("Response").hasCode(ResponseCode.OK);
			Set<Integer> itemIds = response.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getId).collect(Collectors.toSet());
			Integer[] expectedIds = new Integer[expected.length];
			for (int i = 0; i < expected.length; i++) {
				expectedIds[i] = expected[i].getId();
			}
			assertThat(itemIds).as("Returned item IDs").containsOnly(expectedIds);
			assertThat(response.getTotal()).as("Total").isEqualTo(expected.length);
			assertThat(response.getWithoutPermission()).as("Without permission").isEqualTo(0);
		});
	}

	/**
	 * Execute the file test
	 * @param pages list of referencing pages
	 * @param expected expected referenced files
	 * @throws NodeException
	 */
	protected void doFileTest(Page page, File...expected) throws NodeException {
		doFileTest(Arrays.asList(page), expected);
	}

	/**
	 * Execute the file test
	 * @param pages list of referencing pages
	 * @param expected expected referenced files
	 * @throws NodeException
	 */
	protected void doFileTest(List<Page> pages, File...expected) throws NodeException {
		Trx.operate(() -> {
			PageResource res = new PageResourceImpl();
			List<Integer> pageIds = new ArrayList<>();
			for (Page page : pages) {
				pageIds.add(page.getId());
			}
			ReferencedFilesListResponse response = res.getLinkedFiles(0, -1, null, null, pageIds, null);
			assertThat(response).as("Response").hasCode(ResponseCode.OK);
			Set<Integer> itemIds = response.getFiles().stream().map(com.gentics.contentnode.rest.model.File::getId).collect(Collectors.toSet());
			Integer[] expectedIds = new Integer[expected.length];
			for (int i = 0; i < expected.length; i++) {
				expectedIds[i] = expected[i].getId();
			}
			assertThat(itemIds).as("Returned item IDs").containsOnly(expectedIds);
			assertThat(response.getTotal()).as("Total").isEqualTo(expected.length);
			assertThat(response.getWithoutPermission()).as("Without permission").isEqualTo(0);
		});
	}

	/**
	 * Execute the image test
	 * @param pages list of referencing pages
	 * @param expected expected referenced images
	 * @throws NodeException
	 */
	protected void doImageTest(Page page, ImageFile...expected) throws NodeException {
		doImageTest(Arrays.asList(page), expected);
	}

	/**
	 * Execute the image test
	 * @param pages list of referencing pages
	 * @param expected expected referenced images
	 * @throws NodeException
	 */
	protected void doImageTest(List<Page> pages, ImageFile...expected) throws NodeException {
		Trx.operate(() -> {
			PageResource res = new PageResourceImpl();
			List<Integer> pageIds = new ArrayList<>();
			for (Page page : pages) {
				pageIds.add(page.getId());
			}
			ReferencedFilesListResponse response = res.getLinkedImages(0, -1, null, null, pageIds, null);
			assertThat(response).as("Response").hasCode(ResponseCode.OK);
			Set<Integer> itemIds = response.getFiles().stream().map(com.gentics.contentnode.rest.model.File::getId).collect(Collectors.toSet());
			Integer[] expectedIds = new Integer[expected.length];
			for (int i = 0; i < expected.length; i++) {
				expectedIds[i] = expected[i].getId();
			}
			assertThat(itemIds).as("Returned item IDs").containsOnly(expectedIds);
			assertThat(response.getTotal()).as("Total").isEqualTo(expected.length);
			assertThat(response.getWithoutPermission()).as("Without permission").isEqualTo(0);
		});
	}

	/**
	 * Add a tag to page p that referenced the used page
	 * @param p page
	 * @param used used page
	 * @throws NodeException
	 */
	protected static void usePage(Page p, Page used) throws NodeException {
		ContentTag pageTag = p.getContent().addContentTag(pageUrlConstructId);
		getPartType(PageURLPartType.class, pageTag, "page").setTargetPage(used);
	}

	/**
	 * Add a tag to page p that referenced the used file
	 * @param p page
	 * @param used used file
	 * @throws NodeException
	 */
	protected static void useFile(Page p, File used) throws NodeException {
		ContentTag fileTag = p.getContent().addContentTag(fileUrlConstructId);
		getPartType(FileURLPartType.class, fileTag, "file").setTargetFile(used);
	}

	/**
	 * Add a tag to page p that referenced the used image
	 * @param p page
	 * @param used used image
	 * @throws NodeException
	 */
	protected static void useImage(Page p, ImageFile used) throws NodeException {
		ContentTag imageTag = p.getContent().addContentTag(imageUrlConstructId);
		getPartType(ImageURLPartType.class, imageTag, "image").setTargetImage(used);
	}
}
