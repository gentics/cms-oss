package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.StreamingResolvable;
import com.gentics.api.portalnode.connector.CRSync;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.api.portalnode.connector.PortalConnectorHelper;
import com.gentics.lib.content.FilesystemAttributeValue;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentFactory.StoragePathInfo;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementException;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.io.FileRemover;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.infrastructure.TestEnvironment;
import org.junit.experimental.categories.Category;

/**
 * Test cases for ContentRepositories that write attributes into the filesystem
 */
@Category(BaseLibTest.class)
public class FilesystemAttributeTest {

	/**
	 * Object type
	 */
	public final static int OBJECT_TYPE = 5000;

	/**
	 * Data names
	 */
	public final static String[] NAMES = { "Berni", "Clemens", "Johannes", "Norbert", "Petro", "Taylor"};

	/**
	 * Datasource name for the datasource that is the target of a crsync
	 */
	public final static String DS_SYNCTARGET = "synctarget";

	/**
	 * Name of the attribute, that is stored in the filesystem
	 */
	public final static String FILESYSTEM_ATTRIBUTE = "name2";

	/**
	 * Name of the attribute, that is not stored in the filesystem
	 */
	public final static String NON_FILESYSTEM_ATTRIBUTE = "name";

	/**
	 * Name of the multivalue long string attribute, stored in the filesystem
	 */
	public final static String MULTISTRING_ATTRIBUTE = "multistring";

	/**
	 * Name of the singlevalue binary attribute, stored in the filesystem
	 */
	public final static String SINGLEBINARY_ATTRIBUTE = "singlebinary";

	/**
	 * Name of the multivalue binary attribute, stored in the filesystem
	 */
	public final static String MULTIBINARY_ATTRIBUTE = "multibinary";

	/**
	 * Object type
	 */
	public final static String PATH = "c4/53/10008.43.binarycontent.0.47";

	/**
	 * Content IDs of the objects
	 */
	protected List<String> contentIds = new Vector<String>();

	/**
	 * Filesystem datasource
	 */
	protected CNWriteableDatasource filesystemDS;

	/**
	 * Synctarget datasource
	 */
	protected CNWriteableDatasource synctargetDS;

	/**
	 * Create the datasource instances.
	 * The tests use up to two different datasource instances, which are created as hsql in memory
	 * {@link #filesystemDS} will be filled with data and {@link #synctargetDS} will be empty.
	 */
	@Before
	public void setUp() throws Exception {
		File commonBase = new File(".", "target/" + TestEnvironment.getRandomHash(8));
		File fsBase = new File(commonBase, "attributes");
		File syncBase = new File(commonBase, "syncattributes");

		fsBase.mkdirs();
		syncBase.mkdirs();

		Map<String, String> fsHandle = new HashMap<String, String>();

		fsHandle.put("type", "jdbc");

		fsHandle.put("driverClass", "org.hsqldb.jdbcDriver");
		fsHandle.put("url", "jdbc:hsqldb:mem:filesystem");
		fsHandle.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> fsProps = new HashMap<String, String>();

		fsProps.put("sanitycheck2", "true");
		fsProps.put("autorepair2", "true");
		fsProps.put("cache", "true");
		fsProps.put("attribute.path", fsBase.getAbsolutePath());
		fsProps.put("prefetchAttribute.threshold", "0");

		filesystemDS = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(fsHandle, fsProps);

		// create structure and save data
		createStructure(filesystemDS);
		for (String name : NAMES) {
			contentIds.add(saveData(filesystemDS, name));
		}

		Map<String, String> stHandle = new HashMap<String, String>();

		stHandle.put("type", "jdbc");

		stHandle.put("driverClass", "org.hsqldb.jdbcDriver");
		stHandle.put("url", "jdbc:hsqldb:mem:synctarget");
		stHandle.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> stProps = new HashMap<String, String>();

		stProps.put("sanitycheck2", "true");
		stProps.put("autorepair2", "true");
		stProps.put("cache", "true");
		stProps.put("attribute.path", syncBase.getAbsolutePath());
		stProps.put("prefetchAttribute.threshold", "0");

		synctargetDS = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(stHandle, stProps);
	}

	/**
	 * Destroy the datasources
	 */
	@After
	public void tearDown() {
		PortalConnectorHelper.clearCache(filesystemDS);
		PortalConnectorHelper.clearCache(synctargetDS);
		FileUtil.cleanDirectory(new File(filesystemDS.getAttributePath()));
		FileUtil.cleanDirectory(new File(synctargetDS.getAttributePath()));
		PortalConnectorFactory.destroy();
	}

	/**
	 * Test that filtering for attributes that are written into the file system fails
	 * @throws Exception
	 */
	@Test
	public void testFilterForFilesystemAttributes() throws Exception {
		Expression expression = ExpressionParser.getInstance().parse("object.name2 == 'Norbert'");

		try {
			filesystemDS.createDatasourceFilter(expression);
			fail("Creating a datasource filter should have failed");
		} catch (ExpressionParserException e) {// anticipated
		}
	}

	/**
	 * Test whether filtering for non-file system attributes is still possible, even if the datasource
	 * has attributes written into the file system
	 * @throws Exception
	 */
	@Test
	public void testFilterForNonFilesystemAttributes() throws Exception {
		Expression expression = ExpressionParser.getInstance().parse("object.name == 'Norbert'");
		DatasourceFilter filter = filesystemDS.createDatasourceFilter(expression);
		Collection<Resolvable> result = filesystemDS.getResult(filter, null);

		assertEquals("Check # of results", 1, result.size());
		for (Resolvable obj : result) {
			assertEquals("Check name", "Norbert", obj.get("name"));
		}
	}

	/**
	 * Test generating the storage location path
	 * @throws Exception
	 */
	@Test
	public void testStorageLocation() throws Exception {
		String path = GenticsContentFactory.getStoragePath("10008.43", "binarycontent", 0, 47);

		assertEquals(PATH, path);
		StoragePathInfo info = new GenticsContentFactory.StoragePathInfo(path);

		assertEquals("Check parsed contentid", info.contentId, "10008.43");
		assertEquals("Check parsed name", info.name, "binarycontent");
		assertEquals("Check parsed sortorder", info.sortorder, 0);
		assertEquals("Check parsed transaction id", info.transactionId, 47);
	}

