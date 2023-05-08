/*
 * @author norbert
 * @date 28.10.2005
 * @version $Id: SimpleObjectPoolTest.java,v 1.1 2010-02-04 14:25:04 norbert Exp $
 */
package com.gentics.node.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collection;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.lib.pooling.PoolFactoryInterface;
import com.gentics.lib.pooling.PoolInterface;
import com.gentics.lib.pooling.PoolWrapper;
import com.gentics.lib.pooling.Poolable;
import com.gentics.lib.pooling.PoolingException;
import org.junit.experimental.categories.Category;

/**
 * JUnit Testclass for the PoolWrapper. The Test will create a Pool with maximum
 * size {@link #MAX_POOLSIZE} and maximum {@link #MAX_IDLE} idle objects.
 * Clients will block and wait for a free object when the pool is exhausted.
 * Then it creates {@link #THREAD_NUMBER} threads that will run in parallel and
 * each
 * <ol>
 * <li>try to fetch an object from the pool</li>
 * <li>hold the object for {@link #THREAD_RUNTIME} ms</li>
 * <li>return the object to the pool</li>
 * </ol>
 * The test will succeed when all threads could run through correctly in the
 * precalculated maximum allowed time.
 * @author norbert
 */
@Category(BaseLibTest.class)
public class SimpleObjectPoolTest {

	/**
	 * the pool to test
	 */
	private static PoolInterface nakedWomanPool;

	/**
	 * max idle objects in the pool
	 */
	private final static int MAX_IDLE = 5;

	/**
	 * max objects in the pool
	 */
	private final static int MAX_POOLSIZE = 10;

	/**
	 * runtime in ms per thread
	 */
	private final static int THREAD_RUNTIME = 1000;

	/**
	 * number of concurrent threads
	 */
	private final static int THREAD_NUMBER = 100;

	/**
	 * true when threads have to wait for free objects
	 */
	private final static boolean BLOCK_OVERFLOW = true;

	/**
	 * number of successfull threads
	 */
	private static int successfullThreads = 0;

	/**
	 * number of failed threads
	 */
	public static int failedThreads = 0;

	/**
	 * Test method. Create {@link SimpleObjectPoolTest#THREAD_NUMBER} concurrent
	 * threads that will each fetch an object from the pool and occupy it for
	 * {@link SimpleObjectPoolTest#THREAD_RUNTIME} ms. The test fails when the
	 * calculated maximum runtime is exceeded by more than 4 secs.
	 * @throws Exception
	 */
	@Test
	public void testObjectPool() throws Exception {
		// calculate the maximum allowed runtime
		int maxRunTime = ((THREAD_NUMBER / MAX_POOLSIZE) + 1) * THREAD_RUNTIME + 4000;
		Thread[] thread = new Thread[THREAD_NUMBER];

		// create the pool to test
		nakedWomanPool = new PoolWrapper("TestPool", MAX_IDLE, MAX_POOLSIZE, new God(), BLOCK_OVERFLOW);

		// create the concurrent threads and start them
		for (int i = 0; i < THREAD_NUMBER; ++i) {
			thread[i] = new PoolTester("Thread " + i);
			thread[i].start();
		}

		// wait for the maximum allowed execution time
		Thread.sleep(maxRunTime);

		boolean foundRunningThread = false;

		// check whether any of the threads is still alive, if yes, we have an
		// error
		for (int i = 0; i < THREAD_NUMBER; ++i) {
			if (thread[i].isAlive()) {
				// interrupt threads if they are still running
				foundRunningThread = true;
				thread[i].interrupt();
			}
		}

		// we don't want to see any thread still running!
		assertFalse("still found running threads", foundRunningThread);

		// check whether all threads were successfull
		assertEquals("not all threads were successfull - failedThread: {" + failedThreads + "}", THREAD_NUMBER, successfullThreads);
	}

	/**
	 * Object factory class
	 * @author norbert
	 */
	static class God implements PoolFactoryInterface {

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.pooling.PoolFactoryInterface#createObject()
		 */
		public Poolable createObject() throws PoolingException {
			return new NakedWoman();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.pooling.PoolFactoryInterface#destroyObject(com.gentics.lib.pooling.Poolable)
		 */
		public void destroyObject(Poolable object) {// TODO Auto-generated method stub
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.pooling.PoolFactoryInterface#reinitObject(com.gentics.lib.pooling.Poolable)
		 */
		public void reinitObject(Poolable object) {// TODO Auto-generated method stub
		}
	}

	/**
	 * The pooled objects
	 * @author norbert
	 */
	static class NakedWoman implements Poolable {

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.pooling.Poolable#getObject()
		 */
		public Object getObject() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.pooling.Poolable#init(java.util.Collection)
		 */
		public void init(Collection c) {}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.pooling.Poolable#reset()
		 */
		public void reset() {}
	}

	/**
	 * Thread class that will fetch an object from the pool and occupy it for
	 * the configured timespan
	 * @author norbert
	 */
	class PoolTester extends Thread {

		/**
		 * Create an instance of the Thread and set its name
		 * @param threadName
		 */
		public PoolTester(String threadName) {
			setName(threadName);
		}

		/**
		 * main run method of the thread
		 */
		public void run() {
			try {
				// fetch an object from the pool
				Poolable object = nakedWomanPool.getInstance();

				// wait for some time
				sleep(THREAD_RUNTIME);
				// give the object back to the pool
				nakedWomanPool.releaseInstance(object);
				synchronized (SimpleObjectPoolTest.this) {
					successfullThreads++;
				}
			} catch (Exception ignored) {
				synchronized (SimpleObjectPoolTest.this) {
					failedThreads++;
				}
			}
		}
	}
}
