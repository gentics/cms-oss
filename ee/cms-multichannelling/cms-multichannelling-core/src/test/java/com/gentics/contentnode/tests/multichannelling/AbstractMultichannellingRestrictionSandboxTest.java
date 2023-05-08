package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * This class sets up binary tree of Nodes of a channel structure and provides
 * methods to allow for the creation of simple tests in such an environment. The
 * name of a node describes the position of the node (m = master, mlr = right
 * child of left child of master, etc.)
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class AbstractMultichannellingRestrictionSandboxTest {

	protected static Node m;
	protected static Node ml;
	protected static Node mr;
	protected static Node mll;
	protected static Node mlr;
	protected static Node mrl;
	protected static Node mrr;
	protected static Node mlll;
	protected static Node mllr;
	protected static Node mlrl;
	protected static Node mlrr;
	protected static Template tpl;
	protected static Folder rootFolder;
	protected static Folder someFolder;
	protected final String name;
	protected Disinheritable<?> masterObject;
	private Node[] allNodes = {m, ml, mr, mll, mlr, mrl, mrr, mlll, mllr, mlrl, mlrr};
	DisinheritableObjTypes objType;
	protected Folder testFolder;

	/**
	 * Creates a new instance.
	 * @param objType the type of contentobject tested by this class
	 * @param useRootFolder if true, the test contentobjects will be created in the root folder, otherwise they'll be created in a non-root folder
	 */
	protected AbstractMultichannellingRestrictionSandboxTest(DisinheritableObjTypes objType, boolean useRootFolder) {
		this.objType = objType;
		if (useRootFolder) {
			testFolder = rootFolder;
		} else {
			testFolder = someFolder;
		}
		name = String.valueOf(System.identityHashCode(this));
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Sets up a channel tree and a basic template for pages.
	 *
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setup() throws NodeException {
		m = Creator.createNode("m", "m", "/", "/", null);
		ml = Creator.createNode("ml", "ml", "/", "/", null, m);
		mr = Creator.createNode("mr", "mr", "/", "/", null, m);
		mll = Creator.createNode("mll", "mll", "/", "/", null, ml);
		mlr = Creator.createNode("mlr", "mlr", "/", "/", null, ml);
		mrl = Creator.createNode("mrl", "mrl", "/", "/", null, mr);
		mrr = Creator.createNode("mrr", "mrr", "/", "/", null, mr);
		mlll = Creator.createNode("mlll", "mlll", "/", "/", null, mll);
		mlrl = Creator.createNode("mlrl", "mlrl", "/", "/", null, mlr);
		mllr = Creator.createNode("mllr", "mllr", "/", "/", null, mll);
		mlrr = Creator.createNode("mlrr", "mlrr", "/", "/", null, mlr);
		rootFolder = m.getFolder();
		tpl = Creator.createTemplate("Template", "Hissss", rootFolder);
		someFolder = Creator.createFolder(rootFolder, "folder", "/");
	}

	public enum DisinheritableObjTypes {
		FOLDER(Folder.class), PAGE(Page.class), FILE(File.class), IMAGE(ImageFile.class);

		private Class<? extends NodeObject> clazz;

		private DisinheritableObjTypes(Class<? extends NodeObject> clazz) {
			this.clazz = clazz;
		}

		public Class<? extends NodeObject> getClazz() {
			return clazz;
		}
	}

	/**
	 * We commit after each test method.
	 * @throws TransactionException
	 */
	@After
	public void after() throws TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		t.commit(false);
	}

	/**
	 * Creates the object defined by the parameter in the chosen node and
	 * remembers it.
	 *
	 * @param node
	 *            the channel where to create the object
	 * @throws NodeException
	 */
	public void createObject(Node node) throws NodeException {
		masterObject = internalCreate(node);
		assertEquals("Couldn't create object with name", name, masterObject.getName());
	}

	/**
	 * Reuses the name of the object in a different channel and assert
	 * availability of the name.
	 *
	 * @param node
	 *            the channel where to create the object
	 * @throws NodeException
	 */
	public void reuseName(Node node) throws NodeException {
		reuseName(node, false);
	}

	/**
	 * Reuses the name of the object in a different channel and assert
	 * availability of the name.
	 *
	 * @param node
	 *            the channel where to create the object
	 * @param expectCollision
	 *            true iff a name collision should occur
	 * @throws NodeException
	 */
	public void reuseName(Node node, boolean expectCollision) throws NodeException {
		try {
			Disinheritable<?> obj = internalCreate(node);
			if (!expectCollision) {
				assertEquals("Couldn't reuse name", name, obj.getName());
			}
			if (expectCollision && name.equals(obj.getName())) {
				fail("Name collision expected for " + name + " in Channel " + node);
			}
		} catch (Exception e) {
			if (!expectCollision) {
				fail("Should be able to create " + name + " without collision in " + node + ", but this happened: " + e);
			} else {
				System.out.println("swallowed " + e);
			}
		}
	}

	/**
	 * Creates the content object identified by the parameter in the specified
	 * channel.
	 *
	 * @param node
	 *            channel in which the object should be created
	 * @return the newly created content object
	 * @throws NodeException
	 */
	private Disinheritable<?> internalCreate(Node node) throws NodeException {
		Disinheritable<?> result;
		switch (objType) {
		case FILE:
			try {
				result = Creator.createFile(testFolder, name, new byte[] { 1 }, node);
			} catch (Exception e) {
				throw new NodeException(e);
			}
			break;
		case IMAGE:
			try {
				result = Creator.createImage(testFolder, name, node);
			} catch (Exception e) {
				throw new NodeException(e);
			}
			break;
		case FOLDER:
			result = Creator.createFolder(testFolder, name, "/", node);
			break;
		case PAGE:
			result = Creator.createPage(name, testFolder, tpl, null, node);
			break;
		default:
			throw new IllegalArgumentException();
		}
		return (Disinheritable<?>) TransactionManager.getCurrentTransaction().getObject(result.getObjectInfo().getObjectClass(), result.getId());
	}

	/**
	 * Localizes the current object in the specified channel.
	 *
	 * @param node
	 *            the channel to create the localization in
	 * @throws NodeException
	 */
	public void localizeObject(Node node) throws NodeException {
		LocalizableNodeObject<?> copy = (LocalizableNodeObject<?>) masterObject.copy();
		copy.setChannelInfo(node.getId(), masterObject.getChannelSetId());
		copy.save();
	}

	/**
	 * Disinherits the current object in the specified channels.
	 *
	 * @param node
	 * @throws NodeException
	 */
	public void disinheritObject(Node... node) throws NodeException {
		masterObject.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(node)), false);
	}

	/**
	 * Excludes the current object from multichannelling.
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException
	 */
	public void excludeObject() throws InsufficientPrivilegesException, NodeException {
		masterObject.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
	}

	/**
	 * Reincludes the current object into multichannelling.
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException
	 */
	public void reincludeObject() throws InsufficientPrivilegesException, NodeException {
		masterObject.changeMultichannellingRestrictions(false, Collections.<Node> emptySet(), false);
	}

	/**
	 * Checks the existence of the object in the specified channels and
	 * non-existence in non-specified channels.
	 *
	 * @param invert
	 *            iff true, checks for existence in non-specified channels and
	 *            non-existence in specified channels instead.
	 * @param targetNodes
	 *            the channel nodes specified
	 * @throws NodeException
	 */
	public void assertExistsObject(boolean invert, Node ... targetNodes) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		HashSet<Node> targetList = new HashSet<>(Arrays.asList(targetNodes));
		for (Node testNode : allNodes) {
			t.setChannelId(testNode.getId());
			try {
				String channelName = testNode.getFolder().getName();
				boolean exists = t.getObject(objType.getClazz(), masterObject.getId()) != null;
				if (targetList.contains(testNode) ^ invert) {
					assertTrue(objType + " »" + name + "« should exist in channel " + channelName, exists);
				} else {
					assertFalse(objType + " »" + name + "« should not exist in channel " + channelName, exists);
				}
			} finally {
				t.resetChannel();
			}
		}
	}

	/**
	 * @return Creates a channel tree segment for the localization in the given
	 *         channel node.
	 * @param channel
	 *            the channel where to look in
	 * @throws TransactionException
	 * @throws NodeException
	 */
	protected ChannelTreeSegment getSegment(Node channel) throws TransactionException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		t.setChannelId(channel.getId());
		NodeObject karl;
		try {
			karl = t.getObject(objType.getClazz(), masterObject.getId());
		} finally {
			t.resetChannel();
		}
		ChannelTreeSegment seg = new ChannelTreeSegment((Disinheritable<?>) karl, false);
		return seg;
	}

	/**
	 * Assert that the object is listed in the folder
	 * @throws NodeException
	 */
	public void assertListedInFolder(Node... channels) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		for (Node n: channels) {
			t.setChannelId(n.getId());
			try {
				testFolder = t.getObject(Folder.class, testFolder.getId());
				switch(objType) {
				case PAGE:
					assertTrue("Page must be listed in folder", testFolder.getPages().contains(masterObject));
					break;
				case FILE:
					assertTrue("File must be listed in folder", testFolder.getFiles().contains(masterObject));
					break;
				case IMAGE:
					assertTrue("Image must be listed in folder", testFolder.getImages().contains(masterObject));
					break;
				case FOLDER:
					assertTrue("Folder must be listed in folder", testFolder.getChildFolders().contains(masterObject));
					break;
				}
			} finally {
				t.resetChannel();
			}
		}
	}
}