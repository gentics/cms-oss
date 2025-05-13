package com.gentics.contentnode.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.CMSResolver;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.resolving.StackResolver;
import com.gentics.lib.genericexceptions.NotYetImplementedException;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * RenderType provides informations and settings about how code should be rendered.
 * The information is stored in a stack, which can be used to quickly change the
 * parameters and undo all changes after using them. A baseobject can be added
 * to the stack to allow keyname resolving with fallbacks.
 */
public class RenderType implements RenderInfo {

	/**
	 * edit mode for portal rendering
	 */
	public final static int EM_PORTAL = 1;

	/**
	 * edit mode for static publishing
	 */
	public final static int EM_PUBLISH = 2;

	/**
	 * edit mode for previews
	 */
	public final static int EM_PREVIEW = 3;

	/**
	 * edit mode for previews in live mode
	 */
	public final static int EM_LIVEPREVIEW = 5;

	/**
	 * edit mode for Aloha
	 */
	public final static int EM_ALOHA = 8;

	/**
	 * read only mode for Aloha
	 */
	public final static int EM_ALOHA_READONLY = 9;

	/**
	 * Parameter name for storing the page language
	 */
	private final static String LANGUAGE_PARAM = "language";

	private Stack<RenderInfo> infoStack;
	private StackResolver stack;

	/**
	 * stack holding the dependent objects (objects or object/elements currently
	 * rendered) for correct creation of dependencies
	 */
	private Stack<DependencyObject> dependentObjectStack;

	/**
	 * This counter keeps track of how many times a dependent object was
	 * pushed. We need to keep this separate from the dependentObjectStack,
	 * because we only push the first dependent object if
	 * {@link DependencyManager#DIRECT_DEPENDENCIES} is true, but we still
	 * need a balance for calls that push/pop dependent objects.
	 */
	private int dependentObjectStackSkipCounter = 0;

	/**
	 * list of all dependent objects which were used during rendering process
	 */
	private List<DependencyObject> dependentObjects;

	/**
	 * list of all dependencies to be stored
	 */
	private List<Dependency> dependencies;

	/**
	 * version timestamp (default: current version)
	 */
	private int versionTimestamp = -1;

	/**
	 * the logger
	 */
	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * flag to mark whether the rendered tag ids shall be collected or not
	 */
	private boolean collectTagIds = false;

	/**
	 * list of collected content tag ids (when {@link RenderType#collectTagIds} is true)
	 */
	private List<Object> contentTagIds;

	/**
	 * list of collected template tag ids (when {@link RenderType#collectTagIds} is true)
	 */
	private List<Object> templateTagIds;

	/**
	 * stack for cms resolver (used for rendering extensible part types, like the VelocityPartType, NavigationPartType, etc.)
	 */
	private Stack<CMSResolver> cmsResolverStack;

	/**
	 * Flag to mark whether the rendering is done in frontend mode
	 */
	private boolean frontEnd = false;

	/**
	 * A public constructor, which provides the renderType with all required informations.
	 * @param editMode the editmode to set to the root level.
	 * @param evaluate the evaluation mode to set to the root level.
	 * @param handleDependencies enable or disable dependency handling on the root level.
	 * @param defaultRenderer the main default templaterenderer keyname.
	 * @param urlFactory an urlfactory which is used to create new urls.
	 * @param versionTimestamp versiontimestamp
	 */
	public RenderType(int editMode, boolean evaluate, boolean handleDependencies, String defaultRenderer,
			RenderUrlFactory urlFactory, int versionTimestamp) {
		this.versionTimestamp = versionTimestamp;
		infoStack = new Stack<RenderInfo>();
		dependentObjectStack = new Stack<DependencyObject>();
		dependentObjects = new Vector<DependencyObject>();
		dependencies = new Vector<Dependency>();
		SimpleRenderInfo info = new SimpleRenderInfo(editMode, evaluate, handleDependencies, urlFactory, null,
				new HashMap<String, Object>(), defaultRenderer, new ArrayList<String>(RendererFactory.getRendererKeys()), null, null, this);

		infoStack.push(info);
		this.stack = new StackResolver();
		cmsResolverStack = new Stack<CMSResolver>();
	}

	/**
	 * A public constructor with no arguments. Note that you need to set at least the
	 * default templaterenderer keyname and the urlrenderer before you can use this renderType.
	 */
	public RenderType() {
		infoStack = new Stack<RenderInfo>();
		dependentObjectStack = new Stack<DependencyObject>();
		dependentObjects = new Vector<DependencyObject>();
		dependencies = new Vector<Dependency>();
		infoStack.push(new SimpleRenderInfo(this, (StackResolvable) null));
		this.stack = new StackResolver();
		cmsResolverStack = new Stack<CMSResolver>();
	}

	/**
	 * create a new renderType and copy all information from the given rendertype.
	 *
	 * @param renderType rendertype to copy
	 * @param copyStack if true, the complete stack will be copied, else only the current top will be used as new root info.
	 */
	public RenderType(RenderType renderType, boolean copyStack) {
		this.versionTimestamp = renderType.versionTimestamp;
		this.infoStack = new Stack<RenderInfo>();
		dependentObjectStack = new Stack<DependencyObject>();
		dependentObjects = new Vector<DependencyObject>();
		dependencies = new Vector<Dependency>();
		if (copyStack) {
			// TODO think about copying the dependentObjectStack also
			for (int i = 0; i < renderType.getDepth(); i++) {
				infoStack.push(new SimpleRenderInfo(this, renderType.getInfo(i)));
			}
		} else {
			infoStack.push(new SimpleRenderInfo(this, renderType.getInfo()));
		}
		this.stack = copyStack ? renderType.getStack().getCopy() : new StackResolver();
		cmsResolverStack = new Stack<CMSResolver>();
	}

