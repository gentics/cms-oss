package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

/**
 * Test cases for setting permissions on roles
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.ATTRIBUTE_DIRTING })
@Category(MeshTest.class)
@Ignore("GPU-2192")
public class MeshPublishRolesTest extends AbstractMeshPublishRoleTest {

	protected static ObjectTagDefinition templateRolesProperty;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws Exception {
		AbstractMeshPublishRoleTest.setupOnce();

//		int velocityRolesConstructId = Trx.supply(t -> ContentNodeTestDataUtils.createVelocityConstruct(node, "roleConstruct", TPL_PART_KEYWORD));
//		rolesProperty = createObjectPropertyDefinition(Folder.TYPE_FOLDER, rolesConstruct.getId(), "Roles", "roles");
//		templateRolesProperty = createObjectPropertyDefinition(Folder.TYPE_FOLDER, velocityRolesConstructId, "VTLRoles", TPL_OBJECT_TAG_KEYWORD);
	}

	/**
	 * The the {@link ContentRepository#setPermissionProperty(String) permission property} of the Mesh Contentrepository
	 * to the {@code roles} field in the Velocity context of the object tag.
	 */
	@Override
	protected void setVelocityPermissionProperty() throws NodeException {
		Trx.operate(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, crId, true);

			cr.setPermissionProperty(String.format("object.%s.parts.%s.%s", TPL_OBJECT_TAG_KEYWORD, TPL_PART_KEYWORD, TPL_ROLES_FIELD));
			cr.save();
		});
	}

	/**
	 * Set the given roles via the velocity object tag.
	 *
	 * @param container The container to set the roles for.
	 * @param roles The roles to set.
	 */
	@Override
	protected void setTemplateRoles(ObjectTagContainer container, String... roles) throws NodeException {
		ObjectTag objTag = container.getObjectTag(TPL_OBJECT_TAG_KEYWORD);
		StringBuilder valueText = new StringBuilder(String.format("#set($%s = [])\n", TPL_ROLES_FIELD));

		for (String role : roles) {
			valueText.append(String.format("$%s.add(\"%s\")\n", TPL_ROLES_FIELD, role));
		}

		update(objTag, tag -> {
			tag.setEnabled(true);

			getPartType(LongHTMLPartType.class, tag, "template")
				.getValueObject()
				.setValueText(valueText.toString());
		});
	}
}
