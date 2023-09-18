package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.db.DBUtils.IDLIST;
import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
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
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for synchronization of CR's
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class ContentRepositorySyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected PackageSynchronizer pack;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();

		// delete all CRs and nodes
		operate(t -> {
			for (ContentRepository contentRepository : t.getObjects(ContentRepository.class,
					select("SELECT id FROM contentrepository", IDLIST))) {
				contentRepository.delete(true);
			}
		});
		operate(t -> {
			for (Node node : t.getObjects(Node.class, select("SELECT id FROM node", IDLIST))) {
				node.delete(true);
			}
		});
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	@Test
	public void testAddCRWithAssignedNode() throws NodeException {
		Synchronizer.disable();

		// create a node
		Node node = supply(() -> createNode());

		// assign the node to the package
		consume(n -> Synchronizer.addPackage(n, PACKAGE_NAME), node);

		// create a CR
		ContentRepository cr = supply(() -> createContentRepository("Test CR", false, false, "url"));
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// reload node
		node = execute(Node::reload, node);

		// check whether CR is assigned to node now and still has an editor
		try (Trx trx = new Trx()) {
			assertThat(node.getContentRepository()).as("Content Repository of Node").isEqualTo(cr);
			assertThat(node.getEditor()).as("Last editor of node").isNotNull();
			trx.success();
		}
	}

	/**
	 * Test that the username is not synchronized, if not configured as property
	 * @throws NodeException
	 */
	@Test
	public void testNotSynchronizeUsername() throws NodeException {
		Synchronizer.disable();

		// create a CR
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("Test CR");
			create.setUsername("username");
		}).build();
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// change the username
		cr = update(cr, update -> {
			update.setUsername("updated_username");
		}).build();

		// synchronize from the FS -> CMS
		assertResponseOK(new PackageResourceImpl().synchronizeFromFS(PACKAGE_NAME, 0));

		// assert that the username is unchanged
		cr = execute(ContentRepository::reload, cr);
		assertThat(execute(ContentRepository::getUsername, cr)).as("Username").isEqualTo("updated_username");
	}

	/**
	 * Test that the username is synchronized, if configured as property
	 * @throws NodeException
	 */
	@Test
	public void testSynchronizeUsernameProperty() throws NodeException {
		Synchronizer.disable();

		// create a CR
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("Test CR");
			create.setUsername("${sys:cr.username}");
		}).build();
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// change the username
		cr = update(cr, update -> {
			update.setUsername("updated_username");
		}).build();

		// synchronize from the FS -> CMS
		assertResponseOK(new PackageResourceImpl().synchronizeFromFS(PACKAGE_NAME, 0));

		// assert that the username is changed
		cr = execute(ContentRepository::reload, cr);
		assertThat(execute(ContentRepository::getUsername, cr)).as("Username").isEqualTo("${sys:cr.username}");
	}

	/**
	 * Test that the password is not synchronized
	 * @throws NodeException
	 */
	@Test
	public void testNotSynchronizePassword() throws NodeException {
		Synchronizer.disable();

		// create a CR
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("Test CR");
			create.setPassword("password");
		}).build();
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// change the password
		cr = update(cr, update -> {
			update.setPassword("updated_password");
		}).build();

		// synchronize from the FS -> CMS
		assertResponseOK(new PackageResourceImpl().synchronizeFromFS(PACKAGE_NAME, 0));

		// assert that the password is unchanged
		cr = execute(ContentRepository::reload, cr);
		assertThat(execute(ContentRepository::getPassword, cr)).as("Password").isEqualTo("updated_password");
	}

	/**
	 * Test that the password property is synchronized
	 * @throws NodeException
	 */
	@Test
	public void testSynchronizePasswordProperty() throws NodeException {
		Synchronizer.disable();

		// create a CR
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("Test CR");
			create.setPasswordProperty(true);
			create.setPassword("${sys:cr.password}");
		}).build();
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// change the password property
		cr = update(cr, update -> {
			update.setPasswordProperty(false);
			update.setPassword("updated_password");
		}).build();

		// synchronize from the FS -> CMS
		assertResponseOK(new PackageResourceImpl().synchronizeFromFS(PACKAGE_NAME, 0));

		// assert that the password property is changed
		cr = execute(ContentRepository::reload, cr);
		assertThat(execute(ContentRepository::getPassword, cr)).as("Password").isEqualTo("${sys:cr.password}");
		assertThat(execute(ContentRepository::isPasswordProperty, cr)).as("Password is Property").isTrue();
	}

	/**
	 * Test that the url is not synchronized, if not configured as property
	 * @throws NodeException
	 */
	@Test
	public void testNotSynchronizeUrl() throws NodeException {
		Synchronizer.disable();

		// create a CR
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("Test CR");
			create.setUrl("url");
		}).build();
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// change the url
		cr = update(cr, update -> {
			update.setUrl("updated_url");
		}).build();

		// synchronize from the FS -> CMS
		assertResponseOK(new PackageResourceImpl().synchronizeFromFS(PACKAGE_NAME, 0));

		// assert that the url is unchanged
		cr = execute(ContentRepository::reload, cr);
		assertThat(execute(ContentRepository::getUrl, cr)).as("Url").isEqualTo("updated_url");
	}

	/**
	 * Test that the url is synchronized, if configured as property
	 * @throws NodeException
	 */
	@Test
	public void testSynchronizeUrlProperty() throws NodeException {
		Synchronizer.disable();

		// create a CR
		ContentRepository cr = create(ContentRepository.class, create -> {
			create.setName("Test CR");
			create.setUrl("${sys:cr.url}");
		}).build();
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// change the url
		cr = update(cr, update -> {
			update.setUrl("updated_url");
		}).build();

		// synchronize from the FS -> CMS
		assertResponseOK(new PackageResourceImpl().synchronizeFromFS(PACKAGE_NAME, 0));

		// assert that the url is changed
		cr = execute(ContentRepository::reload, cr);
		assertThat(execute(ContentRepository::getUrl, cr)).as("Url").isEqualTo("${sys:cr.url}");
	}
}
