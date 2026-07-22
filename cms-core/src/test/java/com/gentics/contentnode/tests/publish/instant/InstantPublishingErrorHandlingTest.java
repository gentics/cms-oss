package com.gentics.contentnode.tests.publish.instant;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling unknown host errors during instant publishing
 */
@GCNFeature(set = {Feature.INSTANT_CR_PUBLISHING})
public class InstantPublishingErrorHandlingTest extends AbstractInstantPublishingTest {

	/**
	 * Test updating a node (actually the root folder)
	 * @throws NodeException
	 */
	@Test
	public void testUpdateNode() throws NodeException {
		NodeResourceImpl resource = new NodeResourceImpl();
		NodeLoadResponse load = resource.get(Integer.toString(node.getId()), false);
		assertResponseOK(load);
		com.gentics.contentnode.rest.model.Node restNode = load.getNode();

		restNode.setName("Modified Name");
		NodeSaveRequest save = new NodeSaveRequest();
		save.setNode(restNode);
		GenericResponse update = resource.update(Integer.toString(node.getId()), save);
		assertResponseOK(update);

		load = resource.get(Integer.toString(node.getId()), false);
		assertThat(load.getNode()).as("Updated node").hasFieldOrPropertyWithValue("name", "Modified Name");
	}

	/**
	 * Test creating a folder
	 * @throws NodeException
	 */
	@Test
	public void testCreateFolder() throws NodeException {
		FolderCreateRequest create = new FolderCreateRequest();
		create.setMotherId(Integer.toString(supply(() -> node.getFolder().getId())));
		create.setName("New Folder");

		FolderLoadResponse created = supply(() -> getFolderResource().create(create));
		assertResponseOK(created);

		FolderLoadResponse loaded = execute(
				id -> getFolderResource().load(Integer.toString(id), false, false, false, null, null),
				created.getFolder().getId());
		assertThat(loaded.getFolder()).as("Created folder").isNotNull().hasFieldOrPropertyWithValue("name", "New Folder");
	}

	/**
	 * Test updating a folder
	 * @throws NodeException
	 */
	@Test
	public void testUpdateFolder() throws NodeException {
		Folder folder = supply(t -> {
			t.setInstantPublishingEnabled(false);
			return create(Folder.class, c -> {
				c.setMotherId(node.getFolder().getId());
				c.setName("Test Folder");
			}).build();
		});

		FolderSaveRequest update = new FolderSaveRequest();
		com.gentics.contentnode.rest.model.Folder restFolder = new com.gentics.contentnode.rest.model.Folder();
		restFolder.setName("New Name");
		restFolder.setId(folder.getId());
		update.setFolder(restFolder);
		GenericResponse response = supply(() -> getFolderResource().save(Integer.toString(folder.getId()), update));
		assertResponseOK(response);

		FolderLoadResponse loaded = execute(
				id -> getFolderResource().load(Integer.toString(id), false, false, false, null, null), folder.getId());
		assertThat(loaded.getFolder()).as("Updated folder").isNotNull().hasFieldOrPropertyWithValue("name", "New Name");
	}

	/**
	 * Test deleting a folder
	 * @throws NodeException
	 */
	@Test
	public void setDeleteFolder() throws NodeException {
		Folder folder = supply(t -> {
			t.setInstantPublishingEnabled(false);
			return create(Folder.class, c -> {
				c.setMotherId(node.getFolder().getId());
				c.setName("Test Folder");
			}).build();
		});

		GenericResponse response = execute(id -> getFolderResource().delete(Integer.toString(id), null , null), folder.getId());
		assertResponseOK(response);

		folder = execute(f -> f.reload(), folder);
		assertThat(folder).as("Deleted folder").isNull();
	}

	/**
	 * Test publishing a page
	 * @throws NodeException
	 */
	@Test
	public void testPublishPage() throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
		}).build();

		GenericResponse response = execute(id -> getPageResource().publish(Integer.toString(id), null, new PagePublishRequest()), page.getId());
		assertResponseOK(response);

		page = execute(Page::reload, page);
		consume(p -> {
			assertThat(p).as("Published page").isOnline();
		}, page);
	}

	/**
	 * Test deleting a (published) page
	 * @throws NodeException
	 */
	@Test
	public void testDeletePage() throws NodeException {
		Page page = supply(t -> {
			t.setInstantPublishingEnabled(false);
			return create(Page.class, p -> {
				p.setFolderId(node.getFolder().getId());
				p.setTemplateId(template.getId());
			}).publish().build();
		});

		GenericResponse response = execute(id -> getPageResource().delete(Integer.toString(id), null, null), page.getId());
		assertResponseOK(response);

		page = execute(Page::reload, page);
		assertThat(page).as("Deleted Page").isNull();
	}
}
