package com.gentics.contentnode.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Construct
 */
@XmlRootElement
public class Construct implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1008382452392643391L;

	/**
	 * Id of the construct
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Name of the construct
	 */
	private String name;

	/**
	 * Icon of the construct
	 */
	private String icon;

	/**
	 * This construct may contain other tags
	 */
	private Boolean mayContainSubtags;

	/**
	 * This construct may be inserted into other tags
	 */
	private Boolean mayBeSubtag;

	/**
	 * Flag to mark, which construct shall be visible to the editor in the menu.
	 */
	private Boolean visibleInMenu;

	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Description
	 */
	private String description;

	/**
	 * Category ID
	 */
	private Integer categoryId;

	/**
	 * Category
	 */
	private ConstructCategory category;

	/**
	 * Edit do (this is the do, that has to be used for filling tags based on this construct the old PHP way). Either 10008 (tag_fill.php)
	 * for "normal" constructs or 17001 (ds_sel.php) if the construct contains an overview part
	 */
	Integer editdo;

	/**
	 * Creator of the folder
	 */
	private User creator;

	/**
	 * Date when the folder was created
	 */
	private int cdate;

	/**
	 * Contributor to the folder
	 */
	private User editor;

	/**
	 * Date when the folder was modified the last time
	 */
	private int edate;

	/**
	 * Flag for using new tag editor
	 */
	private Boolean newEditor;

	/**
	 * URL to the external editor
	 */
	private String externalEditorUrl;

	/**
	 * Parts of the editable
	 */
	private List<Part> parts;

	/**
	 * Map of names, separated by language code
	 */
	private Map<String, String> nameI18n;

	/**
	 * Map of descriptions, separated by language code
	 */
	private Map<String, String> descriptionI18n;

	/**
	 * Auto-enable the construct after creation
	 */
	private Boolean autoEnable;

	/**
	 * HTML tag, assigned to the editor wrapper of the instances of this construct, in edit mode
	 */
	private String liveEditorTagName;

	/**
	 * The javascript "hopedit" hook which, if available, should replace the "hopedit" call.
	 */
	private String hopeditHook;

	/**
	 * Keyword for this construct
	 * 
	 * @return keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Sets the keyword for this construct
	 * @return fluent API
	 */
	public Construct setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * Sets the icon for this construct
	 * 
	 * @param icon
	 * @return fluent API
	 */
	public Construct setIcon(String icon) {
		this.icon = icon;
		return this;
	}

	/**
	 * Sets the name for this construct
	 * 
	 * @param name
	 * @return fluent API
	 */
	public Construct setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Whether a tag of this construct may be inserted/nested in other tags
	 * 
	 * @return True or false
	 */
	public Boolean getMayBeSubtag() {
		return this.mayBeSubtag;
	}

	/**
	 * Whether a tag of this construct may be inserted/nested in other tags
	 *
	 * @return Optional of flag
	 */
	@JsonIgnore
	public Optional<Boolean> getMayBeSubtagOptional() {
		return Optional.ofNullable(this.mayBeSubtag);
	}

	/**
	 * Sets whether a tag of this construct may be a nested in another tag.
	 * 
	 * @param value
	 * @return fluent API
	 */
	public Construct setMayBeSubtag(Boolean value) {
		this.mayBeSubtag = value;
		return this;
	}

	/**
	 * Whether this construct may contain other tags.
	 * 
	 * @return
	 */
	public Boolean getMayContainSubtags() {
		return this.mayContainSubtags;
	}

	/**
	 * Whether this construct may contain other tags.
	 *
	 * @return Optional of mayContainSubtags flag
	 */
	@JsonIgnore
	public Optional<Boolean> getMayContainSubtagsOptional() {
		return Optional.ofNullable(this.mayContainSubtags);
	}

	/**
	 * Sets whether this construct may contain other tags. This means tags can be inserted into a tag from this construct
	 * 
	 * @param value
	 * @return fluent API
	 */
	public Construct setMayContainSubtags(Boolean value) {
		this.mayContainSubtags = value;
		return this;
	}

	/**
	 * Set the description
	 * 
	 * @param description
	 * @return fluent API
	 */
	public Construct setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the constructId for this construct
	 * 
	 * @param constructId
	 * @deprecated use setId() instead
	 */
	public void setConstructId(Integer constructId) {
		this.id = constructId;
	}

	/**
	 * Construct id of this construct
	 * 
	 * @return
	 * @deprecated use getId() instead
	 */
	public Integer getConstructId() {
		return this.id;
	}

	/**
	 * ID of this construct
	 * 
	 * @return id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the id of this construct
	 * 
	 * @param id
	 *            id
	 * @return fluent API
	 */
	public Construct setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Global ID of the construct
	 * @return global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 * @return fluent API
	 */
	public Construct setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Name of this construct
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Description of this construct
	 * 
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Icon of this construct
	 * 
	 * @return
	 */
	public String getIcon() {
		return this.icon;
	}

	/**
	 * Creator of the construct
	 * 
	 * @return the creator
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * Creation Date of the construct
	 * @return the cdate
	 */
	public int getCdate() {
		return cdate;
	}

	/**
	 * Last Editor of the construct
	 * 
	 * @return the editor
	 */
	public User getEditor() {
		return editor;
	}

	/**
	 * Last Edit Date of the construct
	 * @return the edate
	 */
	public int getEdate() {
		return edate;
	}

	/**
	 * @param creator
	 *            the creator to set
	 * @return fluent API
	 */
	public Construct setCreator(User creator) {
		this.creator = creator;
		return this;
	}

	/**
	 * @param cdate
	 *            the cdate to set
	 * @return fluent API
	 */
	public Construct setCdate(int cdate) {
		this.cdate = cdate;
		return this;
	}

	/**
	 * @param editor
	 *            the editor to set
	 * @return fluent API
	 */
	public Construct setEditor(User editor) {
		this.editor = editor;
		return this;
	}

	/**
	 * @param edate
	 *            the edate to set
	 * @return fluent API
	 */
	public Construct setEdate(int edate) {
		this.edate = edate;
		return this;
	}

	/**
	 * Edit do
	 * 
	 * @return edit do
	 */
	public Integer getEditdo() {
		return editdo;
	}

	/**
	 * Set the edit do
	 * 
	 * @param editdo
	 *            edit do
	 * @return fluent API
	 */
	public Construct setEditdo(Integer editdo) {
		this.editdo = editdo;
		return this;
	}

	/**
	 * Category of the construct
	 * 
	 * @return category
	 */
	public ConstructCategory getCategory() {
		return category;
	}

	/**
	 * Set the category
	 * 
	 * @param category
	 *            category
	 * @return fluent API
	 */
	public Construct setCategory(ConstructCategory category) {
		this.category = category;
		return this;
	}

	/**
	 * Flag for using the new Tag Editor
	 * @return newEditor flag
	 */
	public Boolean getNewEditor() {
		return newEditor;
	}

	/**
	 * Flag for using the new Tag Editor
	 * @return Optional of newEditor flag
	 */
	@JsonIgnore
	public Optional<Boolean> getNewEditorOptional() {
		return Optional.ofNullable(this.newEditor);
	}

	/**
	 * Set the flag for new Tag Editor
	 * @param newEditor flag
	 * @return fluent API
	 */
	public Construct setNewEditor(Boolean newEditor) {
		this.newEditor = newEditor;
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
	 * @return fluent API
	 */
	public Construct setExternalEditorUrl(String externalEditorUrl) {
		this.externalEditorUrl = externalEditorUrl;
		return this;
	}

	/**
	 * Parts of the construct
	 * @return parts
	 */
	public List<Part> getParts() {
		return parts;
	}

	/**
	 * Set the parts
	 * @param parts parts
	 * @return fluent API
	 */
	public Construct setParts(List<Part> parts) {
		this.parts = parts;
		return this;
	}

	/**
	 * True if the construct shall be visible in the menu, false if not
	 * @return true for visible constructs, false for hidden
	 */
	public Boolean getVisibleInMenu() {
		return visibleInMenu;
	}

	/**
	 * Set whether the construct shall be visible in the menu
	 * @param visibleInMenu true for visible, false for hidden
	 * @return fluent API
	 */
	public Construct setVisibleInMenu(Boolean visibleInMenu) {
		this.visibleInMenu = visibleInMenu;
		return this;
	}

	/**
	 * Map of translated names (keys are the language codes)
	 * @return name map
	 */
	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	/**
	 * Set translated names
	 * @param i18nName map of translations
	 * @return fluent API
	 */
	public Construct setNameI18n(Map<String, String> i18nName) {
		this.nameI18n = i18nName;
		return this;
	}

	/**
	 * Set the name in the given language
	 * @param name name
	 * @param language language
	 * @return fluent API
	 */
	public Construct setName(String name, String language) {
		if (this.nameI18n == null) {
			this.nameI18n = new HashMap<>();
		}
		this.nameI18n.put(language, name);
		return this;
	}

	/**
	 * Map of translated descriptions (keys are the language codes)
	 * @return description map
	 */
	public Map<String, String> getDescriptionI18n() {
		return descriptionI18n;
	}

	/**
	 * Set translated descriptions
	 * @param i18nDescription map of translations
	 * @return fluent API
	 */
	public Construct setDescriptionI18n(Map<String, String> i18nDescription) {
		this.descriptionI18n = i18nDescription;
		return this;
	}

	/**
	 * Set the description in the given language
	 * @param description description
	 * @param language language
	 * @return fluent API
	 */
	public Construct setDescription(String description, String language) {
		if (this.descriptionI18n == null) {
			this.descriptionI18n = new HashMap<>();
		}
		this.descriptionI18n.put(language, description);
		return this;
	}

	/**
	 * Flag for automatically enabling new tags, which are created based on this construct
	 * @return flag
	 */
	public Boolean getAutoEnable() {
		return autoEnable;
	}

	/**
	 * Flag for automatically enabling new tags, which are created based on this construct
	 * @return Optional of flag
	 */
	@JsonIgnore
	public Optional<Boolean> getAutoEnableOptional() {
		return Optional.ofNullable(this.autoEnable);
	}

	/**
	 * Set autoEnable flag
	 * @param autoEnable flag
	 * @return fluent API
	 */
	public Construct setAutoEnable(Boolean autoEnable) {
		this.autoEnable = autoEnable;
		return this;
	}

	/**
	 * HTML tag name, which is used when tags based on this construct are edited with the live editor
	 * @return tag name
	 */
	public String getLiveEditorTagName() {
		return liveEditorTagName;
	}

	/**
	 * Set the live editor tagname
	 * @param liveEditorTagName tag name
	 * @return fluent API
	 */
	public Construct setLiveEditorTagName(String liveEditorTagName) {
		this.liveEditorTagName = liveEditorTagName;
		return this;
	}

	/**
	 * Javascript "hopedit" hook which, if available
	 * should replace the "hopedit" call.
	 * @return hopedit hook
	 */
	public String getHopeditHook() {
		return hopeditHook;
	}

	/**
	 * Set the hopedit hook
	 * @param hopeditHook hook
	 * @return fluent API
	 */
	public Construct setHopeditHook(String hopeditHook) {
		this.hopeditHook = hopeditHook;
		return this;
	}

	@Override
	public String toString() {
		return String.format("Construct keyword: %s", keyword);
	}

	/**
	 * ID of the category of this construct
	 * @return category ID
	 */
	public Integer getCategoryId() {
		return categoryId;
	}

	/**
	 * Set the category ID
	 * @param categoryId ID
	 * @return fluent API
	 */
	public Construct setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
		return this;
	}

}
