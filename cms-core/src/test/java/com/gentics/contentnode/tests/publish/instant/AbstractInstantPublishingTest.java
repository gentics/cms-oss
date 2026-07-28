package com.gentics.contentnode.tests.publish.instant;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

public abstract class AbstractInstantPublishingTest {
	@ClassRule
	public final static DBTestContext testContext = new DBTestContext();
	protected static Node node;
	protected static ContentRepository cr;
	protected static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		int crId = createMeshCR("invalid.host", 4711, "bla");

		cr = supply(t -> t.getObject(ContentRepository.class, crId));

		node = update(node, n -> {
			n.setContentrepositoryId(crId);
		}).build();

		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		template = create(Template.class, t -> {
			t.setName("Template");
			t.setMlId(1);
			t.setFolderId(node.getFolder().getId());
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		operate(t -> {
			t.setInstantPublishingEnabled(false);
			clear(node);
		});

		update(supply(() -> node.getFolder()), upd -> {
			upd.setName("Node");
		}).build();
	}
}
