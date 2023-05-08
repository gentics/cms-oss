package com.gentics.contentnode.factory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Folder.FileSearch;
import com.gentics.contentnode.object.Folder.PageSearch;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.publish.Publisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * This static factory class implements multichannelling features.
 */
public class MultichannellingFactory {

	/**
	 * Push the given local object from its channel into the master
	 * @param localObject local(ized) object
	 * @param master master node to push to
	 * @return resulting object
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> LocalizableNodeObject<T> pushToMaster(
			LocalizableNodeObject<T> localObject, Node master) throws ReadOnlyException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (localObject == null) {
			throw new NodeException("Cannot push null object to master");
		}

		if (master == null) {
			throw new NodeException("Cannot push to master: master was null");
		}

		// if this object is already bound to the master, we are done
		Node channel = localObject.getChannel();

		if (channel == null || master.equals(channel)) {
			return localObject;
		}
		List<Node> masterNodes = channel.getMasterNodes();

		// check whether the master node is really a master of the objects channel
		if (!masterNodes.contains(master)) {
			throw new NodeException("Cannot push {" + localObject + "} to master {" + master + "}: {" + channel + "} is no channel of {" + master + "}");
		}

		// check whether there is an object in the given channelset that belongs to the given master
		Map<Integer, Integer> channelSet = localObject.getChannelSet();

		// check whether any channel "between" this channel and the master contains a localized copy
		for (Node node : masterNodes) {
			// stop when we found the master
			if (node.equals(master)) {
				break;
			}

			// check the channelset
			if (channelSet.containsKey(node.getId())) {
				// the channelset contains a localized copy in a channel
				// "between" this channel and the target master, so we must not
				// push the localized copy to the master
				// TODO is this really a failure? Or shall we just omit this object?
				throw new NodeException(
						"Error while pushing {" + localObject + "} from channel {" + channel + "} to {" + master
						+ "}: Channelset contains a localized copy in channel {" + node + "} (which is between channel and master)");
			}
		}

		// get the channel visibility of the object
		ChannelTreeSegment oldVisiblity = null;
		if (localObject instanceof Disinheritable<?>) {
			oldVisiblity = new ChannelTreeSegment((Disinheritable<?>) localObject, false);
		}

		// get an editable version of the local object (which will lock it)
		localObject = (LocalizableNodeObject<T>) t.getObject(localObject.getObjectInfo().getObjectClass(), localObject.getId(), true, false, true);

		// now we can push the localized copy to the master node
		LocalizableNodeObject<T> masterObject = null;

		boolean removeThisObject = false;

		try {
			if (channelSet.containsKey(master.getId())) {
				// get an editable copy of the master object
				masterObject = (LocalizableNodeObject<T>) t.getObject(localObject.getObjectInfo().getObjectClass(), channelSet.get(master.getId()), true, false,
						true);

				// copy this object over the channelset variant
				masterObject.copyFrom(localObject);
				removeThisObject = true;
			} else if (!master.isChannel() && channelSet.containsKey(0)) {
				// get an editable copy of the master object
				masterObject = (LocalizableNodeObject<T>) t.getObject(localObject.getObjectInfo().getObjectClass(), channelSet.get(0), true, false, true);

				// copy this page over the channelset variant
				masterObject.copyFrom(localObject);
				removeThisObject = true;
			} else {
				// check whether the master object exists in a higher master node
				LocalizableNodeObject<T> objectMaster = localObject.getMaster();
				Node masterChannel = objectMaster.getChannel();

				if (masterChannel == null || masterNodes.contains(masterChannel)) {
					// create a new channel variant in the given channel.
					masterObject = (LocalizableNodeObject<T>) localObject.copy();
					masterObject.setChannelInfo(master.getId(), localObject.getChannelSetId());
					removeThisObject = true;
				} else {
					// move the pageMaster into the given channel
					masterObject = (LocalizableNodeObject<T>) t.getObject(localObject.getObjectInfo().getObjectClass(), objectMaster.getId(), true, false, true);
					masterObject.modifyChannelId(master.isChannel() ? master.getId() : 0);
					masterObject.save();
					masterObject.unlock();

					// copy the page over the pageMaster
					if (!masterObject.equals(localObject)) {
						masterObject.copyFrom(localObject);
						removeThisObject = true;
					}
				}
			}

			// save and unlock the master object
			masterObject.save();
			masterObject.unlock();

			// remove this localized copy
			if (removeThisObject) {
				localObject.delete();
				localObject = null;
			}

			if (masterObject instanceof Disinheritable<?>) {
				ChannelTreeSegment newVisibility = new ChannelTreeSegment((Disinheritable<?>) masterObject, false);

				// object was pushed, so it appears in more channels now
				if (!removeThisObject) {
					Set<Node> revealIn = new HashSet<Node>(newVisibility.getAllNodes());
					revealIn.removeAll(oldVisiblity.getAllNodes());
					for (Node n : revealIn) {
						t.addTransactional(new TransactionalTriggerEvent(masterObject, new String[] { n.getId().toString() }, Events.REVEAL));
					}
				}
			}

			NodeObject parent = masterObject.getParentObject();

			if (parent != null) {
				t.addTransactional(new TransactionalDirtCache(parent.getObjectInfo().getObjectClass(), parent.getId()));
			}
		} finally {
			// if the local object was not removed, we unlock it
			if (localObject != null) {
				localObject.unlock();
			}
		}

		return masterObject;
	}

	/**
	 * Get the master object, if the given object is a localized copy. If the given
	 * object is not a localized copy or multichannelling is not activated,
	 * returns the given object object
	 * @param object given object
	 * @return master object for localized copies or this object
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> T getMaster(LocalizableNodeObject<T> object) throws NodeException {
		if (object == null) {
			return null;
		}
		if (object.isMaster()) {
			return object.getObject();
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {

			// multichannelling is active, so get the channelset
			Map<Integer, Integer> channelSet = object.getChannelSet();

			if (!channelSet.isEmpty()) {
				// get the id of the master (channelId is 0)
				Integer masterId = channelSet.get(0);

				if (!AbstractContentObject.isEmptyId(masterId)) {
					// get the master object
					return (T) t.getObject(object.getObjectInfo().getObjectClass(), masterId, -1, false);
				} else {
					// probably, the master object is a local object in another channel, so get all master nodes of this channel
					Node channel = object.getChannel();

					if (channel == null) {
						throw new NodeException("Could not get channel for {" + object + "}, although channelset is not empty");
					}
					List<Node> masterNodes = new ArrayList<Node>(channel.getMasterNodes());

					if (!masterNodes.isEmpty()) {
						Collections.reverse(masterNodes);
					}
					for (Node node : masterNodes) {
						masterId = channelSet.get(node.getId());
						if (!AbstractContentObject.isEmptyId(masterId)) {
							// get the master object
							return (T) t.getObject(object.getObjectInfo().getObjectClass(), masterId, -1, false);
						}
					}

					// found no master, so return this object
					return object.getObject();
				}
			} else {
				// channelset is empty, so return this object
				return object.getObject();
			}
		} else {
			// multichannelling not active, so return this object
			return object.getObject();
		}
	}

	/**
	 * If the given object is a localized copy, get the next higher object (the object, which would be inherited into the object's channel, if this localized copy did not
	 * exist). If the object is a master, return null.
	 * @param object
	 * @return next higher master or null
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> T getNextHigherObject(LocalizableNodeObject<T> object) throws NodeException {
		if (object == null) {
			return null;
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			if (object.isMaster()) {
				return null;
			} else {
				Map<Integer, Integer> channelSet = object.getChannelSet();
				Node channel = object.getChannel();
				List<Node> masterNodes = channel.getMasterNodes();

				for (Node node : masterNodes) {
					if (channelSet.containsKey(node.getId())) {
						return (T) t.getObject(object.getObjectInfo().getObjectClass(), channelSet.get(node.getId()), -1, false);
					}
				}

				if (channelSet.containsKey(0)) {
					return (T) t.getObject(object.getObjectInfo().getObjectClass(), channelSet.get(0), -1, false);
				} else {
					return null;
				}
			}
		} else {
			return null;
		}
	}

	/**
	 * Check whether the given object is visible in the given node or not.
	 * This method will not accept channel variants, if the object is localized in the given node, and the given object is not the
	 * localized copy, it will return false.
	 * @param node node
	 * @param object object to check
	 * @return true if the object is visible in the node, false if not
	 * @throws NodeException
	 */
	public static <T extends NodeObject> boolean isVisibleInNode(Node node, LocalizableNodeObject<T> object) throws NodeException {
		return isVisibleInNode(node, object, null);
	}

