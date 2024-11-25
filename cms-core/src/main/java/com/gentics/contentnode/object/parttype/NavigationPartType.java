/*
 * @author alexander
 * @date 14.03.2007
 * @version $Id: NavigationPartType.java,v 1.13 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Folder.PageSearch;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.velocity.SerializableVelocityTemplateWrapper;

import io.reactivex.Flowable;

/**
 * PartType 35 - Navigation
 */
public class NavigationPartType extends AbstractVelocityPartType implements TransformablePartType {

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(NavigationPartType.class);

	/**
	 * Static type code for folders.
	 */
	protected static final int TYPE_FOLDER = 10002;

	/**
	 * Property name for folders.
	 */
	protected static final String OBJECTS_FOLDERS = "folders";

	/**
	 * Property name for images.
	 */
	protected static final String OBJECTS_IMAGES = "images";

	/**
	 * Property name for files.
	 */
	protected static final String OBJECTS_FILES = "files";

	/**
	 * Property name for pages.
	 */
	protected static final String OBJECTS_PAGES = "pages";

	/**
	 * Property name for all types.
	 */
	protected static final String OBJECTS_ALL = "all";

	/**
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_STARTFOLDER = "startfolder.target";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_STARTFOLDER_DEFAULT = "node.folder";

	/**
	 * Name of input parameter for startfolder PROPERTY
	 */
	protected static final String INPUT_STARTFOLDER_PROPERTY = "startfolderproperty";

	/**
	 * Name of input parameter for template
	 */
	public static final String INPUT_TEMPLATE = "template";

	/**
	 * Name of input parameter for sitemap
	 */
	public static final String INPUT_SITEMAP = "sitemap";

	/**
	 * Default value for sitemap
	 */
	public static final String INPUT_SITEMAP_DEFAULT_STRING = "0";

	/**
	 * Default value for sitemap
	 */
	public static final boolean INPUT_SITEMAP_DEFAULT_BOOLEAN = false;

	/**
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_SORTBY = "sortby";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_SORTBY_DEFAULT = "name";

	/**
	 * Name of input parameter for sortorder
	 */
	protected static final String INPUT_SORTORDER = "sortorder";

	/**
	 * Value of sortorder ascending
	 */
	protected static final String INPUT_SORTORDER_ASC = "ASC";

	/**
	 * Value of sortorder descending
	 */
	protected static final String INPUT_SORTORDER_DESC = "DESC";

	/**
	 * Default value for sortorder
	 */
	protected static final String INPUT_SORTORDER_DEFAULT = INPUT_SORTORDER_ASC;

	/**
	 * Name of input parameter for tagname_sort
	 */
	protected static final String INPUT_TAGNAMESORT = "tagname_sort";

	/**
	 * Default value for tagname_sort
	 */
	protected static final String INPUT_TAGNAMESORT_DEFAULT = "navsort";

	/**
	 * Name of input parameter for tagname_hidden
	 */
	protected static final String INPUT_TAGNAMEHIDDEN = "tagname_hidden";

	/**
	 * Default value for tagname_hidden
	 */
	protected static final String INPUT_TAGNAMEHIDDEN_DEFAULT = "navhidden";

	/**
	 * Name of input parameter for objects
	 */
	protected static final String INPUT_OBJECTS = "objects";

	/**
	 * Name of input parameter for disable_hidden
	 */
	protected static final String INPUT_DISABLEHIDDEN = "disable_hidden";

	/**
	 * Default value for disable_hidden
	 */
	public static final String INPUT_DISABLEHIDDEN_DEFAULT_STRING = "0";

	/**
	 * Default value for disable_hidden
	 */
	protected static final boolean INPUT_DISABLEHIDDEN_DEFAULT_BOOLEAN = false;

	/**
	 * Name of input parameter for page
	 */
	protected static final String INPUT_PAGE = "page";

	/**
	 * Name of attribute for parent
	 */
	protected static final String NAV_PARENT = "parent";