	/**
	 * Parse a linkway string into a constant representation.
	 * The linkway may be one of 'auto', 'host', 'abs' or 'portal'. If the given
	 * linkway matches none of the strings, LINKWAY_AUTO is returned.
	 *
	 * @param linkWay the linkway as string.
	 * @return the corresponding linkway value, or LINKWAY_AUTO, if not recognized.
	 */
	public static int parseLinkWay(String linkWay) {
		if ("auto".equals(linkWay)) {
			return RenderUrl.LINKWAY_AUTO;
		}
		if ("host".equals(linkWay)) {
			return RenderUrl.LINKWAY_HOST;
		}
		if ("abs".equals(linkWay)) {
			return RenderUrl.LINKWAY_ABS;
		}
		if ("portal".equals(linkWay)) {
			return RenderUrl.LINKWAY_PORTAL;
		}
		if ("dyn".equals(linkWay)) {
			// "dyn" should behave like LINKWAY_PORTAL but with an added
			// "linkway prefix" (CN config LINKWAY_FILE_PATH)
			return RenderUrl.LINKWAY_PORTAL;
		}
		return RenderUrl.LINKWAY_AUTO;
	}

	/**
	 * Render the given linkway (human readable). This is the converse to
	 * {@link #parseLinkWay(String)}.
	 * @param linkWay linkway
	 * @return human readable linkway
	 */
	public static String renderLinkWay(int linkWay) {
		switch (linkWay) {
		case RenderUrl.LINKWAY_ABS:
			return "abs";

		case RenderUrl.LINKWAY_HOST:
			return "host";

		case RenderUrl.LINKWAY_PORTAL:
			return "portal";

		case RenderUrl.LINKWAY_AUTO:
		default:
			return "auto";
		}
	}

	/**
	 * Parse an editMode string into a constant value. The editMode must be one of
	 * 'publish', 'preview', 'portal', 'edit'. If none of them matches the given
	 * string, EM_PUBLISH is returned.
	 *
	 * @param editMode the editmode as string.
	 * @return the corresponding editmode value, or EM_PUBLISH if not recognized.
	 */
	public static int parseEditMode(String editMode) {

		if ("publish".equals(editMode)) {
			return EM_PUBLISH;
		}
		if ("preview".equals(editMode)) {
			return EM_PREVIEW;
		}
		if ("portal".equals(editMode)) {
			return EM_PORTAL;
		}
		if ("live".equals(editMode)) {
			return EM_LIVEPREVIEW;
		}
		if ("aloha".equals(editMode)) {
			return EM_ALOHA;
		}
		if ("aloha_readonly".equals(editMode)) {
			return EM_ALOHA_READONLY;
		}
		return EM_PUBLISH;
	}

	/**
	 * Render the edit mode
	 * @param editMode edit mode
	 * @return the edit mode as string
	 */
	public static String renderEditMode(int editMode) {
		switch (editMode) {
		case EM_PUBLISH:
			return "publish";

		case EM_PREVIEW:
			return "preview";

		case EM_PORTAL:
			return "portal";

		case EM_ALOHA:
			return "aloha";

		case EM_ALOHA_READONLY:
			return "aloha_readonly";

		case EM_LIVEPREVIEW:
		default:
			return "live";
		}
	}

	/**
	 * Create a default, preconfigured rendertype for a given editMode. This initializes
	 * editMode, handleDependencies, evaluate, defaultRenderer, urlFactory, linkWay and rendererList.
	 * The sessionId is only needed for preview and edit.
	 *
	 * @param prefs the preferences to use for configuring this renderType.
	 * @param editMode the editmode, for which the rendertype should be created.
	 * @param cnSessionID the content.node session id, or null if not available.
	 * @param versionTimestamp versiontimestamp
	 * @return a new preconfigured editmode.
	 */
	public static RenderType getDefaultRenderType(NodePreferences prefs, int editMode, String cnSessionID, int versionTimestamp) {
		RenderType renderType = null;

		// TODO get correct linkway, urlFactory, defaultrenderer and rendererlist by nodeconfig
		String renderer = "content";

		switch (editMode) {
		case EM_PUBLISH:
			renderType = new RenderType(editMode, true, true, renderer, null, versionTimestamp);
			break;

		case EM_PREVIEW:
		case EM_LIVEPREVIEW:
			renderType = new RenderType(editMode, true, false, renderer, null, versionTimestamp);
			break;

		case EM_PORTAL:
			renderType = new RenderType(editMode, true, true, renderer, null, versionTimestamp);
			break;

		case EM_ALOHA:
		case EM_ALOHA_READONLY:
			renderType = new RenderType(editMode, false, false, renderer, null, versionTimestamp);
			break;

		default:
			break;
		}

		if (renderType != null) {
			renderType.setPreferences(prefs);
		}

		return renderType;
	}

	/**
	 * Get the stack of the stackResolvable elements.
	 * You can use this stack to add your own elements to the stack and resolve keynames with it.
	 *
	 * @return the stackresolver which holds the stackResolvables.
	 */
	public StackResolver getStack() {
		return stack;
	}

