package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.GCNFeature;

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
}
