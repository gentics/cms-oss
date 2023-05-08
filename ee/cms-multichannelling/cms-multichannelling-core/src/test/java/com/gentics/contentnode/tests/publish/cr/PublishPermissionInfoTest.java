package com.gentics.contentnode.tests.publish.cr;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.TestedInheritance;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;

/**
 * Test cases for publishing permission info
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR })
@RunWith(value = Parameterized.class)
public class PublishPermissionInfoTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public final static String ALL_PERMS = new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_CREATE,
			PermHandler.PERM_PAGE_PUBLISH, PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_PAGE_DELETE, PermHandler.PERM_FOLDER_CREATE,
			PermHandler.PERM_FOLDER_DELETE, PermHandler.PERM_FOLDER_UPDATE).toString();

	private static Node crNode;
	private static Node crChannel;
	private static Node mccrNode;
	private static Node mccrChannel;
	private static UserGroup testGroup;

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public TestedInheritance folderInheritance;

	@Parameter(2)
	public TestedInheritance inheritance;

	@Parameter(3)
	public boolean mccr;

	@Parameters(name = "{index}: type {0}, {2}, folder {1}, mccr {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType type : MCTestedType.values()) {
			for (TestedInheritance folderInheritance : TestedInheritance.values()) {
				for (TestedInheritance inheritance : TestedInheritance.values()) {
					if (folderInheritance == TestedInheritance.local && inheritance != TestedInheritance.local) {
						continue;
					}
					for (boolean mccr : Arrays.asList(true, false)) {
						data.add(new Object[] { type, folderInheritance, inheritance, mccr });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setup() throws Exception {
		try (Trx trx = new Trx()) {
			crNode = ContentNodeTestDataUtils.createNode("node", "node", PublishTarget.CONTENTREPOSITORY);
			ContentRepository editableCR = trx.getTransaction().getObject(crNode.getContentRepository(), true);
			editableCR.setPermissionInformation(true);
			editableCR.save();

			crChannel = ContentNodeTestDataUtils.createChannel(crNode, "channel", "channel", "/", PublishTarget.CONTENTREPOSITORY);
			editableCR = trx.getTransaction().getObject(crChannel.getContentRepository(), true);
			editableCR.setPermissionInformation(true);
			editableCR.save();

			mccrNode = ContentNodeTestDataUtils.createNode("mccrnode", "mccrnode", PublishTarget.CONTENTREPOSITORY, true, false);
			editableCR = trx.getTransaction().getObject(mccrNode.getContentRepository(), true);
			editableCR.setPermissionInformation(true);
			editableCR.save();

			mccrChannel = ContentNodeTestDataUtils.createChannel(mccrNode, "mccrchannel", "mccrchannel", "/", PublishTarget.CONTENTREPOSITORY);

			UserGroup nodeGroup = trx.getTransaction().getObject(UserGroup.class, 2);
			testGroup = Creator.createUsergroup("Testgroup", "", nodeGroup);

			trx.success();
		}
	}

	/**
	 * Test perms for published objects
	 * @throws Exception
	 */
	@Test
	public void testPublishedPerms() throws Exception {
		Node node = mccr ? mccrNode : crNode;
		Node channel = mccr ? mccrChannel : crChannel;

		// create test data
		LocalizableNodeObject<?>[] folders = null;
		LocalizableNodeObject<?>[] tested = null;

		try (Trx trx = new Trx()) {
			folders = folderInheritance.create(MCTestedType.folder, node, channel);
			PermHandler.setPermissions(Folder.TYPE_FOLDER, folders[0] != null ? folders[0].getId() : folders[1].getId(), Arrays.asList(testGroup), ALL_PERMS);

			tested = inheritance.createPublished(type, (Folder)(folders[0] != null ? folders[0] : folders[1]), channel);
			if (type == MCTestedType.folder) {
				PermHandler.setPermissions(Folder.TYPE_FOLDER, tested[0] != null ? tested[0].getId() : tested[1].getId(), Arrays.asList(testGroup), ALL_PERMS);
			}

			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert permgroups in CRs
		if (tested[0] != null) {
			checkPermissionAttributes(node, tested[0]);
		}
		if (tested[1] != null) {
			checkPermissionAttributes(channel, tested[1]);
		} else if (tested[0] != null) {
			checkPermissionAttributes(channel, tested[0]);
		}
	}

	/**
	 * Check whether the object is published into the CR of the node and has the permission attributes set
	 * @param node node
	 * @param object object
	 * @throws Exception
	 */
	protected void checkPermissionAttributes(Node node, LocalizableNodeObject<?> object) throws Exception {
		if (object instanceof Folder) {
			checkPermissionAttributes(node, (Folder)object);
		} else if (object instanceof Page) {
			checkPermissionAttributes(node, (Page)object);
		} else if (object instanceof File) {
			checkPermissionAttributes(node, (File)object);
		}
	}

	/**
	 * Check permission attributes for the folder
	 * @param node node
	 * @param folder folder
	 * @throws Exception
	 */
	protected void checkPermissionAttributes(Node node, Folder folder) throws Exception {
		try (Trx trx = new Trx()) {
			Datasource ds = null;
			if (node.getContentMap().isMultichannelling()) {
				WritableMCCRDatasource mccrDs = node.getContentMap().getMCCRDatasource();
				mccrDs.setChannel(node.getId());
				ds = mccrDs;
			} else {
				ds = node.getContentMap().getDatasource();
			}
			Resolvable resolvableFolder = PortalConnectorFactory.getContentObject(Folder.TYPE_FOLDER + "." + folder.getId(), ds);
			assertNotNull(folder + " must be published into CR of " + node, resolvableFolder);
			for (String attr : ContentMap.FOLDER_PERMISSION_ATTRIBUTES) {
				Collection<?> perms = ObjectTransformer.getCollection(resolvableFolder.get(attr), null);
				assertNotNull(attr + " must be set for " + folder, perms);
				assertTrue(attr + " must contain testgroup", perms.contains(testGroup.getId()));
			}
			trx.success();
		}
	}

	/**
	 * Check permission attributes for the page
	 * @param node node
	 * @param page page
	 * @throws Exception
	 */
	protected void checkPermissionAttributes(Node node, Page page) throws Exception {
		try (Trx trx = new Trx()) {
			Datasource ds = null;
			if (node.getContentMap().isMultichannelling()) {
				WritableMCCRDatasource mccrDs = node.getContentMap().getMCCRDatasource();
				mccrDs.setChannel(node.getId());
				ds = mccrDs;
			} else {
				ds = node.getContentMap().getDatasource();
			}
			Resolvable resolvablePage = PortalConnectorFactory.getContentObject(Page.TYPE_PAGE + "." + page.getId(), ds);
			assertNotNull(page + " must be published into CR of " + node, resolvablePage);
			for (String attr : ContentMap.PAGE_PERMISSION_ATTRIBUTES) {
				Collection<?> perms = ObjectTransformer.getCollection(resolvablePage.get(attr), null);
				assertNotNull(attr + " must be set for " + page, perms);
				assertTrue(attr + " must contain testgroup", perms.contains(testGroup.getId()));
			}
			trx.success();
		}
	}

	/**
	 * Check permission attributes for the file
	 * @param node node
	 * @param file file
	 * @throws Exception
	 */
	protected void checkPermissionAttributes(Node node, File file) throws Exception {
		try (Trx trx = new Trx()) {
			Datasource ds = null;
			if (node.getContentMap().isMultichannelling()) {
				WritableMCCRDatasource mccrDs = node.getContentMap().getMCCRDatasource();
				mccrDs.setChannel(node.getId());
				ds = mccrDs;
			} else {
				ds = node.getContentMap().getDatasource();
			}
			Resolvable resolvableFile = PortalConnectorFactory.getContentObject(File.TYPE_FILE + "." + file.getId(), ds);
			assertNotNull(file + " must be published into CR of " + node, resolvableFile);
			for (String attr : ContentMap.PAGE_PERMISSION_ATTRIBUTES) {
				Collection<?> perms = ObjectTransformer.getCollection(resolvableFile.get(attr), null);
				assertNotNull(attr + " must be set for " + file, perms);
				assertTrue(attr + " must contain testgroup", perms.contains(testGroup.getId()));
			}
			trx.success();
		}
	}
}