	/**
	 * push a new RenderInfo into the stack without a stackResolvable.
	 * @see #push(StackResolvable)
	 */
	public void push() {
		push(null);
	}

	/**
	 * push a new RenderInfo into the stack, using the given stackResolvable as top-stackResolvable.
	 * All renderInfos are copied to the new entry.
	 * @param resolvable the new top stackresolvable.
	 */
	public void push(StackResolvable resolvable) {
		infoStack.push(new SimpleRenderInfo(getInfo(), resolvable));
		if (resolvable != null) {
			stack.push(resolvable);

			if (isCollectTagIds()) {
				if (resolvable instanceof ContentTag) {
					ContentTag tag = (ContentTag) resolvable;

					if (!contentTagIds.contains(tag.getId())) {
						contentTagIds.add(tag.getId());
					}
				} else if (resolvable instanceof TemplateTag) {
					TemplateTag tag = (TemplateTag) resolvable;

					if (!templateTagIds.contains(tag.getId())) {
						templateTagIds.add(tag.getId());
					}
				}
			}
		}
	}

	/**
	 * pop the top of the RenderInfo stack.
	 * @return the StackResolvable which was stored in the top level, or null if none was stored.
	 */
	public StackResolvable pop() {
		if (infoStack.size() <= 1) {
			return null;
		}
		RenderInfo info = (RenderInfo) infoStack.pop();
		final StackResolvable levelResolvable = info.getLevelResolvable();

		if (levelResolvable != null) {
			stack.remove(levelResolvable);
		}
		return levelResolvable;
	}

	/**
	 * Try to pop the top RenderInfo from the stack, but only if the top resolvable matches
	 * the given resolvable.
	 * @param resolvable the resolvable which must be on top of the stack.
	 * @return true, if one element was removed from the stack, else false.
	 */
	public boolean pop(StackResolvable resolvable) {
		if (infoStack.size() <= 1) {
			return false;
		}
		final StackResolvable levelResolvable = getInfo().getLevelResolvable();

		if (!Objects.equals(resolvable, levelResolvable)) {
			return false;
		}
		infoStack.pop();
		if (levelResolvable != null) {
			stack.remove(levelResolvable);
		}
		return true;
	}

	/**
	 * Return an {@link AutoCloseable} which will remove the given resolvable from the stack, if it matches the top resolvable
	 * and pushes the removed object back to the stack when {@link AutoCloseable#close()} is called
	 * @param resolvable the resolvable to be removed from the top of the stack
	 * @return AutoCloseable
	 */
	public RemoveTopResolvable withPopped(StackResolvable resolvable) {
		return new RemoveTopResolvable(resolvable);
	}

	/**
	 * Remove a level from the stack which contains the given resolvable.
	 * @see #find(StackResolvable)
	 * @param resolvable the resolvable to search and destroy.
	 * @return true, if found and removed, else false.
	 */
	public boolean remove(StackResolvable resolvable) {
		int i = find(resolvable);

		if (i == -1) {
			return false;
		}
		RenderInfo info = (RenderInfo) infoStack.remove(i);
		StackResolvable levelResolvable = info.getLevelResolvable();

		if (levelResolvable != null) {
			stack.remove(levelResolvable);
		}
		return true;
	}

	/**
	 * get the size of the stack.
	 * @return the number of elements in the info-stack.
	 */
	public int getDepth() {
		return infoStack.size();
	}

	/**
	 * Find a previously pushed stackresolvable in the info-stack.
	 * @param resolvable the resolvable to search.
	 * @return the level of the resolvable if found, else -1.
	 */
	public int find(StackResolvable resolvable) {
		String key = resolvable.getStackHashKey();

		if (key == null) {
			return -1;
		}

		for (int i = 0; i < infoStack.size(); i++) {
			StackResolvable info = ((RenderInfo) infoStack.get(i)).getLevelResolvable();

			if (info != null) {
				if (key.equals(info.getStackHashKey())) {
					return i;
				}
			}

		}
		return -1;
	}

	/**
	 * Get the renderInfo from a given level. The level must be between 0 and getDepth()-1.
	 * @param level the level for which the renderinfo should be retrieved.
	 * @return the renderinfo from this level.
	 */
	public RenderInfo getInfo(int level) {
		return (RenderInfo) infoStack.get(level);
	}

	/**
	 * get the top renderInfo.
	 * @return the top renderInfo.
	 */
	public RenderInfo getInfo() {
		return getInfo(infoStack.size() - 1);
	}

	/**
	 * This is an interface implementation. This returns this renderType object.
	 * @see RenderInfo#getRenderType()
	 * @return the renderType from the top renderInfo level.
	 */
	public RenderType getRenderType() {
		return getInfo().getRenderType();
	}

	/**
	 * get the stackresolvable from the top renderInfo.
	 * @see RenderInfo#getLevelResolvable()
	 * @return the current stackresolvable, or null if not set for this level.
	 */
	public StackResolvable getLevelResolvable() {
		return getInfo().getLevelResolvable();
	}

	/**
	 * Get the current renderurl factory.
	 * @see RenderInfo#getRenderUrlFactory()
	 * @return the current renderurl factory, or null if not set.
	 */
	public RenderUrlFactory getRenderUrlFactory() {
		return getInfo().getRenderUrlFactory();
	}

