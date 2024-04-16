package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.logger.LogCollector;
import com.gentics.contentnode.logger.StringListAppender;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

import io.reactivex.Observable;

/**
 * Scheduler task to purge page/form versions
 */
public class PurgeVersionsJob {
	/**
	 * The logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Configuration parameter for the version age (in months)
	 */
	public final static String VERSION_AGE_PARAM = "cn_versionage";

	/**
	 * Run the purge job and collect log output in the given string list
	 * @param out string list collecting the log output
	 * @throws NodeException
	 */
	public void purge(List<String> out) throws NodeException {
		try (LogCollector logCollector = new LogCollector(logger.getName(),
				new StringListAppender(PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build(), out))) {
			int timestamp = 0;
			try {
				int versionAgeInMonths = Integer.parseInt(NodeConfigRuntimeConfiguration.getPreferences().getProperty(VERSION_AGE_PARAM));
				Calendar now = Calendar.getInstance();
				now.add(Calendar.MONTH, -versionAgeInMonths);
				timestamp = (int)(now.getTimeInMillis() / 1000);
			} catch (NumberFormatException e) {
				throw new NodeException(String.format("Error while purging versions. Could not read configuration %s", VERSION_AGE_PARAM), e);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Starting job " + getClass().getName());
			}

			// check for valid values
			if (timestamp <= 0 || timestamp >= (System.currentTimeMillis() / 1000 - 60)) {
				throw new NodeException("Could not execute job " + getClass().getName() + ": invalid timestamp setting");
			}

			purgePages(timestamp);
			purgeForms(timestamp);
			if (logger.isInfoEnabled()) {
				logger.info("Job " + getClass().getName() + " finished successfully");
			}
		}
	}

