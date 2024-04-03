package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.db.DBUtils.executeUpdate;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONObject;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.AuthorizationRequestFilter;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.UnknownChannelException;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import com.gentics.testutils.infrastructure.EnvironmentException;

/**
 * This class is an extension of the existing NodeTestUtils
 * 
 * @author johannes2
 */
public final class ContentNodeTestUtils {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(ContentNodeTestUtils.class);

	/**
	 * Returns the file that points to the copy_configuration.xml
	 * 
	 * @return
	 * @throws EnvironmentException
	 */
	public static File getCopyConfiguration() throws EnvironmentException {
		File copyConf = new File("../contentnode-php/src/main/php/modules/content/sql/copy_configuration.xml");
		if (!copyConf.exists()) {
			throw new EnvironmentException("Could not locate copy_configuration.xml file in '" + copyConf.getAbsolutePath() + "'");
		}
		return copyConf;
	}

	/**
	 * Generates a files with the given length in bytes and returns an inputstream pointing to the file content
	 * 
	 * @param size
	 *            Size in kb
	 * 
	 * @return
	 * @throws IOException
	 */
	public static InputStream generateDataFile(long size) throws IOException {
		java.io.File file = new java.io.File(System.getProperty("java.io.tmpdir"), "TEMP");

		file.delete();
		file.createNewFile();
		FileOutputStream fos;
		DataOutputStream dos;

		fos = new FileOutputStream(file);
		dos = new DataOutputStream(fos);
		byte[] buffer = new byte[1024];

		// Write size kbs into file
		for (int i = 0; i < size; i++) {
			dos.write(buffer);
		}
		dos.flush();
		dos.close();
		return new FileInputStream(file);
	}

	/**
	 * Creates an inputstream from a string.
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static InputStream generateDataFile(String data) throws IOException {
		InputStream is = new ByteArrayInputStream(data.getBytes());
		return is;
	}

	/**
	 * Clear the nodeobject cache
	 * 
	 * @throws PortalCacheException
	 */
	public static void clearNodeObjectCache() throws PortalCacheException {
		PortalCache.getCache(NodeFactory.CACHEREGION).clear();
	}

	/**
	 * Set a renderType for the current transaction
	 * 
	 * @param editMode
	 *            edit mode of the render type
	 * @throws Exception
	 */
	public static void setRenderType(int editMode) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences preferences = t.getNodeConfig().getDefaultPreferences();

		RenderType renderType = RenderType.getDefaultRenderType(preferences, editMode, t.getSessionId(), 0);

		renderType.setRenderUrlFactory(new DynamicUrlFactory(t.getSessionId()));
		if (editMode == RenderType.EM_ALOHA || editMode == RenderType.EM_ALOHA_READONLY) {
			renderType.setParameter(AlohaRenderer.ADD_SCRIPT_INCLUDES, Boolean.TRUE);
		}

