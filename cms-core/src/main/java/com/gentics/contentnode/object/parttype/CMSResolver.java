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

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagResolvable;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.render.RenderInfo;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.ResolvableMapWrappable;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.log.NodeLogger;

/**
 * Resolver for objects put into the context for AbstractExtensiblePartTypes
 * under the name "cms"
 */
public class CMSResolver implements ResolvableMapWrappable {
	protected ModeResolver modeResolver;

	protected static Map<String, Property> properties = new HashMap<>();

	protected static NodeLogger logger = NodeLogger.getNodeLogger(CMSResolver.class);

	protected final static Set<String> resolvableKeys;

	/**
	 * Service loader for {@link CMSResolverService}s
	 */
	private final static ServiceLoaderUtil<CMSResolverService> cmsResolverServiceLoader = ServiceLoaderUtil
			.load(CMSResolverService.class);

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
	 * Map of all resolvers, provided by {@link CMSResolverService}s
	 */
	protected Map<String, ProvidedResolver> providedResolvers = new HashMap<>();

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
		cmsResolverServiceLoader.forEach(service -> {
			providedResolvers.putAll(service.getResolvers());
		});

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
		} else if (providedResolvers.containsKey(key)) {
			Object value = providedResolvers.get(key);

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
	 * Clean the cms resolver (give imps back to pool, etc.)
	 */
	public void clean() {
		providedResolvers.values().forEach(ProvidedResolver::clean);
	}
}
