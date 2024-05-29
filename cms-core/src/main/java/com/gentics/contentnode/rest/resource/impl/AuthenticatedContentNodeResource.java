/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: ContentNodeResource.java,v 1.10.2.1 2011-02-10 13:43:31 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.reduceList;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.MultiPart;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.MultiChannellingFallbackList;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.utility.AbstractComparator;
import com.gentics.contentnode.object.utility.FileComparator;
import com.gentics.contentnode.object.utility.FolderComparator;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.FileUsageListResponse;
import com.gentics.contentnode.rest.model.response.FolderUsageListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.AuthenticatedResource;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Abstract class for REST resources that provides basic
 * functionality for interacting with GCN.<br /><br />
 *
 * Every Resource that uses this class as superclass must be
 * called with a query parameter "sid".
 * If "sid" is not provided the request will return a "400 - Bad Request" Error.<br /><br />
 *
 * This class provides access to a transaction initialized for the provided session id.
 * After processing a request this transaction is automatically committed if it is open.
 *
 * @author floriangutmann
 */
@Produces({ MediaType.APPLICATION_JSON })
public abstract class AuthenticatedContentNodeResource extends AbstractContentNodeResource implements AuthenticatedResource {

	/**
	 * Session id that is injected by JAX-RS.
	 */
	private String sessionId;
	/**
	 * Bodypart name for blueimp uploader data
	 */
	public static final String BLUEIMP_DATA_BODY_PART_KEY = "files[]";
	/**
	 * Default Bodypart name for uploadify data
	 */
	public static final String UPLOADIFY_DEFAULT_DATA_BODY_PART_KEY = "Filedata";
	/**
	 * Bodypart name for uploadify data
	 */
	public static final String UPLOADIFY_DATA_BODY_PART_KEY = "fileBinaryData";
	/**
	 * Bodypart name for qqfile uploader data
	 */
	public static final String QQFILE_DATA_BODY_PART_KEY = "qqfile";
	/**
	 * GET parameter name for qqfile's filename
	 */
	public static final String QQFILE_FILENAME_PARAMETER_NAME = "qqfile";

	/**
	 * Create an instance
	 */
	public AuthenticatedContentNodeResource() {
		super();
	}

	/**
	 * Create an instance using the given transaction
	 * @param t transaction to use
	 */
	public AuthenticatedContentNodeResource(Transaction t) {
		super(t);
	}

	/**
	 * Initializes the ContentNodeResource with all GCN specific objects
	 */
	@PostConstruct
	public void initialize() {

		// The sessionId must not be null because the transaction that gets
		// started later must have an associated user.
		if (ObjectTransformer.isEmpty(sessionId)) {
			GenericResponse response = new GenericResponse();

			response.setResponseInfo(new ResponseInfo(ResponseCode.AUTHREQUIRED, "Missing SID"));
			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity(response).build());
		}
		super.initialize();

		// the session must either be authenticatable by the sessionId, if it
		// is a session token that contains the secret, or by the session
		// secret stored as a cookie.
		SessionToken token;

		Transaction t = TransactionManager.getCurrentTransactionOrNull();

