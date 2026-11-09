package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = {Feature.MULTICHANNELLING})
public class FolderMCSandboxTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Creates a folder, tries loading and saving different settings of
	 * multichannelling exclusion and disinheriting.
	 *
	 * @throws NodeException
	 */
	@Test
	public void folderMcExcludeTest() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final Class<com.gentics.contentnode.object.Folder> folderClass = com.gentics.contentnode.object.Folder.class;
		List<ContentLanguage> noLanguage = Collections.emptyList();
		Node master = Creator.createNode("blub", "blub", "/", "/", noLanguage);
		Node channel = Creator.createNode("blub2", "blub2", "/", "/", noLanguage, master);

		// Create a folder and check the default settings
		com.gentics.contentnode.object.Folder f = Creator.createFolder(master.getFolder(), "testfolder", "/");
		assertEquals("Exclusion state must be false by default", false, f.isExcluded());
		assertTrue("Disinherit list must be empty by default", f.getDisinheritedChannels().isEmpty());

		// Set a channel disinheritment
		f = t.getObject(folderClass, f.getId(), true);
		f.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(new Node[]{channel})), false);

		f = t.getObject(folderClass, f.getId());
		assertFalse("folder must not be excluded", f.isExcluded());
		assertFalse("folder must disinherit something", f.getDisinheritedChannels().isEmpty());
		assertEquals("channel node must be disinherited", channel, f.getDisinheritedChannels().iterator().next());

		// Set multichannelling exclusion (removes disinheriting)
		f = t.getObject(folderClass, f.getId(), true);
		f.changeMultichannellingRestrictions(true, Collections.<Node> emptySet(), false);
		f = t.getObject(folderClass, f.getId());
		assertTrue("folder must be excluded", f.isExcluded());
		assertTrue("folder must disinherit nothing", f.getDisinheritedChannels().isEmpty());
	}
}
