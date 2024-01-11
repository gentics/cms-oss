/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: AbstractContentObject.java,v 1.29.4.1.2.1 2011-03-07 16:36:43 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.HandleDependenciesTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * The AbstractContentObject defines some generic methods and properties for all
 * objects of the objectlayer of Content.Node.
 */
public abstract class AbstractContentObject implements NodeObject, Resolvable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4716050914531683274L;

	protected Integer id;

	protected transient NodeObjectInfo info;

	/**
	 * Udate of the object
	 */
	protected int udate;

	/**
	 * Global ID of the object
	 */
	protected GlobalId globalId;

	/**
	 * map of resolvable properties
	 */
	private static Map<String, Property> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("id", new Property(new String[] { "id"}) {
			public Object get(AbstractContentObject object, String key) {
				return object.id;
			}
		});
		resolvableProperties.put("ttype", new Property(null) {
			public Object get(AbstractContentObject object, String key) {
				return new Integer(object.getFactory().getTType(object.getObjectInfo().getObjectClass()));
			}
		});
		resolvableProperties.put("ispage", new Property(null) {
			public Object get(AbstractContentObject object, String key) {
				return Boolean.valueOf(object.isPage());
			}
		});
		resolvableProperties.put("isfolder", new Property(null) {
			public Object get(AbstractContentObject object, String key) {
				return Boolean.valueOf(object.isFolder());
			}
		});
		resolvableProperties.put("isfile", new Property(null) {
			public Object get(AbstractContentObject object, String key) {
				return Boolean.valueOf(object.isFile());
			}
		});
		resolvableProperties.put("isimage", new Property(null) {
			public Object get(AbstractContentObject object, String key) {
				return Boolean.valueOf(object.isImage());
			}
		});
		resolvableProperties.put("istag", new Property(null) {
			public Object get(AbstractContentObject object, String key) {
				return Boolean.valueOf(object.isTag());
			}
		});
	}

	protected AbstractContentObject(Integer id, NodeObjectInfo info) {
		this.id = id;
		this.info = info;
	}

	protected transient NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getTType()
	 */
	public Integer getTType() {
		// disable handling dependencies
		try (HandleDependenciesTrx noDeps = new HandleDependenciesTrx(false)) {
		return ObjectTransformer.getInteger(this.getProperty("ttype"), null);
		} catch (NodeException e) {
			// exception is thrown, if no current transaction found (to disable handling dependencies),
			// we can ignore the exception, because when no transaction is found, there is no handling of dependencies either
			return ObjectTransformer.getInteger(this.getProperty("ttype"), null);
		}
	}

	/**
	 * Returns the id of this object.
	 * @return id of the object
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Get a hashkey for the object. If the object has an ID set, the ID will be
	 * returned as String, otherwise, the {@link Object#hashCode()} method will
	 * be called
	 *
	 * @return hashkey
	 */
	protected String getHashKey() {
		if (isEmptyId(id)) {
			return "@" + super.hashCode();
		} else {
			return Integer.toString(id);
		}
	}

	public NodeFactory getFactory() {
		return getObjectInfo().getFactory();
	}

	public NodeObjectInfo getObjectInfo() {
		return info;
	}

	public final Object getProperty(String key) {
		return get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#delete()
	 */
	final public void delete() throws InsufficientPrivilegesException, NodeException {
		delete(false);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.NodeObject#delete(boolean)
	 */
	public void delete(boolean force) throws InsufficientPrivilegesException ,NodeException {}

	@Override
	public boolean isDeleted() {
		// generally, objects cannot be put into the wastebin
		return false;
	}

	@Override
	public int getDeleted() {
		// generally, objects cannot be put into the wastebin
		return 0;
	}

	@Override
	public SystemUser getDeletedBy() throws NodeException {
		// generally, objects cannot be put into the wastebin
		return null;
	}

	@Override
	public void restore() throws NodeException {
		// generally, objects cannot be put into the wastebin, therefore they cannot be restored
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#save()
	 */
	public boolean save(Integer userGroupId) throws InsufficientPrivilegesException, NodeException {
		// empty implementation of save
		assertEditable();
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#save()
	 */
	public boolean save() throws InsufficientPrivilegesException, NodeException {
		// empty implementation of save
		assertEditable();
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#unlock()
	 */
	public void unlock() throws NodeException {// empty dummy implementation
	}

	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			return prop.get(this, key);
		} else {
			return null;
		}
	}

	public boolean canResolve() {
		return true;
	}

	/**
	 * Assert that the given object is not null. Throw an Exception if it is
	 * null. This method is to be used when related objects are loaded that must
	 * exist (empty ids are not allowed). Note that it is required for
	 * descriptive exception messages that
	 * <ol>
	 * <li>The object id is given</li>
	 * <li>The object's relation is given</li>
	 * <li>This class implements {@link Object#toString()} appropriately</li>
	 * </ol>
	 * @param object object to check
	 * @param id id of the object (for descriptive exception message)
	 * @param relation string describing the relation of the required object to
	 *        this object (e.g. "folder", "template", ...)
	 * @throws NodeException when the given object is null
	 */
	protected void assertNodeObjectNotNull(NodeObject object, Integer id, String relation) throws NodeException {
		assertNodeObjectNotNull(object, id, relation, false);
	}

	/**
	 * Variant of {@link #assertNodeObjectNotNull(Object, Object, String)}
	 * that optionally allows null objects for empty ids.
	 * @param object object to check
	 * @param id id of the object
	 * @param relation string describing the relation of the required object to
	 *        this object (e.g. "folder", "template", ...)
	 * @param allowEmptyIds true when null objects for empty ids are allowed,
	 *        false if not
	 * @throws NodeException when the given object is null, but should not
	 */
	protected void assertNodeObjectNotNull(Object object, Integer id, String relation,
			boolean allowEmptyIds) throws NodeException {
		if (object == null && (!allowEmptyIds || !isEmptyId(id))) {
			Transaction t = TransactionManager.getCurrentTransaction();
			throw new InconsistentDataException("Data inconsistent: " + relation + " {" + id + "} of " + this
					+ " does not exist (channel ID {" + t.getChannelId() + "})!", null);
		}
	}

	/**
	 * Helper method to check whether the given object is a valid, but empty
	 * object id
	 * @param id object id to check
	 * @return true if the id is valid, but empty, false if it is valid and not
	 *         empty
	 */
	public static boolean isEmptyId(Integer id) {
		//TODO maybe use common io instead?
		if (id == null) {
			// null is very empty
			return true;
		} else {
			// for Integers, every non-positive value is empty
			return id.intValue() <= 0;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#triggerEvent(com.gentics.contentnode.events.DependencyObject, java.lang.String[], int, int, int)
	 */
	public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException {
		if (Events.isEvent(eventMask, Events.DIRT)) {
			// just dirt the object, no further dependency calculation necessary
			PublishQueue.dirtObject(this, Action.DEPENDENCY, channelId, property);
			return;
		}
		// when the event is MOVE, the property might contain the node, the object was moved FROM
		if (Events.isEvent(eventMask, Events.MOVE) && !ObjectTransformer.isEmpty(property) && this instanceof LocalizableNodeObject<?>) {
			// get the node id
			int oldNodeId = ObjectTransformer.getInt(property[0], 0);

			if (oldNodeId > 0) {
				// get the current node id of the object
				Node newNode = ((LocalizableNodeObject<?>) this).getChannel();
				if (newNode == null) {
					newNode = ((LocalizableNodeObject<?>) this).getOwningNode();
				}

				if (newNode != null) {
					int newNodeId = ObjectTransformer.getInt(newNode.getId(), 0);

					if (oldNodeId != newNodeId) {
						// the object was moved away from the old node, so we dirt the object for the old node
						PublishQueue.dirtObject(this, Action.REMOVE, oldNodeId);

						// when a folder was moved to another node, we also need to dirt all pages and files in that folder
						if (isFolder()) {
							Folder folder = (Folder) this;

							for (Page page : folder.getPages()) {
								// page was removed from old node and moved to new node
								PublishQueue.dirtObject(page, Action.REMOVE, oldNodeId);
								PublishQueue.dirtObject(page, Action.MOVE, newNodeId);
							}

							for (File file : folder.getFilesAndImages()) {
								// file was removed from old node and moved to new node
								PublishQueue.dirtObject(file, Action.REMOVE, oldNodeId);
								PublishQueue.dirtObject(file, Action.MOVE, newNodeId);
							}
						}
					}
				}
			}

			property = null;
		}

		// dirt the object, if the event was triggered directly on it
		if (depth == 0 && (property == null || Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS))) {
			Action action = PublishQueue.getAction(this, eventMask, property);

			if (action != null) {
				PublishQueue.dirtObject(this, action, channelId);
			}
		} else if (depth == 0 && (isImage() || isFile()) && Events.isEvent(eventMask, Events.UPDATE) && !ObjectTransformer.isEmpty(property)
				&& Arrays.asList(property).contains("binarycontent")) {
			// the binarycontent of a file/image was modified, so also dirt the object
			PublishQueue.dirtObject(this, Action.MODIFY, channelId, "binarycontent");
		}
		if (!ObjectTransformer.isEmpty(property)) {
			// we need to transform the dirted properties in all (dependent)
			// properties that have changed
			property = getModifiedPropertiesArray(property);
		}
		NodeLogger depLogger = DependencyManager.getLogger();

		if (DependencyManager.isLogging()) {
			DependencyManager.logEvent(object, property, eventMask, depth);
		}

		// do not trigger the dependencies, when a CHILD event was triggered
		if (Events.isEvent(eventMask, Events.CHILD)) {
			if (depLogger.isDebugEnabled()) {
				depLogger.debug(
						StringUtils.repeat("  ", depth) + " not triggering event {obj: " + object + ", prop: " + (property != null ? Arrays.asList(property) : null)
						+ ", mask: " + Events.toString(eventMask) + "} on object {" + this + "} (is a child event)");
			}
			return;
		}

		// trigger the normal dependencies here
		if (depLogger.isDebugEnabled()) {
			depLogger.debug(
					StringUtils.repeat("  ", depth) + "trigger event {obj: " + object + ", prop: " + (property != null ? Arrays.asList(property) : null) + ", mask: "
					+ Events.toString(eventMask) + "} on object {" + this + "}");
		}
		Node node = null;
		if (channelId > 0) {
			node = TransactionManager.getCurrentTransaction().getObject(Node.class, channelId);
		}

		// Special treatment for objects that got excluded from a channel
		// The dependency map only has dependencies for the DELETE flag
		int correctedEventMask = eventMask;
		if (Events.isEvent(eventMask, Events.HIDE)) {
			correctedEventMask &= ~Events.HIDE;
			correctedEventMask |= Events.DELETE;
		} else if (Events.isEvent(eventMask, Events.REVEAL)) {
			correctedEventMask &= ~Events.REVEAL;
			correctedEventMask |= Events.CREATE;
		}

		List<Dependency> dependencies = DependencyManager.getAllDependencies(object, property, correctedEventMask, node);

		for (Dependency element : dependencies) {
			try {
				element.triggerDependency(Events.DIRT, depth + 1, node);
			} catch (InconsistentDataException e) {
				depLogger.error("Detected inconsistency while triggering dependency for Events.UPDATE, depth {" + depth + "}", e);
			}
		}

		if (this instanceof LocalizableNodeObject<?>) {
			// The folder was created or is inherited again
			if (Events.isEvent(eventMask, Events.CREATE) || Events.isEvent(eventMask, Events.REVEAL)) {
				// when multichannelling is used and the folder is generated for a
				// channel, trigger "delete" for the folder which was inherited before
				// (and is now hidden by the new localized copy)
				// do this for this channel and all subchannels which will inherit
				// the new folder
				Transaction t = TransactionManager.getCurrentTransaction();

				if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
					// get the channel of the folder
					Node channel = ((LocalizableNodeObject<?>) this).getChannel();

					if (channel != null) {
						// Folder was created in a channel
						handleCreateObjectInChannel(channel, ((LocalizableNodeObject<?>) this).getChannelSet(), depth + 1);
					}
				}

				triggeLocalizeableNodeObjectChangeInFolder(depth);
			}
		}
	}

	/**
	 * Trigger Events.UPDATE on dependencies on the corresponding object list properties ("pages", ...) of the containing folder,
	 * because an object was created/published or removed/taken offline
	 * @param depth	current depth
	 * @throws NodeException
	 */
	protected void triggeLocalizeableNodeObjectChangeInFolder(int depth) throws NodeException {
		Folder mother = (Folder)getParentObject();
		if (mother == null) {
			return;
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
			List<Integer> channelIds = new ArrayList<Integer>();

			@SuppressWarnings("unchecked")
			LocalizableNodeObject<? extends NodeObject> localizeable = (LocalizableNodeObject<? extends NodeObject>)this;
			channelIds.addAll(MultichannellingFactory.getNodeIds(localizeable, true));

			for (Integer channelId : channelIds) {
				try (ChannelTrx channelTrx = new ChannelTrx(channelId)) {
					triggerUpdates(mother, depth, channelId);
				}
			}
			// if object does not belong to a channel, we trigger updates for master nodes
			if (localizeable.getChannel() == null) {
				triggerUpdates(mother, depth, 0);
			}
		} else {
			triggerUpdates(mother, depth, 0);
		}
	}

	/**
	 * Trigger updates on dependent objects like the mother folder and the online attribute
	 * @param mother The mother folder
	 * @param depth Depth
	 * @param channelId The channel ID
	 * @throws TransactionException
	 * @throws NodeException
	 */
	protected void triggerUpdates(Folder mother, int depth, Integer channelId) throws TransactionException, NodeException {
		String relationShipProperties[] = mother.getRelationshipProperty(this);
		if (relationShipProperties != null) {
			// The dependencymap2 stores local channel object IDs, that's why a multichannelling fallback is needed
			Folder localMother = TransactionManager.getCurrentTransaction().getObject(mother, false, true);
			DependencyObject folderDep = new DependencyObject(localMother);

			for (String relationShipProperty : relationShipProperties) {
				// Dirt all properties of the folder the object is in, that list the folder content ("pages", ...)
				mother.triggerEvent(folderDep, ((AbstractFolder) mother).getModifiedPropertiesArray(
						new String[] { relationShipProperty }), Events.UPDATE, depth + 1, channelId);
			}
		}

		// Trigger a change on the property "online", this will update dependencies on folders that were restored from the wastebin
		triggerEvent(new DependencyObject(this), new String[] { "online" }, Events.UPDATE, depth, channelId);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
	 */
	public void dirtCache() throws NodeException {// empty default implementation
	}

	/**
	 * Helper method to find the list of modified properties
	 * @param resolvableProperties map of resolvable properties
	 * @param modifiedDataProperties modified data properties
	 * @param modifiedProperties list of modified properties, new properties will be added here, may be null
	 * @return list of modified properties
	 */
	protected final List<String> getModifiedProperties(Map<String, ? extends AbstractProperty> resolvableProperties,
			String[] modifiedDataProperties, List<String> modifiedProperties) {
		if (modifiedProperties == null) {
			modifiedProperties = new Vector<String>();
		}

		// first add all data properties
		if (!ObjectTransformer.isEmpty(modifiedDataProperties)) {
			for (int i = 0; i < modifiedDataProperties.length; i++) {
				if (!modifiedProperties.contains(modifiedDataProperties[i])) {
					modifiedProperties.add(modifiedDataProperties[i]);
				}
			}
		}

		for (Map.Entry<String, ? extends AbstractProperty> entry : resolvableProperties.entrySet()) {
			AbstractProperty property = entry.getValue();

			if (property.dependsOn(modifiedDataProperties)) {
				String prop = entry.getKey();

				if (!modifiedProperties.contains(prop)) {
					modifiedProperties.add(prop);
				}
			}
		}

		return modifiedProperties;
	}

	/**
	 * Get the list of modified properties.<br/>
	 * When overriding this method, super.getModifiedProperties(String[]) must be called.
	 * @param modifiedDataProperties modified data properties
	 * @return list of modified properties
	 */
	protected List<String> getModifiedProperties(String[] modifiedDataProperties) {
		return getModifiedProperties(resolvableProperties, modifiedDataProperties, null);
	}

	/**
	 * Get the modified properties as String array
	 * @param modifiedDataProperties modified data properties
	 * @return modified properties as string array
	 */
	protected String[] getModifiedPropertiesArray(String[] modifiedDataProperties) {
		List<String> properties = getModifiedProperties(modifiedDataProperties);

		return (String[]) properties.toArray(new String[properties.size()]);
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @return property value
		 */
		public abstract Object get(AbstractContentObject object, String key);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof RenderableResolvable) {
			return ((RenderableResolvable) obj).equals(this);
		} else if (obj instanceof NodeObject) {
			// the other object must be an instance of the same class
			// TODO eventually, when both objects have id 0 (not yet stored)
			// they are not equal?

			NodeObject nodeObj = (NodeObject) obj;

			// if both objects have a global ID, we check for equality
			GlobalId thisGlobalId = getGlobalId();
			GlobalId otherGlobalId = nodeObj.getGlobalId();
			if (thisGlobalId != null && otherGlobalId != null) {
				return thisGlobalId.equals(otherGlobalId);
			}

			// check whether object classes are equal
			Class<? extends NodeObject> thisClass = getObjectInfo().getObjectClass();
			Class<? extends NodeObject> otherClass = nodeObj.getObjectInfo().getObjectClass();
			if (thisClass.isAssignableFrom(otherClass) || otherClass.isAssignableFrom(thisClass)) {
				if (isEmptyId(getId())) {
					if (isEmptyId(nodeObj.getId())) {
						// both instances have an empy id, so check for identity
						return nodeObj == this;
					} else {
						// one id is empty, one is not
						return false;
					}
				} else if (isEmptyId(nodeObj.getId())) {
					// one id is empty, one is not
					return false;
				} else {
					// both id's are not empty, so check for identity of the id's
					return nodeObj.getId().equals(getId());
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getObjectInfo().getObjectClass().hashCode() + getHashKey().hashCode();
	}

	/**
	 * Add the dependency on the resolved object (when it was resolved)
	 * @param property resolved property
	 * @param resolvedObject object (value of the resolved property)
	 */
	public void addDependency(String property, Object resolvedObject) {
		try {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

			if (renderType != null && renderType.doHandleDependencies() && this instanceof NodeObject) {
				renderType.addDependency((NodeObject) this, property);
			}
		} catch (NodeException e) {
			logger.error("Error while adding dependency {" + this + "}/{" + property + "}", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getParentObject()
	 */
	public NodeObject getParentObject() throws NodeException {
		// generally, NodeObjects do not have parents
		return null;
	}

	/**
	 * true if the object is a page
	 * @return true or false depending on current object type
	 */
	public boolean isPage() {
		return false;
	}

	/**
	 * true if the object is a folder
	 * @return true or false depending on current object type
	 */
	public boolean isFolder() {
		return false;
	}

	/**
	 * true if the object is a file
	 * @return true or false depending on current object type
	 */
	public boolean isFile() {
		return false;
	}

	/**
	 * true if the object is a image
	 * @return true or false depending on current object type
	 */
	public boolean isImage() {
		return false;
	}

	/**
	 * true if the object is a tag
	 * @return true or false depending on current object type
	 */
	public boolean isTag() {
		return false;
	}

	/**
	 * Assert that the object instance is editable, throw an exception if not
	 * @throws ReadOnlyException when the object is not editable
	 */
	protected void assertEditable() throws ReadOnlyException {
		if (info == null || !info.isEditable()) {
			failReadOnly();
		}
	}

	/**
	 * Throw the ReadOnlyException. Use this method to let all setter Methods fail in ReadOnly Factory Classes
	 * @throws ReadOnlyException since the object is not editable
	 */
	protected void failReadOnly() throws ReadOnlyException {
		throw new ReadOnlyException("Object instance {" + this + "} is readonly and cannot be modified");
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getPublishedObject()
	 */
	public NodeObject getPublishedObject() throws NodeException {
		// dummy implementation just returns the same object.
		// Currently, the only non-trivial implementation of this method is found in class Page.
		return this;
	}

	/**
	 * Handle triggering events when a new object was created in a channel
	 * @param channel channel
	 * @param channelSet channelset of the created object
	 * @param depth event triggering depth
	 * @throws NodeException
	 */
	protected void handleCreateObjectInChannel(Node channel, Map<Integer, Integer> channelSet,
			int depth) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the master nodes of the channel
		List<Node> masterNodes = channel.getMasterNodes();

		// collect the ids of (possibly) inherited objects here
		Collection<NodeObject> inheritedObjects = new Vector<NodeObject>();

		for (Node node : masterNodes) {
			// is there a copy of the object in the master node?
			Integer localObjectId = channelSet.get(node.getId());

			if (!isEmptyId(localObjectId)) {
				inheritedObjects.add(t.getObject(getObjectInfo().getObjectClass(), localObjectId));
			}

			if (!node.isChannel()) {
				localObjectId = channelSet.get(0);
				if (!isEmptyId(localObjectId)) {
					inheritedObjects.add(t.getObject(getObjectInfo().getObjectClass(), localObjectId));
				}
			}
		}

		deleteInheritedObjectsFromSubchannels(inheritedObjects, channel, channelSet, depth);
	}

	/**
	 * Trigger "delete" events for all inherited objects for the given channel
	 * and all subchannels (that have no localized copies)
	 * @param inheritedPages collection of inherited objects
	 * @param channel channel
	 * @param channelSet channelset of the object
	 * @param depth current event depth
	 * @throws NodeException
	 */
	private void deleteInheritedObjectsFromSubchannels(
			Collection<? extends NodeObject> inheritedObjects, Node channel,
			Map<Integer, Integer> channelSet, int depth) throws NodeException {
		// trigger the "delete" event for all inherited pages for the channel
		for (NodeObject object : inheritedObjects) {
			DependencyObject depObj = new DependencyObject(object, (NodeObject) null);

			object.triggerEvent(depObj, null, Events.DELETE, depth + 1, ObjectTransformer.getInt(channel.getId(), 0));
		}

		// get subchannels of the channel
		Collection<Node> channels = channel.getChannels();

		for (Node subChannel : channels) {
			// check whether a localized copy exists in the channel
			if (!channelSet.containsKey(subChannel.getId())) {
				// no localized copy in the subchannel, so the subchannel
				// inherited one of the objects
				deleteInheritedObjectsFromSubchannels(inheritedObjects, subChannel, channelSet, depth);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copyFrom(com.gentics.lib.base.object.NodeObject)
	 */
	public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
		// check whether this object is editable
		assertEditable();
		// given object must not be null
		if (original == null) {
			throw new NodeException("Cannot copy null onto {" + this + "}");
		}
		// given object must be of appropriate class
		if (!getObjectInfo().getObjectClass().isAssignableFrom(original.getObjectInfo().getObjectClass())) {
			throw new NodeException("Cannot copy {" + original + "} over {" + this + "}");
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getUdate()
	 */
	public int getUdate() {
		return udate;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// default implementation just returns the udate
		return getUdate();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getGlobalId()
	 */
	public GlobalId getGlobalId() {
		return globalId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#setGlobalId(com.gentics.lib.base.object.NodeObject.GlobalId)
	 */
	public void setGlobalId(GlobalId globalId) throws ReadOnlyException, NodeException {
		assertEditable();
		if (this.globalId != null && !this.globalId.equals(globalId)) {
			throw new NodeException("Cannot set globalid to " + globalId + ", globalid is already set to " + this.globalId);
		}
		this.globalId = globalId;
	}

	/**
	 * Get the Uuid for inserting it into a new record. If the {@link #globalId} is not set, this method will return an empty string,
	 * which will cause the insert trigger in the db to create a new Uuid
	 * @return Uuid to insert or an empty string
	 */
	protected String getUuid() {
		if (globalId == null) {
			return "";
		} else {
			return globalId.toString();
		}
	}

	/**
	 * Custom object serialization. This implementation will add the info stored in {@link NodeObjectInfo} (which is not serializable)
	 * after the object.
	 *
	 * @param oos output stream
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		// default serialization
		oos.defaultWriteObject();

		// make sure, that info is initialized
		getObjectInfo();
		// after the object, write info from the NodeObjectInfo
		oos.writeObject(info.getObjectClass());
		oos.writeInt(info.getVersionTimestamp());
		oos.writeBoolean(info.isEditable());
	}

	/**
	 * Custom object deserialization. This implementation will fetch info stored after the object and reconstruct the instance of {@link NodeObjectInfo}
	 *
	 * @param ois input stream
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();

		// after the object, we expect objectclass, version timestamp and editable flag
		Object clazzObject = ois.readObject();
		if (!(clazzObject instanceof Class)) {
			throw new IOException(String.format("Expected to read object class, but got %s", clazzObject));
		} else {
			Class<? extends NodeObject> clazz = (Class<? extends NodeObject>)clazzObject;

			int versionTimestamp = ois.readInt();
			boolean editable = ois.readBoolean();

			try {
				if (editable) {
					info = Trx.supply(t -> t.createObjectInfo(clazz, editable));
				} else {
					info = Trx.supply(t -> t.createObjectInfo(clazz, versionTimestamp));
				}
			} catch (NodeException e) {
				throw new IOException(e);
			}
		}
	}

	protected void updateMissingReferences() throws NodeException {
		List<Integer> valueIds = new ArrayList<>();
		List<Integer> referenceIds = new ArrayList<>();

		DBUtils.select(
			"SELECT id, source_id, reference_name FROM missingreference WHERE source_tablename = ? AND target_uuid = ?",
			stmt -> {
				stmt.setString(1, "value");
				stmt.setString(2, getUuid());
			},
			rs -> {
				while (rs.next()) {
					referenceIds.add(rs.getInt("id"));
					valueIds.add(rs.getInt("source_id"));
				}

				return null;
			});


		for (int valueId : valueIds) {
			DBUtils.executeUpdate("UPDATE `value` SET value_ref = ? WHERE id = ?", new Object[] { getId(), valueId });
		}

		DBUtils.executeMassStatement("DELETE FROM `missingreference` WHERE id IN", null, referenceIds, 1, null, Transaction.DELETE_STATEMENT);
	}
}