		try {
			token = new SessionToken(sessionId, getSessionSecret());
		} catch (InvalidSessionIdException e) {
			GenericResponse response = new GenericResponse();

			if (t != null) {
				try {
					t.rollback();
				} catch (TransactionException ignored) {
				}
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.AUTHREQUIRED, "Invalid SID"));
			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity(response).build());
		}

		Session session = transaction.getSession();

		if (!token.authenticates(session)) {
			GenericResponse response = new GenericResponse();

			if (t != null) {
				try {
					t.rollback();
				} catch (TransactionException ignored) {
				}
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.AUTHREQUIRED, "Invalid SID"));
			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity(response).build());
		}
		try {
			session.touch();
			t.commit(false);
		} catch (NodeException e) {
			throw new WebApplicationException(e);
		}

		// set the language id
		ContentNodeHelper.setLanguageId(session.getLanguageId());
	}

	@Override
	public void createTransaction() throws NodeException {
		if (transaction == null) {
			transaction = getFactory().startTransaction(sessionId, true);
			createdTransaction = true;
		}
	}

	/**
	 * Set the sessionId for this request.
	 *
	 * @param sessionId Id of the session to use for this ContentNodeResource
	 */
	@QueryParam("sid")
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Get the session Id of the current request
	 * @return The session Id of the current request
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Helper method to transform the systemuser into a User Model
	 * @param systemUser system user object
	 * @return User model
	 */
	protected User getUser(SystemUser systemUser) {
		if (systemUser == null) {
			return null;
		}

		User user = new User();

		user.setId(ObjectTransformer.getInteger(systemUser.getId(), null));
		user.setFirstName(systemUser.getFirstname());
		user.setLastName(systemUser.getLastname());
		user.setEmail(systemUser.getEmail());
		user.setDescription(systemUser.getDescription());

		return user;
	}

	/**
	 * Check whether the user has the given permission bit set on the given folder
	 * @param folderId id of the folder to check
	 * @param permBit permission bit to check
	 * @return true when the user has the permission bit set, false if not
	 */
	protected boolean checkFolderPermission(Integer folderId, int permBit) {
		PermHandler permHandler = transaction.getPermHandler();

		return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folderId, permBit) || permHandler.checkPermissionBit(Node.TYPE_NODE, folderId, permBit);
	}

	/**
	 * Check whether the user has ALL of the given permission bits set on the given folder
	 * @param folder folder to check
	 * @param perms array of permissions to check
	 * @return true when the user has ALL of the permission bits set, false if not
	 */
	protected boolean checkFolderPermissions(Folder folder, PermHandler.ObjectPermission... perms) throws NodeException {
		boolean perm = true;

		for (PermHandler.ObjectPermission p : perms) {
			perm &= p.checkObject(folder);
		}

		return perm;
	}

	/**
	 * Filter the object IDs in the given result set for the given node.
	 *
	 * @param result The result set to filter.
	 * @param containsDisinheritInfo Whether the result set contains disinherit information.
	 * @param nodeId The node of the object for which usage statistics are generated
	 *
	 * @return The object IDs in the usage result set, that should actually be shown
	 *		for the given node id.
	 */
	private Set<Integer> filterUsageResult(ResultSet result, boolean containsDisinheritInfo, Integer nodeId)
			throws NodeException, SQLException {
		Transaction t = getTransaction();
		Node node = t.getObject(Node.class, nodeId);
		Set<Integer> filtered = new HashSet<>();

		if (node != null
				&& t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			if (node.isChannel()) {
				MultiChannellingFallbackList fallback = new MultiChannellingFallbackList(node);

				while (result.next()) {
					boolean mcExclude;
					Integer disinheritedNode;

					if (containsDisinheritInfo) {
						mcExclude = result.getBoolean("mc_exclude");
						disinheritedNode = result.getInt("disinherited_node");
					} else {
						mcExclude = false;
						disinheritedNode = null;
					}

					fallback.addObject(
						result.getInt("id"),
						result.getInt("channelset_id"),
						result.getInt("channel_id"),
						mcExclude,
						disinheritedNode);
				}

				filtered.addAll(fallback.getObjectIds());
			} else {
				// When the node is a master node, only objects from different
				// channel structures should remain in the result.
				Set<Integer> subChannels = new HashSet<>();

				for (Node channel : node.getAllChannels()) {
					subChannels.add(channel.getId());
				}

				while (result.next()) {
					Integer objChannel = result.getInt("channel_id");

					if (!subChannels.contains(objChannel)) {
						filtered.add(result.getInt("id"));
					}
				}
			}
		} else {
			while (result.next()) {
				filtered.add(result.getInt("id"));
			}
		}

		return filtered;
	}

	/**
	 * Authenticate using the given metaData properties
	 */
	protected void authenticate(Properties metaData) {

		/*
		 * We need to extract the session data here since it is not possible to do this within the AuthenticatedContentNodeResouce. You can
		 * only read the inputstream from the body once. After we read the data we can set the session fields to that class and reinvoke the
		 * initialize method to validate the session.
		 */
		String sessionId = (String) metaData.get(SessionToken.SESSION_ID_QUERY_PARAM_NAME);

		if (sessionId != null) {
			this.setSessionId(sessionId);
		}

		String sessionSecret = (String) metaData.get(SessionToken.SESSION_SECRET_COOKIE_NAME);

		if (sessionSecret != null) {
			this.setSessionSecret(sessionSecret);
		}

		super.initialize();
	}

	/**
	 * Determine a list of file ids which reference the given list of elements.
	 *
	 * @param objId List of element ids
	 * @param objType Element type id
	 * @param nodeId Node id to be used for usage lookup
	 * @param isImage
	 * @return List of file ids
	 */
	protected Set<Integer> getFileUsageIds(List<Integer> objId, int objType, Integer nodeId, boolean isImage) throws NodeException {
		Transaction t = getTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			String idPlaceholders = StringUtils.repeat("?", objId.size(), ",");
			String typeIds = null;
			String dsTypes = null;
			int targetType = isImage ? ImageFile.TYPE_IMAGE : File.TYPE_FILE;

			switch (objType) {
			case ImageFile.TYPE_IMAGE:
			case File.TYPE_FILE:
				typeIds = "6, 8, 14";
				dsTypes = "10008, 10011";
				break;

			case Page.TYPE_PAGE:
				typeIds = "4";
				dsTypes = "10007";
				break;

			default:
				throw new NodeException("Error while getting usage info: unkown type " + objType + " given");
			}

			StringBuffer sql = new StringBuffer();
			List<Integer> params = new Vector<Integer>();

			// next: objects in objecttags
			sql.append("select distinct contentfile.id, contentfile.channelset_id, contentfile.channel_id, contentfile.mc_exclude, contentfile_disinherit.channel_id disinherited_node from contentfile left join objtag on obj_type = ")
			.append(targetType).append(" and obj_id = contentfile.id ")
			.append("LEFT JOIN contentfile_disinherit on contentfile.id = contentfile_disinherit.contentfile_id ")
			.append("left join value on value.objtag_id = objtag.id left join part on value.part_id = part.id ")
			.append("where objtag.enabled = 1 and part.type_id in (").append(typeIds).append(") and value_ref in (").append(idPlaceholders).append(") AND contentfile.deleted = 0 ")
			.append("union distinct ");

			// next: objects directly selected in overviews in objecttags
			sql.append("select distinct contentfile.id, contentfile.channelset_id, contentfile.channel_id, contentfile.mc_exclude, contentfile_disinherit.channel_id disinherited_node from contentfile left join objtag on obj_type = ").append(targetType).append(" and obj_id = contentfile.id ")
			.append("LEFT JOIN contentfile_disinherit on contentfile.id = contentfile_disinherit.contentfile_id ")
			.append("left join ds on objtag.id = ds.objtag_id right join ds_obj on objtag.id = ds_obj.objtag_id ")
			.append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(") AND contentfile.deleted = 0");

			// fill in the ids 2 times
			for (int i = 0; i < 2; ++i) {
				params.addAll(objId);
			}

			pst = t.prepareStatement(sql.toString());
			// fill in the params
			int pCounter = 1;

			for (Object o : params) {
				pst.setObject(pCounter++, o);
			}
			res = pst.executeQuery();

			Set<Integer> usingFileIds = filterUsageResult(res, true, nodeId);
			return usingFileIds;
		} catch (SQLException e) {
			throw new NodeException("Error while getting usageinfo for files", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Get the files using one of the given objects.
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortby
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortorder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param objType type of the objects
	 * @param objId list of object ids
	 * @param nodeId id of the node
	 * @param returnFiles true if the files shall be returned, false for only returning the numbers
	 * @param isImage true of the target objects shall be images, false for files
	 * @return list of files
	 */
	protected FileUsageListResponse getFileUsage(Integer skipCount,
			Integer maxItems, String sortBy, String sortOrder, int objType,
			List<Integer> objId, Integer nodeId, boolean returnFiles, boolean isImage) throws NodeException {

			Set<Integer> usingFileIds = getFileUsageIds(objId, objType, nodeId, isImage);
			// get the files
			Transaction t = getTransaction();
			List<File> usingFiles = new Vector<File>(t.getObjects(File.class, usingFileIds));
			int withoutPermission = 0;
			int total = usingFiles.size();
			List<com.gentics.contentnode.rest.model.File> restFiles = null;

			if (returnFiles) {
				// optionally sort the list
				if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)) {
					Collections.sort(usingFiles, new FileComparator(sortBy, sortOrder));
				}

				// do paging
				reduceList(usingFiles, skipCount, maxItems);

				// prepare list of REST objects
				restFiles = new Vector<com.gentics.contentnode.rest.model.File>(usingFiles.size());
			}

			// filter out (and count) the files without permission
			for (File file : usingFiles) {
				if (PermHandler.ObjectPermission.view.checkObject(file)) {
					if (returnFiles) {
						restFiles.add(ModelBuilder.getFile(file, Collections.emptyList()));
					}
				} else {
					withoutPermission++;
				}
			}

			return new FileUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched files using other objects"), restFiles, total,
					withoutPermission);
	}

	/**
	 * Determine list of folder ids which reference one or more of the given elements.
	 *
	 * @param objId List of element ids
	 * @param objType Element type id
	 * @return List of folder ids
	 */
	protected Set<Integer> getFolderUsageIds(List<Integer> objId, int objType) throws NodeException {
		Transaction t = getTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			String idPlaceholders = StringUtils.repeat("?", objId.size(), ",");
			String typeIds = null;
			String dsTypes = null;

			switch (objType) {
			case ImageFile.TYPE_IMAGE:
			case File.TYPE_FILE:
				typeIds = "6, 8, 14";
				dsTypes = "10008, 10011";
				break;

			case Page.TYPE_PAGE:
				typeIds = "4";
				dsTypes = "10007";
				break;

			default:
				throw new NodeException("Error while getting usage info: unkown type " + objType + " given");
			}

			StringBuffer sql = new StringBuffer();
			List<Integer> params = new Vector<Integer>();

			// next: objects in objecttags
			// TODO: , folder.channelset_id, folder.channel_id
			sql.append("select distinct folder.id from folder left join objtag on obj_type = 10002 and obj_id = folder.id ").append("left join value on value.objtag_id = objtag.id left join part on value.part_id = part.id ").append("where objtag.enabled = 1 and part.type_id in (").append(typeIds).append(") and value_ref in (").append(idPlaceholders).append(") AND folder.deleted = 0 ").append(
					" union distinct ");

			// next: objects directly selected in overviews in objecttags
			// TODO: , folder.channelset_id, folder.channel_id
			sql.append("select distinct folder.id from folder left join objtag on obj_type = 10002 and obj_id = folder.id ").append("left join ds on objtag.id = ds.objtag_id right join ds_obj on objtag.id = ds_obj.objtag_id ").append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(
					") AND folder.deleted = 0");

			// fill in the ids 2 times
			for (int i = 0; i < 2; ++i) {
				params.addAll(objId);
			}

			pst = t.prepareStatement(sql.toString());
			// fill in the params
			int pCounter = 1;

			for (Object o : params) {
				pst.setObject(pCounter++, o);
			}
			res = pst.executeQuery();

			Set<Integer> usingFolderIds = new HashSet<>();

			while (res.next()) {
				usingFolderIds.add(res.getInt("id"));
			}
			t.closeResultSet(res);
			t.closeStatement(pst);
			res = null;
			pst = null;
			return usingFolderIds;
		} catch (SQLException e) {
			throw new NodeException("Error while getting usageinfo for folders", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Return the total usage info for the given element list and type.
	 *
	 * This method should only be used to retrieve the total count for files and
	 * images. Pages have another page specific count method.
	 *
	 * @param masterMap map of master ids to original ids
	 * @param type
	 *            Element type
	 * @param nodeId
	 *            Node id which will be used to retrieve affected element ids
	 * @return Rest response which contains the total usage info
	 */
	protected TotalUsageResponse getTotalUsageInfo(Map<Integer, Integer> masterMap, int type, Integer nodeId) throws NodeException {

		TotalUsageResponse response = new TotalUsageResponse();

		for (Map.Entry<Integer, Integer> entry : masterMap.entrySet()) {
			int masterId = entry.getKey();
			int originalId = entry.getValue();

			TotalUsageInfo info = new TotalUsageInfo();
			// Folders
			Set<Integer> usingFolderIds = getFolderUsageIds(Arrays.asList(masterId), type);
			info.setFolders(usingFolderIds.size());

			// Pages
			Set<Integer> usingPageIds = MiscUtils.getPageUsageIds(Arrays.asList(masterId), type, PageUsage.GENERAL, nodeId);
			info.setPages(usingPageIds.size());

			// Templates
			Set<Integer> usingTemplateIds = MiscUtils.getTemplateUsageIds(Arrays.asList(masterId), type, nodeId);
			info.setTemplates(usingTemplateIds.size());

			// Images
			Set<Integer> usingImageIds = getFileUsageIds(Arrays.asList(masterId), type, nodeId, true);
			info.setImages(usingImageIds.size());

			// Files
			Set<Integer> usingFileIds = getFileUsageIds(Arrays.asList(masterId), type, nodeId, false);
			info.setFiles(usingFileIds.size());
			info.setTotal(usingFolderIds.size() + usingPageIds.size() + usingTemplateIds.size() + usingImageIds.size() + usingFileIds.size());
			response.getInfos().put(originalId, info);
		}
		response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully fetched total usage information"));
		return response;
	}

	/**
	 * Get the folders using one of the given objects.
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *            returning all items
	 * @param sortby
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortorder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param objType
	 *            type of the objects
	 * @param objId
	 *            list of object ids
	 * @param nodeId
	 *            id of the node
	 * @param returnFolders
	 *            true if the folders shall be returned, false for only
	 *            returning the numbers
	 * @return list of folders
	 */
	protected FolderUsageListResponse getFolderUsage(Integer skipCount, Integer maxItems, String sortBy, String sortOrder, int objType, List<Integer> objId, Integer nodeId,
			boolean returnFolders) throws NodeException {
		Set<Integer> usingFolderIds = getFolderUsageIds(objId, objType);

		// get the folders
		Transaction t = getTransaction();
		List<Folder> usingFolders = new Vector<Folder>(t.getObjects(Folder.class, usingFolderIds));
		int withoutPermission = 0;
		int total = usingFolders.size();
		List<com.gentics.contentnode.rest.model.Folder> restFolders = null;

		if (returnFolders) {
			// optionally sort the list
			if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)) {
				Collections.sort(usingFolders, new FolderComparator(sortBy, sortOrder));
			}

			// do paging
			reduceList(usingFolders, skipCount, maxItems);

			// prepare list of REST objects
			restFolders = new Vector<com.gentics.contentnode.rest.model.Folder>(usingFolders.size());
		}

		// filter out (and count) the folders without permission
		for (Folder folder : usingFolders) {
			if (PermHandler.ObjectPermission.view.checkObject(folder)) {
				if (returnFolders) {
					restFolders.add(ModelBuilder.getFolder(folder));
				}
			} else {
				withoutPermission++;
			}
		}
		return new FolderUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched files using other objects"), restFolders, total, withoutPermission);
	}

	/**
	 * Check whether the user has all necessary permissions to implicitly restore the master object and folder(s)
	 * of the given object (if master object and/or folder(s) are in the wastebin).
	 * @param obj object to check
	 * @throws InsufficientPrivilegesException if the user lacks any of the necessary permissions
	 * @throws NodeException if something goes wrong
	 */
	protected void checkImplicitRestorePermissions(LocalizableNodeObject<?> obj)
			throws InsufficientPrivilegesException, NodeException {
		ObjectPermission[] perms = new ObjectPermission[] { ObjectPermission.view, ObjectPermission.wastebin };

		if (!obj.isMaster()) {
			LocalizableNodeObject<?> master = obj.getMaster();

			if (master.isDeleted()) {
				for (PermHandler.ObjectPermission p : perms) {
					if (!p.checkObject(master)) {
						I18nString message = new CNI18nString("wastebin.restore.implicit.master.nopermission");

						message.setParameter("0", obj.getName());
						message.setParameter("1", I18NHelper.getPath(master, false));

						throw new InsufficientPrivilegesException(message.toString(), master, p.getPermType());
					}
				}

			}
		}

		Folder mother = (Folder) obj.getParentObject();

		while (mother != null && mother.isDeleted()) {
			try (ChannelTrx trx = new ChannelTrx(mother.getChannel())) {
				for (PermHandler.ObjectPermission p : perms) {
					if (!p.checkObject(mother)) {
						I18nString message = new CNI18nString("wastebin.restore.implicit.folder.nopermission");

						message.setParameter("0", obj.getName());
						message.setParameter("1", I18NHelper.getPath(mother, false));

						throw new InsufficientPrivilegesException(message.toString(), mother, p.getPermType());
					}
				}
			}

			mother = (Folder) mother.getParentObject();
		}
	}

	/**
	 * Checks whether the given name equals to one of the configured or known data body part names
	 *
	 * @param name
	 * @param customFileDataBodyPartName
	 *            the name of the custom file data body part. It will be ignored if null.
	 * @return
	 */
	protected boolean isKnownFileDataBodyPartName(String name, String customFileDataBodyPartName) {
		return AuthenticatedContentNodeResource.UPLOADIFY_DEFAULT_DATA_BODY_PART_KEY.equalsIgnoreCase(name) || AuthenticatedContentNodeResource.BLUEIMP_DATA_BODY_PART_KEY.equalsIgnoreCase(name)
				|| AuthenticatedContentNodeResource.QQFILE_DATA_BODY_PART_KEY.equalsIgnoreCase(name) || AuthenticatedContentNodeResource.UPLOADIFY_DATA_BODY_PART_KEY.equalsIgnoreCase(name)
				|| (name != null && name.equalsIgnoreCase(customFileDataBodyPartName));
	}

	/**
	 * Returns the bodypart that contains the file data for the multipart object.
	 *
	 * @param multiPart
	 * @return found multipart object or null when no part could be found
	 */
	protected BodyPart getFileDataBodyPart(MultiPart multiPart, String customFileDataBodyPartName) {

		BodyPart fileDataBodyPart = null;

		// Extract information from bodyparts and store it
		for (BodyPart part : multiPart.getBodyParts()) {
			String bodyPartName = part.getContentDisposition().getParameters().get("name");

			// Store the identified bodypart that contains the file data
			if (isKnownFileDataBodyPartName(bodyPartName, customFileDataBodyPartName)) {
				fileDataBodyPart = part;
				continue;
			}
		}
		return fileDataBodyPart;
	}

	/**
	 * Walk through all bodyparts and extract payload information from each part
	 *
	 * @param multiPart The file attributes
	 * @param customFileDataBodyPartName Custom name for the file data body part
	 * @return properties with a set of all meta data attributes
	 * @throws WebApplicationException
	 */
	protected FileUploadMetaData getMetaData(MultiPart multiPart, String customFileDataBodyPartName)
			throws WebApplicationException  {

		FileUploadMetaData metaData = new FileUploadMetaData();

		// Extract information from bodyparts and store it
		for (BodyPart part : multiPart.getBodyParts()) {
			String currentBodyPartName = part.getContentDisposition().getParameters().get("name");

			if (isKnownFileDataBodyPartName(currentBodyPartName, customFileDataBodyPartName)) {

				if (logger.isDebugEnabled()) {
					for (String headerName : multiPart.getHeaders().keySet()) {
						List<String> headerValues = multiPart.getHeaders().get(headerName);

						for (String value : headerValues) {
							logger.debug(headerName + "=" + value);
						}
					}
				}
				if (!metaData.containsKey(FileUploadMetaData.META_DATA_FILE_NAME_KEY)) {
					String fileName = part.getContentDisposition().getParameters().get("filename");

					metaData.setFilename(fileName);
				}
				// break;
			} else if (currentBodyPartName != null) {
				try {
					String value = null;
					Object entity = part.getEntity();

					if (entity instanceof BodyPartEntity) {
						value = StringUtils.readStream(((BodyPartEntity) entity).getInputStream());
					} else if (entity instanceof String) {
						value = (String)part.getEntity();
					} else {
						throw new WebApplicationException(
								new Exception("Unsupported MultiPart entity type: " + entity.getClass().toString()));
					}

					// Do not overwrite a metaData property if is already
					// existing

					if (currentBodyPartName.equalsIgnoreCase(FileUploadMetaData.META_DATA_FILE_NAME_KEY)) {
						metaData.setFilename(value);
					} else {
						if (!metaData.containsKey(currentBodyPartName)) {
							metaData.put(currentBodyPartName, value);
						}
					}

				} catch (IOException e) {
					throw new WebApplicationException(new Exception("Error while reading from stream", e));
				}

			}
		}

		if (!metaData.containsKey(FileUploadMetaData.META_DATA_DESCRIPTION_KEY)) {
			// If the description is not set, set an empty string.
			metaData.setDescription("");
		}

		return metaData;
	}

	/**
	 * Kind of page usage
	 */
	public static enum PageUsage {
		VARIANT, TAG, GENERAL
	}

	/**
	 * Comparator for sorting items
	 */
	protected class ItemComparator extends AbstractComparator implements Comparator<ContentNodeItem> {

		/**
		 * Generates a new ItemComparator
		 * @param attribute the attribute to sort by. May be one of
		 *      name (default), edate
		 * @param way sort way, may be "asc" or "desc" - defaults to asc
		 */
		public ItemComparator(String attribute, String way) {
			super(attribute, way);
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(ContentNodeItem i1, ContentNodeItem i2) {
			int cmp = 0;

			switch (this.attribute) {
			case NAME:
				cmp = StringUtils.mysqlLikeCompare(i1.getName(), i2.getName()) * way;
				break;

			case CREATE_DATE:
				cmp = (i1.getCdate() - i2.getCdate()) * way;
				break;

			case EDIT_DATE:
				cmp = (i1.getEdate() - i2.getEdate()) * way;
				break;

			case TEMPLATE:
				cmp = StringUtils.mysqlLikeCompare(getTemplateName(i1), getTemplateName(i2)) * way;
				break;

			case TYPE:
				cmp = (getTypeOrder(i1) - getTypeOrder(i2)) * way;
				break;

			case SIZE:
				cmp = (getFileSize(i1) - getFileSize(i2)) * way;
				break;

			default:
				cmp = 0;
				break;
			}

			if (cmp == 0) {
				cmp = (ObjectTransformer.getInt(i1.getId(), 0) - ObjectTransformer.getInt(i2.getId(), 0)) * way;
			}

			return cmp;
		}

		/**
		 * Get the template name, if the item is an instance of {@link com.gentics.contentnode.rest.model.Page}, an empty string otherwise
		 * @param item item
		 * @return template name or empty string
		 */
		protected String getTemplateName(ContentNodeItem item) {
			if (item instanceof com.gentics.contentnode.rest.model.Page) {
				com.gentics.contentnode.rest.model.Template template = ((com.gentics.contentnode.rest.model.Page) item).getTemplate();
				if (template != null) {
					return template.getName();
				} else {
					return "";
				}
			}
			return "";
		}

		/**
		 * Get the filesize, if the item is an instance of {@link com.gentics.contentnode.rest.model.File}, 0 otherwise
		 * @param item item
		 * @return filesize or 0
		 */
		protected int getFileSize(ContentNodeItem item) {
			if (item instanceof com.gentics.contentnode.rest.model.File) {
				return ((com.gentics.contentnode.rest.model.File) item).getFileSize();
			}
			return 0;
		}

		/**
		 * Get the sort order of the item's type
		 * @param item item
		 * @return sort order
		 */
		protected int getTypeOrder(ContentNodeItem item) {
			switch (item.getType()) {
			case node:
				return 1;
			case channel:
				return 2;
			case folder:
				return 3;
			case page:
				return 4;
			case file:
				return 5;
			case image:
				return 6;
			default:
				return 0;
			}
		}
	}
}

