package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * Parameter bean for filtering editable objects (by creator or editor)
 */
public class EditableParameterBean {
	/**
	 * (optional) true when only the objects created by the user shall be returned
	 */
	@QueryParam("iscreator")
	@DefaultValue("false")
	public boolean isCreator = false;

	/**
	 * Pattern for restricting objects by creator
	 */
	@QueryParam("creator")
	public String creator;

	/**
	 * IDs for restricting objects by creator.
	 */
	@QueryParam("creatorId")
	public List<Integer> creatorIds;

	/**
	 * Timestamp to search objects, which were created before a given time (0 for all objects)
	 */
	@QueryParam("createdbefore")
	@DefaultValue("0")
	public int createdBefore = 0;

	/**
	 * Timestamp to search objects, which were created since a given time (0 for all objects)
	 */
	@QueryParam("createdsince")
	@DefaultValue("0")
	public int createdSince = 0;

	/**
	 * (optional) true when only the objects last edited by the user shall be returned
	 */
	@QueryParam("iseditor")
	@DefaultValue("false")
	public boolean isEditor = false;

	/**
	 * Pattern for restricting objects by editor
	 */
	@QueryParam("editor")
	public String editor;

	/**
	 * IDs for restricting objects by editor.
	 */
	@QueryParam("editorId")
	public List<Integer> editorIds;

	/**
	 * Timestamp to search objects, which were edited before a given time (0 for all objects)
	 */
	@QueryParam("editedbefore")
	@DefaultValue("0")
	public int editedBefore = 0;

	/**
	 * Timestamp to search objects, which were edited since a given time (0 for all objects)
	 */
	@QueryParam("editedsince")
	@DefaultValue("0")
	public int editedSince = 0;

	public EditableParameterBean setCreator(boolean creator) {
		isCreator = creator;
		return this;
	}

	public EditableParameterBean setCreator(String creator) {
		this.creator = creator;
		return this;
	}

	public EditableParameterBean setCreatorIds(List<Integer> creatorIds) {
		this.creatorIds = creatorIds;
		return this;
	}

	public EditableParameterBean setCreatedBefore(int createdBefore) {
		this.createdBefore = createdBefore;
		return this;
	}

	public EditableParameterBean setCreatedSince(int createdSince) {
		this.createdSince = createdSince;
		return this;
	}

	public EditableParameterBean setEditor(boolean editor) {
		isEditor = editor;
		return this;
	}

	public EditableParameterBean setEditor(String editor) {
		this.editor = editor;
		return this;
	}

	public EditableParameterBean setEditorIds(List<Integer> editorIds) {
		this.editorIds = editorIds;
		return this;
	}

	public EditableParameterBean setEditedBefore(int editedBefore) {
		this.editedBefore = editedBefore;
		return this;
	}

	public EditableParameterBean setEditedSince(int editedSince) {
		this.editedSince = editedSince;
		return this;
	}
}
