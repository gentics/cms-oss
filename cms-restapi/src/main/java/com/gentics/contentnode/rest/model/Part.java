package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Property.Type;

/**
 * Rest Model for parts
 */
@XmlRootElement
public class Part implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3480557545074958705L;

	/**
	 * Markup language ID
	 */
	private Integer markupLanguageId;

	/**
	 * Id of the part
	 */
	private Integer id;

	/**
	 * Global Id of the part
	 */
	private String globalId;

	/**
	 * Name of the part
	 */
	private String name;

	/**
	 * Keyword of the part
	 */
	private String keyword;

	/**
	 * True when the part is hidden
	 */
	private boolean hidden;

	/**
	 * True when the part is editable
	 */
	private boolean editable;

	/**
	 * True when the part is live editable
	 */
	private boolean liveEditable;

	/**
	 * True when the part is mandatory
	 */
	private boolean mandatory;

	/**
	 * Part type
	 */
	private Type type;

	/**
	 * Part type ID
	 */
	private int typeId;

	/**
	 * Default property
	 */
	private Property defaultProperty;

	/**
	 * Regular expression
	 */
	private RegexModel regex;

	/**
	 * Flag to hide part in tag editor
	 */
	private boolean hideInEditor;

	/**
	 * URL to the external editor
	 */
	private String externalEditorUrl;

	/**
	 * Options (when the type is {@link Type#SELECT} or {@link Type#MULTISELECT})
	 */
	private List<SelectOption> options;

	/**
	 * Overview settings
	 */
	private OverviewSetting overviewSettings;

	/**
	 * Select settings
	 */
	private SelectSetting selectSettings;

	/**
	 * HTML markup class
	 */
	private String htmlClass;

	/**
	 * Policy
	 */
	private String policy;

	/**
	 * Order ID
	 */
	private Integer partOrder;

	/**
	 * Map of names, separated by language code
	 */
	private Map<String, String> nameI18n;

	/**
	 * Empty constructor
	 */
	public Part() {}

	/**
	 * Part name
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @return 
	 */
	public Part setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Part keyword
	 * @return the keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * @param keyword the keyword to set
	 * @return 
	 */
	public Part setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * True if the part is hidden
	 * @return the hidden
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * @param hidden the hidden to set
	 * @return 
	 */
	public Part setHidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	/**
	 * True if the part is editable
	 * @return the editable
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * @param editable the editable to set
	 * @return 
	 */
	public Part setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}

	/**
	 * True if the part is live (inline) editable
	 * @return the liveEditable
	 */
	public boolean isLiveEditable() {
		return liveEditable;
	}

	/**
	 * @param liveEditable the liveEditable to set
	 * @return 
	 */
	public Part setLiveEditable(boolean liveEditable) {
		this.liveEditable = liveEditable;
		return this;
	}

	/**
	 * True if the part is mandatory
	 * @return mandatory flag
	 */
	public boolean isMandatory() {
		return mandatory;
	}

	/**
	 * Set the mandatory flag
	 * @param mandatory flag
	 * @return 
	 */
	public Part setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
		return this;
	}

	/**
	 * Part type
	 * @return type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return 
	 */
	public Part setType(Type type) {
		this.type = type;
		return this;
	}

	/**
	 * Part type ID
	 * @return type ID
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * Set the part type ID
	 * @param typeId type ID
	 * @return 
	 */
	public Part setTypeId(int typeId) {
		this.typeId = typeId;
		return this;
	}

	/**
	 * Local ID of the part
	 * @return local id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the local ID
	 * @param id local id
	 * @return 
	 */
	public Part setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Global ID of the part
	 * @return global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 * @return 
	 */
	public Part setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Default property
	 * @return default property
	 */
	public Property getDefaultProperty() {
		return defaultProperty;
	}

	/**
	 * Set the default property
	 * @param defaultProperty default property
	 * @return 
	 */
	public Part setDefaultProperty(Property defaultProperty) {
		this.defaultProperty = defaultProperty;
		return this;
	}

	/**
	 * Regular expression definition for validation of text parttypes
	 * @return regular expression
	 */
	public RegexModel getRegex() {
		return regex;
	}

	/**
	 * Set regular expression
	 * @param regex regular expression
	 * @return 
	 */
	public Part setRegex(RegexModel regex) {
		this.regex = regex;
		return this;
	}

	/**
	 * Flag for hiding the part in the Tag Editor
	 * @return flag
	 */
	public boolean isHideInEditor() {
		return hideInEditor;
	}

	/**
	 * Set hideInEditor flag
	 * @param hideInEditor flag
	 * @return 
	 */
	public Part setHideInEditor(boolean hideInEditor) {
		this.hideInEditor = hideInEditor;
		return this;
	}

	/**
	 * External editor URL
	 * @return URL
	 */
	public String getExternalEditorUrl() {
		return externalEditorUrl;
	}

	/**
	 * Set the URL to the external editor
	 * @param externalEditorUrl URL
	 * @return 
	 */
	public Part setExternalEditorUrl(String externalEditorUrl) {
		this.externalEditorUrl = externalEditorUrl;
		return this;
	}

	/**
	 * Possible options
	 * @return the options
	 */
	public List<SelectOption> getOptions() {
		return options;
	}

	/**
	 * @param options the options to set
	 * @return 
	 */
	public Part setOptions(List<SelectOption> options) {
		this.options = options;
		return this;
	}

	/**
	 * Overview settings (if type is OVERVIEW)
	 * @return overview settings
	 */
	public OverviewSetting getOverviewSettings() {
		return overviewSettings;
	}

	/**
	 * Set overview settings
	 * @param overviewSettings overview settings
	 * @return 
	 */
	public Part setOverviewSettings(OverviewSetting overviewSettings) {
		this.overviewSettings = overviewSettings;
		return this;
	}

	/**
	 * Selection settings (if type is SELECT or MULTISELECT)
	 * @return selection settings
	 */
	public SelectSetting getSelectSettings() {
		return selectSettings;
	}

	/**
	 * Set selection settings
	 * @param selectSettings selection settings
	 * @return 
	 */
	public Part setSelectSettings(SelectSetting selectSettings) {
		this.selectSettings = selectSettings;
		return this;
	}

	/**
	 * Optional markup language ID for restricting the part to a specific template markup
	 * @return markup language ID
	 */
	public Integer getMarkupLanguageId() {
		return markupLanguageId;
	}

	/**
	 * Set the markup language ID
	 * @param markupLanguageId ID
	 * @return fluent API
	 */
	public Part setMarkupLanguageId(Integer markupLanguageId) {
		this.markupLanguageId = markupLanguageId;
		return this;
	}

	/**
	 * HTML Class, if the part is of type {@link Type#LIST}, {@link Type#ORDEREDLIST} or {@link Type#UNORDEREDLIST}
	 * @return html class
	 */
	public String getHtmlClass() {
		return htmlClass;
	}

	/**
	 * Set the html class
	 * @param htmlClass html class
	 * @return fluent API
	 */
	public Part setHtmlClass(String htmlClass) {
		this.htmlClass = htmlClass;
		return this;
	}

	/**
	 * Part order
	 * @return order
	 */
	public Integer getPartOrder() {
		return partOrder;
	}

	/**
	 * Set the part order
	 * @param partOrder order
	 * @return fluent API
	 */
	public Part setPartOrder(Integer partOrder) {
		this.partOrder = partOrder;
		return this;
	}

	/**
	 * Validation policy
	 * @return validation policy
	 */
	public String getPolicy() {
		return policy;
	}

	/**
	 * Set the validation policy
	 * @param policy policy
	 * @return fluent API
	 */
	public Part setPolicy(String policy) {
		this.policy = policy;
		return this;
	}

	/**
	 * Map of translated names (keys are the language codes)
	 * @return translated names
	 */
	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	/**
	 * Set translated names
	 * @param nameI18n translation map
	 * @return fluent API
	 */
	public Part setNameI18n(Map<String, String> nameI18n) {
		this.nameI18n = nameI18n;
		return this;
	}

	/**
	 * Set the name in the given language
	 * @param name name
	 * @param language language
	 * @return fluent API
	 */
	public Part setName(String name, String language) {
		if (this.nameI18n == null) {
			this.nameI18n = new HashMap<>();
		}
		this.nameI18n.put(language, name);
		return this;
	}
}
