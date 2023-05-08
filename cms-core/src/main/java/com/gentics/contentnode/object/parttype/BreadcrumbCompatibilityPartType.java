/*
 * @author alexander
 * @date 14.03.2007
 * @version $Id: BreadcrumbCompatibilityPartType.java,v 1.13 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * This parttype implements the compatibility breadcrumb navigation.
 */

public class BreadcrumbCompatibilityPartType extends AbstractVelocityCompatibilityPartType {

	/**
	 * Static logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(BreadcrumbCompatibilityPartType.class);

	/**
	 * Static type code for folders.
	 */
	protected static final int TYPE_FOLDER = 10002;
    
	/**
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_STARTFOLDER = "startfolder";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_STARTFOLDER_DEFAULT = "node.folder";    
    
	/**
	 * Name of input parameter for template
	 */
	public static final String INPUT_TEMPLATE = "templates";
    
	/**
	 * Name of input parameter for tagname_hidden
	 */
	protected static final String INPUT_TAGNAMEHIDDEN = "tagname_hidden";

	/**
	 * Default value for tagname_hidden
	 */
	protected static final String INPUT_TAGNAMEHIDDEN_DEFAULT = "navhidden";
    
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
	 * Name of input parameter for startpage (of folder) compatibility mode
	 */
	protected static final String INPUT_TAGNAME_STARTPAGE = "tagname_startpage2";

	/**
	 * Default value for startpage of folder
	 */
	protected static final String INPUT_TAGNAME_STARTPAGE_DEFAULT = "object.startpage";

	/**
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_LANGUAGECODE = "languagecode";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_LANGUAGECODE_DEFAULT = "";

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
	 * Name of attribute for translated name
	 */
	protected static final String NAV_NAME_LANGUAGE = "object.name_";
    
	/**
	 * Name of attribute for name
	 */
	protected static final String NAV_NAME = "name";

	/**
	 * Name of attribute for URL
	 */
	protected static final String NAV_URL = "url";

	/**
	 * Name of attribute for ID
	 */
	protected static final String NAV_ID = "id";
    
	/**
	 * Name of attribute for objecttype
	 */
	protected static final String NAV_OBJECTTYPE = "ttype";
    
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
        
		// compatibility breadcrumb starts counting at 1
		int level = 1;
        
