package com.gentics.contentnode.object;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;

import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Operation Result.
 * Contains the result status and optional messages that can be shown to the user, who performed the operation
 */
public class OpResult {
	/**
	 * Static instance for successful operations (without messages)
	 */
	public final static OpResult OK = new OpResult();

	/**
	 * Message to be shown to the user
	 */
	protected List<NodeMessage> messages = new ArrayList<NodeMessage>();

	/**
	 * Operation Status
	 */
	protected Status status = Status.OK;

	/**
	 * Create an instance with status {@link Status#OK} and optional list of messages
	 * @param messages optional list of messages
	 */
	public OpResult(NodeMessage...messages) {
		this(Status.OK, messages);
	}

	/**
	 * Create an instance with given status and optional list of messages
	 * @param status status
	 * @param messages optional list of messages
	 */
	public OpResult(Status status, NodeMessage...messages) {
		this.status = status;
		for (NodeMessage message : messages) {
			this.messages.add(message);
		}
	}

	/**
	 * Get the status
	 * @return status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Return true when the status is OK. Otherwise false.
	 * 
	 * @return
	 */
	public boolean isOK() {
		return Status.OK.equals(status);
	}

	/**
	 * Get the list of messages
	 * @return list of messages
	 */
	public List<NodeMessage> getMessages() {
		return messages;
	}

	/**
	 * Enumeration of possible stati
	 */
	public static enum Status {
		/**
		 * Operation went fine
		 */
		OK,

		/**
		 * Operation failed. The reason should be stated in messages to the user
		 */
		FAILURE
	}

	/**
	 * Return an instance of {@link OpResult} with status {@link Status#FAILURE} and a single message
	 * 
	 * @param clazz The class parameter is used to determine the type of the generated message
	 * @param msgKey message key
	 * @param msgParameters optional message parameters
	 * @return OpResult instance
	 */
	public static OpResult fail(Class clazz, String msgKey, String...msgParameters) {
		CNI18nString message = new CNI18nString(msgKey);
		for (String msgParam : msgParameters) {
			message.addParameter(msgParam);
		}
		return new OpResult(Status.FAILURE, new DefaultNodeMessage(Level.ERROR, clazz, message.toString()));
	}

}
