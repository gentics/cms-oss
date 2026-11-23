package com.gentics.contentnode.rest.model.task;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Task of a user, used as response and as request (null fields stay unchanged on update)
 */
public class UserTaskModel implements Serializable {
	private static final long serialVersionUID = 1453285713094658921L;

	private String id;

	private TaskSource source;

	private String type;

	private String title;

	private String description;

	private TaskStatus status;

	private Integer dueAt;

	private TaskAssignee assignee;

	private TaskAssignee assigneeGroup;

	private TaskCreator creator;

	private Integer createdAt;

	private Integer updatedAt;

	private Integer completedAt;

	private List<TaskObject> objects;

	private Map<String, Object> details;

	private List<TaskAction> actions;

	private String externalKey;

	private TaskGenaix genaix;

	/**
	 * Task ID: the database ID, or source and object for virtual tasks (e.g. <code>publishQueue:page:9142</code>)
	 * @return ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public UserTaskModel setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * Source of the task
	 * @return source
	 */
	public TaskSource getSource() {
		return source;
	}

	/**
	 * Set the source
	 * @param source source
	 * @return fluent API
	 */
	public UserTaskModel setSource(TaskSource source) {
		this.source = source;
		return this;
	}

	/**
	 * What is to be done, e.g. <code>approvePublish</code>, <code>fixBrokenLinks</code>, <code>genaixReadyToPublish</code>
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public UserTaskModel setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Title, empty for virtual tasks
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the title
	 * @param title title
	 * @return fluent API
	 */
	public UserTaskModel setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Description
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description description
	 * @return fluent API
	 */
	public UserTaskModel setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Status
	 * @return status
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * Set the status
	 * @param status status
	 * @return fluent API
	 */
	public UserTaskModel setStatus(TaskStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Due date (only real dates of the source)
	 * @return due date or null
	 */
	public Integer getDueAt() {
		return dueAt;
	}

	/**
	 * Set the due date
	 * @param dueAt due date
	 * @return fluent API
	 */
	public UserTaskModel setDueAt(Integer dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	/**
	 * Recipient, the current user if not set on creation
	 * @return recipient or null
	 */
	public TaskAssignee getAssignee() {
		return assignee;
	}

	/**
	 * Set the recipient
	 * @param assignee recipient
	 * @return fluent API
	 */
	public UserTaskModel setAssignee(TaskAssignee assignee) {
		this.assignee = assignee;
		return this;
	}

	/**
	 * Group a group task was sent to, kept after it was claimed
	 * @return group or null
	 */
	public TaskAssignee getAssigneeGroup() {
		return assigneeGroup;
	}

	/**
	 * Set the group
	 * @param assigneeGroup group
	 * @return fluent API
	 */
	public UserTaskModel setAssigneeGroup(TaskAssignee assigneeGroup) {
		this.assigneeGroup = assigneeGroup;
		return this;
	}

	/**
	 * Creator
	 * @return creator
	 */
	public TaskCreator getCreator() {
		return creator;
	}

	/**
	 * Set the creator
	 * @param creator creator
	 * @return fluent API
	 */
	public UserTaskModel setCreator(TaskCreator creator) {
		this.creator = creator;
		return this;
	}

	/**
	 * Creation date
	 * @return creation time
	 */
	public Integer getCreatedAt() {
		return createdAt;
	}

	/**
	 * Set the creation time
	 * @param createdAt creation time
	 * @return fluent API
	 */
	public UserTaskModel setCreatedAt(Integer createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	/**
	 * Date of the last change
	 * @return time of last change
	 */
	public Integer getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Set the time of the last change
	 * @param updatedAt time
	 * @return fluent API
	 */
	public UserTaskModel setUpdatedAt(Integer updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	/**
	 * Date the task was done or dismissed
	 * @return completion time or null
	 */
	public Integer getCompletedAt() {
		return completedAt;
	}

	/**
	 * Set the completion time
	 * @param completedAt completion time
	 * @return fluent API
	 */
	public UserTaskModel setCompletedAt(Integer completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	/**
	 * CMS objects the task concerns
	 * @return objects
	 */
	public List<TaskObject> getObjects() {
		return objects;
	}

	/**
	 * Set the objects
	 * @param objects objects
	 * @return fluent API
	 */
	public UserTaskModel setObjects(List<TaskObject> objects) {
		this.objects = objects;
		return this;
	}

	/**
	 * Source specific details
	 * @return details
	 */
	public Map<String, Object> getDetails() {
		return details;
	}

	/**
	 * Set the details
	 * @param details details
	 * @return fluent API
	 */
	public UserTaskModel setDetails(Map<String, Object> details) {
		this.details = details;
		return this;
	}

	/**
	 * Actions the current user may perform
	 * @return actions
	 */
	public List<TaskAction> getActions() {
		return actions;
	}

	/**
	 * Set the actions
	 * @param actions actions
	 * @return fluent API
	 */
	public UserTaskModel setActions(List<TaskAction> actions) {
		this.actions = actions;
		return this;
	}

	/**
	 * Key in the creating system, unique per source (e.g. <code>genaix:&lt;sessionId&gt;:&lt;planId&gt;:release</code>)
	 * @return external key or null
	 */
	public String getExternalKey() {
		return externalKey;
	}

	/**
	 * Set the external key
	 * @param externalKey external key
	 * @return fluent API
	 */
	public UserTaskModel setExternalKey(String externalKey) {
		this.externalKey = externalKey;
		return this;
	}

	/**
	 * Link to GenAIx
	 * @return GenAIx link or null
	 */
	public TaskGenaix getGenaix() {
		return genaix;
	}

	/**
	 * Set the GenAIx link
	 * @param genaix GenAIx link
	 * @return fluent API
	 */
	public UserTaskModel setGenaix(TaskGenaix genaix) {
		this.genaix = genaix;
		return this;
	}
}
