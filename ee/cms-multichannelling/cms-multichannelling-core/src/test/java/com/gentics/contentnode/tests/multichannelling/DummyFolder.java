package com.gentics.contentnode.tests.multichannelling;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.object.ExtensibleObjectService;
import com.gentics.contentnode.object.AbstractFolder;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;

/**
 * Dummy Folder for testing purpose.
 * @author Najor
 */
public class DummyFolder extends AbstractFolder {

	private static final long serialVersionUID = 1L;
	private String name;
	private String masterNodeName;

	/**
	 * @param id
	 * @param info
	 */
	protected DummyFolder(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get masterNodeName
	 * @return the masterNodeName
	 */
	@Override
	public String getMasterNodeFolderName() {
		return masterNodeName;
	}


	/**
	 * Set masterNodeName
	 * @param masterNodeName the masterNodeName to set
	 */
	public void setMasterNodeName(String masterNodeName) {
		this.masterNodeName = masterNodeName;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ObjectTagContainer#getObjectTags()
	 */
	@Override
	public Map<String, ObjectTag> getObjectTags() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getChannelSetId()
	 */
	@Override
	public Integer getChannelSetId() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#getOwningNode()
	 */
	@Override
	public Node getOwningNode() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.LocalizableNodeObject#modifyChannelId(java.lang.Integer)
	 */
	@Override
	public void modifyChannelId(Integer channelId) throws ReadOnlyException,
			NodeException {
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copy()
	 */
	@Override
	public NodeObject copy() throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#setName(java.lang.String)
	 */
	@Override
	public String setName(String name) throws ReadOnlyException {
		this.name = name;
		return name;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getDescription()
	 */
	@Override
	public String getDescription() {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#setDescription(java.lang.String)
	 */
	@Override
	public String setDescription(String description) throws ReadOnlyException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getMother()
	 */
	@Override
	public Folder getMother() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getChannelMaster()
	 */
	@Override
	public Folder getChannelMaster() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#setMotherId(java.lang.Integer)
	 */
	@Override
	public Integer setMotherId(Integer folderId) throws NodeException,
			ReadOnlyException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getNode()
	 */
	@Override
	public Node getNode() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getPublishDir()
	 */
	@Override
	public String getPublishDir() {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#setPublishDir(java.lang.String)
	 */
	@Override
	public String setPublishDir(String pubDir) throws ReadOnlyException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getChildFolders()
	 */
	@Override
	public List<Folder> getChildFolders() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getChildFoldersCount()
	 */
	@Override
	public int getChildFoldersCount() throws NodeException {
		
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getTemplates(com.gentics.contentnode.object.Folder.TemplateSearch)
	 */
	@Override
	public List<Template> getTemplates(TemplateSearch search)
			throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getTemplatesCount()
	 */
	@Override
	public int getTemplatesCount() throws NodeException {
		
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getPages(com.gentics.contentnode.object.Folder.PageSearch)
	 */
	@Override
	public List<Page> getPages(PageSearch search) throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getPagesCount()
	 */
	@Override
	public int getPagesCount() throws NodeException {
		
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getFiles(com.gentics.contentnode.object.Folder.FileSearch)
	 */
	@Override
	public List<File> getFiles(FileSearch search) throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getFilesCount()
	 */
	@Override
	public int getFilesCount() throws NodeException {
		
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getImages(com.gentics.contentnode.object.Folder.FileSearch)
	 */
	@Override
	public List<ImageFile> getImages(FileSearch search) throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getImagesCount()
	 */
	@Override
	public int getImagesCount() throws NodeException {
		
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getFilesAndImages(com.gentics.contentnode.object.Folder.FileSearch)
	 */
	@Override
	public List<File> getFilesAndImages(FileSearch search) throws NodeException {
		
		return null;
	}

	@Override
	public List<Form> getForms(FormSearch search) throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getCDate()
	 */
	@Override
	public ContentNodeDate getCDate() {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getEDate()
	 */
	@Override
	public ContentNodeDate getEDate() {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getCreator()
	 */
	@Override
	public SystemUser getCreator() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getEditor()
	 */
	@Override
	public SystemUser getEditor() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#performUnlinkTemplate(java.lang.Integer)
	 */
	@Override
	protected void performUnlinkTemplate(Integer templateId)
			throws NodeException {
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#performDelete()
	 */
	@Override
	protected void performDelete() throws NodeException {
	}

	@Override
	protected void putIntoWastebin() throws NodeException {
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getFeature(java.lang.String)
	 */
	@Override
	protected boolean getFeature(String feature) throws NodeException {
		
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getTemplatesToDelete(java.util.Collection)
	 */
	@Override
	protected Collection<Template> getTemplatesToDelete(
			Collection<Template> templates) throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getTemplatesToDelete(java.util.Collection, boolean)
	 */
	@Override
	protected Collection<Template> getTemplatesToDelete(
			Collection<Template> templates, boolean addFolderToDeleteList)
			throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getStartpage()
	 */
	@Override
	public Page getStartpage() {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getChannelSet()
	 */
	@Override
	public Map<Integer, Integer> getChannelSet() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#getChannel()
	 */
	@Override
	public Node getChannel() throws NodeException {
		
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#hasChannel()
	 */
	@Override
	public boolean hasChannel() throws NodeException {
		
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#setChannelInfo(java.lang.Integer, java.lang.Integer, boolean)
	 */
	@Override
	public void setChannelInfo(Integer channelId, Integer channelSetId, boolean allowChange)
			throws ReadOnlyException, NodeException {
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#isInherited()
	 */
	@Override
	public boolean isInherited() throws NodeException {
		
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#isMaster()
	 */
	@Override
	public boolean isMaster() throws NodeException {
		
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.Folder#isChannelRoot()
	 */
	@Override
	public boolean isChannelRoot() throws NodeException {
		
		return false;
	}

	@Override
	public boolean isRoot() throws NodeException {
		return false;
	}

	@Override
	public boolean isExcluded() {
		return false;
	}

	@Override
	public Set<Node> getDisinheritedChannels() throws NodeException {
		return null;
	}

	@Override
	public void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recursive) throws NodeException {
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public boolean isDisinheritDefault() {
		return false;
	}

	@Override
	public void setDisinheritDefault(boolean value, boolean recursive) throws NodeException {
	}

	@Override
	public void setChannelMaster(Folder folder) throws ReadOnlyException, NodeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNodeId(Integer id) throws ReadOnlyException, NodeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTemplates(List<Template> templates) throws ReadOnlyException, NodeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public OpResult move(Folder target, int targetChannelId) throws ReadOnlyException, NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder getObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setChannelInfo(Integer channelId, Integer channelSetId) throws ReadOnlyException, NodeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Folder getMaster() throws NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder getNextHigherObject() throws NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Folder pushToMaster(Node master) throws ReadOnlyException, NodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public I18nMap getNameI18n() {
		return new I18nMap();
}

	@Override
	public I18nMap getDescriptionI18n() {
		return new I18nMap();
	}

	@Override
	public I18nMap getPublishDirI18n() {
		return new I18nMap();
	}

	@Override
	public List<? extends ExtensibleObjectService<Folder>> getServices() {
		return Collections.emptyList();
	}
}
