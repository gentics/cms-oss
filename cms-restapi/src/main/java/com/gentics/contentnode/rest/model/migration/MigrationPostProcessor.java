package com.gentics.contentnode.rest.model.migration;

import java.io.Serializable;

/**
 * Model for a TagTypeMigrationPostProcessor
 * 
 * @author johannes2
 * 
 */
public class MigrationPostProcessor implements Comparable<MigrationPostProcessor>, Serializable {

	private static final long serialVersionUID = -7848160297754166964L;

	/**
	 * The full class name of the post processor
	 */
	private String className;

	/**
	 * The order the post processor should be applied in
	 */
	private Integer orderId;

	/**
	 * Create an empty instance
	 */
	public MigrationPostProcessor() {
	}

	/**
	 * Create an instance with classname and order id
	 * @param className classname
	 * @param orderId order id
	 */
	public MigrationPostProcessor(String className, Integer orderId) {
		setClassName(className);
		setOrderId(orderId);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public int compareTo(MigrationPostProcessor o) {
		return Integer.compare(this.orderId, o.orderId);
	}

}
