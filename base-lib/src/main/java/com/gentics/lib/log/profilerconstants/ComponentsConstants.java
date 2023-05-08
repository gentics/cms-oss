package com.gentics.lib.log.profilerconstants;

public abstract class ComponentsConstants {

	public final static String GENTICSROOT = "Gentics";
    
	public final static String PORTLETCLASS_PROCESSACTION = "Portlet.processAction";
	public final static String PLUGINCLASS_PROCESSACTION = "Plugin.processAction";

	public final static String PORTLET_PROCESSACTION = PORTLETCLASS_PROCESSACTION + "/instance";
	public final static String PLUGIN_PROCESSACTION = PLUGINCLASS_PROCESSACTION + "/instance";

	public final static String PORTLETCLASS_RENDER = "Portlet.render";
	public final static String PLUGINCLASS_RENDER = "Plugin.render";

	public final static String PORTLET_RENDER = PORTLETCLASS_RENDER + "/instance";
	public final static String PLUGIN_RENDER = PLUGINCLASS_RENDER + "/instance";

	public final static String PORTALEVENTBROKER_DISTRIBUTEEVENT = "PortalEventBroker.distributeEvent";
	public final static String PLUGINEVENTBROKER_DISTRIBUTEEVENT = "PluginEventBroker.distributeEvent";
	public final static String PORTALEVENTBROKER_DISTRIBUTEEVENT_HANDLEEVENT = PORTALEVENTBROKER_DISTRIBUTEEVENT + "/handleEvent";
	public final static String PLUGINEVENTBROKER_DISTRIBUTEEVENT_HANDLEEVENT = PLUGINEVENTBROKER_DISTRIBUTEEVENT + "/handleEvent";
	public final static String VIEWPLUGIN_ACTIONINVOKER_INVOKE = "ViewPlugin/ActionInvoker.invoke";
	public final static String VIEWPLUGIN_ACTIONINVOKER_INVOKE_COMPONENT = "ViewPlugin/ActionInvoker.invoke/Component";
	public final static String VIEWPLUGIN_VIEW_ONVIEW = "ViewPlugin/View.onView";
	public final static String VIEWPLUGIN_VIEW_ONLOAD = "ViewPlugin/View.onLoad";
	public final static String VIEWPLUGIN_RENDER_GETOUTPUT = "ViewPlugin.render/getOutput";
	public final static String FORMPLUGIN2_COMPONENT_RENDER = "FormPlugin2/Component.render";
	public final static String FORMPLUGIN2_PROCESSACTION = "FormPlugin2.processAction";
	public final static String FORMPLUGIN2_PROCESSACTION_LOADPARAMETERVALUES = FORMPLUGIN2_PROCESSACTION + "/loadParameterValues";
	public final static String FORMPLUGIN2_PROCESSACTION_ONLOADFINISHED = FORMPLUGIN2_PROCESSACTION + "/onLoadFinished";
	public final static String FORMPLUGIN2_PROCESSACTION_PROCESSFORMACTIONS = FORMPLUGIN2_PROCESSACTION + "/processFormActions";
	public final static String TEMPLATEHELPER_FILLTEMPLATE = "TemplateHelper.fillTemplate";
	public final static String I18NSTRING_TOSTRING = "I18nString.toString";
	public final static String TEMPLATEENGINE2_VELOCITYTEMPLATEPROCESSOR_TRANSLATETEMPLATESOURCE = "TemplateEngine2/VelocityTemplateProcessor.translateTemplateSource";
	public final static String TEMPLATEENGINE2_VELOCITYTEMPLATEPROCESSOR_IMPPROVIDER_GET = "TemplateEngine2/ImpProvider.get";
	public final static String TEMPLATEENGINE2_VELOCITYTEMPLATEPROCESSOR_EVALUATE = "TemplateEngine2/VelocityTemplateProcessor.translate/evaluate";
	public final static String TEMPLATEENGINE2_VELOCITYTEMPLATEPROCESSOR_EVALUATE_PARSE = "TemplateEngine2/VelocityTemplateProcessor.translate/evaluate/parse";
	public final static String TEMPLATEENGINE2_VELOCITYTEMPLATEPROCESSOR_EVALUATE_RENDER = "TemplateEngine2/VelocityTemplateProcessor.translate/evaluate/render";
	public final static String TEMPLATEENGINE2_TEMPLATEMANAGER_GETTEMPLATE = "TemplateEngine2/TemplateManager.getTemplate";

	public final static String DEFAULTRULETREE_PARSE = "DefaultRuleTree.parse";
	