	/**
	 * create a new renderurl with the top renderurl factory.
	 * @see RenderInfo#getRenderUrl(Class, Object)
	 * @param targetObjClass the target object class of the url.
	 * @param targetObjId the id of the target object of the url.
	 * @return a new renderurl, or null if the factory is not set.
	 */
	public RenderUrl getRenderUrl(Class<? extends NodeObject> targetObjClass, Integer targetObjId) throws NodeException {
		return getInfo().getRenderUrl(targetObjClass, targetObjId);
	}

	/**
	 * get the current editmode.
	 * @see RenderInfo#getEditMode()
	 * @return the current editmode.
	 */
	public int getEditMode() {
		return getInfo().getEditMode();
	}

	/**
	 * get the current evaluation mode.
	 * @see RenderInfo#doEvaluate()
	 * @return the current evaluation mode.
	 */
	public boolean doEvaluate() {
		return getInfo().doEvaluate();
	}

	/**
	 * get the current dependency handling mode.
	 * @see RenderInfo#doHandleDependencies()
	 * @return the current dependency handling mode.
	 */
	public boolean doHandleDependencies() {
		return getInfo().doHandleDependencies();
	}

	/**
	 * get the current markup language key.
	 * @see RenderInfo#getMarkupLanguage()
	 * @return the current markup language key.
	 */
	public String getMarkupLanguage() {
		return getInfo().getMarkupLanguage();
	}

	/**
	 * get the current preferences.
	 * @see RenderInfo#getPreferences()
	 * @return the current preferences or null if not set.
	 */
	public NodePreferences getPreferences() {
		return getInfo().getPreferences();
	}

	/**
	 * get the current value of a render parameter.
	 * @see RenderInfo#getParameter(String, Object)
	 * @param name the name of the value to get.
	 * @param defaultValue the default value to use if the parameter is not set.
	 * @return the value of the parameter, or the given defaultvalue if not set.
	 */
	public Object getParameter(String name, Object defaultValue) {
		return getInfo().getParameter(name, defaultValue);
	}

	/**
	 * get the current value of a render parameter.
	 * @see RenderInfo#getParameter(String)
	 * @param name the name of the value.
	 * @return the current value or null if not set.
	 */
	public Object getParameter(String name) {
		return getInfo().getParameter(name);
	}

	/**
	 * get the current defaultRenderer key.
	 * @see RenderInfo#getDefaultRenderer()
	 * @return the current default renderer key.
	 */
	public String getDefaultRenderer() {
		return getInfo().getDefaultRenderer();
	}

	/**
	 * get a list of all allowed renderer keys.
	 * @see RenderInfo#getRendererList()
	 * @return an unmodifiable list of current renderer keys.
	 */
	public List<String> getRendererList() {
		return getInfo().getRendererList();
	}

	/**
	 * check if a renderer may be used.
	 * @see RenderInfo#useRenderer(String)
	 * @param rendererTag the key of the renderer to check.
	 * @return true, if the renderer may be used, else false.
	 */
	public boolean useRenderer(String rendererTag) {
		return getInfo().useRenderer(rendererTag);
	}

	/**
	 * get the current parameters as unmodifiable map.
	 * @see RenderInfo#getParameters()
	 * @return the current render parameters.
	 */
	public Map<String, Object> getParameters() {
		return getInfo().getParameters();
	}

	/**
	 * clear all current settings in this level.
	 * @see RenderInfo#clear()
	 */
	public void clear() {
		getInfo().clear();
	}

	/**
	 * clear all parameters in this level.
	 * @see RenderInfo#clearParameters()
	 */
	public void clearParameters() {
		getInfo().clearParameters();
	}

	/**
	 * clear all entries in the rendererlist in the current level.
	 * @see RenderInfo#clearRendererList()
	 */
	public void clearRendererList() {
		getInfo().clearRendererList();
	}

	/**
	 * set a parameter in this level.
	 * @see RenderInfo#setParameter(String, Object)
	 * @param name the name of the parameter.
	 * @param value the new value for this parameter.
	 * @return the old value of the parameter, or null if not set.
	 */
	public Object setParameter(String name, Object value) {
		return getInfo().setParameter(name, value);
	}

	/**
	 * unset a parameter in this level.
	 * @see RenderInfo#unsetParameter(String)
	 * @param name name of the parameter to remove.
	 * @return the old value of the parameter, or null if not set.
	 */
	public Object unsetParameter(String name) {
		return getInfo().unsetParameter(name);
	}

	/**
	 * set the editmode for this level.
	 * @see RenderInfo#setEditMode(int)
	 * @param editMode the new editmode.
	 */
	public void setEditMode(int editMode) {
		getInfo().setEditMode(editMode);
	}

	/**
	 * set the evaluation mode for this level.
	 * @see RenderInfo#setEvaluate(boolean)
	 * @param evaluate the new evaluation mode.
	 */
	public void setEvaluate(boolean evaluate) {
		getInfo().setEvaluate(evaluate);
	}

	/**
	 * set the dependency handling mode for this level.
	 * @see RenderInfo#setHandleDependencies(boolean)
	 * @param handleDependencies the dependency handling mode.
	 */
	public void setHandleDependencies(boolean handleDependencies) {
		getInfo().setHandleDependencies(handleDependencies);
	}

	/**
	 * set the preferences for this level.
	 * @see RenderInfo#setPreferences(NodePreferences)
	 * @param preferences the preferences for this level.
	 */
	public void setPreferences(NodePreferences preferences) {
		getInfo().setPreferences(preferences);
	}

	/**
	 * set the renderurl factory for this level
	 * @see RenderInfo#setRenderUrlFactory(RenderUrlFactory)
	 * @param urlFactory the new renderurl factory to use.
	 */
	public void setRenderUrlFactory(RenderUrlFactory urlFactory) {
		getInfo().setRenderUrlFactory(urlFactory);
	}

