package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.DEVTOOLS })
public class ConstructCategorySyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private PackageSynchronizer pack;

	private ConstructCategory category;

	private Construct construct;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}

		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
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
		if (construct != null) {
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				construct = t.getObject(construct, true);
				if (construct != null) {
					construct.delete(true);
				}
			});
			construct = null;
		}
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	/**
	 * Test synchronization of the construct category
	 * @throws NodeException
	 */
	@Test
	public void testSyncCategory() throws NodeException {
		category = Trx.supply(() -> create(ConstructCategory.class, cat -> {
			cat.setName("Testkategorie", 1);
			cat.setName("Testcategory", 2);
			cat.setSortorder(1);
		}));

		construct = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			return update(t.getObject(Construct.class, ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "construct", "part")), constr -> {
				constr.setConstructCategoryId(category.getId());
			});
		});
		GlobalId globalId = construct.getGlobalId();

		// add to package
		Trx.operate(() -> pack.synchronize(construct, true));

		// delete construct
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.getObject(construct, true).delete(true);
		});

		// change category name
		category = Trx.supply(() -> update(category, cat -> {
			cat.setName("Geänderte Testkategorie", 1);
			cat.setName("Modified Testcategory", 2);
		}));

		// sync from FS
		assertThat(Trx.supply(() -> pack.syncAllFromFilesystem(Construct.class))).as("number of synchronized constructs").isEqualTo(1);

		// reload construct
		construct = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(Construct.class, globalId);
		});

		// assert existence and category
		Trx.operate(() -> {
			assertThat(construct).as("Synchronized construct").isNotNull();
			assertThat(construct.getConstructCategory()).as("Construct category").isEqualTo(category);
		});

		// reload category
		category = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(category);
		});

		// assert category names
		Trx.operate(() -> {
			try (LangTrx trx = new LangTrx("de")) {
				assertThat(category.getName().toString()).as("Category name").isEqualTo("Testkategorie");
			}
			try (LangTrx trx = new LangTrx("en")) {
				assertThat(category.getName().toString()).as("Category name").isEqualTo("Testcategory");
			}
		});
	}
}