	public final static String VIEWPLUGIN_RELOADVIEWS = "ViewPlugin.reloadViewsFromFiles";
	public final static String VIEWPLUGIN_CHECKFILECHANGES = "ViewPlugin.checkFileChanges";
	public final static String VIEWPLUGIN_INITVIEWS = "ViewPlugin.generateViews";
    
	public final static String PORTLETURL = "PortletURL";
	public final static String PORTLETURL_BEAUTIFUL = "PortletURL/BeautifulURLs";
	public final static String PORTLETURL_BEAUTIFUL_GENERATE = "PortletURL/BeautifulURLs/generate";
	public final static String PORTLETURL_BEAUTIFUL_PARSE = "PortletURL/BeautifulURLs/parse";
    
	/**
	 * @see com.gentics.lib.db.DB#query(String, Object[], ResultProcessor)
	 */
	public final static String DB_QUERY_WITHOUT_HANDLE = "DB.query_without_handle";

	/**
	 * @see com.gentics.lib.db.DB#query(DBHandle, String, Object[], ResultProcessor)
	 */
	public final static String DB_QUERY_WITH_HANDLE = "DB.query_with_handle";

	/**
	 * @see com.gentics.lib.db.DB#update(String, Object[], UpdateProcessor)
	 */
	public final static String DB_UPDATE_WITH_HANDLE = "DB.update_with_handle";
	
	/**
	 * @see com.gentics.lib.db.DB#update(DBHandle, String, Object[], UpdateProcessor)
	 */
	public static final String DB_UPDATE_WITHOUTHANDLE = "DB.update_without_handle";
	
	public static final String DATASOURCE = "Datasource";
	
	public static final String DATASOURCE_CN = DATASOURCE + "/CNDatasource";

	/**
	 * @see com.gentics.lib.datasource.CNDatasource#getResult(int, int, String, int, int)
	 */
	public static final String DATASOURCE_CN_GETRESULT = DATASOURCE_CN + ".getResult";

	/**
	 * @see com.gentics.lib.datasource.CNDatasource#getCount(int)
	 */
	public static final String DATASOURCE_CN_GETCOUNT = DATASOURCE_CN + ".getCount";

	/**
	 * @see com.gentics.lib.datasource.CNWriteableDatasource#update(Collection, GenticsUser)
	 */
	public static final String DATASOURCE_CN_UPDATE = DATASOURCE_CN + ".update";

	/**
	 * @see com.gentics.lib.datasource.CNWriteableDatasource#store(DatasourceRecordSet, GenticsUser)
	 */
	public static final String DATASOURCE_CN_STORE = DATASOURCE_CN + ".store";

	/**
	 * @see com.gentics.lib.datasource.CNWriteableDatasource#insert(DatasourceRecordSet, GenticsUser)
	 */
	public static final String DATASOURCE_CN_INSERT = DATASOURCE_CN + ".insert";

	/**
	 * @see com.gentics.lib.datasource.CNWriteableDatasource#delete(DatasourceRecordSet, GenticsUser)
	 */
	public static final String DATASOURCE_CN_DELETE = DATASOURCE_CN + ".delete";
	
	public static final String DATASOURCE_LDAP = DATASOURCE + "/LDAPDatasource";

	/**
	 * @see com.gentics.lib.datasource.LDAPDatasource#getResult(int, String[], String[], int, String, int, Map)
	 */
	public static final String DATASOURCE_LDAP_GETRESULT = DATASOURCE_LDAP + ".getResult";

	/**
	 * @see com.gentics.lib.datasource.LDAPDatasource#getCount2()
	 */
	public static final String DATASOURCE_LDAP_GETCOUNT = DATASOURCE_LDAP + ".getCount";
	
	public static final String GENTICSCONTENTOBJECT = "GenticsContentObject";
	
	/**
	 * @see com.gentics.lib.content.GenticsContentObjectImpl#initialize(DBHandle, RenderResult)
	 */
	public static final String GENTICSCONTENTOBJECT_INITIALIZE = GENTICSCONTENTOBJECT + ".initialize";
	public static final String GENTICSCONTENTATTRIBUTE = "GenticsContentAttribute";

	/**
	 * @see com.gentics.lib.content.GenticsContentAttributeImpl#initialize()
	 */
	public static final String GENTICSCONTENTATTRIBUTE_INITIALIZE = GENTICSCONTENTATTRIBUTE + ".initialize";
	
