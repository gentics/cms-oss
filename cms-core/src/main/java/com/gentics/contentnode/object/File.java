/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: File.java,v 1.12.4.3 2011-02-08 14:14:40 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.upload.FileInformation;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.rest.model.PageLanguageCode;

/**
 * This is a content.node file object, which contains binary content.
 * For memory usage reasons, the binary content is not stored in this class,
 * but on the hard drive.
 */
@TType(File.TYPE_FILE)
public interface File extends Resolvable, StageableChanneledNodeObject, Disinheritable<ContentFile>, ObjectTagContainer, NodeObjectWithAlternateUrls, NamedNodeObject, StackResolvableNodeObject, CustomMetaDateNodeObject {

	/**
	 * The ttype of the file.
	 */
	public static final int TYPE_FILE = 10008;
    
	public static final Integer TYPE_FILE_INTEGER = new Integer(TYPE_FILE);

	/**
	 * Maximum filename length
	 */
	public static final int MAX_NAME_LENGTH = 64;

	/**
	 * Maximum description length
	 */
	public static final int MAX_DESCRIPTION_LENGTH = 255;

	/**
	 * Get the filename of the file.
	 * @return the filename.
	 */
	@FieldGetter("name")
	String getName();
    
	/**
	 * Set the name of the file
	 * @param name new name
	 * @return previous name
	 * @throws NodeException when the file was not fetched for updating
	 */
	@FieldSetter("name")
	String setName(String name) throws NodeException;

	/**
	 * get the filetype as mime-type of this file.
	 * @return the mime-type of this file.
	 */
	@FieldGetter("filetype")
	String getFiletype();

	/**
	 * Set the filetype of the file
	 * @param filetype new filetype
	 * @return old filetype
	 */
	@FieldSetter("filetype")
	String setFiletype(String filetype) throws ReadOnlyException;
    
	/**
	 * get the folder of this file.
	 * @return the folder of this file.
	 * @throws NodeException 
	 */
	Folder getFolder() throws NodeException;
  
	/**
	 * Set the folder id of the file
	 * @param folderId new folder id
	 * @return old folder id
	 * @throws ReadOnlyException when the object was not fetched for update
	 */
	Integer setFolderId(Integer folderId) throws NodeException, ReadOnlyException;

	/**
	 * get the filesize of this file in bytes.
	 * @return the size of the file in bytes.
	 */
	@FieldGetter("filesize")
	int getFilesize();

	/**
	 * Set the filesize of the file
	 * @param filesize
	 * @return old filesize of the file
	 */
	@FieldSetter("filesize")
	int setFilesize(int filesize) throws ReadOnlyException;

	/**
	 * get the description of this file.
	 * @return the description of this file.
	 */
	@FieldGetter("description")
	String getDescription();

	/**
	 * Set the description of the file
	 * @param description new description
	 * @return old description
	 * @throws ReadOnlyException when the file was not fetched for updating
	 */
	@FieldSetter("description")
	String setDescription(String description) throws ReadOnlyException;

	/**
	 * Check whether the file shall be published even if nothing depends on it (and {@link Feature#CONTENTFILE_AUTO_OFFLINE} is active for the Node)
	 * @return true if publishing the file shall be forced, false if not
	 */
	@FieldGetter("force_online")
	boolean isForceOnline();

	/**
	 * Set whether the file shall be published even if nothing depends on it (and {@link Feature#CONTENTFILE_AUTO_OFFLINE} is active for the Node)
	 * @param forceOnline true to force publishing, false otherwise
	 * @throws ReadOnlyException
	 */
	@FieldSetter("force_online")
	void setForceOnline(boolean forceOnline) throws ReadOnlyException;

	/**
	 * Retrieve file creator.
	 * @return creator of the file
	 * @throws NodeException
	 */
	SystemUser getCreator() throws NodeException;
    
	/**
	 * Retrieve file editor.
	 * @return last editor of the file
	 * @throws NodeException
	 */
	SystemUser getEditor() throws NodeException;
    
	/**
	 * Check if this file is an image.
	 * @return true, if the file is an image.
	 */
	boolean isImage();
    
