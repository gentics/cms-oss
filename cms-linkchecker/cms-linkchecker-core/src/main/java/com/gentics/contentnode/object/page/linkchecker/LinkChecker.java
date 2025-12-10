package com.gentics.contentnode.object.page.linkchecker;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;

import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.validator.routines.UrlValidator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.MultiPageUpdater;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.UserGroup.ReductionType;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkCheckHistoryEntry;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;
import com.gentics.contentnode.rest.model.linkchecker.ReplaceScope;
import com.gentics.contentnode.rest.resource.impl.internal.JobProgress;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

/**
 * LinkChecker implementation
 */
public final class LinkChecker {
	public final static NodeLogger logger = NodeLogger.getNodeLogger(LinkChecker.class);

	/**
	 * Key of the entry in the nodesetup table (containing the timestamp of the last run)
	 */
	public final static String NODESETUP_KEY = "linkchecker";

	public final static int DEFAULT_HISTORY_LENGTH = 5;

	public final static boolean DEFAULT_NOTIFY = true;

	public final static int DEFAULT_DEBOUNCE = 3;

	public final static int DEFAULT_READ_BUFFER_SIZE = 100;

	public final static int DEFAULT_CHECK_BUFFER_SIZE = 10;

	public final static int DEFAULT_UPDATE_BUFFER_SIZE = 100;

	public final static int DEFAULT_TIMEOUT = 60;

	public final static int DEFAULT_RETRY_AFTER = 3600;

	private static int historyLength = DEFAULT_HISTORY_LENGTH;

	private static boolean notify = DEFAULT_NOTIFY;

	private static int debounce = DEFAULT_DEBOUNCE;

	private static int readBufferSize = DEFAULT_READ_BUFFER_SIZE;

	private static int checkBufferSize = DEFAULT_CHECK_BUFFER_SIZE;

	private static int updateBufferSize = DEFAULT_UPDATE_BUFFER_SIZE;

	private static int retryAfter = DEFAULT_RETRY_AFTER;

	private static ThreadFactory threadFactory = new PrefixedThreadFactory("linkchecker");

	private static ExecutorService httpExecutor;

	private static OkHttpClient client;

	private static Map<String, CompletableFuture<Pair<ExternalLinkStatus, String>>> cache = Collections.synchronizedMap(new HashMap<>());

	private static Set<Integer> newInvalidExternalLinkIds = Collections.synchronizedSet(new HashSet<>());

	/**
	 * Map of hostnames, which returned "Too many Requests" to the time, after which the link checker can retry
	 */
	private static Map<String, Integer> retryAfterMap = Collections.synchronizedMap(new HashMap<>());

