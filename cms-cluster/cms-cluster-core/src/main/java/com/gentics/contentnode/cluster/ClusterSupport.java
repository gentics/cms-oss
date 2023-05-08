package com.gentics.contentnode.cluster;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.distributed.DistributedWebApplicationException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.rest.model.response.cluster.ClusterInfo;
import com.gentics.contentnode.rest.model.response.cluster.MemberInfo;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

/**
 * Encapsulation of clustering support
 */
public final class ClusterSupport {
	/**
	 * Key of the master lock and the master attribute
	 */
	private final static String MASTER = "master";

	/**
	 * Name of the Topic for requesting the master status
	 */
	private final static String REQUEST_MASTER_TOPIC = "request.master";

	/**
	 * Request master message listener
	 */
	private static MessageListener<Void> REQUEST_MASTER_LISTENER = msg -> {
		ClusterSupport.executeIfNotFromLocal(msg, m -> {
			giveUpMasterFlag();
		});
		ClusterSupport.executeIfFromLocal(msg, m -> {
			HazelcastInstance instance = get();
			if (instance != null) {
				Member localMember = instance.getCluster().getLocalMember();
				localMember.setBooleanAttribute(MASTER, true);
			}
		});
	};

	/**
	 * Master lock
	 */
	private static ILock masterLock;

	/**
	 * Internal hazelcast instance
	 */
	private static HazelcastInstance instance;

	/**
	 * Flag that is set, when the instance is merging (back) into the cluster
	 */
	private static boolean merging = false;

	/**
	 * Logger instance
	 */
	static NodeLogger logger = NodeLogger.getNodeLogger(ClusterSupport.class);

