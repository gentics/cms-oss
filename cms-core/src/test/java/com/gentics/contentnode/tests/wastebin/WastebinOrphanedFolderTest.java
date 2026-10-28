package com.gentics.contentnode.tests.wastebin;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateAndPage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.scheduler.CoreInternalSchedulerTask;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for deleting a folder, which has subfolders in the wastebin, while the wastebin feature is deactivated
 * and for purging folders from the wastebin, which have no existing mother
 */
@GCNFeature(set = { Feature.WASTEBIN })
public class WastebinOrphanedFolderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private Node node;

	private Folder mother;

	private Folder child;

	private Page page;

	private File file;

	/**
	 * Create a folder with a subfolder (containing a page and a file), put the subfolder into the wastebin and delete the folder while the wastebin feature is deactivated
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		node = supply(() -> createNode());
		mother = supply(() -> createFolder(node.getFolder(), "Mother"));
		child = supply(() -> createFolder(mother, "Child"));
		page = supply(() -> createTemplateAndPage(child, "Page"));
		file = supply(() -> createFile(child, "file.txt", "File contents".getBytes()));

		// put the child into the wastebin (in the past, so that it is old enough to be purged)
		try (Trx trx = new Trx()) {
			trx.at(getPastTimestamp());
			trx.getTransaction().getObject(child).delete();
			trx.success();
		}

		// delete the mother without wastebin
		try (FeatureClosure wastebin = new FeatureClosure(Feature.WASTEBIN, false)) {
			operate(t -> t.getObject(mother).delete());
		}
	}

	/**
	 * Test that deleting the mother also removes the child, which was in the wastebin, together with its contents
	 * @throws NodeException
	 */
	@Test
	public void testDeleteMother() throws NodeException {
		operate(() -> {
			assertThat(getIds("folder", mother.getId())).as("Records of deleted mother").isEmpty();
			assertThat(getIds("folder", child.getId())).as("Records of child in wastebin").isEmpty();
			assertThat(getIds("page", page.getId())).as("Records of page in wastebin").isEmpty();
			assertThat(getIds("contentfile", file.getId())).as("Records of file in wastebin").isEmpty();
		});
	}

	/**
	 * Test that purging the wastebin succeeds and removes the child, which was in the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testPurgeWastebin() throws NodeException {
		setMaxAge();

		// Reset the current lang context to mimic the truly scheduled env (not possible by default as per ContentNodeTestContext)
		ContentNodeHelper.setLanguageId(-1);
		CoreInternalSchedulerTask.purgewastebin.execute(new ArrayList<>());

		operate(() -> {
			assertThat(getIds("folder", child.getId())).as("Records of child in wastebin").isEmpty();
		});
	}

	/**
	 * Test that purging the wastebin removes a folder (and its subfolder) from the wastebin, which has no existing mother (inconsistent data)
	 * @throws NodeException
	 */
	@Test
	public void testPurgeOrphanedFolder() throws NodeException {
		Folder deletedMother = supply(() -> createFolder(node.getFolder(), "Deleted Mother"));
		Folder orphan = supply(() -> createFolder(deletedMother, "Orphan"));
		Folder orphanChild = supply(() -> createFolder(orphan, "Orphan Child"));

		// put the orphan (together with its child) into the wastebin (in the past, so that it is old enough to be purged)
		try (Trx trx = new Trx()) {
			trx.at(getPastTimestamp());
			trx.getTransaction().getObject(orphan).delete();
			trx.success();
		}

		// remove the record of the mother, so that the orphan references a non existing mother
		operate(t -> {
			DBUtils.update("DELETE FROM folder WHERE id = ?", deletedMother.getId());
			t.dirtObjectCache(Folder.class, deletedMother.getId());
			t.dirtObjectCache(Folder.class, orphan.getId());
		});

		setMaxAge();

		// Reset the current lang context to mimic the truly scheduled env (not possible by default as per ContentNodeTestContext)
		ContentNodeHelper.setLanguageId(-1);
		List<String> out = new ArrayList<>();
		boolean success = CoreInternalSchedulerTask.purgewastebin.execute(out);
		assertThat(success).as("Purge success, output:%n%s", String.join("\n", out)).isTrue();

		operate(() -> {
			assertThat(getIds("folder", orphan.getId())).as("Records of orphan in wastebin").isEmpty();
			assertThat(getIds("folder", orphanChild.getId())).as("Records of orphan child in wastebin").isEmpty();
		});
	}

	/**
	 * Set the maximum wastebin age for the node to 1 second
	 * @throws NodeException
	 */
	protected void setMaxAge() throws NodeException {
		operate(t -> {
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setPropertyMap("wastebin_maxage_node", Map.of(node.getId().toString(), "1"));
		});
	}

	/**
	 * Get a timestamp in the past, which is old enough for objects to be purged from the wastebin
	 * @return unix timestamp
	 */
	protected int getPastTimestamp() {
		return (int) (System.currentTimeMillis() / 1000) - 10;
	}

	/**
	 * Get the IDs of the records with the given ID in the given table, regardless of the wastebin state
	 * @param table table name
	 * @param id record ID
	 * @return set of IDs (empty if the record does not exist)
	 * @throws NodeException
	 */
	protected Set<Integer> getIds(String table, int id) throws NodeException {
		return DBUtils.select("SELECT id FROM " + table + " WHERE id = ?", ps -> ps.setInt(1, id), DBUtils.IDS);
	}
}