/**
 * Simple class that will handle the file upload meta data.
 *
 * @author johannes2
 *
 */
@SuppressWarnings("serial")
class FileUploadMetaData extends Properties {

	public static final String META_DATA_FOLDERID_KEY    = "folderId";
	public static final String META_DATA_DESCRIPTION_KEY = "description";
	public static final String META_DATA_FILE_NAME_KEY   = "fileName";
	public static final String META_DATA_NODE_ID_KEY     = "nodeId";
	public static final String META_DATA_OVERWRITE_KEY   = "overwrite";



	/**
	 * GET parameter name for specifying a custom bodypart key name
	 */
	public static final String META_DATA_BODY_PART_KEY_CUSTOM_PARAMETER_NAME = "databodypart";

	/**
	 * Checks whether the property has been set and is not empty
	 *
	 * @param name
	 * @return
	 */
	public boolean hasProperty(String name) {
		return StringUtils.isEmpty(this.getProperty(name));
	}

	/**
	 * Return the filename
	 *
	 * @return
	 */
	public String getFilename() {
		return getProperty(META_DATA_FILE_NAME_KEY);
	}

	/**
	 * Sets the filename
	 *
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.setProperty(META_DATA_FILE_NAME_KEY, filename);
	}

	/**
	 * Returns the folder id
	 *
	 * @return
	 */
	public Integer getFolderId() {
		String folderIdStr = getProperty(META_DATA_FOLDERID_KEY);

		return Integer.valueOf(folderIdStr);
	}

