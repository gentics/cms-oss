package com.gentics.contentnode.tests.etc;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test DB connection pool blocking behaviour
 */
public class ConnectionPoolDeadlockTest {
	/**
	 * Test context with extra configuration from pool_settings.yml:
	 * <pre>
	 * pool_whenExhaustedAction: BLOCK
	 * pool_size_max: 5
	 * pool_maxWait: 10000
	 * </pre>
	 */
	@ClassRule
	public static DBTestContext testContext = new DBTestContext()
			.config(ConnectionPoolDeadlockTest.class.getResource("pool_settings.yml").getFile());

	/**
	 * Number of parallel threads
	 */
	private final static int THREADS = 10;

	private static Node node;

	private static Template template;

	private static Set<Folder> folders = new HashSet<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		for (int i = 0; i < THREADS; i++) {
			String name = "Folder " + i;
			folders.add(supply(() -> createFolder(node.getFolder(), name)));
		}
	}

	/**
	 * Test that {@link #THREADS} parallel operations do not block for ever (because
	 * the pool_max_wait setting of 10 seconds lets the actions fail after this
	 * time). If the pool did not have the pool_max_wait setting, the operations
	 * would block for ever, because every operation borrows multiple connections.
	 *
	 * @throws NodeException
	 * @throws InterruptedException
	 */
	@Test
	public void test() throws NodeException, InterruptedException {
		ExecutorService service = Executors.newFixedThreadPool(THREADS);

		folders.stream().forEach(f -> {
			service.submit(() -> {
				String name = "Page in " + f.getName();
				try {
					operate(() -> createPage(f, template, name));
				} catch (NodeException e) {
					throw new RuntimeException();
				}
			});
		});

		service.shutdown();
		// wait until the operations finish (somehow) or the timeout occurs and assume that we did not run into the timeout
		assertThat(service.awaitTermination(1, TimeUnit.MINUTES)).as("Jobs finished").isTrue();
	}
}
