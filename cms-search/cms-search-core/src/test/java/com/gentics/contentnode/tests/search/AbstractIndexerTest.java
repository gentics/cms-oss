package com.gentics.contentnode.tests.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.object.*;
import com.gentics.contentnode.object.search.IndexableNodeObject;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.object.search.SearchIndex;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.etc.StringUtils;
import com.jayway.jsonpath.JsonPath;
import io.reactivex.Flowable;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.ClassRule;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Abstract base class for Indexer Tests
 */
public abstract class AbstractIndexerTest {

	@ClassRule
	public static ElasticsearchContext elasticsearch = new ElasticsearchContext(ElasticsearchContext.VERSION_ES9);
	public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	/**
	 * Create an ArrayNode containing the integers from the collection
	 * @param dataCollection collection of integers
	 * @return ArrayNode instance
	 */
	public static ArrayNode createIntArray(Collection<Integer> dataCollection) {
		ArrayNode arrayNode = mapper.createArrayNode();
		for (int i : dataCollection) {
			arrayNode.add(i);
		}
		return arrayNode;
	}

	/**
	 * Create an ArrayNode containing the strings from the collection
	 * @param dataCollection collection of strings
	 * @return ArrayNode instance
	 */
	public static ArrayNode createStringArray(Collection<String> dataCollection) {
		ArrayNode arrayNode = mapper.createArrayNode();
		for (String S : dataCollection) {
			arrayNode.add(S);
		}
		return arrayNode;
	}

	/**
	 * Create the expected PUT body for indexing the given object for the nodes
	 * 
	 * @param object object
	 * @param nodes  optional array of nodes
	 * @return PUT body as JsonNode
	 * @throws NodeException
	 */
	protected JsonNode body(NodeObject object, Node... nodes) throws NodeException {
		try (Trx trx = new Trx(); WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
			object = object.reload();
			trx.success();
		}

		if (object instanceof Page) {
			return pageBody((Page) object, nodes);
		} else if (object instanceof Folder) {
			return folderBody((Folder) object, nodes);
		} else if (object instanceof File) {
			return fileBody((File) object, nodes);
		} else if (object instanceof Form) {
			return formBody((Form) object, nodes);
		} else {
			fail(String.format("Unexpected object %s", object));
			return null;
		}
	}

