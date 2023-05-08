package com.gentics.lib.log.profilerconstants;

public abstract class PortalLifecycleConstants {
    
	public final static String PORTALSERVLET_INIT = ComponentsConstants.GENTICSROOT + "/PortalServlet.init";
	public final static String PORTALSERVLET_DO = ComponentsConstants.GENTICSROOT + "/PortalServlet.do";

	public final static String PORTALSERVLET_PROCESS = PORTALSERVLET_DO + "/process";
	public final static String PORTALWRAPPER_DOLIFECYCLE = PORTALSERVLET_PROCESS + "/PortalWrapper.doLifeCycle";
	public final static String PORTALWRAPPER_SYNCWAIT = PORTALWRAPPER_DOLIFECYCLE + "/syncwait";
	public final static String PORTALWRAPPER_DOAUTH = PORTALWRAPPER_DOLIFECYCLE + "/doAuth";
	public final static String PORTAL_DOLIFECYCLE = PORTALWRAPPER_DOLIFECYCLE + "/Portal.doLifeCycle";

	public final static String PORTAL_PARSEREQUEST = PORTAL_DOLIFECYCLE + "/parseRequest";
	public final static String PORTAL_DOINIT = PORTAL_DOLIFECYCLE + "/doInit";
	public final static String PORTAL_UPDATEMODULEREQ = PORTAL_DOINIT + "/updateModuleRequests";
	public final static String PORTAL_ON_TEMPLATE_CHANGE = PORTAL_UPDATEMODULEREQ + "/onTemplateChange";
	public final static String PORTAL_ON_LOGIN = PORTAL_UPDATEMODULEREQ + "/onLogin";
	public final static String PORTAL_HANDLEACTION = PORTAL_DOLIFECYCLE + "/handleAction";
	public final static String PORTAL_PROCESSPLUGINACTION = PORTAL_HANDLEACTION + "/processPluginAction";
	public final static String PORTAL_PROCESSPORTLETACTION = PORTAL_HANDLEACTION + "/processPortletAction";
	public final static String PORTAL_HANDLEPORTALACTION = PORTAL_HANDLEACTION + "/handlePortalAction";
	public final static String PORTAL_PREPARERENDER = PORTAL_DOLIFECYCLE + "/prepareRender";
	public final static String PORTAL_ON_PREPARE_RENDER = PORTAL_PREPARERENDER + "/onPrepareRender";
	public final static String PORTAL_RENDER = PORTAL_DOLIFECYCLE + "/render";
	public final static String PORTAL_RESOURCE = PORTAL_DOLIFECYCLE + "/serveResource";
	public final static String PORTAL_RENDER_TEMPLATE_RENDER = PORTAL_RENDER + "/UserPortalTemplate.render";
	public final static String PORTAL_RENDER_RENDERPBOX = PORTAL_RENDER_TEMPLATE_RENDER + "/PBox.render";
	public final static String PORTAL_RENDER_RENDERFRAME = PORTAL_RENDER_RENDERPBOX + "/renderFrame";
	public final static String PORTAL_RENDER_HELPER = PORTAL_RENDER_TEMPLATE_RENDER + "/templateHelper";
	public final static String PORTAL_RENDER_XNL = PORTAL_RENDER_TEMPLATE_RENDER + "/xnlParser";
	public final static String PORTAL_RENDER_TEMPLATE_RENDERSINGLE = PORTAL_RENDER + "/UserPortalTemplate.renderSingle";
	public final static String PORTAL_RENDER_POSTPROCESSING = PORTAL_RENDER + "/doPostProcessing";
	public final static String PORTAL_RENDER_POSTPROCESSING_PLINKS = PORTAL_RENDER_POSTPROCESSING + "/processPLinks";
	public final static String PORTAL_RENDER_POSTPROCESSING_TEMPLATEENGINE = PORTAL_RENDER_POSTPROCESSING + "/templateEngine2";
	public final static String PORTAL_DOCLEANUP = PORTAL_DOLIFECYCLE + "/doCleanup";
	public final static String PORTALSERVLET_WRITERESPONSE = PORTALSERVLET_PROCESS + "/writeResponse";

	public final static String PORTALSERVLET_DESTROY = "PortalServlet.destroy";

	public final static String AUTHENTICATIONRESULT_GETUSER = PORTALWRAPPER_DOAUTH + "/getUser";
	public final static String AUTHENTICATIONSYSTEM_CHECKAUTHENTICATION = AUTHENTICATIONRESULT_GETUSER + "/checkAuthentication";
	public final static String AUTHENTICATIONSYSTEM_CREATESECONDARY = AUTHENTICATIONRESULT_GETUSER + "/createSecondary";
	public final static String PORTALWRAPPER_SETUSER = PORTALWRAPPER_DOLIFECYCLE + "/setUser";
	public final static String PORTALWRAPPER_CREATEPORTAL = PORTALWRAPPER_SETUSER + "/createPortal";
	public final static String PORTALWRAPPER_LOGIN = PORTALWRAPPER_SETUSER + "/logIn";
	public final static String PORTALWRAPPER_CREATEPORTALGETTEMPLATE = PORTALWRAPPER_CREATEPORTAL + "/getTemplate";
	public final static String PORTALWRAPPER_CREATEPORTALINITTEMPLATE = PORTALWRAPPER_CREATEPORTAL + "/initTemplate";
	public final static String PORTALWRAPPER_LOGINGETTEMPLATE = PORTALWRAPPER_LOGIN + "/getTemplate";
	public final static String PORTALWRAPPER_LOGININITTEMPLATE = PORTALWRAPPER_LOGIN + "/initTemplate";

}
