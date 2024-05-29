package com.gentics.contentnode.helper;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.Operator.QueueBuilder;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Static helper class that provides a method to delete a collection of objects with possible synchronization over the
 * channelSetId.
 */
public class DeleteHelper {
	/**
	 * Process the job for the given collection of objects. If the objects are
	 * instances of {@link LocalizableNodeObject}, the delete jobs are
	 * synchronized with the channelSetId
	 * 
	 * @param clazz object class
	 * @param ids collection of object ids
	 * @param force true to force deletion
	 * @param timeout timeout in ms
	 * @throws NodeException
	 */
	public static GenericResponse process(Class<? extends NodeObject> clazz, Collection<Integer> ids, boolean force, long timeout) throws NodeException {
		// we will disable instant publishing, when more than one object is
		// deleted or the deleted object is a folder or node
		final boolean disableInstantPublishing = ids.size() > 1 || Node.class.isAssignableFrom(clazz) || Folder.class.isAssignableFrom(clazz);

		QueueBuilder builder = Operator.queue();
		if (LocalizableNodeObject.class.isAssignableFrom(clazz)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<? extends NodeObject> objects = t.getObjects(clazz, ids);

			for (NodeObject nodeObject : objects) {
				LocalizableNodeObject<?> locObject = (LocalizableNodeObject<?>)nodeObject;
				int id = locObject.getId();
				int channelSetId = locObject.getChannelSetId();

				builder.addLocked("", Operator.lock(LockType.channelSet, channelSetId), new Callable<GenericResponse>() {
					@Override
					public GenericResponse call() throws Exception {
						delete(clazz, id, force, disableInstantPublishing);
						return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, ""));
					}
				});
			}
		} else {
			for (final Integer id : ids) {
				builder.add("", new Callable<GenericResponse>() {
					@Override
					public GenericResponse call() throws Exception {
						delete(clazz, id, force, disableInstantPublishing);
						return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, ""));
					}
				});
			}
		}
		return builder.execute(new CNI18nString("deletejob").toString(), timeout);
	}

	/**
	 * Internal delete method
	 * @param clazz object class
	 * @param id object id
	 * @param force true to force delete the object
	 * @param disableInstantPublishing true to disable instant publishing
	 * @throws NodeException
	 */
	protected static void delete(Class<? extends NodeObject> clazz, Integer id, boolean force, boolean disableInstantPublishing) throws NodeException {
		try (WastebinFilter wb = force ? Wastebin.INCLUDE.set() : Wastebin.EXCLUDE.set();
				InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublishing)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodeObject nodeObject = t.getObject(clazz, id);

			if (nodeObject != null) {
				// when deleting a construct, also delete the tags
				if (nodeObject instanceof Construct) {
					List<Tag> tags = ((Construct) nodeObject).getTags();
					
					for (Tag tag : tags) {
						t.dirtObjectCache(tag.getObjectInfo().getObjectClass(), tag.getId(), true);
						NodeObject tagContainer = (NodeObject) tag.getContainer();
						
						if (tagContainer != null) {
							t.dirtObjectCache(tagContainer.getObjectInfo().getObjectClass(), tagContainer.getId(), true);
						}
						// only trigger the DELETE event for the tag, if it is enabled.
						// tags, that are not enabled are treated like being inexistent during the publish process
						if (tag.isEnabled()) {
							Events.trigger(tag, null, Events.DELETE);
						}
						tag.delete();
					}
				} else if (nodeObject instanceof ObjectTagDefinition) {
					ObjectTagDefinition def = (ObjectTagDefinition)nodeObject;
					// delete all tags based on the definition
					List<ObjectTag> objectTags = def.getObjectTags();
					for (ObjectTag tag : objectTags) {
						t.dirtObjectCache(ObjectTag.class, tag.getId(), true);
						NodeObject tagContainer = tag.getNodeObject();
						
						if (tagContainer != null) {
							t.dirtObjectCache(tagContainer.getObjectInfo().getObjectClass(), tagContainer.getId(), true);
						}
						if (tag.isEnabled()) {
							Events.trigger(tag, null, Events.DELETE);
						}
						tag.delete();
					}
				}
				t.dirtObjectCache(clazz, id, true);
				nodeObject.delete(force);
			}
		}
	}
}