	/**
	 * Create the expected PUT body for indexing the given page for the nodes
	 * 
	 * @param page  page
	 * @param nodes optional array of nodes
	 * @return PUT body as JsonNode
	 * @throws NodeException
	 */
	protected JsonNode pageBody(Page page, Node... nodes) throws NodeException {
		return Trx.supply(() -> {
			ObjectNode json = mapper.createObjectNode();
			json.put("_type", "page");
			json.put("id", page.getId());
			json.set("nodeId", createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
			json.put("folderId", page.getFolderId());
			json.put("name", page.getName());
			json.put("filename", page.getFilename());
			json.put("description", page.getDescription());
			json.set("content", mapper.createArrayNode());
			json.put("created", page.getCustomOrDefaultCDate().getIntTimestamp());
			json.put("creatorId", page.getCreator().getId());
			json.put("edited", page.getCustomOrDefaultEDate().getIntTimestamp());
			json.put("editorId", page.getEditor().getId());
			json.put("published", page.getPDate().getIntTimestamp());
			SystemUser publisher = page.getPublisher();
			if (publisher != null) {
				json.put("publisherId", publisher.getId());
			}
			json.put("templateId", page.getTemplate().getId());
			if (page.isOnline()) {
				json.set("online", createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
			} else {
				json.set("online", mapper.createArrayNode());
			}
			json.put("queued", page.isQueued());
			json.put("modified", page.isModified());
			json.put("planned", page.isPlanned());
			setPositiveInt(json, "publishAt", page.getTimePub().getIntTimestamp());
			setPositiveInt(json, "offlineAt", page.getTimeOff().getIntTimestamp());
			setPositiveInt(json, "queuedPublishAt", page.getTimePubQueue().getIntTimestamp());
			setPositiveInt(json, "queuedOfflineAt", page.getTimeOffQueue().getIntTimestamp());
			setPositiveInt(json, "systemCreationDate", page.getCDate().getIntTimestamp());
			setPositiveInt(json, "customCreationDate", page.getCustomCDate().getIntTimestamp());
			setPositiveInt(json, "systemEditDate", page.getEDate().getIntTimestamp());
			setPositiveInt(json, "customEditDate", page.getCustomEDate().getIntTimestamp());
			json.put("deleted", page.isDeleted());
			json.set("niceUrl", createStringArray(page.getNiceAndAlternateUrls()));
			ArrayNode groupIdsArray = mapper.createArrayNode();
			for (Integer groupId : PermissionStore.getInstance().getGroupsWithPerm(Folder.TYPE_FOLDER,
					page.getFolderId(), PermType.readitems, Page.TYPE_PAGE, ObjectTransformer.getInt(page.getLanguageId(), -1))) {
				groupIdsArray.add(groupId);
			}
			json.set("groupId", groupIdsArray);
			return json;
		});
	}

	/**
	 * Create the expected PUT body for indexing the given folder for the nodes
	 * 
	 * @param folder folder
	 * @param nodes  optional array of nodes
	 * @return PUT body as JsonNode
	 * @throws NodeException
	 */
	protected JsonNode folderBody(Folder folder, Node... nodes) throws NodeException {
		return Trx.supply(() -> {
			ObjectNode json = mapper.createObjectNode();
			json.put("_type", "folder");
			json.put("id", folder.getId());
			json.set("nodeId", createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
			json.put("folderId", folder.getMother().getId());
			json.put("name", folder.getName());
			json.put("description", folder.getDescription());
			json.put("created", folder.getCDate().getIntTimestamp());
			json.put("creatorId", folder.getCreator().getId());
			json.put("edited", folder.getEDate().getIntTimestamp());
			json.put("editorId", folder.getEditor().getId());
			json.put("systemCreationDate", folder.getCDate().getIntTimestamp());
			json.put("systemEditDate", folder.getEDate().getIntTimestamp());
			json.put("deleted", folder.isDeleted());
			ArrayNode groupIdsArray = mapper.createArrayNode();
			for (Integer groupId : PermissionStore.getInstance().getGroupsWithPerm(Folder.TYPE_FOLDER,
					folder.getId(), PermType.read, Folder.TYPE_FOLDER, -1)) {
				groupIdsArray.add(groupId);
			}
			json.set("groupId", groupIdsArray);
			return json;
		});
	}

	/**
	 * Create the expected PUT body for indexing the given file for the nodes
	 * 
	 * @param file  file
	 * @param nodes optional array of nodes
	 * @return PUT body as JsonNode
	 * @throws NodeException
	 */
	protected JsonNode fileBody(File file, Node... nodes) throws NodeException {
		return Trx.supply(() -> {
			ObjectNode json = mapper.createObjectNode();
			json.put("_type", file.isImage() ? "image" : "file");
			json.put("id", file.getId());
			json.set("nodeId", createIntArray(Flowable.fromArray(nodes).map(Node::getId).toList().blockingGet()));
			json.set("online", createIntArray(Flowable.fromArray(nodes)
					.filter(n -> FileOnlineStatus.isOnline(file, n)).map(Node::getId).toList().blockingGet()));
			json.put("folderId", file.getFolder().getId());
			json.put("name", file.getName());
			json.put("filename", file.getName());
			json.put("description", file.getDescription());
			if (!file.isImage() && !Objects.equals(file.getFiletype(), "application/zip")) {
                try {
                    json.put("content", StringUtils.readStream(file.getFileStream()));
                } catch (IOException e) {
                    throw new NodeException(e);
                }

				/*
				try (InputStream in = new Base64InputStream(file.getFileStream(), true, 0, null)) {
					json.put("content", StringUtils.readStream(in));
				} catch (IOException e) {
					throw new NodeException(e);
				}
				 */
			}
			json.put("created", file.getCDate().getIntTimestamp());
			json.put("creatorId", file.getCreator().getId());
			json.put("edited", file.getEDate().getIntTimestamp());
			json.put("editorId", file.getEditor().getId());
			json.put("mimetype", file.getFiletype());
			json.put("systemCreationDate", file.getCDate().getIntTimestamp());
			json.put("systemEditDate", file.getEDate().getIntTimestamp());
			json.put("deleted", file.isDeleted());
			json.set("niceUrl", createStringArray(file.getNiceAndAlternateUrls()));
			ArrayNode groupIdsArray = mapper.createArrayNode();
			for (Integer groupId : PermissionStore.getInstance().getGroupsWithPerm(Folder.TYPE_FOLDER,
					file.getFolder().getId(), PermType.readitems, file.getTType(), -1)) {
				groupIdsArray.add(groupId);
			}
			json.set("groupId", groupIdsArray);
			return json;
		});
	}

	/**
	 * Create the expected PUT body for indexing the given form for the nodes
	 * 
	 * @param form  form
	 * @param nodes optional array of nodes
	 * @return PUT body as JsonNode
	 * @throws NodeException
	 */
	protected JsonNode formBody(Form form, Node... nodes) throws NodeException {
		return Trx.supply(() -> {
			Wastebin wb = form.isDeleted() ? Wastebin.INCLUDE : Wastebin.EXCLUDE;

			try (WastebinFilter wbf = wb.set()) {
				ObjectNode json = mapper.createObjectNode();
				json.put("_type", "form");
				json.put("id", form.getId());
				json.set("nodeId", createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
				if (form.isOnline()) {
					json.set("online", createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
				} else {
					json.set("online", mapper.createArrayNode());
				}
				json.put("folderId", form.getFolder().getId());
				json.put("name", form.getName());
				json.put("description", form.getDescription());
				ArrayNode content = mapper.createArrayNode();
				for (String string : form.getIndexableContent("de")) {
					content.add(string);
				}
				json.put("modified", form.isModified());
				json.put("planned", form.isPlanned());
				setPositiveInt(json, "publishAt", form.getTimePub().getIntTimestamp());
				setPositiveInt(json, "offlineAt", form.getTimeOff().getIntTimestamp());
				json.set("content", content);
				json.put("created", form.getCDate().getIntTimestamp());
				json.put("creatorId", form.getCreator().getId());
				json.put("edited", form.getEDate().getIntTimestamp());
				json.put("editorId", form.getEditor().getId());
				json.put("published", form.getPDate().getIntTimestamp());
				SystemUser publisher = form.getPublisher();
				if (publisher != null) {
					json.put("publisherId", publisher.getId());
				}
				json.put("systemCreationDate", form.getCDate().getIntTimestamp());
				json.put("systemEditDate", form.getEDate().getIntTimestamp());
				json.put("deleted", form.isDeleted());
				ArrayNode groupIdsArray = mapper.createArrayNode();
				for (Integer groupId : PermissionStore.getInstance().getGroupsWithPerm(Folder.TYPE_FOLDER,
						form.getFolder().getId(), PermType.viewform, Form.TYPE_FORM, -1)) {
					groupIdsArray.add(groupId);
				}
				json.set("groupId", groupIdsArray);
				return json;
			}
		});
	}

	/**
	 * Create the expected index path for the object
	 * 
	 * @param object object
	 * @return index path
	 */
	protected String path(NodeObject object) {
		if (object instanceof Page) {
			return String.format("genticscms_page/page/%d", object.getId());
		} else if (object instanceof Folder) {
			return String.format("genticscms_folder/folder/%d", object.getId());
		} else if (object instanceof File) {
			if (((File) object).isImage()) {
				return String.format("genticscms_image/image/%d", object.getId());
			} else {
				return String.format("genticscms_file/file/%d", object.getId());
			}
		} else if (object instanceof Form) {
			return String.format("genticscms_form_de/form/%d", object.getId());
		} else {
			fail(String.format("Unexpected object %s", object));
			return null;
		}
	}

	/**
	 * Create the expected index path for the object
	 * 
	 * @param object object
	 * @return index path
	 */
	protected String index(NodeObject object) {
		if (object instanceof Page) {
			return "genticscms_page";
		} else if (object instanceof Folder) {
			return "genticscms_folder";
		} else if (object instanceof File) {
			if (((File) object).isImage()) {
				return "genticscms_image";
			} else {
				return "genticscms_file";
			}
		} else if (object instanceof Form) {
			return "genticscms_form_de";
		} else {
			fail(String.format("Unexpected object %s", object));
			return null;
		}
	}

	/**
	 * Do an index test
	 * 
	 * @param testCode test code
	 * @param asserter asserter
	 * @throws NodeException
	 */
	protected <T> T indexTest(Supplier<T> testCode, Consumer<T> asserter) throws NodeException {
		Indexer.init();
		T result = null;
		try {
			result = testCode.supply();
		} finally {
			int timeout = 1;
			TimeUnit timeoutUnit = TimeUnit.HOURS;
			assertThat(Indexer.shutdown(timeout, timeoutUnit))
					.as(String.format("Indexer was cleanly shut down within %d %s", timeout, timeoutUnit)).isTrue();
		}

		asserter.accept(result);
		return result;
	}

	/**
	 * Assert that the object is indexed as given node
	 * 
	 * @param object object
	 * @param node   node
	 * @throws NodeException
	 */
	protected void assertIndexed(NodeObject object, JsonNode node) throws NodeException {
		String expected = toSortedJson(node);
		String index = index(object);
		HttpClient httpClient = new HttpClient();
		String url = String.format("http://%s:%d/%s/_doc/%d", elasticsearch.getHost(), elasticsearch.getMappedPort(9200), index, object.getId());
		GetMethod loadDOcumentMethod = new GetMethod(url);

        try {
            int responseCode = httpClient.executeMethod(loadDOcumentMethod);

			assertThat(responseCode)
				.as("Index lookup response code")
				.isEqualTo(200);

			ObjectNode doc = (ObjectNode) mapper.readTree(loadDOcumentMethod.getResponseBodyAsString()).get("_source");

			if (doc.has("attachment")) {
				JsonNode attachment = doc.get("attachment");

				doc.remove("attachment");
				doc.put("content", attachment.get("content").asText());
			}

			assertThat(toSortedJson(doc)).isEqualTo(expected);
        } catch (IOException e) {
            throw new NodeException(e);
        }
	}

	/**
	 * Convert the node into JSON, where all properties are sorted by key.
	 * Null values will be removed
	 * @param node node
	 * @return sorted JSON or null, if node is null
	 * @throws NodeException
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected String toSortedJson(JsonNode node) throws NodeException {
		if (node == null) {
			return null;
		}
		try {
			// convert node into a Map
			Map<String, Object> map = mapper.readValue(node.toString(), Map.class);

			// remove null values
			map.values().removeIf(value -> value == null);

			// convert map into a TreeMap (which sorts entries by natural order of the keys)
			map = new TreeMap<>(map);
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof List) {
					List list = (List)value;
					Collections.sort(list);
				}
			}

			// serialize as JSON
			return mapper.writeValueAsString(map);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Assert that the object is not indexed
	 * 
	 * @param object object
	 */
	protected void assertNotIndexed(NodeObject object) throws NodeException {
		String index = index(object);
		HttpClient httpClient = new HttpClient();
		String url = String.format("http://%s:%d/%s/_doc/%d", elasticsearch.getHost(), elasticsearch.getMappedPort(9200), index, object.getId());
		GetMethod loadDOcumentMethod = new GetMethod(url);

		try {
			int responseCode = httpClient.executeMethod(loadDOcumentMethod);

			assertThat(responseCode)
					.as("Index lookup response code")
					.isEqualTo(404);

			/*
			ObjectNode doc = (ObjectNode) mapper.readTree(loadDOcumentMethod.getResponseBodyAsString()).get("_source");

			if (doc.has("attachment")) {
				JsonNode attachment = doc.get("attachment");

				doc.remove("attachment");
				doc.put("content", attachment.get("content").asText());
			}

			assertThat(toSortedJson(doc)).isEqualTo(expected);
			 */
		} catch (IOException e) {
			throw new NodeException(e);
		}
//		fail("assertNotIndexed not implemented");
	}

	/**
	 * Assert the object to be online in exactly the given nodes
	 * 
	 * @param expected expected json object
	 * @param nodeId   node IDs
	 */
	protected void assertOnline(JsonNode expected, Integer... nodeId) {
		List<Integer> onlineIds = JsonPath.read(expected.toString(), "online");
		assertThat(onlineIds).as("object is online").containsOnly(nodeId);
	}

	/**
	 * Assert that the object contains the given group IDs
	 * @param expected expected json object
	 * @param groupId group IDs
	 */
	protected void assertGroupIds(JsonNode expected, Integer...groupId) {
		List<Integer> actual = JsonPath.read(expected.toString(), "groupId");
		assertThat(actual).as("allowed group Ids").contains(groupId);
	}

	/**
	 * Assert that the object does not contain the given group IDs
	 * @param expected expected json object
	 * @param groupId group IDs
	 */
	protected void assertNotGroupIds(JsonNode expected, Integer...groupId) {
		List<Integer> actual = JsonPath.read(expected.toString(), "groupId");
		assertThat(actual).as("allowed group Ids").doesNotContain(groupId);
	}

	/**
	 * If the value is positive, set it as the given attribute, or set the attribute
	 * to null otherwise
	 * 
	 * @param json      json
	 * @param attribute attribute name
	 * @param value     value
	 */
	protected void setPositiveInt(ObjectNode json, String attribute, int value) {
		if (value > 0) {
			json.put(attribute, value);
		} else {
			json.putNull(attribute);
		}
	}

	/**
	 * Get the object count in the index, where the given object is indexed
	 * @param object object
	 * @return index count
	 * @throws NodeException
	 */
	protected int getObjectCount(IndexableNodeObject<? extends NodeObject> object) throws NodeException {
		Set<SearchIndex> indices = Indexer.getIndices(object);
		assertThat(indices).as(String.format("Indices for %s", object)).isNotEmpty();

		int objectCount = -1;
		for (SearchIndex searchIndex : indices) {
			if (objectCount >= 0) {
				assertThat(searchIndex.getObjectCount()).as(String.format("Object count of %s", searchIndex)).isEqualTo(objectCount);
			} else {
				objectCount = searchIndex.getObjectCount();
			}
		}

		return objectCount;
	}
}
