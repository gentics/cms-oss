package com.gentics.contentnode.rest.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.ChannelInheritanceException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.response.DisinheritResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.MultiDisinheritResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.log.NodeLogger;

/**
 * Utility class that gets and sets disinherit info for REST calls
 */
public class DisinheritRestUtil {
	/**
	 * Map of i18n keys for "notfound" messages for specific t-types
	 */
	protected final static Map<Integer, String> notFoundKeys = new HashMap<>();

	/**
	 * Map of i18n keys for "nopermission" message for speficic t-types
	 */
	protected final static Map<Integer, String> noPermissionKeys = new HashMap<>();

	static {
		notFoundKeys.put(Page.TYPE_PAGE, "page.notfound");
		notFoundKeys.put(Folder.TYPE_FOLDER, "folder.notfound");
		notFoundKeys.put(File.TYPE_FILE, "file.notfound");
		notFoundKeys.put(ImageFile.TYPE_IMAGE, "image.notfound");

		noPermissionKeys.put(Page.TYPE_PAGE, "page.nopermission");
		noPermissionKeys.put(Folder.TYPE_FOLDER, "folder.nopermission");
		noPermissionKeys.put(File.TYPE_FILE, "file.nopermission");
		noPermissionKeys.put(ImageFile.TYPE_IMAGE, "image.nopermission");
	}

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(DisinheritRestUtil.class);

	/**
	 * Get the disinherit information for the given object
	 *
	 * @param clazz
	 *            object class
	 * @param id
	 *            object id
	 * @return disinherit information
	 * @throws NodeException
	 */
	public static <U, T extends Disinheritable<U>> DisinheritResponse get(Class<T> clazz, String id) throws NodeException {
		try {
			T obj = getObject(clazz, id, ObjectPermission.view);
			NodeObject parent = obj.getParentObject();
			ChannelTreeSegment parentSegment = new ChannelTreeSegment((Disinheritable<?>) parent, true);

			Set<Node> disinheritedChannels = obj.getDisinheritedChannels();
			List<Integer> disinheritedNodeIds = new ArrayList<>();
			for (Node node : disinheritedChannels) {
				if (ObjectPermission.view.checkObject(obj, node)) {
					disinheritedNodeIds.add(node.getId());
				}
			}

			Node masterChannel = obj.getMaster().getChannel();
			if (masterChannel == null) {
				masterChannel = obj.getOwningNode();
			}

			Collection<Node> inheritableNodes = masterChannel.getAllChannels();
			List<Integer> inheritableNodeIds = new ArrayList<>();
			for (Node node : inheritableNodes) {
				// check whether the folder of the object is visible in the node
				if (parentSegment.contains(node) && ObjectPermission.view.checkObject(obj, node)) {
					inheritableNodeIds.add(node.getId());
				}
			}

			return new DisinheritResponse(
				new ResponseInfo(ResponseCode.OK, "Successfully fetched disinherit status for " + obj),
				obj.isExcluded(),
				obj.isDisinheritDefault(),
				disinheritedNodeIds,
				inheritableNodeIds);
		} catch (EntityNotFoundException e) {
			return new DisinheritResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new DisinheritResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		}
	}

	/**
	 * Get the disinherit and exclusion information for multiple objects
	 * @param clazz object class
	 * @param ids list of object ids
	 * @return response containing the cumulated info
	 * @throws NodeException
	 */
	public static <U, T extends Disinheritable<U>> MultiDisinheritResponse get(Class<T> clazz, List<String> ids) throws NodeException {
		Boolean exclude = null;
		List<Integer> allInheritable = null;
		List<Integer> allDisinherited = null;
		Set<Integer> partialDisinherited = new HashSet<>();
		boolean first = true;

		for (String id : ids) {
			DisinheritResponse singleResponse = get(clazz, id);
			if (singleResponse.getResponseInfo().getResponseCode() != ResponseCode.OK) {
				return new MultiDisinheritResponse(singleResponse.getMessages(), singleResponse.getResponseInfo());
			} else {
				if (first) {
					allInheritable = new ArrayList<>(singleResponse.getInheritable());
					allDisinherited = new ArrayList<>(singleResponse.getDisinherit());
					exclude = singleResponse.isExclude();
					first = false;
				} else {
					allInheritable.retainAll(singleResponse.getInheritable());
					allDisinherited.retainAll(singleResponse.getDisinherit());
					if (exclude != null && exclude != singleResponse.isExclude()) {
						exclude = null;
					}
				}
				partialDisinherited.addAll(singleResponse.getDisinherit());
			}
		}
		partialDisinherited.removeAll(allDisinherited);

		return new MultiDisinheritResponse(new ResponseInfo(ResponseCode.OK, "Successfully fetched disinherit status for " + ids.size() + " objects"), exclude,
				allDisinherited, new ArrayList<Integer>(partialDisinherited), allInheritable);
	}

