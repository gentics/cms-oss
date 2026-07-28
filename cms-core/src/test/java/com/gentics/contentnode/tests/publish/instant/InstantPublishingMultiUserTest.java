package com.gentics.contentnode.tests.publish.instant;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = {Feature.INSTANT_CR_PUBLISHING})
public class InstantPublishingMultiUserTest extends AbstractInstantPublishingTest {

	static SystemUser editor;
	static SystemUser publisher;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, NODE_GROUP_ID));

		editor = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("editoruser");
			u.setFirstname("Editor-First");
			u.setLastname("Editor-Last");
			u.setDescription("This is the editor user");
			u.getUserGroups().add(nodeGroup);
		}).build();

		editor = update(editor, c -> {
			c.setEmail("editor@no.where");
		}).build();

		publisher = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("publisheruser");
			u.setFirstname("Publisher-First");
			u.setLastname("Publisher-Last");
			u.setDescription("This is the publiser user");
			u.getUserGroups().add(nodeGroup);
		}).build();

		publisher = update(publisher, c -> {
			c.setEmail("publisher.where");
		}).build();

		AbstractInstantPublishingTest.setupOnce();
	}

	@Test
	public void testScheduleUserVsPublishingUser() throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
		}).unlock().build();

		page = update(page, p -> {}).publish().takeOffline((int) ((System.currentTimeMillis() / 1000L) + 3600)).as(editor).unlock()
				.build();

		consume(p -> {
			assertThat(p).as("Published page").isOnline();
			assertThat(p.getPublisher().getId()).isEqualTo(editor.getId());
		}, page);



		page = update(page, p -> {
			p.setName("Updated by " + publisher.getFullName());
		}).publish().as(publisher).unlock()
				.build();

		consume(p -> {
			assertThat(p).as("Published page").isOnline();
			assertThat(p.getPublisher().getId()).isEqualTo(publisher.getId());
		}, page);
	}
}
