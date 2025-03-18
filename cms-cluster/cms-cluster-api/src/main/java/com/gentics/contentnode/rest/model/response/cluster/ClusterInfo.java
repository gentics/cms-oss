package com.gentics.contentnode.rest.model.response.cluster;

import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing clustering information
 */
@XmlRootElement
public class ClusterInfo extends GenericResponse {
	/**
	 * Flag for feature
	 */
	protected boolean feature;

	/**
	 * Flag for master node
	 */
	protected boolean master;

	/**
	 * Flag for cluster started
	 */
	protected boolean started;

	/**
	 * Cluster members (uuids)
	 */
	protected Set<String> members;

	/**
	 * Local member
	 */
	protected MemberInfo localMember;

	/**
	 * True if the clustering feature is activated, false if not
	 * @return true for clustering feature
	 */
	public boolean isFeature() {
		return feature;
	}

	/**
	 * Set whether the clustering feature is activated
	 * @param feature true for clustering feature
	 * @return fluent API
	 */
	public ClusterInfo setFeature(boolean feature) {
		this.feature = feature;
		return this;
	}

	/**
	 * True if this is the master node (or clustering is disabled)
	 * @return true for master node
	 */
	public boolean isMaster() {
		return master;
	}

	/**
	 * Set master node info
	 * @param master true for master node
	 * @return fluent API
	 */
	public ClusterInfo setMaster(boolean master) {
		this.master = master;
		return this;
	}

	/**
	 * True if the cluster (hazelcast) is started, false if not
	 * @return true for started cluster
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * Set started info
	 * @param started true if cluster is started
	 * @return fluent API
	 */
	public ClusterInfo setStarted(boolean started) {
		this.started = started;
		return this;
	}

	/**
	 * UUIDs of the cluster members
	 * @return UUID set
	 */
	public Set<String> getMembers() {
		return members;
	}

	/**
	 * Set UUIDs of cluster members
	 * @param members UUID set
	 * @return fluent API
	 */
	public ClusterInfo setMembers(Set<String> members) {
		this.members = members;
		return this;
	}

	/**
	 * Info about the local cluster member
	 * @return local member
	 */
	public MemberInfo getLocalMember() {
		return localMember;
	}

	/**
	 * Set Info about local cluster member
	 * @param localMember local member
	 * @return fluent API
	 */
	public ClusterInfo setLocalMember(MemberInfo localMember) {
		this.localMember = localMember;
		return this;
	}
}
