package com.gentics.contentnode.object.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.search.CustomSSLProtocolSocketFactory;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.publish.cr.DummyTagmapEntry;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import io.reactivex.Flowable;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;

/**
 * Indexer implementation
 */
public class Indexer {
	/**
	 * tType of the search index maintenance
	 */
	public final static int TYPE_SEARCH_MAINTENANCE = 10400;

	public final static String ELASTICSEARCH_URL_PARAM = "elasticsearch.url";
	public final static String ELASTICSEARCH_URL_DEFAULT = "http://localhost:9200/";

	/** Path to the SSL certificate used by Elasticsearch (must be in .pem format). */
	public final static String ELASTICSEARCH_CERT_PATH_PARAM = "elasticsearch.certPath";

	/** Path to the SSL certificate authority with which the certificate was created (must be in .pem format). */
	public final static String ELASTICSEARCH_CA_PATH_PARAM = "elasticsearch.caPath";

	/** Username to use for basic auth when connecting to Elasticsearch. */
	public final static String ELASTICSEARCH_USERNAME = "elasticsearch.username";
	/** Password to use for basic auth when connecting to Elasticsearch. */
	public final static String ELASTICSEARCH_PASSWORD = "elasticsearch.password";

	public final static String INDEX_NAME_PARAM = "elasticsearch.index";
	public final static String INDEX_SETTINGS_PARAM = "elasticsearch.settings";
	public final static String TEMPLATE_PARAM = "elasticsearch.index_template";
	public final static String INDEX_NAME_DEFAULT = "genticscms";
	public final static String THREADS_PARAM = "elasticsearch.threads";
	public final static int THREADS_DEFAULT = 10;

	public final static String INDEX_CONTENT_WHITELIST_PARAM = "elasticsearch.content.whitelist";
	public final static String INDEX_CONTENT_BLACKLIST_PARAM = "elasticsearch.content.blacklist";

	public final static String INDEX_CONTENT_INDEXED_CHARS_PARAM = "elasticsearch.content.indexedChars";

	public final static String ATTACHMENT_PIPELINE = "attachment";

	/**
	 * Batch size for loading objects to index
	 */
	public final static int BATCH_SIZE = 100;

	/**
	 * Batch size for loading document IDs from the index
	 */
	public final static int OBSOLETION_BATCH_SIZE = 10000;

	/**
	 * Regex Pattern for the document counts response
	 */
	public final static Pattern INDEX_DOCS_PATTERN = Pattern.compile("(?<index>[^\\s]+)\\s+(?<count>\\d+)");

	/**
	 * Attribute names for permission attributes
	 */
	public final static Set<String> PERMISSION_ATTRIBUTES = new HashSet<>(Arrays.asList("groupId"));

	/**
	 * Attribute names for nodeId attributes
	 */
	public final static Set<String> NODE_ID_ATTRIBUTES = new HashSet<>(Arrays.asList("nodeId"));

	protected static String elasticsearchUrl = ELASTICSEARCH_URL_DEFAULT;

	protected static String indexName = INDEX_NAME_DEFAULT;

	protected static Protocol customHttpsProtocol;

	protected static Credentials credentials;
	protected static AuthScope authScope;

	/**
	 * Content mimetype whitelist patterns
	 */
	protected static List<Pattern> contentWhitelist = new ArrayList<>();

	/**
	 * Content mimetype blacklist patterns
	 */
	protected static List<Pattern> contentBlacklist = new ArrayList<>();

	/**
	 * Number of indexed characters for the attachement pipeline
	 */
	protected static int indexedChars = -1;

	/**
	 * Logger
	 */
	public final static NodeLogger logger = NodeLogger.getNodeLogger(Indexer.class);

	/**
	 * Map of document field types to attribute types
	 */
	protected final static Map<String, Integer> attributeTypeMap = new HashMap<>();

	/**
	 * Template definition (may be null)
	 */
	protected static JSONObject templateDefinition;

	/**
	 * Mappings per document type
	 */
	protected static Map<IndexType, JSONObject> mappings = new HashMap<>();

	/**
	 * Entries per document type
	 */
	protected static Map<IndexType, Set<TagmapEntryRenderer>> entries = new HashMap<>();

	/**
	 * Map of indextype -> map of language -> index
	 */
	protected static Map<IndexType, Map<String, SearchIndex>> indices = new HashMap<>();

	/**
	 * Thread factory
	 */
	protected static ThreadFactory threadFactory = new PrefixedThreadFactory("indexer");

	/**
	 * Executor Service
	 */
	protected static ExecutorService service;

	static {
		attributeTypeMap.put("text", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG);
		attributeTypeMap.put("keyword", GenticsContentAttribute.ATTR_TYPE_TEXT_LONG);
		attributeTypeMap.put("date", GenticsContentAttribute.ATTR_TYPE_DATE);
		attributeTypeMap.put("integer", GenticsContentAttribute.ATTR_TYPE_INTEGER);
		attributeTypeMap.put("boolean", GenticsContentAttribute.ATTR_TYPE_INTEGER);
	}

	/**
	 * Initialize indexing
	 */
	public static synchronized void init() {
		shutdown();

		NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		elasticsearchUrl = ObjectTransformer.getString(prefs.getProperty(ELASTICSEARCH_URL_PARAM), ELASTICSEARCH_URL_DEFAULT);
		prepareCustomHttpsProtocol(prefs);
		prepareAuthentication(prefs);
		indexName = ObjectTransformer.getString(prefs.getProperty(INDEX_NAME_PARAM), INDEX_NAME_DEFAULT);

		if (prefs.isFeature(Feature.ELASTICSEARCH)) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Elasticsearch URL is %s", elasticsearchUrl));
				logger.info(String.format("Elasticsearch Index prefix is %s", indexName));
			}

