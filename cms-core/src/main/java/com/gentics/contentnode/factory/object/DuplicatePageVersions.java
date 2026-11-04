package com.gentics.contentnode.factory.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * for creating page versions. Afterwards the pages of repaired contents are dirted (and republished, when they are online).
 */
public class DuplicatePageVersions {
	/**
	 * Logger
	 */
	public static NodeLogger logger = NodeLogger.getNodeLogger(DuplicatePageVersions.class);

	/**
	 * Maximum number of contents, for which the check does a dry-run of the repair
	 */
	public final static int SAMPLE_SIZE = 20;

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
	 * @param rows number of rows
	 * @param removedDiffers true if the rows differ in nodeversionremoved
	 * @param dataDiffers true if the rows differ in the versioned data
	 * @param contentId id of the content, null if the rows do not belong to an existing content
	 */
	record DuplicateGroup(VersionedTable table, int id, int timestamp, int rows, boolean removedDiffers, boolean dataDiffers, Integer contentId) {
	}

	/**
	 * Defects of the nodeversion entries of a page
	 * @param pageId page id
	 * @param contentId content id, null if the page does not exist
	 * @param duplicateGroups number of timestamps with more than one nodeversion entry
	 * @param duplicateRows number of nodeversion entries, which would be deleted
	 * @param multiplePublished true if more than one version is marked as published
	 * @param numbersNotIncreasing true if the version numbers are not increasing
	 */
	record PageVersionDefects(int pageId, Integer contentId, int duplicateGroups, int duplicateRows, boolean multiplePublished,
			boolean numbersNotIncreasing) {
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
		 * Duplicate groups in the versioned tables
		 */
		List<DuplicateGroup> groups = new ArrayList<>();

		/**
		 * Defects of nodeversion entries per page id
		 */
		Map<Integer, PageVersionDefects> pageDefects = new TreeMap<>();

		/**
		 * Get the ids of the affected (existing) contents
		 * @return sorted set of content ids
		 */
		Set<Integer> getContentIds() {
			Set<Integer> contentIds = new TreeSet<>();
			groups.stream().filter(g -> g.contentId() != null).forEach(g -> contentIds.add(g.contentId()));
			pageDefects.values().stream().filter(d -> d.contentId() != null).forEach(d -> contentIds.add(d.contentId()));
			return contentIds;
		}

		/**
		 * Get the duplicate groups of the given content
		 * @param contentId content id (null for groups, which do not belong to an existing content)
		 * @return list of duplicate groups
		 */
		List<DuplicateGroup> getGroups(Integer contentId) {
			return groups.stream().filter(g -> Objects.equals(g.contentId(), contentId)).collect(Collectors.toList());
		}

		/**
		 * Get the ids of the pages of the given content with defects in their nodeversion entries
		 * @param contentId content id (null for pages, which do not exist)
		 * @return sorted set of page ids
		 */
		Set<Integer> getDefectPageIds(Integer contentId) {
			return pageDefects.values().stream().filter(d -> Objects.equals(d.contentId(), contentId)).map(PageVersionDefects::pageId)
					.collect(Collectors.toCollection(TreeSet::new));
		}

		/**
		 * Check whether there are defects, which do not belong to an existing content
		 * @return true if there are such defects
		 */
		boolean hasOrphans() {
			return !getGroups(null).isEmpty() || !getDefectPageIds(null).isEmpty();
		}

		/**
		 * Check whether nothing was detected
		 * @return true if nothing was detected
		 */
		boolean isEmpty() {
			return groups.isEmpty() && pageDefects.isEmpty();
		}
	}

	/**
	 * Result of the repair of a content
	 */
	static class RepairResult {
		/**
		 * Number of deleted rows in versioned tables
		 */
		int deletedRows;

		/**
		 * Number of deleted nodeversion entries
		 */
		int deletedVersions;

		/**
		 * Number of versions marked as not published
		 */
		int unpublishedVersions;

		/**
		 * Number of renumbered versions
		 */
		int renumberedVersions;

