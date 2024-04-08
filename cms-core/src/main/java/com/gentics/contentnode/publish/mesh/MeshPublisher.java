package com.gentics.contentnode.publish.mesh;

import static com.gentics.contentnode.publish.mesh.MeshPublishUtils.ifNotFound;
import static com.gentics.contentnode.publish.mesh.MeshPublishUtils.isRecoverable;
import static com.gentics.contentnode.rest.util.PropertySubstitutionUtil.substituteSingleProperty;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.SemaphoreMap;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.ContentLanguageTrx;
import com.gentics.contentnode.factory.HandleDependenciesTrx;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.PublishedNodeTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.factory.object.FileOnlineStatus.FileListForNode;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.image.CNGenticsImageStore;
import com.gentics.contentnode.image.CNGenticsImageStore.ImageInformation;
import com.gentics.contentnode.image.CNGenticsImageStore.ImageVariant;
import com.gentics.contentnode.image.MeshPublisherGisImageInitiator;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.DummyObject;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.SelectPartType;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.PublishAction;
import com.gentics.contentnode.publish.Publisher;
import com.gentics.contentnode.publish.SimplePublishInfo;
import com.gentics.contentnode.publish.TagmapEntryWrapper;
import com.gentics.contentnode.publish.WorkPhaseHandler;
import com.gentics.contentnode.publish.cr.MeshRoleRenderer;
import com.gentics.contentnode.publish.cr.MeshURLRenderer;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.version.CmpProductVersion;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogCollector;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.ProtocolVersion;
import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import com.gentics.mesh.util.UUIDUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.vertx.core.json.JsonObject;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Mesh Publisher for publishing objects into Mesh ContentRepositories
 */
public class MeshPublisher implements AutoCloseable {
	/**
	 * Mapname of the tagmap attribute which will contain the URLs of pages and files (as URL field)
	 */
	public static final String FIELD_GTX_URL = "gtx_url";

