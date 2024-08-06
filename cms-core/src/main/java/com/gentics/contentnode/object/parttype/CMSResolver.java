/*
 * @author norbert
 * @date 18.04.2007
 * @version $Id: CMSResolver.java,v 1.12.2.1 2010-12-01 10:37:06 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.api.portalnode.imp.GenticsImpInterface;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.formatter.CNDateFormatterImp;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagResolvable;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.imps.CMSLoaderImp;
import com.gentics.contentnode.render.RenderInfo;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.resolving.ResolvableMapWrappable;
import com.gentics.portalnode.formatter.GenticsStringFormatter;
import com.gentics.portalnode.formatter.SortImp;
import com.gentics.portalnode.formatter.URLIncludeImp;
import com.gentics.portalnode.formatter.VelocityToolsImp;

/**
 * Resolver for objects put into the context for AbstractExtensiblePartTypes
 * under the name "cms"
 */
public class CMSResolver implements ResolvableMapWrappable {
	protected ModeResolver modeResolver;

	protected ImpsResolver impsResolver;

	protected static Map<String, Property> properties = new HashMap<>();

	protected static NodeLogger logger = NodeLogger.getNodeLogger(CMSResolver.class);

	protected final static Set<String> resolvableKeys;

	static {
		properties.put("rendermode", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getModeResolver();
			}
		});
		properties.put("page", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getPage();
			}
		});
		properties.put("template", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getTemplate();
			}
		});
		properties.put("tag", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getTag();
			}
		});
		properties.put("object", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getObject();
			}
		});
		properties.put("folder", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getFolder();
			}
		});
		properties.put("node", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getNode();
			}
		});
		properties.put("file", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getFile();
			}
		});
		properties.put("imps", new Property() {
			public Object get(CMSResolver cmsResolver) {
				return cmsResolver.getImpsResolver();
			}
		});

		resolvableKeys = SetUtils.difference(properties.keySet(), Collections.singleton("imps"));
	}

	protected Page page;

	protected Template template;

	protected Tag tag;

	protected Folder folder;

	protected Node node;

	protected ContentFile file;

	/**
	 * this is the root object for dependencies
	 */
	protected NodeObject rootObject;

	/**
	 * Create an instance of the cms resolver
	 * @param page page
	 * @param template template
	 * @param tag tag
	 * @param folder folder
	 * @param node node
	 * @param file file
	 * @throws NodeException
	 */
	public CMSResolver(Page page, Template template, Tag tag, Folder folder, Node node,
			ContentFile file) throws NodeException {
		impsResolver = new ImpsResolver();
		this.page = page;
		this.file = file;
		this.template = template;
		if (this.template == null && this.page != null) {
			this.template = this.page.getTemplate();
		}
		this.tag = tag;
		this.folder = folder;
		if (this.folder == null) {
			if (this.page != null) {
				this.folder = this.page.getFolder();
			} else if (this.file != null) {
				this.folder = this.file.getFolder();
			} else if (this.template != null) {
				this.folder = this.template.getFolder();
			}
		}
		this.node = node;
		if (this.node == null && this.folder != null) {
			this.node = this.folder.getNode();
		}

		// determine the rendered root object
		if (this.page != null) {
			this.rootObject = this.page;
		} else if (this.file != null) {
			this.rootObject = this.file;
		} else if (this.folder != null) {
			this.rootObject = this.folder;
		}

		// create the mode resolver and set the flag, whether we are rendering a foreign object
		StackResolvable renderedRootObject = TransactionManager.getCurrentTransaction().getRenderType().getRenderedRootObject();
		modeResolver = new ModeResolver(!Objects.equals(renderedRootObject, this.rootObject));
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		Property prop = (Property) properties.get(key);

		if (prop != null) {
			Object value = prop.get(this);

			addDependency(key, value);
			return value;
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/**
	 * Add the dependency on the resolved property (if a root object exists and
	 * dependency handling is enabled)
	 * @param property resolved property
	 * @param resolvedObject value of the property
	 */
	protected void addDependency(String property, Object resolvedObject) {
		if (rootObject != null) {
			try {
				RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

				if (renderType.doHandleDependencies()) {
					renderType.addDependency(rootObject, property);
				}
			} catch (NodeException e) {
				logger.error("Error while adding dependency {" + rootObject + "}/{" + property + "}", e);
			}
		}
	}

	/**
	 * Get the imps resolver
	 * @return imps resolver
	 */
	protected ImpsResolver getImpsResolver() {
		return impsResolver;
	}

	/**
	 * Get the mode resolver
	 * @return the mode resolver
	 */
	protected ModeResolver getModeResolver() {
		return modeResolver;
	}

	/**
	 * Get the current page
	 * @return current page
	 */
	protected Resolvable getPage() {
		return page;
	}

	/**
	 * Get the current template
	 * @return current template
	 */
	protected Resolvable getTemplate() {
		return template;
	}

	/**
	 * Get the current tag
	 * @return current tag
	 */
	protected Resolvable getTag() {
		return tag;
	}

	/**
	 * Get the objecttag resolver
	 * @return objecttag resolver
	 */
	protected Resolvable getObject() {
		if (page != null) {
			return new ObjectTagResolvable(page);
		} else if (file != null) {
			return new ObjectTagResolvable(file);
		} else if (folder != null) {
			return new ObjectTagResolvable(folder);
		} else {
			return null;
		}
	}

	/**
	 * Get the current folder
	 * @return current folder
	 */
	protected Resolvable getFolder() {
		return folder;
	}

	/**
	 * Get the current node
	 * @return current node
	 */
	protected Resolvable getNode() {
		return node;
	}

	/**
	 * Get the current file
	 * @return current file
	 */
	protected Resolvable getFile() {
		return file;
	}

	/**
	 * Abstract property class
	 */
	protected abstract static class Property {

		/**
		 * Abstract method to get the property
		 * @param cmsResolver cms resolver instance
		 * @return the object
		 */
		public abstract Object get(CMSResolver cmsResolver);
	}

	/**
	 * Resolver for the current mode
	 */
	public static class ModeResolver extends ResolvableBean {

		public static final String PARAM_OVERWRITE_EDITMODE = "overwriteMode";

		/**
		 * Flag, which is set when the velocity part is rendered for a foreign object (i.e. not the rendered root object)
		 */
		protected final boolean renderingForeignObject;

		/**
		 * Create instance
		 * @param renderingForeignObject flag for rendering a foreign object
		 */
		public ModeResolver(boolean renderingForeignObject) {
			this.renderingForeignObject = renderingForeignObject;
		}

		public boolean isDebugPublish() throws NodeException {
			Boolean debugPublish = (Boolean) TransactionManager.getCurrentTransaction().getRenderType().getParameter(RenderInfo.PARAMETER_DEBUG_PUBLISH);

			return debugPublish != null && debugPublish.booleanValue();
		}

		public int getEditMode() throws NodeException {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

			// we are only allowed to fall back to the overwritten edit mode, if not rendering
			// for a foreign object
			if (!renderingForeignObject) {
				Object mode = renderType.getParameter(PARAM_OVERWRITE_EDITMODE);

				if (mode != null && mode instanceof Integer) {
					return ((Integer) mode).intValue();
				}
			}
			return renderType.getEditMode();
		}

		/**
		 * Return true when rendering in publish mode
		 * @return true for publish mode
		 * @throws NodeException
		 */
		public boolean getPublish() throws NodeException {
			return isDebugPublish() || getEditMode() == RenderType.EM_PUBLISH;
		}

		/**
		 * Return true when rendering in live mode
		 * @return true for live mode
		 * @throws NodeException
		 */
		public boolean getLive() throws NodeException {
			return !isDebugPublish() && getEditMode() == RenderType.EM_LIVEPREVIEW;
		}

		/**
		 * Return true when rendering in edit mode
		 * @return true for edit mode
		 * @throws NodeException
		 */
		public boolean getEdit() throws NodeException {
			int editMode = getEditMode();

			return editMode == RenderType.EM_ALOHA;
		}

		/**
		 * Return true when rendering in preview mode
		 * @return true for preview mode
		 * @throws NodeException
		 */
		public boolean getPreview() throws NodeException {
			int editMode = getEditMode();

			return !isDebugPublish() && editMode == RenderType.EM_PREVIEW || editMode == RenderType.EM_ALOHA_READONLY;
		}

		/**
		 * Return true when rendering in real mode (edit or preview)
		 * @return true for real mode
		 * @throws NodeException
		 */
		public boolean getReal() throws NodeException {
			// real mode was removed
			return false;
		}

		/**
		 * Return true when rendering in frontend mode.
		 * The return value is only valid when in edit mode.
		 * @return true for frontend mode
		 * @throws NodeException
		 */
		public boolean getFrontend() throws NodeException {
			return TransactionManager.getCurrentTransaction().getRenderType().isFrontEnd();
		}

		/**
		 * Return true when rendering in backend mode.
		 * The return value is only valid when in edit mode.
		 * @return true for backend mode
		 * @throws NodeException
		 */
		public boolean getBackend() throws NodeException {
			return !getFrontend();
		}
	}

	/**
	 * Imps resolver
	 */
	public static class ImpsResolver {

		/**
		 * velocity tools imp (when fetched from the pool)
		 */
		protected GenticsImpInterface velocityToolsImp;

		/**
		 * gentics string formatter (when fetched from the pool)
		 */
		protected GenticsImpInterface genticsStringFormatter;

		/**
		 * date formatter (when fetched from the pool)
		 */
		protected GenticsImpInterface genticsDateFormatter;

		/**
		 * sort imp (when fetched from the pool)
		 */
		protected GenticsImpInterface sortImp;
        
		/**
		 * loader imp
		 */
		protected GenticsImpInterface loaderImp;

		/**
		 * URL include imp
		 */
		protected GenticsImpInterface urlIncludeImp;

		/**
		 * constant for the velo imp
		 */
		public final static String VELOIMP = "velocitytools";

		/**
		 * constant for the string imp
		 */
		public final static String STRINGIMP = "string";

		/**
		 * constant for the date imp
		 */
		public final static String DATEIMP = "date";

		/**
		 * constant for the sorter imp
		 */
		public final static String SORTIMP = "sorter";
        
		/**
		 * constant for the loader imp
		 */
		public final static String LOADERIMP = "loader";

		/**
		 * constant for the url imp
		 */
		public final static String URLIMP = "url";

		/**
		 * Create instance of the imps resolver
		 */
		public ImpsResolver() {}

		/**
		 * Get the velocity tools imp
		 * @return velocity tools imp
		 */
		public GenticsImpInterface getVelocitytools() {
			if (velocityToolsImp == null) {
				velocityToolsImp = ImpProvider.getImp(VELOIMP);
			}
			return velocityToolsImp;
		}

		/**
		 * Get the velocity tools imp - an alias which is the same as in PN 3.3
		 * @return velocity tools imp
		 */
		public GenticsImpInterface getVelocityTools() {
			if (velocityToolsImp == null) {
				velocityToolsImp = ImpProvider.getImp(VELOIMP);
			}
			return velocityToolsImp;
		}

		/**
		 * Get the gentics string formatter imp
		 * @return gentics string formatter imp
		 */
		public GenticsImpInterface getString() {
			if (genticsStringFormatter == null) {
				genticsStringFormatter = ImpProvider.getImp(STRINGIMP);
			}
			return genticsStringFormatter;
		}

		/**
		 * Get the sorter imp
		 * @return sorter imp
		 */
		public GenticsImpInterface getSorter() {
			if (sortImp == null) {
				sortImp = ImpProvider.getImp(SORTIMP);
			}
			return sortImp;
		}

		/**
		 * Get the date formatter imp
		 * @return date formatter imp
		 */
		public GenticsImpInterface getDate() {
			if (genticsDateFormatter == null) {
				genticsDateFormatter = ImpProvider.getImp(DATEIMP);
			}
			return genticsDateFormatter;
		}
        
		public GenticsImpInterface getLoader() {
			if (loaderImp == null) {
				loaderImp = ImpProvider.getImp(LOADERIMP);
			}
			return loaderImp;
		}

		public GenticsImpInterface getUrl() {
			if (urlIncludeImp == null) {
				urlIncludeImp = ImpProvider.getImp(URLIMP);
			}
			return urlIncludeImp;
		}
	}

	/**
	 * Class for the singleton imp provider, that provides imps which are held
	 * in pools
	 */
	protected final static class ImpProvider {

		/**
		 * internal imp pool
		 */
		protected KeyedObjectPool impPool = null;

		/**
		 * the singleton instance
		 */
		protected static ImpProvider instance = null;

		/**
		 * static method to get an imp from the pool
		 * @param impId id of the imp
		 * @return imp or null
		 */
		public static GenticsImpInterface getImp(String impId) {
			try {
				ImpProvider impProvider = getInstance();
				Object borrowedObject = impProvider.impPool.borrowObject(impId);

				if (logger.isInfoEnabled()) {
					logger.info(
							"borrowed imp {" + impId + "}. Active: " + impProvider.impPool.getNumActive(impId) + ", Idle: " + impProvider.impPool.getNumIdle(impId));
				}
				return (GenticsImpInterface) borrowedObject;
			} catch (Exception e) {
				logger.error("Error while fetching imp {" + impId + "}", e);
				return null;
			}
		}

		/**
		 * static method to return an imp
		 * @param imp imp to return
		 * @param impId id of the imp
		 */
		public static void returnImp(GenticsImpInterface imp, String impId) {
			try {
				ImpProvider impProvider = getInstance();

				impProvider.impPool.returnObject(impId, imp);
				if (logger.isInfoEnabled()) {
					logger.info(
							"returned imp {" + impId + "}. Active: " + impProvider.impPool.getNumActive(impId) + ", Idle: " + impProvider.impPool.getNumIdle(impId));
				}
			} catch (Exception e) {
				logger.error("Error while returning imp {" + impId + "} to pool.", e);
			}
		}

		/**
		 * Get the singleton instance of the imp provider
		 * @return imp provider
		 */
		protected static ImpProvider getInstance() {
			if (instance == null) {
				instance = new ImpProvider();
			}
			return instance;
		}

		/**
		 * private constructor for the singleton
		 */
		private ImpProvider() {
			// create the pool
			impPool = new GenericKeyedObjectPool(new ImpFactory(), 20, GenericObjectPool.WHEN_EXHAUSTED_GROW, -1, 5, false, false);
		}

		/**
		 * Internal imp factory
		 */
		protected class ImpFactory implements KeyedPoolableObjectFactory {

			/*
			 * (non-Javadoc)
			 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#activateObject(java.lang.Object,
			 *      java.lang.Object)
			 */
			public void activateObject(Object key, Object imp) throws Exception {}

			/*
			 * (non-Javadoc)
			 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#destroyObject(java.lang.Object,
			 *      java.lang.Object)
			 */
			public void destroyObject(Object key, Object imp) throws Exception {}

			/*
			 * (non-Javadoc)
			 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#makeObject(java.lang.Object)
			 */
			public Object makeObject(Object key) throws Exception {
				try {
					if (ImpsResolver.VELOIMP.equals(key)) {
						VelocityToolsImp velocityToolsImp = new VelocityToolsImp();

						velocityToolsImp.init("velocitytools", new HashMap());

						return velocityToolsImp;
					} else if (ImpsResolver.STRINGIMP.equals(key)) {
						GenticsStringFormatter genticsStringFormatter = new GenticsStringFormatter();

						genticsStringFormatter.init("genticsstringformatter", new HashMap());

						return genticsStringFormatter;
					} else if (ImpsResolver.SORTIMP.equals(key)) {
						SortImp sortImp = new SortImp();

						sortImp.init("sorter", new HashMap());

						return sortImp;
					} else if (ImpsResolver.LOADERIMP.equals(key)) {
						CMSLoaderImp loaderImp = new CMSLoaderImp();

						loaderImp.init("loader", new HashMap());

						return loaderImp;
					} else if (ImpsResolver.URLIMP.equals(key)) {
						URLIncludeImp urlIncludeImp = new URLIncludeImp();

						urlIncludeImp.init("url", new HashMap());

						return urlIncludeImp;
					} else if (ImpsResolver.DATEIMP.equals(key)) {
						CNDateFormatterImp dateformatterImp = new CNDateFormatterImp();
						return dateformatterImp;
					}
				} catch (Exception e) {
					logger.error("Imp {" + key + "} could not be initialized.", e);
				}
				return null;
			}

			/*
			 * (non-Javadoc)
			 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#passivateObject(java.lang.Object,
			 *      java.lang.Object)
			 */
			public void passivateObject(Object key, Object imp) throws Exception {}

			/*
			 * (non-Javadoc)
			 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#validateObject(java.lang.Object,
			 *      java.lang.Object)
			 */
			public boolean validateObject(Object key, Object imp) {
				return true;
			}
		}
	}

	/**
	 * Clean the cms resolver (give imps back to pool, etc.)
	 */
	public void clean() {
		if (impsResolver != null) {
			if (impsResolver.genticsDateFormatter != null) {
				ImpProvider.returnImp(impsResolver.genticsDateFormatter, ImpsResolver.DATEIMP);
				impsResolver.genticsDateFormatter = null;
			}
			if (impsResolver.genticsStringFormatter != null) {
				ImpProvider.returnImp(impsResolver.genticsStringFormatter, ImpsResolver.STRINGIMP);
				impsResolver.genticsStringFormatter = null;
			}
			if (impsResolver.sortImp != null) {
				ImpProvider.returnImp(impsResolver.sortImp, ImpsResolver.SORTIMP);
				impsResolver.sortImp = null;
			}
			if (impsResolver.velocityToolsImp != null) {
				ImpProvider.returnImp(impsResolver.velocityToolsImp, ImpsResolver.VELOIMP);
				impsResolver.velocityToolsImp = null;
			}
			if (impsResolver.loaderImp != null) {
				ImpProvider.returnImp(impsResolver.loaderImp, ImpsResolver.LOADERIMP);
				impsResolver.loaderImp = null;
			}
			if (impsResolver.urlIncludeImp != null) {
				ImpProvider.returnImp(impsResolver.urlIncludeImp, ImpsResolver.URLIMP);
				impsResolver.urlIncludeImp = null;
			}
			impsResolver = null;
		}
	}
}
