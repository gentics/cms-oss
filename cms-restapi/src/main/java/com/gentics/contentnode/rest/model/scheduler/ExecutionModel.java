package com.gentics.contentnode.rest.model.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Model for a scheduler exection.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class ExecutionModel implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6445915702766492097L;

	/**
	 * ID of the exection.
	 */
	private Integer id;

	/**
	 * Schedule ID
	 */
	private Integer scheduleId;



	/**
	 * The corresponding Schedule
	 */
	private ScheduleModel schedule;

	/**
	 * Start time
	 */
	private Integer startTime;

	/**
	 * End time
	 */
	private Integer endTime;


	/**
	 * Duration
	 */
	private Integer duration;

	/**
	 * Success flag
	 */
	private Boolean result;

	/**
	 * Command output
	 */
	private String log;

	/**
	 * Create a model instance from a database result set.
	 *
	 * <p>
	 *     This method assumes that the cursor is correctly set at the
	 *     element to convert.
	 * </p>
	 *
	 * @param rs The database result
	 * @return An execution model created from the database result
	 * @throws SQLException When the result set can not be read
	 */
	public static ExecutionModel fromDbResult(ResultSet rs) throws SQLException {
		return fromDbResult("id", rs);
	}

	/**
	 * Create a model instance from a database result set.
	 *
	 * <p>
	 *     This method assumes that the cursor is correctly set at the
	 *     element to convert.
	 * </p>
	 *
	 * <p>
	 *     The name of the ID field can be specified if the result is from a
	 *     JOIN statement where {@code id} alone is ambiguous.
	 * </p>
	 *
	 * @param idField The name of the execution ID field.
	 * @param rs The database result
	 * @return An execution model created from the database result
	 * @throws SQLException When the result set can not be read
	 */
	public static ExecutionModel fromDbResult(String idField, ResultSet rs) throws SQLException {
		return new ExecutionModel()
			.setId(rs.getInt(idField))
			.setScheduleId(rs.getInt("scheduler_schedule_id"))
			.setStartTime(rs.getInt("starttime"))
			.setEndTime(rs.getInt("endtime"))
			.setDuration(rs.getInt("duration"))
			.setResult(rs.getInt("result") == 0)
			.setLog(rs.getString("log"));
	}


	/**
	 * Create a model instance from a database result set with the corresponding schedule.
	 *
	 * <p>
	 *     This method assumes that the cursor is correctly set at the
	 *     element to convert.
	 * </p>
	 *
	 *
	 * @param rs The database result
	 * @return An execution model created from the database result with the corresponding schedule.
	 * @throws SQLException When the result set can not be read
	 */
	public static ExecutionModel fromDbResultWithSchedule(ResultSet rs) throws SQLException {
		return fromDbResult(rs).setSchedule(
				new ScheduleModel()
				.setId(rs.getInt("scheduler_schedule_id"))
				.setName(rs.getString("name"))
				.setDescription(rs.getString("description"))
				.setTaskId(rs.getInt("scheduler_task_id"))
				.setParallel(rs.getBoolean("parallel"))
				.setActive(rs.getBoolean("active"))
				.setCdate(rs.getInt("cdate"))
				.setRuns(rs.getInt("runs"))
				.setAverageTime(rs.getInt("cdate")));
	}

	/**
	 * Execution ID
	 * @return The execution ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the execution ID
	 * @param id The execution ID
	 * @return fluent API
	 */
	public ExecutionModel setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Schedule ID
	 * @return The schedule ID
	 */
	public Integer getScheduleId() {
		return scheduleId;
	}

	/**
	 * Set the schedule ID
	 * @param scheduleId The schedule ID
	 * @return fluent API
	 */
	public ExecutionModel setScheduleId(Integer scheduleId) {
		this.scheduleId = scheduleId;
		return this;
	}

	/**
	 * The corresponding schedule
	 * @return schedule
	 */
	public ScheduleModel getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule
	 * @param schedule The schedule
	 * @return fluent API
	 */
	public ExecutionModel setSchedule(ScheduleModel schedule) {
		this.schedule = schedule;
		return this;
	}

	/**
	 * Start time
	 * @return The start time
	 */
	public Integer getStartTime() {
		return startTime;
	}

	/**
	 * Set the start time
	 * @param startTime The start time
	 * @return fluent API
	 */
	public ExecutionModel setStartTime(Integer startTime) {
		this.startTime = startTime;
		return this;
	}

	/**
	 * End time
	 * @return The end time
	 */
	public Integer getEndTime() {
		return endTime;
	}

	/**
	 * Set the end time
	 * @param endTime The end time
	 * @return fluent API
	 */
	public ExecutionModel setEndTime(Integer endTime) {
		this.endTime = endTime;
		return this;
	}

	/**
	 * Duration
	 * @return The duration
	 */
	public Integer getDuration() {
		return duration;
	}

	/**
	 * Set the duration
	 * @param duration The duration
	 * @return fluent API
	 */
	public ExecutionModel setDuration(Integer duration) {
		this.duration = duration;
		return this;
	}

	/**
	 * Result
	 * @return The result
	 */
	public Boolean getResult() {
		return result;
	}

	/**
	 * Set the result
	 * @param result The result
	 * @return fluent API
	 */
	public ExecutionModel setResult(Boolean result) {
		this.result = result;
		return this;
	}

	/**
	 * Command output
	 * @return The command output
	 */
	public String getLog() {
		return log;
	}

	/**
	 * Set the command output
	 * @param log The command output
	 * @return fluent API
	 */
	public ExecutionModel setLog(String log) {
		this.log = log;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ExecutionModel that = (ExecutionModel) o;

		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
