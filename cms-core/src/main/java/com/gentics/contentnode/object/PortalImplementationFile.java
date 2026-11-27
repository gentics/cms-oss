package com.gentics.contentnode.object;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.ObjectReadOnlyException;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.rest.model.PortalImplementationFileModel;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * Interface for files contained in Portal Implementations.
 * Files are identified by their path (including the filename) relative to the root folder of the Portal Implementation.
 * The meta information is stored in the database, the contents are stored in the dbfiles folder.
 */
@TType(PortalImplementationFile.TYPE_PORTAL_IMPLEMENTATION_FILE)
public interface PortalImplementationFile extends NodeObject, Resolvable {
	/**
	 * TType of a Portal Implementation file. Value: {@value}
	 */
	public static final int TYPE_PORTAL_IMPLEMENTATION_FILE = 10502;

	/**
	 * Maximum length of the path
	 */
	public static final int MAX_PATH_LENGTH = 512;

	/**
	 * Path separator
	 */
	public static final String SEPARATOR = "/";

	/**
	 * Transform the node object into its rest model
	 */
	public final static Function<PortalImplementationFile, PortalImplementationFileModel> TRANSFORM2REST = file -> {
		PortalImplementationFileModel model = new PortalImplementationFileModel()
				.setId(file.getId())
				.setPath(file.getPath())
				.setName(file.getName())
				.setFolder(file.getFolder())
				.setMimetype(file.getMimetype())
				.setFilesize(file.getFilesize())
				.setCreator(ModelBuilder.getUser(file.getCreator()))
				.setCdate(file.getCDate().getIntTimestamp())
				.setEditor(ModelBuilder.getUser(file.getEditor()))
				.setEdate(file.getEDate().getIntTimestamp());
		if (file.getGlobalId() != null) {
			model.setGlobalId(file.getGlobalId().toString());
		}
		return model;
	};

	@Override
	default Integer getTType() {
		return TYPE_PORTAL_IMPLEMENTATION_FILE;
	}

	/**
	 * Get the ID of the Portal Implementation
	 * @return Portal Implementation ID
	 */
	int getPortalImplementationId();

	/**
	 * Set the ID of the Portal Implementation (only for new files)
	 * @param portalImplementationId Portal Implementation ID
	 * @throws ReadOnlyException
	 */
	default void setPortalImplementationId(int portalImplementationId) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the Portal Implementation
	 * @return Portal Implementation
	 * @throws NodeException
	 */
	PortalImplementation getPortalImplementation() throws NodeException;

	/**
	 * Get the path (including the filename)
	 * @return path
	 */
	@FieldGetter("path")
	String getPath();

	/**
	 * Set the path (including the filename)
	 * @param path path
	 * @throws ReadOnlyException
	 */
	@FieldSetter("path")
	default void setPath(String path) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the filename (last segment of the path)
	 * @return filename
	 */
	default String getName() {
		return StringUtils.substringAfterLast(SEPARATOR + StringUtils.defaultString(getPath()), SEPARATOR);
	}

	/**
	 * Get the path of the folder (empty for files in the root folder)
	 * @return folder path
	 */
	default String getFolder() {
		String path = StringUtils.defaultString(getPath());
		return path.contains(SEPARATOR) ? StringUtils.substringBeforeLast(path, SEPARATOR) : "";
	}

	/**
	 * Get the mimetype
	 * @return mimetype
	 */
	@FieldGetter("mimetype")
	String getMimetype();

	/**
	 * Set the mimetype
	 * @param mimetype mimetype
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mimetype")
	default void setMimetype(String mimetype) throws ReadOnlyException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the filesize in bytes
	 * @return filesize
	 */
	long getFilesize();

	/**
	 * Set the contents of the file. The contents will be stored in the dbfiles folder when the file is saved.
	 * @param contents stream of the contents
	 * @throws NodeException
	 */
	default void setContents(InputStream contents) throws NodeException {
		throw new ObjectReadOnlyException(this);
	}

	/**
	 * Get the file in the dbfiles folder, which holds the contents
	 * @return binary file
	 */
	java.io.File getBinFile();

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
}