		while (pathIterator.hasNext()) {
			Object folder = pathIterator.next();

			if (folder instanceof Resolvable) {
				// create the corresponding nav object
				NavObject nav = new NavObject(new RenderableResolvable((Resolvable) folder), level, config);

				// check for hidden folder
				if (nav.getHidden()) {
					continue;
				}

				if ("".equals(nav.getName())) {
					if (config.disableFallback == 1) {
						// cut navigation
						logger.debug("Disable fallback == 1, cutting navigation.");
						break;
					} else if (config.disableFallback == 2) {
						// skip item
						logger.debug("Disable fallback == 2, skipping item.");
						continue;
					}
				}
				renderObject(outwriter, nav, level, config);
				level++;
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
		config.disableActivepage = ObjectTransformer.getBoolean(resolve(INPUT_DISABLEACTIVEPAGE), INPUT_DISABLEACTIVEPAGE_DEFAULT);

		// get tagname_hidden
		// default to "navhidden"
		config.tagnameHidden = ObjectTransformer.getString(resolve(INPUT_TAGNAMEHIDDEN), INPUT_TAGNAMEHIDDEN_DEFAULT);
		if (config.tagnameHidden == null || "".equals(config.tagnameHidden)) {
			config.tagnameHidden = INPUT_TAGNAMEHIDDEN_DEFAULT;
		}

		// get languagecode
		// default to ""
		config.languagecode = ObjectTransformer.getString(resolve(INPUT_LANGUAGECODE), INPUT_LANGUAGECODE_DEFAULT);
		if (config.languagecode == null || "".equals(config.languagecode)) {
			config.languagecode = INPUT_LANGUAGECODE_DEFAULT;
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
			// logger.error("Current page not found.");
			// throw new NodeException("Current page not found.");
			logger.warn("Current page not found.");
		}

		// get current folder
		if (config.currentPage != null) {
			Object tmpCurrentFolder = config.currentPage.get(NAV_FOLDER);

			if (tmpCurrentFolder instanceof Resolvable) {
				config.currentFolder = (Resolvable) tmpCurrentFolder;
			} else {
				logger.error("Current folder could not be resolved.");
				throw new NodeException("Current folder could not be resolved.");
			}
		} else {
			logger.warn("Current folder could not be resolved.");
		}

		// get path from current page to startpage and add objects to vector
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
		config.templates = parseTemplates(config.template);

		if (logger.isDebugEnabled()) {
			logger.debug("End reading configuration." + config);
		}
        
		return config;
	}

	/**
	 * Parse old-style XML templates and transform to Velocity templates.
	 * @param inputTemplate All templates as one XML document.
	 * @return A HashMap containing all transformed templates, hashed by level
	 *         and type.
	 * @throws NodeException If an exception occured during XML parsing.
	 */
	protected HashMap parseTemplates(String inputTemplate) throws NodeException {

		logger.debug("Start parsing templates.");
        
		// add surrounding <templates></templates> block to make valid XML
		inputTemplate = "<templates>" + inputTemplate + "</templates>";

		// try to get parsed templates from portal cache
		Object tmpTemplates = getCachedObject(inputTemplate);

		if (tmpTemplates instanceof HashMap) {
			logger.debug("Templates found in cache.");
			return (HashMap) tmpTemplates;
		}
        
		logger.debug("Templates not found in cache, start parsing from XML.");

		// templates not cached, create new
		HashMap templates = new HashMap();

		try {
			// Read XML input string into a DOM document
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(new InputSource(new StringReader(inputTemplate)));

			// loop over all templates
			NodeList nodeTemplates = document.getElementsByTagName("template");

			for (int i = 0; i < nodeTemplates.getLength(); i++) {

				// read a template
				Node node = nodeTemplates.item(i);

				// get template level
				NamedNodeMap nodeAttributes = node.getAttributes();
				Node nodeLevel = nodeAttributes.getNamedItem("level");
				Integer templateLevel = Integer.valueOf(nodeLevel.getNodeValue());

				// read subtemplates
				NodeList nodeSubTemplates = node.getChildNodes();
				HashMap subTemplates = new HashMap();

				for (int j = 0; j < nodeSubTemplates.getLength(); j++) {

					// read template
					Node nodeSubTemplate = nodeSubTemplates.item(j);
                    
					// check to see if node is an element
					if (nodeSubTemplate.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
                    
					String elementName = nodeSubTemplate.getNodeName();
					String elementValue = getTemplateFromNode(nodeSubTemplate);

					// put into hashmap to store in cache
					Template tmp = getTemplateFromString(convertVelocity(elementValue));

					subTemplates.put(elementName, tmp);
				}
				templates.put(templateLevel, subTemplates);
			}

			// put parsed templates into cache
			putObjectIntoCache(inputTemplate, templates);

			logger.debug("End parsing templates.");
            
			// return templates
			return templates;

		} catch (ParserConfigurationException pce) {
			logger.error("ParserConfigurationException while parsing templates. " + pce.getMessage());
			throw new NodeException(pce.getMessage(), pce);
		} catch (IOException ioe) {
			logger.error("IOException while parsing templates. " + ioe.getMessage());
			throw new NodeException(ioe.getMessage(), ioe);
		} catch (SAXException saxe) {
			logger.error("SAXException while parsing templates. " + saxe.getMessage());
			throw new NodeException(saxe.getMessage(), saxe);
		} catch (Exception e) {
			logger.error("Exception while parsing templates. " + e.getMessage());
			throw new NodeException(e.getMessage(), e);
		}
	}

	/**
	 * Replace all <nav xxx> old-style tags by Velocity $nav.xxx statements.
	 * @param input The old-style template.
	 * @return A Velocity-fied template.
	 */
	protected String convertVelocity(String input) {
		return input.replaceAll("<folder.(\\w+)>", "\\${folder.$1}");
	}

	/**
	 * Render a resovable object.
	 * @param outwriter StringWriter to append output to
	 * @param nav The navigation object that should be rendered.
	 * @param level The current level of the object (startpage has level -1, as
	 *        only sub-elements of startpage are shown).
	 * @throws NodeException
	 */
	protected void renderObject(StringWriter outwriter, NavObject nav, int level, 
			ConfigObject config) throws NodeException {

		if (logger.isDebugEnabled()) {
			logger.debug("Start rendering object: " + nav.toString());
		}

		// create Velocity context, populate
		VelocityContext context = new VelocityContext(createContext(true));

		context.put("folder", nav);

		// fetch template for object
		String type = "";

		if ((!"".equals(nav.getLinkstartpage()) && !config.disableActivepage)
				|| (config.disableActivepage && !"".equals(nav.getLinkstartpage())
				&& (config.currentPage != null && !config.currentPage.equals(nav.getLanguageStartpage())))) {
			type = "linked";
		} else {
			type = "notlinked";
		}

		try {
			Template template = getTemplate(type, level, true, config);

			if (template != null) {
				logger.debug("Merging template.");
				template.merge(context, outwriter);
			}

		} catch (ParseErrorException pee) {
			logger.error("ParseErrorException while merging template. " + pee.getMessage());
			throw new NodeException("ParseErrorException while merging template. " + pee.getMessage(), pee);
		} catch (ResourceNotFoundException rnfe) {
			logger.error("ResourceNotFoundException while merging template. " + rnfe.getMessage());
			throw new NodeException("ResourceNotFoundException while merging template. " + rnfe.getMessage(), rnfe);
		} catch (MethodInvocationException mie) {
			logger.error("MethodInvocationException while merging template. " + mie.getMessage());
			throw new NodeException("MethodInvocationException while merging template. " + mie.getMessage(), mie);
		} catch (IOException ioe) {
			logger.error("IOException while merging template. " + ioe.getMessage());
			throw new NodeException("IOException while merging template. " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			logger.error("Exception while merging template. " + e.getMessage());
			throw new NodeException("Exception while merging template. " + e.getMessage(), e);
		}
	}

	/**
	 * Get template for specified object and level.
	 * @param type Type of template to return (selected, unselectedlinked,
	 *        unselectednotlinked)
	 * @param level The template level.
	 * @param fallback True if level should gradually be decremented until a
	 *        template is found, false if not.
	 * @return The already to Velocity converted template.
	 */
	protected Template getTemplate(String type, int level, boolean fallback, 
			ConfigObject config) {
        
		if (logger.isDebugEnabled()) {
			logger.debug("Get template from map. type = " + type + ". level = " + level + ". fallback = " + fallback);
		}
        
		// check level. exit if level < 0 to prevent infinite loop
		if (level < 0) {
			logger.debug("Level < 0, aborting search for template.");
			return null;
		}

		// try to get right template level
		Object tmpSubTemplates = config.templates.get(new Integer(level));

		if (tmpSubTemplates instanceof HashMap) {
			HashMap subTemplates = (HashMap) tmpSubTemplates;

			// get template
			Object template = subTemplates.get(type);

			if (template instanceof Template) {
				logger.debug("Template found.");
				return (Template) template;
			}

			// still no template found, step up one level if fallback enabled
			if (fallback) {
				logger.debug("No template found, fallback to upper level.");
				return getTemplate(type, level - 1, fallback, config);
			} else {
				logger.debug("No template found, fallback disabled.");
				return null;
			}

			// no template found for this level, try to step up
		} else {
			if (fallback) {
				logger.debug("No template found, fallback to upper level.");
				return getTemplate(type, level - 1, fallback, config);
			} else {
				logger.debug("No template found, fallback disabled.");
				return null;
			}
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
			return object;
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
					nameLanguage = ObjectTransformer.getString(PropertyResolver.resolve(object, NAV_NAME_LANGUAGE + config.languagecode), "");
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
		 * @return URL of element.
		 */
		public String getLinkstartpage() {
			// try to get the language specific startpage
			Resolvable startpage = getLanguageStartpage();

			if (startpage != null) {
				return ObjectTransformer.getString(startpage.get(NAV_URL), "");
			} else {
				// no startpage found, this means that object.startpage does not
				// point to a page
				// in that case, simply render object.startpage.url
				try {
					return ObjectTransformer.getString(PropertyResolver.resolve(object, config.tagnameStartpage + ".url"), "");
				} catch (UnknownPropertyException upe) {
					return "";
				}
			}
		}

		/**
		 * @return Resolvable representing startpage of object
		 */
		protected Resolvable getStartpage() {
			// if not page, try to get startpage
			if (getType() == TYPE_FOLDER) {
                
				Object startpage = null;

				try {
					startpage = PropertyResolver.resolve(object, config.tagnameStartpage + ".url.target");
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

			Resolvable startpage = getStartpage();

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
		 * @return The ID of the element.
		 */
		public Object getId() {
			//TODO use obj transformer and return integer?
			return object.get(NAV_ID);
		}

		/**
		 * @return Type of element (e.g. 10002 == folder).
		 */
		public int getType() {
			return ObjectTransformer.getInt(object.get(NAV_OBJECTTYPE), 0);
		}

		/**
		 * @return True if element is hidden from navigation, false otherwise.
		 */
		public boolean getHidden() {
			try {
				return ObjectTransformer.getBoolean(PropertyResolver.resolve(object, "object." + config.tagnameHidden), false);
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
		public Resolvable startfolder;

		/**
		 * Template to use to render navigation.
		 */
		public String template;

		/**
		 * If current page should be linked
		 */
		public boolean disableActivepage;

		/**
		 * Code of language to use in navigation
		 */
		public String languagecode;

		/**
		 * Property which stores the hidden attribute name.
		 */
		public String tagnameHidden;

		/**
		 * If navigation should display hidden elements.
		 */
		public boolean disableHidden;

		/**
		 * If language fallback is enabled
		 */
		public int disableFallback;

		/**
		 * The current page for which the navigation is created.
		 */
		public Resolvable currentPage;

		/**
		 * The current folder for which the navigation is created (parent of
		 * currentPage).
		 */
		public Resolvable currentFolder;

		/**
		 * Set containing all resolvables leading from the current page to the
		 * startpage.
		 */
		public List path;

		/**
		 * Store all templates read from XML input.
		 */
		public HashMap templates;

		/**
		 * Name of tag for startpage of folder
		 */
		public String tagnameStartpage;

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
			templates = null;
			tagnameStartpage = null;          
		}
        
		/**
		 * Return all configuration parameters as string.
		 */
		public String toString() {
			return " startfolder: " + startfolder + ". template: " + template + ". disableActivepage: " + disableActivepage + ". languagecode: " + languagecode
					+ ". tagnameHidden: " + tagnameHidden + ". disableHidden: " + disableHidden + ". disableFallback: " + disableFallback + ". currentPage: "
					+ currentPage + ". currentFolder: " + currentFolder + ". path: " + path + ". templates: " + templates + ". tagnameStartpage: " + tagnameStartpage;
		}
	}
}
