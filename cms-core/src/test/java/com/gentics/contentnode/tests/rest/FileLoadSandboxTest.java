package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

@RunWith(value = Parameterized.class)
public class FileLoadSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test Data
	 */
	public static Map<String, TestFile> testData = new HashMap<String, TestFile>();

	/**
	 * Tested contentType
	 */
	protected String contentType;

	/**
	 * Node
	 */
	protected Node node;

	/**
	 * Tested file
	 */
	protected File file;

	@Parameters(name = "{index}: contentType: {0}")
	public static Collection<Object[]> data() throws Exception {
		testData.put("text/plain", new TestFile("test.txt", "text/plain", "This is the test contents"));
		testData.put("text/plain;charset=utf8", new TestFile("test2.txt", "text/plain;charset=utf8", "This is the utf8 test contents"));
		testData.put("image/jpeg", new TestFile("blume.jpg", "image/jpeg", "blume.jpg"));
		testData.put("image/png", new TestFile("blume.png", "image/png", "blume.png"));

		Collection<Object[]> data = new Vector<Object[]>();

		for (String contentType : testData.keySet()) {
			data.add(new Object[] { contentType });
		}
		return data;
	}

	/**
	 * Create a test instance with given contentType
	 * @param contentType contentType
	 */
	public FileLoadSandboxTest(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("contentnode.maxfilesize", Integer.toString(5 * 1024 * 1024));
		testContext.getContext().startTransaction();
		TestFile testFileData = testData.get(contentType);
		assertNotNull("Test data were null", testFileData);
		node = ContentNodeTestDataUtils.createNode("Test Node", "testnode", "/", null, false, false);
		Transaction t = TransactionManager.getCurrentTransaction();
		file = t.createObject(File.class);
		file.setFileStream(testFileData.getInputStream());
		file.setFolderId(node.getFolder().getId());
		file.setName(testFileData.name);
		file.save();
		t.commit(false);

		file.setFiletype(testFileData.contentType);
		file.save();
		t.commit(false);
	}

	/**
	 * Test whether the correct Content-Type is returned when loading the content
	 * @throws Exception
	 */
	@Test
	public void testLoadContentType() throws Exception {
		FileResource fileResource = getFileResource();
		Response response = fileResource.loadContent(ObjectTransformer.getString(file.getId(), null), null);
		String responseContentType = ObjectTransformer.getString(response.getMetadata().get("Content-Type").get(0), null);
		assertEquals("Check Content-Type", contentType, responseContentType);
	}

	/**
	 * Get a file resource
	 * @return file resource
	 * @throws NodeException
	 */
	protected FileResource getFileResource() throws NodeException {
		FileResourceImpl fileResource = new FileResourceImpl();
		fileResource.setTransaction(TransactionManager.getCurrentTransaction());
		return fileResource;
	}

	/**
	 * Test whether a File can be retrieved from the DB using all available File classes and interfaces
	 * there are currently 3 relevant Classe File, ImageFile and ContentFile
	 */
	@Test
	public void testRevtrieveFileByClass() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Class<? extends File>> clazzes = new ArrayList<Class<? extends File>>();
		clazzes.add(ContentFile.class);
		clazzes.add(File.class);
		clazzes.add(ImageFile.class);
		for (Class<? extends File> clazz : clazzes) {
			Object o = t.getObject(clazz, (Integer) file.getId());
			assertNotNull("File fetched with Class " + clazz.getName() + "should be not null", o);
		}	
	}

	/**
	 * TestFile Data
	 */
	protected static class TestFile {
		/**
		 * File name
		 */
		protected String name;

		/**
		 * ContentType
		 */
		protected String contentType;

		/**
		 * The data resource 
		 */
		private String dataResource;

		/**
		 * Create an instance
		 * @param name name
		 * @param contentType content type
		 * @param dataResource the data resource, a filename for binary data or the text-string for text-data
		 */
		public TestFile(String name, String contentType, String dataResource) {
			this.name = name;
			this.contentType = contentType;
			this.dataResource = dataResource;
		}
		/**
		 * Gets an Input Stream from the data resource depending on the content type:
		 * if the content type is an image the method will try to load the image with the GenericTestUtils
		 * if the content type is text the method will convert the dataResource to an InputStream
		 * 
		 * @return the InputStream containing the dataResource
		 * @throws Exception
		 */
		public InputStream getInputStream() throws Exception {
			if (this.contentType != null && this.contentType.startsWith("image")) {
				return GenericTestUtils.getPictureResource(dataResource);
			}
			String textData = dataResource != null ? dataResource : "";
			return new ByteArrayInputStream(textData.getBytes("UTF8"));
		}
	}
}