			contentBlacklist.clear();
			String[] blackListConfig = prefs.getProperties(INDEX_CONTENT_BLACKLIST_PARAM);
			if (blackListConfig != null) {
				for (String patternString : blackListConfig) {
					try {
						if (logger.isInfoEnabled()) {
							logger.info(String.format("Found blacklist pattern %s", patternString));
						}
						contentBlacklist.add(Pattern.compile(patternString));
					} catch (PatternSyntaxException e) {
						logger.warn(String.format("Ignoring invalid blacklist pattern %s", patternString), e);
					}
				}
			}
			contentWhitelist.clear();
			String[] whiteListConfig = prefs.getProperties(INDEX_CONTENT_WHITELIST_PARAM);
			if (whiteListConfig != null) {
				for (String patternString : whiteListConfig) {
					try {
						if (logger.isInfoEnabled()) {
							logger.info(String.format("Found whitelist pattern %s", patternString));
						}
						contentWhitelist.add(Pattern.compile(patternString));
					} catch (PatternSyntaxException e) {
						logger.warn(String.format("Ignoring invalid whitelist pattern %s", patternString), e);
					}
				}
			}

			if (logger.isInfoEnabled()) {
				logger.info(String.format("Using blacklist patterns: %s", contentBlacklist));
				logger.info(String.format("Using whitelist patterns: %s", contentWhitelist));
			}

			indexedChars = ObjectTransformer.getInt(prefs.getProperty(INDEX_CONTENT_INDEXED_CHARS_PARAM), -1);
			if (logger.isInfoEnabled()) {
				logger.info(String.format(
						"Limit for indexed characters in file contents: %d (-1 means that ElasticSearch limits will be used)",
						indexedChars));
			}

			// check whether index exists
			Map<SearchIndex, Set<String>> reIndexTypes = null;

			try {
				initMappings();
				indices = getRequiredIndices();
				reIndexTypes = prepareIndices(flowableIndices().toList().blockingGet(), false);
			} catch (NodeException e) {
				logger.error("Error while initializing Indexer", e);
			}

			int threads = ObjectTransformer.getInt(prefs.getProperty(THREADS_PARAM), THREADS_DEFAULT);
			service = Executors.newFixedThreadPool(threads, threadFactory);
			if (logger.isInfoEnabled()) {
				logger.info("Started Indexer service");
			}

			if (!ObjectTransformer.isEmpty(reIndexTypes)) {
				reIndex(reIndexTypes, false);
			}