	/**
	 * Name of attribute for folder of page
	 */
	protected static final String NAV_FOLDER = "folder";

	/**
	 * Name of attribute for objecttype
	 */
	protected static final String NAV_OBJECTTYPE = "ttype";

	/**
	 * Render the navigation. Gets all needed input parameters and starts
	 * rendering at startpage.
	 * @throws NodeException
	 */
	public String render() throws NodeException {
		logger.info("Start rendering navigation.");

		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		int editMode = renderType.getEditMode();
		boolean editModeChanged = false;

		try {
			// when edit mode is edit or realedit, switch to preview mode
			if (editMode == RenderType.EM_ALOHA) {
				editModeChanged = true;
				renderType.setEditMode(RenderType.EM_ALOHA_READONLY);
			}
			if (editModeChanged) {
				renderType.setParameter(CMSResolver.ModeResolver.PARAM_OVERWRITE_EDITMODE, new Integer(editMode));
			}

			ConfigObject config = getInitParameters();
			String navigation = renderObject(config.startfolder, 0, 0, 0, config.totalCounter, config);
	
			logger.info("End rendering navigation.");
			if (logger.isDebugEnabled()) {
				logger.debug("Navigation: " + navigation);
			}
	
			return navigation;
		} finally {
			// when edit mode was changed, change it back
			if (editModeChanged) {
				renderType.setEditMode(editMode);
				if (editModeChanged) {
					renderType.setParameter(CMSResolver.ModeResolver.PARAM_OVERWRITE_EDITMODE, null);
				}
			}
		}
	}

