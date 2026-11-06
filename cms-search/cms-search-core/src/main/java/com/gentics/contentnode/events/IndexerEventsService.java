package com.gentics.contentnode.events;

import static com.gentics.contentnode.events.Events.CHILD;
import static com.gentics.contentnode.events.Events.CREATE;
import static com.gentics.contentnode.events.Events.DELETE;
import static com.gentics.contentnode.events.Events.EVENT_CN_PAGESTATUS;
import static com.gentics.contentnode.events.Events.HIDE;
import static com.gentics.contentnode.events.Events.MOVE;
import static com.gentics.contentnode.events.Events.NOTIFY;
import static com.gentics.contentnode.events.Events.REVEAL;
import static com.gentics.contentnode.events.Events.UPDATE;
import static com.gentics.contentnode.events.Events.WASTEBIN;
import static com.gentics.contentnode.events.Events.isEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.search.IndexType;
import com.gentics.contentnode.object.search.IndexableNodeObject;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Implementation of {@link EventsService} that updates the index for modified objects
 */
public class IndexerEventsService implements EventsService {
	/**
	 * Update the search index, if necessary
	 * @param object object which was acted upon
	 * @param eventMask event mask
	 * @param properties optional properties
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void trigger(NodeObject object, int eventMask, String... properties) throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.ELASTICSEARCH)) {
			return;
		}
		// ignore child events
		if (isEvent(eventMask, CHILD)) {
			return;
		}

		Optional<IndexableNodeObject<?>> optWrapped = IndexType.wrap(object);

		// update search index
		if (optWrapped.isPresent()) {
			IndexableNodeObject<?> indexableNodeObject = optWrapped.get();
			if (isEvent(eventMask, NOTIFY)) {
				if (ObjectTransformer.isEmpty(properties)) {
					Indexer.index(indexableNodeObject);
				} else {
					Indexer.update(indexableNodeObject, new HashSet<>(Arrays.asList(properties)));
				}
			} else if (isEvent(eventMask, CREATE) || isEvent(eventMask, UPDATE)
					|| isEvent(eventMask, EVENT_CN_PAGESTATUS) || isEvent(eventMask, HIDE)
					|| isEvent(eventMask, REVEAL) || isEvent(eventMask, MOVE)) {
				Indexer.index(indexableNodeObject);
			} else if (isEvent(eventMask, DELETE)) {
				if (isEvent(eventMask, WASTEBIN)) {
					Indexer.update(indexableNodeObject, new HashSet<>(Arrays.asList("deleted")));
				} else {
					Indexer.remove(indexableNodeObject);
				}
			}

			// when a localized copy was created or deleted, we need to reindex master objects
			if ((isEvent(eventMask, CREATE) || isEvent(eventMask, NOTIFY) || isEvent(eventMask, DELETE))
					&& !indexableNodeObject.isMaster()) {
				LocalizableNodeObject<NodeObject> loc = (LocalizableNodeObject<NodeObject>) object;

				while (loc != null) {
					loc = (LocalizableNodeObject<NodeObject>) MultichannellingFactory.getNextHigherObject(loc);
					if (loc != null) {
						// we need to load the channelset in order to respect deleted objects
						// because indexing will be done in a separate transaction, which might still "see" the deleted objects
						loc.getChannelSet();
						Optional<IndexableNodeObject<?>> optIndexableLoc = IndexType.wrap(loc);
						if (optIndexableLoc.isPresent()) {
							Indexer.index(optIndexableLoc.get());
						}
					}
				}
			}
		}
		// if languages of a Node changed, we need to check for missing indices
		if (object instanceof Node && properties != null && Arrays.asList(properties).contains("languages")) {
			Indexer.updateIndices();
		}
		// if permissions of a Folder changed, we need to update permissions of objects contained in the folder
		if (object instanceof Folder && properties != null && Arrays.asList(properties).contains("permissions")) {
			Indexer.updatePermissions(object.getId());
		}
		// if permissions of a Role changed, we need to update permissions of objects
		// contained in folders, that have the role assigned to at least one group
		if (object instanceof Role && properties != null && Arrays.asList(properties).contains("permissions")) {
			List<Integer> folderIds = DBUtils.select(
					"SELECT DISTINCT rua.obj_id FROM role_usergroup_assignment rua LEFT JOIN role_usergroup ru ON rua.role_usergroup_id = ru.id WHERE ru.role_id = ? AND rua.obj_type IN (?, ?, ?)",
					ps -> {
						ps.setInt(1, object.getId()); // ru.role_id = ?
						ps.setInt(2, 10001); // rua.obj_type IN (?, ?, ?)
						ps.setInt(3, 10002); // rua.obj_type IN (?, ?, ?)
						ps.setInt(4, 10033); // rua.obj_type IN (?, ?, ?)
					}, DBUtils.allInt("obj_id"));
			for (int id : folderIds) {
				Indexer.updatePermissions(id);
			}
		}
	}
}