	/**
	 * Sets the folderId
	 *
	 * @param folderIdStr
	 */
	public void setFolderId(String folderIdStr) {
		this.setProperty(META_DATA_FOLDERID_KEY, folderIdStr);
	}

	/**
	 * Returns the node id
	 *
	 * @return
	 */
	public Integer getNodeId() {
		String nodeIdStr = getProperty(META_DATA_NODE_ID_KEY);

		return ObjectTransformer.getInt(nodeIdStr, 0);

	}

	/**
	 * Sets the folderId
	 *
	 * @param nodeId
	 */
	public void setNodeId(String nodeId) {
		this.setProperty(META_DATA_NODE_ID_KEY, nodeId);
	}

	/**
	 * Returns description that was set within the metadata
	 *
	 * @return
	 */
	public String getDescription() {
		return getProperty(META_DATA_DESCRIPTION_KEY);
	}

	/**
	 * Sets the description
	 *
	 * @param description
	 */
	public void setDescription(String description) {
		this.setProperty(META_DATA_DESCRIPTION_KEY, description);
	}

	/**
	 * Returns whether existing files will be overwritten
	 *
	 * @return True when existing files should be overwritten, null if not defined
	 */
	public boolean getOverwrite() {
		return ObjectTransformer.getBoolean(getProperty(META_DATA_OVERWRITE_KEY), false);
	}

	/**
	 * Sets wether existing files with the same name will be overwritten
	 *
	 * @param value Should be "true" or "false"
	 */
	public void setOverwrite(String value) {
		this.setProperty(META_DATA_OVERWRITE_KEY, value);
	}
}
