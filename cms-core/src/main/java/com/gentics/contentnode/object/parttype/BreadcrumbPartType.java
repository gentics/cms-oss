/*
 * @author alexander
 * @date 14.03.2007
 * @version $Id: BreadcrumbPartType.java,v 1.9 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.velocity.SerializableVelocityTemplateWrapper;

/**
 * PartType 34 - Breadcrumb
 */
public class BreadcrumbPartType extends AbstractVelocityPartType implements TransformablePartType {

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(BreadcrumbPartType.class);
    
	/**
	 * Static type code for folders.
	 */
	protected static final int TYPE_FOLDER = 10002;

	/**
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_STARTFOLDER = "startfolder.target";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_STARTFOLDER_DEFAULT = "node.folder";

	/**
	 * Name of input parameter for template
	 */
	public static final String INPUT_TEMPLATE = "template";

	/**
	 * Name of input parameter for startpage (of folder)
	 */
	protected static final String INPUT_TAGNAME_STARTPAGE = "tagname_startpage";
    
	/**
	 * Default value for startpage of folder
	 */
	protected static final String INPUT_TAGNAME_STARTPAGE_DEFAULT = "startpage";

	/**
	 * Name of input parameter for disable_fallback
	 */
	protected static final String INPUT_DISABLEFALLBACK = "disable_fallback";

	/**
	 * Default value for disable_fallback 0 ... use fallback to default language
	 * 1 ... cut (end breadcrumb if language not available) 2 ... skip (skip
	 * folder if language not available)
	 */
	protected static final String INPUT_DISABLEFALLBACK_DEFAULT = "0";

	/**
	 * Name of input parameter for disable_activepage
	 */
	protected static final String INPUT_DISABLEACTIVEPAGE = "disable_activepage";

	/**
	 * Default value for disable_activepage
	 */
	protected static final boolean INPUT_DISABLEACTIVEPAGE_DEFAULT = false;

	/**
	 * Name of input parameter for tagname_hidden
	 */
	protected static final String INPUT_TAGNAMEHIDDEN = "tagname_hidden";
    
	/**
	 * Default value for tagname_hidden
	 */
	protected static final String INPUT_TAGNAMEHIDDEN_DEFAULT = "navhidden";

	/**
	 * Name of input parameter for disable_hidden
	 */
	protected static final String INPUT_DISABLEHIDDEN = "disable_hidden";

	/**
	 * Default value for disable_hidden
	 */
	protected static final boolean INPUT_DISABLEHIDDEN_DEFAULT = false;

	/**
	 * Name of input parameter for page
	 */
	protected static final String INPUT_PAGE = "page";

	/**
	 * Name of attribute for folder of page
	 */
	protected static final String NAV_FOLDER = "folder";

	/**
	 * Name of attribute for parent
	 */
	protected static final String NAV_PARENT = "parent";

	/**
	 * Name of attribute for objecttype
	 */
	protected static final String NAV_OBJECTTYPE = "ttype";

	/**
	 * Name of attribute for languagecode
	 */
	protected static final String NAV_LANGUAGECODE = "language.code";

	/**
	 * Name of attribute for translated name
	 */
	protected static final String NAV_NAME_LANGUAGE = "object.name_";

	/**
	 * Name of attribute for name
	 */
	protected static final String NAV_NAME = "name";

	/**
	 * Name of attribute for language variants
	 */
	protected static final String NAV_LANGUAGES = "languageset.pages.";

