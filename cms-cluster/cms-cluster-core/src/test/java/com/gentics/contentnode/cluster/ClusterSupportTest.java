package com.gentics.contentnode.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.google.common.base.Objects;
import com.hazelcast.core.HazelcastInstance;

/**
 * Test cases for {@link ClusterSupport}
 */
public class ClusterSupportTest {
	protected List<ClusterSupport> instances = new ArrayList<>();

	@After
	public void tearDown() {
		instances.forEach(ClusterSupport::stop);
		instances.clear();
	}

	/**
	 * Test that when starting multiple instances, the oldest is the master
	 * @throws NodeException
	 */
	@Test
	public void testMasterElection() throws NodeException {
		ClusterSupport first = createInstance(true);
		ClusterSupport second = createInstance(true);
		ClusterSupport third = createInstance(true);

		assertThat(first.isMaster()).as("First instance is master").isTrue();
		assertThat(second.isMaster()).as("Second instance is master").isFalse();
		assertThat(third.isMaster()).as("Third instance is master").isFalse();
	}

	@Test
	public void testMasterReelection() throws NodeException, InterruptedException {
		// create a number of instances
		int number = 3;
		for (int i = 0; i < number; i++) {
			createInstance(true);
		}

		// all must be running now
		instances.forEach(inst -> {
			assertThat(inst.isRunning()).as("Instance is running").isTrue();
		});

		assertExactlyOneMaster();

		// now start/stop instances
		int changes = 5;
		for (int i = 0; i < changes; i++) {
			int instanceToToggle = RandomUtils.nextInt(0, number);
			ClusterSupport instance = instances.get(instanceToToggle);
			if (instance.isRunning()) {
				stop(instance);
			} else {
				start(instance);
			}

			assertExactlyOneMaster();
		}
	}

	/**
	 * Test that it is possible to set an instance to be the new master
	 * @throws NodeException
	 * @throws InterruptedException
	 */
	@Test
	public void testSetMaster() throws NodeException, InterruptedException {
		// create three instances
		ClusterSupport first = createInstance(true);
		ClusterSupport second = createInstance(true);
		ClusterSupport third = createInstance(true);

		// oldest instance is the master
		assertThat(first.isMaster()).as("First instance is master").isTrue();
		assertThat(second.isMaster()).as("Second instance is master").isFalse();
		assertThat(third.isMaster()).as("Third instance is master").isFalse();

		// make the second instance the master
		makeMaster(second);

		assertThat(first.isMaster()).as("First instance is master").isFalse();
		assertThat(second.isMaster()).as("Second instance is master").isTrue();
		assertThat(third.isMaster()).as("Third instance is master").isFalse();

		// make the third instance the master
		makeMaster(third);

		assertThat(first.isMaster()).as("First instance is master").isFalse();
		assertThat(second.isMaster()).as("Second instance is master").isFalse();
		assertThat(third.isMaster()).as("Third instance is master").isTrue();

		// stop the oldest instance
		first.stop();

		// master is still the third instance
		assertThat(second.isMaster()).as("Second instance is master").isFalse();
		assertThat(third.isMaster()).as("Third instance is master").isTrue();
	}

	/**
	 * Test calling tasks on master
	 * @throws InterruptedException
	 * @throws Exception
	 */
	@Test
	public void testCallOnMaster() throws InterruptedException, Exception {
		// create three instances
		ClusterSupport first = createInstance(true);
		ClusterSupport second = createInstance(true);
		ClusterSupport third = createInstance(true);

		ClusterTask task = new ClusterTask();

		// no tasks have been executed on any instance
		assertExecutedTasks(first, 0);
		assertExecutedTasks(second, 0);
		assertExecutedTasks(third, 0);

		// execute directly on first instance
		assertThat(first.callOnMaster(task)).as("Task result").isNotEmpty();

		// no "cluster" tasks have been executed on any instance
		assertExecutedTasks(first, 0);
		assertExecutedTasks(second, 0);
		assertExecutedTasks(third, 0);

		// execute over the second instance
		assertThat(second.callOnMaster(task)).as("Task result").isNotEmpty();

		assertExecutedTasks(first, 1);
		assertExecutedTasks(second, 0);
		assertExecutedTasks(third, 0);

		// execute over the third instance
		assertThat(third.callOnMaster(task)).as("Task result").isNotEmpty();

		assertExecutedTasks(first, 2);
		assertExecutedTasks(second, 0);
		assertExecutedTasks(third, 0);
	}

	/**
	 * Test calling tasks on all members
	 * @throws InterruptedException
	 * @throws Exception
	 */
	@Test
	public void testCallOnAllMembers() throws InterruptedException, Exception {
		// create three instances
		ClusterSupport first = createInstance(true);
		ClusterSupport second = createInstance(true);
		ClusterSupport third = createInstance(true);

		ClusterTask task = new ClusterTask();

		// no tasks have been executed on any instance
		assertExecutedTasks(first, 0);
		assertExecutedTasks(second, 0);
		assertExecutedTasks(third, 0);

		// execute over first instance
		assertThat(first.callOnAllMembers(task)).as("Task result").isNotEmpty();
		assertExecutedTasks(first, 1);
		assertExecutedTasks(second, 1);
		assertExecutedTasks(third, 1);

		// execute over second instance
		assertThat(second.callOnAllMembers(task)).as("Task result").isNotEmpty();
		assertExecutedTasks(first, 2);
		assertExecutedTasks(second, 2);
		assertExecutedTasks(third, 2);

		// execute over third instance
		assertThat(third.callOnAllMembers(task)).as("Task result").isNotEmpty();
		assertExecutedTasks(first, 3);
		assertExecutedTasks(second, 3);
		assertExecutedTasks(third, 3);
	}

