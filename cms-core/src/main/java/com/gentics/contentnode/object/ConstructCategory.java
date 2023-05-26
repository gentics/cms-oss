package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.devtools.model.ConstructCategoryModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * Class for Construct Categories
 */
@TType(ConstructCategory.TYPE_CONSTRUCT_CATEGORY)
public abstract class ConstructCategory extends AbstractContentObject implements I18nNamedNodeObject {

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<com.gentics.contentnode.rest.model.ConstructCategory, ConstructCategory, ConstructCategory> REST2NODE = (from, to) -> {
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		if (from.getNameI18n() != null) {
			I18NHelper.forI18nMap(from.getNameI18n(), (translation, id) -> to.setName(translation, id));
		} else if (from.getName() != null) {
			to.setName(from.getName(), 1);
		}
		if (from.getSortOrder() != null) {
			to.setSortorder(from.getSortOrder());
		}
		return to;
	};

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<ConstructCategory, com.gentics.contentnode.rest.model.ConstructCategory, com.gentics.contentnode.rest.model.ConstructCategory> NODE2REST = (
			from, to) -> {
		to.setId(ObjectTransformer.getInt(from.getId(), 0));
		to.setGlobalId(from.getGlobalId() != null ? from.getGlobalId().toString() : null);
		to.setName(from.getName().toString());
		to.setNameI18n(I18NHelper.toI18nMap(from.getName()));
		to.setSortOrder(from.getSortorder());

		// set the constructs
		Map<String, com.gentics.contentnode.rest.model.Construct> constructs = new HashMap<String, com.gentics.contentnode.rest.model.Construct>();

		for (Construct part : from.getConstructs()) {
			constructs.put(part.getKeyword(), ModelBuilder.getConstruct(part));
		}
		to.setConstructs(constructs);
		return to;
	};

	/**
	 * Lambda that transforms the node model of a Construct into the rest model
	 */
	public final static Function<ConstructCategory, com.gentics.contentnode.rest.model.ConstructCategory> TRANSFORM2REST = nodeConstruct -> {
		return NODE2REST.apply(nodeConstruct, new com.gentics.contentnode.rest.model.ConstructCategory());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<ConstructCategory, ConstructCategoryModel, ConstructCategoryModel> NODE2DEVTOOL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setName(I18NHelper.toI18nMap(from.getName()));
		to.setSortOrder(from.getSortorder());
		return to;
	};


	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7525761751665157710L;

	/**
	 * ttype of a single construct category
	 */
	public static final int TYPE_CONSTRUCT_CATEGORY = 10203;

	/**
	 * ttype of a construct categories
	 */
	public static final int TYPE_CONSTRUCT_CATEGORIES = 10211;

	/**
	 * Create an instance
	 * @param id id
	 * @param info object info
	 */
	protected ConstructCategory(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Object get(String key) {
		switch (key) {
		case "id":
			return getId();

		case "name":
			return getName();

		case "sortorder":
			return getSortorder();

		default:
			return super.get(key);
		}
	}

	/**
	 * get the name of the construct category.
	 * @return the name of the construct category.
	 */
	public abstract I18nString getName();

	/**
	 * Get the name Id of the construct category
	 * @return name Id
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
	 * @param sortorder sortorder
	 * @throws ReadOnlyException
	 */
	@FieldSetter("sortorder")
	public void setSortorder(int sortorder) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the constructs of this category
	 * @return list of constructs
	 * @throws NodeException
	 */
	public abstract List<Construct> getConstructs() throws NodeException;
}