	/**
	 * Initialize cluster support (if feature is activated)
	 * @throws NodeException
	 */
	public static void init() throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.CLUSTER)) {
			start();
		} else {
			stop();
		}
	}

	/**
	 * Start a new hazelcast instance
	 * @throws NodeException
	 */
	public static void start() throws NodeException {
		if (instance == null) {
			instance = Hazelcast.newHazelcastInstance();
			masterLock = instance.getLock(MASTER);

			electMaster();
			addMessageListeners();
		}
	}

	/**
	 * Stop hazelcast instance (if started)
	 */
	public static void stop() {
		if (instance != null) {
			instance.shutdown();
			instance = null;
		}
	}

	/**
	 * Get info about the cluster state
	 * @return info
	 */
	public static ClusterInfo getInfo() {
		HazelcastInstance instance = get();
		ClusterInfo info = new ClusterInfo().setFeature(NodeConfigRuntimeConfiguration.isFeature(Feature.CLUSTER)).setMaster(isMaster())
				.setStarted(instance != null);
		if (instance != null) {
			Cluster cluster = instance.getCluster();
			info.setLocalMember(new MemberInfo().setUuid(cluster.getLocalMember().getUuid()).setName(instance.getName()))
					.setMembers(cluster.getMembers().stream().map(Member::getUuid).collect(Collectors.toSet()));
		}
		return info;
	}

	/**
	 * Check whether this node is the current cluster master.
	 * If cluster support is disabled, this method will return true.
	 * If the instance is currently merging into a cluster (after network partition), this method will return false (regardless of the member attribute).
	 * The reason for this is, that election of the new master can only be done, after merging is completed and meanwhile this instance and another instance
	 * in the cluster might both claim to be the master
	 * @return true for the master
	 */
	public static boolean isMaster() {
		HazelcastInstance instance = get();
		if (instance != null) {
			if (merging) {
				return false;
			}
			return isMaster(instance.getCluster().getLocalMember());
		} else {
			return true;
		}
	}

	/**
	 * Make this instance the master (if it not already is the master)
	 */
	public static void setMaster() {
		if (isMaster()) {
			return;
		}

		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<Void> requestMasterTopic = instance.getTopic(REQUEST_MASTER_TOPIC);
			requestMasterTopic.publish(null);
		}
	}

	/**
	 * If the CMS instance runs in a cluster, acquire the distributed lock with given key
	 * @param key lock key
	 * @throws InterruptedException
	 */
	public static void acquireLock(String key) throws InterruptedException {
		ILock lock = getLock(key);
		if (lock != null) {
			lock.lockInterruptibly();
		}
	}

	/**
	 * If the CMS instance runs in a cluster, acquire the distributed lock with given key
	 * @param key lock key
	 * @param timeout timeout
	 * @param unit timeout unit
	 * @throws NodeException
	 * @throws InterruptedException
	 */
	public static void acquireLock(String key, long timeout, TimeUnit unit) throws NodeException, InterruptedException {
		ILock lock = getLock(key);
		if (lock != null) {
			if (!lock.tryLock(timeout, unit)) {
				throw new NodeException(String.format("Timeout of %d %s reached when trying to acquire lock %d", timeout, unit.name(), key));
			}
		}
	}

	/**
	 * If the CMS instance runs in a cluster, release the distributed lock with given key
	 * @param key lock key
	 */
	public static void releaseLock(String key) {
		ILock lock = getLock(key);
		if (lock != null) {
			lock.unlock();
		}
	}

	/**
	 * <p>Call the given task (which must be {@link Serializable}) on the master node of the cluster.
	 * If this instance is the master (or not part of a cluster), the task is called locally.</p>
	 * <p>The return value must also be {@link Serializable}</p>
	 * <p>If the task throws a {@link DistributedWebApplicationException}, this method will throw the matching {@link WebApplicationException}.</p>
	 * @param task task
	 * @return return value
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static <T> T callOnMaster(Callable<T> task) throws InterruptedException, Exception {
		try {
			if (isMaster()) {
				return task.call();
			}
			HazelcastInstance instance = get();
			if (instance != null) {
				try {
					return instance.getExecutorService("default").submit(task, ClusterSupport::isMaster).get();
				} catch (ExecutionException e) {
					if (e.getCause() instanceof Exception) {
						throw (Exception) e.getCause();
					} else {
						throw e;
					}
				}
			} else {
				return task.call();
			}
		} catch (DistributedWebApplicationException e) {
			throw e.transform();
		}
	}

	/**
	 * Call the given task (which must be {@link Serializable}) on all cluster members and return the result
	 * of the local execution
	 * @param task task
	 * @return return value of the local task execution
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static <T> T callOnAllMembers(Callable<T> task) throws InterruptedException, Exception {
		try {
			HazelcastInstance instance = get();
			if (instance != null) {
				try {
					return instance.getExecutorService("default").submitToAllMembers(task).get(instance.getCluster().getLocalMember()).get();
				} catch (ExecutionException e) {
					if (e.getCause() instanceof Exception) {
						throw (Exception) e.getCause();
					} else {
						throw e;
					}
				}
			} else {
				return task.call();
			}
		} catch (DistributedWebApplicationException e) {
			throw e.transform();
		}
	}

	/**
	 * Execute the given task (which must be {@link Serializable}) on all other cluster members
	 * @param task task to execute
	 */
	public static void executeOnOtherMembers(Runnable task) {
		HazelcastInstance instance = get();
		if (instance != null) {
			try {
				instance.getExecutorService("default").executeOnMembers(task, m -> !m.localMember());
			} catch (RejectedExecutionException e) {
			}
		}
	}

	/**
	 * If the instance runs in a cluster, return the distributed lock with given key, otherwise return null
	 * @param key lock key
	 * @return distributed lock or null
	 */
	private static ILock getLock(String key) {
		HazelcastInstance instance = get();
		if (instance != null) {
			return instance.getLock(key);
		} else {
			return null;
		}
	}

	/**
	 * Let the handler accept the object from the given message, if the message was not published from the local node
	 * @param msg message
	 * @param handler handler
	 */
	static <T> void executeIfNotFromLocal(Message<T> msg, Consumer<T> handler) {
		Member publishingMember = msg.getPublishingMember();
		if (publishingMember != null && !publishingMember.localMember()) {
			handler.accept(msg.getMessageObject());
		}
	}

	/**
	 * Let the handler accept the object from the given message, if the message was published from the local node
	 * @param msg message
	 * @param handler handler
	 */
	static <T> void executeIfFromLocal(Message<T> msg, Consumer<T> handler) {
		Member publishingMember = msg.getPublishingMember();
		if (publishingMember != null && publishingMember.localMember()) {
			handler.accept(msg.getMessageObject());
		}
	}

	/**
	 * Get the hazelcast instance (or null)
	 * @return hazelcast instance
	 */
	static HazelcastInstance get() {
		return instance;
	}

	/**
	 * Give up the master flag
	 */
	static void giveUpMasterFlag() {
		HazelcastInstance instance = get();
		if (instance != null) {
			Member localMember = instance.getCluster().getLocalMember();
			if (isMaster(localMember)) {
				localMember.setBooleanAttribute(MASTER, false);
				// TODO check for publish process, synchronizer, etc.
			}
		}
	}

	/**
	 * Add message listeners
	 */
	private static void addMessageListeners() {
		HazelcastInstance instance = get();
		if (instance != null) {
			// add membership listener for selecting a new master, if a node leaves the cluster
			instance.getCluster().addMembershipListener(new MembershipListener() {
				@Override
				public void memberRemoved(MembershipEvent membershipEvent) {
					logger.info(String.format("Removed %s", membershipEvent.getMember().getUuid()));
					if (isMaster(membershipEvent.getMember())) {
						electMaster();
					}
				}

				@Override
				public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
				}

				@Override
				public void memberAdded(MembershipEvent membershipEvent) {
					logger.info(String.format("Added %s", membershipEvent.getMember().getUuid()));
				}
			});
			instance.getLifecycleService().addLifecycleListener(event -> {
				logger.info(String.format("Lifecycle state changed to %s", event.getState()));
				switch (event.getState()) {
				case MERGING:
					merging = true;
					break;
				case MERGED:
					// when the instance merged into a cluster, we need to elect a new master (to avoid multimaster situations)
					merging = false;
					electMaster();
					// when an instance merged into a cluster, there are possible inconsistencies in the caches, so we let all members clear their caches
					// just to make sure
					try {
						NodeFactory.getInstance().clear();
					} catch (NodeException e) {
						logger.error("Error while clearing node cache", e);
					}
					new PermSyncMessage().setType(PermSyncMessageType.init).execute();
					ClusterPermissionStoreService.init();
					break;
				default:
					break;
				}
			});

			ITopic<Void> requestMasterTopic = instance.getTopic(REQUEST_MASTER_TOPIC);
			requestMasterTopic.addMessageListener(REQUEST_MASTER_LISTENER);

			ClusteredUpdate.addMessageListeners();
			ClusterCacheService.addMessageListeners();
			ClusterPermissionStoreService.addMessageListeners();
			ClusterInstantPublishService.addMessageListeners();
		}
	}

	/**
	 * Safely elect a new master in the cluster
	 */
	private static void electMaster() {
		HazelcastInstance instance = get();
		if (instance != null) {
			Cluster cluster = instance.getCluster();
			logger.info("Locking for master election");
			masterLock.lock();
			try {
				logger.info("Locked for master election");
				if (!cluster.getMembers().stream().filter(m -> isMaster(m)).findFirst().isPresent()) {
					cluster.getLocalMember().setBooleanAttribute(MASTER, true);
					logger.info("Cluster node was elected as new master");
				} else if (cluster.getMembers().stream().filter(m -> isMaster(m)).count() > 1) {
					logger.info("Detected multiple masters in the cluster, giving up the master flag");
					giveUpMasterFlag();
				}
			} finally {
				masterLock.unlock();
				logger.info("Unlocked after master election");
			}
		}
	}

	/**
	 * Check whether the given member currently is the master
	 * @param member member
	 * @return true for the master
	 */
	private static boolean isMaster(Member member) {
		return member.getBooleanAttribute(MASTER) == Boolean.TRUE;
	}
}
