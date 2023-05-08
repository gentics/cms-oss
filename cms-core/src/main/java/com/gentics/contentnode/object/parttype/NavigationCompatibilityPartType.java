/*
 * @author alexander
 * @date 14.03.2007
 * @version $Id: NavigationCompatibilityPartType.java,v 1.23 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.cr.DummyTagmapEntry;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.etc.MiscUtils;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * This parttype implements the navigation.
 */

public class NavigationCompatibilityPartType extends AbstractVelocityCompatibilityPartType {

	/**
	 * Static logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(NavigationCompatibilityPartType.class);

	/**
	 * Static type code for folders.
	 */
	protected static final int TYPE_FOLDER = 10002;

	/**
	 * Static type code of pages
	 */
	protected static final int TYPE_PAGE = 10007;

	/**
	 * Static type code for folders.
	 */
	protected static final int TYPE_FILE = 10008;

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
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_SORTBY = "sortby";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_SORTBY_DEFAULT = "name";

	/**
	 * Name of input parameter for sitemap
	 */
	protected static final String INPUT_SITEMAP = "disable_flapping";

	/**
	 * Default value for sitemap
	 */
	protected static final boolean INPUT_SITEMAP_DEFAULT = false;

	/**
	 * Name of input parameter for disable_fallback
	 */
	protected static final String INPUT_DISABLEFALLBACK = "disable_fallback";

	/**
	 * Default value for disable_fallback
	 */
	protected static final boolean INPUT_DISABLEFALLBACK_DEFAULT = false;

	/**
	 * Name of input parameter for enable_activepath
	 */
	protected static final String INPUT_ACTIVEPATH = "enable_activepath";

	/**
	 * Default value for tagname_hidden
	 */
	protected static final boolean INPUT_ACTIVEPATH_DEFAULT = false;

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
	 * Name of input parameter for startfolder
	 */
	protected static final String INPUT_LANGUAGECODE = "languagecode";

	/**
	 * Default value for startfolder
	 */
	protected static final String INPUT_LANGUAGECODE_DEFAULT = "";

	/**
	 * Name of input parameter for startpage (of folder)
	 */
	protected static final String INPUT_TAGNAME_STARTPAGE = "tagname_startpage";

	/**
	 * Default value for startpage of folder
	 */
	protected static final String INPUT_TAGNAME_STARTPAGE_DEFAULT = "object.startpage";

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
	protected static final boolean INPUT_DISABLEHIDDEN_DEFAULT = false;

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
	 * Name of attribute for translated name
	 */
	protected static final String NAV_NAME_LANGUAGE = "object.name_";

	/**
	 * Name of attribute for translated name
	 */
	protected static final String NAV_LANGUAGES = "languageset.pages.";

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
	 * Name of attribute for edit timestamp
	 */
	protected static final String NAV_EDITTIMESTAMP = "edittimestamp";

	/**
	 * Name of attribute for objecttype
	 */
	protected static final String NAV_OBJECTTYPE = "ttype";

	/**
	 * Name of attribute for mother id
	 */
	protected static final String NAV_MOTHERID = "mother";

	/**
	 * Name of attribute for mother id, if the object is not a folder itself
	 */
	protected static final String NAV_MOTHERID_NONFOLDER = "folder_id";

	/**
	 * Name of attribute for language code of page
	 */
	protected static final String NAV_LANGUAGECODE = "language.code";

	/**
	 * Tagmap cache for basic types Page, Folder and File Structure: keys are
	 * the objecttypes (as instances of Integer), values are maps of mapname
	 * (String) -&gt; {@link CnMapPublisher#TagmapEntry}
	 */
	protected Map tagmapCache = new HashMap(3);

	/**
	 * Render the navigation. Gets all needed input parameters and starts
	 * rendering at startpage. Parse old-style templates from XML.
	 * @throws NodeException
	 */
	public String render() throws NodeException {
		logger.info("Start rendering navigation.");

		buildTagmapCache();

		ConfigObject config = getInitParameters();
		String navigation = renderObject(new RenderableResolvable(config.startfolder), -1, config);

		logger.info("End rendering navigation.");
		if (logger.isDebugEnabled()) {
			logger.debug("Navigation: " + navigation);
		}

		// we don't need that cache anymore
		tagmapCache.clear();

		return navigation;
	}

