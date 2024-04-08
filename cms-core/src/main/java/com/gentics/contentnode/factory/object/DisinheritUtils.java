package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.HandleDependenciesTrx;
import com.gentics.contentnode.factory.MultiChannellingFallbackList;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.string.CNStringUtils;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

import io.reactivex.Flowable;

/**
 * This class holds static utility methods for loading and saving channel
 * disinheritings and multichannelling exclusions.
 *
 * @author escitalopram
 *
 */
public class DisinheritUtils {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(DisinheritUtils.class);

	/**
	 * Private constructor – no instances.
	 */
	private DisinheritUtils() {
	}

	/**
	 * Sets the <code>disinheritDefault</code> flag of the given object to
	 * the specified value.
	 *
	 * @param object The object to update
	 * @param disinheritDefault The new value for the flag
	 * @return <code>true</code> if the flag actually changed,
	 *		<code>false</code> otherwise.
	 * @throws ChannelInheritanceException When the flag should be removed from
	 *		the object while its parent has the flag set.
	 * @throws NodeException On database or transaction errors.
	 */
	static boolean updateDisinheritDefault(
			DisinheritableInternal<?> object,
			boolean disinheritDefault) throws NodeException {
		if (object.isDisinheritDefault() == disinheritDefault) {
			return false;
		}

		NodeObject parent = object.getParentObject();

		if (!disinheritDefault
				&& parent instanceof Folder
				&& ((Folder) parent).isDisinheritDefault()) {
			throw new ChannelInheritanceException(
				"Cannot remove default disinheritance flag from object " + object
					+ ", because parent folder " + parent + " is excluded",
				"disinherit.default.parent.excluded",
				Arrays.asList(I18NHelper.getName(object), I18NHelper.getPath(parent)));
		}

		logger.debug("Setting default disinheritance flag for object ("
			+ object.getId() + "): " + disinheritDefault);

		Transaction t = TransactionManager.getCurrentTransaction();
		String tableName = t.getTable(object.getObjectInfo().getObjectClass());
		DBUtils.executeUpdate(
			"UPDATE " + tableName + " SET disinherit_default = ? WHERE id = ?",
			new Object[] { disinheritDefault, object.getId() });


		for (Integer o: new HashSet<>(object.getChannelSet().values())) {
			t.dirtObjectCache(object.getObjectInfo().getObjectClass(), o);
		}

		return true;
	}

	/**
	 * Saves changes to disinherited channel set.
	 *
	 * @param object
	 *            the disinheritable object to work on
	 * @param disinheritedNodes
	 *            the new set of disinherited nodes
	 * @param skipRecursiveChecks
	 *            iff true, checks on subfolders are not performed. Used during
	 *            recursive disinheriting/excluding.
	 *            <em>Use only when the checks have already been performed!</em>
	 * @return true iff a change was made to the database
	 * @throws NodeException
	 */
	static <T extends NodeObject> boolean updateDisinheritedNodeAssociations(DisinheritableInternal<T> object, boolean excluded, Set<Node> disinheritedNodes, boolean skipRecursiveChecks) throws NodeException {
		if (!object.isMaster()) {
			return false;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		boolean changed = false;
		cleanupDisinheritedNodes(object, excluded, disinheritedNodes);
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			checkChangeConsistency(object, excluded, disinheritedNodes, skipRecursiveChecks);
		}
		ChannelTreeSegment originalVisibility = new ChannelTreeSegment(object, true);
		Set<Integer> originalDisinheritedChannelIds = object.getOriginalDisinheritedNodeIds();
		String tableName = t.getTable(object.getObjectInfo().getObjectClass());
		HashSet<Integer> newNodeIds = new HashSet<>();
		for (Node n : disinheritedNodes) {
			newNodeIds.add(n.getId());
		}

		if (excluded != object.isExcluded()) {
			DBUtils.executeUpdate("UPDATE " + tableName + " set mc_exclude = ? WHERE id = ?", new Object[]{excluded, object.getId()});
			object.setExcluded(excluded);
			changed = true;
		}
		boolean disinheritedChannelsChanged = false;
		if (originalDisinheritedChannelIds != null) {
			for (Integer oid : originalDisinheritedChannelIds) {
				if (!newNodeIds.contains(oid)) {
					DBUtils.executeUpdate("DELETE FROM " + tableName + "_disinherit WHERE " + tableName + "_id = ? AND channel_id = ?",
							new Object[] { object.getId(), oid });
					disinheritedChannelsChanged = true;
				}
			}
		}
		for (Integer nid : newNodeIds) {
			if (originalDisinheritedChannelIds == null || !originalDisinheritedChannelIds.contains(nid)) {
				DBUtils.executeInsert("INSERT INTO " + tableName + "_disinherit (" + tableName + "_id, channel_id) VALUES (?, ?)",
						new Object[] { object.getId(), nid });
				disinheritedChannelsChanged = true;
			}
		}

		if (disinheritedChannelsChanged) {
			object.setOriginalDisinheritedNodeIds(null);
		}

		changed |= disinheritedChannelsChanged;

		if (changed) {
			Folder parent = (Folder)object.getParentObject();
			for (Integer o : new HashSet<>(object.getChannelSet().values())) {
				t.dirtObjectCache(object.getObjectInfo().getObjectClass(), o);
			}
			if (parent != null) {
				t.dirtObjectCache(Folder.class, parent.getId());
			}

			// handle dirting and logcmd
			List<? extends NodeObject> channelVariants = t.getObjects(object.getObjectInfo().getObjectClass(), object.getChannelSet().values(), false, false);
			ChannelTreeSegment newVisibility = new ChannelTreeSegment(object, true);
			Set<Node> hideIn = new HashSet<Node>(originalVisibility.getAllNodes());
			hideIn.removeAll(newVisibility.getAllNodes());
			for (Node node : hideIn) {
				ActionLogger.logCmd(ActionLogger.MC_HIDE, object.getTType(), object.getId(), node.getId(), excluded ? "Excluded from channel "
						+ node.getFolder().getName() : "Disinherited in channel " + node.getFolder().getName());
				for (NodeObject variant : channelVariants) {
					t.addTransactional(new TransactionalTriggerEvent(variant, new String[] {node.getId().toString()}, Events.HIDE));
				}
			}
			Set<Node> revealIn = new HashSet<Node>(newVisibility.getAllNodes());
			revealIn.removeAll(originalVisibility.getAllNodes());
			for (Node node : revealIn) {
				ActionLogger.logCmd(ActionLogger.MC_UNHIDE, object.getTType(), object.getId(), node.getId(), "Reinherited in channel "
						+ node.getFolder().getName());
				t.addTransactional(new TransactionalTriggerEvent(MultichannellingFactory.getChannelVariant(object, node), new String[] { node.getId()
						.toString() }, Events.REVEAL));
			}
		}

		return changed;
	}

	/**
	 * Saves the associations of disinherited channel nodes for. This method is
	 * intended for the first save of a disinherited object.
	 *
	 * @param object
	 *            the object to work on
	 * @param excluded whether the new object should be excluded from multichannelling
	 * @param disinheritedNodes the set of disinherited nodes
	 * @throws NodeException
	 */
	static <T> void saveNewDisinheritedAssociations(DisinheritableInternal<T> object, boolean excluded, Set<Node> disinheritedNodes) throws NodeException {
		cleanupDisinheritedNodes(object, excluded, disinheritedNodes);
		checkCreationConsistency(object, excluded, disinheritedNodes);

		if (!object.isMaster()) {
			return;
		}
		String tableName = TransactionManager.getCurrentTransaction().getTable(object.getObjectInfo().getObjectClass());
		if (disinheritedNodes != null && !disinheritedNodes.isEmpty()) {
			for (Node n : disinheritedNodes) {
				DBUtils.executeInsert(
						"INSERT INTO " + tableName + "_disinherit (" + tableName + "_id, channel_id) VALUES (?, ?)", new Object[] {object.getId(), n.getId()});
			}

			object.setOriginalDisinheritedNodeIds(null);
		}

		DBUtils.executeUpdate("UPDATE " + tableName + " set mc_exclude = ? WHERE id = ?", new Object[]{excluded, object.getId()});
		object.setExcluded(excluded);
	}

	/**
	 * Removes unrelated nodes and nodes that are transitively disinherited from
	 * the disinherited channel node set.
	 *
	 * @param object
	 *            the object the node set is meant for
	 * @param excluded
	 *            whether the object should be excluded
	 * @param disinheritedNodes
	 *            the set of disinherited nodes to work on
	 * @throws NodeException
	 */
	static <T> void cleanupDisinheritedNodes(DisinheritableInternal<T> object, boolean excluded, Set<Node> disinheritedNodes) throws NodeException {
		if (excluded && !object.isExcluded()) {
			if (disinheritedNodes != null && disinheritedNodes.size() > 0) {
				disinheritedNodes.clear();
			}
			return;
		}

		if (disinheritedNodes == null) {
			return;
		}

		Node masterNode = object.getMaster().getChannel(); // TODO: Can we strip the getMaster() for it is probably not allowed to use this method on non-master objects
		if (masterNode == null) {
			masterNode = object.getMaster().getOwningNode(); // Always Correct?
		}

		ChannelTreeSegment.simplifyEndNodes(disinheritedNodes, masterNode);
	}