	/**
	 * Get the extension of the filename.
	 * @return the extension of the filename.
	 */
	String getExtension();
    
	/**
	 * Get an md5 sum of the file content.
	 * @return the md5 sum of the binary content of the file.
	 */
	@FieldGetter("md5")
	String getMd5();
    
	/**
	 * Set the md5 checksum of the file.
	 * @param hash new hash.
	 * @return old hash
	 * @throws ReadOnlyException when the file was not fetched for updating
	 */
	@FieldSetter("md5")
	String setMd5(String hash) throws ReadOnlyException;
    
	/**
	 * get an unbuffered input stream where the binary content of the file can be read.
	 * @return the binary content of the file as stream.
	 */
	InputStream getFileStream() throws NodeException;
    
	/**
	 * Set a new file input stream. This stream will be used once the node
	 * object will be saved to retrieve the binary data that should be stored.
	 * @param stream Stream from which the data will be read 
	 */
	void setFileStream(InputStream stream) throws NodeException, ReadOnlyException;
    
	/**
	 * 
	 * @param name
	 * @param fallback
	 * @return
	 * @throws NodeException
	 */
	ObjectTag getObjectTag(String name, boolean fallback) throws NodeException;

	/**
	 * 
	 * @param name
	 * @return
	 * @throws NodeException
	 */
	ObjectTag getObjectTag(String name) throws NodeException;
    
	/**
	 * Get the object tags of the file
	 * @return
	 * @throws NodeException
	 */
	Map<String, ObjectTag> getObjectTags() throws NodeException; 

	/**
	 * Get the channelset of this file. The keys will be the channel ids
	 * (node_ids) and the values will be the file ids. When there is no multichannelling, the map will be empty.
	 * @return channelset of this file.
	 * @throws NodeException
	 */
	Map<Integer, Integer> getChannelSet() throws NodeException;

	/**
	 * Get the channelset id of this file or 0 if the file is not part of a channelset (or multichannelling is not supported)
	 * @return channelset id (may be null or 0)
	 */
	Integer getChannelSetId() throws NodeException;

	/**
	 * Get the channel of this file, if one set and multichannelling is supported.
	 * @return current channel of this file
	 * @throws NodeException
	 */
	Node getChannel() throws NodeException;

	/**
	 * Set the channel information for the file. The channel information consist
	 * of the channelId and the channelSetId. The channelId identifies the
	 * channel, for which the object is valid. If set to 0 (which is the
	 * default), the object is not a localized copy and no local object in a
	 * channel, but is a normal object in a node. The channelSetId groups the
	 * master object and all its localized copies in channel together. This
	 * method may only be called for new files.
	 * 
	 * @param channel
	 *            id new channel id. If set to 0, this object will be a master
	 *            object in a node and the channelSetId must be given as null
	 *            (which will create a new channelSetId)
	 * @param channelSetId
	 *            id of the channelset. If set to null, a new channelSetId will
	 *            be generated and the object will be a master (in a node or in
	 *            a channel)
	 * @throws ReadOnlyException when the object is not editable
	 * @throws NodeException in case of other errors
	 */
	public void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException;

	/**
	 * Check whether the file is inherited from a master (in multichannel) into
	 * the current channel
	 * @return true when the file is inherited, false if not or
	 *         multichannelling is disabled
	 * @throws NodeException
	 */
	boolean isInherited() throws NodeException;

	/**
	 * Check whether the file is a master or a localized copy
	 * @return true for a master, false for a localized copy
	 * @throws NodeException
	 */
	boolean isMaster() throws NodeException;

	/**
	 * Get the master file, if this file is a localized copy. If this
	 * file is not a localized copy or multichannelling is not activated,
	 * returns this file
	 * 
	 * @return master file for localized copies or this file
	 * @throws NodeException
	 */
	public File getMaster() throws NodeException;
	
	/**
	 * Gets master node folder name.
	 * @return
	 * @throws NodeException 
	 */
	public String getMasterNodeFolderName() throws NodeException;