	/**
	 * Render the navigation. Gets all needed input parameters and starts
	 * rendering at startpage. Parse old-style templates from XML.
	 * @throws NodeException
	 */
	public String render() throws NodeException {
		logger.info("Start rendering breadcrumb.");
        
		ConfigObject config = getInitParameters();

		Iterator pathIterator = config.path.iterator();
		StringWriter outwriter = new StringWriter();

		int level = 0;
		Vector navObjects = new Vector();

		while (pathIterator.hasNext()) {
			Object folder = pathIterator.next();

			if (folder instanceof Resolvable) {
				NavObject nav = new NavObject((Resolvable) folder, level, config);

				level++;

				if ("".equals(nav.getName())) {
					if (config.disableFallback == 1) {
						// cut navigation
						break;
					} else if (config.disableFallback == 2) {
						// skip item
						continue;
					}
				}

				navObjects.add(nav);
			}
		}

		BCObject bc = new BCObject(navObjects, config.disableActivepage);

		// create Velocity context, populate
		VelocityContext context = new VelocityContext(createContext(true));

		context.put("ctx", context);
		context.put("bc", bc);

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

			if (config.wrapper != null) {
				mergeTemplate(config.wrapper, context, outwriter);
			}

		} catch (ParseErrorException pee) {
			logger.error("ParseErrorException while rendering. " + pee.getMessage());
			throw new NodeException("ParseErrorException while rendering. " + pee.getMessage(), pee);
		} catch (ResourceNotFoundException rnfe) {
			logger.error("ResourceNotFoundException while rendering. " + rnfe.getMessage());
			throw new NodeException("ResourceNotFoundException while rendering. " + rnfe.getMessage(), rnfe);
		} catch (MethodInvocationException mie) {
			logger.error("MethodInvocationException while rendering. " + mie.getMessage());
			throw new NodeException("MethodInvocationException while rendering. " + mie.getMessage(), mie);
		} catch (IOException ioe) {
			logger.error("IOException while rendering. " + ioe.getMessage());
			throw new NodeException("IOException while rendering. " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			logger.error("Exception while rendering. " + e.getMessage());
			throw new NodeException("Exception while rendering. " + e.getMessage(), e);
		} finally {
			// when edit mode was changed, change it back
			if (editModeChanged) {
				renderType.setEditMode(editMode);
				if (editModeChanged) {
					renderType.setParameter(CMSResolver.ModeResolver.PARAM_OVERWRITE_EDITMODE, null);
				}
			}
		}

		logger.info("End rendering breadcrumb.");
		if (logger.isDebugEnabled()) {
			logger.debug("Breadcrumb: " + outwriter.toString());
		}

		return outwriter.toString();
	}

	/**
	 * Read all needed init parameters using the resolve() method. Populates
	 * instance variables.
	 * @throws NodeException
	 */
	protected ConfigObject getInitParameters() throws NodeException {
		logger.debug("Start reading configuration.");
        
		ConfigObject config = new ConfigObject();
        
		// get startfolder from input parameters
		Object tmpStartfolder = resolve(INPUT_STARTFOLDER);

		if (tmpStartfolder instanceof Resolvable) {
			config.startfolder = (Resolvable) tmpStartfolder;
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
			throw new NodeException("No templates found.");
		}

		// get disable_activepage flag
		// default to false
		config.disableActivepage = ObjectTransformer.getBoolean(ObjectTransformer.getString(resolve(INPUT_DISABLEACTIVEPAGE), ""),
				INPUT_DISABLEACTIVEPAGE_DEFAULT);

		// get tagname_hidden
		// default to "navhidden"
		config.tagnameHidden = ObjectTransformer.getString(resolve(INPUT_TAGNAMEHIDDEN), INPUT_TAGNAMEHIDDEN_DEFAULT);
		if (config.tagnameHidden == null || "".equals(config.tagnameHidden)) {
			config.tagnameHidden = INPUT_TAGNAMEHIDDEN_DEFAULT;
		}

		// get disable_fallback
		String tmpDisableFallback = ObjectTransformer.getString(resolve(INPUT_DISABLEFALLBACK), INPUT_DISABLEFALLBACK_DEFAULT);

		config.disableFallback = 0;
		if ("2".equals(tmpDisableFallback) || "skip".equalsIgnoreCase(tmpDisableFallback)) {
			config.disableFallback = 2;
		} else if ("1".equals(tmpDisableFallback) || "yes".equalsIgnoreCase(tmpDisableFallback) || "true".equalsIgnoreCase(tmpDisableFallback)) {
			config.disableFallback = 1;
		}

		// get tagname_startpage
		config.tagnameStartpage = ObjectTransformer.getString(resolve(INPUT_TAGNAME_STARTPAGE), INPUT_TAGNAME_STARTPAGE_DEFAULT);
		if (config.tagnameStartpage == null || "".equals(config.tagnameStartpage)) {
			config.tagnameStartpage = INPUT_TAGNAME_STARTPAGE_DEFAULT;
		}

		// get disable_hidden
		config.disableHidden = ObjectTransformer.getBoolean(resolve(INPUT_DISABLEHIDDEN), INPUT_DISABLEHIDDEN_DEFAULT);

		// get current page
		Object tmpCurrentPage = resolve(INPUT_PAGE);

		if (tmpCurrentPage instanceof Resolvable) {
			config.currentPage = (Resolvable) tmpCurrentPage;
		} else {
			logger.warn("Current page not found.");
		}

		// get current folder
		if (config.currentPage != null) {
			Object tmpCurrentFolder = config.currentPage.get(NAV_FOLDER);

			if (tmpCurrentFolder instanceof Resolvable) {
				config.currentFolder = (Resolvable) tmpCurrentFolder;
			} else {
				logger.error("Current folder cannot be resolved.");
				throw new NodeException("Current folder cannot be resolved.");
			}
		} else {
			logger.warn("Current folder cannot be resolved.");
			throw new NodeException("Current folder cannot be resolved.");
		}
        
		// get languagecode
		try {
			config.languagecode = ObjectTransformer.getString(PropertyResolver.resolve(config.currentPage, NAV_LANGUAGECODE), "");
		} catch (UnknownPropertyException upe) {
			config.languagecode = "";
		}
        
		// get path from current page to startpage and add objects to hashset
		config.path = new Vector();
		Resolvable pathitem = config.currentFolder;

		while (!config.startfolder.equals(pathitem) && pathitem != null) {
			config.path.add(pathitem);
			Object parent = pathitem.get(NAV_PARENT);

			if (parent instanceof Resolvable) {
				pathitem = (Resolvable) parent;
			} else {
				pathitem = null;
			}
		}
		// add startfolder to path
		if (config.startfolder.equals(pathitem)) {
			config.path.add(pathitem);
		}           
        
		// reverse path vector
		Collections.reverse(config.path);

		// parse old style templates
		config.wrapper = parseTemplate(config.template);

		config.cutNavigation = false;
        
		if (logger.isDebugEnabled()) {
			logger.debug("End reading configuration." + config);
		}

		return config;
	}

