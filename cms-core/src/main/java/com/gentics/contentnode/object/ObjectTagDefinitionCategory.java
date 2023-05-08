package com.gentics.contentnode.object;

import static com.gentics.contentnode.i18n.I18NHelper.toI18nMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionCategoryModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.ObjectPropertyCategory;

/**
 * Class for ObjectTag Definition Categories
 */
@SuppressWarnings("serial")
@TType(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY)
public abstract class ObjectTagDefinitionCategory extends AbstractContentObject implements NamedNodeObject {

	/**
	 * The ttype of the objecttag definition category object.
	 */
	public static final int TYPE_OBJTAG_DEF_CATEGORY = 108;

	/**
	 * Transform the node object into its REST Model
	 */
	public final static BiFunction<ObjectTagDefinitionCategory, ObjectPropertyCategory, ObjectPropertyCategory> NODE2REST = (
			nodeCategory, category) -> {
		category.setId(ObjectTransformer.getInt(nodeCategory.getId(), 0));
		category.setGlobalId(nodeCategory.getGlobalId() != null ? nodeCategory.getGlobalId().toString() : null);
		category.setName(nodeCategory.getName());
		category.setNameI18n(I18NHelper.toI18nMap(nodeCategory.getNameI18n()));

		return category;
	};

	/**
	 * Transform the node object into its REST Model
	 */
	public final static BiFunction<ObjectPropertyCategory, ObjectTagDefinitionCategory, ObjectTagDefinitionCategory> REST2NODE = (
			from, to) -> {
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		if (from.getNameI18n() != null) {
			I18NHelper.forI18nMap(from.getNameI18n(), (translation, id) -> to.setName(translation, id));
		}
		return to;
	};

	/**
	 * Transform the node object into its REST Model
	 */
	public final static Function<ObjectTagDefinitionCategory, ObjectPropertyCategory> TRANSFORM2REST = nodeCategory -> {
		return NODE2REST.apply(nodeCategory, new ObjectPropertyCategory());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<ObjectTagDefinitionCategory, ObjectTagDefinitionCategoryModel, ObjectTagDefinitionCategoryModel> NODE2DEVTOOL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setName(toI18nMap(from.getNameI18n()));
		to.setSortOrder(from.getSortorder());
		return to;
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<ObjectTagDefinitionCategory>> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, NodeObjectProperty<ObjectTagDefinitionCategory>>();
		resolvableProperties.put("name", new NodeObjectProperty<>((o, key) -> o.getName(), "name"));
		resolvableProperties.put("sortorder", new NodeObjectProperty<>((o, key) -> o.getSortorder(), "sortorder"));
	}

	/**
	 * Create an instance
	 * @param id id
	 * @param info object info
	 */
	protected ObjectTagDefinitionCategory(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Object get(String key) {
		NodeObjectProperty<ObjectTagDefinitionCategory> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	/**
	 * Get the name
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Get the name as i18n string
	 * @return name
	 */
	public abstract I18nString getNameI18n();

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
	 * Get the sortorder
	 * @return sortorder
	 */
	@FieldGetter("sortorder")
	public abstract int getSortorder();

	/**
	 * Set the sortorder
	 * @param sortorder
	 * @throws ReadOnlyException
	 */
	@FieldSetter("sortorder")
	public void setSortorder(int sortorder) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the object tag definitions of this category
	 * @return list of object tag definitions
	 * @throws NodeException
	 */
	public abstract List<ObjectTagDefinition> getObjectTagDefinitions() throws NodeException;
}
