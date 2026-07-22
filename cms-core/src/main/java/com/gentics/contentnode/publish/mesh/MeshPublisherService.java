package com.gentics.contentnode.publish.mesh;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;

/**
 * Interface for service implementations that add functionality to the {@link MeshPublisher}
 */
public interface MeshPublisherService {
	/**
	 * Check whether the service implementation handles objects of the given class
	 * @param clazz class in question
	 * @return true if objects of the class are handled
	 */
	boolean handles(Class<? extends NodeObject> clazz);

	/**
	 * Check whether the service implementation handles the given object
	 * @param object object in question
	 * @return true if the object is handled
	 */
	default boolean handles(NodeObject object) {
		if (object == null) {
			return false;
		} else {
			return handles(object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Create a write task for the given object. This will only be called for
	 * objects, which are handled by the service implementation.
	 * 
	 * @param publisher publisher instance
	 * @param meshProject mesh project
	 * @param object object to be written to mesh
	 * @param nodeId node ID
	 * @param publishProcess true if called within a publish process (false for instant publishing)
	 * @param reportToPublishQueue true when successful publishing shall be reported back to the publish queue
	 * @return write task
	 * @throws NodeException
	 */
	AbstractWriteTask createWriteTask(MeshPublisher publisher, MeshProject meshProject, NodeObject object,
			int nodeId, boolean publishProcess, boolean reportToPublishQueue) throws NodeException;

	/**
	 * Take the object offline. This will only be called for objects, which are
	 * handled by the service implementation.
	 * 
	 * @param publisher publisher instance
	 * @param meshProject mesh project
	 * @param object objet to be taken offline
	 * @throws NodeException
	 */
	void takeOffline(MeshPublisher publisher, MeshProject meshProject, NodeObject object) throws NodeException;

	/**
	 * Delete the object from mesh
	 * @param publisher publisher instance
	 * @param meshProject mesh project
	 * @param meshUuid mesh uuid
	 * @param additionalData optional additional data
	 * @throws NodeException
	 */
	void delete(MeshPublisher publisher, MeshProject meshProject, String meshUuid, Map<String, String> additionalData) throws NodeException;

	/**
	 * Check whether the mesh instance is prepared to have objects published to via this service implementation
	 * @param publisher publisher instance
	 * @param meshProject mesh project
	 * @param currentProjectName current project name
	 * @param repair true to attempt repairing if something is not correct
	 * @param success atomic boolean which must be set to false if something is not correct and cannot be repaired
	 * @throws NodeException
	 */
	void validate(MeshPublisher publisher, MeshProject meshProject, String currentProjectName, boolean repair,
			AtomicBoolean success) throws NodeException;
}
