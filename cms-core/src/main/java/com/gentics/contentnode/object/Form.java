package com.gentics.contentnode.object;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.perm.CreatePermType;
import com.gentics.contentnode.factory.perm.DeletePermType;
import com.gentics.contentnode.factory.perm.EditPermType;
import com.gentics.contentnode.factory.perm.PublishPermType;
import com.gentics.contentnode.factory.perm.ViewPermType;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * Interface for forms
 */
@TType(Form.TYPE_FORM)
@ViewPermType(PermType.viewform)
@CreatePermType(PermType.createform)
@EditPermType(PermType.updateform)
@DeletePermType(PermType.deleteform)
@PublishPermType(PermType.publishform)
public interface Form extends StageableVersionedNodeObject, PublishableNodeObjectInFolder, Resolvable, NamedNodeObject, StackResolvableNodeObject, MetaDateNodeObject {
	/**
	 * The ttype of the form object. Value: {@value}
	 */
	public static final int TYPE_FORM = 10050;

	/**
	 * Maximum length for names
	 */
	public final static int MAX_NAME_LENGTH = 255;

	@Override
	default Integer getTType() {
		return TYPE_FORM;
	}

	@Override
	default boolean isRecyclable() {
		return true;
	}

	/**
	 * Get the name
	 * @return name
	 */
	@FieldGetter("name")
	String getName();

	/**
	 * Set the name
	 * @param name name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	default void setName(String name) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the description
	 * @return description
	 */
	@FieldGetter("description")
	String getDescription();

	/**
	 * Set the description
	 * @param description description
	 * @throws ReadOnlyException
	 */
	@FieldSetter("description")
	default void setDescription(String description) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	@Override
	default void setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Gets the success page ID of a object.
	 * @return The success page ID of the object.
	 * @throws NodeException
	 */
	int getSuccessPageId() throws NodeException;

	/**
	 * Set the success page id of the object
	 * @param successPageId success page id
	 * @throws NodeException
	 */
	default void setSuccessPageId(int successPageId) throws NodeException, ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Gets the success page node ID of a object.
	 * @return The success page node ID of the object.
	 * @throws NodeException
	 */
	int getSuccessNodeId() throws NodeException;

	/**
	 * Set the success page node id of the object
	 * @param successNodeId success page node id
	 * @throws NodeException
	 */
	default void setSuccessNodeId(int successNodeId) throws NodeException, ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	@FieldGetter("languages")
	List<String> getLanguages();

	@FieldSetter("languages")
	default void setLanguages(List<String> languages) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	@FieldGetter("data")
	JsonNode getData();

	@FieldSetter("data")
	default void setData(JsonNode data) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get language specific form data
	 * @param language language
	 * @return language specific form data
	 * @throws NodeException 
	 */
	JsonNode getData(String language) throws NodeException;

	/**
	 * Get the indexable content
	 * @param language language
	 * @return list of strings to index
	 * @throws NodeException
	 */
	List<String> getIndexableContent(String language) throws NodeException;

	/**
	 * Form creator
	 * @return creator of the form
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * Form editor
	 * @return last editor of the form
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;

	/**
	 * Check, if the form is currently locked by a user.
	 * @return true, if the form is currently locked.
	 */
	boolean isLocked() throws NodeException;

	/**
	 * Get the date, since when the form is locked, or null if it is not locked
	 * @return date of form locking or null
	 * @throws NodeException
	 */
	ContentNodeDate getLockedSince() throws NodeException;

	/**
	 * Get the user, by which the form is currently locked (if
	 * {@link #isLocked()} returns true) or null if the form is not currently
	 * locked.
	 * @return a user or null
	 * @throws NodeException
	 */
	SystemUser getLockedBy() throws NodeException;

	@Override
	default String getSuffix() {
		return ".form";
	}

	@Override
	default Optional<StageableVersionedNodeObject> maybeVersioned() {
		return Optional.of(this);
	}

	@Override
	default void setFolder(Node node, Folder parent) throws NodeException {
		setFolderId(parent.getId());
	}

	@Override
	default int getRoleBit(PermType permType) {
		return -1;
	}

	@Override
	default int getRoleCheckId() {
		return -1;
	}
}
