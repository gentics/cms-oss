package com.gentics.contentnode.init;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;

/**
 * Init job that migrates an existing scheduler.suspend file into nodesetup table
 */
public class MigrateSchedulerSuspend extends InitJob {
	protected final static String FILENAME = "../tmp/scheduler.suspend";

	@Override
	public void execute() throws NodeException {
		Properties props = NodeConfigRuntimeConfiguration.getDefault().getConfigurationProperties();
		String nodepath = props.getProperty("contentnode.nodepath");

		File suspendFile = new File(nodepath, FILENAME);

		if (suspendFile.exists()) {
			String contents = null;
			try (InputStream in = new FileInputStream(suspendFile)) {
				contents = StringUtils.readStream(in);
			} catch (IOException e) {
				throw new NodeException(String.format("Error while migrating file %s:", suspendFile), e);
			}

			Map<String, Object> id = new HashMap<>();
			id.put("name", SchedulerFactory.SCHEDULER_SUSPEND_NAME);

			Map<String, Object> data = new HashMap<>();
			data.put("intvalue", 1);
			data.put("textvalue", "");

			if (!StringUtils.isEmpty(contents) && !"undefined".equals(contents.trim())) {
				data.put("textvalue", contents);
			}

			DBUtils.updateOrInsert("nodesetup", id, data);

			TransactionManager.getCurrentTransaction().commit(false);

			suspendFile.delete();
		}
	}
}