		/**
		 * Check whether anything was changed
		 * @return true if something was changed
		 */
		boolean isChanged() {
			return deletedRows + deletedVersions + unpublishedVersions + renumberedVersions > 0;
		}

		/**
		 * Add the given result to this result
		 * @param other other result
		 */
		void add(RepairResult other) {
			deletedRows += other.deletedRows;
			deletedVersions += other.deletedVersions;
			unpublishedVersions += other.unpublishedVersions;
			renumberedVersions += other.renumberedVersions;
		}
	}

	/**
	 * Check for duplicate page versions: output the number of affected rows and pages, do a dry-run of the repair for a sample of contents
	 * (which is rolled back) and output an estimation of the duration of the repair. Nothing is changed.
	 * @param out list for the output lines
	 * @return true
	 * @throws NodeException
	 */
	public static boolean check(List<String> out) throws NodeException {
		long start = System.currentTimeMillis();
		Detection detection = Trx.supply(() -> detect());
		long detectionDuration = System.currentTimeMillis() - start;

		out.add("Detection took %d ms".formatted(detectionDuration));
		report(detection, out);

		if (detection.isEmpty()) {
			out.add("Nothing to repair");
			return true;
		}

		Set<Integer> contentIds = detection.getContentIds();
		List<Integer> sample = contentIds.stream().limit(SAMPLE_SIZE).collect(Collectors.toList());
		long sampleStart = System.currentTimeMillis();

		for (Integer contentId : sample) {
			List<String> dryRunOut = new ArrayList<>();

			try (Trx trx = new Trx()) {
				// the transaction is never committed
				repairContent(contentId, detection.getGroups(contentId), detection.getDefectPageIds(contentId), true, dryRunOut);
			} catch (NodeException e) {
				logger.error("Error during dry-run of the repair of content %d".formatted(contentId), e);
				dryRunOut.add("Content %d: error: %s".formatted(contentId, e.getMessage()));
			}

			dryRunOut.forEach(line -> out.add("[dry-run] " + line));
		}

		long sampleDuration = System.currentTimeMillis() - sampleStart;
		long perContent = sample.isEmpty() ? 0 : sampleDuration / sample.size();

		if (!sample.isEmpty()) {
			out.add("Dry-run of the repair for %d of %d content(s) took %d ms (%d ms per content)".formatted(sample.size(), contentIds.size(),
					sampleDuration, perContent));
		}

		long estimation = detectionDuration + perContent * contentIds.size();

		out.add("Estimated duration of the repair: %d s".formatted(Math.max(1, Math.round(estimation / 1000.))));

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
		report(detection, out);

		if (detection.isEmpty()) {
			out.add("Nothing to repair");
			return true;
		}

		boolean success = true;
		RepairResult total = new RepairResult();
		int repairedContents = 0;
		Set<Integer> contentIds = detection.getContentIds();
		int counter = 0;

		for (Integer contentId : contentIds) {
			try (Trx trx = new Trx()) {
				RepairResult result = repairContent(contentId, detection.getGroups(contentId), detection.getDefectPageIds(contentId), false, out);
				trx.success();
				total.add(result);
				if (result.isChanged()) {
					repairedContents++;
				}
			} catch (NodeException e) {
				logger.error("Error while repairing content %d".formatted(contentId), e);
				out.add("Content %d: error: %s".formatted(contentId, e.getMessage()));
				success = false;
			}

			counter++;
			if (counter % 100 == 0) {
				logger.info("Repaired %d of %d contents".formatted(counter, contentIds.size()));
			}
		}

		if (detection.hasOrphans()) {
			try (Trx trx = new Trx()) {
				total.add(repairContent(null, detection.getGroups(null), detection.getDefectPageIds(null), false, out));
				trx.success();
			} catch (NodeException e) {
				logger.error("Error while repairing versions, which do not belong to an existing page", e);
				out.add("Versions without page: error: %s".formatted(e.getMessage()));
				success = false;
			}
		}

		if (total.isChanged()) {
			// versioned objects are cached with their version timestamp, and dirting only removes the current version from the cache
			for (Class<? extends NodeObject> clazz : VERSIONED_CLASSES) {
				NodeFactory.getInstance().clear(clazz);
			}
		}

		out.add("Repaired %d content(s): deleted %d versioned row(s) and %d page version(s), unpublished %d page version(s), renumbered %d page version(s)"
				.formatted(repairedContents, total.deletedRows, total.deletedVersions, total.unpublishedVersions, total.renumberedVersions));
		logger.info(out.get(out.size() - 1));

		return success;
	}

