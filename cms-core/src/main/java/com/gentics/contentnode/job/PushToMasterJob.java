package com.gentics.contentnode.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.Level;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectCounter;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.lib.i18n.CNI18nString;

/**
 * A job that pushes a list of objects of a specified type to the specified master.<br/>
 * Folders can be pushed recursively.<br/><br/>
 * Parameter:<ul>
 * <li>{@link PushToMasterJob#PARAM_USERID} (mandatory)<br />
 * The user who started the localize job.<br />
 * Against this userId permissions will be checked as well as notifications sent when the job finishes in background.
 * </li>
 * <li>{@link PushToMasterJob#PARAM_SESSIONID} (mandatory)<br />
 * SessionId of a session from the user specified in <code>PARAM_USERID</code>.
 * It doesn't matter if the session with that given id doesn't exist anymore. 
 * This could happen for example on job recover.
 * </li>
 * <li>{@link PushToMasterJob#PARAM_IDS} (mandatory)<br />
 * A Collection of id's to localize
 * </li>
 * <li>{@link PushToMasterJob#PARAM_CLASS} (mandatory)<br />
 * The class of the the object to localize
 * </li>
 * <li>{@link PushToMasterJob#PARAM_MASTER} (mandatory)<br />
 * The id of the master to push to
 * </li>
 * <li>{@link PushToMasterJob#PARAM_CHANNEL} (mandatory)<br />
 * The id of the channel to push from
 * </li>
 * <li>{@link PushToMasterJob#PARAM_RECURSIVE} (optional)<br />
 * True, if folders shall be pushed recursively
 * </li>
 * </ul>
 */
public class PushToMasterJob extends AbstractUserActionJob {

	/**
	 * Parameter that specifies the objects to push to the master
	 */
	public static final String PARAM_IDS = "ids";
    
	/**
	 * Parameter that specifies the objecttype that should be pushed to the master
	 */
	public static final String PARAM_CLASS = "type";

	/**
	 * Parameter that specifies the master id (node id)
	 */
	public final static String PARAM_MASTER = "master";

	/**
	 * Parameter that specifies the channel id (node id)
	 */
	public final static String PARAM_CHANNEL = "channel";

	/**
	 * Parameter to specify whether folders shall be pushed recursively
	 */
	public final static String PARAM_RECURSIVE = "recursive";

	/**
	 * Parameter to specify whether pages in the folder shall be pushed
	 */
	public final static String PARAM_PAGES = "pages";

	/**
	 * Parameter to specify whether templates in the folder shall be pushed
	 */
	public final static String PARAM_TEMPLATES = "templates";

	/**
	 * Parameter to specify whether images in the folder shall be pushed
	 */
	public final static String PARAM_IMAGES = "images";

	/**
	 * Parameter to specify whether files in the folder shall be pushed
	 */
	public final static String PARAM_FILES = "files";

	/**
	 * Parameter to specify whether folders in the folder shall be pushed
	 */
	public final static String PARAM_FOLDERS = "folders";

	/**
	 * Class of objects to push to the master
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * collection of object id's to be pushed to the master
	 */
	protected Collection<Integer> ids;

	/**
	 * Id of the master to push to
	 */
	protected Integer masterId;

	/**
	 * Id of the channel to push from
	 */
	protected Integer channelId;

	/**
	 * True if folders shall be pushed recursive
	 */
	protected boolean recursive;

	/**
	 * True if pages in folders shall be pushed
	 */
	protected boolean pages;

	/**
	 * True if templates in folders shall be pushed
	 */
	protected boolean templates;

	/**
	 * True if images in folders shall be pushed
	 */
	protected boolean images;

	/**
	 * True if files in folders shall be pushed
	 */
	protected boolean files;

