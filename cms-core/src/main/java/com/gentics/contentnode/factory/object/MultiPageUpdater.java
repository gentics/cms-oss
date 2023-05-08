package com.gentics.contentnode.factory.object;

import java.util.ArrayList;
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

			Collection<Integer> batch = getBatch();
			while (!batch.isEmpty()) {
				List<Page> pages = t.getObjects(Page.class, batch, false, false);
				for (Page p : pages) {
					try {
						// if a filter is given, use it
						if (filter == null || filter.apply(p)) {
							if (!t.canEdit(p)) {
								I18nString message = new CNI18nString("rest.page.nopermission");
								message.setParameter("0", p.getId().toString());
								throw new InsufficientPrivilegesException(message.toString(), p, PermType.update);
							}

							boolean onlineAndNotModified = p.isOnline() && !p.isModified();
							p = t.getObject(Page.class, p.getId(), true);

							// getting the editable copy of the page might change the page to be modified, if missing tags were added
							boolean editablePageModified = p.isModified();

							updater.accept(p);

							// save the page. If either something was saved or the status of the editable page was different from the
							// original status (meaning that the page was modified), we handle the original status
							if (p.save(true, false) || editablePageModified) {
								if (onlineAndNotModified) {
									p.publish(0, null, false);
								}
							}
							p.unlock();

							numUpdated++;
							numInTrx++;

							if (commitAfter > 0 && numInTrx >= commitAfter) {
								t.commit(false);
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
				// get next batch
				batch = getBatch();
			}
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