	/**
	 * Check whether the given object is visible in the given node or not.
	 * This method will not accept channel variants, if the object is localized in the given node, and the given object is not the
	 * localized copy, it will return false.
	 * @param node node
	 * @param object object to check
	 * @param filter optional filter to filter objects in the channelset. If the filter returns false for an object ID, the channelset variant will be ignored (as if it does not exist)
	 * @return true if the object is visible in the node, false if not
	 * @throws NodeException
	 */
	public static <T extends NodeObject> boolean isVisibleInNode(Node node, LocalizableNodeObject<T> object, Function<Integer, Boolean> filter)
			throws NodeException {
		// no object is not visible in no node
		if (node == null || object == null) {
			return false;
		}

		// get the node owning this object
		Node owningNode = object.getOwningNode();

		// If there's an owning node, we can decide against visibility in an optimized manner
		if (owningNode != null) {
			if (node.isChannel()) {
				// the given node is a channel, so the object can only be visible, if
				// the owning node either is the given node or a master of it
				if (!node.getMasterNodes().contains(owningNode) && !node.equals(owningNode)) {
					return false;
				}
			} else {
				// the given node is no channel, so the object can only be visible, if
				// the given node is the node owning the object
				if (!node.equals(owningNode)) {
					return false;
				}
			}
		}

		MultiChannellingFallbackList fbList = new MultiChannellingFallbackList(node);
		Integer channelSetId = object.getChannelSetId();
		Map<Integer, Integer> channelSet = object.getChannelSet();

		boolean excluded = false;
		Set<Node> disinheritedNodes = Collections.emptySet();
		if (object instanceof Disinheritable) {
			Disinheritable<T> disinheritable = (Disinheritable<T>)object;
			excluded = disinheritable.isExcluded();
			disinheritedNodes = ((Disinheritable<T>)disinheritable.getMaster()).getDisinheritedChannels();
		}

		if (ObjectTransformer.isEmpty(channelSet)) {
			// the object has no channelset, so just add the single object to the fallback list
			Node channel = object.getChannel();

			fbList.addObject(object.getId(), channelSetId, channel != null ? channel.getId() : 0, excluded, disinheritedNodes);
		} else {
			// add all objects of the channelset to the fallback list
			for (Map.Entry<Integer, Integer> entry : channelSet.entrySet()) {
				if (filter != null) {
					if (!ObjectTransformer.getBoolean(filter.apply(entry.getValue()), false)) {
						continue;
					}
				}
				fbList.addObject(entry.getValue(), channelSetId, entry.getKey(), excluded, disinheritedNodes);
			}
		}

		// now the fallback list tells us, which of the objects is visible in the node/channel
		// if the object's id is among them, the object is visible
		return fbList.getObjectIds().contains(object.getId());
	}

