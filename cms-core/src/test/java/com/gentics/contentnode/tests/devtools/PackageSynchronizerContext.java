package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.clean;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.rules.ExternalResource;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.IFileWatcher;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Testrule for preparing/cleaning package directory and starting/stopping the synchronizer
 */
public class PackageSynchronizerContext extends ExternalResource {
	/**
	 * Maximum wait time for sync
	 */
	public final static int MAX_WAIT_MS = 10000;

	/**
	 * sleep time between sync checks
	 */
	public final static int WAIT_SLEEP_MS = 100;

	/**
	 * Packages root directory
	 */
	protected File packagesRoot;

	/**
	 * Get the packages root directory
	 * @return packages root directory
	 */
	public File getPackagesRoot() {
		return packagesRoot;
	}

	/**
	 * Set the monitor class for the synchronizer.
	 * The synchronizer will be stopped, reconfigured and started
	 * @param monitorClass monitor class
	 * @throws NodeException
	 */
	public void setMonitorClass(Class<? extends IFileWatcher> monitorClass) throws NodeException {
		Synchronizer.stop();

		// configure the synchronizer to use the monitorClass
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		@SuppressWarnings("unchecked")
		Map<String, String> synchronizerProperties = prefs.getPropertyMap(Synchronizer.SYNCHRONIZER_PREFERENCES);

		if (synchronizerProperties == null) {
			synchronizerProperties = new HashMap<>();
		}
		synchronizerProperties.put("class", monitorClass.getName());
		synchronizerProperties.put("interval", "100");
		prefs.setPropertyMap(Synchronizer.SYNCHRONIZER_PREFERENCES, synchronizerProperties);

		Synchronizer.start();
	}

	/**
	 * Stop and start the synchronizer. This can be used to remove any currently queued events
	 * @throws NodeException
	 */
	public void restart() throws NodeException {
		Synchronizer.stop();
		Synchronizer.start();
	}

	@Override
	protected void before() throws Throwable {
		packagesRoot = new File(ConfigurationValue.PACKAGES_PATH.get());

		// create clean package directory
		packagesRoot.mkdirs();

		// start and enable the synchronizer
		Synchronizer.start();
		Synchronizer.enable(1);
	}

	@Override
	protected void after() {
		Synchronizer.disable();
		Synchronizer.stop();
		try {
			clean(packagesRoot, MAX_WAIT_MS, WAIT_SLEEP_MS);
		} catch (InterruptedException e) {
		}
	}
}
