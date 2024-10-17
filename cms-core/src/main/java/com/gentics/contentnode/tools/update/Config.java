package com.gentics.contentnode.tools.update;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration POJO for the Implementation Update Tool
 */
public class Config {
	public final static String BASE_SHORT_PARAM = "b";

	public final static String BASE_LONG_PARAM = "base";

	public final static String USER_SHORT_PARAM = "u";

	public final static String USER_LONG_PARAM = "user";

	public final static String PASSWORD_SHORT_PARAM = "p";

	public final static String PASSWORD_LONG_PARAM = "password";

	public final static String PACKAGES_SHORT_PARAM = "P";

	public final static String PACKAGES_LONG_PARAM = "package";

	public final static String CR_SHORT_PARAM = "c";

	public final static String CR_LONG_PARAM = "contentrepository";

	public final static String TIMEOUT_SHORT_PARAM = "t";

	public final static String TIMEOUT_LONG_PARAM = "timeout";

	public final static String RELOAD_CONFIG_SHORT_PARAM = "r";

	public final static String RELOAD_CONFIG_LONG_PARAM = "reloadConfig";

	public final static String MAINTENANCE_MODE_SHORT_PARAM = "m";

	public final static String MAINTENANCE_MODE_LONG_PARAM = "maintenanceMode";

	public final static String MAINTENANCE_MESSAGE_SHORT_PARAM = "M";

	public final static String MAINTENANCE_MESSAGE_LONG_PARAM = "maintenanceMessage";

	public final static String REPUBLISH_PAGES_SHORT_PARAM = "rp";

	public final static String REPUBLISH_PAGES_LONG_PARAM = "republishPages";

	public final static String REPUBLISH_FILES_SHORT_PARAM = "rfi";

	public final static String REPUBLISH_FILES_LONG_PARAM = "republishFiles";

	public final static String REPUBLISH_FOLDERS_SHORT_PARAM = "rfo";

	public final static String REPUBLISH_FOLDERS_LONG_PARAM = "republishFolders";

	public final static String RESUME_SCHEDULER_LONG_PARAM = "resumeScheduler";

	public final static String RESUME_SCHEDULER_SHORT_PARAM = "rs";

	public final static String FILE_SHORT_PARAM = "f";

	public final static String FILE_LONG_PARAM = "file";

	public final static String TRIGGER_SYNC_PAGES_LONG_PARAM = "triggerSyncPages";

	public final static String TRIGGER_SYNC_PAGES_SHORT_PARAM = "ts";

	public final static String AWAIT_SYNC_PAGES_LONG_PARAM = "awaitSyncPages";

	public final static String AWAIT_SYNC_PAGES_SHORT_PARAM = "as";

	public final static String AWAIT_SYNC_PAGES_TIMEOUT_LONG_PARAM = "awaitSyncPagesTimeout";

	public final static String AWAIT_SYNC_PAGES_TIMEOUT_SHORT_PARAM = "ast";

	public final static String HELP_LONG_PARAM = "help";

	protected String base = "http://localhost";

	protected String user;

	protected String password;

	protected List<String> packages;

	protected List<String> crs;

	protected int timeout = 60 * 1000;

	protected boolean reloadConfig = false;

	protected boolean maintenanceMode = false;

	protected String maintenanceMessage;

	protected boolean republishPages = false;

	protected boolean republishFiles = false;

	protected boolean republishFolders = false;

	protected ResumeScheduler resumeScheduler = ResumeScheduler.always;

	protected boolean triggerSyncPages = false;

	protected boolean awaitSyncPages = false;

	protected int awaitSyncPagesTimeout = 10 * 60 * 1000;

	/**
	 * Create instance with default values
	 */
	public Config() {
	}

	/**
	 * Get the base URL
	 * @return base URL
	 */
	@JsonProperty(BASE_LONG_PARAM)
	public String getBase() {
		return base;
	}

	/**
	 * Set the base URL
	 * @param base URL
	 */
	public void setBase(String base) {
		this.base = base;
	}

	/**
	 * Get the user login name
	 * @return user name
	 */
	@JsonProperty(USER_LONG_PARAM)
	public String getUser() {
		return user;
	}

	/**
	 * Set the user login name
	 * @param user login name
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Get the password
	 * @return password
	 */
	@JsonProperty(PASSWORD_LONG_PARAM)
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password
	 * @param password password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the list of package names to synchronize
	 * @return list of package names
	 */
	@JsonProperty(PACKAGES_LONG_PARAM)
	public List<String> getPackages() {
		return packages;
	}

