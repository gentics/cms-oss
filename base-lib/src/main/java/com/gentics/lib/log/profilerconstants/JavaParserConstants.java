/*
 * @author laurin
 * @date 28.03.2007
 * @version $Id: JavaParserConstants.java,v 1.15 2009-12-16 16:12:08 herbert Exp $
 */
package com.gentics.lib.log.profilerconstants;

/**
 * constants for use with RuntimeProfiler. search for their usage for meaning.
 * @see com.gentics.lib.log.RuntimeProfiler
 * @author laurin
 */
public abstract class JavaParserConstants {
	public final static String TRANSACTION = ComponentsConstants.GENTICSROOT + "/JavaParserTransaction";
    
	public final static String INVOKER = ComponentsConstants.GENTICSROOT + "/Invoker.do";

	public final static String INVOKER_PROCESSPARAMS = JavaParserConstants.INVOKER + "/processParams";

	public final static String INVOKER_RENDER = JavaParserConstants.INVOKER + "/render";

	public final static String PROCESSOR_PROCESS = TRANSACTION + "/Processor.process";
    
	public final static String PROCESSOR_PROCESS_CONTENTNODE = PROCESSOR_PROCESS + "/ContentNodeProcessor.run";

	public final static String PROCESSOR_RENDER = TRANSACTION + "/Processor.render";
    
	public final static String VALUECLASS_RENDER = "Value.render";

	public final static String VALUE_RENDER = VALUECLASS_RENDER + "/instance";

	public final static String PARSER_PARSE = "Parser.parse";

	public final static String PARSER_RENDER = "Parser.render";
    
	public final static String PARSER_PARSESTRUCTRENDERER_GETPARSERTAG = PARSER_RENDER + "/ParseStructRenderer.getParserTag";
    
	public final static String PARSER_PARSESTRUCTRENDERER_RENDERTAG = PARSER_RENDER + "/ParseStructRenderer.renderTag";

	public final static String STACKRESOLVE = "StackResolver.resolve";

	public final static String PAGE_RENDER = "Page.render";

	public final static String RENDERER_RENDER = "Renderer.render";
    
	public final static String TRANSACTION_GETOBJECTS = "Transaction.getObjects";

	public final static String TRANSACTION_GETOBJECT = "Transaction.getObject";

	public final static String XNLFUNC_RENDER = "XnlFunctionTag.render";
    
	public final static String NODEFACTORY_GETOBJECT = "NodeFactory.getObject";
    
	public final static String NODEFACTORY_GETOBJECTS = "NodeFactory.getObjects";
    
	public final static String NODEFACTORY_GETOBJECT_GETCACHE = NODEFACTORY_GETOBJECT + "/getCache";
    
	public final static String NODEFACTORY_GETOBJECT_LOADOBJECT = NODEFACTORY_GETOBJECT + "/loadObject";
    
	public final static String NODEFACTORY_GETOBJECTS_GETCACHE = NODEFACTORY_GETOBJECTS + "/getCache";
    
	public final static String NODEFACTORY_GETOBJECTS_LOADOBJECTS = NODEFACTORY_GETOBJECTS + "/loadObjects";
    
	public final static String NODEFACTORY_GETOBJECTS_LOADCACHEDOBJECT = NODEFACTORY_GETOBJECTS + "/loadCachedObject";

	public final static String PUBLISHER_RUN = JavaParserConstants.INVOKER_PROCESSPARAMS + "/Publisher.run";

	public final static String PUBLISHER_UPDATEMAPS = JavaParserConstants.PUBLISHER_RUN + "/updateMaps";

	public final static String PUBLISHER_WRITEFS = JavaParserConstants.PUBLISHER_RUN + "/writefs";

	public final static String PUBLISHER_WRITEFS_PAGES = PUBLISHER_WRITEFS + "/pages";

	public final static String PUBLISHER_WRITEFS_FILES = PUBLISHER_WRITEFS + "/files";

	public final static String PUBLISHER_WRITEFS_IMAGERESIZER = PUBLISHER_WRITEFS + "/resizer";
    
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_QUERY = PUBLISHER_WRITEFS_IMAGERESIZER + "/query";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_CREATECACHEKEY = PUBLISHER_WRITEFS_IMAGERESIZER + "/createCacheKey";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_GETINFO = PUBLISHER_WRITEFS_IMAGERESIZER + "/getInfo";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_GETCACHE = PUBLISHER_WRITEFS_IMAGERESIZER + "/getCache";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_PUTCACHE = PUBLISHER_WRITEFS_IMAGERESIZER + "/putCache";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_WRITE = PUBLISHER_WRITEFS_IMAGERESIZER + "/write";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_RESIZE = PUBLISHER_WRITEFS_IMAGERESIZER + "/resize";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_GETALLINFO = PUBLISHER_WRITEFS_IMAGERESIZER + "/getAllInfo";
	public final static String PUBLISHER_WRITEFS_IMAGERESIZER_HANDLEPAGE = PUBLISHER_WRITEFS_IMAGERESIZER + "/handlePage";

	public final static String PUBLISHER_FILEDEPENDENCIES = JavaParserConstants.PUBLISHER_RUN + "/checkFileDependencies";

	public final static String PUBLISHER_UPDATEPAGES = JavaParserConstants.PUBLISHER_RUN + "/updatePages";

	public final static String PUBLISHER_UPDATEMAPS_TYPES = JavaParserConstants.PUBLISHER_UPDATEMAPS + "/SyncTypes";

	public final static String PUBLISHER_UPDATEMAPS_FOLDERS = JavaParserConstants.PUBLISHER_UPDATEMAPS + "/Folders";

	public final static String PUBLISHER_UPDATEMAPS_FILES = JavaParserConstants.PUBLISHER_UPDATEMAPS + "/Files";

	public final static String PUBLISHER_UPDATEMAPS_DELETED = JavaParserConstants.PUBLISHER_UPDATEMAPS + "/Deleted";

	public final static String PUBLISHER_UPDATEMAPS_LASTUPDATE = JavaParserConstants.PUBLISHER_UPDATEMAPS + "/LastUpdate";
    
	public final static String RENDERTYPE_STOREDEP = "RenderType.storeDependencies";

	public static final String DEPENDENCY_MANAGER = "DependencyManager";

	public static final String DEPENDENCY_MANAGER_DELETE = DEPENDENCY_MANAGER + "/delete";

	public static final String DEPENDENCY_MANAGER_DELETE_EXECUTE = DEPENDENCY_MANAGER_DELETE + "/execute";
    
	public static final String DEPENDENCY_MANAGER_DELETE_PREPARE = DEPENDENCY_MANAGER_DELETE + "/prepare";

	public static final String DEPENDENCY_MANAGER_STORE = DEPENDENCY_MANAGER + "/store";
    
	public static final String DEPENDENCY_MANAGER_STORE_EXECUTE = DEPENDENCY_MANAGER_STORE + "/execute";
    
	public static final String DEPENDENCY_MANAGER_STORE_PREPARE = DEPENDENCY_MANAGER_STORE + "/prepare";

	public static final String PUBLISHER_UPDATEPAGES_FINISH = PUBLISHER_UPDATEPAGES + "/finish";
    
	public static final String IMAGESTORE_DORESIZE = "GenticsImageStore.doResize";

}
