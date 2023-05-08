/*
 * @author Stefan Hepp
 * @date 1.2.2006
 * @version $Id: RenderInfo.java,v 1.9.22.1 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.render;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.resolving.StackResolvable;

import java.util.List;
import java.util.Map;

/**
 * RenderInfo stores informations about the way an object or template should be rendered.
 * It also provides methods to create new renderurls.
 */
public interface RenderInfo {
    
	/**
	 * Parameter defining if publish - debug is activated (Boolean value)
	 */
	public static final String PARAMETER_DEBUG_PUBLISH = "debugPublish";

	/**
	 * folder id of currently rendered folder, only used in edit & preview modes
	 */
	public static final String PARAMETER_CURRENT_FOLDER_ID = "folderId";

	/**
	 * Get a reference to the current renderType.
	 * @return the rendertype object, to which this renderInfo belongs to.
	 */
	RenderType getRenderType();

	/**
	 * Get the StackResolvable object, which was registered for this RenderInfo
	 * instance.
	 * @return the current StackResolvable, or null if no object was registered.
	 */
	StackResolvable getLevelResolvable();

	/**
	 * Get the current RenderUrlFactory.
	 * @return the RenderUrlFactory used to create new urls.
	 */
	RenderUrlFactory getRenderUrlFactory();

	/**
	 * create a new renderUrl, using the current renderFactory.
	 *
	 * @see RenderUrl
	 * @param targetObjClass class of the target of the url.
	 * @param targetObjId id of the target of the object.
	 * @return a new RenderUrl, or null if no renderFactory is available.
	 * @throws NodeException 
	 */
	RenderUrl getRenderUrl(Class<? extends NodeObject> targetObjClass, Integer targetObjId) throws NodeException;

	/**
	 * Get the current editmode which should be used to render objects.
	 * For available constants, see {@link RenderType}.
	 *
	 * @return the current edit mode.
	 */
	int getEditMode();

	/**
	 * Check, if expressions and conditions in the template should be evaluated.
	 * @return True, if expressions and conditions should be evaluated during parsing, else false.
	 */
	boolean doEvaluate();

	/**
	 * Check, if dependencies should be updated during parsing.
	 * @return True, if dependencies should be updated, else false.
	 */
	boolean doHandleDependencies();

	/**
	 * Get the current markup language keyname, for which this template should be rendered,
	 * or null if the markup language is not set.
	 * @return the current markuplanguage key or null if none is set.
	 */
	String getMarkupLanguage();

	/**
	 * Returns the current Preferences, either the default preferences, or the preferences
	 * for the current user.
	 * @return the current preferences, or null if no preferences are set.
	 */
	NodePreferences getPreferences();

	/**
	 * Get a parameter, which was previously set. A parameter can be any object.
	 * This can be used to pass implementation-specific render-informations to sub-render calls.
	 *
	 * @param name name of the parameter to get.
	 * @param defaultValue the defaultvalue to use if the parameter is not set.
	 * @return the parameter, or the defaultValue if the parameter is not set.
	 */
	Object getParameter(String name, Object defaultValue);

	/**
	 * Get a parameter, which was previously set. A parameter can be any object.
	 * This can be used to pass implementation-specific render-informations to sub-render calls.
	 *
	 * @param name the name of the parameter to get.
	 * @return the parameter, or null if the parameter is not set.
	 */
	Object getParameter(String name);

	/**
	 * Get the key of the of the templaterenderer which should be used to render sub-templates.
	 * This should always be set to a valid key, which is known by {@link RendererFactory}.
	 *
	 * @return the key of the current default templaterenderer.
	 */
	String getDefaultRenderer();

	/**
	 * get a (sorted) list of all allowed templaterenderers. All keys must be known
	 * by {@link RendererFactory}.
	 * @return a list of allowed templaterenderers for sub-render calls.
	 */
	List<String> getRendererList();

	/**
	 * Check if a given templaterenderer should be used, i.e. is an element of the rendererList.
	 * @param rendererTag the key of the templaterenderer to check.
	 * @return true, if the renderer may be used, else false.
	 */
	boolean useRenderer(String rendererTag);

