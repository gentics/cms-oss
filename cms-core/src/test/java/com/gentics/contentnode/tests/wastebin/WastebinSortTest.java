package com.gentics.contentnode.tests.wastebin;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for sorting the wastebin
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.WASTEBIN })
public class WastebinSortTest {
	/**
	 * System User password
	 */
	private final static String PASSWORD = "password";

	/**
	 * Max nesting level
	 */
	private final static int MAX_LEVELS = 2;

	/**
	 * Folder names
	 */
	private final static List<String> NAMES = Arrays.asList("A Folder", "B Folder", "C Folder", "D Folder", "E Folder");

	/**
	 * Random
	 */
	public final static Random rand = new Random();

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test template
	 */
	private static Template template;

	/**
	 * Test folder ID
	 */
	private static String folderId;

	@Parameters(name = "{index}: class {0}, sortorder {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Class<? extends NodeObject> clazz : Arrays.asList(Folder.class, Page.class, File.class, ImageFile.class)) {
			for (String order : Arrays.asList("asc", "desc")) {
				data.add(new Object[] {clazz, order});
			}
		}
		return data;
	}

	/**
	 * Setup test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		TransactionManager.getCurrentTransaction().commit();

		// set systemuser password
		Trx.operate(() -> {
			SystemUser systemUser = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, 1);
			update(systemUser, user -> {
				user.setPassword(SystemUserFactory.hashPassword(PASSWORD, user.getId()));
			});
		});

		node = Trx.supply(() -> createNode());
		folderId = Trx.supply(() -> Integer.toString(node.getFolder().getId()));
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		Trx.operate(() -> createFolders(node.getFolder(), 0));

		Trx.operate(() -> {
			for (Folder f : node.getFolder().getChildFolders()) {
				f.delete();
			}
		});
	}

	/**
	 * Recursively create folders in random order up to {@link #MAX_LEVELS}
	 * @param mother mother folder
	 * @param level current level
	 * @throws NodeException
	 */
	protected static void createFolders(Folder mother, int level) throws NodeException {
		List<String> tmpNames = new ArrayList<>(NAMES);
		while (!tmpNames.isEmpty()) {
			int index = rand.nextInt(tmpNames.size());
			String name = tmpNames.remove(index);
			Folder folder = createFolder(mother, name);

			createPage(folder, template, "Page");
			try {
				createImage(folder, "image.jpg", IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg")));
			} catch (IOException e) {
				throw new NodeException(e);
			}
			createFile(folder, "file.txt", "File contents".getBytes());

			if (level + 1 < MAX_LEVELS) {
				createFolders(folder, level + 1);
			}
		}
	}

	/**
	 * Tested class
	 */
	@Parameter(0)
	public Class<? extends NodeObject> clazz;

	/**
	 * Tested sortorder
	 */
	@Parameter(1)
	public String order;

	/**
	 * Test sorting by path
	 * @throws NodeException
	 */
	@Test
	public void testSortByPath() throws RestException {
		try (LoggedInClient client = restContext.client("system", PASSWORD)) {
			List<String> paths = null;
			if (clazz == Folder.class) {
				LegacyFolderListResponse response = client.get().base().path("folder").path("getFolders").path(folderId)
						.queryParam("recursive", "true")
						.queryParam("sortby", "path")
						.queryParam("sortorder", order)
						.queryParam("wastebin", "only")
						.request().get(LegacyFolderListResponse.class);
				client.get().assertResponse(response);
				paths = response.getFolders().stream().map(com.gentics.contentnode.rest.model.Folder::getPath).collect(Collectors.toList());
			} else if (clazz == Page.class) {
				LegacyPageListResponse response = client.get().base().path("folder").path("getPages").path(folderId)
						.queryParam("recursive", "true")
						.queryParam("sortby", "path")
						.queryParam("sortorder", order)
						.queryParam("wastebin", "only")
						.request().get(LegacyPageListResponse.class);
				client.get().assertResponse(response);
				paths = response.getPages().stream().map(com.gentics.contentnode.rest.model.Page::getPath).collect(Collectors.toList());
			} else if (clazz == File.class) {
				LegacyFileListResponse response = client.get().base().path("folder").path("getFiles").path(folderId)
						.queryParam("recursive", "true")
						.queryParam("sortby", "path")
						.queryParam("sortorder", order)
						.queryParam("wastebin", "only")
						.request().get(LegacyFileListResponse.class);
				client.get().assertResponse(response);
				paths = response.getFiles().stream().map(com.gentics.contentnode.rest.model.File::getPath).collect(Collectors.toList());
			} else if (clazz == ImageFile.class) {
				LegacyFileListResponse response = client.get().base().path("folder").path("getImages").path(folderId)
						.queryParam("recursive", "true")
						.queryParam("sortby", "path")
						.queryParam("sortorder", order)
						.queryParam("wastebin", "only")
						.request().get(LegacyFileListResponse.class);
				client.get().assertResponse(response);
				paths = response.getFiles().stream().map(com.gentics.contentnode.rest.model.File::getPath).collect(Collectors.toList());
			}

			if ("desc".equals(order)) {
				assertThat(paths).as("Path list").isNotNull().isNotEmpty().isSortedAccordingTo(Collections.reverseOrder());
			} else {
				assertThat(paths).as("Path list").isNotNull().isNotEmpty().isSorted();
			}
		}
	}
}