		t.setRenderType(renderType);
	}

	/**
	 * Publish the given pages (by calling the PHP-do). Calling this method will run the publish process, until all of the given pages are really published (have status
	 * 2). It will fail after 10 publish processes, that did not succeed in publishing all of the pages
	 * 
	 * @param context
	 *            test context
	 * @param pages
	 *            pages to be published
	 * @throws Exception
	 */
	public static void publishPage(ContentNodeTestContext context, SQLUtils dbUtils, Page... pages) throws Exception {
		if (ObjectTransformer.isEmpty(pages)) {
			return;
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		StringBuffer sql = new StringBuffer("SELECT count(*) c FROM publishqueue WHERE obj_type = 10007 AND obj_id IN (-1");

		// initialize dependency triggering
		DependencyManager.initDependencyTriggering();

		// publish all pages
		for (Page page : pages) {
			try {
				page.publish();
			} catch (ReadOnlyException e) {
				t.getObject(Page.class, page.getId(), true).publish();
			}
			sql.append(",").append(page.getId());
		}
		sql.append(")");

		// reset dependency triggering
		PublishQueue.finishFastDependencyDirting();
		DependencyManager.resetDependencyTriggering();

		// commit transaction
		t.commit(false);

		// clear the node object cache
		ContentNodeTestUtils.clearNodeObjectCache();

		// wait until everything was dirted
		context.waitForDirtqueueWorker(dbUtils);

		int tryCount = 0;
		int pubPageNum = 0;

		do {
			if (tryCount >= 10) {
				fail("Not all pages were published after " + tryCount + " publish runs (" + pubPageNum + "/" + pages.length + " pages still dirty)");
			}

			// wait 1 sec between publish runs
			if (tryCount > 0) {
				Thread.sleep(1000);
			}

			// run the publish process
			PublishInfo publishInfo = context.publish(false);

			assertTrue("Check whether publish process succeeded", publishInfo.getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

			// check whether all of the pages were published
			ResultSet dirtedPages = dbUtils.executeQuery(sql.toString());

			dirtedPages.next();

			pubPageNum = dirtedPages.getInt("c");
			tryCount++;
		} while (pubPageNum > 0);
	}

	public static String convertStreamToString(InputStream is) throws IOException {

		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	/**
	 * Creates a CR database and returns the sqlutils to that database
	 * 
	 * @param clazz
	 * @param postfix
	 *            The string that should be used to postfix the database name
	 * 
	 * @throws JDBCMalformedURLException
	 * @throws SQLUtilException
	 */
	public static SQLUtils createCRDatabase(Class<?> clazz, String postfix) throws JDBCMalformedURLException, SQLUtilException {
		logger.debug("Init the contentrepository..");
		TestDatabase crTestDB = TestDatabaseRepository.getMySQLNewStableDatabase();

		crTestDB.setRandomDatabasename("sandbox_" + postfix);
		SQLUtils crDBUtils = SQLUtilsFactory.getSQLUtils(crTestDB);
		crDBUtils.connectDatabase();
		crDBUtils.createCRDatabase(clazz);

		return crDBUtils;
	}

	/**
	 * Wait the number of milliseconds
	 * 
	 * @param ms
	 */
	public static void waitMS(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Wait until the system time is guaranteed to deliver another second than before this call
	 * @throws Exception
	 */
	public static void waitForNextSecond() {
		long startSecond = System.currentTimeMillis() / 1000;
		while ((System.currentTimeMillis() / 1000) == startSecond) {
			waitMS(100);
		}
	}

	/**
	 * Removes the selected database
	 * 
	 * @throws SQLUtilException
	 * @throws SQLException 
	 */
	public static void removeDB(SQLUtils sqlUtils) throws SQLUtilException, SQLException {
		if (sqlUtils != null) {
			if (sqlUtils.getConnection() == null || sqlUtils.getConnection().isClosed()) {
				sqlUtils.connectDatabase();
			}
			sqlUtils.removeDatabase();
			logger.debug("Removed CMS Database");
		}
	}

	/**
	 * Assert that exactly the given set of object were dirted for the given node
	 * @param clazz class of the objects to check
	 * @param node node
	 * @param expected list of expected objects
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T extends NodeObject> void assertDirtedObjects(final Class<T> clazz, Node node, T... expected) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<T> objects = null;
		try (ChannelTrx cTrx = new ChannelTrx(node)) {
			objects = t.getObjects(clazz, PublishQueue.getDirtedObjectIds(clazz, false, node));
		}
		assertThat(objects).as("Dirted objects of type " + clazz.getSimpleName() + " in node " + node.getFolder().getName()).containsOnly(expected);
	}

	/**
	 * Assert that exactly the given number of object were dirted for the given node
	 * @param clazz class of the objects to check
	 * @param node node
	 * @param expected expected number of dirted objects
	 * @throws NodeException
	 */
	public static <T extends NodeObject> void assertDirtedObjects(final Class<T> clazz, Node node, int expected) throws NodeException {
		List<Integer> ids = null;
		try (ChannelTrx cTrx = new ChannelTrx(node)) {
			ids = PublishQueue.getDirtedObjectIds(clazz, false, node);
		}
		assertThat(ids).as("Dirted objects of type " + clazz.getSimpleName() + " in node " + node.getFolder().getName()).hasSize(expected);
	}

	/**
	 * Check whether the object was published into the cr for the given node
	 * @param object object
	 * @param node node
	 * @param expected true if the object is expected to be published, false if not
	 * @param asserters optional list of additional asserters
	 * @throws Exception
	 */
	@SafeVarargs
	public static void assertPublishCR(NodeObject object, Node node, boolean expected, Consumer<Resolvable>... asserters) throws NodeException {
		ContentMap contentMap = node.getContentMap();
		if (contentMap == null && node.isChannel()) {
			contentMap = node.getMaster().getContentMap();
		}
		assertNotNull(node.getFolder().getName() + " must publish into a contentmap", contentMap);
		int ttype = object.getTType();
		if (ttype == ContentFile.TYPE_IMAGE) {
			ttype = ContentFile.TYPE_FILE;
		}
		String contentId = ttype + "." + object.getId();
		Datasource ds = null;
		if (contentMap.isMultichannelling()) {
			MCCRDatasource mccrDs = contentMap.getMCCRDatasource();
			try {
				mccrDs.setChannel(node.getId());
			} catch (UnknownChannelException e) {
				if (expected == false) {
					// if we expect the object to not be published into the MCCR, it is ok
					// when the channel does not even exist
					return;
				} else {
					throw e;
				}
			}
			ds = mccrDs;
		} else {
			ds = contentMap.getDatasource();
		}
		Resolvable contentObject = PortalConnectorFactory.getContentObject(contentId, ds);
		if (expected) {
			assertNotNull(object + " should have been published into the cr of " + node.getFolder().getName(), contentObject);
			for (Consumer<Resolvable> asserter : asserters) {
				asserter.accept(contentObject);
			}
		} else {
			assertNull(object + " should not have been published into cr of " + node.getFolder().getName(), contentObject);
		}
	}

	/**
	 * Assert that the page was published into the filesystem (or was not published)
	 * @param pubDir base pub dir
	 * @param page page
	 * @param node node
	 * @param expected true if the object is expected to be published, false if not
	 * @throws NodeException
	 */
	public static void assertPublishFS(File pubDir, LocalizableNodeObject<?> object, Node node, boolean expected) throws NodeException {
		if (object instanceof Page) {
			assertPublishFS(pubDir, (Page)object, node, expected);
		} else if (object instanceof com.gentics.contentnode.object.File) {
			assertPublishFS(pubDir, (com.gentics.contentnode.object.File)object, node, expected);
		}
	}

	/**
	 * Assert that the page was published into the filesystem (or was not published)
	 * @param pubDir base pub dir
	 * @param page page
	 * @param node node
	 * @param expected true if the object is expected to be published, false if not
	 * @return checked file
	 * @throws NodeException
	 */
	public static File assertPublishFS(File pubDir, Page page, Node node, boolean expected) throws NodeException {
		String languageCode = page.getLanguage() != null ? page.getLanguage().getCode() : null;
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			Set<String> urls = page.getNiceAndAlternateUrls();
			for (String url : urls) {
				assertPublishFS(pubDir, false, url, node, expected);
			}
		}

		return assertPublishFS(pubDir, false, page.getFilename(), languageCode, page.getFolder(), node, expected);
	}

	/**
	 * Assert that the file was published into the filesystem (or was not published)
	 * @param pubDir base pub dir
	 * @param File file
	 * @param node node
	 * @param expected true if the object is expected to be published, false if not
	 * @return checked file
	 * @throws NodeException
	 */
	public static File assertPublishFS(File pubDir, com.gentics.contentnode.object.File file, Node node, boolean expected) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			Set<String> urls = file.getNiceAndAlternateUrls();
			for (String url : urls) {
				assertPublishFS(pubDir, true, url, node, expected);
			}
		}
		return assertPublishFS(pubDir, true, file.getFilename(), null, file.getFolder(), node, expected);
	}

	/**
	 * Assert that the file with given name was published into the filesystem (or was not published)
	 * @param pubDir base pub dir
	 * @param binary true for files, false for pages
	 * @param fileName file name
	 * @param languageCode optional language code (may be null)
	 * @param folder folder
	 * @param node node
	 * @param expected true if the object is expected to be published, false if not
	 * @return checked file
	 * @throws NodeException
	 */
	public static File assertPublishFS(File pubDir, boolean binary, String fileName, String languageCode, Folder folder, Node node, boolean expected) throws NodeException {
		File baseDir = new File(pubDir, node.getHostname());
		File nodePubDir = StaticUrlFactory.ignoreNodePublishDir(node.getContentRepository()) ? baseDir
				: new File(baseDir,
						binary && !StaticUrlFactory.ignoreSeparateBinaryPublishDir(node.getContentRepository())
								? node.getBinaryPublishDir()
								: node.getPublishDir());

		if (languageCode != null && node.getPageLanguageCode() == PageLanguageCode.PATH) {
			nodePubDir = new File(nodePubDir, languageCode);
		}

		File folderPubDir = null;
		if (node.isPubDirSegment()) {
			folderPubDir = nodePubDir;
			List<Folder> folders = new ArrayList<>();
			folders.add(folder);
			folders.addAll(folder.getParents());
			Collections.reverse(folders);

			for (Folder f : folders) {
				folderPubDir = new File(folderPubDir, f.getPublishDir());
			}
		} else {
			folderPubDir = new File(nodePubDir, folder.getPublishDir());
		}

		File pageFile = new File(folderPubDir, fileName);

		if (expected) {
			assertTrue("Expecting " + pageFile + " to exist", pageFile.exists());
		} else {
			assertFalse("Expecting " + pageFile + " to not exist", pageFile.exists());
		}
		return pageFile;
	}

	/**
	 * Assert that the file with given nice URL was published into the filesystem (or was not published)
	 * @param pubDir base pub dir
	 * @param binary true for files, false for pages
	 * @param niceUrl nice URL
	 * @param node node
	 * @param expected true if the object is expected to be published, false if not
	 * @return checked file
	 * @throws NodeException
	 */
	public static File assertPublishFS(File pubDir, boolean binary, String niceUrl, Node node, boolean expected) throws NodeException {
		File baseDir = new File(pubDir, node.getHostname());
		File nodePubDir = StaticUrlFactory.ignoreNodePublishDir(node.getContentRepository()) ? baseDir
				: new File(baseDir,
						binary && !StaticUrlFactory.ignoreSeparateBinaryPublishDir(node.getContentRepository())
								? node.getBinaryPublishDir()
								: node.getPublishDir());

		File pageFile = new File(nodePubDir, niceUrl);

		if (expected) {
			assertTrue("Expecting " + pageFile + " to exist", pageFile.exists());
		} else {
			assertFalse("Expecting " + pageFile + " to not exist", pageFile.exists());
		}
		return pageFile;
	}

	/**
	 * Assert that the GIS file was published into the filesystem (or was not published)
	 * @param pubDir base pub dir
	 * @param file file
	 * @param node node
	 * @param width resize width
	 * @param height resize height
	 * @param mode resize mode
	 * @param expected true if the file is expected, false if not
	 * @return checked file
	 * @throws NodeException
	 */
	public static File assertPublishGISFS(File pubDir, com.gentics.contentnode.object.File file, Node node,
			String width, String height, String mode, boolean expected) throws NodeException {
		File base = Paths.get(pubDir.getAbsolutePath(), node.getHostname(), "GenticsImageStore", width, height, mode,
				StaticUrlFactory.ignoreNodePublishDir(node.getContentRepository()) ? ""
						: (StaticUrlFactory.ignoreSeparateBinaryPublishDir(node.getContentRepository())
								? node.getPublishDir()
								: node.getBinaryPublishDir()))
				.toFile();
		Folder folder = file.getFolder();

		File folderPubDir = null;
		if (node.isPubDirSegment()) {
			folderPubDir = base;
			List<Folder> folders = new ArrayList<>();
			folders.add(folder);
			folders.addAll(folder.getParents());
			Collections.reverse(folders);

			for (Folder f : folders) {
				folderPubDir = new File(folderPubDir, f.getPublishDir());
			}
		} else {
			folderPubDir = new File(base, folder.getPublishDir());
		}
		File gisFile = new File(folderPubDir, file.getFilename());

		if (expected) {
			assertTrue("Expecting " + gisFile + " to exist", gisFile.exists());
		} else {
			assertFalse("Expecting " + gisFile + " to not exist", gisFile.exists());
		}

		return gisFile;
	}

	/**
	 * Assert that the page was published with the given content
	 * @param node node
	 * @param page page
	 * @param expectedContent expected page content
	 * @throws Exception
	 */
	public static void assertPublishedPageContent(final Node node, final Page page, String expectedContent) throws Exception {
		final String[] actualContent = new String[1];
		DBUtils.executeStatement("SELECT source FROM publish WHERE node_id = ? AND page_id = ? AND active = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setObject(1, node.getId());
				stmt.setObject(2, page.getId());
				stmt.setInt(3, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					actualContent[0] = rs.getString("source");
				}
			}
		});

		assertNotNull("The published page content was not found", actualContent[0]);
		assertEquals("Check the published page content", expectedContent, actualContent[0]);
	}


	/**
	 * Make an assertion about the response
	 * @param response response
	 */
	public static void assertResponseCodeOk(GenericResponse response) {
		ContentNodeTestUtils.assertResponseCode(response, ResponseCode.OK);
	}

	/**
	 * Make an assertion about the response
	 * @param response response
	 */
	public static void assertResponseCode(GenericResponse response, ResponseCode responseCode) {
		assertNotNull("The response did not contain a response info.", response.getResponseInfo());
		assertTrue("Response was not " + responseCode.toString() + ": " + response.getResponseInfo().getResponseCode().toString()
				+ " - Messages: " + response.getResponseInfo().getResponseMessage(),
				response.getResponseInfo().getResponseCode() == responseCode);
	}

	/**
	 * Assert that the given object collections contain exactly the same objects (order does not matter)
	 * @param expected expected object collection
	 * @param list object collection to check
	 */
	public static <T extends NodeObject> void assertCollection(Collection<T> expected, Collection<T> list) {
		Collection<T> tmp = new ArrayList<T>(expected);
		tmp.removeAll(list);
		assertTrue("Did not find expected objects: " + tmp, tmp.isEmpty());

		tmp.clear();
		tmp.addAll(list);
		tmp.removeAll(expected);
		assertTrue("Found unexpected objects: " + tmp, tmp.isEmpty());
	}

	/**
	 * Checks whether the child nodeObject type can inherit
	 * object property permissions from parent
	 * @param child   The child object type (page.TYPE_PAGE, ...)
	 * @param parent  The parent object type
	 * @return
	 */
	public static boolean doesObjectTypeInheritObjectPropertyPermissionsFromObjectType(int child, int parent) {
		if (child == Page.TYPE_PAGE && parent == Template.TYPE_TEMPLATE) {
			return true;
		}

		if ((child == Page.TYPE_PAGE || child == com.gentics.contentnode.object.File.TYPE_FILE
				|| child == ImageFile.TYPE_IMAGE)
				&& parent == Folder.TYPE_FOLDER) {
			return true;
		}

		return false;
	}

	/**
	 * Compares two {@link JSONArray JSON arrays} for equality.
	 *
	 * Two JSON arrays are considered equal, if they are both <code>null</code>, or they
	 * equal values in the same order. If any objects in the array are themselves JSON
	 * objects or arrays they are compared recursively with
	 * {@link #equal(JSONObject, JSONObject)} or {@link #equal(JSONArray, JSONArray)}.
	 *
	 * @param a1 The first JSON array.
	 * @param a2 The second JSON array.
	 * @return <code>true</code> if the arrays are equal, <code>false</code> otherwise.
	 */
	public static boolean equal(JSONArray a1, JSONArray a2) {
		if (a1 == null || a2 == null) {
			return a1 == a2;
		}

		if (a1.length() != a2.length()) {
			return false;
		}

		for (int ii = 0, len = a1.length(); ii < len; ii++) {
			Object o1 = a1.opt(ii);
			Object o2 = a2.opt(ii);

			if (o1 instanceof JSONObject && o2 instanceof JSONObject) {
				if (!equal((JSONObject) o1, (JSONObject) o2)) {
					return false;
				}
			} else if (o1 instanceof JSONArray && o2 instanceof JSONArray) {
				if (!equal((JSONArray) o1, (JSONArray) o2)) {
					return false;
				}
			} else if (ObjectTransformer.equals(o1, o2)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Test two {@link JSONObject JSON objects} for equality.
	 *
	 * Two JSON objects are considered equal, if they are both <code>null</code>,
	 * or they contain the same key &rarr; value mappings. If any of the values
	 * is itself a JSON object or an array, the values are compared recursively
	 * with {@link #equal(JSONObject, JSONObject)} or {@link #equal(JSONArray, JSONArray)}.
	 *
	 * @param o1 The first JSON object.
	 * @param o2 The second JSON object.
	 * @return <code>true</code> if the objects are equal, <code>false</code> otherwise.
	 */
	public static boolean equal(JSONObject o1, JSONObject o2) {
		if (o1 == null || o2 == null) {
			return o1 == o2;
		}

		String[] o1names = JSONObject.getNames(o1);
		Set<String> o2nameSet = new HashSet<>(Arrays.asList(JSONObject.getNames(o2)));

		if (o1names.length != o2nameSet.size()) {
			return false;
		}

		for (String name : o1names) {
			if (!o2nameSet.contains(name)) {
				return false;
			}

			Object o1val = o1.opt(name);
			Object o2val = o2.opt(name);

			if (o1val instanceof JSONObject && o2val instanceof JSONObject) {
				if (!equal((JSONObject) o1val, (JSONObject) o2val)) {
					return false;
				}
			} else if (o1val instanceof JSONArray && o2val instanceof JSONArray) {
				if (!equal((JSONArray) o1val, (JSONArray) o2val)) {
					return false;
				}
			} else if (!ObjectTransformer.equals(o1val, o2val)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Check if an action is in the publish queue for all of the selected channels and for no other channel
	 * @param typeid event object type
	 * @param objid	event object id
	 * @param action	action
	 * @param channels	array of node ids where the event is expected to be contained
	 * @throws Exception 
	 */
	public static void assertInPublishqueue(final int typeid, final int objid, final Action action, int[] channels) throws Exception {
		final Set<Integer> results = new HashSet<Integer>();
		DBUtils.executeStatement("SELECT channel_id FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND action = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, typeid);
				stmt.setInt(2, objid);
				stmt.setString(3, action.toString());
			}
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while(rs.next()) {
					results.add(rs.getInt(1));
				}
			}
		});
		for (int channel:channels) {
			assertTrue("Channel " + channel + " expected to be affected, but isn't.", results.contains(channel));
		}
		assertEquals("Extra channels affected", channels.length, results.size());
	}

	/**
	 * Get the group permissions from the DB as set of triples (type, id, perm)
	 * @param groupId group ID
	 * @return permissions
	 * @throws NodeException
	 */
	public static Set<Triple<Integer, Integer, String>> getGroupPerms(Integer groupId) throws NodeException {
		return DBUtils.select("SELECT o_type, o_id, perm FROM perm WHERE usergroup_id = ?", ps -> ps.setInt(1, groupId), rs -> {
			Set<Triple<Integer, Integer, String>> perms = new HashSet<>();
			while (rs.next()) {
				perms.add(Triple.of(rs.getInt("o_type"), rs.getInt("o_id"), rs.getString("perm")));
			}
			return perms;
		});
	}

	/**
	 * Assert that executing the given action with the user (in the group) requires the given permissions.
	 * First the action is executed with all but one permissions are set and a failure (due to insufficient permissions) is expected.
	 * The the action is executed with all permissions set and success is expected.
	 * Executing this method will change the permissions set for the group
	 * @param group group, which will get the permissions set.
	 * @param user user to execute the action with
	 * @param action action to execute
	 * @param requiredPerms list of required permissions. Each triple consists of objType, objId and permBit.
	 * @return response of the successful action
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T extends GenericResponse> T assertRequiredPermissions(UserGroup group, SystemUser user, Supplier<T> action,
			Triple<Integer, Integer, Integer>... requiredPerms) throws NodeException {
		return assertRequiredPermissions(group, user, action, ResponseCode.OK, requiredPerms);
	}

	/**
	 * Assert that executing the given action with the user (in the group) requires the given permissions.
	 * First the action is executed with all but one permissions are set and a failure (due to insufficient permissions) is expected.
	 * The the action is executed with all permissions set and success is expected.
	 * Executing this method will change the permissions set for the group
	 * @param group group, which will get the permissions set.
	 * @param user user to execute the action with
	 * @param action action to execute
	 * @param responseCode expected response code
	 * @param requiredPerms list of required permissions. Each triple consists of objType, objId and permBit.
	 * @return response of the successful action
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T extends GenericResponse> T assertRequiredPermissions(UserGroup group, SystemUser user, Supplier<T> action,
			ResponseCode responseCode, Triple<Integer, Integer, Integer>... requiredPerms) throws NodeException {

		// filter out null values and duplicates
		List<Triple<Integer, Integer, Integer>> requiredPermsList = Stream.of(requiredPerms).filter(triple -> triple != null).distinct().collect(Collectors.toList());

		// set all permission but one and check that action fails with permission error
		for (Triple<Integer, Integer, Integer> omit : requiredPermsList) {
			Map<Pair<Integer, Integer>, Permission> permissionsMap = new HashMap<>();

			for (Triple<Integer, Integer, Integer> set : requiredPermsList) {
				if (omit == set) {
					continue;
				}

				Pair<Integer, Integer> typeAndId = Pair.of(set.getLeft(), set.getMiddle());
				permissionsMap.computeIfAbsent(typeAndId, key -> new Permission()).mergeBits(new Permission(set.getRight()).toString());
			}

			// delete all existing permissions for the group
			operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { group.getId() }));
			operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));

			// set the permissions
			for (Map.Entry<Pair<Integer, Integer>, Permission> entry : permissionsMap.entrySet()) {
				operate(() -> setPermissions(entry.getKey().getLeft(), entry.getKey().getRight(), Arrays.asList(group), entry.getValue().toString()));
			}

			try (Trx trx = new Trx(user)) {
				assertResponseCode(action.supply(), ResponseCode.PERMISSION);
			} catch (InsufficientPrivilegesException e) {
				// this exception is expected
			}
		}

		// delete all existing permissions for the group
		operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { group.getId() }));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));

		// set all permissions
		Map<Pair<Integer, Integer>, Permission> permissionsMap = new HashMap<>();

		for (Triple<Integer, Integer, Integer> set : requiredPermsList) {
			Pair<Integer, Integer> typeAndId = Pair.of(set.getLeft(), set.getMiddle());
			permissionsMap.computeIfAbsent(typeAndId, key -> new Permission()).mergeBits(new Permission(set.getRight()).toString());
		}

		// set the permissions
		for (Map.Entry<Pair<Integer, Integer>, Permission> entry : permissionsMap.entrySet()) {
			operate(() -> setPermissions(entry.getKey().getLeft(), entry.getKey().getRight(), Arrays.asList(group), entry.getValue().toString()));
		}

		T response = null;
		try (Trx trx = new Trx(user)) {
			response = action.supply();
			assertResponseCode(response, responseCode);
		} catch (InsufficientPrivilegesException e) {
			fail("Action failed due to insufficient privileges");
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T extends GenericResponse> T assertRequiredPermissions(UserGroup group, SystemUser user, Object resourceInstance, String methodName, Class<?>[] parameterTypes, Object[] parameters,
			Triple<Integer, Integer, Integer>... requiredPerms) throws NodeException {

		Class<?> resourceClass = resourceInstance.getClass();
		Method method;
		try {
			method = resourceInstance.getClass().getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new NodeException(e);
		}

		return assertRequiredPermissions(group, user, () -> {
			AuthorizationRequestFilter.check(method, resourceClass);
			try {
				return (T) method.invoke(resourceInstance, parameters);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				if (e.getCause() instanceof NodeException) {
					throw (NodeException) e.getCause();
				} else {
					throw new NodeException(e);
				}
			}
		}, requiredPerms);
	}

	/**
	 * Assert that executing the given action using the REST Client with the user (in the group) requires the given permissions.
	 * First the action is executed with all but one permissions are set and a failure (due to insufficient permissions) is expected.
	 * The the action is executed with all permissions set and success is expected.
	 * Executing this method will change the permissions set for the group
	 * @param group group, which will get the permissions set.
	 * @param login login name of the user
	 * @param password password of the user
	 * @param context rest application context
	 * @param action action to execute
	 * @param requiredPerms list of required permissions. Each triple consists of objType, objId and permBit.
	 * @return response of the successful action
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T extends GenericResponse> T assertRequiredPermissions(UserGroup group, String login,
			String password, RESTAppContext context, Function<WebTarget, T> action,
			Triple<Integer, Integer, Integer>... requiredPerms) throws NodeException {
		return assertRequiredPermissions(group, login, password, context, action, ResponseCode.OK, requiredPerms);
	}

	/**
	 * Assert that executing the given action using the REST Client with the user (in the group) requires the given permissions.
	 * First the action is executed with all but one permissions are set and a failure (due to insufficient permissions) is expected.
	 * The the action is executed with all permissions set and a selected response code is expected.
	 * Executing this method will change the permissions set for the group
	 * @param group group, which will get the permissions set.
	 * @param login login name of the user
	 * @param password password of the user
	 * @param context rest application context
	 * @param action action to execute
	 * @param responseCode a custom expected response code
	 * @param requiredPerms list of required permissions. Each triple consists of objType, objId and permBit.
	 * @return response of the successful action
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T extends GenericResponse> T assertRequiredPermissions(UserGroup group, String login,
			String password, RESTAppContext context, Function<WebTarget, T> action, ResponseCode responseCode,
			Triple<Integer, Integer, Integer>... requiredPerms) throws NodeException {

		return assertRequiredPermissions(group, login, password, context, action, ContentNodeTestUtils::assertResponseCode, ResponseCode.PERMISSION, responseCode, requiredPerms);
	}

	/**
	 * Assert that executing the given action using the REST Client with the user (in the group) requires the given permissions.
	 * First the action is executed with all but one permissions are set and a failure (due to insufficient permissions) is expected.
	 * The the action is executed with all permissions set and a selected response code is expected.
	 * Executing this method will change the permissions set for the group
	 * @param group group, which will get the permissions set.
	 * @param login login name of the user
	 * @param password password of the user
	 * @param context rest application context
	 * @param action action to execute
	 * @param responseCode a custom expected response code
	 * @param requiredPerms list of required permissions. Each triple consists of objType, objId and permBit.
	 * @return response of the successful action
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T, R> T assertRequiredPermissions(UserGroup group, String login,
			String password, RESTAppContext context, Function<WebTarget, T> action, BiConsumer<T, R> responseCodeChecker, R permissionResponseCode, R responseCode,
			Triple<Integer, Integer, Integer>... requiredPerms) throws NodeException {

		// set all permission but one and check that action fails with permission error
		for (Triple<Integer, Integer, Integer> omit : requiredPerms) {
			Map<Pair<Integer, Integer>, Permission> permissionsMap = new HashMap<>();

			for (Triple<Integer, Integer, Integer> set : requiredPerms) {
				if (omit == set) {
					continue;
				}

				Pair<Integer, Integer> typeAndId = Pair.of(set.getLeft(), set.getMiddle());
				permissionsMap.computeIfAbsent(typeAndId, key -> new Permission()).mergeBits(new Permission(set.getRight()).toString());
			}

			// delete all existing permissions for the group
			operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { group.getId() }));
			operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));

			// set the permissions
			for (Map.Entry<Pair<Integer, Integer>, Permission> entry : permissionsMap.entrySet()) {
				operate(() -> setPermissions(entry.getKey().getLeft(), entry.getKey().getRight(), Arrays.asList(group), entry.getValue().toString()));
			}

			try (LoggedInClient client = context.client(login, password)) {
				responseCodeChecker.accept(action.apply(client.get().base()), permissionResponseCode);
			} catch (ForbiddenException e) {
				// this exception is expected
			} catch (RestException e) {
				throw new NodeException(e);
			}
		}

		// delete all existing permissions for the group
		operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { group.getId() }));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));

		// set all permissions
		Map<Pair<Integer, Integer>, Permission> permissionsMap = new HashMap<>();

		for (Triple<Integer, Integer, Integer> set : requiredPerms) {
			Pair<Integer, Integer> typeAndId = Pair.of(set.getLeft(), set.getMiddle());
			permissionsMap.computeIfAbsent(typeAndId, key -> new Permission()).mergeBits(new Permission(set.getRight()).toString());
		}

		// set the permissions
		for (Map.Entry<Pair<Integer, Integer>, Permission> entry : permissionsMap.entrySet()) {
			operate(() -> setPermissions(entry.getKey().getLeft(), entry.getKey().getRight(), Arrays.asList(group), entry.getValue().toString()));
		}

		T response = null;
		try (LoggedInClient client = context.client(login, password)) {
			response = action.apply(client.get().base());
			responseCodeChecker.accept(response, responseCode);
		} catch (InsufficientPrivilegesException e) {
			fail("Action failed due to insufficient privileges");
		} catch (RestException e) {
			throw new NodeException(e);
		}
		return response;
	}

	/**
	 * Get the required permission as Triple of Integers, which can be passed to
	 * {@link #assertRequiredPermissions(UserGroup, SystemUser, Supplier, Triple...)}
	 * or
	 * {@link #assertRequiredPermissions(UserGroup, String, String, RESTAppContext, Function, Triple...)}
	 * 
	 * @param object object
	 * @param permType permission type
	 * @return Triple of Integers
	 * @throws NodeException 
	 */
	public static Triple<Integer, Integer, Integer> perm(NodeObject object, PermType permType) throws NodeException {
		if (object instanceof Folder && ((Folder) object).isRoot()) {
			return Triple.of(Node.TYPE_NODE, object.getId(), permType.getBit());
		} else {
			return Triple.of(object.getTType(), object.getId(), permType.getBit());
		}
	}

	public static Triple<Integer, Integer, Integer> perm(TypePerms type, PermType permType) {
		return Triple.of(type.type(), 0, permType.getBit());
	}
}
