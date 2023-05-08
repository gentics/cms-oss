/*
 * @author herbert
 * @date Jan 8, 2009
 * @version $Id: CMSLoaderImp.java,v 1.2 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.object.parttype.imps;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.imp.AbstractGenticsImp;
import com.gentics.api.portalnode.imp.GenticsImpInterface;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.lib.log.NodeLogger;

/**
 * A simple loader imp which can be used to load content objects like pages, folders, images and files.
 * @author herbert
 */
public class CMSLoaderImp extends AbstractGenticsImp implements GenticsImpInterface {
    
	private static NodeLogger logger = NodeLogger.getNodeLogger(CMSLoaderImp.class);
    
	public Object getObject(int type, Integer id) {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			Class clazz = t.getClass(type);
			NodeObject obj = t.getObject(clazz, id);

			if (obj == null && type == File.TYPE_FILE) {
				// if we haven't found a file with the given id, try images ..
				return new RenderableResolvable(t.getObject(t.getClass(ImageFile.TYPE_IMAGE), id));
			}
			return new RenderableResolvable(obj);
		} catch (TransactionException e) {
			logger.error("Error while retrieving transaction.", e);
			return null;
		} catch (NodeException e) {
			logger.error("Error while retrieving object of type {" + type + "} with id {" + id + "}", e);
			return null;
		}
	}
    
	/**
	 * get an object of the given type with the given id.
	 * @param type - the type of the object, one of: page, folder, file, image, node
	 * @param id the id of the content object
	 * @return the content object or null if not found or an error occurred.
	 */
	public Object getObject(String type, Integer id) {
		int typeId = 0;

		if ("page".equals(type)) {
			typeId = Page.TYPE_PAGE;
		} else if ("folder".equals(type)) {
			typeId = Folder.TYPE_FOLDER;
		} else if ("file".equals(type)) {
			typeId = File.TYPE_FILE;
		} else if ("image".equals(type)) {
			typeId = ImageFile.TYPE_IMAGE;
		} else if ("node".equals(type)) {
			typeId = Node.TYPE_NODE;
		} else {
			logger.warn("Requested invalid object type {" + type + "}");
			return null;
		}
		return getObject(typeId, id);
	}
    
	/**
	 * load object by contentid. - for example 10007.1234
	 */
	public Object getObject(String contentId) {
		int pos = contentId.indexOf(".");
		String typeString = contentId.substring(0, pos);
		String idString = contentId.substring(pos + 1);
        
		int type = Integer.parseInt(typeString);
		Integer id = new Integer(idString);

		return getObject(type, id);
	}

	public Object getFolder(Integer id) {
		return getObject(Folder.TYPE_FOLDER, id);
	}
    
	public Object getImage(Integer id) {
		return getObject(ImageFile.TYPE_IMAGE, id);
	}
    
	public Object getFile(Integer id) {
		return getObject(File.TYPE_FILE, id);
	}
    
	public Object getPage(Integer id) {
		return getObject(Page.TYPE_PAGE, id);
	}
    
	public Object getNode(Integer id) {
		return getObject(Node.TYPE_NODE, id);
	}

}