	private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[] {};
		}
	} };

	private static final SSLContext trustAllSslContext;

	static {
		try {
			trustAllSslContext = SSLContext.getInstance("SSL");
			trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}

	private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();

	/**
	 * Lambda that reads the resultset and returns a collection of ExternalLink instances
	 */
	private final static HandleSelectResultSet<Collection<ExternalLink>> EXTERNAL_LINKS_READER = rs -> {
		Collection<ExternalLink> data = new ArrayList<>();
		while (rs.next()) {
			data.add(
				new ExternalLink()
					.setId(rs.getInt("id"))
					.setContentId(rs.getInt("content_id"))
					.setContenttagId(rs.getInt("contenttag_id"))
					.setValueId(rs.getInt("value_id"))
					.setUrl(rs.getString("url"))
					.setText(rs.getString("text"))
					.setLastCheckTimestamp(rs.getInt("last_check_timestamp"))
					.setLastStatus(ExternalLinkStatus.getForCode(rs.getInt("last_status")))
					.setLastReason(rs.getString("last_reason"))
			);
		}
		return data;
	};

	private LinkChecker() {
	}

	/**
	 * Initialize the link checker's http client (if feature is enabled)
	 */
	public static synchronized void init() {
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		historyLength = ObjectTransformer.getInt(prefs.getProperty("link_checker.history_length"), DEFAULT_HISTORY_LENGTH);
		notify = ObjectTransformer.getBoolean(prefs.getProperty("link_checker.notify"), DEFAULT_NOTIFY);
		debounce = ObjectTransformer.getInt(prefs.getProperty("link_checker.debounce"), DEFAULT_DEBOUNCE);
		readBufferSize = ObjectTransformer.getInt(prefs.getProperty("link_checker.read_buffer_size"), DEFAULT_READ_BUFFER_SIZE);
		checkBufferSize = ObjectTransformer.getInt(prefs.getProperty("link_checker.check_buffer_size"), DEFAULT_CHECK_BUFFER_SIZE);
		updateBufferSize = ObjectTransformer.getInt(prefs.getProperty("link_checker.update_buffer_size"), DEFAULT_UPDATE_BUFFER_SIZE);
		retryAfter = ObjectTransformer.getInt(prefs.getProperty("link_checker.retry_after"), DEFAULT_RETRY_AFTER);

		if (prefs.isFeature(Feature.LINK_CHECKER)) {
			int callTimeout = ObjectTransformer.getInt(prefs.getProperty("link_checker.call_timeout"), DEFAULT_TIMEOUT);
			int connectTimeout = ObjectTransformer.getInt(prefs.getProperty("link_checker.connect_timeout"), DEFAULT_TIMEOUT);
			int writeTimeout = ObjectTransformer.getInt(prefs.getProperty("link_checker.write_timeout"), DEFAULT_TIMEOUT);
			int readTimeout = ObjectTransformer.getInt(prefs.getProperty("link_checker.read_timeout"), DEFAULT_TIMEOUT);

			client = new OkHttpClient.Builder()
					.callTimeout(Duration.ofSeconds(callTimeout))
					.connectTimeout(Duration.ofSeconds(connectTimeout))
					.writeTimeout(Duration.ofSeconds(writeTimeout))
					.readTimeout(Duration.ofSeconds(readTimeout))
					.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0])
					.hostnameVerifier(new HostnameVerifier() {
						@Override
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					})
					.build();
		}
	}

	/**
	 * Start link checker threads
	 * @throws NodeException
	 */
	public synchronized final static void start() throws NodeException {
		stop();

		httpExecutor = Executors.newFixedThreadPool(checkBufferSize, threadFactory);
	}

	/**
	 * Stop link checker threads
	 * @throws NodeException
	 */
	public synchronized final static void stop() throws NodeException {
		if (httpExecutor != null) {
			httpExecutor.shutdownNow();
			httpExecutor = null;
		}
	}

	/**
	 * Terminate link checker threads
	 * @throws NodeException
	 */
	public synchronized final static void terminate() throws NodeException {
		client = null;
		stop();
	}

	/**
	 * Load external Links currently stored for the given content
	 * @param contentId content ID
	 * @return collection of {@link ExternalLink} instances
	 * @throws NodeException
	 */
	public static Collection<ExternalLink> load(int contentId) throws NodeException {
		Collection<ExternalLink> links = DBUtils.select("SELECT * FROM external_link WHERE content_id = ?", stmt -> stmt.setInt(1, contentId), EXTERNAL_LINKS_READER);
		links.stream().forEach(externalLinkExtender(links));
		return links;
	}

	/**
	 * Get the link with given ID or null, if not found
	 * @param contentId content ID
	 * @param linkId link ID
	 * @return link or null
	 * @throws NodeException
	 */
	public static ExternalLink load(int contentId, int linkId) throws NodeException {
		Collection<ExternalLink> links = DBUtils.select("SELECT * FROM external_link WHERE id = ? AND content_id = ?",
				stmt -> {
					stmt.setInt(1, linkId);
					stmt.setInt(2, contentId);
				}, EXTERNAL_LINKS_READER);
		Iterator<ExternalLink> iterator = links.iterator();
		if (iterator.hasNext()) {
			ExternalLink link = iterator.next();
			externalLinkExtender(links).accept(link);
			return link;
		} else {
			return null;
		}
	}

	/**
	 * Find occurrences of the URL
	 * @param url URL to check
	 * @param linkPageId ID of the page, the URL is taken from
	 * @param linkNodeId ID of the node/channel of the page
	 * @return triple containing occurrences in page/node/global
	 * @throws NodeException
	 */
	public static Triple<Integer, Integer, Integer> findOccurrences(String url, int linkPageId, int linkNodeId) throws NodeException {
		AtomicInteger countInPage = new AtomicInteger();
		AtomicInteger countInNode = new AtomicInteger();
		AtomicInteger countGlobal = new AtomicInteger();

		processOccurrences(url, rs -> {
			int pageId = rs.getInt("page_id");
			int nodeId = rs.getInt("node_id");
			if (rs.getInt("channel_id") != 0) {
				nodeId = rs.getInt("channel_id");
			}

			if (pageId == linkPageId) {
				countInPage.incrementAndGet();
			}
			if (nodeId == linkNodeId) {
				countInNode.incrementAndGet();
			}
			countGlobal.incrementAndGet();

			return null;
		});

		return Triple.of(countInPage.get(), countInNode.get(), countGlobal.get());
	}

	/**
	 * Update occurrences of the given URL with a new URL
	 * @param url URL to replace
	 * @param linkId ID of the link containing the URL
	 * @param newUrl new URL
	 * @param linkPageId ID of the page containing the link
	 * @param linkNodeId ID of the node containing the link
	 * @param scope replace scope
	 * @return result object
	 * @throws NodeException
	 */
	public static UpdatePagesResult updateOccurrences(String url, int linkId, String newUrl, int linkPageId, int linkNodeId, ReplaceScope scope) throws NodeException {
		PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();

		AtomicInteger noPermissionCount = new AtomicInteger();
		Map<Integer, Set<Pair<Integer, Integer>>> linkMap = new HashMap<>();

		// find the occurrences and collect in the linkMap. This will filter out pages
		// without edit permission (and count in noPermissionCount)
		processOccurrences(url, rs -> {
			int id = rs.getInt("id");
			int contentGroupId = rs.getInt("contentgroup_id");
			int nodeId = rs.getInt("node_id");
			if (rs.getInt("channel_id") != 0) {
				nodeId = rs.getInt("channel_id");
			}
			int folderId = rs.getInt("folder_id");
			int pageId = rs.getInt("page_id");

			// filter by scope
			switch (scope) {
			case link:
				if (id != linkId) {
					return null;
				}
				break;
			case page:
				if (pageId != linkPageId) {
					return null;
				}
				break;
			case node:
				if (nodeId != linkNodeId) {
					return null;
				}
				break;
			default:
				break;
			}

			// check permission to edit pages (with the language) in the folder
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				if (!permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_UPDATE,
						Page.TYPE_PAGE, contentGroupId, PermHandler.ROLE_VIEW)) {
					noPermissionCount.incrementAndGet();
					return null;
				}
			}

			int contentTagId = rs.getInt("contenttag_id");
			int valueId = rs.getInt("value_id");
			linkMap.computeIfAbsent(pageId, key -> new HashSet<>()).add(Pair.of(contentTagId, valueId));

			return null;
		});

		// now update the pages by replacing the URLs
		UpdatePagesResult result = null;
		if (!linkMap.isEmpty()) {
			result = new MultiPageUpdater(page -> {
				Set<Pair<Integer, Integer>> tagSet = linkMap.get(page.getId());
				if (!ObjectTransformer.isEmpty(tagSet)) {
					for (Pair<Integer, Integer> pair : tagSet) {
						ContentTag tag = page.getContentTag(pair.getLeft());
						if (tag != null) {
							Value value = tag.getValues().getById(pair.getRight());
							if (value != null) {
								PageURLPartType pt = value.getPartType();
								pt.setExternalTarget(newUrl);
							}
						}
					}
				}

			}).setIds(linkMap.keySet()).setInsufficientPermissionIsError(false).setLockedIsError(false).execute();
		} else {
			result = new UpdatePagesResult(0, 0, 0, 0, 0);
		}

		// add the count for pages without permission, because they were filtered out before
		result.addNoPermission(noPermissionCount.get());
		return result;
	}

	/**
	 * Find all occurrences of the given URL and process with the given handler. The handler will be called for every row in the resultset (and thus should not call rs.next()).
	 * Deleted pages and pages without view permission will be filtered out.
	 * @param url given URL
	 * @param handler resultset handler
	 * @throws NodeException
	 */
	protected static void processOccurrences(String url, HandleSelectResultSet<Void> handler) throws NodeException {
		String sql = "SELECT p.id page_id, p.channel_id channel_id, f.id folder_id, f.node_id node_id, p.contentgroup_id contentgroup_id, el.* "
				+ "FROM external_link el LEFT JOIN page p ON el.content_id = p.content_id LEFT JOIN folder f ON p.folder_id = f.id "
				+ "WHERE p.deleted = 0 AND f.deleted = 0 AND LOWER(url) = ?";

		DBUtils.select(sql, stmt -> stmt.setString(1, url.toLowerCase()), rs -> {
			PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();

			while (rs.next()) {
				int contentGroupId = rs.getInt("contentgroup_id");
				int nodeId = rs.getInt("node_id");
				if (rs.getInt("channel_id") != 0) {
					nodeId = rs.getInt("channel_id");
				}
				int folderId = rs.getInt("folder_id");

				// check permission to view pages (with the language) in the folder
				try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
					if (!permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folderId, PermHandler.PERM_PAGE_VIEW,
							Page.TYPE_PAGE, contentGroupId, PermHandler.ROLE_VIEW)) {
						continue;
					}
				}

				handler.handle(rs);
			}

			return null;
		});
	}

	/**
	 * Load the status history entries for a collection of external links
	 * @param externalLinks collection of external links
	 * @param idExtractor function that extracts the IDs of the collection of external links
	 * @return map of external link id to lists of history entries (ordered by timestamp)
	 * @throws NodeException
	 */
	public static <T> Map<Integer, List<ExternalLinkCheckHistoryEntry>> loadStatusHistory(Collection<T> externalLinks, Function<T, Integer> idExtractor)
			throws NodeException {
		if (ObjectTransformer.isEmpty(externalLinks)) {
			return Collections.emptyMap();
		}
		String sql = String.format(
				"SELECT external_link_id, timestamp, status, reason FROM external_link_status_history WHERE external_link_id IN (%s) ORDER BY external_link_id ASC, timestamp ASC",
				StringUtils.repeat("?", ",", externalLinks.size()));
		return DBUtils.select(sql, stmt -> {
			int index = 1;
			for (T object : externalLinks) {
				stmt.setInt(index++, idExtractor.apply(object));
			}
		}, rs -> {
			Map<Integer, List<ExternalLinkCheckHistoryEntry>> map = new HashMap<>();
			while (rs.next()) {
				int id = rs.getInt("external_link_id");
				ExternalLinkCheckHistoryEntry entry = new ExternalLinkCheckHistoryEntry().setTimestamp(rs.getInt("timestamp"))
						.setStatus(ExternalLinkStatus.getForCode(rs.getInt("status"))).setReason(rs.getString("reason"));
				map.computeIfAbsent(id, key -> new ArrayList<>()).add(entry);
			}
			return map;
		});
	}

	/**
	 * Attach the status histories to the given collection of external links
	 * @param links collection of external links
	 * @throws NodeException
	 */
	public static void attachStatusHistory(Collection<ExternalLink> links) throws NodeException {
		Map<Integer, List<ExternalLinkCheckHistoryEntry>> historyEntries = loadStatusHistory(links, ExternalLink::getId);
		links.stream().forEach(link -> link.setHistory(historyEntries.get(link.getId())));
	}

	/**
	 * Get the diff between the given collection of links and the links found in the content
	 * @param links collection of Links
	 * @param content content to search for links
	 * @return diff
	 * @throws NodeException
	 */
	public static ExternalLinkDiff getDiff(Collection<ExternalLink> links, Content content) throws NodeException {
		Map<Integer, ExternalLink> lookupMap = links.stream().collect(Collectors.toMap(ExternalLink::getValueId, o -> o));
		Set<Integer> foundValueIds = new HashSet<>();

		ExternalLinkDiff result = new ExternalLinkDiff();

		// update/add existing external URLs
		for (ContentTag contentTag : content.getContentTags().values()) {
			ValueList values = contentTag.getValues();
			for (Part part : contentTag.getConstruct().getParts()) {
				if (part.getPartTypeId() != PageURLPartType.EXTERNAL_URL_TYPE || !part.isEditable()) {
					continue;
				}
				Value value = values.getByPartId(part.getId());
				if (value == null) {
					continue;
				}
				PageURLPartType pageUrlPartType = value.getPartType();
				if (!isChecked(pageUrlPartType)) {
					continue;
				}

				foundValueIds.add(value.getId());
				ExternalLink link = lookupMap.get(value.getId());
				if (link == null) {
					link = new ExternalLink();
					result.getNewLinks().add(link);
					extractLinkData(content, contentTag, pageUrlPartType, link);
				} else {
					link = link.clone();
					Set<String> modified = extractLinkData(content, contentTag, pageUrlPartType, link);
					if (!modified.isEmpty()) {
						if (modified.contains("url")) {
							result.getModifiedUrlLinks().add(link);
						} else {
							result.getModifiedLinks().add(link);
						}
					}
				}
			}
		}

		// remove no longer found external Links
		result.getRemovedLinks().addAll(links.stream().filter(link -> !foundValueIds.contains(link.getValueId())).collect(Collectors.toSet()));

		return result;
	}

	/**
	 * Persist the diff
	 * @param contentId content ID
	 * @param diff to persist
	 * @throws NodeException
	 */
	public static void persist(int contentId, ExternalLinkDiff diff) throws NodeException {
		DBUtils.update("INSERT IGNORE INTO external_link_content (content_id) VALUES (?)", contentId);

		if (!diff.getNewLinks().isEmpty()) {
			DBUtils.executeBatch("INSERT INTO external_link (content_id, contenttag_id, value_id, url, text) VALUES (?, ?, ?, ?, ?)", Transaction.INSERT_STATEMENT, diff.getNewLinks(),
					link -> new Object[] { link.getContentId(), link.getContenttagId(), link.getValueId(), link.getUrl(), link.getText() }, null);
		}
		if (!diff.getModifiedLinks().isEmpty()) {
			DBUtils.executeBatch("UPDATE external_link SET text = ? WHERE id = ?", Transaction.UPDATE_STATEMENT, diff.getModifiedLinks(),
					link -> new Object[] { link.getText(), link.getId() }, null);
		}
		if (!diff.getModifiedUrlLinks().isEmpty()) {
			// if the URL was modified, we remove the last check status and clear the history
			DBUtils.executeBatch("UPDATE external_link SET url = ?, text = ?, last_status = ?, last_check_timestamp = ? WHERE id = ?",
					Transaction.UPDATE_STATEMENT, diff.getModifiedUrlLinks(), link -> new Object[] { link.getUrl(), link.getText(), 0, 0, link.getId() }, null);
			DBUtils.executeBatch("DELETE FROM external_link_status_history WHERE external_link_id = ?", Transaction.DELETE_STATEMENT, diff.getModifiedUrlLinks(),
					link -> new Object[] { link.getId() }, null);
		}
		if (!diff.getRemovedLinks().isEmpty()) {
			DBUtils.executeBatch("DELETE FROM external_link WHERE id = ?", Transaction.DELETE_STATEMENT, diff.getRemovedLinks(), link -> new Object[] { link.getId() },
					null);
		}
	}

	/**
	 * Update the status indicators in the collection of links (ignore all entries that have neither {@link ExternalLinkStatus#valid} nor {@link ExternalLinkStatus#invalid} as status).
	 * Also add a new history entry and truncate number of history entries to at most {@link #DEFAULT_HISTORY_LENGTH} entries
	 * @param links collection of links
	 * @throws NodeException
	 */
	public static void updateStatus(Collection<ExternalLink> links) throws NodeException {
		List<Integer> toRemove = links.stream().filter(link -> link.getLastStatus() == null).map(ExternalLink::getId).collect(Collectors.toList());
		if (!ObjectTransformer.isEmpty(toRemove)) {
			logger.info(String.format("Removing %d links, which should not be checked any more", toRemove.size()));

			DBUtils.update(String.format("DELETE FROM external_link WHERE id IN (%s)", StringUtils.repeat("?", ",", toRemove.size())), toRemove.toArray());
		}

		if (links != null) {
			links = links.stream().filter(link -> link.getLastStatus() == ExternalLinkStatus.valid || link.getLastStatus() == ExternalLinkStatus.invalid).collect(Collectors.toList());
		}
		if (!ObjectTransformer.isEmpty(links)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			int timestamp = t.getUnixTimestamp();

			DBUtils.executeBatch("UPDATE external_link SET last_status = ?, last_reason = ?, last_check_timestamp = ? WHERE id = ?", Transaction.UPDATE_STATEMENT, links,
					link -> new Object[] { link.getLastStatus().getCode(), link.getLastReason(), timestamp, link.getId() }, null);

			DBUtils.executeBatch("INSERT IGNORE INTO external_link_status_history (status, reason, timestamp, external_link_id) VALUES (?, ?, ?, ?)", Transaction.INSERT_STATEMENT, links,
					link -> new Object[] { link.getLastStatus().getCode(), link.getLastReason(), timestamp, link.getId() }, null);

			List<Integer> idList = links.stream().map(ExternalLink::getId).collect(Collectors.toList());

			Map<Integer, Integer> purgableHistoryEntries = DBUtils.select("SELECT external_link_id, timestamp FROM external_link_status_history h1 WHERE h1.external_link_id in ("
					+ StringUtils.repeat("?", ",", idList.size())
					+ ") AND (? - 1) = (SELECT COUNT(DISTINCT(timestamp)) FROM external_link_status_history h2 WHERE h1.external_link_id = h2.external_link_id AND h2.timestamp > h1.timestamp )",
					stmt -> {
						int index = 1;
						for (Integer id : idList) {
							stmt.setInt(index++, id);
						}
						stmt.setInt(index++, historyLength);
					}, rs -> {
						Map<Integer, Integer> map = new HashMap<>();
						while (rs.next()) {
							map.put(rs.getInt("external_link_id"), rs.getInt("timestamp"));
						}
						return map;
					});

			if (!ObjectTransformer.isEmpty(purgableHistoryEntries)) {
				DBUtils.executeBatch("DELETE FROM external_link_status_history WHERE external_link_id = ? AND timestamp < ?", Transaction.DELETE_STATEMENT,
						purgableHistoryEntries.entrySet(), entry -> {
							return new Object[] { entry.getKey(), entry.getValue() };
						}, null);
			}
		}
	}

	/**
	 * Return a completable, that will check all links when subscribed on
	 * @param progress progress
	 * @return completable
	 * @throws NodeException
	 */
	public static Completable checkLinks(JobProgress progress) throws NodeException {
		Flowable<ExternalLink> obs = Flowable.generate(() -> new LinkedList<ExternalLink>(), (state, emitter) -> {
			// if the current state is empty, we get the next batch
			if (state.isEmpty()) {
				state.addAll(supply(() -> {
					// update the next batch to be processed
					DBUtils.update("UPDATE external_link SET processed = ? WHERE processed = ? ORDER BY last_check_timestamp ASC LIMIT ?", 1, 0, readBufferSize);

					Collection<ExternalLink> batch = DBUtils.select("SELECT * FROM external_link WHERE processed = ?", stmt -> {
						stmt.setInt(1, 1);
					}, EXTERNAL_LINKS_READER);

					// update again (so we do not read the batch again)
					DBUtils.update("UPDATE external_link SET processed = ? WHERE processed = ?", 2, 1);

					logger.info(String.format("Prepared batch of %d external links to check", batch.size()));

					return batch;
				}));
			}

			if (state.isEmpty()) {
				emitter.onComplete();
			} else {
				emitter.onNext(state.poll());
			}
		});
		return obs.doOnSubscribe(disp -> {
			// mark all links to be processed
			logger.info("Initiate processing of external links");
			cache.clear();
			newInvalidExternalLinkIds.clear();
			operate(() -> DBUtils.update("UPDATE external_link SET processed = ?", 0));
		}).flatMap(item -> {
			return Flowable.just(item).observeOn(Schedulers.from(httpExecutor)).map(link -> {
				logger.debug(String.format("Checking %s", link));

				String testedUrl = link.getUrl();
				// should we check the URL?
				if (LinkChecker.isChecked(testedUrl)) {
				Set<String> tests = new HashSet<>();
				if (StringUtils.startsWith(testedUrl, "//")) {
					// if url starts with //, it is protocol relative, so we need to check, whether any of the pages nodes has http or https
					tests.addAll(addProtocol(link));
				} else if (!StringUtils.startsWithAny(testedUrl, "http:", "https:")) {
					// if url does not start with // or http: or https: we will try to make it relative to the page's external URL
					tests.addAll(makeAbsolute(link));
				} else {
					tests.add(testedUrl);
				}

					Pair<ExternalLinkStatus, String> result = Pair.of(ExternalLinkStatus.valid, null);
				for (String test : tests) {
						Pair<ExternalLinkStatus, String> testResult = checkWithCache(test);
						if (testResult.getLeft() != ExternalLinkStatus.valid) {
						result = testResult;
						break;
					}
				}
					link.setLastStatus(result.getLeft());
				link.setLastReason(result.getRight());
				} else {
					// this happens, if the URL was considered to be checked at the time of parsing,
					// but should not be checked any more (e.g. due to a CMS update meanwhile)
					// in this case, we set the last status and last reason both to null, which will cause the link to be removed later on
					link.setLastStatus(null);
					link.setLastReason(null);
				}

				if (progress != null) {
					progress.incDone();
				}
				return link;
			});
		}, false, readBufferSize, readBufferSize).buffer(updateBufferSize).doOnNext(list -> {
			logger.info(String.format("Storing check result for %d links", list.size()));
			operate(() -> updateStatus(list));

			if (notify) {
				newInvalidExternalLinkIds.addAll(getEntriesTurningInvalid(supply(() -> loadStatusHistory(list, ExternalLink::getId))));
			}
		}).ignoreElements().doOnComplete(() -> {
			if (!newInvalidExternalLinkIds.isEmpty()) {
				notifyEditors();
			}
		}).doAfterTerminate(() -> {
			logger.info("Clearing cache");
			cache.clear();
			newInvalidExternalLinkIds.clear();
		});
	}

	/**
	 * Remove all external links entries for contents of pages, that do not have the feature enabled
	 * @throws NodeException
	 */
	public static void cleanEntries() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Integer> nodeIds = DBUtils.select("SELECT node_id id FROM node_feature WHERE feature = ?",
				stmt -> stmt.setString(1, Feature.LINK_CHECKER.getName()), DBUtils.IDS);

		if (nodeIds.isEmpty()) {
			// remove everything
			DBUtils.update("DELETE FROM external_link_content");
			DBUtils.update("DELETE FROM external_link");
			t.commit(false);
		} else {
			// get content IDs for pages in nodes that have the feature disabled
			Set<Integer> contentIds = DBUtils.select(
					"SELECT DISTINCT(external_link_content.content_id) id FROM external_link_content LEFT JOIN page ON external_link_content.content_id = page.content_id LEFT JOIN folder ON page.folder_id = folder.id WHERE folder.node_id NOT IN ("
							+ StringUtils.repeat("?", ",", nodeIds.size()) + ")",
					stmt -> {
						int index = 1;
						for (int nodeId : nodeIds) {
							stmt.setInt(index++, nodeId);
						}
					}, DBUtils.IDS);

			// check all contents, because they might also belong to pages in nodes with the feature
			List<Content> contents = t.getObjects(Content.class, contentIds);
			for (Content content : contents) {
				for (Page page : content.getPages()) {
					if (nodeIds.contains(page.getOwningNode().getId())) {
						// found a page in a node with the feature, so remove the content ID from the set of content IDs to be deleted
						contentIds.remove(content.getId());
						break;
					}
				}
			}

			if (!contentIds.isEmpty()) {
				DBUtils.executeMassStatement("DELETE FROM external_link_content WHERE content_id IN ", null, new ArrayList<>(contentIds), 1, null,
						Transaction.DELETE_STATEMENT);
				DBUtils.executeMassStatement("DELETE FROM external_link WHERE content_id IN ", null, new ArrayList<>(contentIds), 1, null,
						Transaction.DELETE_STATEMENT);
				t.commit(false);
			}
		}
	}

	/**
	 * Find all contents of pages in nodes with the feature activated and parse them for external links
	 * @throws NodeException
	 */
	public static void parseMissingPages() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Integer> nodeIds = DBUtils.select("SELECT node_id id FROM node_feature WHERE feature = ?", stmt -> stmt.setString(1, Feature.LINK_CHECKER.getName()), DBUtils.IDS);

		logger.info(String.format("Start parsing pages for %d nodes", nodeIds.size()));
		int nodesProgress = 0;
		int totalNodes = nodeIds.size();
		for (int nodeId : nodeIds) {
			Set<Integer> contentIds = DBUtils.select(
					"SELECT DISTINCT page.content_id id FROM page LEFT JOIN folder ON page.folder_id = folder.id LEFT JOIN external_link_content ON page.content_id = external_link_content.content_id WHERE page.deleted = 0 AND folder.node_id = ? AND external_link_content.content_id IS NULL",
					stmt -> stmt.setInt(1, nodeId), DBUtils.IDS);

			List<Content> contents = t.getObjects(Content.class, contentIds);
			int contentsProgress = 0;
			int totalContents = contents.size();
			for (Content content : contents) {
				Collection<ExternalLink> existing = LinkChecker.load(content.getId());
				ExternalLinkDiff diff = LinkChecker.getDiff(existing, content);
				LinkChecker.persist(content.getId(), diff);
				t.commit(false);
				contentsProgress++;
				logger.debug(String.format("Finished %d/%d contents", contentsProgress, totalContents));
			}
			nodesProgress++;
			logger.debug(String.format("Finished %d/%d nodes", nodesProgress, totalNodes));
		}
		logger.info("Done parsing pages");
	}

	/**
	 * Get total number of links
	 * @return number of links
	 * @throws NodeException
	 */
	public static int getTotalLinks() throws NodeException {
		return DBUtils.select("SELECT count(*) c FROM external_link", rs -> {
			if (rs.next()) {
				return rs.getInt("c");
			} else {
				return 0;
			}
		});
	}

	/**
	 * Get map of node -> map of link status -> number of links, sorted by node name
	 * @return link stats
	 * @throws NodeException
	 */
	public static Map<Node, Map<ExternalLinkStatus, Integer>> getLinkStats() throws NodeException {
		Map<Node, Map<ExternalLinkStatus, Integer>> nodeMap = new TreeMap<>(
				(node1, node2) -> {
					try {
						return com.gentics.lib.etc.StringUtils.mysqlLikeCompare(node1.getFolder().getName(), node2.getFolder().getName());
					} catch (NodeException e) {
						return 0;
					}
				});
		Transaction t = TransactionManager.getCurrentTransaction();
		for (Node node : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
			if (!NodeConfigRuntimeConfiguration.isFeature(Feature.LINK_CHECKER, node)) {
				continue;
			}
			Node n = node.isChannel() ? node.getMaster() : node;
			Node c = node.isChannel() ? node : null;

			Map<Integer, Integer> statusMap = DBUtils.select(
				"SELECT last_status, COUNT(*) c FROM external_link, page, folder WHERE external_link.content_id = page.content_id AND page.deleted = 0 AND page.folder_id = folder.id AND folder.node_id = ? AND page.channel_id = ? GROUP BY last_status",
				stmt -> {
					stmt.setInt(1, n.getId());
					stmt.setInt(2, c != null ? c.getId() : 0);
				}, rs -> {
					Map<Integer, Integer> map = new HashMap<>();
					while (rs.next()) {
						map.put(rs.getInt("last_status"), rs.getInt("c"));
					}
					return map;
				});
			nodeMap.computeIfAbsent(node, key -> {
				Map<ExternalLinkStatus, Integer> map = new HashMap<>();
				for (ExternalLinkStatus status : ExternalLinkStatus.values()) {
					map.put(status, statusMap.getOrDefault(status.getCode(), 0));
				}
				return map;
			});
		}
		return nodeMap;
	}

	/**
	 * Return either "http" or "https" depending on whether the page's node supports https
	 * @param page page
	 * @return http/https
	 * @throws NodeException
	 */
	public static String getDefaultProtocol(Page page) throws NodeException {
		Node node = page.getOwningNode();
		return node.isHttps() ? "https" : "http";
	}

	/**
	 * Get the base URL to be used for absolute urls without protocol and hostname.
	 * The base URL will look like "[protocol]://[hostname]" and will never and with "/"
	 * @param page page
	 * @return base URL
	 * @throws NodeException
	 */
	public static String getAbsoluteBase(Page page) throws NodeException {
		Node node = page.getOwningNode();
		return String.format("%s://%s", getDefaultProtocol(page), node.getHostname());
	}

	/**
	 * Get the base URL to be used for relative urls
	 * The base URL will look like "[protocol]://[hostname]/[path]/" ([path] is optional) and will always end with "/"
	 * @param page page
	 * @return base URL
	 * @throws NodeException
	 */
	public static String getRelativeBase(Page page) throws NodeException {
		Node node = page.getOwningNode();
		Folder folder = page.getFolder();
		List<String> segments = new ArrayList<>();

		if (page.getOwningNode().getPageLanguageCode() == PageLanguageCode.PATH) {
			segments.add(page.getLanguage().getCode());
		}

		segments.add(node.getPublishDir());
		segments.add(folder.getPublishPath());

		return String.format(
			"%s://%s%s",
			getDefaultProtocol(page),
			node.getHostname(),
			FilePublisher.getPath(true, true, segments.toArray(new String[0])));
	}

	/**
	 * Get consumer that will extend the given links by adding tag name and part name
	 * @param links links to extend
	 * @return consumer
	 * @throws NodeException
	 */
	protected static Consumer<ExternalLink> externalLinkExtender(Collection<ExternalLink> links) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Value> values = t.getObjects(Value.class, links.stream().map(ExternalLink::getValueId).collect(Collectors.toSet()));
		Map<Integer, Value> valueMap = values.stream().collect(Collectors.toMap(Value::getId, value -> value));

		return link -> {
			if (valueMap.containsKey(link.getValueId())) {
				Value value = valueMap.get(link.getValueId());
				try {
					link.setContenttagName(((Tag)value.getContainer()).getName());
					link.setPartName(value.getPart().getName().toString());
				} catch (NodeException e) {
					logger.warn(String.format("Error while adding data to %s", link), e);
				}
			}
		};
	}

	/**
	 * Extract link data from the tag (fetched from the content) and store in the given external link instance.
	 * If the tag's construct is the magic link construct for one of the pages nodes, the text will be extracted
	 * from the part "text", otherwise the external link text will be left empty.
	 * @param content content containing the tag
	 * @param tag tag
	 * @param pageUrlPartType part type containing the URL
	 * @param link link, which is updated
	 * @return set containing the modified properties (empty if nothing changed)
	 * @throws NodeException
	 */
	protected static Set<String> extractLinkData(Content content, ContentTag tag, PageURLPartType pageUrlPartType, ExternalLink link) throws NodeException {
		Set<String> modified = new HashSet<>();
		if (link.getContentId() != content.getId()) {
			link.setContentId(content.getId());
			modified.add("contentId");
		}
		if (link.getContenttagId() != tag.getId()) {
			link.setContenttagId(tag.getId());
			modified.add("contenttagId");
		}
		if (link.getValueId() != pageUrlPartType.getValueObject().getId()) {
			link.setValueId(pageUrlPartType.getValueObject().getId());
			modified.add("valueId");
		}
		if (!Objects.equals(link.getUrl(), pageUrlPartType.getExternalTarget())) {
			link.setUrl(pageUrlPartType.getExternalTarget());
			modified.add("url");
		}

		String tagKeyword = tag.getConstruct().getKeyword();
		boolean isMagicLinkConstruct = !Observable.fromIterable(content.getPages()).map(Page::getOwningNode).distinct()
				.map(node -> AlohaRenderer.getMagicLinkConstructKeyword(node)).filter(keyword -> Objects.equals(keyword, tagKeyword)).isEmpty().blockingGet();

		String text = null;
		if (isMagicLinkConstruct) {
			Value textValue = tag.getValues().getByKeyname("text");
			if (textValue != null) {
				text = textValue.getValueText();
			}
		}

		if (!Objects.equals(link.getText(), text)) {
			link.setText(text);
			modified.add("text");
		}

		return modified;
	}

	/**
	 * Check the map of check history entries for external links, which turned invalid (i.e. where valid before and are now invalid or where checked for the first time)
	 * @param historyMap map of IDs to history entries
	 * @return set of IDs of entries turning invalid
	 */
	protected static Set<Integer> getEntriesTurningInvalid(Map<Integer, List<ExternalLinkCheckHistoryEntry>> historyMap) {
		return historyMap.entrySet().stream().filter(entry -> {
			// entry turned invalid if either it was checked for the first time, or if exactly "debounce" entries are invalid after a valid entry
			List<ExternalLinkCheckHistoryEntry> history = entry.getValue();

			// just to make sure, we have no "out-of-bounds" errors
			if (history.isEmpty()) {
				return false;
			}

			// check the newest entry (last of the list), if it is "valid", everything is fine
			if (history.get(history.size() - 1).getStatus() != ExternalLinkStatus.invalid) {
				return false;
			}

			if (history.size() == 1) {
				logger.debug(String.format("External Link %d was checked for the first time and is invalid", entry.getKey()));
				// first check for the external link -> "turned" invalid
				return true;
			} else if (history.size() < debounce + 1) {
				// not enough checks
				return false;
			} else if (history.get(history.size() - debounce - 1).getStatus() != ExternalLinkStatus.valid) {
				// link was not valid before
				return false;
			} else {
				// check all entries "in between"
				for (int i = history.size() - debounce; i < history.size() - 1; i++) {
					if (history.get(i).getStatus() != ExternalLinkStatus.invalid) {
						// found an valid check
						return false;
					}
				}
				logger.debug(String.format("External Link %d turned from valid to invalid (with debounce of %d)", entry.getKey(), debounce));
				return true;
			}
		}).map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	/**
	 * Notify editors about invalid links
	 * @throws NodeException
	 */
	protected static void notifyEditors() throws NodeException {
		Flowable.fromIterable(newInvalidExternalLinkIds).buffer(readBufferSize).map(list -> {
			return supply(t -> t.getObjects(Page.class, DBUtils.select(
					"SELECT DISTINCT page.id FROM external_link LEFT JOIN page ON external_link.content_id = page.content_id WHERE page.deleted = 0 AND external_link.id IN ("
							+ StringUtils.repeat("?", ",", list.size()) + ")",
					stmt -> {
						int index = 1;
						for (int linkId : list) {
							stmt.setInt(index++, linkId);
						}
					}, DBUtils.IDS)));
		}).flatMap(pages -> Flowable.fromIterable(pages)).flatMap(page -> {
			return supply(t -> {
				SystemUser editor = page.getEditor();
				if (!editor.getUserGroups().isEmpty()) {
					// last editor is active, return it
					return Flowable.just(editor);
				} else {
					// last editor is not active, get groups with permission to edit the page
					List<UserGroup> editingGroups = t.getObjects(UserGroup.class,
							PermHandler.getGroupsWithPermissionBit(Folder.TYPE_FOLDER, page.getFolderId(), PermHandler.PERM_PAGE_UPDATE));
					editingGroups = UserGroup.reduceUserGroups(editingGroups, ReductionType.CHILD);
					return Flowable.fromIterable(editingGroups).flatMap(group -> Flowable.fromIterable(supply(() -> group.getMembers())));
				}
			});
		}).distinct().blockingForEach(user -> {
			operate(t -> {
				logger.debug(String.format("Sending inbox message to user %s", user));
				DBUtils.executeStatement("SELECT language FROM systemsession WHERE user_id = ? ORDER BY id DESC LIMIT 1", Transaction.SELECT_STATEMENT,
						stmt -> {
							stmt.setInt(1, user.getId());
						}, rs -> {
							if (rs.next()) {
								ContentNodeHelper.setLanguageId(rs.getInt("language"));
							} else {
								ContentNodeHelper.setLanguageId(1);
							}
						});
				MessageSender messageSender = new MessageSender();
				t.addTransactional(messageSender);
				CNI18nString i18n = new CNI18nString("linkchecker.invalidlinks.message");
				messageSender.sendMessage(new Message(t.getUserId(), user.getId(), i18n.toString()));
			});
		});
	}

	/**
	 * Determine the protocols (http: or https:) used by the nodes of the pages containing the external link
	 * @param link external link
	 * @return set of links with protocols
	 * @throws NodeException
	 */
	protected static Set<String> addProtocol(ExternalLink link) throws NodeException {
		return supply(t -> {
			Set<String> urls = new HashSet<>();
			for (Page page : getPages(link)) {
				urls.add(String.format("%s:%s", getDefaultProtocol(page), link.getUrl()));
			}
			return urls;
		});
	}

	/**
	 * Make the link absolute by prepending the protocol, hostname and publish path to the url (this is done for all pages containing the link)
	 * @param link link to make absolute
	 * @return set of absolute URLs
	 * @throws NodeException
	 */
	protected static Set<String> makeAbsolute(ExternalLink link) throws NodeException {
		return supply(t -> {
			Set<String> urls = new HashSet<>();
			if (StringUtils.startsWith(link.getUrl(), "/")) {
				// prefix with protocol and hostname
				for (Page page : getPages(link)) {
					urls.add(String.format("%s%s", getAbsoluteBase(page), link.getUrl()));
				}
			} else {
				// prefix with path to folder of page
				for (Page page : getPages(link)) {
					urls.add(String.format("%s%s", getRelativeBase(page), link.getUrl()));
				}
			}
			return urls;
		});
	}

	/**
	 * Get list of pages containing the link
	 * @param link link
	 * @return list of pages
	 * @throws NodeException
	 */
	protected static List<Page> getPages(ExternalLink link) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Content content = t.getObject(Content.class, link.getContentId());
		if (content != null) {
			return content.getPages();
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Check the url. Get the result from the cache, if present.
	 * @param url url to check
	 * @return check result
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	protected static Pair<ExternalLinkStatus, String> checkWithCache(String url) throws InterruptedException, ExecutionException {
		AtomicBoolean newFuture = new AtomicBoolean(false);
		CompletableFuture<Pair<ExternalLinkStatus, String>> future = cache.computeIfAbsent(url, key -> {
			newFuture.set(true);
			return new CompletableFuture<>();
		});

		if (newFuture.get()) {
			future.complete(check(url));
		}
		return future.get();
	}

	/**
	 * Perform the check as HEAD request for the url
	 * @param url url to check
	 * @return check result
	 */
	public static Pair<ExternalLinkStatus, String> check(String url) {
		return check(StringUtils.trim(url), CheckMethod.HEAD);
	}

	/**
	 * Perform the check, either as HEAD or as GET request
	 * @param url url to check
	 * @param method check method
	 * @return check result as pair of flag and reason (reason will be null, if url is valid)
	 */
	protected static Pair<ExternalLinkStatus, String> check(String url, CheckMethod method) {
		try {
			logger.debug(String.format("Performing %s Request to %s", method, url));

			// Neither of {@link Request.Builder} and {@link HttpUrl#parse} is capable of checking 
			// the URL validity, as they both try to safeguard the request creation by disassembling 
			// the given url into parts and filling the request with these parts separately. This is why
			// the validity of a given URL has to be checked separately. ALLOW_LOCAL_URLS is set mainly 
			// for the unit testing purposes.
			if (!new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(url)) {
				throw new IllegalArgumentException("Invalid URL: " + url);
			}

			// check whether the host is currently blocked (responded with "Too many requests")
			String host = new URL(url).getHost();
			int now = (int)(System.currentTimeMillis() / 1000);
			int storedRetryAfter = retryAfterMap.getOrDefault(host, 0);
			if (storedRetryAfter > now) {
				logger.debug(String.format("Requests to host %s are currently blocked and should be retried after %d (now is %d)", host, storedRetryAfter, now));
				return Pair.of(ExternalLinkStatus.unchecked, "Blocked due to prior 429");
			} else {
				retryAfterMap.remove(host);
			}

			Request request = method.request(url);

			try (Response response = client.newCall(request).execute()) {
				int responseCode = response.code();
				if ((responseCode >= 200 && responseCode < 300) || responseCode == 301 || responseCode == 401 || responseCode == 403) {
					// every 2xx response and 401 or 403 are considered valid
					// 301 response is also considered valid, because this means that we were redirected to a Location, that the client will not follow (e.g. mailto:)
					return Pair.of(ExternalLinkStatus.valid, null);
				} else if (method == CheckMethod.HEAD && (responseCode == 400 || responseCode == 404 || responseCode == 405 || responseCode == 409 || responseCode == 503 || responseCode == 500)) {
					// 400: "bad request" some servers return 400 on HEAD requests, so try GET
					// 404: "not found" some servers will return 404 on HEAD requests, but 200 on GET
					// 405: "method not allowed" so we try GET
					// 409: "conflict"
					// 503: "service unavailable"
					// 500: "internal server error" server expects no HEAD requests"
					return check(url, CheckMethod.GET);
				} else if (responseCode == 429) {
					logger.debug(String.format("%s Request to %s returned code %d", method, url, responseCode));
					int usedRetryAfter = retryAfter;
					String retryAfterResponse = response.header("Retry-After", Integer.toString(retryAfter));
					try {
						usedRetryAfter = Integer.parseInt(retryAfterResponse);
					} catch (NumberFormatException e) {
						logger.warn(String.format("Retry-After header '%s' could not be interpreted as number, fallback to %d", retryAfterResponse, retryAfter));
					}
					retryAfterMap.put(host, now + usedRetryAfter);

					return Pair.of(ExternalLinkStatus.unchecked, Integer.toString(responseCode));
				} else {
					logger.warn(String.format("%s Request to %s returned code %d", method, url, responseCode));
					return Pair.of(ExternalLinkStatus.invalid, Integer.toString(responseCode));
				}
			}
		} catch (UnknownHostException e) {
			logger.warn(String.format("%s Request to %s caused exception %s (%s)", method, url, e.getClass().getName(), e.getLocalizedMessage()));
			return Pair.of(ExternalLinkStatus.invalid, "unknown host");
		} catch (ProtocolException e) {
			// special cases, where HEAD requests end in an infinite redirect loop
			if (StringUtils.startsWith(e.getLocalizedMessage(), "Too many follow-up requests:") && method == CheckMethod.HEAD) {
				return check(url, CheckMethod.GET);
			} else {
				logger.warn(String.format("%s Request to %s caused exception %s (%s)", method, url, e.getClass().getName(), e.getLocalizedMessage()));
				return Pair.of(ExternalLinkStatus.invalid, e.getLocalizedMessage());
			}
		} catch (Exception e) {
			logger.warn(String.format("%s Request to %s caused exception %s (%s)", method, url, e.getClass().getName(), e.getLocalizedMessage()));
			return Pair.of(ExternalLinkStatus.invalid, e.getLocalizedMessage());
		}
	}

	/**
	 * Check whether the given part shall be checked for broken external links.
	 * The part will not be checked if one of the following is true:
	 * <ul>
	 *   <li>The link is internal</li>
	 *   <li>The external target is empty</li>
	 *   <li>The external target starts with # (is only the link to an anchor)</li>
	 *   <li>The external target is a mailto: link</li>
	 *   <li>The external target is javascript:</li>
	 *   <li>The external target is a file: link</li>
	 *   <li>The external target is a callto: link</li>
	 *   <li>The external target is a tel: link</li>
	 *   <li>The external target is a skype: link</li>
	 * </ul>
	 * @param pageUrlPartType part to check
	 * @return true when the external target shall be checked, false if not
	 */
	protected static boolean isChecked(PageURLPartType pageUrlPartType) {
		String externalTarget = pageUrlPartType.getExternalTarget();
		if (pageUrlPartType.getInternal() == 1 || !isChecked(externalTarget)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Check whether the given external URL is checked
	 * @param url external URL
	 * @return true when checked, false if not
	 */
	public static boolean isChecked(String url) {
		if (StringUtils.isEmpty(url)
				|| StringUtils.startsWithIgnoreCase(url, "mailto:")
				|| StringUtils.startsWithIgnoreCase(url, "javascript:")
				|| StringUtils.startsWithIgnoreCase(url, "file:")
				|| StringUtils.startsWithIgnoreCase(url, "callto:")
				|| StringUtils.startsWithIgnoreCase(url, "tel:")
				|| StringUtils.startsWithIgnoreCase(url, "skype:")
				|| StringUtils.startsWithIgnoreCase(url, "#")) {
			return false;
		} else {
			return true;
}
	}

	/**
	 * Reset the map of blocked hosts (due to "Too many requests")
	 * This method is for testing purposes only
	 */
	public static void resetRetryAfterMap() {
		retryAfterMap.clear();
	}

	/**
	 * Enum for possible check methods
	 */
	public static enum CheckMethod {
		/**
		 * Check with a HEAD request
		 */
		HEAD(Builder::head),

		/**
		 * Check with a GET request
		 */
		GET(Builder::get);

		/**
		 * Function that builds the request
		 */
		Function<Builder, Builder> builderFunction;

		/**
		 * Create instance
		 * @param builderFunction builder
		 */
		CheckMethod(Function<Builder, Builder> builderFunction) {
			this.builderFunction = builderFunction;
		}

		/**
		 * Create a request to the url
		 * @param url url
		 * @return request
		 * @throws NodeException
		 */
		public Request request(String url) throws NodeException {
			Builder builder = new Request.Builder()
					.url(url)
					.header("User-Agent", "LinkChecker");
			return builderFunction.apply(builder).build();
		}
	}
}
