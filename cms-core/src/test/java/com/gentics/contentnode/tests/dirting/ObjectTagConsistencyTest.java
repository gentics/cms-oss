package com.gentics.contentnode.tests.dirting;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ObjectPropertyResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import java.io.IOException;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;

public class ObjectTagConsistencyTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	final String TAG_NAME = "copyright_tag";

	@Before
	public void setupOnce() throws NodeException, IOException {
		testContext.startTransaction(1);
		node = supply(() -> createNode());
	}

	@Test
	public void testFolderTagPropertiesShouldBeConsistentAfterUpdate() throws NodeException {
		final int CONSTRUCT_ID = supply(() -> createConstruct(node, HTMLPartType.class, "html", "html"));
		ObjectTagDefinition tagDefinition =  supply(() -> createObjectTagDefinition(TAG_NAME, Folder.TYPE_FOLDER, CONSTRUCT_ID));

		Folder folder1 = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder1"));
		Folder folder2 = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder2"));

		ObjectProperty changedObjectProperty = changeObjectTagDefinitionProperty(folder1.getObjectTag(TAG_NAME));

		final String TAG_DEFINITION_ID = Integer.toString(tagDefinition.getObjectTag().getId());
		supply(() -> new ObjectPropertyResourceImpl().update(TAG_DEFINITION_ID, changedObjectProperty));

		com.gentics.contentnode.rest.model.ObjectTag retrievedTagFolder1 = extractTagFromResponse(folder1);
		com.gentics.contentnode.rest.model.ObjectTag retrievedTagFolder2 = extractTagFromResponse(folder2);

		assertThat(retrievedTagFolder1.getRequired()).isTrue();
		assertThat(retrievedTagFolder1.getInheritable()).isTrue();

		assertThat(retrievedTagFolder2.getRequired()).isTrue();
		assertThat(retrievedTagFolder2.getInheritable()).isTrue();
	}

	private com.gentics.contentnode.rest.model.ObjectTag extractTagFromResponse(Folder folder) throws NodeException {
		FolderLoadResponse restFolder = supply(() -> new FolderResourceImpl().load(
				folder.getId().toString(),
				false,
				false,
				true,
				node.getId(),
				null
		));

		return (com.gentics.contentnode.rest.model.ObjectTag) restFolder.getFolder()
				.getTags()
				.get(String.format("object.%s", TAG_NAME));
	}

	private ObjectProperty changeObjectTagDefinitionProperty(ObjectTag objectTag) throws NodeException {
		ObjectProperty objectProperty = new ObjectProperty();
		objectProperty.setName(objectProperty.getName());
		objectProperty.setRequired(true);
		objectProperty.setInheritable(true);
		objectProperty.setConstructId(objectTag.getConstructId());
		objectProperty.setType(objectTag.getObjType());

		return objectProperty;
	}


}
