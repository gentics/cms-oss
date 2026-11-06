package com.gentics.contentnode.rest.model.response.cluster;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Info about the local member
 */
@XmlRootElement
public class MemberInfo {
	/**
	 * Member UUID
	 */
	private String uuid;

	/**
	 * Instance name
	 */
	private String name;

	/**
	 * Member UUID
	 * @return UUID
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set member UUID
	 * @param uuid UUID
	 * @return fluent API
	 */
	public MemberInfo setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * Instance name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set instance name
	 * @param name name
	 * @return fluent API
	 */
	public MemberInfo setName(String name) {
		this.name = name;
		return this;
	}
}