	/**
	 * If this object is a localized copy, get the next higher object (the object, which would be inherited into the object's channel, if this localized copy did not
	 * exist). If the object is a master, return null.
	 * @return next higher master or null
	 * @throws NodeException
	 */
	public File getNextHigherObject() throws NodeException;

	/**
	 * Push this file into the given master
	 * @param master master node to push this file to
	 * @return target file
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public File pushToMaster(Node master) throws ReadOnlyException, NodeException;

	/**
	 * Get the channel variant of this file in the given channel
	 * @param channel channel
	 * @return channel variant
	 * @throws NodeException
	 */
	public File getChannelVariant(Node channel) throws NodeException;

	/**
	 * Check whether the file is broken (binary data does not exist)
	 * @return true if the file is broken, false if not
	 * @throws NodeException
	 */
	public boolean isBroken() throws NodeException;

	/**
	 * Check whether the file is in use
	 * @param usageMap The file usage map as gotten from com.gentics.contentnode.events.DependencyManager.getFileUsageMap()
	 * @return true if the file is used, false otherwise
	 * @throws NodeException
	 */
	default boolean isUsed(Map<Integer, Set<Integer>> usageMap) throws NodeException {
		return isUsed(usageMap, null);
	}

	/**
	 * Check whether the file is in use
	 * @param usageMap The file usage map as gotten from com.gentics.contentnode.events.DependencyManager.getFileUsageMap()
	 * @param nodeIds optional collection of nodeIds to check in
	 * @return true if the file is used, false otherwise
	 * @throws NodeException
	 */
	boolean isUsed(Map<Integer, Set<Integer>> usageMap, Collection<Integer> nodeIds) throws NodeException;

	/**
	 * Move this file into the target folder in the target channel.
	 * The implementation will do all necessary checks, including permission checks.
	 * @param target target folder, must not be null
	 * @param targetChannelId target channel id. 0 to move the file into the master node of the given folder, > 0 to move the file into a channel.
	 * @return operation result
	 * @throws NodeException if the move operation cannot be performed due to other reasons
	 */
	public OpResult move(Folder target, int targetChannelId) throws NodeException;

	/**
	 * Get a FileInformation object backed by the dbfile.
	 * @return the FileInformation object
	 * @throws TransactionException
	 */
	public FileInformation getFileInformation() throws TransactionException;

	/**
	 * Get the binary file
	 * @return binary file
	 * @throws TransactionException
	 */
	public java.io.File getBinFile() throws TransactionException;

	@Override
	default String getFullPublishPath(boolean trailingSlash, boolean includeNodePublishDir) throws NodeException {
		return getFullPublishPath(getFolder(), trailingSlash, includeNodePublishDir);
	}

	@Override
	default String getFullPublishPath(Folder folder, boolean trailingSlash, PageLanguageCode pageLanguageCode,
			boolean includeNodePublishDir) throws NodeException {
		List<String> segments = new ArrayList<>();

		if (includeNodePublishDir) {
			Node node = folder.getNode();
			ContentRepository cr = node.getContentRepository();

			if (cr == null || !cr.ignoreNodePublishDir()) {
				segments.add(node.getBinaryPublishDir());
			}
		}

		segments.add(folder.getPublishPath());

		return FilePublisher.getPath(
			true,
			trailingSlash,
			segments.toArray(new String[0]));
	}

	@Override
	default String getSuffix() {
		return isImage() ? ".image" : ".file";
	}

	@Override
	default Optional<File> maybeBinary() {
		return Optional.of(this);
	}

	@Override
	default Optional<Node> maybeGetChannel() throws NodeException {
		return Optional.ofNullable(getChannel());
	}

	@Override
	default void setFolder(Node node, Folder parent) throws NodeException {
		if (isNew()) {
			setFolderId(parent.getId());
		} else {
			OpResult moveResult = move(parent, Optional.ofNullable(node).map(Node::getId).orElse(0));
			if (!moveResult.isOK()) {
				String message = moveResult.getMessages().stream().map(NodeMessage::getMessage).collect(Collectors.joining(", "));
				throw new NodeException(message);
			}
		}
	}
}
