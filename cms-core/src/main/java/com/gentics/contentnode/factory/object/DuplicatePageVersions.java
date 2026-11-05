package com.gentics.contentnode.factory.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Check and repair of duplicate page versions (SUP-20172), used by the internal scheduler tasks
 * {@link com.gentics.contentnode.scheduler.CoreInternalSchedulerTask#checkduplicatepageversions} and
 * {@link com.gentics.contentnode.scheduler.CoreInternalSchedulerTask#repairduplicatepageversions}.
 * <p>
 * Concurrent creation of page versions could create
 * <ul>
 * <li>several rows with the same id and nodeversiontimestamp in the versioned tables of pages,</li>
 * <li>several nodeversion entries of a page with the same timestamp,</li>
 * <li>several nodeversion entries of a page marked as published,</li>
 * <li>version numbers, which are not increasing.</li>
 * </ul>
 * The repair keeps one row per duplicate group (for value_nodeversion an "empty" row is only kept, if all rows are empty, otherwise the row with
 * the highest auto_id), keeps one nodeversion entry per timestamp (the published one, otherwise the one with the highest id), keeps only the newest
 * published version as published and renumbers versions with a version number, which is not greater than the number of the previous version.
 * The repair is done per content (all page variants share the content) in a separate transaction, while holding the same lock, which is used
 * for saving, publishing and restoring pages. Afterwards the pages of repaired contents are dirted (and republished, when they are online).
 * Duplicates, which do not belong to an existing content, are ignored.
 */
public class DuplicatePageVersions {
	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(DuplicatePageVersions.class);

	/**
	 * Classes of versioned objects (besides pages and contents), which are cached with their version timestamps
	 */
	protected final static List<Class<? extends NodeObject>> VERSIONED_CLASSES = List.of(ContentTag.class, Value.class, Overview.class,
			OverviewEntry.class, Datasource.class, DatasourceEntry.class);

	/**
	 * SQL expression for the content id of the page with id d.ref
	 */
	protected final static String PAGE_CONTENT = "(SELECT p.content_id FROM page p WHERE p.id = d.ref)";

	/**
	 * SQL expression for the content id of the contenttag with id d.ref (from the contenttag, or from its versions, if it was deleted)
	 */
	protected final static String CONTENTTAG_CONTENT = "COALESCE((SELECT ct.content_id FROM contenttag ct WHERE ct.id = d.ref), "
			+ "(SELECT MIN(ctv.content_id) FROM contenttag_nodeversion ctv WHERE ctv.id = d.ref))";

	/**
	 * SQL expression for the content id of the datasource with id d.ref (the content of the contenttag, which contains a value of a datasource part
	 * referencing the datasource). Contains placeholders for the ids of the datasource parts.
	 */
	protected final static String DATASOURCE_CONTENT = "COALESCE((SELECT MIN(ct.content_id) FROM value v JOIN contenttag ct ON ct.id = v.contenttag_id "
			+ "WHERE v.part_id IN (%1$s) AND v.value_ref = d.ref), "
			+ "(SELECT MIN(ctv.content_id) FROM value_nodeversion v JOIN contenttag_nodeversion ctv ON ctv.id = v.contenttag_id "
			+ "WHERE v.part_id IN (%1$s) AND v.value_ref = d.ref))";

	/**
	 * Versioned tables of pages (see {@link PageFactory#createPageVersion(Page, boolean, boolean, Integer, int)}) with the column referencing the
	 * owning object and the SQL expression for the content id
	 */
	enum VersionedTable {
		page("id", PAGE_CONTENT),
		page_alt_url("page_id", PAGE_CONTENT),
		contenttag("content_id", "d.ref"),
		value("contenttag_id", CONTENTTAG_CONTENT),
		ds("contenttag_id", CONTENTTAG_CONTENT),
		ds_obj("contenttag_id", CONTENTTAG_CONTENT),
		datasource("id", DATASOURCE_CONTENT),
		datasource_value("datasource_id", DATASOURCE_CONTENT);

		/**
		 * Column referencing the owning object
		 */
		private String refColumn;

		/**
		 * SQL expression for the content id
		 */
		private String contentExpression;

		/**
		 * Create an instance
		 * @param refColumn column referencing the owning object
		 * @param contentExpression SQL expression for the content id
		 */
		VersionedTable(String refColumn, String contentExpression) {
			this.refColumn = refColumn;
			this.contentExpression = contentExpression;
		}

		/**
		 * Get the name of the versioned table
		 * @return table name
		 */
		String getVersionTable() {
			return name() + "_nodeversion";
		}
	}

	/**
	 * Group of rows with the same id and nodeversiontimestamp in a versioned table
	 * @param table versioned table
	 * @param id record id
	 * @param timestamp version timestamp
	 */
	record DuplicateGroup(VersionedTable table, int id, int timestamp) {
	}

	/**
	 * Row of a versioned table
	 * @param autoId auto_id
	 * @param latest nodeversionlatest
	 * @param empty true if the row is "empty" (only for value_nodeversion)
	 */
	record VersionRow(int autoId, int latest, boolean empty) {
	}

	/**
	 * Nodeversion entry of a page
	 * @param id id
	 * @param timestamp version timestamp
	 * @param published published flag
	 * @param number version number
	 */
	record PageVersionRow(int id, int timestamp, boolean published, String number) {
	}

	/**
	 * Result of the detection
	 */
	static class Detection {
		/**
		 * Duplicate groups in the versioned tables per content id
		 */
		Map<Integer, List<DuplicateGroup>> groups = new TreeMap<>();

		/**
		 * Ids of the pages with defects in their nodeversion entries per content id
		 */
		Map<Integer, Set<Integer>> defectPageIds = new TreeMap<>();

		/**
		 * Get the ids of the affected contents
		 * @return sorted set of content ids
		 */
		Set<Integer> getContentIds() {
			Set<Integer> contentIds = new TreeSet<>(groups.keySet());
			contentIds.addAll(defectPageIds.keySet());
			return contentIds;
		}
	}

	/**
	 * Check for duplicate page versions and output the number of affected pages. Nothing is changed.
	 * @param out list for the output lines
	 * @return true
	 * @throws NodeException
	 */
	public static boolean check(List<String> out) throws NodeException {
		Set<Integer> contentIds = Trx.supply(() -> detect()).getContentIds();
		List<Integer> pageIds = Trx.supply(() -> getPageIds(contentIds));

		if (pageIds.isEmpty()) {
			out.add("Nothing to repair");
		} else {
			out.add("Pages with duplicate versions: %d".formatted(pageIds.size()));
		}

		return true;
	}

	/**
	 * Repair duplicate page versions. Every content is repaired in a separate transaction. Errors are logged and the repair continues with the next
	 * content.
	 * @param out list for the output lines
	 * @return true if all contents were repaired, false if an error occurred
	 * @throws NodeException
	 */
	public static boolean repair(List<String> out) throws NodeException {
		Detection detection = Trx.supply(() -> detect());
		Set<Integer> contentIds = detection.getContentIds();

		if (contentIds.isEmpty()) {
			out.add("Nothing to repair");
			return true;
		}

		boolean success = true;
		int repairedPages = 0;

		for (Integer contentId : contentIds) {
			List<DuplicateGroup> groups = detection.groups.getOrDefault(contentId, List.of());
			Set<Integer> defectPageIds = detection.defectPageIds.getOrDefault(contentId, Set.of());

			try {
				// the transaction is started after the lock was acquired and committed before the lock is released
				repairedPages += Operator.executeLockedInCurrentThread(Operator.lock(LockType.contentId, contentId),
						() -> Trx.supply(() -> repairContent(contentId, groups, defectPageIds)));
			} catch (NodeException e) {
				logger.error("Error while repairing content %d".formatted(contentId), e);
				out.add("Content %d: error: %s".formatted(contentId, e.getMessage()));
				success = false;
			}
		}

		if (repairedPages > 0) {
			// versioned objects are cached with their version timestamp, and dirting only removes the current version from the cache
			for (Class<? extends NodeObject> clazz : VERSIONED_CLASSES) {
				NodeFactory.getInstance().clear(clazz);
			}
		}

		out.add("Repaired pages: %d".formatted(repairedPages));
		logger.info(out.get(out.size() - 1));

		return success;
	}

	/**
	 * Detect duplicate page versions
	 * @return detection result
	 * @throws NodeException
	 */
	static Detection detect() throws NodeException {
		Detection detection = new Detection();
		String datasourcePartIds = PageFactory.getDatasourcePartIds();

		for (VersionedTable table : VersionedTable.values()) {
			String contentExpression = table.contentExpression.formatted(datasourcePartIds);
			String sql = "SELECT d.id, d.nodeversiontimestamp, (SELECT c.id FROM content c WHERE c.id = " + contentExpression + ") content_id FROM ("
					+ "SELECT id, nodeversiontimestamp, MIN(`" + table.refColumn + "`) ref FROM `" + table.getVersionTable()
					+ "` GROUP BY id, nodeversiontimestamp HAVING COUNT(*) > 1) d ORDER BY d.id, d.nodeversiontimestamp";

			DBUtils.select(sql, rs -> {
				while (rs.next()) {
					int contentId = rs.getInt("content_id");
					if (!rs.wasNull()) {
						detection.groups.computeIfAbsent(contentId, key -> new ArrayList<>())
								.add(new DuplicateGroup(table, rs.getInt("id"), rs.getInt("nodeversiontimestamp")));
					}
				}
				return null;
			});
		}

		// pages with more than one nodeversion entry with the same timestamp
		Set<Integer> pageIds = new TreeSet<>(DBUtils.select(
				"SELECT o_id id FROM nodeversion WHERE o_type = ? GROUP BY o_id, timestamp HAVING COUNT(*) > 1", ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
				}, DBUtils.IDLIST));

		// pages with more than one published version
		pageIds.addAll(DBUtils.select("SELECT o_id id FROM nodeversion WHERE o_type = ? AND published = 1 GROUP BY o_id HAVING COUNT(*) > 1", ps -> {
			ps.setInt(1, Page.TYPE_PAGE);
		}, DBUtils.IDLIST));

		// pages with a version number, which is not greater than the number of the previous version
		String major = "CAST(SUBSTRING_INDEX(%s, '.', 1) AS UNSIGNED)";
		String minor = "CAST(SUBSTRING_INDEX(%s, '.', -1) AS UNSIGNED)";
		pageIds.addAll(DBUtils.select("SELECT DISTINCT x.o_id id FROM ("
				+ "SELECT o_id, nodeversion, LAG(nodeversion) OVER (PARTITION BY o_id ORDER BY timestamp, id) prev FROM nodeversion WHERE o_type = ?) x "
				+ "WHERE x.nodeversion IS NOT NULL AND x.prev IS NOT NULL AND (" + major.formatted("x.nodeversion") + " < " + major.formatted("x.prev")
				+ " OR (" + major.formatted("x.nodeversion") + " = " + major.formatted("x.prev") + " AND " + minor.formatted("x.nodeversion") + " <= "
				+ minor.formatted("x.prev") + "))", ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
				}, DBUtils.IDLIST));

		for (Map.Entry<Integer, Integer> entry : getContentIds(pageIds).entrySet()) {
			detection.defectPageIds.computeIfAbsent(entry.getValue(), key -> new TreeSet<>()).add(entry.getKey());
		}

		return detection;
	}

	/**
	 * Repair the duplicate page versions of the given content. This method must be called in a separate transaction, which is committed
	 * afterwards.
	 * @param contentId content id
	 * @param groups duplicate groups of the content
	 * @param defectPageIds ids of the pages of the content with defects in their nodeversion entries
	 * @return number of repaired pages (all pages of the content, if something was changed, 0 otherwise)
	 * @throws NodeException
	 */
	static int repairContent(int contentId, List<DuplicateGroup> groups, Set<Integer> defectPageIds) throws NodeException {
		boolean changed = false;

		for (DuplicateGroup group : groups) {
			changed |= repairGroup(group);
		}

		for (Integer pageId : defectPageIds) {
			changed |= repairPageVersions(pageId);
		}

		if (!changed) {
			return 0;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		List<Integer> pageIds = getPageIds(List.of(contentId));

		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			for (Integer pageId : pageIds) {
				t.dirtObjectCache(Page.class, pageId);
				Page page = t.getObject(Page.class, pageId);

				if (page != null && page.isOnline()) {
					page.dirtPage(0);
				}
			}
		}

		return pageIds.size();
	}

	/**
	 * Repair the given duplicate group: keep one row and delete the others
	 * @param group duplicate group
	 * @return true if rows were deleted
	 * @throws NodeException
	 */
	static boolean repairGroup(DuplicateGroup group) throws NodeException {
		VersionedTable table = group.table();
		boolean checkEmpty = table == VersionedTable.value;
		String versionTable = table.getVersionTable();

		List<VersionRow> rows = DBUtils.select("SELECT auto_id, nodeversionlatest" + (checkEmpty ? ", value_text, value_ref, info" : "") + " FROM `"
				+ versionTable + "` WHERE id = ? AND nodeversiontimestamp = ? ORDER BY auto_id", ps -> {
					ps.setInt(1, group.id());
					ps.setInt(2, group.timestamp());
				}, rs -> {
					List<VersionRow> list = new ArrayList<>();
					while (rs.next()) {
						boolean empty = checkEmpty && StringUtils.isEmpty(rs.getString("value_text")) && rs.getInt("value_ref") == 0
								&& rs.getInt("info") == 0;
						list.add(new VersionRow(rs.getInt("auto_id"), rs.getInt("nodeversionlatest"), empty));
					}
					return list;
				});

		if (rows.size() < 2) {
			return false;
		}

		// empty rows are only kept, if all rows are empty. Of the remaining rows, the row with the highest auto_id is kept
		boolean allEmpty = rows.stream().allMatch(VersionRow::empty);
		VersionRow keep = rows.stream().filter(row -> allEmpty || !row.empty()).reduce((first, second) -> second).get();
		List<Integer> delete = rows.stream().filter(row -> row != keep).map(VersionRow::autoId).collect(Collectors.toList());

		DBUtils.update("DELETE FROM `" + versionTable + "` WHERE auto_id IN (" + StringUtils.repeat("?", delete.size(), ",") + ")", delete.toArray());

		if (keep.latest() == 0 && rows.stream().anyMatch(row -> row.latest() == 1)) {
			DBUtils.update("UPDATE `" + versionTable + "` SET nodeversionlatest = ? WHERE auto_id = ?", 1, keep.autoId());
		}

		return true;
	}

	/**
	 * Repair the nodeversion entries of the given page: keep one entry per timestamp, keep only the newest published version as published and
	 * renumber versions, which have a version number not greater than the number of the previous version
	 * @param pageId page id
	 * @return true if something was changed
	 * @throws NodeException
	 */
	static boolean repairPageVersions(int pageId) throws NodeException {
		boolean changed = false;
		List<PageVersionRow> versions = readPageVersions(pageId);

		if (versions.stream().anyMatch(v -> v.number() == null)) {
			// generate the missing version numbers
			PageFactory.loadPageVersions(pageId);
			versions = readPageVersions(pageId);
		}

		// keep one nodeversion entry per timestamp: the published one, otherwise the one with the highest id
		Map<Integer, List<PageVersionRow>> perTimestamp = versions.stream()
				.collect(Collectors.groupingBy(PageVersionRow::timestamp, LinkedHashMap::new, Collectors.toList()));
		List<PageVersionRow> remaining = new ArrayList<>();

		for (List<PageVersionRow> sameTimestamp : perTimestamp.values()) {
			PageVersionRow keep = sameTimestamp.stream().filter(PageVersionRow::published).reduce((first, second) -> second)
					.orElse(sameTimestamp.get(sameTimestamp.size() - 1));
			remaining.add(keep);

			List<Integer> delete = sameTimestamp.stream().filter(v -> v != keep).map(PageVersionRow::id).collect(Collectors.toList());
			if (delete.isEmpty()) {
				continue;
			}

			// the planned publishing references the nodeversion entries (with ON DELETE RESTRICT)
			String in = "(" + StringUtils.repeat("?", delete.size(), ",") + ")";
			List<Object> params = new ArrayList<>();
			params.add(keep.id());
			params.addAll(delete);
			DBUtils.update("UPDATE page SET time_pub_version = ? WHERE time_pub_version IN " + in, params.toArray());
			DBUtils.update("UPDATE page SET time_pub_version_queue = ? WHERE time_pub_version_queue IN " + in, params.toArray());
			DBUtils.update("DELETE FROM nodeversion WHERE id IN " + in, delete.toArray());
			changed = true;
		}

		// only the newest published version is the published version (see PublishableNodeObject.getPublishedVersion())
		List<PageVersionRow> published = remaining.stream().filter(PageVersionRow::published).collect(Collectors.toList());
		if (published.size() > 1) {
			List<Integer> unpublish = published.subList(0, published.size() - 1).stream().map(PageVersionRow::id).collect(Collectors.toList());
			DBUtils.update("UPDATE nodeversion SET published = 0 WHERE id IN (" + StringUtils.repeat("?", unpublish.size(), ",") + ")",
					unpublish.toArray());
			changed = true;
		}

		// renumber versions with a number, which is not greater than the number of the previous version
		DefaultPageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();
		int[] previous = null;
		String previousNumber = null;

		for (PageVersionRow version : remaining) {
			String number = version.number();
			int[] current = gen.parseVersionNumber(number);

			if (previous != null && (current[0] < previous[0] || (current[0] == previous[0] && current[1] <= previous[1]))) {
				number = gen.getNextVersionNumber(previousNumber, current[1] == 0);
				current = gen.parseVersionNumber(number);
				DBUtils.update("UPDATE nodeversion SET nodeversion = ? WHERE id = ?", number, version.id());
				changed = true;
			}

			previous = current;
			previousNumber = number;
		}

		return changed;
	}

	/**
	 * Read the nodeversion entries of the page, ordered by timestamp and id
	 * @param pageId page id
	 * @return list of nodeversion entries
	 * @throws NodeException
	 */
	static List<PageVersionRow> readPageVersions(int pageId) throws NodeException {
		return DBUtils.select("SELECT id, timestamp, published, nodeversion FROM nodeversion WHERE o_type = ? AND o_id = ? ORDER BY timestamp, id",
				ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
					ps.setInt(2, pageId);
				}, rs -> {
					List<PageVersionRow> list = new ArrayList<>();
					while (rs.next()) {
						list.add(new PageVersionRow(rs.getInt("id"), rs.getInt("timestamp"), rs.getBoolean("published"), rs.getString("nodeversion")));
					}
					return list;
				});
	}

	/**
	 * Get the content ids of the given pages
	 * @param pageIds page ids
	 * @return map of page ids to content ids (not existing pages are not contained)
	 * @throws NodeException
	 */
	static Map<Integer, Integer> getContentIds(Collection<Integer> pageIds) throws NodeException {
		Map<Integer, Integer> contentIds = new HashMap<>();

		if (!pageIds.isEmpty()) {
			DBUtils.executeMassStatement("SELECT id, content_id FROM page WHERE id IN", pageIds, 1, new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						contentIds.put(rs.getInt("id"), rs.getInt("content_id"));
					}
				}
			});
		}

		return contentIds;
	}

	/**
	 * Get the ids of the pages of the given contents
	 * @param contentIds content ids
	 * @return list of page ids
	 * @throws NodeException
	 */
	static List<Integer> getPageIds(Collection<Integer> contentIds) throws NodeException {
		List<Integer> pageIds = new ArrayList<>();

		if (!contentIds.isEmpty()) {
			DBUtils.executeMassStatement("SELECT id FROM page WHERE content_id IN", contentIds, 1, new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						pageIds.add(rs.getInt("id"));
					}
				}
			});
		}

		return pageIds;
	}
}
