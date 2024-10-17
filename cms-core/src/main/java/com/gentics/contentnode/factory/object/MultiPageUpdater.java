package com.gentics.contentnode.factory.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.logging.log4j.Level;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.TimingStats;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Helper for updating multiple pages.
 * <ul>
 *   <li>Loads pages in batches</li>
 *   <li>Handles multiple pages in one transaction</li>
 *   <li>Handles page status
 *     <ul>
 *       <li>Published/Unmodified pages will be published</li>
 *       <li>Published/Modified pages will be modified but not published</li>
 *       <li>Planned pages will be modified and stay planned</li>
 *       <li>Queued pages will be modified and stay queued</li>
 *       <li>Offline pages will stay offline</li>
 *     </ul>
 *   </li>
 *   <li>Collects messages (up to a configurable limit)</li>
 *   <li>Counts modified pages, pages without permission and locked pages</li>
 * </ul>
 */
public class MultiPageUpdater {
	private final static NodeLogger logger = NodeLogger.getNodeLogger(MultiPageUpdater.class);

	/**
	 * Limit for pages handled in a single transaction (without commiting)
	 */
	private int commitAfter = 100;

	/**
	 * Maximum number of collected messages (0 means: no limit)
	 */
	private int maxMessages = 0;

	/**
	 * Batch size for loading pages
	 */
	private int batchSize = 100;

	/**
	 * Optional filter (filtered pages will be ignored)
	 */
	private Function<Page, Boolean> filter;

	/**
	 * Update consumer
	 */
	private Consumer<Page> updater;

	/**
	 * ID queue
	 */
	private Queue<Integer> ids = new LinkedList<>();

	/**
	 * Flag to mark whether pages with insufficient permissions should be counted as error
	 */
	private boolean insufficientPermissionIsError = true;

	/**
	 * Flag to mark whether locked pages should be counted as error
	 */
	private boolean lockedIsError = true;

	/**
	 * Create an instance with the updater implementation
	 * @param updater updater (must not be null)
	 */
	public MultiPageUpdater(Consumer<Page> updater) {
		Objects.requireNonNull(updater, "Updater must not be null");
		this.updater = updater;
	}

	/**
	 * Commit (but do not close) transaction after given number of pages. Default is 100
	 * @param commitAfter page count
	 * @return fluent API
	 */
	public MultiPageUpdater setCommitAfter(int commitAfter) {
		this.commitAfter = commitAfter;
		return this;
	}