	/**
	 * True if folders in folders shall be pushed
	 */
	protected boolean folders;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	@Override
	public String getJobDescription() {
		return new CNI18nString("channelsync.push").toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean getJobParameters(JobDataMap map) {
		clazz = (Class<? extends NodeObject>) map.get(PARAM_CLASS);
		ids = ObjectTransformer.getCollection(map.get(PARAM_IDS), null);
		masterId = ObjectTransformer.getInteger(map.get(PARAM_MASTER), null);
		channelId = ObjectTransformer.getInteger(map.get(PARAM_CHANNEL), null);
		recursive = ObjectTransformer.getBoolean(map.get(PARAM_RECURSIVE), false);
		pages = ObjectTransformer.getBoolean(map.get(PARAM_PAGES), false);
		templates = ObjectTransformer.getBoolean(map.get(PARAM_TEMPLATES), false);
		images = ObjectTransformer.getBoolean(map.get(PARAM_IMAGES), false);
		files = ObjectTransformer.getBoolean(map.get(PARAM_FILES), false);
		folders = ObjectTransformer.getBoolean(map.get(PARAM_FOLDERS), false);

		return clazz != null && ids != null && masterId != null && channelId != null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction()
	 */
	@Override
	protected void processAction() throws InsufficientPrivilegesException,
				NodeException, JobExecutionException {
		// get master and channel, check whether they are really master and channel
		Node master = t.getObject(Node.class, masterId);

		if (master == null) {
			throw new NodeException("Could not find master node {" + masterId + "}");
		}
		Node channel = t.getObject(Node.class, channelId);

		if (channel == null) {
			throw new NodeException("Could not find channel {" + channelId + "}");
		}
		if (!channel.getMasterNodes().contains(master)) {
			throw new NodeException("{" + channel + "} is not a channel of {" + master + "}");
		}

		// prepare a counter to count readonly elements
		ObjectCounter readOnlyCounter = new ObjectCounter();

		// prepare a counter to count objects, that could not be pushed to the master,
		// because they are located in a folder, that does not exist in the master
		ObjectCounter inLocalFolderCounter = new ObjectCounter();

		// prepare a counter to count pages, that could not be pushed to the master,
		// because they use local templates, that do not exist in the master
		ObjectCounter localTemplateCounter = new ObjectCounter();

		// prepare the list of already pushed templates
		List<Template> pushedTemplates = new Vector<Template>();

		// set the channel id
		t.setChannelId(channelId);

		try {
			// get the objects to push to master
			List<? extends NodeObject> objects = t.getObjects(clazz, ids);
			
			for (NodeObject o : objects) {
				if (o instanceof Page) {
				pushPage((Page) o, master, channel, readOnlyCounter, inLocalFolderCounter, localTemplateCounter);
				} else if (o instanceof File) {
					pushFile((File) o, master, channel, readOnlyCounter, inLocalFolderCounter);
				} else if (o instanceof Template) {
					pushTemplate((Template) o, master, channel, pushedTemplates, readOnlyCounter, inLocalFolderCounter);
				} else if (o instanceof Folder) {
					Folder folder = (Folder) o;

					folder = pushFolder(folder, master, channel, readOnlyCounter, inLocalFolderCounter);

					// push the contents to the master
					pushFolderContentsToMaster(folder, channel, master, pushedTemplates, readOnlyCounter, inLocalFolderCounter, localTemplateCounter);

					// if recursive flag is set, push child folders
					if (recursive) {
						recursivePushFolderToMaster(folder, channel, master, pushedTemplates, readOnlyCounter, inLocalFolderCounter, localTemplateCounter);
					}
				}
			}

			// check if we encountered readonly objects, that could not be pushed
			if (readOnlyCounter.hasCount()) {
				CNI18nString message = new CNI18nString("channelsync.push.readonly");
				StringBuffer msgBuf = new StringBuffer(message.toString());
				
				msgBuf.append(" ").append(readOnlyCounter.getI18nString());
				addMessage(new DefaultNodeMessage(Level.INFO, getClass(), msgBuf.toString()));
			}
			
			// if inLocalFolderCounter has counts, we need to append to the message
			if (inLocalFolderCounter.hasCount()) {
				CNI18nString message = new CNI18nString("channelsync.push.inlocalfolder");
				StringBuffer msgBuf = new StringBuffer(message.toString());
				
				msgBuf.append(" ").append(inLocalFolderCounter.getI18nString());
				addMessage(new DefaultNodeMessage(Level.INFO, getClass(), msgBuf.toString()));
			}

			// append a message for pages that could not be pushed because they use local templates
			if (localTemplateCounter.hasCount()) {
				CNI18nString message = new CNI18nString("channelsync.push.localtemplate");
				StringBuffer msgBuf = new StringBuffer(message.toString());
	
				msgBuf.append(" ").append(localTemplateCounter.getI18nString());
				addMessage(new DefaultNodeMessage(Level.INFO, getClass(), msgBuf.toString()));
			}
		} finally {
			t.resetChannel();
		}
	}

	/**
	 * Push the folder contents to the master
	 * @param folder folder
	 * @param channel channel
	 * @param master master node
	 * @param pushedTemplates list of already pushed templates
	 * @param readOnlyCounter counter for readonly elements
	 * @param inLocalFolderCounter counter for object reside in local folders
	 * @param localTemplateCounter counter for pages with local templates
	 * @throws NodeException
	 */
	protected void pushFolderContentsToMaster(Folder folder, Node channel,
			Node master, List<Template> pushedTemplates, ObjectCounter readOnlyCounter, ObjectCounter inLocalFolderCounter, ObjectCounter localTemplateCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (pages) {
			List<Page> pages = folder.getPages();

			for (Page page : pages) {
				// for pages, we need to check for every instance, because the
				// permission might depend on the language (roles)
				if (t.canView(page)) {
					pushPage(page, master, channel, readOnlyCounter, inLocalFolderCounter, localTemplateCounter);
				}
			}
		}
		if (files && t.canView(folder, File.class, null)) {
			List<File> files = folder.getFiles();

			for (File file : files) {
				pushFile(file, master, channel, readOnlyCounter, inLocalFolderCounter);
			}
		}
		if (images && t.canView(folder, ImageFile.class, null)) {
			List<ImageFile> images = folder.getImages();

			for (ImageFile image : images) {
				pushFile(image, master, channel, readOnlyCounter, inLocalFolderCounter);
			}
		}
		if (templates && t.canView(folder, Template.class, null)) {
			List<Template> templates = folder.getTemplates();

			for (Template template : templates) {
				pushTemplate(template, master, channel, pushedTemplates, readOnlyCounter, inLocalFolderCounter);
			}
		}
		if (folders) {
			List<Folder> childFolders = folder.getChildFolders();

			for (Folder child : childFolders) {
				pushFolder(child, master, channel, readOnlyCounter, inLocalFolderCounter);
			}
		}
	}

	/**
	 * Recursive method to push the folder children to the master
	 * @param folder folder
	 * @param channel channel
	 * @param master master
	 * @param pushedTemplates list of already pushed templates
	 * @param readOnlyCounter counter to count readonly elements
	 * @param inLocalFolderCounter counter for object reside in local folders
	 * @param localTemplateCounter counter for pages with local templates
	 * @throws NodeException 
	 */
	protected void recursivePushFolderToMaster(Folder folder, Node channel, Node master, List<Template> pushedTemplates, ObjectCounter readOnlyCounter,
			ObjectCounter inLocalFolderCounter, ObjectCounter localTemplateCounter) throws NodeException {
		List<Folder> childFolders = folder.getChildFolders();

		for (Folder child : childFolders) {
			pushFolderContentsToMaster(child, channel, master, pushedTemplates, readOnlyCounter, inLocalFolderCounter, localTemplateCounter);
			recursivePushFolderToMaster(child, channel, master, pushedTemplates, readOnlyCounter, inLocalFolderCounter, localTemplateCounter);
		}
	}

	/**
	 * Push the page to the given master if it exists in the given channel.
	 * Check for permissions first and increase the page counter if insufficient
	 * @param page page to push
	 * @param master master to push the page to
	 * @param channel channel from which to push
	 * @param readOnlyCounter counter for readonly objects
	 * @param inLocalFolderCounter counter for object reside in local folders
	 * @param localTemplateCounter counter for pages that use local templates
	 * @throws NodeException
	 */
	protected void pushPage(Page page, Node master, Node channel,
			ObjectCounter readOnlyCounter, ObjectCounter inLocalFolderCounter, ObjectCounter localTemplateCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.canView(page)) {
			return;
		}
		if (channel.equals(page.getChannel())) {
			if (page.isMaster() && !t.canCreate(page.getFolder(), Page.class, page.getLanguage())) {
				readOnlyCounter.inc(Page.class);
			} else if (!page.isMaster() && !t.canEdit(page)) {
				readOnlyCounter.inc(Page.class);
			} else if (!folderExistsInNode(master, page.getFolder())) {
				inLocalFolderCounter.inc(Page.class);
			} else if (!templateExistsInNode(master, page.getTemplate())) {
				localTemplateCounter.inc(Page.class);
			} else {
				try {
					page.pushToMaster(master);
				} catch (ReadOnlyException e) {
					// count the number of readonly pages
					readOnlyCounter.inc(Page.class);
				}
			}
		}
	}

	/**
	 * Push the file to the given master if it exists in the given channel.
	 * Check for permissions first and increase the file counter if insufficient
	 * @param file file to push
	 * @param master master to push the file to
	 * @param channel channel from which to push
	 * @param readOnlyCounter counter for readonly objects
	 * @param inLocalFolderCounter counter for object reside in local folders
	 * @throws NodeException
	 */
	protected void pushFile(File file, Node master, Node channel,
			ObjectCounter readOnlyCounter, ObjectCounter inLocalFolderCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.canView(file)) {
			return;
		}
		if (channel.equals(file.getChannel())) {
			if (file.isMaster() && !t.canCreate(file.getFolder(), file.getObjectInfo().getObjectClass(), null)) {
				readOnlyCounter.inc(file.getObjectInfo().getObjectClass());
			} else if (!file.isMaster() && !t.canEdit(file)) {
				readOnlyCounter.inc(file.getObjectInfo().getObjectClass());
			} else if (!folderExistsInNode(master, file.getFolder())) {
				inLocalFolderCounter.inc(file.getObjectInfo().getObjectClass());
			} else {
				file.pushToMaster(master);
			}
		}
	}

