package com.gentics.contentnode.tests.dirting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for disinheriting objects and dirting dependent objects
 */
@GCNFeature(set = {Feature.MULTICHANNELLING})
@RunWith(value = Parameterized.class)
public class McOverviewDirtingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Object type for parameterized run
	 */
	@Parameter(0)
	public DisinheritableObjTypes objectType;

	/**
	 * Whether the folder where the inherited objects are in is localized
	 */
	@Parameter(1)
	public boolean overviewFolderLocalized;

	/**
	 * Master node
	 */
	protected static Node master;

	/**
	 * Channel
	 */
	protected static Node channel;;

	/**
	 * Foreign Node
	 */
	protected static Node foreign;

	/**
	 * Folder that contains inherited objects
	 */
	protected Folder overviewFolder;

	/**
	 * Page with overview tag
	 */
	Page overviewPage = null;



	@Parameterized.Parameters(name = "{index} objectType: {0} overviewFolderLocalized: {1}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> result = new ArrayList<>();

		for (DisinheritableObjTypes type : DisinheritableObjTypes.values()) {
			result.add(new Object[] { type, true });
			result.add(new Object[] { type, false });
		}

		return result;
	}

	/**
	 * Setup test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpStaticTestData() throws Exception {
		Trx.operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECt id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});
		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
			DBUtils.executeUpdate("DELETE FROM publishqueue", null);
		});

		try (Trx trx = new Trx()) {
			master = ContentNodeTestDataUtils.createNode("Node", "Node", PublishTarget.NONE);
			channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "Channel", "/");
			foreign = ContentNodeTestDataUtils.createNode("Foreign", "Foreign", PublishTarget.NONE);

			trx.success();
		}
	}

	@Before
	public void setup() throws NodeException {
		// clean data
		Trx.operate(() -> {
			Folder root = master.getFolder();
			for (Folder f : root.getChildFolders()) {
				f.delete(true);
			}
			for (Page p : root.getPages()) {
				p.delete(true);
			}
			for (File f : root.getFilesAndImages()) {
				f.delete(true);
			}
		});
		Trx.operate(() -> {
			overviewFolder = ContentNodeTestDataUtils.createFolder(master.getFolder(), "Overview folder");

			if (overviewFolderLocalized) {
				ContentNodeTestDataUtils.localize(overviewFolder, channel);
			}
		});
	}

	public enum DisinheritableObjTypes {
		FOLDER(10002), PAGE(10007), FILE(10008), IMAGE(10011);

		private int type;

		private DisinheritableObjTypes(int type) {
			this.type = type;
		}

		public int getValue() {
			return type;
		}
	}

	/**
	 * Test exluding an object from a channel that is used in an overview in a page
	 * and then including this object again. The overview must be dirted after each action.
	 * After disinheriting the object, the overview page must be dirted.
	 * @throws Exception
	 */
	@Test
	public void testInheritDisinherit() throws Exception {
		Disinheritable<?> dependency = null;

		try (Trx trx = new Trx()) {
			dependency = (Disinheritable<?>)ContentNodeTestDataUtils.createNodeObject(objectType.type, overviewFolder, "Test dependency object", master, true);
			dependency = trx.getTransaction().getObject(dependency, true);

			overviewPage = createOverviewPage(channel, trx.getTransaction().getClass(objectType.type));

			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}
	
		try (Trx trx = new Trx()) {
			// Disinherit dependency in channel
			Set<Node> disinheritedNodes = new HashSet<>();
			disinheritedNodes.add(channel);
			dependency.changeMultichannellingRestrictions(true, disinheritedNodes, false);
			dependency.save();

			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{ channel.getId() });
		}


		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			// Inherit dependency in channel again
			dependency.changeMultichannellingRestrictions(false, new HashSet<>(), false);
			dependency.save();

			trx.success();
		}

		try (Trx trx = new Trx() ) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{ channel.getId() });
		}
	}

	/**
	 * Test exluding an object from a channel that is used in an overview in a page
	 * and then including this object again. The overview must be dirted after each action.
	 * After disinheriting the object, the overview page must be dirted.
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteMasterObject() throws Exception {
		Disinheritable<?> dependency = null;

		try (Trx trx = new Trx()) {
			overviewPage = createOverviewPage(channel, trx.getTransaction().getClass(objectType.type));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			dependency = (Disinheritable<?>)ContentNodeTestDataUtils.createNodeObject(objectType.type, overviewFolder, "Test dependency object", master, true);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{ channel.getId() });
		}

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			// Delete object
			trx.getTransaction().getObject(dependency, true).delete();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[]{ channel.getId() });
		}
	}

	/**
	 * Test dirting an overview page in another node
	 * @throws NodeException
	 */
	@Test
	public void testOverviewInForeignNode() throws Exception {
		Trx.operate(t -> {
			overviewPage = createOverviewPage(foreign, t.getClass(objectType.type));
		});

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			ContentNodeTestDataUtils.createNodeObject(objectType.type, overviewFolder, "Test dependency object", master, true);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			ContentNodeTestUtils.assertInPublishqueue(Page.TYPE_PAGE, overviewPage.getId(), Action.DEPENDENCY, new int[] { foreign.getId() });
		}
	}

	/**
	 * Create the overview page
	 * @param node node of the overview page
	 * @param clazz class of overview objects
	 * @return Page
	 * @throws NodeException
	 */
	protected Page createOverviewPage(Node node, Class<? extends NodeObject> clazz) throws NodeException {
		Page overviewPage = null;

		try (Trx trx = new Trx()) {
			Template template = ContentNodeTestDataUtils.createTemplate(node.getMaster().getFolder(), "Template");
			template = trx.getTransaction().getObject(template, true);

			// Create Overview tag in template
			int overviewConstructId = ContentNodeTestDataUtils.createConstruct(node.getMaster(), OverviewPartType.class, "OverviewConstruct", "ov");
			Construct editableConstruct = trx.getTransaction().getObject(Construct.class, overviewConstructId, true);
			editableConstruct.getParts().get(0).getDefaultValue().setInfo(1);
			editableConstruct.save();

			TemplateTag overviewTemplateTag = ContentNodeTestDataUtils.createTemplateTag(overviewConstructId, "ov", true, false);
			template.getTemplateTags().put("ov", overviewTemplateTag);
			template.setSource("<node ov>");
			template.save();

			overviewPage = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "testDisinheritObject", node);
			overviewPage = trx.getTransaction().getObject(overviewPage, true);
			ContentTag overviewContentTag = overviewPage.getContentTag("ov");
	
			ContentNodeTestDataUtils.fillOverview(overviewContentTag, "ov", "<node object.name>", clazz,
					Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false, Arrays.asList(overviewFolder));

			overviewPage.save();
			overviewPage.publish();

			trx.success();
		}

		return overviewPage;
	}
}
