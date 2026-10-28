package com.gentics.contentnode.tests.wastebin;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateAndPage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.scheduler.CoreInternalSchedulerTask;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for purging the wastebin, when purging fails for a node
 */
@GCNFeature(set = { Feature.WASTEBIN })
public class PurgeWastebinNodeErrorTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test that the purge task continues with the other nodes, when purging fails for a node, and reports the failure
	 * @throws NodeException
	 */
	@Test
	public void testPurgeContinuesAfterFailingNode() throws NodeException {
		// the failing node is created first, so that it is processed before the other node (nodes are read in the order of their IDs)
		Node failingNode = supply(() -> createNode());
		Node node = supply(() -> createNode());
		Page page = supply(() -> createTemplateAndPage(node.getFolder(), "Page"));

		// put the page into the wastebin (in the past, so that it is old enough to be purged)
		try (Trx trx = new Trx()) {
			trx.at((int) (System.currentTimeMillis() / 1000) - 10);
			trx.getTransaction().getObject(page).delete();
			trx.success();
		}

		// let the failing node reference a non existing root folder (inconsistent data)
		operate(t -> {
			DBUtils.update("UPDATE node SET folder_id = ? WHERE id = ?", Integer.MAX_VALUE, failingNode.getId());
			t.dirtObjectCache(Node.class, failingNode.getId());
		});

		operate(t -> {
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
			prefs.setPropertyMap("wastebin_maxage_node", Map.of(node.getId().toString(), "1"));
		});

		// Reset the current lang context to mimic the truly scheduled env (not possible by default as per ContentNodeTestContext)
		ContentNodeHelper.setLanguageId(-1);
		List<String> out = new ArrayList<>();
		boolean success = CoreInternalSchedulerTask.purgewastebin.execute(out);

		assertThat(success).as("Purge success").isFalse();
		assertThat(out).as("Purge output").anyMatch(line -> line.startsWith("Error while purging wastebin for node"));
		operate(() -> {
			assertThat(DBUtils.select("SELECT id FROM page WHERE id = ?", ps -> ps.setInt(1, page.getId()), DBUtils.IDS))
					.as("Records of page in wastebin").isEmpty();
		});
	}
}