	/**
	 * Set the disinherit information for the given object
	 *
	 * @param clazz
	 *            object class
	 * @param id
	 *            object id
	 * @param request
	 *            request object
	 * @param commit true if the transaction shall be committed (or rolled back) within this method, false if not
	 * @return generic response
	 * @throws NodeException
	 */
	public static <U, T extends Disinheritable<U>> GenericResponse set(Class<T> clazz, String id, DisinheritRequest request, boolean commit) throws NodeException {
		try (AutoCommit auto = commit ? new AutoCommit() : null) {
			Transaction t = TransactionManager.getCurrentTransaction();
			if (Boolean.TRUE.equals(request.getExclude())) {
				if (request.getDisinherit() != null) {
					request.getDisinherit().clear();
				}
				if (request.getReinherit() != null) {
					request.getReinherit().clear();
				}
			}

			T obj = getObject(clazz, id, ObjectPermission.view);

			assertInheritPermission(obj);

			Set<Folder> allSubFolders = null;

			if (obj instanceof Folder) {
				Folder f = (Folder)obj;
				allSubFolders = new HashSet<>();
				List<Folder> toAdd = new ArrayList<>(f.getChildFolders());

				while (!toAdd.isEmpty()) {
					Folder c = toAdd.remove(0);
					allSubFolders.add(c);
					toAdd.addAll(c.getChildFolders());
				}
			}

			setDisinheritDefault(
				obj,
				ObjectTransformer.getBoolean(request.getDisinheritDefault(), false),
				ObjectTransformer.getBoolean(request.getRecursive(), false));

			// check permission to exclude
			if (Boolean.TRUE.equals(request.getExclude()) && !obj.isExcluded()) {
				if (!ObjectPermission.inheritance.checkObject(obj, obj.getChannel()) || !ObjectPermission.delete.checkObject(obj)) {
					throw new InsufficientPrivilegesException("Insufficient privileges to exclude " + obj, "exclude.nopermission", I18NHelper.getName(obj), obj,
							PermType.updateinheritance);
				}

				// when a folder is excluded, everything in the folder is excluded, so the user requires permission to do so
				if (!ObjectTransformer.isEmpty(allSubFolders)) {
					for (Folder folder : allSubFolders) {
						if (!ObjectPermission.delete.checkObject(folder)) {
							throw new InsufficientPrivilegesException("Insufficient privileges to exclude " + folder, "exclude.nopermission",
									I18NHelper.getPath(folder), folder, PermType.updateinheritance);
						}
					}
				}
			}

			if (Boolean.TRUE.equals(request.getExclude())) {
				obj.changeMultichannellingRestrictions(true, new HashSet<Node>(), ObjectTransformer.getBoolean(request.getRecursive(), false));
			} else {
				List<Node> disinheritedNodes = null;
				List<Node> reinheritedNodes = null;
				try (ChannelTrx trx = new ChannelTrx()) {
					disinheritedNodes = t.getObjects(Node.class, request.getDisinherit());
					reinheritedNodes = t.getObjects(Node.class, request.getReinherit());
				}
				// check permissions on nodes
				for (Node node : disinheritedNodes) {
					if (!ObjectPermission.inheritance.checkObject(obj, node) || !ObjectPermission.delete.checkObject(obj, node)) {
						throw new InsufficientPrivilegesException("Insufficient privileges to disinherit " + obj + " in " + node, "disinherit.nopermission",
								Arrays.asList(I18NHelper.getName(obj), I18NHelper.getName(node)), obj, PermType.updateinheritance);
					}
					if (!ObjectTransformer.isEmpty(allSubFolders)) {
						for (Folder folder : allSubFolders) {
							if (!ObjectPermission.delete.checkObject(folder, node)) {
								throw new InsufficientPrivilegesException("Insufficient privileges to disinherit " + folder + " in " + node, "disinherit.nopermission",
										Arrays.asList(I18NHelper.getPath(folder), I18NHelper.getName(node)), obj, PermType.updateinheritance);
							}
						}
					}
				}
				for (Node node : reinheritedNodes) {
					if (!ObjectPermission.inheritance.checkObject(obj, node) || !ObjectPermission.delete.checkObject(obj, node)) {
						throw new InsufficientPrivilegesException("Insufficient privileges to disinherit " + obj + " in " + node, "disinherit.nopermission",
								Arrays.asList(I18NHelper.getName(obj), I18NHelper.getName(node)), obj, PermType.updateinheritance);
					}

					if (!ObjectTransformer.isEmpty(allSubFolders)) {
						for (Folder folder : allSubFolders) {
							if (!ObjectPermission.delete.checkObject(folder, node)) {
								throw new InsufficientPrivilegesException("Insufficient privileges to disinherit " + folder + " in " + node, "disinherit.nopermission",
										Arrays.asList(I18NHelper.getPath(folder), I18NHelper.getName(node)), obj, PermType.updateinheritance);
							}
						}
					}
				}

				Set<Node> currentlyDisinherited = obj.getDisinheritedChannels();
				currentlyDisinherited.addAll(disinheritedNodes);
				currentlyDisinherited.removeAll(reinheritedNodes);

				if (Boolean.FALSE.equals(request.getExclude())) {
					obj.changeMultichannellingRestrictions(false, currentlyDisinherited, ObjectTransformer.getBoolean(request.getRecursive(), false));
				} else {
					// TODO: Isn't this dead code? PK
					obj.changeMultichannellingRestrictions(obj.isExcluded(), currentlyDisinherited, ObjectTransformer.getBoolean(request.getRecursive(), false));
				}
			}

			if (auto != null) {
				auto.success();
			}
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully changed disinheriting status of " + obj));

		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ChannelInheritanceException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.INVALIDDATA, e.getMessage()));
		}
	}

	/**
	 * Set the mc exclusion and disinherit status for multiple objects
	 * @param clazz object class
	 * @param ids list of object ids
	 * @param request request
	 * @return response
	 * @throws NodeException
	 */
	public static <U, T extends Disinheritable<U>> GenericResponse set(Class<T> clazz, List<String> ids, DisinheritRequest request) throws NodeException {
		// folders must be done in the correct order (mother folders first), so we get all folders and sort them by the length of their path
		if (clazz.isAssignableFrom(Folder.class)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<Folder> folders = new ArrayList<>();
			for (String id : ids) {
				folders.add(t.getObject(Folder.class, id));
			}

			Collections.sort(folders, new Comparator<Folder>() {
				@Override
				public int compare(Folder o1, Folder o2) {
					try {
						return o1.getParents().size() - o2.getParents().size();
					} catch (NodeException e) {
						throw new ClassCastException("Error while sorting folders");
					}
				}
			});
			ids = new ArrayList<>();
			for (Folder f : folders) {
				ids.add(Integer.toString(f.getId()));
			}
		}

		try (AutoCommit auto = new AutoCommit()) {
			for (String id : ids) {
				GenericResponse singleResponse = set(clazz, id, request, false);
				if (singleResponse.getResponseInfo().getResponseCode() != ResponseCode.OK) {
					return singleResponse;
				}
			}
			auto.success();
		}

		return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully changed disinheriting status for " + ids.size() + " objects"));
	}

	/**
	 * Tests if the logged in user has the required permission
	 * to change inheritance settings for the given object.
	 *
	 * @param obj The object for which the permission is required.
	 * @throws InsufficientPrivilegesException When the user does not
	 *		have the required permissions.
	 * @throws NodeException When there is an error during the
	 *		permission check.
	 */
	private static void assertInheritPermission(NodeObject obj) throws NodeException {
		if (!ObjectPermission.inheritance.checkObject(obj)) {
			throw new InsufficientPrivilegesException(
				"Insufficient privileges to exclude " + obj,
				"exclude.nopermission",
				I18NHelper.getName(obj), obj, PermType.updateinheritance);
		}
	}

	/**
	 * Sets the {@link Disinheritable#disinheritDefault} flag of
	 * the given object to the specified value.
	 *
	 * This method only performs the necessary permission checks,
	 * and then calls {@link Disinheritable#setDisinheritDefault}.
	 *
	 * @param obj The object to change the flag for
	 * @param value The new value for the flag.
	 * @param recursive When <code>true</code> the flag of all ancestor
	 *		objects is also set to the given value.
	 * @throws NodeException When an error occurs while setting the flag.
	 */
	private static void setDisinheritDefault(
			Disinheritable<?> obj,
			boolean value,
			boolean recursive) throws NodeException {
		recursive |= value;

		if (recursive) {
			if (obj instanceof Folder) {
				Queue<Folder> folders = new LinkedList<>();

				folders.add((Folder) obj);
				while (!folders.isEmpty()) {
					Folder folder = folders.remove();

					for (Folder child: folder.getChildFolders()) {
						assertInheritPermission(child);
						folders.add(child);
					}

					for (File child: folder.getFilesAndImages()) {
						assertInheritPermission(child);
					}

					for (Page child: folder.getPages()) {
						assertInheritPermission(child);
					}
				}
			}
		}

		obj.setDisinheritDefault(value, recursive);
	}

	/**
	 * Get the object and check for permissions
	 *
	 * @param clazz
	 *            object class
	 * @param id
	 *            object id
	 * @param perms
	 *            optional permissions to check
	 * @return object
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected static <U, T extends Disinheritable<U>> T getObject(Class<T> clazz, String id, ObjectPermission... perms) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		T obj = t.getObject(clazz, id);
		if (obj == null) {
			throw new EntityNotFoundException("Could not find object " + id, notFoundKeys.get(t.getTType(clazz)), id);
		}

		// check permission bits
		for (ObjectPermission p : perms) {
			if (!p.checkObject(obj)) {
				throw new InsufficientPrivilegesException("Insufficient privileges for " + id, noPermissionKeys.get(t.getTType(clazz)), id, obj,
						p.getPermType());
			}
		}

		return (T) obj.getMaster();
	}
}
