package com.gentics.contentnode.rest.model.objtag;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rest Model of a tag out of sync
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class SyncItem implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1320369054375530963L;

	private int id;

	private int groupId;

	private String name;

	private int objType;

	private int objId;

	private String objName;

	private String objLanguage;

	private String objPath;

	/**
	 * Tag ID
	 * @return tag ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the tag ID
	 * @param id ID
	 * @return fluent API
	 */
	public SyncItem setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Group ID
	 * @return group ID
	 */
	public int getGroupId() {
		return groupId;
	}

	/**
	 * Set the group ID
	 * @param groupId ID
	 * @return fluent API
	 */
	public SyncItem setGroupId(int groupId) {
		this.groupId = groupId;
		return this;
	}

	/**
	 * Tag name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the tag name
	 * @param name name
	 * @return fluent API
	 */
	public SyncItem setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Object Type
	 * @return object type
	 */
	public int getObjType() {
		return objType;
	}

	/**
	 * Set the object type
	 * @param objType object type
	 * @return fluent API
	 */
	public SyncItem setObjType(int objType) {
		this.objType = objType;
		return this;
	}

	/**
	 * Object ID
	 * @return object ID
	 */
	public int getObjId() {
		return objId;
	}

	/**
	 * Set the object ID
	 * @param objId object ID
	 * @return fluent API
	 */
	public SyncItem setObjId(int objId) {
		this.objId = objId;
		return this;
	}

	/**
	 * Object name
	 * @return object name
	 */
	public String getObjName() {
		return objName;
	}

	/**
	 * Set the object name
	 * @param objName object name
	 * @return fluent API
	 */
	public SyncItem setObjName(String objName) {
		this.objName = objName;
		return this;
	}

	/**
	 * Object language
	 * @return language
	 */
	public String getObjLanguage() {
		return objLanguage;
	}

	/**
	 * Set the object language
	 * @param objLanguage object language
	 * @return fluent API
	 */
	public SyncItem setObjLanguage(String objLanguage) {
		this.objLanguage = objLanguage;
		return this;
	}

	/**
	 * Object path
	 * @return object path
	 */
	public String getObjPath() {
		return objPath;
	}

	/**
	 * Set the object path
	 * @param objPath object path
	 * @return fluent API
	 */
	public SyncItem setObjPath(String objPath) {
		this.objPath = objPath;
		return this;
	}
}
