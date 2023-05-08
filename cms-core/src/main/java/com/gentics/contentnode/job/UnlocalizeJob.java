package com.gentics.contentnode.job;

import java.util.Collection;
import java.util.List;
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
 * A job that unlocalizes a list of objects of a specified type in the specified channel.<br/>
 * Folders can be unlocalized recursively.<br/><br/>
 * Parameter:<ul>
 * <li>{@link UnlocalizeJob#PARAM_USERID} (mandatory)<br />
 * The user who started the localize job.<br />
 * Against this userId permissions will be checked as well as notifications sent when the job finishes in background.
 * </li>
 * <li>{@link UnlocalizeJob#PARAM_SESSIONID} (mandatory)<br />
 * SessionId of a session from the user specified in <code>PARAM_USERID</code>.
 * It doesn't matter if the session with that given id doesn't exist anymore. 
 * This could happen for example on job recover.
 * </li>
 * <li>{@link UnlocalizeJob#PARAM_IDS} (mandatory)<br />
 * A Collection of id's to localize
 * </li>
 * <li>{@link UnlocalizeJob#PARAM_CLASS} (mandatory)<br />
 * The class of the the object to localize
 * </li>
 * <li>{@link UnlocalizeJob#PARAM_CHANNEL} (mandatory)<br />
 * The id of the channel to unlocalize in
 * </li>
 * <li>{@link UnlocalizeJob#PARAM_RECURSIVE} (optional)<br />
 * True, if folders shall be unlocalized recursively
 * </li>
 * </ul>
 */
public class UnlocalizeJob extends AbstractUserActionJob {

	/**
	 * Parameter that specifies the objects to unlocalize
	 */
	public static final String PARAM_IDS = "ids";
    
	/**
	 * Parameter that specifies the objecttype that should be unlocalized
	 */
	public static final String PARAM_CLASS = "type";

	/**
	 * Parameter that specifies the channel id (node id)
	 */
	public final static String PARAM_CHANNEL = "channel";

	/**
	 * Parameter to specify whether folders shall be unlocalized recursively
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
	 * Class of objects to unlocalize
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * collection of object id's to be unlocalized
	 */
	protected Collection<Integer> ids;

	/**
	 * Id of the channel to unlocalize in
	 */
	protected Integer channelId;

	/**
	 * True if folders shall be unlocalized recursive
	 */
	protected boolean recursive;

	/**
	 * True if pages in folders shall be unlocalized
	 */
	protected boolean pages;

	/**
	 * True if templates in folders shall be unlocalized
	 */
	protected boolean templates;

	/**
	 * True if images in folders shall be unlocalized
	 */
	protected boolean images;

	/**
	 * True if files in folders shall be unlocalized
	 */
	protected boolean files;

	/**
	 * True if folders in folders shall be unlocalized
	 */
	protected boolean folders;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	@Override
	public String getJobDescription() {
		return new CNI18nString("channelsync.unlocalize").toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean getJobParameters(JobDataMap map) {
		clazz = (Class<? extends NodeObject>) map.get(PARAM_CLASS);
		ids = ObjectTransformer.getCollection(map.get(PARAM_IDS), null);
		channelId = ObjectTransformer.getInteger(map.get(PARAM_CHANNEL),null);
		recursive = ObjectTransformer.getBoolean(map.get(PARAM_RECURSIVE), false);
		pages = ObjectTransformer.getBoolean(map.get(PARAM_PAGES), false);
		templates = ObjectTransformer.getBoolean(map.get(PARAM_TEMPLATES), false);
		images = ObjectTransformer.getBoolean(map.get(PARAM_IMAGES), false);
		files = ObjectTransformer.getBoolean(map.get(PARAM_FILES), false);
		folders = ObjectTransformer.getBoolean(map.get(PARAM_FOLDERS), false);

		return clazz != null && ids != null && channelId != null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction()
	 */
	@Override
	protected void processAction() throws InsufficientPrivilegesException,
				NodeException, JobExecutionException {
		Node channel = t.getObject(Node.class, channelId);

		if (channel == null) {
			throw new NodeException("Could not find channel {" + channelId + "}");
		}
		// if the "channel" is not really one, we do nothing
		if (!channel.isChannel()) {
			return;
		}

		// prepare a counter to count readonly elements
		ObjectCounter readOnlyCounter = new ObjectCounter();

		// prepare the list of already unlocalized templates
		List<Template> unlocalizedTemplates = new Vector<Template>();

		t.setChannelId(channelId);
		try {
			// get the objects to unlocalize
			List<? extends NodeObject> objects = t.getObjects(clazz, ids);
	
			for (NodeObject o : objects) {
				if (o instanceof Page) {
					unlocalizePage((Page) o, channel, readOnlyCounter);
				} else if (o instanceof File) {
					unlocalizeFile((File) o, channel, readOnlyCounter);
				} else if (o instanceof Template) {
					unlocalizeTemplate((Template) o, channel, unlocalizedTemplates, readOnlyCounter);
				} else if (o instanceof Folder) {
					Folder folder = (Folder) o;

					// unlocalize the contents of the folder
					unlocalizeFolderContents(folder, channel, unlocalizedTemplates, readOnlyCounter);

					// if recursive flag is set, unlocalize child folders
					if (recursive) {
						recursiveUnlocalizeFolder(folder, channel, unlocalizedTemplates, readOnlyCounter);
					}

					unlocalizeFolder(folder, channel, readOnlyCounter);
				}
			}
		} finally {
			t.resetChannel();
		}

		// check if we ancountered readonly objects, that could not be unlocalized
		if (readOnlyCounter.hasCount()) {
			CNI18nString message = new CNI18nString("channelsync.unlocalize.readonly");
			StringBuffer msgBuf = new StringBuffer(message.toString());

			msgBuf.append(" ").append(readOnlyCounter.getI18nString());
			addMessage(new DefaultNodeMessage(Level.INFO, getClass(), msgBuf.toString()));
		}
	}

	/**
	 * Unlocalize the folder contents
	 * @param folder folder
	 * @param channel channel
	 * @param unlocalizedTemplates list of already unlocalized templates
	 * @param readOnlyCounter readonly counter
	 * @throws NodeException
	 */
	protected void unlocalizeFolderContents(Folder folder, Node channel, List<Template> unlocalizedTemplates, ObjectCounter readOnlyCounter) throws NodeException {
		if (pages) {
			List<Page> pages = folder.getPages();

			for (Page page : pages) {
				unlocalizePage(page, channel, readOnlyCounter);
			}
		}
		if (files) {
			List<File> files = folder.getFiles();

			for (File file : files) {
				unlocalizeFile(file, channel, readOnlyCounter);
			}
		}
		if (images) {
			List<ImageFile> images = folder.getImages();

			for (ImageFile image : images) {
				unlocalizeFile(image, channel, readOnlyCounter);
			}
		}
		if (templates) {
			List<Template> templates = folder.getTemplates();

			for (Template template : templates) {
				unlocalizeTemplate(template, channel, unlocalizedTemplates, readOnlyCounter);
			}
		}
		if (folders) {
			List<Folder> children = folder.getChildFolders();

			for (Folder child : children) {
				unlocalizeFolder(child, channel, readOnlyCounter);
			}
		}
	}

	/**
	 * Recursive method to unlocalize the folder children
	 * @param folder folder
	 * @param channel channel
	 * @param unlocalizedTemplates list of already unlocalized templates
	 * @param readOnlyCounter readonly counter
	 * @throws NodeException
	 */
	protected void recursiveUnlocalizeFolder(Folder folder, Node channel,
			List<Template> unlocalizedTemplates, ObjectCounter readOnlyCounter) throws NodeException {
		List<Folder> childFolders = folder.getChildFolders();

		for (Folder child : childFolders) {
			unlocalizeFolderContents(child, channel, unlocalizedTemplates, readOnlyCounter);
			recursiveUnlocalizeFolder(child, channel, unlocalizedTemplates, readOnlyCounter);
		}
	}

	/**
	 * Unlocalize the given page from the given channel.
	 * Check for permissions first.
	 * @param page page to unlocalize
	 * @param channel channel from which to unlocalize
	 * @param readOnlyCounter readonly counter
	 * @throws NodeException
	 */
	protected void unlocalizePage(Page page, Node channel, ObjectCounter readOnlyCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.canView(page)) {
			return;
		}
		if (channel.equals(page.getChannel())) {
			if (page.isMaster() && !t.canDelete(page)) {
				readOnlyCounter.inc(Page.class);
			} else if (!page.isMaster() && !t.canEdit(page)) {
				readOnlyCounter.inc(Page.class);
			} else {
				try {
					t.getObject(Page.class, page.getId(), true);
					
					// remove the local page
					page.delete();
				} catch (ReadOnlyException e) {
					readOnlyCounter.inc(Page.class);
				}
			}
		}
	}

	/**
	 * Unlocalize the given file from the given channel.
	 * Check for permissions first.
	 * @param file file to unlocalize 
	 * @param channel channel from which to unlocalize
	 * @param readOnlyCounter readonly counter
	 * @throws NodeException
	 */
	protected void unlocalizeFile(File file, Node channel,
			ObjectCounter readOnlyCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.canView(file)) {
			return;
		}
		if (channel.equals(file.getChannel())) {
			if (file.isMaster() && !t.canDelete(file)) {
				readOnlyCounter.inc(file.getObjectInfo().getObjectClass());
			} else if (!file.isMaster() && !t.canEdit(file)) {
				readOnlyCounter.inc(file.getObjectInfo().getObjectClass());
			} else {
				// remove the local file
				file.delete();
			}
		}
	}

	/**
	 * Unlocalize the given template from the given channel.
	 * Check for permissions first.
	 * @param template template to unlocalize
	 * @param channel channel from which to unlocalize
	 * @param unlocalizedTemplates list of templates, which were already unlocalized
	 * @param readOnlyCounter readonly counter
	 * @throws NodeException
	 */
	protected void unlocalizeTemplate(Template template, Node channel,
			List<Template> unlocalizedTemplates, ObjectCounter readOnlyCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.canView(template)) {
			return;
		}
		// for templates, we need to check whether we already unlocalized them
		// (since templates may be linked to multiple folders)
		if (!unlocalizedTemplates.contains(template) && channel.equals(template.getChannel())) {
			// add the template to the list of already unlocalized templates.
			// this is done, before the template is actually unlocalized,
			// because even if unlocalizing fails, we don't want to treat this
			// template twice
			unlocalizedTemplates.add(template);
			if (template.isMaster() && !t.canDelete(template)) {
				readOnlyCounter.inc(Template.class);
			} else if (!template.isMaster() && !t.canEdit(template)) {
				readOnlyCounter.inc(Template.class);
			} else {
				try {
					// get an editable copy of the template (which will check
					// whether the template is locked)
					t.getObject(Template.class, template.getId(), true);
					template.delete();
				} catch (ReadOnlyException e) {
					readOnlyCounter.inc(Template.class);
				}
			}
		}
	}

	/**
	 * Unlocalize the given folder from the given channel.
	 * Check for permissions first.
	 * @param folder folder to unlocalize
	 * @param channel channel from which to unlocalize
	 * @param readOnlyCounter readonly counter
	 * @throws NodeException
	 */
	protected void unlocalizeFolder(Folder folder, Node channel,
			ObjectCounter readOnlyCounter) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (folder.isChannelRoot() || !t.canView(folder)) {
			return;
		}
		if (channel.equals(folder.getChannel())) {
			if (folder.isMaster() && !t.canDelete(folder)) {
				readOnlyCounter.inc(Folder.class);
			} else if (!folder.isMaster() && !t.canEdit(folder)) {
				readOnlyCounter.inc(Folder.class);
			} else {
				// remove the local folder
				folder.delete();
			}
		}
	}
}
