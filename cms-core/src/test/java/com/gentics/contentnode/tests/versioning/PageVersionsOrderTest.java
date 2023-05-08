package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.job.AbstractUserActionJob;
import com.gentics.contentnode.migration.jobs.TagTypeMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.tests.migration.EmptyPostProcessor;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;

/**
 * Test cases for the order of page versions
 */
public class PageVersionsOrderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static Integer constructId;

	private static Integer otherConstructId;

	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "construct", "part"));
		otherConstructId = Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "otherconstruct", "part"));
		user = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(SystemUser.class, 1));
	}

	/**
	 * Test creation of an implicit page version when starting to edit a page that has unversioned changes
	 * @throws NodeException
	 */
	@Test
	public void testImplicitVersion() throws NodeException {
		// create a page
		Page page = null;
		Version v1 = null;
		try (Trx trx = new Trx()) {
			v1 = new Version(trx.getTransaction().getUnixTimestamp(), "0.1");
			page = createPage(node.getFolder(), template, "Page - Created");
			trx.success();
		}

		assertPageVersions(page, v1);

		ContentTag tag = null;
		Version v2 = null;

		// update the page (creating a version)
		ContentNodeTestUtils.waitForNextSecond();
		try (Trx trx = new Trx()) {
			v2 = new Version(trx.getTransaction().getUnixTimestamp(), "0.2");
			Page editable = trx.getTransaction().getObject(page, true);
			editable.setName("Page - first Modification");
			tag = editable.getContent().addContentTag(constructId);
			editable.save();
			editable.unlock();
			trx.success();
		}

		assertPageVersions(page, v2, v1);

		// update the tag, but create no version
		Version v3 = null;
		ContentNodeTestUtils.waitForNextSecond();
		try (Trx trx = new Trx()) {
			v3 = new Version(trx.getTransaction().getUnixTimestamp(), "0.3");
			ContentTag editableTag = trx.getTransaction().getObject(tag, true);
			editableTag.getValues().getByKeyname("part").setValueText("bla");
			editableTag.save();
			trx.success();
		}

		assertPageVersions(page, v2, v1);

		// get editable copy (we expect an implicit version)
		ContentNodeTestUtils.waitForNextSecond();
		try (Trx trx = new Trx()) {
			Page editablePage = trx.getTransaction().getObject(page, true);
			editablePage.unlock();
			trx.success();
		}

		assertPageVersions(page, v3, v2, v1);

		// now delete the tag (without creating a version
		ContentNodeTestUtils.waitForNextSecond();
		try (Trx trx = new Trx()) {
			ContentTag editableTag = trx.getTransaction().getObject(tag, true);
			editableTag.delete();
			trx.success();
		}

		assertPageVersions(page, v3, v2, v1);
		assertModified(page);

		// get editable copy (we expect no new implicit version)
		ContentNodeTestUtils.waitForNextSecond();
		try (Trx trx = new Trx()) {
			Page editablePage = trx.getTransaction().getObject(page, true);
			editablePage.unlock();
			trx.success();
		}

		assertPageVersions(page, v3, v2, v1);
		assertUnmodified(page);
	}

	/**
	 * Test versions when page is handled by a TagTypeMigration
	 * @throws NodeException
	 */
	@Test
	public void testTagTypeMigration() throws NodeException {
		// create a published page
		Page page = null;
		Version v1 = null;
		try (Trx trx = new Trx()) {
			v1 = new Version(trx.getTransaction().getUnixTimestamp(), "1.0");
			page = update(createPage(node.getFolder(), template, "Page"), p -> {
				p.getContent().addContentTag(constructId);
				p.publish();
			});
			page.unlock();
			trx.success();
		}

		assertPageVersions(page, v1);

		// perform TTM
		TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
		mapping.setFromTagTypeId(constructId);
		mapping.setToTagTypeId(otherConstructId);

		MigrationPartMapping partMapping = new MigrationPartMapping();
		partMapping
				.setFromPartId(Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId).getParts().get(0).getId()));
		partMapping.setToPartId(
				Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(Construct.class, otherConstructId).getParts().get(0).getId()));

		mapping.setPartMappings(Arrays.asList(partMapping));

		TagTypeMigrationRequest request = new TagTypeMigrationRequest();
		request.setMappings(new ArrayList<>(Arrays.asList(mapping)));
		request.setType("page");
		request.setObjectIds(new ArrayList<>(Arrays.asList(page.getId())));

		MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
		configuredPostProcessor.setClassName(WaitingPostProcessor.class.getName());
		configuredPostProcessor.setOrderId(0);
		request.setEnabledPostProcessors(Arrays.asList(configuredPostProcessor));

		ContentNodeTestUtils.waitForNextSecond();
		// no check for timestamp of version 2.0, because we do not exactly know, what it will be
		Version v2 = new Version("2.0");
		try (Trx trx = new Trx(user)) {
			Transaction t = trx.getTransaction();
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			job.addParameter(TagTypeMigrationJob.PARAM_REQUEST, request);
			job.addParameter(TagTypeMigrationJob.PARAM_TYPE, request.getType());
			job.addParameter(TagTypeMigrationJob.PARAM_OBJECTIDS, new ArrayList<>(request.getObjectIds()));
			String sessionId = t.getSessionId();
			job.addParameter(TagTypeMigrationJob.PARAM_HANDLE_PAGES_BY_TEMPLATE, false);
			job.addParameter(TagTypeMigrationJob.PARAM_HANDLE_ALL_NODES, false);
			assertNotNull(sessionId);
			job.addParameter(AbstractUserActionJob.PARAM_SESSIONID, sessionId);
			job.addParameter(AbstractUserActionJob.PARAM_USERID, t.getUserId());
			job.addParameter(TagTypeMigrationJob.PARAM_PREVENT_TRIGGER_EVENT, false);

			job.execute(1000);

			trx.success();
		}

		assertPageVersions(page, v2, v1);
	}

	/**
	 * Assert that the page has exactly the given versions
	 * @param page page
	 * @param expected expected versions (newest first)
	 * @throws NodeException
	 */
	protected void assertPageVersions(Page page, Version... expected) throws NodeException {
		List<Version> versions = Trx.supply(() -> Arrays.asList(TransactionManager.getCurrentTransaction().getObject(page).getVersions()).stream()
				.map(v -> new Version(v)).collect(Collectors.toList()));
		assertThat(versions).as(String.format("Versions of %s", page)).containsExactly(expected);
	}

	/**
	 * Assert that the page is modified (i.e. different from the last stored version)
	 * @param page page
	 * @throws NodeException
	 */
	protected void assertModified(Page page) throws NodeException {
		// this check is a bit hackish, we let the PageFactory create a new version (this will check for modifications)
		// and assert that it would create a version,
		// but then we rollback the transaction (by not calling trx.success()), so that the DB is not actually changed
		try (Trx trx = new Trx()) {
			assertThat(PageFactory.createPageVersion(page, false, 0)).as("Would create version").isTrue();
		}
	}

	/**
	 * Assert that the page is unmodified (i.e. not different from the last stored version)
	 * @param page page
	 * @throws NodeException
	 */
	protected void assertUnmodified(Page page) throws NodeException {
		// this check is a bit hackish, we let the PageFactory create a new version (this will check for modifications)
		// and assert that it would not create a version,
		// but then we rollback the transaction (by not calling trx.success()), so that the DB is not actually changed
		try (Trx trx = new Trx()) {
			assertThat(PageFactory.createPageVersion(page, false, 0)).as("Would create version").isFalse();
		}
	}

	/**
	 * Page version (containing number and timestamp) for assertions<br>
	 * If a page version has timestamp 0, only the number will be compared
	 */
	public static class Version {
		/**
		 * Timestamp
		 */
		protected int timestamp;

		/**
		 * Number
		 */
		protected String number;

		/**
		 * Create version with number
		 * @param number number
		 */
		public Version(String number) {
			this(0, number);
		}

		/**
		 * Create version with timestamp and number
		 * @param timestamp timestamp
		 * @param number number
		 */
		public Version(int timestamp, String number) {
			this.timestamp = timestamp;
			this.number = number;
		}

		/**
		 * Create version from the given pageversion
		 * @param version pageversion
		 */
		public Version(NodeObjectVersion version) {
			this.timestamp = version.getDate().getIntTimestamp();
			this.number = version.getNumber();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Version) {
				Version other = (Version)obj;
				if (!number.equals(other.number)) {
					return false;
				}
				// if one of the versions has timestamp 0, we do not compare timestamps
				if (timestamp == 0 || other.timestamp == 0) {
					return true;
				}
				return timestamp == other.timestamp;
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("%s @%d", number, timestamp);
		}
	}

	/**
	 * Postprocessor instance that will wait a bit, and then modify the page
	 */
	public static class WaitingPostProcessor extends EmptyPostProcessor {
		@Override
		public void applyPostMigrationProcessing(com.gentics.contentnode.rest.model.Page page, TagTypeMigrationRequest request, NodeLogger logger)
				throws MigrationException {
			ContentNodeTestUtils.waitForNextSecond();
			page.setName(page.getName() + " postprocessed");
		}
	}
}
