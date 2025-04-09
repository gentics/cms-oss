/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Construct.java,v 1.20.6.1 2011-03-07 16:36:43 norbert Exp $
 */
package com.gentics.contentnode.object;

import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.model.ConstructCategoryModel;
import com.gentics.contentnode.devtools.model.ConstructModel;
import com.gentics.contentnode.devtools.model.PartModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.EditorControlStyle;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * This is the object for Constructs. A Construct contains a list of Parts, as well as the default values
 * for the parts.
 */
@SuppressWarnings("serial")
@TType(Construct.TYPE_CONSTRUCT)
public abstract class Construct extends ValueContainer implements SynchronizableNodeObject, I18nNamedNodeObject, MetaDateNodeObject {
	public final static Integer TYPE_CONSTRUCTS_INTEGER = new Integer(10003);

	public static final int TYPE_CONSTRUCT = 10004;

	public static final int TYPE_CONSTRUCT_CATEGORY = 10203;

	public final static Integer TYPE_CONSTRUCT_CATEGORY_INTEGER = new Integer(TYPE_CONSTRUCT_CATEGORY);

	/**
	 * Maximum length for keywords
	 */
	public final static int MAX_KEYWORD_LENGTH = 64;

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<com.gentics.contentnode.rest.model.Construct, Construct, Construct> REST2NODE = (from, to) -> {
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		if (from.getDescriptionI18n() != null) {
			I18NHelper.forI18nMap(from.getDescriptionI18n(), (translation, id) -> to.setDescription(translation, id));
		}
		if (from.getNameI18n() != null) {
			I18NHelper.forI18nMap(from.getNameI18n(), (translation, id) -> to.setName(translation, id));
		} else if (from.getName() != null) {
			to.setName(from.getName(), 1);
		}
		if (from.getKeyword() != null) {
			to.setKeyword(from.getKeyword());
		}
		if (from.getLiveEditorTagName() != null) {
			to.setLiveEditorTagName(from.getLiveEditorTagName());
		}
		if (from.getExternalEditorUrl() != null) {
			to.setExternalEditorUrl(from.getExternalEditorUrl());
		}
		if (from.getCategoryId() != null) {
			to.setConstructCategoryId(from.getCategoryId());
		}
		unwrap(()-> {
			from.getAutoEnableOptional().ifPresent(wrap(to::setAutoEnable));
			from.getMayContainSubtagsOptional().ifPresent(wrap(to::setMayContainSubtags));
			from.getMayBeSubtagOptional().ifPresent(wrap(to::setMayBeSubtag));
		});

		if (from.getEditorControlStyle() != null) {
			to.setEditorControlStyle(from.getEditorControlStyle());
		}

		if (from.getEditorControlsInside() != null) {
			to.setEditorControlInside(from.getEditorControlsInside());
		}

		if (from.getOpenEditorOnInsert() != null) {
			to.setEditOnInsert(from.getOpenEditorOnInsert());
		}

		if (from.getParts() != null) {
			List<Part> parts = to.getParts();
			parts.clear();

			unwrap(() -> {
				if (from.getParts() != null) {
					from.getParts().forEach(p -> {
						wrap(() -> {
							Transaction t = TransactionManager.getCurrentTransaction();

							Part editablePart = t.getObject(Part.class, p.getGlobalId(), true);
							if (editablePart == null) {
								editablePart = t.createObject(Part.class);
							}
							Part.REST2NODE.apply(p, editablePart);
							parts.add(editablePart);
						});
					});
				}
			});

			// sort parts
			parts.sort((p1, p2) -> p1.getPartOrder() - p2.getPartOrder());
		}
		return to;
	};

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<Construct, com.gentics.contentnode.rest.model.Construct, com.gentics.contentnode.rest.model.Construct> NODE2REST = (
			from, to) -> {
		Transaction t = TransactionManager.getCurrentTransaction();

		to.setId(ObjectTransformer.getInt(from.getId(), 0));
		to.setGlobalId(from.getGlobalId() != null ? from.getGlobalId().toString() : null);
		to.setName(from.getName().toString());
		to.setDescription(from.getDescription().toString());
		to.setDescriptionI18n(I18NHelper.toI18nMap(from.getDescription()));
		to.setNameI18n(I18NHelper.toI18nMap(from.getName()));
		to.setKeyword(from.getKeyword());
		to.setCreator(ModelBuilder.getUser(from.getCreator()));
		to.setCdate(from.getCDate().getIntTimestamp());
		to.setEditor(ModelBuilder.getUser(from.getEditor()));
		to.setEdate(from.getEDate().getIntTimestamp());
		to.setAutoEnable(from.isAutoEnable());
		to.setMayBeSubtag(from.mayBeSubtag());
		to.setMayContainSubtags(from.mayContainSubtags());
		to.setOpenEditorOnInsert(from.editOnInsert());
		to.setEditorControlStyle(from.editorControlStyle());
		to.setEditorControlsInside(from.editorControlInside());
		to.setExternalEditorUrl(from.getExternalEditorUrl());
		ConstructCategory category = from.getConstructCategory();

		if (category == null) {
			to.setCategoryId(null);
			to.setCategory(null);
			to.setVisibleInMenu(true);
		} else {
			to.setCategoryId(category.getId());
			//Important: Check VIEW rights for construct categories independently from VIEW rights on Content.Admin
			to.setVisibleInMenu(PermHandler.perm(t.getConnection(), t.getUserId(), ConstructCategory.TYPE_CONSTRUCT_CATEGORY, category.getId(), PermHandler.PERM_VIEW));
		}

		// set the parts
		List<com.gentics.contentnode.rest.model.Part> parts = new Vector<com.gentics.contentnode.rest.model.Part>();

		for (Part part : from.getParts()) {
			parts.add(ModelBuilder.getPart(part));
		}
		to.setParts(parts);
		return to;
	};

