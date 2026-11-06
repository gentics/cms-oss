package com.gentics.contentnode.tests.dirting;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for dirting overviews after objects were moved
 */
@RunWith(value = Parameterized.class)
public class DirtMovedObjectOverviewTest {
	public final static String TAG_NAME = "overview";

	public final static String PART_NAME = "overview";

	@Rule
	public DBTestContext testContext = new DBTestContext();

	private boolean multichannelling;

	private ObjectType listed;

	private boolean recursive;

	private ObjectType moved;

	private Node node;

	private Folder sourceFolder;

	private Folder targetFolder;

	private int overviewConstructId;

	private Template template;

	@Parameters(name = "{index}: multichannelling {0}, listed {1}, recursive {2}, moved {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean multichannelling : Arrays.asList(true, false)) {
			for (ObjectType listed : ObjectType.values()) {
				for (boolean recursive : Arrays.asList(true, false)) {
					for (ObjectType moved : ObjectType.values()) {
						data.add(new Object[] { multichannelling, listed, recursive, moved });
					}
				}
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param multichannelling true for multichannelling
	 * @param listed type of listed objects
	 * @param recursive true for recursive
	 * @param moved type of moved objects
	 */
	public DirtMovedObjectOverviewTest(boolean multichannelling, ObjectType listed, boolean recursive, ObjectType moved) {
		this.multichannelling = multichannelling;
		this.listed = listed;
		this.recursive = recursive;
		this.moved = moved;
	}

	@Before
	public void setup() throws Exception {
		try (Trx trx = new Trx()) {
			NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), multichannelling);

			node = ContentNodeTestDataUtils.createNode();
			sourceFolder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Source Folder");
			targetFolder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Target Folder");
			overviewConstructId = ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, TAG_NAME, PART_NAME);
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node " + TAG_NAME + ">", "Template",
					ContentNodeTestDataUtils.createTemplateTag(overviewConstructId, TAG_NAME, true, false));
			trx.success();
		}
	}

	@Test
	public void testMove() throws Exception {
		Page sourceOverview = null;
		Page targetOverview = null;
		Folder folder = null;
		Page page = null;
		File file = null;
		File image = null;

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			// create source overview
			sourceOverview = ContentNodeTestDataUtils.createPage(sourceFolder, template, "Source Overview");
			sourceOverview = t.getObject(Page.class, sourceOverview.getId(), true);
			ContentNodeTestDataUtils.fillOverview(sourceOverview.getContentTag(TAG_NAME), PART_NAME, "<ds folder.name>", listed.getClazz(),
					Overview.SELECTIONTYPE_PARENT, -1, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, recursive, null);
			sourceOverview.save();
			sourceOverview.publish();
			sourceOverview = t.getObject(Page.class, sourceOverview.getId());

			// create target overview
			targetOverview = ContentNodeTestDataUtils.createPage(targetFolder, template, "Target Overview");
			targetOverview = t.getObject(Page.class, targetOverview.getId(), true);
			ContentNodeTestDataUtils.fillOverview(targetOverview.getContentTag(TAG_NAME), PART_NAME, "<ds folder.name>", listed.getClazz(),
					Overview.SELECTIONTYPE_PARENT, -1, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, recursive, null);
			targetOverview.save();
			targetOverview.publish();
			targetOverview = t.getObject(Page.class, targetOverview.getId());

			// create some additional objects
			folder = ContentNodeTestDataUtils.createFolder(sourceFolder, "Folder");
			page = ContentNodeTestDataUtils.createTemplateAndPage(sourceFolder, "Page");
			t.getObject(Page.class, page.getId(), true).publish();
			file = ContentNodeTestDataUtils.createFile(sourceFolder, "testfile.txt", "Test File Contents".getBytes());
			InputStream inputStream = GenericTestUtils.getPictureResource("blume.jpg");
			image = ContentNodeTestDataUtils.createImage(sourceFolder, "blume.jpg", IOUtils.toByteArray(inputStream));

			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			OpResult moveResult = null;
			switch (moved) {
			case PAGE:
				moveResult = t.getObject(Page.class, page.getId(), true).move(targetFolder, 0, false);
				break;
			case FOLDER:
				moveResult = t.getObject(Folder.class, folder.getId(), true).move(targetFolder, 0);
				break;
			case FILE:
				moveResult = t.getObject(File.class, file.getId(), true).move(targetFolder, 0);
				break;
			case IMAGE:
				moveResult = t.getObject(ImageFile.class, image.getId(), true).move(targetFolder, 0);
				break;
			}
			assertEquals("Check result", OpResult.Status.OK, moveResult.getStatus());
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();

			if (listed == moved || (moved == ObjectType.FOLDER && recursive)) {
				if (moved == ObjectType.PAGE) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, page, sourceOverview, targetOverview);
				} else {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, sourceOverview, targetOverview);
				}
			} else {
				if (moved == ObjectType.PAGE) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, page);
				} else {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node);
				}
			}
		}
	}

	/**
	 * Enumeration of possible object type combinations
	 */
	public enum ObjectType {
		PAGE("page", Page.TYPE_PAGE, Page.class), IMAGE("image", ContentFile.TYPE_IMAGE, ImageFile.class), FILE("file", ContentFile.TYPE_FILE, File.class), FOLDER(
				"folder", Folder.TYPE_FOLDER, Folder.class);

		private String shortName;

		private int objType;

		private Class<? extends NodeObject> clazz;

		private ObjectType(String shortName, int objType, Class<? extends NodeObject> clazz) {
			this.shortName = shortName;
			this.objType = objType;
			this.clazz = clazz;
		}

		public String getShortName() {
			return shortName;
		}

		/**
		 * Get the object type
		 * 
		 * @return object type
		 */
		public int getObjType() {
			return objType;
		}

		/**
		 * Get the object class
		 * @return object class
		 */
		public Class<? extends NodeObject> getClazz() {
			return clazz;
		}
	}
}
