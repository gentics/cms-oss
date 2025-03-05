package com.gentics.contentnode.tests.devtools;

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
import com.gentics.contentnode.object.*;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.tests.utils.NodeObjectHandler;
import com.gentics.contentnode.testutils.GCNFeature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class ObjectPropertiesSyncTest extends AbstractObjectPropertiesSyncTest {
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

		syncTest(type, objProp -> {
			objProp.setCategoryId(category.getId());
		}, asNew, objProp -> {
			objProp.setCategoryId(null);
		}, () -> {
			// change category name
			category = Trx.supply(() -> update(category, cat -> {
				cat.setName("Geänderte Testkategorie", 1);
				cat.setName("Modified Testcategory", 2);
			}));
		}, null, () -> {
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
		syncTest(type, objProp -> {
			objProp.getObjectTag().setInheritable(true);
		}, asNew, objProp -> {
			objProp.getObjectTag().setInheritable(false);
		}, () -> {
		}, null, () -> {
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
		syncTest(type, objProp -> {
			objProp.getObjectTag().setRequired(true);
		}, asNew, objProp -> {
			objProp.getObjectTag().setRequired(false);
		}, () -> {
		}, null, () -> {
			// assert existence and inheritable flag
			Trx.operate(() -> {
				assertThat(objectProperty).as("Synchronized object property").isNotNull();
				assertThat(objectProperty.getObjectTag().isRequired()).as("Object property required flag").isEqualTo(true);
			});
		});
	}

	@Test
	public void testNodeAssignment() throws NodeException {
		syncTest(type,
			objProp -> node.addObjectTagDefinition(objProp),
			asNew,
			objProp -> node.removeObjectTagDefinition(objProp),
			() -> {},
			null, () -> {
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
