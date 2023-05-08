package com.gentics.api.lib.datasource;

import java.io.Serializable;

/**
 * Class for channels that are written into datasources
 */
public class DatasourceChannel implements Comparable<DatasourceChannel>, Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4665009524161405607L;

	/**
	 * ID of the channel
	 */
	protected int id;

	/**
	 * Name of the channel
	 */
	protected String name;

	/**
	 * Create an instance with given id and name
	 * @param id id
	 * @param name name
	 */
	public DatasourceChannel(int id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Get the channel id
	 * @return channel id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get the channel name
	 * @return channel name
	 */
	public String getName() {
		return name;
	}
	
	public boolean equals(DatasourceChannel that) {
		if (this.getId() != that.getId() || !this.getName().equals(that.getName())) {
			return false;
		}
		
		return true;
	}

	public int compareTo(DatasourceChannel that) {
		return this.getId() - that.getId();
	}

	@Override
	public String toString() {
		return name + "(" + id + ")";
	}
}