	/**
	 * Test existence of files for attributes stored in the file system
	 * @throws Exception
	 */
	@Test
	public void testFilesystemAttributeFiles() throws Exception {
		String basepath = ((CNDatasource) filesystemDS).getAttributePath();
		Collection<Resolvable> result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), null);

		assertEquals("Check # of results", 6, result.size());
		for (Resolvable res : result) {
			File nameAttributeFile = new File(basepath,
					GenticsContentFactory.getStoragePath(
					ObjectTransformer.getString(res.get("obj_type"), "") + "." + ObjectTransformer.getString(res.get("obj_id"), ""), "name", 0, 1));
			File name2AttributeFile = new File(basepath,
					GenticsContentFactory.getStoragePath(
					ObjectTransformer.getString(res.get("obj_type"), "") + "." + ObjectTransformer.getString(res.get("obj_id"), ""), "name2", 0, 1));

			assertFalse("Attribute file " + nameAttributeFile.getAbsolutePath() + " must not exist", nameAttributeFile.exists());
			assertTrue("Attribute file " + name2AttributeFile.getAbsolutePath() + " must exist", name2AttributeFile.exists());
		}
	}

	/**
	 * Make an assertion about a streaming attribute
	 * @param res streaming resolvable
	 * @param attributeName name of the attribute
	 * @param values expected attribute values
	 * @throws Exception
	 */
	protected void assertAttributeStreams(StreamingResolvable res,
			String attributeName, Object... values) throws Exception {
		assertTrue("Attribute " + attributeName + " must be streamable", res.isStreamable(attributeName));
		assertEquals("Check number of available streams for " + attributeName, values.length, res.getNumStreams(attributeName));
		for (int i = 0; i < values.length; ++i) {
			InputStream in = res.getInputStream(attributeName, i);

			assertNotNull("InputStream must not be null", in);
			if (values[i] instanceof String) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				String value = reader.readLine();

				assertEquals("Check streamed value", values[i], value);
			} else if (values[i] instanceof byte[]) {
				assertStreamEquals("Check streamed value", new ByteArrayInputStream((byte[]) values[i]), in);
			} else {
				fail("Value is neither String nor byte[]");
			}
		}

		try {
			res.getInputStream(attributeName, values.length);
			fail("Getting an superfluoes input stream for " + attributeName + " should have failed");
		} catch (ArrayIndexOutOfBoundsException e) {// expected
		}
	}

	/**
	 * Test streaming of file system attribute, when not prefilled
	 * @throws Exception
	 */
	@Test
	public void testStreamFilesystemAttributeNoPrefill() throws Exception {
		String name = "Johannes";
		String reverseName = new StringBuffer(name).reverse().toString();
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Johannes'")), null);

		assertEquals("Check # of results", 1, result.size());
		for (Resolvable res : result) {
			assertTrue("Object must be a StreamingResolvable", res instanceof StreamingResolvable);
			StreamingResolvable sRes = (StreamingResolvable) res;

			assertAttributeStreams(sRes, FILESYSTEM_ATTRIBUTE, name);
			assertAttributeStreams(sRes, MULTISTRING_ATTRIBUTE, name, reverseName);
			assertAttributeStreams(sRes, SINGLEBINARY_ATTRIBUTE, name.getBytes("UTF-8"));
			assertAttributeStreams(sRes, MULTIBINARY_ATTRIBUTE, name.getBytes("UTF-8"), reverseName.getBytes("UTF-8"));
		}
	}

	/**
	 * Test streaming of file system attribute, when prefilled
	 * @throws Exception
	 */
	@Test
	public void testStreamFilesystemAttributeWithPrefill() throws Exception {
		String name = "Johannes";
		String reverseName = new StringBuffer(name).reverse().toString();
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Johannes'")), new String[] {
			NON_FILESYSTEM_ATTRIBUTE, FILESYSTEM_ATTRIBUTE, MULTISTRING_ATTRIBUTE, SINGLEBINARY_ATTRIBUTE, MULTIBINARY_ATTRIBUTE });

		assertEquals("Check # of results", 1, result.size());
		for (Resolvable res : result) {
			assertTrue("Object must be a StreamingResolvable", res instanceof StreamingResolvable);
			StreamingResolvable sRes = (StreamingResolvable) res;

			assertAttributeStreams(sRes, FILESYSTEM_ATTRIBUTE, name);
			assertAttributeStreams(sRes, MULTISTRING_ATTRIBUTE, name, reverseName);
			assertAttributeStreams(sRes, SINGLEBINARY_ATTRIBUTE, name.getBytes("UTF-8"));
			assertAttributeStreams(sRes, MULTIBINARY_ATTRIBUTE, name.getBytes("UTF-8"), reverseName.getBytes("UTF-8"));
		}
	}

	/**
	 * Test streaming of non-file system attribute
	 * @throws Exception
	 */
	@Test
	public void testStreamNonFilesystemAttribute() throws Exception {
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Johannes'")), null);

		assertEquals("Check # of results", 1, result.size());
		for (Resolvable res : result) {
			assertTrue("Object must be a StreamingResolvable", res instanceof StreamingResolvable);
			StreamingResolvable sRes = (StreamingResolvable) res;

			assertFalse("Attribute " + NON_FILESYSTEM_ATTRIBUTE + " must not be streamable", sRes.isStreamable(NON_FILESYSTEM_ATTRIBUTE));
			assertEquals("Check number of available streams for " + NON_FILESYSTEM_ATTRIBUTE, 0, sRes.getNumStreams(NON_FILESYSTEM_ATTRIBUTE));
			try {
				sRes.getInputStream(NON_FILESYSTEM_ATTRIBUTE, 0);
				fail("Getting the input stream for " + NON_FILESYSTEM_ATTRIBUTE + " should have failed");
			} catch (IOException e) {// expected
			}
		}
	}

	/**
	 * Make an assertion about existence and content of a value file
	 * @param file value file
	 * @param value expected value
	 * @throws IOException
	 */
	protected void assertFileContents(File file, Object value) throws IOException {
		assertTrue("Value file " + file.getAbsolutePath() + " must exist", file.exists());
		if (value instanceof String) {
			assertEquals("Check value file contents", value, FileUtil.file2String(file));
		} else if (value instanceof byte[]) {
			ByteArrayOutputStream fileData = new ByteArrayOutputStream();

			FileUtil.inputStreamToOutputStream(new FileInputStream(file), fileData);
			assertByteEquals("Check value file contents", (byte[]) value, fileData.toByteArray());
		} else {
			fail("Value was neither String nor byte[]");
		}
	}

	/**
	 * Make an assertion about the data of the given object
	 * @param wDs datasource instance
	 * @param object object
	 * @param name name of the attribute
	 * @param multivalue true if the attribute is multivalue
	 * @param values expected values
	 * @throws Exception
	 */
	protected void assertAttributeData(final CNWriteableDatasource wDs, Resolvable object, final String name,
			final boolean multivalue, final Object... values) throws Exception {
		DB.query(wDs.getHandle().getDBHandle(), "SELECT * FROM contentattribute WHERE contentid = ? AND name = ?", new Object[] { object.get("contentid"), name},
				new ResultProcessor() {
			public void process(ResultSet rs) throws SQLException {
				if (multivalue) {
					while (rs.next()) {
						int sortorder = rs.getInt("sortorder");

						try {
							assertFileContents(new File(wDs.getAttributePath(), rs.getString("value_text")), values[sortorder - 1]);
						} catch (IOException e) {
							fail("Checking the file failed with exception " + e.getLocalizedMessage());
						}
					}
				} else {
					if (rs.next()) {
						try {
							assertFileContents(new File(wDs.getAttributePath(), rs.getString("value_text")), values[0]);
						} catch (IOException e) {
							fail("Checking the file failed with exception " + e.getLocalizedMessage());
						}
					} else {
						fail("Did not find entry for attribute " + name + " in contentattribute");
					}
				}
			}

			public void takeOver(ResultProcessor p) {}
		});

		// clear the caches
		PortalConnectorHelper.clearCache(wDs);

		// read the object again
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("contentid", object.get("contentid"));
		object = wDs.create(data);

		if (multivalue) {
			Collection<?> storedValues = ObjectTransformer.getCollection(object.get(name), Collections.emptyList());

			assertEquals("Check # of resolved values", values.length, storedValues.size());
			Iterator<?> iterator = storedValues.iterator();

			for (int i = 0; i < values.length; ++i) {
				Object storedValue = iterator.next();

				if (values[i] instanceof String) {
					assertEquals("Check resolved value", values[i], storedValue);
				} else if (values[i] instanceof byte[]) {
					assertByteEquals("Check resolved value", (byte[]) values[i], ObjectTransformer.getBinary(storedValue, null));
				} else {
					fail("Value #" + i + " was neither String nor byte[]");
				}
			}
		} else {
			if (values[0] instanceof String) {
				assertEquals("Check resolved value", values[0], object.get(name));
			} else if (values[0] instanceof byte[]) {
				assertByteEquals("Check resolved value", (byte[]) values[0], ObjectTransformer.getBinary(object.get(name), null));
			} else {
				fail("Value was neither String nor byte[]");
			}
		}
	}

	/**
	 * Helper method to create an object with an attribute set
	 * @param name name of the attribute
	 * @param multivalue true if the attribute is multivalue
	 * @param values attribute values
	 * @return object
	 * @throws Exception
	 */
	protected Changeable createObject(String name, boolean multivalue,
			Object... values) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("obj_type", OBJECT_TYPE);
		if (multivalue) {
			data.put(name, values);
		} else {
			data.put(name, values[0]);
		}

		Changeable newObject = filesystemDS.create(data);

		filesystemDS.insert(Collections.singleton(newObject));
		return newObject;
	}

	/**
	 * Internal helper method for tests to insert filesystem attributes
	 * @param name name of the attribute
	 * @param multivalue true when the attribute is multivalue
	 * @param values values (should be either Strings or byte[])
	 * @throws Exception
	 */
	protected void internalTestInsertingFSAttribute(String name,
			boolean multivalue, Object... values) throws Exception {
		Changeable newObject = createObject(name, multivalue, values);

		assertAttributeData(filesystemDS, newObject, name, multivalue, values);
	}

	/**
	 * Internal helper method for tests to update filesystem attributes
	 * @param object object
	 * @param name name of the attribute
	 * @param multivalue true when the attribute is multivalue
	 * @param values new values (should be either Strings or byte[])
	 * @throws Exception
	 */
	protected void internalTestUpdatingFSAttribute(Changeable object, String name,
			boolean multivalue, Object... values) throws Exception {
		if (multivalue) {
			object.setProperty(name, values);
		} else {
			object.setProperty(name, values[0]);
		}
		filesystemDS.update(Collections.singleton(object));

		assertAttributeData(filesystemDS, object, name, multivalue, values);
	}

	/**
	 * Internal test for removing a filesystem attribute
	 * @param object object
	 * @param name name of the attribute
	 * @throws Exception
	 */
	protected void internalTestRemovingFSAttribute(Changeable object, String name) throws Exception {
		DBHandle handle = filesystemDS.getHandle().getDBHandle();
		final String basePath = filesystemDS.getAttributePath();

		final List<File> valueFiles = new Vector<File>();

		DB.query(handle, "SELECT value_text FROM contentattribute WHERE contentid = ? AND name = ?", new Object[] { object.get("contentid"), name },
				new ResultProcessor() {
			public void takeOver(ResultProcessor p) {}

			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					valueFiles.add(new File(basePath, rs.getString("value_text")));
				}
			}
		});

		for (File file : valueFiles) {
			assertTrue("Value file " + file.getAbsolutePath() + " must exist", file.exists());
		}

		object.setProperty(name, null);
		filesystemDS.update(Collections.singleton(object));

		DB.query(handle, "SELECT * FROM contentattribute WHERE contentid = ? AND name = ?", new Object[] { object.get("contentid"), name },
				new ResultProcessor() {
			public void takeOver(ResultProcessor p) {}

			public void process(ResultSet rs) throws SQLException {
				if (rs.next()) {
					fail("Unexpected record found in table contentattribute");
				}
			}
		});

		for (File file : valueFiles) {
			assertFalse("Value file " + file.getAbsolutePath() + " must not exist", file.exists());
		}
	}

	/**
	 * Internal test for removing an object with filesystem attributes
	 * @param object object
	 * @param name name of the attribute
	 * @throws Exception
	 */
	protected void internalTestRemovingObject(Changeable object, String name) throws Exception {
		DBHandle handle = filesystemDS.getHandle().getDBHandle();
		final String basePath = filesystemDS.getAttributePath();

		final List<File> valueFiles = new Vector<File>();

		DB.query(handle, "SELECT value_text FROM contentattribute WHERE contentid = ? AND name = ?", new Object[] { object.get("contentid"), name },
				new ResultProcessor() {
			public void takeOver(ResultProcessor p) {}

			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					valueFiles.add(new File(basePath, rs.getString("value_text")));
				}
			}
		});

		for (File file : valueFiles) {
			assertTrue("Value file " + file.getAbsolutePath() + " must exist", file.exists());
		}

		filesystemDS.delete(Collections.singleton(object));

		DB.query(handle, "SELECT * FROM contentattribute WHERE contentid = ? AND name = ?", new Object[] { object.get("contentid"), name },
				new ResultProcessor() {
			public void takeOver(ResultProcessor p) {}

			public void process(ResultSet rs) throws SQLException {
				if (rs.next()) {
					fail("Unexpected record found in table contentattribute");
				}
			}
		});

		for (File file : valueFiles) {
			assertFalse("Value file " + file.getAbsolutePath() + " must not exist", file.exists());
		}
	}

	/**
	 * Test inserting a singlevalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testInsertingSingleStringFSAttribute() throws Exception {
		internalTestInsertingFSAttribute(FILESYSTEM_ATTRIBUTE, false, "Beppo");
	}

	/**
	 * Test inserting a multivalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testInsertingMultiStringFSAttribute() throws Exception {
		internalTestInsertingFSAttribute(MULTISTRING_ATTRIBUTE, true, "one", "two", "three");
	}

	/**
	 * Test inserting a singlevalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testInsertingSingleBinaryFSAttribute() throws Exception {
		internalTestInsertingFSAttribute(SINGLEBINARY_ATTRIBUTE, false, new byte[] { 1, 2, 3});
	}

	/**
	 * Test inserting a multivalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testInsertingMultiBinaryFSAttribute() throws Exception {
		internalTestInsertingFSAttribute(MULTIBINARY_ATTRIBUTE, true, new byte[] { 6, 5, 4}, new byte[] { 1, 2, 3}, new byte[] { 3, 5, 7});
	}

	/**
	 * Test updating a singlevalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testUpdatingSingleStringFSAttribute() throws Exception {
		Changeable object = createObject(FILESYSTEM_ATTRIBUTE, false, "Beppo");

		internalTestUpdatingFSAttribute(object, FILESYSTEM_ATTRIBUTE, false, "Striezi");
	}

	/**
	 * Test whether updating an unchanged filesystem attribute does not change the file
	 * @throws Exception
	 */
	@Test
	public void testUpdatingUnchangedAttribute() throws Exception {
		String value = "Hansi";
		Changeable object = createObject(FILESYSTEM_ATTRIBUTE, false, value);
		File valueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(object.get("contentid"), ""), FILESYSTEM_ATTRIBUTE, 0, 1));

		assertTrue("Value file " + valueFile.getAbsolutePath() + " must exist", valueFile.exists());
		FilesystemAttributeValue newValue = new FilesystemAttributeValue();

		newValue.setData(value, StringUtils.md5(value).toLowerCase(), value.getBytes("UTF-8").length);
		object.setProperty(FILESYSTEM_ATTRIBUTE, newValue);
		filesystemDS.update(Collections.singleton(object));

		assertTrue("Value file " + valueFile.getAbsolutePath() + " must still exist", valueFile.exists());
	}

	/**
	 * Test updating a multivalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testUpdatingMultiStringFSAttribute() throws Exception {
		Changeable object = createObject(MULTISTRING_ATTRIBUTE, true, "one", "two", "three");

		internalTestUpdatingFSAttribute(object, MULTISTRING_ATTRIBUTE, true, "three", "four");
	}

	/**
	 * Test updating a singlevalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testUpdatingSingleBinaryFSAttribute() throws Exception {
		Changeable object = createObject(SINGLEBINARY_ATTRIBUTE, false, new byte[] { 1, 2, 3});

		internalTestUpdatingFSAttribute(object, SINGLEBINARY_ATTRIBUTE, false, new byte[] { 2, 3, 5, 7, 11, 13});
	}

	/**
	 * Test updating a multivalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testUpdatingMultiBinaryFSAttribute() throws Exception {
		Changeable object = createObject(MULTIBINARY_ATTRIBUTE, true, new byte[] { 6, 5, 4}, new byte[] { 1, 2, 3});

		internalTestUpdatingFSAttribute(object, MULTIBINARY_ATTRIBUTE, true, new byte[] { 1, 4, 9, 16}, new byte[] { 2, 3, 5, 7, 11, 13}, new byte[] { 42});
	}

	/**
	 * Test removing a singlevalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingSingleStringFSAttribute() throws Exception {
		Changeable object = createObject(FILESYSTEM_ATTRIBUTE, false, "Beppo");

		internalTestRemovingFSAttribute(object, FILESYSTEM_ATTRIBUTE);
	}

	/**
	 * Test removing a multivalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingMultiStringFSAttribute() throws Exception {
		Changeable object = createObject(MULTISTRING_ATTRIBUTE, true, "one", "two", "three");

		internalTestRemovingFSAttribute(object, MULTISTRING_ATTRIBUTE);
	}

	/**
	 * Test removing a singlevalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingSingleBinaryFSAttribute() throws Exception {
		Changeable object = createObject(SINGLEBINARY_ATTRIBUTE, false, new byte[] { 1, 2, 3});

		internalTestRemovingFSAttribute(object, SINGLEBINARY_ATTRIBUTE);
	}

	/**
	 * Test removing a multivalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingMultiBinaryFSAttribute() throws Exception {
		Changeable object = createObject(MULTIBINARY_ATTRIBUTE, true, new byte[] { 6, 5, 4}, new byte[] { 1, 2, 3});

		internalTestRemovingFSAttribute(object, MULTIBINARY_ATTRIBUTE);
	}

	/**
	 * Test removing an object with a singlevalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingObjectWithSingleStringFSAttribute() throws Exception {
		Changeable object = createObject(FILESYSTEM_ATTRIBUTE, false, "Beppo");

		internalTestRemovingObject(object, FILESYSTEM_ATTRIBUTE);
	}

	/**
	 * Test removing an object with a multivalue string filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingObjectWithMultiStringFSAttribute() throws Exception {
		Changeable object = createObject(MULTISTRING_ATTRIBUTE, true, "one", "two", "three");

		internalTestRemovingObject(object, MULTISTRING_ATTRIBUTE);
	}

	/**
	 * Test removing an object with a singlevalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingObjectWithSingleBinaryFSAttribute() throws Exception {
		Changeable object = createObject(SINGLEBINARY_ATTRIBUTE, false, new byte[] { 1, 2, 3});

		internalTestRemovingObject(object, SINGLEBINARY_ATTRIBUTE);
	}

	/**
	 * Test removing an object with a multivalue binary filesystem attribute
	 * @throws Exception
	 */
	@Test
	public void testRemovingObjectWithMultiBinaryFSAttribute() throws Exception {
		Changeable object = createObject(MULTIBINARY_ATTRIBUTE, true, new byte[] { 6, 5, 4}, new byte[] { 1, 2, 3});

		internalTestRemovingObject(object, MULTIBINARY_ATTRIBUTE);
	}

	/**
	 * Test that old value files are removed
	 * @throws Exception
	 */
	@Test
	public void testFileRemoval() throws Exception {
		System.setProperty(FileRemover.INTERVAL_PARAM, "100");
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);

		assertEquals("Check # of results", 1, result.size());
		Changeable obj = (Changeable) result.iterator().next();

		// check that the expected value file exists
		File oldValueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(obj.get("obj_type"), "") + "." + ObjectTransformer.getString(obj.get("obj_id"), ""),
				FILESYSTEM_ATTRIBUTE, 0, 1));

		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must exist", oldValueFile.exists());
		// now open a filestream
		InputStream in = ((StreamingResolvable) obj).getInputStream(FILESYSTEM_ATTRIBUTE, 0);

		// change the object
		obj.setProperty(FILESYSTEM_ATTRIBUTE, "Clemens modified");
		filesystemDS.update(Collections.singleton(obj));

		// Check whether the already opened stream can still be read (either the file was not yet deleted, or the file was deleted, but the stream is still valid)
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		FileUtil.pooledBufferInToOut(in, out);

		// check the read data
		String data = new String(out.toByteArray(), "UTF-8");

		assertEquals("Check old read data", "Clemens", data);

		// close the filestream
		in.close();

		// wait 1 second
		Thread.sleep(1000);

		assertFalse("Old value file " + oldValueFile.getAbsolutePath() + " must have been removed now", oldValueFile.exists());
	}

	/**
	 * Test whether the file removal is done on commit (not earlier)
	 * @throws Exception
	 */
	@Test
	public void testFileRemovalOnCommit() throws Exception {
		System.setProperty(FileRemover.INTERVAL_PARAM, "100");
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);

		assertEquals("Check # of results", 1, result.size());
		Changeable obj = (Changeable) result.iterator().next();

		// check that the expected value file exists
		File oldValueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(obj.get("obj_type"), "") + "." + ObjectTransformer.getString(obj.get("obj_id"), ""),
				FILESYSTEM_ATTRIBUTE, 0, 1));

		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must exist", oldValueFile.exists());

		// change the object in a transaction
		DB.startTransaction(filesystemDS.getHandle().getDBHandle());
		obj.setProperty(FILESYSTEM_ATTRIBUTE, "Clemens modified");
		filesystemDS.update(Collections.singleton(obj));

		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must still exist before commit", oldValueFile.exists());

		// commit the transaction
		DB.commitTransaction(filesystemDS.getHandle().getDBHandle());

		// old value file must be deleted now
		assertFalse("Old value file " + oldValueFile.getAbsolutePath() + " must be removed after commit", oldValueFile.exists());
	}

	/**
	 * Test whether the new value file is removed and the old value file persists on rollback
	 * @throws Exception
	 */
	@Test
	public void testFileRemovalOnRollback() throws Exception {
		System.setProperty(FileRemover.INTERVAL_PARAM, "100");
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);

		assertEquals("Check # of results", 1, result.size());
		Changeable obj = (Changeable) result.iterator().next();

		File oldValueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(obj.get("obj_type"), "") + "." + ObjectTransformer.getString(obj.get("obj_id"), ""),

				FILESYSTEM_ATTRIBUTE, 0, 1));
		File newValueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(obj.get("obj_type"), "") + "." + ObjectTransformer.getString(obj.get("obj_id"), ""),
				FILESYSTEM_ATTRIBUTE, 0, 2));

		// create a second object that has it's attribute set in a transaction
		Collection<Resolvable> result2 = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Taylor'")), null);

		assertEquals("Check # of results", 1, result2.size());
		Changeable obj2 = (Changeable) result2.iterator().next();

		DB.startTransaction(filesystemDS.getHandle().getDBHandle(), true);
		obj2.setProperty(FILESYSTEM_ATTRIBUTE, "Taylor modified");
		filesystemDS.update(Collections.singleton(obj2));
		DB.commitTransaction(filesystemDS.getHandle().getDBHandle(), false);

		File newValueFile2 = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(obj2.get("obj_type"), "") + "." + ObjectTransformer.getString(obj2.get("obj_id"), ""),
				FILESYSTEM_ATTRIBUTE, 0, 2));

		// check that the expected value file exists
		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must exist", oldValueFile.exists());
		assertFalse("New value file " + newValueFile.getAbsolutePath() + " must not exist", newValueFile.exists());

		// change the object in a transaction
		DB.startTransaction(filesystemDS.getHandle().getDBHandle(), true);
		obj.setProperty(FILESYSTEM_ATTRIBUTE, "Clemens modified");
		filesystemDS.update(Collections.singleton(obj));

		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must still exist before rollback", oldValueFile.exists());
		assertTrue("New value file " + newValueFile.getAbsolutePath() + " must still exist before rollback", newValueFile.exists());
		assertTrue("Last transaction's file should still exist", newValueFile2.exists());

		// roll the transaction back
		DB.rollbackTransaction(filesystemDS.getHandle().getDBHandle(), false);

		// old value file must still exist
		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must still exist after rollback", oldValueFile.exists());
		// new value file must be removed
		assertFalse("New value file " + newValueFile.getAbsolutePath() + " must be removed after rollback", newValueFile.exists());
		assertTrue("Last transaction's file should not be rollbacked", newValueFile2.exists());

		// We now create a new empty transaction and commit it, to see if files that should be
		// deleted on a previous' rollbacked transaction's commit accidentally disappear
		DB.startTransaction(filesystemDS.getHandle().getDBHandle(), true);
		DB.commitTransaction(filesystemDS.getHandle().getDBHandle(), false);
		assertTrue("Old value file " + oldValueFile.getAbsolutePath() + " must not be removed in unrelated transaction", oldValueFile.exists());
	}

	/**
	 * Make an assertion about equality of the datasource objects
	 * @param source source datasource
	 * @param target target datasource
	 * @throws Exception
	 */
	protected void assertDatasourcesEqual(Datasource source,
			CNWriteableDatasource target) throws Exception {
		Map<String, Object> dataMap = new HashMap<String, Object>();

		// read all objects from the source ds
		Collection<Resolvable> sourceObjects = source.getResult(source.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), null);

		for (Resolvable sourceObject : sourceObjects) {
			// check whether the object is present in the target ds
			dataMap.clear();
			dataMap.put("contentid", sourceObject.get("contentid"));
			Changeable targetObject = target.create(dataMap);

			assertNotNull("Check that target object " + sourceObject + " exists", targetObject);
			assertAttributeData(target, targetObject, FILESYSTEM_ATTRIBUTE, false, sourceObject.get(FILESYSTEM_ATTRIBUTE));
			assertAttributeData(target, targetObject, MULTISTRING_ATTRIBUTE, true,
					ObjectTransformer.getCollection(sourceObject.get(MULTISTRING_ATTRIBUTE), Collections.emptyList()).toArray());
			assertAttributeData(target, targetObject, SINGLEBINARY_ATTRIBUTE, false, sourceObject.get(SINGLEBINARY_ATTRIBUTE));
			assertAttributeData(target, targetObject, MULTIBINARY_ATTRIBUTE, true,
					ObjectTransformer.getCollection(sourceObject.get(MULTIBINARY_ATTRIBUTE), Collections.emptyList()).toArray());
		}
	}

	/**
	 * Assert equality of two byte arrays
	 * @param message message
	 * @param expected expected byte array
	 * @param actual actual byte array
	 */
	protected static void assertByteEquals(String message, byte[] expected, byte[] actual) {
		if (expected == null) {
			if (actual == null) {
				return;
			} else {
				fail(message + " expected: null but was byte[]");
			}
		} else if (actual == null) {
			fail(message + " expected byte[] but was null");
		}
		assertEquals(message + " length", expected.length, actual.length);
		for (int i = 0; i < expected.length; ++i) {
			assertEquals(message + " byte " + i, expected[i], actual[i]);
		}
	}

	/**
	 * Assert equality of data read from two InputStreams
	 * @param message message
	 * @param expected InputStream delivering the expected data
	 * @param actual InputStream delivering the actual data
	 * @throws IOException
	 */
	public static void assertStreamEquals(String message, InputStream expected,
			InputStream actual) throws IOException {
		int expectedByte = -1;
		int actualByte = -1;
		int byteCounter = 0;

		do {
			expectedByte = expected.read();
			actualByte = actual.read();
			assertEquals(message + ": byte # " + byteCounter, expectedByte, actualByte);
			byteCounter++;
		} while (expectedByte != -1 || actualByte != -1);
	}

	/**
	 * Test performing a full sync with attributes that are stored in the filesystem
	 * @throws Exception
	 */
	@Test
	public void testFullSync() throws Exception {
		// do the sync
		CRSync sync = new CRSync(filesystemDS, synctargetDS, null, false, true, true, true, true, true, 100, null);

		sync.doSync();

		assertDatasourcesEqual(filesystemDS, synctargetDS);
	}

	/**
	 * Test performing a sync that updates filesystem attributes
	 * @throws Exception
	 */
	@Test
	public void testSyncAfterUpdate() throws Exception {
		// do an initial sync to have identical contents
		CRSync sync = new CRSync(filesystemDS, synctargetDS, null, false, true, true, true, true, true, 100, null);

		sync.doSync();

		// update an object
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Petro'")), null);

		assertEquals("Check # of results", 1, result.size());
		Changeable object = (Changeable) result.iterator().next();

		object.setProperty(FILESYSTEM_ATTRIBUTE, object.get(FILESYSTEM_ATTRIBUTE) + " - modified");
		@SuppressWarnings("unchecked")
		List<Object> data = new Vector<Object>(ObjectTransformer.getCollection(object.get(MULTISTRING_ATTRIBUTE), Collections.emptyList()));

		Collections.reverse(data);
		data.add("new Value");
		object.setProperty(MULTISTRING_ATTRIBUTE, data);
		filesystemDS.store(Collections.singleton(object));

		PortalConnectorHelper.clearCache(filesystemDS);
		PortalConnectorHelper.clearCache(synctargetDS);

		// sync again
		sync.doSync();

		assertDatasourcesEqual(filesystemDS, synctargetDS);

		// check that the unmodified filesystem attributes were not written again
		File valueFile = new File(synctargetDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(object.get("contentid"), null), SINGLEBINARY_ATTRIBUTE, 0, 1));

		assertTrue("Unmodified filesystem attribute must not be rewritten", valueFile.exists());
	}

	/**
	 * Test accessing the value file of an attribute, that has been changed before
	 * @throws Exception
	 */
	@Test
	public void testAccessToModifiedValueFile() throws Exception {
		// get an object and have the filesystem attribute prefilled
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), new String[] { FILESYSTEM_ATTRIBUTE });

		assertEquals("Check # of results", 1, result.size());
		Resolvable resolvable = result.iterator().next();

		// get the object again
		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		Changeable changeable = (Changeable) result.iterator().next();

		// change the changeable object
		changeable.setProperty(FILESYSTEM_ATTRIBUTE, "Clemens modified");
		filesystemDS.update(Collections.singleton(changeable));

		// try to access the changed attribute
		assertEquals("Check changed attribute", "Clemens modified", changeable.get(FILESYSTEM_ATTRIBUTE));
		// try to access the original cached attribute (which will give the new content now)
		assertEquals("Check original cached attribute", "Clemens modified", resolvable.get(FILESYSTEM_ATTRIBUTE));
	}

	/**
	 * Test moving a single attribute from the filesystem to the database and back again
	 * @throws Exception
	 */
	@Test
	public void testMigrationSingle() throws Exception {
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), new String[] { FILESYSTEM_ATTRIBUTE });

		assertEquals("Check # of results", 1, result.size());
		Resolvable resolvable = result.iterator().next();

		String path = GenticsContentFactory.getStoragePath(ObjectTransformer.getString(resolvable.get("contentid"), ""), FILESYSTEM_ATTRIBUTE, 0, 1);
		File file = new File(filesystemDS.getAttributePath(), path);

		assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());

		migrateAttribute(FILESYSTEM_ATTRIBUTE, filesystemDS, false);
		assertFalse("File should not exist at this point: " + file.getAbsolutePath(), file.exists());

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();
		assertEquals("Check attribute value", "Clemens", resolvable.get(FILESYSTEM_ATTRIBUTE));

		// remove object and create object new
		filesystemDS.delete(result);
		String newContentId = saveData(filesystemDS, "Clemens");

		path = GenticsContentFactory.getStoragePath(newContentId, FILESYSTEM_ATTRIBUTE, 0, 1);
		file = new File(filesystemDS.getAttributePath(), path);

		// DB -> Filesystem
		migrateAttribute(FILESYSTEM_ATTRIBUTE, filesystemDS, true);
		assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		assertEquals("Check attribute value", "Clemens", resolvable.get(FILESYSTEM_ATTRIBUTE));
	}

	/**
	 * Test moving a multivalue attribute from the filesystem to the database and back again
	 * @throws Exception
	 */
	@Test
	public void testMigrationMulti() throws Exception {
		// Filesystem -> DB
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), new String[] { MULTISTRING_ATTRIBUTE });

		assertEquals("Check # of results", 1, result.size());
		Resolvable resolvable = result.iterator().next();

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(ObjectTransformer.getString(resolvable.get("contentid"), ""), MULTISTRING_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());
		}

		migrateAttribute(MULTISTRING_ATTRIBUTE, filesystemDS, false);

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(ObjectTransformer.getString(resolvable.get("contentid"), ""), MULTISTRING_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertFalse("File should not exist at this point: " + file.getAbsolutePath(), file.exists());
		}

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		Collection actualValues = ObjectTransformer.getCollection(resolvable.get(MULTISTRING_ATTRIBUTE), Collections.emptyList());
		Collection expectedValues = Arrays.asList("Clemens", "snemelC");

		assertEquals("Collections should be equal lengths", actualValues.size(), expectedValues.size());

		Iterator iterator = expectedValues.iterator();

		for (Object actualValue : actualValues) {
			Object expectedValue = iterator.next();

			assertEquals(actualValue, expectedValue);
		}

		// remove object and create object new
		filesystemDS.delete(result);
		String newContentId = saveData(filesystemDS, "Clemens");

		// DB -> Filesystem
		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(newContentId, MULTISTRING_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertFalse("File should not exist at this point: " + file.getAbsolutePath(), file.exists());
		}

		migrateAttribute(MULTISTRING_ATTRIBUTE, filesystemDS, true);

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(newContentId, MULTISTRING_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());
		}

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		actualValues = ObjectTransformer.getCollection(resolvable.get(MULTISTRING_ATTRIBUTE), Collections.emptyList());
		expectedValues = Arrays.asList("Clemens", "snemelC");
		assertEquals("Collections should be equal lengths", actualValues.size(), expectedValues.size());

		iterator = expectedValues.iterator();
		for (Object actualValue : actualValues) {
			Object expectedValue = iterator.next();

			assertEquals(actualValue, expectedValue);
		}

	}

	/**
	 * Test moving a single binary from the filesystem to the database and back again
	 * @throws Exception
	 */
	@Test
	public void testMigrationSingleBinary() throws Exception {
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), new String[] { SINGLEBINARY_ATTRIBUTE });

		assertEquals("Check # of results", 1, result.size());
		Resolvable resolvable = result.iterator().next();

		String path = GenticsContentFactory.getStoragePath(ObjectTransformer.getString(resolvable.get("contentid"), ""), SINGLEBINARY_ATTRIBUTE, 0, 1);
		File file = new File(filesystemDS.getAttributePath(), path);

		assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());

		migrateAttribute(SINGLEBINARY_ATTRIBUTE, filesystemDS, false);
		assertFalse("File should not exist at this point: " + file.getAbsolutePath(), file.exists());

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		byte[] b = (byte[]) resolvable.get(SINGLEBINARY_ATTRIBUTE);

		assertByteEquals("Check attribute value", "Clemens".getBytes("UTF-8"), b);

		// remove object and create object new
		filesystemDS.delete(result);
		String newContentId = saveData(filesystemDS, "Clemens");

		path = GenticsContentFactory.getStoragePath(newContentId, FILESYSTEM_ATTRIBUTE, 0, 1);
		file = new File(filesystemDS.getAttributePath(), path);

		migrateAttribute(SINGLEBINARY_ATTRIBUTE, filesystemDS, true);
		assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		b = (byte[]) resolvable.get(SINGLEBINARY_ATTRIBUTE);
		assertByteEquals("Check attribute value", "Clemens".getBytes("UTF-8"), b);
	}

	/**
	 * Test moving a multivalue binary from the filesystem to the database and back again
	 * @throws Exception
	 */
	@Test
	public void testMigrationMultiBinary() throws Exception {
		Collection<Resolvable> result = filesystemDS.getResult(
				filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), new String[] { MULTIBINARY_ATTRIBUTE });

		assertEquals("Check # of results", 1, result.size());
		Resolvable resolvable = result.iterator().next();

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(ObjectTransformer.getString(resolvable.get("contentid"), ""), MULTIBINARY_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());
		}

		migrateAttribute(MULTIBINARY_ATTRIBUTE, filesystemDS, false);

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(ObjectTransformer.getString(resolvable.get("contentid"), ""), MULTIBINARY_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertFalse("File should not exist at this point: " + file.getAbsolutePath(), file.exists());
		}

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		Collection actualValues = ObjectTransformer.getCollection(resolvable.get(MULTIBINARY_ATTRIBUTE), Collections.emptyList());
		Collection expectedValues = Arrays.asList("Clemens".getBytes("UTF-8"), "snemelC".getBytes("UTF-8"));

		assertEquals("Collections should be equal lengths", actualValues.size(), expectedValues.size());

		Iterator iterator = expectedValues.iterator();

		for (Object actualValue : actualValues) {
			Object expectedValue = iterator.next();

			assertByteEquals("Values should be equal", (byte[]) actualValue, (byte[]) expectedValue);
		}

		// remove object and create object new
		filesystemDS.delete(result);
		String newContentId = saveData(filesystemDS, "Clemens");

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(newContentId, MULTIBINARY_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertFalse("File should not exist at this point: " + file.getAbsolutePath(), file.exists());
		}

		migrateAttribute(MULTIBINARY_ATTRIBUTE, filesystemDS, true);

		for (int i = 1; i <= 2; i++) {
			String path = GenticsContentFactory.getStoragePath(newContentId, MULTIBINARY_ATTRIBUTE, i, 1);
			File file = new File(filesystemDS.getAttributePath(), path);

			assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());
		}

		result = filesystemDS.getResult(filesystemDS.createDatasourceFilter(ExpressionParser.getInstance().parse("object.name == 'Clemens'")), null);
		assertEquals("Check # of results", 1, result.size());
		resolvable = result.iterator().next();

		actualValues = ObjectTransformer.getCollection(resolvable.get(MULTIBINARY_ATTRIBUTE), Collections.emptyList());
		expectedValues = Arrays.asList("Clemens".getBytes("UTF-8"), "snemelC".getBytes("UTF-8"));
		assertEquals("Collections should be equal lengths", actualValues.size(), expectedValues.size());

		iterator = expectedValues.iterator();
		for (Object actualValue : actualValues) {
			Object expectedValue = iterator.next();

			assertByteEquals("Values should be equal", (byte[]) actualValue, (byte[]) expectedValue);
		}
	}

	/**
	 * Test file recreation, when the file is missing and the data is saved unchanged
	 * @throws Exception
	 */
	@Test
	public void testFileRecreation() throws Exception {
		String value = "Hansi";
		Changeable object = createObject(FILESYSTEM_ATTRIBUTE, false, value);
		File valueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(object.get("contentid"), ""), FILESYSTEM_ATTRIBUTE, 0, 1));

		assertTrue("Value file " + valueFile.getAbsolutePath() + " must exist", valueFile.exists());

		// we remove the file now
		assertTrue("Value file " + valueFile.getAbsolutePath() + " could not be deleted", valueFile.delete());

		FilesystemAttributeValue newValue = new FilesystemAttributeValue();

		newValue.setData(value, StringUtils.md5(value).toLowerCase(), value.getBytes("UTF-8").length);
		object.setProperty(FILESYSTEM_ATTRIBUTE, newValue);
		filesystemDS.update(Collections.singleton(object));

		assertTrue("Value file " + valueFile.getAbsolutePath() + " must be recreated", valueFile.exists());
	}

	/**
	 * Test file recreation, when the file is missing and the data is changed
	 * @throws Exception
	 */
	@Test
	public void testFileRecreationChangedValue() throws Exception {
		String value = "Beppo";
		Changeable object = createObject(FILESYSTEM_ATTRIBUTE, false, value);

		File valueFile = new File(filesystemDS.getAttributePath(),
				GenticsContentFactory.getStoragePath(ObjectTransformer.getString(object.get("contentid"), ""), FILESYSTEM_ATTRIBUTE, 0, 1));

		assertTrue("Value file " + valueFile.getAbsolutePath() + " must exist", valueFile.exists());

		// we remove the file now
		assertTrue("Value file " + valueFile.getAbsolutePath() + " could not be deleted", valueFile.delete());

		internalTestUpdatingFSAttribute(object, FILESYSTEM_ATTRIBUTE, false, "Striezi");

		// the old value file must still not exist
		assertFalse("Value file " + valueFile.getAbsolutePath() + " must not exist", valueFile.exists());
	}

	/**
	 * Move an attribute in a given datasource between the database and fileystem
	 * @param attribute name of the attribute to be migrated
	 * @param datasource datasource of the attribute
	 * @param filesystem flag if the attribute should be written to the filesystem (true) or in the databse (false)
	 * @throws Exception
	 */
	protected void migrateAttribute(String attribute, CNWriteableDatasource datasource, boolean filesystem) throws Exception {

		Collection<ObjectAttributeBean> objectAttributeBeans = ObjectManagementManager.loadAttributeTypes(datasource.getHandle().getDBHandle());

		for (ObjectAttributeBean bean : objectAttributeBeans) {
			if (bean.getName().equals(attribute)) {
				bean.setFilesystem(filesystem);
				ObjectManagementManager.saveAttributeType(datasource, bean, false);
			}
		}

		PortalConnectorHelper.clearCache(datasource);
	}

	/**
	 * Create the db structure
	 * @param wds writeable datasource
	 * @throws ObjectManagementException
	 */
	protected void createStructure(CNWriteableDatasource wds) throws ObjectManagementException {
		// create the object type
		ObjectTypeBean type = new ObjectTypeBean();

		type.setName("data");
		type.setType(OBJECT_TYPE);
		ObjectManagementManager.saveObjectType(wds, type);

		// create a singlevalue attribute
		ObjectAttributeBean nameAttr = new ObjectAttributeBean();

		nameAttr.setObjecttype(OBJECT_TYPE);
		nameAttr.setAttributetype(GenticsContentAttribute.ATTR_TYPE_TEXT);
		nameAttr.setName(NON_FILESYSTEM_ATTRIBUTE);
		ObjectManagementManager.saveAttributeType(wds, nameAttr, false);

		// create another singlevalue attribute, that will write into the filesystem for the "filesystem" datasource
		ObjectAttributeBean nameAttr2 = new ObjectAttributeBean();

		nameAttr2.setObjecttype(OBJECT_TYPE);
		nameAttr2.setAttributetype(GenticsContentAttribute.ATTR_TYPE_TEXT_LONG);
		nameAttr2.setName(FILESYSTEM_ATTRIBUTE);
		if (filesystemDS.equals(wds)) {
			nameAttr2.setFilesystem(true);
		}
		ObjectManagementManager.saveAttributeType(wds, nameAttr2, false);

		// create a multivalue string attribute (writing into the filesystem)
		ObjectAttributeBean multiString = new ObjectAttributeBean();

		multiString.setObjecttype(OBJECT_TYPE);
		multiString.setAttributetype(GenticsContentAttribute.ATTR_TYPE_TEXT_LONG);
		multiString.setName(MULTISTRING_ATTRIBUTE);
		multiString.setMultivalue(true);
		if (filesystemDS.equals(wds)) {
			multiString.setFilesystem(true);
		}
		ObjectManagementManager.saveAttributeType(wds, multiString, false);

		// create a singlevalue binary attribute (writing into the filesystem)
		ObjectAttributeBean singleBinary = new ObjectAttributeBean();

		singleBinary.setObjecttype(OBJECT_TYPE);
		singleBinary.setAttributetype(GenticsContentAttribute.ATTR_TYPE_BLOB);
		singleBinary.setName(SINGLEBINARY_ATTRIBUTE);
		if (filesystemDS.equals(wds)) {
			singleBinary.setFilesystem(true);
		}
		ObjectManagementManager.saveAttributeType(wds, singleBinary, false);

		// create a multivalue binary attribute (writing into the filesystem)
		ObjectAttributeBean multiBinary = new ObjectAttributeBean();

		multiBinary.setObjecttype(OBJECT_TYPE);
		multiBinary.setAttributetype(GenticsContentAttribute.ATTR_TYPE_BLOB);
		multiBinary.setName(MULTIBINARY_ATTRIBUTE);
		multiBinary.setMultivalue(true);
		if (filesystemDS.equals(wds)) {
			multiBinary.setFilesystem(true);
		}
		ObjectManagementManager.saveAttributeType(wds, multiBinary, false);
	}

	/**
	 * Save some data
	 * @param wds writeable datasource
	 * @param name name
	 * @return contentid
	 * @throws Exception
	 */
	protected String saveData(WriteableDatasource wds, String name) throws Exception {
		Map<String, Object> objectParams = new HashMap<String, Object>();
		String reverseName = new StringBuffer(name).reverse().toString();

		objectParams.put("obj_type", OBJECT_TYPE);
		objectParams.put(NON_FILESYSTEM_ATTRIBUTE, name);
		objectParams.put(FILESYSTEM_ATTRIBUTE, name);
		objectParams.put(MULTISTRING_ATTRIBUTE, new String[] { name, reverseName});
		objectParams.put(SINGLEBINARY_ATTRIBUTE, name.getBytes("UTF-8"));
		objectParams.put(MULTIBINARY_ATTRIBUTE, new byte[][] { name.getBytes("UTF-8"), reverseName.getBytes("UTF-8")});
		Changeable object = wds.create(objectParams);

		wds.store(Collections.singleton(object));
		return ObjectTransformer.getString(object.get("contentid"), null);
	}
}