	/**
	 * Get the channelset variant of the given object, that is visible from the given node (channel).
	 * 
	 * @param object object
	 * @param node node (channel)
	 * @return channelset variant of the object, that is visible from the give node
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> T getChannelVariant(LocalizableNodeObject<T> object, Node node) throws NodeException {
		// edge cases
		if (object == null) {
			return null;
		}
		// when no node is given, we don't do any fallback
		if (node == null) {
			return object.getObject();
		}

		Node channel = object.getChannel();

		// easy cases
		if (channel == null && !node.isChannel()) {
			return object.getObject();
		}

		MultiChannellingFallbackList fbList = new MultiChannellingFallbackList(node);
		Integer channelSetId = object.getChannelSetId();
		Map<Integer, Integer> channelSet = object.getChannelSet();

		boolean excluded = false;
		Set<Node> disinheritedNodes = Collections.emptySet();
		if (object instanceof Disinheritable) {
			Disinheritable<T> disinheritable = (Disinheritable<T>)object;
			excluded = disinheritable.isExcluded();
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				disinheritedNodes = ((Disinheritable<T>)disinheritable.getMaster()).getDisinheritedChannels();
			}
		}

		if (ObjectTransformer.isEmpty(channelSet)) {
			// the object has no channelset, so just add the single object to the fallback list
			fbList.addObject(object.getId(), channelSetId, channel != null ? channel.getId() : 0, excluded, disinheritedNodes);
		} else {
			// add all objects of the channelset to the fallback list
			for (Map.Entry<Integer, Integer> entry : channelSet.entrySet()) {
				fbList.addObject(entry.getValue(), channelSetId, entry.getKey(), excluded, disinheritedNodes);
			}
		}

		if (fbList.getObjectIds().isEmpty()) {
			// when the fallbacklist returns no id's that means that the object
			// is not visible in the scope of the given node, so we return null
			return null;
		} else {
			Transaction t = TransactionManager.getCurrentTransaction();

			Integer fallbackId = fbList.getObjectIds().get(0);
			if (object.getId().equals(fallbackId)) {
				return object.getObject();
			} else {
				return (T) t.getObject(object.getObjectInfo().getObjectClass(), fallbackId, false, false, true);
			}
		}
	}

	/**
	 * Get the id's of the node, this object is visible in.
	 * @param object object
	 * @param considerPublishStatus true to consider the publish status (of pages), false to not consider the publish status.
	 * @return id's of nodes
	 * @throws NodeException
	 */
	public static <T extends NodeObject> Collection<Integer> getNodeIds(LocalizableNodeObject<T> object, boolean considerPublishStatus) throws NodeException {
		// start with the channel or node this object belongs to
		Collection<Node> startingNodes = new ArrayList<Node>();

		Node channel = object.getChannel();

		if (channel != null) {
			startingNodes.add(channel);
		} else {
			if (object instanceof Template) {
				// for templates, get the folders, this template is assigned to and add the owning nodes
				Template template = (Template) object;
				List<Folder> folders = template.getFolders();

				for (Folder folder : folders) {
					channel = folder.getOwningNode();
					if (!startingNodes.contains(folder.getOwningNode())) {
						startingNodes.add(channel);
					}
				}
			} else {
				channel = object.getOwningNode();
				if (channel != null) {
					startingNodes.add(channel);
				}
			}
		}

		Collection<Integer> nodeIds = new ArrayList<Integer>();

		recursiveAddNodeIds(object, startingNodes, nodeIds, considerPublishStatus);

		return nodeIds;
	}

