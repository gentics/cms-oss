package com.gentics.contentnode.object;

import static com.gentics.contentnode.devtools.Synchronizer.unwrap;
import static com.gentics.contentnode.devtools.Synchronizer.wrap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.model.ObjectTagModel;
import com.gentics.contentnode.devtools.model.TemplateModel;
import com.gentics.contentnode.devtools.model.TemplateTagModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * This is the template object of the object layer.
 */
@TType(Template.TYPE_TEMPLATE)
public interface Template extends TagContainer, GCNRenderable, ObjectTagContainer, LocalizableNodeObject<Template>, Resolvable, SynchronizableNodeObject, NamedNodeObject {
	/**
	 * the keynames which the template can handle.
	 */
	public static final String[] RENDER_KEYS = new String[] { "template"};

	public static final int TYPE_TEMPLATE = 10006;
    
	public static final Integer TYPE_TEMPLATE_INTEGER = new Integer(TYPE_TEMPLATE);

	/**
	 * Maximum length for names
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Maximum length for descriptions
	 */
	public final static int MAX_DESCRIPTION_LENGTH = 255;

	/**
	 * Lambda that transforms the node model of a Template into the rest model
	 */
	public final static BiFunction<Template, com.gentics.contentnode.rest.model.Template, com.gentics.contentnode.rest.model.Template> NODE2REST = (
			nodeTemplate, restTemplate) -> {
		restTemplate.setId(ObjectTransformer.getInt(nodeTemplate.getId(), 0));
		restTemplate.setGlobalId(nodeTemplate.getGlobalId() != null ? nodeTemplate.getGlobalId().toString() : null);
		restTemplate.setName(nodeTemplate.getName());
		restTemplate.setDescription(nodeTemplate.getDescription());
		restTemplate.setCreator(ModelBuilder.getUser(nodeTemplate.getCreator()));
		restTemplate.setEditor(ModelBuilder.getUser(nodeTemplate.getEditor()));
		restTemplate.setCdate(nodeTemplate.getCDate().getIntTimestamp());
		restTemplate.setEdate(nodeTemplate.getEDate().getIntTimestamp());
		restTemplate.setLocked(nodeTemplate.isLocked());
		restTemplate.setMarkupLanguage(MarkupLanguage.TRANSFORM2REST.apply(nodeTemplate.getMarkupLanguage()));

		restTemplate.setInherited(nodeTemplate.isInherited());
		Template master = nodeTemplate.getMaster();

		if (master != null && !nodeTemplate.equals(master)) {
			restTemplate.setMasterId(ObjectTransformer.getInteger(master.getId(), null));
		}

		Node channel = nodeTemplate.getChannel();

		if (channel != null) {
			restTemplate.setChannelId(ObjectTransformer.getInteger(channel.getId(), null));
		}
		restTemplate.setChannelSetId(ObjectTransformer.getInteger(nodeTemplate.getChannelSetId(), null));
		restTemplate.setMaster(nodeTemplate.isMaster());

		if (channel == null) {
			// get the currently set channel
			Transaction t = TransactionManager.getCurrentTransaction();

			channel = t.getChannel();
			if (channel != null) {
				channel = ModelBuilder.getMaster(channel);
			}
		}
		if (channel != null) {
			restTemplate.setInheritedFrom(channel.getFolder().getName());
		}
		Node masterNode = nodeTemplate.getMaster().getChannel();

		if (masterNode == null) {
			// get the currently set channel
			Transaction t = TransactionManager.getCurrentTransaction();

			masterNode = t.getChannel();
			if (masterNode != null) {
				masterNode = ModelBuilder.getMaster(masterNode);
			}
		}
		if (masterNode != null) {
			restTemplate.setMasterNode(masterNode.getFolder().getName());
		}

		return restTemplate;
	};