	/**
	 * Detect duplicate page versions (with a consistent read)
	 * @return detection result
	 * @throws NodeException
	 */
	static Detection detect() throws NodeException {
		Detection detection = new Detection();
		String datasourcePartIds = PageFactory.getDatasourcePartIds();

		for (VersionedTable table : VersionedTable.values()) {
			String dataColumns = new TableVersion().getVersionedColumns(table.name()).stream().filter(column -> !"id".equals(column))
					.map(column -> "IFNULL(`%s`, '<NULL>')".formatted(column)).collect(Collectors.joining(", "));
			String contentExpression = table.contentExpression.formatted(datasourcePartIds);
			String sql = "SELECT d.*, (SELECT c.id FROM content c WHERE c.id = " + contentExpression + ") content_id FROM ("
					+ "SELECT id, nodeversiontimestamp, MIN(`" + table.refColumn + "`) ref, COUNT(*) n, COUNT(DISTINCT nodeversionremoved) rv, "
					+ "COUNT(DISTINCT MD5(CONCAT_WS('|', " + dataColumns + "))) dv FROM `" + table.getVersionTable()
					+ "` GROUP BY id, nodeversiontimestamp HAVING COUNT(*) > 1) d ORDER BY d.id, d.nodeversiontimestamp";

			DBUtils.select(sql, rs -> {
				while (rs.next()) {
					int contentId = rs.getInt("content_id");
					detection.groups.add(new DuplicateGroup(table, rs.getInt("id"), rs.getInt("nodeversiontimestamp"), rs.getInt("n"), rs.getInt("rv") > 1,
							rs.getInt("dv") > 1, rs.wasNull() ? null : contentId));
				}
				return null;
			});
		}

		Map<Integer, int[]> duplicates = DBUtils.select(
				"SELECT o_id, COUNT(*) n FROM nodeversion WHERE o_type = ? GROUP BY o_id, timestamp HAVING COUNT(*) > 1", ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
				}, rs -> {
					Map<Integer, int[]> map = new HashMap<>();
					while (rs.next()) {
						int[] counts = map.computeIfAbsent(rs.getInt("o_id"), key -> new int[2]);
						counts[0]++;
						counts[1] += rs.getInt("n") - 1;
					}
					return map;
				});

