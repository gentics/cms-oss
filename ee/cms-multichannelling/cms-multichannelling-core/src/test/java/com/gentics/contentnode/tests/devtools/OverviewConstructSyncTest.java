package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.factory.object.OverviewPartSetting.ObjectType;
import com.gentics.contentnode.factory.object.OverviewPartSetting.SelectionType;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Special test cases for synchronizing construct with overviews
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS, Feature.MULTICHANNELLING })
public class OverviewConstructSyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: new {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean newConstruct : Arrays.asList(true, false)) {
			data.add(new Object[] { newConstruct });
		}
		return data;
	}

	private static Node node;

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	@Parameter(0)
	public boolean newConstruct;

	private PackageSynchronizer pack;

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
	 * Test synchronization of restricted object types
	 * @throws NodeException
	 */
	@Test
	public void testRestrictedObjectTypes() throws NodeException {
		syncTestSetting(setting -> {
			setting.getRestrictedObjectTypes().add(ObjectType.page);
		}, setting -> {
			setting.getRestrictedObjectTypes().clear();
			setting.getRestrictedObjectTypes().add(ObjectType.file);
			setting.getRestrictedObjectTypes().add(ObjectType.image);
		}, setting -> {
			assertThat(setting.getRestrictedObjectTypes()).as("Restricted object types").containsOnly(ObjectType.page);
		});
	}

	/**
	 * Test synchronization of restricted selection types
	 * @throws NodeException
	 */
	@Test
	public void testRestrictedSelectionTypes() throws NodeException {
		syncTestSetting(setting -> {
			setting.getRestrictedSelectionTypes().clear();
			setting.getRestrictedSelectionTypes().add(SelectionType.single);
			setting.getRestrictedSelectionTypes().add(SelectionType.parent);
		}, setting -> {
			setting.getRestrictedSelectionTypes().clear();
			setting.getRestrictedSelectionTypes().add(SelectionType.folder);
		}, setting -> {
			assertThat(setting.getRestrictedSelectionTypes()).as("Restricted selection types").containsOnly(SelectionType.single, SelectionType.parent);
		});
	}

	/**
	 * Test synchronization of hideSortOption flag
	 * @throws NodeException
	 */
	@Test
	public void testHideSortOption() throws NodeException {
		syncTestSetting(setting -> {
			setting.setHideSortOption(true);
		}, setting -> {
			setting.setHideSortOption(false);
		}, setting -> {
			assertThat(setting.isHideSortOption()).as("Hide sort option").isTrue();
		});
	}

	/**
	 * Test synchronization of stickyChannel flag
	 * @throws NodeException
	 */
	@Test
	public void testStickyChannel() throws NodeException {
		syncTestSetting(setting -> {
			setting.setStickyChannel(true);
		}, setting -> {
			setting.setStickyChannel(false);
		}, setting -> {
			assertThat(setting.isStickyChannel()).as("Sticky channel").isTrue();
		});
	}

	/**
	 * Test synchronization of template
	 * @throws NodeException
	 */
	@Test
	public void testTemplate() throws NodeException {
		syncTestValue(value -> {
			value.setValueText("initial template");
		}, value -> {
			value.setValueText("changed template");
		}, value -> {
			assertThat(value.getValueText()).as("template").isEqualTo("initial template");
		});
	}

	/**
	 * Test synchronization of changeable flag
	 * @throws NodeException
	 */
	@Test
	public void testChangeableFlag() throws NodeException {
		syncTestValue(value -> {
			value.setInfo(1);
		}, value -> {
			value.setInfo(0);
		}, value -> {
			assertThat(value.getInfo()).as("changeable flag").isEqualTo(1);
		});
	}

	/**
	 * Internal synchronization method
	 * @param init consumer to initialize the part
	 * @param update consumer to update the part
	 * @param check consumer to perform assertions on the part
	 * @throws NodeException
	 */
	protected void syncTestPart(Consumer<Part> init, Consumer<Part> update, Consumer<Part> check) throws NodeException {
		// create construct
		construct = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(Construct.class, ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, "overview", "ds"));
		});
		GlobalId globalId = construct.getGlobalId();

		// set data
		construct = Trx.supply(() -> update(construct, c -> {
			init.accept(c.getParts().get(0));
		}));

		// add to package
		Trx.operate(() -> pack.synchronize(construct, true));

		// delete/update locally
		if (newConstruct) {
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				t.getObject(construct, true).delete(true);
			});
		} else {
			construct = Trx.supply(() -> update(construct, c -> {
				update.accept(c.getParts().get(0));
			}));
		}

		// sync from FS
		assertThat(Trx.supply(() -> pack.syncAllFromFilesystem(Construct.class))).as("number of synchronized constructs").isEqualTo(1);

		// reload construct
		construct = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(Construct.class, globalId);
		});

		// assert existence and overview setting
		Trx.operate(() -> {
			assertThat(construct).as("Synchronized construct").isNotNull();
			check.accept(construct.getParts().get(0));
		});
	}

	/**
	 * Internal synchronization method
	 * @param init consumer to initialize the overview setting
	 * @param update consumer to update the overview setting
	 * @param check consumer to perform assertions on the overview setting
	 * @throws NodeException
	 */
	protected void syncTestSetting(Consumer<OverviewPartSetting> init, Consumer<OverviewPartSetting> update, Consumer<OverviewPartSetting> check) throws NodeException {
		syncTestPart(part -> {
			OverviewPartSetting setting = new OverviewPartSetting(part);
			init.accept(setting);
			setting.setTo(part);
		}, part -> {
			OverviewPartSetting setting = new OverviewPartSetting(part);
			update.accept(setting);
			setting.setTo(part);
		}, part -> {
			OverviewPartSetting setting = new OverviewPartSetting(part);
			check.accept(setting);
		});
	}

	/**
	 * Internal synchronization method
	 * @param init consumer to initialize the default value
	 * @param update consumer to update the default value
	 * @param check consumer to perform assertions on the default value
	 * @throws NodeException
	 */
	protected void syncTestValue(Consumer<Value> init, Consumer<Value> update, Consumer<Value> check) throws NodeException {
		syncTestPart(part -> {
			init.accept(part.getDefaultValue());
		}, part -> {
			update.accept(part.getDefaultValue());
		}, part -> {
			check.accept(part.getDefaultValue());
		});
	}
}