	/**
	 * Read all needed init parameters using the resolve() method. Populates
	 * instance variables.
	 * @throws NodeException
	 */
	protected ConfigObject getInitParameters() throws NodeException {

		logger.debug("Start reading configuration.");

		ConfigObject config = new ConfigObject();

		// get startfolder property from input parameters
		String startfolderProperty = ObjectTransformer.getString(resolve(INPUT_STARTFOLDER_PROPERTY), "");

		if (!"".equals(startfolderProperty)) {
			Object tmpStartfolder = resolve(startfolderProperty);

			if (tmpStartfolder instanceof Resolvable) {
				config.startfolder = (Resolvable) tmpStartfolder;
			}
		}

		if (config.startfolder == null) {
			// get startfolder from input parameters
			Object tmpStartfolder = resolve(INPUT_STARTFOLDER);

			if (tmpStartfolder instanceof Resolvable) {
				config.startfolder = (Resolvable) tmpStartfolder;
			}
		}

		// if no startfolder specified, use default node.folder
		if (config.startfolder == null) {
			config.startfolder = (Resolvable) resolve(INPUT_STARTFOLDER_DEFAULT);
		}

		// if still no startfolder, something has gone wrong
		if (config.startfolder == null) {
			logger.error("No startfolder set and couldn't find default start folder.");
			throw new NodeException("No startfolder set and couldn't find default start folder.");
		}

		// get template
		config.template = ObjectTransformer.getString(resolve(INPUT_TEMPLATE), "");
		if ("".equals(config.template)) {
			logger.error("No templates found.");
		}

		// get sortby
		// default to "name"
		config.sortBy = ObjectTransformer.getString(resolve(INPUT_SORTBY), INPUT_SORTBY_DEFAULT);
		if (config.sortBy == null || "".equals(config.sortBy)) {
			config.sortBy = INPUT_SORTBY_DEFAULT;
		}

		// get sortorder
		// default to "ASC"
		String sortOrder = ObjectTransformer.getString(resolve(INPUT_SORTORDER), INPUT_SORTORDER_DEFAULT);

		config.sortOrder = getSortOrder(sortOrder);

		// get tagname_sort
		// default to "navsort"
		config.tagnameSort = ObjectTransformer.getString(resolve(INPUT_TAGNAMESORT), INPUT_TAGNAMESORT_DEFAULT);
		if (config.tagnameSort == null || "".equals(config.tagnameSort)) {
			config.tagnameSort = INPUT_TAGNAMESORT_DEFAULT;
		}

		// get sitemap flag
		// default to false
		config.sitemap = ObjectTransformer.getBoolean(ObjectTransformer.getString(resolve(INPUT_SITEMAP), INPUT_SITEMAP_DEFAULT_STRING),
				INPUT_SITEMAP_DEFAULT_BOOLEAN);

		// get tagname_hidden
		// default to "navhidden"
		config.tagnameHidden = ObjectTransformer.getString(resolve(INPUT_TAGNAMEHIDDEN), INPUT_TAGNAMEHIDDEN_DEFAULT);
		if (config.tagnameHidden == null || "".equals(config.tagnameHidden)) {
			config.tagnameHidden = INPUT_TAGNAMEHIDDEN_DEFAULT;
		}

		// get objects to be displayed
		// default to pages, folders, files
		String tmpObjects = ObjectTransformer.getString(resolve(INPUT_OBJECTS), "");

		config.objects = new HashSet();
		if (tmpObjects == null || "".equals(tmpObjects)) {
			config.objects.add(OBJECTS_PAGES);
			config.objects.add(OBJECTS_FOLDERS);
			config.objects.add(OBJECTS_FILES);
		} else {
			String[] tObjects = tmpObjects.split(",");

			for (int i = 0; i < tObjects.length; i++) {
				if (OBJECTS_FILES.equals(tObjects[i].toLowerCase())) {
					config.objects.add(OBJECTS_FILES);
				} else if (OBJECTS_FOLDERS.equals(tObjects[i].toLowerCase())) {
					config.objects.add(OBJECTS_FOLDERS);
				} else if (OBJECTS_IMAGES.equals(tObjects[i].toLowerCase())) {
					config.objects.add(OBJECTS_IMAGES);
				} else if (OBJECTS_PAGES.equals(tObjects[i].toLowerCase())) {
					config.objects.add(OBJECTS_PAGES);
				} else if (OBJECTS_ALL.equals(tObjects[i].toLowerCase())) {
					config.objects.add(OBJECTS_ALL);
				}
			}
		}

		// get disable_hidden
		config.disableHidden = ObjectTransformer.getBoolean(ObjectTransformer.getString(resolve(INPUT_DISABLEHIDDEN), INPUT_DISABLEHIDDEN_DEFAULT_STRING),
				INPUT_DISABLEHIDDEN_DEFAULT_BOOLEAN);

		// get current page
		Object tmpCurrentPage = resolve(INPUT_PAGE);

		if (tmpCurrentPage instanceof Resolvable) {
			config.currentPage = (Resolvable) tmpCurrentPage;
		} else {
			logger.warn("Current page not found.");
		}

		// get path from current page to startpage
		config.path = new HashSet();
		Resolvable pathitem = config.currentPage;

		while (pathitem != null) {
			config.path.add(pathitem);
			Object parent = pathitem.get(NAV_PARENT);

			if (parent instanceof Resolvable) {
				pathitem = (Resolvable) parent;
			} else {
				parent = pathitem.get(NAV_FOLDER);
				if (parent != pathitem && parent instanceof Resolvable) {
					pathitem = (Resolvable) parent;
				} else {
					pathitem = null;
				}
			}
		}

		config.totalCounter = 0;

		// parse template
		config.wrapper = parseTemplate(config.template);

		if (logger.isDebugEnabled()) {
			logger.debug("End reading configuration." + config);
		}

		return config;
	}