	/**
	 * Get the inherited, already rendered source of the page.
	 * This method will check all dependencies of the inherited page in a super channel for localization changes
	 * @param page page
	 * @param renderResult render result
	 * @return inherited source or null if not found
	 * @throws NodeException
	 */
	public static String getInheritedPageSource(final Page page, RenderResult renderResult) throws NodeException {
		// if the page is not inherited, we return null
		if (!page.isInherited()) {
			return null;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean omitPublishTable = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.OMIT_PUBLISH_TABLE);
		RenderType renderType = t.getRenderType();
		Node currentChannel = t.getChannel();

		// if the current channel is no channel, we return null
		if (currentChannel == null || !currentChannel.isChannel()) {
			return null;
		}

		TransactionStatistics stats = t.getStatistics();
		if (stats != null) {
			stats.get(Item.CHECK_INHERITED_SOURCE).start();
		}

		try {
			// get the direct super channel
			Node superChannel = currentChannel.getMasterNodes().get(0);

			// if the superChannel does not publish into the filesystem and the feature omit_publish_table is on,
			// we cannot find the source in the publish table, so we cannot check
			if (omitPublishTable && !superChannel.doPublishFilesystem()) {
				return null;
			}

			String source = null;
			// read all dependencies for the page in its own node. The dependencies
			// for the object (for all channels and properties) are already stored
			// in the rendertype,
			// so we just filter by node and property
			List<Dependency> contentDeps = new ArrayList<Dependency>(renderType.getDependencies());
			DependencyManager.filterForChannel(contentDeps, superChannel);
			DependencyManager.filterForProperty(contentDeps, "content");

			if (!contentDeps.isEmpty()) {
				boolean foundDifferentDependency = false;

				// now check all dependencies, whether they are localized in the current channel
				for (Dependency dep : contentDeps) {
					NodeObject depSource = dep.getSource().getObject();

					if (depSource instanceof LocalizableNodeObject<?>) {
						// compare the channelset variants for the current channel and the owning node
						@SuppressWarnings("unchecked")
						LocalizableNodeObject<NodeObject> locDepSource = (LocalizableNodeObject<NodeObject>) depSource;

						if (!ObjectTransformer.equals(MultichannellingFactory.getChannelVariant(locDepSource, superChannel),
								MultichannellingFactory.getChannelVariant(locDepSource, currentChannel))) {
							foundDifferentDependency = true;
							break;
						}
					} else if (superChannel.equals(depSource) && !ObjectTransformer.isEmpty(dep.getSourceProperty())) {
						// special treatment for dependencies on the current node, as they will be different all of the time

						// we will compare the rendered properties
						String property = dep.getSourceProperty();
						// temporarily disable handling dependencies 
						boolean handleDeps = renderType.doHandleDependencies();

						try {
							renderType.setHandleDependencies(false);
							if (!StringUtils.isEqual(ObjectTransformer.getString(superChannel.get(property), null),
									ObjectTransformer.getString(currentChannel.get(property), null))) {
								foundDifferentDependency = true;
								break;
							}
						} finally {
							// enable handling dependencies
							renderType.setHandleDependencies(handleDeps);
						}
					}

					// If we have a dependency on a Folder's content, we must check for local objects
					// that might be e.g. appended to an overview
					final String srcProp = dep.getSourceProperty();
					if (depSource instanceof Folder && srcProp != null) {
						final Folder depFolder = (Folder) depSource;
						List<?> results = Collections.emptyList();
						if (srcProp.equals(Folder.PAGES_PROPERTY)) {
							PageSearch localPageSearch = new PageSearch();
							localPageSearch.setInherited(false);
							results = (List<?>) depFolder.getPages(localPageSearch);
						} else if (srcProp.equals(Folder.IMAGES_PROPERTY) || srcProp.equals(Folder.FILES_PROPERTY)
								|| srcProp.equals(Folder.FILESANDIMAGES_PROPERTY)) {
							FileSearch localFileSearch = new FileSearch();
							localFileSearch.setInherited(false);
							results = new ArrayList<AbstractContentObject>();
							if (!srcProp.equals(Folder.FILES_PROPERTY)) {
								results.addAll((List) depFolder.getImages(localFileSearch));
							}
							if (!srcProp.equals(Folder.IMAGES_PROPERTY)) {
								results.addAll((List) depFolder.getFiles(localFileSearch));
							}
						} else if (srcProp.equals(Folder.FOLDERS_PROPERTY)) {
							List<Folder> subfolders = new LinkedList<Folder>(depFolder.getChildFolders());
							for (Iterator<Folder> fi = subfolders.iterator(); fi.hasNext();) {
								Folder f = fi.next();
								if (f.isInherited()) {
									fi.remove();
								}
							}
							results = (List) subfolders;
						}
						if (results.size() > 0) {
							foundDifferentDependency = true;
							break;
						}
					}
				}

				if (!foundDifferentDependency) {
					final int nodeId = ObjectTransformer.getInt(superChannel.getId(), 0);
					final String[] sources = new String[1];

					// get the rendered source of the master object
					DBUtils.executeStatement("SELECT source FROM publish WHERE page_id = ? AND node_id = ? AND active = ?", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, ObjectTransformer.getInt(page.getId(), 0)); // page_id = ?
							stmt.setInt(2, nodeId); // node_id = ?
							stmt.setInt(3, 1); // active = ?
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							if (rs.next()) {
								sources[0] = rs.getString("source");
							}
						}
					});
					if (!StringUtils.isEmpty(sources[0])) {
						// we found a source, so we will re-use it
						source = sources[0];
						renderResult.info(Publisher.class, page + " uses rendered source from channel " + superChannel);
						// add the dependencies to the current dependencies
						for (Dependency dep : contentDeps) {
							DependencyObject depSource = dep.getSource();
							DependencyObject newDepSource = depSource;

							// if the dependency is on the channel, we need to update it
							if (superChannel.equals(newDepSource.getObject())) {
								newDepSource = new DependencyObject(currentChannel);
							}

							renderType.addDependency(newDepSource, dep.getSourceProperty());
						}
					}
				}
			}

			return source;
		} finally {
			if (stats != null) {
				stats.get(Item.CHECK_INHERITED_SOURCE).stop();
			}
		}
	}

	/**
	 * Correct the channel id for new objects. If the channel id is 0 (no specific channel specified), but the folder belongs to a channel, the folder's channel will be returned (so the object will be created in the folder's channel).
	 * If a channel id is given, but the folder is not visible in that channel, an exception will be thrown (object cannot be created in the requested channel, because the folder does not exist in that channel).
	 * @param folderId folder id where the object shall be created
	 * @param channelId channel id, for which the object shall be created
	 * @return the channel id, for which the object can be created
	 * @throws NodeException if creation of the object in the channel is not possible
	 */
	public static int correctChannelId(int folderId, int channelId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, folderId, -1, false);
		if (folder == null) {
			throw new NodeException("Cannot create object in folder " + folderId + ", because this folder does not exist");
		}
		if (channelId == 0) {
			Node folderChannel = folder.getChannel();
			if (folderChannel != null) {
				return ObjectTransformer.getInt(folderChannel.getId(), 0);
			} else {
				return channelId;
			}
		} else {
			Node requestedChannel = t.getObject(Node.class, channelId);
			if (requestedChannel == null) {
				throw new NodeException("Cannot create object in channel " + channelId + ", because this channel does not exist");
			}
			Node folderNode = folder.getOwningNode();
			if (!requestedChannel.equals(folderNode) && !requestedChannel.isChannelOf(folderNode)) {
				throw new NodeException("Cannot create object in folder " + folder + " and channel " + requestedChannel
						+ ", because the folder is not visible in that channel");
			}
			return channelId;
		}
	}

	/**
	 * Recursively determine the ids of the nodes, where the given object is visible in
	 * @param object object
	 * @param nodes collection of nodes to check
	 * @param nodeIds collection of node ids to modify
	 * @param considerPublishStatus true if the publish status of pages shall be considered
	 * @throws NodeException
	 */
	private static <T extends NodeObject> void recursiveAddNodeIds(LocalizableNodeObject<T> object, Collection<Node> nodes, Collection<Integer> nodeIds, boolean considerPublishStatus) throws NodeException {
		// get the channelset
		Map<Integer, Integer> channelSet = object.getChannelSet();
		// get the object's id
		int objectId = ObjectTransformer.getInt(object.getId(), 0);

		// iterate over all nodes
		for (Node node : nodes) {
			if (!MultichannellingFactory.isVisibleInNode(node, object)) {
				continue;
			}

			// get the node id
			Integer nodeId = ObjectTransformer.getInteger(node.getId(), null);

			if (nodeId != null) {
				// get the channelset variant of the object in the current node (default to the object's id)
				int variantId = ObjectTransformer.getInt(channelSet.get(nodeId), objectId);

				// if the object does not have a different channelset variant, it is visible in the node, so add it and do the recursion
				if (!nodeIds.contains(nodeId)) {
					if (objectId == variantId) {
						nodeIds.add(nodeId);
						recursiveAddNodeIds(object, node.getChannels(), nodeIds, false);
					} else {// TODO check publish status for pages
					}
				}
			}
		}
	}

	/**
	 * Performs multichanneling fallback for multiple channels using objects
	 * returned by the given SQL query.
	 *
	 * @param sqlQuery
	 *            the SQL query used to produce the objects. It must produce a
	 *            result set containing at least the columns "id", "channel_id",
	 *            "channelset_id", "mc_exclude" and "disinherited_channel"
	 * @param args
	 *            the arguments passed to the query
	 * @param channels
	 *            the set of channel nodes to perform multichannelling fallback
	 *            on
	 * @return the filled multichannelling fallback lists
	 * @throws NodeException
	 */
	public static Map<Node, MultiChannellingFallbackList> performMultichannelMultichannellingFallback(String sqlQuery, final Object[] args, Set<Node> channels)
			throws NodeException {
		final Map<Node, MultiChannellingFallbackList> result = new HashMap<>();
		for (Node n : channels) {
			result.put(n, new MultiChannellingFallbackList(n));
		}

		DBUtils.executeStatement(sqlQuery, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int arg = 1;
				for (Object o : args) {
					stmt.setObject(arg++, o);
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					int id = rs.getInt("id");
					int channelId = rs.getInt("channel_id");
					int channelsetId = rs.getInt("channelset_id");
					boolean excluded = rs.getBoolean("mc_exclude");
					Integer disinheritedChannel = rs.getInt("disinherited_channel");
					if (rs.wasNull()) {
						disinheritedChannel = null;
					}
					for (Entry<Node, MultiChannellingFallbackList> e : result.entrySet()) {
						e.getValue().addObject(id, channelsetId, channelId, excluded, disinheritedChannel);
					}
				}
			}
		});
		return result;
	}

	/**
	 * Performs multichanneling fallback for multiple channels using objects
	 * returned by the given SQL query.
	 *
	 * @param sqlQuery
	 *            the SQL query used to produce the objects. It must produce a
	 *            result set containing at least the columns "id", "channel_id",
	 *            "channelset_id", "mc_exclude" and "disinherited_channel"
	 * @param channels
	 *            the set of channel nodes to perform multichannelling fallback
	 *            on
	 * @param args
	 *            the arguments passed to the query
	 * @return the filled multichannelling fallback lists
	 * @throws NodeException
	 */
	public static Map<Node, MultiChannellingFallbackList> performMultichannelMultichannellingFallback(String sqlQuery, Set<Node> channels, Object...args)
			throws NodeException {
		final Map<Node, MultiChannellingFallbackList> result = new HashMap<>();
		for (Node n : channels) {
			result.put(n, new MultiChannellingFallbackList(n));
		}

		DBUtils.executeStatement(sqlQuery, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int arg = 1;
				for (Object o : args) {
					if (o instanceof Collection) {
						for (Object subO : (Collection<?>)o) {
							stmt.setObject(arg++, subO);
						}
					} else {
						stmt.setObject(arg++, o);
					}
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					int id = rs.getInt("id");
					int channelId = rs.getInt("channel_id");
					int channelsetId = rs.getInt("channelset_id");
					boolean excluded = rs.getBoolean("mc_exclude");
					Integer disinheritedChannel = rs.getInt("disinherited_channel");
					if (rs.wasNull()) {
						disinheritedChannel = null;
					}
					for (Entry<Node, MultiChannellingFallbackList> e : result.entrySet()) {
						e.getValue().addObject(id, channelsetId, channelId, excluded, disinheritedChannel);
					}
				}
			}
		});
		return result;
	}

	/**
	 * Load the channelset from the given objects for the channelset id.
	 * The returned map will contain an entry for every {@link Wastebin} value, which maps the channel_id's to record id's
	 * @param clazz object class
	 * @param channelSetId channelset ID
	 * @return channelsets
	 * @throws NodeException
	 */
	public static Map<Wastebin, Map<Integer, Integer>> loadChannelset(Class<? extends NodeObject> clazz, int channelSetId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String table = t.getTable(clazz);
		ObjectFactory objectFactory = t.getObjectFactory(clazz);

		Map<Wastebin, Map<Integer, Integer>> channelSet = new HashMap<Wastebin, Map<Integer, Integer>>();
		channelSet.put(Wastebin.INCLUDE, new HashMap<Integer, Integer>());
		channelSet.put(Wastebin.EXCLUDE, new HashMap<Integer, Integer>());
		channelSet.put(Wastebin.ONLY, new HashMap<Integer, Integer>());
		if (channelSetId > 0) {
			DBUtils.executeStatement("SELECT id, channel_id, deleted, is_master FROM " + table + " WHERE channelset_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, channelSetId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int id = rs.getInt("id");
						int channelId = rs.getInt("channel_id");
						int deleted = rs.getInt("deleted");
						boolean master = rs.getBoolean("is_master");

						// ignore localized copies, which are about to be deleted
						if (!master && objectFactory.isInDeletedList(clazz, id)) {
							continue;
						}

						if (deleted == 0) {
							channelSet.get(Wastebin.INCLUDE).put(channelId, id);
							channelSet.get(Wastebin.EXCLUDE).put(channelId, id);
						} else {
							channelSet.get(Wastebin.INCLUDE).put(channelId, id);
							channelSet.get(Wastebin.ONLY).put(channelId, id);
						}
					}
				}
			});
		}

		return channelSet;
	}

	/**
	 * Check whether the given channelset is empty
	 * @param channelSet channelset
	 * @return true iff the channelset is empty
	 */
	public static boolean isEmpty(Map<Wastebin, Map<Integer, Integer>> channelSet) {
		if (ObjectTransformer.isEmpty(channelSet)) {
			return true;
		}

		for (Map<Integer, Integer> map : channelSet.values()) {
			if (!ObjectTransformer.isEmpty(map)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get map of objectId -> channelId of all localized copies of the given object.
	 * This method will not consider permissions of the current user
	 * @param object object
	 * @return map
	 * @throws NodeException
	 */
	public static Map<Integer, Integer> getLocalizedCopyChannels(LocalizableNodeObject<?> object)
			throws NodeException {
		/* This looks really cool, but *not* using streams here means actually
		 * less typing :-).
		Collector<Map.Entry<Integer, Integer>, ?, Map<Integer, Integer>> collector = Collectors.toMap(
			Map.Entry::getValue,
			Map.Entry::getKey,
			(u, v) -> u,
			LinkedHashMap::new);

		return channelSet.entrySet().stream().collect(collector);
		*/
		Map<Integer, Integer> channelSet = object.getChannelSet();
		Map<Integer, Integer> localizedCopyChannels = new LinkedHashMap<>(channelSet.size());

		channelSet.remove(0);
		Node channel = object.getChannel();
		if (channel != null) {
			channelSet.remove(channel.getId());
		}
		for (Map.Entry<Integer, Integer> entry : channelSet.entrySet()) {
			localizedCopyChannels.put(entry.getValue(), entry.getKey());
		}

		return localizedCopyChannels;
	}

	/**
	 * Remove the channelIds for the collection for which the current user does not have view permission on the given object
	 * @param channelIds collection of channel Ids (must be mutable)
	 * @param object object
	 * @throws NodeException
	 */
	public static void filterPermissions(Collection<Integer> channelIds, LocalizableNodeObject<?> object) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (Iterator<Integer> i = channelIds.iterator(); i.hasNext();) {
			int channelId = i.next();

			Node channel = t.getObject(Node.class, channelId);
			if (!ObjectPermission.view.checkObject(object, channel)) {
				i.remove();
			}
		}
	}

	/**
	 * Get a map for folderId -> map of channelId -> count of localized/local objects in the channel within the folder structure of the given folder
	 * @param folder folder
	 * @return localization map
	 * @throws NodeException
	 */
	public static Map<Integer, Map<Integer, AtomicInteger>> getLocalizationCounts(Folder folder) throws NodeException {
		Map<Integer, Map<Integer, AtomicInteger>> result = new HashMap<>();

		List<Integer> folderIds = new ArrayList<>(FolderFactory.getSubfolderIds(folder, true, true));

		Node channel = folder.getChannel();
		int channelId = channel != null ? channel.getId() : 0;

		SQLExecutor executor = new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, channelId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					int folderId = rs.getInt("folder_id");
					int cId = rs.getInt("channel_id");
					int count = rs.getInt("c");
					result.computeIfAbsent(folderId, k -> new HashMap<>()).computeIfAbsent(cId, k -> new AtomicInteger()).addAndGet(count);
				}
			}
		};

		// self
		DBUtils.select(
				"SELECT id folder_id, channel_id, count(*) c FROM folder WHERE deleted = 0 AND (is_master = 0 OR (channel_id != 0 AND channel_id != ?)) AND channelset_id = ?",
				st -> {
					st.setInt(1, channelId);
					st.setInt(2, folder.getChannelSetId());
				}, rs -> {
					while (rs.next()) {
						int folderId = rs.getInt("folder_id");
						int cId = rs.getInt("channel_id");
						int count = rs.getInt("c");
						result.computeIfAbsent(folderId, k -> new HashMap<>()).computeIfAbsent(cId, k -> new AtomicInteger()).addAndGet(count);
					}
					return null;
				});

		// subfolders
		DBUtils.executeMassStatement(
				"SELECT id folder_id, channel_id, count(*) c FROM folder WHERE deleted = 0 AND (is_master = 0 OR (channel_id != 0 AND channel_id != ?)) AND mother IN", "GROUP BY id, channel_id",
				folderIds, 2, executor, Transaction.SELECT_STATEMENT);

		// pages
		DBUtils.executeMassStatement(
				"SELECT folder_id, channel_id, count(*) c FROM page WHERE deleted = 0 AND (is_master = 0 OR (channel_id != 0 AND channel_id != ?)) AND folder_id IN", "GROUP BY folder_id, channel_id",
				folderIds, 2, executor, Transaction.SELECT_STATEMENT);

		// images and files
		DBUtils.executeMassStatement(
				"SELECT folder_id, channel_id, count(*) c FROM contentfile WHERE deleted = 0 AND (is_master = 0 OR (channel_id != 0 AND channel_id != ?)) AND folder_id IN", "GROUP BY folder_id, channel_id",
				folderIds, 2, executor, Transaction.SELECT_STATEMENT);

		return result;
	}
}
