package com.gentics.contentnode.tests.publish.attributes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Multithreaded Version of {@link AttributePublishTest}
 */
@GCNFeature(set={Feature.ATTRIBUTE_DIRTING, Feature.MULTICHANNELLING, Feature.MCCR, Feature.MULTITHREADED_PUBLISHING, Feature.CR_FILESYSTEM_ATTRIBUTES})
public class MultithreadedAttributePublishTest extends AttributePublishTest {

	@Test
	public void testPagePublishHandlerCache() throws Exception {
		Node node = null;
		Page page = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode MCCR", PublishTarget.CONTENTREPOSITORY, true, false);
			ContentNodeTestDataUtils.createChannel(node, "Testchannel MCCR", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// set basepath
			ContentNodeTestDataUtils.update(node.getContentRepository(), cr -> cr.setBasepath(System.getProperty("java.io.tmpdir")));

			// add non optimized tagmap entry
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.name", "test",
					null, false, false, false, 0, null, null);
			// add non optimized filesystem tagmap entry
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, "page.name", "testfs",
					null, false, true, false, 0, null, null);

			// add publish handlers
			ContentNodeTestDataUtils.addPublishHandler(node.getContentRepository(), CacheTestingPublishHandler.class);

			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage");
			Template template = trx.getTransaction().getObject(page.getTemplate(), true);
			template.setSource(TEMPLATE);
			template.save();

			page = trx.getTransaction().getObject(page, true);
			page.publish();
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CacheTestingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// dirt name for page in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(page, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		CacheTestingPublishHandler.reset();
		CacheTestingPublishHandler.set("name", "content", "node_id", "publishtimestamp", "test", "testfs");
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// no additional SQL Statements allowed
		assertThat(CacheTestingPublishHandler.get().getCount()).as("SQL Count (" + CacheTestingPublishHandler.get().getLoggedStatements() + ")").isEqualTo(0);
	}
}
