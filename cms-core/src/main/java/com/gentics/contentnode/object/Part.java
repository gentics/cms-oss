/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Part.java,v 1.13.2.1 2011-02-10 13:43:42 tobiassteiner Exp $
 */
package com.gentics.contentnode.object;

import java.net.URI;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.devtools.model.PartModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.PartTypeFactory;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.factory.object.SelectPartSetting;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.model.OverviewSetting;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * The part object of the object layer.
 */
@SuppressWarnings("serial")
@TType(Part.TYPE_PART)
public abstract class Part extends AbstractContentObject implements I18nNamedNodeObject {

	public static final int TEXT = 1;
	public static final int TEXTHMTL = 2;
	public static final int HTML = 3;
	public static final int URLPAGE = 4;
	public static final int TAGGLOBAL = 5;
	public static final int URLIMAGE = 6;
	public static final int URLFILE = 8;
	public static final int TEXTSHORT = 9;
	public static final int TEXTHTMLLONG = 10;
	public static final int TAGPAGE = 11;
	public static final int OVERVIEW = 13;
	public static final int LIST = 15;
	public static final int LISTUNORDERED = 16;
	public static final int LISTORDERED = 17;
	public static final int SELECTIMAGEHEIGHT = 18;
	public static final int SELECTIMAGEWIDTH = 19;
	public static final int TAGTEMPLATE = 20;
	public static final int HTMLLONG = 21;
	public static final int FILELOCALPATH = 22;
	public static final int TABLEEXT = 23;
	public static final int URLFOLDER = 25;
	public static final int JAVAEDITOR = 26;
	public static final int DHTMLEDITOR = 27;
	public static final int SELECTSINGLE = 29;
	public static final int SELECTMULTIPLE = 30;
	public static final int CHECKBOX = 31;
	public static final int DATASOURCE = 32;
	public static final int VELOCITY = 33;
	public static final int BREADCRUMB = 34;
	public static final int NAVIGATION = 35;
	public static final int HTMLCUSTOMFORM = 36;
	public static final int TEXTCUSTOMFORM = 37;
	public static final int FILEUPLOAD = 38;
	public static final int FOLDERUPLOAD = 39;
	public static final int NODE = 40;
	
	/**
	 * The ttype of the part object.
	 */
	public static final int TYPE_PART = 10025;

	private int isValueless = -1;

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<com.gentics.contentnode.rest.model.Part, Part, Part> REST2NODE = (from, to) -> {
		to.setEditable(from.isEditable() ? (from.isLiveEditable() ? 2 : 1) : 0);
		if (from.getGlobalId() != null) {
			to.setGlobalId(new GlobalId(from.getGlobalId()));
		}
		to.setHidden(from.isHidden());
		to.setKeyname(ObjectTransformer.getString(from.getKeyword(), ""));
		if (from.getMarkupLanguageId() != null) {
			to.setMlId(from.getMarkupLanguageId());
		}
		if (from.getNameI18n() != null) {
			I18NHelper.forI18nMap(from.getNameI18n(), (translation, id) -> to.setName(translation, id));
		} else if (from.getName() != null) {
			to.setName(from.getName(), ContentNodeHelper.getLanguageId(1));
		}
		if (from.getPartOrder() != null) {
			to.setPartOrder(from.getPartOrder());
		}
		to.setPartTypeId(from.getTypeId());
		to.setPolicy(from.getPolicy());
		to.setRequired(from.isMandatory());
		to.setHideInEditor(from.isHideInEditor());
		to.setExternalEditorUrl(from.getExternalEditorUrl());

		switch (to.getPartTypeId()) {
		case Part.TEXT:
		case Part.TEXTHMTL:
		case Part.HTML:
		case Part.TEXTSHORT:
		case Part.TEXTHTMLLONG:
		case Part.HTMLLONG:
			if (from.getRegex() != null) {
				to.setInfoInt(from.getRegex().getId());
			}
			break;
		default:
			break;
		}

		switch (to.getPartTypeId()) {
		case Part.LIST:
		case Part.LISTORDERED:
		case Part.LISTUNORDERED:
			to.setInfoText(from.getHtmlClass());
			break;
		case Part.OVERVIEW:
		{
			OverviewSetting overviewSettings = from.getOverviewSettings();
			if (overviewSettings != null) {
				OverviewPartSetting overviewPartSetting = new OverviewPartSetting(to);
				OverviewPartSetting.REST2NODE.apply(from.getOverviewSettings(), overviewPartSetting);
				overviewPartSetting.setTo(to);
			}
			break;
		}
		default:
			break;
		}

		if (SelectPartSetting.isSelectPart(to) && from.getSelectSettings() != null) {
			SelectPartSetting selectPartSetting = new SelectPartSetting(to);
			SelectPartSetting.REST2NODE.apply(from.getSelectSettings(), selectPartSetting);
			selectPartSetting.setTo(to);
		}

		if (from.getDefaultProperty() != null) {
			// for new parts, we create a new default value
			if (Part.isEmptyId(to.getId()) || Part.isEmptyId(to.getDefaultValue().getId())) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Value defaultValue = t.createObject(Value.class);
				defaultValue.setPart(to);
				to.setDefaultValue(defaultValue);
			}

			to.getDefaultValue().getPartType().fromProperty(from.getDefaultProperty());
		}

