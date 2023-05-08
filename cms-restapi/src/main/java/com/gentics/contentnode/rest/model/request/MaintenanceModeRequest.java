package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Rest Model of the request to set/unset the maintenance mode
 */
@XmlRootElement
public class MaintenanceModeRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 429701066089200663L;

	/**
	 * Maintenance mode flag
	 */
	protected Boolean maintenance;

	/**
	 * Banner flag
	 */
	protected Boolean banner;

	/**
	 * Maintenance message
	 */
	protected String message;

	/**
	 * Maintenance mode flag. True to activate maintenancemode, false to deactivate maintenancemode
	 * @return the maintenance
	 */
	public Boolean getMaintenance() {
		return maintenance;
	}

	/**
	 * Set maintenance mode flag
	 * @param maintenance the maintenance to set
	 * @return fluent API
	 */
	public MaintenanceModeRequest setMaintenance(Boolean maintenance) {
		this.maintenance = maintenance;
		return this;
	}

	/**
	 * Flag to show the maintenance message in the banner
	 * @return banner flag
	 */
	public Boolean getBanner() {
		return banner;
	}

	/**
	 * Set the banner flag
	 * @param banner flag
	 * @return fluent API
	 */
	public MaintenanceModeRequest setBanner(Boolean banner) {
		this.banner = banner;
		return this;
	}

	/**
	 * Maintenance message. The message will be shown on the login screen, when the
	 * maintenance mode is active, and/or in the banner, if the banner flag is set.
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the maintenance message
	 * @param message the message to set
	 * @return fluent API
	 */
	public MaintenanceModeRequest setMessage(String message) {
		this.message = message;
		return this;
	}
}