	/**
	 * Set maximum number of collected messages. Default is 0 (keep all messages)
	 * @param maxMessages maximum number of messages
	 * @return fluent API
	 */
	public MultiPageUpdater setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
		return this;
	}

	/**
	 * Set batch size for loading pages. Default is 100
	 * @param batchSize batch size
	 * @return fluent API
	 */
	public MultiPageUpdater setBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	/**
	 * Set optional filter. Filtered pages are ignored (neither handled, nor counted)
	 * @param filter optional filter
	 * @return fluent API
	 */
	public MultiPageUpdater setFilter(Function<Page, Boolean> filter) {
		this.filter = filter;
		return this;
	}

	/**
	 * Set the page IDs
	 * @param ids page IDs
	 * @return fluent API
	 */
	public MultiPageUpdater setIds(Collection<Integer> ids) {
		this.ids.addAll(ids);
		return this;
	}

	/**
	 * Set whether pages with insufficient edit permission are counted as error. Default is true.
	 * If this is true, pages with insufficient permission will cause the result to be not OK, and the message will have Level ERROR, otherwise,
	 * the page is just ignored and counted and the message will have level INFO
	 * @param insufficientPermissionIsError flag
	 * @return fluent API
	 */
	public MultiPageUpdater setInsufficientPermissionIsError(boolean insufficientPermissionIsError) {
		this.insufficientPermissionIsError = insufficientPermissionIsError;
		return this;
	}

	/**
	 * Set whether locked pages are counted as error. Default is true.
	 * If this is true, locked pages will cause the result to be not OK, and the message will have Level ERROR, otherwise,
	 * the page is just ignored and counted and the message will have level INFO
	 * @param insufficientPermissionIsError flag
	 * @return fluent API
	 */
	public MultiPageUpdater setLockedIsError(boolean lockedIsError) {
		this.lockedIsError = lockedIsError;
		return this;
	}

	/**
	 * Execute the update
	 * @return result object
	 * @throws NodeException
	 */
	public UpdatePagesResult execute() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// we will be modifying many pages, to unlocking must be done at transaction commit
		// otherwise other users trying to lock the same pages will have errors due to lock wait timeouts
		t.getAttributes().put(NodeFactory.UNLOCK_AT_TRX_COMMIT, true);
		try (InstantPublishingTrx ip = new InstantPublishingTrx(false)) {
			int numUpdated = 0;
			int numErrors = 0;
			int numInTrx = 0;
			int numDone = 0;
			int numNoPermission = 0;
			int numLocked = 0;
			List<NodeMessage> msg = null;
			if (commitAfter > 0 && maxMessages > 0) {
				msg = new ArrayList<NodeMessage>(maxMessages);
			}
			int numPages = ids.size();
			long start = System.currentTimeMillis();
			if (logger.isDebugEnabled()) {
				logger.debug("Updating " + numPages + " pages");
			}

			// prepare statistics
			TimingStats getBatch = new TimingStats().as("Get next batch of pages").withLogger(logger);
			TimingStats applyFilter = new TimingStats().as("Apply filter").withLogger(logger);
			TimingStats checkPermissions = new TimingStats().as("Check permission").withLogger(logger);
			TimingStats getEditablePage = new TimingStats().as("Get editable page").withLogger(logger);
			TimingStats updatePage = new TimingStats().as("Update page").withLogger(logger);
			TimingStats savePage = new TimingStats().as("Save page").withLogger(logger);
			TimingStats publishPage = new TimingStats().as("Publish page").withLogger(logger);
			TimingStats unlockPage = new TimingStats().as("Unlock page").withLogger(logger);
			TimingStats commitTransaction = new TimingStats().as("Commit transaction").withLogger(logger);

			List<TimingStats> inLoop = Arrays.asList(applyFilter, checkPermissions, getEditablePage, updatePage,
					savePage, publishPage, unlockPage, commitTransaction);

			Collection<Integer> batch = getBatch();
			while (!batch.isEmpty()) {
				List<Page> pages = getBatch.apply(b -> t.getObjects(Page.class, b, false, false), batch);

				// reset statistics
				inLoop.forEach(TimingStats::reset);

				for (Page p : pages) {
					try {
						// if a filter is given, use it
						if (filter == null || applyFilter.apply(filter, p)) {
							if (!checkPermissions.apply(p1 -> t.canEdit(p1), p)) {
								I18nString message = new CNI18nString("rest.page.nopermission");
								message.setParameter("0", p.getId().toString());
								throw new InsufficientPrivilegesException(message.toString(), p, PermType.update);
							}

							boolean onlineAndNotModified = p.isOnline() && !p.isModified();
							p = getEditablePage.apply(id -> t.getObject(Page.class, id, true), p.getId());

							// getting the editable copy of the page might change the page to be modified, if missing tags were added
							boolean editablePageModified = p.isModified();

							updatePage.accept(updater, p);

							// save the page. If either something was saved or the status of the editable page was different from the
							// original status (meaning that the page was modified), we handle the original status
							boolean pageChanged = savePage.apply(p1 -> p1.save(true, false), p);

							if (pageChanged || editablePageModified) {
								if (onlineAndNotModified) {
									publishPage.accept(p1 -> p1.publish(0, null, false), p);
								}
							}
							unlockPage.accept(p1 -> p1.unlock(), p);

							numUpdated++;
							numInTrx++;

							if (commitAfter > 0 && numInTrx >= commitAfter) {
								commitTransaction.operate(() -> t.commit(false));
								numInTrx = 0;
							}
						}

						numDone++;
						if (logger.isDebugEnabled()) {
							long duration = System.currentTimeMillis() - start;
							long avgDuration = duration / numDone;
							long eta = avgDuration * (numPages - numDone);
							logger.debug("Done: " + numDone + "/" + numPages + ". ETA: " + DurationFormatUtils.formatDurationWords(eta, true, true));
						}
					} catch (NodeException e) {
						// an error occurred. If we must not commit in between, we simply throw the error
						if (commitAfter <= 0) {
							throw e;
						}

						Level level = Level.ERROR;
						if (e instanceof InsufficientPrivilegesException) {
							numNoPermission++;
							if (insufficientPermissionIsError) {
								numErrors++;
							} else {
								level = Level.INFO;
							}
						} else if (e instanceof ReadOnlyException) {
							numLocked++;
							if (lockedIsError) {
								numErrors++;
							} else {
								level = Level.INFO;
							}
						} else {
							numErrors++;
						}

						// collect some errors
						if (msg != null && msg.size() < maxMessages) {
							msg.add(new DefaultNodeMessage(level, Template.class, e.getLocalizedMessage()));
						}
					}
				}

				// commit the last batch
				if (commitAfter > 0 && numInTrx > 0) {
					commitTransaction.operate(() -> t.commit(false));
					numInTrx = 0;
				}

				// log statistics
				inLoop.forEach(TimingStats::logStatistics);

				// get next batch
				batch = getBatch();
			}
			getBatch.logStatistics();

			UpdatePagesResult result = new UpdatePagesResult(numUpdated, numPages, numErrors, numNoPermission, numLocked);
			if (msg != null) {
				result.getMessages().addAll(msg);
			}
			return result;
		}
	}

	/**
	 * Get the next batch of IDs from the internal queue. If the queue is empty, this returns an empty collection.
	 * @return next batch
	 */
	protected Collection<Integer> getBatch() {
		Collection<Integer> batch = new ArrayList<>();

		while (!ids.isEmpty() && batch.size() < batchSize) {
			batch.add(ids.remove());
		}

		return batch;
	}
}
