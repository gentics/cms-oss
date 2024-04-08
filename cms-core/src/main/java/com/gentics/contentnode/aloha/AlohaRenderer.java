/*
 * @author floriangutmann
 * @date Apr 12, 2010
 * @version $Id: AlohaRenderer.java,v 1.35.2.4 2011-02-21 06:14:49 tobiassteiner Exp $
 */
package com.gentics.contentnode.aloha;

import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.Level;
import org.json.JSONException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.FormDirective;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.render.renderer.MetaEditableRenderer;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse.Editable;
import com.gentics.contentnode.rest.model.response.PageRenderResponse.MetaEditable;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Renderer, that renders all aloha specific output like aloha includes and settings.
 *
 * @author floriangutmann
 */
public class AlohaRenderer implements TemplateRenderer {

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(AlohaRenderer.class);

	/**
	 * Logger for aloha client side logs
	 */
	private static NodeLogger alohaLogger = NodeLogger.getNodeLogger("com.gentics.aloha");

	/**
	 * Service loader for {@link AlohaPluginService}s
	 */
	private final static ServiceLoaderUtil<AlohaPluginService> alohaPluginServiceLoader = ServiceLoaderUtil
			.load(AlohaPluginService.class);

	/**
	 * List of allowed HTML elements for editable root elements
	 */
	public static final List<String> allowedEditables = Arrays.asList(new String[] { "div", "p", "a", "span", "h1", "h2", "h3", "h4", "h5", "h6"});

	/**
	 * List of allowed HTML elements for block root elements
	 */
	public static final List<String> allowedBlocks = Arrays.asList(new String[] { "div", "span", "img", "a", "p", "h1", "h2", "h3", "h4", "h5", "h6" });

	/**
	 * Prefix for editable id's
	 */
	public static final String EDITABLE_PREFIX = "GENTICS_EDITABLE_";

	/**
	 * Prefix for block id's
	 */
	public static final String BLOCK_PREFIX = "GENTICS_BLOCK_";

	/**
	 * Prefix for the tagname class
	 */
	public static final String TAGNAME_PREFIX = "GENTICS_tagname_";

	/**
	 * Prefix for the construct keyword class
	 */
	public static final String CONSTRUCT_PREFIX = "GENTICS_construct_";

	/**
	 * Prefix for the tagpart class
	 */
	public static final String TAGPART_PREFIX = "GENTICS_tagpart_";

	/**
	 * Prefix for the parttype class
	 */
	public static final String PARTTYPE_PREFIX = "GENTICS_parttype_";

	/**
	 * Name of the render result parameter that holds a list of HTML id's for blocks
	 */
	public static final String PARAM_BLOCK_HTML_IDS = "block_html_ids";

	/**
	 * Name of the render result parameter that holds a list of Tag id's for blocks
	 */
	public static final String PARAM_BLOCK_TAG_IDS = "block_tag_ids";

	/**
	 * Name of the render result parameter that holds a list of HTML id's for editables
	 */
	public static final String PARAM_EDITABLE_HTML_IDS = "editable_html_ids";

	/**
	 * Name of the render result parameter that holds a list of value id's for editables
	 */
	public static final String PARAM_EDITABLE_VALUE_IDS = "editable_value_ids";

	/**
	 * Name of the render result parameter that holds a list of contenttag names for editables
	 */
	public static final String PARAM_EDITABLE_TAG_NAMES = "editable_tag_names";

	/**
	 * Name of the render result parameter that holds a list of part names for editables
	 */
	public static final String PARAM_EDITABLE_PART_IDS = "editable_part_names";

	/**
	 * Name of the render result parameter that holds the page id
	 */
	public static final String PARAM_PAGE_ID = "aloha_pageId";

	/**
	 * Name of the render result parameter that holds the name of the magiclink construct
	 */
	public static final String PARAM_MAGICLINK_CONSTRUCT = "aloha_magiclinkconstruct";

	/**
	 * Tag name for the aloha settings where they should be pasted in a template.
	 */
	public static final String ALOHA_SETTINGS_PLACE_HOLDER = "<aloha_settings>";

	/**
	 * Tag name for the aloha scripts where they should be pasted in a template.
	 */
	public static final String ALOHA_SCRIPTS_PLACE_HOLDER = "<aloha_scripts>";

	/**
	 * Parameter name for the aloha settings for the renderresult.
	 */
	public static final String ALOHA_SETTINGS_PARAMETER = "aloha_settings";

	/**
	 * Parameter name for the aloha scripts for the renderresult.
	 */
	public static final String ALOHA_SCRIPTS_PARAMETER = "aloha_scripts";

	/**
	 * The name of the magic link construct.
	 */
	public static final String MAGICLINK_KEYWORD = "gtxalohapagelink";

	/**
	 * Name of the rendertype parameter that holds the flag for whether the aloha settings and script
	 * includes need to be generated
	 */
	public final static String RENDER_SETTINGS = "render_aloha_settings";

	/**
	 * Name of the rendertype parameter that holds the flag for whether editables shall be replaced
	 */
	public final static String REPLACE_EDITABLES = "replace_editables";

	/**
	 * name of the rendertype parameter that holds the flag for whether the
	 * script includes shall be added to the content (in the head) or returned
	 * as render result
	 */
	public final static String ADD_SCRIPT_INCLUDES = "add_script_includes";

	/**
	 * name of the rendertype parameter that holds the flag for whether the
	 * html5 doctype shall be added to the content
	 */
	public final static String ADD_HTML5_DOCTYPE = "add_html5_doctype";

	/**
	 * name of the render result parameter that will hold the script includes if
	 * they shall not be added to the content
	 */
	public final static String SCRIPT_INCLUDES = "script_includes";

	/**
	 * Name of the rendertype parameter which determines whether the links are
	 * "frontend" links or "backend" links
	 */
	public final static String LINKS_TYPE = "aloha_links";

	/**
	 * Name of the rendertype parameter, that is set to "true" if the user requested edit mode,
	 * but had no permission
	 */
	public final static String READONLY_PERM = "aloha_readonly_perm";

	/**
	 * Name of the rendertype parameter, that is set to "true" if the user requested edit mode,
	 * but the page was locked
	 */
	public final static String READONLY_LOCKED = "aloha_readonly_locked";

	/**
	 * Name of the rendertype parameter, that is set to "true" if the page is deleted
	 */
	public final static String DELETED = "aloha_deleted";

	/**
	 * Name of the rendertype parameter which determines the page id from which
	 * this page is translated (empty, if the page is not translated)
	 */
	public final static String TRANSLATION_MASTER = "translation_master";

	/**
	 * Name of the rendertype parameter which determines the version from which
	 * this page is translated. Empty if translated from the current version of
	 * the other page.
	 */
	public final static String TRANSLATION_VERSION = "translation_version";

	/**
	 * Name of the rendertype parameter of the tagname, that shall be marked with surrounding markers
	 */
	public final static String MARK_TAG = "aloha_block_marked";

	/**
	 * The last action that was performed before this page has loaded
	 */
	public final static String LAST_ACTION = null;

	/**
	 * Pattern for removing the aloha-block classes within the class attribute
	 */
	private final static String ALOHA_BLOCK_CLASSES_REGEX = "aloha-block[\\s$]|^aloha-block$";

	/**
	 * The name of the folder that contains aloha editor
	 */
	private final static String ALOHA_EDITOR_FOLDER_NAME = "alohaeditor";

	/**
	 * Name of the system property containing the aloha editor base URL
	 */
	public final static String ALOHA_EDITOR_BASE_URL_PARAM = "com.gentics.contentnode.alohaeditor.url";

	/**
	 * Name of the system property containing the build timestamp (which is part of some URLs)
	 */
	public final static String BUILD_TIMESTAMP = "build_root_timestamp";

	/**
	 * Empty string.
	 */
	private static final String EMPTY_STRING = "";

	/**
	 * Pattern for finding an id in a html tag
	 */
	private static Pattern idPattern = Pattern.compile("\\s+id\\s*=\\s*[\"\']?([^\"\'\\s\\>]*)[\"\']?", Pattern.DOTALL | Pattern.COMMENTS);