	/**
	 * Get a map of all currently set parameters. The keys are the parameter-names as string,
	 * the values are the parameter-objects.
	 * @return a map of string->object of all parameters.
	 */
	Map<String, Object> getParameters();

	/**
	 * reset all infos, including parametermap and rendererlist, but not the url-factory.
	 */
	void clear();

	/**
	 * clear the parameter map.
	 */
	void clearParameters();

	/**
	 * clear the allowed renderer list.
	 */
	void clearRendererList();

	/**
	 * Set a parameter to a given value.
	 * @param name name of the parameter to set.
	 * @param value the new value of the parameter.
	 * @return the previously set value, or null if it was not set.
	 */
	Object setParameter(String name, Object value);

	/**
	 * unset a given parameter.
	 * @param name name of the parameter to clear.
	 * @return the previously stored value, or null if it was not set.
	 */
	Object unsetParameter(String name);

	/**
	 * Set the current editmode. Constants for valid modes are
	 * defined in {@link RenderType}.
	 *
	 * @see #getEditMode()
	 * @param editMode the new editmode.
	 */
	void setEditMode(int editMode);

	/**
	 * set the current evaluation mode.
	 * @see #doEvaluate()
	 * @param evaluate the new value for the evaluation mode.
	 */
	void setEvaluate(boolean evaluate);

	/**
	 * set the new dependency handling mode.
	 * @see #doHandleDependencies()
	 * @param handleDependencies the new dependency handling mode.
	 */
	void setHandleDependencies(boolean handleDependencies);

	/**
	 * set the current preferences which should be used by render methods.
	 * @see #getPreferences()
	 * @param preferences the current preferences to use.
	 */
	void setPreferences(NodePreferences preferences);

	/**
	 * set the current urlfactory to use to create new urls.
	 * @param urlFactory the new urlfactory.
	 */
	void setRenderUrlFactory(RenderUrlFactory urlFactory);

	/**
	 * set the current default renderer. The value must be a known key of
	 * {@link RendererFactory}.
	 *
	 * @see #getDefaultRenderer()
	 * @param defaultRenderer the new default templaterenderer key.
	 */
	void setDefaultRenderer(String defaultRenderer);

	/**
	 * add a templaterenderer key to the list of allowed renderer keys.
	 * @see #getRendererList()
	 * @param renderer a new rendererkey.
	 */
	void addRenderer(String renderer);

	/**
	 * Add a renderer to the list of allowed renderers, using a given index to
	 * insert the key into the list. The position must be less or equal the size
	 * of the current renderer-list size.
	 * @see #getRendererList()
	 * @param index the position of the new entry, starting with 0.
	 * @param renderer the key of the templaterenderer to add to the list.
	 */
	void addRenderer(int index, String renderer);

	/**
	 * remove a renderer-key from the list of the allowed renderers.
	 * @see #getRendererList()
	 * @param renderer the key to remove.
	 */
	void removeRenderer(String renderer);

	/**
	 * set the current markup language key.
	 * @param ml the new markup language key, or null if no markup language is available.
	 */
	void setMarkupLanguage(String ml);

	/**
	 * Set/clear the flag for contentmap rendering
	 * @param renderContentmap true when contentmap rendering shall be set, false if not
	 */
	void setRenderContentmap(boolean renderContentmap);

	/**
	 * Get the flag for contentmap rendering
	 * @return true for contentmap rendering, false if not
	 */
	boolean isRenderContentmap();

	/**
	 * Set whether collected dependencies shall be stored.
	 * @param storeDependencies true to store collected dependencies
	 */
	void setStoreDependencies(boolean storeDependencies);

	/**
	 * Get true if collected dependencies shall be stored, false if not
	 * @return true for storing collected dependencies
	 */
	boolean isStoreDependencies();

	/**
	 * Set the currently rendered property
	 * @param renderedProperty currently rendered property
	 */
	void setRenderedProperty(String renderedProperty);

	/**
	 * Get the currently rendered property
	 * @return currently rendered property
	 */
	String getRenderedProperty();
}
