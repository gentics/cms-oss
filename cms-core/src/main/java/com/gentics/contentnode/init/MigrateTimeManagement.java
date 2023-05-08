package com.gentics.contentnode.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.servlets.UdateChecker;
import com.gentics.lib.db.DB;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Initialization Job for migrating old timemanagement data
 */
public class MigrateTimeManagement extends InitJob {
	public final static int BATCH_SIZE = 1000;

	@Override
	public void execute() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			if (DB.fieldExists(t.getDBHandle(), "page", "time_start")) {
				pagesToMigrate().buffer(BATCH_SIZE).flatMap(this::load).flatMapCompletable(this::migrate).andThen(dropUnusedColumns()).andThen(checkTriggers())
						.doOnSubscribe(d -> {
							logger.info("Start migrating time management");
						}).doOnComplete(() -> {
							logger.info("Done migrating time management");
						}).blockingAwait();
			} else {
				logger.info("Column page.time_start does not exist any more, migration of time management already done");
			}
		} catch (SQLException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Load page data for pages that probably need to be migrated.
	 * @return observable emitting PageData instances
	 * @throws NodeException
	 */
	protected Observable<PageData> pagesToMigrate() throws NodeException {
		return Observable.fromIterable(DBUtils
				.select("SELECT id, time_start, time_end, time_pub, status, online FROM page WHERE deleted = 0 AND (time_start != 0 OR time_end != 0 OR time_pub != 0 OR status = 4)", rs -> {
					List<PageData> data = new ArrayList<>();
					while (rs.next()) {
						data.add(new PageData(rs));
					}
					return data;
				}));
	}

	/**
	 * Load batch of pages
	 * @param pageData list of PageData instances
	 * @return observable emitting Pairs of PageData and the Page
	 * @throws NodeException
	 */
	protected Observable<Pair<PageData, Page>> load(List<PageData> pageData) throws NodeException {
		logger.info(String.format("Loading batch of %d pages", pageData.size()));

		// prepare lookup map to find PageData instances by id
		Map<Integer, PageData> dataMap = pageData.stream().collect(Collectors.toMap(data -> data.id, Function.identity()));

		// batch-load the pages, combine each page with the correlating PageData and return observable
		Transaction t = TransactionManager.getCurrentTransaction();
		return Observable.fromIterable(
				t.getObjects(Page.class, dataMap.keySet()).stream().map(page -> Pair.of(dataMap.get(page.getId()), page)).collect(Collectors.toList()));
	}

	/**
	 * Migrate the page
	 * @param page page
	 * @return completable that completes when the page has been migrated
	 * @throws NodeException
	 */
	protected Completable migrate(Pair<PageData, Page> toMigrate) throws NodeException {
		return Completable.fromAction(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			PageData data = toMigrate.getLeft();
			Page page = toMigrate.getRight();
			logger.info(String.format("Migrating %s", page));

			page = t.getObject(page, true);
			if (data.timePub != 0) {
				if (data.timeStart != 0) {
					if (data.timeStart < data.timePub) {
						// time_start is before "publish At", so just migrate the time_start and forget about the "publish At" time
						migrateTimeStart(page, data);
					} else {
						// time_start is after the "publish At", so take the time_start time as new "publish At" time
						migratePublishAt(page, data, data.timeStart);
					}
				} else {
					// only "publish At" time found
					migratePublishAt(page, data, data.timePub);
				}
			} else {
				if (data.timeStart != 0) {
					// only time_start found, migrate to "publish At"
					migrateTimeStart(page, data);
				} else if (data.status == Page.STATUS_QUEUE) {
					migrateQueue(page, data);
				}
			}

			if (data.timeEnd != 0) {
				// time_end is migrated to "takeOffline At" (independent from all other settings)
				page.takeOffline(data.timeEnd);
			}

			t.dirtObjectCache(Page.class, data.id);
		});
	}

	/**
	 * Migrate time_start setting to "publish At"
	 * @param page page to migrate
	 * @param data old timemanagement data
	 * @throws NodeException
	 */
	protected void migrateTimeStart(Page page, PageData data) throws NodeException {
		// get version ID of published version (or null if no published version found)
		NodeObjectVersion publishedPageVersion = page.getPublishedVersion();
		Integer publishedPageVersionId = publishedPageVersion != null ? publishedPageVersion.getId() : null;

		switch (data.status) {
		case Page.STATUS_TIMEMANAGEMENT:
			// take page offline and set publishAt with published version
			DBUtils.update("UPDATE page SET online = ?, time_pub = ?, time_pub_version = ? WHERE id = ?", 0, data.timeStart,
					publishedPageVersionId, data.id);
			break;
		case Page.STATUS_MODIFIED:
			// page is modified, just set the publish At and published version (if found)
			DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ? WHERE id = ?", data.timeStart, publishedPageVersionId, data.id);
			break;
		case Page.STATUS_QUEUE:
			// page is in queue, just set the publish At and published version
			// into queue (if found), if no published version found, create a major version
			if (publishedPageVersion == null) {
				NodeObjectVersion majorVersion = createMajorVersion(page);
				DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ?, pub_queue = ?, time_pub_queue = ?, time_pub_version_queue = ? WHERE id = ?",
						0, null, page.getEditor().getId(), data.timeStart, majorVersion.getId(), data.id);
			} else {
				DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ?, pub_queue = ?, time_pub_queue = ?, time_pub_version_queue = ? WHERE id = ?",
						0, null, page.getEditor().getId(), data.timeStart, publishedPageVersionId, data.id);
			}
			break;
		}
	}

	/**
	 * Migrate old "publish At"
	 * @param page page to migrate
	 * @param data old timemanagement data
	 * @param publishAt new timestamp for publish At
	 * @throws NodeException
	 */
	protected void migratePublishAt(Page page, PageData data, int publishAt) throws NodeException {
		switch (data.status) {
		case Page.STATUS_TOPUBLISH_AT:
		case Page.STATUS_TIMEMANAGEMENT: {
			// create new major version
			NodeObjectVersion latest = createMajorVersion(page);
			// set publish At timestamp with new major version
			DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ? WHERE id = ?", publishAt, latest.getId(), data.id);
			break;
		}
		case Page.STATUS_QUEUE: {
			// create new major version
			NodeObjectVersion latest = createMajorVersion(page);
			// set publish At timestamp with new major version into queue
			DBUtils.update("UPDATE page SET time_pub = ?, time_pub_version = ?, pub_queue = ?, time_pub_queue = ?, time_pub_version_queue = ? WHERE id = ?", 0,
					null, page.getEditor().getId(), publishAt, latest.getId(), data.id);
			break;
		}
		default:
			// only update the "publish At" timestamp, if necessary
			if (publishAt != data.timePub) {
				DBUtils.update("UPDATE page SET time_pub = ? WHERE id = ?", publishAt, data.id);
			}
			break;
		}
	}

	/**
	 * Migrate page in queue
	 * @param page page
	 * @param data old timemanagement data
	 * @throws NodeException
	 */
	protected void migrateQueue(Page page, PageData data) throws NodeException {
		DBUtils.update("UPDATE page SET pub_queue = ? WHERE id = ?", page.getEditor().getId(), data.id);
	}

	/**
	 * Drop unused columns in page table
	 * @return completable that completes when the columns where dropped
	 * @throws NodeException
	 */
	protected Completable dropUnusedColumns() throws NodeException {
		return Completable.fromAction(() -> {
			logger.info("Dropping unused columns");
			DBUtils.update(
					"ALTER TABLE page DROP COLUMN status, DROP COLUMN time_start, DROP COLUMN time_end, DROP COLUMN time_mon, DROP COLUMN time_tue, DROP COLUMN time_wed, DROP COLUMN time_thu, DROP COLUMN time_fri, DROP COLUMN time_sat, DROP COLUMN time_sun");
		});
	}

	/**
	 * Check triggers
	 * @return completable that completes when triggers are checked/recreated
	 * @throws NodeException
	 */
	protected Completable checkTriggers() throws NodeException {
		return Completable.fromAction(() -> {
			logger.info("Recreating triggers");
			DB.clearTableFieldCache();
			PortalCache cache = PortalCache.getCache("gentics-portal-cachedb-metadata");
			if (cache != null) {
				cache.clear();
			}
			UdateChecker.recreateUpdateTrigger("page");
		});
	}

	/**
	 * Create a new major version (without making it the published version)
	 * @param page page
	 * @return new major version
	 * @throws NodeException
	 */
	protected NodeObjectVersion createMajorVersion(Page page) throws NodeException {
		PageFactory.createPageVersion(page, true, false, page.getEditor().getId());
		NodeObjectVersion[] versions = page.getVersions();
		if (versions.length > 0) {
			return versions[0];
		} else {
			throw new NodeException(String.format("Could not create new major version for page %s", page));
		}
	}

	/**
	 * Class for holding page id and old timemanagement data
	 */
	protected final static class PageData {
		/**
		 * Page ID
		 */
		protected int id;

		/**
		 * Value of time_start
		 */
		protected int timeStart;

		/**
		 * Value of time_end
		 */
		protected int timeEnd;

		/**
		 * Value of time_pub
		 */
		protected int timePub;

		/**
		 * Page status
		 */
		protected int status;

		/**
		 * Value of online
		 */
		protected int online;

		/**
		 * Create instance from current record
		 * @param rs recordset
		 * @throws SQLException
		 */
		protected PageData(ResultSet rs) throws SQLException {
			this.id = rs.getInt("id");
			this.timeStart = rs.getInt("time_start");
			this.timeEnd = rs.getInt("time_end");
			this.timePub = rs.getInt("time_pub");
			this.status = rs.getInt("status");
			this.online = rs.getInt("online");
		}
	}
}