	/**
	 * Pattern for finding the class attribute in a html tag
	 */
	private static Pattern classPattern = Pattern.compile("\\s+class\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL | Pattern.COMMENTS);

	/**
	 * Pattern for finding plinks
	 */
	private static Pattern plinkPattern = Pattern.compile("<plink\\s*(?:(?:[^>=]*>)|(?:(?:[^>=]*=\"[^\"]*\")*/?>))");

	/**
	 * Pattern for finding root tags
	 */
	private static Pattern rootTagPattern = Pattern.compile("^(<([a-zA-Z0-9]*)\\b[^>]*>)(.*?</\\2\\s*>$)", Pattern.DOTALL | Pattern.COMMENTS);

	/**
	 * Helper method to get the editables stored in the given render result as list of JSON objects
	 * @param renderResult render result
	 * @return list of JSON objects containing information about the editables
	 * @throws NodeException
	 * @throws JSONException
	 */
	public static List<JsonNode> getEditables(RenderResult renderResult, ObjectMapper mapper) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<JsonNode> cnEditables = new ArrayList<JsonNode>();

		String[] editableHtmlIds = (String[]) renderResult.getParameters().get(PARAM_EDITABLE_HTML_IDS);
		String[] editableValueIds = (String[]) renderResult.getParameters().get(PARAM_EDITABLE_VALUE_IDS);

		Collection<?> readonlies;

		if (renderResult.getParameters().containsKey(MetaEditableRenderer.READONLIES_KEY)) {
			readonlies = ObjectTransformer.getCollection(renderResult.getParameters().get(MetaEditableRenderer.READONLIES_KEY), Collections.EMPTY_LIST);
		} else {
			readonlies = Collections.EMPTY_LIST;
		}

		if (editableHtmlIds != null) {
			for (int i = 0; i < editableHtmlIds.length; i++) {
				ObjectNode cnEditable = mapper.createObjectNode();

				cnEditable.put("id", editableHtmlIds[i]);

				// get the Value
				Value value = (Value) t.getObject(Value.class, ObjectTransformer.getInteger(editableValueIds[i], null));

				// and store tag name and part keyname
				cnEditable.put("tagname", value.getContainer().get("name").toString());
				cnEditable.put("partname", value.getPart().getKeyname());
				if (readonlies.contains(value.getContainer().get("name").toString())) {
					cnEditable.put("readonly", true);
				}
				cnEditables.add(cnEditable);
			}
		}

		// now add meta editables, which where added by the MetaEditableRenderer
		if (renderResult.getParameters().containsKey(MetaEditableRenderer.METAEDITABLES_KEY)) {
			Collection<?> me = ObjectTransformer.getCollection(renderResult.getParameters().get(MetaEditableRenderer.METAEDITABLES_KEY), null);
			String id;

			for (Iterator<?> iterator = me.iterator(); iterator.hasNext();) {
				id = ObjectTransformer.getString(iterator.next(), null);
				ObjectNode cnEditable = mapper.createObjectNode();

				// unfortunately this has to be done here, as jQuery would
				// interprete "."-chars as the start of a css class name

				// TODO this is extremely ugly, due to RenderResult not supporting
				// Maps in addParameters. Review this with NOP
				cnEditable.put("id", MetaEditableRenderer.EDITABLE_PREFIX + id.replaceAll("\\.", "_"));
				cnEditable.put("metaproperty", id);
				cnEditables.add(cnEditable);
			}
		}

		return cnEditables;
	}

	/**
	 * Helper method to get the blocks stored in the given render result as list of JSON objects
	 * @param renderResult render result
	 * @return list of JSON objects containing information about the blocks
	 * @throws NodeException
	 * @throws JSONException
	 */
	public static List<JsonNode> getBlocks(RenderResult renderResult, ObjectMapper mapper) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<JsonNode> cnBlocks = new Vector<JsonNode>();

		String[] blockHtmlIds = (String[]) renderResult.getParameters().get(PARAM_BLOCK_HTML_IDS);
		String[] blockTagIds = (String[]) renderResult.getParameters().get(PARAM_BLOCK_TAG_IDS);

		if (blockHtmlIds != null) {
			// Add blocks to CN integration plugin
			for (int i = 0; i < blockTagIds.length; i++) {
				ObjectNode cnBlock = mapper.createObjectNode();

				cnBlock.put("id", blockHtmlIds[i]);
				cnBlock.put("tagid", blockTagIds[i]);

				// get the tag
				ContentTag tag = (ContentTag) t.getObject(ContentTag.class, ObjectTransformer.getInteger(blockTagIds[i], null));

				// and store the tagname
				cnBlock.put("tagname", tag.getName());

				// get the construct
				Construct construct = tag.getConstruct();

				// store the iconurl and icontitle
				if (construct.getIcon() != null) {
					cnBlock.put("iconurl", construct.getIcon().getURL());
				}
				try {
					cnBlock.put("constructid", construct.getId().toString());
				} catch (Exception e) {
					logger.error("construct for tag {" + blockTagIds[i] + "} has no id.");
				}
				cnBlock.put("icontitle", tag.getName() + " (" + construct.getName() + ")");
				cnBlock.put("editdo", tag.containsOverviewPart() ? "17001" : "10008");

				cnBlocks.add(cnBlock);
			}
		}

