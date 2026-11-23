package com.gentics.contentnode.rest.model.task;

import java.io.Serializable;

/**
 * Link of a task to GenAIx
 */
public class TaskGenaix implements Serializable {
	private static final long serialVersionUID = -1880569047919226052L;

	private String sessionId;

	private String planId;

	private String instruction;

	/**
	 * GenAIx session ID
	 * @return session ID
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Set the session ID
	 * @param sessionId session ID
	 * @return fluent API
	 */
	public TaskGenaix setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	/**
	 * GenAIx plan ID
	 * @return plan ID
	 */
	public String getPlanId() {
		return planId;
	}

	/**
	 * Set the plan ID
	 * @param planId plan ID
	 * @return fluent API
	 */
	public TaskGenaix setPlanId(String planId) {
		this.planId = planId;
		return this;
	}

	/**
	 * Instruction for the agent, when a session is started from the task
	 * @return instruction
	 */
	public String getInstruction() {
		return instruction;
	}

	/**
	 * Set the instruction
	 * @param instruction instruction
	 * @return fluent API
	 */
	public TaskGenaix setInstruction(String instruction) {
		this.instruction = instruction;
		return this;
	}
}