			try {
				Trx.operate(() -> DBUtils.update("UPDATE indexerqueue SET replay = ?", 1));
				int indexerQueueToReplay = Trx.supply(() -> DBUtils.select("SELECT COUNT(*) c FROM indexerqueue WHERE replay = ?", ps -> ps.setInt(1, 1), DBUtils.firstInt("c")));

				if (indexerQueueToReplay > 0) {
					replayIndexerQueue();
				}
			} catch (NodeException e) {
				logger.error("Error while re-scheduling left-over indexing tasks", e);
			}
		}
	}

	/**
	 * Create custom HTTPS protocol using the configured certificate and certificate authority for Elasticsearch.
	 *
	 * <p>
	 *     The created custom {@link Protocol} is stored in {@link #customHttpsProtocol}.
	 *
	 * </p>
	 * @param prefs The CMS settings.
	 */
	protected static void prepareCustomHttpsProtocol(NodePreferences prefs) {
		String certPath = prefs.getProperty(ELASTICSEARCH_CERT_PATH_PARAM);
		String caPath = prefs.getProperty(ELASTICSEARCH_CA_PATH_PARAM);

		if (elasticsearchUrl.startsWith("https://")) {
			if (!StringUtils.isEmpty(certPath) && !StringUtils.isEmpty(caPath)) {
				customHttpsProtocol = new Protocol("https", new CustomSSLProtocolSocketFactory(certPath, caPath), 9200);
			} else if (logger.isInfoEnabled()) {
				logger.info(String.format("Elasticsearch URL uses HTTPS but no custom certificate (%s) and/or certificate authority (%s) specified. Make sure that Elasticsearch is not using a self signed certificate.", ELASTICSEARCH_CERT_PATH_PARAM, ELASTICSEARCH_CA_PATH_PARAM));
			}
		}
	}

	protected static void prepareAuthentication(NodePreferences prefs) {
		URL url;

		try {
			url = new URL(elasticsearchUrl);
		} catch (MalformedURLException e) {
			if (logger.isWarnEnabled()) {
				logger.warn(String.format("Could not initialize basic auth for Elasticsearch client: invalid Elasticsearch URL {%s}", elasticsearchUrl));
			}

			return;
		}

		String username = prefs.getProperty(ELASTICSEARCH_USERNAME);
		String password = prefs.getProperty(ELASTICSEARCH_PASSWORD);

		if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
			credentials = new UsernamePasswordCredentials(username, password);
			authScope = new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM);
		}
	}

	/**
	 * Shutdown indexer
	 * @return true if the indexer service was cleanly shut down, false if tasks were still running after waiting the timeout.
	 */
	public static boolean shutdown() {
		return shutdown(10, TimeUnit.SECONDS);
	}

	/**
	 * Shutdown indexer
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @return true if the indexer service was cleanly shut down, false if tasks were still running after waiting the timeout.
	 */
	public static synchronized boolean shutdown(long timeout, TimeUnit unit) {
		boolean success = true;
		if (service != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Stopping Indexer service");
			}
			service.shutdown();
			try {
				if (!service.awaitTermination(timeout, unit)) {
					logger.warn("Indexer service did not stop in time, forcing stop now");
					service.shutdownNow();
					success = false;
				} else {
					if (logger.isInfoEnabled()) {
						logger.info("Indexer service stopped");
					}
				}
			} catch (InterruptedException e) {
				logger.warn("Error while stopping Indexer service", e);
				success = false;
			}
			service = null;
		}
		return success;
	}

	/**
	 * Asynchronously add the object to the index
	 * @param object object
	 * @throws NodeException
	 */
	public static synchronized void index(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		if (!isFeature() || service == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Schedule indexing of %s", object));
		}

		Set<Integer> queueIds = addToIndexerQueue(object, IndexerAction.put);

		service.execute(() -> {
			try {
				putObject(object);
				Trx.operate(() -> removeFromIndexerQueue(queueIds));
			} catch (NodeException e) {
				logger.error(String.format("Error while indexing %s", object), e);
			}
		});
	}

	/**
	 * Asynchronously update the attributes of an object in the index
	 * @param object object
	 * @param attributes set of attributes to be updated
	 * @throws NodeException
	 */
	public static synchronized void update(IndexableNodeObject<? extends NodeObject> object, Set<String> attributes) throws NodeException {
		if (!isFeature() || service == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Schedule updating of %s", object));
		}

		Set<Integer> queueIds = addToIndexerQueue(object, IndexerAction.put);

		service.execute(() -> {
			try {
				updateObject(object, attributes);
				Trx.operate(() -> removeFromIndexerQueue(queueIds));
			} catch (NodeException e) {
				logger.error(String.format("Error while updating %s", object), e);
			}
		});
	}

	/**
	 * Asynchronously remove the object from the index
	 * @param object object
	 * @throws NodeException
	 */
	public static synchronized void remove(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		if (!isFeature() || service == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Schedule removing of %s from index", object));
		}

		Set<Integer> queueIds = addToIndexerQueue(object, IndexerAction.delete);

		service.execute(() -> {
			try {
				deleteObject(object);
				Trx.operate(() -> removeFromIndexerQueue(queueIds));
			} catch (NodeException e) {
				logger.error(String.format("Error while removing %s from index", object), e);
			}
		});
	}

	/**
	 * Update the permissions (groupIds) of all objects in the folder (but not child folders)
	 * @param folderId folder ID
	 */
	public static void updatePermissions(int folderId) {
		updatePermissions(Collections.singleton(folderId), false);
	}

	/**
	 * Update the permissions (groupIds) of all objects in the folders (but not child folders)
	 * @param folderIds collection of folder IDs
	 * @param updateFolders true to also update the folders themselves, false to only update objects in the folders
	 */
	public static void updatePermissions(Collection<Integer> folderIds, boolean updateFolders) {
		if (!isFeature() || service == null) {
			return;
		}

		if (updateFolders) {
			flowableIndices().filter(index -> index.type.getObjectType() == Folder.TYPE_FOLDER).blockingForEach(index -> {
				service.execute(() -> {
					try {
						if (logger.isInfoEnabled()) {
							logger.info(String.format("Starting reindex permission attributes for %s", index));
						}
						index.reIndex(PERMISSION_ATTRIBUTES, () -> new HashSet<>(folderIds));
						if (logger.isInfoEnabled()) {
							logger.info(String.format("Reindex of permission attributes in %s done", index));
						}
					} catch (NodeException e) {
						logger.error(String.format("Error while reindexing %s", index), e);
					}
				});
			});
		}

		flowableIndices().filter(index -> index.type.getObjectType() != Folder.TYPE_FOLDER).blockingForEach(index -> {
			service.execute(() -> {
				try {
					if (logger.isInfoEnabled()) {
						logger.info(String.format("Starting reindex permission attributes for %s", index));
					}
					for (int folderId : folderIds) {
						index.reIndexSubObjects(PERMISSION_ATTRIBUTES, folderId);
					}
					if (logger.isInfoEnabled()) {
						logger.info(String.format("Reindex of permission attributes in %s done", index));
					}
				} catch (NodeException e) {
					logger.error(String.format("Error while reindexing %s", index), e);
				}
			});
		});
	}

	/**
	 * Update the node IDs of all objects in the given node
	 * @param nodeId node ID
	 */
	public static void updateNodeIds(int nodeId) {
		if (!isFeature() || service == null) {
			return;
		}

		flowableIndices().blockingForEach(index -> {
			service.execute(() -> {
				try {
					if (logger.isInfoEnabled()) {
						logger.info(String.format("Starting reindex nodeId attributes for %s", index));
					}
					index.reIndexObjectsInNode(NODE_ID_ATTRIBUTES, nodeId);
					if (logger.isInfoEnabled()) {
						logger.info(String.format("Reindex of nodeId attributes in %s done", index));
					}
				} catch (NodeException e) {
					logger.error(String.format("Error while reindexing %s", index), e);
				}
			});
		});
	}

	/**
	 * Return flowable of all indices
	 * @return flowable
	 */
	public static Flowable<SearchIndex> flowableIndices() {
		return Flowable.fromIterable(indices.values()).flatMap(m -> Flowable.fromIterable(m.values()));
	}

	/**
	 * Asynchronously reindex all objects
	 */
	public static void reIndexAll() {
		Map<SearchIndex, Set<String>> indexSet = flowableIndices().toMap(k -> k, v -> (Set<String>) null).blockingGet();
		reIndex(indexSet, false);
	}

	/**
	 * Asynchronously recreate the index
	 * @param index index to recreate
	 * @param drop true to drop first
	 */
	public static void reIndex(SearchIndex index, boolean drop) {
		Map<SearchIndex, Set<String>> indexMap = new HashMap<>();
		indexMap.put(index, null);
		reIndex(indexMap, drop);
	}

	/**
	 * Asynchronously reindex objects of the given indices
	 * @param indices indices
	 * @param drop true to drop first
	 */
	protected static synchronized void reIndex(Map<SearchIndex, Set<String>> indices, boolean drop) {
		if (!isFeature() || service == null) {
			return;
		}
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Schedule reindex for %s", indices.keySet()));
		}

		HttpClient client = getClient();

		for (SearchIndex index : indices.keySet()) {
			try {
				prepare(client, index, drop);
			} catch (NodeException e) {
				logger.error(String.format("Error while preparing index %s", index), e);
			}
		}

		for (Entry<SearchIndex, Set<String>> entry : indices.entrySet()) {
			SearchIndex index = entry.getKey();
			service.execute(() -> {
				try {
					if (logger.isInfoEnabled()) {
						logger.info(String.format("Starting reindex %s", index));
					}
					index.reIndex(entry.getValue());
					if (logger.isInfoEnabled()) {
						logger.info(String.format("Reindex of %s done", index));
					}
				} catch (NodeException e) {
					logger.error(String.format("Error while reindexing %s", index), e);
				}
			});
		}
	}

	/**
	 * Get the search indices for the object
	 * @param object object
	 * @return search indices (may be empty)
	 * @throws NodeException
	 */
	public final static Set<SearchIndex> getIndices(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		IndexType type = object.getIndexType();
		Set<String> languages = new HashSet<>();
		if (type.isMultiLanguage()) {
			if (type.isLanguagePerInstance()) {
				String language = supply(() -> object.getIndexLanguage());
				if (language != null) {
					languages.add(language);
				}
			} else {
				languages.addAll(supply(() -> object.getIndexLanguages()));
			}
		}
		return getIndices(type, languages);
	}

	/**
	 * Get the search indices for the index type and languages
	 * @param type index type
	 * @param languages optional languages
	 * @return search indices (may be empty)
	 */
	public final static Set<SearchIndex> getIndices(IndexType type, Set<String> languages) {
		Map<String, SearchIndex> map = indices.get(type);
		if (map == null) {
			return null;
		}

		if (languages.isEmpty()) {
			return Collections.singleton(map.get(null));
		} else {
			return languages.stream().map(l -> map.get(l)).filter(i -> i != null).collect(Collectors.toSet());
		}
	}

	/**
	 * Get the search index for the index type and language
	 * @param type index type
	 * @param language optional language
	 * @return search index (null if not found)
	 */
	public final static SearchIndex getIndex(IndexType type, String language) {
		Map<String, SearchIndex> map = indices.get(type);
		if (map == null) {
			return null;
		}

		return map.get(language);
	}

	/**
	 * Try to update the object (render only the provided attributes). If the object does not yet exist in the index,
	 * it will be indexed completely by using {@link #putObject(IndexableNodeObject)}.
	 * @param object object
	 * @param attributes set of attributes (must not be null or empty)
	 * @throws NodeException
	 */
	protected static void updateObject(IndexableNodeObject<? extends NodeObject> object, Set<String> attributes) throws NodeException {
		Set<SearchIndex> indices = getIndices(object);
		if (!indices.isEmpty()) {
			for (SearchIndex index : indices) {
				index.update(object, attributes);
			}

			// if the object contains the languages, we need to remove it from all other indices
			IndexType type = object.getIndexType();
			if (type.isMultiLanguage() && !type.isLanguagePerInstance()) {
				Map<String, SearchIndex> map = Indexer.indices.get(type);
				ArrayList<SearchIndex> removeFrom = new ArrayList<>(map.values());
				removeFrom.removeAll(indices);
				for (SearchIndex index : removeFrom) {
					index.delete(object);
				}
			}
		} else {
			if (logger.isDebugEnabled()) {
				operate(() -> logger.debug(String.format("Omit updating of %s, because index was not found", object)));
			}
		}
	}

	/**
	 * Synchronously add the object to the index
	 * @param object object
	 * @throws NodeException
	 */
	protected static void putObject(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		Set<SearchIndex> indices = getIndices(object);
		if (!indices.isEmpty()) {
			for (SearchIndex index : indices) {
				index.put(object);
			}

			// if the object contains the languages, we need to remove it from all other indices
			IndexType type = object.getIndexType();
			if (type.isMultiLanguage() && !type.isLanguagePerInstance()) {
				Map<String, SearchIndex> map = Indexer.indices.get(type);
				ArrayList<SearchIndex> removeFrom = new ArrayList<>(map.values());
				removeFrom.removeAll(indices);
				for (SearchIndex index : removeFrom) {
					index.delete(object);
				}
			}
		} else {
			if (logger.isDebugEnabled()) {
				operate(() -> logger.debug(String.format("Omit indexing of %s, because index was not found", object)));
			}
		}
	}

	/**
	 * Synchronously remove the object from the index
	 * @param object object
	 * @throws NodeException
	 */
	protected static void deleteObject(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		Set<SearchIndex> indices = getIndices(object);
		if (!indices.isEmpty()) {
			for (SearchIndex index : indices) {
				index.delete(object);
			}
		} else {
			if (logger.isDebugEnabled()) {
				operate(() -> logger.debug(String.format("Omit deleting of %s, because index was not found", object)));
			}
		}
	}

	/**
	 * Return true when the feature is activated
	 * @return true for feature
	 */
	public final static boolean isFeature() {
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		return prefs.isFeature(Feature.ELASTICSEARCH);
	}

	/**
	 * Get the index template name
	 * @return index template name
	 */
	public final static String getTemplateName() {
		return String.format("%s_template", indexName);
	}

	/**
	 * Get a configured HTTP client for Elasticsearch.
	 *
	 * <p>
	 *     When Elasticsearch uses HTTPS and the settings contain a certificate and certificate authority,
	 *     the client will be configured to use this custom HTTPS configuration.
	 * </p>
	 *
	 * @return The configured HTTP client for Elasticsearch
	 */
	public static HttpClient getClient() {
		HttpClient httpClient = new HttpClient();
		String proto;
		String host;
		int port;

		try {
			URL esUrl = new URL(elasticsearchUrl);

			proto = esUrl.getProtocol();
			host = esUrl.getHost();
			port = esUrl.getPort();
		} catch (MalformedURLException e) {
			logger.error(String.format("Invalid Elasticsearch URL {%s}: %s", elasticsearchUrl, e.getMessage()), e);

			return httpClient;
		}

		if (customHttpsProtocol != null) {
			httpClient.getHostConfiguration().setHost(host, port, customHttpsProtocol);
		} else {
			Protocol protocol = "https".equalsIgnoreCase(proto) ? Protocol.getProtocol("https") : Protocol.getProtocol("http");

			httpClient.getHostConfiguration().setHost(host, port, protocol);
		}

		if (authScope != null && credentials != null) {
			httpClient.getState().setCredentials(authScope, credentials);
		}

		return httpClient;
	}

	/**
	 * Get the URL for the index template
	 * @return index template URL
	 */
	public final static String getTemplateUrl() {
		return String.format("/_template/%s", getTemplateName());
	}

	/**
	 * Get the URL for the given pipeline
	 * @param pipeline name of the pipeline
	 * @return pipeline URL
	 */
	public final static String getPipelineUrl(String pipeline) {
		return String.format("/_ingest/pipeline/%s", pipeline);
	}

	/**
	 * Get the URL of the index
	 * @param name index name
	 * @return index URL
	 */
	public final static String getIndexUrl(String name) {
		return String.format("/%s", name);
	}

	/**
	 * Get the Index name
	 * @param type object type
	 * @return index name
	 */
	public final static String getIndexName(String type) {
		return String.format("%s_%s", indexName, type);
	}

	/**
	 * Get mapping URL
	 * @param type object type
	 * @return mapping URL
	 */
	public final static String getMappingUrl(String type) {
		return String.format("%s/_mapping/%s", getIndexUrl(type), type);
	}

	/**
	 * Get the URL for all indices
	 * @param languages
	 * @param types
	 * @return multi index URL
	 */
	public final static String getMultiIndexUrl(List<String> languages, List<IndexType> types) {
		Flowable<SearchIndex> flowable = flowableIndices();
		if (!ObjectTransformer.isEmpty(types)) {
			flowable = flowable.filter(index -> types.contains(index.type));
		}
		if (!ObjectTransformer.isEmpty(languages)) {
			flowable = flowable.filter(index -> !index.type.isMultiLanguage() || languages.contains(index.language));
		}
		String names = flowable.map(SearchIndex::getName).collectInto(new StringBuilder(), (builder, name) -> {
			if (builder.length() > 0) {
				builder.append(",");
			}
			builder.append(name);
		}).blockingGet().toString();

		return String.format("/%s", names);
	}

	/**
	 * Update the index stats (document counts) in the {@link SearchIndex} instances
	 * @throws NodeException
	 */
	public final static void updateIndexStats() throws NodeException {
		try {
			HttpClient httpClient = getClient();
			GetMethod getStats = new GetMethod("/_cat/indices");
			getStats.setQueryString(new NameValuePair[] {new NameValuePair("h", "index,docs.count")});
			int statsResponseCode = httpClient.executeMethod(getStats);

			if (statsResponseCode != 200) {
				throw new NodeException(String.format("Request to get stats returned http code %d, body %s", statsResponseCode,
						getStats.getResponseBodyAsString()));
			}

			Map<String, Integer> counts = new HashMap<>();
			try (BufferedReader reader = new BufferedReader(new StringReader(getStats.getResponseBodyAsString()))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					Matcher matcher = INDEX_DOCS_PATTERN.matcher(line);
					if (matcher.matches()) {
						String indexName = matcher.group(1);
						int count = Integer.parseInt(matcher.group(2));
						counts.put(indexName, count);
					}
				}
			}

			flowableIndices().blockingForEach(index -> {
				if (counts.containsKey(index.getName())) {
					index.setDocCount(counts.get(index.getName()));
				}
			});

			Set<SearchIndex> indices = new HashSet<>();
			Trx.supply(() -> DBUtils.select(
					"SELECT index_type, index_language, COUNT(*) c FROM indexerqueue GROUP BY index_type, index_language",
					rs -> {
						while (rs.next()) {
							SearchIndex index = getIndex(IndexType.valueOf(rs.getString("index_type")),
									rs.getString("index_language"));
							if (index != null) {
								index.setPendingCount(rs.getInt("c"));
								indices.add(index);
							}
						}
						return true;
					}));
			flowableIndices().filter(index -> !indices.contains(index))
					.blockingForEach(index -> index.setPendingCount(0));
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Check whether any required indices are missing or existing indices are superfluous.
	 * Create missing and drop superfluous
	 * @throws NodeException
	 */
	public final static void updateIndices() throws NodeException {
		Map<IndexType, Map<String, SearchIndex>> newIndices = getRequiredIndices();
		Set<SearchIndex> missing = new HashSet<>();
		Set<SearchIndex> superfluous = new HashSet<>();

		// check which new indices are not yet known
		List<SearchIndex> newList = Flowable.fromIterable(newIndices.values()).flatMap(m -> Flowable.fromIterable(m.values())).toList().blockingGet();
		List<SearchIndex> oldList = flowableIndices().toList().blockingGet();

		missing.addAll(newList);
		missing.removeAll(oldList);

		superfluous.addAll(oldList);
		superfluous.removeAll(newList);

		// when there is a diff, we need to do something
		if (!missing.isEmpty() || !superfluous.isEmpty()) {
			HttpClient httpClient = getClient();

			for (SearchIndex newIndex : missing) {
				if (!newIndex.exists(httpClient)) {
					newIndex.create(httpClient);
				}
			}
			for (SearchIndex oldIndex : superfluous) {
				if (oldIndex.exists(httpClient)) {
					oldIndex.drop(httpClient);
				}
			}
			indices = newIndices;
		}
	}

	/**
	 * Get the name of the index
	 * @param type type of indexed objects
	 * @param language optional language
	 * @return full index name
	 */
	protected final static String getName(IndexType type, String language) {
		return String.format("%s_%s%s", indexName, type.name(), language != null ? "_" + language : "");
	}

	/**
	 * Get the configured index settings for the given type and optional language.
	 * This will first try to get language specific settings, and do a fallback to the default settings, if not found.
	 * If no index settings are configured, the function will return null.
	 * @param type type
	 * @param language optional language
	 * @return index settings object or null
	 * @throws NodeException
	 */
	protected final static JSONObject getIndexSettings(IndexType type, String language) throws NodeException {
		List<String> keys = new ArrayList<>();
		if (language != null) {
			keys.add(String.format("%s_%s", type.name(), language));
		}
		keys.add(type.name());

		for (String key : keys) {
			Map<String, Object> settingsMap = NodeConfigRuntimeConfiguration.getPreferences().getPropertyMap(String.format("%s.%s", INDEX_SETTINGS_PARAM, key));
			if (!ObjectTransformer.isEmpty(settingsMap)) {
				ObjectMapper mapper = new ObjectMapper();
				try {
					return new JSONObject(mapper.writeValueAsString(settingsMap));
				} catch (JsonProcessingException | JSONException e) {
					throw new NodeException(e);
				}
			}
		}
		return null;
	}

	/**
	 * Get the configured template definition. May return null, if no template configured
	 * @return json object or null
	 * @throws NodeException
	 */
	protected final static JSONObject getTemplateDefinition() throws NodeException {
		Map<String, Object> templateDefinition = NodeConfigRuntimeConfiguration.getPreferences().getPropertyMap(TEMPLATE_PARAM);
		if (!ObjectTransformer.isEmpty(templateDefinition)) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JSONObject def = new JSONObject(mapper.writeValueAsString(templateDefinition));
				def.put("index_patterns", Arrays.asList(String.format("%s_*", indexName)));
				return def;
			} catch (JsonProcessingException | JSONException e) {
				throw new NodeException(e);
			}
		}
		return null;
	}

	/**
	 * Load the index template
	 * @param httpClient http client
	 * @return index template as JSON object, if found, null if not found
	 * @throws NodeException in case of errors
	 */
	protected final static JSONObject loadTemplate(HttpClient httpClient) throws NodeException {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Loading index template");
			}
			GetMethod loadTemplateMethod = new GetMethod(getTemplateUrl());
			int responseCode = httpClient.executeMethod(loadTemplateMethod);

			switch(responseCode) {
				case 200:
					if (logger.isInfoEnabled()) {
						logger.info("Index template found");
					}
					JSONObject response = new JSONObject(loadTemplateMethod.getResponseBodyAsString());
					return response.getJSONObject(getTemplateName());
				case 404:
					if (logger.isInfoEnabled()) {
						logger.info("Index template not found");
					}
					return null;
				default:
					throw new NodeException(String.format("Request to load index template returned http code %d, body %s", responseCode,
							loadTemplateMethod.getResponseBodyAsString()));
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Delete the template
	 * @param httpClient http client
	 * @throws NodeException
	 */
	protected final static void deleteTemplate(HttpClient httpClient) throws NodeException {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Deleting index template");
			}
			DeleteMethod deleteMethod = new DeleteMethod(getTemplateUrl());
			int deleteResponseCode = httpClient.executeMethod(deleteMethod);

			switch (deleteResponseCode) {
				case 200:
					if (logger.isInfoEnabled()) {
						logger.info("Index template deleted");
					}
					break;
				case 404:
					if (logger.isInfoEnabled()) {
						logger.info("Index template not found");
					}
					break;
				default:
					throw new NodeException(String.format("Request to delete index template returned http code %d, body %s",
							deleteResponseCode, deleteMethod.getResponseBodyAsString()));
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Create the template
	 * @param httpClient http client
	 * @throws NodeException
	 */
	protected final static void createTemplate(HttpClient httpClient)
			throws NodeException {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Creating index template");
			}
			PutMethod createMethod = new PutMethod(getTemplateUrl());
			createMethod.setRequestEntity(
					new StringRequestEntity(templateDefinition.toString(), "application/json", "UTF-8"));

			int createResponseCode = httpClient.executeMethod(createMethod);
			if (createResponseCode >= 400) {
				throw new NodeException(String.format("Request to create index template returned http code %d, body %s",
						createResponseCode, createMethod.getResponseBodyAsString()));
			}

			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info("Created index template");
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Check existence of attachment pipeline
	 * @param httpClient http client
	 * @return true if pipeline exists, false if not
	 * @throws NodeException
	 */
	protected final static boolean checkAttachmentPipeline(HttpClient httpClient) throws NodeException {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Checking attachment pipeline");
			}
			GetMethod checkPipelineMethod = new GetMethod(getPipelineUrl(ATTACHMENT_PIPELINE));
			int pipelineResponseCode = httpClient.executeMethod(checkPipelineMethod);

			switch (pipelineResponseCode) {
				case 200:
					if (logger.isInfoEnabled()) {
						logger.info("Attachment pipeline found");
					}
					return true;
				case 404:
					if (logger.isInfoEnabled()) {
						logger.info("Attachment pipeline not found");
					}
					return false;
				default:
					throw new NodeException(String.format("Request to check attachment pipeline returned http code %d, body %s", pipelineResponseCode,
							checkPipelineMethod.getResponseBodyAsString()));
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Put attachment pipeline
	 * @param httpClient client
	 * @throws NodeException
	 */
	protected final static void putAttachmentPipeline(HttpClient httpClient) throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info("Define attachment pipeline");
		}
		try {
			JSONObject pipeline = null;
			try (InputStream in = Indexer.class.getResourceAsStream("attachment_pipeline.json")) {
				pipeline = new JSONObject(StringUtils.readStream(in));
			}

			if (indexedChars > 0) {
				pipeline.getJSONArray("processors").getJSONObject(0).getJSONObject("attachment").put("indexed_chars", indexedChars);
			}

			PutMethod putPipeline = new PutMethod(getPipelineUrl(ATTACHMENT_PIPELINE));
			putPipeline.setRequestEntity(new StringRequestEntity(pipeline.toString(), "application/json", "UTF-8"));
			int responseCode = httpClient.executeMethod(putPipeline);

			if (responseCode >= 400) {
				throw new NodeException(String.format("Request to put attachment pipeline returned http status %d, body %s", responseCode,
						putPipeline.getResponseBodyAsString()));
			}

		} catch (JSONException | IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Initialize the mappings
	 * @throws NodeException
	 */
	protected final static void initMappings() throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info("Initialize mappings");
		}
		mappings.clear();
		entries.clear();
		for (IndexType type : IndexType.values()) {
			String mappingResourceName = type.name() + "_mapping.json";
			Set<TagmapEntryRenderer> tagmapEntries = new HashSet<>();
			try (InputStream in = Indexer.class.getResourceAsStream(mappingResourceName)) {
				JSONObject mapping = new JSONObject(StringUtils.readStream(in));

				// create the tagmap entries
				JSONObject properties = mapping.getJSONObject("properties");
				for (String name : JSONObject.getNames(properties)) {
					JSONObject property = properties.getJSONObject(name);
					if (property.has("gtx_tagname")) {
						tagmapEntries.add(new DummyTagmapEntry(type.getObjectType(), property.getString("gtx_tagname"), name,
								attributeTypeMap.get(property.getString("type")), 0));
						property.remove("gtx_tagname");
					}
				}

				mappings.put(type, mapping);
				entries.put(type, tagmapEntries);
			} catch (IOException | JSONException e) {
				throw new NodeException(e);
			}
		}
		templateDefinition = getTemplateDefinition();
	}

	/**
	 * Prepare the search indices (if this instance is the cluster master or not in a cluster)
	 * @param toPrepare indices to prepare
	 * @param drop true to drop and recreate existing indices
	 * @return the set of indices which need to be refreshed
	 * @throws NodeException
	 */
	protected final static Map<SearchIndex, Set<String>> prepareIndices(Collection<SearchIndex> toPrepare, boolean drop) throws NodeException {
		if (!DistributionUtil.isTaskExecutionAllowed()) {
			return Collections.emptyMap();
		}
		Map<SearchIndex, Set<String>> result = new HashMap<>();
		HttpClient httpClient = getClient();
		JSONObject template = loadTemplate(httpClient);

		if (template == null && templateDefinition != null) {
			createTemplate(httpClient);
		} else if (template != null && templateDefinition == null) {
			deleteTemplate(httpClient);
		} else if (template != null && templateDefinition != null) {
			ObjectMapper mapper = new ObjectMapper();

			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> existingMap = mapper.readValue(template.toString(), Map.class);
				@SuppressWarnings("unchecked")
				Map<String, Object> settingsMap = mapper.readValue(templateDefinition.toString(), Map.class);

				transformMap(existingMap);
				transformMap(settingsMap);

				// all keys of the settings must exist in the index and have the identical value
				boolean equal = true;
				for (String key : settingsMap.keySet()) {
					if (!existingMap.containsKey(key)) {
						equal = false;
						break;
					}
					if (!Objects.deepEquals(existingMap.get(key), settingsMap.get(key))) {
						equal = false;
						break;
					}
				}

				if (!equal) {
					deleteTemplate(httpClient);
					createTemplate(httpClient);
				}
			} catch (Exception e) {
				throw new NodeException(e);
			}
		}

		putAttachmentPipeline(httpClient);

		for (SearchIndex index : toPrepare) {
			Pair<SearchIndex, Set<String>> response = prepare(httpClient, index, drop);
			if (response != null) {
				result.put(response.getKey(), response.getValue());
			}
		}

		return result;
	}

	/**
	 * Prepare the index for using.
	 * @param httpClient http client
	 * @param index index
	 * @param drop true to drop and recreate it (if it exists)
	 * @return pair of the index and optional attributes, that need to be reindexed, or null if no objects need to be reindexed
	 * @throws NodeException
	 */
	protected final static Pair<SearchIndex, Set<String>> prepare(HttpClient httpClient, SearchIndex index, boolean drop) throws NodeException {
		if (!index.exists(httpClient)) {
			// index does not exist, create it
			index.create(httpClient);
			return Pair.of(index, null);
		} else if (drop) {
			// index exists, but must be dropped first
			index.drop(httpClient);
			index.create(httpClient);
			return Pair.of(index, null);
		} else {
			// index exists, check mapping
			Set<String> diff = index.checkMapping(httpClient);
			if (!diff.isEmpty()) {
				index.updateMapping(httpClient);
				return Pair.of(index, diff);
			}
		}
		return null;
	}

	/**
	 * Get map of required indices
	 * @return index map
	 * @throws NodeException
	 */
	protected static Map<IndexType, Map<String, SearchIndex>> getRequiredIndices() throws NodeException {
		Map<IndexType, Map<String, SearchIndex>> indices = new HashMap<>();

		Set<String> languages = Trx.supply(t -> {
			Set<String> tmpLangs = new HashSet<>();
			tmpLangs.add(null);
			List<Node> nodes = t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS));
			for (Node node : nodes) {
				List<ContentLanguage> nodeLanguages = node.getLanguages();
				for (ContentLanguage language : nodeLanguages) {
					tmpLangs.add(language.getCode());
				}
			}
			return tmpLangs;
		});

		for (IndexType type : IndexType.values()) {
			if (type.isMultiLanguage()) {
				for (String code : languages) {
					if (code == null && !type.isLanguagePerInstance()) {
						continue;
					}
					indices.computeIfAbsent(type, k -> new HashMap<>()).put(code, new SearchIndex(type, code));
				}
			} else {
				indices.computeIfAbsent(type, k -> new HashMap<>()).put(null, new SearchIndex(type));
			}
		}
		return indices;
	}

	/**
	 * Transform the given map by replacing all Number or Boolean instances with a string representation
	 * @param map map to be transformed
	 */
	@SuppressWarnings("unchecked")
	protected static void transformMap(Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Map) {
				transformMap((Map<String, Object>) value);
			} else if (value instanceof Number) {
				entry.setValue(value.toString());
			} else if (value instanceof Boolean) {
				entry.setValue(value.toString());
			}
		}
	}

	/**
	 * Check whether the actual map contains all entries of the expected map. If the actual map contains more entries, they are ignored.
	 * All expected entries are compared recursively (using Objects.deepEquals)
	 * @param expected expected map
	 * @param actual actual map
	 * @return true iff maps are "equal"
	 */
	protected static boolean areEqual(Map<String, Object> expected, Map<String, Object> actual) {
		transformMap(expected);
		transformMap(actual);

		for (String key : expected.keySet()) {
			if (!actual.containsKey(key)) {
				return false;
			}
			if (!Objects.deepEquals(actual.get(key), expected.get(key))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the content for the file shall be indexed
	 * @param file file
	 * @return true, iff the content shall be indexed
	 * @throws NodeException
	 */
	protected static boolean putContentIntoIndex(File file) throws NodeException {
		if (file.isBroken()) {
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Do not index content for broken file %s", file));
			}
			return false;
		}

		String mimeType = file.getFiletype();

		// first check the blacklist
		if (!contentBlacklist.isEmpty()) {
			for (Pattern pattern : contentBlacklist) {
				// if the mimetype matches one of the blacklist patterns, do not index the
				// content
				if (pattern.matcher(mimeType).matches()) {
					if (Indexer.logger.isDebugEnabled()) {
						Indexer.logger.debug(String.format(
								"Do not index content for file with mimetype %s, because it is blacklisted with pattern %s",
								mimeType, pattern.toString()));
					}

					return false;
				}
			}
		}

		// now check the whitelist
		if (!contentWhitelist.isEmpty()) {
			for (Pattern pattern : contentWhitelist) {
				// if the mimetype matches one of the whitelist patterns, do index the content
				if (pattern.matcher(mimeType).matches()) {
					return true;
				}
			}

			// when we have a non-empty whitelist, but the mimetype does not match any of
			// the patterns, do not index
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format(
						"Do not index content for file with mimetype %s, because it is not whitelisted with patterns %s",
						mimeType, contentWhitelist));
			}
			return false;
		} else {
			// if the mimetype was not blacklisted, and we have no whitelist, index the content
			return true;
		}
	}

	/**
	 * Add the object to the indexerqueue
	 * @param object object
	 * @param action indexer action
	 * @return IDs of the new entries in the indexerqueue
	 * @throws NodeException
	 */
	protected static Set<Integer> addToIndexerQueue(IndexableNodeObject<? extends NodeObject> object, IndexerAction action) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		IndexType type = object.getIndexType();
		Set<String> languages = new HashSet<>();
		if (type.isMultiLanguage() && !type.isLanguagePerInstance()) {
			languages.addAll(object.getIndexLanguages());
		} else {
			languages.add(object.getIndexLanguage());
		}
		Set<Integer> ids = new HashSet<>();

		for (String language : languages) {
			ids.addAll(DBUtils.executeInsert(
					"INSERT INTO indexerqueue (timestamp, index_type, index_language, obj_id, action) VALUES (?, ?, ?, ?, ?)",
					new Object[] { (int) (t.getTimestamp() / 1000), type.name(), language, object.getId(),
							action.name() }));
		}

		return ids;
	}

	/**
	 * Remove the entries from the indexerqueue
	 * @param ids entry IDs
	 * @throws NodeException
	 */
	protected static void removeFromIndexerQueue(Set<Integer> ids) throws NodeException {
		if (!ids.isEmpty()) {
			DBUtils.update("DELETE FROM indexerqueue WHERE id IN (" + StringUtils.repeat("?", ids.size(), ",") + ")", ids.toArray());
		}
	}

	/**
	 * Replace all entries from the indexerqueue, which were marked
	 */
	protected static void replayIndexerQueue() {
		if (!isFeature() || service == null) {
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Replaying left-over indexing tasks");
		}

		service.execute(() -> {
			try {
				List<ReplayTask> replayTasks = null;
				while (!(replayTasks = Trx.supply(
						() -> DBUtils.select("SELECT * FROM indexerqueue WHERE replay = ? ORDER BY id ASC LIMIT 10",
								ps -> ps.setInt(1, 1), rs -> {
									List<ReplayTask> list = new ArrayList<>();
									while (rs.next()) {
										list.add(new ReplayTask().setId(rs.getInt("id"))
												.setIndexType(IndexType.valueOf(rs.getString("index_type")))
												.setIndexLanguage(rs.getString("index_language"))
												.setObjId(rs.getInt("obj_id"))
												.setAction(IndexerAction.valueOf(rs.getString("action"))));
									}
									return list;
								}))).isEmpty()) {
					for (ReplayTask task : replayTasks) {
						SearchIndex index = getIndex(task.getIndexType(), task.getIndexLanguage());
						if (index != null) {
							switch (task.getAction()) {
								case delete:
									index.delete(task.getObjId());
									break;
								case put:
									IndexableNodeObject<? extends NodeObject> toPut = task.getIndexType().load(task.getObjId());
									if (toPut != null) {
										index.put(toPut);
									}
									break;
								default:
									break;
							}
						}

						Trx.operate(() -> removeFromIndexerQueue(Collections.singleton(task.getId())));
					}
				}
			} catch (NodeException e) {
				logger.error("Error while replaying left-over indexing tasks", e);
			}
		});
	}
}
