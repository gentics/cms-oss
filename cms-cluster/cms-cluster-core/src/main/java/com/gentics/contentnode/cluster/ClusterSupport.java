package com.gentics.contentnode.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.distributed.DistributedWebApplicationException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.rest.model.response.cluster.ClusterInfo;
import com.gentics.contentnode.rest.model.response.cluster.MemberInfo;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

/**
 * Encapsulation of clustering support
 */
public final class ClusterSupport {
	/**
	 * Name of the Topic for requesting the master status
	 */
	private final static String REQUEST_MASTER_TOPIC = "request.master";

	/**
	 * Name of the member attribute specifying the starttime (in ms)
	 */
	private final static String MEMBER_ATTRIBUTE_STARTTIME = "member.starttime";

	/**
	 * Request master message listener
	 */
	private static MessageListener<Void> REQUEST_MASTER_LISTENER = msg -> {
		// we just need to store the uuid of the member, that published the request
		currentMasterUuid = msg.getPublishingMember().getUuid();
	};

	/**
	 * Comparator, which is used to elect a (new) master. The master is always "lowest" member according to this comparator.
	 * The comparator will compare the start timestamp of each member (stored in the member attribute {@link #MEMBER_ATTRIBUTE_STARTTIME}).
	 * If a member does not have this attribute set (for whatever reason) the value will be set to {@link Long#MAX_VALUE}.
	 * If two members have the same start timestamp, the comparator will compare the member UUIDs.
	 */
	private static Comparator<Member> MASTER_COMPARATOR = (m1, m2) -> {
		long timestamp1 = ObjectTransformer.getLong(m1.getAttribute(MEMBER_ATTRIBUTE_STARTTIME), Long.MAX_VALUE);
		long timestamp2 = ObjectTransformer.getLong(m2.getAttribute(MEMBER_ATTRIBUTE_STARTTIME), Long.MAX_VALUE);

		int longCompare = Long.compare(timestamp1, timestamp2);
		if (longCompare != 0) {
			return longCompare;
		} else {
			return m1.getUuid().compareTo(m2.getUuid());
		}
	};

	/**
	 * Internal hazelcast instance
	 */
	private static HazelcastInstance instance;

	/**
	 * Flag that is set, when the instance is merging (back) into the cluster
	 */
	private static boolean merging = false;

	/**
	 * UUID of the current master member
	 */
	private static UUID currentMasterUuid;

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
			Config config = Config.loadDefault();
			MemberAttributeConfig memberAttributeConfig = new MemberAttributeConfig(config.getMemberAttributeConfig());
			memberAttributeConfig.setAttribute(MEMBER_ATTRIBUTE_STARTTIME, Long.toString(System.currentTimeMillis()));
			Config.loadDefault().setMemberAttributeConfig(memberAttributeConfig);
			instance = Hazelcast.newHazelcastInstance(config);

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
			info.setLocalMember(new MemberInfo().setUuid(cluster.getLocalMember().getUuid().toString()).setName(instance.getName()))
					.setMembers(cluster.getMembers().stream().map(member -> member.getUuid().toString()).collect(Collectors.toSet()));
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

			List<Member> members = new ArrayList<>(cluster.getMembers());
			currentMasterUuid = members.stream().min(MASTER_COMPARATOR).map(m -> m.getUuid()).orElse(null);

			if (isMaster()) {
				logger.info("Cluster node was elected as new master");
			} else if (currentMasterUuid == null) {
				logger.warn("Currently, there is no master");
			}
		}
	}

	/**
	 * Check whether the given member currently is the master
	 * @param member member
	 * @return true for the master
	 */
	private static boolean isMaster(Member member) {
		return Objects.equals(member.getUuid(), currentMasterUuid);
	}
}
