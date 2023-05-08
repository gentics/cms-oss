package com.gentics.contentnode.tests.rest.util;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.Lock;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.Operator.QueueBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;

import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import de.jkeylockmanager.manager.exception.KeyLockManagerTimeoutException;

/**
 * Test cases for processing callable queues
 */
public class OperatorTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test queue where one job takes longer than the overall timeout
	 * @throws NodeException
	 */
	@Test
	public void testQueueTimeout() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = Operator
					.queue()
					.add("Sleep 1000",
							() -> {
								Thread.sleep(1000);
								return new GenericResponse(new Message(Message.Type.SUCCESS, "First sleep successful"), new ResponseInfo(ResponseCode.OK,
										"First sleep successful"));
							})
					.add("Sleep 2000",
							() -> {
								Thread.sleep(2000);
								return new GenericResponse(new Message(Message.Type.SUCCESS, "Second sleep successful"), new ResponseInfo(ResponseCode.OK,
										"Second sleep successful"));
							}).execute("Queue Job", 1500);

			CNI18nString expected = new CNI18nString("job_sent_to_background");
			expected.addParameter("Queue Job");
			ContentNodeRESTUtils.assertResponse(response, ResponseCode.OK, expected.toString(), new Message(Message.Type.INFO, expected.toString()));
		});
	}

	/**
	 * Test queue without timeout
	 * @throws NodeException
	 */
	@Test
	public void testQueueNoTimeout() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = Operator
					.queue()
					.add("Sleep 1000",
							() -> {
								Thread.sleep(1000);
								return new GenericResponse(new Message(Message.Type.SUCCESS, "First sleep successful"), new ResponseInfo(ResponseCode.OK,
										"First sleep successful"));
							})
					.add("Sleep 1000",
							() -> {
								Thread.sleep(1000);
								return new GenericResponse(new Message(Message.Type.SUCCESS, "Second sleep successful"), new ResponseInfo(ResponseCode.OK,
										"Second sleep successful"));
							}).execute("Queue Job", 0);

			ContentNodeRESTUtils.assertResponse(response, ResponseCode.OK, "First sleep successful",
					new Message(Message.Type.SUCCESS, "First sleep successful"), new Message(Message.Type.SUCCESS, "Second sleep successful"));
		});
	}

	/**
	 * Test queue where the first job has a general error
	 * @throws NodeException
	 */
	@Test
	public void testQueueGeneralErrorInFirst() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = Operator.queue().add("First job", () -> {
				throw new NodeException("Failed");
			}).add("Second Job", () -> {
				return new GenericResponse(new Message(Message.Type.SUCCESS, "Second Job successful"), new ResponseInfo(ResponseCode.OK, ""));
			}).execute("Queue Job", 0);

			ContentNodeRESTUtils.assertResponse(response, ResponseCode.FAILURE, "Error while Queue Job: com.gentics.api.lib.exception.NodeException: Failed",
					new Message(Message.Type.CRITICAL, new CNI18nString("rest.general.error").toString()));
		});
	}

	/**
	 * Test queue where the second job has a general error
	 * @throws NodeException
	 */
	@Test
	public void testQueueGeneralErrorInSecond() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = Operator.queue().add("First job", () -> {
				return new GenericResponse(new Message(Message.Type.SUCCESS, "First Job successful"), new ResponseInfo(ResponseCode.OK, ""));
			}).add("Second Job", () -> {
				throw new NodeException("Failed");
			}).execute("Queue Job", 0);

			ContentNodeRESTUtils.assertResponse(response, ResponseCode.FAILURE, "Error while Queue Job: com.gentics.api.lib.exception.NodeException: Failed",
					new Message(Message.Type.CRITICAL, new CNI18nString("rest.general.error").toString()));
		});
	}

	/**
	 * Test queue where the first job throws an EntityNotFoundException
	 * @throws NodeException
	 */
	@Test
	public void testQueueNotFoundInFirst() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = Operator.queue().add("First job", () -> {
				throw new EntityNotFoundException("Not Found", "rest.page.notfound", "4711");
			}).add("Second Job", () -> {
				return new GenericResponse(new Message(Message.Type.SUCCESS, "Second Job successful"), new ResponseInfo(ResponseCode.OK, ""));
			}).execute("Queue Job", 0);

			CNI18nString msg = new CNI18nString("rest.page.notfound");
			msg.addParameter("4711");
			ContentNodeRESTUtils.assertResponse(response, ResponseCode.NOTFOUND, "Not Found", new Message(Message.Type.CRITICAL, msg.toString()), new Message(
					Message.Type.SUCCESS, "Second Job successful"));
		});
	}

	/**
	 * Test queue where the second job throws an EntityNotFoundException
	 * @throws NodeException
	 */
	@Test
	public void testQueueNotFoundInSecond() throws NodeException {
		Trx.operate(() -> {
			GenericResponse response = Operator.queue().add("First job", () -> {
				return new GenericResponse(new Message(Message.Type.SUCCESS, "First Job successful"), new ResponseInfo(ResponseCode.OK, ""));
			}).add("Second Job", () -> {
				throw new EntityNotFoundException("Not Found", "rest.page.notfound", "4711");
			}).execute("Queue Job", 0);

			CNI18nString msg = new CNI18nString("rest.page.notfound");
			msg.addParameter("4711");
			ContentNodeRESTUtils.assertResponse(response, ResponseCode.NOTFOUND, "Not Found", new Message(Message.Type.SUCCESS, "First Job successful"),
					new Message(Message.Type.CRITICAL, msg.toString()));
		});
	}

	/**
	 * Test identical locks
	 * @throws Exception
	 */
	@Test(expected=KeyLockManagerTimeoutException.class)
	public void testLocks() throws Throwable {
		testLocks(Operator.lock(LockType.channelSet, 4711), Operator.lock(LockType.channelSet, 4711));
	}

	/**
	 * Test locks with same key but different types
	 * @throws Exception
	 */
	@Test
	public void testDifferentLockTypes() throws Throwable {
		testLocks(Operator.lock(LockType.contentSet, 4711), Operator.lock(LockType.channelSet, 4711));
	}

	/**
	 * Test locks with same type but different keys
	 * @throws Exception
	 */
	@Test
	public void testDifferentLockKeys() throws Throwable {
		testLocks(Operator.lock(LockType.channelSet, 815), Operator.lock(LockType.channelSet, 4711));
	}

	/**
	 * Test executing a dummy entry (this test is necessary to make sure, the
	 * expected Exceptions for the next couple of tests is thrown at the
	 * expected method call)
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testQueue() throws NodeException {
		Trx.operate(() -> {
			QueueBuilder builder = Operator.queue().add("", () -> new GenericResponse());
			builder.execute("", 0);
		});
	}

	/**
	 * Test executing twice
	 * @throws NodeException
	 */
	@Test(expected=NodeException.class)
	public void testQueueExecuteTwice() throws NodeException {
		Trx.operate(() -> {
			QueueBuilder builder = Operator.queue().add("", () -> new GenericResponse());
			builder.execute("", 0);
			builder.execute("", 0);
		});
	}

	/**
	 * Test add to executed queue
	 * @throws NodeException
	 */
	@Test(expected=NodeException.class)
	public void testQueueAddToExecuted() throws NodeException {
		Trx.operate(() -> {
			QueueBuilder builder = Operator.queue().add("", () -> new GenericResponse());
			builder.execute("", 0);
			builder.add("", () -> new GenericResponse());
		});
	}

	/**
	 * Test executing code locked with the given locks (in different threads).
	 * @param lock1 first lock
	 * @param lock2 second lock
	 */
	protected void testLocks(Lock lock1, Lock lock2) throws Throwable {
		KeyLockManager lockManager = KeyLockManagers.newLock(500, TimeUnit.MILLISECONDS);
		Throwable[] exceptions = new Throwable[1];
		assertTrue("Check execution result", lockManager.executeLocked(lock1, () -> {
			ExecutorService executor = Executors.newFixedThreadPool(1);
			try {
				return executor.submit(() -> lockManager.executeLocked(lock2, () -> true)).get();
			} catch (ExecutionException e) {
				exceptions[0] = e.getCause();
				return true;
			} catch (Exception e) {
				return false;
			} finally {
				executor.shutdownNow();
			}
		}));

		if (exceptions[0] != null) {
			throw exceptions[0];
		}
	}
}
