package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.StageableVersionedNodeObject;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;

/**
 * Abstract test cases for staging of versioned objects
 */
public abstract class AbstractVersionedContentStagingTest<T extends StageableVersionedNodeObject> extends AbstractBaseContentStagingTest<T> {
	protected int publishAt;

	/**
	 * Create instance
	 * @param clazz class
	 * @param type type
	 */
	protected AbstractVersionedContentStagingTest(Class<T> clazz, String type) {
		super(clazz, type);
		this.publishAt = (int)(System.currentTimeMillis() / 1000L) + 86400;
	}

	/**
	 * Test importing a new object with multiple versions in the content package
	 * @throws NodeException
	 */
	@Test
	public void testImportNewObjectWithVersions() throws NodeException {
		// create object
		T stagedObject = createObject(supply(() -> node.getFolder()), 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// publish the object
		stagedObject = update(stagedObject, upd -> {}).at(1).publish().build();

		// modify the object
		stagedObject = updateObject(stagedObject, 2);

		// schedule the object
		update(stagedObject, upd -> {}).at(2).publish(publishAt).build();

		// modify the object again
		stagedObject = updateObject(stagedObject, 3);

		consume(o -> {
			assertThat(o).as("Original object")
				.isNotNull()
				.hasVersions(
						new NodeObjectVersion().setNumber("2.1").setPublished(false).setCurrent(true).setDate(3),
						new NodeObjectVersion().setNumber("2.0").setPublished(false).setCurrent(false).setDate(2),
						new NodeObjectVersion().setNumber("1.0").setPublished(true).setCurrent(false).setDate(1)
				)
				.isOnline()
				.isModified()
				.isPlanned()
				.hasPublishAt(publishAt, "2.0");
		}, stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		// delete object in CMS
		consume(f -> f.delete(true), stagedObject);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));

		consume(o -> {
			assertThat(o).as("Staged object")
				.isNotNull()
				.comparingVersionFields("number", "current", "published")
				.hasVersions(
						new NodeObjectVersion().setNumber("2.1").setPublished(false).setCurrent(true),
						new NodeObjectVersion().setNumber("2.0").setPublished(false).setCurrent(false),
						new NodeObjectVersion().setNumber("1.0").setPublished(true).setCurrent(false)
				)
				.isOnline()
				.isModified()
				.isPlanned()
				.hasPublishAt(publishAt, "2.0");
		}, stagedObject);
	}

	/**
	 * Test importing an updated object with multiple versions in the content package
	 * @throws NodeException
	 */
	@Test
	public void testImportUpdatedObjectWithVersions() throws NodeException {
		// create object
		T stagedObject = createObject(supply(() -> node.getFolder()), 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// publish the object
		stagedObject = update(stagedObject, upd -> {}).at(1).publish().build();

		// modify the object
		stagedObject = updateObject(stagedObject, 2);

		// schedule the object
		update(stagedObject, upd -> {}).at(2).publish(publishAt).build();

		// modify the object again
		stagedObject = updateObject(stagedObject, 3);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		// modify and publish the object in the CMS
		stagedObject = updateObject(stagedObject, 4);
		update(stagedObject, upd -> {}).at(4).publish().build();

		// modify again
		stagedObject = updateObject(stagedObject, 5);

		consume(o -> {
			assertThat(o).as("Original object")
				.isNotNull()
				.hasVersions(
						new NodeObjectVersion().setNumber("3.1").setPublished(false).setCurrent(true).setDate(5),
						new NodeObjectVersion().setNumber("3.0").setPublished(true).setCurrent(false).setDate(4),
						new NodeObjectVersion().setNumber("2.1").setPublished(false).setCurrent(false).setDate(3),
						new NodeObjectVersion().setNumber("2.0").setPublished(false).setCurrent(false).setDate(2),
						new NodeObjectVersion().setNumber("1.0").setPublished(false).setCurrent(false).setDate(1)
				)
				.isOnline()
				.isModified()
				.isNotPlanned();
		}, stagedObject);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));

		consume(o -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			assertThat(o).as("Staged object")
				.isNotNull()
				.isOnline()
				.isModified()
				.isPlanned()
				.comparingVersionFields("number", "current", "published")
				.hasVersions(
						new NodeObjectVersion().setNumber("5.1").setPublished(false).setCurrent(true),
						new NodeObjectVersion().setNumber("5.0").setPublished(false).setCurrent(false),
						new NodeObjectVersion().setNumber("4.0").setPublished(true).setCurrent(false),
						new NodeObjectVersion().setNumber("3.1").setPublished(false).setCurrent(false),
						new NodeObjectVersion().setNumber("3.0").setPublished(false).setCurrent(false),
						new NodeObjectVersion().setNumber("2.1").setPublished(false).setCurrent(false),
						new NodeObjectVersion().setNumber("2.0").setPublished(false).setCurrent(false),
						new NodeObjectVersion().setNumber("1.0").setPublished(false).setCurrent(false)
				)
				.hasPublishAt(publishAt, "5.0");
			T publishedObject = t.getObject(clazz, o.getId(), o.getPublishedVersion().getDate().getIntTimestamp());
			assertThat(publishedObject).as("Published object").isNotNull().hasFieldOrPropertyWithValue("name", "Staged " + type);

			T plannedObject = t.getObject(clazz, o.getId(), o.getTimePubVersion().getDate().getIntTimestamp());
			assertThat(plannedObject).as("Planned object").isNotNull().hasFieldOrPropertyWithValue("name", "Modified " + type + " @2");
		}, stagedObject);
	}
}
