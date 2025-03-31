package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.channelsync;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.createfolder;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.createform;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.createitems;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.createoverview;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.createtemplates;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.delete;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.deletefolder;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.deleteform;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.deleteitems;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.deletetemplates;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.edit;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.formreport;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.importitems;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.linkoverview;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.linktemplates;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.publishform;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.publishpages;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.readitems;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.translatepages;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.update;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.updateconstructs;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.updatefolder;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.updateform;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.updateinheritance;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.updateitems;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.updatetemplates;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.view;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.viewform;
import static com.gentics.contentnode.perm.PermHandler.ObjectPermission.wastebin;
import static com.gentics.contentnode.rest.util.MiscUtils.checkFields;
import static com.gentics.contentnode.rest.util.MiscUtils.comparator;
import static com.gentics.contentnode.rest.util.MiscUtils.createNodeConflictMessage;
import static com.gentics.contentnode.rest.util.MiscUtils.getLanguage;
import static com.gentics.contentnode.rest.util.MiscUtils.getNode;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.require;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Timing;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.Editor;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.NodeFeatureModel;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.NodeCopyRequest;
import com.gentics.contentnode.rest.model.request.NodeFeatureRequest;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.FeatureList;
import com.gentics.contentnode.rest.model.response.FeatureModelList;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.model.response.LanguageListResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.NodeFeatureResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.NodeSettingsResponse;
import com.gentics.contentnode.rest.model.response.PagedConstructListResponse;
import com.gentics.contentnode.rest.model.response.PagedObjectPropertyListResponse;
import com.gentics.contentnode.rest.model.response.PagedTemplateListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.NodeObjectFilter;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.PermissionFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlet.queue.NodeCopyQueueEntry;
import com.gentics.contentnode.staging.StagingUtil;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.ClassHelper;

