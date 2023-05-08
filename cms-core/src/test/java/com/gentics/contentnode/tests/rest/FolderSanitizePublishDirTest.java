package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.request.FolderPublishDirSanitizeRequest;
import com.gentics.contentnode.rest.model.response.FolderPublishDirSanitizeResponse;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for sanitizing the folder publish directory
 */
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT })
@RunWith(value = Parameterized.class)
public class FolderSanitizePublishDirTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node withPubDirSegment;
	private static Node withoutPubDirSegment;
	private static UserGroup group;
	private static SystemUser user;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@Parameter(0)
	public String publishDir;

	@Parameter(1)
	public String expectedSanitizedSegment;

	@Parameter(2)
	public String expectedSanitizedPath;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		withPubDirSegment = supply(() -> update(createNode(), upd -> upd.setPubDirSegment(true)));
		withoutPubDirSegment = supply(() -> update(createNode(), upd -> upd.setPubDirSegment(false)));

		group = supply(() -> createUserGroup("Test", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("first", "last", null, "tester", "tester", Arrays.asList(group)));
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"bla bla", "bla-bla", "/bla-bla/"});
		data.add(new Object[] {"örks", "oerks", "/oerks/"});
		data.add(new Object[] {"/slash", "slash", "/slash/"});
		data.add(new Object[] {"/sla/shes", "sla-shes", "/sla/shes/"});
		return data;
	}

	/**
	 * Test in the node with pub dir segment enabled
	 * @throws NodeException
	 */
	@Test
	public void testSegment() throws NodeException {
		FolderPublishDirSanitizeResponse response = assertRequiredPermissions(group, user, () -> {
			FolderResourceImpl res = new FolderResourceImpl();
			res.setTransaction(TransactionManager.getCurrentTransaction());
			return res.sanitizePubdir(new FolderPublishDirSanitizeRequest().setPublishDir(publishDir)
					.setNodeId(withPubDirSegment.getId()));
		}, Triple.of(Node.TYPE_NODE, supply(() -> withPubDirSegment.getFolder().getId()), PermHandler.PERM_VIEW));
		assertThat(response.getPublishDir()).as("Sanitized segment").isEqualTo(expectedSanitizedSegment);
	}

	/**
	 * Test in the node with pub dir segment disabled
	 * @throws NodeException
	 */
	@Test
	public void testPath() throws NodeException {
		FolderPublishDirSanitizeResponse response = assertRequiredPermissions(group, user, () -> {
			FolderResourceImpl res = new FolderResourceImpl();
			res.setTransaction(TransactionManager.getCurrentTransaction());
			return res.sanitizePubdir(new FolderPublishDirSanitizeRequest().setPublishDir(publishDir)
					.setNodeId(withoutPubDirSegment.getId()));
		}, Triple.of(Node.TYPE_NODE, supply(() -> withoutPubDirSegment.getFolder().getId()), PermHandler.PERM_VIEW));
		assertThat(response.getPublishDir()).as("Sanitized path").isEqualTo(expectedSanitizedPath);
	}

	/**
	 * Test for an unknown node
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Der Node mit der ID 4711 konnte nicht gefunden werden.")
	public void testUnknown() throws NodeException {
		operate(t -> {
			FolderResourceImpl res = new FolderResourceImpl();
			res.setTransaction(TransactionManager.getCurrentTransaction());
			res.sanitizePubdir(new FolderPublishDirSanitizeRequest().setPublishDir(publishDir).setNodeId(4711));
		});
	}
}
