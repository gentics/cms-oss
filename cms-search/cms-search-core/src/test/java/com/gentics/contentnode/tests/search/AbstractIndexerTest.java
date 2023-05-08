package com.gentics.contentnode.tests.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

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
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.search.IndexableNodeObject;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.object.search.SearchIndex;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.lib.etc.StringUtils;
import com.jayway.jsonpath.JsonPath;

import io.reactivex.Flowable;

/**
 * Abstract base class for Indexer Tests
 */
public abstract class AbstractIndexerTest {
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
			ObjectNode json = FakeElasticsearchResource.mapper.createObjectNode();
			json.put("id", page.getId());
			json.set("nodeId", FakeElasticsearchResource
					.createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
			json.put("folderId", page.getFolderId());
			json.put("name", page.getName());
			json.put("filename", page.getFilename());
			json.put("description", page.getDescription());
			json.set("content", FakeElasticsearchResource.mapper.createArrayNode());
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
				json.set("online", FakeElasticsearchResource
						.createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
			} else {
				json.set("online", FakeElasticsearchResource.mapper.createArrayNode());
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
			json.set("niceUrl", FakeElasticsearchResource.createStringArray(page.getNiceAndAlternateUrls()));
			ArrayNode groupIdsArray = FakeElasticsearchResource.mapper.createArrayNode();
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
			ObjectNode json = FakeElasticsearchResource.mapper.createObjectNode();
			json.put("id", folder.getId());
			json.set("nodeId", FakeElasticsearchResource
					.createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
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
			ArrayNode groupIdsArray = FakeElasticsearchResource.mapper.createArrayNode();
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
			ObjectNode json = FakeElasticsearchResource.mapper.createObjectNode();
			json.put("id", file.getId());
			json.set("nodeId", FakeElasticsearchResource
					.createIntArray(Flowable.fromArray(nodes).map(Node::getId).toList().blockingGet()));
			json.set("online", FakeElasticsearchResource.createIntArray(Flowable.fromArray(nodes)
					.filter(n -> FileOnlineStatus.isOnline(file, n)).map(Node::getId).toList().blockingGet()));
			json.put("folderId", file.getFolder().getId());
			json.put("name", file.getName());
			json.put("filename", file.getName());
			json.put("description", file.getDescription());
			if (!file.isImage() && !Objects.equals(file.getFiletype(), "application/zip")) {
				try (InputStream in = new Base64InputStream(file.getFileStream(), true, 0, null)) {
					json.put("content", StringUtils.readStream(in));
				} catch (IOException e) {
					throw new NodeException(e);
				}
			}
			json.put("created", file.getCDate().getIntTimestamp());
			json.put("creatorId", file.getCreator().getId());
			json.put("edited", file.getEDate().getIntTimestamp());
			json.put("editorId", file.getEditor().getId());
			json.put("mimetype", file.getFiletype());
			json.put("systemCreationDate", file.getCDate().getIntTimestamp());
			json.put("systemEditDate", file.getEDate().getIntTimestamp());
			json.put("deleted", file.isDeleted());
			json.set("niceUrl", FakeElasticsearchResource.createStringArray(file.getNiceAndAlternateUrls()));
			ArrayNode groupIdsArray = FakeElasticsearchResource.mapper.createArrayNode();
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
				ObjectNode json = FakeElasticsearchResource.mapper.createObjectNode();
				json.put("id", form.getId());
				json.set("nodeId", FakeElasticsearchResource
						.createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
				if (form.isOnline()) {
					json.set("online", FakeElasticsearchResource
							.createIntArray(Stream.of(nodes).map(Node::getId).collect(Collectors.toList())));
				} else {
					json.set("online", FakeElasticsearchResource.mapper.createArrayNode());
				}
				json.put("folderId", form.getFolder().getId());
				json.put("name", form.getName());
				json.put("description", form.getDescription());
				ArrayNode content = FakeElasticsearchResource.mapper.createArrayNode();
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
				ArrayNode groupIdsArray = FakeElasticsearchResource.mapper.createArrayNode();
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
		return indexTest(testCode, asserter, true);
	}

	/**
	 * Do an index test
	 * 
	 * @param testCode test code
	 * @param asserter asserter
	 * @param clear    true to clear the fake resource
	 * @throws NodeException
	 */
	protected <T> T indexTest(Supplier<T> testCode, Consumer<T> asserter, boolean clear) throws NodeException {
		if (clear) {
			FakeElasticsearchResource.clear();
		}
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
		String index = index(object);

		String actual = toSortedJson(FakeElasticsearchResource.store.getOrDefault(index, Collections.emptyMap())
				.get(Integer.toString(object.getId())));
		String expected = toSortedJson(node);
		assertThat(actual).isEqualTo(expected);
	}

	/**
	 * Convert the node into JSON, where all properties are sorted by key
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
			Map<String, Object> map = FakeElasticsearchResource.mapper.readValue(node.toString(), Map.class);

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
			return FakeElasticsearchResource.mapper.writeValueAsString(map);
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Assert that the object is not indexed
	 * 
	 * @param object object
	 */
	protected void assertNotIndexed(NodeObject object) {
		String index = index(object);
		assertThat(FakeElasticsearchResource.store.getOrDefault(index, Collections.emptyMap()))
				.doesNotContainKey(Integer.toString(object.getId()));
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

	@Path("/")
	public final static class FakeElasticsearchResource {
		public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
		public static List<Triple<String, String, JsonNode>> requests = new ArrayList<>();

		public static Map<String, Map<String, JsonNode>> store = new HashMap<>();

		@Context
		UriInfo uriInfo;

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
		 * Clear everything
		 */
		public static void clear() {
			requests.clear();
			store.clear();
		}

		@HEAD
		@Path("{path: .*}")
		public Response head(@PathParam("path") String path) {
			requests.add(Triple.of("HEAD", path, null));
			return Response.ok().build();
		}

		@GET
		@Path("/{index}/_mapping/{type}")
		public Response getMapping(@PathParam("index") String index, @PathParam("type") String type) {
			requests.add(Triple.of("GET", uriInfo.getPath(), null));

			ObjectNode json = FakeElasticsearchResource.mapper.createObjectNode();
			ObjectNode indexObj = FakeElasticsearchResource.mapper.createObjectNode();
			json.set(index, indexObj);
			ObjectNode mappings = FakeElasticsearchResource.mapper.createObjectNode();
			indexObj.set("mappings", mappings);
			ObjectNode typeObj = FakeElasticsearchResource.mapper.createObjectNode();
			mappings.set(type, typeObj);
			ObjectNode properties = FakeElasticsearchResource.mapper.createObjectNode();
			typeObj.set("properties", properties);
			// TODO set properties
			return Response.ok(json, MediaType.APPLICATION_JSON).build();
		}

		@PUT
		@Path("/{index}/_mapping/{type}")
		public Response updateMapping(@PathParam("index") String index, @PathParam("type") String type, String body)
				throws Exception {
			requests.add(Triple.of("PUT", uriInfo.getPath(), mapper.readValue(body, JsonNode.class)));
			return Response.ok().build();
		}

		@POST
		@Path("/{index}/{type}/{id}/_update")
		public Response update(@PathParam("index") String index, @PathParam("type") String type,
				@PathParam("id") String id, String body) throws Exception {
			Map<String, JsonNode> indexStore = store.computeIfAbsent(index, k -> new HashMap<>());
			if (!indexStore.containsKey(id)) {
				return Response.status(Status.NOT_FOUND).build();
			}

			JsonNode document = mapper.readValue(body, JsonNode.class).get("doc");
			ObjectNode stored = (ObjectNode) indexStore.get(id);

			for (Iterator<Entry<String, JsonNode>> iterator = document.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> entry = iterator.next();
				stored.set(entry.getKey(), entry.getValue());
			}

			indexStore.put(id, stored);

			requests.add(Triple.of("POST", uriInfo.getPath(), mapper.readValue(body, JsonNode.class)));
			return Response.ok().build();
		}

		@POST
		@Path("{path: .*}")
		public Response post(@PathParam("path") String path, String body) throws Exception {
			requests.add(Triple.of("POST", path, mapper.readValue(body, JsonNode.class)));
			return Response.ok().build();
		}

		@PUT
		@Path("{path: .*}")
		public Response put(@PathParam("path") String path, String body) throws Exception {
			JsonNode document = mapper.readValue(body, JsonNode.class);
			Pair<String, String> indexAndId = parsePath(path);
			String index = indexAndId.getLeft();
			String id = indexAndId.getRight();

			store.computeIfAbsent(index, k -> new HashMap<>()).put(id, document);

			requests.add(Triple.of("PUT", path, document));
			return Response.ok().build();
		}

		@DELETE
		@Path("{path: .*}")
		public Response delete(@PathParam("path") String path) throws Exception {
			Pair<String, String> indexAndId = parsePath(path);
			String index = indexAndId.getLeft();
			String id = indexAndId.getRight();

			store.computeIfAbsent(index, k -> new HashMap<>()).remove(id);
			requests.add(Triple.of("DELETE", path, null));

			return Response.ok().build();
		}

		/**
		 * Parse the path and return pair of index and document Id
		 * 
		 * @param path path
		 * @return pair of index and document id
		 * @throws Exception
		 */
		protected Pair<String, String> parsePath(String path) throws Exception {
			String[] parts = path.split("/");
			if (parts.length == 3) {
				return Pair.of(parts[0], parts[2]);
			} else {
				throw new Exception(String.format("Unexpected path %s", path));
			}
		}
	}

}