	/**
	 * cache the tagmap to a hashmap
	 */
	protected void buildTagmapCache() {
		Transaction t;

		// build basic cache structure
		tagmapCache.put(new Integer(Page.TYPE_PAGE), new HashMap());
		tagmapCache.put(new Integer(Folder.TYPE_FOLDER), new HashMap());
		tagmapCache.put(new Integer(File.TYPE_FILE), new HashMap());

		try {
			t = TransactionManager.getCurrentTransaction();

			PreparedStatement stmt = null;
			ResultSet rs = null;

			try {
				com.gentics.contentnode.object.Node node = getNodeOfRenderedObject();

				stmt = t.prepareStatement(
						"SELECT tagname, mapname, object, objtype, attributetype FROM tagmap LEFT JOIN node ON tagmap.contentrepository_id = node.contentrepository_id WHERE object IN ("
								+ Page.TYPE_PAGE + ", " + Folder.TYPE_FOLDER + ", " + File.TYPE_FILE + ") AND node.id = ? ORDER BY object ASC");
				stmt.setObject(1, node != null ? node.getId() : "0");
				rs = stmt.executeQuery();

				while (rs.next()) {
					((HashMap) tagmapCache.get(rs.getObject("object"))).put(rs.getObject("mapname"),
							new DummyTagmapEntry(rs.getInt("object"), rs.getString("tagname"), rs.getString("mapname"), rs.getInt("attributetype"), rs.getInt("objType")));
				}
			} catch (Exception e) {
				logger.error("could not load tagmap entries", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}
		} catch (TransactionException e1) {
			logger.error("unable to get current transaction", e1);
		}
	}

	/**
	 * Read all needed init parameters using the resolve() method. Populates
	 * instance variables.
	 * @throws NodeException
	 */
	protected ConfigObject getInitParameters() throws NodeException {

		logger.debug("Start reading configuration.");

		// create new config object to store configuration parameters
		// can't use instance variables since same instance might render
		// different navigations
		ConfigObject config = new ConfigObject();

		// get startfolder from input parameters
		Object tmpStartfolder = resolve(INPUT_STARTFOLDER);

		if (tmpStartfolder instanceof Resolvable) {
			config.startfolder = (Resolvable) tmpStartfolder;
		}

		// if no startfolder specified, use default node.folder
		// if (config.startfolder == null) {
		// config.startfolder = (Resolvable) resolve(INPUT_STARTFOLDER_DEFAULT);
		// }

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
		config.sitemap = ObjectTransformer.getBoolean(resolve(INPUT_SITEMAP), INPUT_SITEMAP_DEFAULT);

		// get enable_activepath flag
		// default to false
		config.activepath = ObjectTransformer.getBoolean(resolve(INPUT_ACTIVEPATH), INPUT_ACTIVEPATH_DEFAULT);

		// get tagname_hidden
		// default to "navhidden"
		config.tagnameHidden = ObjectTransformer.getString(resolve(INPUT_TAGNAMEHIDDEN), INPUT_TAGNAMEHIDDEN_DEFAULT);
		if (config.tagnameHidden == null || "".equals(config.tagnameHidden)) {
			config.tagnameHidden = INPUT_TAGNAMEHIDDEN_DEFAULT;
		}

		// get disable_fallback
		config.disableFallback = ObjectTransformer.getBoolean(resolve(INPUT_DISABLEFALLBACK), INPUT_DISABLEFALLBACK_DEFAULT);

		// get tagname_startpage
		config.tagnameStartpage = ObjectTransformer.getString(resolve(INPUT_TAGNAME_STARTPAGE), INPUT_TAGNAME_STARTPAGE_DEFAULT);
		if (config.tagnameStartpage == null || "".equals(config.tagnameStartpage)) {
			config.tagnameStartpage = INPUT_TAGNAME_STARTPAGE_DEFAULT;
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

		// get languagecode
		// default to language code of current page
		config.languagecode = ObjectTransformer.getString(resolve(INPUT_LANGUAGECODE), INPUT_LANGUAGECODE_DEFAULT);
		if (config.languagecode == null || "".equals(config.languagecode)) {
			try {
				config.languagecode = ObjectTransformer.getString(PropertyResolver.resolve(config.currentPage, NAV_LANGUAGECODE), INPUT_LANGUAGECODE_DEFAULT);
			} catch (UnknownPropertyException upe) {
				logger.error("Language of current page could not be resolved.");
			}
		}

		// get path from current page to startpage and add objects to hashset
		config.path = new HashSet();
		Resolvable pathitem = config.currentPage;

		while (pathitem != null) {
			config.path.add(pathitem);
			Object parent = pathitem.get(NAV_PARENT);

			if (logger.isDebugEnabled()) {
				logger.debug("resolving path to startpage: currentPage {" + config.currentPage + "} pathitem {" + pathitem + "} parent {" + parent + "}");
			}
			if (parent instanceof Resolvable) {
				pathitem = (Resolvable) parent;
			} else {
				parent = pathitem.get(NAV_FOLDER);
				// folders can resolve themselves (folder.folder) - so check if
				// the
				// resolved item is equal to the old item to avoid endless
				// loops.
				if (parent instanceof Resolvable && !parent.equals(pathitem)) {
					pathitem = (Resolvable) parent;
				} else {
					pathitem = null;
				}
			}
		}

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
			int maxLevel = 0;

			for (int i = 0; i < nodeTemplates.getLength(); i++) {

				// read a template
				Node node = nodeTemplates.item(i);

				// get template level
				NamedNodeMap nodeAttributes = node.getAttributes();
				Node nodeLevel = nodeAttributes.getNamedItem("level");
				Integer templateLevel = Integer.valueOf(nodeLevel.getNodeValue());

				if (templateLevel.intValue() > maxLevel) {
					maxLevel = templateLevel.intValue();
				}

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

					// when the subtemplate is the "childs" template, but does
					// not contain <nav childs>, we replace it by the fake
					// childs template (same behaviour as in 3.6, fixes part of
					// RT#21705)
					if ("childs".equals(elementName) && elementValue.indexOf("<nav childs>") < 0) {
						Template tmp = getTemplateFromString("$nav.childs");

						subTemplates.put(elementName, tmp);
					} else {
						// put into hashmap to store in cache
						Template tmp = getTemplateFromString(convertVelocity(elementValue));

						subTemplates.put(elementName, tmp);
					}
				}

				// Append template for child elements. (RT #9581)
				// Either use a <childs> template if it exists, otherwise
				// use fake template since navigation relies on $nav.childs
				// to do the recursion.
				String templateName = "childs";

				if (!subTemplates.containsKey(templateName)) {
					Template tmp = getTemplateFromString("$nav.childs");

					subTemplates.put(templateName, tmp);
				}

				templates.put(templateLevel, subTemplates);

			}

			// add "childs" template for level -1 (root level)
			// needed since we do all recursion using $nav.childs, we need
			// an entry point
			String templateName = "childs";
			Template tmp = getTemplateFromString("$nav.childs");
			HashMap rootTemplate = new HashMap();

			rootTemplate.put(templateName, tmp);
			templates.put(new Integer(-1), rootTemplate);

			// put parsed templates into cache
			putObjectIntoCache(inputTemplate, templates);

			logger.debug("End parsing templates.");

			// return templates
			return templates;

		} catch (ParserConfigurationException pce) {
			logger.error("ParserConfigurationException while parsing templates. " + pce.getMessage());
			throw new NodeException("ParserConfigurationException while parsing templates. " + pce.getMessage(), pce);
		} catch (IOException ioe) {
			logger.error("IOException while parsing templates. " + ioe.getMessage());
			throw new NodeException("IOException while parsing templates. " + ioe.getMessage(), ioe);
		} catch (SAXException saxe) {
			logger.error("SAXException while parsing templates. " + saxe.getMessage());
			throw new NodeException("SAXException while parsing templates. " + saxe.getMessage(), saxe);
		} catch (Exception e) {
			logger.error("Exception while parsing templates. " + e.getMessage());
			throw new NodeException("Exception while parsing templates. " + e.getMessage(), e);
		}
	}