	@Override
	public Type getPropertyType() {
		return Type.BREADCRUMB;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}

	@Override
	public Property toProperty() throws NodeException {
		return null;
	}

	/**
	 * The object representing the breadcrumb.
	 * @author alexander
	 */
	public class BCObject {

		/**
		 * Vector holding all navigation objects in path
		 */
		protected Vector navObjects;

		/**
		 * True if active page should not be linked, false otherwise.
		 */
		protected boolean disableActivepage;

		/**
		 * Create new object.
		 * @param navObjects The vector of all folders in path for breadcrumb
		 * @param disableActivepage True if active page should not be linked.
		 */
		public BCObject(Vector navObjects, boolean disableActivepage) {
			this.navObjects = navObjects;
			this.disableActivepage = disableActivepage;
		}

		/**
		 * @return Vector of all folders in path to current page.
		 */
		public Vector getPath() {
			return navObjects;
		}

		/**
		 * @return True if active page should not be linked.
		 */
		public boolean getDisableactivepage() {
			return disableActivepage;
		}
	}

	/**
	 * The object representing a navigation element.
	 * @author alexander
	 */
	public class NavObject {

		/**
		 * The level of this element.
		 */
		protected int level;

		/**
		 * The resolvable object this navobject renders.
		 */
		protected Resolvable object;

		/**
		 * Store configuration
		 */
		protected ConfigObject config;
        
		/**
		 * Constructor of the Navigation Object.
		 * @param object The resolvable object that is rendered.
		 * @param level The level of the object.
		 */
		public NavObject(Resolvable object, int level, ConfigObject config) {
			this.object = object;
			this.level = level;
			this.config = config;
		}

		/**
		 * @return The resolvable object that is rendered.
		 */
		public Resolvable getObject() {
			return new RenderableResolvable(object);
		}

		/**
		 * @return Level of the element (startfolder has level -1).
		 */
		public int getLevel() {
			return level;
		}

		/**
		 * @return Name of the element.
		 */
		public String getName() {

			String nameLanguage = "";

			if (getType() == TYPE_FOLDER && !"".equals(config.languagecode)) {
                
				try {
					nameLanguage = ObjectTransformer.getString(PropertyResolver.resolve(getObject(), NAV_NAME_LANGUAGE + config.languagecode), "");
				} catch (UnknownPropertyException upe) {
					nameLanguage = "";
				}
                
				if ((nameLanguage == null || "".equals(nameLanguage)) && config.disableFallback != 0) {
					return "";
				}
			}

			if (!"".equals(nameLanguage)) {
				return nameLanguage;
			}

			// fallback to default language
			return ObjectTransformer.getString(object.get(NAV_NAME), "");            
		}

		/**
		 * @return Resolvable representing startpage of object
		 */
		public Resolvable getStartpage() {
			// try to get language specific startpage for folder
			// this will fail if the startpage is an external url
			Object obj = getLanguageStartpage();

			if (obj != null) {
				return new RenderableResolvable(obj);
			}
            
			// startpage is an external URL
			try {
				return new RenderableResolvable(PropertyResolver.resolve(object, "object." + config.tagnameStartpage));
			} catch (UnknownPropertyException upe) {
				return null;
			}
		}