	/**
	 * Set the list of package names to synchronize
	 * @param packages list of package names
	 */
	public void setPackages(List<String> packages) {
		this.packages = packages;
	}

	/**
	 * Get the list of local or global CR IDs
	 * @return CR IDs
	 */
	@JsonProperty(CR_LONG_PARAM)
	public List<String> getCrs() {
		return crs;
	}

	/**
	 * Set the list of local or global CR IDs
	 * @param crs
	 */
	public void setCrs(List<String> crs) {
		this.crs = crs;
	}

	/**
	 * Set the wait timeout in ms
	 * @return wait timeout
	 */
	@JsonProperty(TIMEOUT_LONG_PARAM)
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Get the wait timeout in ms
	 * @param timeout in ms
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get flag to reload config
	 * @return reload config flag
	 */
	@JsonProperty(RELOAD_CONFIG_LONG_PARAM)
	public boolean isReloadConfig() {
		return reloadConfig;
	}

	/**
	 * Set the reload config flag
	 * @param reloadConfig flag
	 */
	public void setReloadConfig(boolean reloadConfig) {
		this.reloadConfig = reloadConfig;
	}

	/**
	 * Flag to set maintenance mode
	 * @return maintenance mode flag
	 */
	@JsonProperty(MAINTENANCE_MODE_LONG_PARAM)
	public boolean isMaintenanceMode() {
		return maintenanceMode;
	}

	/**
	 * Set maintenance mode flag
	 * @param maintenanceMode flag
	 */
	public void setMaintenanceMode(boolean maintenanceMode) {
		this.maintenanceMode = maintenanceMode;
	}

	/**
	 * Get the maintenance message
	 * @return maintenance message
	 */
	@JsonProperty(MAINTENANCE_MESSAGE_LONG_PARAM)
	public String getMaintenanceMessage() {
		return maintenanceMessage;
	}

	/**
	 * Set the maintenance message
	 * @param maintenanceMessage message
	 */
	public void setMaintenanceMessage(String maintenanceMessage) {
		this.maintenanceMessage = maintenanceMessage;
	}

	/**
	 * Flag to republish pages
	 * @return republish pages flag
	 */
	@JsonProperty(REPUBLISH_PAGES_LONG_PARAM)
	public boolean isRepublishPages() {
		return republishPages;
	}

	/**
	 * Set the republish pages flag
	 * @param republishPages flag
	 */
	public void setRepublishPages(boolean republishPages) {
		this.republishPages = republishPages;
	}

	/**
	 * Flag to republish files
	 * @return republish files flag
	 */
	@JsonProperty(REPUBLISH_FILES_LONG_PARAM)
	public boolean isRepublishFiles() {
		return republishFiles;
	}

	/**
	 * Set the republish files flag
	 * @param republishFiles flag
	 */
	public void setRepublishFiles(boolean republishFiles) {
		this.republishFiles = republishFiles;
	}

	/**
	 * Flag to republish folders
	 * @return republish folders flag
	 */
	@JsonProperty(REPUBLISH_FOLDERS_LONG_PARAM)
	public boolean isRepublishFolders() {
		return republishFolders;
	}

	/**
	 * Set the republish folders flag
	 * @param republishFolders flag
	 */
	public void setRepublishFolders(boolean republishFolders) {
		this.republishFolders = republishFolders;
	}

	/**
	 * Flag to trigger page sync
	 * @return flag value
	 */
	@JsonProperty(TRIGGER_SYNC_PAGES_LONG_PARAM)
	public boolean isTriggerSyncPages() {
		return triggerSyncPages;
	}

	/**
	 * Set the flag to trigger page sync
	 * @param triggerSyncPages flag value
	 */
	public void setTriggerSyncPages(boolean triggerSyncPages) {
		this.triggerSyncPages = triggerSyncPages;
	}

	/**
	 * Flag to await page sync
	 * @return flag value
	 */
	@JsonProperty(AWAIT_SYNC_PAGES_LONG_PARAM)
	public boolean isAwaitSyncPages() {
		return awaitSyncPages;
	}

	/**
	 * Set the flag to await page sync
	 * @param awaitSyncPages flag value
	 */
	public void setAwaitSyncPages(boolean awaitSyncPages) {
		this.awaitSyncPages = awaitSyncPages;
	}

