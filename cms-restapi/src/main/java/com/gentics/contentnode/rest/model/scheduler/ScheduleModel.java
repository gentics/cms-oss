package com.gentics.contentnode.rest.model.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.User;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * Model for a scheduler schedule.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class ScheduleModel implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1564726505791754588L;

	/**
	 * Id of the item
	 */
	private Integer id;

	/**
	 * Name of the item
	 */
	private String name;

	/**
	 * Description of the item
	 */
	private String description;

	/**
	 * ID of the task to execute.
	 */
	private Integer taskId;


	/**
	 * The referenced Task.
	 */
	private TaskModel task;

	/**
	 * Schedule data
	 */
	private ScheduleData scheduleData;

	/**
	 * Whether the schedule can be executed in parallel.
	 */
	private Boolean parallel;

	/**
	 * Active flag
	 */
	private Boolean active;

	/**
	 * The current status of the schedule (idle, due or running).
	 */
	private ScheduleStatus status;

	/**
	 * Email addresses to notify in case of an error.
	 */
	private List<String> notificationEmail;

	/**
	 * The number of executions for this schedule.
	 */
	private int runs;

	/**
	 * The average runtime of this schedules executions.
	 */
	private int averageTime;

	/**
	 * Information about the last execution.
	 */
	private ExecutionModel lastExecution;

	/**
	 * Creator of the item
	 */
	private User creator;

	/**
	 * Date when the item was created
	 */
	private int cdate;

	/**
	 * Contributor to the item
	 */
	private User editor;

	/**
	 * Date when the item was modified the last time
	 */
	private int edate;


	/**
	 * Task ID
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public ScheduleModel setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Task name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public ScheduleModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Task description
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
	public ScheduleModel setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * ID of the executed task
	 * @return task ID
	 */
	public Integer getTaskId() {
		return taskId;
	}

	/**
	 * Set the ID of the executed task
	 * @param taskId task ID
	 * @return fluent API
	 */
	public ScheduleModel setTaskId(Integer taskId) {
		this.taskId = taskId;
		return this;
	}

	/**
	 * Get the task of the schedule
	 * @return task
	 */
	public TaskModel getTask() {
		return task;
	}

	/**
	 * Set the task of the schedule
	 * @param task task
	 * @return fluent API
	 */
	public ScheduleModel setTask(TaskModel task) {
		this.task = task;
		return this;
	}

	/**
	 * Get the schedule data
	 * @return schedule data
	 */
	public ScheduleData getScheduleData() {
		return scheduleData;
	}

	/**
	 * Set schedule data
	 * @param scheduleData data
	 * @return fluent API
	 */
	public ScheduleModel setScheduleData(ScheduleData scheduleData) {
		this.scheduleData = scheduleData;
		return this;
	}

	/**
	 * When {@code true} the schedule can be executed at the same time as other
	 * parallel schedules.
	 *
	 * @return parallel flag
	 */
	public Boolean getParallel() {
		return parallel;
	}

	/**
	 * Set the parallel flag.
	 * @param parallel parallel flag
	 * @return fluent API
	 */
	public ScheduleModel setParallel(Boolean parallel) {
		this.parallel = parallel;
		return this;
	}

	/**
	 * True for internal tasks, false for external tasks
	 * @return flag
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * Set the active flag
	 * @param active flag
	 * @return fluent API
	 */
	public ScheduleModel setActive(Boolean active) {
		this.active = active;
		return this;
	}

	/**
	 * Get the current status of the schedule.
	 *
	 * @return Get the current status of the schedule.
	 */
	public ScheduleStatus getStatus() {
		return status;
	}

	/**
	 * Set the current status of the schedule.
	 * @param status The current status of the schedule.
	 * @return fluent API
	 */
	public ScheduleModel setStatus(ScheduleStatus status) {
		this.status = status;

		return this;
	}

	/**
	 * Get the notification email addresses.
	 * @return Notification email addresses.
	 */
	public List<String> getNotificationEmail() {
		return notificationEmail;
	}

	/**
	 * Set the notification email addresses.
	 * @param notificationEmail Notification email addresses
	 * @return fluent API
	 */
	public ScheduleModel setNotificationEmail(List<String> notificationEmail) {
		this.notificationEmail = notificationEmail;
		return this;
	}

	/**
	 * Get the number of executions for this schedule.
	 * @return The number of executions for this schedule.
	 */
	public int getRuns() {
		return runs;
	}

	/**
	 * Set the number of executions for this schedule.
	 * @param runs The number of executions for this schedule.
	 * @return fluent API
	 */
	public ScheduleModel setRuns(int runs) {
		this.runs = runs;

		return this;
	}

	/**
	 * Get the average runtime of this schedules executions.
	 * @return The average runtime of this schedules executions.
	 */
	public int getAverageTime() {
		return averageTime;
	}

	/**
	 * Set the average runtime of this schedules executions.
	 * @param averageTime The average runtime of this schedules executions.
	 * @return fluent API
	 */	public ScheduleModel setAverageTime(int averageTime) {
		this.averageTime = averageTime;

		return this;
	}

	/**
	 * Get the data for the last execution if any.
	 * @return The data for the last execution.
	 */
	public ExecutionModel getLastExecution() {
		return lastExecution;
	}

	/**
	 * Set the data for the last execution.
	 * @param lastExecution The data for the last execution.
	 * @return fluent API
	 */
	public ScheduleModel setLastExecution(ExecutionModel lastExecution) {
		this.lastExecution = lastExecution;

		return this;
	}

	/**
	 * Task creator
	 * @return creator
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * Set the creator
	 * @param creator creator
	 * @return fluent API
	 */
	public ScheduleModel setCreator(User creator) {
		this.creator = creator;
		return this;
	}

	/**
	 * Task creation timestamp
	 * @return creation timestamp
	 */
	public int getCdate() {
		return cdate;
	}

	/**
	 * Set the creation timestamp
	 * @param cdate timestamp
	 * @return fluent API
	 */
	public ScheduleModel setCdate(int cdate) {
		this.cdate = cdate;
		return this;
	}

	/**
	 * Last task editor
	 * @return editor
	 */
	public User getEditor() {
		return editor;
	}

	/**
	 * Set the editor
	 * @param editor editor
	 * @return fluent API
	 */
	public ScheduleModel setEditor(User editor) {
		this.editor = editor;
		return this;
	}

	/**
	 * Last task edit timestamp
	 * @return edit timestamp
	 */
	public int getEdate() {
		return edate;
	}

	/**
	 * Set the edit timestamp
	 * @param edate timestamp
	 * @return fluent API
	 */
	public ScheduleModel setEdate(int edate) {
		this.edate = edate;
		return this;
	}
}
