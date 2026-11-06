package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Tests for creating folders
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class FolderCreateTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public final static int THREADS = 10;

	/**
	 * Tested node
	 */
	private static Node node;

	private static Node channelSiblingA;
	private static Node channelSiblingB;

	/**
	 * Create static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setup() throws NodeException {
		node = Trx.supply(() -> createNode());
		channelSiblingA = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "channelA", "channelhostA", "/channelpubA"));
		channelSiblingB = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "channelB", "channelhostB", "/channelpubB"));
	}

	/**
	 * Clean data created by previous test runs
	 * @throws NodeException
	 */
	@Before
	public void cleanData() throws NodeException {
		Trx.operate(() -> {
			for (Folder child : node.getFolder().getChildFolders()) {
				child.delete(true);
			}
		});
	}

	/**
	 * Test that naming uniqueness is maintained when creating folders in parallel
	 * @throws NodeException
	 * @throws InterruptedException
	 */
	@Test
	public void testParallelNamingUniqueness() throws NodeException, InterruptedException {
		ExecutorService service = Executors.newCachedThreadPool();
		AtomicInteger threadsFailed = new AtomicInteger(0);

		FolderCreateRequest request = new FolderCreateRequest();
		request.setMotherId(Trx.supply(() -> String.valueOf(node.getFolder().getId())));
		request.setName("Test Folder Name");

		try {
			for (int i = 0; i < THREADS; i++) {
				service.execute(() -> {
					try {
						ContentNodeHelper.setLanguageId(1);
						FolderLoadResponse response = getFolderResource().create(request);
						if (response.getResponseInfo().getResponseCode() != ResponseCode.OK) {
							threadsFailed.incrementAndGet();
						}
					} catch (NodeException e) {
						threadsFailed.incrementAndGet();
					}
				});
			}
		} finally {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.MINUTES);
		}

		assertThat(threadsFailed.get()).as("Number of failed creation requests").isEqualTo(0);

		List<String> createdFolderNames = Trx.supply(() -> node.getFolder().getChildFolders().stream().map(Folder::getName).collect(Collectors.toList()));
		assertThat(createdFolderNames).as("Folder names").hasSize(THREADS).doesNotHaveDuplicates();
	}

	@Test
	public void testCreateInChannel() throws NodeException {
		String conflictingName = "test";
		Folder folderA = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(channelSiblingA.getFolder(), conflictingName, channelSiblingA));
		FolderCreateRequest request = new FolderCreateRequest();

		request.setName(conflictingName);
		request.setFailOnDuplicate(true);
		request.setMotherId(Trx.supply(() -> String.valueOf(node.getFolder().getId())));
		request.setNodeId(channelSiblingB.getId());

		FolderLoadResponse response = Trx.supply(() -> getFolderResource().create(request));

		ContentNodeRESTUtils.assertResponseOK(response);
	}
}
