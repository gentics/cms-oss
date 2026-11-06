package com.gentics.contentnode.tests.multichannelling;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test creating objects in a disinherited folder.
 * The filename uniqueness check must correctly handle the channel restrictions and not
 * try to load the folder in the channel where the folder is not inherited.
 * SUP-5092, SUP-5241 
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class CreateObjectInDisinheritedFolderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test channel
	 */
	private static Node channel;

	/**
	 * Test folder
	 */
	private Folder folder;

	@Parameter(0)
	public boolean featureNiceUrlsActivated;

	@Parameters(name = "{index}: featureNiceUrlsActivated {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean featureNiceUrlsActivated : Arrays.asList(true, false)) {
			data.add(new Object[] { featureNiceUrlsActivated });
		}

		return data;
	}

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			t.commit();
		}
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
	}

	/**
	 * Create test folder
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			NodePreferences nodePreferences = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();
			nodePreferences.setFeature(Feature.NICE_URLS, featureNiceUrlsActivated);
		});

		node = ContentNodeTestDataUtils.update(node, n -> {
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		});

		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));

		folder = ContentNodeTestDataUtils.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("/");
		});

		Trx.operate(() -> {
			folder = TransactionManager.getCurrentTransaction().getObject(folder, true);
			folder.changeMultichannellingRestrictions(true, new HashSet<Node>(Arrays.asList(channel)), true);
			folder.save();
		});
	}

	/**
	 * Delete the test folder (and everything in it)
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		Trx.operate(() -> {
			folder.delete(true);
			channel.delete(true);
		});

		folder = null;
	}

	/**
	 * Test create a page in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void testCreateDisinheritedFolderPage() throws NodeException {
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template"));

		ContentNodeTestDataUtils.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(folder.getId());
			p.setName("page name");
			p.setFilename("filename");
		});
	}

	/**
	 * Test create a page in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void testCreateDisinheritedFolderFile() throws NodeException {
		ContentNodeTestDataUtils.create(File.class, o -> {
			o.setFolderId(folder.getId());
			o.setName("name");
			o.setFileStream(new ByteArrayInputStream("content".getBytes()));
		});
	}

	/**
	 * Test create a folder in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void testCreateDisinheritedFolderFolder() throws NodeException {
		ContentNodeTestDataUtils.create(Folder.class, o -> {
			o.setMotherId(folder.getId());
			o.setName("name");
		});
	}

	/**
	 * Test create a template in a disinherited folder
	 * @throws NodeException
	 */
	@Test
	public void testCreateDisinheritedFolderTemplate() throws NodeException {
		ContentNodeTestDataUtils.create(Template.class, o -> {
			o.setFolderId(folder.getId());
			o.setName("name");
			o.setSource("");
		});
	}
}
