package com.gentics.contentnode.job;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInFolder;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Background job for objects removal.
 */
public class DeleteJob extends AbstractBackgroundJob {

	/**
	 * collection of operands to be deleted
	 */
	protected Collection<DeletionOperand<?>> ops;

	/**
	 * Create an instance with data
	 * @param ops operands
	 */
	public DeleteJob(Collection<DeletionOperand<?>> ops) {
		this.ops = ops;
	}

	/**
	 * Create an instance with data
	 * @param ops operands
	 */
	public DeleteJob(DeletionOperand<?>... ops) {
		this(Arrays.asList(ops));
	}

	@Override
	public String getJobDescription() {
		return new CNI18nString("deletejob").toString();
	}

	@Override
	protected void processAction() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		// Stores current state of instant publishing, so that it can be re-enabled if it needs to be disabled
		boolean instantPublishingEnabled = t.isInstantPublishingEnabled();

		// Disable instant publishing
		if (ops.size() > 1) {
			t.setInstantPublishingEnabled(false);
		} else if (ops.size() > 0) {
			// Check if single object is a folder
			DeletionOperand<?> op = ops.toArray(new DeletionOperand<?>[ops.size()])[0];
			NodeObject nodeObject = t.getObject(op.clazz, op.objectId);

			if (nodeObject != null && nodeObject instanceof Folder) {
				t.setInstantPublishingEnabled(false);
			}
		}

		AtomicInteger count = new AtomicInteger(0);
		for (DeletionOperand<?> op : ops) {
			String typeName = t.getTable(op.clazz);

			NodeObject nodeObject = MiscUtils.loadUnsafe(op.clazz, Integer.toString(op.objectId), true, true, ObjectPermission.view, ObjectPermission.delete);

			// set the channel ID if given
			boolean isChannelIdset = MiscUtils.setChannelToTransaction(op.nodeId);

			Node node = null;
			if (LocalizableNodeObject.class.isInstance(nodeObject)) {
				node = LocalizableNodeObject.class.cast(nodeObject).getChannel();
			} else if (NodeObjectInFolder.class.isInstance(nodeObject)) {
				node = NodeObjectInFolder.class.cast(nodeObject).getOwningNode();
			}

			int nodeIdOfObject = -1;

			if (node != null) {
				nodeIdOfObject = ObjectTransformer.getInteger(node.getId(), -1);
			}

			int nodeId = 0;
			if (op.nodeId != null) {
				nodeId = op.nodeId;
			}

			if (isChannelIdset && LocalizableNodeObject.class.cast(nodeObject).isInherited()) {
				addMessage(new DefaultNodeMessage(Level.ERROR, getClass(), String.format("Can't delete an inherited %s '%s' (#%d), the %s has to be deleted in the master node.", typeName, LocalizableNodeObject.class.cast(nodeObject).getName(), op.objectId, typeName)));
				continue;
			}

			if (nodeId > 0 && nodeIdOfObject > 0 && nodeIdOfObject != nodeId) {
				addMessage(new DefaultNodeMessage(Level.ERROR, getClass(), String.format("The specified %s #%d exists, but is not part of the node #d you specified.", typeName, op.objectId, nodeId)));
				continue;
			}

			int channelSetId = LocalizableNodeObject.class.isInstance(nodeObject) ? ObjectTransformer.getInteger(LocalizableNodeObject.class.cast(nodeObject).getChannelSetId(), 0) : 0;
			if (channelSetId > 0 && !LocalizableNodeObject.class.cast(nodeObject).isMaster()) {
				addMessage(new DefaultNodeMessage(Level.ERROR, getClass(), String.format("Deletion of localized %ss is currently not implemented, you maybe want to unlocalize it instead.", typeName)));
				continue;
			}

			op.customDeleter.ifPresentOrElse(cd -> {
				try {
					cd.accept(nodeObject);
					count.incrementAndGet();
				} catch (NodeException e) {
					addMessage(new DefaultNodeMessage(Level.ERROR, getClass(), String.format("Error on deletion of %s #%d: %s.", typeName, op.objectId, e.getLocalizedMessage())));
				}
			}, () -> {
				try {
					nodeObject.delete();
					count.incrementAndGet();
				} catch (NodeException e) {
					addMessage(new DefaultNodeMessage(Level.ERROR, getClass(), String.format("Error on deletion of %s #%d: %s.", typeName, op.objectId, e.getLocalizedMessage())));
				}
			});

			if (t.isInterrupted()) {
				if (t.isOpen()) {
					t.rollback();
				}
				throw new NodeException();
			}
		}

		// Restores temporarily disabled instant publishing
		t.setInstantPublishingEnabled(instantPublishingEnabled);
		addMessage(new DefaultNodeMessage(Level.INFO, getClass(), count.get() + " objects were deleted."));
	}

	public static final class DeletionOperand<T extends NodeObject> {
		/**
		 * class of objects to be deleted
		 */
		protected final Class<? extends T> clazz;
		/**
		 * node id to check the objects to belong to
		 */
		protected final Integer nodeId;
		/**
		 * object id to delete
		 */
		protected final Integer objectId;
		/**
		 * custom deletion mechanism
		 */
		protected final Optional<Consumer<NodeObject>> customDeleter;
		/**
		 * ctor
		 * 
		 * @param clazz
		 * @param nodeId
		 * @param objectId
		 * @param customDeleter
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public DeletionOperand(Class<? extends T> clazz, Integer nodeId, Integer objectId,
				Optional<Consumer<T>> customDeleter) {
			super();
			this.clazz = clazz;
			this.nodeId = nodeId;
			this.objectId = objectId;
			this.customDeleter = (Optional) customDeleter;
		}
		/**
		 * ctor with no custom deleter
		 * 
		 * @param clazz
		 * @param nodeId
		 * @param objectId
		 */
		public DeletionOperand(Class<? extends T> clazz, Integer nodeId, Integer objectId) {
			this(clazz, nodeId, objectId, Optional.empty());
		}
	}
}
