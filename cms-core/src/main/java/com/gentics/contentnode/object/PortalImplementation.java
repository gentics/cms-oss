package com.gentics.contentnode.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * Interface for Portal Implementation NodeObjects
 */
@TType(PortalImplementation.TYPE_PORTAL_IMPLEMENTATION)
public interface PortalImplementation extends NamedNodeObject, Resolvable {
	/**
	 * TType of the Portal Implementations admin (used for permissions). Value: {@value}
	 */
	public static final int TYPE_PORTAL_IMPLEMENTATIONS = 10500;

	/**
	 * TType of a single Portal Implementation. Value: {@value}
	 */
	public static final int TYPE_PORTAL_IMPLEMENTATION = 10501;

	/**
	 * Maximum length of keyword and name
	 */
	public static final int MAX_LENGTH = 255;

	/**
	 * Transform the node object into its rest model
	 */
	public final static Function<PortalImplementation, PortalImplementationModel> TRANSFORM2REST = impl -> {
		PortalImplementationModel model = new PortalImplementationModel()
				.setId(impl.getId())
				.setKeyword(impl.getKeyword())
				.setName(impl.getName())
				.setDescription(impl.getDescription())
				.setType(impl.getType())
				.setCreator(ModelBuilder.getUser(impl.getCreator()))
				.setCdate(impl.getCDate().getIntTimestamp())
				.setEditor(ModelBuilder.getUser(impl.getEditor()))
				.setEdate(impl.getEDate().getIntTimestamp());
		if (impl.getGlobalId() != null) {
			model.setGlobalId(impl.getGlobalId().toString());
		}
		return model;
	};

	/**
	 * Transfer the attributes set in the rest model into the given node object. Attributes, which are not set (null) in the rest model are not changed.
	 */
	public final static BiFunction<PortalImplementationModel, PortalImplementation, PortalImplementation> REST2NODE = (restModel, impl) -> {
		if (restModel.getKeyword() != null) {
			impl.setKeyword(restModel.getKeyword());
		}
		if (restModel.getName() != null) {
			impl.setName(restModel.getName());
		}
		if (restModel.getDescription() != null) {
			impl.setDescription(restModel.getDescription());
		}
		if (restModel.getType() != null) {
			impl.setType(restModel.getType());
		}
		return impl;
	};

	@Override
	default Integer getTType() {
		return TYPE_PORTAL_IMPLEMENTATION;
	}

	/**
	 * Get the keyword
	 * @return keyword
	 */
	@FieldGetter("keyword")
	String getKeyword();

	/**
	 * Set the keyword
	 * @param keyword keyword
	 * @throws ReadOnlyException
	 */
	@FieldSetter("keyword")
	default void setKeyword(String keyword) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the name
	 * @return name
	 */
	@Override
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

	/**
	 * Get the type
	 * @return type (may be null for new objects)
	 */
	PortalImplementationModel.Type getType();

	/**
	 * Set the type
	 * @param type type
	 * @throws ReadOnlyException
	 */
	default void setType(PortalImplementationModel.Type type) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the creation date
	 * @return creation date
	 */
	ContentNodeDate getCDate();

	/**
	 * Get the creator
	 * @return creator, null if the user does not exist (anymore)
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;

	/**
	 * Get the date of last editing
	 * @return edit date
	 */
	ContentNodeDate getEDate();

	/**
	 * Get the last editor
	 * @return editor, null if the user does not exist (anymore)
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;
	/**
	 * Get the files contained in the given folder (including all subfolders)
	 * @param folder path of the folder, empty for the root folder (i.e. all files)
	 * @return list of files
	 * @throws NodeException
	 */
	List<PortalImplementationFile> getFiles(String folder) throws NodeException;

	/**
	 * Get the file with the given path
	 * @param path path of the file
	 * @return file or null, if no such file exists
	 * @throws NodeException
	 */
	PortalImplementationFile getFile(String path) throws NodeException;
}