	/**
	 * set the default templaterenderer key for this level.
	 * @see RenderInfo#setDefaultRenderer(String)
	 * @param defaultRenderer the new default renderer key.
	 */
	public void setDefaultRenderer(String defaultRenderer) {
		getInfo().setDefaultRenderer(defaultRenderer);
	}

	/**
	 * add a renderer key to the list of allowed renderers in this level.
	 * @see RenderInfo#addRenderer(String)
	 * @param renderer the new rendererkey to add.
	 */
	public void addRenderer(String renderer) {
		getInfo().addRenderer(renderer);
	}

	/**
	 * add a rendererkey to the list of allowed renderers in this level, using a
	 * given index to insert. The index must be between 0 and size-1.
	 * @see RenderInfo#addRenderer(int, String)
	 * @param index the index for the new entry.
	 * @param renderer the renderer key to add.
	 */
	public void addRenderer(int index, String renderer) {
		getInfo().addRenderer(index, renderer);
	}

	/**
	 * remove a rendererkey from the list of allowed renderers in this level.
	 * @see RenderInfo#removeRenderer(String)
	 * @param renderer the renderer key to remove.
	 */
	public void removeRenderer(String renderer) {
		getInfo().removeRenderer(renderer);
	}

	/**
	 * set the current markup language key for this level.
	 * @see RenderInfo#setMarkupLanguage(String)
	 * @param ml the markup language key to set. 
	 */
	public void setMarkupLanguage(String ml) {
		getInfo().setMarkupLanguage(ml);
	}

	/**
	 * Get the version timestamp
	 * @return version timestamp
	 */
	public int getVersionTimestamp() {
		return versionTimestamp;
	}

	/**
	 * Will push a dependent object on to the stack of dependent objects.
	 * 
	 * Will initialize the {@link #dependencies} for the dependent object.
	 * The dependencies of the dependent object can be marked with
	 * {@link Dependency#setExisting()} or added-to by
	 * {@link #addDependency(DependencyObject, String)} and related methods.
	 * Any dependencies that were not marked as existing, or that were not
	 * newly added, will be deleted by {@link #storeDependencies()}.
	 * 
	 * For dependent objects that shouldn't be nested, like files and folders,
	 * {@link #pushRootDependentObject(DependencyObject)} should be used
	 * instead.
	 * 
	 * Dependencies that can be nested are e.g. {@link Page} or
	 * {@link TagContainer}.
	 * 
	 * @param depObj the object of which the dependencies are being processed.
	 * @throws NodeException
	 */
	public void pushDependentObject(DependencyObject depObj) throws NodeException {
		if (doHandleDependencies()) {
			if (dependentObjectStack.isEmpty()) {
				// the dependent object is not rendered within another
				// dependent object so we can proceed to initialize the
				// dependencies that currently exist.
				initDependencies(depObj.getObject(), TransactionManager.getCurrentTransaction().getChannelId());

				// push the dependent object to the stack
				pushDependentObjectNoInit(depObj);
                
			} else if (DependencyManager.DIRECT_DEPENDENCIES) {
				// this case occurs if e.g. a page is rendered in another page,
				// and DIRECT_DEPENDENCIES is true. we don't need to push a
				// dependent object or initialize dependencies, since the
				// dependencies for the nested page are simply added to the
				// containing page - that is, any modification of an
				// object that would cause the nested page to be dirted
				// will now cause the containing page to be dirted.
            
				// we still need to keep track of how many times an object
				// was pushed, so we can tell whether popping the dependent
				// object needs to be skipped.
				dependentObjectStackSkipCounter++;
                
			} else {
				// this case occurs e.g. if a page is rendered in another page,
				// and DIRECT_DEPENDENCIES is false.
                
				// TODO: to implement indirect dependencies,
				// storeDependencies() and possibly initDependencies()
				// must be modified - the list of dependencies that is
				// added-to by initDependencies() may need to be handled
				// separately for each pushed dependent object.
				// It may be possible to modify storeDependencies() to
				// store only those dependencies, where the top-most object
				// on the the dependentObjectStack is the dependent
				// object of the dependency, but I think it is best to
				// handle the list of dependencies that is initialized by
				// initDependencies() separately for each dep object.

				throw new NotYetImplementedException();
			}
		}
	}
    
	/**
	 * Like {@link #pushDependentObject(DependencyObject)}} but ensures that
	 * the dependency is being added as the root dependent object (the first
	 * dependent object.)
	 * 
	 * Dependent object like files and folders, that can't be rendered in another
	 * dependent object, should use this method.
	 * 
	 * @param depObj
	 * @throws NodeException if {@link #areDependenciesCleared()} is false.
	 */
	public void pushRootDependentObject(DependencyObject depObj) throws NodeException {
		if (doHandleDependencies()) {
			if (!areDependenciesCleared()) {
				throw new NodeException("tried to push {" + depObj + "} on to a non-empty" + " dependent object stack: " + dependentObjectStack);
			}
			pushDependentObject(depObj);
		}
	}

	/**
	 * Push a new DependencyObject onto the stack of dependent objects.
	 * This is an internal method since it doesn't initialize the
	 * {@link #dependencies} of the dependent object.
	 * @param depObject DependencyObject
	 */
	private void pushDependentObjectNoInit(DependencyObject depObject) {
		if (depObject != null) {
			// push the object on top of the stack
			dependentObjectStack.push(depObject);
			// also add it to the list (if not already there)
			if (!dependentObjects.contains(depObject)) {
				dependentObjects.add(depObject);
			}
		}
	}