	/**
	 * Lambda that transforms the node model of a Template into the rest model
	 */
	public final static Function<Template, com.gentics.contentnode.rest.model.Template> TRANSFORM2REST = nodeTemplate -> {
		return NODE2REST.apply(nodeTemplate, new com.gentics.contentnode.rest.model.Template());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<Template, TemplateModel, TemplateModel> NODE2DEVTOOL = (from, to) -> {
		Node channel = from.getChannel();
		if (channel != null) {
			to.setChannelId(channel.getGlobalId().toString());
		}

		to.setDescription(from.getDescription());
		to.setGlobalId(from.getGlobalId().toString());
		to.setName(from.getName());
		to.setType(from.getMarkupLanguage().getName());

		unwrap(() -> {
			Map<String, TemplateTag> orderedTemplateTags = new TreeMap<>(from.getTemplateTags());
			to.setTemplateTags(orderedTemplateTags.values().stream().map(templateTag -> {
				return wrap(() -> TemplateTag.NODE2DEVTOOL.apply(templateTag, new TemplateTagModel()));
			}).collect(Collectors.toList()));

			TreeMap<String, ObjectTag> orderedObjectTags = new TreeMap<>(from.getObjectTags());
			to.setObjectTags(orderedObjectTags.values().stream().map(objectTag -> {
				return wrap(() -> ObjectTag.NODE2DEVTOOL.apply(objectTag, new ObjectTagModel()));
			}).collect(Collectors.toList()));
		});

		return to;
	};

	/**
	 * get the name of the template.
	 * @return the name of the template.
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * Set the name of the template
	 * @param name new name
	 * @return previous name
	 * @throws ReadOnlyException when the template was not fetched for updating
	 */
	@FieldSetter("name")
	String setName(String name) throws ReadOnlyException;

	/**
	 * get the description of the template.
	 * @return the description of the template.
	 */
	@FieldGetter("description")
	String getDescription();

	/**
	 * Set the description of the template
	 * @param description new description
	 * @return previous description
	 * @throws ReadOnlyException
	 */
	@FieldSetter("description")
	String setDescription(String description) throws ReadOnlyException;

	/**
	 * get the source code of the template.
	 * @return the source code of the template.
	 */
	@FieldGetter("ml")
	String getSource();

	/**
	 * Set the source code of the template
	 * @param source new source code
	 * @return previous source code
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ml")
	String setSource(String source) throws ReadOnlyException;

	/**
	 * get the id of the markup language of the template.
	 * @return the id of the markup language, or 0 if not set.
	 */
	@FieldGetter("ml_id")
	Integer getMlId();

	/**
	 * Set the markup language id of the template.
	 * @param mlId new markup language id
	 * @return previous markup language id
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ml_id")
	Integer setMlId(Integer mlId) throws ReadOnlyException;

	/**
	 * get the markup language of the template.
	 * @return the markup langauge, or null if not set.
	 * @throws NodeException 
	 */
	MarkupLanguage getMarkupLanguage() throws NodeException;

	/**
	 * check, if the template is currenty locked by a user.
	 * @return true, if the template is currently locked.
	 */
	boolean isLocked() throws NodeException;

	/**
	 * Check whether the template is locked by the current user
	 * @return true if the template is locked by the current user
	 * @throws NodeException
	 */
	boolean isLockedByCurrentUser() throws NodeException;

	/**
	 * Get the user, by which the template is currently locked (if
	 * {@link #isLocked()} returns true) or null if the template is not currently
	 * locked.
	 * @return a user or null
	 * @throws NodeException
	 */
	public abstract SystemUser getLockedBy() throws NodeException;

	/**
	 * check, if the template can be unlinked from its folder. The template
	 * can't be unlinked if its only linked to one folder and if there are pages
	 * that reference to it.
	 * 
	 * @param folder Folder from which the template shall be removed.
	 * @return true, if the template can be unlinked from its folder.
	 */
	boolean isUnlinkable(Folder folder) throws NodeException;

	/**
	 * Check whether the template is inherited from a master (in multichannel) into
	 * the current channel
	 * @return true when the template is inherited, false if not or
	 *         multichannelling is disabled
	 * @throws NodeException
	 */
	boolean isInherited() throws NodeException;

	/**
	 * Check whether this template is a master or a localized copy
	 * @return true for a master template, false for a localized copy
	 * @throws NodeException
	 */
	boolean isMaster() throws NodeException;

	/**
	 * Retrieve template creator.
	 * @return creator of the template
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * Retrieve template editor.
	 * @return last editor of the template
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * get the creation date as a unix timestamp 
	 * @return creation date unix timestamp
	 */
	ContentNodeDate getCDate();

	/**
	 * get the edit date as a unix timestamp 
	 * @return edit date unix timestamp
	 */
	ContentNodeDate getEDate();

	/**
	 * get a templatettag of this template by name.
	 * @param name the name of the tag.
	 * @return the templatetag, or null if not found.
	 */
	default TemplateTag getTemplateTag(String name) throws NodeException {
		return (TemplateTag) getTemplateTags().get(name);
	}

	/**
	 * get a private templatettag of this template by name.
	 * @param name the name of the tag.
	 * @return the templatetag, or null if not found.
	 */
	default TemplateTag getPrivateTemplateTag(String name) throws NodeException {
		return (TemplateTag) getPrivateTemplateTags().get(name);
	}

	/**
	 * get the list of all templatetags with their keynames.
	 * @return a map of all templatetags by keyname, as String->TemplateTag.
	 * @throws NodeException 
	 */
	Map<String, TemplateTag> getTemplateTags() throws NodeException;

	@Override
	Map<String, TemplateTag> getTags() throws NodeException;

	/**
	 * Get the folder in which the template was created.
	 * If the folder in which the template was initially created does not exist any more, this method will return null!
	 * @return folder or null
	 * @throws NodeException
	 */
	Folder getFolder() throws NodeException;

	/**
	 * Get the list of folders, the template is linked to
	 * @return list of folders, the template is linked to
	 * @throws NodeException
	 */
	List<Folder> getFolders() throws NodeException;

	/**
	 * Set the folder id of the template
	 * @param folderId new folder id of the template
	 * @return previous folder id of the template
	 * @throws ReadOnlyException
	 */
	Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException;

	/**
	 * Adds a folder to the list of folders the template is linked to
	 * @throws NodeException
	 */
	void addFolder(Folder folder) throws NodeException;

	/**
	 * returns all private (ie. non-editable) template tags.
	 * @throws NodeException 
	 *
	 */
	Map<String, TemplateTag> getPrivateTemplateTags() throws NodeException;

	/**
	 * Set the channel information for the template. The channel information consist
	 * of the channelId and the channelSetId. The channelId identifies the
	 * channel, for which the object is valid. If set to 0 (which is the
	 * default), the object is not a localized copy and no local object in a
	 * channel, but is a normal object in a node. The channelSetId groups the
	 * master object and all its localized copies in channel together. This
	 * method may only be called for new templates.
	 * 
	 * @param channel
	 *            id new channel id. If set to 0, this object will be a master
	 *            object in a node and the channelSetId must be given as null
	 *            (which will create a new channelSetId)
	 * @param channelSetId
	 *            id of the channelset. If set to null, a new channelSetId will
	 *            be generated and the object will be a master (in a node or in
	 *            a channel)
	 * @throws ReadOnlyException when the object is not editable
	 * @throws NodeException in case of other errors
	 */
	default void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException {
		setChannelInfo(channelId, channelSetId, false);
	}

	@Override
	Template getMaster() throws NodeException;

	@Override
	Template getNextHigherObject() throws NodeException;

	/**
	 * Get the templategroup id
	 * @return templategroup id
	 */
	Integer getTemplategroupId();

	/**
	 * Set the templategroup id
	 * @param templategroupId templategroup id
	 * @throws ReadOnlyException
	 */
	void setTemplategroupId(Integer templategroupId) throws ReadOnlyException;

	/**
	 * Set the global id of the templategroup
	 * @param globalId global id
	 * @throws ReadOnlyException
	 */
	void setGlobalTemplategroupId(GlobalId globalId) throws ReadOnlyException, NodeException;

	/**
	 * Get all pages using this template
	 * @return list of pages using this template
	 * @throws NodeException
	 */
	List<Page> getPages() throws NodeException;

	/**
	 * Get a List of all Nodes where pages would use this very version of the template
	 * @return List of Nodes
	 * @throws NodeException
	 */
	Set<Node> getNodes() throws NodeException;

	/**
	 * Get the (non editable) set of nodes, this template is assigned to
	 * @return Set of nodes
	 * @throws NodeException
	 */
	Set<Node> getAssignedNodes() throws NodeException;

	/**
	 * Save the template
	 * @param syncPages true to synchronize the pages while saving, false if not
	 * @return true when the template was modified, false if not
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	boolean save(boolean syncPages) throws InsufficientPrivilegesException, NodeException;

	/**
	 * Update pages (compatible parts of contenttags with changed tagtypes should be migrated)
	 * @param commitAfter number of pages, after which the transaction shall be committed (but not closed). If set to 0, no intermediate commit will be done.
	 * @param maxMessages maximum number of messages collected in the result object
	 * @return result object
	 * @throws NodeException
	 */
	default UpdatePagesResult updatePages(int commitAfter, int maxMessages) throws NodeException {
		return updatePages(commitAfter, maxMessages, null, false);
	}

	/**
	 * Update pages (compatible parts of contenttags with changed tagtypes should be migrated)
	 * @param commitAfter number of pages, after which the transaction shall be committed (but not closed). If set to 0, no intermediate commit will be done.
	 * @param maxMessages maximum number of messages collected in the result object
	 * @param tagnames optional list of tagnames to update
	 * @param force true for force incompatible migrations
	 * @return result object
	 * @throws NodeException
	 */
	UpdatePagesResult updatePages(int commitAfter, int maxMessages, List<String> tagnames, boolean force) throws NodeException;
}
