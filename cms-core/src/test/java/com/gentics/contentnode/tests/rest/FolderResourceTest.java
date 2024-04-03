package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests for checking publish dir restrictions when creating or updating folders
 * via the {@link com.gentics.contentnode.rest.resource.FolderResource}.
 */
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT })
public class FolderResourceTest {

	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(RESTAppContext.Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static Node node;
	private static Folder testFolder;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("test", "test", ContentNodeTestDataUtils.PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		node = supply(() -> update(node, upd -> upd.setPubDirSegment(true)));

		testFolder = supply(() -> createFolder(node.getFolder(), "Folder"));
	}
	@Test
	public void testCreateWithInvalidPubDirSegment() throws NodeException {
		FolderCreateRequest rq = new FolderCreateRequest();

		rq.setName("TestFolderEmptyPubDir");
		rq.setMotherId(testFolder.getId().toString());
		rq.setPublishDir("/");

		FolderLoadResponse rs = supply(() -> new FolderResourceImpl().create(rq));

		assertThat(rs.getResponseInfo().getResponseCode())
			.as("Response code")
			.isEqualTo(ResponseCode.INVALIDDATA);

		assertThat(rs.getResponseInfo().getProperty())
			.as("Invalid property")
			.isEqualTo("publishDir");
	}

	@Test
	public void testUpdateWithInvalidPubDirSegment() throws NodeException {
		FolderSaveRequest rq = new FolderSaveRequest();
		com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();

		folder.setId(testFolder.getId());
		folder.setPublishDir("/");
		rq.setFolder(folder);

		GenericResponse rs = supply(() -> new FolderResourceImpl().save(testFolder.getId().toString(), rq));

		assertThat(rs.getResponseInfo().getResponseCode())
			.as("Response code")
			.isEqualTo(ResponseCode.INVALIDDATA);

		assertThat(rs.getResponseInfo().getProperty())
			.as("Invalid property")
			.isEqualTo("publishDir");
	}

	@Test
	public void testUpdateRootFolderWithEmptyPubDirSegment() throws NodeException {
		Integer rootFolderId = execute(node -> node.getFolder().getId(), node);
		FolderSaveRequest rq = new FolderSaveRequest();
		com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();

		folder.setId(rootFolderId);
		folder.setPublishDir("");
		rq.setFolder(folder);

		GenericResponse rs = supply(() -> new FolderResourceImpl().save(node.getFolder().getId().toString(), rq));

		assertThat(rs.getResponseInfo().getResponseCode())
			.as("Response code")
			.isEqualTo(ResponseCode.OK);
	}

	@Test
	public void testUpdateRootFolderWithSlashPubDirSegment() throws NodeException {
		Integer rootFolderId = execute(node -> node.getFolder().getId(), node);
		FolderSaveRequest rq = new FolderSaveRequest();
		com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();

		folder.setId(rootFolderId);
		folder.setPublishDir("/");
		rq.setFolder(folder);

		GenericResponse rs = supply(() -> new FolderResourceImpl().save(node.getFolder().getId().toString(), rq));

		assertThat(rs.getResponseInfo().getResponseCode())
			.as("Response code")
			.isEqualTo(ResponseCode.OK);
	}
}