	/**
	 * Initialize the dependencies for the given object.
	 * This is not done, when {@link #isStoreDependencies()} is false
	 * @param object object
	 * @param channelId channel id (if any set)
	 * @throws NodeException
	 */
	private void initDependencies(NodeObject object, Object channelId) throws NodeException {
		if (isStoreDependencies()) {
			dependencies.addAll(DependencyManager.getDependenciesForObject(object, null, null));
			if (ObjectTransformer.getInt(channelId, 0) != 0 && object instanceof LocalizableNodeObject<?>) {
				// when getting the dependencies for the object for a specific
				// channel, we also get the dependencies of the master objects
				// for that channel. The reason is that if the object is a
				// localized version, a inhertied version of that object could
				// have been published into the channel first.
				@SuppressWarnings("unchecked")
				LocalizableNodeObject<NodeObject> locObject = (LocalizableNodeObject<NodeObject>)object;
				locObject = locObject.getNextHigherObject();
				while (locObject != null) {
					dependencies.addAll(DependencyManager.getDependenciesForObject(locObject, null, null));
					locObject = locObject.getNextHigherObject();
				}
			}
			Collections.sort(dependencies);
		}
	}

	/**
	 * Remove the top object from the stack of dependent objects
	 */
	public void popDependentObject() throws NodeException {
		if (doHandleDependencies()) {
            
			if (DependencyManager.DIRECT_DEPENDENCIES) {
				if (0 != dependentObjectStackSkipCounter) {
					dependentObjectStackSkipCounter--;
					// with direct dependencies on, there will only be a single
					// dependent object on the stack, which is the root dependent
					// object. any other dependent objects will be skipped.
					return;
				}
			}            
            
			try {
				dependentObjectStack.pop();
			} catch (EmptyStackException e) {
				throw new NodeException("Error while modifying dependency stack", e);
			}
		}
	}

	/**
	 * Push a new dependent element (together with the latest dependent object)
	 * onto the dependent object stack
	 * @param depElement dependent Element
	 * @throws NodeException
	 */
	public void pushDependentElement(NodeObject depElement) throws NodeException {
		if (doHandleDependencies()) {
			try {
				DependencyObject topObject = (DependencyObject) dependentObjectStack.peek();
				DependencyObject newObject = new DependencyObject(topObject.getObjectClass(), topObject.getObjectId(), depElement);

				// push the new combination on top of the stack
				dependentObjectStack.push(newObject);
				// also add it to the list (if not already there)
				if (!dependentObjects.contains(newObject)) {
					dependentObjects.add(newObject);
				}
			} catch (EmptyStackException e) {
				throw new NodeException("Error while modifying dependency stack", e);
			}
		}
	}

	/**
	 * Add the dependency on the given property of the source object
	 * @param sourceObject source object
	 * @param sourceProperty property (may be null)
	 * @throws NodeException
	 */
	public void addDependency(NodeObject sourceObject, String sourceProperty) throws NodeException {
		addDependency(new DependencyObject(sourceObject), sourceProperty);
	}

	/**
	 * Add the dependency on the given dep object
	 * @param source source object
	 * @param sourceProperty property (may be null)
	 * @throws NodeException
	 */
	public void addDependency(DependencyObject source, String sourceProperty) throws NodeException {
		if (doHandleDependencies()) {
			// check whether at least one of source.element and sourceProperty is not null
			if (source.getElementClass() == null && sourceProperty == null) {
				logger.error(
						"Error while adding dependency for {" + source + ", " + sourceProperty
						+ "}: mod element and mod property must not both be NULL at the same time");
				return;
			}

			// special handling of dependencies on Object Tags. Add the name of the Object Tag as dependency
			if (sourceProperty == null && source.getElement() instanceof ObjectTag) {
				addDependency(source.getObject(), ((ObjectTag)source.getElement()).getName());
			}

			try {
				DependencyObject dependent = (DependencyObject) dependentObjectStack.peek();

				if (sourceProperty != null || !dependent.equals(source)) {
					Transaction t = TransactionManager.getCurrentTransaction();
					Dependency dep = DependencyManager.createDependency(source, sourceProperty, dependent, Events.UPDATE | Events.DELETE);
					int channelId = 0;
					// get the published node
					Node channel = t.getObject(Node.class, t.getPublishedNodeId(), -1, false);
					if (channel != null && channel.isChannel()) {
						channelId = ObjectTransformer.getInt(channel.getId(), 0);
					}
					dep.addChannelId(channelId);
					int index = Collections.binarySearch(dependencies, dep);

					if (index < 0) {
						// new dependency found, insert it
						NodeLogger depLogger = DependencyManager.getLogger();

						if (depLogger.isDebugEnabled()) {
							depLogger.debug("Adding Dependency {" + dep + "}");
						}
						dependencies.add(-index - 1, dep);

						// add the currently rendered property
						dep.addDependentProperty(channelId, getRenderedProperty());
					} else {
						// old dependency found, mark it as being still existent
						Dependency foundDep = (Dependency) dependencies.get(index);

						foundDep.setExisting();
						foundDep.addChannelId(channelId);
						// when in simulation mode, and the dependency is
						// the targeted one, we store the current render stack
						// in the dependency manager
						if (DependencyManager.isSimulationMode() && foundDep.getId() == DependencyManager.getTargetedSimulationDependencyId()) {
							DependencyManager.setDependencyObjectStack(stack.getObjectStack());
						}

						// add the currently rendered property
						foundDep.addDependentProperty(channelId, getRenderedProperty());
					}
				}
			} catch (EmptyStackException e) {
				throw new NodeException("Error while adding a dependency", e);
			}
		}
	}