		return to;
	};

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<Part, com.gentics.contentnode.rest.model.Part, com.gentics.contentnode.rest.model.Part> NODE2REST = (
			from, to) -> {
		to.setId(from.getId());
		to.setEditable(from.isEditable());
		to.setGlobalId(from.getGlobalId().toString());
		to.setLiveEditable(from.isInlineEditable());
		to.setKeyword(from.getKeyname());
		to.setMarkupLanguageId(ObjectTransformer.getInt(from.getMlId(), 0));
		to.setName(from.getName().toString());
		to.setNameI18n(I18NHelper.toI18nMap(from.getName()));
		to.setPartOrder(from.getPartOrder());
		to.setPolicy(from.getPolicy());
		to.setMandatory(from.isRequired());
		to.setTypeId(from.getPartTypeId());
		to.setHidden(from.isHidden());
		to.setHideInEditor(from.isHideInEditor());
		to.setExternalEditorUrl(from.getExternalEditorUrl());
		to.setType(com.gentics.contentnode.rest.model.Property.Type.get(from.getPartTypeId()));
		to.setTypeId(from.getPartTypeId());

		switch (from.getPartTypeId()) {
		case Part.TEXT:
		case Part.TEXTHMTL:
		case Part.HTML:
		case Part.TEXTSHORT:
		case Part.TEXTHTMLLONG:
		case Part.HTMLLONG:
			if (from.getRegex() != null) {
				to.setRegex(ModelBuilder.getRegex(from.getRegex()));
			}
			break;
		case Part.LIST:
		case Part.LISTORDERED:
		case Part.LISTUNORDERED:
			to.setHtmlClass(from.getInfoText());
			break;
		case Part.OVERVIEW:
			to.setOverviewSettings(OverviewPartSetting.TRANSFORM2REST.apply(new OverviewPartSetting(from)));
			break;
		default:
			break;
		}

		Value defaultValue = from.getDefaultValue();
		if (defaultValue.getId() > 0) {
			to.setDefaultProperty(defaultValue.getPartType().toProperty());
		}
		if (SelectPartSetting.isSelectPart(from)) {
			to.setSelectSettings(SelectPartSetting.TRANSFORM2REST.apply(new SelectPartSetting(from)));
		}
		return to;
	};

	/**
	 * Lambda that transforms the node model of a Part into the rest model
	 */
	public final static Function<Part, com.gentics.contentnode.rest.model.Part> TRANSFORM2REST = nodeConstruct -> {
		return NODE2REST.apply(nodeConstruct, new com.gentics.contentnode.rest.model.Part());
	};

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<Part, PartModel, PartModel> NODE2DEVTOOL = (from, to) -> {
		to.setEditable(from.isEditable());
		to.setGlobalId(from.getGlobalId().toString());
		to.setInlineEditable(from.isInlineEditable());
		to.setKeyword(from.getKeyname());
		to.setMlId(ObjectTransformer.getInt(from.getMlId(), 0));
		to.setName(I18NHelper.toI18nMap(from.getName()));
		to.setOrder(from.getPartOrder());
		to.setPolicy(from.getPolicy());
		to.setRequired(from.isRequired());
		to.setTypeId(from.getPartTypeId());
		to.setVisible(from.isVisible());
		to.setHideInEditor(from.isHideInEditor());
		to.setExternalEditorUrl(from.getExternalEditorUrl());

		switch (from.getPartTypeId()) {
		case Part.TEXT:
		case Part.TEXTHMTL:
		case Part.HTML:
		case Part.TEXTSHORT:
		case Part.TEXTHTMLLONG:
		case Part.HTMLLONG:
			to.setRegexId(from.getInfoInt());
			break;
		default:
			break;
		}

		switch (from.getPartTypeId()) {
		case Part.LIST:
		case Part.LISTORDERED:
		case Part.LISTUNORDERED:
			to.setHtmlClass(from.getInfoText());
			break;
		default:
			break;
		}

		return to;
	};

	protected Part(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * get the name of the part.
	 * @return the name of the part.
	 */
	public abstract I18nString getName();

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
	 * get the keyname of the part.
	 * @return the keyname of this part.
	 */
	@FieldGetter("keyword")
	public abstract String getKeyname();

	/**
	 * Set the keyname of the part
	 * @param keyname keyname
	 * @throws ReadOnlyException
	 */
	@FieldSetter("keyword")
	public void setKeyname(String keyname) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * check, if this part is visible.
	 * @return true, if this part is visible.
	 */
	public boolean isVisible() {
		return !isHidden();
	}

	/**
	 * Check if this part is hidden
	 * @return true if this part is hidden
	 */
	@FieldGetter("hidden")
	public abstract boolean isHidden();

	/**
	 * Set whether this part is hidden
	 * @param hidden true to hide the part
	 * @throws ReadOnlyException
	 */
	@FieldSetter("hidden")
	public void setHidden(boolean hidden) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * check if this part is editable in tags.
	 * @return true, if this part's value can be edited.
	 */
	public abstract boolean isEditable();

	/**
	 * Get the editable flag
	 * @return 0 for not editable, 1 for editable (but not inline), > 1 for inline editable
	 */
	@FieldGetter("editable")
	public abstract int getEditable();

	/**
	 * Set this part editable
	 * @param editable 0 for not editable, 1 for editable (but not inline), > 1 for inline editable
	 * @throws ReadOnlyException
	 */
	@FieldSetter("editable")
	public void setEditable(int editable) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Check whether this part is inline editable.
	 * @return true, when this part's value can be edited with the inline editor
	 */
	public abstract boolean isInlineEditable();

	/**
	 * check, if this part must not be empty.
	 * @return true, if a value must be given for this part.
	 */
	@FieldGetter("required")
	public abstract boolean isRequired();

	/**
	 * Set whether the part is required
	 * @param required true for required
	 * @throws ReadOnlyException
	 */
	@FieldSetter("required")
	public void setRequired(boolean required) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the construct id which contains this part.
	 * @return the id of the construct containing this part.
	 */
	public abstract Object getConstructId();

	/**
	 * get the construct which contains this part.
	 * @return the construct containing this part.
	 * @throws NodeException 
	 */
	public abstract Construct getConstruct() throws NodeException;

	/**
	 * Set the construct id
	 * @param constructId construct id
	 * @throws ReadOnlyException
	 * @throws NodeException 
	 */
	public void setConstructId(Integer constructId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * get the number by which the parts should be sorted.
	 * @return the order number of this part.
	 */
	@FieldGetter("partorder")
	public abstract int getPartOrder();

	/**
	 * Set the partorder
	 * @param partOrder partprder
	 * @throws ReadOnlyException
	 */
	@FieldSetter("partorder")
	public void setPartOrder(int partOrder) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * the the id of the parttype to use to render this part.
	 * @return the id of the parttype of this part.
	 */
	@FieldGetter("type_id")
	public abstract int getPartTypeId();

	/**
	 * Set the type id of the parttype
	 * @param partTypeId
	 * @throws ReadOnlyException
	 */
	@FieldSetter("type_id")
	public void setPartTypeId(int partTypeId) throws ReadOnlyException {
		failReadOnly();
	}

	@FieldGetter("partoption_id")
	public abstract int getPartoptionId();

	@FieldSetter("partoption_id")
	public void setPartoptionId(int partOptionId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the markup language id of this part.
	 * @return the id of the markup language object of this part, or 0 if not set.
	 */
	@FieldGetter("ml_id")
	public abstract Object getMlId();

	/**
	 * Set the markup language id
	 * @param mlId markup language id
	 * @throws ReadOnlyException
	 */
	@FieldSetter("ml_id")
	public void setMlId(int mlId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the markup language of this part.
	 * @return the markup language of this part, or null if not set.
	 * @throws NodeException 
	 */
	public abstract MarkupLanguage getMarkupLanguage() throws NodeException;

	/**
	 * get the info int value of this part.
	 * @return the info int value of this part.
	 */
	@FieldGetter("info_int")
	public abstract int getInfoInt();

	/**
	 * Set the info into value
	 * @param infoInt info int value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("info_int")
	public void setInfoInt(int infoInt) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the info text value of this part.
	 * @return the info text of this part.
	 */
	@FieldGetter("info_text")
	public abstract String getInfoText();

	/**
	 * Set the info text value
	 * @param infoText info text value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("info_text")
	public void setInfoText(String infoText) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * @return the policy URI configured for this part,
	 *   or null, if no policy has been configured. 
	 */
	public abstract URI getPolicyURI();

	/**
	 * Get the policy
	 * @return policy
	 */
	@FieldGetter("policy")
	public abstract String getPolicy();

	/**
	 * Set the policy
	 * @param policy policy
	 * @throws ReadOnlyException
	 * @throws NodeException 
	 */
	@FieldSetter("policy")
	public void setPolicy(String policy) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get the flag for hiding part in editor
	 * @return flag
	 */
	@FieldGetter("hide_in_editor")
	public abstract boolean isHideInEditor();

	/**
	 * Set flag for hiding part in editor
	 * @param hideInEditor flag
	 * @throws ReadOnlyException
	 */
	@FieldSetter("hide_in_editor")
	public void setHideInEditor(boolean hideInEditor) throws ReadOnlyException {
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
	 * get the default value for this part.
	 * @return the default value for this part.
	 * @throws NodeException 
	 */
	public abstract Value getDefaultValue() throws NodeException;

	/**
	 * Set the default value
	 * @param value default value
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void setDefaultValue(Value value) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * get the parttype for this part, using a given value.
	 * @param value the value which should be passed to the parttype.
	 * @return the parttype which handles this part.
	 */
	public PartType getPartType(Value value) throws NodeException {
		return PartTypeFactory.getInstance().getPartType(getPartTypeId(), value);
	}    

	/**
	 * Check whether the part is valueless or not
	 * @return true for valueless parts, false if not
	 * @throws NodeException
	 */
	public boolean isValueless() throws NodeException {
		return isValueless(true);
	}

	/**
	 * Check whether the part is valueless or not
	 * @param failIfPartTypeNotFound true if this method should fail, when the part type is not found
	 * @return true for valueless parts, false if not
	 * @throws NodeException
	 */
	public boolean isValueless(boolean failIfPartTypeNotFound) throws NodeException {
		if (isValueless == -1) {
			isValueless = PartTypeFactory.getInstance().isValueless(getPartTypeId(), failIfPartTypeNotFound) ? 1 : 0;
		}
		return isValueless == 1;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the part's udate
		int udate = getUdate();

		// check the default value
		udate = Math.max(udate, getDefaultValue().getEffectiveUdate());
		return udate;
	}

	/**
	 * Check whether this part matches the given parttype
	 * @param partType parttype to check
	 * @return true if the part matches the parttype, false if not
	 * @throws NodeException
	 */
	public boolean matches(PartType partType) throws NodeException {
		return PartTypeFactory.getInstance().matches(this, partType);
	}

	/**
	 * Get the regular expression (for text parttypes)
	 * @return regex instance or null
	 * @throws NodeException
	 */
	public Regex getRegex() throws NodeException {
		// only text parttypes may have regexes
		switch (getPartTypeId()) {
		case 1:
		case 2:
		case 3:
		case 9:
		case 10:
		case 21:
			return TransactionManager.getCurrentTransaction().getObject(Regex.class, getInfoInt());
		default:
			return null;
		}
	}
}