	/**
	 * Push the template to the given master if it exists in the given channel.
	 * Check for permissions first and increase the template counter if insufficient.
	 * Additionally, since template may be linked to multiple folders, check whether the template was already pushed.
	 * @param template template to push
	 * @param master master to push the template to
	 * @param channel channel from which to push
	 * @param pushedTemplates list of already pushed templates (will be modified)
	 * @param readOnlyCounter counter for readonly objects
	 * @param inLocalFolderCounter counter for object reside in local folders
	 * @throws NodeException
	 */
	protected void pushTemplate(Template template, Node master, Node channel,
			List<Template> pushedTemplates, ObjectCounter readOnlyCounter, ObjectCounter inLocalFolderCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.canView(template)) {
			return;
		}
		// for templates, we need to check whether we already pushed them (since
		// templates may be linked to multiple folders)
		if (!pushedTemplates.contains(template) && channel.equals(template.getChannel())) {
			// add the template to the list of already pushed templates.
			// this is done, before the template is actually pushed, because
			// even if pushing fails, we don't want to treat this template twice
			pushedTemplates.add(template);
			List<Folder> templateFolders = template.getFolders();
			boolean permCreateTemplate = false;

			for (Folder templateFolder : templateFolders) {
				permCreateTemplate |= t.canCreate(templateFolder, Template.class, null);
			}
			if (template.isMaster() && !permCreateTemplate) {
				readOnlyCounter.inc(Template.class);
			} else if (!template.isMaster() && !t.canEdit(template)) {
				readOnlyCounter.inc(Template.class);
			} else if (!folderExistsInNode(master, templateFolders.toArray(new Folder[templateFolders.size()]))) {
				inLocalFolderCounter.inc(Template.class);
			} else {
				try {
					template.pushToMaster(master);
				} catch (ReadOnlyException e) {
					readOnlyCounter.inc(Template.class);
				}
			}
		}
	}

	/**
	 * Push the folder to the given master if it exists in the given channel.
	 * Silently omit channel root folders and folders the user cannot view.
	 * Check for permissions first and increase the folder counter if insufficient
	 * @param folder folder to push
	 * @param master master to push the folder to
	 * @param channel channel from which to push
	 * @param readOnlyCounter counter for readonly objects
	 * @param inLocalFolderCounter counter for object reside in local folders
	 * @throws NodeException
	 */
	protected Folder pushFolder(Folder folder, Node master, Node channel,
			ObjectCounter readOnlyCounter, ObjectCounter inLocalFolderCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (folder.isChannelRoot() || !t.canView(folder)) {
			return folder;
		}
		if (channel.equals(folder.getChannel())) {
			if (folder.isMaster() && !t.canCreate(folder.getMother(), Folder.class, null)) {
				readOnlyCounter.inc(Folder.class);
			} else if (!folder.isMaster() && !t.canEdit(folder)) {
				readOnlyCounter.inc(Folder.class);
			} else if (!folderExistsInNode(master, folder.getMother())) {
				inLocalFolderCounter.inc(Folder.class);
			} else {
				return folder.pushToMaster(master);
			}
		}
		
		return folder;
	}

	/**
	 * Check whether any of the given folders exist in the given node
	 * @param node node
	 * @param folders list of folders
	 * @return return true when at least one of the given folders (or any localized copies of it) exists in the given node, false if not
	 * @throws NodeException
	 */
	protected boolean folderExistsInNode(Node node, Folder... folders) throws NodeException {
		List<Object> channelIds = new Vector<Object>();

		channelIds.add(node.getId());
		channelIds.add(0);
		List<Node> masterNodes = node.getMasterNodes();

		for (Node master : masterNodes) {
			channelIds.add(master.getId());
		}
		for (Folder folder : folders) {
			Map<Integer, Integer> channelSet = folder.getChannelSet();

			if (channelSet.isEmpty()) {
				// the folder does not have a channelset, so it is a single master.
				Node channel = folder.getChannel();

				if (channel == null) {
					// the object exists in the master node, so it is visible
					return true;
				} else if (channelIds.contains(channel.getId())) {
					return true;
				}
			} else {
				for (Object channelId : channelSet.keySet()) {
					if (channelIds.contains(channelId)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the given template exists in the given node
	 * @param node node
	 * @param template template
	 * @return true if the template exists in the node, false if not
	 * @throws NodeException
	 */
	protected boolean templateExistsInNode(Node node, Template template) throws NodeException {
		Template master = template.getMaster();
		if (master.getChannel() == null) {
			return true;
		} else {
			Node channel = master.getChannel();
			return channel.equals(node) || node.isChannelOf(channel);
}
	}
}