	/**
	 * Timeout for awaiting page sync with template (in ms)
	 * @return timeout in ms
	 */
	@JsonProperty(AWAIT_SYNC_PAGES_TIMEOUT_LONG_PARAM)
	public int getAwaitSyncPagesTimeout() {
		return awaitSyncPagesTimeout;
	}

	/**
	 * Set timeout for awaiting page sync in ms
	 * @param awaitSyncPagesTimeout timeout in ms
	 */
	public void setAwaitSyncPagesTimeout(int awaitSyncPagesTimeout) {
		this.awaitSyncPagesTimeout = awaitSyncPagesTimeout;
	}

	/**
	 * Setting for resuming the publisher
	 * @return resume publisher setting
	 */
	@JsonProperty(RESUME_SCHEDULER_LONG_PARAM)
	public ResumeScheduler getResumeScheduler() {
		return resumeScheduler;
	}

	/**
	 * Set resume publisher setting
	 * @param resumeScheduler setting
	 */
	public void setResumeScheduler(ResumeScheduler resumeScheduler) {
		this.resumeScheduler = resumeScheduler;
	}

	/**
	 * Update configuration from cmd line parameters
	 * @param line cmd line
	 */
	public void update(CommandLine line) {
		if (line.hasOption(BASE_SHORT_PARAM)) {
			base = line.getOptionValue(BASE_SHORT_PARAM);
		}
		if (line.hasOption(USER_SHORT_PARAM)) {
			user = line.getOptionValue(USER_SHORT_PARAM);
		}
		if (line.hasOption(PASSWORD_SHORT_PARAM)) {
			password = line.getOptionValue(PASSWORD_SHORT_PARAM);
		}
		if (line.hasOption(PACKAGES_SHORT_PARAM)) {
			packages = Arrays.asList(line.getOptionValues(PACKAGES_SHORT_PARAM));
		}
		if (line.hasOption(CR_SHORT_PARAM)) {
			crs = Arrays.asList(line.getOptionValues(CR_SHORT_PARAM));
		}
		if (line.hasOption(TIMEOUT_SHORT_PARAM)) {
			timeout = Integer.parseInt(line.getOptionValue(TIMEOUT_SHORT_PARAM));
		}
		if (line.hasOption(RELOAD_CONFIG_SHORT_PARAM)) {
			reloadConfig = true;
		}
		if (line.hasOption(MAINTENANCE_MODE_SHORT_PARAM)) {
			maintenanceMode = true;
		}
		if (line.hasOption(MAINTENANCE_MESSAGE_SHORT_PARAM)) {
			maintenanceMessage = line.getOptionValue(MAINTENANCE_MESSAGE_SHORT_PARAM);
		}
		if (line.hasOption(REPUBLISH_PAGES_SHORT_PARAM)) {
			republishPages = true;
		}
		if (line.hasOption(REPUBLISH_FILES_SHORT_PARAM)) {
			republishFiles = true;
		}
		if (line.hasOption(REPUBLISH_FOLDERS_SHORT_PARAM)) {
			republishFolders = true;
		}
		if (line.hasOption(TRIGGER_SYNC_PAGES_SHORT_PARAM)) {
			triggerSyncPages = true;
		}
		if (line.hasOption(AWAIT_SYNC_PAGES_SHORT_PARAM)) {
			awaitSyncPages = true;
		}
		if (line.hasOption(AWAIT_SYNC_PAGES_TIMEOUT_SHORT_PARAM)) {
			awaitSyncPagesTimeout = Integer.parseInt(line.getOptionValue(AWAIT_SYNC_PAGES_TIMEOUT_SHORT_PARAM));
		}
	}

	/**
	 * Validate the configuration
	 * @throws Exception if configuration is invalid
	 */
	public void validate() throws Exception {
		if (StringUtils.isEmpty(base)) {
			throw new Exception("host must not be empty");
		}
		if (StringUtils.isEmpty(user)) {
			throw new Exception("user must not be empty");
		}
		if (StringUtils.isEmpty(password)) {
			throw new Exception("password must not be empty");
		}
	}

	/**
	 * Values for resume scheduler setting
	 */
	public static enum ResumeScheduler {
		/**
		 * Always resume scheduler when updating succeeds
		 */
		always,

		/**
		 * Only resume scheduler, if it was running before
		 */
		running,

		/**
		 * Never resume scheduler
		 */
		never
	}
}
