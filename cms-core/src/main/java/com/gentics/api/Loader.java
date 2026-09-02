package com.gentics.api;

import static com.gentics.contentnode.render.RenderUtils.wrap;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.log.NodeLogger;

/**
 * Loader helper, which should be used in Groovy Scripts to load objects
 */
public class Loader {
	private static NodeLogger logger = NodeLogger.getNodeLogger(Loader.class);

	/**
	 * Map of types to type ids
	 */
	private final static Map<String, Integer> TYPES = Map.of(
			"page", Page.TYPE_PAGE,
			"folder", Folder.TYPE_FOLDER,
			"file", File.TYPE_FILE,
			"image", ImageFile.TYPE_IMAGE,
			"node", Node.TYPE_NODE);

	/**
	 * Private constructor
	 */
	private Loader() {
	}

	/**
	 * Load object with given type and id
	 * @param type object type
	 * @param id object id (as integer)
	 * @return loaded object or null, if not found
	 * @throws NodeException
	 */
	public static Resolvable object(int type, int id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Class<? extends NodeObject> clazz = t.getClass(type);
		if (clazz == null) {
			logger.warn("Unable to load object of unknown type ID %d".formatted(type));
			return null;
		}

		NodeObject object = t.getObject(clazz, id);
		if (object == null && type == File.TYPE_FILE) {
			return object(ImageFile.TYPE_IMAGE, id);
		}

		return wrap(object);
	}

	/**
	 * Load object with given type and id
	 * @param type object type
	 * @param id object id (as string)
	 * @return loaded object or null, if not found
	 * @throws NodeException
	 */
	public static Resolvable object(int type, String id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Class<? extends NodeObject> clazz = t.getClass(type);
		if (clazz == null) {
			logger.warn("Unable to load object of unknown type ID %d".formatted(type));
			return null;
		}

		NodeObject object = t.getObject(clazz, id);
		if (object == null && type == File.TYPE_FILE) {
			return object(ImageFile.TYPE_IMAGE, id);
		}

		return wrap(object);
	}

	/**
	 * Load object with given type and id
	 * @param type type
	 * @param id id (as integer)
	 * @return loaded object or null, if not found
	 * @throws NodeException
	 */
	public static Resolvable object(String type, int id) throws NodeException {
		if (TYPES.containsKey(type)) {
			return object(TYPES.get(type), id);
		} else {
			logger.warn("Unable to load object of unknown type %s".formatted(type));
			return null;
		}
	}

	/**
	 * Load object with given type and id
	 * @param type type
	 * @param id id (as string)
	 * @return loaded object or null, if not found
	 * @throws NodeException
	 */
	public static Resolvable object(String type, String id) throws NodeException {
		if (TYPES.containsKey(type)) {
			return object(TYPES.get(type), id);
		} else {
			logger.warn("Unable to load object of unknown type %s".formatted(type));
			return null;
		}
	}

	/**
	 * Load page
	 * @param id page id
	 * @return wrapped page
	 * @throws NodeException
	 */
	public static Resolvable page(int id) throws NodeException {
		return object(Page.TYPE_PAGE, id);
	}

	/**
	 * Load page
	 * @param id page id
	 * @return wrapped page
	 * @throws NodeException
	 */
	public static Resolvable page(String id) throws NodeException {
		return object(Page.TYPE_PAGE, id);
	}

	/**
	 * Load folder
	 * @param id folder id
	 * @return wrapped folder
	 * @throws NodeException
	 */
	public static Resolvable folder(int id) throws NodeException {
		return object(Folder.TYPE_FOLDER, id);
	}

	/**
	 * Load folder
	 * @param id folder id
	 * @return wrapped folder
	 * @throws NodeException
	 */
	public static Resolvable folder(String id) throws NodeException {
		return object(Folder.TYPE_FOLDER, id);
	}

	/**
	 * Load file
	 * @param id file id
	 * @return wrapped file
	 * @throws NodeException
	 */
	public static Resolvable file(int id) throws NodeException {
		return object(File.TYPE_FILE, id);
	}

	/**
	 * Load file
	 * @param id file id
	 * @return wrapped file
	 * @throws NodeException
	 */
	public static Resolvable file(String id) throws NodeException {
		return object(File.TYPE_FILE, id);
	}

	/**
	 * Load image
	 * @param id image id
	 * @return wrapped image
	 * @throws NodeException
	 */
	public static Resolvable image(int id) throws NodeException {
		return object(ImageFile.TYPE_IMAGE, id);
	}

	/**
	 * Load image
	 * @param id image id
	 * @return wrapped image
	 * @throws NodeException
	 */
	public static Resolvable image(String id) throws NodeException {
		return object(ImageFile.TYPE_IMAGE, id);
	}

	/**
	 * Load node
	 * @param id node id
	 * @return wrapped node
	 * @throws NodeException
	 */
	public static Resolvable node(int id) throws NodeException {
		return object(Node.TYPE_NODE, id);
	}

	/**
	 * Load node
	 * @param id node id
	 * @return wrapped node
	 * @throws NodeException
	 */
	public static Resolvable node(String id) throws NodeException {
		return object(Node.TYPE_NODE, id);
	}
}
