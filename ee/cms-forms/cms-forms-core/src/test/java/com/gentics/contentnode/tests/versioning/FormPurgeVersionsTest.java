package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.scheduler.PurgeVersionsJob;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for purging versions of forms
 */
@GCNFeature(set = { Feature.FORMS, Feature.WASTEBIN, Feature.MESH_CONTENTREPOSITORY })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormPurgeVersionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("hostname", "Test Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		consume(n -> {
			n.activateFeature(Feature.FORMS);
		}, node);
	}

	@After
	public void clean() throws NodeException {
		operate(() -> clear(node));
	}

	/**
	 * Test purging all old versions
	 * @throws NodeException
	 */
	@Test
	public void testPurge() throws NodeException {
		// version 0.1 @1
		Form form = createForm("Initial Version", 1);

		// version 0.2 @2
		form = updateForm(form, "Version 2", 2);

		// version 0.3 @3
		form = updateForm(form, "Version 3", 3);

		consume(f -> assertThat(f).hasVersions(
			new NodeObjectVersion().setNumber("0.3").setCurrent(true).setPublished(false).setDate(3),
			new NodeObjectVersion().setNumber("0.2").setCurrent(false).setPublished(false).setDate(2),
			new NodeObjectVersion().setNumber("0.1").setCurrent(false).setPublished(false).setDate(1)
		), form);

		operate(() -> runPurgeVersionsJob(4));
		form = execute(Form::reload, form);

		consume(f -> assertThat(f).hasVersions(
			new NodeObjectVersion().setNumber("0.3").setCurrent(true).setPublished(false).setDate(3)
		), form);
	}

	/**
	 * Test purging versions older than the published version
	 * @throws NodeException
	 */
	@Test
	public void testPurgePublished() throws NodeException {
		// version 0.1 @1
		Form form = createForm("Initial Version", 1);

		// version 0.2 @2
		form = updateForm(form, "Version 2", 2);

		// publish 0.2 -> 1.0 @3
		form = publishForm(form, 3);

		// version 1.1 @4
		form = updateForm(form, "Version 3", 4);

		consume(f -> assertThat(f).hasVersions(
			new NodeObjectVersion().setNumber("1.1").setCurrent(true).setPublished(false).setDate(4),
			new NodeObjectVersion().setNumber("1.0").setCurrent(false).setPublished(true).setDate(2),
			new NodeObjectVersion().setNumber("0.1").setCurrent(false).setPublished(false).setDate(1)
		), form);

		operate(() -> runPurgeVersionsJob(5));
		form = execute(Form::reload, form);

		consume(f -> assertThat(f).hasVersions(
				new NodeObjectVersion().setNumber("1.1").setCurrent(true).setPublished(false).setDate(4),
				new NodeObjectVersion().setNumber("1.0").setCurrent(false).setPublished(true).setDate(2)
		), form);
	}

	/**
	 * Test purging versions older than the planned version
	 * @throws NodeException
	 */
	@Test
	public void testPurgePlanned() throws NodeException {
		// version 0.1 @1
		Form form = createForm("Initial Version", 1);

		// version 0.2 @2
		form = updateForm(form, "Version 2", 2);

		// publish 0.2 -> 1.0 @3
		form = planForm(form, 10, 3);

		// version 1.1 @4
		form = updateForm(form, "Version 3", 4);

		consume(f -> assertThat(f).hasVersions(
			new NodeObjectVersion().setNumber("1.1").setCurrent(true).setPublished(false).setDate(4),
			new NodeObjectVersion().setNumber("1.0").setCurrent(false).setPublished(false).setDate(2),
			new NodeObjectVersion().setNumber("0.1").setCurrent(false).setPublished(false).setDate(1)
		), form);

		operate(() -> runPurgeVersionsJob(5));
		form = execute(Form::reload, form);

		consume(f -> assertThat(f).hasVersions(
				new NodeObjectVersion().setNumber("1.1").setCurrent(true).setPublished(false).setDate(4),
				new NodeObjectVersion().setNumber("1.0").setCurrent(false).setPublished(false).setDate(2)
		), form);
	}

	/**
	 * Test purging versions in the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testPurgeWastebin() throws NodeException {
		// version 0.1 @1
		Form form = createForm("Initial Version", 1);

		// version 0.2 @2
		form = updateForm(form, "Version 2", 2);

		// version 0.3 @3
		form = updateForm(form, "Version 3", 3);

		// delete the form
		deleteForm(form);

		consume(f -> assertThat(f).hasVersions(
			new NodeObjectVersion().setNumber("0.3").setCurrent(true).setPublished(false).setDate(3),
			new NodeObjectVersion().setNumber("0.2").setCurrent(false).setPublished(false).setDate(2),
			new NodeObjectVersion().setNumber("0.1").setCurrent(false).setPublished(false).setDate(1)
		), form);

		operate(() -> runPurgeVersionsJob(4));
		form = execute(f -> {
			try (WastebinFilter wb = new WastebinFilter(Wastebin.INCLUDE)) {
				return f.reload();
			}
		}, form);

		consume(f -> assertThat(f).isDeleted().hasVersions(
			new NodeObjectVersion().setNumber("0.3").setCurrent(true).setPublished(false).setDate(3)
		), form);
	}

	/**
	 * Test purging versions older than the purge timestamp
	 * @throws NodeException
	 */
	@Test
	public void testPurgePartial() throws NodeException {
		// version 0.1 @1
		Form form = createForm("Initial Version", 1);

		// version 0.2 @2
		form = updateForm(form, "Version 2", 2);

		// version 0.3 @3
		form = updateForm(form, "Version 3", 3);

		// version 0.4 @4
		form = updateForm(form, "Version 4", 4);

		consume(f -> assertThat(f).hasVersions(
			new NodeObjectVersion().setNumber("0.4").setCurrent(true).setPublished(false).setDate(4),
			new NodeObjectVersion().setNumber("0.3").setCurrent(false).setPublished(false).setDate(3),
			new NodeObjectVersion().setNumber("0.2").setCurrent(false).setPublished(false).setDate(2),
			new NodeObjectVersion().setNumber("0.1").setCurrent(false).setPublished(false).setDate(1)
		), form);

		operate(() -> runPurgeVersionsJob(3));
		form = execute(Form::reload, form);

		consume(f -> assertThat(f).hasVersions(
				new NodeObjectVersion().setNumber("0.4").setCurrent(true).setPublished(false).setDate(4),
				new NodeObjectVersion().setNumber("0.3").setCurrent(false).setPublished(false).setDate(3)
		), form);
	}

	/**
	 * Create a form
	 * @param name form name
	 * @param timestamp timestamp
	 * @return new form
	 * @throws NodeException
	 */
	protected Form createForm(String name, int timestamp) throws NodeException {
		return supply(t -> {
			t.setTimestamp(timestamp * 1000L);
			return create(Form.class, f -> {
				f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
				f.setFolderId(node.getFolder().getId());
				f.setName(name);
			});
		});
	}

	/**
	 * Update a form
	 * @param form form
	 * @param name new form name
	 * @param timestamp timestamp
	 * @return updated form
	 * @throws NodeException
	 */
	protected Form updateForm(Form form, String name, int timestamp) throws NodeException {
		return execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(timestamp * 1000L);
			return update(f, upd -> {
				upd.setName(name);
			});
		}, form);
	}

	/**
	 * Publish the form
	 * @param form form
	 * @param timestamp timestamp
	 * @return published form
	 * @throws NodeException
	 */
	protected Form publishForm(Form form, int timestamp) throws NodeException {
		return execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(timestamp * 1000L);
			f.publish();
			return f;
		}, form);
	}

	/**
	 * Plan the form to be published
	 * @param form form
	 * @param at planned time
	 * @param timestamp timestamp
	 * @return planned form
	 * @throws NodeException
	 */
	protected Form planForm(Form form, int at, int timestamp) throws NodeException {
		return execute(f -> {
			TransactionManager.getCurrentTransaction().setTimestamp(timestamp * 1000L);
			f.publish(at, false);
			return f;
		}, form);
	}

	/**
	 * Delete the form (to the wastebin)
	 * @param form form
	 * @throws NodeException
	 */
	protected void deleteForm(Form form) throws NodeException {
		consume(f -> f.delete(), form);
	}

	/**
	 * Starts the purge versions job that will remove all older versions.
	 * 
	 * @throws NodeException
	 */
	protected void runPurgeVersionsJob(int purgeTimestamp) throws NodeException {
		PurgeVersionsJob job = new PurgeVersionsJob();
		job.purgeForms(purgeTimestamp);
	}
}
