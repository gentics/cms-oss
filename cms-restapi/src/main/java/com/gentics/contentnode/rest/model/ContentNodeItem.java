package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.webcohesion.enunciate.metadata.ReadOnly;

/**
 * ContentNodeItem which can be a Page, File, Image or Folder
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class ContentNodeItem implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -9009068552989541126L;

	/**
	 * Types of items to be fetched with method
	 * @author norbert
	 *
	 */
	public static enum ItemType {
		page, file, image, folder, channel, node, form
	}

	/**
	 * Id of the item
	 */
	private Integer id;

	/**
	 * global ID of the item
	 */
	private String globalId;

	/**
	 * Name of the item
	 */
	private String name;

	/**
	 * Creator of the item
	 */
	private User creator;

	/**
	 * Date when the item was created
	 */
	private int cdate;

	/**
	 * Contributor to the item
	 */
	private User editor;

	/**
	 * Date when the item was modified the last time
	 */
	private int edate;

	/**
	 * Type of the item
	 */
	private ItemType type;

	/**
	 * Deletion info (if object was deleted)
	 */
	private DeleteInfo deleted;

	/**
	 * Deletion info about the objects master (if applicable).
	 */
	private DeleteInfo masterDeleted;

	/**
	 * Deletion info about the objects folder (if applicable).
	 */
	private DeleteInfo folderDeleted;

	/**
	 * Default constructor needed by JAXB
	 */
	public ContentNodeItem() {}

	/**
	 * Create an empty instance of an item
	 */
	public ContentNodeItem(ItemType type) {
		setType(type);
	}

	/**
	 * ID of the item
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Global ID of the item
	 * @return the global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Name of the item
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creator of the item
	 * @return the creator
	 */
	@ReadOnly
	public User getCreator() {
		return creator;
	}

	/**
	 * Creation date of the item as unix timestamp
	 * @return the cdate
	 */
	@ReadOnly
	public int getCdate() {
		return cdate;
	}

	/**
	 * Last editor of the item
	 * @return the editor
	 */
	@ReadOnly
	public User getEditor() {
		return editor;
	}

	/**
	 * Last Edit Date of the item as unix timestamp
	 * @return the edate
	 */
	@ReadOnly
	public int getEdate() {
		return edate;
	}

	/**
	 * Item type
	 * @return item type
	 */
	@ReadOnly
	public ItemType getType() {
		return type;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 */
	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(User creator) {
		this.creator = creator;
	}

	/**
	 * @param cdate the cdate to set
	 */
	public void setCdate(int cdate) {
		this.cdate = cdate;
	}

	/**
	 * @param editor the editor to set
	 */
	public void setEditor(User editor) {
		this.editor = editor;
	}

	/**
	 * @param edate the edate to set
	 */
	public void setEdate(int edate) {
		this.edate = edate;
	}

	/**
	 * Set the item type
	 * @param type item type
	 */
	public void setType(ItemType type) {
		this.type = type;
	}

	/**
	 * Deletion information, if object was deleted
	 * @return deletion info
	 */
	@ReadOnly
	public DeleteInfo getDeleted() {
		return deleted;
	}

	/**
	 * Set deletion info
	 * @param deleted deletion info
	 */
	public void setDeleted(DeleteInfo deleted) {
		this.deleted = deleted;
	}

	/**
	 * Deletion information about the master (if the object is not a master itself).
	 *
	 * @return deletion info.
	 */
	@ReadOnly
	public DeleteInfo getMasterDeleted() {
		return masterDeleted;
	}

	/**
	 * Set master deletion info.
	 * @param masterDeleted deletion info
	 */
	public void setMasterDeleted(DeleteInfo masterDeleted) {
		this.masterDeleted = masterDeleted;
	}

	/**
	 * Deletion information about the containing folder.
	 *
	 * @return deletion info.
	 */
	@ReadOnly
	public DeleteInfo getFolderDeleted() {
		return folderDeleted;
	}

	/**
	 * Set containing folder deletion info.
	 * @param masterDeleted deletion info
	 */
	public void setFolderDeleted(DeleteInfo folderDeleted) {
		this.folderDeleted = folderDeleted;
	}
}
