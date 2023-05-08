package com.gentics.contentnode.tests.etc;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for deadlock handling
 */
public class DeadlockTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Folder folder1;
	private static Folder folder2;

	private ExecutorService service;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		folder1 = supply(() -> createFolder(node.getFolder(), "Folder 1"));
		folder2 = supply(() -> createFolder(node.getFolder(), "Folder 2"));
	}

	@Before
	public void setup() {
		service = Executors.newCachedThreadPool();
	}

	/**
	 * Provoke a deadlock by submitting two tasks to the executor service, where each task updates one folder, then waits
	 * until the count down latch reaches zero, and then tries to update the other folder. One of the tasks is expected to
	 * fail due to the deadlock
	 * @throws NodeException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testDeadlock() throws NodeException, InterruptedException, ExecutionException {
		CountDownLatch latch = new CountDownLatch(2);

		Future<?> firstFuture = executeWithRetries(t -> {
			// update first folder
			updateFolderName(t, folder1.getId(), "Folder 1 Updated from first");

			// wait some time
			try {
				latch.countDown();
				latch.await(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				throw new NodeException(e);
			}

			// try to update second folder
			updateFolderName(t, folder2.getId(), "Folder 2 Updated from first");
		}, 0);

		Future<?> secondFuture = executeWithRetries(t -> {
			// update second folder
			updateFolderName(t, folder2.getId(), "Folder 2 Updated from second");

			// wait some time
			try {
				latch.countDown();
				latch.await(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				throw new NodeException(e);
			}

			// try to update first folder
			updateFolderName(t, folder1.getId(), "Folder 1 Updated from second");
		}, 0);

		// wait for all tasks to complete
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);

		// get the "results" (which will be null) to check that at least one task failed
		try {
			firstFuture.get();
			secondFuture.get();
			fail("One Task was expected to fail due to the deadlock");
		} catch (ExecutionException expected) {
		}

		// check the in the end, one transaction "won" (updated both folders)
		folder1 = execute(Folder::reload, folder1);
		folder2 = execute(Folder::reload, folder2);

		if ("Folder 1 Updated from first".equals(folder1.getName())) {
			assertThat(folder2).hasFieldOrPropertyWithValue("name", "Folder 2 Updated from first");
		} else if ("Folder 1 Updated from second".equals(folder1.getName())) {
			assertThat(folder2).hasFieldOrPropertyWithValue("name", "Folder 2 Updated from second");
		} else {
			fail(String.format("Folder 1 has unexpected name %s", folder1.getName()));
		}
	}

	/**
	 * Provoke a deadlock by submitting two tasks to the executor service, where each task updates one folder, then waits
	 * until the count down latch reaches zero, and then tries to update the other folder. This time, the transaction, which
	 * is rolled back due to the deadlock is retried, so we expect all tasks to succeed.
	 * @throws NodeException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testDeadlockWithRetry() throws NodeException, InterruptedException, ExecutionException {
		CountDownLatch latch = new CountDownLatch(2);

		Future<?> firstFuture = executeWithRetries(t -> {
			// update first folder
			updateFolderName(t, folder1.getId(), "Folder 1 Updated2 from first");

			// wait some time
			try {
				latch.countDown();
				latch.await(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				throw new NodeException(e);
			}

			// try to update second folder
			updateFolderName(t, folder2.getId(), "Folder 2 Updated2 from first");
		}, 1);

		Future<?> secondFuture = executeWithRetries(t -> {
			// update second folder
			updateFolderName(t, folder2.getId(), "Folder 2 Updated2 from second");

			// wait some time
			try {
				latch.countDown();
				latch.await(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				throw new NodeException(e);
			}

			// try to update first folder
			updateFolderName(t, folder1.getId(), "Folder 1 Updated2 from second");
		}, 1);

		// wait for all tasks to complete
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);

		// get the "results" (which will be null) to check that this time, all tasks succeed
		firstFuture.get();
		secondFuture.get();

		// check the in the end, one transaction "won" (updated both folders)
		folder1 = execute(Folder::reload, folder1);
		folder2 = execute(Folder::reload, folder2);

		if ("Folder 1 Updated2 from first".equals(folder1.getName())) {
			assertThat(folder2).hasFieldOrPropertyWithValue("name", "Folder 2 Updated2 from first");
		} else if ("Folder 1 Updated2 from second".equals(folder1.getName())) {
			assertThat(folder2).hasFieldOrPropertyWithValue("name", "Folder 2 Updated2 from second");
		} else {
			fail(String.format("Folder 1 has unexpected name %s", folder1.getName()));
		}
	}

	/**
	 * Test that the given number of retries is respected
	 * @throws NodeException
	 */
	@Test
	public void testRetries() throws NodeException {
		for (int retries : Arrays.asList(0, 1, 5)) {
			AtomicInteger attemptCounter = new AtomicInteger(0);
			try {
				operate(() -> {
					TransactionManager.execute(() -> {
						assertThat(attemptCounter.incrementAndGet()).as("Attempt #").isLessThanOrEqualTo(retries + 1);
						throw new NodeException(new SQLTransientException());
					}, null, retries);
				});
			} catch (NodeException e) {
				if (e.getCause() instanceof SQLTransientException) {
					// this is expected
				} else {
					throw e;
				}
			}

			assertThat(attemptCounter.get()).as("Attempts").isEqualTo(retries + 1);
		}
	}

	/**
	 * Submit a callable to the executor service, that creates a transaction and executes the given operation within a new temporary transaction
	 * with the given number of retries in case of transient SQL Exceptions (like a deadlock)
	 * @param op operation
	 * @param retries number of retries (0 for none)
	 * @return future result
	 */
	protected Future<?> executeWithRetries(Consumer<Transaction> op, int retries) {
		return service.submit(() -> {
			try {
				operate(() -> {
					TransactionManager.execute(() -> {
						Transaction t = TransactionManager.getCurrentTransaction();
						op.accept(t);
					}, null, retries);
				});
			} catch (NodeException e) {
				fail("Execution failed", e);
			}
		});
	}

	/**
	 * Update the folder name in the given transaction. Dirt the object cache also
	 * @param t transaction
	 * @param folderId folder ID
	 * @param name new name
	 * @return update count
	 * @throws NodeException
	 */
	protected int updateFolderName(Transaction t, int folderId, String name) throws NodeException {
		PreparedStatement stmt = null;

		try {
			stmt = t.prepareStatement("UPDATE folder SET name = ? WHERE id = ?", Transaction.UPDATE_STATEMENT);

			stmt.setString(1, name);
			stmt.setInt(2, folderId);
			stmt.execute();

			t.dirtObjectCache(Folder.class, folderId);
			return stmt.getUpdateCount();
		} catch (SQLException e) {
			throw new NodeException("Error while updating folder name", e);
		} finally {
			t.closeStatement(stmt);
		}
	}
}