	/**
	 * Gets the set of disinherited channels for the specified disinheritable,
	 * if applicable, loading them from the database if not yet loaded.
	 *
	 * @param object
	 *            the disinheritable factory object to work on
	 * @return set of disinherited channels
	 * @throws NodeException
	 */
	protected static <T> Set<Node> loadDisinheritedChannelsInternal(DisinheritableInternal<T> object) throws NodeException {
		if (!object.isMaster()) {
			throw new NodeException("Disinherited channels set requested from localized object " + object.getTType() + "."+ object.getId());
		}

		// if the object is new, it cannot have its own disinherited channels, so we get the disinherited channels of the parent object
		if (AbstractContentObject.isEmptyId(object.getId()) && object.getParentObject() instanceof DisinheritableInternal<?>) {
			DisinheritableInternal<?> parentObject = null;
			try (NoMcTrx noMc = new NoMcTrx()) {
				parentObject = (DisinheritableInternal<?>)(object.getParentObject());
			}
			return loadDisinheritedChannelsInternal(parentObject);
		}

		if (object.getOriginalDisinheritedNodeIds() == null) {
			loadDisinheritedChannelIds(object);
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		try (ChannelTrx trx = new ChannelTrx()) {
			Set<Node> disinheritedNodes = new HashSet<Node>(t.getObjects(Node.class, object.getOriginalDisinheritedNodeIds()));
			return disinheritedNodes;
		}
	}

	/**
	 * Initializes the disinheritedChannelIds field of the specified
	 * disinheritable object.
	 *
	 * @param object
	 *            the disinheritable factory object to work on
	 * @throws NodeException
	 */
	private static <T> void loadDisinheritedChannelIds(final DisinheritableInternal<T> object) throws NodeException {
		final Set<Integer> result = new HashSet<Integer>();
		String tableName = TransactionManager.getCurrentTransaction().getTable(object.getObjectInfo().getObjectClass());
		if (object.getId() != null) {
			DBUtils.executeStatement("SELECT channel_id FROM " + tableName + "_disinherit where " + tableName + "_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, object.getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						result.add(rs.getInt("channel_id"));
					}
				}
			});
		}
		object.setOriginalDisinheritedNodeIds(result);
	}

	/**
	 * Creates a map of all subobjects of a given folder in all channels to
	 * their multichannelling inheritance data.
	 *
	 * @param folder
	 *            the folder to retrieve the information for
	 * @return the newly created map
	 * @throws NodeException
	 */
	public static Map<DisinheritableObjectReference, DisinheritableObjectData> getChannelIndependentFolderChildren(final Folder folder) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String pageDeleteClause = null;
		String folderDeleteClause = null;
		String fileDeleteClause = null;
		switch (t.getWastebinFilter()) {
		case INCLUDE:
			pageDeleteClause = "";
			folderDeleteClause = "";
			fileDeleteClause = "";
			break;
		case ONLY:
			pageDeleteClause = " AND p.deleted != 0";
			folderDeleteClause = " AND f.deleted != 0";
			fileDeleteClause = " AND c.deleted != 0";
			break;
		case EXCLUDE:
		default:
			pageDeleteClause = " AND p.deleted = 0";
			folderDeleteClause = " AND f.deleted = 0";
			fileDeleteClause = " AND c.deleted = 0";
			break;
		}
		final HashMap<DisinheritableObjectReference, DisinheritableObjectData> result = new HashMap<>();
		DBUtils.executeStatement(
				"(SELECT p.id, 10007 AS type, CASE WHEN p.channel_id = 0 THEN cf.node_id ELSE p.channel_id END channel_id, p.is_master, p.mc_exclude, pd.channel_id disinherited_channel FROM page p LEFT JOIN page_disinherit pd ON p.id = pd.page_id JOIN folder cf ON p.folder_id = cf.id WHERE p.folder_id = ? " + pageDeleteClause + ") "
						+ "UNION (SELECT f.id, 10002 AS type, CASE WHEN f.channel_id = 0 THEN f.node_id ELSE f.channel_id END channel_id, f.is_master, f.mc_exclude, fd.channel_id disinherited_channel FROM folder f LEFT JOIN folder_disinherit fd ON f.id = fd.folder_id WHERE f.mother = ? " + folderDeleteClause + ") "
						+ "UNION (SELECT c.id, 10008 AS type, CASE WHEN c.channel_id = 0 THEN cf.node_id ELSE c.channel_id END channel_id, c.is_master, c.mc_exclude, cd.channel_id disinherited_channel FROM contentfile c LEFT JOIN contentfile_disinherit cd ON c.id = cd.contentfile_id JOIN folder cf ON c.folder_id = cf.id WHERE c.folder_id = ? " + fileDeleteClause + ")",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						for (int i = 1; i <= 3; ++i) {
							stmt.setInt(i, folder.getId());
						}
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							Integer disinheritedChnanelId = rs.getInt("disinherited_channel");
							if (rs.wasNull()) {
								disinheritedChnanelId = null;
							}
							DisinheritableObjectReference entry = new DisinheritableObjectReference(rs.getInt("id"), rs.getInt("type"),
									rs.getInt("channel_id"), rs.getBoolean("is_master"));
							if (!result.containsKey(entry)) {
								result.put(entry, new DisinheritableObjectData(rs.getBoolean("mc_exclude")));
							}
							if (disinheritedChnanelId != null) {
								result.get(entry).addDisinheritedChannelId(disinheritedChnanelId);
							}
						}
					}
				});
		return result;
	}

	/**
	 * Checks whether changes in the excluded flag and the disinherited channel
	 * set conserve consistency.
	 *
	 * @param object
	 *            the disinheritable to check
	 * @param skipRecursiveChecks
	 *            iff true, checks on subfolders are not performed. Used during
	 *            recursive disinheriting/excluding.
	 *            <em>Use only when the checks have already been performed!</em>
	 * @throws ChannelInheritanceException
	 *             when the object is not in a consistent state with the channel
	 *             tree
	 * @throws NodeException
	 */
	public static <T> void checkChangeConsistency(DisinheritableInternal<T> object, boolean excluded, Set<Node> disinheritedNodes, boolean skipRecursiveChecks) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (!object.isMaster()) {
			return;
		}
		Folder folder = null;
		Node objectChannel = object.getChannel();

		if (objectChannel == null ) {
			objectChannel = object.getOwningNode();
		}

		int objectChannelId = objectChannel.getId();

		if (disinheritedNodes.contains(objectChannel)) {
			throw new ChannelInheritanceException("Can't disinherit an object's master's channel");
		}

		Folder parentFolder = (Folder) object.getParentObject();
		if (object instanceof Folder) {
			folder = (Folder) object;
		}
		boolean newlyExcluded = excluded && Boolean.FALSE.equals(object.isExcluded());
		boolean reincluded = !excluded && Boolean.TRUE.equals(object.isExcluded());

		ChannelTreeSegment originalSegment = new ChannelTreeSegment(object, true);
		ChannelTreeSegment newSegment = new ChannelTreeSegment(objectChannel, excluded,  disinheritedNodes);

		@SuppressWarnings("unchecked")
		Set<Node> netDisinheritedChannels = new HashSet<>(ListUtils.removeAll(originalSegment.getAllNodes(), newSegment.getAllNodes()));
		@SuppressWarnings("unchecked")
		Set<Node> netReinheritedChannels = new HashSet<>(ListUtils.removeAll(newSegment.getAllNodes(), originalSegment.getAllNodes()));
		Set<Integer> netDisinheritedChannelIds = new HashSet<>();
		Set<Integer> netReinheritedChannelIds = new HashSet<>();
		for (Node node : netDisinheritedChannels) {
			netDisinheritedChannelIds.add(node.getId());
		}
		for (Node node : netReinheritedChannels) {
			netReinheritedChannelIds.add(node.getId());
		}

		Map<DisinheritableObjectReference, DisinheritableObjectData> potentialObstructors = Collections.emptyMap();
		if ((newlyExcluded || !netDisinheritedChannelIds.isEmpty()) && folder != null) {
			potentialObstructors = getChannelIndependentFolderChildren(folder);
		}

		if (newlyExcluded) {
			if (object.getChannelSet().size() > 1) {
				Node channel = null;
				for (int channelId : object.getChannelSet().keySet()) {
					if (channelId != 0) {
						channel = t.getObject(Node.class, channelId, -1, false);
					}
					if (channel != null) {
						break;
					}
				}
				throw new ChannelInheritanceException("Cannot exclude object " + object + " from multichannelling because it has an obstructing localization",
						"disinherit.exclude.localization", Arrays.asList(I18NHelper.getName(object), I18NHelper.getName(channel)));
			}
			if (!skipRecursiveChecks && folder != null) {
				for (Entry<DisinheritableObjectReference, DisinheritableObjectData> entry : potentialObstructors.entrySet()) {
					if (entry.getKey().getChannelId() != objectChannelId) {
						throw new ChannelInheritanceException("Cannot exclude folder " + folder + " from multichannelling because it is obstructed by "
								+ entry.getKey(), "disinherit.exclude.obstructedby." + entry.getKey().getType(), Arrays.asList(I18NHelper.getName(folder),
								I18NHelper.getPath(entry.getKey().getObject())));
					}
				}
			}
		}
		if (reincluded) {
			if (parentFolder.isExcluded()) {
				throw new ChannelInheritanceException("Cannot include object " + object + " into multichannelling, because parent folder " + parentFolder
						+ " is excluded", "disinherit.include.parent.excluded", Arrays.asList(I18NHelper.getName(object), I18NHelper.getPath(parentFolder)));
			}
		}
		if (!netDisinheritedChannelIds.isEmpty()) {
			if (!excluded) {
				Set<Integer> channelSetNodeIds = object.getChannelSet().keySet();
				channelSetNodeIds.remove(0);
				List<Node> channelSetNodes = t.getObjects(Node.class, channelSetNodeIds);
				for (Node n : channelSetNodes) {
					if (!newSegment.contains(n)) {
						NodeObject obstruction = t.getObject(object.getObjectInfo().getObjectClass(), object.getChannelSet().get(n.getId()), -1, false);
						throw new ChannelInheritanceException("Cannot disinherit a channel for object " + object
								+ ", because it is obstructed by a localization in " + n, "disinherit.obstructedby.localized", Arrays.asList(
								I18NHelper.getName(object), I18NHelper.getName(n), I18NHelper.getPath(obstruction)));
					}
				}
			}
			for (Entry<DisinheritableObjectReference, DisinheritableObjectData> entry : potentialObstructors.entrySet()) {
				DisinheritableObjectReference childObj = entry.getKey();
				if (netDisinheritedChannelIds.contains(childObj.getChannelId())) {
					throw new ChannelInheritanceException("Object " + object + " cannot disinherit a channel because it is obstructed by " + childObj,
							"disinherit.obstructedby.localized", Arrays.asList(I18NHelper.getName(object),
									I18NHelper.getName(t.getObject(Node.class, childObj.getChannelId())), I18NHelper.getPath(childObj.getObject())));
				}
				if (!skipRecursiveChecks && childObj.isMaster() && childObj.getType() == Folder.TYPE_FOLDER) {
					Folder nextFolder = t.getObject(Folder.class, childObj.getId(), -1, false);
					ChannelTreeSegment nextSegment = new ChannelTreeSegment(nextFolder, true);
					nextSegment = nextSegment.addRestrictions(excluded, disinheritedNodes);
					@SuppressWarnings("unchecked")
					DisinheritableInternal<T> nextDisinheritableInternal = (DisinheritableInternal<T>) nextFolder;
					checkChangeConsistency(nextDisinheritableInternal, nextSegment.isExcluded(), nextSegment.getRestrictions(), false); 
				}
			}
		}
		if (!netReinheritedChannelIds.isEmpty()) {
			// parent folder must be visible in all net reinherited channels
			for (Node n : netReinheritedChannels) {
				if (MultichannellingFactory.getChannelVariant(parentFolder, n) == null) {
					throw new ChannelInheritanceException("Can't reinherit channel " + n + " for object " + object + " because parent folder " + parentFolder
							+ " is not visible there", "reinherit.folder.invisible", Arrays.asList(I18NHelper.getName(object), I18NHelper.getName(n), I18NHelper.getPath(parentFolder)));
				}
			}

			// check for (file-) name collisions
			for (NodeObject v : t.getObjects(object.getObjectInfo().getObjectClass(), object.getChannelSet().values(), false, false)) {
				ChannelTreeSegment localSegment = new ChannelTreeSegment((Disinheritable<?>)v, newSegment);
				if (object instanceof ContentFile || object instanceof Page) {
					Set<Folder> pcf = DisinheritUtils.getFoldersWithPotentialObstructors(parentFolder, localSegment);
					NodeObject hinderingObject = getObjectUsingFilename((Disinheritable<?>)v, pcf, localSegment);
					if (hinderingObject != null) {
						throw new ChannelInheritanceException("Cannot reinclude/reinherit object " + object
								+ " because an object with the same file name already exists in a folder publishing to the same pubdir",
								"disinherit.reinclude.filenamecollision", Arrays.asList(I18NHelper.getName(object), I18NHelper.getPath(hinderingObject)));
					}
				}
				if (object instanceof Page || object instanceof Folder) {
					NodeObject hinderingObject = getObjectUsingName((Disinheritable<NodeObject>)v, localSegment);
					if (hinderingObject != null) {
						throw new ChannelInheritanceException("Cannot reinclude/reinherit object " + object
								+ " because an object with the same name already exists in its folder", "disinherit.reinclude.namecollision",
								Arrays.asList(I18NHelper.getName(object), I18NHelper.getPath(hinderingObject)));
					}
				}
				if (object instanceof Folder) {
					Folder f = (Folder)object;
					if (f.getNode().isPubDirSegment()) {
						NodeObject hinderingObject = getObjectUsingProperty(f, o -> o.getObject().getPublishDir(), "pub_dir", localSegment);
						if (hinderingObject != null) {
							throw new ChannelInheritanceException("Cannot reinclude/reinherit object " + object
									+ " because an object with the same pub_dir already exists in its folder", "disinherit.reinclude.pub_dir_collision",
									Arrays.asList(I18NHelper.getName(object), I18NHelper.getPath(hinderingObject)));
						}
					}
				}
			}
		}
	}

	/**
	 * Checks whether an object can be created with the current channel
	 * inheritance structure.
	 *
	 * @param object
	 *            the disinheritable to check
	 * @param excluded
	 *            wether the object should be excluded (data from master object
	 *            required for localizations)
	 * @param disinheritedChannels
	 *            the channels that should be disinherited on the object (data
	 *            from master object required for localizations)
	 * @throws ChannelInheritanceException
	 *             when the object is not in a consistent state with the channel
	 *             tree
	 * @throws NodeException
	 */
	public static <T> void checkCreationConsistency(DisinheritableInternal<T> object, boolean excluded, Set<Node> disinheritedChannels) throws NodeException {
		Node channel = object.getChannel();
		if (!object.isMaster()) {
			Disinheritable<T> masterObject = (Disinheritable<T>) object.getMaster();
			if (masterObject.isExcluded()) {
				throw new ChannelInheritanceException("Can't create localized object of excluded object {" + masterObject + "}");
			}
			boolean disinherited = disinheritedChannels.contains(channel)
					|| !CollectionUtils.intersection(disinheritedChannels, channel.getMasterNodes()).isEmpty();
			if (disinherited) {
				throw new ChannelInheritanceException("Can't create localized object " + object + "in disinherited channel " + channel);
			}
		} else if (channel != null) {
			Disinheritable<?> container;
			try (NoMcTrx nmt = new NoMcTrx()) {
				container = (Disinheritable<?>) ((Disinheritable<?>) object.getParentObject()).getMaster();
			}

			if (container.isExcluded() && !excluded) {
				throw new ChannelInheritanceException("Can't create object " + object + " in channel " + channel + ", because its container object "
						+ container + "is excluded from multichannelling");
			}

			boolean parentDisinherited = container.getDisinheritedChannels().contains(channel)
					|| !CollectionUtils.intersection(container.getDisinheritedChannels(), channel.getMasterNodes()).isEmpty();
			if (parentDisinherited) {
				throw new ChannelInheritanceException("Can't create object " + object + " in " + container + " because it disinherits the channel " + channel);
			}
		}
	}

	/**
	 * Checks if the object's name is available. Works for folders and pages.
	 * @param checkObject the disinheritable to check
	 * @param objectSegment object segment
	 * @return true iff the name can be used on the specified object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> boolean isObjectnameAvailable(Disinheritable<T> checkObject, ChannelTreeSegment objectSegment) throws NodeException {
		return getObjectUsingName(checkObject, objectSegment) == null;
	}

	/**
	 * Checks if the object's property is available.
	 * @param checkObject the disinheritable to check
	 * @param propertyFunction function that extracts the property to check
	 * @param propertyName name of the property
	 * @param objectSegment object segment
	 * @return true iff the property can be used on the specified object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> boolean isPropertyAvailable(Disinheritable<T> checkObject, Function<Disinheritable<T>, String> propertyFunction, String propertyName, ChannelTreeSegment objectSegment) throws NodeException {
		return getObjectUsingProperty(checkObject, propertyFunction, propertyName, objectSegment) == null;
	}

	/**
	 * Get object, that is hindering the given object to use it's name in the given object segment or null if the name is available
	 * @param checkObject the disinheritable to check
	 * @param objectSegment object segment
	 * @return null iff the name can be used on the specified object or the object, that is in the way
	 * @throws NodeException
	 */
	public static <T extends NodeObject> NodeObject getObjectUsingName(Disinheritable<T> checkObject, ChannelTreeSegment objectSegment) throws NodeException {
		return getObjectUsingProperty(checkObject, LocalizableNodeObject::getName, "name", objectSegment);
	}

	/**
	 * Get object, that is hindering the given object to use a property in the given object segment or null if the property is available
	 * @param checkObject the disinheritable to check
	 * @param propertyFunction function that extracts the property to check
	 * @param propertyName name of the property
	 * @param objectSegment object segment
	 * @return null iff the property can be used on the specified object or the object, that is in the way
	 * @throws NodeException
	 */
	public static <T extends NodeObject> NodeObject getObjectUsingProperty(Disinheritable<T> checkObject,
			Function<Disinheritable<T>, String> propertyFunction, String propertyName, ChannelTreeSegment objectSegment) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder masterFolder;
		try (NoMcTrx nmt = new NoMcTrx()) {
			if (checkObject.getParentObject() == null) {
				return null;
			}
			masterFolder = ((Folder)checkObject.getParentObject()).getMaster();
		}
		Class<? extends NodeObject> clazz = checkObject.getObjectInfo().getObjectClass();

		Set<Node> relevantNodes = new HashSet<>();
		// ChannelTreeSegment objectSegment = new ChannelTreeSegment(checkObject, false);
		relevantNodes.addAll(objectSegment.getAllNodes());
		relevantNodes.addAll(objectSegment.getStartChannel().getMasterNodes());

		ArrayList<Object> parameters = new ArrayList<>();
		for (Node n : relevantNodes) {
			parameters.add(n.getId());
		}
		parameters.add(0);
		parameters.addAll(parameters);
		parameters.add(checkObject.getChannelSetId());
		parameters.add(propertyFunction.apply(checkObject));
		parameters.add(masterFolder.getId());

		String tableName = t.getTable(checkObject.getObjectInfo().getObjectClass());
		String folderIdField = checkObject instanceof Folder ? "mother" : "folder_id";
		String getObjectsSameNameSameFolderSQL = "SELECT o2.id, o2.channel_id, o2.channelset_id, o2.mc_exclude, od.channel_id disinherited_channel " +
				"FROM " + tableName + " o1 " +
				"JOIN " + tableName + " o2 ON o2.channelset_id = o1.channelset_id " +
				"LEFT JOIN " + tableName + "_disinherit od ON od." + tableName + "_id = o2.id " +
				"WHERE o1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
				"AND o2.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
				"AND o1.channelset_id != ? and o1." + propertyName + " = ? AND o1." + folderIdField + " = ? AND o1.deleted = 0 AND o2.deleted = 0";
		if (checkObject instanceof Page) {
			Integer contentsetId = ((Page)checkObject).getContentsetId();
			if (ObjectTransformer.getInteger(contentsetId, 0) != 0) {
				getObjectsSameNameSameFolderSQL += " AND (o1.contentset_id != ? OR o1.contentset_id = 0)";
				parameters.add(contentsetId);
			}
		}

		Object[] parameterArray = parameters.toArray();

		Map<Node, MultiChannellingFallbackList> objectFallbacks = MultichannellingFactory.performMultichannelMultichannellingFallback(getObjectsSameNameSameFolderSQL,
				parameterArray, objectSegment.getAllNodes());

		for (Entry<Node, MultiChannellingFallbackList> entry : objectFallbacks.entrySet()) {
			List<? extends NodeObject> objects;
			objects = t.getObjects(clazz, entry.getValue().getObjectIds(), false, false);
			for (NodeObject f: objects) {
				if (propertyFunction.apply((Disinheritable<T>)f).equals(propertyFunction.apply(checkObject))) {
					return f;
				}
			}
		}
		return null;
	}

	/**
	 * Checks if the filename can be used by the specified object. If the
	 * object's folder or folders that can potentially contain conflicting
	 * filenames already contain an object with the given filename, the name
	 * cannot be used. Works on pages and images/files.
	 *
	 * @param checkObject
	 *            the object bearing the filename to check
	 * @param potentialConflictingFolders
	 *            folder localizations that share the same publishdir with the
	 *            target folder in at least one channel. Must not be empty (must
	 *            at least contain the object's folder).
	 * @return true iff the object can use its name (none of the given folders
	 *         contain a conflicting filename in the scope of checkedObject)
	 * @throws NodeException
	 */
	public static boolean isFilenameAvailable(Disinheritable<?> checkObject, Set<Folder> potentialConflictingFolders) throws NodeException {
		return isFilenameAvailable(checkObject, potentialConflictingFolders, null);
	}

	/**
	 * Checks if the filename can be used by the specified object. If the
	 * object's folder or folders that can potentially contain conflicting
	 * filenames already contain an object with the given filename, the name
	 * cannot be used. Works on pages and images/files.
	 *
	 * @param checkObject
	 *            the object bearing the filename to check
	 * @param potentialConflictingFolders
	 *            folder localizations that share the same publishdir with the
	 *            target folder in at least one channel. Must not be empty (must
	 *            at least contain the object's folder).
	 * @param overrideRestrictions
	 *            if not null, these multichannelling restrictions are used
	 *            instead of checkObject's
	 * @return true iff the object can use its name (none of the given folders
	 *         contain a conflicting filename in the scope of checkedObject)
	 * @throws NodeException
	 */
	public static boolean isFilenameAvailable(Disinheritable<?> checkObject, Set<Folder> potentialConflictingFolders, ChannelTreeSegment overrideRestrictions) throws NodeException {
		return getObjectUsingFilename(checkObject, potentialConflictingFolders, overrideRestrictions) == null;
	}

	/**
	 * Get the first object found, which uses the same publish URL as the given object.
	 * The object is identified by its channelSetId and the publish URL to check for with functions that extract the path and name portions of the URL
	 *
	 * @param checkObject checked object
	 * @param path function for the path portion of the URL for the given node. The path must always begin and end with a slash
	 * @param name supplier for the name portion of the URL
	 * @param potentialConflictingFolders set of folders that may contain conflicting objects
	 * @param objectSegment visibility segment to check in
	 * @return first conflicting object or null
	 * @throws NodeException
	 */
	public static Disinheritable<?> getObjectUsingURL(Disinheritable<?> checkObject, Function<Node, String> path,
			Supplier<String> name, Set<Folder> potentialConflictingFolders, ChannelTreeSegment objectSegment) throws NodeException {
		int channelSetId = checkObject.getChannelSetId();
		Transaction t = TransactionManager.getCurrentTransaction();
		String filename = name.supply();

		// prepare all different publish URLs of the object (in all channels, where it is inherited) together with the Set of nodes
		// in which the publish URL is valid
		Map<String, Set<Node>> publishedUrls = new HashMap<>();
		for (Node node : objectSegment.getAllNodes()) {
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				String url = String.format("%s%s", path.apply(node), filename);
				publishedUrls.computeIfAbsent(url, key -> new HashSet<>()).add(node);
			}
		}

		if (!potentialConflictingFolders.isEmpty()) {
			// TODO: Could be moved to Folder()
			Set<Folder> masterFolders = new HashSet<>(); // maybe pass as a parameter?
			Set<ChannelTreeSegment> segments = new HashSet<>();  // maybe pass as a parameter?

			for (Folder pcf : potentialConflictingFolders) {
				masterFolders.add(pcf.getMaster());
				segments.add(new ChannelTreeSegment(pcf, false));
			}

			Set<Node> relevantNodes = new HashSet<>();
			relevantNodes.addAll(objectSegment.getAllNodes());
			relevantNodes.addAll(objectSegment.getStartChannel().getMasterNodes());

			ArrayList<Object> parameters = new ArrayList<>();
			for (Node n : relevantNodes) {
				parameters.add(n.getId());
			}
			parameters.add(0);
			parameters.addAll(parameters);

			for (Folder f : masterFolders) {
				parameters.add(f.getId());
			}
			parameters.add(filename);
			parameters.add(channelSetId);

			Object[] parameterArray = parameters.toArray();

			String selectFilesByNameSQL = "SELECT DISTINCT f2.id, f2.channelset_id, f2.channel_id, f2.mc_exclude, cfd.channel_id disinherited_channel " +
					"FROM contentfile f1 " +
					"JOIN contentfile f2 ON f2.channelset_id = f1.channelset_id " +
					"LEFT JOIN contentfile_disinherit cfd ON cfd.contentfile_id = f2.id " +
					"WHERE f1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
					"AND f2.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
					"AND f1.folder_id in " + DBUtils.makeSqlPlaceHolders(masterFolders.size(), "")+ " " +
					"AND f1.name = ? AND f1.channelset_id != ? AND f1.deleted = 0 AND f2.deleted = 0" ;

			Map<Node, MultiChannellingFallbackList> fileFallbacks = MultichannellingFactory.performMultichannelMultichannellingFallback(selectFilesByNameSQL, parameterArray, objectSegment.getAllNodes());

			for (Entry<Node, MultiChannellingFallbackList> entry : fileFallbacks.entrySet()) {
				List<File> files;
				files = t.getObjects(File.class, entry.getValue().getObjectIds(), false, false);

				Disinheritable<?> conflictingObject = checkEqualUrls(files, publishedUrls);
				if (conflictingObject != null) {
					return conflictingObject;
				}
			}

			String selectPagesFromVersionsSQL = "SELECT DISTINCT p.id FROM page p LEFT JOIN page_nodeversion pn ON p.id = pn.id WHERE (pn.filename = ? OR p.filename = ?) AND p.deleted = 0 AND p.folder_id IN "
					+ DBUtils.makeSqlPlaceHolders(masterFolders.size(), "");

			Set<Integer> pageIds = DBUtils.select(selectPagesFromVersionsSQL, rs -> {
				int index = 1;
				rs.setString(index++, filename);
				rs.setString(index++, filename);
				for (Folder f : masterFolders) {
					rs.setInt(index++, f.getId());
				}
			}, DBUtils.IDS);

			if (!pageIds.isEmpty()) {
				String selectPagesByFilenameSQL = "SELECT DISTINCT p2.id, p2.channelset_id, p2.channel_id, p2.mc_exclude, pd.channel_id disinherited_channel " +
						"FROM page p1 " +
						"JOIN page p2 ON p2.channelset_id = p1.channelset_id " +
						"LEFT JOIN page_disinherit pd ON pd.page_id = p2.id " +
						"WHERE p1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
						"AND p2.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
						"AND p1.id IN " + DBUtils.makeSqlPlaceHolders(pageIds.size(), "") + " AND p1.channelset_id != ? AND p1.deleted = 0 AND p2.deleted = 0";

				List<Integer> nodeIds = relevantNodes.stream().map(Node::getId).collect(Collectors.toList());
				Map<Node, MultiChannellingFallbackList> pageFallbacks = MultichannellingFactory.performMultichannelMultichannellingFallback(
						selectPagesByFilenameSQL, objectSegment.getAllNodes(), nodeIds, 0, nodeIds, 0, pageIds, channelSetId);

				for (Entry<Node, MultiChannellingFallbackList> entry : pageFallbacks.entrySet()) {
					List<Page> pages = t.getObjects(Page.class, entry.getValue().getObjectIds(), false, false);

					try (FeatureClosure noPublishCache = new FeatureClosure(Feature.PUBLISH_CACHE, false)) {
						// for all online pages, add the published version also
						List<Page> pagesWithPublished = Flowable.fromIterable(pages).flatMap(p -> {
							if (p.isOnline()) {
								Page published = p.getPublishedObject();
								if (published != null) {
									return Flowable.fromArray(p, published);
								}
							}
							return Flowable.fromArray(p);
						}).toList().blockingGet();

						Disinheritable<?> conflictingObject = checkEqualUrls(pagesWithPublished, publishedUrls);
						if (conflictingObject != null) {
							return conflictingObject;
						}
					}
				}
			}
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			// now check for pages with nice URLs identical to folder.pub_dir + object.filename of this object

			Map<String, Set<Node>> potentialPageNiceUrls = new HashMap<>();
			Map<String, Set<Node>> potentialFileNiceUrls = new HashMap<>();
			for (Map.Entry<String, Set<Node>> entry : publishedUrls.entrySet()) {
				String fullUrl = entry.getKey();
				Set<Node> nodes = entry.getValue();
				for (Node n : nodes) {
					String niceUrl = fullUrl;
					if (!StringUtils.equals(n.getPublishDir(), "/")) {
						niceUrl = StringUtils.removeStart(niceUrl, n.getPublishDir());
					}
					potentialPageNiceUrls.computeIfAbsent(niceUrl, key -> new HashSet<>()).add(n);

					niceUrl = fullUrl;
					if (!StringUtils.equals(n.getBinaryPublishDir(), "/")) {
						niceUrl = StringUtils.removeStart(niceUrl, n.getBinaryPublishDir());
					}
					potentialFileNiceUrls.computeIfAbsent(niceUrl, key -> new HashSet<>()).add(n);
				}
			}

			// select all pages that have possibly conflicting nice URLs or alternate URLs
			String select = "SELECT DISTINCT page.id FROM page LEFT JOIN page_alt_url ON page.id = page_alt_url.page_id WHERE (page.nice_url = ? OR page_alt_url.url = ?) AND page.channelset_id != ? AND page.deleted = 0";
			for (Map.Entry<String, Set<Node>> entry : potentialPageNiceUrls.entrySet()) {
				List<Page> pages = t.getObjects(Page.class, DBUtils.select(select, ps -> {
					ps.setString(1, entry.getKey());
					ps.setString(2, entry.getKey());
					ps.setInt(3, channelSetId);
				}, DBUtils.IDS), false, false);

				// filter out pages that are not visible in any of the nodes
				for (Page p : pages) {
					for (Node n : entry.getValue()) {
						if (MultichannellingFactory.isVisibleInNode(n, p)) {
							return p;
						}
					}
				}
			}

			// select all files that have possibly conflicting nice URLs or alternate URLs
			select = "SELECT DISTINCT contentfile.id FROM contentfile LEFT JOIN contentfile_alt_url ON contentfile.id = contentfile_alt_url.contentfile_id WHERE (contentfile.nice_url = ? OR contentfile_alt_url.url = ?) AND contentfile.channelset_id != ? AND contentfile.deleted = 0";
			for (Map.Entry<String, Set<Node>> entry : potentialFileNiceUrls.entrySet()) {
				List<File> files = t.getObjects(File.class, DBUtils.select(select, ps -> {
					ps.setString(1, entry.getKey());
					ps.setString(2, entry.getKey());
					ps.setInt(3, channelSetId);
				}, DBUtils.IDS), false, false);

				// filter out files that are not visible in any of the nodes
				for (File f : files) {
					for (Node n : entry.getValue()) {
						if (MultichannellingFactory.isVisibleInNode(n, f)) {
							return f;
						}
					}
				}
			}

			// select pages in other versions that have possibly conflicting nice URLs or alternate URLs
			select = "SELECT id FROM page_nodeversion WHERE nice_url = ?";
			try (FeatureClosure noPublishCache = new FeatureClosure(Feature.PUBLISH_CACHE, false)) {
				for (Map.Entry<String, Set<Node>> entry : potentialPageNiceUrls.entrySet()) {
					Set<Integer> pageIds = new HashSet<>();
					// select for nice_url
					pageIds.addAll(DBUtils.select("SELECT DISTINCT id FROM page_nodeversion WHERE nice_url = ?", ps -> {
						ps.setString(1, entry.getKey());
					}, DBUtils.IDS));
					// select for alternate URLs
					pageIds.addAll(DBUtils
							.select("SELECT DISTINCT page_id id FROM page_alt_url_nodeversion WHERE url = ?", ps -> {
								ps.setString(1, entry.getKey());
							}, DBUtils.IDS));

					AtomicReference<Page> found = new AtomicReference<>();
					MiscUtils.doBuffered(pageIds, 100, idList -> {
						List<Page> pages = t.getObjects(Page.class, idList, false, false);

						// filter out pages that are not visible in any of the nodes
						for (Page p : pages) {
							if (ObjectTransformer.getInt(p.getChannelSetId(), 0) == channelSetId) {
								continue;
							}
							p = p.getPublishedObject();
							if (p == null) {
								continue;
							}
							if (p.isDeleted() || !p.isOnline()) {
								continue;
							}
							if (!p.getNiceAndAlternateUrls().contains(entry.getKey())) {
								continue;
							}
							for (Node n : entry.getValue()) {
								if (MultichannellingFactory.isVisibleInNode(n, p)) {
									found.set(p);
									return false;
								}
							}
						}

						return true;
					});

					if (found.get() != null) {
						return found.get();
					}
				}
			}
		}

		return null;
	}

	/**
	 * Checks if the filename can be used by the specified object. If the
	 * object's folder or folders that can potentially contain conflicting
	 * filenames already contain an object with the given filename, the name
	 * cannot be used. Works on pages and images/files.
	 *
	 * @param checkObject
	 *            the object bearing the filename to check
	 * @param potentialConflictingFolders
	 *            folder localizations that share the same publishdir with the
	 *            target folder in at least one channel. Must not be empty (must
	 *            at least contain the object's folder).
	 * @param overrideRestrictions
	 *            if not null, these multichannelling restrictions are used
	 *            instead of checkObject's
	 * @return null iff the object can use its name (none of the given folders
	 *         contain a conflicting filename in the scope of checkedObject) or a conflicting object instead
	 * @throws NodeException
	 */
	public static Disinheritable<?> getObjectUsingFilename(Disinheritable<?> checkObject, Set<Folder> potentialConflictingFolders, ChannelTreeSegment overrideRestrictions) throws NodeException {
		return getObjectUsingURL(checkObject, node -> {
			return checkObject.getFullPublishPath(true);
		}, () -> checkObject.getFilename(), potentialConflictingFolders,
				overrideRestrictions != null ? overrideRestrictions : new ChannelTreeSegment(checkObject, false));
	}

	/**
	 * Same check as {@link #getObjectUsingFilename(Disinheritable, Set, ChannelTreeSegment)}, but for the nice URL
	 *
	 * @param object the object to check
	 * @param niceUrl nice URL to check
	 * @param potentialConflictingFolders folders potentially containing conflicting objects
	 * @param overrideRestrictions
	 *            if not null, these multichannelling restrictions are used
	 *            instead of checkObject's
	 * @return null iff the object can use its nice URL (none of the given folders
	 *         contain a conflicting filename in the scope of checkedObject) or a conflicting object instead
	 * @throws NodeException
	 */
	public static NodeObject getObjectUsingNiceURL(Disinheritable<?> object, String niceUrl, Set<Folder> potentialConflictingFolders, ChannelTreeSegment overrideRestrictions)
			throws NodeException {
		return getObjectUsingURL(object, node -> {
			return NodeObjectWithAlternateUrls.PATH.apply(FilePublisher.getPath(true, false, node.getPublishDir(), niceUrl));
		}, () -> NodeObjectWithAlternateUrls.NAME.apply(niceUrl), potentialConflictingFolders,
				overrideRestrictions != null ? overrideRestrictions : new ChannelTreeSegment(object, false));
	}

	/**
	 * Get the set of filenames which can not be used for the given object
	 * @param checkObject object to check
	 * @param filenamePatternStr filename pattern (<strong>note:</strong> the
	 *            pattern will be used as a Java regular regular expression
	 *            <em>and</em> a SQL regular expression)
	 * @param potentialConflictingFolders
	 *            folder localizations that share the same publishdir with the
	 *            target folder in at least one channel. Must not be empty (must
	 *            at least contain the object's folder).
	 * @param overrideRestrictions
	 *            if not null, these multichannelling restrictions are used
	 *            instead of checkObject's
	 * @return set of filenames (empty if no conflicts are found) all made lowercase
	 * @throws NodeException
	 */
	public static Set<String> getUsedFilenames(Disinheritable<?> checkObject, String filenamePatternStr, Set<Folder> potentialConflictingFolders,
			ChannelTreeSegment overrideRestrictions) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Folder> masterFolders = new HashSet<>(); // maybe pass as a parameter?
		Set<ChannelTreeSegment> segments = new HashSet<>();  // maybe pass as a parameter?

		for (Folder pcf : potentialConflictingFolders) {
			masterFolders.add(pcf.getMaster());
			segments.add(new ChannelTreeSegment(pcf, false));
		}

		Set<Node> relevantNodes = new HashSet<>();
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(checkObject, false);
		if (overrideRestrictions != null) {
			objectSegment = overrideRestrictions;
		}
		relevantNodes.addAll(objectSegment.getAllNodes());
		relevantNodes.addAll(objectSegment.getStartChannel().getMasterNodes());

		ArrayList<Object> parameters = new ArrayList<>();
		for (Node n : relevantNodes) {
			parameters.add(n.getId());
		}
		parameters.add(0);
		parameters.addAll(parameters);

		for (Folder f : masterFolders) {
			parameters.add(f.getId());
		}
		parameters.add(filenamePatternStr);
		parameters.add(checkObject.getChannelSetId());

		Object[] parameterArray = parameters.toArray();

		String selectFilesByNameSQL = "SELECT DISTINCT f2.id, f2.channelset_id, f2.channel_id, f2.mc_exclude, cfd.channel_id disinherited_channel " +
				"FROM contentfile f1 " +
				"JOIN contentfile f2 ON f2.channelset_id = f1.channelset_id " +
				"LEFT JOIN contentfile_disinherit cfd ON cfd.contentfile_id = f2.id " +
				"WHERE f1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
				"AND f2.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
				"AND f1.folder_id in " + DBUtils.makeSqlPlaceHolders(masterFolders.size(), "")+ " " +
				"AND f1.name REGEXP ? AND f1.channelset_id != ? AND f1.deleted = 0 AND f2.deleted = 0" ;

		// prepare all different publish URLs of the object (in all channels, where it is inherited) together with the Set of nodes
		// in which the publish URL is valid
		Map<Pattern, Set<Node>> publishUrlsPatterns = new HashMap<>();
		Map<String, Set<Node>> potentialPageNiceUrlPatterns = new HashMap<>();
		Map<String, Set<Node>> potentialFileNiceUrlPatterns = new HashMap<>();
		for (Node node : objectSegment.getAllNodes()) {
			try (ChannelTrx cTrx = new ChannelTrx(node); HandleDependenciesTrx hTrx = new HandleDependenciesTrx(false)) {
				Pattern urlPattern = Pattern.compile(String.format("%s%s", CNStringUtils.escapeRegex(checkObject.getFullPublishPath(true)), filenamePatternStr), Pattern.CASE_INSENSITIVE);
				publishUrlsPatterns.computeIfAbsent(urlPattern, key -> new HashSet<>()).add(node);

				// check nice URL pattern for pages
				String niceUrlPattern = urlPattern.pattern();
				String publishDir = node.getPublishDir();
				if (!StringUtils.equals(publishDir, "/")) {
					niceUrlPattern = StringUtils.removeStart(niceUrlPattern, CNStringUtils.escapeRegex(publishDir));
				}
				potentialPageNiceUrlPatterns.computeIfAbsent(niceUrlPattern, key -> new HashSet<>()).add(node);

				// check nice URL pattern for files
				niceUrlPattern = urlPattern.pattern();
				String binaryPublishDir = node.getBinaryPublishDir();
				if (!StringUtils.equals(binaryPublishDir, "/")) {
					niceUrlPattern = StringUtils.removeStart(niceUrlPattern, CNStringUtils.escapeRegex(binaryPublishDir));
				}
				potentialFileNiceUrlPatterns.computeIfAbsent(niceUrlPattern, key -> new HashSet<>()).add(node);
			}
		}

		Map<Node, MultiChannellingFallbackList> fileFallbacks = MultichannellingFactory.performMultichannelMultichannellingFallback(selectFilesByNameSQL, parameterArray, objectSegment.getAllNodes());
		Set<String> filenameSet = new HashSet<>();

		for (Entry<Node, MultiChannellingFallbackList> entry : fileFallbacks.entrySet()) {
			List<File> files = t.getObjects(File.class, entry.getValue().getObjectIds(), false, false);
			filenameSet.addAll(checkMatchingUrls(files, publishUrlsPatterns));
		}

		String selectPagesFromVersionsSQL = "SELECT DISTINCT p.id FROM page p LEFT JOIN page_nodeversion pn ON p.id = pn.id WHERE (pn.filename REGEXP ? OR p.filename REGEXP ?) AND p.deleted = 0 AND p.folder_id IN "
				+ DBUtils.makeSqlPlaceHolders(masterFolders.size(), "");

		Set<Integer> pageIds = DBUtils.select(selectPagesFromVersionsSQL, rs -> {
			int index = 1;
			rs.setString(index++, filenamePatternStr);
			rs.setString(index++, filenamePatternStr);
			for (Folder f : masterFolders) {
				rs.setInt(index++, f.getId());
			}
		}, DBUtils.IDS);

		if (!pageIds.isEmpty()) {
			String selectPagesByFilenameSQL = "SELECT DISTINCT p2.id, p2.channelset_id, p2.channel_id, p2.mc_exclude, pd.channel_id disinherited_channel " +
					"FROM page p1 " +
					"JOIN page p2 ON p2.channelset_id = p1.channelset_id " +
					"LEFT JOIN page_disinherit pd ON pd.page_id = p2.id " +
					"WHERE p1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
					"AND p2.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
					"AND p1.id IN " + DBUtils.makeSqlPlaceHolders(pageIds.size(), "") + " AND p1.channelset_id != ? AND p1.deleted = 0 AND p2.deleted = 0";

			List<Integer> nodeIds = relevantNodes.stream().map(Node::getId).collect(Collectors.toList());
			Map<Node, MultiChannellingFallbackList> pageFallbacks = MultichannellingFactory.performMultichannelMultichannellingFallback(
					selectPagesByFilenameSQL, objectSegment.getAllNodes(), nodeIds, 0, nodeIds, 0, pageIds, checkObject.getChannelSetId());

			for (Entry<Node, MultiChannellingFallbackList> entry : pageFallbacks.entrySet()) {
				List<Page> pages = t.getObjects(Page.class, entry.getValue().getObjectIds(), false, false);

				try (FeatureClosure noPublishCache = new FeatureClosure(Feature.PUBLISH_CACHE, false)) {
					// for all online pages, add the published version also
					List<Page> pagesWithPublished = Flowable.fromIterable(pages).flatMap(p -> {
						if (p.isOnline()) {
							Page published = p.getPublishedObject();
							if (published != null) {
								return Flowable.fromArray(p, published);
							}
						}
						return Flowable.fromArray(p);
					}).toList().blockingGet();

					filenameSet.addAll(checkMatchingUrls(pagesWithPublished, publishUrlsPatterns));
				}
			}
		}

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			// now check for pages with nice URLs identical to folder.pub_dir + object.filename of this object

			// SQL queries to check for pages
			List<String> pageChecks = Arrays.asList(
					"SELECT nice_url url, id FROM page WHERE nice_url REGEXP ? AND channelset_id != ? AND deleted = 0",
					"SELECT page_alt_url.url, page.id FROM page_alt_url, page WHERE page_alt_url.page_id = page.id AND page_alt_url.url REGEXP ? AND page.channelset_id != ? AND page.deleted = 0");
			// SQL queries to check for files
			List<String> fileChecks = Arrays.asList(
					"SELECT nice_url url, id FROM contentfile WHERE nice_url REGEXP ? AND channelset_id != ? AND deleted = 0",
					"SELECT contentfile_alt_url.url, contentfile.id FROM contentfile_alt_url, contentfile WHERE contentfile_alt_url.contentfile_id = contentfile.id AND contentfile_alt_url.url REGEXP ? AND contentfile.channelset_id != ? AND contentfile.deleted = 0");

			// transform the resultset into a map of objectID -> set of URLs
			HandleSelectResultSet<Map<Integer, Set<String>>> ret = rs -> {
				Map<Integer, Set<String>> temp = new HashMap<>();
				while (rs.next()) {
					int pageId = rs.getInt("id");
					String url = rs.getString("url");
					temp.computeIfAbsent(pageId, key -> new HashSet<>()).add(url);
				}
				return temp;
			};

			// check pages
			for (Map.Entry<String, Set<Node>> entry : potentialPageNiceUrlPatterns.entrySet()) {
				String niceUrlPattern = entry.getKey();
				Set<Node> nodes = entry.getValue();

				// prepare the SQL statement by filling in the bind parameters
				PrepareStatement prep = ps -> {
					ps.setString(1, niceUrlPattern);
					ps.setInt(2, checkObject.getChannelSetId());
				};

				for (String check : pageChecks) {
					filenameSet.addAll(getFilenamesOfVisibleObjects(DBUtils.select(check, prep, ret), nodes, Page.class));
				}
			}

			// check files
			for (Map.Entry<String, Set<Node>> entry : potentialFileNiceUrlPatterns.entrySet()) {
				String niceUrlPattern = entry.getKey();
				Set<Node> nodes = entry.getValue();

				// prepare the SQL statement by filling in the bind parameters
				PrepareStatement prep = ps -> {
					ps.setString(1, niceUrlPattern);
					ps.setInt(2, checkObject.getChannelSetId());
				};

				for (String check : fileChecks) {
					filenameSet.addAll(getFilenamesOfVisibleObjects(DBUtils.select(check, prep, ret), nodes, File.class));
				}
			}
		}

		return filenameSet;
	}

	/**
	 * Check, which objects of given class and IDs given as keys of the urlMap are visible in any of the given nodes and return the filename portions of the URLs (values of the urlMap)
	 * @param urlMap map of object IDs to sets of URLs
	 * @param nodes set of nodes to check visibility
	 * @param clazz object class
	 * @return set of filenames
	 * @throws NodeException
	 */
	protected static Set<String> getFilenamesOfVisibleObjects(Map<Integer, Set<String>> urlMap, Set<Node> nodes,
			Class<? extends LocalizableNodeObject<? extends NodeObject>> clazz) throws NodeException {
		Set<String> filenameSet = new HashSet<>();
		Transaction t = TransactionManager.getCurrentTransaction();

		MiscUtils.doBuffered(urlMap.keySet(), 100, idList -> {
			for (LocalizableNodeObject<? extends NodeObject> p : t.getObjects(clazz, idList, false, false)) {
				for (Node n : nodes) {
					if (MultichannellingFactory.isVisibleInNode(n, p)) {
						for (String url : urlMap.get(p.getId())) {
							filenameSet.add(NodeObjectWithAlternateUrls.NAME.apply(url));
						}
					}
				}
			}
		});
		return filenameSet;
	}

	/**
	 * Returns a set of folders that share the same pubdir with a given folder
	 * in a given channel tree segment.
	 *
	 * @param targetFolder the folder to perform the search for
	 * @param targetSegment the segment to limit the search for
	 * @return the set of (localized) folders with the same pubdir
	 * @throws NodeException
	 */
	public static Set<Folder> getFoldersWithPotentialObstructors(Folder targetFolder, ChannelTreeSegment targetSegment) throws NodeException {
		// TODO Check if this method should be moved to Folder
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Folder> result = new HashSet<>();

		// key = segment of localization, value = pub_dir
		Map<ChannelTreeSegment, String> folderPubdirMap = new HashMap<>();

		// get the channelset in terms of folder objects for the folder
		List<Folder> channelSet;
		channelSet = t.getObjects(Folder.class, targetFolder.getChannelSet().values(), false, false);

		for (Folder current: channelSet) {
			ChannelTreeSegment folderChannelVariantSegment = new ChannelTreeSegment(current, false);
			if (folderChannelVariantSegment.intersects(targetSegment)) {
				folderPubdirMap.put(folderChannelVariantSegment, current.getPublishDir());
			}
		}

		Integer motherId = null;
		if (targetFolder.getMother() != null) {
			if (targetFolder.getNode().isPubDirSegment()) {
				try (NoMcTrx trx = new NoMcTrx()) {
					motherId = targetFolder.getMother().getId();
				}
			}
		}

		Map<Node, MultiChannellingFallbackList> fallbackLists = getFoldersByPublishDirInSegment(targetSegment, new HashSet<String>(folderPubdirMap.values()), motherId);

		for (Entry<ChannelTreeSegment, String> entry : folderPubdirMap.entrySet()) {
			for (Node channel : entry.getKey().getAllNodes()) {
				if (!targetSegment.contains(channel)) {
					continue;
				}
				List<Folder> testFolders;
				testFolders = t.getObjects(Folder.class, fallbackLists.get(channel).getObjectIds(), false, false);
				for (Folder testFolder : testFolders) {
					if (testFolder.getPublishDir().equalsIgnoreCase(entry.getValue())) {
							result.add(testFolder);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns a set of folders that have a pubdir that is equal to the path portion of a nice URL
	 * 
	 * @param niceUrlPath path of a nice URL
	 * @param targetSegment the segment to limit the search for
	 * @return the set of folders with potentially conflicting pubdir
	 * @throws NodeException
	 */
	public static Set<Folder> getFoldersWithPotentialObstructors(String niceUrlPath, ChannelTreeSegment targetSegment) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<Folder> result = new HashSet<>();

		if (!StringUtils.isEmpty(niceUrlPath)) {
			// make a set of all paths and trailing subpaths
			// this is necessary, because the nice URL has to be checked agains Node.pub_dir + Folder.pub_dir, and the Node.pub_dir
			// depends on the Node/Channel (which is yet unknown at this stage)
			// therefore, the fallbackLists will possibly return too many folders, but there is an exact check (including Node.pub_dir)
			// later anyway
			Set<String> paths = new HashSet<>();
			paths.add(niceUrlPath);
			int slashIndex = 0;
			while ((slashIndex = niceUrlPath.indexOf('/', slashIndex + 1)) > 0) {
				paths.add(niceUrlPath.substring(slashIndex));
			}
			paths.add("/");

			Map<Node, MultiChannellingFallbackList> fallbackLists = getFoldersByPublishDirInSegment(targetSegment, paths, null);
			for (Node channel : targetSegment.getAllNodes()) {
				List<Folder> testFolders = t.getObjects(Folder.class, fallbackLists.get(channel).getObjectIds(), false, false);
				for (Folder testFolder : testFolders) {
					if (FolderFactory.getPath("/", testFolder.getPublishDir(), true).equalsIgnoreCase(niceUrlPath)) {
						result.add(testFolder);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Creates a multichannelling fallback list with all folders with a pubdir
	 * contained in a given set for each channel of a given segment.
	 *
	 * @param targetSegment
	 *            look for folders only in this segment
	 * @param folderPubDirs
	 *            set of pubdirs to look for
	 * @param motherId optional motherid to restrict search to folders with the same mother
	 * @return a map of the node to its multichannelling fallback list
	 * @throws NodeException
	 */
	private static Map<Node, MultiChannellingFallbackList> getFoldersByPublishDirInSegment(final ChannelTreeSegment targetSegment,
			final Collection<String> folderPubDirs, Integer motherId) throws NodeException {
		// TODO Check if this method should be moved to Folder - probably a good idea, in order to exclude the actual folder itself [no, bad idea, shouldn't be excluded])
		Set<Node> targetNodes = targetSegment.getAllNodes();
		Set<Node> relevantNodes = new HashSet<>(targetNodes);
		Node masterNode = targetSegment.getStartChannel().getMaster();
		relevantNodes.addAll(targetSegment.getStartChannel().getMasterNodes());

		String FOLDER_PUBLISHDIR_SQL = "SELECT distinct f2.id, f2.channel_id, f2.channelset_id, f2.mc_exclude, fd.channel_id disinherited_channel FROM folder f1 "
				+ "JOIN folder f2 ON f2.channelset_id = f1.channelset_id "
				+ "LEFT JOIN folder_disinherit fd ON f2.id = fd.folder_id "
				+ "WHERE f1.pub_dir in " + DBUtils.makeSqlPlaceHolders(folderPubDirs.size(), "") + " "
				+ (motherId != null ? "AND f1.mother = ? " : "")
				+ "AND (f1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size(), "")
				+ " OR (f1.channel_id = 0 AND f1.node_id = ?)) AND f1.deleted = 0 AND f2.deleted = 0";
		List<Object> arguments = new ArrayList<>();
		arguments.addAll(folderPubDirs);
		if (motherId != null) {
			arguments.add(motherId);
		}
		for (Node n: relevantNodes) {
			arguments.add(n.getId());
		}
		arguments.add(masterNode.getId());
		return MultichannellingFactory.performMultichannelMultichannellingFallback(FOLDER_PUBLISHDIR_SQL, arguments.toArray(), targetNodes);
	}

	/**
	 * Makes the object name of the specified disinheritable unique in its
	 * scope.
	 * @param object object
	 * @param type separator type
	 * @param maxLength max allowed length
	 * @return unique name
	 * @throws NodeException
	 */
	public static <T extends NodeObject> String makeUniqueDisinheritable(Disinheritable<T> object, UniquifyHelper.SeparatorType type, int maxLength) throws NodeException {
		return makeUniqueDisinheritable(object, o -> o.getName(), (o, name) -> o.setName(name), "name", type, maxLength);
	}

	/**
	 * Make a specific property on an object unique disinheritable
	 * @param object object
	 * @param propertyFunction function that extracts the property
	 * @param propertySetter consumer that sets the property
	 * @param propertyName name of the property
	 * @param type separator type
	 * @param maxLength max allowed length
	 * @return unique property value
	 * @throws NodeException
	 */
	public static <T extends NodeObject> String makeUniqueDisinheritable(Disinheritable<T> object, Function<Disinheritable<T>, String> propertyFunction,
			BiConsumer<Disinheritable<T>, String> propertySetter, String propertyName, UniquifyHelper.SeparatorType type, int maxLength) throws NodeException {
		if (type == null) {
			type = UniquifyHelper.SeparatorType.none;
		}

		// check whether conflicting values are found
		ChannelTreeSegment objectSegment = new ChannelTreeSegment(object, false);
		if (isPropertyAvailable(object, propertyFunction, propertyName, objectSegment)) {
			return propertyFunction.apply(object);
		}

		// now add numbers to the given value and try again
		String base = propertyFunction.apply(object);
		String separator = type.getSeparator();
		int number = 0;

		Matcher matcher = type.getMatcher(base);

		if (matcher.matches()) {
			base = matcher.group(1);
			separator = "";
			number = Integer.parseInt(matcher.group(2));
		}

		while (true) {
			// try the next number
			++number;
			String toAdd = String.format("%s%d", separator, number);
			if (maxLength > 0 && (base.length() + toAdd.length()) > maxLength) {
				propertySetter.accept(object, String.format("%s%s", base.substring(0, maxLength - toAdd.length()), toAdd));
			} else {
				propertySetter.accept(object, String.format("%s%s", base, toAdd));
			}
			if (isPropertyAvailable(object, propertyFunction, propertyName, objectSegment)) {
				return propertyFunction.apply(object);
			}
		}
	}

	/**
	 * Get filenames used in the folders with given ids in the given channel
	 * @param channel channel
	 * @param folderIds folder IDs
	 * @return set of used filenames
	 * @throws NodeException
	 */
	private static Set<String> getUsedFilenames(Node channel, Collection<Integer> folderIds) throws NodeException {
		Set<String> fileNames = new HashSet<String>();
		Transaction t = TransactionManager.getCurrentTransaction();
		try (ChannelTrx trx = new ChannelTrx(channel)) {
			List<Folder> folders = t.getObjects(Folder.class, folderIds);
			for (Folder folder : folders) {
				List<File> filesAndImages = folder.getFilesAndImages();
				for (File file : filesAndImages) {
					fileNames.add(file.getFilename());
				}

				List<Page> pages = folder.getPages();
				for (Page page : pages) {
					fileNames.add(page.getFilename());
				}
			}
		}

		return fileNames;
	}

	/**
	 * Check whether changing the publish directory of the given folder to the given value would cause duplicate filenames
	 * @param folder folder about to be changed
	 * @param pubDir proposed new publish directory
	 * @return true if changing is possible, false if not
	 * @throws NodeException
	 */
	public static boolean checkPubDirChangeConsistency(Folder folder, String pubDir) throws NodeException {
		ChannelTreeSegment folderSegment = new ChannelTreeSegment(folder, false);
		Integer motherId = null;
		if (folder.getMother() != null) {
			if (folder.getNode().isPubDirSegment()) {
				try (NoMcTrx trx = new NoMcTrx()) {
					motherId = folder.getMother().getId();
				}
			}
		}
		Map<Node, MultiChannellingFallbackList> folderLists = getFoldersByPublishDirInSegment(folderSegment, Collections.singleton(pubDir), motherId);

		for (Map.Entry<Node, MultiChannellingFallbackList> entry : folderLists.entrySet()) {
			Node node = entry.getKey();
			MultiChannellingFallbackList fbList = entry.getValue();

			List<Integer> foldersToCheck = fbList.getObjectIds();
			foldersToCheck.remove(folder.getId());
			Set<String> existingFilenames = getUsedFilenames(node, foldersToCheck);
			Set<String> folderFilenames = getUsedFilenames(node, Collections.singleton(folder.getId()));
			if (!CollectionUtils.intersection(existingFilenames, folderFilenames).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get all siblings (of same type) of the given object
	 * @param <T> type of the object
	 * @param checkObject object
	 * @param objectSegment visibility segment
	 * @return map of node to fallback lists, which will return the siblings (in the respective node)
	 * @throws NodeException
	 */
	public static <T extends NodeObject> Map<Node, MultiChannellingFallbackList> getSiblings(
			Disinheritable<T> checkObject, ChannelTreeSegment objectSegment) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder masterFolder;
		try (NoMcTrx nmt = new NoMcTrx()) {
			if (checkObject.getParentObject() == null) {
				return Collections.emptyMap();
			}
			masterFolder = ((Folder)checkObject.getParentObject()).getMaster();
		}

		Set<Node> relevantNodes = new HashSet<>();
		// ChannelTreeSegment objectSegment = new ChannelTreeSegment(checkObject, false);
		relevantNodes.addAll(objectSegment.getAllNodes());
		relevantNodes.addAll(objectSegment.getStartChannel().getMasterNodes());

		ArrayList<Object> parameters = new ArrayList<>();
		for (Node n : relevantNodes) {
			parameters.add(n.getId());
		}
		parameters.add(0);
		parameters.addAll(parameters);
		parameters.add(checkObject.getChannelSetId());
		parameters.add(masterFolder.getId());

		String tableName = t.getTable(checkObject.getObjectInfo().getObjectClass());
		String folderIdField = checkObject instanceof Folder ? "mother" : "folder_id";
		String getObjectsSameNameSameFolderSQL = "SELECT o2.id, o2.channel_id, o2.channelset_id, o2.mc_exclude, od.channel_id disinherited_channel " +
				"FROM " + tableName + " o1 " +
				"JOIN " + tableName + " o2 ON o2.channelset_id = o1.channelset_id " +
				"LEFT JOIN " + tableName + "_disinherit od ON od." + tableName + "_id = o2.id " +
				"WHERE o1.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
				"AND o2.channel_id in " + DBUtils.makeSqlPlaceHolders(relevantNodes.size() + 1, "") + " " +
				"AND o1.channelset_id != ? AND o1." + folderIdField + " = ? AND o1.deleted = 0 AND o2.deleted = 0";
		if (checkObject instanceof Page) {
			Integer contentsetId = ((Page)checkObject).getContentsetId();
			if (ObjectTransformer.getInteger(contentsetId, 0) != 0) {
				getObjectsSameNameSameFolderSQL += " AND (o1.contentset_id != ? OR o1.contentset_id = 0)";
				parameters.add(contentsetId);
			}
		}

		Object[] parameterArray = parameters.toArray();

		return MultichannellingFactory.performMultichannelMultichannellingFallback(getObjectsSameNameSameFolderSQL,
				parameterArray, objectSegment.getAllNodes());
	}

	/**
	 * Check whether the full publish path of any of the given objects matches any of the publish URL patterns
	 * for any of the nodes. Collect and return the filenames (lower case) of all matching objects
	 * @param toCheck objects to check
	 * @param publishUrlsPatterns map of publish URL patterns to set of nodes to check against
	 * @return set of filenames for matching objects
	 * @throws NodeException
	 */
	private static Set<String> checkMatchingUrls(Collection<? extends Disinheritable<? extends NodeObject>> toCheck,
			Map<Pattern, Set<Node>> publishUrlsPatterns) throws NodeException {
		Set<String> filenames = new HashSet<>();
		for (Disinheritable<? extends NodeObject> f : toCheck) {
			String filename = f.getFilename();
			for (Map.Entry<Pattern, Set<Node>> urlPatternEntry : publishUrlsPatterns.entrySet()) {
				Pattern urlPattern = urlPatternEntry.getKey();

				for (Node n : urlPatternEntry.getValue()) {
					// when the object to check is not visible in the target node, we omit the check
					if (!MultichannellingFactory.isVisibleInNode(n, f)) {
						continue;
					}
					try (ChannelTrx cTrx = new ChannelTrx(n)) {
						String fileUrl = String.format("%s%s", f.getFullPublishPath(true), filename);
						if (urlPattern.matcher(fileUrl).matches()) {
							filenames.add(filename.toLowerCase());
						}
					}
				}
			}
		}
		return filenames;
	}

	/**
	 * Check whether the full publish path of any of the given objects is equal to any of the given publish URLs
	 * @param toCheck objects to check
	 * @param publishedUrls map of publish URLs to set of nodes to check against
	 * @return first matching object (null if none matches)
	 * @throws NodeException
	 */
	private static Disinheritable<?> checkEqualUrls(Collection<? extends Disinheritable<? extends NodeObject>> toCheck,
			Map<String, Set<Node>> publishedUrls) throws NodeException {
		for (Disinheritable<? extends NodeObject> f : toCheck) {
			String filename = f.getFilename();
			for (Map.Entry<String, Set<Node>> urlEntry : publishedUrls.entrySet()) {
				String url = urlEntry.getKey();

				for (Node n : urlEntry.getValue()) {
					try (ChannelTrx cTrx = new ChannelTrx(n)) {
						// when the object to check is not visible in the target node, we omit the check
						if (!MultichannellingFactory.isVisibleInNode(n, f)) {
							continue;
						}
						String fileUrl = String.format("%s%s", f.getFullPublishPath(true), filename);
						if (fileUrl.equalsIgnoreCase(url)) {
							return f;
						}
					}
				}
			}
		}
		return null;
	}
}
