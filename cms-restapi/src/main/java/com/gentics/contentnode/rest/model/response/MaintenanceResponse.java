package com.gentics.contentnode.rest.model.response;

/**
 * Response containing maintenance information
 */
public class MaintenanceResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6522662697629058042L;

	/**
	 * Maintenance mode flag
	 */
	protected boolean maintenance;

	/**
	 * Banner flag
	 */
	protected boolean banner;

	/**
	 * Maintenance message
	 */
	protected String message;

	/**
	 * Create empty instance
	 */
	public MaintenanceResponse() {
		super();
	}

	/**
	 * Create instance with message and response info
	 * @param message response message
	 * @param responseInfo response info
	 */
	public MaintenanceResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * True when the maintenance mode is active, false if not
	 * @return true for maintenance mode
	 */
	public boolean isMaintenance() {
		return maintenance;
	}

	/**
	 * Set the maintenance mode
	 * @param maintenance true for maintenance mode
	 */
	public void setMaintenance(boolean maintenance) {
		this.maintenance = maintenance;
	}

	/**
	 * True when the maintenance message should be shown in the ui
	 * @return true for maintenance message
	 */
	public boolean isBanner() {
		return banner;
	}

	/**
	 * Set true for display of the maintenance message in the ui
	 * @param banner true/false
	 */
	public void setBanner(boolean banner) {
		this.banner = banner;
	}

	/**
	 * Maintenance message
	 * @return maintenance message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the maintenance message
	 * @param message maintenance message
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
