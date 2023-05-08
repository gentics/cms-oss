package com.gentics.contentnode.tests.publish.cr.fragment;

import static com.gentics.contentnode.perm.PermHandler.PERM_CHANGE_PERM;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_CREATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_DELETE;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.publish.cr.fragment.CrFragmentTestUtils.assertEntries;
import static com.gentics.contentnode.tests.publish.cr.fragment.CrFragmentTestUtils.getReferenceEntry;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for Cr Fragments
 */
public class CrFragmentTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static UserGroup nodeGroup;

	private static UserGroup testGroup;

	private static SystemUser testUser;

	/**
	 * Setup common test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		nodeGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		testGroup = Trx.supply(() -> Creator.createUsergroup("Testgroup", "", nodeGroup));
		testUser = Trx.supply(() -> Creator.createUser("test", "test", "tester", "tester", "", Arrays.asList(testGroup)));
	}

	/**
	 * Clear data
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM cr_fragment", null));
	}

	/**
	 * Test creating a CR Fragment
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
		}));

		assertThat(created).as("Created Fragment").isNotNull().has(attribute("name", "Test Fragment"));
	}

	/**
	 * Test whether initial permissions are set correctly
	 * @throws NodeException
	 */
	@Test
	public void testInitialPerm() throws NodeException {
		Trx.operate(() -> {
			setPermissions(CrFragment.TYPE_CR_FRAGMENTS, Arrays.asList(nodeGroup, testGroup),
					Permissions.get(PERM_VIEW, PERM_CONTENTREPOSITORY_CREATE).toString());
		});
		Session session = Trx.supply(() -> new Session(testUser, "", "", null, 0));
		CrFragment created = null;
		try (Trx trx = new Trx(session, true)) {
			created = create(CrFragment.class, f -> {
				f.setName("Test Fragment");
			});
		}

		Set<Integer> expectedPermBits = new HashSet<Integer>(Arrays.asList(PERM_VIEW, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE));
		Trx.consume(c -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (UserGroup group : Arrays.asList(nodeGroup, testGroup)) {
				PermHandler groupPermHandler = t.getGroupPermHandler(group.getId());
				for (int bit = 0; bit < 32; bit++) {
					assertThat(groupPermHandler.checkPermissionBit(CrFragment.TYPE_CR_FRAGMENT, c.getId(), bit))
							.as(String.format("Permission bit %d for group %s", bit, group)).isEqualTo(expectedPermBits.contains(bit));
				}
			}
		}, created);
	}

	/**
	 * Test whether initial permissions are set correctly, with grant option
	 * @throws NodeException
	 */
	@Test
	public void testInitialPermGranting() throws NodeException {
		Trx.operate(() -> {
			setPermissions(CrFragment.TYPE_CR_FRAGMENTS, Arrays.asList(nodeGroup, testGroup),
					Permissions.get(PERM_VIEW, PERM_CONTENTREPOSITORY_CREATE, PERM_CHANGE_PERM).toString());
		});
		Session session = Trx.supply(() -> new Session(testUser, "", "", null, 0));
		CrFragment created = null;
		try (Trx trx = new Trx(session, true)) {
			created = create(CrFragment.class, f -> {
				f.setName("Test Fragment");
			});
		}

		Set<Integer> expectedPermBits = new HashSet<Integer>(
				Arrays.asList(PERM_VIEW, PERM_CHANGE_PERM, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE));
		Trx.consume(c -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (UserGroup group : Arrays.asList(nodeGroup, testGroup)) {
				PermHandler groupPermHandler = t.getGroupPermHandler(group.getId());
				for (int bit = 0; bit < 32; bit++) {
					assertThat(groupPermHandler.checkPermissionBit(CrFragment.TYPE_CR_FRAGMENT, c.getId(), bit))
							.as(String.format("Permission bit %d for group %s", bit, group)).isEqualTo(expectedPermBits.contains(bit));
				}
			}
		}, created);
	}

	/**
	 * Test loading a CR fragment
	 * @throws NodeException
	 */
	@Test
	public void testLoad() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
		}));

		CrFragment loadedWithLocalId = Trx.supply(t -> t.getObject(CrFragment.class, created.getId()));
		assertThat(loadedWithLocalId.getModel()).as("Fragment loaded with local ID").isNotNull().isEqualToComparingFieldByField(created.getModel());

		CrFragment loadedWithGlobalId = Trx.supply(t -> t.getObject(CrFragment.class, created.getGlobalId()));
		assertThat(loadedWithGlobalId.getModel()).as("Fragment loaded with global ID").isNotNull().isEqualToComparingFieldByField(created.getModel());
	}

	/**
	 * Test updating a CR Fragment
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
		}));

		CrFragment updated = Trx.execute(o -> update(o, u -> u.setName("Updated Test Fragment")), created);
		assertThat(updated).as("Updated Fragment").isNotNull().has(attribute("name", "Updated Test Fragment"));
	}

	/**
	 * Test deleting a CR Fragment
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
		}));

		Trx.consume(o -> o.delete(), created);

		CrFragment deleted = Trx.supply(t -> t.getObject(created));
		assertThat(deleted).as("Deleted CrFragment").isNull();
	}

	/**
	 * Test creating a CR fragment entry
	 * @throws NodeException
	 */
	@Test
	public void testCreateEntry() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
			f.getEntries().add(create(CrFragmentEntry.class, e -> {
				e.setObjType(Folder.TYPE_FOLDER);
				e.setAttributeTypeId(AttributeType.text.getType());
				e.setMapname("target");
				e.setTagname("source");
			}, false));
		}));

		assertEntries(created, getReferenceEntry("created_entry.json"));
	}

	/**
	 * Test creating a CR fragment entry with elasticsearch config
	 * @throws NodeException
	 */
	@Test
	public void testCreateEntryWithElasticsearch() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
			f.getEntries().add(create(CrFragmentEntry.class, e -> {
				e.setObjType(Folder.TYPE_FOLDER);
				e.setAttributeTypeId(AttributeType.text.getType());
				e.setMapname("target");
				e.setTagname("source");
				e.setElasticsearch("{\"bla\":true}");
			}, false));
		}));

		assertEntries(created, getReferenceEntry("created_entry_elasticsearch.json"));
	}

	/**
	 * Test updating a CR fragment entry
	 * @throws NodeException
	 */
	@Test
	public void testUpdateEntry() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
			f.getEntries().add(create(CrFragmentEntry.class, e -> {
				e.setObjType(Folder.TYPE_FOLDER);
				e.setAttributeTypeId(AttributeType.text.getType());
				e.setMapname("target");
				e.setTagname("source");
			}, false));
		}));

		created = Trx.execute(o -> update(o, up -> {
			CrFragmentEntry entry = up.getEntries().get(0);
			entry.setMapname("target_new");
			entry.setTagname("source_new");
			entry.setOptimized(true);
			entry.setMultivalue(true);
		}), created);

		assertEntries(created, getReferenceEntry("updated_entry.json"));
	}

	/**
	 * Test deleting a CR fragment entry
	 * @throws NodeException
	 */
	@Test
	public void testDeleteEntry() throws NodeException {
		CrFragment created = Trx.supply(() -> create(CrFragment.class, f -> {
			f.setName("Test Fragment");
			f.getEntries().add(create(CrFragmentEntry.class, e -> {
				e.setObjType(Folder.TYPE_FOLDER);
				e.setAttributeTypeId(AttributeType.text.getType());
				e.setMapname("target");
				e.setTagname("source");
			}, false));
		}));

		created = Trx.execute(o -> update(o, up -> {
			up.getEntries().remove(0);
		}), created);

		List<CrFragmentEntry> entries = Trx.execute(c -> c.getEntries(), created);
		assertThat(entries).as("Fragment entries").isEmpty();
	}
}
