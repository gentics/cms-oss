package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for saving node pubDir over the REST API
 */
@RunWith(value = Parameterized.class)
public class NodePubDirSaveTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Initial pubDir
	 */
	public final static String INITIAL_PUBDIR = "/Content.Node";

	/**
	 * Tested node
	 */
	protected static Node node;

	@BeforeClass
	public static void setup() throws Exception {
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode();
			trx.success();
		}
	}

	@Parameters(name = "{index}: pubDir [{0}] -> [{1}]")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] { null, INITIAL_PUBDIR });
		data.add(new Object[] { "", "" });
		data.add(new Object[] { "/", "" });
		data.add(new Object[] { "/leading/but/not/trailing", "/leading/but/not/trailing" });
		data.add(new Object[] { "/leading/and/trailing/", "/leading/and/trailing" });
		data.add(new Object[] { "not/leading/but/trailing/", "/not/leading/but/trailing" });
		data.add(new Object[] { "not/leading/and/not/trailing", "/not/leading/and/not/trailing" });
		data.add(new Object[] { "noslash", "/noslash" });
		return data;
	}

	/**
	 * PubDir that is set
	 */
	@Parameter(0)
	public String pubDir;

	/**
	 * Expected PubDir
	 */
	@Parameter(1)
	public String expected;

	@Test
	public void testSet() throws NodeException {
		// reset node
		try (Trx trx = new Trx()) {
			Node editable = trx.getTransaction().getObject(node, true);
			editable.setUtf8(true);
			editable.setPublishDir(INITIAL_PUBDIR);
			editable.save();
			trx.success();
		}

		// set via REST
		try (Trx trx = new Trx()) {
			NodeResourceImpl resource = new NodeResourceImpl();
			NodeSaveRequest request = new NodeSaveRequest();
			com.gentics.contentnode.rest.model.Node restNode = new com.gentics.contentnode.rest.model.Node();
			restNode.setPublishDir(pubDir);
			request.setNode(restNode);
			ContentNodeRESTUtils.assertResponseOK(resource.update(node.getId().toString(), request));
			trx.success();
		}

		// assert data
		try (Trx trx = new Trx()) {
			node = trx.getTransaction().getObject(node);
			assertEquals("Check pubDir", expected, node.getPublishDir());
			trx.success();
		}
	}
}