	/**
	 * Replace all <nav xxx> old-style tags by Velocity $nav.xxx statements.
	 * @param input The old-style template.
	 * @return A Velocity-fied template.
	 */
	protected String convertVelocity(String input) {
		return input.replaceAll("<nav (\\w+)>", "\\${nav.$1}");
	}

	/**
	 * Render a resovable object.
	 * @param object The resovable object that should be rendered.
	 * @param level The current level of the object (startpage has level -1, as
	 *        only sub-elements of startpage are shown).
	 * @return The rendered string containing this and all sub-elements.
	 * @throws NodeException
	 */
	protected String renderObject(Resolvable object, int level, ConfigObject config) throws NodeException {

		if (logger.isDebugEnabled()) {
			logger.debug("Start rendering object: " + object.toString());
		}

		// create the corresponding nav object
		NavObject nav = new NavObject(object, level, config);

		// create Velocity context, populate
		VelocityContext context = new VelocityContext(createContext(true));

		context.put("nav", nav);

		// TODO initial size
		StringWriter outwriter = new StringWriter();

		// fetch template for object
		String type = "";

		if (nav.isCurrentpage() || (config.activepath && nav.isInpath())) {
			type = "selected";
		} else if (!"".equals(nav.getUrl())) {
			type = "unselectedlinked";
		} else {
			type = "unselectednotlinked";
		}

		String objecttype = "";

		if (nav.getType() == TYPE_FOLDER) {
			objecttype = "_folder";
		} else if (nav.getType() == TYPE_FILE) {
			objecttype = "_file";
		}

		try {
			Template template = getTemplate(type, objecttype, level, true, config);

			if (template != null) {
				logger.debug("Merging main template.");
				template.merge(context, outwriter);
			} else {
				logger.debug("No template found, skipping.");
			}

			// always render nav childs template if element is open to be
			// compatible to php
			if (nav.isOpen()) {
				Template childTemplate = getTemplate("childs", "", level, true, config);

				if (childTemplate != null) {
					logger.debug("Merging child template.");
					childTemplate.merge(context, outwriter);
				} else {
					logger.debug("No child template found, skipping.");
				}
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

		logger.debug("End rendering object.");

		return outwriter.toString();
	}

	/**
	 * Get template for specified object and level.
	 * @param type Type of template to return (selected, unselectedlinked,
	 *        unselectednotlinked)
	 * @param objecttype Type of object to allow specific templates for folders
	 *        and files.
	 * @param level The template level.
	 * @param fallback True if level should gradually be decremented until a
	 *        template is found, false if not.
	 * @return The already to Velocity converted template.
	 */
	protected Template getTemplate(String type, String objecttype, int level, boolean fallback,
			ConfigObject config) {

		if (logger.isDebugEnabled()) {
			logger.debug("Get template from map. type = " + type + ". objecttype = " + objecttype + ". level = " + level + ". fallback = " + fallback);
		}

		// check level. exit if level < -1 to prevent infinite loop
		if (level < -1) {
			logger.debug("Level < -1, aborting search for template.");
			return null;
		}

		// try to get right template level
		Object tmpSubTemplates = config.templates.get(new Integer(level));

		if (tmpSubTemplates instanceof HashMap) {

			HashMap subTemplates = (HashMap) tmpSubTemplates;

			// get specific template
			Object template = subTemplates.get(type + objecttype);

			if (template instanceof Template) {
				logger.debug("Specific template found.");
				return (Template) template;
			}

			// no specific template found, get generic template
			template = subTemplates.get(type);
			if (template instanceof Template) {
				logger.debug("Generic template found.");
				return (Template) template;
			}

			logger.debug("No template found, but template node for level exists. Returning null.");
			return null;

			// no template found for this level, try to step up
		} else {
			if (fallback) {
				logger.debug("No template found, fallback to upper level.");
				return getTemplate(type, objecttype, level - 1, fallback, config);
			} else {
				logger.debug("No template found, fallback disabled.");
				return null;
			}
		}
	}

	/**
	 * Get the Node of the rendered root object or null
	 * @return Node or null
	 * @throws NodeException
	 */
	protected com.gentics.contentnode.object.Node getNodeOfRenderedObject() throws NodeException {
		com.gentics.contentnode.object.Node node = null;

		Transaction t = TransactionManager.getCurrentTransaction();
		StackResolvable renderedRootObject = t.getRenderType().getRenderedRootObject();

		if (renderedRootObject instanceof Page) {
			node = ((Page) renderedRootObject).getFolder().getNode();
		} else if (renderedRootObject instanceof Folder) {
			node = ((Folder) renderedRootObject).getNode();
		} else if (renderedRootObject instanceof ContentFile) {
			node = ((ContentFile) renderedRootObject).getFolder().getNode();
		}

		return node;
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
		 * The name of the attribute by which children of this element are
		 * sorted.
		 */
		protected String folderSortBy;

		/**
		 * The order by which children of this element are sorted.
		 */
		protected int folderSortOrder;

		/**
		 * The cached result of the hasItems() method.
		 */
		protected boolean hasItems;

		/**
		 * If hasItems contains a valid result.
		 */
		protected boolean hasItemsCache;

		/**
		 * The cached result of the isOpen() method.
		 */
		protected boolean isOpen;

		/**
		 * If isOpen contains a valid result.
		 */
		protected boolean isOpenCache;

		/**
		 * Store configuration parameters
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

			this.isOpen = false;
			this.isOpenCache = false;

			this.folderSortBy = config.sortBy;
			this.folderSortOrder = config.sortOrder;

			this.hasItems = false;
			this.hasItemsCache = false;
		}

		/**
		 * Check if this object has a specific sorting set as a property. Decode
		 * sorting as sortorder;sortby. If it exists, set as sortorder for this
		 * folder.
		 */
		protected void checkSorting() {
			Object tmpSorting = resolveTagmapProperty(getObject(), config.tagnameSort);
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
					if (ObjectTransformer.isEmpty(folderSortBy)) {
						folderSortBy = config.sortBy;
					}
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug(
						"sorting from tagname is {" + sorting + "}, setting folderSortBy to {" + folderSortBy + "} and folderSortOrder to {" + folderSortOrder + "}");
			}
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

				if ((nameLanguage == null || "".equals(nameLanguage)) && config.disableFallback) {
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
		public String getUrl() {
			// try to get the language specific startpage
			Resolvable startpage = getLanguageStartpage();

			if (startpage != null) {
				// when the page is offline, simply render empty string
				if (!ObjectTransformer.getBoolean(startpage.get("online"), false)) {
					logger.debug("    startpage not online");
					return "";
				}
				if (logger.isDebugEnabled()) {
					logger.debug("    returning url: {" + ObjectTransformer.getString(startpage.get(NAV_URL), "") + "}");
				}
				return ObjectTransformer.getString(startpage.get(NAV_URL), "");
			} else {
				// no startpage found, this means that object.startpage does not
				// point to a page
				// in that case, simply render object.startpage.url
				try {
					if (logger.isDebugEnabled()) {
						logger.debug(
								"    resolving {" + config.tagnameStartpage + "}: {"
								+ ObjectTransformer.getString(PropertyResolver.resolve(object, config.tagnameStartpage + ".url"), "") + "}");
					}
					// first try to resolve the URL ...
					String url = ObjectTransformer.getString(PropertyResolver.resolve(object, config.tagnameStartpage + ".url"), null);

					if (url != null) {
						return url;
					}
                    
					// now try to resolve through the tagmap
					Object obj = resolveTagmapProperty(object, config.tagnameStartpage);

					if (obj instanceof Resolvable) {
						// we resolved the startpage (i guess ?)
						obj = ((Resolvable) obj).get(NAV_URL);
					}
					return ObjectTransformer.getString(obj, "");
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
			if (getType() != TYPE_PAGE) {

				Object startpage = null;

				try {
					if (logger.isDebugEnabled()) {
						logger.debug("resolving {" + config.tagnameStartpage + ".url.target" + "}");
					}
					startpage = PropertyResolver.resolve(object, config.tagnameStartpage + ".url.target");
				} catch (UnknownPropertyException upe) {
					if (logger.isDebugEnabled()) {
						logger.debug("No startpage found for folder: " + object);
					}
					startpage = null;
				}
                
				if (startpage == null) {
					// try to resolve startpage through tagmap ..
					startpage = resolveTagmapProperty(object, config.tagnameStartpage);
				}

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
			return object.get(NAV_ID);
		}

		/**
		 * @return Update timestamp of element.
		 */
		public int getUpdate() {
			return ObjectTransformer.getInt(object.get(NAV_EDITTIMESTAMP), 0);
		}

		/**
		 * @return Type of element (e.g. 10002 == folder).
		 */
		public int getType() {
			return ObjectTransformer.getInt(object.get(NAV_OBJECTTYPE), 0);
		}

		/**
		 * @return "" if object property is not active, "0" if element is not
		 *         hidden and "1" if element is hidden.
		 */
		public String getHidden() {
			try {
				return ObjectTransformer.getString(PropertyResolver.resolve(object, "object." + config.tagnameHidden), "");
			} catch (UnknownPropertyException upe) {
				return "";
			}
		}

		/**
		 * @param element The resolvable for which the hidden attribute should
		 *        be resolved.
		 * @return True if the element is hidden from navigation, false
		 *         otherwise.
		 */
		protected boolean getHidden(Resolvable element) {
			try {
				return ObjectTransformer.getBoolean(PropertyResolver.resolve(element, "object." + config.tagnameHidden), false);
			} catch (UnknownPropertyException upe) {
				return false;
			}
		}

		/**
		 * @return Id of parent element.
		 */
		public Object getMotherid() {
			return "10002." + (objectIsFolder() ? object.get(NAV_MOTHERID) : object.get(NAV_MOTHERID_NONFOLDER));
		}

		/**
		 * Return true when the navobject is a folder, false if not
		 * @return true for folders, false for other objects
		 */
		protected boolean objectIsFolder() {
			Object tmpType = getObject().get(NAV_OBJECTTYPE);

			return ObjectTransformer.getInt(tmpType, 0) == TYPE_FOLDER;
		}

		/**
		 * Check to see if this folder should be opened in the navigation. Cache
		 * to speed up following calls.
		 * @return If folder is open.
		 */
		protected boolean isOpen() {

			if (isOpenCache) {
				return isOpen;
			}

			boolean open = false;
			// get object type for this object
			Object tmpType = getObject().get(NAV_OBJECTTYPE);
			int type = ObjectTransformer.getInt(tmpType, 0);

			// only folders can have sub items
			if (type == TYPE_FOLDER) {
				open = isInpath() || config.sitemap || config.startfolder.equals(getObject());
			}

			isOpenCache = true;
			isOpen = open;
			return open;
		}

		/**
		 * Check to see if this element is in the path between the current page
		 * and the startpage
		 * @return If this element is in the path.
		 */
		protected boolean isInpath() {
			return config.path.contains(getObject());
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

		/**
		 * Checks any child elements that are included in objects and are
		 * visible for the navigation. Cache the result to speed up following
		 * calls.
		 * @return If the element has child elements.
		 */
		protected boolean hasItems() {
			// check cache to see if we already calculated hasItems
			if (hasItemsCache) {
				return hasItems;
			}

			// only open elements have items
			if (!isOpen()) {
				return false;
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

				if (tmpPages instanceof Collection) {
					Collection pagesList = (Collection) tmpPages;
					Iterator it = pagesList.iterator();

					while (it.hasNext()) {
						Object tmpPage = it.next();

						if (tmpPage instanceof Resolvable) {
							Resolvable page = (Resolvable) tmpPage;
							boolean hidden = getHidden(page);

							String languageCode = "";

							try {
								languageCode = ObjectTransformer.getString(PropertyResolver.resolve(page, NAV_LANGUAGECODE), "");
							} catch (UnknownPropertyException upe) {
								languageCode = "";
							}
                            
							// exit loop if page is not published
							boolean online = ((Boolean) page.get("online")).booleanValue();

							if (!online) {
								continue;
							}

							if ((!hidden || config.disableHidden) && (config.currentPage == null || config.languagecode.equals(languageCode))) {
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

				if (tmpFolders instanceof Collection) {
					Collection foldersList = (Collection) tmpFolders;
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
			// note that files and images appear to be the same to xnlnav
			if (config.objects.contains(OBJECTS_FILES) || config.objects.contains(OBJECTS_ALL)) {
				Object tmpFiles = object.get(OBJECTS_FILES);

				if (tmpFiles instanceof Collection) {
					Collection filesList = (Collection) tmpFiles;
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
				Object tmpImages = object.get(OBJECTS_IMAGES);

				if (tmpImages instanceof Collection) {
					Collection imagesList = (Collection) tmpImages;
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
		 * Render any child elements this element has.
		 * @return The rendered output for all child elements.
		 * @throws NodeException If rendering fails.
		 */
		public String getChilds() throws NodeException {
			// there is an unexpected null pointer exception hidden in here
			// somewhere. Unfortunately, Velocity will intercept the exception 
			// and throw a different one by itself, which doesn't include the
			// line number where the NPE occurred, so we have to catch and log 
			// the exception here.
			try {
				// check if node is open
				if (!isOpen()) {
					return "";
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Get all children for folder: " + getObject().toString());
				}

				// TODO initial size for stringbuffer
				StringWriter outputWriter = new StringWriter();

				checkSorting();

				// merge all children
				// create new vector to avoid unmodifiable singleton lists
				Vector children = new Vector();

				// get folders
				if (config.objects.contains(OBJECTS_FOLDERS) || config.objects.contains(OBJECTS_ALL)) {
					Object tmpFolders = getObject().get(OBJECTS_FOLDERS);

					if (tmpFolders instanceof Collection) {
						children.addAll((Collection) tmpFolders);
					}
				}

				// get pages
				if (config.objects.contains(OBJECTS_PAGES) || config.objects.contains(OBJECTS_ALL)) {
					Object tmpPages = getObject().get(OBJECTS_PAGES);

					if (tmpPages instanceof Collection) {
						for (Iterator pageIt = ((Collection) tmpPages).iterator(); pageIt.hasNext();) {
							RenderableResolvable tmpPage = (RenderableResolvable) pageIt.next();
							boolean online = ((Boolean) tmpPage.get("online")).booleanValue();

							if (online) {
								children.add(tmpPage);
							}
						}

					}
				}

				// get files
				if (config.objects.contains(OBJECTS_FILES) || config.objects.contains(OBJECTS_ALL)) {
					Object tmpFiles = getObject().get(OBJECTS_FILES);

					if (tmpFiles instanceof Collection) {
						children.addAll((Collection) tmpFiles);
					}
					Object tmpImages = getObject().get(OBJECTS_IMAGES);

					if (tmpImages instanceof Collection) {
						children.addAll((Collection) tmpImages);
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Children for folder " + getObject().toString() + ": " + children.toString());
				}

				// Collections.sort(children, new ResolvableComparator(folderSortBy,
				// folderSortOrder, false));
				Collections.sort(children, new ResolvableComparator(folderSortBy, folderSortOrder, false) {

					/**
					 * Compare the given resolvables with the given sorting setting
					 * @param r1 first resolvable
					 * @param r2 second resolvable
					 * @param sorting sorting
					 * @return -1 if r1 is "smaller", 1 if r1 is "greater" or 0 if
					 *         the objects are equal
					 */
					protected int compare(Resolvable r1, Resolvable r2, Sorting sorting) {

						// switch resolvables if we sort descending ..
						switch (sorting.getSortOrder()) {
						case Datasource.SORTORDER_ASC:
							break;

						case Datasource.SORTORDER_DESC:
							Resolvable tmp = r2;

							r2 = r1;
							r1 = tmp;
							break;

						default:
							return 0;
						}

						Object value1 = resolveTagmapProperty(r1, sorting.getColumnName());
						Object value2 = resolveTagmapProperty(r2, sorting.getColumnName());

						int c = MiscUtils.compareObjects(value1, value2, caseSensitive);

						// If it is the same for the selected property, sort by "name"
						if (c == 0) {
							value1 = resolveTagmapProperty(r1, "name");
							value2 = resolveTagmapProperty(r2, "name");
							c = MiscUtils.compareObjects(value1, value2, caseSensitive);
						}

						return c;
					}
				});

				Iterator it = children.iterator();

				while (it.hasNext()) {
					Object tmpChild = it.next();

					if (tmpChild instanceof Resolvable) {
						Resolvable child = (Resolvable) tmpChild;
						boolean hidden = getHidden(child);

						// skip hidden items
						if (hidden && !config.disableHidden) {
							continue;
						}

						// skip pages in other languages
						if (TYPE_PAGE == ObjectTransformer.getInt(child.get(NAV_OBJECTTYPE), 0)) {
							String languageCode = "";

							try {
								languageCode = ObjectTransformer.getString(PropertyResolver.resolve(child, NAV_LANGUAGECODE), "");
							} catch (UnknownPropertyException upe) {
								languageCode = "";
							}

							if (!StringUtils.isEmpty(languageCode) && !StringUtils.isEmpty(config.languagecode) && !languageCode.equals(config.languagecode)
									&& config.currentPage != null) {
								continue;
							}
						}

						// skip if the name is empty (fixes RT#22639)
						NavObject nav = new NavObject(child, level + 1, config);

						if (StringUtils.isEmpty(nav.getName())) {
							continue;
						}

						outputWriter.write(renderObject(child, level + 1, config));
					}
				}

				return outputWriter.toString();
			} catch (RuntimeException e) {
				logger.error("Unexpected Exception", e);
				throw e;
			}
		}

		/**
		 * find an object's property inside the tagmap, then resolve it
		 * @param property to be resolved
		 * @param obj to resolve the property for
		 * @return resolved property
		 */
		public Object resolveTagmapProperty(Resolvable obj, String property) {
			Object resolvedProperty = null;
			Integer objType = ObjectTransformer.getInteger(obj.get("ttype"), null);

			// images are saved with TYPE_FILE actually, so we have to alter
			// that before lookup
			if (objType.intValue() == ContentFile.TYPE_IMAGE) {
				objType = new Integer(ContentFile.TYPE_FILE);
			}

			if (objType == null) {
				logger.error("could not determine type for object {" + obj + "}");
				return null;
			}

			if (tagmapCache.containsKey(objType)) {
				TagmapEntryRenderer tagmapEntry = (TagmapEntryRenderer) ((Map) tagmapCache.get(objType)).get(property);

				if (tagmapEntry == null && property.startsWith("object.")) {
					tagmapEntry = (TagmapEntryRenderer) ((Map) tagmapCache.get(objType)).get(property.substring(7));
				}
				if (tagmapEntry != null) {
					try {
						resolvedProperty = PropertyResolver.resolve(obj, tagmapEntry.getTagname());
						// when the property could be resolved to something
						// which is not null, we check the attributetype of the
						// tagmap entry and possibly transform the resolved
						// value into the proper type
						if (resolvedProperty != null) {
							switch (tagmapEntry.getAttributeType()) {
							case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
								resolvedProperty = ObjectTransformer.getDouble(resolvedProperty, null);
								break;

							case GenticsContentAttribute.ATTR_TYPE_LONG:
								resolvedProperty = ObjectTransformer.getLong(resolvedProperty, null);
								break;

							case GenticsContentAttribute.ATTR_TYPE_INTEGER:
								resolvedProperty = ObjectTransformer.getInteger(resolvedProperty, null);
								break;

							default:
								break;
							}
						}
					} catch (UnknownPropertyException e) {
						logger.error("Unknown property {" + tagmapEntry.getTagname() + "}", e);
					}
				}
			}

			return resolvedProperty;
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
		 * If current path should be highlighted
		 */
		public boolean activepath;

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
		public boolean disableFallback;

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
		public HashMap templates;

		/**
		 * Name of tag for startpage of folder
		 */
		public String tagnameStartpage;

		public ConfigObject() {
			startfolder = null;
			template = null;
			sortBy = null;
			sortOrder = 0;
			tagnameSort = null;
			sitemap = INPUT_SITEMAP_DEFAULT;
			activepath = INPUT_ACTIVEPATH_DEFAULT;
			languagecode = null;
			tagnameHidden = null;
			disableHidden = INPUT_DISABLEHIDDEN_DEFAULT;
			disableFallback = INPUT_DISABLEFALLBACK_DEFAULT;
			objects = null;
			currentPage = null;
			path = null;
			templates = null;
			tagnameStartpage = null;
		}

		/**
		 * Return all configuration parameters as string.
		 */
		public String toString() {
			return " startfolder: " + startfolder + ". template: " + template + ". sortBy: " + sortBy + ". sortOrder: " + sortOrder + ". tagnameSort: "
					+ tagnameSort + ". sitemap: " + sitemap + ". activepath: " + activepath + ". languagecode: " + languagecode + ". tagnameHidden: " + tagnameHidden
					+ ". disableHidden: " + disableHidden + ". disableFallback: " + disableFallback + ". objects: " + objects + ". currentPage: " + currentPage
					+ ". path: " + path + ". templates: " + templates + ". tagnameStartpage: " + tagnameStartpage;
		}

	}
}