		/**
		 * @return Startpage object for folder.
		 */
		protected Resolvable getInternalStartpage() {
			// if not page, try to get startpage
			if (getType() == TYPE_FOLDER) {
                
				Object startpage = null;

				try {
					startpage = PropertyResolver.resolve(object, "object." + config.tagnameStartpage + ".url.target");
				} catch (UnknownPropertyException upe) {
					if (logger.isDebugEnabled()) {
						logger.debug("No startpage found for folder: " + object);
					}
					startpage = null;
				}
                
				// Object startpage = object.get(config.tagnameStartpage);
				if (startpage instanceof Resolvable) {
					return (Resolvable) startpage;
				}
				return null;
			}

			// if page, return itself
			return object;
		}

		/**
		 * Get language-variant of startpage.
		 * @return The language-variant of the startpage.
		 */
		protected Resolvable getLanguageStartpage() {
			Resolvable startpage = getInternalStartpage();

			if (startpage == null) {
				return null;
			}

			if (StringUtils.isEmpty(config.languagecode)) {
				return startpage;
			}

			Object page = null;

			try {
				page = PropertyResolver.resolve(startpage, NAV_LANGUAGES + config.languagecode);
			} catch (UnknownPropertyException upe) {
				page = null;
			}

			if (page instanceof Resolvable) {
				return (Resolvable) page;
			}

			return startpage;
		}

		/**
		 * @return Type of element (e.g. 10002 == folder).
		 */
		protected int getType() {
			return ObjectTransformer.getInt(object.get(NAV_OBJECTTYPE), 0);
		}

		/**
		 * @return True if element is hidden from navigation, false otherwise.
		 */
		public boolean getHidden() {
			try {
				return ObjectTransformer.getBoolean(PropertyResolver.resolve(getObject(), "object." + config.tagnameHidden), false);
			} catch (UnknownPropertyException upe) {
				return false;
			}
		}

		/**
		 * Check to see if this element is the current page.
		 * @return If this element is the current page.
		 */
		protected boolean isCurrentpage() {
			if (config.currentPage != null) {
				return config.currentPage.equals(getObject());
			} else {
				return false;
			}
		}
	}
    
	public class ConfigObject {

		/**
		 * Startfolder of the navigation.
		 */
		protected Resolvable startfolder;

		/**
		 * Template to use to render navigation.
		 */
		protected String template;

		/**
		 * If current page should be linked
		 */
		protected boolean disableActivepage;

		/**
		 * Code of language to use in navigation
		 */
		protected String languagecode;

		/**
		 * Property which stores the hidden attribute name.
		 */
		protected String tagnameHidden;

		/**
		 * If navigation should display hidden elements.
		 */
		protected boolean disableHidden;

		/**
		 * If language fallback is enabled
		 */
		protected int disableFallback;

		/**
		 * The current page for which the navigation is created.
		 */
		protected Resolvable currentPage;

		/**
		 * The current folder for which the navigation is created (parent of
		 * currentPage).
		 */
		protected Resolvable currentFolder;

		/**
		 * Set containing all resolvables leading from the current page to the
		 * startpage.
		 */
		protected List path;

		/**
		 * Store all templates read from XML input.
		 */
		protected SerializableVelocityTemplateWrapper wrapper;

		/**
		 * Name of tag for startpage of folder
		 */
		protected String tagnameStartpage;

		/**
		 * True if navigation should be cut (ie no more items should be rendered)
		 */
		protected boolean cutNavigation;

		public ConfigObject() {
			startfolder = null;
			template = null;
			disableActivepage = INPUT_DISABLEACTIVEPAGE_DEFAULT;
			languagecode = null;
			tagnameHidden = null;
			disableHidden = INPUT_DISABLEHIDDEN_DEFAULT;
			disableFallback = 0;
			currentPage = null;
			currentFolder = null;
			path = null;
			wrapper = null;
			tagnameStartpage = null;
			cutNavigation = false;
		}
        
		/**
		 * Return all configuration parameters as string.
		 */
		public String toString() {
			return " startfolder: " + startfolder + ". template: " + template + ". disableActivepage: " + disableActivepage + ". languagecode: " + languagecode
					+ ". tagnameHidden: " + tagnameHidden + ". disableHidden: " + disableHidden + ". disableFallback: " + disableFallback + ". currentPage: "
					+ currentPage + ". currentFolder: " + currentFolder + ". path: " + path + ". tagnameStartpage: " + tagnameStartpage;
		}

	}
}
