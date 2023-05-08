package com.gentics.contentnode.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.DefaultPageVersionNumberGenerator;
import com.gentics.contentnode.factory.object.PageVersionNumberGenerator;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.lib.db.SQLExecutor;

/**
 * Background job that synchronizes the published versions of page variants
 */
public class SynchronizePageVariantVersions extends InitJob {
	/**
	 * Constant for the "published" stati
	 */
	protected final static List<Integer> PUBLISHED_STATI = Arrays.asList(Page.STATUS_PUBLISHED, Page.STATUS_TOPUBLISH);

	@Override
	public void execute() throws NodeException {
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();

		Transaction t = null;

		try {
			t = TransactionManager.getCurrentTransaction();

			// first get all contents that are used by more than one page
			if (logger.isInfoEnabled()) {
				logger.info("Get content IDs that are used in multiple pages.");
			}
			final Set<Integer> contentIds = new HashSet<Integer>();
			DBUtils.executeStatement("SELECT content.id FROM content, page WHERE content.id = page.content_id GROUP BY content.id HAVING COUNT(*) > 1", new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						contentIds.add(rs.getInt("id"));
					}
				}
			});

			if (logger.isInfoEnabled()) {
				logger.info("Found " + contentIds.size() + " content IDs.");
			}

			// iterate over all contents and get all page variants
			List<Content> contents = t.getObjects(Content.class, contentIds);
			for (Content content : contents) {
				if (logger.isInfoEnabled()) {
					logger.info("Processing " + content);
				}

				List<Page> pages = content.getPages();

				Map<Integer, NodeObjectVersion> pageVersionTimestamps = new HashMap<Integer, NodeObjectVersion>();
				int newestPublishedVersion = 0;
				for (Page page : pages) {
					if (!page.isOnline() || page.isModified()) {
						if (logger.isInfoEnabled()) {
							logger.info("Omit " + page + " (not online or locally modified)");
						}
						continue;
					}
					NodeObjectVersion lastVersion = page.getLastVersion();
					if (lastVersion == null || !lastVersion.isPublished()) {
						if (logger.isInfoEnabled()) {
							logger.info("Omit " + page + " (latest version is not the published one)");
						}
						continue;
					}

					int timestamp = lastVersion.getDate().getIntTimestamp();
					pageVersionTimestamps.put(ObjectTransformer.getInt(page.getId(), 0), lastVersion);
					newestPublishedVersion = Math.max(newestPublishedVersion, timestamp);
				}

				if (logger.isInfoEnabled()) {
					logger.info("Found " + pageVersionTimestamps.size() + " pages to check. Newest version timestamp is " + newestPublishedVersion);
				}

				for (Map.Entry<Integer, NodeObjectVersion> entry : pageVersionTimestamps.entrySet()) {
					int pageId = entry.getKey();
					NodeObjectVersion version = entry.getValue();

					if (logger.isInfoEnabled()) {
						logger.info("Processing " + version + " of page " + pageId);
					}

					if (version.getDate().getIntTimestamp() < newestPublishedVersion) {
						if (logger.isInfoEnabled()) {
							logger.info("Version is older than " + newestPublishedVersion + ": Comparing verions");
						}

						Page page = t.getObject(Page.class, pageId);
						if (page == null) {
							if (logger.isInfoEnabled()) {
								logger.info("Page " + pageId + " no longer found, omiting.");
							}
							continue;
						}

						Set<Integer> modifiedContenttags = page.getModifiedContenttags(version.getDate().getIntTimestamp(), newestPublishedVersion);
						if (modifiedContenttags.isEmpty()) {
							if (logger.isInfoEnabled()) {
								logger.info("No changes found between " + version.getDate().getIntTimestamp() + " and " + newestPublishedVersion + ": Omiting " + page);
							}
							continue;
						}

						String nextVersionNumber = gen.getNextVersionNumber(version.getNumber(), true);
						DBUtils.executeInsert("INSERT INTO nodeversion (timestamp, user_id, o_type, o_id, published, nodeversion) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {
							newestPublishedVersion,
							version.getEditor() == null ? 0 : version.getEditor().getId(),
							Page.TYPE_PAGE,
							pageId,
							true,
							nextVersionNumber
						});

						ActionLogger.logCmd(ActionLogger.MAJORVERSION, Page.TYPE_PAGE, pageId, 0, "created version " + nextVersionNumber);
						DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND timestamp != ?",
								new Object[] { Page.TYPE_PAGE, pageId, newestPublishedVersion });

						// dirt the page and the cache
						PublishQueue.dirtObject(page, Action.MODIFY, 0);
						t.dirtObjectCache(Page.class, pageId, true);

						if (logger.isInfoEnabled()) {
							logger.info("Created version " + nextVersionNumber + " @" + newestPublishedVersion + " for Page " + pageId);
						}
					} else {
						if (logger.isInfoEnabled()) {
							logger.info("No need to create new version for Page " + pageId);
						}
					}
				}
			}

			if (logger.isInfoEnabled()) {
				logger.info("Done synchronizing page variant versions");
			}

		} finally {
			if (t != null) {
				try {
					t.commit(false);
				} catch (TransactionException e) {
					throw new NodeException("Error while synchronizing page variant versions", e);
				}
			}
		}
	}
}
