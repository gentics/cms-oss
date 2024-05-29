package com.gentics.contentnode.helper;

import java.util.Collection;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.job.AbstractLocalizeCallable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.Operator.QueueBuilder;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Static helper class for localization of objects
 */
public class LocalizeHelper {
	/**
	 * Process the job for the given collection of objects. The jobs are
	 * synchronized with the channelSetId
	 *
	 * @param clazz object class
	 * @param ids collection of object ids
	 * @param channelId channel ID
	 * @param timeout timeout in ms
	 * @throws NodeException
	 */
	public static GenericResponse process(Class<? extends NodeObject> clazz, Collection<Integer> ids, int channelId, long timeout) throws NodeException {
		// we will disable instant publishing, when more than one object is
		// localized or the localized object is a folder or node
		final boolean disableInstantPublishing = ids.size() > 1 || Node.class.isAssignableFrom(clazz) || Folder.class.isAssignableFrom(clazz);

		QueueBuilder builder = Operator.queue();
		Transaction t = TransactionManager.getCurrentTransaction();
		List<? extends NodeObject> objects = t.getObjects(clazz, ids);

		for (NodeObject nodeObject : objects) {
			LocalizableNodeObject<?> locObject = (LocalizableNodeObject<?>) nodeObject;
			int id = locObject.getId();
			int channelSetId = locObject.getChannelSetId();

			builder.addLocked("", Operator.lock(LockType.channelSet, channelSetId),
					AbstractLocalizeCallable.get(clazz, id, channelId, disableInstantPublishing));
		}

		return builder.execute(new CNI18nString("localizejob").toString(), timeout);
	}
}
