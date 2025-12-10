package com.gentics.contentnode.object.search;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.contentnode.object.*;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.search.IndexModel;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.jayway.jsonpath.JsonPath;

import io.reactivex.Flowable;

/**
 * SearchIndex definition
 */
public class SearchIndex {
	/**
	 * Maximum time (in ms), the object count is valid
	 */
	public final static long OBJECT_COUNT_VALIDATION_TIMEOUT = Duration.ofMinutes(1).toMillis();

	/**
	 * Function that transforms the search index into the rest model
	 */
	public final static Function<SearchIndex, IndexModel> TRANSFORM2REST = searchIndex -> {
		IndexModel model = new IndexModel();
		HttpClient httpClient = Indexer.getClient();
		model.setName(searchIndex.name);
		boolean found = searchIndex.exists(httpClient);
		model.setFound(found);
		if (found) {
			model.setMappingValid(searchIndex.checkMapping(httpClient).isEmpty());
			model.setSettingsValid(searchIndex.checkSettings(httpClient));
			model.setIndexed(searchIndex.getDocCount());
		} else {
			model.setMappingValid(false);
			model.setSettingsValid(false);
		}
		model.setObjects(searchIndex.getObjectCount());
		model.setQueued(searchIndex.getQueued());
		model.setPending(searchIndex.getPendingCount());
		return model;
	};

	/**
	 * Elasticsearch URL to the index
	 */
	protected String url;

	/**
	 * Type of the indexed objects
	 */
	protected IndexType type;

	/**
	 * Optional language
	 */
	protected String language;

	/**
	 * Index name
	 */
	protected String name;

	/**
	 * Mapping info
	 */
	protected JSONObject mapping;

	/**
	 * Optional index settings
	 */
	protected JSONObject settings;

	/**
	 * Tagmap entries
	 */
	protected Set<TagmapEntryRenderer> tagmapEntries;

	/**
	 * Number of documents in the index
	 */
	protected int docCount;

	/**
	 * Total number of objects eligible for this index
	 */
	protected int objectCount;

	/**
	 * Number of pending 'put' operations for this index
	 */
	protected int pendingCount;

	/**
	 * AtomicLong to check, whether the current object count must be updated.
	 * The long value is the timestamp (in ms), when the object count was considered valid the last time
	 */
	protected AtomicLong objectCountValid = new AtomicLong(0);

	/**
	 * Currently queued documents
	 */
	protected AtomicInteger queued = new AtomicInteger();

	/**
	 * Check whether the given attribute should be rendered into the JSON
	 * @param attribute attribute to query
	 * @param attributes set of attribute to index. If null, every attribute will be accepted
	 * @return true iff the attribute should be indexed
	 */
	protected static boolean doIndex(String attribute, Set<String> attributes) {
		if (attributes == null) {
			return true;
		} else {
			return attributes.contains(attribute);
		}
	}

	/**
	 * Optionally add the value supplied by the supplier to the json object
	 * @param json json
	 * @param name attribute name
	 * @param attributes set of indexed attributes
	 * @param supplier supplier
	 * @throws JSONException
	 * @throws NodeException
	 */
	protected static void add(JSONObject json, String name, Set<String> attributes, Supplier<Object> supplier) throws JSONException, NodeException {
		if (doIndex(name, attributes)) {
			json.put(name, supplier.supply());
		}
	}

	/**
	 * Optionally add the value supplied by the supplier to the json object
	 * @param json json
	 * @param name attribute name
	 * @param attributes set of indexed attributes
	 * @param supplier supplier
	 * @throws JSONException
	 * @throws NodeException
	 */
	protected static void addPositiveInt(JSONObject json, String name, Set<String> attributes, Supplier<Integer> supplier) throws JSONException, NodeException {
		if (doIndex(name, attributes)) {
			Integer value = supplier.supply();
			if (value != null && value.intValue() > 0) {
				json.put(name, value);
			} else {
				json.put(name, JSONObject.NULL);
			}
		}
	}