	/**
	 * Purge versions for all pages
	 * @param timestamp timestamp
	 * @throws NodeException
	 * @throws JobExecutionException
	 */
	public void purgePages(int timestamp) throws NodeException {
		if (logger.isDebugEnabled()) {
			logger.debug("Start reading all page ids");
		}

		Set<Integer> pageIds = supply(() -> DBUtils.select("SELECT id FROM page", DBUtils.IDS));

		int allPages = pageIds.size();

		if (logger.isInfoEnabled()) {
			logger.info("Start purging versions older than " + timestamp + " for " + allPages + " pages");
		}

		int tenPerc = 0;
		int pagesHandled = 0;

		Set<Integer> handledVariants = new HashSet<Integer>();

		// iterate over all page id's
		for (Integer pageId : pageIds) {
			// TODO
//			abortWhenInterrupted();

			// omit the page if already handled as page variant of another page
			if (handledVariants.contains(pageId)) {
				continue;
			}

			try (Trx trx = new Trx(); WastebinFilter wb = new WastebinFilter(Wastebin.INCLUDE)) {
				// get the page
				Page page = trx.getTransaction().getObject(Page.class, pageId);

				if (logger.isDebugEnabled()) {
					logger.debug("Checking " + page);
				}

				List<Page> pageVariants = page.getPageVariants();

				// get all page versions
				List<NodeObjectVersion> pageVersions = new ArrayList<NodeObjectVersion>();
				for (Page variant : pageVariants) {
					pageVersions.addAll(Arrays.asList(variant.getVersions()));
				}

				// collect planned page versions
				List<NodeObjectVersion> plannedVersions = Observable.fromIterable(pageVariants).flatMap(variant -> {
					Set<NodeObjectVersion> versionSet = new HashSet<>();
					if (variant.getTimePubVersion() != null) {
						versionSet.add(variant.getTimePubVersion());
					}
					if (variant.getTimePubVersionQueue() != null) {
						versionSet.add(variant.getTimePubVersionQueue());
					}
					return Observable.fromIterable(versionSet);
				}).filter(version -> version != null).toList().blockingGet();

				// sort (we need the old versions first)
				Collections.sort(pageVersions, new Comparator<NodeObjectVersion>() {
					/* (non-Javadoc)
					 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
					 */
					public int compare(NodeObjectVersion v1, NodeObjectVersion v2) {
						return v1.getDate().getIntTimestamp() - v2.getDate().getIntTimestamp();
					}
				});

				// get the last version to keep. This is either the published
				// version, or the first version newer than the given timestamp,
				// whichever comes first

				// initialize with the youngest version, because it might be
				// that all versions of the page are too old and none is
				// published. In that case, we will at least keep the youngest
				// version
				NodeObjectVersion oldestToKeep = pageVersions.size() > 0 ? pageVersions.get(pageVersions.size() - 1) : null;

				for (NodeObjectVersion pageVersion : pageVersions) {
					if (pageVersion.isPublished() || pageVersion.getDate().getIntTimestamp() >= timestamp || plannedVersions.contains(pageVersion)) {
						oldestToKeep = pageVersion;
						break;
					}
				}

				if (oldestToKeep != null) {
					// if the oldest version to be kept is the oldest version of the page, we can skip this page
					if (oldestToKeep == pageVersions.get(0)) {
						if (logger.isDebugEnabled()) {
							if (oldestToKeep.isPublished()) {
								logger.debug("Oldest version of {" + page + "} must be kept, because it is the currently published version.");
							} else {
								logger.debug("Oldest version of {" + page + "} is too young to be purged.");
							}
						}
					} else {
						for (Page variant : pageVariants) {
							// purge the versions here
							variant.purgeOlderVersions(oldestToKeep);
							if (logger.isDebugEnabled()) {
								logger.debug("Purged versions older than " + oldestToKeep.getDate().getIntTimestamp() + " for " + variant);
							}
						}
					}
					
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not purging versions for " + page);
					}
				}

				trx.success();

				pagesHandled += pageVariants.size();
				for (Page variant : pageVariants) {
					handledVariants.add(ObjectTransformer.getInteger(variant.getId(), null));
				}
				if (pagesHandled * 10 / allPages > tenPerc) {
					tenPerc = pagesHandled * 10 / allPages;
					if (logger.isInfoEnabled()) {
						logger.info("Purged versions for " + pagesHandled + "/" + allPages + " pages (" + (tenPerc * 10) + "%)");
					}
				}
			}
		}
	}

	/**
	 * Purge versions for all forms
	 * @param timestamp timestamp
	 * @throws NodeException
	 */
	public void purgeForms(int timestamp) throws NodeException {
		if (logger.isDebugEnabled()) {
			logger.debug("Start reading all form ids");
		}

		Set<Integer> formIds = supply(() -> DBUtils.select("SELECT id FROM form", DBUtils.IDS));

		int allForms = formIds.size();

		if (logger.isInfoEnabled()) {
			logger.info("Start purging versions older than " + timestamp + " for " + allForms + " forms");
		}

		int tenPerc = 0;
		int formsHandled = 0;

		// iterate over all form ids
		for (Integer formId : formIds) {
			// TODO
//			abortWhenInterrupted();

			try (Trx trx = new Trx(); WastebinFilter wb = new WastebinFilter(Wastebin.INCLUDE)) {
				// get the form
				Form form = trx.getTransaction().getObject(Form.class, formId);

				if (logger.isDebugEnabled()) {
					logger.debug("Checking " + form);
				}

				purgeFormVersions(form, timestamp);

				trx.success();

				formsHandled++;
				if (formsHandled * 10 / allForms > tenPerc) {
					tenPerc = formsHandled * 10 / allForms;
					if (logger.isInfoEnabled()) {
						logger.info("Purged versions for " + formsHandled + "/" + allForms + " forms (" + (tenPerc * 10) + "%)");
					}
				}
			}
		}
	}

	/**
	 * Purge the old versions of the form
	 * @param form form
	 * @param timestamp timestamp
	 * @throws NodeException
	 */
	protected void purgeFormVersions(Form form, int timestamp) throws NodeException {
		// get all form versions
		List<NodeObjectVersion> formVersions = Arrays.asList(form.getVersions());

		// get planned object versions
		NodeObjectVersion plannedVersion = form.getTimePubVersion();

		// sort (we need the old versions first)
		Collections.sort(formVersions, new Comparator<NodeObjectVersion>() {
			/* (non-Javadoc)
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			public int compare(NodeObjectVersion v1, NodeObjectVersion v2) {
				return v1.getDate().getIntTimestamp() - v2.getDate().getIntTimestamp();
			}
		});

		// get the last version to keep. This is either the published
		// version, or the first version newer than the given timestamp,
		// whichever comes first

		// initialize with the youngest version, because it might be
		// that all versions of the form are too old and none is
		// published. In that case, we will at least keep the youngest
		// version
		NodeObjectVersion oldestToKeep = formVersions.size() > 0 ? formVersions.get(formVersions.size() - 1) : null;

		for (NodeObjectVersion formVersion : formVersions) {
			if (formVersion.isPublished() || formVersion.getDate().getIntTimestamp() >= timestamp || Objects.equals(formVersion, plannedVersion)) {
				oldestToKeep = formVersion;
				break;
			}
		}

		if (oldestToKeep != null) {
			// if the oldest version to be kept is the oldest version of the form, we can skip this form
			if (oldestToKeep == formVersions.get(0)) {
				if (logger.isDebugEnabled()) {
					if (oldestToKeep.isPublished()) {
						logger.debug("Oldest version of {" + form + "} must be kept, because it is the currently published version.");
					} else {
						logger.debug("Oldest version of {" + form + "} is too young to be purged.");
					}
				}
			} else {
				// purge the versions here
				form.purgeOlderVersions(oldestToKeep);
				if (logger.isDebugEnabled()) {
					logger.debug("Purged versions older than " + oldestToKeep.getDate().getIntTimestamp() + " for " + form);
				}
			}

		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not purging versions for " + form);
			}
		}
	}
}