	/**
	 * Lambda that transforms the node model of a Construct into the rest model
	 */
	public final static Function<Construct, com.gentics.contentnode.rest.model.Construct> TRANSFORM2REST = nodeConstruct -> {
		return NODE2REST.apply(nodeConstruct, new com.gentics.contentnode.rest.model.Construct());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<Construct, ConstructModel, ConstructModel> NODE2DEVTOOL = (from, to) -> {
		to.setDescription(I18NHelper.toI18nMap(from.getDescription()));
		to.setGlobalId(from.getGlobalId().toString());
		to.setKeyword(from.getKeyword());
		to.setLiveEditorTagName(from.getLiveEditorTagName());
		to.setMayBeSubtag(from.mayBeSubtag());
		to.setMayContainsSubtags(from.mayContainSubtags());
		to.setEditOnInsert(from.editOnInsert());
		to.setEditorControlStyle(from.editorControlStyle());
		to.setEditorControlsInside(from.editorControlInside());
		to.setAutoEnable(from.isAutoEnable());
		to.setName(I18NHelper.toI18nMap(from.getName()));
		to.setExternalEditorUrl(from.getExternalEditorUrl());

		unwrap(() -> {
			to.setParts(from.getParts().stream().map(part -> {
				return wrap(() -> Part.NODE2DEVTOOL.apply(part, new PartModel()));
			}).collect(Collectors.toList()));
		});

		ConstructCategory constructCategory = from.getConstructCategory();
		if (constructCategory != null) {
			to.setCategory(ConstructCategory.NODE2DEVTOOL.apply(constructCategory, new ConstructCategoryModel()));
		}

		return to;
	};

	public static Consumer<com.gentics.contentnode.rest.model.Construct> EMBED_CATEGORY = restConstruct -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		ConstructCategory category = t.getObject(ConstructCategory.class, restConstruct.getCategoryId());
		if (category == null) {
			return;
		}

		restConstruct.setCategory(ConstructCategory.TRANSFORM2REST.apply(category));
	};

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, NodeObjectProperty<Construct>> resolvableProperties;

	static {
		resolvableProperties = new HashMap<String, NodeObjectProperty<Construct>>();
		resolvableProperties.put("name", new NodeObjectProperty<>((o, key) -> o.getName().toString(), "name"));
		resolvableProperties.put("description", new NodeObjectProperty<>((o, key) -> o.getDescription().toString(), "description"));
		resolvableProperties.put("keyword", new NodeObjectProperty<>((o, key) -> o.getKeyword(), "keyword"));
		resolvableProperties.put("category", new NodeObjectProperty<>((o, key) -> o.getConstructCategory(), "category"));
	}

