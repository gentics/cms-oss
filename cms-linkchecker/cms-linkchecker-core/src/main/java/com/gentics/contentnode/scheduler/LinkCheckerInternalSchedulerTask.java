package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodeSetup;
import com.gentics.contentnode.logger.LogCollector;
import com.gentics.contentnode.logger.StringListAppender;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.page.linkchecker.LinkChecker;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;
import com.gentics.contentnode.rest.resource.impl.internal.JobProgress;

/**
 * Internal scheduler tasks for the linkchecker
 */
public enum LinkCheckerInternalSchedulerTask implements InternalSchedulerTask {
	/**
	 * Task that checks links
	 */
	linkchecker("Link Checker", "Check validity of external links", out -> {
		LinkChecker.start();
		try (LogCollector logCollector = new LogCollector(LinkChecker.logger.getName(),
				new StringListAppender(PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build(), out))) {
			JobProgress progress = supply(() -> new JobProgress().setDone(0).setTotal(LinkChecker.getTotalLinks())
					.setStarted((int) (System.currentTimeMillis() / 1000L)));

			// find external links in pages, that have not yet been parsed
			operate(() -> LinkChecker.parseMissingPages());

			// remove external links from pages, where the feature is off
			operate(() -> LinkChecker.cleanEntries());

			// check all links
			operate(() -> progress.setTotal(LinkChecker.getTotalLinks()));
			LinkChecker.checkLinks(progress).blockingAwait();
			int finishTime = (int)(System.currentTimeMillis() / 1000L);
			progress.setFinished(finishTime);

			operate(() -> NodeSetup.setKeyValue(LinkChecker.NODESETUP_KEY, finishTime));

			out.add(String.format("Checked %d links in %d ms", progress.getDone(), (progress.getFinished() - progress.getStarted())));

			operate(() -> {
				Map<Node, Map<ExternalLinkStatus, Integer>> linkStats = LinkChecker.getLinkStats();
				if (linkStats.isEmpty()) {
					out.add("Link Checker is not activated for any node");
				}

				for (Map.Entry<Node, Map<ExternalLinkStatus, Integer>> entry : linkStats.entrySet()) {
					Node node = entry.getKey();
					Map<ExternalLinkStatus, Integer> data = entry.getValue();
					out.add(
							String.format("Node %s contains %d valid, %d invalid and %d unchecked links", node.getFolder().getName(),
									data.getOrDefault(ExternalLinkStatus.valid, 0), data.getOrDefault(ExternalLinkStatus.invalid, 0),
									data.getOrDefault(ExternalLinkStatus.unchecked, 0)));
				}
			});
		} finally {
			LinkChecker.stop();
		}
		return true;
	});

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
	LinkCheckerInternalSchedulerTask(String name, String description, Function<List<String>, Boolean> taskHandler) {
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
