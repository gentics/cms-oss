package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.NodeObjectHandler;
import com.gentics.contentnode.testutils.DBTestContext;

public abstract class AbstractObjectPropertiesSyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node node;

	protected static Integer constructId;

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected PackageSynchronizer pack;

	protected ObjectTagDefinition objectProperty;

	protected ObjectTagDefinitionCategory category;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}

		Node newNode = Trx.supply(() -> ContentNodeTestDataUtils.createNode());

		node = Trx.supply(t -> t.getObject(newNode, true));
		constructId = Trx.supply(() -> createConstruct(node, HTMLPartType.class, "construct", "part"));
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);
		Synchronizer.disable();

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		if (objectProperty != null) {
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				objectProperty = t.getObject(objectProperty, true);

				for (ObjectTag tag : objectProperty.getObjectTags()) {
					tag.delete();
				}

				if (objectProperty != null) {
					objectProperty.delete(true);
				}
			});
			objectProperty = null;
		}
		if (category != null) {
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				category = t.getObject(category, true);
				if (category != null) {
					category.delete(true);
				}
			});
			category = null;
		}
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	/**
	 * Do the sync test
	 * @param type object type
	 * @param prepare prepare object property
	 * @param delete true to delete the object property before importing
	 * @param change handler to change the object property (if it is not sync'ed as new object)
	 * @param beforeSync optional operator to change something after object property has been deleted/modified (before sync from fs)
	 * @param afterSync optional operator to change something after the sync from the fs
	 * @param asserter asserter after sync from fs
	 * @throws NodeException
	 */
	protected void syncTest(int type, NodeObjectHandler<ObjectTagDefinition> prepare, boolean delete,
			NodeObjectHandler<ObjectTagDefinition> change, Operator beforeSync, Operator afterSync, Operator asserter)
			throws NodeException {
		objectProperty = Trx.supply(() -> update(createObjectPropertyDefinition(type, constructId, "Test Object Property", "testoe"), prepare));
		GlobalId globalId = objectProperty.getGlobalId();

		// add to package
		Trx.operate(() -> pack.synchronize(objectProperty, true));

		if (delete) {
			// delete object property
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				t.getObject(objectProperty, true).delete(true);
			});
		} else {
			objectProperty = Trx.supply(() -> update(objectProperty, change));
		}

		if (beforeSync != null) {
			beforeSync.operate();
		}

		// sync from FS
		assertThat(Trx.supply(() -> pack.syncAllFromFilesystem(ObjectTagDefinition.class))).as("number of synchronized object properties").isEqualTo(1);

		if (afterSync != null) {
			afterSync.operate();
		}

		// reload object property
		objectProperty = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(ObjectTagDefinition.class, globalId);
		});

		asserter.operate();
	}

}
