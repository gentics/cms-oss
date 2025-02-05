package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

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
import com.gentics.contentnode.devtools.AbstractSynchronizer;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.NodeObjectHandler;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class ObjectPropertiesSyncTest {
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

	private ObjectTagDefinition objectProperty;

	private ObjectTagDefinitionCategory category;

	private static Integer constructId;

	/**
	 * Get test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: type {0}, new {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int type : Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, Template.TYPE_TEMPLATE, ImageFile.TYPE_IMAGE, File.TYPE_FILE)) {
			for (boolean asNew : Arrays.asList(true, false)) {
				data.add(new Object[] { type, asNew });
			}
		}
		return data;
	}

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

	/**
	 * Tested object type
	 */
	@Parameter(0)
	public int type;

	/**
	 * Sync as new object
	 */
	@Parameter(1)
	public boolean asNew;

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
	 * Test synchronization of the object property category
	 * @throws NodeException
	 */
	@Test
	public void testCategory() throws NodeException {
		category = Trx.supply(() -> create(ObjectTagDefinitionCategory.class, cat -> {
			cat.setName("Testkategorie", 1);
			cat.setName("Testcategory", 2);
			cat.setSortorder(1);
		}));

		syncTest(objProp -> {
			objProp.setCategoryId(category.getId());
		}, objProp -> {
			objProp.setCategoryId(null);
		}, () -> {
			// change category name
			category = Trx.supply(() -> update(category, cat -> {
				cat.setName("Geänderte Testkategorie", 1);
				cat.setName("Modified Testcategory", 2);
			}));
		}, () -> {
			// assert existence and category
			Trx.operate(() -> {
				assertThat(objectProperty).as("Synchronized object property").isNotNull();
				assertThat(objectProperty.getCategory()).as("Object property category").isEqualTo(category);
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
		});
	}

	/**
	 * Test synchronization of the inheritable flag
	 * @throws NodeException
	 */
	@Test
	public void testInheritableFlag() throws NodeException {
		syncTest(objProp -> {
			objProp.getObjectTag().setInheritable(true);
		}, objProp -> {
			objProp.getObjectTag().setInheritable(false);
		}, () -> {
		}, () -> {
			// assert existence and inheritable flag
			Trx.operate(() -> {
				assertThat(objectProperty).as("Synchronized object property").isNotNull();
				// only object properties for folders can be inheritable
				assertThat(objectProperty.getObjectTag().isInheritable()).as("Object property inheritable flag").isEqualTo(type == Folder.TYPE_FOLDER);
			});
		});
	}

	/**
	 * Test synchronization of the required flag
	 * @throws NodeException
	 */
	@Test
	public void testRequiredFlag() throws NodeException {
		syncTest(objProp -> {
			objProp.getObjectTag().setRequired(true);
		}, objProp -> {
			objProp.getObjectTag().setRequired(false);
		}, () -> {
		}, () -> {
			// assert existence and inheritable flag
			Trx.operate(() -> {
				assertThat(objectProperty).as("Synchronized object property").isNotNull();
				assertThat(objectProperty.getObjectTag().isRequired()).as("Object property required flag").isEqualTo(true);
			});
		});
	}

	@Test
	public void testNodeAssignment() throws NodeException {
		syncTest(
			objProp -> node.addObjectTagDefinition(objProp),
			objProp -> node.removeObjectTagDefinition(objProp),
			() -> {},
			() -> {
				Trx.operate(() -> assertThat(objectProperty.getNodes())
					.as("Linked nodes")
					.containsExactly(node));
			}
		);
	}

	/**
	 * Test importing with no description
	 * @throws NodeException
	 */
	@Test
	public void testNoDescription() throws NodeException {
		syncTest(
			objProp -> {
				objProp.setDescription("", 1);
				objProp.setDescription("", 2);
			},
			objProp -> {
				objProp.setDescription("Beschreibung", 1);
				objProp.setDescription("Description", 2);
			},
			() -> {
				// remove the description from the file in the package
				java.io.File structureFile = Path.of(pack.getPackagePath().toString(), PackageSynchronizer.OBJECTPROPERTIES_DIR,
						String.format("%s.object.testoe", ObjectTagDefinitionTypeModel.fromValue(type).toString()),
						AbstractSynchronizer.STRUCTURE_FILE).toFile();
				assertThat(structureFile).exists().isFile();

				try {
					Map<?, ?> value = Synchronizer.mapper().readValue(structureFile, Map.class);
					value.remove("description");
					Synchronizer.mapper().writeValue(structureFile, value);
				} catch (IOException e) {
					fail("Modification of structure file failed", e);
				}
			},
			() -> {
				Trx.operate(() -> {
					assertThat(objectProperty).as("Synchronized object property").isNotNull();
					if (asNew) {
						assertThat(objectProperty.getDescription()).as("Object property description").isEmpty();
					} else {
						assertThat(objectProperty.getDescription()).as("Object property description").isEqualTo("Beschreibung");
					}
				});
			}
		);
	}

	/**
	 * Test importing with empty description
	 * @throws NodeException
	 */
	@Test
	public void testEmptyDescription() throws NodeException {
		syncTest(
			objProp -> {
				objProp.setDescription("", 1);
				objProp.setDescription("", 2);
			},
			objProp -> {
				objProp.setDescription("Beschreibung", 1);
				objProp.setDescription("Description", 2);
			},
			() -> { },
			() -> {
				Trx.operate(() -> {
					assertThat(objectProperty).as("Synchronized object property").isNotNull();
					assertThat(objectProperty.getDescription()).as("Object property description").isEmpty();
				});
			}
		);
	}

	/**
	 * Test importing with a non-empty description
	 * @throws NodeException
	 */
	@Test
	public void testNonEmptyDescription() throws NodeException {
		syncTest(
			objProp -> {
				objProp.setDescription("Beschreibung", 1);
				objProp.setDescription("Description", 2);
			},
			objProp -> {
				objProp.setDescription("", 1);
				objProp.setDescription("", 2);
			},
			() -> {},
			() -> {
				Trx.operate(() -> {
					assertThat(objectProperty).as("Synchronized object property").isNotNull();
					assertThat(objectProperty.getDescription()).as("Object property description").isEqualTo("Beschreibung");
				});
			}
		);
	}

	/**
	 * Do the sync test
	 * @param prepare prepare object property
	 * @param change handler to change the object property (if it is not sync'ed as new object)
	 * @param changeOther optional operator to change something after object property has been deleted (before sync from fs)
	 * @param asserter asserter after sync from fs
	 * @throws NodeException
	 */
	protected void syncTest(NodeObjectHandler<ObjectTagDefinition> prepare, NodeObjectHandler<ObjectTagDefinition> change, Operator changeOther, Operator asserter) throws NodeException {
		objectProperty = Trx.supply(() -> update(createObjectPropertyDefinition(type, constructId, "Test Object Property", "testoe"), prepare));
		GlobalId globalId = objectProperty.getGlobalId();

		// add to package
		Trx.operate(() -> pack.synchronize(objectProperty, true));

		if (asNew) {
			// delete object property
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				t.getObject(objectProperty, true).delete(true);
			});
		} else {
			objectProperty = Trx.supply(() -> update(objectProperty, change));
		}

		if (changeOther != null) {
			changeOther.operate();
		}

		// sync from FS
		assertThat(Trx.supply(() -> pack.syncAllFromFilesystem(ObjectTagDefinition.class))).as("number of synchronized object properties").isEqualTo(1);

		// reload object property
		objectProperty = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObject(ObjectTagDefinition.class, globalId);
		});

		asserter.operate();
	}
}
