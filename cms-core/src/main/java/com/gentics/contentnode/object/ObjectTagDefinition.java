package com.gentics.contentnode.object;

import static com.gentics.contentnode.i18n.I18NHelper.toI18nMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionCategoryModel;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionModel;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.TriFunction;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Class for ObjectTag Definitions.
 * Note: ObjectTagDefinitions are special, because they are mainly stored in objtag (having obj_id = 0), alongside with ObjectTag instances.
 * The id and globalid of ObjectTagDefinition will represent the id and globalid entry of objtag.
 * Additional information about the ObjectTagDefinition is stored in the table objprop. Also assignment to nodes is done via the crosstable
 * objprop_node, that references the objprop entry.
 */
@SuppressWarnings("serial")
@TType(ObjectTagDefinition.TYPE_OBJTAG_DEF)
public abstract class ObjectTagDefinition extends AbstractContentObject implements SynchronizableNodeObject, NamedNodeObject {

	/**
	 * The ttype for objecttag definitions in general
	 */
	public static final int TYPE_OBJTAG_DEFS = 12;

	/**
	 * The ttype for objecttag definitions per type (target type is the id)
	 */
	public static final int TYPE_OBJTAG_DEF_FOR_TYPE = 14;

	/**
	 * The ttype of the objecttag definition object.
	 */
	public static final int TYPE_OBJTAG_DEF = 40;

	/**
	 * Maximum name length
	 */
	public final static int MAX_NAME_LENGTH = 255;