/**
 * NodeResource Implementation
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("node")
public class NodeResourceImpl extends AbstractContentNodeResource implements NodeResource {

	private final static String CONFIG_NODE_SETTINGS = "node_settings";
	private final static String CONFIG_NODE_SETTINGS_GLOBAL = "node_settings_global";
	private final static String REGEX_KEY_PATH = "regex.24";
	private final static String REGEX_KEY_HOSTNAME = "regex.25";
	private final static String PUB_DIR_TABLE = "pub_dir";
	private final static String PUB_DIR_BIN_TABLE = "pub_dir_bin";
	private final static int NO_CONFLICT = 0;

	protected static NodeLogger logger = NodeLogger.getNodeLogger(NodeResourceImpl.class);

	/**
	 * Fixes the given path string.
	 *
	 * If the string is either <code>null</code> or empty the
	 * resulting path is just <code>""</code>.
	 *
	 * Otherwise this method makes sure, that the path starts
	 * with, but does not end with a <code>"/"</code> character.
	 *
	 * @param path The path string to check
	 * @return A string containing just <code>"/"</code> if
	 *		<code>path</code> is <code>null</code> or empty,
	 *		and the given path with a leading but no trailing
	 *		<code>"/"</code> character.
	 */
	private String fixPath(String path) {
		if (ObjectTransformer.isEmpty(path)) {
			return "";
		}

		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		return path;
	}

	/**
	 * Checks if the information in the given {@link NodeSaveRequest request}
	 * is consistent.
	 *
	 * This method also makes sure that the
	 * {@link com.gentics.contentnode.rest.model.Node#getPublishDir publish directory}
	 * and the
	 * {@link com.gentics.contentnode.rest.model.Node#getBinaryPublishDir binary publish directory}
	 * start with a <code>"/"</code> character but do not end with one.
	 *
	 * The following checks are performed:
	 * <ul>
	 *  <li>if provided, there must not exist a node with the same
	 *      {@link com.gentics.contentnode.rest.model.Node#getName channel name}</li>
	 *  <li>if provided, the
	 *      {@link com.gentics.contentnode.rest.model.Node#getHost hostname}
	 *      must be a valid hostname</li>
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getPublishDir publish directory}
	 *      must be a valid UNIX directory.
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getBinaryPublishDir binary publish directory}
	 *      must be a valid UNIX directory.
	 *  <li>there must exist no other node with the same hostname and
	 *      (binary) publish directory</li>
	 * </ul>
	 *
	 * @param node The node being edited, or <code>null</code> if checking consistency
	 *		of a node creation request.
	 * @param request The request to check.
	 * @param response A response to gather potential error messages.
	 * @return <code>true</code> if all checks were  successful,
	 *		<code>false</code> otherwise.
	 * @throws NodeException On database and transaction errors.
	 */
	private boolean checkRequestConsistency(
			Node node,
			NodeSaveRequest request,
			GenericResponse response)
			throws NodeException {
		CNI18nString msg;
		StringJoiner errors = new StringJoiner(",", "Invalid request: [", "]");
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.rest.model.Node reqNode = request.getNode();
		boolean ret = true;
		String nodeName = node == null ? null : node.getFolder().getName();

		if (!ObjectTransformer.isEmpty(reqNode.getName()) && !reqNode.getName().equals(nodeName)) {
			String sql = "SELECT id FROM folder WHERE id <> ? AND mother = 0 AND name = ?";
			String[] args = {
				node == null ? "0" : node.getId().toString(),
				reqNode.getName()
			};

			if (DBUtils.executeSelectAndCountRows(sql, args) > 0) {
				errors.add("node with this name exists");
				msg = new CNI18nString("a_node_with_this_name");
				response.addMessage(
					new Message(Type.CRITICAL, "name", msg.toString()));
				ret = false;
			}
		}

		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		String hostname = reqNode.getHost();

		if (!ObjectTransformer.isEmpty(hostname)
				&& !hostname.matches(prefs.getProperty(REGEX_KEY_HOSTNAME))) {
			errors.add("no hostname");
			msg = new CNI18nString("domne_oder_ip_adresse.zb_www.gentics.com");
			response.addMessage(
				new Message(Type.CRITICAL, "host", msg.toString()));
			ret = false;
		}

		String dir;
		String pathRegex = prefs.getProperty(REGEX_KEY_PATH);

		if (reqNode.getPublishDir() != null) {
			dir = fixPath(reqNode.getPublishDir());

			if (!dir.matches(pathRegex)) {
				errors.add("invalid pathname (pubDir)");
				msg = new CNI18nString("verzeichnispfad_unix");
				response.addMessage(
						new Message(Type.CRITICAL, "publishDir", msg.toString()));
				ret = false;
			} else {
				reqNode.setPublishDir(dir);
			}
		}

		if (reqNode.getBinaryPublishDir() != null) {
			dir = fixPath(reqNode.getBinaryPublishDir());

			if (!dir.matches(pathRegex)) {
				errors.add("invalid pathname (binPubDir)");
				msg = new CNI18nString("verzeichnispfad_unix");
				response.addMessage(
						new Message(Type.CRITICAL, "binaryPublishDir", msg.toString()));
				ret = false;
			} else {
				reqNode.setBinaryPublishDir(dir);
			}
		}

		if (!ret) {
			logger.warn(errors.toString());
		}

		return ret;
	}

	/**
	 * Checks if the information in the given {@link NodeSaveRequest request}
	 * is consistent and sufficient to create a new {@link Node}.
	 *
	 * The following checks are performed (most of them by
	 * {@link #checkRequestConsistency}):
	 *
	 * <ul>
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getName channel name}
	 *      and there must not exist a node with the same name</li>
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getHost hostname}
	 *      must be provided and valid</li>
	 *  <li>if the {@link com.gentics.contentnode.rest.model.Node#getMasterId master id}
	 *      is set and greater than zero, there must exist a node
	 *      with this id</li>
	 *  <li>if the {@link com.gentics.contentnode.rest.model.Node#getDefaultFileFolderId() default file folder id}
	 *      is set and greater than zero, there must exist a folder
	 *      with this id</li>
	 *  <li>if the {@link com.gentics.contentnode.rest.model.Node#getDefaultImageFolderId() default image folder id}
	 *      is set and greater than zero, there must exist a folder
	 *      with this id</li>
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getPublishDir publish directory}
	 *      must be a valid UNIX directory.
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getBinaryPublishDir binary publish directory}
	 *      must be a valid UNIX directory.
	 *  <li>there must exist no other node with the same hostname and
	 *      (binary) publish directory</li>
	 *  <li>if the {@link Editor#AlohaEditor} is to be used,
	 *      {@link com.gentics.contentnode.rest.model.Node#isUtf8} must
	 *      be enabled too</li>
	 * </ul>
	 *
	 * @see #checkRequestConsistency
	 * @param request The request to check.
	 * @param response A response to gather potential error messages.
	 * @return <code>true</code> if all checks were  successful,
	 *		<code>false</code> otherwise.
	 * @throws NodeException On database and transaction errors.
	 */
	private boolean checkCreateRequest(NodeSaveRequest request, GenericResponse response)
			throws NodeException {
		CNI18nString msg;
		StringJoiner errors = new StringJoiner(",", "Invalid create request: [", "]");
		com.gentics.contentnode.rest.model.Node reqNode = request.getNode();
		boolean ret = true;

		if (ObjectTransformer.isEmpty(reqNode.getName())) {
			errors.add("no nodename");
			msg = new CNI18nString("text_mit_max._255_lnge");
			response.addMessage(
				new Message(Type.CRITICAL, "name", msg.toString()));
			ret = false;
		}

		if (org.apache.commons.lang3.StringUtils.isAllBlank(reqNode.getHost(), reqNode.getHostProperty())) {
			errors.add("no hostname");
			msg = new CNI18nString("domne_oder_ip_adresse.zb_www.gentics.com");
			response.addMessage(
				new Message(Type.CRITICAL, "host", msg.toString()));
			ret = false;
		}

		int id = ObjectTransformer.getInt(reqNode.getMasterId(), 0);

		if (id > 0) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder masterFolder = t.getObject(Folder.class, id);

			if (masterFolder == null || !masterFolder.isRoot()) {
				errors.add("invalid master id");
				msg = new CNI18nString("rest.node.invalid.masterid");
				response.addMessage(
						new Message(Type.CRITICAL, "masterId", msg.toString()));
				ret = false;
			}
		}

		if (ObjectTransformer.isEmpty(reqNode.getPublishDir())) {
			reqNode.setPublishDir("/");
		}

		if (ObjectTransformer.isEmpty(reqNode.getBinaryPublishDir())) {
			reqNode.setBinaryPublishDir("/");
		}

		Editor editor = Editor.getByCode(
			ObjectTransformer.getInt(reqNode.getEditorVersion(), Editor.LiveEditor.ordinal()));

		if (editor == Editor.AlohaEditor) {
			if (!ObjectTransformer.getBoolean(reqNode.isUtf8(), false)) {
				errors.add("Aloha needs UTF8");
				msg = new CNI18nString("rest.node.needutf8");
				response.addMessage(new Message(Type.CRITICAL, "utf8", msg.toString()));
				ret = false;
			}
		}

		if (!ret) {
			logger.warn(errors.toString());
		}

		// We could fail fast here by switching the operands, but we want
		// to send a response with all detected errors.
		return checkRequestConsistency(null, request, response) && ret;
	}

	/**
	 * Checks if the information in the given {@link NodeSaveRequest request}
	 * is consistent.
	 *
	 * The following checks are performed (most of them by
	 * @link {@link #checkRequestConsistency}):
	 *
	 * <ul>
	 *  <li>if provided, there must not exist a node with the same
	 *      {@link com.gentics.contentnode.rest.model.Node#getName channel name}</li>
	 *  <li>if provided, the
	 *      {@link com.gentics.contentnode.rest.model.Node#getHost hostname}
	 *      must be a valid hostname</li>
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getPublishDir publish directory}
	 *      must be a valid UNIX directory.
	 *  <li>the
	 *      {@link com.gentics.contentnode.rest.model.Node#getBinaryPublishDir binary publish directory}
	 *      must be a valid UNIX directory.
	 *  <li>there must exist no other node with the same hostname and
	 *      (binary) publish directory</li>
	 *  <li>if the {@link Editor#AlohaEditor} is to be used,
	 *      {@link com.gentics.contentnode.rest.model.Node#isUtf8} must
	 *      be enabled too</li>
	 *  <li>When the node shall be changed to use pubDirSegments, the existing segments of folders must be unique</li>
	 *  <li>When {@link com.gentics.contentnode.rest.model.Node#getPageLanguageCode() page language code placement} is changed from {@link PageLanguageCode#PATH} to something else,
	 *      the existing page URLs (including nice URLs) must still be unique</li>
	 * </ul>
	 *
	 * @param node The node beeing edited, or <code>null</code> if checking consistency
	 *		of a node creation request.
	 * @param request The request to check.
	 * @param response A response to gather potential error messages.
	 * @return <code>true</code> if all checks were  successful,
	 *		<code>false</code> otherwise.
	 * @throws NodeException On database and transaction errors.
	 */
	private boolean checkSaveRequest(Node node, NodeSaveRequest request, GenericResponse response) throws NodeException {
		CNI18nString msg;
		StringJoiner errors = new StringJoiner(",", "Invalid save request: [", "]");
		com.gentics.contentnode.rest.model.Node reqNode = request.getNode();
		boolean ret = checkRequestConsistency(node, request, response);
		boolean utf8 = ObjectTransformer.getBoolean(reqNode.isUtf8(), node.isUtf8());
		Integer editorCode = reqNode.getEditorVersion();

		if (editorCode == null) {
			editorCode = node.getEditorversion();
		}

		Editor editor = Editor.getByCode(editorCode);

		if (editor == null) {
			errors.add("invalid editor");
			msg = new CNI18nString("rest.node.invalid.editor");
			response.addMessage(new Message(Type.CRITICAL, "editorVersion", msg.toString()));
			ret = false;
		} else if (editor == Editor.AlohaEditor && !utf8) {
			errors.add("Aloha needs UTF8");
			msg = new CNI18nString("rest.node.needutf8");
			response.addMessage(new Message(Type.CRITICAL, "utf8", msg.toString()));
			ret = false;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		int id = ObjectTransformer.getInt(reqNode.getDefaultFileFolderId(), 0);

		if (id > 0) {
			Folder fileFolder = null;

			try (ChannelTrx trx = new ChannelTrx(node)) {
				fileFolder = t.getObject(Folder.class, id);
			}

			if (fileFolder == null) {
				errors.add("invalid default file folder id");
				msg = new CNI18nString("rest.node.invalid.folderid");
				response.addMessage(
						new Message(Type.CRITICAL, "defaultFileFolderId", msg.toString()));
				ret = false;
			}
		}

		id = ObjectTransformer.getInt(reqNode.getDefaultImageFolderId(), 0);
		if (id > 0) {
			Folder imageFolder = null;

			try (ChannelTrx trx = new ChannelTrx(node)) {
				imageFolder = t.getObject(Folder.class, id);
			}

			if (imageFolder == null) {
				errors.add("invalid default image folder id");
				msg = new CNI18nString("rest.node.invalid.folderid");
				response.addMessage(
						new Message(Type.CRITICAL, "defaultImageFolderId", msg.toString()));
				ret = false;
			}
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT) && !node.isPubDirSegment() && reqNode.isPubDirSegment() == Boolean.TRUE) {
			Folder pubDirSegmentConflict = FolderFactory.checkPubDirUniqueness(node.getFolder());
			if (pubDirSegmentConflict != null) {
				msg = new CNI18nString("rest.node.conflict.publishDirSegment");
				msg.addParameter(I18NHelper.getPath(pubDirSegmentConflict));
				response.addMessage(new Message(Type.CRITICAL, "pubDirSegment", msg.toString()));
				ret = false;
			}
			if (!node.isChannel()) {
				for (Node c : node.getAllChannels()) {
					try (ChannelTrx cTrx = new ChannelTrx(c)) {
						pubDirSegmentConflict = FolderFactory.checkPubDirUniqueness(c.getFolder());
						if (pubDirSegmentConflict != null) {
							msg = new CNI18nString("rest.node.conflict.publishDirSegment");
							msg.addParameter(I18NHelper.getPath(pubDirSegmentConflict));
							response.addMessage(new Message(Type.CRITICAL, "pubDirSegment", msg.toString()));
							ret = false;
							break;
						}
					}
				}
			}
		}

		// if the page language code placement is changed, check whether this would violate uniqueness of URLs
		if (!node.isChannel() && reqNode.getPageLanguageCode() != null && !Objects.equals(reqNode.getPageLanguageCode(), node.getPageLanguageCode())) {
			if (reqNode.getPageLanguageCode() == PageLanguageCode.PATH || node.getPageLanguageCode() == PageLanguageCode.PATH) {
				// check node
				NodeObject conflictingObject = checkURLUniqueness(node, reqNode.getPageLanguageCode());

				// check channels
				if (conflictingObject == null && !node.isChannel()) {
					for (Node c : node.getAllChannels()) {
						try (ChannelTrx cTrx = new ChannelTrx(c)) {
							conflictingObject = checkURLUniqueness(c, reqNode.getPageLanguageCode());
							if (conflictingObject != null) {
								break;
							}
						}
					}
				}

				// add error message, if conflicting object was found
				if (conflictingObject != null) {
					msg = new CNI18nString("rest.node.conflict.publishPath");
					msg.addParameter(I18NHelper.getPath(conflictingObject));
					response.addMessage(new Message(Type.CRITICAL, "pageLanguageCode", msg.toString()));
					ret = false;
				}
			}
		}

		if (!ret) {
			logger.warn(errors.toString());
		}

		return ret;
	}

	/**
	 * Disinherits all objects with the {@link Disinheritable#isDisinheritDefault}
	 * flag set.
	 *
	 * @param node The newly created node.
	 *
	 * @throws NodeException On error.
	 */
	private void disinheritObjects(Node node) throws NodeException {
		Queue<Folder> folders = new LinkedList<>();

		folders.add(node.getFolder());

		if (logger.isDebugEnabled()) {
			logger.debug("disinheriting objects for new node {"
				+ node.getFolder().getName() + ", " + node.getId() + "}");
		}

		Set<Node> disinheritedNodes;

		while (!folders.isEmpty()) {
			Folder folder = folders.remove().getMaster();

			if (folder.isDisinheritDefault()) {
				if (logger.isDebugEnabled()) {
					logger.debug("disinheriting folder {" + folder.getName()
						+ ", " + folder.getId() + "}");
				}
				disinheritedNodes = folder.getDisinheritedChannels();
				disinheritedNodes.add(node);

				try (ChannelTrx trx = new ChannelTrx()) {
					folder.changeMultichannellingRestrictions(
						folder.isExcluded(),
						disinheritedNodes,
						true);
				}

				continue;
			}

			for (Folder childFolder: folder.getChildFolders()) {
				folders.add(childFolder);
			}

			for (File file: folder.getFilesAndImages()) {
				file = file.getMaster();

				if (file.isDisinheritDefault()) {
					disinheritedNodes = file.getDisinheritedChannels();
					disinheritedNodes.add(node);
					if (logger.isDebugEnabled()) {
						logger.debug("disinheriting file {" + file.getName()
							+ ", " + file.getId() + "}");
					}

					try (ChannelTrx trx = new ChannelTrx()) {
						file.changeMultichannellingRestrictions(
							file.isExcluded(),
							disinheritedNodes,
							true);
					}
				}
			}

			for (Page page: folder.getPages()) {
				page = page.getMaster();

				if (page.isDisinheritDefault()) {
					disinheritedNodes = page.getDisinheritedChannels();
					disinheritedNodes.add(node);
					if (logger.isDebugEnabled()) {
						logger.debug("disinheriting page {" + page.getName()
							+ ", " + page.getId() + "}");
					}
					try (ChannelTrx trx = new ChannelTrx()) {
						page.changeMultichannellingRestrictions(
							page.isExcluded(),
							disinheritedNodes,
							true);
					}
				}
			}
		}
	}

	/**
	 * Set the default file and image folders for the given node.
	 *
	 * When a file/image folder id is specified, the method first ensures that
	 * the folder is visible in the given node. If not, an error message is
	 * added to the response.
	 *
	 * @param fileFolderId The id of the default file folder
	 * @param imageFolderId The id of the default image folder
	 * @param node The node that is currently changed
	 * @param response The response object of the current request
	 * @param save true if the node shall be saved
	 * @return <code>true</code> if all specified folder ids can be resolved to
	 *		folders that are visible in the given node, <code>false</code>
	 *		otherwise.
	 *
	 * @throws NodeException On internal errors
	 */
	private boolean setDefaultFolders(Integer fileFolderId, Integer imageFolderId, Node node, GenericResponse response, boolean save)
			throws NodeException {
		CNI18nString msg;
		boolean allOk = true;
		boolean nodeChanged = false;
		Transaction t = TransactionManager.getCurrentTransaction();

		if (fileFolderId != null) {
			int id = ObjectTransformer.getInt(fileFolderId, 0);

			if (id > 0) {
				Folder folder = MultichannellingFactory.getChannelVariant(t.getObject(Folder.class, id), node);

				if (folder == null) {
					msg = new CNI18nString("rest.node.invalid.folderid");
					response.addMessage(
							new Message(Type.CRITICAL, "defaultFileFolderId", msg.toString()));
					allOk = false;
				} else {
					node.setDefaultFileFolder(folder);
					nodeChanged = true;
				}
			} else {
				node.setDefaultFileFolder(null);
			}
		}

		if (imageFolderId != null) {
			int id = ObjectTransformer.getInt(imageFolderId, 0);

			if (id > 0) {
				Folder folder = MultichannellingFactory.getChannelVariant(t.getObject(Folder.class, id), node);

				if (folder == null) {
					msg = new CNI18nString("rest.node.invalid.folderid");
					response.addMessage(
							new Message(Type.CRITICAL, "defaultImageFolderId", msg.toString()));
					allOk = false;
				} else {
					node.setDefaultImageFolder(folder);
					nodeChanged = true;
				}
			} else {
				node.setDefaultImageFolder(null);
			}
		}

		if (save && allOk && nodeChanged) {
			node.save();
		}

		return allOk;
	}

	private void checkLanguageUsage(Node node, com.gentics.contentnode.object.ContentLanguage language) throws NodeException {
		// check whether pages exist, that use the language
		int pagesUsingLanguage = DBUtils.select("SELECT COUNT(*) c FROM page JOIN folder ON page.folder_id = folder.id WHERE folder.node_id = ? AND contentgroup_id = ?", st -> {
			st.setInt(1, node.getId());
			st.setInt(2, language.getId());
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("c");
			} else {
				return 0;
			}
		});
		if (pagesUsingLanguage > 0) {
			int pagesUsingLanguageInWastebin = DBUtils.select("SELECT COUNT(*) c FROM page JOIN folder ON page.folder_id = folder.id WHERE page.deleted > 0 AND folder.node_id = ? AND contentgroup_id = ?", st -> {
				st.setInt(1, node.getId());
				st.setInt(2, language.getId());
			}, rs -> {
				if (rs.next()) {
					return rs.getInt("c");
				} else {
					return 0;
				}
			});

			throw new RestMappedException(
					I18NHelper.get("cg_unlink_pages_left", Integer.toString(pagesUsingLanguage), Integer.toString(pagesUsingLanguageInWastebin)))
							.setResponseCode(ResponseCode.FAILURE).setStatus(Status.BAD_REQUEST);
		}
	}

	/**
	 * Check URL uniqueness in node, if page language code is changed
	 * @param node node/channel to check
	 * @param pageLanguageCode new page language code
	 * @return first found conflicting object or null
	 * @throws NodeException
	 */
	private NodeObject checkURLUniqueness(Node node, PageLanguageCode pageLanguageCode) throws NodeException {
		Map<String, Pair<Integer, Integer>> collectedURLs = new HashMap<>();

		// function to transform a checked object into Pair of type and id
		Function<NodeObject, Pair<Integer, Integer>> node2Key = o -> {
			return Pair.of(o.getTType(), o.getId());
		};

		// function to check, whether the given URL has a conflict with an already collected URL
		BiFunction<Pair<Integer, Integer>, String, Boolean> hasUrlConflict = (typeAndId, url) -> {
			// URL may be null
			if (url != null) {
				// check whether the map contains an entry with the url as key pointing to a different object (other typeAndId)
				if (!Objects.equals(collectedURLs.getOrDefault(url, typeAndId), typeAndId)) {
					return true;
				}
				collectedURLs.put(url, typeAndId);
			}

			return false;
		};

		// function to check whether the given page has a conflict with already collected URLs
		Function<Page, Boolean> checkPage = p -> {
			Pair<Integer, Integer> key = node2Key.apply(p);

			// check the regular URL of the page
			if (hasUrlConflict.apply(key, p.getFullPublishPath(p.getFolder(), true, pageLanguageCode, true) + p.getFilename())) {
				return true;
			}

			// check (optional) nice URL
			if (hasUrlConflict.apply(key, p.getNiceUrl())) {
				return true;
			}

			return false;
		};

		return FolderFactory.getFromFoldersRecursive(node.getFolder(), folder -> {
			for (File file : folder.getFilesAndImages()) {
				Pair<Integer, Integer> key = node2Key.apply(file);
				if (hasUrlConflict.apply(key, file.getFullPublishPath(file.getFolder(), true, pageLanguageCode, true) + file.getName())) {
					return file;
				}
			}

			for (Page page : folder.getPages()) {
				if (checkPage.apply(page)) {
					return page;
				}

				if (page.isOnline()) {
					try (FeatureClosure noPublishCache = new FeatureClosure(Feature.PUBLISH_CACHE, false)) {
						Page publishedPage = page.getPublishedObject();
						if (publishedPage != null) {
							if (checkPage.apply(publishedPage)) {
								return publishedPage;
							}
						}
					}
				}
			}

			return null;
		});
	}

	@Override
	@PUT
	public NodeLoadResponse add(NodeSaveRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (!ObjectPermission.create.checkClass(null, Node.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("rest.node.create.nopermission"), null, null, Node.TYPE_NODE, 0, PermType.create);
			}

			NodeLoadResponse response = new NodeLoadResponse();
			com.gentics.contentnode.rest.model.Node reqNode = request.getNode();

			if (!checkCreateRequest(request, response)) {
				response.setNode(null);
				response.setResponseInfo(
					new ResponseInfo(ResponseCode.INVALIDDATA, "node creation failed"));
				throw new RestMappedException(response).setStatus(Status.BAD_REQUEST);
			}

			Node newNode = t.createObject(Node.class);
			Folder rootFolder = t.createObject(Folder.class);
			Integer masterId = ObjectTransformer.getInteger(reqNode.getMasterId(), 0);

			if (masterId > 0) {
				Folder master = t.getObject(Folder.class, masterId);

				if (master != null) {
					rootFolder.setChannelMaster(master);
				}
			}

			rootFolder.setName(reqNode.getName());

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT) && reqNode.isPubDirSegment() != null) {
				newNode.setPubDirSegment(reqNode.isPubDirSegment());
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT) && ObjectTransformer.getBoolean(reqNode.isPubDirSegment(), false)) {
				rootFolder.setPublishDir(rootFolder.getName().toLowerCase());
			} else {
				rootFolder.setPublishDir("/");
			}

			if (!ObjectTransformer.isEmpty(request.getDescription())) {
				rootFolder.setDescription(request.getDescription());
			}

			newNode.setFolder(rootFolder);
			newNode.setHostname(reqNode.getHost());
			newNode.setHostnameProperty(reqNode.getHostProperty());

			if (reqNode.isHttps() != null) {
				newNode.setHttps(reqNode.isHttps());
			}
			if (reqNode.isUtf8() != null) {
				newNode.setUtf8(reqNode.isUtf8());
			}
			if (reqNode.getPublishDir() != null) {
				newNode.setPublishDir(reqNode.getPublishDir());
			}
			if (reqNode.getBinaryPublishDir() != null) {
				newNode.setBinaryPublishDir(reqNode.getBinaryPublishDir());
			}
			if (reqNode.isPublishFs() != null) {
				newNode.setPublishFilesystem(reqNode.isPublishFs());
			}
			if (reqNode.getPublishFsPages() != null) {
				newNode.setPublishFilesystemPages(reqNode.getPublishFsPages());
			}
			if (reqNode.getPublishFsFiles() != null) {
				newNode.setPublishFilesystemFiles(reqNode.getPublishFsFiles());
			}
			if (reqNode.isPublishContentMap() != null) {
				newNode.setPublishContentmap(reqNode.isPublishContentMap());
			}
			if (reqNode.getPublishContentMapPages() != null) {
				newNode.setPublishContentMapPages(reqNode.getPublishContentMapPages());
			}
			if (reqNode.getPublishContentMapFiles() != null) {
				newNode.setPublishContentMapFiles(reqNode.getPublishContentMapFiles());
			}
			if (reqNode.getPublishContentMapFolders() != null) {
				newNode.setPublishContentMapFolders(reqNode.getPublishContentMapFolders());
			}

			if (reqNode.isDisablePublish() != null) {
				newNode.setPublishDisabled(reqNode.isDisablePublish());
			}

			if (reqNode.getContentRepositoryId() != null) {
				ContentRepository contentRepository = t.getObject(ContentRepository.class, reqNode.getContentRepositoryId());

				if (contentRepository == null) {
					String errorMsg = "ContentRepository {" + reqNode.getContentRepositoryId() + "} not found";
					CNI18nString message = new CNI18nString("rest.contentrepository.notfound");

					message.addParameter(reqNode.getContentRepositoryId().toString());

					throw new RestMappedException(
							new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.NOTFOUND, errorMsg)))
									.setStatus(Status.BAD_REQUEST);
				}

				if (!t.getPermHandler().canEdit(contentRepository)) {
					throw new InsufficientPrivilegesException(I18NHelper.get("rest.contentrepository.nopermission", reqNode.getContentRepositoryId().toString()), contentRepository, PermType.update);
				}

				newNode.setContentrepositoryId(reqNode.getContentRepositoryId());
			}

			Integer editor = reqNode.getEditorVersion();
			if (editor != null) {
				newNode.setEditorversion(editor);
			}

			if (reqNode.getUrlRenderWayPages() != null) {
				newNode.setUrlRenderWayPages(reqNode.getUrlRenderWayPages());
			}

			if (reqNode.getUrlRenderWayFiles() != null) {
				newNode.setUrlRenderWayFiles(reqNode.getUrlRenderWayFiles());
			}

			if (reqNode.getOmitPageExtension() != null) {
				newNode.setOmitPageExtension(reqNode.getOmitPageExtension());
			}

			if (reqNode.getPageLanguageCode() != null) {
				newNode.setPageLanguageCode(reqNode.getPageLanguageCode());
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				if (reqNode.getMeshPreviewUrl() != null) {
					newNode.setMeshPreviewUrl(reqNode.getMeshPreviewUrl());
				}
				if (reqNode.getMeshPreviewUrlProperty() != null) {
					newNode.setMeshPreviewUrlProperty(reqNode.getMeshPreviewUrlProperty());
				}

				if (reqNode.getInsecurePreviewUrl() != null) {
					newNode.setInsecurePreviewUrl(reqNode.getInsecurePreviewUrl());
				}

				if (reqNode.isPublishImageVariants() != null) {
					newNode.setPublishImageVariants(reqNode.isPublishImageVariants());
				}

				if (reqNode.getMeshProjectName() != null) {
					newNode.setMeshProjectName(reqNode.getMeshProjectName());
				}
			}

			if (reqNode.getLanguagesId() != null) {
				List<com.gentics.contentnode.object.ContentLanguage> oldLanguages = newNode.getLanguages();
				List<com.gentics.contentnode.object.ContentLanguage> newLanguages =
					t.getObjects(com.gentics.contentnode.object.ContentLanguage.class, reqNode.getLanguagesId());

				oldLanguages.clear();
				oldLanguages.addAll(newLanguages);
			}

			// check for conflicts
			Node conflictingNode = newNode.getConflictingNode();
			if (conflictingNode != null) {
				response.addMessage(createNodeConflictMessage(newNode, conflictingNode, "host"));
				response.setNode(null);
				response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "node creation failed"));

				return response;
			}

			newNode.save();
			newNode = t.getObject(newNode, true);

			try (ChannelTrx cTrx = new ChannelTrx(newNode)) {
				disinheritObjects(newNode);
			}

			boolean defaultFoldersOk = setDefaultFolders(
				reqNode.getDefaultFileFolderId(),
				reqNode.getDefaultImageFolderId(),
				newNode,
				response, true);

			if (!defaultFoldersOk) {
				response.setNode(null);
				response.setResponseInfo(
					new ResponseInfo(ResponseCode.INVALIDDATA, "node creation failed"));

				throw new RestMappedException(response).setStatus(Status.BAD_REQUEST);
			}

			trx.success();

			I18nString message = new CNI18nString("rest.node.create.success");

			return new NodeLoadResponse(
				new Message(Type.SUCCESS, message.toString()),
				new ResponseInfo(ResponseCode.OK, "created node with id: " + newNode.getId()),
				ModelBuilder.getNode(t.getObject(newNode)));
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public NodeLoadResponse get(@PathParam("id") String nodeId,
			@DefaultValue("false") @QueryParam("update") boolean update) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return new NodeLoadResponse(null, new ResponseInfo(ResponseCode.OK, "Node loaded"), ModelBuilder.getNode(getNode(nodeId, update ? ObjectPermission.edit : ObjectPermission.view)));
		}
	}

	@Override
	@POST
	@Path("/{id}")
	public GenericResponse update(@PathParam("id") String nodeId, NodeSaveRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Node node = t.getObject(getNode(nodeId, ObjectPermission.edit), true);

			Folder rootFolder = node.getFolder();

			GenericResponse response = new GenericResponse();
			com.gentics.contentnode.rest.model.Node reqNode = request.getNode();

			if (!checkSaveRequest(node, request, response)) {
				response.setResponseInfo(
					new ResponseInfo(ResponseCode.INVALIDDATA, "node saving failed"));

				return response;
			}

			if (!ObjectTransformer.isEmpty(reqNode.getName())) {
				rootFolder.setName(reqNode.getName());
			}
			if (request.getDescription() != null) {
				rootFolder.setDescription(request.getDescription());
			}
			if (!ObjectTransformer.isEmpty(reqNode.getHost())) {
				node.setHostname(reqNode.getHost());
			}
			if (reqNode.getHostProperty() != null) {
				node.setHostnameProperty(reqNode.getHostProperty());
			}
			if (reqNode.isHttps() != null) {
				node.setHttps(reqNode.isHttps());
			}
			if (reqNode.isUtf8() != null) {
				node.setUtf8(reqNode.isUtf8());
			}
			if (reqNode.getEditorVersion() != null) {
				node.setEditorversion(reqNode.getEditorVersion());
			}
			if (reqNode.getPublishDir() != null) {
				node.setPublishDir(reqNode.getPublishDir());
			}
			if (reqNode.getBinaryPublishDir() != null) {
				node.setBinaryPublishDir(reqNode.getBinaryPublishDir());
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT) && reqNode.isPubDirSegment() != null) {
				node.setPubDirSegment(reqNode.isPubDirSegment());
			}
			if (reqNode.isPublishFs() != null) {
				node.setPublishFilesystem(reqNode.isPublishFs());
			}
			if (reqNode.getPublishFsPages() != null) {
				node.setPublishFilesystemPages(reqNode.getPublishFsPages());
			}
			if (reqNode.getPublishFsFiles() != null) {
				node.setPublishFilesystemFiles(reqNode.getPublishFsFiles());
			}
			if (reqNode.isPublishContentMap() != null) {
				node.setPublishContentmap(reqNode.isPublishContentMap());
			}
			if (reqNode.getPublishContentMapPages() != null) {
				node.setPublishContentMapPages(reqNode.getPublishContentMapPages());
			}
			if (reqNode.getPublishContentMapFiles() != null) {
				node.setPublishContentMapFiles(reqNode.getPublishContentMapFiles());
			}
			if (reqNode.getPublishContentMapFolders() != null) {
				node.setPublishContentMapFolders(reqNode.getPublishContentMapFolders());
			}
			if (reqNode.isDisablePublish() != null) {
				node.setPublishDisabled(reqNode.isDisablePublish());
			}
			if (reqNode.getContentRepositoryId() != null && reqNode.getContentRepositoryId() != node.getContentrepositoryId()) {
				if (!ContentRepository.isEmptyId(reqNode.getContentRepositoryId())) {
				ContentRepository contentRepository = t.getObject(ContentRepository.class, reqNode.getContentRepositoryId());

				if (contentRepository == null) {
					String errorMsg = "ContentRepository {" + reqNode.getContentRepositoryId() + "} not found";
					CNI18nString message = new CNI18nString("rest.contentrepository.notfound");

					message.addParameter(reqNode.getContentRepositoryId().toString());

					return new GenericResponse(
						new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, errorMsg));
				}

				if (!t.getPermHandler().canEdit(contentRepository)) {
					CNI18nString message = new CNI18nString("rest.contentrepository.nopermission");

					message.addParameter(reqNode.getContentRepositoryId().toString());

					return new GenericResponse(
						new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.PERMISSION, "No permission to edit the content repository " + reqNode.getContentRepositoryId()));
				}

					Map<String, Set<Node>> conflictingNodes = contentRepository.getConflictingNodes(node);
					if (!conflictingNodes.isEmpty()) {
						for (Map.Entry<String, Set<Node>> entry : conflictingNodes.entrySet()) {
							List<String> nodeNames = new ArrayList<>();
							for (Node n : entry.getValue()) {
								nodeNames.add(I18NHelper.getName(n));
							}
							String joinedNames = nodeNames.stream().collect(Collectors.joining(","));
							response.addMessage(new Message(Type.CRITICAL, I18NHelper.get(entry.getKey(), joinedNames)));
						}
						response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "node saving failed"));
						return response;
					}

					if (NodeConfigRuntimeConfiguration.isFeature(Feature.DEVTOOLS)) {
						// when assigning a CR to the node, assigned to the devtools packages, assign all the missing CR fragments of the packages to the CR
						Set<Integer> crFragmentIds = contentRepository.getAssignedFragments().stream().map(NodeObject::getId).collect(Collectors.toSet());
						boolean crUpdated = false;
						for (String pkg: Synchronizer.getPackages(node)) {
							for (PackageObject<CrFragment> pkgFragment: Synchronizer.getPackage(pkg).getObjects(CrFragment.class)) {
								if (!crFragmentIds.contains(pkgFragment.getId())) {
									if (!crUpdated) {
										contentRepository = t.getObject(contentRepository, true);
										crUpdated = true;
									}
									contentRepository.getAssignedFragments().add(t.getObject(CrFragment.class, pkgFragment.getId()));
								}
							}
						}
						if (crUpdated) {
							contentRepository.save();
						}
					}

					// when assigning a master to a multichannelling aware CR, we assign all channels as well
					if (contentRepository.isMultichannelling() && !node.isChannel()) {
						for (Node channel : node.getAllChannels()) {
							Node editableChannel = t.getObject(channel, true);
							editableChannel.setContentrepositoryId(contentRepository.getId());
							editableChannel.save();
						}
					}

					// when assigning a master/channel to a non-multichannelling aware CR, no other node/channel of the same structure must be assigned to the
					// same CR
					if (!contentRepository.isMultichannelling() && NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
						Set<Node> toCheck = new HashSet<>();
						toCheck.add(node.getMaster());
						toCheck.addAll(node.getMaster().getAllChannels());
						for (Node channel : toCheck) {
							if (!channel.equals(node) && Objects.equals(channel.getContentrepositoryId(), reqNode.getContentRepositoryId())) {
								return new GenericResponse(new Message(Type.CRITICAL, I18NHelper.get("cr_channel_master_no_assign")),
										new ResponseInfo(ResponseCode.INVALIDDATA, "node saving failed"));
							}
						}
					}
				} else {
					// when unassigning the master from an mccr, the channels must be unassigned also
					ContentRepository currentCr = node.getContentRepository();
					if (!node.isChannel() && currentCr != null && currentCr.getCrType() == ContentRepositoryModel.Type.mccr) {
						try {
							t.getAttributes().put(FolderFactory.OMIT_CR_VERIFY, true);
							for (Node channel : node.getAllChannels()) {
								Node editableChannel = t.getObject(channel, true);
								editableChannel.setContentrepositoryId(0);
								editableChannel.save();
							}
						} finally {
							t.getAttributes().remove(FolderFactory.OMIT_CR_VERIFY);
						}
					}
				}

				node.setContentrepositoryId(reqNode.getContentRepositoryId());
			}
			if (reqNode.getLanguagesId() != null) {
				List<com.gentics.contentnode.object.ContentLanguage> oldLanguages = node.getLanguages();
				List<com.gentics.contentnode.object.ContentLanguage> newLanguages =
					t.getObjects(com.gentics.contentnode.object.ContentLanguage.class, reqNode.getLanguagesId());

				oldLanguages.clear();
				oldLanguages.addAll(newLanguages);
			}

			if (reqNode.getUrlRenderWayPages() != null) {
				node.setUrlRenderWayPages(reqNode.getUrlRenderWayPages());
			}

			if (reqNode.getUrlRenderWayFiles() != null) {
				node.setUrlRenderWayFiles(reqNode.getUrlRenderWayFiles());
			}

			if (reqNode.getOmitPageExtension() != null) {
				node.setOmitPageExtension(reqNode.getOmitPageExtension());
			}

			if (reqNode.getPageLanguageCode() != null) {
				node.setPageLanguageCode(reqNode.getPageLanguageCode());
			}

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
				if (reqNode.getMeshPreviewUrl() != null) {
					node.setMeshPreviewUrl(reqNode.getMeshPreviewUrl());
				}

				if (reqNode.getMeshPreviewUrlProperty() != null) {
					node.setMeshPreviewUrlProperty(reqNode.getMeshPreviewUrlProperty());
				}

				if (reqNode.getInsecurePreviewUrl() != null) {
					node.setInsecurePreviewUrl(reqNode.getInsecurePreviewUrl());
				}

				if (reqNode.isPublishImageVariants() != null) {
					node.setPublishImageVariants(reqNode.isPublishImageVariants());
				}

				if (reqNode.getMeshProjectName() != null) {
					node.setMeshProjectName(reqNode.getMeshProjectName());
				}
			}

			if (!setDefaultFolders(reqNode.getDefaultFileFolderId(), reqNode.getDefaultImageFolderId(), node, response, false)) {
				response.setResponseInfo(
						new ResponseInfo(ResponseCode.INVALIDDATA, "node saving failed"));
				return response;
			}

			// check for conflicts
			Node conflictingNode = node.getConflictingNode();
			if (conflictingNode != null) {
				response.addMessage(createNodeConflictMessage(node, conflictingNode, "host"));
				response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "node saving failed", "host"));
				return response;
			}

			node.save();
			trx.success();

			return new GenericResponse(
				new Message(Type.SUCCESS, I18NHelper.get("rest.node.save.success", I18NHelper.getName(node))),
				new ResponseInfo(ResponseCode.OK, "saved node with id {" + node.getId() + "}"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public GenericResponse delete(@PathParam("id") String nodeId, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return Operator.executeRethrowing(I18NHelper.get("node.delete.job"), waitMs, () -> {
				// load the node and check for permission
				Node node = getNode(nodeId, ObjectPermission.delete);

				// check whether channels exist
				if (!node.getChannels().isEmpty()) {
					throw new RestMappedException(I18NHelper.get("delete_error_channel_found")).setMessageType(Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
				}

				// check whether deleting the node would create orphaned constructs
				List<Construct> assigned = node.getConstructs();
				Set<Construct> wouldBeOrphan = new HashSet<>();
				for (Construct construct : assigned) {
					Set<Node> constructNodes = new HashSet<>(construct.getNodes());
					constructNodes.remove(node);
					if (constructNodes.isEmpty()) {
						wouldBeOrphan.add(construct);
					}
				}
				if (!wouldBeOrphan.isEmpty()) {
					String message = String.format("%s\n\n%s\n\n%s", I18NHelper.get("nodedelete.dangling_tagtypes"),
							wouldBeOrphan.stream().map(Construct::getKeyword).collect(Collectors.joining("\n")),
							I18NHelper.get("nodedelete.link_tagtypes"));
					throw new RestMappedException(message).setMessageType(Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
				}

				// delete the node
				node.delete();

				trx.success();
				return new GenericResponse(new Message(Type.SUCCESS, I18NHelper.get("rest.node.delete.success", nodeId)),
						new ResponseInfo(ResponseCode.OK, "deleted node with id {" + node.getId() + "}"));
			});
		}
	}

	@Override
	@GET
	public NodeList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms,
			@QueryParam("package") String stagingPackageName) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<Node> nodes = trx.getTransaction().getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS));

			NodeObjectFilter permFilter = PermissionFilter.get(ObjectPermission.view);

			Map<String, String> sortFieldMapping = new HashMap<>();
			sortFieldMapping.put("name", "folder.name");

			NodeList nodeList = ListBuilder.from(nodes, Node.TRANSFORM2REST)
				.filter(o -> permFilter.matches(o))
				.filter(ResolvableFilter.get(filter, "id", "folder.name"))
				.sort(ResolvableComparator.get(sorting, sortFieldMapping, "id", "name"))
				.page(paging)
				.perms(permFunction(perms, ObjectPermission.read, view, edit, update, delete, createfolder, updatefolder,
						deletefolder, linkoverview, createoverview, readitems, createitems, updateitems,
						deleteitems, importitems, publishpages, translatepages, viewform, createform, updateform,
						deleteform, publishform, formreport, createtemplates, updatetemplates, deletetemplates,
						linktemplates, updateconstructs, channelsync, updateinheritance, wastebin))
				.to(new NodeList());

			nodeList.setStagingStatus(StagingUtil.checkStagingStatus(nodes, stagingPackageName, o -> o.getGlobalId().toString()));
			trx.success();
			return nodeList;
		}
	}

	@Override
	@GET
	@Path("/{id}/languages")
	public LanguageList languages(@PathParam("id") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam PagingParameterBean paging)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			LanguageList languageList = ListBuilder.from(node.getLanguages(), com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST)
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "code"))
				.page(paging)
				.to(new LanguageList());

			trx.success();
			return languageList;
		}
	}

	@Override
	@GET
	@Path("/{id}/availableLanguages")
	public LanguageList availableLanguages(@PathParam("id") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			getNode(nodeId, ObjectPermission.view);

			Transaction t = TransactionManager.getCurrentTransaction();
			List<com.gentics.contentnode.object.ContentLanguage> languages = t.getObjects(
					com.gentics.contentnode.object.ContentLanguage.class,
					DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS));

			LanguageList response = ListBuilder.from(languages, com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST)
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "code"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "code"))
					.page(paging).to(new LanguageList());

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}/languages/{languageId}")
	public GenericResponse addLanguage(@PathParam("id") String nodeId, @PathParam("languageId") String languageId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.edit);
			if (trx.getTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && node.isChannel()) {
				throw new RestMappedException(I18NHelper.get("devtools.action.not_allowed_for_channels")).setStatus(Status.BAD_REQUEST);
			}
			com.gentics.contentnode.object.ContentLanguage language = getLanguage(languageId);

			List<com.gentics.contentnode.object.ContentLanguage> languages = node.getLanguages();
			if (!languages.contains(language)) {
				Node editableNode = trx.getTransaction().getObject(node, true);
				editableNode.getLanguages().add(language);
				editableNode.save();
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, String.format("Added language %s to node %s", languageId, nodeId)));
		}
	}

	@Override
	@DELETE
	@Path("/{id}/languages/{languageId}")
	public GenericResponse removeLanguage(@PathParam("id") String nodeId, @PathParam("languageId") String languageId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.edit);
			if (trx.getTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && node.isChannel()) {
				throw new RestMappedException(I18NHelper.get("devtools.action.not_allowed_for_channels")).setStatus(Status.BAD_REQUEST);
			}
			com.gentics.contentnode.object.ContentLanguage language = getLanguage(languageId);

			List<com.gentics.contentnode.object.ContentLanguage> languages = node.getLanguages();
			if (languages.contains(language)) {
				checkLanguageUsage(node, language);

				Node editableNode = trx.getTransaction().getObject(node, true);
				editableNode.getLanguages().remove(language);
				editableNode.save();
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, String.format("Removed language %s from node %s", languageId, nodeId)));
		}
	}

	@Override
	@POST
	@Path("/{id}/languages")
	public LanguageList setLanguages(@PathParam("id") String nodeId, List<ContentLanguage> languages) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.edit);
			if (trx.getTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING) && node.isChannel()) {
				throw new RestMappedException(I18NHelper.get("devtools.action.not_allowed_for_channels")).setStatus(Status.BAD_REQUEST);
			}
			checkFields(() -> Pair.of("languages", languages));

			List<com.gentics.contentnode.object.ContentLanguage> requestedLanguages = new ArrayList<>();
			for (ContentLanguage language : languages) {
				com.gentics.contentnode.object.ContentLanguage nodeLanguage = null;
				if (language.getId() != null) {
					nodeLanguage = getLanguage(Integer.toString(language.getId()));
				} else if (language.getCode() != null) {
					nodeLanguage = getLanguage(language.getCode());
				} else {
					// TODO is this an error? neither id nor code were given
				}
				if (nodeLanguage != null && !requestedLanguages.contains(nodeLanguage)) {
					requestedLanguages.add(nodeLanguage);
				}
			}

			// determine, which languages would be removed and check, whether any of that is still used in the node
			Set<com.gentics.contentnode.object.ContentLanguage> langSet = new HashSet<>(node.getLanguages());
			langSet.removeAll(requestedLanguages);
			if (!langSet.isEmpty()) {
				for (com.gentics.contentnode.object.ContentLanguage removed : langSet) {
					checkLanguageUsage(node, removed);
				}
			}

			Node editableNode = trx.getTransaction().getObject(node, true);
			List<com.gentics.contentnode.object.ContentLanguage> nodeLanguages = editableNode.getLanguages();
			nodeLanguages.clear();
			nodeLanguages.addAll(requestedLanguages);
			editableNode.save();

			node = node.reload();

			LanguageList languageList = ListBuilder.from(node.getLanguages(), com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST)
					.to(new LanguageList());

			trx.success();

			return languageList;
		}
	}

	@Override
	@POST
	@Path("/create")
	@Deprecated
	public NodeLoadResponse create(NodeSaveRequest request) throws NodeException {
		return add(request);
	}

	@Override
	@POST
	@Path("/save/{id}")
	@Deprecated
	public GenericResponse save(@PathParam("id") String nodeId, NodeSaveRequest request) throws NodeException {
		return update(nodeId, request);
	}

	@Override
	@GET
	@Path("/load/{id}")
	@Deprecated
	public NodeLoadResponse load(@PathParam("id") String nodeId,
			@DefaultValue("false") @QueryParam("update") boolean update) throws NodeException {
		return get(nodeId, update);
	}

	@GET
	@Path("/getLanguages/{id}")
	@Override
	@Deprecated
	public LanguageListResponse languages(@PathParam("id") String nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			List<com.gentics.contentnode.object.ContentLanguage> languages = node.getLanguages();

			LanguageListResponse response = new LanguageListResponse();
			List<ContentLanguage> restLanguages = new ArrayList<ContentLanguage>(languages.size());
			for (com.gentics.contentnode.object.ContentLanguage language : languages) {
				restLanguages.add(com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(language));
			}
			response.setLanguages(restLanguages);
			response.setNumItems(restLanguages.size());
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully loaded node languages"));
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/features")
	public FeatureList features(@PathParam("id") String nodeId, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			FeatureList featureList = ListBuilder.from(node.getFeatures(), f -> ModelBuilder.getFeature(f))
					.filter(f -> ObjectTransformer.isEmpty(filter.query) ? true : f.name().toLowerCase().contains(filter.query.toLowerCase()))
					.sort(comparator(sorting, (f, key) -> f.name(), "name"))
					.page(paging).to(new FeatureList());

			trx.success();
			return featureList;
		}
	}

	@Override
	@PUT
	@Path("/{id}/features/{feature}")
	public GenericResponse activateFeature(@PathParam("id") String nodeId, @PathParam("feature") NodeFeature feature) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.edit);
			Feature nodeFeature = Feature.valueOf(feature.toString().toUpperCase());
			require(nodeFeature);

			node.activateFeature(nodeFeature);

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully activated feature for node"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}/features/{feature}")
	public GenericResponse deactivateFeature(@PathParam("id") String nodeId, @PathParam("feature") NodeFeature feature) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.edit);
			Feature nodeFeature = Feature.valueOf(feature.toString().toUpperCase());
			require(nodeFeature);

			node.deactivateFeature(nodeFeature);

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully deactivated feature for node"));
		}
	}

	@Override
	@GET
	@Path("/features/{id}")
	@Deprecated
	public NodeFeatureResponse getFeatures(@PathParam("id") String nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			List<Feature> features = node.getFeatures();
			List<NodeFeature> nodeFeatures = new Vector<NodeFeature>();

			for (Feature feature : features) {
				NodeFeature nodeFeature = ModelBuilder.getFeature(feature);

				if (nodeFeature != null) {
					nodeFeatures.add(nodeFeature);
				}
			}

			trx.success();
			return new NodeFeatureResponse(new ResponseInfo(ResponseCode.OK, "Fetched Features"), nodeFeatures);
		}
	}

	@Override
	@POST
	@Path("/features/activate/{id}")
	@Deprecated
	public GenericResponse activateFeatures(@PathParam("id") String nodeId, NodeFeatureRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.edit);

			for (NodeFeature nodeFeature : request.getFeatures()) {
				node.activateFeature(Feature.valueOf(nodeFeature.toString().toUpperCase()));
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully activated features for node"));
		}
	}

	@Override
	@POST
	@Path("/features/deactivate/{id}")
	@Deprecated
	public GenericResponse deactivateFeatures(@PathParam("id") String nodeId, NodeFeatureRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			// check permission to edit the node
			if (!PermHandler.ObjectPermission.edit.checkObject(node)) {
				I18nString message = new CNI18nString("rest.node.nopermission");

				message.setParameter("0", nodeId);
				throw new InsufficientPrivilegesException(message.toString(), node, PermType.update);
			}

			for (NodeFeature nodeFeature : request.getFeatures()) {
				node.deactivateFeature(Feature.valueOf(nodeFeature.toString().toUpperCase()));
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully deactivated features for node"));
		}
	}

	@Override
	@POST
	@Path("/features/set/{id}")
	@Deprecated
	public GenericResponse setFeatures(@PathParam("id") String nodeId, NodeFeatureRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			// check permission to edit the node
			if (!PermHandler.ObjectPermission.edit.checkObject(node)) {
				I18nString message = new CNI18nString("rest.node.nopermission");

				message.setParameter("0", nodeId);
				throw new InsufficientPrivilegesException(message.toString(), node, PermType.update);
			}

			for (NodeFeature nodeFeature : NodeFeature.values()) {
				Feature feature = Feature.valueOf(nodeFeature.toString().toUpperCase());
				if (!feature.isActivated()) {
					continue;
				}

				if (request.getFeatures().contains(nodeFeature)) {
					node.activateFeature(feature);
				} else {
					node.deactivateFeature(feature);
				}
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully set features for node"));
		}
	}

	@Override
	@GET
	@Path("/{nodeId}/templates")
	public PagedTemplateListResponse getTemplates(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				node = node.getMaster();
				Set<Template> templates = node.getTemplates();

				// to improve performance of permission checks, get the assignment info for all templates in the node
				try (Timing timing = Timing.get(duration -> logger.debug(String.format("Perparing template/folder map took %d ms", duration)))) {
					trx.getTransaction().getPermHandler().prepareFolderTemplateMap(node.getId());
				}

				trx.success();
				return ListBuilder.from(templates, Template.TRANSFORM2REST)
						.filter(PermFilter.get(ObjectPermission.view))
						.filter(ResolvableFilter.get(filter, "name", "description"))
				.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
						.sort(ResolvableComparator.get(sorting, "name", "description"))
						.page(paging).to(new PagedTemplateListResponse());
			} finally {
				trx.getTransaction().getPermHandler().resetFolderTemplateMap();
			}
		}
	}

	@Override
	@GET
	@Path("/{nodeId}/templates/{templateId}")
	public TemplateLoadResponse getTemplate(@PathParam("nodeId") String nodeId, @PathParam("templateId") String templateId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Template template = MiscUtils.getTemplate(templateId, ObjectPermission.view);
				if (!template.getAssignedNodes().contains(node)) {
					I18nString message = new CNI18nString("template.notfound");
					message.setParameter("0", templateId);
					throw new EntityNotFoundException(message.toString());
				}
				trx.success();
				com.gentics.contentnode.rest.model.Template restTemplate = ModelBuilder.getTemplate(template, null);
				TemplateLoadResponse response = new TemplateLoadResponse();
				response.setTemplate(restTemplate);
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Loaded template with id { " + templateId + " } successfully"));
				return response;
			}
		}
	}

	@Override
	@PUT
	@Path("/{nodeId}/templates/{templateId}")
	public GenericResponse addTemplate(@PathParam("nodeId") String nodeId, @PathParam("templateId") String templateId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Node node = getNode(nodeId, ObjectPermission.edit);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				if (!t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PermHandler.PERM_TEMPLATE_LINK)) {
					I18nString message = new CNI18nString("rest.node.nopermission");
					message.setParameter("0", nodeId);
				throw new InsufficientPrivilegesException(message.toString(), node, PermType.linktemplates);
				}

				Template template = t.getObject(Template.class, templateId);

				if (template == null) {
					I18nString message = new CNI18nString("template.notfound");
					message.setParameter("0", templateId);
					throw new EntityNotFoundException(message.toString());
				}

				if (!PermHandler.ObjectPermission.view.checkObject(template)) {
					I18nString message = new CNI18nString("template.nopermission");
					message.setParameter("0", templateId);
				throw new InsufficientPrivilegesException(message.toString(), template, PermType.read);
				}

				if (template.isInherited()) {
					I18nString message = new CNI18nString("template_node.template.inherited");
					message.setParameter("0", I18NHelper.getName(template));
					return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.INVALIDDATA, ""));
				}

				node.addTemplate(template);
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		}
	}

	@Override
	@DELETE
	@Path("/{nodeId}/templates/{templateId}")
	public GenericResponse removeTemplate(@PathParam("nodeId") String nodeId, @PathParam("templateId") String templateId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Node node = getNode(nodeId, ObjectPermission.view);

			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				node = node.getMaster();

				if (!t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PermHandler.PERM_TEMPLATE_DELETE)) {
					I18nString message = new CNI18nString("rest.node.nopermission");
					message.setParameter("0", nodeId);
				throw new InsufficientPrivilegesException(message.toString(), node, PermType.deletetemplates);
				}

				Template template = t.getObject(Template.class, templateId);
				if (template == null) {
					I18nString message = new CNI18nString("template.notfound");
					message.setParameter("0", templateId);
					throw new EntityNotFoundException(message.toString());
				}

				if (!PermHandler.ObjectPermission.view.checkObject(template)) {
					I18nString message = new CNI18nString("template.nopermission");
					message.setParameter("0", templateId);
				throw new InsufficientPrivilegesException(message.toString(), template, PermType.read);
				}

				if (template.isInherited()) {
					I18nString message = new CNI18nString("template_node.template.inherited");
					message.setParameter("0", I18NHelper.getName(template));
					return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.INVALIDDATA, ""));
				}

				if (node.getTemplates().contains(template)) {
					template = t.getObject(template, true);

					// check whether the template would be linked to another folder
					int iNodeId = node.getId();
					int iTemplateId = template.getId();
					int otherUsageCount = DBUtils.select(
						"SELECT COUNT(*) c FROM template_folder tf LEFT JOIN folder f ON tf.folder_id = f.id WHERE f.node_id != ? AND tf.template_id = ?",
						stmt -> {
							stmt.setInt(1, iNodeId);
							stmt.setInt(2, iTemplateId);
						}, rs -> {
							if (rs.next()) {
								return rs.getInt("c");
							} else {
								return 0;
							}
						});
					if (otherUsageCount == 0) {
						throw new WebApplicationException(I18NHelper.get("template.unlink.notlinked", template.toString()),
								Response.Status.CONFLICT);
					}

					// remove template from all folders of the node
					List<Folder> folders = template.getFolders();
					for (Iterator<Folder> iter = folders.iterator(); iter.hasNext(); ) {
						Folder folder = iter.next();
						if (folder.getNode().getMaster().equals(node)) {
							iter.remove();
						}
					}
					template.save(false);
					template.unlock();

					// remove template from the node
					DBUtils.executeUpdate("DELETE FROM template_node WHERE template_id = ? AND node_id = ?",
							new Object[] { template.getMaster().getId(), node.getId() });
					t.dirtObjectCache(Node.class, node.getId());
				}

				trx.success();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
			}
		}
	}

	@Override
	@GET
	@Path("/{nodeId}/constructs")
	public PagedConstructListResponse getConstructs(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			node = node.getMaster();
			List<Construct> constructs = node.getConstructs();

			// Pre-fill the i18n dictionary with all translated names and
			// description, so that it will not be necessary to make an
			// additional SQL query for each a construct object is initialized
			CNDictionary.prefillDictionary("construct", "name_id");
			CNDictionary.prefillDictionary("part", "name_id");
			CNDictionary.prefillDictionary("construct", "description_id");
			CNDictionary.prefillDictionary("construct_category", "name_id");

			trx.success();
			return ListBuilder.from(constructs, Construct.TRANSFORM2REST)
					.filter(PermFilter.get(ObjectPermission.view))
					.filter(ResolvableFilter.get(filter, "keyword", "name", "description"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "keyword", "name", "description"))
					.page(paging).to(new PagedConstructListResponse());
		}
	}

	@Override
	@PUT
	@Path("/{nodeId}/constructs/{constructId}")
	public GenericResponse addConstruct(@PathParam("nodeId") String nodeId, @PathParam("constructId") String constructId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				if (!t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PermHandler.PERM_NODE_CONSTRUCT_MODIFY)) {
					I18nString message = new CNI18nString("rest.node.nopermission");
					message.setParameter("0", nodeId);
					throw new InsufficientPrivilegesException(message.toString(), node, PermType.updateconstructs);
				}

				Construct construct = t.getObject(Construct.class, constructId);

				if (construct == null) {
					I18nString message = new CNI18nString("construct.notfound");
					message.setParameter("0", constructId);
					throw new EntityNotFoundException(message.toString());
				}

				if (!PermHandler.ObjectPermission.view.checkObject(construct)) {
					I18nString message = new CNI18nString("construct.nopermission");
					message.setParameter("0", constructId);
				throw new InsufficientPrivilegesException(message.toString(), construct, PermType.read);
				}

				node.addConstruct(construct);
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		}
	}

	@Override
	@DELETE
	@Path("/{nodeId}/constructs/{constructId}")
	public GenericResponse removeConstruct(@PathParam("nodeId") String nodeId, @PathParam("constructId") String constructId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				if (!t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PermHandler.PERM_NODE_CONSTRUCT_MODIFY)) {
					I18nString message = new CNI18nString("rest.node.nopermission");
					message.setParameter("0", nodeId);
				throw new InsufficientPrivilegesException(message.toString(), node, PermType.updateconstructs);
				}

				Construct construct = t.getObject(Construct.class, constructId);

				if (construct == null) {
					I18nString message = new CNI18nString("construct.notfound");
					message.setParameter("0", constructId);
					throw new EntityNotFoundException(message.toString());
				}

				if (!PermHandler.ObjectPermission.view.checkObject(construct)) {
					I18nString message = new CNI18nString("construct.nopermission");
					message.setParameter("0", constructId);
				throw new InsufficientPrivilegesException(message.toString(), construct, PermType.read);
				}

				node.removeConstruct(construct);
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		}
	}

	@Override
	@GET
	@Path("/{nodeId}/objectproperties")
	public PagedObjectPropertyListResponse getObjectProperties(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			node = node.getMaster();
			List<ObjectTagDefinition> otds = node.getObjectTagDefinitions();

			trx.success();
			return ListBuilder.from(otds, ObjectTagDefinition.TRANSFORM2REST)
					.filter(PermFilter.get(ObjectPermission.view))
					.filter(ResolvableFilter.get(filter, "keyword", "name", "description"))
			.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "keyword", "name", "description"))
					.page(paging).to(new PagedObjectPropertyListResponse());
		}
	}

	@Override
	@PUT
	@Path("/{nodeId}/objectproperties/{objectPropertyId}")
	public GenericResponse addObjectProperty(@PathParam("nodeId") String nodeId, @PathParam("objectPropertyId") String opId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				if (!t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PermHandler.PERM_NODE_CONSTRUCT_MODIFY)) {
					I18nString message = new CNI18nString("rest.node.nopermission");
					message.setParameter("0", nodeId);
					throw new InsufficientPrivilegesException(message.toString(), node, PermType.updateconstructs);
				}

				ObjectTagDefinition otd = t.getObject(ObjectTagDefinition.class, opId);

				if (otd == null) {
					I18nString message = new CNI18nString("objectproperty.notfound");
					message.setParameter("0", opId);
					throw new EntityNotFoundException(message.toString());
				}

				if (!PermHandler.ObjectPermission.view.checkObject(otd)) {
					I18nString message = new CNI18nString("objectproperty.nopermission");
					message.setParameter("0", opId);
				throw new InsufficientPrivilegesException(message.toString(), otd, PermType.read);
				}

				node.addObjectTagDefinition(otd);
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		}
	}

	@Override
	@DELETE
	@Path("/{nodeId}/objectproperties/{objectPropertyId}")
	public GenericResponse removeObjectProperty(@PathParam("nodeId") String nodeId, @PathParam("objectPropertyId") String opId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Transaction t = TransactionManager.getCurrentTransaction();

				if (!t.getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER, node.getFolder().getId(), PermHandler.PERM_NODE_CONSTRUCT_MODIFY)) {
					I18nString message = new CNI18nString("rest.node.nopermission");
					message.setParameter("0", nodeId);
				throw new InsufficientPrivilegesException(message.toString(), node, PermType.updateconstructs);
				}

				ObjectTagDefinition otd = t.getObject(ObjectTagDefinition.class, opId);

				if (otd == null) {
					I18nString message = new CNI18nString("objectproperty.notfound");
					message.setParameter("0", opId);
					throw new EntityNotFoundException(message.toString());
				}

				if (!PermHandler.ObjectPermission.view.checkObject(otd)) {
					I18nString message = new CNI18nString("objectproperty.nopermission");
					message.setParameter("0", opId);
				throw new InsufficientPrivilegesException(message.toString(), otd, PermType.read);
				}

				node.removeObjectTagDefinition(otd);
			}

			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, null));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.NodeResource#settings(java.lang.String)
	 */
	@GET
	@Path("/{nodeId}/settings")
	@Override
	public NodeSettingsResponse settings(@PathParam("nodeId") String nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, ObjectPermission.view);

			if (node == null) {
				String errorMsg = "node {" + nodeId + "} not found";
				CNI18nString message = new CNI18nString("rest.node.notfound");

				logger.error("Could not load node settings: " + errorMsg);
				message.addParameter(nodeId);

				return new NodeSettingsResponse(
					new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.NOTFOUND, errorMsg));
			}

			nodeId = node.getId().toString();

			NodePreferences prefs = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> property = prefs.getPropertyMap(CONFIG_NODE_SETTINGS_GLOBAL);
			JsonNode settings;

			try {
				settings = ObjectTransformer.isEmpty(property) ? null : mapper.convertValue(property, JsonNode.class);
				property = prefs.getPropertyObject(node, CONFIG_NODE_SETTINGS);

				if (!ObjectTransformer.isEmpty(property)) {
					JsonNode nodeSettings = mapper.convertValue(property, JsonNode.class);

					if (settings == null) {
						settings = nodeSettings;
					} else if (nodeSettings != null) {
						mapper.updateValue(settings, nodeSettings);
					}
				}
			} catch (Exception e) {
				String errorMsg = "Could not load node settings: " + e.getMessage();
				CNI18nString message = new CNI18nString("rest.node.settings");

				logger.error(errorMsg, e);
				message.addParameter(nodeId);

				return new NodeSettingsResponse(
					new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, errorMsg));
			}

			trx.success();
			return new NodeSettingsResponse(settings, new ResponseInfo(ResponseCode.OK, "Loaded settings for " + node));
		}
	}

	@Override
	@GET
	@Path("/features")
	public FeatureModelList availableFeatures(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<NodeFeature> available = Stream.of(NodeFeature.values())
					.filter(f -> Feature.valueOf(f.toString().toUpperCase()).isActivated())
					.collect(Collectors.toList());

			List<NodeFeatureModel> availableModels = new ArrayList<>();
			for (NodeFeature f : available) {
				availableModels.add(new NodeFeatureModel().setId(f).setName(I18NHelper.get("feature." + f.name()))
						.setDescription(I18NHelper.get("feature." + f.name() + ".help")));
			}

			BiFunction<NodeFeatureModel, String, Object> resolver = (o, key) -> {
				try {
					return ClassHelper.invokeGetter(o, key);
				} catch (Exception e) {
					return new NodeException(e);
				}
			};

			FeatureModelList featureModelList = ListBuilder.from(availableModels, m -> m)
					.filter(MiscUtils.filter(filter, resolver, "id", "name", "description")).sort(comparator(sorting, resolver, "id", "name", "description"))
					.page(paging).to(new FeatureModelList());

			trx.success();

			return featureModelList;
		}
	}

	@Override
	@POST
	@Path("/{nodeId}/copy")
	public GenericResponse copy(@PathParam("nodeId") String nodeId, @QueryParam("wait") @DefaultValue("0") long waitMs, NodeCopyRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return Operator.executeRethrowing(I18NHelper.get("node_copy"), waitMs, () -> {
				if (!ObjectPermission.create.checkClass(null, Node.class, null)) {
					throw new InsufficientPrivilegesException(I18NHelper.get("rest.node.create.nopermission"), null, null, Node.TYPE_NODE, 0, PermType.create);
				}

				Node node = getNode(nodeId, ObjectPermission.view);
				if (node.isChannel()) {
					throw new RestMappedException(I18NHelper.get("devtools.action.not_allowed_for_channels"))
							.setStatus(Status.BAD_REQUEST);
				}
				String message = NodeCopyQueueEntry.copy(node, request);

				trx.success();
				return new GenericResponse(new Message(Type.SUCCESS, message), new ResponseInfo(ResponseCode.OK, ""));
			});
		}
	}
}
