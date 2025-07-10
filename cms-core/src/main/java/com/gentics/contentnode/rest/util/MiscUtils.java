/*
 * @author tobiassteiner
 * @date Jan 22, 2011
 * @version $Id: MiscUtils.java,v 1.1.2.1 2011-02-10 13:43:36 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.etc.ThrowingConsumer;
import com.gentics.contentnode.exception.FeatureRequiredException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.MultiChannellingFallbackList;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.object.utility.CNTemplateComparator;
import com.gentics.contentnode.object.utility.PageComparator;
import com.gentics.contentnode.perm.NamedPerm;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.InstantPublisher.Result;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.RoleItem;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;
import com.gentics.contentnode.rest.model.perm.TypePermissions;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TemplateSortAttribute;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TypePermissionList;
import com.gentics.contentnode.rest.model.response.UserListResponse;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.AuthenticatedContentNodeResource.PageUsage;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
// import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.ClassHelper;

import io.reactivex.Flowable;
import jakarta.ws.rs.core.Response.Status;

public class MiscUtils {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MiscUtils.class);

	/**
	 * Function that checks for blank/null value in fields for new entity
	 */
	public final static Function<Pair<String, Object>, String> NEW_FIELD_CHECKER = pair -> StringUtils
			.isBlank(ObjectTransformer.getString(pair.getValue(), null))
					? I18NHelper.get("exception.missing.field", pair.getKey())
					: null;
	/**
	 * Function that checks for blank value in fields for updated entity
	 */
	public final static Function<Pair<String, Object>, String> UPDATE_FIELD_CHECKER = pair -> {
		String value = ObjectTransformer.getString(pair.getValue(), null);
		if (value == null || !StringUtils.isBlank(value)) {
			return null;
		} else {
			return I18NHelper.get("exception.missing.field", pair.getKey());
		}
	};

	/**
	 * Transform the given language code into a contentlanguage which can be set to the given page
	 * @param page page
	 * @param languageCode language code
	 * @return contentlanguage object or null if no matching was found
	 * @throws NodeException
	 */
	public static ContentLanguage getRequestedContentLanguage(Page page, String languageCode) throws NodeException {
		return getRequestedContentLanguage(page.getFolder(), languageCode);
	}

	/**
	 * Transform the given language code into a contentlanguage which can be set to pages in the given folder
	 * @param folder folder
	 * @param languageCode language code
	 * @return contentlanguage object or null if no matching was found
	 * @throws NodeException
	 */
	public static ContentLanguage getRequestedContentLanguage(Folder folder, String languageCode) throws NodeException {
		// transform the language code into a language
		if (!ObjectTransformer.isEmpty(languageCode)) {
			Node node = folder.getNode();

			if (node != null) {
				List<ContentLanguage> languages = node.getLanguages();

				for (Iterator<ContentLanguage> l = languages.iterator(); l.hasNext();) {
					ContentLanguage language = l.next();

					if (languageCode.equals(language.getCode())) {
						return language;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Checks if the user of the current transaction has edit/update
	 * permission on all the passed restobject tags and throws an
	 * InsufficientPrivilegesException exception if not.
	 * @param restTags    A map of object tags, as gotten from rest.model.Page.getTags
	 * @param objectTags  A map of object tags, as gotten from Page.getObjectTags
	 * @param checkModified  Only throw an error if the tag was modified
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException
	 */
	public static void checkObjectTagEditPermissions(Map<String, com.gentics.contentnode.rest.model.Tag> restTags,
			Map<String, ObjectTag> objectTags, boolean checkModified) throws NodeException {
		checkObjectTagEditPermissions(new ArrayList<String>(restTags.keySet()), objectTags, checkModified);
	}

	/**
	 * Checks if the user of the current transaction has edit/update
	 * permission on all the passed restobject tags and throws an
	 * InsufficientPrivilegesException exception if not.
	 * @param restTags       A list of objecttag names, including the "object." part
	 * @param objectTags     A map of object tags, as gotten from Page.getObjectTags
	 * @param checkModified  Only throw an error if the tag was modified
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException
	 */
	public static void checkObjectTagEditPermissions(List<String> restTags,
			Map<String, ObjectTag> objectTags, boolean checkModified) throws NodeException {
		PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();

		if (restTags != null) {
			for (String restTagName : restTags) {
				if (restTagName.length() == 0) {
					continue;
				}

				if (restTagName.startsWith("object.")) {
					// Object property
					String realName = restTagName.substring(7);
					ObjectTag objectTag = objectTags.get(realName);

					if (objectTag == null) {
						// If the object tag doesn't exist or has not been
						// modified at all, we actually don't care about it.
						continue;
					}

					// Ignore the tag if checkModified is false or the tag is not modified
					if (!checkModified || !objectTag.isTagOrValueModified()) {
						continue;
					}

					// Check if the user has actually permission to modify that object tag
					if (!permHandler.canEdit(objectTag)) {
						I18nString message = new CNI18nString("objecttag.nopermission.edit");
						message.setParameter("0", objectTag.getName());
						throw new InsufficientPrivilegesException(message.toString(), objectTag, PermType.update);
					}
				}
			}
		}
	}

	/**
	 * Create a GenericResponse instance for an internal server error
	 * @return generic response
	 */
	public static GenericResponse serverError() {
		return new GenericResponse(new Message(Message.Type.CRITICAL, I18NHelper.get("rest.general.error")), new ResponseInfo(ResponseCode.FAILURE,
				"Server Error"));
	}

	/**
	 * Load an object with internal or external id and check the view permission (and other optional permissions)
	 * When the object is not found, an {@link EntityNotFoundException} is thrown.
	 * When the transaction does not provide sufficient permissions an {@link InsufficientPrivilegesException} is thrown.
	 * <br/>
	 * The exceptions will contain translated messages where the keys are [tablename].notfound or [tablename].nopermission,
	 * where [tablename] is the name of the DB table where instances of clazz are stored. The translations are expected to contain
	 * a single variable, which will be filled with the given ID.
	 * @param clazz object class
	 * @param id object id (internal or external)
	 * @param perms optional permissions to check
	 * @return returned object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T load(Class<T> clazz, String id, ObjectPermission...perms) throws NodeException {
		return load(clazz, id, true, perms);
	}

	/**
	 * Load an object with internal or external id and don't check the permission
	 * When the object is not found, an {@link EntityNotFoundException} is thrown (if flag expectExistence is true), or null will be returned.
	 * When the transaction does not provide sufficient permissions an {@link InsufficientPrivilegesException} is thrown.
	 * <br/>
	 * The exceptions will contain translated messages where the keys are [tablename].notfound or [tablename].nopermission,
	 * where [tablename] is the name of the DB table where instances of clazz are stored. The translations are expected to contain
	 * a single variable, which will be filled with the given ID.
	 * @param clazz object class
	 * @param id object id (internal or external)
	 * @param expectExistence flag to influence behaviour, when the object is not found: true will throw an EntityNotFoundException, false will return null
	 * @return returned object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T loadWithoutPermissionCheck(Class<T> clazz, String id, boolean expectExistence) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		T obj = t.getObject(clazz, id);
		if (obj == null) {
			if (expectExistence) {
				throw new EntityNotFoundException(
						I18NHelper.get(String.format("%s.notfound", t.getTable(clazz)), id));
			} else {
				return null;
			}
		}
		return obj;
	}

	/**
	 * Load an object with internal or external id and check the view permission (and other optional permissions)
	 * When the object is not found, an {@link EntityNotFoundException} is thrown (if flag expectExistence is true), or null will be returned.
	 * When the transaction does not provide sufficient permissions an {@link InsufficientPrivilegesException} is thrown.
	 * <br/>
	 * The exceptions will contain translated messages where the keys are [tablename].notfound or [tablename].nopermission,
	 * where [tablename] is the name of the DB table where instances of clazz are stored. The translations are expected to contain
	 * a single variable, which will be filled with the given ID.
	 * @param clazz object class
	 * @param id object id (internal or external)
	 * @param expectExistence flag to influence behaviour, when the object is not found: true will throw an EntityNotFoundException, false will return null
	 * @param perms optional permissions to check
	 * @return returned object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T load(Class<T> clazz, String id, boolean expectExistence, ObjectPermission...perms) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		T obj = loadWithoutPermissionCheck(clazz, id, expectExistence);
		if (obj == null) {
			return null;
		}

		if (!t.getPermHandler().canView(obj)) {
			throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", t.getTable(clazz)), id), obj, PermType.read);
		}

		for (ObjectPermission perm : perms) {
			if (!perm.checkObject(obj)) {
				throw new InsufficientPrivilegesException(
						I18NHelper.get(String.format("%s.nopermission", t.getTable(clazz)),
								String.format("%s (%d)", I18NHelper.getName(obj), obj.getId())),
						obj, perm.getPermType());
			}
		}

		return obj;
	}

	/**
	 * Check the object against permission checkers and throw an {@link InsufficientPrivilegesException} for the first checker that fails
	 * @param nodeObject object to check
	 * @param permType perm type to use in the exception
	 * @param checker single checker
	 * @param moreCheckers optional more checkers
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T extends NodeObject> void check(T nodeObject, PermType permType, BiFunction<T, PermHandler, Boolean> checker,
			BiFunction<T, PermHandler, Boolean>... moreCheckers) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String table = t.getTable(nodeObject.getObjectInfo().getObjectClass());
		String id = ObjectTransformer.getString(nodeObject.getId(), "");
		if (!checker.apply(nodeObject, t.getPermHandler())) {
			throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", table), id), nodeObject, permType);
		}
		for (BiFunction<T, PermHandler, Boolean> check : moreCheckers) {
			if (!check.apply(nodeObject, t.getPermHandler())) {
				throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", table), id), nodeObject, permType);
			}
		}
	}

	/**
	 * Get an object from a collection of objects
	 * When the object is not found, an {@link EntityNotFoundException} is thrown.
	 * When the transaction does not provide sufficient permissions an {@link InsufficientPrivilegesException} is thrown.
	 * <br/>
	 * The exceptions will contain translated messages where the keys are [tablename].notfound or [tablename].nopermission,
	 * where [tablename] is the name of the DB table where instances of clazz are stored. The translations are expected to contain
	 * a single variable, which will be filled with the given ID.
	 * @param clazz object class
	 * @param items collection of items to search
	 * @param id object id (internal or external)
	 * @param perms optional permissions to check
	 * @return found object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T get(Class<T> clazz, Collection<T> items, String id, ObjectPermission... perms) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		T obj = items.stream().filter(item -> id.equals(Integer.toString(item.getId())) || id.equals(item.getGlobalId().toString())).findFirst()
				.orElseThrow(() -> new EntityNotFoundException(I18NHelper.get(String.format("%s.notfound", t.getTable(clazz)), id)));

		for (ObjectPermission perm : perms) {
			if (!perm.checkObject(obj)) {
				throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", t.getTable(clazz)), id), obj, perm.getPermType());
			}
		}

		return obj;
	}

	/**
	 * Check whether two instances of {@link Resolvable} are equal, when checking the given list of properties.
	 * Returns true, when both r1 and r2 are null.
	 * @param r1 resolvable
	 * @param r2 resolvable
	 * @param properties list of property names to compare
	 * @return true iff the resolvables are equal
	 */
	public static boolean areEqual(Resolvable r1, Resolvable r2, String... properties) {
		if (r1 == null && r2 == null) {
			return true;
		}
		if (r1 == null || r2 == null) {
			return false;
		}
		for (String key : properties) {
			if (!ObjectTransformer.equals(r1.get(key), r2.get(key))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Check whether all resolvables in the collection are equal, when checking the given list of properties.
	 * Returns true if the collection is null, empty or contains just one item.
	 * @param resolvables resolvables to check
	 * @param properties list of property names to compare
	 * @return true iff all resolvables are equal
	 */
	public static boolean areEqual(Collection<? extends Resolvable> resolvables, String...properties) {
		if (ObjectTransformer.isEmpty(resolvables)) {
			return true;
		}
		if (resolvables.size() == 1) {
			return true;
		}

		Resolvable reference = null;
		for (Resolvable res : resolvables) {
			if (reference == null) {
				reference = res;
			} else {
				if (!areEqual(reference, res, properties)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Create conflict message for node
	 * @param modified modified/created node
	 * @param conflictingNode conflicting node
	 * @param field name of the field which should show the error message
	 * @return message
	 * @throws NodeException
	 */
	public static Message createNodeConflictMessage(Node modified, Node conflictingNode, String field) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String basePath = null;
		String conflictingName = null;
		if (t.canView(conflictingNode)) {
			conflictingName = I18NHelper.getName(conflictingNode) + " (ID " + conflictingNode.getId() + ")";
		} else {
			conflictingName = "(" + I18NHelper.get("usageinfo_noperm") + ")";
		}
		if (modified.isPubDirSegment()) {
			basePath = "//" + FilePublisher.getPath(false, true, modified.getHostname(), modified.getPublishDir(), modified.getFolder().getPublishDir());
		} else {
			basePath = "//" + FilePublisher.getPath(false, true, modified.getHostname(), modified.getPublishDir());
		}

		return new Message(Type.CRITICAL, field, I18NHelper.get("rest.node.conflict.baseUrl", basePath, conflictingName));
	}

	/**
	 * Check the flag and call one of the operators, depending on the flag value (nothing is called, when flag is null)
	 * @param flag flag to check
	 * @param whenTrue called, when flag is true
	 * @param whenFalse called, when flag is false
	 * @throws NodeException
	 */
	public static void when(Boolean flag, Operator whenTrue, Operator whenFalse) throws NodeException {
		if (Boolean.TRUE.equals(flag)) {
			whenTrue.operate();
		} else if (Boolean.FALSE.equals(flag)) {
			whenFalse.operate();
		}
	}

	/**
	 * Check whether the feature is globally activated and throw a {@link FeatureRequiredException} if not
	 * @param feature feature to check
	 * @throws NodeException
	 */
	public static void require(Feature feature) throws NodeException {
		if (!feature.isActivated()) {
			throw new FeatureRequiredException(feature);
		}
	}

	/**
	 * Check whether the feature is activated for the node and throw a {@link FeatureRequiredException} if not
	 * @param feature feature to check
	 * @param node node to check for
	 * @throws NodeException
	 */
	public static void require(Feature feature, Node node) throws NodeException {
		if (!feature.isActivated(node)) {
			throw new FeatureRequiredException(feature);
		}
	}

	/**
	 * Get the PermType for the given type (which may be either numerical or a word). Throw EntityNotFoundException if given type is unknown
	 * @param type type
	 * @return PermType instance (never null)
	 * @throws EntityNotFoundException if type is not found
	 */
	public static TypePerms getPermType(String type) throws EntityNotFoundException {
		TypePerms permType = TypePerms.get(type);
		if (permType == null) {
			throw new EntityNotFoundException(I18NHelper.get("perm.type.notfound", type));
		} else {
			return permType;
		}
	}

	/**
	 * Check whether permType supports instances, if not throw a {@link RestMappedException}.
	 * @param permType type to check
	 * @throws NodeException
	 */
	public static void expectInstances(TypePerms permType) throws NodeException {
		if (!permType.hasInstances()) {
			throw new RestMappedException(I18NHelper.get("perm.type.noinstance.required", permType.toString())) {
				/**
				 * Serial Version UID
				 */
				private static final long serialVersionUID = -2004194299901958849L;
			}.setMessageType(Type.WARNING).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
	}

	/**
	 * Check whether permType does not support instances, if it does throw a {@link RestMappedException}.
	 * @param permType type to check
	 * @throws NodeException
	 */
	public static void expectNoInstances(TypePerms permType) throws NodeException {
		if (permType.hasInstances()) {
			throw new RestMappedException(I18NHelper.get("perm.type.instance.required", permType.toString())) {
				/**
				 * Serial Version UID
				 */
				private static final long serialVersionUID = -2004194299901958849L;
			}.setMessageType(Type.WARNING).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
	}

	/**
	 * Set permissions on the type according to the request
	 * @param type type
	 * @param req request
	 * @return generic response
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	public static GenericResponse doSetPermissions(TypePerms type, SetPermsRequest req) throws InsufficientPrivilegesException, NodeException {
		// check whether user is allowed to set permissions on the type
		if (!type.canSetPerms()) {
			throw new InsufficientPrivilegesException(I18NHelper.get("perm.type.nopermission", type.toString()), null, null, type.type(), 0, PermType.setperm);
		}

		// get the groups
		List<UserGroup> groups = getGroups(req);

		// set the permission
		setPermissions(type, null, groups, type.pattern(req.getPerm()), req.isSubObjects());

		return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully set permissions"));
	}

	/**
	 * Set permissions on the object according to the request
	 * @param type type
	 * @param objId object ID
	 * @param req request
	 * @return generic response
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	public static GenericResponse doSetPermissions(TypePerms type, int objId, SetPermsRequest req) throws InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (req.isSubGroups() || req.isSubObjects()) {
			t.setInstantPublishingEnabled(false);
		}
		PermHandler permHandler = t.getPermHandler();
		// get the groups
		List<UserGroup> groups = getGroups(req);

		// check whether the type has an object class
		Class<? extends NodeObject> objClass = type.getObjectClass();
		if (objClass != null) {
			// get the object
			if (objClass.equals(Node.class)) {
				objClass = Folder.class;
			}
			NodeObject object = t.getObject(objClass, objId);
			if (object == null) {
				throw new EntityNotFoundException(I18NHelper.get("perm.instance.notfound", type.toString(), Integer.toString(objId)));
			}

			// check whether the user is allowed to change permissions for the object
			if (!type.canSetPerms(objId)) {
				throw new InsufficientPrivilegesException(
						I18NHelper.get("perm.instance.nopermission", Integer.toString(object.getTType()), Integer.toString(object.getId())), object, PermType.setperm);
			}

			// set the permission
			setPermissions(object, groups, req.getPerm(), req.getRoleIds(), permHandler, req.isSubObjects());
		} else {
			// check existence of instance
			if (!type.hasInstance(objId)) {
				throw new EntityNotFoundException(I18NHelper.get("perm.instance.notfound", type.toString(), Integer.toString(objId)));
			}

			// check whether the user is allowed to change permissions for the object
			if (!type.canSetPerms(objId)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("perm.instance.nopermission",
						Integer.toString(type.type()), Integer.toString(objId)), null, null, type.type(), objId, PermType.setperm);
			}

			// set the permission
			setPermissions(type, objId, groups, req.getPerm(), req.isSubObjects());
		}

		return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully set permissions"));
	}

	/**
	 * Get the group/group(s) addressed by the given request to set permissions. Check for existence and permission to change permissions
	 * of the group(s)
	 * @param req request
	 * @return list of groups
	 * @throws NodeException
	 */
	public static List<UserGroup> getGroups(SetPermsRequest req) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();
		// get the group
		UserGroup group = t.getObject(UserGroup.class, req.getGroupId());
		if (group == null) {
			throw new EntityNotFoundException(I18NHelper.get("usergroup.notfound", Integer.toString(req.getGroupId())));
		}

		// check whether the user is allowed to change permissions for the group
		if (!permHandler.canSetPerms(group)) {
			throw new InsufficientPrivilegesException(I18NHelper.get("usergroup.nopermission", Integer.toString(req.getGroupId())), group, PermType.setperm);
		}

		List<UserGroup> groups = new ArrayList<>();
		groups.add(group);

		// optionally get subgroups
		if (req.isSubGroups()) {
			recursiveAddSubGroups(group, groups);
		}

		return groups;
	}

	/**
	 * Recursively add all child groups to the given list
	 * @param group group to start with
	 * @param subGroups list where all child groups will be added
	 * @throws NodeException
	 */
	public static void recursiveAddSubGroups(UserGroup group, List<UserGroup> subGroups) throws NodeException {
		List<UserGroup> childGroups = group.getChildGroups();
		subGroups.addAll(childGroups);
		for (UserGroup child : childGroups) {
			recursiveAddSubGroups(child, subGroups);
		}
	}

	/**
	 * Set permissions on the type/instance
	 * @param type type
	 * @param id instance ID (if type has instances)
	 * @param groups groups to set permissions
	 * @param perm permission (pattern) to set
	 * @param recursive true to call recursively for sub types
	 * @throws NodeException
	 */
	public static void setPermissions(TypePerms type, Integer id, List<UserGroup> groups, String perm, boolean recursive) throws NodeException {
		if (perm != null) {
			// set the permission
			if (id != null) {
				PermHandler.setPermissions(type.type(), id, groups, type.pattern(perm));
			} else {
				PermHandler.setPermissions(type.type(), groups, type.pattern(perm));
			}
		}

		if (recursive) {
			for (TypePerms child : type.getChildTypes()) {
				if (child.hasInstances()) {
					for (Integer childId : child.getInstanceIds(id, null)) {
						if (child.canSetPerms(childId)) {
							setPermissions(child, childId, groups, perm, recursive);
						}
					}
				} else {
					if (child.canSetPerms()) {
						setPermissions(child, null, groups, perm, recursive);
					}
				}
			}
		}
	}

	/**
	 * Set the permission on the given object.
	 * Recursive setting is currently only implemented for folders
	 * @param object object for which the permission shall be set
	 * @param groups groups for which the permissions shall be set
	 * @param perm permission bits
	 * @param roleIds all role IDs that should be set after this method completes. IDs not mentioned will be removed. If null, no changes to the roles are performed
	 * @param permHandler perm handler
	 * @param recursive true for setting recursively
	 * @throws NodeException
	 */
	public static void setPermissions(NodeObject object, List<UserGroup> groups, String perm, Set<Integer> roleIds, PermHandler permHandler, boolean recursive)
			throws NodeException {
		if (perm != null) {
			// set the permission
			PermHandler.setPermissions(object.getTType(), object.getId(), groups, perm);
		}

		if (roleIds != null) {
			PermHandler.setRoles(object.getTType(), object.getId(), groups, roleIds);
		}

		if (object instanceof Folder) {
			Folder folder = (Folder)object;

			if (folder.isRoot()) {
				if (perm != null) {
					PermHandler.setPermissions(Node.TYPE_NODE, ObjectTransformer.getInt(object.getId(), 0), groups, perm);
				}
				if (roleIds != null) {
					PermHandler.setRoles(Node.TYPE_NODE, ObjectTransformer.getInt(object.getId(), 0), groups, roleIds);
				}
			}

			if (recursive) {
				// for setting permissions recursively, we get all child folders
				List<Folder> children = folder.getChildFolders();
				for (Folder child : children) {
					// only set permissions on master folders, which we are allowed to set permissions
					if (child.isMaster() && permHandler.canSetPerms(child)) {
						setPermissions(child, groups, TypePerms.folder.pattern(perm), roleIds, permHandler, recursive);
					}
				}

				// now also get all local child folders in channels
				Transaction t = TransactionManager.getCurrentTransaction();
				Node node = folder.getNode();
				Collection<Node> channels = node.getAllChannels();
				for (Node channel : channels) {
					t.setChannelId(channel.getId());
					List<Folder> channelChildren = new ArrayList<Folder>();
					try {
						// get all children in the channel
						channelChildren.clear();
						channelChildren.addAll(folder.getChildFolders());
						// remove all folders, that either are inherited or localized, remaining folders will be channel local
						for (Iterator<Folder> i = channelChildren.iterator(); i.hasNext();) {
							Folder channelChild = i.next();
							if (channelChild.isInherited() || !channelChild.isMaster() || !permHandler.canSetPerms(channelChild)) {
								i.remove();
							}
						}
					} finally {
						t.resetChannel();
					}

					for (Folder channelChild : channelChildren) {
						setPermissions(channelChild, groups, perm, roleIds, permHandler, recursive);
					}
				}
			}
		}
	}

	/**
	 * Transform the given list of permission items into a permission pattern.
	 * Every item with <code>value: true</code> will create a 1, every item with <code>value: false</code> will create a 0.
	 * All other bits will be set to . (dot)
	 * @param items list of permission items
	 * @return pattern
	 */
	public static String getPermPattern(List<TypePermissionItem> items) {
		char[] characters = StringUtils.repeat(".", 32).toCharArray();
		if (!ObjectTransformer.isEmpty(items)) {
			for (TypePermissionItem item : items) {
				if (item.getType() != null) {
					characters[item.getType().getBit()] = item.isValue() ? '1' : '0';
				}
			}
		}
		return String.valueOf(characters);
	}

	/**
	 * Get list of type permissions for the given group
	 * @param group group
	 * @param types list of types, for which the permissions shall be returned
	 * @param parentId optional parent ID (if parent type has instances)
	 * @param channelId optional channel ID for getting folders
	 * @return list of type permissions
	 * @throws NodeException
	 */
	public static TypePermissionList getTypePermissionList(UserGroup group, List<TypePerms> types, Integer parentId, Integer channelId) throws NodeException {
		PermHandler permHandler = TransactionManager.getCurrentTransaction().getGroupPermHandler(group.getId());
		boolean editable = TransactionManager.getCurrentTransaction().getPermHandler().canSetPerms(group);
		List<TypePermissions> list = new ArrayList<>();
		for (TypePerms type : types) {
			if (type.hasInstances()) {
				for (Integer instanceId : type.getInstanceIds(parentId, channelId)) {
					try (ChannelTrx cTrx = new ChannelTrx(channelId)) {
						if (!type.canView(instanceId)) {
							continue;
						}

						list.add(getInstancePermissions(group, permHandler, type, instanceId, channelId, editable));
					}
				}
			} else {
				if (!type.canView()) {
					continue;
				}

				list.add(getTypePermissions(permHandler, type, editable));
			}
		}

		return ListBuilder.from(list, o -> o).to(new TypePermissionList());
	}

	/**
	 * Get list of permission items for the type
	 * @param type type
	 * @param groupPermissions group permissions entry
	 * @param editable true if types shall be editable
	 * @return list of permission items
	 */
	public static List<TypePermissionItem> getPermissionItems(TypePerms type, Permissions groupPermissions, boolean editable) {
		List<TypePermissionItem> itemList = new ArrayList<>();
		for (NamedPerm perm : type.getBits()) {
			// omit types, that do not have regular bits (only role bits)
			if (perm.getType().getBit() < 0) {
				continue;
			}
			itemList.add(new TypePermissionItem().setType(perm.getType()).setLabel(perm.getLabelI18n())
					.setDescription(perm.getDescriptionI18n()).setCategory(perm.getCategoryI18n())
					.setValue(groupPermissions.check(perm.getType().getBit())).setEditable(editable));
		}
		return itemList;
	}

	/**
	 * Get permissions on the type
	 * @param permHandler group permission handler
	 * @param type type
	 * @param editable true if type shall be editable
	 * @return type permissions
	 * @throws NodeException
	 */
	public static TypePermissions getTypePermissions(PermHandler permHandler, TypePerms type, boolean editable) throws NodeException {
		editable = editable && type.canSetPerms();
		PermissionPair permissions = permHandler.getPermissions(type.type(), null, -1, -1);
		return new TypePermissions().setType(type.name()).setLabel(type.getLabel())
				.setPerms(getPermissionItems(type, permissions.getGroupPermissions(), editable)).setChildren(type.hasChildren())
				.setEditable(editable);
	}

	/**
	 * Get permissions on an instance
	 * @param group group
	 * @param permHandler group permission handler
	 * @param type type
	 * @param instanceId instance ID
	 * @param channelId optional channel ID
	 * @param editable true if type shall be editable
	 * @return instance permissions
	 * @throws NodeException
	 */
	public static TypePermissions getInstancePermissions(UserGroup group, PermHandler permHandler, TypePerms type, Integer instanceId, Integer channelId,
			boolean editable) throws NodeException {
		editable = editable && type.canSetPerms(instanceId);
		PermissionPair permissions = permHandler.getPermissions(type.type(), instanceId, -1, -1);
		TypePermissions perms = new TypePermissions().setType(type.name()).setId(instanceId).setChannelId(channelId).setLabel(type.getLabel(instanceId))
				.setPerms(getPermissionItems(type, permissions.getGroupPermissions(), editable)).setChildren(type.hasChildren(instanceId, channelId))
				.setEditable(editable);
		// add roles
		if (type.isRoles()) {
			perms.setRoles(getRoleItems(group, type, instanceId, editable));
		}
		return perms;
	}

	/**
	 * Get role items
	 * @param group group
	 * @param type type
	 * @param instanceId instance ID
	 * @param editable editable
	 * @return list of role items
	 * @throws NodeException
	 */
	public static List<RoleItem> getRoleItems(UserGroup group, TypePerms type, Integer instanceId, boolean editable) throws NodeException {
		Set<Integer> setRoles = PermHandler.getRoles(type.type(), instanceId, group);
		List<Role> roles = new ArrayList<>(
				TransactionManager.getCurrentTransaction().getObjects(Role.class, DBUtils.select("SELECT id FROM role", DBUtils.IDS)));
		Collections.sort(roles, new Comparator<Role>() {
			@Override
			public int compare(Role o1, Role o2) {
				return com.gentics.lib.etc.StringUtils.mysqlLikeCompare(o1.getName().toString(), o2.getName().toString());
			}
		});

		return roles.stream().map(role -> new RoleItem()
			.setId(role.getId())
			.setLabel(role.getName().toString())
			.setDescription(role.getDescription().toString())
			.setValue(setRoles.contains(role.getId()))
			.setEditable(editable))
			.collect(Collectors.toList());
	}

	/**
	 * Reduce the given list by skipping skipCount elements at the start and trimming to no more than maxItems elements
	 * @param list list of objects to be reduced
	 * @param skipCount number of elements to be skipped
	 * @param maxItems maximum number of items to be returned
	 */
	public static void reduceList(List<? extends Object> list, Integer skipCount, Integer maxItems) {
		// skip skipCount elements from the start
		if (skipCount > 0) {
			skipCount = Math.min(skipCount, list.size());
			for (int i = 0; i < skipCount; i++) {
				list.remove(0);
			}
		}

		if (maxItems >= 0) {
			// remove elements from the end, until no more than maxItems are present in the list
			while (list.size() > 0 && list.size() > maxItems) {
				list.remove(list.size() - 1);
			}
		}
	}

	/**
	 *
	 * @param nodeMessage
	 * @return
	 */
	public static Message getMessageFromNodeMessage(NodeMessage nodeMessage) {
		Message message = new Message();

		Level level = nodeMessage.getLevel();
		if (level == Level.FATAL || level == Level.ERROR) {
			message.setType(Type.CRITICAL);
		}
		if (level == Level.WARN) {
			message.setType(Type.WARNING);
		}
		if (level == Level.INFO) {
			message.setType(Type.INFO);
		}
		if (level == Level.DEBUG) {
			message.setType(Type.NEUTRAL);
		}
		message.setMessage(nodeMessage.getMessage());

		return message;
	}

	/**
	 * Collect linked objects in the given tag.
	 *
	 * Only parts of the {@link UrlPartType} are considered while searching
	 * for linked objects.
	 *
	 * @param tag The tag to search for linked objects
	 * @param pages A set to store page dependencies
	 * @param files A set to store file dependencies
	 * @param images A set to store image dependencies
	 *
	 * @throws NodeException On error
	 */
	public static void collectLinkedObjectsFromTag(Tag tag, Set<Page> pages, Set<File> files, Set<ImageFile> images) throws NodeException {
		for (com.gentics.contentnode.object.Value value : tag.getValues()) {
			PartType partType = value.getPartType();

			if (!(partType instanceof UrlPartType)) {
				continue;
			}

			NodeObject target = ((UrlPartType) partType).getTarget();

			if (target instanceof Page) {
				pages.add((Page) target);
			} else if (target instanceof ContentFile) {
				ContentFile file = (ContentFile) target;

				if (file.isImage()) {
					images.add(file);
				} else {
					files.add(file);
				}
			}
		}
	}

	/**
	 * Collect linked pages, files and images for the specified objects.
	 *
	 * The method iterates over tags and object tags and uses
	 * {@link #collectLinkedObjectsFromTag} to gather the actual linked
	 * objects.
	 *
	 * @param objType The class of the source objects
	 * @param objIds The IDs of the source objects
	 * @param pages A set for storing page dependencies
	 * @param files A set for storing file dependencies
	 * @param images A set for storing image dependencies
	 *
	 * @throws NodeException On errors
	 */
	public static <T extends NodeObject> void collectLinkedObjects(Class<T> objType, Collection<Integer> objIds, Set<Page> pages, Set<File> files, Set<ImageFile> images) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (T obj : t.getObjects(objType, objIds)) {
			if (obj instanceof TagContainer) {
				for (Tag tag : ((TagContainer) obj).getTags().values()) {
					collectLinkedObjectsFromTag(tag, pages, files, images);
				}
			}

			if (obj instanceof ObjectTagContainer) {
				for (Tag tag : ((ObjectTagContainer) obj).getObjectTags().values()) {
					collectLinkedObjectsFromTag(tag, pages, files, images);
				}
			}
		}
	}

	/**
	 * Convert a collection of {@link NodeObject NodeObjects} to their REST equivalent.
	 *
	 * @param objects The objects to convert
	 * @param targetClass The target REST model class
	 *
	 * @return A collection containing the respective REST models of the given objects
	 *
	 * @throws NodeException On errors
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ContentNodeItem> List<T> convertToRest(Collection<? extends NodeObject> objects, Class<T> targetClass) throws NodeException {
		List<T> ret = new ArrayList<>();

		if (targetClass.equals(com.gentics.contentnode.rest.model.Page.class)) {
			for (NodeObject page : objects) {
				ret.add((T) ModelBuilder.getPage((Page) page, (Collection<Reference>) null));
			}
		} else if (targetClass.equals(com.gentics.contentnode.rest.model.File.class)) {
			for (NodeObject file : objects) {
				ret.add((T) ModelBuilder.getFile((File) file, Collections.emptyList()));
			}
		} else if (targetClass.equals(com.gentics.contentnode.rest.model.Image.class)) {
			for (NodeObject image : objects) {
				ret.add((T) ModelBuilder.getImage((ImageFile) image, (Collection<Reference>) null));
			}
		}

		return ret;
	}

	/**
	 * Convenience method for {@link #filterByPermission(Collection, ObjectPermission)} with
	 * the {@link ObjectPermission#view view} permission.
	 *
	 * @param objects The objects to be filtered
	 *
	 * @return The number of removed objects
	 */
	public static int filterByPermission(Collection<? extends NodeObject> objects) {
		return filterByPermission(objects, PermHandler.ObjectPermission.view);
	}

	/**
	 * Removes elements from <code>objects</code> where the current user does not
	 * have the specified permission.
	 *
	 * @param objects The objects to be filtered
	 * @param perm The necessary permission
	 *
	 * @return The number of removed objects
	 */
	public static int filterByPermission(Collection<? extends NodeObject> objects, PermHandler.ObjectPermission perm) {
		int withoutPermission = 0;
		Iterator<? extends NodeObject> it = objects.iterator();

		while (it.hasNext()) {
			boolean havePermission;
			NodeObject nodeObject = it.next();

			try {
				havePermission = perm.checkObject(nodeObject);
			} catch (NodeException e) {
				logger.warn("Could not check permission \"" + perm.name() + "\" for " + nodeObject, e);

				havePermission = false;
			}

			if (!havePermission) {
				withoutPermission++;
				it.remove();
			}
		}

		return withoutPermission;
	}

	/**
	 * Removes elements from <code>objects</code> which are either already present
	 * in the target node, or which are localised "between" the source and the
	 * target channel.
	 *
	 * @param srcNodeId The ID of the source channel
	 * @param dstNodeId The ID of the target channel
	 * @param objects The objects to be filtered
	 * @param objType The concrete type of the elements in <code>objects</code>
	 *
	 * @throws NodeException On errors
	 */
	public static <T extends NodeObject> void filterByObstructions(
			Integer srcNodeId,
			Integer dstNodeId,
			Collection<? extends Disinheritable<T>> objects,
			Class<T> objType) throws NodeException {
		if (ObjectTransformer.isEmpty(objects)) {
			return;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		Iterator<? extends Disinheritable<T>> it = objects.iterator();
		Set<Integer> channels = t.getObject(Node.class, srcNodeId).getMasterNodes().stream().map(Node::getId).collect(Collectors.toSet());

		channels.add(srcNodeId);

		while (it.hasNext()) {
			Disinheritable<T> obj = it.next();

			try (ChannelTrx trx = new ChannelTrx(dstNodeId)) {
				T objInDest = t.getObject(objType, obj.getId());

				if (objInDest != null) {
					if (logger.isDebugEnabled()) {
						logger.debug(obj + " already exists in node " + dstNodeId);
					}

					it.remove();

					continue;
				}
			}

			if (!obj.isMaster() && channels.contains(obj.getChannel().getId())) {
				if (logger.isDebugEnabled()) {
					logger.debug(obj + " is localised between channels " + srcNodeId + " and " + dstNodeId);
				}

				it.remove();
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(
				"Remaining objects: ["
				+ String.join(", ", objects.stream().map(o -> ((Disinheritable<?>) o).getId().toString()).collect(Collectors.toList()))
				+ "]");
		}
	}

	/**
	 * Check for permission to synchronize between the given channelId and the masterId
	 * @param masterId master id
	 * @param channelId channel id
	 * @return true or false
	 * @throws NodeException
	 */
	public static boolean checkChannelSyncPerm(int masterId, int channelId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		// get the channel
		Node channel = t.getObject(Node.class, channelId, -1, false);
		if (channel == null) {
			throw new NodeException("Node with ID " + channelId + " does not exist");
		}
		// get the master
		Node master = t.getObject(Node.class, masterId, -1, false);
		if (master == null) {
			throw new NodeException("Node with ID " + masterId + " does not exist");
		}
		// check whether the master is master of the channel
		List<Node> masterNodes = channel.getMasterNodes();
		if (!masterNodes.contains(master)) {
			throw new NodeException("Node with ID " + masterId + " is not master of node " + channelId);
		}

		// add the channel to the list of master nodes
		masterNodes.add(0, channel);
		// remove all master nodes beyond the given master
		masterNodes.removeAll(master.getMasterNodes());
		for (Node node : masterNodes) {
			if (!checkChannelSyncPerm(node)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the current transaction allows channelsync
	 * @param channelId channel id
	 * @return true if channelsync is allowed, false if not
	 * @throws NodeException
	 */
	public static boolean checkChannelSyncPerm(int channelId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		if (permHandler == null) {
			return true;
		} else {
			Node channel = t.getObject(Node.class, channelId);
			if (channel == null) {
				throw new NodeException("Node with ID " + channelId + " does not exist");
			}
			return checkChannelSyncPerm(channel);
		}
	}

	/**
	 * Check whether the current transaction allows channelsync for the given channel
	 * @param channel channel
	 * @return true if channelsync is allowed, false if not
	 * @throws NodeException
	 */
	public static boolean checkChannelSyncPerm(Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		int transactionChannel = 0;
		if (channel.isChannel()) {
			transactionChannel = ObjectTransformer.getInt(channel.getId(), 0);
		}
		try {
			t.setChannelId(transactionChannel);
			return permHandler.checkPermissionBit(Node.TYPE_NODE, channel.getFolder().getId(), PermHandler.PERM_CHANNEL_SYNC);
		} finally {
			t.resetChannel();
		}
	}

	/**
	 * Checks whether the current user can delete at least
	 * one element from the supplied list of object IDs.
	 *
	 * Note that this method will return <code>false</code>
	 * when the object type or the list of IDs is
	 * <code>null</code> or empty.
	 *
	 * @param channelId The channel to check permissions in.
	 * @param objType The object type.
	 * @param ids The list of IDs to be deleted.
	 *
	 * @return <code>true</code> if the user can delete at least
	 *		one object in the given channel from the list of
	 *		IDs, and <code>false</code> otherwise.
	 *
	 * @throws NodeException On errors.
	 */
	public static boolean checkDeletePermSome(int channelId, Class<? extends NodeObject> objType, List<Integer> ids)
			throws NodeException {
		if (objType == null || ids == null) {
			return false;
		}

		try (ChannelTrx trx = new ChannelTrx(channelId)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<? extends NodeObject> objects = t.getObjects(objType, ids);

			for (NodeObject object : objects) {
				if (t.canDelete(object)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Get the folder with given id, check whether the folder exists (if not,
	 * throw a EntityNotFoundException).
	 * @param id id of the folder
	 * @param permissions permissions to check
	 * @return folder
	 * @throws NodeException when loading the folder fails due to underlying
	 *		 error
	 * @throws EntityNotFoundException when the folder was not found
	 * @throws InsufficientPrivilegesException when the user has no permission
	 *		 on the folder
	 */
	public static Folder getFolder(String id, ObjectPermission... permissions)
			throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, id);
		if (folder == null) {
			I18nString message = new CNI18nString("folder.notfound");
			message.setParameter("0", id);
			throw new EntityNotFoundException(message.toString());
		}
		// check additional permission bits
		for (ObjectPermission perm : permissions) {
			if (!perm.checkObject(folder)) {
				I18nString message = new CNI18nString("folder.nopermission");
				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), folder, perm.getPermType());
			}
		}
		return folder;
	}

	/**
	 * Get the template with given id, check whether the template exists (if not,
	 * throw a EntityNotFoundException).
	 * @param id id of the template
	 * @param permissions permissions to check
	 * @return template
	 * @throws NodeException when loading the template fails due to underlying
	 *		 error
	 * @throws EntityNotFoundException when the template was not found
	 * @throws InsufficientPrivilegesException when the user has no permission
	 *		 on the template
	 */
	public static Template getTemplate(String id, ObjectPermission... permissions)
			throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.getObject(Template.class, id);
		if (template == null) {
			I18nString message = new CNI18nString("template.notfound");
			message.setParameter("0", id);
			throw new EntityNotFoundException(message.toString());
		}
		// check additional permission bits
		for (ObjectPermission perm : permissions) {
			if (!perm.checkObject(template)) {
				I18nString message = new CNI18nString("template.nopermission");
				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), template, perm.getPermType());
			}
		}
		return template;
	}

	/**
	 * Get the node with given id, check whether the node exists (if not, throw an EntityNotFoundException).
	 * Also check for optionally given permissions
	 * @param id id of the node
	 * @param permissions permissions to check
	 * @return node
	 * @throws EntityNotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	public static Node getNode(String id, ObjectPermission... permissions) throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.getObject(Node.class, id);
		if (node == null) {
			I18nString message = new CNI18nString("rest.node.notfound");
			message.setParameter("0", id);
			throw new EntityNotFoundException(message.toString());
		}
		// check additional permission bits
		for (ObjectPermission perm : permissions) {
			if (!perm.checkObject(node)) {
				I18nString message = new CNI18nString("rest.node.nopermission");
				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), node, perm.getPermType());
			}
		}
		return node;
	}

	/**
	 * Get the language with given id, global id or code
	 * @param languageId id, global id or code
	 * @param permissions optional permissions to check
	 * @return language
	 * @throws EntityNotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	public static ContentLanguage getLanguage(String languageId, ObjectPermission... permissions)
			throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentLanguage language = t.getObject(ContentLanguage.class, languageId);

		if (language == null) {
			int id = DBUtils.select("SELECT id FROM contentgroup WHERE code = ?", ps -> {
				ps.setString(1, languageId);
			}, rs -> {
				if (rs.next()) {
					return rs.getInt("id");
				} else {
					return 0;
				}
			});
			language = t.getObject(ContentLanguage.class, id);
		}

		if (language == null) {
			throw new EntityNotFoundException(I18NHelper.get("rest.language.notfound", languageId));
		}
		for (ObjectPermission perm : permissions) {
			if (!perm.checkObject(language)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("rest.language.nopermission", languageId), language, perm.getPermType());
			}
		}

		return language;
	}

	/**
	 * Check validity of the given request body. The body is expected to be non-null and if fields are checked, they are also expected to be non-null
	 * @param <T> type of the body
	 * @param requestBody request body
	 * @param fields optional fields to check
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T> void checkBody(T requestBody, Function<T, Pair<String, Object>>...fields) throws NodeException {
		checkBodyWithFunction(requestBody, pair -> pair.getValue() == null ? I18NHelper.get("exception.missing.field", pair.getKey()) : null, fields);
	}

	/**
	 * Check validity of the given request body. The body is expected to be non-null and if fields are checked, the checker function will be used
	 * @param <T> type of the body
	 * @param requestBody request body
	 * @param checker function that gets the fields pair (name and value) and returns a message, if the field is not correct or null, if the field is correct
	 * @param fields optional fields to check
	 * @throws NodeException
	 */
	@SafeVarargs
	public static <T> void checkBodyWithFunction(T requestBody, Function<Pair<String, Object>, String> checker,
			Function<T, Pair<String, Object>>... fields) throws NodeException {
		if (requestBody == null) {
			throw new RestMappedException(I18NHelper.get("exception.missing.body")).setMessageType(Message.Type.WARNING)
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}

		for (Function<T, Pair<String, Object>> fieldFunction : fields) {
			Pair<String, Object> fieldPair = fieldFunction.apply(requestBody);
			String message = checker.apply(fieldPair);
			if (message != null) {
				throw new RestMappedException(message).setMessageType(Message.Type.WARNING)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}
	}

	/**
	 * Check field values to be not null
	 * @param fields fields
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void checkFields(Supplier<Pair<String, Object>>... fields) throws NodeException {
		checkFields(pair -> pair.getValue() == null ? I18NHelper.get("exception.missing.field", pair.getKey()) : null, fields);
	}

	/**
	 * Check all fields with the given checker function
	 * @param checker function that gets the fields pair (name and value) and returns a message, if the field is not correct or null, if the field is correct
	 * @param fields fields
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void checkFields(Function<Pair<String, Object>, String> checker, Supplier<Pair<String, Object>>... fields) throws NodeException {
		for (Supplier<Pair<String, Object>> fieldSupplier : fields) {
			Pair<String, Object> fieldPair = fieldSupplier.supply();
			String message = checker.apply(fieldPair);
			if (message != null) {
				throw new RestMappedException(message).setMessageType(Message.Type.WARNING)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}
		}
	}

	/**
	 * If the perms parameter is "true", return a function that determines, which of the given perms the current user has on an object.
	 * Otherwise return null
	 * @param permsParam optional perms parameter bean
	 * @param perms perms to query
	 * @return function
	 */
	public static <T extends NodeObject> Function<T, Pair<Integer, Set<Permission>>> permFunction(PermsParameterBean permsParam, ObjectPermission... perms) {
		return permFunction(permsParam, obj -> obj, perms);
	}

	/**
	 * If the perms parameter is "true", return a function that determines, which of the given perms the current user has on an object.
	 * Otherwise return null
	 * @param permsParam optional perms parameter bean
	 * @param extractor function that extracts the node object from the given object
	 * @param perms perms to query
	 * @return function
	 */
	public static <T, U extends NodeObject> Function<T, Pair<Integer, Set<Permission>>> permFunction(PermsParameterBean permsParam, Function<T, U> extractor,
			ObjectPermission... perms) {
		if (permsParam != null && permsParam.perms) {
			return obj -> {
				U object = extractor.apply(obj);
				Set<Permission> objectPerms = new HashSet<>();
				for (ObjectPermission perm : perms) {
					if (perm.checkObject(object)) {
						objectPerms.add(perm.getEffectivePermission());
					}
				}
				return Pair.of(object.getId(), objectPerms);
			};
		} else {
			return null;
		}
	}


	/**
	 * Get a comparator for sorting according to the given settings.
	 * @param sorting sorting settings
	 * @param resolver resolver that resolves properties
	 * @param attrs attributes, that can be sorted
	 * @return comparator or null
	 * @throws NodeException
	 */
	public static <T> Comparator<T> comparator(SortParameterBean sorting, BiFunction<T, String, Object> resolver, String... attrs) throws NodeException {
		ResolvableComparator<Resolvable> resolvableComparator = ResolvableComparator.get(sorting, attrs);

		if (resolvableComparator == null) {
			return null;
		}

		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return resolvableComparator.compare(resolvable(o1, resolver), resolvable(o2, resolver));
			}
		};
	}

	/**
	 * Get a comparator for sorting beans according to the given settings.
	 * @param sorting sorting settings
	 * @param attrs attributes, that can be sorted
	 * @return comparator or null
	 * @throws NodeException
	 */
	public static <T> Comparator<T> comparator(SortParameterBean sorting, String... attrs) throws NodeException {
		return comparator(sorting, (o, key) -> {
			try {
				return ClassHelper.invokeGetter(o, key);
			} catch (Exception e) {
				return new NodeException(e);
			}
		}, attrs);
	}

	/**
	 * Get filter for filtering objects according to the given settings
	 * @param filter filter settings
	 * @param resolver resolver that resolves properties
	 * @param attrs attributes, that will be filtered
	 * @return filter or null
	 * @throws NodeException
	 */
	public static <T> Filter<T> filter(FilterParameterBean filter, BiFunction<T, String, Object> resolver, String...attrs) throws NodeException {
		ResolvableFilter<Resolvable> resolvableFilter = ResolvableFilter.get(filter, attrs);

		if (resolvableFilter == null) {
			return null;
		}

		return new Filter<T>() {
			@Override
			public boolean matches(T object) throws NodeException {
				return resolvableFilter.matches(resolvable(object, resolver));
			}
		};
	}

	/**
	 * Get filter for filtering beans according to the given settings
	 * @param filter filter settings
	 * @param attrs attributes, that will be filtered
	 * @return filter or null
	 * @throws NodeException
	 */
	public static <T> Filter<T> filter(FilterParameterBean filter, String...attrs) throws NodeException {
		return filter(filter, (o, key) -> {
			try {
				return ClassHelper.invokeGetter(o, key);
			} catch (Exception e) {
				return new NodeException(e);
			}
		}, attrs);
	}

	/**
	 * Create a resolvable wrapper for the object
	 * @param object wrapped object
	 * @param resolver resolving implementation
	 * @return resolvable wrapper
	 */
	public static <T> Resolvable resolvable(T object, BiFunction<T, String, Object> resolver) {
		return new Resolvable() {
			@Override
			public Object getProperty(String key) {
				return get(key);
			}
			@Override
			public Object get(String key) {
				try {
					return resolver.apply(object, key);
				} catch (NodeException e) {
					return null;
				}
			}
			@Override
			public boolean canResolve() {
				return true;
			}
		};
	}


	/**
	 * Merge nested settings
	 * @param map original map (will be modified)
	 * @param other other settings (will be merged into map)
	 * @return modified map
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> merge(Map<String, Object> map, Map<String, Object> other) {
		for (Map.Entry<String, Object> e : other.entrySet()) {
			if (e.getValue() == null) {
				map.put(e.getKey(), null);
			} else {
				map.merge(e.getKey(), e.getValue(), (oldVal, newVal) -> {
					if (oldVal instanceof Map && newVal instanceof Map) {
						return merge((Map<String, Object>) oldVal, (Map<String, Object>) newVal);
					}
					return newVal;
				});
			}
		}
		return map;
	}

	/**
	 * Compare two lists with given comparator
	 * @param <T> item type
	 * @param firstList first list
	 * @param secondList second list
	 * @param comparator comparator
	 * @return true iff the lists have equal number of items and all items at the same list position are equal to each other according to the given comparator
	 * @throws NodeException
	 */
	public static <T> boolean equals(Collection<T> firstList, Collection<T> secondList, BiFunction<T, T, Boolean> comparator) throws NodeException {
		if (firstList == null && secondList == null) {
			return true;
		}
		if ((firstList == null && secondList != null) || (firstList != null && secondList == null)) {
			return false;
		}

		if (firstList.size() != secondList.size()) {
			return false;
		}

		Iterator<T> firstIt = firstList.iterator();
		Iterator<T> secondIt = secondList.iterator();

		while (firstIt.hasNext() && secondIt.hasNext()) {
			T first = firstIt.next();
			T second = secondIt.next();
			if (!comparator.apply(first, second)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Determine the template usage ids for the given list of ids and type.
	 *
	 * @param objId List of element Ids
	 * @param objType Element type id
	 * @param nodeId Id of node
	 * @return List of template ids
	 */
	public static Set<Integer> getTemplateUsageIds(List<Integer> objId, int objType, Integer nodeId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			String idPlaceholders = StringUtils.repeat("?", ",", objId.size());
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

			case Form.TYPE_FORM:
				typeIds = "42";
				dsTypes = "10050";
				break;

			default:
				throw new NodeException("Error while getting usage info: unkown type " + objType + " given");
			}

			StringBuffer sql = new StringBuffer();
			List<Integer> params = new Vector<Integer>();

			if (objType == Page.TYPE_PAGE) {
				sql.append("SELECT template.id, template.channelset_id, template.channel_id FROM template ")
						.append("INNER JOIN templatetag t1 ON template.id = t1.template_id ")
						.append("INNER JOIN value v1 ON t1.id = v1.templatetag_id AND v1.value_text = 'p' AND v1.info IN (").append(idPlaceholders)
						.append(")");

				params.addAll(objId);
			} else {
				// next: objects in templatetags of template
				sql.append("select distinct template.id, template.channelset_id, template.channel_id from template left join templatetag on ").append("template.id = templatetag.template_id left join value on templatetag.id = value.templatetag_id ").append("left join part on part.id = value.part_id where ").append("templatetag.enabled = 1 and part.type_id in (").append(typeIds).append(") and value.value_ref in (").append(idPlaceholders).append(
						")");

				// next: objects in objecttags
				sql.append(" union distinct ").append("select distinct template.id, template.channelset_id, template.channel_id from template left join objtag on obj_type = 10006 and obj_id = template.id ").append("left join value on value.objtag_id = objtag.id left join part on ").append("value.part_id = part.id ").append("where objtag.enabled = 1 and part.type_id in (").append(typeIds).append(") and value_ref in (").append(idPlaceholders).append(
						")");

				// next: objects directly selected in overviews in templatetags
				sql.append(" union distinct ").append("select distinct template.id, template.channelset_id, template.channel_id from template left join templatetag on ").append("template.id = templatetag.template_id ").append("left join ds on templatetag.id = ds.templatetag_id right join ds_obj on templatetag.id = ds_obj.templatetag_id ").append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(
						")");

				// next: objects directly selected for overviews in objecttags
				sql.append(" union distinct ").append("select distinct template.id, template.channelset_id, template.channel_id from template left join objtag on obj_type = 10006 and obj_id = template.id ").append("left join ds on objtag.id = ds.objtag_id right join ds_obj on objtag.id = ds_obj.objtag_id ").append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(
						")");

				// fill in the ids 4 times
				for (int i = 0; i < 4; ++i) {
					params.addAll(objId);
				}
			}

			pst = t.prepareStatement(sql.toString());
			// fill in the params
			int pCounter = 1;

			for (Object o : params) {
				pst.setObject(pCounter++, o);
			}
			res = pst.executeQuery();

			Set<Integer> usingTemplateIds = filterUsageResult(res, false, nodeId);
			return usingTemplateIds;
		} catch (SQLException e) {
			throw new NodeException("Error while getting usageinfo for templates", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Get the templates using one of the given objects.
	 *
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param objType type of the objects
	 * @param objId list of object ids
	 * @param nodeId id of the node
	 * @param returnTemplates true if the templates shall be returned, false for only returning the numbers
	 * @return list of templates
	 */
	public static TemplateUsageListResponse getTemplateUsage(Integer skipCount, Integer maxItems, String sortBy, String sortOrder, int objType, List<Integer> objId, Integer nodeId,
			boolean returnTemplates) throws NodeException {
		if (ObjectTransformer.isEmpty(objId)) {
			return new TemplateUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using other objects"), Collections.emptyList(), 0, 0);
		}
		Set<Integer> usingTemplateIds = getTemplateUsageIds(objId, objType, nodeId);
		return fillTemplateUsageList(skipCount, maxItems, sortBy, sortOrder, objType, usingTemplateIds, returnTemplates);
	}

	public static TemplateUsageListResponse fillTemplateUsageList(Integer skipCount, Integer maxItems, String sortBy, String sortOrder, int objType, Set<Integer> usingTemplateIds,
			boolean returnTemplates) throws NodeException {
		// get the templates
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Template> usingTemplates = new Vector<Template>(t.getObjects(Template.class, usingTemplateIds));
		int withoutPermission = 0;
		int total = usingTemplates.size();
		List<com.gentics.contentnode.rest.model.Template> restTemplates = null;

		if (returnTemplates) {
			// optionally sort the list
			if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)) {
				Collections.sort(usingTemplates,
						new CNTemplateComparator(TemplateSortAttribute.valueOf(sortBy), SortOrder.valueOf(sortOrder)));
			}

			// do paging
			reduceList(usingTemplates, skipCount, maxItems);

			// prepare list of REST objects
			restTemplates = new Vector<com.gentics.contentnode.rest.model.Template>(usingTemplates.size());
		}

		// filter out (and count) the templates without permission
		for (Template template : usingTemplates) {
			if (PermHandler.ObjectPermission.view.checkObject(template)) {
				if (returnTemplates) {
					restTemplates.add(ModelBuilder.getTemplate(template, null));
				}
			} else {
				withoutPermission++;
			}
		}

		return new TemplateUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using other objects"), restTemplates, total,
				withoutPermission);
	}

	/**
	 * Get the pages using one of the given objects (usage is defined by given sql statement)
	 * @param skipCount
	 *            number of items to be skipped, set to 0 for skipping no items
	 * @param maxItems
	 *            maximum number of items to be returned, set to -1 for
	 *        returning all items
	 * @param sortBy
	 *            (optional) attribute to sort by. defaults to name
	 * @param sortOrder
	 *            (optional) result sort order - may be "asc" for ascending or
	 *            "desc" for descending other strings will be ignored. defaults
	 *            to "asc".
	 * @param objType type of the objects
	 * @param objId list of object ids
	 * @param kind kind of the page usage
	 * @param nodeId id of the node
	 * @param returnPages true if the pages shall be returned, false for only returning the numbers
	 * @param pageModel page model parameters
	 * @return list of pages using the given pages
	 */
	public static PageUsageListResponse getPageUsage(Integer skipCount, Integer maxItems, String sortBy, String sortOrder, int objType, List<Integer> objId,
			PageUsage kind, Integer nodeId, boolean returnPages, PageModelParameterBean pageModel) throws NodeException {
		if (ObjectTransformer.isEmpty(objId)) {
			return new PageUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched pages using other pages"), Collections.emptyList(), 0, 0);
		}
		Set<Integer> usingPageIds = getPageUsageIds(objId, objType, kind, nodeId);
		// get the pages
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Page> usingPages = new Vector<Page>(t.getObjects(Page.class, usingPageIds));

		int withoutPermission = 0;
		int total = usingPages.size();
		List<com.gentics.contentnode.rest.model.Page> restPages = null;

		if (returnPages) {
			// optionally sort the list
			if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)) {
				Collections.sort(usingPages, new PageComparator(sortBy, sortOrder));
			}

			// do paging
			reduceList(usingPages, skipCount, maxItems);

			// prepare list of REST objects
			restPages = new Vector<com.gentics.contentnode.rest.model.Page>(usingPages.size());
		}

		Collection<Reference> fillRefs = new Vector<Reference>();

		if (pageModel != null) {
			if (pageModel.template) {
				fillRefs.add(Reference.TEMPLATE);
			}
			if (pageModel.folder) {
				fillRefs.add(Reference.FOLDER);
			}
			if (pageModel.languageVariants) {
				fillRefs.add(Reference.LANGUAGEVARIANTS);
			}
			if (pageModel.contentTags) {
				fillRefs.add(Reference.CONTENT_TAGS);
			}
			if (pageModel.objectTags) {
				fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
			}
			if (pageModel.translationStatus) {
				fillRefs.add(Reference.TRANSLATIONSTATUS);
			}
		}
		// filter out (and count) the pages without permission
		for (Page page : usingPages) {
			if (PermHandler.ObjectPermission.view.checkObject(page)) {
				if (returnPages) {
					restPages.add(ModelBuilder.getPage(page, fillRefs));
				}
			} else {
				withoutPermission++;
			}
		}

		return new PageUsageListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched pages using other objects"), restPages, total, withoutPermission);
	}

	/**
	 * Determine the page usage ids for the given list of pages and type.
	 *
	 * @param objId
	 * @param objType
	 * @param kind
	 * @param nodeId
	 * @return List of pages that use one or more of the given pages
	 * @throws NodeException
	 */
	public static Set<Integer> getPageUsageIds(List<Integer> objId, int objType, PageUsage kind, Integer nodeId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			StringBuffer sql = new StringBuffer();
			List<Integer> params = new Vector<Integer>();
			String idPlaceholders = StringUtils.repeat("?", ",", objId.size());
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

			case Form.TYPE_FORM:
				typeIds = "42";
				dsTypes = "10050";
				break;

			default:
				throw new NodeException("Error while getting usage info: unkown type " + objType + " given");
			}

			switch (kind) {
			case TAG:
				sql.append("SELECT page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node ")
					.append("FROM page ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("INNER JOIN contenttag c1 ON page.content_id = c1.content_id ")
					.append("INNER JOIN value v1 ON c1.id = v1.contenttag_id ")
					.append("AND v1.value_text = 'p' AND v1.info IN (")
					.append(idPlaceholders)
					.append(")")
					.append(" WHERE page.deleted = 0");
				params.addAll(objId);
				break;

			case VARIANT:
				sql.append("SELECT p1.id, p1.channelset_id, p1.channel_id, p1.mc_exclude, page_disinherit.channel_id disinherited_node ")
					.append("FROM page ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("LEFT JOIN page p1 ON page.content_id = p1.content_id ")
					.append("WHERE p1.folder_id IS NOT NULL ")
					.append("AND page.id = (").append(idPlaceholders).append(") AND p1.id NOT IN (").append(idPlaceholders).append(") ")
					.append("AND page.deleted = 0");
				// fill in the ids 2 times
				for (int i = 0; i < 2; ++i) {
					params.addAll(objId);
				}
				break;

			case GENERAL:
			default:
				// next: objects in contenttags
				sql.append("select distinct page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node from page left join contenttag on ")
					.append("page.content_id = contenttag.content_id ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("left join value on contenttag.id = value.contenttag_id left join part ")
					.append("on value.part_id = part.id ")
					.append("where contenttag.enabled = 1 and part.type_id in (")
					.append(typeIds).append(") and ").append("value.value_ref in (").append(idPlaceholders).append(") ")
					.append("AND page.deleted = 0");

				// next: objects in objecttags
				sql.append(" union distinct ")
					.append("select distinct page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node from page left join objtag on obj_type = 10007 ")
					.append("and obj_id = page.id ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("left join value on value.objtag_id = objtag.id left join part on ")
					.append("value.part_id = part.id ")
					.append("where objtag.enabled = 1 and part.type_id in (").append(typeIds).append(") and value_ref in (").append(idPlaceholders).append(") ")
					.append("AND page.deleted = 0");

				// next: objects directly selected for overviews in contenttags
				sql.append(" union distinct ")
					.append("select distinct page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node from page left join contenttag on page.content_id = contenttag.content_id ")
					.append("left join ds on contenttag.id = ds.contenttag_id right join ds_obj on contenttag.id = ds_obj.contenttag_id ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(") ")
					.append("AND page.deleted = 0");

				// next: objects directly selected for overviews in templatetags
				sql.append(" union distinct ")
					.append("select distinct page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node from page left join templatetag on ")
					.append("page.template_id = templatetag.template_id ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("left join ds on templatetag.id = ds.templatetag_id right join ds_obj on templatetag.id = ds_obj.templatetag_id ")
					.append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(") ")
					.append("AND page.deleted = 0");

				// next: objects directly selected for overviews in objecttags
				sql.append(" union distinct ")
					.append("select distinct page.id, page.channelset_id, page.channel_id, page.mc_exclude, page_disinherit.channel_id disinherited_node from page left join objtag on obj_type = 10007 ")
					.append("and obj_id = page.id ").append("left join ds on objtag.id = ds.objtag_id right join ds_obj on objtag.id = ds_obj.objtag_id ")
					.append("LEFT JOIN page_disinherit on page.id=page_disinherit.page_id ")
					.append("where ds_obj.o_id in (").append(idPlaceholders).append(") and ds.o_type in (").append(dsTypes).append(") ")
					.append("AND page.deleted = 0");

				// fill in the ids 5 times
				for (int i = 0; i < 5; ++i) {
					params.addAll(objId);
				}

				break;
			}

			pst = t.prepareStatement(sql.toString());
			// fill in the params
			int pCounter = 1;

			for (Object o : params) {
				pst.setObject(pCounter++, o);
			}
			res = pst.executeQuery();

			Set<Integer> usingPageIds = filterUsageResult(res, true, nodeId);
			return usingPageIds;
		} catch (SQLException e) {
			throw new NodeException("Error while getting usageinfo for pages", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
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
	public static Set<Integer> filterUsageResult(ResultSet result, boolean containsDisinheritInfo, Integer nodeId)
			throws NodeException, SQLException {
		Transaction t = TransactionManager.getCurrentTransaction();
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
	 * Get the list of matching systemusers for the given pattern
	 * @param pattern pattern
	 * @param userIds list of allowed user IDs
	 * @return list of matching systemusers
	 */
	public static List<SystemUser> getMatchingSystemUsers(String pattern, List<Integer> userIds) throws NodeException {
		// when no pattern is given, we return null
		if (StringUtils.isBlank(pattern) && (userIds == null || userIds.isEmpty())) {
			return null;
		}

		// initialize the user resource
		Transaction t = TransactionManager.getCurrentTransaction();
		UserResource userResource = new UserResourceImpl();

		// split the pattern by spaces and correct the list (prepend and append % if missing)
		List<String> namesList = (pattern != null ? Arrays.asList(pattern.split(" ")) : Collections.singletonList("%")).stream()
			.map(name -> StringUtils.appendIfMissing(StringUtils.prependIfMissing(name, "%"), "%"))
			.collect(Collectors.toList());
		// search for firstnames and lastnames
		List<User> users = new ArrayList<>();
		UserListResponse response = userResource.list(0, -1, userIds, null, namesList, null, null, null, null, null, null, false);

		users.addAll(response.getUsers());
		response = userResource.list(0, -1, userIds, null, null, namesList, null, null, null, null, null, false);
		users.addAll(response.getUsers());

		// transform all users to SystemUser instances
		List<SystemUser> matchingUsers = new ArrayList<>(users.size());

		for (User user : users) {
			matchingUsers.add(t.getObject(SystemUser.class, user.getId()));
		}

		return matchingUsers;
	}

	/**
	 * Execute the given handler for chunks of the given Iterable (each chunk can contain up to bufferSize items).
	 * If the handler throws a NodeException, execution is stopped and the NodeException will be thrown
	 * @param <T> type of the elements in the Iterable
	 * @param iterable iterable
	 * @param bufferSize buffer size
	 * @param handler handler
	 * @throws NodeException
	 */
	public static <T> void doBuffered(Iterable<T> iterable, int bufferSize, Consumer<List<T>> handler)
			throws NodeException {
		try {
			Flowable.fromIterable(iterable).buffer(bufferSize).blockingForEach(handler::accept);
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Execute the given handler for chunks of the given Iterable (each chunk can contain up to bufferSize items)
	 * as long as the handler returns true.
	 * If the handler throws a NodeException, execution is stopped and the NodeException will be thrown
	 * @param <T> type of the elements in the Iterable
	 * @param iterable iterable
	 * @param bufferSize buffer size
	 * @param handler handler
	 * @throws NodeException
	 */
	public static <T> void doBuffered(Iterable<T> iterable, int bufferSize, Function<List<T>, Boolean> handler)
			throws NodeException {
		try {
			Flowable.fromIterable(iterable).buffer(bufferSize).takeWhile(idList -> {
				return handler.apply(idList);
			}).blockingSubscribe();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Get the i18n message for duplicate URL
	 * @param url conflicting URL
	 * @param conflictingObject conflicting object
	 * @return i18n message
	 * @throws NodeException
	 */
	public static I18nString getUrlDuplicationMessage(String url, NodeObject conflictingObject) throws NodeException {
		String i18nKey = "object.url.duplicate";
		String typeKey = "object";
		// when the conflicting page is not the current version, it has to be the published version
		if (!conflictingObject.getObjectInfo().isCurrentVersion()) {
			i18nKey = "object.url.duplicate.published";
		}
		switch (conflictingObject.getTType()) {
		case Page.TYPE_PAGE:
			typeKey = "page";
			break;
		case ImageFile.TYPE_IMAGE:
			typeKey = "image";
			break;
		case File.TYPE_FILE:
			typeKey = "file";
			break;
		}
		CNI18nString message = new CNI18nString(i18nKey);
		message.addParameter(I18NHelper.get(typeKey));
		message.addParameter(url);
		message.addParameter(I18NHelper.getPath(conflictingObject));
		message.addParameter(Integer.toString(conflictingObject.getId()));
		return message;
	}

	/**
	 * Create a new random name
	 * @return random name
	 */
	public static final String getRandomNameOfLength(int length) {
		StringBuilder name = new StringBuilder();
		Random random = new Random();

		for (int i = 0; i < length; i++) {
			name.append((char)('a' + random.nextInt(26)));
		}

		return name.toString();
	}

	/**
	 * Unwrap and re-throw any NodeException wrapped into a RuntimeException and thrown from the given operator
	 * @param operator operator
	 * @throws NodeException
	 */
	public static void unwrap(Operator operator) throws NodeException {
		try {
			operator.operate();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Unwrap and re-throw any NodeException wrapped into a RuntimeException and throws from the given supplier
	 * @param supplier supplier
	 * @return supplied value
	 * @throws NodeException
	 */
	public static <R> R unwrap(Supplier<R> supplier) throws NodeException {
		try {
			return supplier.supply();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Wrap the given operator into a try catch and rethrow any thrown NodeException wrapped into a RuntimeException
	 * @param operator operator
	 */
	public static void wrap(Operator operator) {
		try {
			operator.operate();
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wrap the given supplier into a try catch and rethrow any thrown NodeException wrapped into a RuntimeException
	 * @param supplier supplier
	 * @return supplied value
	 */
	public static <R> R wrap(Supplier<R> supplier) {
		try {
			return supplier.supply();
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wrap the given consumer into a try catch and rethrow any thrown NodeException wrapped into a RuntimeException
	 * @param throwingConsumer consumer that throws {@link NodeException}
	 */
	public static <R> java.util.function.Consumer<R> wrap(
			Consumer<R> throwingConsumer) {
		return value -> {
			try {
				throwingConsumer.accept(value);
			} catch (NodeException exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	/**
	 * Make a new object mapper, with LF newlines, non-nulls serialization, output indentation.
	 * 
	 * @return mapper instance
	 */
	public static ObjectMapper newObjectMapper() {
		return newObjectMapper(Include.NON_NULL);
	}

	/**
	 * Make a new object mapper, with LF newlines, given serialization inclusion and output indentation.
	 * @param serializationInclusion serialization inclusion
	 * @return mapper instance
	 */
	public static ObjectMapper newObjectMapper(Include serializationInclusion) {
		return new ObjectMapper()
				.setDefaultPrettyPrinter(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter().withLinefeed("\n")))
				.setSerializationInclusion(serializationInclusion)
				.enable(SerializationFeature.INDENT_OUTPUT);
	}

	/**
	 * Assert that the given contentrepository is a Mesh CR. If not, throw a {@link RestMappedException} with an appropriate message
	 * @param cr contentrepository to check
	 * @throws NodeException
	 */
	public static void assertMeshCr(ContentRepository cr) throws NodeException {
		if (cr.getCrType() != ContentRepositoryModel.Type.mesh) {
			String translatedType = I18NHelper.get("crtype." + cr.getCrType().name() + ".short");
			throw new RestMappedException(I18NHelper.get("contentrepository.invalidtype", cr.getName(), translatedType)).setResponseCode(ResponseCode.INVALIDDATA)
					.setStatus(Status.CONFLICT);
		}
	}

	/**
	 * Transform the given list of loaded NodeObjects into ContentNodeItems. The order of the elements is given by the list of ids, by which the elements were loaded.
	 * 
	 * @param <O> type of output items
	 * @param <I> type of input items
	 * @param ids list of originally requested IDs
	 * @param loaded loaded objects. The items might have different IDs in cases of multichannelling
	 * @param idFunction function which gets all (possible) original IDs which might have resulted in loaded the specific entity
	 * @param transformer transformer function to transform the input entity into the output entity
	 * @param filter optional filter function
	 * @param fillWithNulls true to fill the output list with nulls, if requested IDs could not be loaded at all (or were filtered by the filter function).
	 * @return output list
	 * @throws NodeException
	 */
	public static <O extends ContentNodeItem, I extends NodeObject> List<O> getItemList(List<Integer> ids,
			Collection<I> loaded, Function<I, Set<Integer>> idFunction, Function<I, O> transformer,
			Function<I, Boolean> filter, boolean fillWithNulls) throws NodeException {
		if (filter == null) {
			filter = entity -> true;
		}

		Map<Integer, I> lookupMap = new HashMap<>();
		for (I entity : loaded) {
			Set<Integer> entityIds = idFunction.apply(entity);
			for (Integer id : entityIds) {
				lookupMap.put(id, entity);
			}
		}

		List<O> output = new ArrayList<>();

		for (Integer id : ids) {
			I entity = lookupMap.get(id);

			if (entity != null && !filter.apply(entity)) {
				entity = null;
			}

			if (entity != null) {
				output.add(transformer.apply(entity));
			} else if (fillWithNulls) {
				output.add(null);
			}
		}

		return output;
	}

	/**
	 * Execution the given function with the input and return the result. If the function throws an exception, return null.
	 * @param <I> type of the input
	 * @param <O> type of the output
	 * @param function function to execute
	 * @param input input data
	 * @return output
	 */
	public static <I, O> O execOrNull(Function<I, O> function, I input) {
		try {
			return function.apply(input);
		} catch (NodeException e) {
			return null;
		}
	}

	/**
	 * Check, whether the string is URL and contains a protocol prefix.
	 * At the moment only HTTP and HTTPS are supported.
	 * @param host string to test
	 * @return true, if `http(s)://` prefix is present in the hostname value.
	 */
	public static boolean isUrlWithProtocol(String host) {
		return host != null && host.matches("^(http|https)://(.+)");
	}

	/**
	 * Set the hostname and secure connection flag, based on an input string, that can possibly contain and URL, prefixed with a protocol.
	 *
	 * @param input
	 * @param secureSetter
	 * @param hostSetter
	 * @throws NodeException
	 */
	public static void setHostAndProtocol(String input, ThrowingConsumer<Boolean, ReadOnlyException> secureSetter, ThrowingConsumer<String, ReadOnlyException> hostSetter) throws ReadOnlyException {
		if (StringUtils.isNotBlank(input)) {
			input = input.trim();
			if (isUrlWithProtocol(input)) {
				secureSetter.accept(input.startsWith("https://"));
				hostSetter.accept(input.substring(input.indexOf("://") + 3));
			} else {
				hostSetter.accept(input);
			}
		}
	}

	/**
	 * When the instant publishing result is not null and contains a reason message, add the message to the response
	 * @param instantPublishingResult optional instant publishing result
	 * @param response response
	 */
	public static void addMessage(Result instantPublishingResult, GenericResponse response) {
		if (instantPublishingResult != null && !StringUtils.isEmpty(instantPublishingResult.reason())) {
			Message message = new Message().setMessage(instantPublishingResult.reason());
			switch(instantPublishingResult.status()) {
			case failed:
				message.setType(Type.WARNING);
				break;
			case skipped:
				message.setType(Type.INFO);
				break;
			case success:
				message.setType(Type.SUCCESS);
				break;
			default:
				message.setType(Type.INFO);
				break;
			}
			response.addMessage(message);
		}
	}
}
