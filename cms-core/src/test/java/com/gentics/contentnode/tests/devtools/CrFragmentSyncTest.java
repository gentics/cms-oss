package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
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
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.DEVTOOLS })
public class CrFragmentSyncTest {
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
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	@Test
	public void testEntryMicronodeFilter() throws NodeException {
		Synchronizer.disable();

		boolean noIndex = (System.currentTimeMillis() % 2) == 0;

		CrFragment fragment = supply(() -> create(CrFragment.class, created -> {
			created.setName("Test CR");

			created.getEntries().add(create(CrFragmentEntry.class, fr -> {
				fr.setObjType(Folder.TYPE_FOLDER);
				fr.setAttributeTypeId(AttributeType.micronode.getType());
				fr.setMapname("target");
				fr.setTagname("source");
				fr.setMicronodeFilter("micronodefilter bla");
				fr.setNoIndex(noIndex);
			}, false));
		}));
		GlobalId globalId = supply(() -> fragment.getGlobalId());

		consume(f -> pack.synchronize(f, true), fragment);

		operate(() -> update(fragment, CrFragment::delete));

		operate(() -> assertThat(pack.syncAllFromFilesystem(CrFragment.class)).as("Number of synchronized fragments").isEqualTo(1));

		operate(t -> {
			CrFragment afterSync = t.getObject(CrFragment.class, globalId);
			assertThat(afterSync).as("Fragment after sync").isNotNull();
			assertThat(afterSync.getEntries().get(0)).as("Entry after sync")
				.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER)
				.hasFieldOrPropertyWithValue("attributeTypeId", AttributeType.micronode.getType())
				.hasFieldOrPropertyWithValue("mapname", "target")
				.hasFieldOrPropertyWithValue("tagname", "source")
				.hasFieldOrPropertyWithValue("noIndex", noIndex)
				.hasFieldOrPropertyWithValue("micronodeFilter", "micronodefilter bla");
		});
	}
}