	/**
	 * Check whether the object contains the field "content".
	 * The object may be the update object, which contains the document as "doc", which will also be checked
	 * @param object object to check
	 * @return true, iff it contains "content"
	 */
	protected static boolean containsContent(JSONObject object) {
		if (object.has("content")) {
			return true;
		} else if (object.has("doc")) {
			try {
				return containsContent(object.getJSONObject("doc"));
			} catch (JSONException e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Create instance without language
	 * @param type type
	 * @throws NodeException
	 */
	public SearchIndex(IndexType type) throws NodeException {
		this(type, null);
	}

	/**
	 * Create instance
	 * @param type type
	 * @param language language
	 * @throws NodeException
	 */
	public SearchIndex(IndexType type, String language) throws NodeException {
		this.type = type;
		this.language = language;
		this.name = Indexer.getName(this.type, this.language);
		this.url = Indexer.getIndexUrl(this.name);
		this.mapping = Indexer.mappings.get(this.type);
		this.tagmapEntries = Indexer.entries.get(this.type);
		this.settings = Indexer.getIndexSettings(this.type, this.language);
	}

	/**
	 * Get the index name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get document count
	 * @return document count
	 */
	public int getDocCount() {
		return docCount;
	}

	/**
	 * Set document count
	 * @param docCount document count
	 */
	public void setDocCount(int docCount) {
		this.docCount = docCount;
	}

	/**
	 * Get object count
	 * @return object count
	 * @throws NodeException
	 */
	public int getObjectCount() throws NodeException {
		if ((System.currentTimeMillis() - objectCountValid.get()) > OBJECT_COUNT_VALIDATION_TIMEOUT) {
			updateObjectCount();
		}
		return objectCount;
	}

	/**
	 * Get number of queued documents
	 * @return queued documents
	 */
	public int getQueued() {
		return queued.get();
	}

	/**
	 * Set the count of pending indexer actions
	 * @param pendingCount count
	 */
	public void setPendingCount(int pendingCount) {
		this.pendingCount = pendingCount;
	}

	/**
	 * Get the count of pending indexer actions
	 * @return count
	 */
	public int getPendingCount() {
		return pendingCount;
	}

	/**
	 * Check existence of index
	 * @param httpClient http client
	 * @return true if index exists, false if not
	 * @throws NodeException in case of errors
	 */
	public boolean exists(HttpClient httpClient) throws NodeException {
		try {
			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info(String.format("Checking Index %s", this.url));
			}
			HeadMethod checkIndexMethod = new HeadMethod(this.url);
			int indexResponseCode = httpClient.executeMethod(checkIndexMethod);

			switch (indexResponseCode) {
			case 200:
				if (Indexer.logger.isInfoEnabled()) {
					Indexer.logger.info("Index found");
				}
				return true;
			case 404:
				if (Indexer.logger.isInfoEnabled()) {
					Indexer.logger.info("Index not found");
				}
				return false;
			default:
				throw new NodeException(
						String.format("Request to check index %s returned http code %d, body %s", name, indexResponseCode, checkIndexMethod.getResponseBodyAsString()));
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Use the http client to create the index
	 * @param httpClient http client
	 * @throws NodeException
	 */
	public void create(HttpClient httpClient) throws NodeException {
		try {
			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info(String.format("Index %s does not exist, trying to create it.", name));
			}
			PutMethod createIndexMethod = new PutMethod(url);
			JSONObject indexSettings = new JSONObject();
			indexSettings.put("mappings", mapping);

			if (settings != null) {
				indexSettings.put("settings", settings);
			}

			createIndexMethod.setRequestEntity(new StringRequestEntity(indexSettings.toString(), "application/json", "UTF-8"));
			int createResponseCode = httpClient.executeMethod(createIndexMethod);
			if (createResponseCode >= 400) {
				throw new NodeException(String.format("Request to create index %s returned http code %d, body %s", name, createResponseCode,
						createIndexMethod.getResponseBodyAsString()));
			}

			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info("Created index");
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Drop the index
	 * @param httpClient http client
	 * @throws NodeException
	 */
	public void drop(HttpClient httpClient) throws NodeException {
		try {
			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info(String.format("Dropping index %s.", name));
			}
			DeleteMethod dropIndexMethod = new DeleteMethod(url);

			int dropResponseCode = httpClient.executeMethod(dropIndexMethod);
			if (dropResponseCode >= 400) {
				throw new NodeException(String.format("Request to drop index %s returned http code %d, body %s", name, dropResponseCode,
						dropIndexMethod.getResponseBodyAsString()));
			}

			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info("Dropped index");
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Check index settings (against configured settings)
	 * @param httpClient http client
	 * @return true iff index settings match the configured settings, false if not
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public boolean checkSettings(HttpClient httpClient) throws NodeException {
		try {
			if (settings == null) {
				return true;
			}
			ObjectMapper mapper = new ObjectMapper();
			String settingsUrl = String.format("%s/_settings", url);
			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info(String.format("Checking Settings %s", settingsUrl));
			}
			GetMethod getSettingsMethod = new GetMethod(settingsUrl);
			int settingsResponseCode = httpClient.executeMethod(getSettingsMethod);

			if (settingsResponseCode != 200) {
				throw new NodeException(String.format("Request to get settings of index %s returned http code %d, body %s", name, settingsResponseCode,
						getSettingsMethod.getResponseBodyAsString()));
			}

			JSONObject response = new JSONObject(getSettingsMethod.getResponseBodyAsString());
			JSONObject existing = response.getJSONObject(name).getJSONObject("settings");
			Map<String, Object> existingMap = mapper.readValue(existing.getJSONObject("index").toString(), Map.class);

			Map<String, Object> settingsMap = null;
			if (Indexer.templateDefinition != null && Indexer.templateDefinition.has("settings") && Indexer.templateDefinition.getJSONObject("settings").has("index")) {
				settingsMap = mapper.readValue(Indexer.templateDefinition.getJSONObject("settings").getJSONObject("index").toString(), Map.class);
				settingsMap = MiscUtils.merge(settingsMap, mapper.readValue(settings.toString(), Map.class));
			} else {
				settingsMap = mapper.readValue(settings.toString(), Map.class);
			}

			return Indexer.areEqual(settingsMap, existingMap);
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Check existent index mapping
	 * @param httpClient http client
	 * @return diff as set of missing attributes
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public Set<String> checkMapping(HttpClient httpClient) throws NodeException {
		try {
			String mappingUrl = String.format("%s/_mapping", url);
			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info(String.format("Checking Mapping %s", mappingUrl));
			}
			GetMethod getMappingMethod = new GetMethod(mappingUrl);
			int mappingResponseCode = httpClient.executeMethod(getMappingMethod);

			if (mappingResponseCode != 200) {
				throw new NodeException(String.format("Request to get mapping of index %s returned http code %d, body %s", name, mappingResponseCode,
						getMappingMethod.getResponseBodyAsString()));
			}

			JSONObject response = new JSONObject(getMappingMethod.getResponseBodyAsString());
			JSONObject existing = response.getJSONObject(name).getJSONObject("mappings").getJSONObject("properties");

			Set<String> diff = new HashSet<>(Arrays.asList(JSONObject.getNames(mapping.getJSONObject("properties"))));
			String[] names = JSONObject.getNames(existing);
			if (names != null) {
				ObjectMapper mapper = new ObjectMapper();
				for (String name : names) {
					if (diff.contains(name)) {
						Map<String, Object> expectedFieldMapping = mapper.readValue(
								mapping.getJSONObject("properties").getJSONObject(name).toString(), Map.class);
						Map<String, Object> existingFieldMapping = mapper
								.readValue(existing.getJSONObject(name).toString(), Map.class);
						if (Indexer.areEqual(expectedFieldMapping, existingFieldMapping)) {
							diff.remove(name);
						}
					}
				}
			}
			return diff;
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Update index mapping
	 * @param httpClient http client
	 * @throws NodeException
	 */
	public void updateMapping(HttpClient httpClient) throws NodeException {
		try {
			String mappingUrl = String.format("%s/_mapping", url);
			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info(String.format("Updating Mapping %s", mappingUrl));
			}
			PutMethod updateMappingMethod = new PutMethod(mappingUrl);
			updateMappingMethod.setRequestEntity(new StringRequestEntity(mapping.toString(), "application/json", "UTF-8"));
			int updateResponseCode = httpClient.executeMethod(updateMappingMethod);
			if (updateResponseCode >= 400) {
				throw new NodeException(String.format("Request to update mapping of index %s returned http code %d, body %s", name, updateResponseCode,
						updateMappingMethod.getResponseBodyAsString()));
			}

			if (Indexer.logger.isInfoEnabled()) {
				Indexer.logger.info("Updated mapping");
			}
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Get URL to the object
	 * @param object object
	 * @return URL
	 */
	public String getUrl(IndexableNodeObject<? extends NodeObject> object) {
		return getUrl(object.getId(), false);
	}

	/**
	 * Get URL to the object
	 * @param object object
	 * @param update true for update URL
	 * @return URL
	 */
	public String getUrl(IndexableNodeObject<? extends NodeObject> object, boolean update) {
		return getUrl(object.getId(), update);
	}

	/**
	 * Get URL to the object with given ID
	 * @param id object ID
	 * @param update true for update URL
	 * @return URL
	 */
	public String getUrl(int id, boolean update) {
		return update
			? String.format("%s/_update/%d", url, id)
			: String.format("%s/_doc/%d", url, id);
	}

	/**
	 * Synchronously add the object to the index
	 * @param object object
	 * @throws NodeException
	 */
	public void put(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		objectCountValid.set(0);
		Wastebin wb = object.isDeleted() ? Wastebin.INCLUDE : Wastebin.EXCLUDE;
		try (Trx trx = new Trx(); RenderTypeTrx rtTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW); LangTrx lTrx = new LangTrx("en"); WastebinFilter wbf = wb.set()) {
			// omit root folders
			if (object instanceof Folder) {
				if (((Folder)object).isRoot()) {
					if (Indexer.logger.isDebugEnabled()) {
						Indexer.logger.debug(String.format("Omit indexing of %s, because it is a root folder", object));
					}
					return;
				}
			}

			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Indexing %s", object));
			}
			HttpClient httpClient = Indexer.getClient();

			PutMethod indexObjectMethod = new PutMethod(getUrl(object));
			JSONObject indexJSON = getIndexJSON(object);
			indexObjectMethod.setRequestEntity(new StringRequestEntity(indexJSON.toString(), "application/json", "UTF-8"));

			// use attachment pipeline for files (which are not broken)
			if (object.getIndexType() == IndexType.file && containsContent(indexJSON)) {
				indexObjectMethod.setQueryString(new NameValuePair[] { new NameValuePair("pipeline", Indexer.ATTACHMENT_PIPELINE) });
			}

			int responseCode = httpClient.executeMethod(indexObjectMethod);
			String response = indexObjectMethod.getResponseBodyAsString();

			if (responseCode >= 400) {
				// check for "known" error types, and optionally repeat indexing without the content
				boolean repeatWithoutContent = false;
				try {
					List<String> errorTypes = JsonPath.read(response, "$..type");
					// document content was encrypted
					if (errorTypes.contains("encrypted_document_exception")) {
						repeatWithoutContent = true;
					}
				} catch (Exception e) {
				}

				if (repeatWithoutContent) {
					indexJSON = removeBinaryContent(indexJSON);
					indexObjectMethod = new PutMethod(getUrl(object));
					indexObjectMethod.setRequestEntity(new StringRequestEntity(indexJSON.toString(), "application/json", "UTF-8"));

					responseCode = httpClient.executeMethod(indexObjectMethod);
					response = indexObjectMethod.getResponseBodyAsString();
				}
			}

			if (responseCode >= 400) {
				throw new NodeException(String.format("Indexing of %s returned http status code %d and response body %s", object, responseCode, response));
			}

			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Indexed %s. Response code: %d", object, responseCode));
			}
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Try to update the object (render only the provided attributes). If the object does not yet exist in the index,
	 * it will be indexed completely by using {@link #put(IndexableNodeObject)}.
	 * @param object object
	 * @param attributes set of attributes (must not be null or empty)
	 * @throws NodeException
	 */
	public void update(IndexableNodeObject<? extends NodeObject> object, Set<String> attributes) throws NodeException {
		Wastebin wb = object.isDeleted() ? Wastebin.INCLUDE : Wastebin.EXCLUDE;
		try (Trx trx = new Trx(); RenderTypeTrx rtTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW); LangTrx lTrx = new LangTrx("en"); WastebinFilter wbf = wb.set()) {
			if (ObjectTransformer.isEmpty(attributes)) {
				if (Indexer.logger.isDebugEnabled()) {
					Indexer.logger.debug(String.format("Omit updating of %s, because no attributes given", object));
				}
				return;
			}
			// omit root folders
			if (object instanceof Folder) {
				if (((Folder)object).isRoot()) {
					if (Indexer.logger.isDebugEnabled()) {
						Indexer.logger.debug(String.format("Omit indexing of %s, because it is a root folder", object));
					}
					return;
				}
			}

			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Updating %s (%s)", object, attributes));
			}

			HttpClient httpClient = Indexer.getClient();

			PostMethod updateObjectMethod = new PostMethod(getUrl(object, true));
			JSONObject updateJSON = getUpdateJSON(object, attributes);
			updateObjectMethod.setRequestEntity(new StringRequestEntity(updateJSON.toString(), "application/json", "UTF-8"));

			int responseCode = httpClient.executeMethod(updateObjectMethod);
			String response = updateObjectMethod.getResponseBodyAsString();

			// check whether object was not found
			if (responseCode == 404) {
				// index full object
				PutMethod indexObjectMethod = new PutMethod(getUrl(object));
				JSONObject indexJSON = getIndexJSON(object);
				indexObjectMethod.setRequestEntity(new StringRequestEntity(indexJSON.toString(), "application/json", "UTF-8"));

				// use attachment pipeline for files
				if (object.getIndexType() == IndexType.file && containsContent(indexJSON)) {
					indexObjectMethod.setQueryString(new NameValuePair[] { new NameValuePair("pipeline", Indexer.ATTACHMENT_PIPELINE) });
				}

				responseCode = httpClient.executeMethod(indexObjectMethod);
				response = indexObjectMethod.getResponseBodyAsString();
			}

			if (responseCode >= 400) {
				// check for "known" error types, and optionally repeat indexing without the content
				boolean repeatWithoutContent = false;
				try {
					List<String> errorTypes = JsonPath.read(response, "$..type");
					// document content was encrypted
					if (errorTypes.contains("encrypted_document_exception")) {
						repeatWithoutContent = true;
					}
				} catch (Exception e) {
				}

				if (repeatWithoutContent) {
					updateJSON = removeBinaryContent(updateJSON);
					updateObjectMethod = new PostMethod(getUrl(object, true));
					updateObjectMethod.setRequestEntity(new StringRequestEntity(updateJSON.toString(), "application/json", "UTF-8"));

					responseCode = httpClient.executeMethod(updateObjectMethod);
					response = updateObjectMethod.getResponseBodyAsString();
				}
			}

			if (responseCode >= 400) {
				throw new NodeException(String.format("Indexing of %s returned http status code %d and response body %s", object, responseCode, response));
			}

			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Updated %s. Response code: %d", object, responseCode));
			}
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Synchronously remove the object from the index
	 * @param object object
	 * @throws NodeException
	 */
	public void delete(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		objectCountValid.set(0);
		try (Trx trx = new Trx()) {
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Removing %s", object));
			}
			HttpClient httpClient = Indexer.getClient();

			DeleteMethod removeObjectMethod = new DeleteMethod(getUrl(object));

			int responseCode = httpClient.executeMethod(removeObjectMethod);
			String response = removeObjectMethod.getResponseBodyAsString();

			// 404 (NOT FOUND) is ok, because this means the object was not indexed anyway
			if (responseCode >= 400 && responseCode != 404) {
				throw new NodeException(String.format("Removing of %s from index returned http status code %d and response body %s", object, responseCode, response));
			}
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Removed %s. Response code: %d", object, responseCode));
			}
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Synchronously remove the object with given ID from the index
	 * @param id object ID
	 * @throws NodeException
	 */
	public void delete(int id) throws NodeException {
		objectCountValid.set(0);
		try (Trx trx = new Trx()) {
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Removing object with ID %d", id));
			}
			HttpClient httpClient = Indexer.getClient();

			DeleteMethod removeObjectMethod = new DeleteMethod(getUrl(id, false));

			int responseCode = httpClient.executeMethod(removeObjectMethod);
			String response = removeObjectMethod.getResponseBodyAsString();

			// 404 (NOT FOUND) is ok, because this means the object was not indexed anyway
			if (responseCode >= 400 && responseCode != 404) {
				throw new NodeException(String.format("Removing of object with ID %d from index returned http status code %d and response body %s", id, responseCode, response));
			}
			if (Indexer.logger.isDebugEnabled()) {
				Indexer.logger.debug(String.format("Removed object with ID %d. Response code: %d", id, responseCode));
			}
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Get the JSON to be indexed
	 * @param object object
	 * @return JSON
	 * @throws NodeException
	 */
	protected JSONObject getIndexJSON(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		return getIndexJSON(object.getWrapped(), null);
	}

	/**
	 * Get the JSON to be indexed
	 * @param object object
	 * @param attributes optional set of attributes to restrict returned document
	 * @return JSON
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected JSONObject getIndexJSON(StackResolvableNodeObject object, Set<String> attributes) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		JSONObject json = new JSONObject();
		RenderType renderType = t.getRenderType();
		try {
			if (object instanceof Page) {
				json.put("_type", "page");
			} else if (object instanceof Folder) {
				json.put("_type", "folder");
			} else if (object instanceof File) {
				json.put("_type", ((File) object).isImage() ? "image" : "file");
			} else if (object instanceof Form) {
				json.put("_type", "form");
			}

			for (TagmapEntryRenderer entry : tagmapEntries) {
				if (!doIndex(entry.getMapname(), attributes)) {
					continue;
				}
				if ("content".equals(entry.getTagname()) && object instanceof Page) {
					Page page = (Page) object;

					List<String> content = Flowable.fromIterable(page.getContentTags().values()).flatMap(tag -> Flowable.fromIterable(tag.getValues())).filter(value -> {
						PartType partType = value.getPartType();
						return (partType instanceof TextPartType);
					}).map(Value::getValueText).filter(value -> org.apache.commons.lang3.StringUtils.isNotBlank(value)).toList().blockingGet();

					json.put(entry.getMapname(), content);
				} else if ("binarycontent".equals(entry.getTagname()) && object instanceof File) {
					File file = (File) object;
					if (Indexer.putContentIntoIndex(file)) {
						try (InputStream in = new Base64InputStream(file.getFileStream(), true, 0, null)) {
							json.put(entry.getMapname(), StringUtils.readStream(in));
						}
					}
				} else if ("content".equals(entry.getTagname()) && object instanceof Form) {
					Form form = (Form) object;
					json.put(entry.getMapname(), form.getIndexableContent(language));
				} else {
					renderType.push(object);
					try {
						Object value = entry.getRenderedTransformedValue(renderType, new RenderResult(), CnMapPublisher.LINKTRANSFORMER);
						if (value == null) {
							json.put(entry.getMapname(), JSONObject.NULL);
						} else {
							json.put(entry.getMapname(), value);
						}
					} finally {
						renderType.pop();
					}
				}
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING) && object instanceof LocalizableNodeObject && doIndex("nodeId", attributes)) {
				Collection<Integer> nodeIds = MultichannellingFactory.getNodeIds((LocalizableNodeObject<NodeObject>)object, false);
				json.put("nodeId", nodeIds);
			}

			// for images/files, set the nodes in which the file is online
			if (object instanceof File && doIndex("online", attributes)) {
				File file = (File) object;
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
					List<Node> nodes = t.getObjects(Node.class, MultichannellingFactory.getNodeIds((LocalizableNodeObject<NodeObject>) object, false));
					List<Integer> ids = new ArrayList<>();
					for (Node node : nodes) {
						if (FileOnlineStatus.isOnline(file, node)) {
							ids.add(node.getId());
						}
					}
					json.put("online", ids);
				} else {
					if (FileOnlineStatus.isOnline(file)) {
						json.put("online", file.getOwningNode().getId());
					}
				}
			}

			// for pages, add flags for "online", "modified", "planned" and "queued",
			// set system and custom creation/edit dates and set timemanagement data
			if (object instanceof Page) {
				Page page = (Page) object;
				add(json, "online", attributes, () -> {
					if (page.isOnline()) {
						if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
							return MultichannellingFactory.getNodeIds(page, false);
						} else {
							return page.getNode().getId();
						}
					} else {
						return Collections.emptyList();
					}
				});
				add(json, "modified", attributes, () -> page.isModified());
				add(json, "planned", attributes, () -> page.isPlanned());
				add(json, "queued", attributes, () -> page.isQueued());
				addPositiveInt(json, "publishAt", attributes, () -> page.getTimePub().getIntTimestamp());
				addPositiveInt(json, "offlineAt", attributes, () -> page.getTimeOff().getIntTimestamp());
				addPositiveInt(json, "queuedPublishAt", attributes, () -> page.getTimePubQueue().getIntTimestamp());
				addPositiveInt(json, "queuedOfflineAt", attributes, () -> page.getTimeOffQueue().getIntTimestamp());
				addPositiveInt(json, "systemCreationDate", attributes, () -> page.getCDate().getIntTimestamp());
				addPositiveInt(json, "customCreationDate", attributes, () -> page.getCustomCDate().getIntTimestamp());
				addPositiveInt(json, "systemEditDate", attributes, () -> page.getEDate().getIntTimestamp());
				addPositiveInt(json, "customEditDate", attributes, () -> page.getCustomEDate().getIntTimestamp());
			}

			if (object instanceof Form) {
				Form form = (Form) object;
				Node node = form.getFolder().getOwningNode();
				int nodeId = node.getId();

				// get all node IDs the form's folder (including all its localized copies) is visible in
				ChannelTreeSegment folderVisibility = new ChannelTreeSegment(form.getFolder(), true);
				Collection<Integer> nodeIds = folderVisibility.getAllNodes().stream().map(Node::getId).collect(Collectors.toList());

				add(json, "online", attributes, () -> {
					if (form.isOnline()) {
						if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
							return nodeIds;
						} else {
							return nodeId;
						}
					} else {
						return Collections.emptyList();
					}
				});
				add(json, "nodeId", attributes, () -> {
					if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
						return nodeIds;
					} else {
						return nodeId;
					}
				});
				add(json, "modified", attributes, () -> form.isModified());
				add(json, "planned", attributes, () -> form.isPlanned());
				addPositiveInt(json, "publishAt", attributes, () -> form.getTimePub().getIntTimestamp());
				addPositiveInt(json, "offlineAt", attributes, () -> form.getTimeOff().getIntTimestamp());
			}

			// nice URLs and alternate URLs are all indexed as "niceUrl"
			if (object instanceof NodeObjectWithAlternateUrls
					&& (doIndex("niceUrl", attributes) || doIndex("alternateUrls", attributes))) {
				NodeObjectWithAlternateUrls niceUrlContainer = (NodeObjectWithAlternateUrls) object;
				json.put("niceUrl", niceUrlContainer.getNiceAndAlternateUrls());
			}

			// for every object, add "deleted" flag
			add(json, "deleted", attributes, () -> object.isDeleted());

			// add the group IDs with permission to view the object
			if (object instanceof Folder) {
				add(json, "groupId", attributes, () -> PermissionStore.getInstance()
						.getGroupsWithPerm(Folder.TYPE_FOLDER, object.getId(), PermType.read, -1, -1));
			} else {
				PermType perm = object instanceof Form ? PermType.viewform : PermType.readitems;
				NodeObject folder = object.getParentObject();
				if (folder != null) {
					int checkLangId = -1;
					if (object instanceof Page) {
						checkLangId = ObjectTransformer.getInt(((Page) object).getLanguageId(), -1);
					}
					int finalCheckLangId = checkLangId;
					add(json, "groupId", attributes,
							() -> PermissionStore.getInstance().getGroupsWithPerm(Folder.TYPE_FOLDER, folder.getId(),
									perm, object.getTType(), finalCheckLangId));
				}
			}
		} catch (JSONException | IOException e) {
			throw new NodeException(e);
		}

		return json;
	}

	/**
	 * Get the JSON body for updating the given attributes
	 * @param object object
	 * @param attributes attributes
	 * @return json body
	 * @throws NodeException
	 */
	protected JSONObject getUpdateJSON(IndexableNodeObject<? extends NodeObject> object, Set<String> attributes) throws NodeException {
		try {
			JSONObject json = new JSONObject();
			json.put("doc", getIndexJSON(object.getWrapped(), attributes));
			return json;
		} catch (JSONException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Remove the binary content from the object
	 * @param object object
	 * @return modified object
	 */
	protected JSONObject removeBinaryContent(JSONObject object) {
		if (object.has("doc")) {
			try {
				removeBinaryContent(object.getJSONObject("doc"));
			} catch (JSONException ignored) {
			}
		} else {
			for (TagmapEntryRenderer entry : tagmapEntries) {
				if ("binarycontent".equals(entry.getTagname())) {
					object.remove(entry.getMapname());
				}
			}
		}
		return object;
	}

	/**
	 * Get IDs of currently indexed objects
	 * @return set of IDs
	 * @throws NodeException
	 */
	protected Flowable<List<Integer>> getIndexedObjects() throws NodeException {
		Flowable<Integer> idFlowable = Flowable.generate(() -> Pair.of(new AtomicReference<String>(), new LinkedList<Integer>()), (state, emitter) -> {
			AtomicReference<String> scrollId = state.getLeft();
			Deque<Integer> deque = state.getRight();
			HttpClient httpClient = Indexer.getClient();
			if (deque.isEmpty()) {
				PostMethod searchMethod = null;
				JSONObject search = new JSONObject();

				if (StringUtils.isEmpty(scrollId.get())) {
					// this is the first request
					searchMethod = new PostMethod(String.format("%s/_search", url));
					searchMethod.setQueryString(new NameValuePair[] { new NameValuePair("scroll", "1m") });
					search
						.put("_source", "_type")
						.put("size", Indexer.OBSOLETION_BATCH_SIZE)
						.put("query", new JSONObject().put("term", new JSONObject().put("_type", type.name())));
				} else {
					// this is a subsequent scroll request
					searchMethod = new PostMethod("/_search/scroll");
					search
						.put("scroll", "1m")
						.put("scroll_id", scrollId.get());
				}
				searchMethod.setRequestEntity(new StringRequestEntity(search.toString(), "application/json", "utf8"));

				int responseCode = httpClient.executeMethod(searchMethod);
				if (responseCode != 200) {
					throw new NodeException(String.format("Getting indexed objects of type %s returned status code %d. Message was %s", type.name(), responseCode,
							searchMethod.getResponseBodyAsString()));
				}

				JSONObject response = new JSONObject(searchMethod.getResponseBodyAsString());
				if (response.has("hits")) {
					JSONObject hitsObject = response.getJSONObject("hits");
					if (hitsObject.has("hits")) {
						JSONArray hitsArray = hitsObject.getJSONArray("hits");
						for (int i = 0; i < hitsArray.length(); i++) {
							JSONObject hit = hitsArray.getJSONObject(i);
							deque.add(ObjectTransformer.getInt(hit.getString("_id"), 0));
						}
					}
				}

				// store scroll ID
				if (response.has("_scroll_id")) {
					scrollId.set(response.getString("_scroll_id"));
				}
			}

			if (deque.isEmpty()) {
				emitter.onComplete();
			} else {
				emitter.onNext(deque.poll());
			}
		});

		return idFlowable.buffer(Indexer.BATCH_SIZE);
	}

	/**
	 * Remove objects of given type from index, that do not exist in the CMS any more
	 * @throws NodeException
	 */
	protected void clean() throws NodeException {
		getIndexedObjects().blockingForEach(this::clean);
	}

	/**
	 * Remove objects of given type with given ids from index, if they do not exist in the CMS any more
	 * @param ids id batch
	 * @throws NodeException
	 */
	protected void clean(Collection<Integer> ids) throws NodeException {
		Set<Integer> existingIds = Trx.supply(() -> {
			Stream<? extends IndexableNodeObject<? extends NodeObject>> stream = type.getObjects(ids).stream();

			// add filter to remove objects of wrong language
			if (type.isMultiLanguage()) {
				stream = stream.filter(object -> {
					try {
						return Objects.equals(object.getIndexLanguage(), language);
					} catch (NodeException e) {
						throw new RuntimeException(e);
					}
				});
			}

			return stream.map(NodeObject::getId).collect(Collectors.toSet());
		});
		ids.removeAll(existingIds);

		try {
			if (!ids.isEmpty()) {
				HttpClient httpClient = Indexer.getClient();
				for (Integer id : ids) {
					DeleteMethod removeObjectMethod = new DeleteMethod(getUrl(id, false));
					int responseCode = httpClient.executeMethod(removeObjectMethod);

					if (responseCode >= 400 && responseCode != 404) {
						Indexer.logger.warn(String.format("Removed %s %d. Response code: %d", type, id, responseCode));
					} else {
						if (Indexer.logger.isDebugEnabled()) {
							Indexer.logger.debug(String.format("Removed %s %d. Response code: %d", type, id, responseCode));
						}
					}
				}
			}
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Reindex objects
	 * @param attributes optional attributes to update (if empty or null, reindex whole object)
	 */
	protected void reIndex(Set<String> attributes) throws NodeException {
		reIndex(attributes, () -> type.ids().apply(language));
	}

	/**
	 * Reindex objects in the given folder
	 * @param attributes optional attributes to update (if empty or null, reindex whole object)
	 * @param folderId folder ID
	 */
	protected void reIndexSubObjects(Set<String> attributes, int folderId) throws NodeException {
		reIndex(attributes, () -> type.childIds().apply(language, folderId));
	}

	/**
	 * Reindex objects from the given node
	 * @param attributes optional attributes to update (if empty or null, reindex while object)
	 * @param nodeId node ID
	 * @throws NodeException
	 */
	protected void reIndexObjectsInNode(Set<String> attributes, int nodeId) throws NodeException {
		reIndex(attributes, () -> type.inNodeIds().apply(language, nodeId));
	}

	/**
	 * Reindex objects where the IDs are supplied by the given supplier
	 * @param attributes optional attributes to update (if empty or null, reindex whole object)
	 * @param idSupplier id supplier
	 * @throws NodeException
	 */
	protected void reIndex(Set<String> attributes, Supplier<Set<Integer>> idSupplier) throws NodeException {
		boolean full = ObjectTransformer.isEmpty(attributes);
		try {
			Set<Integer> idsSet = idSupplier.supply();
			queued.addAndGet(idsSet.size());
			if (full) {
				clean();
			}
			Flowable.fromIterable(idsSet).buffer(Indexer.BATCH_SIZE).map(ids -> Trx.supply(t -> {
				try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
					return type.getObjects(ids);
				}
			})).flatMap(list -> Flowable.fromIterable(list)).blockingForEach(object -> {
				try {
					if (full) {
						put(object);
					} else {
						update(object, attributes);
					}
				} catch (Throwable t) {
					Indexer.logger.error(String.format("Error while reindexing %s", object), t);
					throw t;
				} finally {
					queued.decrementAndGet();
				}
			});
		} finally {
			queued.set(0);
		}
	}

	/**
	 * Update the object count
	 * @throws NodeException
	 */
	protected void updateObjectCount() throws NodeException {
		objectCountValid.set(System.currentTimeMillis());
		try {
			objectCount = type.count().apply(language);
		} catch (NodeException e) {
			objectCountValid.set(0);
			throw e;
		}
	}

	@Override
	public String toString() {
		return String.format("SearchIndex %s", name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SearchIndex) {
			SearchIndex other = (SearchIndex) obj;
			return StringUtils.isEqual(name, other.name);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