	@Override
	public Object get(String key) {
		NodeObjectProperty<Construct> prop = resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	protected Construct(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	public String getTypeKeyword() {
		return "construct";
	}

	/**
	 * get the name of the construct.
	 * @return the name of the construct.
	 */
	public abstract I18nString getName();

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
	 * Get the name id of the construct
	 * @return name if
	 */
	public abstract int getNameId();

	/**
	 * get the keyword of the construct.
	 * @return the keyword of the construct.
	 */
	@FieldGetter("keyword")
	public abstract String getKeyword();

	/**
	 * Set the keyword
	 * @param keyword keyword
	 * @throws ReadOnlyException
	 */
	@FieldSetter("keyword")
	public void setKeyword(String keyword) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get a description of this construct.
	 * @return the description for this construct.
	 */
	public abstract I18nString getDescription();

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
	 * get a list of all parts, sorted by the partorder.
	 * @return all parts in the correct order.
	 * @throws NodeException when getting the parts failed
	 */
	public abstract List<Part> getParts() throws NodeException;

	/**
	 * Get a list of all contenttags built from this construct
	 * @return contenttags
	 * @throws NodeException
	 */
	public abstract List<ContentTag> getContentTags() throws NodeException;

	/**
	 * Get a list of all templatetags built from this construct
	 * @return templatettags
	 * @throws NodeException
	 */
	public abstract List<TemplateTag> getTemplateTags() throws NodeException;

	/**
	 * Get a list of all objecttags built from this construct
	 * @return objecttags
	 * @throws NodeException
	 */
	public abstract List<ObjectTag> getObjectTags() throws NodeException;

	/**
	 * Get the construct category
	 * @return construct category (may be null)
	 * @throws NodeException
	 */
	public abstract ConstructCategory getConstructCategory() throws NodeException;

	/**
	 * Get the construct category ID
	 * @return construct category ID (may be null)
	 * @throws NodeException
	 */
	public abstract Integer getConstructCategoryId() throws NodeException;

	/**
	 * Set the construct category id
	 * @param id construct category id
	 * @throws ReadOnlyException
	 */
	public void setConstructCategoryId(Integer id) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the nodes to which this construct is assigned to
	 * @return list of nodes
	 * @throws NodeException
	 */
	public abstract List<Node> getNodes() throws NodeException;

	/**
	 * Get a list of all tags of this construct
	 * @return list of all tags
	 * @throws NodeException
	 */
	public List<Tag> getTags() throws NodeException {
		List<Tag> tags = new Vector<Tag>(getContentTags());

		tags.addAll(getTemplateTags());
		tags.addAll(getObjectTags());
		return tags;
	}

	/**
	 * Check whether the construct is used
	 * @return true, iff the construct is used
	 * @throws NodeException
	 */
	public abstract boolean isUsed() throws NodeException;

	public String getStackHashKey() {
		return "construct:" + getHashKey();
	}

	/**
	 * Check whether this construct is editable
	 * @return true when this construct is editable, false if not
	 * @throws NodeException
	 */
	public boolean isEditable() throws NodeException {
		List<Part> parts = getParts();

		for (Part part : parts) {
			if (part.isEditable()) {
				// found an editable part
				return true;
			}
		}

		// no part is editable
		return false;
	}

	/**
	 * Check whether tags generated from this construct shall automatically be enabled
	 * @return true if autoenable is on, false if not
	 * @throws NodeException
	 */
	@FieldGetter("autoenable")
	public abstract boolean isAutoEnable() throws NodeException;

	/**
	 * Set the autoenable flag
	 * @param autoEnable autoenable flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("autoenable")
	public void setAutoEnable(boolean autoEnable) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Check whether this construct is inline editable
	 * @return true when this construct is inline editable, false if not
	 * @throws NodeException
	 */
	public boolean isInlineEditable() throws NodeException {
		List<Part> parts = getParts();

		for (Part part : parts) {
			if (part.isInlineEditable()) {
				// the part is marked as "inline editable", so check whether the
				// parttype is capable
				if (part.getPartType(part.getDefaultValue()).isLiveEditorCapable()) {
					// found an inline editable part
					return true;
				}
			}
		}

		// no part is inline editable
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ValueContainer#resolvePartsWithShortCuts()
	 */
	protected boolean resolvePartsWithShortCuts() {
		// constructs will always resolve parts by their shortcuts
		return true;
	}

	/**
	 * Get the tagname of the tag which shall be wrapped around tags of this construct when rendering for live-editor.
	 * @return configured tagname or null if none configured
	 */
	@FieldGetter("liveeditortagname")
	public abstract String getLiveEditorTagName();

	/**
	 * Set the liveeditor tagname
	 * @param liveEditorTagName liveeditor tagname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("liveeditortagname")
	public void setLiveEditorTagName(String liveEditorTagName) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the external editor URL
	 * @return URL
	 */
	@FieldGetter("external_editor_url")
	public abstract String getExternalEditorUrl();

	/**
	 * Set the external editor URL
	 * @param externalEditorUrl URL
	 * @throws ReadOnlyException
	 */
	@FieldSetter("external_editor_url")
	public void setExternalEditorUrl(String externalEditorUrl) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * retrieve creator
	 * @return creator
	 * @throws NodeException
	 */
	public abstract SystemUser getCreator() throws NodeException;

	/**
	 * retrieve last editor
	 * @return last editor
	 * @throws NodeException
	 */
	public abstract SystemUser getEditor() throws NodeException;

	/**
	 * Get the ml_id
	 * @return ml_id
	 */
	@FieldGetter("ml_id")
	public abstract int getMlId();

	/**
	 * Set the mlId
	 * @param mlId mlId
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ml_id")
	public void setMlId(int mlId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Return true if tags of this construct may contain subtags
	 * @return true if tags may contain subtags
	 */
	@FieldGetter("childable")
	public abstract boolean mayContainSubtags();

	/**
	 * Set whether tags of this construct may containe subtags
	 * @param mayContainSubtags true or false
	 * @throws ReadOnlyException
	 */
	@FieldSetter("childable")
	public void setMayContainSubtags(boolean mayContainSubtags) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Return true if tags of this construct may be subtags
	 * @return true if tags may be subtags
	 */
	@FieldGetter("intext")
	public abstract boolean mayBeSubtag();

	/**
	 * Set whether tags of this construct may be subtags
	 * @param mayBeSubtag true or false
	 * @throws ReadOnlyException
	 */
	@FieldSetter("intext")
	public void setMayBeSubtag(boolean mayBeSubtag) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Return true if tag editor should be opened immediately when the
	 * construct is inserted.
	 * @return true if tag editor should be opened on insert.
	 */
	@FieldGetter("edit_on_insert")
	public abstract boolean editOnInsert();

	/**
	 * Set whether the tag editor should be opened immediately when the
	 * construct is inserted.
	 *
	 * @param editOnInsert Whether the tag editor should be opened on insert.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("edit_on_insert")
	public void setEditOnInsert(boolean editOnInsert) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get mode where the editor icons should be placed for this construct.
	 * @return Mode where the editor icons should be placed for this construct.
	 */
	@FieldGetter("editor_control_style")
	public abstract EditorControlStyle editorControlStyle();

	/**
	 * Set the mode where the editor controls should be placed.
	 * @param editorControlStyle The mode where the editor controls should be placed.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("editor_control_style")
	public void setEditorControlStyle(EditorControlStyle editorControlStyle) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Return true if the editor controls should be displayed inside the rendered construct.
	 * @return true if the editor controls should be displayed inside the rendered construct.
	 */
	@FieldGetter("editor_control_inside")
	public abstract boolean editorControlInside();

	/**
	 * Set whether the editor controls should be placed inside the rendered construct.
	 * @param editorControlInside Whether the editor controls should be placed inside the rendered construct.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("editor_control_inside")
	public void setEditorControlInside(boolean editorControlInside) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Check whether the construct contains an editable overview part
	 * @return true if the construct contains an editalbe overview part, false if not
	 * @throws NodeException
	 */
	public boolean containsOverviewPart() throws NodeException {
		// Iterate over all parts
		List<Part> parts = getParts();

		for (Part part : parts) {
			PartType partType = part.getPartType(part.getDefaultValue());

			if (partType instanceof OverviewPartType && part.isEditable()) {
				return true;
			}
		}

		// found no overview parttypes
		return false;
	}

	/**
	 * Check whether tags of this construct can be converted to use the other construct, without (possibly) losing data.
	 * Conversion would lose data in one of the following cases:
	 * <ol>
	 * <li>For an editable part, there is no editable part in the other construct with the same keyword</li>
	 * <li>The part types of the matching editable parts are different and are not both text based</li>
	 * </ol>
	 * @param other other construct
	 * @return true iff tags can safely be converted
	 * @throws NodeException
	 */
	public abstract boolean canConvertTo(Construct other) throws NodeException;
}