		return cnBlocks;
	}

	/**
	 * Get the keyword of the magic link construct used for the node
	 * @param node node
	 * @return construct keyword
	 * @throws NodeException
	 */
	public static String getMagicLinkConstructKeyword(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct magicLinkConstruct = null;
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// get the global config for the magiclinkconstruct
		Integer globalConfigConstructId = ObjectTransformer.getInteger(
				prefs.getProperty("aloha_settings.plugins.gcn.magiclinkconstruct"), null);

		if (globalConfigConstructId != null) {
			magicLinkConstruct = t.getObject(Construct.class, globalConfigConstructId);
		}

		if (node != null) {
			// get the node specific config for the magiclinkconstruct
			Integer nodeConfigConstructId = null;
			Map<String, Object> nodeSettings = prefs.getPropertyObject(node, "aloha_settings_node");
			if (nodeSettings != null) {
				nodeConfigConstructId = ObjectTransformer.getInteger(new MapPreferences(nodeSettings).getPropertyObject("plugins.gcn.magiclinkconstruct"),
						null);
			}

			if (nodeConfigConstructId != null) {
				Construct nodeSpecificMagicLinkConstruct = t.getObject(Construct.class, nodeConfigConstructId);
				if (nodeSpecificMagicLinkConstruct != null) {
					magicLinkConstruct = nodeSpecificMagicLinkConstruct;
				}
			}
		}

		return magicLinkConstruct != null ? magicLinkConstruct.getKeyword() : MAGICLINK_KEYWORD;
	}

	/**
	 * Check if the current user has permission for the devtool packages.
	 * @param t the current transaction
	 * @return true if the user has permission to view the devtool packages
	 */
	private Boolean hasDevtoolsPerms(Transaction t) {
		return t.getPermHandler().checkPermissionBit(PermHandler.TYPE_ADMIN, null, PermHandler.PERM_VIEW) &&
				t.getPermHandler().checkPermissionBit(PermHandler.TYPE_CONADMIN, null, PermHandler.PERM_VIEW) &&
				t.getPermHandler().checkPermissionBit(PermHandler.TYPE_DEVTOOLS_PACKAGES, null, PermHandler.PERM_VIEW);
	}

	/**
	 * Get the configuration for Aloha
	 * @param node node of the rendered page
	 * @param renderResult render result
	 * @param renderType render type
	 * @param mapper object mapper
	 * @return JSONObject with aloha configuration
	 * @throws JSONException
	 * @throws NodeException
	 */
	public JsonNode getAlohaSettings(Node node, RenderResult renderResult, RenderType renderType, ObjectMapper mapper) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getPreferences();

		// Basic JSON object for settings
		ObjectNode settings = mapper.createObjectNode();
		ObjectNode plugins = mapper.createObjectNode();

		settings.put("plugins", plugins);

		// Load system dependend settings
		String buildRootTimestamp = System.getProperty(BUILD_TIMESTAMP);
		String webappPrefix = prefs.getProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM);
		String gcnJSLibVersion = prefs.getProperty("gcn_lib_version");
		// current language
		String currentLanguage = UserLanguageFactory.getById(ContentNodeHelper.getLanguageId(), true).getCode();

		ObjectNode i18n = mapper.createObjectNode();

		i18n.put("current", currentLanguage);
		settings.put("i18n", i18n);
		settings.put("locale", currentLanguage);

		// add the proxyURL as general setting
		settings.put("proxyUrl",
				prefs.getProperty("stag_prefix") + "?sid=" + t.getSessionId() + "&do=19191&url=");

		// Log levels
		ObjectNode logSettings = mapper.createObjectNode();

		settings.put("logLevels", logSettings);
		if (alohaLogger.isDebugEnabled()) {
			logSettings.put("debug", true);
		}
		if (alohaLogger.isInfoEnabled()) {
			logSettings.put("info", true);
		}
		if (alohaLogger.isWarnEnabled()) {
			logSettings.put("warn", true);
		}
		if (alohaLogger.isErrorEnabled()) {
			logSettings.put("error", true);
		}

		// GCN page object
		com.gentics.contentnode.object.Page gcnPage = (com.gentics.contentnode.object.Page) t.getRenderType().getRenderedRootObject();

		// Page id
		Integer pageId = gcnPage.getId();

		// read-only flag
		boolean readonly;

		if (renderType.getEditMode() == RenderType.EM_ALOHA_READONLY) {
			readonly = true;
		} else {
			readonly = false;
		}

		// Load page meta data
		PageResourceImpl pageResource = new PageResourceImpl(t);

		pageResource.setSessionId(t.getSessionId());
		pageResource.setSessionSecret(t.getSession().getSessionSecret());
		pageResource.setTransaction(t);
		pageResource.initialize();
		PageLoadResponse pageLoadResponse = pageResource.load(String.valueOf(pageId), !readonly, false, false, false, false, false, false, false, false, false, null, null);

		if (pageLoadResponse.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			throw new NodeException("Error while loading page metadata", new Exception(pageLoadResponse.getResponseInfo().getResponseMessage()));
		}
		Page page = pageLoadResponse.getPage();

		// add LinkChecker plugin config
		ObjectNode linkCheckerPlugin = mapper.createObjectNode();

		linkCheckerPlugin.put("proxyUrl",
				prefs.getProperty("stag_prefix") + "?sid=" + t.getSessionId() + "&do=19191&url=");
		plugins.put("linkchecker", linkCheckerPlugin);

		for (AlohaPluginService service : alohaPluginServiceLoader) {
			service.addPluginConfiguration(settings, node, gcnPage, renderResult, renderType);
		}

		// Create configuration for CN integration plugin
		ObjectNode cnIntegrationPlugin = mapper.createObjectNode();

		plugins.put("gcn", cnIntegrationPlugin);

		cnIntegrationPlugin.put("sid", t.getSessionId());
		cnIntegrationPlugin.put("buildRootTimestamp", buildRootTimestamp);
		cnIntegrationPlugin.put("gcnLibVersion", gcnJSLibVersion);
		cnIntegrationPlugin.put("webappPrefix", webappPrefix);
		cnIntegrationPlugin.put("languageid", t.getLanguage().getId());
		cnIntegrationPlugin.put("pagelanguage", page.getLanguage());
		cnIntegrationPlugin.put("links", ObjectTransformer.getString(renderType.getParameter(LINKS_TYPE), "backend"));
		cnIntegrationPlugin.put("id", page.getId());
		cnIntegrationPlugin.put("name", page.getName());
		cnIntegrationPlugin.put("priority", page.getPriority());
		cnIntegrationPlugin.put("description", page.getDescription());
		cnIntegrationPlugin.put("templateId", page.getTemplateId());
		cnIntegrationPlugin.put("nodeFolderId", gcnPage.getFolder().getNode().getFolder().getId().toString());
		cnIntegrationPlugin.put("nodeId", ObjectTransformer.getString(gcnPage.getFolder().getNode().getId(), null));
		cnIntegrationPlugin.put("folderId", page.getFolderId());
		cnIntegrationPlugin.put("fileName", page.getFileName());
		cnIntegrationPlugin.put("online", page.isOnline());
		cnIntegrationPlugin.put("modified", page.isModified());
		cnIntegrationPlugin.put("isPublicationPermitted", PermHandler.ObjectPermission.publish.checkObject(gcnPage));
		// we need to check if the devtools feature is active and if the user has the permission to view the
		// devtools rest-endpoint before we can activate the devtools settings
		if (prefs.isFeature(Feature.DEVTOOLS) && hasDevtoolsPerms(t)) {
			cnIntegrationPlugin.put("devtools", true);
		}
		// add info about forms feature
		if (prefs.isFeature(Feature.FORMS)) {
			cnIntegrationPlugin.put("forms", true);
		}
		cnIntegrationPlugin.put("stag_prefix", prefs.getProperty("stag_prefix"));
		cnIntegrationPlugin.put("portletapp_prefix", prefs.getProperty("portletapp_prefix"));
		cnIntegrationPlugin.put("lastAction", ObjectTransformer.getString(renderType.getParameter(LAST_ACTION), ""));
		// Add the proxy_prefix parameter when one was specified. This parameter will be used to build certain urls to images and css
		String proxy_prefix = prefs.getProperty(DynamicUrlFactory.PROXY_PREFIX_PARAM);

		if (proxy_prefix != null) {
			cnIntegrationPlugin.put("proxy_prefix", proxy_prefix);
		}
		// check whether feature copy_tags is active
		cnIntegrationPlugin.put("copy_tags", prefs.isFeature(Feature.COPY_TAGS, node));

		// Add translation sync information if rendering for translation
		// check whether render the page for translating it
		Integer translationMaster = ObjectTransformer.getInteger(renderType.getParameter(TRANSLATION_MASTER), null);

		if (translationMaster != null) {
			cnIntegrationPlugin.put(TRANSLATION_MASTER, translationMaster);
			Integer translationVersion = ObjectTransformer.getInteger(renderType.getParameter(TRANSLATION_VERSION), null);

			if (translationVersion != null) {
				cnIntegrationPlugin.put(TRANSLATION_VERSION, translationVersion);
			}
		}

		// Add available languages
		ArrayNode languages = mapper.createArrayNode();
		List<ContentLanguage> nodeLanguages = gcnPage.getFolder().getNode().getLanguages();

		for (ContentLanguage nodeLanguage : nodeLanguages) {
			boolean canView = PermHandler.ObjectPermission.view.checkObject(gcnPage);

			// Check if the user has view permissions for pages in that language
			if (canView) {
				ObjectNode language = mapper.createObjectNode();

				language.put("name", nodeLanguage.getName());
				language.put("code", nodeLanguage.getCode());
				language.put("id", nodeLanguage.getId().toString());
				languages.add(language);
			}
		}
		if (languages.size() > 0) {
			cnIntegrationPlugin.put("languageMenu", languages);
		}

		boolean canWrite = PermHandler.ObjectPermission.edit.checkObject(gcnPage);

		Message message = null;

		if ((!readonly && !canWrite) || ObjectTransformer.getBoolean(renderType.getParameter(READONLY_PERM), false)) {
			// User doesn't have permission to write this page
			readonly = true;
			message = new Message(Type.INFO, new CNI18nString("aloha_msg_no_write_perm_opened_readonly").toString());
		} else if ((!readonly && page.isReadOnly()) || ObjectTransformer.getBoolean(renderType.getParameter(READONLY_LOCKED), false)) {
			// Page is locked by another user
			readonly = true;
			message = new Message(Type.INFO, new CNI18nString("aloha_msg_locked_opened_readonly").toString());
		}

		settings.put("readonly", readonly);
		if (ObjectTransformer.getBoolean(renderType.getParameter(DELETED), false)) {
			settings.put("deleted", true);
		}

		if (!readonly) {
			// Add editables to CN integration plugin
			ArrayNode editablesNode = mapper.createArrayNode();
			List<JsonNode> editables = getEditables(renderResult, mapper);

			for (Iterator<JsonNode> it = editables.iterator(); it.hasNext();) {
				editablesNode.add(it.next());
			}
			// NOTE can't use putArray().addAll() here as there is a NPE in the current Jackson version
			cnIntegrationPlugin.put("editablesNode", editablesNode);

			// Add blocks to CN integration plugin
			ArrayNode blocksNode = mapper.createArrayNode();
			List<JsonNode> blocks = getBlocks(renderResult, mapper);

			for (Iterator<JsonNode> iterator = blocks.iterator(); iterator.hasNext();) {
				blocksNode.add(iterator.next());
			}

			// NOTE can't use putArray().addAll() here as there is a NPE in the current Jackson version
			cnIntegrationPlugin.put("blocks", blocksNode);

			// add tags information in the new style (suitable for the GCN Library).
			List<PageRenderResponse.Tag> tags = PageResourceImpl.getTags(renderResult);
			ArrayNode tagsNode = mapper.createArrayNode();

			for (PageRenderResponse.Tag tag : tags) {
				ObjectNode cnTag = mapper.createObjectNode();

				cnTag.put("element", tag.getElement());
				cnTag.put("tagname", tag.getTagname());
				cnTag.put("onlyeditables", tag.isOnlyeditables());
				List<Editable> tagEditables = tag.getEditables();

				if (tagEditables != null) {
					ArrayNode tagEditablesNode = mapper.createArrayNode();

					for (Editable tagEditable : tagEditables) {
						ObjectNode cnTagEditable = mapper.createObjectNode();

						cnTagEditable.put("element", tagEditable.getElement());
						cnTagEditable.put("partname", tagEditable.getPartname());
						tagEditablesNode.add(cnTagEditable);
					}
					cnTag.put("editablesNode", tagEditablesNode);
				}
				tagsNode.add(cnTag);
			}
			cnIntegrationPlugin.put("tags", tagsNode);

			// add meta editables information in the new style (suitable for the GCN Library)
			List<MetaEditable> metaEditables = PageResourceImpl.getMetaEditables(renderResult);
			ArrayNode metaEditablesNode = mapper.createArrayNode();

			for (MetaEditable metaEditable : metaEditables) {
				ObjectNode cnMetaEditable = mapper.createObjectNode();

				cnMetaEditable.put("element", metaEditable.getElement());
				cnMetaEditable.put("metaproperty", metaEditable.getMetaproperty());
				metaEditablesNode.add(cnMetaEditable);
			}
			cnIntegrationPlugin.put("metaeditables", metaEditablesNode);

			// add Content.Node Tags available for insertion
			PreparedStatement stmt = null;
			ResultSet rs = null;

			try {
				// TODO handle if( !$FEATURE['construct_categories'] )
				stmt = t.prepareStatement(
						"SELECT cc.id AS category_id ,d.value AS name, c.id, c.icon, c.keyword, ccd.value AS category_name " + "FROM construct c "
						+ "LEFT JOIN dicuser d " + "ON ( d.output_id = c.name_id AND d.language_id = ?) " + "LEFT JOIN construct_category cc "
						+ "ON c.category_id = cc.id " + "LEFT JOIN dicuser ccd " + "ON ccd.output_id = cc.name_id AND ccd.language_id = ? "
						+ "INNER JOIN construct_node cn " + "ON cn.construct_id = c.id " + "WHERE c.intext = 1 AND cn.node_id = ? " + "GROUP BY c.id "
						+ "ORDER BY cc.sortorder, category_name, name");
				stmt.setInt(1, Integer.parseInt(t.getLanguage().getId()));
				stmt.setInt(2, Integer.parseInt(t.getLanguage().getId()));

				// set the node id. If the node is a channel, use its master
				// instead (tagtypes are inherited from the master)
				Node constructContainerNode = gcnPage.getFolder().getNode();

				if (constructContainerNode.isChannel()) {
					List<Node> masterNodes = constructContainerNode.getMasterNodes();

					if (masterNodes.size() > 0) {
						constructContainerNode = masterNodes.get(masterNodes.size() - 1);
					}
				}
				stmt.setInt(3, ObjectTransformer.getInt(constructContainerNode.getId(), -1));

				rs = stmt.executeQuery();

				List<JsonNode> constructCategories = new Vector<JsonNode>();
				List<JsonNode> constructs = new Vector<JsonNode>();
				ObjectNode constructCategory = null;
				String lastConstructCategoryName = null;
				String categoryName = "";
				int categoryId = 0;
				String magicLinkConstructId = null;

				while (rs.next()) {
					// special handling for the magic link construct
					if (MAGICLINK_KEYWORD.equals(rs.getString("keyword"))) {
						magicLinkConstructId = rs.getString("id");
						cnIntegrationPlugin.put("magiclinkconstruct", magicLinkConstructId);

						continue;
					}

					ObjectNode construct = mapper.createObjectNode();

					construct.put("id", rs.getString("id"));
					construct.put("name", rs.getString("name"));
					construct.put("icon", rs.getString("icon"));
					construct.put("keyword", rs.getString("keyword"));

					categoryName = rs.getString("category_name");
					categoryId = rs.getInt("category_id");
					// handle constructs without category (set category name to empty string)
					if (categoryName == null) {
						categoryName = "";
					}

					if (!categoryName.equals(lastConstructCategoryName)) {
						// add the old category to categories
						if (constructCategory != null) {
							ArrayNode constructsNode = mapper.createArrayNode();

							for (Iterator<JsonNode> it = constructs.iterator(); it.hasNext();) {
								constructsNode.add(it.next());
							}
							constructCategory.put("constructs", constructsNode);

							constructCategories.add(constructCategory);
						}

						// start a new construct category
						constructCategory = mapper.createObjectNode();
						constructCategory.put("name", categoryName);
						constructCategory.put("id", categoryId);

						constructs.clear();
					}

					constructs.add(construct);
					lastConstructCategoryName = categoryName;
				}

				// Check if we should enable the anchor link feature.
				boolean hasAnchor = false;

				if (magicLinkConstructId != null) {
					Construct magicLinkConstruct = t.getObject(Construct.class, magicLinkConstructId);

					hasAnchor = magicLinkConstruct != null
						&& magicLinkConstruct.getParts().stream().anyMatch(p -> "anchor".equals(p.getKeyname()));
				}

				ObjectNode linkPlugin = mapper.createObjectNode();

				linkPlugin.put("anchorLinks", hasAnchor);
				plugins.put("link", linkPlugin);

				// don't forget to add the last category
				if (constructCategory != null) {
					ArrayNode constructsNode = mapper.createArrayNode();

					for (Iterator<JsonNode> it = constructs.iterator(); it.hasNext();) {
						constructsNode.add(it.next());
					}
					constructCategory.put("constructs", constructsNode);

					constructCategories.add(constructCategory);
				}

				ArrayNode constructCategoriesNode = mapper.createArrayNode();

				for (Iterator<JsonNode> it = constructCategories.iterator(); it.hasNext();) {
					constructCategoriesNode.add(it.next());
				}
				cnIntegrationPlugin.put("constructCategories", constructCategoriesNode);
			} catch (SQLException e) {
				throw new NodeException("Could not load insertable constructs", e);
			} finally {
				t.closeResultSet(rs);
				t.closeStatement(stmt);
			}

			// add meta editables
			if (renderResult.getParameters().get("metaeditables") != null) {}
		}

		// add the render messages here
		Collection<NodeMessage> resultMessages = renderResult.getMessages();
		ArrayNode renderMessages = mapper.createArrayNode();
		NodeLogger logger = NodeLogger.getNodeLogger("com.gentics.aloha");

		for (NodeMessage renderMsg : resultMessages) {
			if (logger.isEnabled(renderMsg.getLevel())) {
				ObjectNode msg = mapper.createObjectNode();

				msg.put("level", renderMsg.getLevel().toString());
				msg.put("message", renderMsg.getMessage());
				renderMessages.add(msg);
			}
		}
		cnIntegrationPlugin.put("renderMessages", renderMessages);

		if (message != null) {
			ArrayNode messages = mapper.createArrayNode();
			ObjectNode msg = mapper.createObjectNode();

			// image message timestamp type
			msg.put("image", message.getImage());
			msg.put("message", message.getMessage());
			msg.put("timestamp", message.getTimestamp());
			msg.put("type", message.getType().toString());
			messages.add(msg);
			cnIntegrationPlugin.put("welcomeMessages", messages);
		}

		// finally get pre-configured settings and add to JSON Object
		setSettingMapProperty(node, mapper, t, settings, "aloha_settings");
		// get node specific settings (if available)
		Map<String, Object> nodeSettings = NodeConfigRuntimeConfiguration.getPreferences().getPropertyObject(node, "aloha_settings_node");
		if (nodeSettings != null) {
			setMapToJSON(nodeSettings, settings, mapper, true);
		}

		setSettingMapPropertyWithKeyName("sanitizeCharacters", node, mapper, t, settings, "sanitize_character");

		return settings;
	}

	/**
	 * Sets the map property 'propertyName' inside 'settings' with name 'keyName'.
	 * @param keyName
	 * @param node
	 * @param mapper
	 * @param t
	 * @param settings
	 * @param propertyName
	 */
	private void setSettingMapPropertyWithKeyName(String keyName, Node node, ObjectMapper mapper, Transaction t,
			ObjectNode settings, String propertyName) {
		Object propertyObject = t.getNodeConfig().getDefaultPreferences().getPropertyObject(propertyName);

		if (propertyObject instanceof Map) {
			Map<String, Object> newMap = new HashMap<String, Object>();
			newMap.put(keyName, propertyObject);
			setMapToJSON(newMap, settings, mapper, true);
		}

	}

	/**
	 * Sets map property 'propertyName' into 'settings'.
	 * @param node
	 * @param mapper
	 * @param t
	 * @param settings
	 * @param propertyName
	 */
	private void setSettingMapProperty(Node node, ObjectMapper mapper, Transaction t,
			ObjectNode settings, String propertyName) {
		Object propertyObject = t.getNodeConfig().getDefaultPreferences().getPropertyObject(propertyName);

		if (propertyObject instanceof Map) {
			setMapToJSON((Map)propertyObject, settings, mapper, true);
		}
	}

	/**
	 * Helper method to set the given Map of data into the given json object.
	 * Maps will be converted into objects, Collections into Arrays and all other data will be set as value
	 * @param dataMap data map containing data
	 * @param json json object where to set the data to
	 * @param mapper object mapper to be used for generating JSON
	 * @param overwrite true if existing values may be overwritten, false if not
	 */
	@SuppressWarnings("unchecked")
	protected static void setMapToJSON(Map<String, Object> dataMap, ObjectNode json, ObjectMapper mapper, boolean overwrite) {
		if (dataMap == null || dataMap.size() == 0) {
			return;
		}

		for (Entry<String, Object> e : dataMap.entrySet()) {
			Object value = e.getValue();
			String key = e.getKey();

			if (value instanceof Map) {
				ObjectNode node = null;
				JsonNode existingNode = json.get(key);

				if (existingNode == null) {
					// create a new ObjectNode and put it into the object
					node = mapper.createObjectNode();
					json.put(key, node);
				} else if (existingNode instanceof ObjectNode) {
					// existing node is of correct type, so we use it
					node = (ObjectNode) existingNode;
				} else if (overwrite) {
					// existing node is of wrong type, but we may overwrite
					node = mapper.createObjectNode();
					json.put(key, node);
				}

				// when an appropriate node exists, we set the map into the node
				if (node != null) {
					setMapToJSON((Map<String, Object>) value, node, mapper, overwrite);
				}
			} else if (value instanceof Collection) {
				ArrayNode node = null;
				JsonNode existingNode = json.get(key);

				if (existingNode == null) {
					// create a new ArrayNode and put it into the object
					node = mapper.createArrayNode();
					json.put(key, node);
				} else if (existingNode instanceof ArrayNode) {
					// existing node is of correct type, so we use it
					node = (ArrayNode) existingNode;
				} else if (overwrite) {
					// existing node is of wrong type, but we may overwrite
					node = mapper.createArrayNode();
					json.put(key, node);
				}

				// when an appropriate node exists, we set the collection into the node
				if (node != null) {
					setCollectionToJSON((Collection<Object>) value, node, mapper, overwrite);
				}
			} else {
				JsonNode existingNode = json.get(key);
				String existingValue = null;

				if (existingNode != null) {
					existingValue = existingNode.asText();
				}
				if (ObjectTransformer.isEmpty(existingValue) || overwrite) {
					json.put(key, ObjectTransformer.getString(value, null));
				}
			}
		}
	}

	/**
	 * Helper method to set a collection of objects into a JSON Array
	 * @param collection collection of objects
	 * @param array JSON array
	 * @param mapper object mapper for generating JSON objects
	 * @param overwrite true when existing values may be overwritten
	 */
	@SuppressWarnings("unchecked")
	protected static void setCollectionToJSON(Collection<Object> collection, ArrayNode array,
			ObjectMapper mapper, boolean overwrite) {
		if (ObjectTransformer.isEmpty(collection)) {
			return;
		}

		for (Object value : collection) {
			if (value instanceof Map) {
				ObjectNode node = mapper.createObjectNode();

				array.add(node);
				setMapToJSON((Map<String, Object>) value, node, mapper, overwrite);
			} else if (value instanceof Collection) {
				ArrayNode node = mapper.createArrayNode();

				array.add(node);
				setCollectionToJSON((Collection<Object>) value, node, mapper, overwrite);
			} else {
				array.add(ObjectTransformer.getString(value, null));
			}
		}
	}

	/**
	 * Extract the classes from the given start tag
	 * @param startTag start tag
	 * @return list of found classes
	 */
	protected static List<String> getClasses(String startTag) {
		Matcher classMatcher = classPattern.matcher(startTag);

		if (classMatcher.find()) {
			return Arrays.asList(classMatcher.group(1).split("\\s+"));
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Check whether the start tag is the forms preview tag
	 * @param startTag start tag
	 * @return true iff forms feature is active and the start tag is the forms preview tag
	 */
	protected static boolean isFormsPreviewTag(String startTag) {
		return getClasses(startTag).contains(FormDirective.FORMS_PREVIEW_CLASS);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		Object rootObject = renderType.getRenderedRootObject();
		Object actual = renderType.getStack().peek();

		// Only run when in aloha mode and the actual rendered object is the root object
		if ((renderType.getEditMode() == RenderType.EM_ALOHA || renderType.getEditMode() == RenderType.EM_ALOHA_READONLY) && rootObject.equals(actual)
				&& rootObject instanceof com.gentics.contentnode.object.Page) {
			com.gentics.contentnode.object.Page page = (com.gentics.contentnode.object.Page) rootObject;
			Node node = page.getFolder().getNode();

			// replace editables
			if (isReplaceEditables(renderType)) {
			try {
				template = replaceEditables(template, renderResult);
			} catch (Exception e) {
				logger.error("Error while replacing editables", e);
			}
			}

			if (!isRenderSettings(renderType)) {
				return template;
			}

			// check template code for html head tag to add an appropriate warning if neccessary
			if (!StringUtils.getHeadMatcher(template).find()) {
				renderResult.addMessage(
						new DefaultNodeMessage(Level.WARN, AlohaRenderer.class,
						"Your template does not include a html head tag, which might cause problems when using Aloha Editor."));
			}

			String alohaSettings = getAlohaSettings(renderResult, renderType, node);
			String alohaScripts = getAlohaScriptFiles(t, node);
			String alohaCSSFile = getAlohaCSSFiles(t, node);

			if (!isAddToContent(renderType)) {
				StringBuilder head = new StringBuilder();
				// Settings have to go before Scripts
				head.append(alohaCSSFile);
				head.append(alohaSettings);
				head.append(alohaScripts);

				renderResult.setParameter(SCRIPT_INCLUDES, head.toString());
				renderResult.setParameter(ALOHA_SETTINGS_PARAMETER, alohaSettings);
				renderResult.setParameter(ALOHA_SCRIPTS_PARAMETER, alohaScripts);

				return template;
			}

			template = setAlohaConfiguration(template, alohaSettings, alohaCSSFile + "\n" + alohaScripts);

			// We want to add a html doctype when aloha is being used (when scripts are included)
			if (ObjectTransformer.getBoolean(renderType.getParameter(ADD_HTML5_DOCTYPE), true)) {
				template = StringUtils.insertHtml5DocType(template);
			}
		}

		return removeAlohaPlaceHolders(template);
	}

	/**
	 * Removes aloha place holders.
	 * @param template
	 * @return
	 */
	private String removeAlohaPlaceHolders(String template) {
		return setAlohaPlaceHolders(template, EMPTY_STRING, EMPTY_STRING);
	}

	/**
	 * Adds aloha configuration to the template and returns it.
	 * @param template
	 * @param alohaSettings
	 * @param alohaScripts
	 * @return
	 */
	private String setAlohaConfiguration(String template, String alohaSettings, String alohaScripts) {
		String alohaConfPlaceHolders = ALOHA_SETTINGS_PLACE_HOLDER + "\n" + ALOHA_SCRIPTS_PLACE_HOLDER;

		if (existAlohaPlaceHolder(template, ALOHA_SCRIPTS_PLACE_HOLDER) && !existAlohaPlaceHolder(template, ALOHA_SETTINGS_PLACE_HOLDER)) {
			template = template.replace(ALOHA_SCRIPTS_PLACE_HOLDER, alohaConfPlaceHolders);
		} else if (!existAlohaPlaceHolder(template, ALOHA_SCRIPTS_PLACE_HOLDER) && existAlohaPlaceHolder(template, ALOHA_SETTINGS_PLACE_HOLDER)) {
			template = template.replace(ALOHA_SETTINGS_PLACE_HOLDER, alohaConfPlaceHolders);
		} else if (!existAlohaPlaceHolder(template, ALOHA_SCRIPTS_PLACE_HOLDER) && !existAlohaPlaceHolder(template, ALOHA_SETTINGS_PLACE_HOLDER)) {
			template = StringUtils.insertHtmlIntoHead(template, alohaConfPlaceHolders);
		}

		return setAlohaPlaceHolders(template, alohaSettings, alohaScripts);
	}


	/**
	 * Checks whether the script includes shall be added to the content.
	 * @param renderType
	 * @return
	 */
	private boolean isAddToContent(RenderType renderType) {
		return ObjectTransformer.getBoolean(renderType.getParameter(ADD_SCRIPT_INCLUDES), true);
	}

	/**
	 * Checks whether aloha settings and script includes need to be generated
	 * @param renderType
	 * @return flag
	 */
	private boolean isRenderSettings(RenderType renderType) {
		return ObjectTransformer.getBoolean(renderType.getParameter(RENDER_SETTINGS), true);
	}

	/**
	 * Checks whether editables shall be replaced
	 * @param renderType rendertype
	 * @return flag
	 */
	private boolean isReplaceEditables(RenderType renderType) {
		return ObjectTransformer.getBoolean(renderType.getParameter(REPLACE_EDITABLES), true);
	}

	/**
	 * Adds aloha settings and script to template.
	 * @param template
	 * @param settings
	 * @param scripts
	 * @return
	 */
	private String setAlohaPlaceHolders(String template, String settings, String scripts) {
		return template
				.replace(ALOHA_SETTINGS_PLACE_HOLDER, settings)
				.replace(ALOHA_SCRIPTS_PLACE_HOLDER, scripts);
	}

	/**
	 * Checks if aloha settings are declared in the template.
	 * @param template
	 * @return
	 */
	private boolean existAlohaPlaceHolder(String template, String placeHolder) {
		return template.contains(placeHolder);
	}

	/**
	 * Gets aloha CSS files
	 * @param t
	 * @param node
	 * @return
	 * @throws TransactionException
	 */
	private String getAlohaCSSFiles(Transaction t, Node node) throws TransactionException {
		return String.format("<link rel=\"stylesheet\" href=\"%s/css/aloha.css\" />", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM));
	}

	/**
	 * Gets aloha scripts files
	 * @param t
	 * @param node
	 * @return
	 * @throws NodeException
	 */
	private String getAlohaScriptFiles(Transaction t, Node node) throws NodeException {
		String alohaPlugins = getAlohaPlugins(node);

		StringBuilder scripts = new StringBuilder();

		scripts.append("<script type=\"text/javascript\" src=\"/alohaeditor/gcmsui-scripts-launcher.js\"></script>\n");

		if (StringUtils.isEqual(System.getProperty(BUILD_TIMESTAMP), "DEV")) {
			scripts.append(String.format("<script src=\"%s/lib/require.js\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM)));
			scripts.append(String.format("<script src=\"%s/lib/vendor/jquery-3.7.0.js\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM)));
//			scripts.append(String.format("<script src=\"%s/lib/vendor/jquery-1.7.2.js\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM)));
			scripts.append(String.format("<script src=\"%s/lib/vendor/jquery.layout.js\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM)));
			scripts.append(String.format("<script src=\"%s/lib/aloha-jquery-noconflict.js\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM)));
			scripts.append(String.format("<script src=\"%s/lib/aloha.js\" data-aloha-plugins=\"%s\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM), alohaPlugins));
		} else {
			scripts.append(String.format("<script src=\"%s/lib/aloha.js\" data-aloha-plugins=\"%s\"></script>\n", System.getProperty(ALOHA_EDITOR_BASE_URL_PARAM), alohaPlugins));
		}

		return scripts.toString();
	}

	/**
	 * Gets aloha settings.
	 * @param renderResult
	 * @param renderType
	 * @param node
	 * @return
	 * @throws NodeException
	 */
	private String getAlohaSettings(RenderResult renderResult,
			RenderType renderType, Node node) throws NodeException  {
		try {
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();

			JsonGenerator jg = new JsonFactory().createJsonGenerator(sw);

			jg.useDefaultPrettyPrinter();
			mapper.writeValue(jg, getAlohaSettings(node, renderResult, renderType, mapper));

			return "<script type=\"text/javascript\">\n" + "Aloha = {}; Aloha.settings = " + sw.toString() + ";" + "\n</script>";
		} catch (Exception e) {
			throw new NodeException("Error while writing aloha settings", e);
		}
	}

	/**
	 * Adds <code>aloha-block</code> to the <code>class</code> attribute of the tag.
	 *
	 * @param startTag The tag to be modified.
	 * @return The <code>startTag</code> with <code>aloha-block</code> added to the
	 *		<code>class</code> attribute.
	 */
	private String checkAlohaBlockClass(String startTag, String rootTagName, boolean needAlohaBlock) {
		Matcher classMatcher = classPattern.matcher(startTag);

		if (classMatcher.find()) {
			// we already have classes, append our classes
			StringBuffer buildNewStartTag = new StringBuffer();

			buildNewStartTag.append(startTag.substring(0, classMatcher.start(1)));

			// Remove already existing aloha-block classes and add "aloha-block"
			String classes = classMatcher.group(1).replaceAll(ALOHA_BLOCK_CLASSES_REGEX, "");

			if (needAlohaBlock) {
				classes += " aloha-block";
			}

			buildNewStartTag.append(classes.trim());
			buildNewStartTag.append(startTag.substring(classMatcher.end(1)));

			return buildNewStartTag.toString();
		} else if (needAlohaBlock){
			// we have no classes
			return startTag.replaceFirst(rootTagName, rootTagName + " class=\"aloha-block\"");
		}

		// No classes present, and it is a valid link tag. Nothing to do.
		return startTag;
	}

	/**
	 * Makes an Aloha block of the given source code.<br /><br />
	 *
	 * If the block has a supported root element it will be used as the
	 * block root element, otherwise the code is surrounded with a div
	 * element which is used as block root element.
	 *
	 * If the feature copy_tags is enabled for the node, the block root tag will
	 *
	 * @param code The html code of the block
	 * @param container The tag of the block
	 * @param renderResult The renderResult
	 * @return The updated code of the block
	 */
	public String block(String code, ParserTag parserTag, RenderResult renderResult) throws NodeException {
		long startTime = System.currentTimeMillis();
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		RenderType renderType = t.getRenderType();
		int editMode = renderType.getEditMode();
		Node node = null;
		com.gentics.contentnode.object.Page page = null;

		Object rootObject = renderType.getRenderedRootObject();

		if (rootObject instanceof com.gentics.contentnode.object.Page) {
			page = (com.gentics.contentnode.object.Page) rootObject;
			node = page.getFolder().getNode();
		}

		// render the tag as block if either aloha edit mode or aloha readonly (preview) and the feature copy_tags is on
		if (parserTag instanceof ContentTag) {
			ContentTag tag = (ContentTag) parserTag;
			Content content = (Content) tag.getContainer();

			if (editMode == RenderType.EM_ALOHA && tag.isEditable() && tag.isAlohaBlock() && content.getPages().contains(page)) {
				// save eventually contained plinks
				Map<String, String> savedPLinks = savePLinks(code);

				code = savedPLinks.get("code");

				String htmlId = "";
				boolean insertDiv = true;

				Matcher matcher = rootTagPattern.matcher(code);

				if (matcher.matches()) {
					String rootTagName = matcher.group(2); // name of the root tag
					String startTag = matcher.group(1); // the starting root tag
					String rest = matcher.group(3); // content and closing tag

					// we allow reusing the root tag, if it is among the allowed tags and if it is not the preview tag for a form
					if (allowedBlocks.contains(rootTagName) && !isFormsPreviewTag(startTag)) {
						// check if the opening root tag is the closing root tag
						Pattern tagPattern = Pattern.compile("<(/?)" + rootTagName);
						Matcher tagMatcher = tagPattern.matcher(code);
						int count = 0;
						boolean rootTag = true;

						while (tagMatcher.find()) {
							if (StringUtils.isEmpty(tagMatcher.group(1))) {
								count++;
							} else {
								count--;
							}
							if (count == 0) {
								if (tagMatcher.find()) {
									rootTag = false;
								}
								break;
							}
						}

						// if the opening tag is the real root tag we don't need to insert a div
						if (rootTag) {
							// For a real tag getConstruct() would not return null, but
							// some tests work with dummy tag implementations.
							boolean notMagicLink = !rootTagName.toLowerCase().equals("a") || !isMagicLinkConstruct(renderResult, tag.getConstruct(), node);

							insertDiv = false;

							// check for an id
							Matcher idMatcher = idPattern.matcher(startTag);

							if (idMatcher.find()) {
								htmlId = idMatcher.group(1);

								// check if the id we've found does not match an id we've generated previously
								String[] ids = (String[]) renderResult.getParameters().get(PARAM_BLOCK_HTML_IDS);

								if (ids != null) {
									for (int i = 0; i < ids.length; i++) {
										if (ids[i].equals(htmlId)) {
											// this is a block element id generated by a nested content tag
											// therefore we have to insert our own div
											insertDiv = true;
											break;
										}
									}
								}

								if (!insertDiv) {
									startTag = checkAlohaBlockClass(startTag, rootTagName, notMagicLink);
									startTag = startTag.replaceFirst(
										rootTagName,
										rootTagName + " " + renderTagAnnotations(page, tag));

									code = startTag + rest;
								}
							} else {
								htmlId = BLOCK_PREFIX + tag.getId();
								String newStartTag = startTag.replaceFirst(rootTagName, rootTagName + " id=\"" + htmlId + "\"");

								newStartTag = checkAlohaBlockClass(newStartTag, rootTagName, notMagicLink);
								newStartTag = newStartTag.replaceFirst(
									rootTagName,
									rootTagName + " " + renderTagAnnotations(page, tag));

								code = newStartTag + rest;
							}
						}
					}
				}

				String tagName = null;

				if (insertDiv) {
					htmlId = BLOCK_PREFIX + tag.getId();

					// check for liveedit_per_construct setting to choose tagname
					if (TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getFeature("liveedit_tag_perconstruct")) {
						tagName = tag.getConstruct().getLiveEditorTagName();
					}

					// if no tag name has been defined so far we default to div
					if (tagName == null || "".equals(tagName)) {
						tagName = "div";
					}

					code = "<" + tagName + " " + renderTagAnnotations(page, tag) + " class=\"aloha-block\" id=\"" + htmlId + "\">" + code + "</" + tagName + ">";
				}

				// Add the HTML id and tag id of this block to the render result.
				// IDs are meant to be unique, so we only add them once, even if the block is
				// encountered multiple times.
				String tagId = tag.getId().toString();

				if (!ArrayUtils.contains(renderResult.getParameters().get(PARAM_BLOCK_HTML_IDS), htmlId)
						&& !ArrayUtils.contains(renderResult.getParameters().get(PARAM_BLOCK_TAG_IDS), tagId)) {
					renderResult.addParameter(PARAM_BLOCK_HTML_IDS, htmlId);
					renderResult.addParameter(PARAM_BLOCK_TAG_IDS, tagId);
				}

				// finally restore the plinks
				code = restorePLinks(code, savedPLinks);
			} else if (editMode == RenderType.EM_ALOHA_READONLY && prefs.isFeature(Feature.COPY_TAGS, node)) {
				Matcher matcher = rootTagPattern.matcher(code);

				if (matcher.matches()) {
					String rootTagName = matcher.group(2); // name of the root tag
					String startTag = matcher.group(1); // the starting root tag
					String rest = matcher.group(3); // content and closing tag

					// check if the opening root tag is the closing root tag
					Pattern tagPattern = Pattern.compile("<(/?)" + rootTagName);
					Matcher tagMatcher = tagPattern.matcher(code);
					int count = 0;
					boolean rootTag = true;

					while (tagMatcher.find()) {
						if (StringUtils.isEmpty(tagMatcher.group(1))) {
							count++;
						} else {
							count--;
						}
						if (count == 0) {
							if (tagMatcher.find()) {
								rootTag = false;
							}
							break;
						}
					}

					if (rootTag) {
						// annotate the tag
						startTag = startTag.replaceFirst(rootTagName, rootTagName + " " + renderTagAnnotations(page, tag));
						code = startTag + rest;
					}
				}
			}

			if (tag.getName().equals(renderType.getParameter(MARK_TAG))) {
				code = String.format("<gtxtag %s>%s</gtxtag %s>", tag.getName(), code, tag.getName());
		}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Aloha Renderer needed " + (System.currentTimeMillis() - startTime) + " ms to build a block of the input");
		}

		return code;
	}

	/**
	 * Looks for all &lt;gtxEditable&gt; elements and checks for a proper html parent element.<br /><br />
	 *
	 * If the gtxEditable has a direct parent (no content in between the parent and the gtxEditable)
	 * element with a proper type as defined in {@link #allowedEditables}
	 * this element will be used as editable.<br /><br />
	 *
	 * If the gtxEditable has no direct parent, a &lt;div&gt; element will be inserted as editable element.
	 *
	 * @param template The markup in which the replacement should be performed
	 * @return The final result after replacing the &lt;gtxEditable&gt; tags
	 * @throws Exception in case of problems with the transaction
	 */
	public static String replaceEditables(String template, RenderResult renderResult) throws Exception {
		StringBuilder templateBuilder = new StringBuilder(template);

		// Regular expression for finding editables and a surrounding tag
		String editableExpression = "((<([a-zA-Z0-9]*)\\b[^>]*>)?" + // possible tag before the editable
				"<gtxEditable\\s([0-9a-zA-Z_]+)>" + // followed by the editable
				"(.*?)" + // anything in the editable
				"</gtxEditable\\s\\4>" + // end of the editable
				"(</\\3\\s*>)?)"; // possible tag after the editable

		Pattern editablePattern = Pattern.compile(editableExpression, Pattern.DOTALL | Pattern.COMMENTS);

		long startTime = System.currentTimeMillis();
		Matcher matcher = editablePattern.matcher(template);

		// collection of html id's of blocks
		Collection<?> blockHTMLIds = ObjectTransformer.getCollection(renderResult.getParameters().get(PARAM_BLOCK_HTML_IDS), Collections.EMPTY_LIST);

		Transaction t = TransactionManager.getCurrentTransaction();
		// shall we annotate the editables with classes?
		boolean annotate = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ALOHA_ANNOTATE_EDITABLES);

		while (matcher.find()) {
			// The complete opening tag
			String openingTag = matcher.group(2);
			// The tag name of the opening tag
			String openingTagName = matcher.group(3);
			// The content of the gtxEditable
			String content = matcher.group(5);
			// The id of the gtxEditable
			String editableId = matcher.group(4);
			// parse the editable id as integer
			int parsedEditableId = ObjectTransformer.getInt(editableId, -1);

			// make a "clean" copy of the editableId String
			// currently, the editableId String would reference the original character array of the template.
			// since we modify the template on each iteration, we would create (and keep in memory) a new copy
			// of the template in each iteration, which easily could lead to OOM errors
			if (parsedEditableId > 0) {
				editableId = Integer.toString(parsedEditableId);
			} else {
				editableId = new String(editableId);
			}

			// if the id is not a number, the editable is a meta editable
			boolean metaEditable = parsedEditableId < 0;

			// The complete closing tag if it is the same as the opening tag
			String closingTag = matcher.group(6);

			String htmlId;
			String replacement = "";
			String tagName = "div";

			// determine the construct, part and tag of the editable (if not a meta editable)
			Construct construct = null;
			Part part = null;
			PartType partType = null;
			Tag tag = null;
			Value value = null;

			if (!metaEditable) {
				value = t.getObject(Value.class, parsedEditableId);
				if (value != null) {
					part = value.getPart();
					partType = value.getPartType();
					construct = part.getConstruct();
					ValueContainer valueContainer = value.getContainer();

					if (valueContainer instanceof Tag) {
						tag = (Tag) valueContainer;
					}
				}
			}

			// check for liveedit_per_construct setting to retrieve tag specific html tag setting
			if (t.getRenderType().getPreferences().getFeature("liveedit_tag_perconstruct") && !metaEditable && construct != null) {
				tagName = construct.getLiveEditorTagName();
				if (tagName == null || "".equals(tagName)) {
					tagName = "div";
				}
			}

			// Whether or not the matched editable has a parent node that can
			// be used as the editable's contenteditable container.
			boolean hasEditableRoot = !StringUtils.isEmpty(openingTag) && !StringUtils.isEmpty(closingTag)
					&& allowedEditables.contains(openingTagName.toLowerCase());

			// If the editable does not have a parent node, or if its parent
			// node is not one of the `allowedEditables', then wrap the
			// editable (`tagName') with an element that will be used as its
			// contenteditable container.
			if (!hasEditableRoot) {
				htmlId = (metaEditable ? MetaEditableRenderer.EDITABLE_PREFIX : AlohaRenderer.EDITABLE_PREFIX) + editableId;
				String classesAttr = "";

				if (annotate) {
					String classes = renderTagClasses(construct, part, partType, tag);

					if (!ObjectTransformer.isEmpty(classes)) {
						classesAttr = " class=\"" + classes + "\"";
					}
				}
				if (openingTag != null && closingTag != null) {
					replacement = openingTag + "<" + tagName + classesAttr + " id=\"" + htmlId + "\">" + content + "</" + tagName + ">" + closingTag;
				} else if (openingTag != null) {
					replacement = openingTag + "<" + tagName + classesAttr + " id=\"" + htmlId + "\">" + content + "</" + tagName + ">";
				} else {
					replacement = "<" + tagName + classesAttr + " id=\"" + htmlId + "\">" + content + "</" + tagName + ">";
				}
			} else {
				// we have a valid root tag so we check if it contains an id
				String newOpeningTag = "";

				Matcher idMatcher = idPattern.matcher(openingTag);

				if (idMatcher.find()) {
					// we have an id
					htmlId = idMatcher.group(1);
					newOpeningTag = openingTag;
				} else {
					// We have no id
					htmlId = (metaEditable ? MetaEditableRenderer.EDITABLE_PREFIX : AlohaRenderer.EDITABLE_PREFIX) + editableId;
					newOpeningTag = openingTag.replaceFirst(openingTagName, openingTagName + " id=\"" + htmlId + "\"");
				}

				// add annotation classes, if necessary
				if (annotate) {
					String classes = renderTagClasses(construct, part, partType, tag);

					if (!ObjectTransformer.isEmpty(classes)) {
						Matcher classMatcher = classPattern.matcher(newOpeningTag);

						if (classMatcher.find()) {
							// we already have classes, append our classes
							StringBuffer buildNewOpeningTag = new StringBuffer();

							buildNewOpeningTag.append(newOpeningTag.substring(0, classMatcher.end(1)));
							buildNewOpeningTag.append(" ").append(classes);
							buildNewOpeningTag.append(newOpeningTag.substring(classMatcher.end(1)));

							newOpeningTag = buildNewOpeningTag.toString();
						} else {
							// we have no classes
							newOpeningTag = newOpeningTag.replaceFirst(openingTagName, openingTagName + " class=\"" + classes + "\"");
						}
					}
				}

				// check whether the found htmltag denotes a block (and if yes, don't use it)
				if (blockHTMLIds.contains(htmlId)) {
					// the tag around the editable is a block, so it is not usable as editable tag
					htmlId = (metaEditable ? MetaEditableRenderer.EDITABLE_PREFIX : AlohaRenderer.EDITABLE_PREFIX) + editableId;
					String classesAttr = "";

					if (annotate) {
						String classes = renderTagClasses(construct, part, partType, tag);

						if (!ObjectTransformer.isEmpty(classes)) {
							classesAttr = " class=\"" + classes + "\"";
						}
					}
					replacement = openingTag + "<" + tagName + classesAttr + " id=\"" + htmlId + "\">" + content + "</" + tagName + ">" + closingTag;
				} else {
					replacement = newOpeningTag + content + closingTag;
				}
			}

			if (!metaEditable) {
				// add the currently processed editable to the editables
				renderResult.addParameter(PARAM_EDITABLE_HTML_IDS, htmlId);
				renderResult.addParameter(PARAM_EDITABLE_VALUE_IDS, editableId);
			}

			// replace the current find with the replacement and reset the matcher
			templateBuilder.replace(matcher.start(), matcher.end(), replacement);
			matcher.reset(templateBuilder.toString());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("AlohaRenderer needed " + (System.currentTimeMillis() - startTime) + " ms to replace editables");
		}

		return templateBuilder.toString();
	}

	protected Map<String, String> savePLinks(String code) {
		Matcher m = plinkPattern.matcher(code);
		Map<String, String> foundPLinks = new HashMap<String, String>();
		int plinkCounter = 0;

		while (m.find()) {
			String replacement = "###PLINK###" + plinkCounter + "###PLINK###";

			foundPLinks.put(replacement, m.group());
			code = m.replaceFirst(replacement);
			plinkCounter++;
			m = plinkPattern.matcher(code);
		}
		foundPLinks.put("code", code);

		return foundPLinks;
	}

	protected String restorePLinks(String code, Map<String, String> savedPLinks) {
		for (Iterator<Map.Entry<String, String>> iterator = savedPLinks.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, String> entry = iterator.next();

			if (!StringUtils.isEqual(entry.getKey(), "code")) {
				code = code.replaceAll(entry.getKey(), entry.getValue());
			}
		}

		return code;
	}

	/**
	 * Get the aloha plugins, configured for the specific node, in the format for loading plugins using require.
	 * The order is also defined in node.conf.
	 * @param node node
	 * @return aloha plugins to be loaded
	 * @throws NodeException
	 */
	protected String getAlohaPlugins(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		Set<String> pluginSet = new LinkedHashSet<>();

		// add the default plugins
		pluginSet.add("gcn/gcn");

		// add additional plugins
		for (AlohaPluginService service : alohaPluginServiceLoader) {
			pluginSet.addAll(service.getPlugins(node));
		}

		String[] nodePlugins = null;
		Collection<String> nodePluginsColl = prefs.getPropertyObject(node, "aloha_plugins_node");
		if (!ObjectTransformer.isEmpty(nodePluginsColl)) {
			nodePlugins = nodePluginsColl.toArray(new String[nodePluginsColl.size()]);
		}
		String[] globalPlugins = t.getNodeConfig().getDefaultPreferences().getProperties("aloha_plugins_global");

		// check whether node specific plugins were configured
		if (nodePlugins != null) {
			for (String plugin : nodePlugins) {
				pluginSet.add(plugin);
			}
		} else if (globalPlugins != null) {
			for (String plugin : globalPlugins) {
				pluginSet.add(plugin);
			}
			}

		return StringUtils.merge((String[]) pluginSet.toArray(new String[pluginSet.size()]), ",");
	}

	/**
	 * Render the space separated list of class names that annotate the editable
	 * @param construct construct
	 * @param part part
	 * @param partType part type
	 * @param tag tag
	 * @return space separated list of class names
	 */
	protected static String renderTagClasses(Construct construct, Part part, PartType partType, Tag tag) {
		StringBuffer classes = new StringBuffer();

		if (construct != null) {
			classes.append(CONSTRUCT_PREFIX).append(construct.getKeyword());
		}
		if (tag != null) {
			if (classes.length() > 0) {
				classes.append(" ");
			}
			classes.append(TAGNAME_PREFIX).append(tag.getName());
		}
		if (part != null) {
			if (!ObjectTransformer.isEmpty(part.getKeyname())) {
				if (classes.length() > 0) {
					classes.append(" ");
				}
				classes.append(TAGPART_PREFIX).append(part.getKeyname());
			}
		}
		if (partType != null) {
			if (classes.length() > 0) {
				classes.append(" ");
			}
			classes.append(PARTTYPE_PREFIX).append(partType.getAnnotationClass());
		}
		return classes.toString();
	}

	/**
	 * Render the data- attribute annotations for the given tag
	 * @param page page containing the tag
	 * @param tag tag
	 * @return data- attribute annotations
	 * @throws NodeException
	 */
	protected String renderTagAnnotations(com.gentics.contentnode.object.Page page, Tag tag) throws NodeException {
		StringBuffer annotations = new StringBuffer();

		if (page != null) {
			annotations.append("data-gcn-pageid=\"").append(page.getId()).append("\"");
		}
		if (tag != null) {
			if (annotations.length() > 0) {
				annotations.append(" ");
			}
			annotations.append("data-gcn-tagid=\"").append(tag.getId()).append("\"");
			annotations.append(" data-gcn-tagname=\"").append(tag.getName()).append("\"");

			Construct construct = tag.getConstruct();
			if (construct != null) {
				annotations.append(" data-gcn-i18n-constructname=\"").append(StringUtils.escapeXML(construct.getName().toString())).append("\"");
			}
		}
		return annotations.toString();
	}

	/**
	 * Check whether the given construct is the magic link construct for the node
	 * @param renderResult render result
	 * @param construct construct
	 * @param node node
	 * @return true iff the construct is the magic link construct
	 * @throws NodeException
	 */
	protected boolean isMagicLinkConstruct(RenderResult renderResult, Construct construct, Node node) throws NodeException {
		if (construct == null) {
			return false;
		}

		String[] magicLinkConstructName = renderResult.getParameters().get(PARAM_MAGICLINK_CONSTRUCT);
		if (magicLinkConstructName == null) {
			renderResult.addParameter(PARAM_MAGICLINK_CONSTRUCT, getMagicLinkConstructKeyword(node));
			magicLinkConstructName = renderResult.getParameters().get(PARAM_MAGICLINK_CONSTRUCT);
		}

		return ObjectTransformer.equals(magicLinkConstructName[0], construct.getKeyword());
	}
}
