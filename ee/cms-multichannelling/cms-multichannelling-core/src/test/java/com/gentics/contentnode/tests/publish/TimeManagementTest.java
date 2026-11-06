package com.gentics.contentnode.tests.publish;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishCR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for publishing pages with timemanagement
 */
@RunWith(value = Parameterized.class)
public class TimeManagementTest	 {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Node channel;

	private static Template template;

	/**
	 * Flag for multithreaded publishing
	 */
	@Parameter(0)
	public boolean multithreaded;

	/**
	 * Flag for using feature "publish_cache"
	 */
	@Parameter(1)
	public boolean publishCache;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: multithreaded publishing {0}, publish_cache: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean multithreaded : Arrays.asList(true, false)) {
			if (multithreaded) {
				for (boolean publishCache : Arrays.asList(true, false)) {
					data.add(new Object[] {multithreaded, publishCache});
				}
			} else {
				data.add(new Object[] {multithreaded, false});
			}
		}

		return data;
	}

	@BeforeClass
	public static void setUpOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		Trx.operate(() -> DBUtils.update("UPDATE node SET disable_publish = ?", 1));

		// set the feature for multichannelling
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING, true);
		// switch off GIS
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(Feature.TAG_IMAGE_RESIZER, false);

		// create a node
		node = Trx.supply(() -> createNode("hostname", "Node Name", PublishTarget.CONTENTREPOSITORY));

		// create a channel
		channel = Trx.supply(() -> createChannel(node, "Channel", "channel", "/", PublishTarget.CONTENTREPOSITORY));

		// create a template
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Before
	public void setUp() throws NodeException {
		// set the feature for multithreaded publishing
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTITHREADED_PUBLISHING, multithreaded);
		// set feature for publish_cache
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(Feature.PUBLISH_CACHE, publishCache);

		node = Trx.supply(() -> update(node, upd -> upd.setPublishDisabled(false)));
		channel = Trx.supply(() -> update(channel, upd -> upd.setPublishDisabled(true)));
	}

	/**
	 * Test that an offline (new) page goes online with the time management
	 * @throws Exception
	 */
	@Test
	public void testPageGoOnline() throws Exception {
		int createTime         = 1;
		int timeStart          = 4;
		int firstPublish       = 2;
		int firstPublishCheck  = 3;
		int secondPublish      = 5;
		int secondPublishCheck = 6;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish(timeStart, null));
		});

		// check page versions
		try (Trx trx = new Trx()) {
			assertThat(page).as("Test page").hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(createTime).setCurrent(true));
			trx.success();
		}

		// run a publish process (which should not publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(firstPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Test page").isOffline().isNotModified().hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(createTime).setCurrent(true));
			assertPublishCR(page, node, false);
			trx.success();
		}

		// check page versions

		// run a second publish process, which should publish the page now
		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(secondPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Tested page").isOnline().isNotModified()
					.hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(createTime).setCurrent(true).setPublished(true));
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(secondPublish);
			});
			trx.success();
		}
	}

	/**
	 * Test that an offline (new) page goes online with the time management
	 * @throws Exception
	 */
	@Test
	public void testEditedPageGoOnline() throws Exception {
		int createTime         = 1;
		int timeStart          = 5;
		int updateTime         = 2;
		int firstPublish       = 3;
		int firstPublishCheck  = 4;
		int secondPublish      = 6;
		int secondPublishCheck = 7;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish(timeStart, null));
		});

		String originalName = Trx.execute(Page::getName, page);

		// update page
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			Page update = trx.getTransaction().getObject(page, true);
			update.setName("Modified" + update.getName());
			update.save();

			page = page.reload();
			trx.success();
		}

		String updatedName = Trx.execute(Page::getName, page);
		assertThat(updatedName).as("Updated Name").isNotEqualTo(originalName);

		// check page versions
		try (Trx trx = new Trx()) {
			assertThat(page).as("Tested page").isOffline().isModified().hasVersions(new NodeObjectVersion().setNumber("1.1").setDate(updateTime).setCurrent(true),
					new NodeObjectVersion().setNumber("1.0").setDate(createTime));
			trx.success();
		}

		// run a publish process (which should not publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(firstPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Test page").isOffline().isModified();
			assertPublishCR(page, node, false);
			trx.success();
		}

		// run a second publish process, which should publish the page now
		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(secondPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Test page").isOnline().isModified().hasVersions(new NodeObjectVersion().setNumber("1.1").setDate(updateTime).setCurrent(true),
				new NodeObjectVersion().setNumber("1.0").setDate(createTime).setPublished(true));
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(secondPublish);
				assertThat(res.get("name")).as("Published name").isEqualTo(originalName);
			});
			trx.success();
		}
	}

	/**
	 * Test that an online page goes offline with the time management
	 * @throws Exception
	 */
	@Test
	public void testPageGoOffline() throws Exception {
		int createTime         = 1;
		int timeEnd            = 4;
		int firstPublish       = 2;
		int firstPublishCheck  = 3;
		int secondPublish      = 5;
		int secondPublishCheck = 6;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> {
				upd.publish();
				upd.takeOffline(timeEnd);
			});
		});

		// run a publish process (which should publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(firstPublishCheck * 1000L);
			page = page.reload();

			// check page
			assertThat(page).as("Test page").isOnline().isNotModified();
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(createTime);
			});
			trx.success();
		}

		// run a second publish process, which should take the page offline
		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(secondPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Test page").isOffline().isNotModified()
					.hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(createTime).setCurrent(true).setPublished(true));
			assertPublishCR(page, node, false);
			trx.success();
		}
	}

	/**
	 * Test that an online page goes offline with the time management, even if it was edited
	 * @throws Exception
	 */
	@Test
	public void testEditedPageGoOffline() throws Exception {
		int createTime         = 1;
		int timeEnd            = 5;
		int updateTime         = 2;
		int firstPublish       = 3;
		int firstPublishCheck  = 4;
		int secondPublish      = 6;
		int secondPublishCheck = 7;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> {
				upd.publish();
				upd.takeOffline(timeEnd);
			});
		});

		// update page
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			Page update = trx.getTransaction().getObject(page, true);
			update.setName("Modified" + update.getName());
			update.save();

			page = page.reload();
			trx.success();
		}

		// run a publish process (which should publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(firstPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Test page").isOnline().isModified();
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(createTime);
			});
			trx.success();
		}

		// run a second publish process, which should take the page offline
		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(secondPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Test page").isOffline().isModified().hasVersions(new NodeObjectVersion().setNumber("1.1").setDate(updateTime).setCurrent(true),
					new NodeObjectVersion().setNumber("1.0").setDate(createTime).setPublished(true));
			assertPublishCR(page, node, false);
			trx.success();
		}
	}

	/**
	 * Test republishing an online page
	 * @throws Exception
	 */
	@Test
	public void testRepublishPage() throws Exception {
		int createTime    = 1;
		int firstPublish  = 2;
		int updateTime    = 3;
		int timeStart     = 5;
		int secondPublish = 4;
		int thirdPublish  = 6;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish());
		});

		String originalName = Trx.execute(Page::getName, page);

		// run a publish process (which should publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// update the page and let it be republished in the future
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			Page update = trx.getTransaction().getObject(page, true);
			update.setName("Republished" + update.getName());
			update.save();

			update.publish(timeStart, null);

			page = page.reload();
			trx.success();
		}

		String republishedName = Trx.execute(Page::getName, page);

		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// check page
		try (Trx trx = new Trx()) {
			page = page.reload();

			assertThat(page).as("Test page").isOnline().isNotModified().hasVersions(new NodeObjectVersion().setNumber("2.0").setDate(updateTime).setCurrent(true),
					new NodeObjectVersion().setNumber("1.0").setDate(createTime).setPublished(true));
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(createTime);
				assertThat(res.get("name")).as("Published name").isEqualTo(originalName);
			});
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(thirdPublish);
			trx.success();
		}

		// check page
		try (Trx trx = new Trx()) {
			page = page.reload();

			assertThat(page).as("Test page").isOnline().isNotModified().hasVersions(
					new NodeObjectVersion().setNumber("2.0").setDate(updateTime).setCurrent(true).setPublished(true),
					new NodeObjectVersion().setNumber("1.0").setDate(createTime));
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(thirdPublish);
				assertThat(res.get("name")).as("Published name").isEqualTo(republishedName);
			});
			trx.success();
		}
	}

	/**
	 * Test republishing an online page after it has been modified
	 * @throws Exception
	 */
	@Test
	public void testRepublishEditedPage() throws Exception {
		int createTime    = 1;
		int firstPublish  = 2;
		int firstUpdate   = 3;
		int timeStart     = 6;
		int secondUpdate  = 4;
		int secondPublish = 5;
		int thirdPublish  = 7;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish());
		});

		String originalName = Trx.execute(Page::getName, page);

		// run a publish process (which should publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// update the page and let it be republished in the future
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(firstUpdate * 1000L);
			Page update = trx.getTransaction().getObject(page, true);
			update.setName("Republished" + update.getName());
			update.save();

			update.publish(timeStart, null);

			page = page.reload();
			trx.success();
		}

		String republishedName = Trx.execute(Page::getName, page);

		// update the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(secondUpdate * 1000L);
			Page update = trx.getTransaction().getObject(page, true);
			update.setName("Modified" + update.getName());
			update.save();

			page = page.reload();
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// check page
		try (Trx trx = new Trx()) {
			page = page.reload();

			assertThat(page).as("Test page").isOnline().isModified().hasVersions(new NodeObjectVersion().setNumber("2.1").setDate(secondUpdate).setCurrent(true),
					new NodeObjectVersion().setNumber("2.0").setDate(firstUpdate), new NodeObjectVersion().setNumber("1.0").setDate(createTime).setPublished(true));
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(createTime);
				assertThat(res.get("name")).as("Published name").isEqualTo(originalName);
			});
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.publish(thirdPublish);
			trx.success();
		}

		// check page
		try (Trx trx = new Trx()) {
			page = page.reload();

			assertThat(page).as("Test page").isOnline().isModified().hasVersions(new NodeObjectVersion().setNumber("2.1").setDate(secondUpdate).setCurrent(true),
					new NodeObjectVersion().setNumber("2.0").setDate(firstUpdate).setPublished(true), new NodeObjectVersion().setNumber("1.0").setDate(createTime));
			assertPublishCR(page, node, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(thirdPublish);
				assertThat(res.get("name")).as("Published name").isEqualTo(republishedName);
			});
			trx.success();
		}
	}

	/**
	 * Test doing time management in a channel where publishing is disabled for the master
	 * @throws Exception
	 */
	@Test
	public void testInChannelWithMasterDisabled() throws Exception {
		int createTime         = 1;
		int timeStart          = 4;
		int firstPublish       = 2;
		int firstPublishCheck  = 3;
		int secondPublish      = 5;
		int secondPublishCheck = 6;

		// disable publishing for node, enable for channel
		node = Trx.supply(() -> update(node, upd -> upd.setPublishDisabled(true)));
		channel = Trx.supply(() -> update(channel, upd -> upd.setPublishDisabled(false)));

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish(timeStart, null));
		});

		// run a publish process (which should not publish this page)
		try (Trx trx = new Trx()) {
			testContext.publish(firstPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(firstPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Tested page").isOffline().isNotModified();
			assertPublishCR(page, channel, false);
			trx.success();
		}

		// run a second publish process, which should publish the page now
		try (Trx trx = new Trx()) {
			testContext.publish(secondPublish);
			trx.success();
		}

		// get the page again
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(secondPublishCheck * 1000L);
			page = page.reload();

			// check status and online
			assertThat(page).as("Tested page").isOnline().isNotModified();
			assertPublishCR(page, channel, true, res -> {
				assertThat(res.get("publishtimestamp")).as("Publish timestamp").isEqualTo(secondPublish);
			});
			trx.success();
		}
	}

	/**
	 * Test clearing a previously set time management
	 * @throws Exception
	 */
	@Test
	public void testClearTimePub() throws Exception {
		int createTime = 1;
		int updateTime = 2;
		int timeStart = 4;

		Page page = Trx.supply(t -> {
			t.setTimestamp(createTime * 1000L);
			Page created = createPage(node.getFolder(), template, "Page");
			return update(created, upd -> upd.publish(timeStart, null));
		});

		// check page
		try (Trx trx = new Trx()) {
			assertThat(page).as("Test page").isOffline().isNotModified().hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(createTime).setCurrent(true));
			trx.success();
		}

		// update page
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(updateTime * 1000L);
			Page update = trx.getTransaction().getObject(page, true);
			update.clearTimePub();

			page = page.reload();
			trx.success();
		}

		// check page
		try (Trx trx = new Trx()) {
			assertThat(page).as("Test page").isOffline().isNotModified().hasVersions(new NodeObjectVersion().setNumber("1.0").setDate(createTime).setCurrent(true));
			trx.success();
		}
	}
}
