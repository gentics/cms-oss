package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.factory.Trx.operate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.logger.LogCollector;
import com.gentics.contentnode.logger.StringListAppender;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.LogFileArchiver;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.IntegerColumnRetriever;

/**
 * Enum containing all internal scheduler tasks
 */
public enum CoreInternalSchedulerTask implements InternalSchedulerTask {
	/**
	 * Task that purges old page/form versions
	 */
	purgeversions("Purge Versions", "Purge versions of pages and forms, which are older than the configured maximum version age.", out -> {
		PurgeVersionsJob job = new PurgeVersionsJob();
		job.purge(out);
		return true;
	}),

	/**
	 * Task that purges the wastebin
	 */
	purgewastebin("Purge Wastebin", "Purge objects from the wastebin, which are older than the configured maximum wastebin age", out -> {
		operate(t -> {
			try (LogCollector logCollector = new LogCollector("wastebin",
					new StringListAppender(PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build(), out))) {
				t.setInstantPublishingEnabled(false);
				IntegerColumnRetriever ex = new IntegerColumnRetriever("id");
				DBUtils.executeStatement("SELECT id FROM node", ex);

				List<Node> nodes = t.getObjects(Node.class, ex.getValues());
				for (Node node : nodes) {
					Map<Integer, Integer> result = node.purgeWastebin();
					out.add(String.format("Purged %d page(s), %d file(s), %d folder(s) for node %s",
							result.getOrDefault(Page.TYPE_PAGE_INTEGER, 0),
							result.getOrDefault(File.TYPE_FILE_INTEGER, 0),
							result.getOrDefault(Folder.TYPE_FOLDER_INTEGER, 0), I18NHelper.getName(node)));
				}
			}
		});
		return true;
	}),

	/**
	 * Task that purges logs (logcmd, logerror, scheduler_exection)
	 */
	purgelogs("Purge Logs", "Purge log entries from the database, which are older than the configured maximum log age", out -> {
		PurgeLogsJob job = new PurgeLogsJob();
		job.purge(out);
		return true;
	}),

	/**
	 * Task that purges inbox messages
	 */
	purgemessages("Purge Inbox Messsages", "Purge inbox messages, which are older than the configured maximum message age", out -> {
		PurgeMessagesJob job = new PurgeMessagesJob();
		job.purge(out);
		return true;
	}),


	/**
	 * Task that publishes dirted objects
	 */
	publish("Run Publish Process", "Run the publish process, which publishes all objects, that were modified or need to be republished.", out -> {
		// start the publish process
		boolean started = PublishController.startPublish(false, System.currentTimeMillis());
		if (!started) {
			// if the publish process was already running, we rejoined it
			out.add("Publish process is already running... rejoining");
		}
		// wait for the publish process to finish
		PublishController.joinPublisherLocally();

		// get the publish info
		PublishInfo publishInfo = PublishController.getPublishInfo();

		// output ERROR messages
		for (NodeMessage message : publishInfo.getMessages()) {
			if (message.getLevel().isMoreSpecificThan(Level.WARN)) {
				out.add(message.toString());
			}
		}

		// output information about published objects
		out.add(String.format("INFO: successfully published folders: %d", publishInfo.getPublishedFolderCount()));
		out.add(String.format("INFO: successfully published pages: %d", publishInfo.getPublishedPageCount()));
		out.add(String.format("INFO: successfully published files: %d", publishInfo.getPublishedFileCount()));
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.FORMS)) {
			out.add(String.format("INFO: successfully published forms: %d", publishInfo.getPublishedFormCount()));
		}

		// archive log files
		java.io.File logdir = new java.io.File(ConfigurationValue.LOGS_PATH.get(), "publish");
		try {
			new LogFileArchiver(logdir).archivePublishLogs();
		} catch (IOException e) {
			out.add("Error while archiving publish log files:");
			out.add(ExceptionUtils.getStackTrace(e));
		}

		return publishInfo.getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS;
	})
	;

	/**
	 * Default name
	 */
	private String name;

	/**
	 * Default description
	 */
	private String description;

	/**
	 * Task handler, which will get a list of strings, where log output can be added (line wise)
	 * and should return the success status
	 */
	private Function<List<String>, Boolean> taskHandler;

	/**
	 * Create instance with the task handler
	 * @param taskHandler task handler
	 */
	CoreInternalSchedulerTask(String name, String description, Function<List<String>, Boolean> taskHandler) {
		this.name = name;
		this.description = description;
		this.taskHandler = taskHandler;
	}

	@Override
	public String getCommand() {
		return name();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public boolean execute(List<String> out) throws NodeException {
		return taskHandler.apply(out);
	}
}