		Set<Integer> multiplePublished = new TreeSet<>(DBUtils.select(
				"SELECT o_id id FROM nodeversion WHERE o_type = ? AND published = 1 GROUP BY o_id HAVING COUNT(*) > 1", ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
				}, DBUtils.IDLIST));

		// a page has version numbers, which are not increasing, if a version number is not greater than the number of the previous version
		String major = "CAST(SUBSTRING_INDEX(%s, '.', 1) AS UNSIGNED)";
		String minor = "CAST(SUBSTRING_INDEX(%s, '.', -1) AS UNSIGNED)";
		Set<Integer> notIncreasing = new TreeSet<>(DBUtils.select("SELECT DISTINCT x.o_id id FROM ("
				+ "SELECT o_id, nodeversion, LAG(nodeversion) OVER (PARTITION BY o_id ORDER BY timestamp, id) prev FROM nodeversion WHERE o_type = ?) x "
				+ "WHERE x.nodeversion IS NOT NULL AND x.prev IS NOT NULL AND (" + major.formatted("x.nodeversion") + " < " + major.formatted("x.prev")
				+ " OR (" + major.formatted("x.nodeversion") + " = " + major.formatted("x.prev") + " AND " + minor.formatted("x.nodeversion") + " <= "
				+ minor.formatted("x.prev") + "))", ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
				}, DBUtils.IDLIST));

		Set<Integer> pageIds = new TreeSet<>(duplicates.keySet());
		pageIds.addAll(multiplePublished);
		pageIds.addAll(notIncreasing);

		Map<Integer, Integer> contentIds = getContentIds(pageIds);

		for (Integer pageId : pageIds) {
			int[] counts = duplicates.getOrDefault(pageId, new int[2]);
			detection.pageDefects.put(pageId, new PageVersionDefects(pageId, contentIds.get(pageId), counts[0], counts[1],
					multiplePublished.contains(pageId), notIncreasing.contains(pageId)));
		}

		return detection;
	}

	/**
	 * Output the detection result
	 * @param detection detection result
	 * @param out list for the output lines
	 * @throws NodeException
	 */
	static void report(Detection detection, List<String> out) throws NodeException {
		for (VersionedTable table : VersionedTable.values()) {
			List<DuplicateGroup> groups = detection.groups.stream().filter(g -> g.table() == table).collect(Collectors.toList());
			out.add("Table %s: %d duplicate group(s), %d row(s) to delete, %d group(s) with differing nodeversionremoved, %d group(s) with differing data"
					.formatted(table.getVersionTable(), groups.size(), groups.stream().mapToInt(g -> g.rows() - 1).sum(),
							groups.stream().filter(DuplicateGroup::removedDiffers).count(), groups.stream().filter(DuplicateGroup::dataDiffers).count()));
		}

		Collection<PageVersionDefects> pageDefects = detection.pageDefects.values();
		out.add("Table nodeversion: %d duplicate group(s), %d row(s) to delete".formatted(
				pageDefects.stream().mapToInt(PageVersionDefects::duplicateGroups).sum(),
				pageDefects.stream().mapToInt(PageVersionDefects::duplicateRows).sum()));
		out.add("Pages with more than one published version: %d".formatted(pageDefects.stream().filter(PageVersionDefects::multiplePublished).count()));
		out.add("Pages with version numbers, which are not increasing: %d".formatted(
				pageDefects.stream().filter(PageVersionDefects::numbersNotIncreasing).count()));

		Set<Integer> contentIds = detection.getContentIds();
		Map<Integer, List<int[]>> pagesPerContent = Trx.supply(() -> getPages(contentIds));
		List<int[]> pages = pagesPerContent.values().stream().flatMap(List::stream).collect(Collectors.toList());

		out.add("Affected contents: %d, affected pages: %d (online: %d)".formatted(contentIds.size(), pages.size(),
				pages.stream().filter(page -> page[2] == 1).count()));

		if (detection.hasOrphans()) {
			out.add("Duplicate groups not belonging to an existing page: %d, nodeversion entries of not existing pages: %d page(s)".formatted(
					detection.getGroups(null).size(), detection.getDefectPageIds(null).size()));
		}

		for (int[] page : pages) {
			out.add("Page %d (content %d%s)".formatted(page[0], page[1], page[2] == 1 ? ", online" : ""));
		}
	}

	/**
	 * Repair the duplicate page versions of the given content. This method must be called in a separate transaction, which is committed
	 * afterwards (or rolled back for a dry-run).
	 * @param contentId content id, null to repair the versions, which do not belong to an existing content or page
	 * @param groups duplicate groups of the content
	 * @param defectPageIds ids of the pages of the content with defects in their nodeversion entries
	 * @param dryRun true for a dry-run (the pages are not dirted)
	 * @param out list for the output lines
	 * @return repair result
	 * @throws NodeException
	 */
	static RepairResult repairContent(Integer contentId, List<DuplicateGroup> groups, Set<Integer> defectPageIds, boolean dryRun, List<String> out)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RepairResult result = new RepairResult();
		String prefix = contentId != null ? "Content %d".formatted(contentId) : "Versions without page";

		if (contentId != null) {
			// take the lock, which is also taken when page versions are created
			PageFactory.lockForVersioning(contentId);
		}

		for (DuplicateGroup group : groups) {
			result.deletedRows += repairGroup(group, prefix, out);
		}

		for (Integer pageId : defectPageIds) {
			repairPageVersions(pageId, prefix, result, out);
		}

		if (contentId != null && result.isChanged() && !dryRun) {
			List<Integer> pageIds = DBUtils.select("SELECT id FROM page WHERE content_id = ? ORDER BY id", ps -> ps.setInt(1, contentId), DBUtils.IDLIST);
			List<Integer> republished = new ArrayList<>();

			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				for (Integer pageId : pageIds) {
					t.dirtObjectCache(Page.class, pageId);
					Page page = t.getObject(Page.class, pageId);

					if (page != null && page.isOnline()) {
						page.dirtPage(0);
						republished.add(pageId);
					}
				}
			}

			out.add("%s: dirted page(s) %s, republishing page(s) %s".formatted(prefix, pageIds, republished));
		}

		return result;
	}

	/**
	 * Repair the given duplicate group: keep one row and delete the others
	 * @param group duplicate group
	 * @param prefix prefix for the output lines
	 * @param out list for the output lines
	 * @return number of deleted rows
	 * @throws NodeException
	 */
	static int repairGroup(DuplicateGroup group, String prefix, List<String> out) throws NodeException {
		VersionedTable table = group.table();
		boolean checkEmpty = table == VersionedTable.value;
		String versionTable = table.getVersionTable();

		// read the current rows with a locking read
		List<VersionRow> rows = DBUtils.select("SELECT auto_id, nodeversionlatest" + (checkEmpty ? ", value_text, value_ref, info" : "") + " FROM `"
				+ versionTable + "` WHERE id = ? AND nodeversiontimestamp = ? ORDER BY auto_id FOR UPDATE", ps -> {
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
				}, Transaction.UPDATE_STATEMENT);

		if (rows.size() < 2) {
			return 0;
		}

		// empty rows are only kept, if all rows are empty. Of the remaining rows, the row with the highest auto_id is kept
		boolean allEmpty = rows.stream().allMatch(VersionRow::empty);
		VersionRow keep = rows.stream().filter(row -> allEmpty || !row.empty()).reduce((first, second) -> second).get();
		List<Integer> delete = rows.stream().filter(row -> row != keep).map(VersionRow::autoId).collect(Collectors.toList());

		DBUtils.update("DELETE FROM `" + versionTable + "` WHERE auto_id IN (" + StringUtils.repeat("?", delete.size(), ",") + ")", delete.toArray());

		if (keep.latest() == 0 && rows.stream().anyMatch(row -> row.latest() == 1)) {
			DBUtils.update("UPDATE `" + versionTable + "` SET nodeversionlatest = ? WHERE auto_id = ?", 1, keep.autoId());
		}

		out.add("%s: %s id %d @%d: kept auto_id %d, deleted auto_id(s) %s".formatted(prefix, versionTable, group.id(), group.timestamp(), keep.autoId(),
				delete));

		return delete.size();
	}

	/**
	 * Repair the nodeversion entries of the given page: keep one entry per timestamp, keep only the newest published version as published and
	 * renumber versions, which have a version number not greater than the number of the previous version
	 * @param pageId page id
	 * @param prefix prefix for the output lines
	 * @param result repair result, which will be updated
	 * @param out list for the output lines
	 * @throws NodeException
	 */
	static void repairPageVersions(int pageId, String prefix, RepairResult result, List<String> out) throws NodeException {
		List<PageVersionRow> versions = readPageVersions(pageId);

		if (versions.stream().anyMatch(v -> v.number() == null)) {
			// generate the missing version numbers
			PageFactory.loadPageVersions(pageId, true);
			versions = readPageVersions(pageId);
		}

		// keep one nodeversion entry per timestamp: the published one, otherwise the one with the highest id
		Map<Integer, List<PageVersionRow>> perTimestamp = versions.stream()
				.collect(Collectors.groupingBy(PageVersionRow::timestamp, LinkedHashMap::new, Collectors.toList()));
		List<PageVersionRow> remaining = new ArrayList<>();

		for (Map.Entry<Integer, List<PageVersionRow>> entry : perTimestamp.entrySet()) {
			List<PageVersionRow> sameTimestamp = entry.getValue();
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
			int timePub = DBUtils.update("UPDATE page SET time_pub_version = ? WHERE time_pub_version IN " + in, params.toArray());
			int timePubQueue = DBUtils.update("UPDATE page SET time_pub_version_queue = ? WHERE time_pub_version_queue IN " + in, params.toArray());
			DBUtils.update("DELETE FROM nodeversion WHERE id IN " + in, delete.toArray());
			result.deletedVersions += delete.size();

			out.add("%s: page %d: version @%d: kept id %d (%s%s), deleted id(s) %s (%s)%s".formatted(prefix, pageId, entry.getKey(), keep.id(),
					keep.number(), keep.published() ? ", published" : "", delete,
					sameTimestamp.stream().filter(v -> v != keep).map(PageVersionRow::number).collect(Collectors.joining(", ")),
					timePub + timePubQueue > 0 ? ", moved %d planned publishing reference(s) to id %d".formatted(timePub + timePubQueue, keep.id()) : ""));
		}

		// only the newest published version is the published version (see PublishableNodeObject.getPublishedVersion())
		List<PageVersionRow> published = remaining.stream().filter(PageVersionRow::published).collect(Collectors.toList());
		if (published.size() > 1) {
			List<Integer> unpublish = published.subList(0, published.size() - 1).stream().map(PageVersionRow::id).collect(Collectors.toList());
			DBUtils.update("UPDATE nodeversion SET published = 0 WHERE id IN (" + StringUtils.repeat("?", unpublish.size(), ",") + ")",
					unpublish.toArray());
			result.unpublishedVersions += unpublish.size();
			out.add("%s: page %d: marked version id(s) %s as not published, published version is id %d (%s)".formatted(prefix, pageId, unpublish,
					published.get(published.size() - 1).id(), published.get(published.size() - 1).number()));
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
				result.renumberedVersions++;
				out.add("%s: page %d: version id %d @%d: renumbered %s -> %s".formatted(prefix, pageId, version.id(), version.timestamp(),
						version.number(), number));
			}

			previous = current;
			previousNumber = number;
		}
	}

	/**
	 * Read the nodeversion entries of the page (with a locking read), ordered by timestamp and id
	 * @param pageId page id
	 * @return list of nodeversion entries
	 * @throws NodeException
	 */
	static List<PageVersionRow> readPageVersions(int pageId) throws NodeException {
		return DBUtils.select("SELECT id, timestamp, published, nodeversion FROM nodeversion WHERE o_type = ? AND o_id = ? ORDER BY timestamp, id FOR UPDATE",
				ps -> {
					ps.setInt(1, Page.TYPE_PAGE);
					ps.setInt(2, pageId);
				}, rs -> {
					List<PageVersionRow> list = new ArrayList<>();
					while (rs.next()) {
						list.add(new PageVersionRow(rs.getInt("id"), rs.getInt("timestamp"), rs.getBoolean("published"), rs.getString("nodeversion")));
					}
					return list;
				}, Transaction.UPDATE_STATEMENT);
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
	 * Get the pages of the given contents
	 * @param contentIds content ids
	 * @return map of content ids to the lists of pages (as arrays of page id, content id and online flag), ordered by page id
	 * @throws NodeException
	 */
	static Map<Integer, List<int[]>> getPages(Collection<Integer> contentIds) throws NodeException {
		Map<Integer, List<int[]>> pages = new TreeMap<>();

		if (!contentIds.isEmpty()) {
			DBUtils.executeMassStatement("SELECT id, content_id, online FROM page WHERE content_id IN", " ORDER BY id", contentIds, 1, new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						pages.computeIfAbsent(rs.getInt("content_id"), key -> new ArrayList<>())
								.add(new int[] { rs.getInt("id"), rs.getInt("content_id"), rs.getInt("online") });
					}
				}
			});
		}

		return pages;
	}
}