	/** Pattern for role object tags. */
	private static final Pattern PATTERN_ROLE_OBJECT_TAG = Pattern.compile("object\\.[^.]+");

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MeshPublisher.class);

	/**
	 * Pattern for the url
	 */
	public final static Pattern URL_PATTERN = Pattern.compile("(?<protocol>(http://|https://|//))?(?<host>[^\\:]+)(\\:(?<port>[0-9]+))?/(?<project>.+)");

	/**
	 * Postfix of the name of the schema for folders
	 */
	public final static String FOLDER_SCHEMA = "folder";

	/**
	 * Postfix of the name of the schema for pages
	 */
	public final static String PAGE_SCHEMA = "content";

	/**
	 * Postfix of the name of the schema for files
	 */
	public final static String FILE_SCHEMA = "binary_content";

	/**
	 * Name of the schema for forms
	 */
	public final static String FORM_SCHEMA = "form";

	/**
	 * Name of the tag family used for the implementation version tags
	 */
	public final static String IMPLEMENTATION_VERSION_TAGFAMILY = "gcmsImplementationVersion";

	/**
	 * Name of the tag family used for the channel UUID tags
	 */
	public final static String CHANNEL_UUID_TAGFAMILY = "gcmsChannelUuid";

	/**
	 * Name of the tag family used for internal tags
	 */
	public final static String GCMS_INTERNAL_TAGFAMILY = "gcmsInternal";

	/**
	 * Name of the "latest" tag
	 */
	public final static String LATEST_TAG = "latest";

	/**
	 * Size of the taskqueue
	 */
	public final static int TASKQUEUE_SIZE = 100;

	/**
	 * Size of the renderer pool
	 */
	public final static int RENDERERPOOL_SIZE = 5;

	/**
	 * Polling interval in ms
	 */
	public final static int POLL_INTERVAL_MS = 1000;

	/**
	 * Default pagesize for fetching all objects using graphql
	 */
	public final static int DEFAULT_GRAPHQL_PAGESIZE = 10000;

	/**
	 * Number of retries, if requests to mesh fail with 404.
	 * Currently, after creating a project in mesh (or renaming a project), the project routers are created asynchronously (even in non-cluster environments),
	 * which can cause 404 errors some time.
	 * Therefore, if requests fail with a 404 error, they are retried after some delay
	 */
	public final static int RETRIES = 3;

	/**
	 * Delay timeout
	 */
	public final static int RETRY_DELAY_MS = 100;

	/**
	 * Default timeouts in seconds
	 */
	public final static int DEFAULT_TIMEOUT = 60;

	/**
	 * Threshold in ms for logging the wait time when putting an item to a queue
	 */
	public final static int QUEUE_WAIT_LOG_THRESHOLD_MS = 1000;

	/**
	 * Retry handler
	 */
	public final static BiPredicate<? super Integer, ? super Throwable> RETRY_HANDLER = (n, t) -> {
		boolean notFoundError = false;
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			if (meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
				notFoundError = true;
			}
		}
		if (n < RETRIES && notFoundError) {
			Thread.sleep(RETRY_DELAY_MS);
			return true;
		} else {
			return false;
		}
	};

	/**
	 * Character replacement map for sanitization of node names
	 */
	public final static Map<String, String> REPLACEMENT_MAP = Arrays.asList(" ").stream()
			.collect(Collectors.toMap(java.util.function.Function.identity(), c -> FileUtil.DEFAULT_REPLACEMENT_CHARACTER));

	/**
	 * Lambda that transforms a value into a link
	 */
	public static BiFunction<TagmapEntryRenderer, Object, Object> LINKTRANSFORMER = (entry, value) -> {
		if (value instanceof AbstractContentObject) {
			AbstractContentObject object = (AbstractContentObject) value;

			if (!object.isFolder() && !object.isFile() && !object.isImage() && !object.isPage()) {
				return null;
			}

			object.addDependency("id", object.getId());
			if (value instanceof Page) {
				Page page = (Page) value;
				object.addDependency("online", page.isOnline());
			}

			return new MeshLink(object);
		} else {
			return null;
		}
	};

	/**
	 * Lambda for rendering mesh links
	 */
	public static com.gentics.contentnode.etc.BiFunction<NodeObject, String, String> LINKRENDERER = (target, branchName) -> {
		boolean isPage = target instanceof Page;
		ContentLanguage lang = null;

		if (isPage || target instanceof AbstractContentObject) {
			((AbstractContentObject) target).addDependency("id", target.getId());

			if (isPage) {
				((AbstractContentObject) target).addDependency("online", ((Page) target).isOnline());
				lang = ((Page) target).getLanguage();
			}
		}

		return String.format(
			"{{mesh.link(%s, %s, %s)}}",
			getMeshUuid(target),
			lang == null ? "en" : lang.getCode(),
			branchName);
	};

	/**
	 * First Mesh Version, which supports publishing and setting role permissions with the create/update request
	 */
	public final static CmpProductVersion FIRST_MESH_VERSION_WITH_PUBLISH_ON_CREATE = new CmpProductVersion("1.10.8");

	/**
	 * First Mesh SQL Version, which supports publishing and setting role permissions with the create/update request
	 */
	public final static CmpProductVersion FIRST_MESH_SQL_VERSION_WITH_PUBLISH_ON_CREATE = new CmpProductVersion("1.10.5");

	/**
	 * Semaphore map for synchronization of instant publishing transactions to the Mesh CRs
	 */
	protected final static SemaphoreMap<Integer> semaphoreMap = new SemaphoreMap<>("mesh_trx");

	/**
	 * Map of object types to schema names
	 */
	protected static Map<Integer, String> schemaNames = new HashMap<>();

	/**
	 * Set of object types, which are mapped to container schemas
	 */
	protected static Set<Integer> containerTypes = new HashSet<>(Arrays.asList(Folder.TYPE_FOLDER));

	/**
	 * End Marker for the Task Queue
	 */
	protected static WriteTask END = new WriteTask();

	/**
	 * Keys in the schema change operation, that can be ignored
	 */
	protected static List<String> IGNORABLE_UPDATE_KEYS = Arrays.asList(SchemaChangeModel.DESCRIPTION_KEY, SchemaChangeModel.FIELD_NAME_KEY,
			SchemaChangeModel.LABEL_KEY, SchemaChangeModel.FIELD_ORDER_KEY);

	/**
	 * Get a filter for ignoring changes, which were detected by Mesh, but should not lead to schema updates
	 * @param expected expected schema
	 * @param existing existing schema
	 * @return filter
	 */
	protected static Predicate<? super SchemaChangeModel> getSchemaChangeFilter(SchemaModel expected, SchemaModel existing) {
		return model -> {
			switch (model.getOperation()) {
			case EMPTY:
				// empty changes are ignored in any case
				return false;
			case UPDATEFIELD:
			case UPDATESCHEMA:
				// updates to fields or the schema are ignored, if they only list ignorable properties
				if (IGNORABLE_UPDATE_KEYS.containsAll(model.getProperties().keySet())) {
					return false;
				}

				// if the URL fields change, we check whether the change is only in the order
				if (model.getProperties().containsKey(SchemaChangeModel.URLFIELDS_KEY)) {
					@SuppressWarnings("unchecked")
					Set<String> expectedUrlFields = new HashSet<>(ObjectTransformer.getCollection(expected.getUrlFields(), Collections.emptyList()));
					@SuppressWarnings("unchecked")
					Set<String> existingUrlFields = new HashSet<>(ObjectTransformer.getCollection(existing.getUrlFields(), Collections.emptyList()));
					return !expectedUrlFields.containsAll(existingUrlFields) || !existingUrlFields.containsAll(expectedUrlFields);
				}
				return true;
			default:
				// all other changes are relevant
				return true;
			}
		};
	}

	/**
	 * Map holding the uuid's onto which root folders of nodes need to be published (if the uuid cannot be composed of the folder's global ID)
	 * This is the case, if the node is published into a Mesh CR with "projectPerNode" enabled, because in this case, the root folder is published onto the root Node of the project.
	 */
	protected static Map<Node, String> rootFolderUuid = new ConcurrentHashMap<>();

	/**
	 * Parameter "wait"="false"
	 */
	protected static GenericParameters doNotWaitForIdle = new GenericParametersImpl();

	static {
		schemaNames.put(Folder.TYPE_FOLDER, FOLDER_SCHEMA);
		schemaNames.put(Page.TYPE_PAGE, PAGE_SCHEMA);
		schemaNames.put(File.TYPE_FILE, FILE_SCHEMA);
		doNotWaitForIdle.setParameter("wait", "false");
	}

	/**
	 * Map of ID sets of objects already handled (per node ID)
	 */
	protected Map<Integer, Map<Integer, Set<Integer>>> handled = new ConcurrentHashMap<>();

	/**
	 * MeshPublisherController, when the publisher was started in a publish process
	 */
	protected MeshPublishController controller;

	/**
	 * ContentRepository instance
	 */
	protected ContentRepository cr;

	/**
	 * Lock key for the semaphore
	 */
	protected Integer lockKey;

	/**
	 * Tagmap entries per types
	 */
	protected Map<Integer, List<TagmapEntryRenderer>> tagmapEntries = new HashMap<>();

	/**
	 * Configuration for the Mesh Rest Client
	 */
	protected MeshRestClientConfig clientConfig;

	/**
	 * OK Http Client, which is used by the Mesh Rest Client
	 */
	protected OkHttpClient okHttpClient;

	/**
	 * Logged in client instance
	 */
	protected MeshRestClient client;

	/**
	 * Flag that is set, when the client is a Mesh admin
	 */
	protected boolean clientIsAdmin;

	/**
	 * Mesh host
	 */
	protected String host;

	/**
	 * Mesh port
	 */
	protected int port;

	/**
	 * SSL connection to Mesh
	 */
	protected boolean ssl;

	/**
	 * Prefix for the schemata
	 */
	protected String schemaPrefix;

	/**
	 * Map of Node ID -> MeshProject
	 */
	protected Map<Integer, MeshProject> projectMap = new HashMap<>();

	/**
	 * Set of (possible) alternative MeshProjects.
	 * This contains projects, that exist in the Mesh CR and may contain objects, that need to be moved to the correct projects.
	 * These projects would be used, if the CR would have a different setting for projectPerNode
	 */
	protected Set<MeshProject> alternativeProjects = new HashSet<>();

	/**
	 * Role map single
	 */
	protected Single<Map<String, String>> roleMapSingle;

	/**
	 * Role map
	 */
	protected Map<String, String> roleMap;

	/**
	 * Server Info
	 */
	protected Single<MeshServerInfoModel> serverInfo;

	/**
	 * Flag, which is set when publishing into Mesh SQL
	 */
	protected boolean meshSql = false;

	/**
	 * Flag to mark whether an error was found
	 */
	protected boolean error = false;

	/**
	 * List of throwables that were caught while publishing
	 */
	protected List<Throwable> throwables = new ArrayList<>();

	/**
	 * Set of postponed write tasks
	 */
	protected Set<WriteTask> postponedTasks = new HashSet<>();

	/**
	 * Optional publish info instance
	 */
	protected SimplePublishInfo publishInfo;

	/**
	 * Render result
	 */
	protected RenderResult renderResult;

	/**
	 * Micronode publisher
	 */
	protected MeshMicronodePublisher micronodePublisher;

	/**
	 * Map containing the IDs of all objects, which were written to Mesh during the publish process (per nodeId and objectType)
	 */
	protected Map<Integer, Map<Integer, Set<Integer>>> written = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Map containing the IDs of all objects, which were already checked and are known to be missing in Mesh (per nodeId and objectType)
	 */
	protected Map<Integer, Map<Integer, Set<Integer>>> missing = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Image data
	 */
	protected Map<String, ImageInformation> allImageData = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Lambda that handles errors
	 */
	protected java.util.function.Consumer<Throwable> errorHandler = t -> {
		logger.error(String.format("Error while publishing into Mesh CR %s", cr.getName()), t);
		throwables.add(t);
		error = true;
		if (controller != null && controller.publishProcess) {
			PublishController.setError(t);
		}
	};

	/**
	 * Configured call timeout (in seconds)
	 */
	protected long callTimeout = DEFAULT_TIMEOUT;

	/**
	 * Flag, which is set to true, when the Mesh Server supports publishing (and setting role permission) on create/update requests
	 */
	protected boolean supportsPublishOnCreate = false;

	/**
	 * Counter for requests to get a single node from Mesh
	 */
	protected AtomicInteger getNodeCounter = new AtomicInteger();

	/**
	 * Counter for requests to save a node to Mesh
	 */
	protected AtomicInteger saveNodeCounter = new AtomicInteger();

	/**
	 * Counter for graphql requests
	 */
	protected AtomicInteger graphQlCounter = new AtomicInteger();

	/**
	 * Shutdown Mesh Publisher
	 */
	public static void shutdown() {
	}

	/**
	 * Get the level of the folder, which is the number of parent folders (until the root folder is reached).
	 * Root folders have level 0, their immediate children level 1, ...
	 * @param folder folder
	 * @param levelMap map containing already determined levels
	 * @return level
	 * @throws NodeException
	 */
	protected static int getFolderLevel(Folder folder, Map<Integer, Integer> levelMap) {
		return levelMap.computeIfAbsent(folder.getId(), id -> {
			try {
				if (folder.isRoot()) {
					return 0;
				} else {
					return getFolderLevel(folder.getMother(), levelMap) + 1;
				}
			} catch (NodeException e) {
				throw new ClassCastException(String.format("Error while getting folder level of %s: %s", folder, e.getLocalizedMessage()));
			}
		});
	}

	/**
	 * Get the Mesh UUID of the given object. For pages, this is the global contentset ID, for other objects, it's their global ID
	 * For localized copies, the global ID of the master is returned.
	 * @param object object
	 * @return Mesh UUID
	 * @throws NodeException
	 */
	public static String getMeshUuid(NodeObject object) throws NodeException {
		return getMeshUuid(object, true);
	}

	/**
	 * Get the Mesh UUID of the given object. For pages, this is the global contentset ID, for other objects, it's their global ID
	 * For localized copies, the global ID of the master is returned.
	 * @param object object
	 * @param useProjectRootNode true when the returned uuid may be the uuid of the project's root node (if project per node is activated). False to always use the uuid converted from the cms uuid
	 * @return Mesh UUID
	 * @throws NodeException
	 */
	public static String getMeshUuid(NodeObject object, boolean useProjectRootNode) throws NodeException {
		if (object instanceof DummyObject) {
			return ((DummyObject) object).getMeshUuid();
		} else if (object instanceof Page) {
			Page page = (Page) object;
			GlobalId globalId = GlobalId.getGlobalId("contentset", page.getMaster().getContentsetId());
			if (globalId != null) {
				return toMeshUuid(globalId.toString());
			} else {
				return null;
			}
		} else if (useProjectRootNode && object instanceof Folder) {
			Folder folder = (Folder) object;
			folder = folder.getMaster();
			Node node = folder.getNode().getMaster();
			// if the folder is the root folder and the root folder uuid is stored for the node, we use it
			if (folder.isRoot() && rootFolderUuid.containsKey(node)) {
				return rootFolderUuid.get(node);
			} else {
				return toMeshUuid(folder.getGlobalId().toString());
			}
		} else if (object instanceof LocalizableNodeObject) {
			@SuppressWarnings("unchecked")
			LocalizableNodeObject<NodeObject> locObject = (LocalizableNodeObject<NodeObject>)object;
			return toMeshUuid(locObject.getMaster().getGlobalId().toString());
		} else {
			return toMeshUuid(object.getGlobalId().toString());
		}
	}

	/**
	 * Transform the given CMS uuid into a mesh compatible uuid
	 * @param uuid CMS uuid
	 * @return mesh compatible uuid
	 */
	public static String toMeshUuid(String uuid) {
		if (uuid == null) {
			return null;
		}
		// check whether globalId is old style (generated from sequence)
		if (uuid.contains("-")) {
			// new style, remove the global prefix and all -
			if (uuid.contains(".")) {
				uuid = uuid.substring(uuid.indexOf('.') + 1);
			}
			uuid = uuid.replaceAll("\\-", "");
		} else {
			// old style, remove . between prefix and sequence
			uuid = uuid.replaceAll("\\.", "").toLowerCase();
			// then pad with 0 (at the start), to fill up to 32 characters
			uuid = StringUtils.pad(uuid, "0", 32, StringUtils.PADDING_START);
		}

		return uuid;
	}

	/**
	 * Prepare the maps of mesh UUIDs to sets of internal object IDs for all object types
	 * @return map of type -> map of mesh UUID -> set of internal IDs
	 * @throws NodeException
	 */
	public static Map<Integer, Map<String, Set<Integer>>> prepareMeshUuidMap() throws NodeException {
		Map<Integer, Map<String, Set<Integer>>> map = new HashMap<>();
		for (Integer objType : schemaNames.keySet()) {
			map.put(objType, prepareMeshUuidMap(objType));
		}
		// add forms
		map.put(Form.TYPE_FORM, prepareMeshUuidMap(Form.TYPE_FORM));
		return map;
	}

	/**
	 * Prepare the map of mesh UUIDs to sets of internal object IDs for the given type
	 * @param objType object type
	 * @return map of mesh UUID -> set of internal IDs
	 * @throws NodeException
	 */
	public static Map<String, Set<Integer>> prepareMeshUuidMap(int objType) throws NodeException {
		String sql = null;
		switch (objType) {
		case Folder.TYPE_FOLDER:
			sql = "SELECT id, uuid FROM folder WHERE is_master = 1 AND deleted = 0";
			break;
		case Page.TYPE_PAGE:
			sql = "SELECT page.id, contentset.uuid FROM page,contentset WHERE page.contentset_id = contentset.id AND page.is_master = 1 AND page.deleted = 0";
			break;
		case File.TYPE_FILE:
			sql = "SELECT id, uuid FROM contentfile WHERE is_master = 1 AND deleted = 0";
			break;
		case Form.TYPE_FORM:
			sql = "SELECT id, uuid FROM form WHERE deleted = 0";
			break;
		default:
			throw new NodeException(String.format("Cannot prepare map of mesh UUIDs to internal IDs for unknown type %d", objType));
		}
		return DBUtils.select(sql, rs -> {
			Map<String, Set<Integer>> temp = new HashMap<>();
			while (rs.next()) {
				String meshUuid = toMeshUuid(rs.getString("uuid"));
				int id = rs.getInt("id");
				temp.computeIfAbsent(meshUuid, key -> new HashSet<>()).add(id);
			}
			return temp;
		});
	}

	/**
	 * Load the objects of given type, which have the given mesh UUIDs
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param meshUuidMap map of mesh UUIDs to sets of internal IDs
	 * @param meshUuids set of mesh UUIDs to load
	 * @return list of objects
	 * @throws NodeException
	 */
	public static <T extends NodeObject> List<T> fromMeshUuid(Class<T> clazz, Map<String, Set<Integer>> meshUuidMap,
			Set<String> meshUuids) throws NodeException {
		Set<Integer> ids = meshUuids.stream().flatMap(meshUuid -> {
			if (meshUuidMap.containsKey(meshUuid)) {
				return meshUuidMap.get(meshUuid).stream();
			} else {
				return Stream.empty();
			}
		}).collect(Collectors.toSet());
		return TransactionManager.getCurrentTransaction().getObjects(clazz, ids);
	}

	/**
	 * Get the Mesh language for the given object. Pages will reuse their language (if they have one), pages that have no language, folders and files
	 * will have "en" as language in Mesh.
	 * @param object object
	 * @return Mesh language
	 * @throws NodeException
	 */
	public static String getMeshLanguage(NodeObject object) throws NodeException {
		if (object instanceof DummyObject) {
			return ((DummyObject) object).getMeshLanguage();
		} else if (object instanceof Page) {
			Page page = (Page)object;
			ContentLanguage language = page.getLanguage();
			if (language != null) {
				return language.getCode();
			} else {
				return "en";
			}
		} else {
			return "en";
		}
	}

	/**
	 * Get the alternative Mesh language for the given object.
	 * This will return additional languages for folders (if translations exist) other than "en" (which is the primary language)
	 * @param object object
	 * @return Mesh language
	 * @throws NodeException
	 */
	public static Set<String> getAlternativeMeshLanguages(NodeObject object) throws NodeException {
		if (object instanceof Folder) {
			Folder folder = (Folder) object;
			Set<String> languages = new HashSet<>();
			languages.addAll(I18nMap.TRANSFORM2REST.apply(folder.getNameI18n()).keySet());
			languages.addAll(I18nMap.TRANSFORM2REST.apply(folder.getDescriptionI18n()).keySet());
			languages.addAll(I18nMap.TRANSFORM2REST.apply(folder.getPublishDirI18n()).keySet());
			// remove the primary language "en"
			languages.remove("en");
			return languages;
		} else {
			return Collections.emptySet();
		}
	}

	/**
	 * Check whether the object supports alternative languages (currently only folders).
	 * Alternative languages means that the same object (same cms_id) may be published onto multiple language variants in Mesh.
	 * Pages do <b>not</b> support alternative languages, because every language variant has a unique cms_id
	 * @param object object
	 * @return true iff the object supports alternative languages
	 */
	public static boolean supportsAlternativeLanguages(NodeObject object) {
		return supportsAlternativeLanguages(object.getTType());
	}

	/**
	 * Check whether objects with given type support alternative languages (currently only folders).
	 * Alternative languages means that the same object (same cms_id) may be published onto multiple language variants in Mesh.
	 * Pages do <b>not</b> support alternative languages, because every language variant has a unique cms_id
	 * @param objType object type
	 * @return true iff the object type supports alternative languages
	 */
	public static boolean supportsAlternativeLanguages(int objType) {
		return objType == Folder.TYPE_FOLDER || objType == Form.TYPE_FORM;
	}

	/**
	 * Transform the node name into a name (for project and/or branch)
	 * @param node channel
	 * @return mesh name
	 * @throws NodeException
	 */
	public static String getMeshName(Node node) throws NodeException {
		return FileUtil.sanitizeName(node.getFolder().getName(), REPLACEMENT_MAP, null, null);
	}

	/**
	 * Get branch name for the given channel and implementation version
	 * @param channel channel
	 * @param implementationVersion optional implementation version
	 * @return branch name
	 * @throws NodeException
	 */
	public static String getBranchName(Node channel, String implementationVersion) throws NodeException {
		return getBranchName(channel.getFolder().getName(), implementationVersion);
	}

	/**
	 * Get branch name for the given name (prefix) and implementation version
	 * @param name name prefix
	 * @param implementationVersion optional implementation version
	 * @return branch name
	 * @throws NodeException
	 */
	public static String getBranchName(String name, String implementationVersion) throws NodeException {
		StringBuilder fullName = new StringBuilder(name);
		if (!StringUtils.isEmpty(implementationVersion)) {
			fullName.append(" ").append(implementationVersion);
		}
		return FileUtil.sanitizeName(fullName.toString(), REPLACEMENT_MAP, null, null);
	}

	/**
	 * Get the mesh project name for the given node, when it publishes into Mesh, null if not
	 * @param node node
	 * @return mesh project name
	 * @throws NodeException
	 */
	public static String getMeshProjectName(Node node) throws NodeException {
		node = node.getMaster();
		ContentRepository cr = node.getContentRepository();
		if (cr == null) {
			return null;
		}
		if (cr.getCrType() != Type.mesh) {
			return null;
		}
		if (cr.isProjectPerNode()) {
			return getMeshName(node);
		} else {
			Matcher urlMatcher = URL_PATTERN.matcher(cr.getEffectiveUrl());
			if (!urlMatcher.matches()) {
				return null;
			}
			return urlMatcher.group("project");
		}
	}

	/**
	 * Clean the path prefix like Mesh does it.
	 * <ol>
	 * <li>if pathPrefix is null or empty, return it unchanged</li>
	 * <li>Make sure, the pathPrefix starts with /, but does not end with /</li>
	 * <li>If pathPrefix is only /, return empty string</li>
	 * </ol>
	 * @param pathPrefix
	 * @return
	 */
	public static String cleanPathPrefix(String pathPrefix) {
		if (StringUtils.isEmpty(pathPrefix)) {
			return pathPrefix;
		}
		if ("/".equals(pathPrefix)) {
			return "";
		}
		if (!pathPrefix.startsWith("/")) {
			pathPrefix = "/" + pathPrefix;
		}
		if (pathPrefix.endsWith("/")) {
			pathPrefix = pathPrefix.substring(0, pathPrefix.length() - 1);
		}
		return pathPrefix;
	}

	/**
	 * Check whether the branch is tagged with the given tag
	 * @param branch branch
	 * @param familyName tag family name
	 * @param tagName tag name
	 * @return true iff the branch is tagged with the given tag
	 */
	public static boolean hasTag(BranchResponse branch, String familyName, String tagName) {
		return branch.getTags().stream().filter(tag -> familyName.equals(tag.getTagFamily()) && tagName.equals(tag.getName())).findFirst().isPresent();
	}

	/**
	 * Check whether the branch is tagged by ANY tag of the given family
	 * @param branch branch
	 * @param familyName tag family name
	 * @return true iff the branch is tagged
	 */
	public static boolean hasTag(BranchResponse branch, String familyName) {
		return getTag(branch, familyName).isPresent();
	}

	/**
	 * Get optional tag
	 * @param branch branch
	 * @param familyName tag family name
	 * @return optional containing the first tag of the tag family
	 */
	public static Optional<TagReference> getTag(BranchResponse branch, String familyName) {
		return branch.getTags().stream().filter(tag -> familyName.equals(tag.getTagFamily())).findFirst();
	}

	/**
	 * Get readable output for SchemaChangeModel
	 * @param change change
	 * @return readable info
	 */
	protected static String getReadableInfo(SchemaChangeModel change) {
		return String.format("Change %s, properties %s", change.getOperation().name(), change.getProperties());
	}

	/**
	 * Normalize the object type
	 * @param objType object type
	 * @return normalized
	 */
	public static int normalizeObjType(int objType) {
		if (objType == ImageFile.TYPE_IMAGE) {
			return File.TYPE_FILE;
		}
		return objType;
	}

	/**
	 * Create an instance of the MeshPublisher
	 * @param cr ContentRepository
	 * @throws NodeException
	 */
	public MeshPublisher(ContentRepository cr) throws NodeException {
		this(cr, true);
	}

	/**
	 * Create an instance of the MeshPublisher
	 * @param cr ContentRepository
	 * @param connect true if the client shall be created, false if not
	 * @throws NodeException
	 */
	public MeshPublisher(ContentRepository cr, boolean connect) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		micronodePublisher = new MeshMicronodePublisher(this);
		renderResult = t.getRenderResult();

		this.cr = cr;
		if (cr.getCrType() != Type.mesh) {
			throw new NodeException(String.format("Cannot connect to CR %s of type %s. Only type %s is supported.", cr.getName(), cr.getCrType(), Type.mesh));
		}
		Matcher urlMatcher = URL_PATTERN.matcher(cr.getEffectiveUrl());
		if (!urlMatcher.matches()) {
			throw new RestMappedException(I18NHelper.get("meshcr.invalid.url", cr.getUrl(), cr.getName())).setMessageType(Message.Type.CRITICAL)
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}

		lockKey = cr.getId();
		semaphoreMap.init(lockKey);

		ssl = false;
		int defaultPort = 80;
		if (urlMatcher.group("protocol") != null) {
			String protocol = urlMatcher.group("protocol");
			if (protocol.startsWith("https")) {
				ssl = true;
				defaultPort = 443;
			}
		}
		host = urlMatcher.group("host");
		port = defaultPort;
		if (urlMatcher.group("port") != null) {
			port = Integer.parseInt(urlMatcher.group("port"));
		}
		schemaPrefix = urlMatcher.group("project");
		String username = cr.getEffectiveUsername();
		String password = cr.getEffectivePassword();

		initMeshProjects();

		for (int type : schemaNames.keySet()) {
			tagmapEntries.put(type, new ArrayList<>(cr.getAllEntries().stream().filter(e -> e.getObject() == type).map(e -> new TagmapEntryWrapper(e)).collect(Collectors.toList())));
			if (!ObjectTransformer.isEmpty(cr.getPermissionProperty())) {
				tagmapEntries.get(type).add(new MeshRoleRenderer(type, cr.getPermissionProperty()));
			}
			if (isConsiderGtxUrlField() && MeshURLRenderer.supportsType(type)) {
				tagmapEntries.get(type).add(new MeshURLRenderer(type));
			}
		}

		if (connect) {
			if (ObjectTransformer.isEmpty(password)) {
				throw new RestMappedException(I18NHelper.get("meshcr.apitoken.missing", cr.getName())).setMessageType(Message.Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
			callTimeout = ObjectTransformer.getLong(prefs.getProperty("mesh.client.callTimeout"), DEFAULT_TIMEOUT);
			long connectTimeout = ObjectTransformer.getLong(prefs.getProperty("mesh.client.connectTimeout"), DEFAULT_TIMEOUT);
			long writeTimeout = ObjectTransformer.getLong(prefs.getProperty("mesh.client.writeTimeout"), DEFAULT_TIMEOUT);
			long readTimeout = ObjectTransformer.getLong(prefs.getProperty("mesh.client.readTimeout"), DEFAULT_TIMEOUT);

			clientConfig = new MeshRestClientConfig.Builder().setHost(host).setPort(port).setSsl(ssl).build();
			okHttpClient = OkHttpClientProvider.get(callTimeout, connectTimeout, writeTimeout, readTimeout, cr.isHttp2() ? ProtocolVersion.HTTP_2 : ProtocolVersion.HTTP_1_1);
			client = new MeshRestOkHttpClientImpl(clientConfig, okHttpClient);

			if (ObjectTransformer.isEmpty(username)) {
				client.setAPIKey(password);
			} else {
				client.setLogin(username, password).login().blockingGet();
			}

			// check whether the client is an admin
			Boolean adminFlag = client.me().blockingGet().getAdmin();
			clientIsAdmin = adminFlag != null ? adminFlag.booleanValue() : false;

			roleMapSingle = client.findRoles().toSingle().map(response -> response.getData().stream().filter(role -> !"admin".equals(role.getName()))
					.collect(Collectors.toMap(RoleResponse::getName, RoleResponse::getUuid))).cache();
			roleMapSingle.subscribe();

			serverInfo = client.getApiInfo().toSingle().cache();
			serverInfo.subscribe();

			// determine, whether the Mesh version supports publishing (and setting roles) with the save requests
			meshSql = !serverInfo.blockingGet().getDatabaseVendor().contains("orientdb");
			supportsPublishOnCreate = getMeshServerVersion().isGreaterOrEqual(meshSql ? FIRST_MESH_SQL_VERSION_WITH_PUBLISH_ON_CREATE : FIRST_MESH_VERSION_WITH_PUBLISH_ON_CREATE);
		} else {
			micronodePublisher.initForPreview();
		}

	}

	/**
	 * Get Mesh host
	 * @return mesh host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get Mesh port
	 * @return mesh port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Flag for SSL connection to mesh
	 * @return true for SSL
	 */
	public boolean isSsl() {
		return ssl;
	}

	/**
	 * Get schema prefix
	 * @return schema prefix
	 */
	public String getSchemaPrefix() {
		return schemaPrefix;
	}

	/**
	 * Initialize the mesh projects
	 * @throws NodeException
	 */
	protected void initMeshProjects() throws NodeException {
		if (cr.isProjectPerNode()) {
			// create a mesh project for every node
			projectMap.putAll(cr.getNodes().stream().filter(n -> !n.isPublishDisabled() && n.doPublishContentmap()).map(n -> {
				try {
					return n.getMaster();
				} catch (NodeException e) {
					throw new RuntimeException(e);
				}
			}).distinct().collect(Collectors.toMap(Node::getId, node -> new MeshProject(node))));

			alternativeProjects.add(new MeshProject(schemaPrefix));
		} else {
			// create a single project with the schema prefix as name
			projectMap.put(0, new MeshProject(schemaPrefix));
			cr.getNodes().forEach(rootFolderUuid::remove);

			alternativeProjects.addAll(cr.getNodes().stream().map(n -> new MeshProject(n)).collect(Collectors.toSet()));
		}
	}

	/**
	 * Get the mesh project for the node
	 * @param node node
	 * @return mesh project (never null)
	 * @throws NodeException if no mesh project could be found
	 */
	public MeshProject getProject(Node node) throws NodeException {
		return getProject(node.getMaster().getId());
	}

	/**
	 * Get the mesh project for the node ID
	 * @param nodeId node ID
	 * @return mesh project (never null)
	 * @throws NodeException if no mesh project could be found
	 */
	public MeshProject getProject(int nodeId) throws NodeException {
		MeshProject meshProject = projectMap.get(cr.isProjectPerNode() ? nodeId : 0);
		if (meshProject == null) {
			throw new NodeException(String.format("Could not find mesh project for nodeId %d", nodeId));
		}
		return meshProject;
	}

	/**
	 * Get the mesh project for the node object as Optional.
	 * @param object node object
	 * @return Optional mesh project
	 * @throws NodeException
	 */
	public Optional<MeshProject> getProject(NodeObject object) throws NodeException {
		if (object == null) {
			return Optional.empty();
		}
		Node node = null;
		if (object instanceof Folder) {
			node = ((Folder) object).getNode();
		} else {
			return getProject(object.getParentObject());
		}

		try {
			return Optional.of(getProject(node));
		} catch (NodeException e) {
			return Optional.empty();
		}
	}

	/**
	 * Check whether there is another project than the given one, which might contain nodes
	 * @param project project to check
	 * @return true iff there are other projects
	 */
	public boolean hasOtherProjects(MeshProject project) {
		return Stream.concat(projectMap.values().stream(), alternativeProjects.stream())
				.filter(p -> !Objects.equals(p, project)).findFirst().isPresent();
	}

	/**
	 * Transform the tagmap entries of the ContentRepository for the object type into the Mesh Schema
	 * @param type object type
	 * @param schema schema to update
	 * @return updated schema
	 * @throws NodeException
	 */
	public <T extends SchemaModel> T getSchema(int type, T schema) throws NodeException {
		if (cr == null) {
			throw new NodeException("Cannot create schema for no ContentRepository");
		}
		if (cr.getCrType() != Type.mesh) {
			throw new NodeException(String.format("Cannot create schema for ContentRepository of type %s, only %s supported", cr.getCrType(), Type.mesh));
		}
		if (!schemaNames.containsKey(type)) {
			throw new NodeException(String.format("Cannot create schema for type %d", type));
		}
		switch(type) {
		case Folder.TYPE_FOLDER:
			schema.setNoIndex(cr.isNoFoldersIndex());
			break;
		case ImageFile.TYPE_IMAGE:
		case File.TYPE_FILE:
			schema.setNoIndex(cr.isNoFilesIndex());
			break;
		case Form.TYPE_FORM:
			schema.setNoIndex(cr.isNoFormsIndex());
			break;
		case Page.TYPE_PAGE:
			schema.setNoIndex(cr.isNoPagesIndex());
			break;
		}
		schema.setName(getSchemaName(type));
		schema.setContainer(containerTypes.contains(type));

		// collect all url field names
		List<String> urlFields = new ArrayList<>();

		// the schema either contains a "gtxurl" field, or a segment field
		if (isConsiderGtxUrlField()) {
			// when we need to add the "gtxurl" field, we make it a URL field
			if (addGtxUrlFieldSchema(type, schema)) {
				urlFields.add(FIELD_GTX_URL);
			}
		} else {
			TagmapEntry segmentField = cr.getAllEntries()
				.stream()
				.filter(entry -> entry.getObject() == type)
				.filter(entry -> entry.isSegmentfield())
				.findFirst()
				.orElseThrow(() -> new NodeException(String.format("Cannot create schema for ContentRepository %s and type %s, no segmentfield found", cr.getName(), type)));
			schema.setSegmentField(segmentField.getMapname());
		}

		TagmapEntry displayField = cr.getAllEntries()
			.stream()
			.filter(entry -> entry.getObject() == type)
			.filter(entry -> entry.isDisplayfield())
			.findFirst()
			.orElseThrow(() -> new NodeException(String.format("Cannot create schema for ContentRepository %s and type %s, no displayfield found", cr.getName(), type)));
		schema.setDisplayField(displayField.getMapname());

		urlFields.addAll(cr.getAllEntries()
			.stream()
			.filter(entry -> entry.getObject() == type)
			.filter(entry -> entry.isUrlfield())
			.map(TagmapEntry::getMapname)
			.collect(Collectors.toList()));
		schema.setUrlFields(urlFields);

		cr.getAllEntries().stream().filter(entry -> entry.getObject() == type).forEach(entry -> {
			addFieldSchema(schema, entry);
		});

		// set elasticsearch config
		String elasticsearch = cr.getElasticsearch();
		if ("null".equals(elasticsearch)) {
			elasticsearch = null;
		}
		if (!StringUtils.isEmpty(elasticsearch)) {
			JsonObject elasticSearch = new JsonObject(elasticsearch);
			String key = null;
			switch (type) {
			case Page.TYPE_PAGE:
				key = "page";
				break;
			case Folder.TYPE_FOLDER:
				key = "folder";
				break;
			case File.TYPE_FILE:
				key = "file";
				break;
			}
			JsonObject typeConfig = elasticSearch.getJsonObject(key, null);
			if (typeConfig != null) {
				schema.setElasticsearch(typeConfig);
			} else {
				schema.setElasticsearch(new JsonObject());
			}
		} else {
			schema.setElasticsearch(new JsonObject());
		}

		return schema;
	}

	/**
	 * Get ContentRepository
	 * @return ContentRepository
	 */
	public ContentRepository getCr() {
		return cr;
	}

	/**
	 * Get the Mesh status
	 * @return status
	 */
	public MeshStatus getStatus() {
		return client.meshStatus().blockingGet().getStatus();
	}

	/**
	 * Check the Mesh Status
	 * @return mesh status
	 * @throws NodeException
	 */
	public boolean checkStatus() throws NodeException {
		MeshStatus status = getStatus();
		if (status == MeshStatus.READY) {
			info(String.format("Mesh status is %s", status));
		} else {
			error(String.format("Mesh status is %s", status));
		}
		return status == MeshStatus.READY;
	}

	/**
	 * Check validity of Mesh Data
	 * <ol>
	 * <li>Check existence and validity of schemas</li>
	 * <li>If a schema does not exist or is invalid, and "repair" is true, transform the tagmap entries to schema fields and create/update schema</li>
	 * <li>Check existence of projects</li>
	 * <li>If project does not exist and "repair" is true, create it</li>
	 * <li>Check assignment of schemas to projects</li>
	 * <li>If schema is not assigned to project and "repair" is true, assign it</li>
	 * </ol>
	 * @param repair true to repair invalid structure
	 * @param checkMicroschemas true to also check microschema validity (if false, only existing microschemas will be loaded)
	 * @return check result
	 * @throws NodeException
	 */
	public boolean checkSchemasAndProjects(boolean repair, boolean checkMicroschemas) throws NodeException {
		info(String.format("Check schemas and projects for '%s'", cr.getName()));

		// first check whether nodes publishing into the CR have inconsistent settings for "publish directory segment"
		Set<Node> nodesWithPubDirSegment = getNodes(true);
		Set<Node> nodesWithoutPubDirSegment = getNodes(false);
		if (!nodesWithPubDirSegment.isEmpty() && !nodesWithoutPubDirSegment.isEmpty()) {
			List<String> namesWith = Flowable.fromIterable(nodesWithPubDirSegment).map(n -> I18NHelper.getName(n)).toList().blockingGet();
			List<String> namesWithout = Flowable.fromIterable(nodesWithoutPubDirSegment).map(n -> I18NHelper.getName(n)).toList().blockingGet();

			error("Inconsistent setting of \"publish directory segment\" found for assigned nodes:\n  Nodes with \"publish directory segment\": %s\n  Nodes without \"publish directory segment\": %s",
					namesWith, namesWithout);
			return false;
		}

		// prepare check result
		AtomicBoolean success = new AtomicBoolean(true);

		// read all existing schemas
		Map<String, SchemaResponse> schemaMap = client.findSchemas().blockingGet().getData()
				.stream().collect(Collectors.toMap(SchemaResponse::getName, java.util.function.Function.identity()));

		// check whether all required schemas exist
		for (int objectType : schemaNames.keySet()) {
			String schemaName = getSchemaName(objectType);
			SchemaResponse schema = schemaMap.get(schemaName);

			if (schema == null) {
				info(String.format("Did not find schema %s", schemaName));

				if (repair) {
					SchemaCreateRequest create = getSchema(objectType, new SchemaCreateRequest());

					SchemaResponse createdSchema = client.createSchema(create).blockingGet();

					info(String.format("Created schema %s", schemaName));
					schemaMap.put(schemaName, createdSchema);
				} else {
					success.set(false);
				}
			} else {
				SchemaUpdateRequest expectedSchema = getSchema(objectType, new SchemaUpdateRequest());

				// diff the schema
				SchemaResponse updatedSchema = client.diffSchema(schema.getUuid(), expectedSchema).toSingle().flatMap(diffResponse -> {
					List<SchemaChangeModel> filteredChanges = Flowable.fromIterable(diffResponse.getChanges()).filter(getSchemaChangeFilter(expectedSchema, schema)).toList()
							.blockingGet();
					if (filteredChanges.isEmpty()) {
						info(String.format("Schema %s is valid", schemaName));
						// do nothing
						return Single.just(schema);
					} else if (repair) {
						info(String.format("Schema %s is not valid and will be updated", schemaName));
						filteredChanges.forEach(change -> logger.warn(getReadableInfo(change)));
						// update the schema
						return client.updateSchema(schema.getUuid(), expectedSchema, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false))
								.toSingle().flatMap(r -> client.findSchemaByUuid(schema.getUuid()).toSingle());
					} else {
						info(String.format("Schema %s is not valid", schemaName));
						filteredChanges.forEach(change -> logger.warn(getReadableInfo(change)));
						success.set(false);

						// validate schema
						return client.validateSchema(expectedSchema).toSingle().map(resp -> {
							switch (resp.getStatus()) {
							case INVALID:
								logger.warn(String.format("Schema validation failed: %s", resp.getMessage().getMessage()));
								break;
							case VALID:
								logger.info("Schema validation succeeded");
								break;
							default:
								logger.warn("Schema validation returned unknown status");
								break;
							}
							return schema;
						});
					}
				}).blockingGet();
				schemaMap.put(schemaName, updatedSchema);
			}
			info("--");
		}

		micronodePublisher.init(checkMicroschemas, repair, success);

		// we cannot create an inexistent project, if schemas were not repaired successfully
		if (!success.get() && repair) {
			error("Skip checking project, since repairing schemas was not successful");
			return success.get();
		}

		// validate projects
		for (MeshProject project : projectMap.values()) {
			project.validate(schemaMap, repair, success);
		}

		// check which alternative projects exist
		alternativeProjects.removeIf(project -> !project.exists());

		// Since Mesh-SQL is case-insensitive when loading projects by name,
		// alternative projects with the same name apart from case which are
		// already in the project map must also be ignored.
		if (meshSql) {
			Set<String> projectNames = projectMap.values().stream()
				.map(p -> p.name)
				.filter(Objects::nonNull)
				.map(String::toLowerCase)
				.collect(Collectors.toSet());

			alternativeProjects.removeIf(p -> p.name == null || projectNames.contains(p.name.toLowerCase()));
		}

		for (MeshProject project : alternativeProjects) {
			project.validate(Collections.emptyMap(), false, new AtomicBoolean());
		}

		return success.get();
	}

	/**
	 * Get the datasources used by the permission property.
	 * Will throw a NodeException if either no permission property is set, or is set to an object property, that does not exist.
	 * If the object property exists, but does not use any Datasource (i.e. has no SelectPart) an empty Set is returned
	 * @return set of datasources, may be empty but never null
	 * @throws NodeException
	 */
	public Set<Datasource> getRoleDatasources() throws NodeException {
		return getRoleDatasources(getRoleObjectTagDefinitions());
	}

	/**
	 * Get the datasources used by the permission property.
	 * Will throw a NodeException if either no permission property is set, or is set to an object property, that does not exist.
	 * If the object property exists, but does not use any Datasource (i.e. has no SelectPart) an empty Set is returned
	 * @param objTagDefinitions definition list
	 * @return set of datasources, may be empty but never null
	 * @throws NodeException
	 */
	public Set<Datasource> getRoleDatasources(List<ObjectTagDefinition> objTagDefinitions) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (objTagDefinitions.isEmpty()) {
			throw new RestMappedException(I18NHelper.get("meshcr.permissionproperty.notfound", cr.getPermissionProperty()))
					.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}

		// collect the constructs used in the object properties
		Set<Construct> constructs = new HashSet<>();
		for (ObjectTagDefinition def : objTagDefinitions) {
			constructs.add(def.getObjectTag().getConstruct());
		}

		// check all constructs
		Set<Datasource> datasources = new HashSet<>();
		for (Construct construct : constructs) {
			for (Part part : construct.getParts()) {
				Value value = t.createObject(Value.class);
				value.setPart(part);
				PartType partType = part.getPartType(value);
				if (partType instanceof SelectPartType) {
					datasources.add(((SelectPartType) partType).getDatasource());
				}
			}
		}

		return datasources;
	}

	/**
	 * Get the object tag definitions used as roles. This will fail, if either the permission property is not set or no roles property exists
	 * @return list of object tag definitions
	 * @throws NodeException
	 */
	public List<ObjectTagDefinition> getRoleObjectTagDefinitions() throws NodeException {
		if (ObjectTransformer.isEmpty(cr.getPermissionProperty())) {
			throw new RestMappedException(I18NHelper.get("meshcr.permissionproperty.notset")).setMessageType(Message.Type.CRITICAL)
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		List<ObjectTagDefinition> objTagDefinitions = t.getObjects(ObjectTagDefinition.class,
				DBUtils.select("SELECT id FROM objtag WHERE obj_id = ? AND name = ?", ps -> {
					ps.setInt(1, 0);
					ps.setString(2, cr.getPermissionProperty());
				}, DBUtils.IDS));

		if (objTagDefinitions.isEmpty()) {
			throw new RestMappedException(I18NHelper.get("meshcr.permissionproperty.notfound", cr.getPermissionProperty()))
					.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}

		return objTagDefinitions;
	}

	/**
	 * Check setting of roles property.
	 *
	 * <p>
	 *     When the property string does <em>not</em> match a simple object
	 *     property (i.e. {@code object.OBJ_PROP_NAME}), the check is
	 *     automatically successful. Otherwise the following checks are
	 *     performed:
	 *     <ol>
	 *         <li>Check existence of object property</li>
	 *         <li>Check structure of construct</li>
	 *         <li>Check values in datasource</li>
	 *     </ol>
	 * </p>
	 * @param repair Ignored
	 * @return {@code true} when the check succeeds, and {@code false} otherwise.
	 * @throws NodeException
	 */
	public boolean checkRoles(boolean repair) throws NodeException {
		info(String.format("Check role property for '%s'", cr.getName()));

		String permissionProperty = cr.getPermissionProperty();

		if (ObjectTransformer.isEmpty(permissionProperty) || !PATTERN_ROLE_OBJECT_TAG.matcher(permissionProperty).matches()) {
			info(String.format("No role property set or property is not an object tag: %s", permissionProperty));
			return true;
		}

		// get the role datasources, which will check for existence of the object property
		try {
			boolean success = true;
			List<ObjectTagDefinition> objectTagDefinitions = getRoleObjectTagDefinitions();

			boolean multichannelling = NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING);
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
				for (ObjectTagDefinition def : objectTagDefinitions) {
					switch (def.getTargetType()) {
					case Page.TYPE_PAGE:
						if (multichannelling && !def.isSyncChannelset()) {
							error("ERROR: Role property must be synchronized for channels");
							success = false;
						}
						if (!def.isSyncContentset()) {
							error("ERROR: Role property must be synchronized for languages");
							success = false;
						}
						break;
					default:
						if (multichannelling && !def.isSyncChannelset()) {
							error("ERROR: Role property must be synchronized for channels");
							success = false;
						}
						break;
					}
				}
			} else {
				// TODO make this an error
			}

			getRoleDatasources(objectTagDefinitions);
			return success;
		} catch (NodeException e) {
			info(e.getLocalizedMessage());
			return false;
		}
	}

	/**
	 * Set the roles to the datasources. All other entries will be removed from the datasource entries
	 * @param datasources datasources, that will be modified
	 * @param roles list of roles to set
	 * @throws NodeException
	 */
	public void setRoles(Set<Datasource> datasources, List<String> roles) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		for (Datasource datasource : datasources) {
			Set<String> found = new HashSet<>();
			Set<String> remove = new HashSet<>();
			Set<String> change = new HashSet<>();
			int maxDsId = 0;
			for (DatasourceEntry entry : datasource.getEntries()) {
				String name = entry.getKey();
				String value = entry.getValue();
				maxDsId = Math.max(entry.getDsid(), maxDsId);

				if (!roles.contains(name)) {
					remove.add(name);
				} else {
					found.add(name);
					if (!StringUtils.isEqual(name, value)) {
						change.add(name);
					}
				}
			}

			Set<String> missing = roles.stream().filter(name -> !found.contains(name)).collect(Collectors.toSet());

			if (!remove.isEmpty() || !change.isEmpty() || !missing.isEmpty()) {
				Datasource editable = t.getObject(datasource, true);

				for (Iterator<DatasourceEntry> i = editable.getEntries().iterator(); i.hasNext();) {
					DatasourceEntry entry = i.next();
					String name = entry.getValue();

					if (remove.contains(name)) {
						i.remove();
					}
					if (change.contains(name)) {
						entry.setValue(name);
					}
				}

				for (String name : missing) {
					DatasourceEntry entry = t.createObject(DatasourceEntry.class);
					entry.setValue(name);
					entry.setKey(name);
					entry.setDsid(++maxDsId);
					editable.getEntries().add(entry);
				}

				// sort entries by name
				Collections.sort(editable.getEntries(), (e1, e2) -> e1.getKey().compareTo(e2.getKey()));
				editable.save();
				editable.unlock();
				info(String.format("Updated datasource %d", datasource.getId()));
			}
		}
	}

	/**
	 * Check existence of default role
	 * @param repair true to repair (not used, since roles in mesh will not be created)
	 * @return true iff valid (role is set and exists)
	 * @throws NodeException
	 */
	public boolean checkDefaultRole(boolean repair) throws NodeException {
		info(String.format("Check default role for '%s'", cr.getName()));
		if (!StringUtils.isEmpty(cr.getDefaultPermission())) {
			if (getRoleMap().containsKey(cr.getDefaultPermission())) {
				info(String.format("Role '%s' exists", cr.getDefaultPermission()));
				return true;
			} else {
				info(String.format("Role '%s' does not exist", cr.getDefaultPermission()));
				return false;
			}
		} else {
			info(String.format("No default role set."));
			return true;
		}
	}

	/**
	 * Check whether any jobs exist in Mesh, which are "RUNNING", "QUEUED" or "STARTING" and trigger job processing (just in case)
	 */
	public void triggerJobProcessing() {
		if (clientIsAdmin) {
			JobListResponse jobsList = client.findJobs().blockingGet();
			List<JobStatus> stati = Arrays.asList(JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.STARTING);
			if (jobsList.getData().stream().filter(job -> stati.contains(job.getStatus())).findAny().isPresent()) {
				info("Found jobs which are either QUEUED, STARTING or RUNNING. Invoking Job Processing.");
				client.invokeJobProcessing().blockingAwait();
			}
		} else {
			info("Client is no admin. Invoking of Job Processing not possible");
		}
	}

	/**
	 * Wait for possibly running schema migrations
	 * @throws NodeException
	 */
	public void waitForSchemaMigrations() throws NodeException {
		info(String.format("Wait for schema migration status for '%s'", cr.getName()));
		Map<String, Map<String, Long>> unmigratedSchemas = new HashMap<>();

		while (!checkSchemaStatus(unmigratedSchemas)) {
			try {
				Thread.sleep(POLL_INTERVAL_MS);
			} catch (InterruptedException e) {
				throw new NodeException(String.format("Interrupted while waiting for schema migration status to become %s", JobStatus.IDLE), e);
			}
		}
	}

	/**
	 * Read the schema status for the default branch and log (as info)
	 * @param unmigratedSchemas map of schema names to the timestamp, when they were detected to be unmigrated
	 * @return true if all schemas are migrated
	 * @throws NodeException when a migration failed
	 */
	protected boolean checkSchemaStatus(Map<String, Map<String, Long>> unmigratedSchemas) throws NodeException {
		boolean allDone = true;

		for (MeshProject project : projectMap.values()) {
			if (!project.checkSchemaMigrations(unmigratedSchemas)) {
				allDone = false;
			}
		}

		return allDone;
	}

	/**
	 * Check consistency
	 * @param repair true to repair inconsistencies (not yet implemented)
	 * @param result stringbuilder where result will be appended
	 * @return true if the CR is consistent
	 * @throws NodeException
	 */
	public boolean checkDataConsistency(boolean repair, StringBuilder result) throws NodeException {
		ConsistencyCheckResponse response;
		if (repair) {
			response = client.repairConsistency().blockingGet();
		} else {
			response = client.checkConsistency().blockingGet();
		}
		result.append(String.format("Mesh data is %s after %s\n", response.getResult(), repair ? "repair" : "check"));
		for (InconsistencyInfo info : response.getInconsistencies()) {
			result.append(String.format("%s\ton %s: %s\n", info.getSeverity(), info.getElementUuid(), info.getDescription()));
		}
		boolean consistent = response.getResult() == ConsistencyRating.CONSISTENT;

		NodeLogger logger = NodeLogger.getNodeLogger(MeshPublisher.class);

		try (NodeLogCollector collector = new NodeLogCollector(Level.INFO, logger)) {
			if (checkSchemasAndProjects(false, true)) {
				consistent &= checkObjectConsistency(repair, false);
				result.append(collector.getLog());
			} else {
				result.append("Not checking data, because projects/schemas are not valid");
				consistent = false;
			}
			return consistent;
		}
	}

	/**
	 * Wait for possibly running node migrations
	 * @throws NodeException
	 */
	public void waitForNodeMigrations() throws NodeException {
		info(String.format("Wait for node migration status for '%s'", cr.getName()));
		Map<String, Map<String, Long>> unmigratedBranchs = new HashMap<>();

		while (!checkBranchStatus(unmigratedBranchs)) {
			try {
				Thread.sleep(POLL_INTERVAL_MS);
			} catch (InterruptedException e) {
				throw new NodeException(String.format("Interrupted while waiting for node migrations"), e);
			}
		}
	}

	/**
	 * Read the branch status for all branches and log (as info)
	 * @param unmigratedBranchs map of branch names to the timestamp, when they were detected to be unmigrated
	 * @return true if all branches are migrated
	 * @throws NodeException when a migration failed
	 */
	protected boolean checkBranchStatus(Map<String, Map<String, Long>> unmigratedBranchs) throws NodeException {
		boolean allDone = true;

		for (MeshProject project : projectMap.values()) {
			if (!project.checkNodeMigrations(unmigratedBranchs.computeIfAbsent(project.name, name -> new HashMap<>()))) {
				allDone = false;
			}
		}

		return allDone;
	}

	/**
	 * Collect all current image variants.
	 * 
	 * @throws NodeException 
	 */
	public void collectImageData() throws NodeException {
		Publisher.writeFiles(publishInfo, cr.getNodes().stream().filter(Node::isPublishImageVariants).collect(Collectors.toList()), 
				allImageData, renderResult, false, Optional.empty(), Optional.empty(), node -> node.getFolder().getImages(new Folder.FileSearch().setRecursive(true)));
	}

	/**
	 * Create variants for image binaries, collected during the publish phase.
	 * 
	 * @throws NodeException
	 */
	public void createImageVariants() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		for (Node node : cr.getNodes()) {
			if (!node.isPublishImageVariants()) {
				continue;
			}
			int nodeId = node.getId();

			info(String.format("Creating variants of images at '%s' into '%s'", node, cr.getName()));

			// get images with stale variants and remove all variants from mesh
			List<ContentFile> files = CNGenticsImageStore.getStaleMeshPublishFiles(nodeId);
			for (ContentFile file : files) {
				String uuid = getMeshUuid(file);
				ImageManipulationRequest imageManipulationRequest = new ImageManipulationRequest().setVariants(Collections.emptyList()).setDeleteOther(true);

				MeshRequest<ImageVariantsResponse> request = client.upsertNodeBinaryFieldImageVariants(node.getMeshProject(), uuid, "binarycontent", imageManipulationRequest);
				request.getResponse()
						.doOnError(error -> error("Error during removal of image variants of %s (%s) / %s :\n %s", 
								uuid, "binarycontent", imageManipulationRequest, error))
						.doOnSuccess(creationResponse -> info("Variants creation for %s (%s) resulted in %d / %s", 
								uuid, "binarycontent", creationResponse.getStatusCode(), imageManipulationRequest))
						.blockingGet();
			}

			HashMap<Pair<String, String>, ImageManipulationRequest> uuidKeyRequests = new HashMap<>();

			List<ImageVariant> variants = CNGenticsImageStore.collectImageVariants(nodeId);
			for (ImageVariant variant : variants) {
				String fieldKey = "binarycontent";
				String webroot = variant.information.getFilePath();
				webroot = (StaticUrlFactory.ignoreSeparateBinaryPublishDir(node.getContentRepository()) && webroot.startsWith("/" + node.getBinaryPublishDir())) 
						? webroot.split(node.getBinaryPublishDir())[1] : webroot;
				webroot = (StaticUrlFactory.ignoreNodePublishDir(node.getContentRepository()) && webroot.startsWith("/" + node.getPublishDir())) 
						? webroot.split(node.getPublishDir())[1] : webroot;
				webroot = (webroot.startsWith("/" + node.getFolder().getPublishDir()))
						? webroot.split(node.getFolder().getPublishDir())[1] : webroot;

				String uuid;
				if (variant.information.getFileId() != 0) {
					uuid = getMeshUuid(t.getObject(ImageFile.class, variant.information.getFileId()));
				} else {
					uuid = null;
				}
				String transform = variant.description.transform;

				Matcher m = CNGenticsImageStore.TRANSFORM_PATTERN.matcher(transform);
				if (!m.matches()) {
					throw new NodeException("Couldn't parse " + transform);
				}
				ImageManipulationRequest imageManipulationRequest = uuidKeyRequests.computeIfAbsent(Pair.of(uuid != null ? uuid : webroot, fieldKey), key -> new ImageManipulationRequest().setVariants(new ArrayList<>()));
				imageManipulationRequest.setDeleteOther(true);

				ImageVariantRequest imageVariantRequest = new ImageVariantRequest();
				imageVariantRequest.setWidth(m.group("width"));
				imageVariantRequest.setHeight(m.group("height"));

				String mode = m.group("mode");
				imageVariantRequest.setResizeMode(StringUtils.isEmpty(mode) ? null : ResizeMode.get(mode) );

				String cropMode = m.group("cropmode");
				imageVariantRequest.setCropMode(StringUtils.isEmpty(cropMode) ? null : CropMode.get(cropMode));

				String topleft_x = m.group("tlx");
				String topleft_y = m.group("tly");
				String cropwidth = m.group("cw");
				String cropheight = m.group("ch");
				if (StringUtils.isInteger(topleft_x) && StringUtils.isInteger(topleft_y) && StringUtils.isInteger(cropwidth) && StringUtils.isInteger(cropheight)) {
					imageVariantRequest.setRect(Integer.parseInt(topleft_x), Integer.parseInt(topleft_y), Integer.parseInt(cropwidth), Integer.parseInt(cropheight));
				}
				imageManipulationRequest.getVariants().add(imageVariantRequest);
			}
			for (Entry<Pair<String, String>, ImageManipulationRequest> uuidKeyRequest : uuidKeyRequests.entrySet()) {
				MeshRequest<?> request;
				if (UUIDUtil.isUUID(uuidKeyRequest.getKey().getKey())) {
					request = client.upsertNodeBinaryFieldImageVariants(node.getMeshProject(), uuidKeyRequest.getKey().getKey(), uuidKeyRequest.getKey().getValue(), uuidKeyRequest.getValue());
				} else {
					request = client.upsertWebrootFieldImageVariants(node.getMeshProject(), uuidKeyRequest.getKey().getValue(), uuidKeyRequest.getKey().getKey(), uuidKeyRequest.getValue());
				}
				MeshResponse<?> response = request.getResponse()
					.doOnError(error -> error("Error during creation of image variants of %s (%s) / %s :\n %s", 
							uuidKeyRequest.getKey().getKey(), uuidKeyRequest.getKey().getValue(), uuidKeyRequest.getValue(), error))
					.doOnSuccess(creationResponse -> info("Variants creation for %s (%s) resulted in %d / %s", 
							uuidKeyRequest.getKey().getKey(), uuidKeyRequest.getKey().getValue(), creationResponse.getStatusCode(), uuidKeyRequest.getValue()))
					.blockingGet();
				debug("Image creation responded with: %d: %s", response.getStatusCode(), response.getBodyAsString());
			}
		}
	}

	/**
	 * Begin publishing dirted folders and files
	 * @param phase workphase handler
	 * @throws NodeException
	 */
	public void publishFoldersAndFiles(WorkPhaseHandler phase) throws NodeException {
		boolean reportToPublishQueue = NodeConfigRuntimeConfiguration.isFeature(Feature.RESUMABLE_PUBLISH_PROCESS) && cr.isInstantPublishing();

		for (Node node : cr.getNodes()) {
			int nodeId = node.getId();
			info(String.format("Publish folders and files of '%s' into '%s'", node, cr.getName()));

			if (PublishController.getState() != PublishController.State.running) {
				logger.debug(String.format("Stop publishing folders and files, because publisher state is %s", PublishController.getState()));
				return;
			}

			List<Scheduled> entries = new ArrayList<>();
			if (node.doPublishContentMapFolders()) {
				entries.addAll(Scheduled.from(nodeId, PublishQueue.getDirtedObjectsWithAttributes(Folder.class, node), reportToPublishQueue));
			}
			if (node.doPublishContentMapFiles()) {
				entries.addAll(Scheduled.from(nodeId, PublishQueue.getDirtedObjectsWithAttributes(File.class, node), reportToPublishQueue));
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.FORMS, node)) {
				entries.addAll(Scheduled.from(nodeId, PublishQueue.getDirtedObjectsWithAttributes(Form.class, node), reportToPublishQueue));
			}
			if (!entries.isEmpty()) {
				int numEntries = entries.size();
				if (reportToPublishQueue) {
					for (Scheduled scheduled : entries) {
						NodeObject object = scheduled.get().getObject();
						PublishQueue.initiatePublishAction(normalizeObjType(object.getTType()), object.getId(), nodeId, PublishAction.WRITE_CR);
					}
				}

				removeObjectsFromIncorrectProject(nodeId, entries, getProject(node));

				processQueue(entries, node, null, null);
				info(String.format("%d folders and files of '%s' have been queued for '%s'", numEntries, node, cr.getName()));
			}
		}
	}

	/**
	 * Get the number of folders dirted for publishing into the CR
	 * @return number of dirted folders
	 * @throws NodeException
	 */
	public int getNumDirtedFolders() throws NodeException {
		int totalCount = 0;
		for (Node node : cr.getNodes()) {
			if (node.doPublishContentMapFolders()) {
				totalCount += PublishQueue.countDirtedObjects(Folder.class, true, node);
			}
		}
		return totalCount;
	}

	/**
	 * Get the number of files dirted for publishing into the CR
	 * @return number of dirted files
	 * @throws NodeException
	 */
	public int getNumDirtedFiles() throws NodeException {
		int totalCount = 0;
		for (Node node : cr.getNodes()) {
			if (node.doPublishContentMapFiles()) {
				totalCount += PublishQueue.countDirtedObjects(File.class, true, node);
			}
		}
		return totalCount;
	}

	/**
	 * Get the number of forms dirted for publishing into the CR
	 * @return number of dirted forms
	 * @throws NodeException
	 */
	public int getNumDirtedForms() throws NodeException {
		int totalCount = 0;
		for (Node node : cr.getNodes()) {
			if (node.doPublishContentmap()) {
				totalCount += PublishQueue.countDirtedObjects(Form.class, true, node);
			}
		}
		return totalCount;
	}

	/**
	 * Publish the page into mesh
	 * @param node node
	 * @param page page
	 * @param tagmapEntries map of rendered tagmap entries
	 * @param source page source
	 * @param attributes optional set of attributes to be written
	 * @param reportToPublishQueue true to report back to the publish queue when done
	 * @throws NodeException
	 */
	public void publishPage(Node node, Page page, Map<TagmapEntryRenderer, Object> tagmapEntries, String source, Set<String> attributes, boolean reportToPublishQueue)
			throws NodeException {
		Scheduled scheduledPage = Scheduled.from(node.getId(), new NodeObjectWithAttributes<>(page, attributes), reportToPublishQueue);
		// prepare the source as tagmap entry "content"
		if (ObjectTransformer.isEmpty(attributes) || attributes.contains("content")) {
			TagmapEntryRenderer contentEntry = tagmapEntries.keySet().stream()
					.filter(e -> "content".equals(e.getMapname()) && StringUtils.isEmpty(e.getTagname())).findFirst().orElse(null);
			if (contentEntry != null) {
				tagmapEntries.put(contentEntry, source);
			}
		}
		scheduledPage.tagmapEntries = tagmapEntries;

		removeObjectsFromIncorrectProject(node.getId(), Collections.singleton(scheduledPage), getProject(node));
		processQueue(Collections.singleton(scheduledPage), node, null, null);
	}

	/**
	 * Handle postponed updates
	 * @param phase workphase handler
	 * @throws NodeException
	 */
	public void handlePostponedUpdates(WorkPhaseHandler phase) throws NodeException {
		if (controller == null) {
			throw new NodeException("Postponed updates can only be handled, when run in a Publish process");
		}
		info(String.format("Handle postponed updates into '%s'", cr.getName()));

		for (WriteTask task : postponedTasks) {
			// we can postpone a single task at most once
			task.postponable = false;
			if (PublishController.getState() != PublishController.State.running) {
				logger.debug(String.format("Stop handling postponed updates, because publisher state is %s", PublishController.getState()));
				return;
			}
			logger.debug(String.format("Handling postponed update of %d.%d", task.objType, task.objId));
			if (task.postponed != null) {
				handleRenderedEntries(task.nodeId, task.objId, task.objType, task.postponed, null, fields -> task.fields = fields, null, null, null);
			}
			if (!task.fields.isEmpty()) {
				task.postponed = null;
				controller.putWriteTask(task);
			} else {
				logger.debug(String.format("Postponed update of %d.%d still cannot be done, ignoring", task.objType, task.objId));
				task.reportDone();
			}
			phase.work();
		}
		postponedTasks.clear();
	}

	/**
	 * Remove offline objects
	 * @param repair true to repair (remove incorrect objects)
	 * @param publishProcess true if this is running during a publish process
	 * @return true if everything was consistent or was repaired
	 * @throws NodeException
	 */
	public boolean checkObjectConsistency(boolean repair, boolean publishProcess) throws NodeException {
		if (publishProcess && (PublishController.getState() != PublishController.State.running)) {
			logger.debug(String.format("Stop checking offline objects, because publisher state is %s", PublishController.getState()));
			return false;
		}

		if (publishProcess) {
			// remove root folders in alternative projects
			Set<String> schemas = schemaNames.keySet().stream().map(this::getSchemaName).collect(Collectors.toSet());
			for (MeshProject project : alternativeProjects) {
				NodeListResponse nodeList = client.findNodeChildren(project.name, project.rootNodeUuid,
						new GenericParametersImpl().setETag(false).setFields("schema", "uuid")).blockingGet();
				for (NodeResponse node : nodeList.getData()) {
					if (controller.publishProcess && (PublishController.getState() != PublishController.State.running)) {
						logger.debug(String.format("Stop checking offline objects, because publisher state is %s", PublishController.getState()));
						return false;
					}

					if (schemas.contains(node.getSchema().getName())) {
						client.deleteNode(project.name, node.getUuid(), new DeleteParametersImpl().setRecursive(true)).blockingAwait();
					}
				}
			}
		}

		info(String.format("Checking objects in '%s'", cr.getName()));

		// prepare maps of mesh UUIDs to sets of internal IDs, which will be used to check, whether objects in Mesh still exist in the CMS
		// and should still be published into the Mesh CR. We do not rely on the cms_id stored with the Mesh object, because when objects are
		// localized in channels, the cms_id of the object in the Mesh branch will change.
		Map<Integer, Map<String, Set<Integer>>> meshUuidMap = prepareMeshUuidMap();
		boolean consistent = true;
		for (MeshProject project : projectMap.values()) {
			consistent &= checkObjectConsistency(project, repair, publishProcess, meshUuidMap);
		}

		return consistent;
	}

	/**
	 * Remove offline objects from the project
	 * @param project mesh project
	 * @param repair true to repair (remove incorrect objects)
	 * @param publishProcess true if this is running during a publish process
	 * @param meshUuidMap map of meshUuids to local IDs
	 * @return true if everything was consistent or was repaired
	 * @throws NodeException
	 */
	protected boolean checkObjectConsistency(MeshProject project, boolean repair, boolean publishProcess, Map<Integer, Map<String, Set<Integer>>> meshUuidMap) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean consistent;

		try (PublishCacheTrx trx = new PublishCacheTrx(false)) {
			consistent = checkObjectConsistency(project, project.defaultBranchParameter, null, repair, publishProcess, meshUuidMap);

			for (Entry<Integer, VersioningParameters> entry : project.branchParamMap.entrySet()) {
				Node channel = t.getObject(Node.class, entry.getKey(), -1, false);
				consistent &= checkObjectConsistency(project, entry.getValue(), channel, repair, publishProcess, meshUuidMap);
			}
		}

		return consistent;
	}

	/**
	 * Remove offline objects from the project
	 * @param project mesh project
	 * @param branch branch parameter
	 * @param node optional channel
	 * @param repair true to repair (remove incorrect objects)
	 * @param publishProcess true if this is running during a publish process
	 * @param meshUuidMap map of meshUuids to local IDs
	 * @return true if everything was consistent or was repaired
	 * @throws NodeException
	 */
	protected boolean checkObjectConsistency(MeshProject project, VersioningParameters branch, Node node,
			boolean repair, boolean publishProcess, Map<Integer, Map<String, Set<Integer>>> meshUuidMap) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!publishProcess) {
			return true;
		}

		Node checkedNode = node;
		if (checkedNode == null) {
			checkedNode = project.node;
		}

		// when the checked node is null, the Mesh CR does not have "project per node" activated, so we need to check for all nodes, which are currently assigned to the Mesh CR
		if (checkedNode == null && !cr.isProjectPerNode()) {
			List<Node> nodes = cr.getNodes();
			if (!CollectionUtils.isEmpty(nodes)) {
				boolean consistent = true;
				for (Node n : nodes) {
					consistent &= checkObjectConsistency(project, branch, n, repair, publishProcess, meshUuidMap);
				}
				return consistent;
			} else {
				return true;
			}
		}

		Set<Integer> types = new HashSet<>(schemaNames.keySet());
		// also check forms, if the node is no channel and has the feature activated
		if (checkedNode != null && !checkedNode.isChannel() && NodeConfigRuntimeConfiguration.isFeature(Feature.FORMS, checkedNode)) {
			types.add(Form.TYPE_FORM);
		}
		for (int objectType : types) {
			Class<? extends NodeObject> clazz = t.getClass(objectType);

			Map<Integer, Set<String>> map = PublishQueue.getObjectIdsWithAttributes(clazz, true, checkedNode, Action.DELETE, Action.REMOVE, Action.OFFLINE);

			if (checkedNode != null) {
				Trx.consume(nodeId -> CNGenticsImageStore.removeFromMeshPublish(nodeId, objectType, map.keySet()), checkedNode.getId());
			}

			// for forms, check which ones still exist in the CMS but are offline and take them offline in Mesh (instead of deleting)
			if (objectType == Form.TYPE_FORM) {
				List<Form> forms = t.getObjects(Form.class, map.keySet());
				for (Form form : forms) {
					if (controller.publishProcess && (PublishController.getState() != PublishController.State.running)) {
						logger.debug(String.format("Stop checking offline objects, because publisher state is %s", PublishController.getState()));
						return false;
					}

					map.remove(form.getId());
					if (!cr.mustContain(form)) {
						String meshUuid = getMeshUuid(form);
						getExistingFormLanguages(project, meshUuid).flatMapCompletable(languages -> {
							for (String lang : languages) {
								offline(project, branch, objectType, meshUuid, lang);
							}
							return Completable.complete();
						}).blockingAwait();
					}
				}
			}
			Map<String, Set<String>> toDelete = new HashMap<>();
			for (Map.Entry<Integer, Set<String>> entry : map.entrySet()) {
				if (controller.publishProcess && (PublishController.getState() != PublishController.State.running)) {
					logger.debug(String.format("Stop checking offline objects, because publisher state is %s", PublishController.getState()));
					return false;
				}

				String meshUuid = null;
				String meshLanguage = null;
				if (entry.getValue() != null) {
					for (String value : entry.getValue()) {
						if (org.apache.commons.lang3.StringUtils.startsWith(value, "uuid:")) {
							meshUuid = org.apache.commons.lang3.StringUtils.removeStart(value, "uuid:");
						} else if (org.apache.commons.lang3.StringUtils.startsWith(value, "language:")) {
							meshLanguage = org.apache.commons.lang3.StringUtils.removeStart(value, "language:");
						}
					}
				}
				if (meshUuid != null) {
					toDelete.computeIfAbsent(meshUuid, k -> new HashSet<>()).add(meshLanguage);
				}
			}

			// check whether we really want to delete the object in Mesh
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				List<? extends NodeObject> objects = fromMeshUuid(clazz,
						meshUuidMap.getOrDefault(objectType, Collections.emptyMap()), toDelete.keySet());
				for (NodeObject object : objects) {
					if (controller.publishProcess && (PublishController.getState() != PublishController.State.running)) {
						logger.debug(String.format("Stop checking offline objects, because publisher state is %s", PublishController.getState()));
						return false;
					}

					if (cr.mustContain(object, checkedNode)) {
						toDelete.remove(getMeshUuid(object));
					}
				}
			}

			for (Entry<String, Set<String>> entry : toDelete.entrySet()) {
				if (controller.publishProcess && (PublishController.getState() != PublishController.State.running)) {
					logger.debug(String.format("Stop checking offline objects, because publisher state is %s", PublishController.getState()));
					return false;
				}

				String meshUuid = entry.getKey();
				Set<String> languages = entry.getValue();
				for (String language : languages) {
					remove(project, checkedNode, objectType, meshUuid, language, true);
				}
			}
		}

		return true;
	}

	/**
	 * Remove the given list of objects from the node
	 * @param node node
	 * @param objectsToDelete list of objects to remove
	 * @throws NodeException
	 */
	public void remove(Node node, List<NodeObject> objectsToDelete) throws NodeException {
		MeshProject project = getProject(node);
		for (NodeObject nodeObject : objectsToDelete) {
			remove(project, node, nodeObject.getTType(), getMeshUuid(nodeObject), getMeshLanguage(nodeObject));
		}
	}

	/**
	 * Try to remove an object from mesh
	 * @param project mesh project
	 * @param node node from which to remove the object
	 * @param objectType object type
	 * @param meshUuid mesh UUID
	 * @param meshLanguage mesh Language (null to delete all language variants)
	 * @throws NodeException
	 */
	public void remove(MeshProject project, Node node, int objectType, String meshUuid, String meshLanguage) throws NodeException {
		remove(project, node, objectType, meshUuid, meshLanguage, true);
	}

	/**
	 * Try to remove an object from mesh
	 * @param project mesh project
	 * @param node node from which to remove the object
	 * @param objectType object type
	 * @param meshUuid mesh UUID
	 * @param meshLanguage mesh Language (null to delete all language variants)
	 * @param withSemaphore whether the acquire a semaphore
	 * @throws NodeException
	 */
	public void remove(MeshProject project, Node node, int objectType, String meshUuid, String meshLanguage, boolean withSemaphore) throws NodeException {
		if (cr.isProjectPerNode()) {
			remove(project, project.enforceBranch(node.getId()), objectType, meshUuid, meshLanguage, withSemaphore);
		} else {
			remove(project, (VersioningParameters)null, objectType, meshUuid, meshLanguage, withSemaphore);
		}
	}

	/**
	 * Try to remove an object from mesh
	 * @param project mesh project
	 * @param branch optional branch parameter
	 * @param objectType object type
	 * @param meshUuid mesh UUID
	 * @param meshLanguage mesh Language (null to delete all language variants)
	 * @throws NodeException
	 */
	public void remove(MeshProject project, VersioningParameters branch, int objectType, String meshUuid, String meshLanguage) throws NodeException {
		remove(project, branch, objectType, meshUuid, meshLanguage, true);
	}

	/**
	 * Try to remove an object from mesh
	 * @param project mesh project
	 * @param branch optional branch parameter
	 * @param objectType object type
	 * @param meshUuid mesh UUID
	 * @param meshLanguage mesh Language (null to delete all language variants)
	 * @param withSemaphore whether the acquire a semaphore
	 * @throws NodeException
	 */
	public void remove(MeshProject project, VersioningParameters branch, int objectType, String meshUuid, String meshLanguage, boolean withSemaphore) throws NodeException {
		if (withSemaphore) {
			semaphoreMap.acquire(lockKey, callTimeout, TimeUnit.SECONDS);
		}
		try {
			logger.debug(String.format("Start removing %d.%s", objectType, meshUuid));
			if (meshLanguage != null) {
				// delete the language version
				if (branch != null) {
					client.deleteNode(project.name, meshUuid, meshLanguage, new DeleteParametersImpl().setRecursive(true), branch).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				} else {
					client.deleteNode(project.name, meshUuid, meshLanguage, new DeleteParametersImpl().setRecursive(true)).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				}
			} else if (objectType == Form.TYPE_FORM) {
				// forms are removed via the forms plugin
				client.deleteEmpty(String.format("/%s/plugins/forms/forms/%s", encodeSegment(project.name), meshUuid))
						.toCompletable().onErrorResumeNext(throwable -> {
							return ifNotFound(throwable, () -> Completable.complete());
						}).blockingAwait();
			} else {
				// delete the node
				if (branch != null) {
					client.deleteNode(project.name, meshUuid, new DeleteParametersImpl().setRecursive(true), branch).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				} else {
					client.deleteNode(project.name, meshUuid, new DeleteParametersImpl().setRecursive(true)).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				}
			}
		} catch (Throwable t) {
			if (meshLanguage != null) {
				throw new NodeException(String.format("Error while removing language %s of object of type %d with uuid %s", meshLanguage, objectType, meshUuid), t);
			} else {
				throw new NodeException(String.format("Error while removing object of type %d with uuid %s", objectType, meshUuid), t);
			}
		} finally {
			logger.debug(String.format("Finished removing %d.%s", objectType, meshUuid));
			if (withSemaphore) {
				semaphoreMap.release(lockKey);
			}
		}
	}

	/**
	 * Try to take an object offline in mesh
	 * @param project mesh project
	 * @param branch optional branch parameter
	 * @param objectType object type
	 * @param meshUuid mesh UUID
	 * @param meshLanguage mesh Language (null to delete all language variants)
	 * @throws NodeException
	 */
	public void offline(MeshProject project, VersioningParameters branch, int objectType, String meshUuid, String meshLanguage) throws NodeException {
		semaphoreMap.acquire(lockKey, callTimeout, TimeUnit.SECONDS);
		try {
			logger.debug(String.format("Start taking %d.%s offline", objectType, meshUuid));
			if (objectType == Page.TYPE_PAGE && meshLanguage != null) {
				// for pages, we take offline the language version
				if (branch != null) {
					client.takeNodeLanguageOffline(project.name, meshUuid, meshLanguage, branch).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				} else {
					client.takeNodeLanguageOffline(project.name, meshUuid, meshLanguage).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				}
			} else if (objectType == Form.TYPE_FORM) {
				if (meshLanguage != null) {
					// forms are taken offline via the forms plugin
					client.deleteEmpty(String.format("/%s/plugins/forms/forms/%s/online/%s",
							encodeSegment(project.name), meshUuid, encodeSegment(meshLanguage))).toCompletable()
							.onErrorResumeNext(throwable -> {
								return ifNotFound(throwable, () -> Completable.complete());
							}).blockingAwait();
				}
			} else {
				// for folders and files, there is only one language version, so take the node offline
				if (branch != null) {
					client.takeNodeOffline(project.name, meshUuid, branch).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				} else {
					client.takeNodeOffline(project.name, meshUuid).toCompletable().onErrorResumeNext(throwable -> {
						return ifNotFound(throwable, () -> Completable.complete());
					}).blockingAwait();
				}
			}
		} catch (Throwable t) {
			if (objectType == Page.TYPE_PAGE && meshLanguage != null) {
				throw new NodeException(String.format("Error while taking language %s of object of type %d with uuid %s offline", meshLanguage, objectType, meshUuid), t);
			} else {
				throw new NodeException(String.format("Error while taking object of type %d with uuid %s offline", objectType, meshUuid), t);
			}
		} finally {
			logger.debug(String.format("Finished taking %d.%s offline", objectType, meshUuid));
			semaphoreMap.release(lockKey);
		}
	}

	/**
	 * Check whether the mesh publisher had an error
	 * @throws NodeException
	 */
	public void checkForErrors() throws NodeException {
		if (error) {
			RenderResult renderResult = TransactionManager.getCurrentTransaction().getRenderResult();
			if (renderResult != null) {
				for (Throwable t : throwables) {
					renderResult.error(MeshPublisher.class, "Error caught while publishing", t);
				}
			}

			throw new NodeException(String.format("Publishing into Mesh CR %s failed with an error", cr.getName()));
		}
	}

	@Override
	public void close() {
		// logout from Mesh
		if (client != null) {
			client.logout().blockingGet();
			client.close();
			client = null;
		}

		logger.debug(String.format("%d requests to load single node", getNodeCounter.get()));
		logger.debug(String.format("%d requests to create/update single node", saveNodeCounter.get()));
		logger.debug(String.format("%d graphql requests", graphQlCounter.get()));
	}

	/**
	 * Process the entries in a queue. Entries that cannot be handled right now (because their parent folder does not exist in Mesh) will be repaced
	 * by their dependency and be postponed (put back into the end of the queue)
	 * @param entries entries to handle
	 * @param node node for which the entries shall be handled
	 * @param phase workphase handler
	 * @param dependencies optional list, which - when not null - will be filled with dependencies found while rendering pages/folders
	 * @throws NodeException
	 */
	public void processQueue(Collection<Scheduled> entries, Node node, WorkPhaseHandler phase, List<Dependency> dependencies) throws NodeException {
		if (controller == null) {
			throw new NodeException("Queue can only be handled, when run in a Publish process");
		}
		int nodeId = node.getId();
		Queue<Scheduled> queue = new ArrayDeque<>(entries);
		MeshProject project = getProject(node);

		// if files may be offline, we need to check for that
		final FileListForNode fileList = controller.publishProcess ? FileOnlineStatus.prepareForNode(node) : null;

		try (HandleDependenciesTrx depTrx = new HandleDependenciesTrx(false)) {
			Scheduled next = null;
			while ((next = queue.poll()) != null) {
				if (controller.publishProcess && (PublishController.getState() != PublishController.State.running)) {
					logger.debug(String.format("Stop processing the queue, because publisher state is %s", PublishController.getState()));
					return;
				}
				if (!controller.publishProcess) {
					checkForErrors();
				}
				logger.debug(String.format("Fetched %s from queue", next.get().getObject()));

				// omit offline files
				if (fileList != null && next.wrapped.getObject() instanceof File && !fileList.isOnline((File) next.wrapped.getObject())) {
					logger.debug(String.format("Omit offline file %s", next.wrapped.getObject()));
					if (phase != null) {
						phase.work();
					}
				} else if (next.wrapped.getObject() instanceof Form) {
					// publish a form
					Form form = (Form) next.wrapped.getObject();
					logger.debug(String.format("Handling %s", form));
					int objType = form.getTType();
					int objId = form.getId();
					Map<Integer, Set<Integer>> nodeMap = handled.computeIfAbsent(nodeId, key -> new ConcurrentHashMap<>());
					Set<Integer> idSet = nodeMap.computeIfAbsent(objType, key -> new HashSet<>());
					boolean reportToPublishQueue = next.reportToPublishQueue;

					// already handled
					if (idSet.contains(objId)) {
						logger.debug(String.format("%s was already done before", form));
						next.reportDone(publishInfo);
						return;
					}

					logger.debug(String.format("Submit task to render %s", form));
					controller.runRenderTask(() -> {
						try {
							Trx.operate(trx -> {
								try (LangTrx lTrx = new LangTrx("en");
										RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, form, controller.publishProcess,
												controller.publishProcess, controller.publishProcess)) {

									ObjectNode data = (ObjectNode) form.getData();
									JsonNode downloadUrl = data == null ? null : data.get("downloadBaseUrl");

									if (downloadUrl == null || !downloadUrl.isTextual() || StringUtils.isEmpty(downloadUrl.asText())) {
										NodePreferences prefs = trx.getNodeConfig().getDefaultPreferences();
										String downloadBaseUrl = String.format(
											"%s/editor/#proxy%srest/form/%d/data",
											prefs.getProperty("cn_external_server"),
											prefs.getProperty("portletapp_prefix"),
											form.getId());

										data.put("downloadBaseUrl", downloadBaseUrl);
									}

									FormWriteTask task = new FormWriteTask(form, nodeId, this);
									// task.project is the current project of the node, not necessarily the desired project
									task.project = project;
									// target project is the desired project
									task.targetProject = project;
									task.reportToPublishQueue = reportToPublishQueue;

									logger.debug(String.format("Put task %s into queue", task));
									controller.putWriteTask(task);
								}
							});
							controller.renderTasks.finish();
						} catch (Throwable e) {
							errorHandler.accept(new NodeException(
									String.format("Error while preparing '%s' for publishing into '%s'", form, cr.getName()), e));
						}
					});
					if (phase != null) {
						phase.work();
					}
				} else {
					handle(project, nodeId, next, (scheduled, meshObject) -> {
						NodeObjectWithAttributes<? extends NodeObject> o = scheduled.get();
						logger.debug(String.format("Submit task to render %s", o));
						controller.runRenderTask(() -> {
							logger.debug(String.format("Rendering %s", o));
							try {
								Trx.operate(() -> {
									WriteTask task = generateWriteTask(node, project, scheduled, meshObject, dependencies);

									logger.debug(String.format("Put task %s into queue", task));
									controller.putWriteTask(task);
								});
								controller.renderTasks.finish();
							} catch (Throwable e) {
								errorHandler.accept(new NodeException(
										String.format("Error while preparing '%s' for publishing into '%s'", o.getObject(), cr.getName()), e));
							}
						});
						if (phase != null) {
							phase.work();
						}
					}, scheduled -> {
						logger.debug(String.format("Put back %s into queue", scheduled.wrapped));
						queue.add(scheduled);
						if (phase != null) {
							phase.addWork();
						}
					});
				}
			}
		}
	}

	/**
	 * Generate the write task for the given scheduled object
	 * @param node node
	 * @param project mesh project
	 * @param scheduled scheduled object
	 * @param meshObject mesh object
	 * @param dependencies optional dependencies
	 * @return write task
	 * @throws NodeException
	 */
	protected WriteTask generateWriteTask(Node node, MeshProject project, Scheduled scheduled, MeshObject meshObject, List<Dependency> dependencies) throws NodeException {
		NodeObjectWithAttributes<? extends NodeObject> o = scheduled.get();
		int nodeId = node.getId();

		// dependencies need to be handled during the publish process, when the object is not pre-rendered
		// or when instant publishing and the feature "contentfile_autooffline" is true
		boolean handleDependencies = (controller.publishProcess
				|| NodeConfigRuntimeConfiguration.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE)) && scheduled.tagmapEntries == null;

		// depencencies need to be stored when they are handled during a publish process
		boolean storeDependencies = handleDependencies && controller.publishProcess;

		try (LangTrx lTrx = new LangTrx("en");
				ChannelTrx cTrx2 = new ChannelTrx(node);
				PublishCacheTrx pTrx = new PublishCacheTrx(controller.publishProcess);
				PublishedNodeTrx pnTrx = TransactionManager.getCurrentTransaction().initPublishedNode(node);
				RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, o.getObject(), handleDependencies,
						storeDependencies, controller.publishProcess)) {
			WriteTask task = new WriteTask();
			// task.project is the current project of the node, not necessarily the desired project
			task.project = meshObject != null ? meshObject.project : project;
			// target project is the desired project
			task.targetProject = project;
			task.reportToPublishQueue = scheduled.reportToPublishQueue;
			task.exists = meshObject != null;
			task.objType = o.getObject().getTType();
			task.objId = o.getObject().getId();
			task.description = o.getObject().toString();
			task.nodeId = nodeId;
			task.schema = getSchemaName(task.objType);
			task.uuid = getMeshUuid(o.getObject());
			NodeObject mother = o.getObject().getParentObject();
			if (StringUtils.isEqual(task.uuid, project.rootNodeUuid)) {
				task.parentUuid = null;
				task.folderId = 0;
			} else if (mother == null || (cr.isProjectPerNode() && mother.getParentObject() == null)) {
				task.parentUuid = project.rootNodeUuid;
				if (mother != null) {
					task.folderId = mother.getId();
				}
			} else {
				task.parentUuid = getMeshUuid(mother);
				if (mother != null) {
					task.folderId = mother.getId();
				}
			}
			task.language = scheduled.language;
			if (supportsAlternativeLanguages(task.objType)) {
				task.alternativeMeshLanguages = getAlternativeMeshLanguages(o.getObject());
			}

			Map<TagmapEntryRenderer, Object> renderedEntries = scheduled.tagmapEntries;
			if (renderedEntries == null) {
				// render the tagmap entries.
				renderedEntries = render(getEntries(o.getObject().getTType()),
						o.getAttributes(), scheduled.getRenderedLanguage());
			}

			// if the rendered object is the Root-Folder and we have "projectPerNode" enabled, we need to remove the pub_dir
			if (cr.isProjectPerNode() && o.getObject() instanceof Folder && ((Folder)o.getObject()).isRoot()) {
				renderedEntries.keySet().removeIf(e -> "folder.pub_dir".equals(e.getTagname()));
			}

			handleRenderedEntries(task.nodeId, task.objId, task.objType, renderedEntries, o.getAttributes(), fields -> {
				task.fields = fields;
			}, roles -> {
				task.roles = roles;
			}, postponed -> {
				task.postponed = postponed;
			}, func -> {
				if (func != null) {
					task.addPostSave(func);
				}
			});

			task.publisher = this;

			// postSave for updating binary data
			if (o.getObject() instanceof File) {
				File file = (File) o.getObject();
				boolean uploadBinary = (ObjectTransformer.isEmpty(o.getAttributes()) || o.getAttributes().contains("binarycontent")) && file.getFilesize() > 0;
				boolean setFocalPointInfo = file.isImage();

				if (uploadBinary) {
					// add dependencies
					RenderType renderType = rTrx.get();
					renderType.setRenderedProperty("binarycontent");
					renderType.addDependency(file, "name");
					renderType.addDependency(file, "type");
					renderType.addDependency(file, "filesize");

					task.addPostSave(resp -> {
						return Trx.supply(() -> {
							InputStream in = file.getFileStream();
							if (in == null) {
								throw new NodeException(String.format("Cannot publish %s, binary data not found", file));
							}

							if (supportsPublishOnCreate) {
								return client.updateNodeBinaryField(
										task.project.name,
										task.uuid,
										getMeshLanguage(file),
										"draft",
										"binarycontent",
										in,
										file.getFilesize(),
										file.getFilename(),
										file.getFiletype(),
										true,
										task.project.enforceBranch(task.nodeId))
									.toSingle();
							}

							return client.updateNodeBinaryField(
									task.project.name,
									task.uuid,
									getMeshLanguage(file),
									"draft",
									"binarycontent",
									in,
									file.getFilesize(),
									file.getFilename(),
									file.getFiletype(),
									task.project.enforceBranch(task.nodeId))
								.toSingle();
						});
					});
				}

				if (setFocalPointInfo) {
					ImageFile image = (ImageFile) file;
					task.addPostSave(resp -> {
						return Trx.supply(() -> {
							if (needFocalPointUpdate(resp, image)) {
								NodeUpdateRequest update = new NodeUpdateRequest();
								update.setLanguage(getMeshLanguage(image));
								update.setVersion("draft");

								if (supportsPublishOnCreate) {
									update.setPublish(true);
								}

								update.getFields().put("binarycontent", new BinaryFieldImpl().setFocalPoint(image.getFpX(), image.getFpY()));
								return client.updateNode(task.project.name, task.uuid, update, task.project.enforceBranch(task.nodeId))
										.toSingle();
							} else {
								return Single.just(resp);
							}
						});
					});
				}
			}

			// when an object is instant published, and autooffline is used, we need to collect all dependencies
			if (dependencies != null && !controller.publishProcess && NodeConfigRuntimeConfiguration.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE)
					&& (task.objType == Page.TYPE_PAGE || task.objType == Folder.TYPE_FOLDER)) {
				dependencies.addAll(rTrx.get().getDependencies());
			}
			return task;
		}
	}

	/**
	 * Remove all objects in the collection from their current project, if it is not the given project
	 * TODO: when mesh supports moving of objects between projects, remove this method
	 * @param nodeId node ID
	 * @param entries collection of entries
	 * @param project project
	 * @throws NodeException
	 */
	protected void removeObjectsFromIncorrectProject(int nodeId, Collection<Scheduled> entries, MeshProject project) throws NodeException {
	}

	/**
	 * Get the role map: name -> uuid
	 * @return map of roles
	 */
	public Map<String, String> getRoleMap() {
		if (roleMap == null) {
			roleMap = Collections.synchronizedMap(new HashMap<>(roleMapSingle.blockingGet()));
		}
		return roleMap;
	}

	/**
	 * Get current mesh server version
	 * @return server version
	 */
	public CmpProductVersion getMeshServerVersion() {
		return new CmpProductVersion(serverInfo.blockingGet().getMeshVersion());
	}

	/**
	 * Log the message as debug to the renderresult, if set
	 * @param message message
	 */
	protected void debug(String message, Object...objects) {
		if (renderResult != null) {
			renderResult.debug(MeshPublisher.class, objects.length == 0 ? message : String.format(message, objects), null, null);
		}
	}

	/**
	 * Log the message as info to the renderresult, if set
	 * @param message message
	 */
	protected void info(String message, Object...objects) {
		if (renderResult != null) {
			renderResult.info(MeshPublisher.class, objects.length == 0 ? message : String.format(message, objects), null, null);
		}
	}

	/**
	 * Log the message as error to the renderresult, if set
	 * @param message message
	 */
	protected void error(String message, Object...objects) {
		String logged = objects.length == 0 ? message : String.format(message, objects);
		if (renderResult != null) {
			renderResult.error(MeshPublisher.class, logged, null, null);
		}
		if (publishInfo != null) {
			publishInfo.addMessage(new DefaultNodeMessage(Level.ERROR, MeshPublisher.class, cr.getName() + ": " + logged));
		}
	}

	/**
	 * Should 'gtx_url' field be considered for the given ContentRepository.
	 * @return true, if at least one node in the repository did not enable Publish Directory Segments.
	 */
	protected boolean isConsiderGtxUrlField() throws NodeException {
		return ContentRepositoryModel.Type.mesh == cr.getCrType() && cr.getNodes().stream().anyMatch(node -> !node.isPubDirSegment());
	}

	/**
	 * Get the nodes, which publish into the CR, filtered by their setting for "Publish Directory Segment"
	 * @param publishDirSegment filter flag
	 * @return set of nodes
	 * @throws NodeException
	 */
	protected Set<Node> getNodes(boolean publishDirSegment) throws NodeException {
		return cr.getNodes().stream().filter(node -> node.isPubDirSegment() == publishDirSegment).collect(Collectors.toSet());
	}

	/**
	 * Add field for SEO URLs support, if the type is either Page or File.
	 * @param type node type to check against
	 * @param container schema to add the field to
	 * @return true, when the field was added, false if not
	 */
	protected boolean addGtxUrlFieldSchema(int type, FieldSchemaContainer container) {
		if (Page.TYPE_PAGE == type || File.TYPE_FILE == type) {
			FieldSchema gtxUrlfieldSchema = new StringFieldSchemaImpl();

			gtxUrlfieldSchema.setName(FIELD_GTX_URL);
			gtxUrlfieldSchema.setElasticsearch(null);
			gtxUrlfieldSchema.setLabel("Gentics URL");

			container.addField(gtxUrlfieldSchema);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Transform the tagmap entry into the field schema and add it to the container
	 * @param container field schema container
	 * @param entry tagmap entry
	 */
	protected void addFieldSchema(FieldSchemaContainer container, TagmapEntry entry) {
		FieldSchema fieldSchema = null;

		switch (entry.getAttributetype()) {
		case text:
			if (entry.isMultivalue()) {
				fieldSchema = new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString());
			} else {
				fieldSchema = new StringFieldSchemaImpl();
			}
			break;
		case link:
			if (entry.isMultivalue()) {
				fieldSchema = new ListFieldSchemaImpl().setListType(FieldTypes.NODE.toString()).setAllowedSchemas(getSchemaName(entry.getTargetType()));
			} else {
				fieldSchema = new NodeFieldSchemaImpl().setAllowedSchemas(getSchemaName(entry.getTargetType()));
			}
			break;
		case integer:
			if (entry.isMultivalue()) {
				fieldSchema = new ListFieldSchemaImpl().setListType(FieldTypes.NUMBER.toString());
			} else {
				fieldSchema = new NumberFieldSchemaImpl();
			}
			break;
		case binary:
			fieldSchema = new BinaryFieldSchemaImpl();
			break;
		case date:
			if (entry.isMultivalue()) {
				fieldSchema = new ListFieldSchemaImpl().setListType(FieldTypes.DATE.toString());
			} else {
				fieldSchema = new DateFieldSchemaImpl();
			}
			break;
		case bool:
			if (entry.isMultivalue()) {
				fieldSchema = new ListFieldSchemaImpl().setListType(FieldTypes.BOOLEAN.toString());
			} else {
				fieldSchema = new BooleanFieldSchemaImpl();
			}
			break;
		case micronode:
			if (entry.isMultivalue()) {
				fieldSchema = new ListFieldSchemaImpl().setListType(FieldTypes.MICRONODE.toString());
			} else {
				fieldSchema = new MicronodeFieldSchemaImpl().setAllowedMicroSchemas(new String[0]);
			}
		default:
			break;
		}

		if (fieldSchema != null) {
			fieldSchema.setName(entry.getMapname());
			fieldSchema.setNoIndex(entry.isNoIndex());
			String elasticsearch = entry.getElasticsearch();
			if ("null".equals(elasticsearch)) {
				elasticsearch = null;
			}
			if (!StringUtils.isEmpty(elasticsearch)) {
				fieldSchema.setElasticsearch(new JsonObject(elasticsearch));
			} else {
				fieldSchema.setElasticsearch(new JsonObject());
			}

			// check for duplicates
			FieldSchema otherField = container.getField(entry.getMapname());
			if (otherField != null) {
				SchemaChangeModel change = otherField.compareTo(fieldSchema);
				if (change.getOperation() != SchemaChangeOperation.EMPTY) {
					logger.warn(String.format("Ignoring duplicate field %s", entry.getMapname()));
				}
			} else {
				container.addField(fieldSchema);
			}
		}
	}

	/**
	 * Get the project specific name of the schema for the given object type
	 * @param objectType object type
	 * @return project schema name
	 */
	public String getSchemaName(int objectType) {
		switch (objectType) {
		case Form.TYPE_FORM:
			return FORM_SCHEMA;
		default:
			return String.format("%s_%s", schemaPrefix, schemaNames.get(normalizeObjType(objectType)));
		}
	}

	/**
	 * Get the object type for the given schema name
	 * @param schemaName schema name
	 * @return optional object type, empty if the schema does not belong to a CMS object
	 */
	public Optional<Integer> getObjectType(String schemaName) {
		if (org.apache.commons.lang3.StringUtils.equals(schemaName, FORM_SCHEMA)) {
			return Optional.of(Form.TYPE_FORM);
		}
		if (!org.apache.commons.lang3.StringUtils.startsWith(schemaName, schemaPrefix + "_")) {
			return Optional.empty();
		}
		String typeName = org.apache.commons.lang3.StringUtils.removeStart(schemaName, schemaPrefix + "_");
		switch (typeName) {
		case FOLDER_SCHEMA:
			return Optional.of(Folder.TYPE_FOLDER);
		case PAGE_SCHEMA:
			return Optional.of(Page.TYPE_PAGE);
		case FILE_SCHEMA:
			return Optional.of(File.TYPE_FILE);
		default:
			return Optional.empty();
		}
	}

	/**
	 * Get tagmap entries for the given object type
	 * @param objectType object type
	 * @return list of tagmap entries
	 */
	public List<TagmapEntryRenderer> getEntries(int objectType) {
		return tagmapEntries.get(normalizeObjType(objectType));
	}

	/**
	 * Render the tagmap entries for the object. This method will not reuse the renderresult of the transaction, but create a new temporary renderresult
	 * for each tagmap entry
	 * @param tagmapEntries tagmap entries
	 * @param attributes optional set of attributes to be rendered (if empty or null, all attributes will be rendered)
	 * @param language language in which the entries must be rendered
	 * @return tagmapEntries map
	 * @throws NodeException
	 */
	public Map<TagmapEntryRenderer, Object> render(List<TagmapEntryRenderer> tagmapEntries, Set<String> attributes, String language) throws NodeException {
		return render(tagmapEntries, attributes, language, true);
	}

	/**
	 * Render the tagmap entries for the object
	 * @param tagmapEntries tagmap entries
	 * @param attributes optional set of attributes to be rendered (if empty or null, all attributes will be rendered)
	 * @param language language in which the entries must be rendered
	 * @param createRenderResult true to create a new renderresult for each tagmap entry, false to use the renderresult of the current transaction
	 * @return tagmapEntries map
	 * @throws NodeException
	 */
	public Map<TagmapEntryRenderer, Object> render(List<TagmapEntryRenderer> tagmapEntries, Set<String> attributes, String language, boolean createRenderResult) throws NodeException {
		Map<TagmapEntryRenderer, Object> renderedEntries = new HashMap<>();
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		try (ContentLanguageTrx clTrx = new ContentLanguageTrx(language)) {
			for (TagmapEntryRenderer entry : tagmapEntries) {
				if (entry.canSkip() && !ObjectTransformer.isEmpty(attributes) && !attributes.contains(entry.getMapname())) {
					renderType.preserveDependencies(entry.getMapname());
				} else {
					// set the rendered property
					renderType.setRenderedProperty(entry.getMapname());
					RenderResult renderResult = createRenderResult ? new RenderResult() : t.getRenderResult();
					renderedEntries.put(entry, entry.getRenderedTransformedValue(renderType, renderResult, LINKTRANSFORMER));
				}
			}
		}

		return renderedEntries;
	}

	/**
	 * Render the form in the given language
	 * @param form form
	 * @param language language
	 * @return rendered form
	 * @throws NodeException
	 */
	public String render(Form form, String language) throws NodeException {
		String data = form.getData(language).toString();
		String projectName = getMeshProjectName(form.getOwningNode());
		String uuid = getMeshUuid(form);

		RestModel dataAsModel = new RestModel() {
			@Override
			public String toJson() {
				return data;
			}
		};

		return client.post(String.format("/%s/plugins/forms/forms/%s/preview", encodeSegment(projectName), uuid),
				dataAsModel, FormsPluginRenderResponse.class).toSingle().doOnSubscribe(disp -> {
					MeshPublisher.logger.debug(
							String.format("Rendering preview of form %s, language %s, json: %s", uuid, language, data));
				}).blockingGet().getHtml();
	}

	/**
	 * Make an authenticated get request to mesh.
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response get(Consumer<UriBuilder> uriBuilderConsumer) throws NodeException {
		return get(uriBuilderConsumer, null);
	}

	/**
	 * Make an authenticated get request to mesh.
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @param requestBuilderConsumer optional consumer for the Builder instance to setup additional headers
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response get(Consumer<UriBuilder> uriBuilderConsumer,
			Consumer<Builder> requestBuilderConsumer) throws NodeException {
		return performRequest("GET", uriBuilderConsumer, requestBuilderConsumer, null);
	}

	/**
	 * Make an authenticated post request to mesh with an empty body.
	 *
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response post(Consumer<UriBuilder> uriBuilderConsumer) throws NodeException {
		return post(uriBuilderConsumer, null, RequestBody.create(null, new byte[0]));
	}

	/**
	 * Make an authenticated post request to mesh with the given body.
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @param body request body
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response post(Consumer<UriBuilder> uriBuilderConsumer, RequestBody body) throws NodeException {
		return post(uriBuilderConsumer, null, body);
	}

	/**
	 * Make an authenticated post request to mesh with the given body.
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @param requestBuilderConsumer optional consumer for the Builder instance to setup additional headers
	 * @param body request body
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response post(Consumer<UriBuilder> uriBuilderConsumer, Consumer<Builder> requestBuilderConsumer, RequestBody body) throws NodeException {
		return performRequest("POST", uriBuilderConsumer, requestBuilderConsumer, body);
	}

	/**
	 * Perform a request to Mesh with the given method (currently supported:
	 * GET, POST, DELETE).
	 *
	 * <p>
	 *     The base URI and final request can be modified via the
	 *     {@code uriBuilderConsumer} and {@code requestBuilderConsumer}.
	 * </p>
	 *
	 * <p>
	 *     The {@code body} is only necessary for POST requests, and can be
	 *     {@code null} for GET and DELETE requests.
	 * </p>
	 *
	 * @param method The request method. One of GET, POST and DELETE.
	 * @param uriBuilderConsumer Modificator for the request URI.
	 * @param requestBuilderConsumer Modificator for the request itself.
	 * @param body The body for POST requests (can be {@code null} for other requests).
	 * @return The response from Mesh.
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response performRequest(String method, Consumer<UriBuilder> uriBuilderConsumer, Consumer<Builder> requestBuilderConsumer, RequestBody body) throws NodeException {
		try {
			UriBuilder uriBuilder = UriBuilder.fromPath(clientConfig.getBaseUrl());

			if (uriBuilderConsumer != null) {
				uriBuilderConsumer.accept(uriBuilder);
			}

			Builder requestBuilder = prepareRequestBuilder(method, uriBuilder.build().toString(), requestBuilderConsumer, body);
			// add auth info as headers
			Map<String, String> authHeaders = client.getAuthentication().getHeaders();
			authHeaders.forEach((name, value) -> requestBuilder.addHeader(name, value));

			Call call = okHttpClient.newCall(requestBuilder.build());
			Response response = call.execute();

			return javax.ws.rs.core.Response.status(response.code())
					.header("Content-Type", response.header("Content-Type"))
					.header("Content-Disposition", response.header("Content-Disposition"))
					.entity(new StreamingOutput() {
						@Override
						public void write(OutputStream output) throws IOException, WebApplicationException {
							try (ResponseBody responseBody = response.body()) {
								IOUtils.copy(responseBody.byteStream(), output);
							}
						}
					}).build();
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Prepare a {@link Builder request builder} for the given Method and body.
	 *
	 * <p>
	 *     When {@code requestBuilderConsumer} is specified it is also applied
	 *     to the resulting request builder.
	 * </p>
	 *
	 * @param method The request method (one of GET, POST and DELETE).
	 * @param url The request URL.
	 * @param requestBuilderConsumer Modificator for the request.
	 * @param body The body for POST requests.
	 * @return The prepared request builder.
	 * @throws NodeException
	 */
	private Builder prepareRequestBuilder(String method, String url, Consumer<Builder> requestBuilderConsumer, RequestBody body) throws NodeException {
		Builder requestBuilder = new Request.Builder().url(url);

		switch (method.toUpperCase()) {
		case "POST":
			requestBuilder = requestBuilder.post(body);

			break;

		case "DELETE":
			requestBuilder = requestBuilder.delete();

			break;

		default:
		case "GET":
			requestBuilder = requestBuilder.get();

			break;
		}

		if (requestBuilderConsumer != null) {
			requestBuilderConsumer.accept(requestBuilder);
		}

		return requestBuilder;
	}

	/**
	 * Make an authenticated delete request to mesh.
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response delete(Consumer<UriBuilder> uriBuilderConsumer) throws NodeException {
		return delete(uriBuilderConsumer, null);
	}

	/**
	 * Make an authenticated delete request to mesh.
	 * @param uriBuilderConsumer consumer for the UriBuilder to setup additional path segments and query parameters to the request
	 * @param requestBuilderConsumer optional consumer for the Builder instance to setup additional headers
	 * @return response
	 * @throws NodeException
	 */
	public javax.ws.rs.core.Response delete(Consumer<UriBuilder> uriBuilderConsumer,
			Consumer<Builder> requestBuilderConsumer) throws NodeException {
		return performRequest("DELETE", uriBuilderConsumer, requestBuilderConsumer, null);
	}

	/**
	 * Handle rendered tagmap entries.
	 * Most will be transformed into a FieldMap, which can be handled in the handler.
	 * Entries, that need to be handled later, can be postponed.
	 * Entries that resolve to mesh roles can be handled in a separate consumer
	 * @param nodeId node ID
	 * @param objectId
	 * @param objectType
	 * @param tagmapEntries rendered tagmap entries
	 * @param attributes optional set of attributes that were rendered (if empty or null, all attributes were rendered)
	 * @param fieldMapHandler handler for the fieldmap
	 * @param roleHandler handler for roles
	 * @param postpone Consumer that handles postponed tagmap entries
	 * @param postSaveFuncConsumer
	 * @return FieldMap
	 * @throws NodeException
	 */
	public void handleRenderedEntries(int nodeId, int objectId, int objectType, Map<TagmapEntryRenderer, Object> tagmapEntries, Set<String> attributes, Consumer<FieldMap> fieldMapHandler,
			Consumer<Collection<String>> roleHandler, Consumer<Map<TagmapEntryRenderer, Object>> postpone, Consumer<Function<NodeResponse, Single<NodeResponse>>> postSaveFuncConsumer) throws NodeException {
		handleRenderedEntries(false, nodeId, objectId, objectType, null, tagmapEntries, attributes, fieldMapHandler, roleHandler, postpone, postSaveFuncConsumer);
	}

	/**
	 * If the given source contains the term /GenticsImageStore/, return an Optional of {@link MeshPublisherGisImageInitiator}, otherwise return an empty Optional
	 * @param source source to check
	 * @param nodeId node ID
	 * @param entityId entity ID
	 * @param fieldKey field key
	 * @param entityType entity Type
	 * @return Optional
	 * @throws NodeException
	 */
	public Optional<MeshPublisherGisImageInitiator> handleCollectingGisImages(String source, int nodeId, int entityId, String fieldKey, int entityType) throws NodeException {
		if (source == null || !source.contains("/GenticsImageStore/")) {
			return Optional.empty();
		}

		Node node = TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId);
		if (!node.isPublishImageVariants()) {
			return Optional.empty();
		}

		return Optional.of(new MeshPublisherGisImageInitiator(nodeId, entityId, entityType, fieldKey).setSource(source));
	}

	/**
	 * Handle rendered tagmap entries.
	 * Most will be transformed into a FieldMap, which can be handled in the handler.
	 * Entries, that need to be handled later, can be postponed.
	 * Entries that resolve to mesh roles can be handled in a separate consumer
	 * @param preview true for preview, false for publishing
	 * @param nodeId node ID
	 * @param objectId
	 * @param objectType
	 * @param fieldMapSupplier field map supplier
	 * @param tagmapEntries rendered tagmap entries
	 * @param attributes optional set of attributes that were rendered (if empty or null, all attributes were rendered)
	 * @param fieldMapHandler handler for the fieldmap
	 * @param roleHandler handler for roles
	 * @param postpone Consumer that handles postponed tagmap entries
	 * @param postSaveFuncConsumer Consumer that handles post save functions
	 * @return FieldMap
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public void handleRenderedEntries(boolean preview, int nodeId, int objectId, int objectType, Supplier<FieldMap> fieldMapSupplier, Map<TagmapEntryRenderer, Object> tagmapEntries, Set<String> attributes,
			Consumer<FieldMap> fieldMapHandler, Consumer<Collection<String>> roleHandler, Consumer<Map<TagmapEntryRenderer, Object>> postpone, Consumer<Function<NodeResponse, Single<NodeResponse>>> postSaveFuncConsumer) throws NodeException {
		FieldMap fields = null;
		if (fieldMapSupplier != null) {
			fields = fieldMapSupplier.get();
		} else {
			fields = new FieldMapImpl();
		}

		Map<TagmapEntryRenderer, Object> postponed = new HashMap<>();
		Collection<String> roles = null;
		List<MeshPublisherGisImageInitiator> gisInitiators = new ArrayList<>();

		for (Map.Entry<TagmapEntryRenderer, Object> mapEntry : tagmapEntries.entrySet()) {
			TagmapEntryRenderer entry = mapEntry.getKey();
			Object value = mapEntry.getValue();
			if (entry.canSkip() && !ObjectTransformer.isEmpty(attributes) && !attributes.contains(entry.getMapname())) {
				continue;
			}

			if (entry instanceof MeshRoleRenderer) {
				if (!ObjectTransformer.isEmpty(value)) {
					roles = ObjectTransformer.getCollection(value, Collections.emptyList());
				} else {
					roles = Collections.emptyList();
				}
			} else if (value == null) {
				fields.put(entry.getMapname(), null);
			} else {
				switch (AttributeType.getForType(entry.getAttributeType())) {
				case integer:
				{
					if (entry.isMultivalue()) {
						FieldList<Number> field = new NumberFieldListImpl();
						fields.put(entry.getMapname(), field);
						for (Object o : ObjectTransformer.getCollection(value, Collections.emptyList())) {
							Number number = ObjectTransformer.getNumber(o, null);
							if (number != null) {
								field.add(number);
							}
						}
					} else {
						fields.put(entry.getMapname(), new NumberFieldImpl().setNumber(ObjectTransformer.getNumber(value, null)));
					}
					break;
				}
				case text:
				case longtext:
				{
					if (entry.isMultivalue()) {
						FieldList<String> field = new StringFieldListImpl();
						fields.put(entry.getMapname(), field);
						for (Object o : ObjectTransformer.getCollection(value, Collections.emptyList())) {
							String string = ObjectTransformer.getString(o, null);
							if (string != null) {
								field.add(string);
								if (!preview && postSaveFuncConsumer != null) {
									handleCollectingGisImages(string, nodeId, objectId, entry.getMapname(), objectType).ifPresent(gisInitiators::add);
								}
							}
						}
					} else {
						String string = ObjectTransformer.getString(value, null);
						fields.put(entry.getMapname(), new StringFieldImpl().setString(string));
						if (!preview && postSaveFuncConsumer != null) {
							handleCollectingGisImages(string, nodeId, objectId, entry.getMapname(), objectType).ifPresent(gisInitiators::add);
						}
					}
					break;
				}
				case link:
				{
					if (entry.isMultivalue()) {
						FieldList<NodeFieldListItem> field = new NodeFieldListImpl();
						fields.put(entry.getMapname(), field);
						for (Object o : ObjectTransformer.getCollection(value, Collections.emptyList())) {
							if (o instanceof MeshLink) {
								MeshLink link = (MeshLink)o;
								if (preview) {
									field.add(new NodeFieldListItemImpl(link.meshUuid));
								} else {
									Optional<MeshProject> optionalProject = getProject(link.object);
									if (optionalProject.isPresent() && existsInMesh(nodeId, optionalProject.get(), link.object)) {
										field.add(new NodeFieldListItemImpl(link.meshUuid));
									} else {
										postponed.put(entry, value);
									}
								}
							}
						}
					} else {
						if (value instanceof MeshLink) {
							MeshLink link = (MeshLink)value;
							if (preview) {
								fields.put(entry.getMapname(), new NodeFieldImpl().setUuid(link.meshUuid));
							} else {
								Optional<MeshProject> optionalProject = getProject(link.object);
								if (optionalProject.isPresent() && existsInMesh(nodeId, optionalProject.get(), link.object)) {
									fields.put(entry.getMapname(), new NodeFieldImpl().setUuid(link.meshUuid));
								} else {
									postponed.put(entry, value);
								}
							}
						}
					}
					break;
				}
				case bool:
				{
					if (entry.isMultivalue()) {
						FieldList<Boolean> field = new BooleanFieldListImpl();
						fields.put(entry.getMapname(), field);
						for (Object o : ObjectTransformer.getCollection(value, Collections.emptyList())) {
							Boolean bool = ObjectTransformer.getBoolean(o, null);
							if (bool != null) {
								field.add(bool);
							}
						}
					} else {
						fields.put(entry.getMapname(), new BooleanFieldImpl().setValue(ObjectTransformer.getBoolean(value, null)));
					}
					break;
				}
				case date:
				{
					if (entry.isMultivalue()) {
						FieldList<String> field = new DateFieldListImpl();
						fields.put(entry.getMapname(), field);
						for (Object o : ObjectTransformer.getCollection(value, Collections.emptyList())) {
							Integer time = ObjectTransformer.getInteger(o, null);
							if (time != null) {
								field.add(Instant.ofEpochSecond(time).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
							}
						}
					} else {
						Integer time = ObjectTransformer.getInteger(value, null);
						if (time != null) {
							fields.put(entry.getMapname(),
									new DateFieldImpl().setDate(Instant.ofEpochSecond(time).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)));
						}
					}
					break;
				}
				case micronode:
				{
					if (entry.isMultivalue()) {
						FieldList<MicronodeField> field = new MicronodeFieldListImpl();
						fields.put(entry.getMapname(), field);
						for (Object o : ObjectTransformer.getCollection(value, Collections.emptyList())) {
							if (o instanceof ContentTag) {
								MicronodeResponse micronode = micronodePublisher
										.toMicronode(entry.getMapname(), nodeId, (Tag) o, () -> postponed.put(entry, value)).blockingGet();
								if (micronode != null) {
									field.add(micronode);
								}
							}
						}
					} else {
						if (value instanceof ContentTag) {
							MicronodeResponse micronode = micronodePublisher
									.toMicronode(entry.getMapname(), nodeId, (Tag) value, () -> postponed.put(entry, value)).blockingGet();
							if (micronode != null) {
								fields.put(entry.getMapname(), micronode);
							}
						}
					}
					break;
				}
				default:
					break;
				}
			}
		}

		if (fieldMapHandler != null) {
			fieldMapHandler.accept(fields);
		}

		if (ObjectTransformer.isEmpty(roles) && !ObjectTransformer.isEmpty(cr.getDefaultPermission())) {
			roles = Collections.singleton(cr.getDefaultPermission());
		}

		if (roles != null && roleHandler != null) {
			roleHandler.accept(roles);
		}

		if (!postponed.isEmpty() && postpone != null) {
			postpone.accept(postponed);
		}

		if (!ObjectTransformer.isEmpty(gisInitiators) && postSaveFuncConsumer != null) {
			postSaveFuncConsumer.accept(resp -> {
				handleGisInitiators(nodeId, gisInitiators);
				return Single.just(resp);
			});
		}
	}

	/**
	 * Do some reverse engineering to get the CMS object which was published to Mesh with the given UUID and language
	 * @param project mesh project
	 * @param nodeId node ID
	 * @param meshUuid mesh UUID
	 * @param meshLanguage mesh language
	 * @return optional pair of object type and NodeObject. The optional is empty, if the object in Mesh could not be found or was not published from
	 * the CMS. Otherwise the pair will always contain the object type and also the NodeObject, if it could be found in the CMS
	 * @throws NodeException
	 */
	public Optional<Pair<Integer, NodeObject>> getNodeObject(MeshProject project, int nodeId, String meshUuid, String meshLanguage) throws NodeException {
		GenericParameters genParams = new GenericParametersImpl().setETag(false).setFields("uuid", "fields", "schema");
		NodeParameters langParam = new NodeParametersImpl().setLanguages(meshLanguage);
		NodeResponse conflict = client
				.findNodeByUuid(project.name, meshUuid, project.enforceBranch(nodeId), genParams, langParam).toMaybe()
				.onErrorComplete().blockingGet();
		if (conflict != null) {
			String schemaName = conflict.getSchema().getName();
			Optional<Integer> optCmsId = Optional.ofNullable(conflict.getFields().getNumberField("cms_id"))
					.flatMap(f -> Optional.ofNullable(f.getNumber())).map(Number::intValue);
			Optional<Integer> optType = getObjectType(schemaName);
			if (optType.isPresent() && optCmsId.isPresent()) {
				int objType = optType.get();
				int cmsId = optCmsId.get();

				NodeObject object = null;
				try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
					Transaction t = trx.getTransaction();
					object = t.getObject(t.getClass(objType), cmsId);
				}

				return Optional.of(Pair.of(objType, object));
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Get the parent object of the given object.
	 * Do not return the root folder, if the CR publishes into project per node
	 * @param object object
	 * @return root object or null
	 * @throws NodeException
	 */
	protected NodeObject getParent(NodeObject object) throws NodeException {
		return object.getParentObject();
	}

	/**
	 * Decide whether the object can be handled or has to be put back, because the object's parent does not exist in mesh
	 * @param project mesh project
	 * @param nodeId node ID
	 * @param scheduled object to handle
	 * @param handler consumer that handles the object and the base version (null if the object is new)
	 * @param putBack consumer that puts the object back (will be handled later)
	 * @throws NodeException
	 */
	protected void handle(MeshProject project, int nodeId, Scheduled scheduled,
			BiConsumer<Scheduled, MeshObject> handler, Consumer<Scheduled> putBack) throws NodeException {
		NodeObject object = scheduled.get().getObject();
		logger.debug(String.format("Handling %s", object));
		int objType = object.getTType();
		int objId = object.getId();
		Map<Integer, Set<Integer>> nodeMap = handled.computeIfAbsent(nodeId, key -> new ConcurrentHashMap<>());
		Set<Integer> idSet = nodeMap.computeIfAbsent(objType, key -> new HashSet<>());

		// already handled
		if (idSet.contains(objId)) {
			logger.debug(String.format("%s was already done before", object));
			scheduled.reportDone(publishInfo);
			return;
		}

		if (supportsAlternativeLanguages(object)) {
			// the object supports alternative languages, so create/update all language variants in Mesh
			Set<String> alternativeMeshLanguages = getAlternativeMeshLanguages(object);
			for (String alternativeLanguage : alternativeMeshLanguages) {
				handler.accept(new Scheduled(nodeId, new NodeObjectWithAttributes<NodeObject>(object),
						alternativeLanguage, false), null);
			}
		}

		handler.accept(scheduled, null);
	}

	/**
	 * Check whether the given object is known to exist in Mesh
	 * @param nodeId node ID
	 * @param project mesh project
	 * @param object object to check
	 * @return true iff known to exist
	 * @throws NodeException
	 */
	protected boolean existsInMesh(int nodeId, MeshProject project, NodeObject object) throws NodeException {
		int id = object.getId();
		if (written.getOrDefault(nodeId, Collections.emptyMap()).getOrDefault(object.getTType(), Collections.emptySet()).contains(id)) {
			return true;
		}
		if (missing.getOrDefault(nodeId, Collections.emptyMap()).getOrDefault(object.getTType(), Collections.emptySet()).contains(id)) {
			return false;
		}

		String meshUuid = getMeshUuid(object);
		return client.findNodeByUuid(project.name, meshUuid, new GenericParametersImpl().setETag(false)).toSingle().map(Optional::of).onErrorResumeNext(t -> {
			return ifNotFound(t, () -> {
				return Single.just(Optional.empty());
			});
		}).map(optionalResponse -> {
			if (optionalResponse.isPresent()) {
				getWrittenSet(nodeId, object.getTType()).add(id);
				return true;
			} else {
				getMissingSet(nodeId, object.getTType()).add(id);
				return false;
			}
		}).blockingGet();
	}

	/**
	 * Write an object to Mesh:
	 * <ol>
	 * <li>Create/Update the language version of the node (with the fields)</li>
	 * <li>Optionally move the object to its correct parent</li>
	 * <li>Perform the optional postSave operation</li>
	 * <li>Publish the language version of the node</li>
	 * <li>Mark the task to be done and report back to the PublishQueue</li>
	 * </ol>
	 * @param task
	 * @throws NodeException
	 */
	protected void save(WriteTask task) throws NodeException {
		save(task, true, null);
	}

	/**
	 * Write an object to Mesh:
	 * <ol>
	 * <li>Create/Update the language version of the node (with the fields)</li>
	 * <li>Optionally move the object to its correct parent</li>
	 * <li>Perform the optional postSave operation</li>
	 * <li>Publish the language version of the node</li>
	 * <li>Mark the task to be done and report back to the PublishQueue</li>
	 * </ol>
	 * @param task
	 * @param withSemaphore whether the acquire a semaphore
	 * @param doAfter optional completable to do after saving
	 * @throws NodeException
	 */
	protected void save(WriteTask task, boolean withSemaphore, Completable doAfter) throws NodeException {
		if (withSemaphore) {
			semaphoreMap.acquire(lockKey, callTimeout, TimeUnit.SECONDS);
		}
		try {
			logger.debug(String.format("Start saving %d.%d", task.objType, task.objId));
			Completable completable = prepare(task);
			if (doAfter != null) {
				completable = completable.andThen(doAfter);
			}
			completable.blockingAwait();
		} catch (Throwable t) {
			if (task.postponable && isRecoverable(t)) {
				if (MeshPublishUtils.isNotFound(t) || MeshPublishUtils.isBadRequestAfterMove(t)) {
					// get parent folder
					Folder parentFolder = Trx.supply(tx -> tx.getObject(Folder.class, task.folderId));
					if (parentFolder == null) {
						throw t;
					}
					Node node = Trx.supply(tx -> tx.getObject(Node.class, task.nodeId, false, false, true));

					// generate the write task for the parent folder
					Scheduled scheduledParent = Scheduled.from(task.nodeId, new NodeObjectWithAttributes<>(parentFolder));
					WriteTask parentTask = Trx.supply(() -> generateWriteTask(node, task.project, scheduledParent, null, null));

					Completable postponedCompletable = prepare(task);
					if (doAfter != null) {
						postponedCompletable = postponedCompletable.andThen(doAfter);
					}
					save(parentTask, false, postponedCompletable);
				} else {
					boolean postpone = true;
					Optional<Pair<String,String>> conflictingNode = MeshPublishUtils.getConflictingNode(t);
					if (conflictingNode.isPresent()) {
						String conflictingUuid = conflictingNode.get().getLeft();
						String conflictingLanguage = conflictingNode.get().getRight();

						if (org.apache.commons.lang3.StringUtils.equals(conflictingUuid, task.uuid)
								&& !org.apache.commons.lang3.StringUtils.equals(conflictingLanguage, task.language)) {
							Node node = Trx.supply(tx -> tx.getObject(Node.class, task.nodeId, false, false, true));
							NodeObject languageVariant = Trx.supply(() -> task.getLanguageVariant(conflictingLanguage));

							if (!Trx.supply(() -> cr.mustContain(languageVariant))) {
								// remove language variant
								remove(task.project, node, task.objType, conflictingUuid, conflictingLanguage, false);
								// repeat task
								task.perform(false);
								postpone = false;
							}
						} else {
							// check whether the conflicting node should be removed
							Optional<Pair<Integer, NodeObject>> optNodeObject = getNodeObject(task.project, task.nodeId, conflictingUuid, conflictingLanguage);
							if (optNodeObject.isPresent()) {
								Node node = Trx.supply(tx -> tx.getObject(Node.class, task.nodeId, false, false, true));

								int objType = optNodeObject.get().getLeft();
								NodeObject conflictingNodeObject = optNodeObject.get().getRight();
								if (conflictingNodeObject == null || !Trx.supply(() -> cr.mustContain(conflictingNodeObject))) {
									// remove the object from Mesh
									remove(task.project, node, objType, conflictingUuid, conflictingLanguage, false);
									// repeat task
									task.perform(false);
									postpone = false;
								}
							}
						}
					}

					if (postpone) {
						if (logger.isDebugEnabled()) {
							logger.debug(String.format("Postponing update of %d.%d due to recoverable error '%s'", task.objType, task.objId, t.getMessage()));
						}
						postponedTasks.add(task);
					}
				}
			} else {
				throw t;
			}
		} finally {
			logger.debug(String.format("Finished saving %d.%d", task.objType, task.objId));
			if (withSemaphore) {
				semaphoreMap.release(lockKey);
			}
		}
	}

	/**
	 * Prepare the completable to write the given task
	 * @param task task
	 * @return completable
	 */
	protected Completable prepare(WriteTask task) {
		MeshRequest<NodeResponse> response = null;

		// upsert the node
		NodeUpsertRequest request = new NodeUpsertRequest();
		request.setLanguage(task.language);
		request.setParentNodeUuid(task.parentUuid);
		request.setSchema(new SchemaReferenceImpl().setName(task.schema));
		request.setFields(task.fields);

		if (supportsPublishOnCreate) {
			request.setPublish(true);
			if (task.roles != null) {
				request.setGrant(createPermissionUpdateRequests(task));
			}
		}

		boolean supportsAlternativeLanguages = supportsAlternativeLanguages(task.objType) && task.alternativeMeshLanguages != null;
		GenericParameters params = supportsAlternativeLanguages
				? new GenericParametersImpl().setETag(false).setFields("uuid", "parent", "languages")
				: new GenericParametersImpl().setETag(false).setFields("uuid", "parent");
		response = client.upsertNode(task.project.name, task.uuid, request, task.project.enforceBranch(task.nodeId),
				params);

		AtomicLong start = new AtomicLong();
		return ensureRoles(task.roles)
			.andThen(
				response.toSingle()
					.doOnSubscribe(disp -> {
						start.set(System.currentTimeMillis());
						saveNodeCounter.incrementAndGet();
					})
					.flatMap(node -> setRolePermissions(task, node))
					.flatMap(node -> move(task, node))
					.flatMap(node -> postSave(task, node))
					.flatMap(node -> publish(task, node))
					.doOnError(t -> {
						if (!task.postponable || !isRecoverable(t)) {
							errorHandler.accept(new NodeException(String.format("Error while performing task '%s' for '%s'", task, cr.getName()), t));
						}
					})
					.doOnSuccess(node -> {
						if (renderResult != null) {
							try {
								long duration = System.currentTimeMillis() - start.get();
								renderResult.info(MeshPublisher.class,
										String.format("written %d.%d into {%s} for node %d in %d ms", task.objType, task.objId, cr.getName(), task.nodeId, duration));
							} catch (NodeException e) {
							}
						}
						setWritten(task);

						if (supportsAlternativeLanguages && node.getAvailableLanguages() != null) {
							Set<String> languages = node.getAvailableLanguages().keySet();
							// remove the primary language
							languages.remove("en");
							// remove all the languages that should exist
							languages.removeAll(task.alternativeMeshLanguages);

							if (!languages.isEmpty()) {
								Node cmsNode = Trx.supply(tx -> tx.getObject(Node.class, task.nodeId, false, false, true));
								for (String lang : languages) {
									remove(task.project, cmsNode, task.objType, task.uuid, lang, false);
								}
							}
						}

						if (task.postponed != null) {
							logger.debug(String.format("Postponing update of %d.%d", task.objType, task.objId));
							task.exists = true;
							task.clearPostSave();
							postponedTasks.add(task);
						} else {
							task.reportDone();
						}
				})).ignoreElement()
			.andThen(task.project.setPermissions(task.roles));
	}

	/**
	 * Set the object contained in the task to be written to Mesh
	 * @param task write task
	 */
	protected void setWritten(WriteTask task) {
		getWrittenSet(task.nodeId, task.objType).add(task.objId);
		Set<Integer> missingSet = missing.getOrDefault(task.nodeId, Collections.emptyMap())
			.getOrDefault(task.objType, Collections.emptySet());
		if (!missingSet.isEmpty()) {
			missingSet.remove(task.objId);
		}
	}

	/**
	 * Get the modifiable set containing IDs of objects, which were already written to Mesh for the nodeId and objType.
	 * If the set does not yet exist, create it
	 * @param nodeId node ID
	 * @param objType object type
	 * @return set of IDs
	 */
	protected Set<Integer> getWrittenSet(int nodeId, int objType) {
		return written.computeIfAbsent(nodeId, k -> Collections.synchronizedMap(new HashMap<>()))
				.computeIfAbsent(objType, k -> Collections.synchronizedSet(new HashSet<>()));
	}

	/**
	 * Get the modifiable set containing IDs of objects, which are known to be missing in Mesh for the nodeId and objType.
	 * If the set does not yet exist, create it
	 * @param nodeId node ID
	 * @param objType object type
	 * @return set of IDs
	 */
	protected Set<Integer> getMissingSet(int nodeId, int objType) {
		return missing.computeIfAbsent(nodeId, k -> Collections.synchronizedMap(new HashMap<>()))
				.computeIfAbsent(objType, k -> Collections.synchronizedSet(new HashSet<>()));
	}

	/**
	 * Move node if necessary
	 * @param task write task
	 * @param node node response (from the save request)
	 * @return single node response
	 */
	protected Single<NodeResponse> move(WriteTask task, NodeResponse node) {
		if (task.parentUuid != null && (node.getParentNode() == null || !node.getParentNode().getUuid().equals(task.parentUuid))) {
			if (task.project.equals(task.targetProject)) {
				return client.moveNode(task.project.name, task.uuid, task.parentUuid, task.project.enforceBranch(task.nodeId))
						.toCompletable()
						.andThen(Single.just(node));
			} else {
				// TODO when mesh supports that, move the object between projects
				return Single.just(node);
			}
		} else {
			return Single.just(node);
		}
	}

	/**
	 * Set role permissions
	 * @param task write task
	 * @param node node response (from the save request)
	 * @return single node response
	 */
	protected Single<NodeResponse> setRolePermissions(WriteTask task, NodeResponse node) {
		if (task.roles == null) {
			return Single.just(node);
		}

		if (supportsPublishOnCreate) {
			return Single.just(node);
		}

		return ensureRoles(task.roles)
			.andThen(updatePermissions(task, node.getUuid()))
			.andThen(task.project.setPermissions(task.roles))
			.andThen(Single.just(node));
	}

	/**
	 * Create the request that will perform the permission update requests.
	 *
	 * @param task The current write task.
	 * @return the request for updating the permissions
	 */
	protected ObjectPermissionGrantRequest createPermissionUpdateRequests(WriteTask task) {
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setReadPublished(task.roles.stream().map(roleName -> new RoleReference().setName(roleName)).collect(Collectors.toList()));
		request.setRead(Collections.emptyList());
		request.setExclusive(true);
		request.setIgnore(Collections.singletonList(new RoleReference().setName("admin")));
		return request;
	}

	/**
	 * Create the completable that will perform the permission update requests.
	 *
	 * @param task The current write task.
	 * @param nodeUuid The UUID of the currently published node.
	 * @return A {@code Completable} which will perform the necessary permission update requets.
	 */
	protected Completable updatePermissions(WriteTask task, String nodeUuid) {
		return client.grantNodeRolePermissions(task.project.name, nodeUuid, createPermissionUpdateRequests(task)).toCompletable();
	}

	/**
	 * Make sure the specified roles exist in Mesh.
	 *
	 * @param neededRoles The roles to check.
	 * @return A completable which will be completed when all missing roles
	 * 		have been created in Mesh.
	 */
	protected Completable ensureRoles(Collection<String> neededRoles) {
		if (CollectionUtils.isEmpty(neededRoles)) {
			return Completable.complete();
		}
		Map<String, String> currentRolesMap = getRoleMap();

		return Observable.fromIterable(neededRoles)
			.filter(role -> !currentRolesMap.containsKey(role))
			.doOnNext(role -> logger.debug("Creating missing role in Mesh: " + role))
			.map(role -> new RoleCreateRequest().setName(role))
			.flatMapSingle(request -> client.createRole(request).toSingle())
			.flatMapCompletable(role -> {
				// put the created map into the roles map
				currentRolesMap.put(role.getName(), role.getUuid());
				return Completable.complete();
			});
	}

	/**
	 * Apply postsave (e.g. binary field)
	 * @param task write task
	 * @param node node response (from the save request)
	 * @return single node response
	 */
	protected Single<NodeResponse> postSave(WriteTask task, NodeResponse node) {
		if (task.hasPostSave()) {
			Single<NodeResponse> singleResponse = Single.just(node);

			for (Function<NodeResponse, Single<NodeResponse>> function : task.getPostSave()) {
				singleResponse = singleResponse.flatMap(function);
			}

			return singleResponse;
		} else {
			return Single.just(node);
		}
	}

	/**
	 * Publish the node
	 * @param task write task
	 * @param node node response (from the save request)
	 * @return single node response
	 */
	protected Single<NodeResponse> publish(WriteTask task, NodeResponse node) {
		if (supportsPublishOnCreate) {
			return Single.just(node);
		}

		return client.publishNodeLanguage(task.project.name, task.uuid, task.language, task.project.enforceBranch(task.nodeId)).toSingle()
				.flatMap(status -> Single.just(node));
	}

	/**
	 * Check whether the focal point information in the given node response is different from the info of the image
	 * @param node node response
	 * @param image image
	 * @return true iff focal point info is different
	 * @throws NodeException
	 */
	protected boolean needFocalPointUpdate(NodeResponse node, ImageFile image) throws NodeException {
		FieldMap fields = node.getFields();
		if (fields == null) {
			return true;
		}
		BinaryField binaryField = fields.getBinaryField("binarycontent");
		if (binaryField == null) {
			return true;
		}
		FocalPoint focalPoint = binaryField.getFocalPoint();
		if (focalPoint == null) {
			return true;
		}

		return Float.compare(image.getFpX(), focalPoint.getX()) != 0 || Float.compare(image.getFpY(), focalPoint.getY()) != 0;
	}

	/**
	 * Get existing languages for the form in Mesh (returns empty set, if node does not yet exist)
	 * @param project mesh project
	 * @param uuid form uuid
	 * @return single emitting the set of existing language tags
	 */
	protected Single<Set<String>> getExistingFormLanguages(MeshProject project, String uuid) {
		return client.findNodeByUuid(project.name, uuid, new GenericParametersImpl().setETag(false)).toSingle().map(Optional::of).onErrorResumeNext(t -> {
			return ifNotFound(t, () -> {
				MeshPublisher.logger.debug(String.format("Node %s not found", uuid));
				return Single.just(Optional.empty());
			});
		}).map(optionalResponse -> {
			if (optionalResponse.isPresent()) {
				MeshPublisher.logger.debug(String.format("Found languages %s in form %s", optionalResponse.get().getAvailableLanguages().keySet(), uuid));
				return optionalResponse.get().getAvailableLanguages().keySet();
			} else {
				MeshPublisher.logger.debug(String.format("Found no languages for form %s", uuid));
				return Collections.emptySet();
			}
		});
	}

	/**
	 * Handle the list of {@link MeshPublisherGisImageInitiator} by processing for GIS URLs and storing them in the DB
	 * @param nodeId node ID
	 * @param gisInitiators list of gis initiators
	 * @throws NodeException
	 */
	protected void handleGisInitiators(int nodeId, List<MeshPublisherGisImageInitiator> gisInitiators) throws NodeException {
		if (ObjectTransformer.isEmpty(gisInitiators)) {
			return;
		}
		Trx.operate(t -> {
			Node node = t.getObject(Node.class, nodeId, -1, false);
			for (MeshPublisherGisImageInitiator initiator : gisInitiators) {
				CNGenticsImageStore.processGISUrls(initiator, node, initiator.getSource(), null, allImageData,
						CNGenticsImageStore::storeGISLink, CNGenticsImageStore::deleteExcessGISLinksForPublishId);
				initiator.setSource(null);
			}
		});
	}

	/**
	 * Implementation of a Scheduled job to render an object and put it to the taskqueue
	 */
	public static class Scheduled {
		/**
		 * Node ID
		 */
		protected int nodeId;

		/**
		 * Wrapped object
		 */
		protected NodeObjectWithAttributes<? extends NodeObject> wrapped;

		/**
		 * Language of the object (in Mesh)
		 */
		protected String language;

		/**
		 * Optionally set tagmap entries
		 */
		protected Map<TagmapEntryRenderer, Object> tagmapEntries;

		/**
		 * Flag to mark scheduled objects that need to be reported to the publish queue
		 */
		protected boolean reportToPublishQueue = false;

		/**
		 * Wrap every element of the collection into an instance of {@link Scheduled}
		 * @param nodeId node ID
		 * @param collection collection to wrap
		 * @return collection of wrappers
		 */
		public static <U> Collection<Scheduled> from(int nodeId, Collection<? extends NodeObjectWithAttributes<? extends NodeObject>> collection) {
			return from(nodeId, collection, false);
		}

		/**
		 * Wrap every element of the collection into an instance of {@link Scheduled}
		 * @param nodeId node ID
		 * @param collection collection to wrap
		 * @param reportToPublishQueue true to report back to the publish queue
		 * @return collection of wrappers
		 */
		public static <U> Collection<Scheduled> from(int nodeId, Collection<? extends NodeObjectWithAttributes<? extends NodeObject>> collection, boolean reportToPublishQueue) {
			return Flowable.fromIterable(collection).map(e -> Scheduled.from(nodeId, e, reportToPublishQueue)).toList().blockingGet();
		}

		/**
		 * Wrap the object into an instance of {@link Scheduled}
		 * @param nodeId node ID
		 * @param object object to wrap
		 * @return wrapper
		 * @throws NodeException
		 */
		public static Scheduled from(int nodeId, NodeObjectWithAttributes<? extends NodeObject> object) throws NodeException {
			return from(nodeId, object, false);
		}

		/**
		 * Wrap the object into an instance of {@link Scheduled}
		 * @param nodeId node ID
		 * @param object object to wrap
		 * @param reportToPublishQueue true to report back to the publish queue
		 * @return wrapper
		 * @throws NodeException
		 */
		public static Scheduled from(int nodeId, NodeObjectWithAttributes<? extends NodeObject> object, boolean reportToPublishQueue) throws NodeException {
			return new Scheduled(nodeId, object, reportToPublishQueue);
		}

		/**
		 * Create an instance
		 * @param nodeId node ID
		 * @param wrapped wrapped object
		 * @param reportToPublishQueue true to report back to the publish queue
		 * @throws NodeException
		 */
		protected Scheduled(int nodeId, NodeObjectWithAttributes<? extends NodeObject> wrapped, boolean reportToPublishQueue) throws NodeException {
			this(nodeId, wrapped, getMeshLanguage(wrapped.getObject()), reportToPublishQueue);
		}

		/**
		 * Create an instance
		 * @param nodeId node ID
		 * @param wrapped wrapped object
		 * @param language language of the object
		 * @param reportToPublishQueue true to report back to the publish queue
		 * @throws NodeException
		 */
		protected Scheduled(int nodeId, NodeObjectWithAttributes<? extends NodeObject> wrapped, String language, boolean reportToPublishQueue) throws NodeException {
			this.nodeId = nodeId;
			this.wrapped = wrapped;
			this.reportToPublishQueue = reportToPublishQueue;
			this.language = language;
		}

		/**
		 * Get the wrapped object
		 * @return wrapped object
		 */
		public NodeObjectWithAttributes<? extends NodeObject> get() {
			return wrapped;
		}

		/**
		 * Report publishing an object
		 * @param publishInfo optional publish info
		 */
		public void reportDone(SimplePublishInfo publishInfo) {
			if (publishInfo != null) {
				if (wrapped instanceof Folder) {
					MBeanRegistry.getPublisherInfo().publishedFolder(nodeId);
					publishInfo.folderRendered();
				} else if (wrapped instanceof File) {
					MBeanRegistry.getPublisherInfo().publishedFile(nodeId);
					publishInfo.fileRendered();
				}
			}
		}

		/**
		 * Get the language, with which the object needs to be rendered.
		 * <ol>
		 * <li>For pages, this is the page language</li>
		 * <li>For pages without language, this is null</li>
		 * <li>For files, this is always null (files do not have a language in the CMS)</li>
		 * <li>For folders, this is the scheduled language</li>
		 * </ol>
		 * @return language code (may be null)
		 * @throws NodeException
		 */
		public String getRenderedLanguage() throws NodeException {
			NodeObject renderedObject = wrapped.getObject();
			if (renderedObject instanceof Page) {
				if (((Page) renderedObject).getLanguage() == null) {
					return null;
				} else {
					return language;
				}
			} else if (renderedObject instanceof File) {
				return null;
			} else {
				return language;
			}
		}

		@Override
		public String toString() {
			return String.format("Scheduled %s, language %s", wrapped, language);
		}
	}

	/**
	 * Encapsulation of a link to another object in Mesh
	 */
	protected static class MeshLink {
		/**
		 * Target CMS Object
		 */
		protected NodeObject object;

		/**
		 * Mesh UUID of the target object
		 */
		protected String meshUuid;

		/**
		 * Create instance
		 * @param object target object
		 * @throws NodeException
		 */
		protected MeshLink(NodeObject object) throws NodeException {
			this.object = object;
			this.meshUuid = getMeshUuid(object);
		}
	}

	/**
	 * Class for mesh objects loaded via graphql
	 */
	protected class MeshObject {
		/**
		 * Id of the object in GCMS
		 */
		protected int cmsId;

		/**
		 * Language
		 */
		protected String language;

		/**
		 * Parent UUID
		 */
		protected String parentUuid;

		/**
		 * Project, from which the object was loaded
		 */
		protected MeshProject project;

		/**
		 * Create an instance from the given JsonObject
		 * @param project project
		 * @param element Json object
		 */
		public MeshObject(MeshProject project, JsonObject element) {
			this.project = project;
			language = element.getString("language");
			JsonObject parent = element.getJsonObject("parent");
			if (parent != null) {
				parentUuid = parent.getString("uuid");
			}
			JsonObject fields = element.getJsonObject("fields");
			if (fields != null) {
				cmsId = ObjectTransformer.getInt(fields.getInteger("cms_id", 0), 0);
			}
		}

		/**
		 * Create an instance
		 * @param cmsId cms ID
		 * @param language language
		 * @param parentUuid parent UUID
		 * @param project project
		 */
		public MeshObject(int cmsId, String language, String parentUuid, MeshProject project) {
			this.cmsId = cmsId;
			this.language = language;
			this.parentUuid = parentUuid;
			this.project = project;
		}

		/**
		 * Get the cms Id
		 * @return cms Id
		 */
		public int getCmsId() {
			return cmsId;
		}

		/**
		 * Get the language
		 * @return language
		 */
		public String getLanguage() {
			return language;
		}

		/**
		 * Get the parent UUID
		 * @return parent UUID (may be null)
		 */
		public String getParentUuid() {
			return parentUuid;
		}

		/**
		 * Get the project
		 * @return project
		 */
		public MeshProject getProject() {
			return project;
		}
	}

	/**
	 * Encapsulation of a mesh project
	 */
	protected class MeshProject {
		/**
		 * Project name
		 */
		protected String name;

		/**
		 * Project hostname
		 */
		protected String hostname;

		/**
		 * Project path prefix
		 */
		protected String pathPrefix;

		/**
		 * Project https
		 */
		protected Boolean https;

		/**
		 * Project UUID
		 */
		protected String uuid;

		/**
		 * Node to which the project belongs (if projectPerNode is activated)
		 */
		protected Node node;

		/**
		 * Root Node UUID of the project
		 */
		protected String rootNodeUuid;

		/**
		 * Project's default branch
		 */
		protected BranchResponse defaultBranch;

		/**
		 * versioning parameters containing the default branch
		 */
		protected VersioningParameters defaultBranchParameter;

		/**
		 * Map of channel -&gt; branch
		 */
		protected Map<Node, BranchResponse> branchMap = new HashMap<>();

		/**
		 * Map of nodeId -&gt; versioning parameters containing the branch
		 */
		protected Map<Integer, VersioningParameters> branchParamMap = new HashMap<>();

		/**
		 * Collect the roles with read permission on project and branches which were already set.
		 */
		protected Set<String> rolesWithPermissions = new HashSet<>();

		/**
		 * UUID of the "latest" tag
		 */
		protected String latestTagUuid;

		/**
		 * Create an instance for a single node
		 * @param node node
		 */
		protected MeshProject(Node node) throws RuntimeException {
			try {
				this.node = node;
				name = getMeshName(node);

				// prepare all channels
				node.getAllChannels().stream().filter(n -> !n.isPublishDisabled() && n.doPublishContentmap()).forEach(n -> branchMap.put(n, null));

				hostname = node.getHostname();
				pathPrefix = getPathPrefix(node);
				https = node.isHttps();
				uuid = getMeshUuid(node);
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Create an instance with given name
		 * @param name name
		 */
		protected MeshProject(String name) {
			this.name = name;
		}

		/**
		 * Check whether the project exists
		 * @return true if the project exists
		 */
		protected boolean exists() {
			Optional<ProjectResponse> projectResult = null;
			if (uuid == null) {
				projectResult = client.findProjectByName(name).toSingle().map(Optional::of).onErrorResumeNext(t -> {
					return ifNotFound(t, () -> {
						return Single.just(Optional.empty());
					});
				}).blockingGet();
			} else {
				projectResult = client.findProjectByUuid(uuid).toSingle().map(Optional::of).onErrorResumeNext(t -> {
					return ifNotFound(t, () -> {
						return Single.just(Optional.empty());
					});
				}).blockingGet();
			}
			return projectResult != null && projectResult.isPresent();
		}

		/**
		 * Validate the mesh project. Checks for
		 * <ul>
		 * <li>Existence</li>
		 * <li>Assignment of schemas</li>
		 * </ul>
		 * @param schemaMap schema map
		 * @param repair true to repair invalid projects
		 * @param success success flag (will be set to false, if invalid projects found, that cannot be repaired)
		 * @throws NodeException
		 */
		protected void validate(Map<String, SchemaResponse> schemaMap, boolean repair, AtomicBoolean success) throws NodeException {
			info(String.format("Start checking project %s", name));

			Function<? super Throwable, ? extends SingleSource<? extends Optional<ProjectResponse>>> errorHandler = t -> {
				return ifNotFound(t, () -> {
					if (repair) {
						info(String.format("Creating project %s", name));
						ProjectCreateRequest request = new ProjectCreateRequest();
						request.setName(name);
						if (hostname != null) {
							request.setHostname(hostname);
						}
						if (pathPrefix != null) {
							request.setPathPrefix(pathPrefix);
						}
						if (https != null) {
							request.setSsl(https);
						}
						request.setSchema(new SchemaReferenceImpl().setName(getSchemaName(Folder.TYPE_FOLDER)));
						if (uuid != null) {
							return client.createProject(uuid, request).toSingle().map(Optional::of);
						} else {
							return client.createProject(request).toSingle().map(Optional::of);
						}
					} else {
						if (uuid != null) {
							error(String.format("Did not find project %s with uuid %s", name, uuid));
						} else {
							error(String.format("Did not find project %s", name));
						}
						success.set(false);
						return Single.just(Optional.empty());
					}
				});
			};

			// check existence of project
			Optional<ProjectResponse> projectResult = null;
			if (uuid != null) {
				projectResult = client.findProjectByUuid(uuid).toSingle().map(Optional::of).onErrorResumeNext(errorHandler).blockingGet();
			} else {
				projectResult = client.findProjectByName(name).toSingle().map(Optional::of).onErrorResumeNext(errorHandler).blockingGet();
			}

			// check project branches (name, hostname, http/s) and assignment of schemas
			if (projectResult != null && projectResult.isPresent()) {
				ProjectResponse project = projectResult.get();

				// read roles with read permission
				rolesWithPermissions.addAll(client.getProjectRolePermissions(project.getUuid()).blockingGet().getRead().stream()
						.map(RoleReference::getName).collect(Collectors.toSet()));

				String currentProjectName = project.getName();
				rootNodeUuid = project.getRootNode().getUuid();
				if (node != null) {
					if (cr.isProjectPerNode()) {
						rootFolderUuid.put(node, rootNodeUuid);
					} else {
						rootFolderUuid.remove(node);
					}
				}
				uuid = project.getUuid();

				// get implementation version from CR
				String implementationVersion = cr.getVersion();

				// check for existence of tag families
				checkTagFamilies(currentProjectName, repair, success);

				// check schema assignment to project
				Set<String> projectSchemas = client.findSchemas(currentProjectName).toSingle().retry(RETRY_HANDLER)
						.blockingGet().getData().stream().map(SchemaResponse::getName).collect(Collectors.toSet());

				for (int objectType : schemaNames.keySet()) {
					String schemaName = getSchemaName(objectType);
					if (!projectSchemas.contains(schemaName)) {
						if (repair) {
							info(String.format("Assigning Schema %s to project %s", schemaName, name));
							client.assignSchemaToProject(currentProjectName, schemaMap.get(schemaName).getUuid())
									.toSingle().retry(RETRY_HANDLER).blockingGet();
						} else {
							error(String.format("Schema %s not assigned to project %s", schemaName, name));
							success.set(false);
						}
					}
				}

				// check microschema assignment to project
				micronodePublisher.checkMicroschemaAssignment(currentProjectName, repair, success).blockingAwait();

				// get all branches
				BranchListResponse branches = client.findBranches(currentProjectName).toSingle().retry(RETRY_HANDLER).blockingGet();

				// here we collect the current latest branches per channel
				BranchResponse latestDefaultBranch = null;
				Map<Node, BranchResponse> latestChannelBranches = new HashMap<>();

				for (BranchResponse branch : branches.getData()) {
					// collect the latest branches
					if (hasTag(branch, GCMS_INTERNAL_TAGFAMILY, LATEST_TAG)) {
						Optional<TagReference> channelTag = getTag(branch, CHANNEL_UUID_TAGFAMILY);
						if (channelTag.isPresent()) {
							for (Node channel : branchMap.keySet()) {
								if (channelTag.get().getName().equals(getMeshUuid(channel))) {
									latestChannelBranches.put(channel, branch);
									break;
								}
							}
						} else {
							latestDefaultBranch = branch;
						}
					}

					if (isDefaultBranch(branch, currentProjectName, implementationVersion)) {
						defaultBranch = branch;
					} else {
						for (Map.Entry<Node, BranchResponse> entry : branchMap.entrySet()) {
							Node channel = entry.getKey();
							if (isChannelBranch(branch, channel, implementationVersion)) {
								String branchName = getBranchName(channel, implementationVersion);
								entry.setValue(validateBranch(branch, currentProjectName, branchName, channel, implementationVersion,
										channel.getHostname(), getPathPrefix(channel), channel.isHttps(), schemaMap, repair, success));
							}
						}
					}
				}

				// when we found no default branch for the implementation version, we create a new branch
				if (defaultBranch == null && !StringUtils.isEmpty(implementationVersion)) {
					defaultBranch = createBranch(currentProjectName, getBranchName(name, implementationVersion), hostname, pathPrefix, ssl, latestDefaultBranch, true);
				}

				// check default branch
				if (defaultBranch == null) {
					error("Could not find default branch");
					success.set(false);
				} else {
					// check project name
					if (!project.getName().equals(name)) {
						if (repair) {
							info(String.format("Changing name of project %s to %s", project.getName(), name));

							// also update default branch
							BranchUpdateRequest updateBranch = new BranchUpdateRequest();
							updateBranch.setName(name);

							project = client.updateBranch(project.getName(), defaultBranch.getUuid(), updateBranch)
									.toSingle().flatMap(r -> {
										defaultBranch = r;
										return client.updateProject(uuid, new ProjectUpdateRequest().setName(name))
												.toSingle();
									}).blockingGet();

							currentProjectName = project.getName();
						} else {
							error(String.format("Project %s should be named %s", project.getName(), name));
							success.set(false);
						}
					}

					defaultBranch = validateBranch(defaultBranch, currentProjectName,
							null, null, implementationVersion, hostname,
							pathPrefix, https, schemaMap, repair, success);
					defaultBranchParameter = new VersioningParametersImpl().setBranch(defaultBranch.getName());
				}

				// check whether all required branches were found
				for (Map.Entry<Node, BranchResponse> entry : branchMap.entrySet()) {
					Node channel = entry.getKey();
					if (entry.getValue() == null) {
						if (repair) {
							info(String.format("Creating branch for channel %s", channel));
							String branchName = getBranchName(channel, implementationVersion);

							entry.setValue(validateBranch(
									createBranch(currentProjectName, branchName, channel.getHostname(), getPathPrefix(channel), channel.isHttps(),
											latestChannelBranches.get(channel), !StringUtils.isEmpty(implementationVersion)),
									currentProjectName, branchName, channel, implementationVersion, null, getPathPrefix(channel), null, schemaMap, true, success));

							branchParamMap.put(channel.getId(), new VersioningParametersImpl().setBranch(branchName));
						} else {
							error(String.format("Could not find branch for channel %s", channel));
							success.set(false);
						}
					} else {
						branchParamMap.put(channel.getId(), new VersioningParametersImpl().setBranch(entry.getValue().getName()));
					}
				}

				// if feature forms is activated, check whether plugin is active for node
				if (node != null && NodeConfigRuntimeConfiguration.isFeature(Feature.FORMS, node)) {
					List<String> expectedLanguages = node.getLanguages().stream().map(ContentLanguage::getCode).collect(Collectors.toList());
					String languagesUrl = String.format("/%s/plugins/forms/languages", encodeSegment(currentProjectName));

					info("Checking forms plugin for project %s", currentProjectName);
					FormsPluginStatusResponse formsStatus = client
							.get(String.format("/%s/plugins/forms/active", encodeSegment(currentProjectName)),
									FormsPluginStatusResponse.class)
							.toSingle().onErrorReturn(t -> {
								return ifNotFound(t, () -> {
									error("Could not check forms plugin. Maybe plugin is not deployed or incompatible.");
									FormsPluginStatusResponse response = new FormsPluginStatusResponse();
									response.setActive(false);
									return response;
								});
							}).blockingGet();
					if (formsStatus.getActive() == Boolean.TRUE) {
						info("Forms plugin is active for project %s", currentProjectName);

						FormsPluginLanguages formsPluginLanguages = client.get(languagesUrl, FormsPluginLanguages.class)
								.blockingGet();
						if (!Objects.deepEquals(expectedLanguages, formsPluginLanguages.getLanguages())) {
							if (repair) {
								info("Setting forms plugin languages to %s for project %s", expectedLanguages, currentProjectName);
								formsPluginLanguages.setLanguages(expectedLanguages);
								client.put(languagesUrl, formsPluginLanguages, FormsPluginLanguages.class).blockingAwait();
							} else {
								error("Forms plugin languages for project %s should be %s, but are %s",
										currentProjectName, expectedLanguages, formsPluginLanguages.getLanguages());
								success.set(false);
							}
						}
					} else {
						if (repair) {
							info("Activating forms plugin for project %s", currentProjectName);
							formsStatus = client
									.put(String.format("/%s/plugins/forms/active", encodeSegment(currentProjectName)),
											FormsPluginStatusResponse.class)
									.blockingGet();
							if (formsStatus.getActive() == Boolean.TRUE) {
								info("Activated forms plugin for project %s", currentProjectName);

								info("Setting forms plugin languages to %s for project %s", expectedLanguages, currentProjectName);
								FormsPluginLanguages formsPluginLanguages = new FormsPluginLanguages().setLanguages(expectedLanguages);
								client.put(languagesUrl, formsPluginLanguages, FormsPluginLanguages.class).blockingAwait();
							} else {
								error("Could not activate forms plugin for project %s", currentProjectName);
								success.set(false);
							}
						} else {
							error("Forms plugin is not active for project %s", currentProjectName);
							success.set(false);
						}
					}
				}

				if (success.get()) {
					info(String.format("Project %s is valid", name));
				} else {
					error(String.format("Project %s is not valid", name));
				}
			}
			info("--");
		}

		/**
		 * Validate the branch. This will check whether the branch name, hostname and https setting are correct.
		 * Check whether latest schema versions are assigned to the branch
		 *
		 * @param branch branch
		 * @param currentProjectName current project name (can be different from the required project name, if master node was renamed)
		 * @param branchName expected branch name (may be null for the default branch)
		 * @param channel channel to which the branch belongs (null for default branch)
		 * @param implementationVersion implementation version
		 * @param hostname expected hostname
		 * @param pathPrefix expected path prefix (if not null)
		 * @param https expected https setting
		 * @param schemaMap schema map
		 * @param repair true to repair incorrect settings
		 * @param success atomic boolean (will be set to "false" in case of errors)
		 * @return branch (which was repaired, if necessary and requested)
		 * @throws NodeException
		 */
		protected BranchResponse validateBranch(BranchResponse branch, String currentProjectName, String branchName, Node channel, String implementationVersion,
				String hostname, String pathPrefix, Boolean https, Map<String, SchemaResponse> schemaMap, boolean repair, AtomicBoolean success) throws NodeException {
			pathPrefix = cleanPathPrefix(pathPrefix);
			BranchUpdateRequest update = null;
			String branchUuid = branch.getUuid();

			if (branchName != null && !StringUtils.isEqual(branchName, branch.getName())) {
				if (repair) {
					info(String.format("Changing name of branch %s to %s", branch.getName(), branchName));
					update = new BranchUpdateRequest();
					update.setName(branchName);
				} else {
					error(String.format("Branch %s should be named %s", branch.getName(), branchName));
					success.set(false);
				}
			}

			if (hostname != null && !StringUtils.isEqual(hostname, branch.getHostname())) {
				if (repair) {
					if (branchName != null) {
						info(String.format("Changing hostname of branch %s to %s", branchName, hostname));
					} else {
						info(String.format("Changing hostname of project %s to %s", name, hostname));
					}
					if (update == null) {
						update = new BranchUpdateRequest();
					}
					update.setHostname(hostname);
				} else {
					if (branchName != null) {
						error(String.format("Hostname of branch %s is %s, but should be %s", branchName, branch.getHostname(), hostname));
					} else {
						error(String.format("Hostname of project %s is %s, but should be %s", name, branch.getHostname(), hostname));
					}
					success.set(false);
				}
			}

			if (pathPrefix != null && !StringUtils.isEqual(pathPrefix, branch.getPathPrefix())) {
				if (repair) {
					if (branchName != null) {
						info(String.format("Changing path prefix of branch %s to '%s'", branchName, pathPrefix));
					} else {
						info(String.format("Changing path prefix of project %s to '%s'", name, pathPrefix));
					}
					if (update == null) {
						update = new BranchUpdateRequest();
					}
					update.setPathPrefix(pathPrefix);
				} else {
					if (branchName != null) {
						error(String.format("Path prefix of branch %s is '%s', but should be '%s'", branchName, branch.getPathPrefix(), pathPrefix));
					} else {
						error(String.format("Path prefix of project %s is '%s', but should be '%s'", name, branch.getPathPrefix(), pathPrefix));
					}
					success.set(false);
				}
			}

			if (https != null && https != branch.getSsl()) {
				if (repair) {
					if (branchName != null) {
						info(String.format("Changing SSL of branch %s to %s", branchName, https));
					} else {
						info(String.format("Changing SSL of project %s to %s", name, https));
					}
					if (update == null) {
						update = new BranchUpdateRequest();
					}
					update.setSsl(https);
				} else {
					if (branchName != null) {
						error(String.format("SSL of branch %s is %s, but should be %s", branchName, branch.getSsl(), https));
					} else {
						error(String.format("SSL of project %s is %s, but should be %s", name, branch.getSsl(), https));
					}
					success.set(false);
				}
			}

			if (branchName == null && !branch.getLatest()) {
				if (repair) {
					info(String.format("Making default branch of project %s the latest branch", name));
					branch = client.setLatestBranch(currentProjectName, branchUuid).toSingle().retry(RETRY_HANDLER)
							.blockingGet();
				} else {
					error(String.format("Default branch of project %s should be latest branch, but is not", name));
					success.set(false);
				}
			}

			// check required tags
			TagListUpdateRequest request = new TagListUpdateRequest();

			// when implementation version is not empty, the branch must be tagged with it
			if (!StringUtils.isEmpty(implementationVersion) && !hasTag(branch, IMPLEMENTATION_VERSION_TAGFAMILY)) {
				request.getTags().add(new TagReference().setTagFamily(IMPLEMENTATION_VERSION_TAGFAMILY).setName(implementationVersion));
				request.getTags().add(new TagReference().setTagFamily(GCMS_INTERNAL_TAGFAMILY).setName(LATEST_TAG));
			}

			// branches for channels must be tagged with the channel UUID
			if (channel != null && !hasTag(branch, CHANNEL_UUID_TAGFAMILY)) {
				request.getTags().add(new TagReference().setTagFamily(CHANNEL_UUID_TAGFAMILY).setName(getMeshUuid(channel)));
			}

			if (!request.getTags().isEmpty()) {
				for (TagReference ref : branch.getTags()) {
					request.getTags().add(ref);
				}

				client.updateTagsForBranch(currentProjectName, branchUuid, request).toSingle().retry(RETRY_HANDLER)
						.blockingGet();
			}

			// check schema versions
			BranchInfoSchemaList schemaInfoList = client.getBranchSchemaVersions(currentProjectName, branchUuid)
					.toSingle().retry(RETRY_HANDLER).blockingGet();
			BranchInfoSchemaList updateList = new BranchInfoSchemaList();
			for (BranchSchemaInfo schemaInfo : schemaInfoList.getSchemas()) {
				SchemaResponse schema = schemaMap.get(schemaInfo.getName());
				if (schema != null) {
					if (!StringUtils.isEqual(schema.getVersion(), schemaInfo.getVersion())) {
						// schema version not equal
						if (repair) {
							info(String.format("Updating schema version of schema %s to %s in branch %s", schema.getName(), schema.getVersion(), branchName));
							updateList.add(new SchemaReferenceImpl().setUuid(schema.getUuid()).setVersion(schema.getVersion()));
						} else {
							error(String.format("Branch %s has version %s of schema %s assigned, but latest schema version is %s", branchName,
									schemaInfo.getVersion(), schema.getName(), schema.getVersion()));
							success.set(false);
						}
					}
				}
			}

			// when at least one schema version needs to be updated, we do this now, and then start the migration
			if (!updateList.getSchemas().isEmpty()) {
				client.assignBranchSchemaVersions(currentProjectName, branchUuid, updateList).toCompletable()
						.retry(RETRY_HANDLER).blockingAwait();
			}

			// check microschema versions
			micronodePublisher.checkMicroschemaBranchVersions(currentProjectName, branchUuid, branchName, repair, success).blockingAwait();

			if (update != null) {
				return client.updateBranch(currentProjectName, branch.getUuid(), update).toSingle().retry(RETRY_HANDLER)
						.blockingGet();
			} else {
				return branch;
			}
		}

		/**
		 * Check schema migrations for all branches in the project
		 * @param unmigratedSchemas map of unmigrated schemas per branch name
		 * @return true if all done
		 * @throws NodeException
		 */
		protected boolean checkSchemaMigrations(Map<String, Map<String, Long>> unmigratedSchemas) throws NodeException {
			boolean allDone = true;

			List<BranchResponse> branches = new ArrayList<>();
			if (defaultBranch != null) {
				branches.add(defaultBranch);
			}
			branches.addAll(branchMap.values());

			for (BranchResponse branch : branches) {
				if (!checkSchemaMigrations(branch, unmigratedSchemas.computeIfAbsent(branch.getName(), key -> new HashMap<>()))) {
					allDone = false;
				}
			}

			return allDone;
		}

		/**
		 * Check the schema migrations of the branch
		 * @param branch branch
		 * @param unmigratedSchemas map of unmigrated schemas
		 * @return true if all migrations are done
		 * @throws NodeException
		 */
		protected boolean checkSchemaMigrations(BranchResponse branch, Map<String, Long> unmigratedSchemas) throws NodeException {
			boolean allDone = true;

			BranchInfoSchemaList schemaStatusList = client.getBranchSchemaVersions(name, branch.getUuid()).blockingGet();
			for (BranchSchemaInfo info : schemaStatusList.getSchemas()) {
				switch (info.getMigrationStatus()) {
				case COMPLETED:
				case UNKNOWN:
					Long startTimestamp = unmigratedSchemas.get(info.getName());
					if (startTimestamp != null) {
						long duration = System.currentTimeMillis() - startTimestamp;
						info(String.format("Branch %s: Migration status for schema %s, version %s changed to %s after waiting %d seconds", branch.getName(),
								info.getName(), info.getVersion(), info.getMigrationStatus().name(),
								TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)));
						unmigratedSchemas.remove(info.getName());
					}
					break;
				case FAILED:
					throw new NodeException(String.format("Branch %s: Migration status for schema %s, version %s is %s", branch.getName(), info.getName(),
							info.getVersion(), info.getMigrationStatus().name()));
				default:
					unmigratedSchemas.computeIfAbsent(info.getName(), schemaName -> {
						info(String.format("Branch %s: Migration status for schema %s, version %s is %s", branch.getName(), info.getName(), info.getVersion(),
								info.getMigrationStatus().name()));
						return System.currentTimeMillis();
					});
					allDone = false;
					break;
				}
			}

			allDone &= micronodePublisher.checkMicroschemaMigrations(this, branch, unmigratedSchemas);

			return allDone;
		}

		/**
		 * Check whether all branches have been migrated
		 * @param unmigratedBranchs map of unmigrated branches
		 * @return true if all migrations are done
		 * @throws NodeException
		 */
		protected boolean checkNodeMigrations(Map<String, Long> unmigratedBranchs) throws NodeException {
			boolean allDone = true;
			BranchListResponse branchList = client.findBranches(name).blockingGet();
			for (BranchResponse branch : branchList.getData()) {
				if (branch.getMigrated()) {
					Long startTimestamp = unmigratedBranchs.get(branch.getName());
					if (startTimestamp != null) {
						long duration = System.currentTimeMillis() - startTimestamp;
						info(String.format("Branch %s is migrated after waiting %d seconds", branch.getName(),
								TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)));
						unmigratedBranchs.remove(branch.getName());
					}
				} else {
					unmigratedBranchs.computeIfAbsent(branch.getName(), name -> {
						info(String.format("Branch %s is not fully migrated", branch.getName()));
						return System.currentTimeMillis();
					});
					allDone = false;
				}
			}

			return allDone;
		}

		/**
		 * Get versioning parameters containing the branch for the given nodeId.
		 * @param nodeId node ID
		 * @return versioning parameters
		 */
		protected VersioningParameters enforceBranch(int nodeId) {
			return branchParamMap.getOrDefault(nodeId, defaultBranchParameter);
		}

		/**
		 * Create a branch and wait until the node migration to the branch is done
		 * @param currentProjectName current project name
		 * @param branchName branch name
		 * @param host branch hostname
		 * @param pathPrefix optional path prefix
		 * @param ssl ssl setting
		 * @param baseBranch optional base branch
		 * @param tagAsLatest flag to tag the branch as "latest"
		 * @return created branch
		 * @throws NodeException
		 */
		protected BranchResponse createBranch(String currentProjectName, String branchName, String host, String pathPrefix, boolean ssl, BranchResponse baseBranch, boolean tagAsLatest) throws NodeException {
			pathPrefix = cleanPathPrefix(pathPrefix);
			BranchCreateRequest create = new BranchCreateRequest().setName(branchName).setHostname(host).setPathPrefix(pathPrefix)
					.setSsl(ssl).setLatest(false);

			if (baseBranch != null) {
				create.setBaseBranch(new BranchReference().setUuid(baseBranch.getUuid()));
			}

			BranchResponse branch = client.createBranch(currentProjectName, create).blockingGet();

			// set the permissions on the branch
			if (!rolesWithPermissions.isEmpty()) {
				List<RoleReference> roleReferences = rolesWithPermissions.stream()
						.map(roleName -> new RoleReference().setName(roleName)).collect(Collectors.toList());
				client.grantBranchRolePermissions(currentProjectName, branch.getUuid(), new ObjectPermissionGrantRequest().setRead(roleReferences)).blockingAwait();
			}

			if (tagAsLatest) {
				branch = client.addTagToBranch(currentProjectName, branch.getUuid(), latestTagUuid).blockingGet();
				if (baseBranch != null) {
					client.removeTagFromBranch(currentProjectName, baseBranch.getUuid(), latestTagUuid).blockingAwait();
				}
			}

			if (!branch.getMigrated()) {
				branch = client.findBranchByUuid(currentProjectName, branch.getUuid()).blockingGet();
			}

			while (!branch.getMigrated()) {
				try {
					Thread.sleep(POLL_INTERVAL_MS);
					branch = client.findBranchByUuid(currentProjectName, branch.getUuid()).blockingGet();
				} catch (InterruptedException e) {
					throw new NodeException(String.format("Interrupted while waiting for node migration for branch %s", branchName), e);
				}
			}

			// migrate schemas versions (if necessary)
			client.migrateBranchSchemas(currentProjectName, branch.getUuid()).blockingAwait();

			return branch;
		}

		/**
		 * Set the role permissions on the project, its branches and its root node.
		 *
		 * <p>
		 *     Permissions are only set for roles which are not already in
		 *     {@link #rolesWithPermissions}. After the missing permissions are
		 *     set those are added to {@link #rolesWithPermissions}.
		 * </p>
		 *
		 * @param roles The roles for which to set the read permission.
		 * @return completable
		 */
		protected Completable setPermissions(Collection<String> roles) {
			if (CollectionUtils.isEmpty(roles)) {
				return Completable.complete();
			}
			Set<String> missingPermissions = new HashSet<>(roles);

			missingPermissions.removeAll(rolesWithPermissions);

			if (missingPermissions.isEmpty()) {
				return Completable.complete();
			}

			Set<String> branchUuids = new HashSet<>();
			branchUuids.addAll(branchMap.values().stream().map(BranchResponse::getUuid).collect(Collectors.toSet()));
			if (defaultBranch != null) {
				branchUuids.add(defaultBranch.getUuid());
			}

			List<RoleReference> roleReferences = missingPermissions.stream()
					.map(roleName -> new RoleReference().setName(roleName)).collect(Collectors.toList());

			List<Completable> completables = new ArrayList<>();
			// set read on project
			completables.add(client.grantProjectRolePermissions(uuid, new ObjectPermissionGrantRequest().setRead(roleReferences)).toCompletable());

			// set read on all branches
			completables.addAll(branchUuids.stream()
					.map(branchUuid -> client.grantBranchRolePermissions(name, branchUuid,
							new ObjectPermissionGrantRequest().setRead(roleReferences)).toCompletable())
					.collect(Collectors.toList()));

			// set read_published on the root node
			completables.add(client.grantNodeRolePermissions(name, rootNodeUuid,
							new ObjectPermissionGrantRequest()
								.setReadPublished(roleReferences)
						).toCompletable());

			return Completable.merge(completables)
				.andThen(Completable.fromAction(() -> rolesWithPermissions.addAll(missingPermissions)));
		}

		/**
		 * Check existence of tag families
		 * @param projectName current project name
		 * @param repair true to repair (i.e. generate missing tag families)
		 * @param success atomic boolean that will be set to false if tag family is missing and repair is false
		 */
		protected void checkTagFamilies(String projectName, boolean repair, AtomicBoolean success) {
			List<TagFamilyResponse> tagFamilies = client.findTagFamilies(projectName).toSingle().retry(RETRY_HANDLER)
					.blockingGet().getData();

			Map<String, String> tagFamilyUuids = new HashMap<>();

			for (String familyName : Arrays.asList(IMPLEMENTATION_VERSION_TAGFAMILY, CHANNEL_UUID_TAGFAMILY, GCMS_INTERNAL_TAGFAMILY)) {
				Optional<TagFamilyResponse> optionalFamily = tagFamilies.stream().filter(fam -> familyName.equals(fam.getName())).findFirst();
				if (!optionalFamily.isPresent()) {
					if (repair) {
						info(String.format("Creating tag family %s", familyName));
						TagFamilyResponse response = client
								.createTagFamily(projectName, new TagFamilyCreateRequest().setName(familyName))
								.toSingle().retry(RETRY_HANDLER).blockingGet();
						tagFamilyUuids.put(familyName, response.getUuid());
					} else {
						error(String.format("Tag family %s does not exist", familyName));
						success.set(false);
					}
				} else {
					tagFamilyUuids.put(familyName, optionalFamily.get().getUuid());
				}
			}

			if (success.get()) {
				String internalTagFamilyUuid = tagFamilyUuids.get(GCMS_INTERNAL_TAGFAMILY);
				List<TagResponse> tags = client.findTags(projectName, internalTagFamilyUuid).toSingle()
						.retry(RETRY_HANDLER).blockingGet().getData();
				Optional<TagResponse> optionalLatestTag = tags.stream().filter(tag -> LATEST_TAG.equals(tag.getName())).findFirst();
				if (optionalLatestTag.isPresent()) {
					latestTagUuid = optionalLatestTag.get().getUuid();
				} else {
					if (repair) {
						info(String.format("Creating \"%s\" tag", LATEST_TAG));
						TagResponse response = client
								.createTag(projectName, internalTagFamilyUuid,
										new TagCreateRequest().setName(LATEST_TAG))
								.toSingle().retry(RETRY_HANDLER).blockingGet();
						latestTagUuid = response.getUuid();
					} else {
						error(String.format("Tag \"%s\" does not exist", LATEST_TAG));
						success.set(false);
					}
				}
			}
		}

		/**
		 * Check whether the branch is the default branch
		 * @param branch branch
		 * @param currentProjectName current project name
		 * @param implementationVersion implementation version
		 * @return true iff the branch is the default branch
		 */
		protected boolean isDefaultBranch(BranchResponse branch, String currentProjectName, String implementationVersion) {
			// branches, which are tagged for a channel cannot be the default branch (belonging to the node)
			if (hasTag(branch, CHANNEL_UUID_TAGFAMILY)) {
				return false;
			}

			if (!StringUtils.isEmpty(implementationVersion)) {
				// if an implementation version is set, the branch must be tagged with it
				return hasTag(branch, IMPLEMENTATION_VERSION_TAGFAMILY, implementationVersion);
			} else {
				// when using no implementation version, the branch must not have an implementation version tag and must be named after the project
				return !hasTag(branch, IMPLEMENTATION_VERSION_TAGFAMILY) && currentProjectName.equals(branch.getName());
			}
		}

		/**
		 * Check whether the branch is the branch of the given channel
		 * @param branch branch
		 * @param channel channel
		 * @param implementationVersion implementation version
		 * @return true iff the branch is the branch for the channel
		 * @throws NodeException
		 */
		protected boolean isChannelBranch(BranchResponse branch, Node channel, String implementationVersion) throws NodeException {
			String channelUuidInMesh = getMeshUuid(channel);
			if (channelUuidInMesh.equals(branch.getUuid()) && !hasTag(branch, CHANNEL_UUID_TAGFAMILY)) {
				// this is the case, when migrating from old projects, where branches were not tagged at all
				return true;
			} else if (!StringUtils.isEmpty(implementationVersion)) {
				return hasTag(branch, CHANNEL_UUID_TAGFAMILY, channelUuidInMesh)
						&& hasTag(branch, IMPLEMENTATION_VERSION_TAGFAMILY, implementationVersion);
			} else {
				return hasTag(branch, CHANNEL_UUID_TAGFAMILY, channelUuidInMesh) && !hasTag(branch, IMPLEMENTATION_VERSION_TAGFAMILY);
			}
		}

		/**
		 * Get the path prefix for the given node
		 * @param node node
		 * @return path prefix
		 * @throws NodeException
		 */
		protected String getPathPrefix(Node node) throws NodeException {
			if (cr.isProjectPerNode() && node.getMaster().isPubDirSegment()) {
				return cleanPathPrefix(FilePublisher.getPath(true, false, node.getPublishDir(), node.getFolder().getPublishDir()));
			} else {
				return cleanPathPrefix(node.getPublishDir());
			}
		}
	}
}