	/**
	 * Create a new instance of {@link ClusterSupport}
	 * @param start true to start the instance
	 * @return instance
	 * @throws NodeException
	 */
	protected ClusterSupport createInstance(boolean start) throws NodeException {
		ClusterSupport instance = new ClusterSupport();
		instances.add(instance);
		if (start) {
			instance.start();
		}
		return instance;
	}

	/**
	 * Assert that from all running instances, there is exactly one master (if at least one instance is running)
	 */
	protected void assertExactlyOneMaster() {
		List<Boolean> masterFlags = instances.stream().filter(ClusterSupport::isRunning).map(ClusterSupport::isMaster)
				.collect(Collectors.toList());
		// when no instance is running, we do not check anything
		if (masterFlags.isEmpty()) {
			return;
		}
		assertThat(masterFlags).as("List of master flags").containsOnlyOnce(true);
	}

	/**
	 * Make the given instance the master and wait for all instances to get the message
	 * @param instance instance to make the master
	 * @throws InterruptedException
	 */
	protected void makeMaster(ClusterSupport instance) throws InterruptedException, NodeException {
		execute(instance, (hz, callback) -> {
			return hz.getTopic(ClusterSupport.REQUEST_MASTER_TOPIC).addMessageListener(msg -> callback.run());
		}, (hz, uuid) -> {
			hz.getTopic(ClusterSupport.REQUEST_MASTER_TOPIC).removeMessageListener(uuid);
		}, ClusterSupport::setMaster);
	}

	/**
	 * Start the given instance and wait for all instances to get the message
	 * @param instance instance to start
	 * @throws InterruptedException
	 * @throws NodeException
	 */
	protected void start(ClusterSupport instance) throws InterruptedException, NodeException {
		execute(instance, (hz, callback) -> {
			return hz.getLifecycleService().addLifecycleListener(event -> callback.run());
		}, (hz, uuid) -> {
			hz.getLifecycleService().removeLifecycleListener(uuid);
		}, ClusterSupport::start);
	}

	/**
	 * Stop the given instance and wait for all instances to get the message
	 * @param instance instance to stop
	 * @throws InterruptedException
	 * @throws NodeException
	 */
	protected void stop(ClusterSupport instance) throws InterruptedException, NodeException {
		execute(instance, (hz, callback) -> {
			return hz.getLifecycleService().addLifecycleListener(event -> callback.run());
		}, inst -> !Objects.equal(inst, instance), (hz, uuid) -> {
			hz.getLifecycleService().removeLifecycleListener(uuid);
		}, ClusterSupport::stop);
	}

	/**
	 * Use the listener appender to add message listeners to all running instances,
	 * pass the instance to the consumer, assert that all running instances got the
	 * message listener called and remove the listeners
	 * 
	 * @param instance         instance
	 * @param listenerAppender function to append the message listener to the given
	 *                         hazelcast instance and return the registration UUID.
	 *                         The listener must call the given runnable.
	 * @param listenerRemover  remove the listener with given registration UUID from
	 *                         the hazelcast instance
	 * @param consumer         consume the instance
	 * @throws InterruptedException
	 * @throws NodeException
	 */
	protected void execute(ClusterSupport instance, BiFunction<HazelcastInstance, Runnable, UUID> listenerAppender,
			BiConsumer<HazelcastInstance, UUID> listenerRemover, Consumer<ClusterSupport> consumer)
			throws InterruptedException, NodeException {
		execute(instance, listenerAppender, inst -> true, listenerRemover, consumer);
	}

	/**
	 * Use the listener appender to add message listeners to all running instances
	 * (filtered by the given filter), pass the instance to the consumer, assert
	 * that all running instances (filtered by the given filter) got the message
	 * listener called and remove the listeners
	 * 
	 * @param instance         instance
	 * @param listenerAppender function to append the message listener to the given
	 *                         hazelcast instance and return the registration UUID.
	 *                         The listener must call the given runnable.
	 * @param filter           filter for running instances
	 * @param listenerRemover  remove the listener with given registration UUID from
	 *                         the hazelcast instance
	 * @param consumer         consume the instance
	 * @throws InterruptedException
	 * @throws NodeException
	 */
	protected void execute(ClusterSupport instance, BiFunction<HazelcastInstance, Runnable, UUID> listenerAppender, Predicate<ClusterSupport> filter,
			BiConsumer<HazelcastInstance, UUID> listenerRemover, Consumer<ClusterSupport> consumer)
			throws InterruptedException, NodeException {
		List<ClusterSupport> runningInstances = instances.stream().filter(ClusterSupport::isRunning).filter(filter)
				.collect(Collectors.toList());

		CountDownLatch latch = new CountDownLatch(runningInstances.size());
		Map<HazelcastInstance, UUID> listeners = new HashMap<>();

		try {
			// register a message listener to all running instances, that counts down the latch
			runningInstances.forEach(inst -> {
				HazelcastInstance hazelcastInstance = inst.get();
				UUID listenerUuid = listenerAppender.apply(hazelcastInstance, () -> latch.countDown());
				listeners.put(hazelcastInstance, listenerUuid);
			});

			// call the consumer
			consumer.accept(instance);

			// wait until all members got the message
			latch.await(10, TimeUnit.SECONDS);
		} finally {
			// remove the listeners
			listeners.forEach(listenerRemover);
		}
	}

	/**
	 * Assert that the given instance executed the given number of tasks with the "default" executor service
	 * @param instance instance to check
	 * @param expected number of expected tasks to have been completed
	 */
	protected void assertExecutedTasks(ClusterSupport instance, long expected) {
		assertThat(instance.get().getExecutorService("default").getLocalExecutorStats().getCompletedTaskCount())
				.as("Completed Task Count").isEqualTo(expected);
	}
}
