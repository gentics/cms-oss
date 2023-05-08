package com.gentics.contentnode.rest.model.scheduler;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.User;

/**
 * REST Model of a Scheduler Task
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class TaskModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4641705682952465384L;

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
	 * Command of the item
	 */
	private String command;

	/**
	 * "Internal" flag
	 */
	private Boolean internal;

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
	public TaskModel setId(Integer id) {
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
	public TaskModel setName(String name) {
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
	public TaskModel setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Task command
	 * @return command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Set the command
	 * @param command command
	 * @return fluent API
	 */
	public TaskModel setCommand(String command) {
		this.command = command;
		return this;
	}

	/**
	 * True for internal tasks, false for external tasks
	 * @return flag
	 */
	public Boolean getInternal() {
		return internal;
	}

	/**
	 * Set the internal flag
	 * @param internal flag
	 * @return fluent API
	 */
	public TaskModel setInternal(Boolean internal) {
		this.internal = internal;
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
	public TaskModel setCreator(User creator) {
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
	public TaskModel setCdate(int cdate) {
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
	public TaskModel setEditor(User editor) {
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
	public TaskModel setEdate(int edate) {
		this.edate = edate;
		return this;
	}
}