	/**
	 * Lambda that transforms the node model of an object property definition to its rest model
	 */
	public final static BiFunction<ObjectTagDefinition, ObjectProperty, ObjectProperty> NODE2REST = (nodeDefinition, restModel) -> {
		restModel.setId(ObjectTransformer.getInt(nodeDefinition.getId(), 0));
		restModel.setGlobalId(nodeDefinition.getGlobalId() != null ? nodeDefinition.getGlobalId().toString() : null);
		restModel.setName(nodeDefinition.getName());
		restModel.setDescription(nodeDefinition.getDescription());
		restModel.setDescriptionI18n(I18NHelper.toI18nMap(nodeDefinition.getDescriptionI18n()));
		restModel.setNameI18n(I18NHelper.toI18nMap(nodeDefinition.getNameI18n()));
		restModel.setType(nodeDefinition.getTargetType());
		restModel.setKeyword(nodeDefinition.getObjectTag().getName());
		restModel.setConstructId(nodeDefinition.getObjectTag().getConstructId());
		restModel.setRequired(nodeDefinition.getObjectTag().isRequired());
		restModel.setInheritable(nodeDefinition.getObjectTag().isInheritable());
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
			if (nodeDefinition.getTargetType() == Page.TYPE_PAGE) {
				restModel.setSyncContentset(nodeDefinition.isSyncContentset());
				restModel.setSyncVariants(nodeDefinition.isSyncVariants());
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING)) {
				restModel.setSyncChannelset(nodeDefinition.isSyncChannelset());
			}
		}
		restModel.setRestricted(nodeDefinition.isRestricted());
		ObjectTagDefinitionCategory cat = nodeDefinition.getCategory();
		if (cat != null) {
			restModel.setCategoryId(cat.getId());
		}

		return restModel;
	};

	/**
	 * Lambda that transforms the node model of an object property definition to its rest model
	 */
	public final static Function<ObjectTagDefinition, ObjectProperty> TRANSFORM2REST = nodeDefinition -> {
		return NODE2REST.apply(nodeDefinition, new ObjectProperty());
	};

	/**
	 * Transform the rest model into the node model
	 */
	public final static BiFunction<ObjectProperty, ObjectTagDefinition, ObjectTagDefinition> REST2NODE = (restModel, nodeDefinition) -> {
		if (restModel.getDescriptionI18n() != null) {
			I18NHelper.forI18nMap(restModel.getDescriptionI18n(), (translation, id) -> nodeDefinition.setDescription(translation, id));
		} else if (restModel.getDescription() != null) {
			nodeDefinition.setDescription(restModel.getDescription(), ContentNodeHelper.getLanguageId(1));
		}
		if (restModel.getNameI18n() != null) {
			I18NHelper.forI18nMap(restModel.getNameI18n(), (translation, id) -> nodeDefinition.setName(translation, id));
		} else if (restModel.getName() != null) {
			nodeDefinition.setName(restModel.getName(), ContentNodeHelper.getLanguageId(1));
		}
		if (restModel.getInheritable() != null) {
			nodeDefinition.getObjectTag().setInheritable(restModel.getInheritable());
		}
		if (restModel.getRequired() != null) {
			nodeDefinition.getObjectTag().setRequired(restModel.getRequired());
		}
		if (restModel.getConstructId() != null) {
			nodeDefinition.getObjectTag().setConstructId(restModel.getConstructId());
		} else if (restModel.getConstruct() != null) {
			nodeDefinition.getObjectTag().setConstructId(restModel.getConstruct().getId());
		}
		if (restModel.getKeyword() != null) {
			nodeDefinition.getObjectTag().setName(restModel.getKeyword());
		}
		if (restModel.getType() != null) {
			nodeDefinition.getObjectTag().setObjType(restModel.getType());
		}
		if (restModel.getCategoryId() != null) {
			nodeDefinition.setCategoryId(restModel.getCategoryId());
		}
		if (restModel.getRequired() != null) {
			nodeDefinition.getObjectTag().setRequired(restModel.getRequired());
		}
		if (restModel.getInheritable() != null && nodeDefinition.getTargetType() == Folder.TYPE_FOLDER) {
			nodeDefinition.getObjectTag().setInheritable(restModel.getInheritable());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC)) {
			if (nodeDefinition.getTargetType() == Page.TYPE_PAGE && restModel.getSyncContentset() != null) {
				nodeDefinition.setSyncContentset(restModel.getSyncContentset());
			}
			if (nodeDefinition.getTargetType() == Page.TYPE_PAGE && restModel.getSyncVariants() != null) {
				nodeDefinition.setSyncVariants(restModel.getSyncVariants());
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING) && restModel.getSyncChannelset() != null) {
				nodeDefinition.setSyncChannelset(restModel.getSyncChannelset());
			}
		}
		if (restModel.getRestricted() != null) {
			nodeDefinition.setRestricted(restModel.getRestricted());
		}
		return nodeDefinition;
	};

	/**
	 * Lambda that embeds the rest model of the construct into the rest model of the object property definition
	 */
	public final static Consumer<ObjectProperty> EMBED_CONSTRUCT = restModel -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.getObject(Construct.class, restModel.getConstructId());
		if (construct != null) {
			restModel.setConstruct(Construct.TRANSFORM2REST.apply(construct));
		}
	};

	/**
	 * Lambda that embeds the rest model of the category into the rest model of the object property definition
	 */
	public final static Consumer<ObjectProperty> EMBED_CATEGORY = restModel -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		ObjectTagDefinitionCategory category = t.getObject(ObjectTagDefinitionCategory.class, restModel.getCategoryId());
		if (category != null) {
			restModel.setCategory(ObjectTagDefinitionCategory.TRANSFORM2REST.apply(category));
		}
	};

	/**
	 * Lambda that transforms the object property definition into the translated type name
	 */
	public final static Function<ObjectTagDefinition, String> TYPENAME = nodeDefinition -> {
		switch (nodeDefinition.getTargetType()) {
		case Folder.TYPE_FOLDER:
			return I18NHelper.get("402.folder");
		case Template.TYPE_TEMPLATE:
			return I18NHelper.get("templates");
		case Page.TYPE_PAGE:
			return I18NHelper.get("pages");
		case File.TYPE_FILE:
			return I18NHelper.get("files");
		case ImageFile.TYPE_IMAGE:
			return I18NHelper.get("images");
		default:
			return "";
		}
	};

	/**
	 * Function to convert the object to the devtools model with optional feature check
	 */
	public final static TriFunction<ObjectTagDefinition, ObjectTagDefinitionModel, Boolean, ObjectTagDefinitionModel> NODE2DEVTOOL_FEATURE_CHECK = (from, to, check) -> {
		if (check == null) {
			check = true;
		}
		to.setGlobalId(from.getGlobalId().toString());
		to.setType(ObjectTagDefinitionTypeModel.fromValue(from.getObjectTag().getObjType()));
		to.setKeyword(from.getObjectTag().getName());
		to.setName(toI18nMap(from.getNameId()));
		to.setDescription(toI18nMap(from.getDescriptionId()));
		to.setConstructId(from.getObjectTag().getConstruct().getGlobalId().toString());
		to.setRequired(from.getObjectTag().isRequired());
		if (from.getTargetType() == Folder.TYPE_FOLDER) {
			to.setInheritable(from.getObjectTag().isInheritable());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.OBJTAG_SYNC) || !check) {
			if (from.getTargetType() == Page.TYPE_PAGE) {
				to.setSyncContentset(from.isSyncContentset());
				to.setSyncVariants(from.isSyncVariants());
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING) || !check) {
				to.setSyncChannelset(from.isSyncChannelset());
			}
		}
		to.setRestricted(from.isRestricted());

		ObjectTagDefinitionCategory category = from.getCategory();
		if (category != null) {
			to.setCategory(ObjectTagDefinitionCategory.NODE2DEVTOOL.apply(category, new ObjectTagDefinitionCategoryModel()));
		}

		List<Node> nodes = from.getNodes();

		if (nodes != null) {
			to.setNodeIds(nodes.stream().map(node -> node.getGlobalId().toString()).collect(Collectors.toList()));
		}

		return to;
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<ObjectTagDefinition, ObjectTagDefinitionModel, ObjectTagDefinitionModel> NODE2DEVTOOL = (from, to) -> {
		return NODE2DEVTOOL_FEATURE_CHECK.apply(from, to, true);
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<ObjectTagDefinition>> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, NodeObjectProperty<ObjectTagDefinition>>();
		resolvableProperties.put("keyword", new NodeObjectProperty<>((o, key) -> o.getObjectTag().getName(), "keyword"));
		resolvableProperties.put("name", new NodeObjectProperty<>((o, key) -> o.getName(), "name"));
		resolvableProperties.put("description", new NodeObjectProperty<>((o, key) -> o.getDescription(), "description"));
		resolvableProperties.put("type", new NodeObjectProperty<>((o, key) -> o.getTargetType()));
		resolvableProperties.put("keyword", new NodeObjectProperty<>((o, key) -> o.getObjectTag().getName()));
		resolvableProperties.put("required", new NodeObjectProperty<>((o, key) -> o.getObjectTag().isRequired()));
		resolvableProperties.put("inheritable", new NodeObjectProperty<>((o, key) -> o.getObjectTag().isInheritable()));
		resolvableProperties.put("construct", new NodeObjectProperty<>((o, key) -> o.getObjectTag().getConstruct()));
		resolvableProperties.put("category", new NodeObjectProperty<>((o, key) -> o.getCategory()));
		resolvableProperties.put("syncChannelset", new NodeObjectProperty<>((o, key) -> o.isSyncChannelset()));
		resolvableProperties.put("syncVariants", new NodeObjectProperty<>((o, key) -> o.isSyncVariants()));
		resolvableProperties.put("syncContentset", new NodeObjectProperty<>((o, key) -> o.isSyncContentset()));
		resolvableProperties.put("restricted", new NodeObjectProperty<>((o, key) -> o.isRestricted()));
	}

	@Override
	public Object get(String key) {
		NodeObjectProperty<ObjectTagDefinition> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * Get the locale-backed name
	 *
	 * @return
	 */
	public abstract I18nString getNameI18n();

	/**
	 * Get the locale-backed description
	 *
	 * @return
	 */
	public abstract I18nString getDescriptionI18n();

	/**
	 * Create an instance
	 * @param id id
	 * @param info object info
	 */
	protected ObjectTagDefinition(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get the name
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Get the name id
	 * @return name id
	 */
	public abstract int getNameId();

	/**
	 * Set the name in the given language
	 * @param name name
	 * @param language language
	 * @throws ReadOnlyException
	 */
	public void setName(String name, int language) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the description
	 * @return description
	 */
	public abstract String getDescription();

	/**
	 * Get the description id
	 * @return description id
	 */
	public abstract int getDescriptionId();

	/**
	 * Set the description in the given language
	 * @param description description
	 * @param language language
	 * @throws ReadOnlyException
	 */
	public void setDescription(String description, int language) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the "objecttag" for this definition
	 * @return the objecttag
	 * @throws NodeException
	 */
	public abstract ObjectTag getObjectTag() throws NodeException;

	/**
	 * Get the objprop id or 0 if non set
	 * @return objprop id
	 */
	public abstract int getObjectPropId();

	/**
	 * Get the target ttype
	 * @return target ttype
	 */
	public abstract int getTargetType();

	/**
	 * Set the target type
	 * @param targetType target type
	 * @throws ReadOnlyException
	 */
	public void setTargetType(int targetType) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the category
	 * @return category
	 * @throws NodeException
	 */
	public abstract ObjectTagDefinitionCategory getCategory() throws NodeException;

	/**
	 * Set the category id
	 * @param id category id
	 * @throws ReadOnlyException
	 */
	public void setCategoryId(Integer id) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the nodes to which this definition is assigned
	 * @return list of nodes
	 * @throws NodeException
	 */
	public abstract List<Node> getNodes() throws NodeException;

	/**
	 * Determine whether the definition is visible in the given node
	 * @param node node
	 * @return false, when node is null or the definition is restricted to other nodes, true otherwise
	 * @throws NodeException
	 */
	public boolean isVisibleIn(Node node) throws NodeException {
		if (!isRestricted()) {
			return true;
		}
		if (node == null) {
			return false;
		}
		try (NoMcTrx noMcTrx = new NoMcTrx()) {
			return getNodes().contains(node);
		}
	}

	/**
	 * Get the list of real objecttags referencing this definition
	 * @return list of objecttags
	 * @throws NodeException
	 */
	public abstract List<ObjectTag> getObjectTags() throws NodeException;

	/**
	 * Get the flag for "Synchronize Contentset"
	 * @return syncContentset flag
	 */
	@FieldGetter("sync_contentset")
	public abstract boolean isSyncContentset();

	/**
	 * Set the flag for "Synchronize Contentset"
	 * @param syncContentset flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("sync_contentset")
	public void setSyncContentset(boolean syncContentset) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the flag for "Synchronize Channelset"
	 * @return syncChannelset flag
	 */
	@FieldGetter("sync_channelset")
	public abstract boolean isSyncChannelset();

	/**
	 * Set the flag for "Synchronize Channelset"
	 * @param syncChannelset flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("sync_channelset")
	public void setSyncChannelset(boolean syncChannelset) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the flag for "Synchronize Variants"
	 * @return syncVariants flag
	 */
	@FieldGetter("sync_variants")
	public abstract boolean isSyncVariants();

	/**
	 * Set the flag for "Synchronize Variants"
	 * @param syncVariants flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("sync_variants")
	public void setSyncVariants(boolean syncVariants) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the flag for "Restricted"
	 * @return restricted flag
	 */
	@FieldGetter("restricted")
	public abstract boolean isRestricted();

	/**
	 * Set the flag for "Restricted"
	 * @param restricted flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("restricted")
	public void setRestricted(boolean restricted) throws ReadOnlyException {
		failReadOnly();
	}
}
