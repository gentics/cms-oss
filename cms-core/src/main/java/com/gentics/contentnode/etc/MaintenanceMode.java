package com.gentics.contentnode.etc;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodeSetup.NODESETUP_KEY;
import com.gentics.contentnode.rest.model.response.MaintenanceResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Helper class for management of maintenance mode
 */
public class MaintenanceMode {
	/**
	 * Flag for enabled maintenance mode
	 */
	protected boolean enabled = false;

	/**
	 * Maintenance message
	 */
	protected String message = "";

	/**
	 * Flag for display the maintenance message in a banner
	 */
	protected boolean banner = false;

	/**
	 * Function to transform the maintenance mode settings into their REST Model
	 */
	public final static Function<MaintenanceMode, MaintenanceResponse> TRANSFORM2REST = mode -> {
		MaintenanceResponse response = new MaintenanceResponse(null, new ResponseInfo(ResponseCode.OK, null));

		response.setBanner(mode.banner);
		response.setMaintenance(mode.enabled);
		response.setMessage(mode.message);
		return response;
	};

	/**
	 * Get the current maintenance mode settings
	 * @return settings
	 * @throws NodeException
	 */
	public static MaintenanceMode get() throws NodeException {
		return new MaintenanceMode();
	}

	/**
	 * Set the maintenance mode
	 * @param enabled flag to enable/disable maintenance mode (null to not change)
	 * @param message optional message (null to not change, empty string to clear)
	 * @param banner flag to show message in banner (null to not change)
	 * @throws NodeException
	 */
	public static void set(Boolean enabled, String message, Boolean banner) throws NodeException {
		if (enabled != null && message != null) {
			NodeSetup.setKeyValue(NODESETUP_KEY.maintenancemode, message, enabled ? 1 : 0);
		} else if (enabled != null) {
			NodeSetup.setKeyValue(NODESETUP_KEY.maintenancemode, enabled ? 1 : 0);
		} else if (message != null) {
			NodeSetup.setKeyValue(NODESETUP_KEY.maintenancemode, message);
		}

		if (banner != null) {
			NodeSetup.setKeyValue(NODESETUP_KEY.maintenancebanner, banner ? 1 : 0);
		}
	}

	/**
	 * Create an instance with the current settings
	 * @throws NodeException
	 */
	protected MaintenanceMode() throws NodeException {
		NodeSetupValuePair modeSetting = NodeSetup.getKeyValue(NodeSetup.NODESETUP_KEY.maintenancemode);
		NodeSetupValuePair bannerSetting = NodeSetup.getKeyValue(NodeSetup.NODESETUP_KEY.maintenancebanner);

		if (modeSetting != null) {
			enabled = modeSetting.getIntValue() > 0;
			message = ObjectTransformer.getString(modeSetting.getTextValue(), "");
		}
		if (bannerSetting != null) {
			banner = bannerSetting.getIntValue() > 0;
		}
	}

	/**
	 * Check whether maintenance mode is enabled
	 * @return enabled flag
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Get current maintenance message
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Check whether maintenance message should be shown in the banner
	 * @return banner flag
	 */
	public boolean isBanner() {
		return banner;
	}
}