	public void addElementDependency(NodeObject element, String sourceProperty) throws NodeException {
		if (doHandleDependencies()) {
			try {
				DependencyObject dependent = (DependencyObject) dependentObjectStack.peek();

				addDependency(new DependencyObject(dependent.getObjectClass(), dependent.getObjectId(), element), sourceProperty);
			} catch (EmptyStackException e) {
				throw new NodeException("Error while adding a dependency", e);
			}
		}
	}

	/**
	 * Reset all dependencies currently stored
	 * @throws NodeException
	 */
	public void resetDependencies() throws NodeException {
		if (doHandleDependencies()) {
			dependentObjectStackSkipCounter = 0;
			dependentObjectStack.clear();
			dependentObjects.clear();
			dependencies.clear();
		}
	}

	/**
	 * Check whether all dependencies are cleared (were stored)
	 * @return true when the dependencies are all cleared, false if not
	 */
	public boolean areDependenciesCleared() {
		return dependentObjectStack.isEmpty() && dependentObjects.isEmpty() && dependencies.isEmpty();
	}

	/**
	 * Store all currently set dependencies (first clear all dependencies for
	 * the dependent objects).
	 * If {@link #isStoreDependencies()} is false, the dependencies will not be stored, but reset
	 * @throws NodeException
	 */
	public void storeDependencies() throws NodeException {
		try {
			RuntimeProfiler.beginMark(JavaParserConstants.RENDERTYPE_STOREDEP);

			if (doHandleDependencies()) {
				if (!dependentObjectStack.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug(
								"Not storing/deleting {" + dependencies.size() + "} dependencies, since dependentObjectStack still contains at least {"
								+ dependentObjectStack.peek() + "}");
					}
				} else {
					if (isStoreDependencies()) {
						// store new dependencies, remove old ones
						DependencyManager.storeDependencies(dependencies);

						// now reset the dependencies
						resetDependencies();
					}
				}
			}
		} finally {
			RuntimeProfiler.endMark(JavaParserConstants.RENDERTYPE_STOREDEP);
		}
	}

	/**
	 * Get the current list of dependencies
	 * @return list of dependencies
	 */
	public List<Dependency> getDependencies() {
		return dependencies;
	}

	/**
	 * Get the rendered root object
	 * @return rendered root object
	 * @throws NodeException
	 */
	public StackResolvable getRenderedRootObject() throws NodeException {
		return stack.getRootObject();
	}

	/**
	 * Get the topmost tag container
	 * @return tag container
	 * @throws NodeException when no tag container was found
	 */
	public TagContainer getTopmostTagContainer() throws NodeException {
		return stack.getTopmostTagContainer();
	}

	/**
	 * Get the topmost Tag from the object stack
	 * @return topmost Tag or null if non found
	 * @throws NodeException
	 */
	public Tag getTopmostTag() throws NodeException {
		return stack.getTopmostTag();
	}

	public void setRenderContentmap(boolean renderContentmap) {
		getInfo().setRenderContentmap(renderContentmap);
	}

	public boolean isRenderContentmap() {
		return getInfo().isRenderContentmap();
	}

	/**
	 * Get the current render stack in human readable form (for logging what is currently rendered).
	 * @return human readable render stack
	 */
	public String getReadableStack() {
		return stack.getReadableStack();
	}

	/**
	 * Set the flag for collecting content and template tag id's during rendering
	 * @param collectTagIds true for collecting the id's, false for not doing that
	 */
	public void setCollectTagIds(boolean collectTagIds) {
		this.collectTagIds = collectTagIds;
		if (this.collectTagIds) {
			contentTagIds = new Vector<Object>();
			templateTagIds = new Vector<Object>();
		} else {
			contentTagIds = null;
			templateTagIds = null;
		}
	}

	/**
	 * Check whether content and template tag id's are collected during rendering
	 * @return true when tag id's are collected, false if not
	 */
	public boolean isCollectTagIds() {
		return collectTagIds;
	}

	/**
	 * Get the list of collected content tag id's
	 * @return list of collected content tag id's or null if collection is off
	 */
	public List<Object> getCollectedContentTagIds() {
		return contentTagIds;
	}

	/**
	 * Get the list of collected template tag id's
	 * @return list of collected template tag id's or null if collection is off
	 */
	public List<Object> getCollectedTemplateTagIds() {
		return templateTagIds;
	}

	/**
	 * Create a new CMSResolver and push it onto the stack.
	 * @param cmsResolver
	 */
	public void createCMSResolver() throws NodeException {
		Page page = null;
		Template template = null;
		Tag tag = null;
		Folder folder = null;
		Node node = null;
		ContentFile file = null;

		for (int i = this.getDepth() - 1; i >= 0; i--) {
			StackResolvable baseObj = this.getInfo(i).getLevelResolvable();

			if (baseObj == null) {
				continue;
			}

			if (page == null && baseObj instanceof Page) {
				page = (Page) baseObj;
			} else if (folder == null && baseObj instanceof Folder) {
				folder = (Folder) baseObj;
			} else if (template == null && baseObj instanceof Template) {
				template = (Template) baseObj;
			} else if (tag == null && baseObj instanceof Tag) {
				tag = (Tag) baseObj;
			} else if (node == null && baseObj instanceof Node) {
				node = (Node) baseObj;
			} else if (file == null && baseObj instanceof ContentFile) {
				file = (ContentFile) baseObj;
			}
		}

		cmsResolverStack.push(new CMSResolver(page, template, tag, folder, node, file));
	}

	/**
	 * Remove the topmost cms resolver from the stack
	 */
	public void popCMSResolver() {
		CMSResolver cmsResolver = (CMSResolver) cmsResolverStack.pop();

		cmsResolver.clean();
	}

	/**
	 * Get the topmost CMSResolver from the stack
	 * @return CMSResolver
	 */
	public CMSResolver getCMSResolver() {
		return (CMSResolver) cmsResolverStack.peek();
	}

	/**
	 * Set whether rendering is done in frontend mode
	 * @param frontEnd true for frontend mode, false for backend mode
	 */
	public void setFrontEnd(boolean frontEnd) {
		this.frontEnd = frontEnd;
	}

	/**
	 * Check whether rendering is done in frontend mode.
	 * The return value is only valid when in edit mode.
	 * @return true for frontend mode, false for backend mode
	 */
	public boolean isFrontEnd() {
		return frontEnd;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#setStoreDependencies(boolean)
	 */
	public void setStoreDependencies(boolean storeDependencies) {
		getInfo().setStoreDependencies(storeDependencies);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#isStoreDependencies()
	 */
	public boolean isStoreDependencies() {
		return getInfo().isStoreDependencies();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#setRenderedProperty(java.lang.String)
	 */
	public void setRenderedProperty(String renderedProperty) {
		getInfo().setRenderedProperty(renderedProperty);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#getRenderedProperty()
	 */
	public String getRenderedProperty() {
		return getInfo().getRenderedProperty();
	}

	/**
	 * Preserve the dependencies for the given rendered property (which is not actually rendered)
	 * @param renderedProperty property
	 * @throws NodeException
	 */
	public void preserveDependencies(String renderedProperty) throws NodeException {
		if (doHandleDependencies() && dependencies != null) {
			Transaction t = TransactionManager.getCurrentTransaction();
			DependencyObject dependent = (DependencyObject) dependentObjectStack.peek();

			int channelId = 0;
			// get the published node
			Node channel = t.getObject(Node.class, t.getPublishedNodeId(), -1, false);
			if (channel != null && channel.isChannel()) {
				channelId = ObjectTransformer.getInt(channel.getId(), 0);
			}
			for (Dependency dep : dependencies) {
				if (dep.getDependent().equals(dependent) && dep.getStoredDependentProperties()
						.getOrDefault(renderedProperty, Collections.emptySet()).contains(channelId)
						&& dep.getChannelIds().contains(channelId)) {
					dep.addDependentProperty(channelId, renderedProperty);
					dep.setExisting();
				}
			}
		}
	}

	/**
	 * Get current content language
	 * @return current content language (may be null)
	 */
	public ContentLanguage getLanguage() {
		Object lang = getParameter(LANGUAGE_PARAM);
		if (lang instanceof ContentLanguage) {
			return (ContentLanguage) lang;
		} else {
			return null;
		}
	}

	/**
	 * Set the current content language
	 * @param language language
	 */
	public void setLanguage(ContentLanguage language) {
		setParameter(LANGUAGE_PARAM, language);
	}

	/**
	 * Set the parameter of the instance to the given value and return an {@link AutoCloseable} instance, that will
	 * set the parameter back to the original value, when closed.
	 * 
	 * @param name parameter name
	 * @param value new parameter value
	 * @return autocloseable instance
	 */
	public ParameterScope withParameter(String name, Object value) {
		return new ParameterScope(name, value);
	}

	/**
	 * AutoCloseable instance that will set the parameter back to the original value when closed
	 */
	public class ParameterScope implements AutoCloseable {
		/**
		 * Parameter name
		 */
		protected String name;

		/**
		 * Old value
		 */
		protected Object oldValue;

		/**
		 * Create instance, set the parameter to the new value
		 * @param name parameter name
		 * @param newValue new value
		 */
		public ParameterScope(String name, Object newValue) {
			this.name = name;
			oldValue = getParameter(name);
			setParameter(name, newValue);
		}

		@Override
		public void close() {
			setParameter(name, oldValue);
		}
	}

	/**
	 * {@link AutoCloseable} implementation, which will pop the given {@link StackResolvable} from the stack,
	 * if it is the top stack entry and will push the popped object back when {@link RemoveTopResolvable#close()} is called.
	 */
	public class RemoveTopResolvable implements AutoCloseable {
		/**
		 * Popped object (which is equal but possibly not identical to the given object)
		 */
		protected StackResolvable popped;

		/**
		 * Create an instance
		 * @param toPop object to pop from the stack
		 */
		protected RemoveTopResolvable(StackResolvable toPop) {
			if (infoStack.size() <= 1) {
				return;
			}
			final StackResolvable levelResolvable = getInfo().getLevelResolvable();

			if (!Objects.equals(toPop, levelResolvable)) {
				return;
			}
			infoStack.pop();
			if (levelResolvable != null) {
				stack.remove(levelResolvable);
			}

			popped = levelResolvable;
		}

		@Override
		public void close() {
			if (popped != null) {
				push(popped);
			}
		}
	}
}
