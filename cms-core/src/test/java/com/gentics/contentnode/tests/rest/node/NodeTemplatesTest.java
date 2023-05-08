package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.PagedTemplateListResponse;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for handling template assignment to nodes
 */
public class NodeTemplatesTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node testNode;
	private static Folder testFolder;
	private static Template templateA;
	private static Template templateB;
	private static Template templateC;

	/**
	 * Create test node, folder and templates
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		testNode = supply(() -> createNode());
		testFolder = supply(() -> createFolder(testNode.getFolder(), "Folder"));

		Node node = supply(() -> createNode());
		templateA = supply(() -> createTemplate(node.getFolder(), "A"));
		templateB = supply(() -> createTemplate(node.getFolder(), "b"));
		templateC = supply(() -> createTemplate(node.getFolder(), "[c]"));
	}

	/**
	 * Remove assignments of templates to test node and folder
	 * @throws NodeException
	 * @throws PortalCacheException
	 */
	@Before
	public void setup() throws NodeException, PortalCacheException {
		operate(t -> {
			DBUtils.executeUpdate("DELETE FROM template_node WHERE node_id = ?", new Object[] {testNode.getId()});
			DBUtils.executeUpdate("DELETE FROM template_folder wHERE folder_id IN (?, ?)", new Object[] {testNode.getFolder().getId(), testFolder.getId()});
		});
		ContentNodeTestUtils.clearNodeObjectCache();

		testNode = execute(NodeObject::reload, testNode);
		testFolder = execute(NodeObject::reload, testFolder);
		templateA = execute(NodeObject::reload, templateA);
		templateB = execute(NodeObject::reload, templateB);
		templateC = execute(NodeObject::reload, templateC);
	}

	/**
	 * Test sorting of templates assigned to node
	 * @throws Exception
	 */
	@Test
	public void testSortNodeTemplates() throws Exception {
		try (Trx trx = new Trx()) {
			assertResponseCodeOk(getNodeResource().addTemplate(String.valueOf(testNode.getId()), String.valueOf(templateA.getId())));
			assertResponseCodeOk(getNodeResource().addTemplate(String.valueOf(testNode.getId()), String.valueOf(templateB.getId())));
			assertResponseCodeOk(getNodeResource().addTemplate(String.valueOf(testNode.getId()), String.valueOf(templateC.getId())));
			trx.success();
		}

		com.gentics.contentnode.rest.model.Template templateAModel = supply(() -> Template.TRANSFORM2REST.apply(templateA));
		com.gentics.contentnode.rest.model.Template templateBModel = supply(() -> Template.TRANSFORM2REST.apply(templateB));
		com.gentics.contentnode.rest.model.Template templateCModel = supply(() -> Template.TRANSFORM2REST.apply(templateC));

		try (Trx trx = new Trx()) {
			PagedTemplateListResponse templatesResponse = getNodeResource().getTemplates(String.valueOf(testNode.getId()), null, new SortParameterBean(), null, null);
			assertResponseCodeOk(templatesResponse);

			assertThat(templatesResponse.getItems()).as("Sorted templates of node").containsExactly(templateAModel, templateBModel, templateCModel);
			trx.success();
		}
	}
}