	/**
	 * @see com.gentics.api.lib.resolving.PropertyResolver
	 */
	public static final String PROPERTY_RESOLVER = "PropertyResolver";

	/**
	 * @see com.gentics.api.lib.resolving.PropertyResolver#resolve(String, boolean)
	 */
	public static final String PROPERTY_RESOLVER_RESOLVE = PROPERTY_RESOLVER + ".resolve";

	/**
	 * @see com.gentics.portalnode.templateengine.templateprocessors.VelocityTemplateProcessor.ImpProvider#get(String)
	 */
	public static final String VELOCITYTEMPLATEPROCESSOR_IMPPROVIDER_GET = "VelocityTemplate.ImpProvider.get";
	
	public static final String PORTALCACHE = "PortalCache";
	public static final String PORTALCACHE_JCS = PORTALCACHE + "/JCS";

	/**
	 * @see com.gentics.lib.cache.JCSPortalCache#get(Object)
	 */
	public static final String PORTALCACHE_JCS_GET = PORTALCACHE_JCS + ".get";

	/**
	 * @see com.gentics.lib.cache.JCSPortalCache#put(Object, Object)
	 */
	public static final String PORTALCACHE_JCS_PUT = PORTALCACHE_JCS + ".put";

	public static final String EXPRESSIONPARSER = "ExpressionParser";
	public static final String EXPRESSIONPARSER_PARSE = EXPRESSIONPARSER + ".parse";
	public static final String EXPRESSIONPARSER_CNDATASOURCEFILTER = EXPRESSIONPARSER + ".createCNDatasourceFilter";
	public static final String EXPRESSIONPARSER_LDAPATASOURCEFILTER = EXPRESSIONPARSER + ".createLDAPDatasourceFilter";
	public static final String EXPRESSIONPARSER_EVALUATION = EXPRESSIONPARSER + ".evaluate";
	public static final String EXPRESSIONPARSER_ASSIGNMENT = EXPRESSIONPARSER + ".assign";
    
	public static final String DEBUGVELOCITY = "DebugVelocity";
	public static final String DEBUGVELOCITY_GETPROPERTY = DEBUGVELOCITY + ".getProperty";
	public static final String DEBUGVELOCITY_INVOKEMETHOD = DEBUGVELOCITY + ".invokeMethod";
    
	public static final String PLUGGABLEACTIONINVOKER = "PluggableAction.invoke";
    
	public static final String IMPORTEXPORT = "ImportExport";
	public static final String STRUCTURECOPY = "StructureCopy";
	public static final String COPYCONTROLLER = "CopyController";
	public static final String COPYCONTROLLER_FIRSTRUN = COPYCONTROLLER + ".firstRun";
	public static final String COPYCONTROLLER_SECONDRUN = COPYCONTROLLER + ".secondRun";
	public static final String COPYCONTROLLER_THIRDRUN = COPYCONTROLLER + ".thirdRun";
	public static final String IMPORTEXPORT_IMPORTCONTROLLER = IMPORTEXPORT + ".ImportController";
	public static final String IMPORTEXPORT_EXPORTCONTROLLER = IMPORTEXPORT + ".ExportController";
	public static final String IMPORTEXPORT_EXPORTCONTROLLER_COPYOBJECT = IMPORTEXPORT_EXPORTCONTROLLER + ".copyObject";
	public static final String IMPORTEXPORT_IMPORTCONTROLLER_COPYOBJECT = IMPORTEXPORT_IMPORTCONTROLLER + ".copyObject";
	public static final String IMPORTEXPORT_EXPORTCONTROLLER_ZIP = IMPORTEXPORT_EXPORTCONTROLLER + ".zip";
	public static final String IMPORTEXPORT_IMPORTCONTROLLER_UNZIP = IMPORTEXPORT_IMPORTCONTROLLER + ".unzip";
	public static final String IMPORTEXPORT_EXPORTCONTROLLER_GETOBJECTSTRUCTURE = IMPORTEXPORT_EXPORTCONTROLLER + ".getObjectStructure";
	public static final String IMPORTEXPORT_IMPORTCONTROLLER_GETOBJECTSTRUCTURE = IMPORTEXPORT_IMPORTCONTROLLER + ".getObjectStructure";

	public static final String XNLPARSER = "XnlParser";
	public static final String XNLPARSER_FINDTAGS = XNLPARSER + "/findTags";
	public static final String XNLPARSER_BUILDSTRUCTURE = XNLPARSER + "/buildStructure";
	public static final String XNLPARSER_PARSESTRUCTURE = XNLPARSER + "/parseStructure";
}