	/**
	 * Render a resovable object.
	 * @param object The resovable object that should be rendered.
	 * @param level The current level of the object (startpage has level 0).
	 * @param relativeNr The position of this element relative to the parent
	 *        element.
	 * @param relativeCount The number of elements with the same parent and the
	 *        same level as this one.
	 * @param absoluteNr The total position of this element in the navigation
	 * @return The rendered string containing this and all sub-elements.
	 * @throws NodeException
	 */
	protected String renderObject(Resolvable object, int level, int relativeNr,
			int relativeCount, int absoluteNr, ConfigObject config) throws NodeException {

		if (logger.isDebugEnabled()) {
			logger.debug("Start rendering object: " + object.toString());
		}

		NavObject nav = new NavObject(object, level, relativeNr, relativeCount, absoluteNr, config);
		VelocityContext context = new VelocityContext(createContext(true));

		context.put("ctx", context);
		context.put("nav", nav);

		// TODO initial size
		StringWriter outwriter = new StringWriter();

		try {
			if (config.wrapper != null) {
				logger.debug("Merging main template.");
				mergeTemplate(config.wrapper, context, outwriter);
			} else {
				logger.warn("No template available.");
			}
		} catch (ParseErrorException pee) {
			logger.error("ParseErrorException while rendering object. " + pee.getMessage());
			throw new NodeException("ParseErrorException while rendering object. " + pee.getMessage(), pee);
		} catch (ResourceNotFoundException rnfe) {
			logger.error("ResourceNotFoundException while rendering object. " + rnfe.getMessage());
			throw new NodeException("ResourceNotFoundException while rendering object. " + rnfe.getMessage(), rnfe);
		} catch (MethodInvocationException mie) {
			logger.error("MethodInvocationException while rendering object. " + mie.getMessage());
			throw new NodeException("MethodInvocationException while rendering object. " + mie.getMessage(), mie);
		} catch (IOException ioe) {
			logger.error("IOException while rendering object. " + ioe.getMessage());
			throw new NodeException("IOException while rendering object. " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			logger.error("Exception while rendering object. " + e.getMessage());
			throw new NodeException("Exception while rendering object. " + e.getMessage(), e);
		}

		return outwriter.toString();
	}

	/**
	 * Decode sort order from String to int
	 * @param sortOrder String representing sortorder ("asc", "desc")
	 * @return int representing sortorder
	 */
	protected int getSortOrder(String sortOrder) {
		if (INPUT_SORTORDER_ASC.equalsIgnoreCase(sortOrder)) {
			return Datasource.SORTORDER_ASC;
		} else if (INPUT_SORTORDER_DESC.equalsIgnoreCase(sortOrder)) {
			return Datasource.SORTORDER_DESC;
		} else {
			return Datasource.SORTORDER_ASC;
		}
	}

	@Override
	public Type getPropertyType() {
		return Type.NAVIGATION;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}

	@Override
	public Property toProperty() throws NodeException {
		return null;
	}

	/**
	 * The object representing a navigation element.
	 * @author alexander
	 */
	public class NavObject {

		/**
		 * The resolvable object this object renders
		 */
		protected Resolvable object;

		/**
		 * The level of this element
		 */
		protected int level;

		/**
		 * The position of this element relative to the parent element.
		 */
		protected int relativeNr;

		/**
		 * The total position of this element in the navigation
		 */
		protected int absoluteNr;

		/**
		 * The number of elements with the same parent and same level as this
		 * one (number of siblings).
		 */
		protected int relativeCount;

		/**
		 * The cached result of the hasItems() method.
		 */
		protected boolean hasItems;

		/**
		 * If hasItems contains a valid result.
		 */
		protected boolean hasItemsCache;

		/**
		 * The cached result of the getChildrenCount() method.
		 */
		protected int childrenCount;

		/**
		 * If childrenCount contains a valid result.
		 */
		protected boolean childrenCountCache;

		/**
		 * The cached result of the isOpen() method.
		 */
		protected boolean isOpen;

		/**
		 * If isOpen contains a valid result.
		 */
		protected boolean isOpenCache;

		/**
		 * The name of the attribute by which elements in the folder should be
		 * sorted. Defaults to the global attribute.
		 */
		protected String folderSortBy;

		/**
		 * Order by which elements in the folder should be sorted. Defaults to
		 * the global order.
		 */
		protected int folderSortOrder;

		/**
		 * Store configuration settings
		 */
		protected ConfigObject config;

		/**
		 * Constructor of the Navigation Object.
		 * @param object The resolvable object that is rendered.
		 * @param level The level of the object.
		 * @param relativeNr The position of this element relative to its
		 *        parent.
		 * @param relativeCount The number of elements with the same level and
		 *        the same parent (number of siblings).
		 * @param absoluteNr The total position of this element in the
		 *        navigation.
		 */
		public NavObject(Resolvable object, int level, int relativeNr, int relativeCount,
				int absoluteNr, ConfigObject config) {

			this.object = object;
			this.level = level;
			this.relativeNr = relativeNr;
			this.absoluteNr = absoluteNr;
			this.relativeCount = relativeCount;
			this.config = config;

			this.hasItems = false;
			this.hasItemsCache = false;

			this.childrenCount = 0;
			this.childrenCountCache = false;

			this.isOpen = false;
			this.isOpenCache = false;

			folderSortBy = config.sortBy;
			folderSortOrder = config.sortOrder;
		}

		/**
		 * Check if this object has a specific sorting set as a property.
		 */
		protected void checkSorting() {
			// get tagname_sort, which should contain a string in the format
			// sortorder;sortby. if it exists, set as default sort order for
			// this folder
			Object tmpSorting = null;

			try {
				tmpSorting = PropertyResolver.resolve(object, "object." + config.tagnameSort);
			} catch (UnknownPropertyException upe) {
				tmpSorting = null;
			}
			String sorting = ObjectTransformer.getString(tmpSorting, "");

			if (sorting == null || "".equals(sorting)) {
				folderSortBy = config.sortBy;
				folderSortOrder = config.sortOrder;
			} else {
				String[] sort = sorting.split(";");

				if (sort.length != 2) {
					folderSortBy = config.sortBy;
					folderSortOrder = config.sortOrder;
				} else {
					folderSortOrder = getSortOrder(sort[0]);
					folderSortBy = sort[1];
				}
			}
		}

		/**
		 * Get the resovable object that is rendered.
		 * @return The resolvable object.
		 */
		public Resolvable getObject() {
			return new RenderableResolvable(object);
		}

		/**
		 * Get the level of the element (startfolder has level 0).
		 * @return Level of the element.
		 */
		public int getLevel() {
			return level;
		}

		/**
		 * @param element The resolvable for which the hidden attribute should
		 *        be resolved.
		 * @return True if the element is hidden from navigation, false
		 *         otherwise.
		 */
		protected boolean getHidden(Resolvable element) {
			try {
				return ObjectTransformer.getBoolean(PropertyResolver.resolve(new RenderableResolvable(element), "object." + config.tagnameHidden), false);
			} catch (UnknownPropertyException upe) {
				return false;
			}
		}

		/**
		 * Checks any child elements that are included in objects and are
		 * visible for the navigation. Cache the result to speed up following
		 * calls.
		 * @return If the element has child elements.
		 */
		public boolean hasItems() {
			// check cache to see if we already calculated hasItems
			if (hasItemsCache) {
				return hasItems;
			}

			// get object type for this object
			Object tmpType = object.get(NAV_OBJECTTYPE);
			int type = ObjectTransformer.getInt(tmpType, 0);

			// only folders can have sub items
			if (type != TYPE_FOLDER) {
				return false;
			}

			// check if object contains pages
			if (config.objects.contains(OBJECTS_PAGES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpPages = object.get(OBJECTS_PAGES);

				if (tmpPages instanceof List) {
					List pagesList = (List) tmpPages;
					Iterator it = pagesList.iterator();

					while (it.hasNext()) {
						Object tmpPage = it.next();

						if (tmpPage instanceof Resolvable) {
							Resolvable page = (Resolvable) tmpPage;
							boolean online = (page.get("online") == Boolean.TRUE);
							boolean hidden = getHidden(page);

							if ((!hidden || config.disableHidden) && online) {
								hasItems = true;
								hasItemsCache = true;
								return true;
							}
						}
					}
				}
			}

			// check if object contains folders
			if (config.objects.contains(OBJECTS_FOLDERS) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFolders = object.get(OBJECTS_FOLDERS);

				if (tmpFolders instanceof List) {
					List foldersList = (List) tmpFolders;
					Iterator it = foldersList.iterator();

					while (it.hasNext()) {
						Object tmpFolder = it.next();

						if (tmpFolder instanceof Resolvable) {
							Resolvable folder = (Resolvable) tmpFolder;
							boolean hidden = getHidden(folder);

							if (!hidden || config.disableHidden) {
								hasItems = true;
								hasItemsCache = true;
								return true;
							}
						}
					}
				}
			}

			// check if object contains files
			if (config.objects.contains(OBJECTS_FILES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFiles = object.get(OBJECTS_FILES);

				if (tmpFiles instanceof List) {
					List filesList = (List) tmpFiles;
					Iterator it = filesList.iterator();

					while (it.hasNext()) {
						Object tmpFile = it.next();

						if (tmpFile instanceof Resolvable) {
							Resolvable file = (Resolvable) tmpFile;
							boolean hidden = getHidden(file);

							if (!hidden || config.disableHidden) {
								hasItems = true;
								hasItemsCache = true;
								return true;
							}
						}
					}
				}
			}

			// check if object contains images
			if (config.objects.contains(OBJECTS_IMAGES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpImages = object.get(OBJECTS_IMAGES);

				if (tmpImages instanceof List) {
					List imagesList = (List) tmpImages;
					Iterator it = imagesList.iterator();

					while (it.hasNext()) {
						Object tmpImage = it.next();

						if (tmpImage instanceof Resolvable) {
							Resolvable image = (Resolvable) tmpImage;
							boolean hidden = getHidden(image);

							if (!hidden || config.disableHidden) {
								hasItems = true;
								hasItemsCache = true;
								return true;
							}
						}
					}
				}
			}

			// neither files, folders, images, pages found, folder is empty
			hasItems = false;
			hasItemsCache = true;

			return false;
		}

		/**
		 * Check to see if this folder should be opened in the navigation. Cache
		 * to speed up following calls.
		 * @return If folder is open.
		 */
		public boolean isOpen() {
			if (isOpenCache) {
				return isOpen;
			}

			boolean open = false;
			// get object type for this object
			Object tmpType = object.get(NAV_OBJECTTYPE);
			int type = ObjectTransformer.getInt(tmpType, 0);

			// only folders can have sub items
			if (type == TYPE_FOLDER) {
				open = isInpath() || config.sitemap || config.startfolder.equals(object);
			}

			isOpenCache = true;
			isOpen = open;
			return open;
		}

		/**
		 * Get the relative position of this element relative to its parent.
		 * @return The relative position.
		 */
		public int getRelativenr() {
			return relativeNr;
		}

		/**
		 * Get the absolute position of this element in the navigation.
		 * @return The absolute position.
		 */
		public int getAbsolutenr() {
			return absoluteNr;
		}

		/**
		 * Get the number of elements in the same level with the same parent
		 * (number of siblings).
		 * @return The number of siblings.
		 */
		public int getRelativecount() {
			return relativeCount;
		}

		/**
		 * Get the number of child elements for this folder. Only count elements
		 * enabled through the objects parameter and only count visible
		 * elements.
		 * @return The number of children for this element.
		 */
		public int getChildrencount() {
			// check cache to see if we already calculated hasItems
			if (childrenCountCache) {
				return childrenCount;
			}

			int count = 0;

			// get object type for this object
			Object tmpType = object.get(NAV_OBJECTTYPE);
			int type = ObjectTransformer.getInt(tmpType, 0);

			// only folders can have sub items
			if (type != TYPE_FOLDER) {
				return count;
			}

			// check if object contains pages
			if (config.objects.contains(OBJECTS_PAGES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpPages = object.get(OBJECTS_PAGES);

				if (tmpPages instanceof List) {
					List pagesList = (List) tmpPages;
					Iterator it = pagesList.iterator();

					while (it.hasNext()) {
						Object tmpPage = it.next();

						if (tmpPage instanceof Resolvable) {
							Resolvable page = (Resolvable) tmpPage;
							boolean hidden = getHidden(page);
							boolean online = (Boolean)page.get("online");

							if (online && (!hidden || config.disableHidden)) {
								count++;
							}
						}
					}
				}
			}

			// check if object contains folders
			if (config.objects.contains(OBJECTS_FOLDERS) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFolders = object.get(OBJECTS_FOLDERS);

				if (tmpFolders instanceof List) {
					List foldersList = (List) tmpFolders;
					Iterator it = foldersList.iterator();

					while (it.hasNext()) {
						Object tmpFolder = it.next();

						if (tmpFolder instanceof Resolvable) {
							Resolvable folder = (Resolvable) tmpFolder;
							boolean hidden = getHidden(folder);

							if (!hidden || config.disableHidden) {
								count++;
							}
						}
					}
				}
			}

			// check if object contains files
			if (config.objects.contains(OBJECTS_FILES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFiles = object.get(OBJECTS_FILES);

				if (tmpFiles instanceof List) {
					List filesList = (List) tmpFiles;
					Iterator it = filesList.iterator();

					while (it.hasNext()) {
						Object tmpFile = it.next();

						if (tmpFile instanceof Resolvable) {
							Resolvable file = (Resolvable) tmpFile;
							boolean hidden = getHidden(file);

							if (!hidden || config.disableHidden) {
								count++;
							}
						}
					}
				}
			}

			// check if object contains images
			if (config.objects.contains(OBJECTS_IMAGES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpImages = object.get(OBJECTS_IMAGES);

				if (tmpImages instanceof List) {
					List imagesList = (List) tmpImages;
					Iterator it = imagesList.iterator();

					while (it.hasNext()) {
						Object tmpImage = it.next();

						if (tmpImage instanceof Resolvable) {
							Resolvable image = (Resolvable) tmpImage;
							boolean hidden = getHidden(image);

							if (!hidden || config.disableHidden) {
								count++;
							}
						}
					}
				}
			}

			childrenCount = count;
			childrenCountCache = true;

			return count;
		}

		/**
		 * Check to see if this element is in the path between the current page
		 * and the startpage
		 * @return If this element is in the path.
		 */
		public boolean isInpath() {
			return config.path.contains(object);
		}

		/**
		 * Check to see if this element is the current page.
		 * @return If this element is the current page.
		 */
		public boolean isCurrentpage() {
			if (config.currentPage != null) {
				return config.currentPage.equals(object);
			} else {
				return false;
			}
		}

		/**
		 * Render the subtree (all child elements) of this element. Only render
		 * elements listed in objects, and only render visible objects.
		 * @return The rendered subtree.
		 * @throws NodeException
		 */
		public String getSubtree() throws NodeException {
			// check if node is open
			if (!isOpen()) {
				return "";
			}

			// TODO initial size for stringbuffer
			StringBuffer outputBuffer = new StringBuffer();

			// check to see if this object has a special sort order set
			// in tagname_sort
			checkSorting();

			// local counters
			int relativeNr = 0;

			// this list will hold all objects from the navigation for sorting
			List<Resolvable> objects = new ArrayList<>();

			// get folders
			if (config.objects.contains(OBJECTS_FOLDERS) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFolders = object.get(OBJECTS_FOLDERS);

				if (tmpFolders instanceof List) {
					Flowable<Resolvable> flowable = Flowable.fromIterable((List<?>) tmpFolders).filter(o -> o instanceof Resolvable).map(o -> (Resolvable) o);
					if (!config.disableHidden) {
						// filter hidden
						flowable = flowable.filter(res -> !getHidden(res));
					}
					objects.addAll(flowable.toList().blockingGet());
				}
			}

			// get pages
			if (config.objects.contains(OBJECTS_PAGES) || config.objects.contains(OBJECTS_ALL)) {
				if (object instanceof Folder) {
					Folder folder = (Folder)object;
					PageSearch search = new PageSearch();
					search.setOnline(true);
					List<Page> tmpPages = folder.getPages(search);

					if (!config.disableHidden) {
						// filter hidden
						tmpPages = Flowable.fromIterable(tmpPages).filter(res -> !getHidden(res)).toList().blockingGet();
					}
					objects.addAll(tmpPages);
				}
			}

			// get files
			if (config.objects.contains(OBJECTS_FILES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFiles = object.get(OBJECTS_FILES);

				if (tmpFiles instanceof List) {
					Flowable<Resolvable> flowable = Flowable.fromIterable((List<?>) tmpFiles).filter(o -> o instanceof Resolvable).map(o -> (Resolvable) o);
					if (!config.disableHidden) {
						// filter hidden
						flowable = flowable.filter(res -> !getHidden(res));
					}
					objects.addAll(flowable.toList().blockingGet());
				}
			}

			// get images
			if (config.objects.contains(OBJECTS_IMAGES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpImages = object.get(OBJECTS_IMAGES);

				if (tmpImages instanceof List) {
					Flowable<Resolvable> flowable = Flowable.fromIterable((List<?>) tmpImages).filter(o -> o instanceof Resolvable).map(o -> (Resolvable) o);
					if (!config.disableHidden) {
						// filter hidden
						flowable = flowable.filter(res -> !getHidden(res));
					}
					objects.addAll(flowable.toList().blockingGet());
				}
			}

			// sort the list
			Collections.sort(objects, new ResolvableComparator(folderSortBy, folderSortOrder));

			childrenCount = objects.size();
			childrenCountCache = true;

			// render items
			for (Resolvable obj : objects) {
				outputBuffer.append(renderObject(obj, level + 1, ++relativeNr, childrenCount, ++config.totalCounter, config));
			}
			return outputBuffer.toString();
		}
	}

	public class ConfigObject {

		/**
		 * Startfolder of the navigation.
		 */
		public Resolvable startfolder;

		/**
		 * Template to use to render navigation.
		 */
		public String template;

		/**
		 * Property by which navigation should be sorted.
		 */
		public String sortBy;

		/**
		 * Order by which property should be sorted.
		 */
		public int sortOrder;

		/**
		 * Name of property which stores specific sort order for a folder.
		 */
		public String tagnameSort;

		/**
		 * If navigation should be displayed as sitemap (all folders flapped
		 * open).
		 */
		public boolean sitemap;

		/**
		 * Property which stores the hidden attribute name.
		 */
		public String tagnameHidden;

		/**
		 * If navigation should display hidden elements.
		 */
		public boolean disableHidden;

		/**
		 * Set containing all objects that should be included in the navigation.
		 */
		public HashSet objects;

		/**
		 * The current page for which the navigation is created.
		 */
		public Resolvable currentPage;

		/**
		 * Set containing all resolvables leading from the current page to the
		 * startpage.
		 */
		public HashSet path;

		/**
		 * Store all templates read from XML input.
		 */
		public SerializableVelocityTemplateWrapper wrapper;

		/**
		 * Counter over all elements in the navigation.
		 */
		public int totalCounter;

		public ConfigObject() {
			startfolder = null;
			template = null;
			sortBy = null;
			sortOrder = 0;
			tagnameSort = null;
			sitemap = INPUT_SITEMAP_DEFAULT_BOOLEAN;
			tagnameHidden = null;
			disableHidden = INPUT_DISABLEHIDDEN_DEFAULT_BOOLEAN;
			objects = null;
			currentPage = null;
			path = null;
			wrapper = null;
			totalCounter = 0;
		}

		/**
		 * Return all configuration parameters as string.
		 */
		public String toString() {
			return " startfolder: " + startfolder + ". template: " + template + ". sortBy: " + sortBy + ". sortOrder: " + sortOrder + ". tagnameSort: "
					+ tagnameSort + ". sitemap: " + sitemap + ". tagnameHidden: " + tagnameHidden + ". disableHidden: " + disableHidden + ". objects: " + objects
					+ ". currentPage: " + currentPage + ". path: " + path;
		}
	}

}
