package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.testutils.GCNFeature;

import io.vertx.core.json.JsonArray;

/**
 * Test cases for setting permissions on roles
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.ATTRIBUTE_DIRTING })
@Category(MeshTest.class)
public class MeshPublishJSONRolesTest extends AbstractMeshPublishRoleTest {

	protected static ObjectTagDefinition templateRolesProperty;

	@BeforeClass
	public static void setupOnce() throws Exception {
		AbstractMeshPublishRoleTest.setupOnce();

		Construct jsonRolesConstruct = Trx.supply(() -> create(Construct.class, construct -> {
			construct.setAutoEnable(true);
			construct.setKeyword("roleConstruct");
			construct.setName("roles", 1);
			construct.getNodes().add(node);

			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(1);
				part.setHidden(false);
				part.setKeyname(TPL_PART_KEYWORD);
				part.setName("roles", 1);
				part.setPartTypeId(getPartTypeId(JSONPartType.class));
				part.setDefaultValue(create(Value.class, value -> {}, false));
			}, false));
		}));
		rolesProperty = createObjectPropertyDefinition(Folder.TYPE_FOLDER, rolesConstruct.getId(), "Roles", "roles");
		templateRolesProperty = createObjectPropertyDefinition(Folder.TYPE_FOLDER, jsonRolesConstruct.getId(), "VTLRoles", TPL_OBJECT_TAG_KEYWORD);
	}

	/**
	 * The the {@link ContentRepository#setPermissionProperty(String) permission property} of the Mesh Contentrepository
	 * to the {@code roles} field in the Velocity context of the object tag.
	 */
	@Override
	protected void setVelocityPermissionProperty() throws NodeException {
		Trx.operate(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, crId, true);

			cr.setPermissionProperty(String.format("object.%s.parts.%s", TPL_OBJECT_TAG_KEYWORD, TPL_PART_KEYWORD));
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
		JsonArray rolesArray = new JsonArray();

		for (String role : roles) {
			rolesArray.add(role);
		}

		update(objTag, tag -> {
			tag.setEnabled(true);

			getPartType(JSONPartType.class, tag, TPL_PART_KEYWORD)
				.getValueObject()
				.setValueText(rolesArray.encode());
		});
	}
}
