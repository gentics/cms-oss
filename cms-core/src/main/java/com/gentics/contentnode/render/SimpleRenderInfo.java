/*
 * @author Stefan Hepp
 * @date 31.12.2005
 * @version $Id: SimpleRenderInfo.java,v 1.8 2007-08-17 10:37:26 norbert Exp $
 */
package com.gentics.contentnode.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * A simple container implementation of RenderInfo with lots of constructors to pass information.
 * @see RenderInfo
 */
public class SimpleRenderInfo implements RenderInfo {

	private int editMode;
	private boolean evaluate;
	private boolean handleDependencies;

	/**
	 * Flag to mark, whether collected dependencies shall be stored
	 */
	private boolean storeDependencies = true;

	private RenderUrlFactory urlFactory;
	private NodePreferences preferences;
	private Map<String, Object> parameters;
	private String defaultRenderer;
	private List<String> rendererList;
	private String ml;
	private StackResolvable levelResolvable;
	private RenderType renderType;
	private boolean renderContentmap;

	/**
	 * Currently rendered property
	 */
	private String renderedProperty;

	public SimpleRenderInfo(int editMode, boolean evaluate, boolean handleDependencies, RenderUrlFactory urlFactory,
			NodePreferences preferences, Map<String, Object> parameters, String defaultRenderer, List<String> rendererList,
			String ml, StackResolvable levelResolvable, RenderType renderType) {
		this.editMode = editMode;
		this.evaluate = evaluate;
		this.handleDependencies = handleDependencies;
		this.urlFactory = urlFactory;
		this.preferences = preferences;
		this.renderType = renderType;
		this.parameters = new HashMap<String, Object>(parameters);
		this.defaultRenderer = defaultRenderer;
		this.rendererList = new ArrayList<String>(rendererList);
		this.ml = ml;
		this.levelResolvable = levelResolvable;
		this.renderContentmap = false;
	}

	public SimpleRenderInfo(RenderInfo info, StackResolvable resolvable) {
		this.editMode = info.getEditMode();
		this.evaluate = info.doEvaluate();
		this.handleDependencies = info.doHandleDependencies();
		this.urlFactory = info.getRenderUrlFactory();
		this.preferences = info.getPreferences();
		this.parameters = new HashMap<String, Object>(info.getParameters());
		this.defaultRenderer = info.getDefaultRenderer();
		this.rendererList = new ArrayList<String>(info.getRendererList());
		this.ml = info.getMarkupLanguage();
		this.levelResolvable = resolvable;
		renderType = info.getRenderType();
		this.renderContentmap = info.isRenderContentmap();
		this.renderedProperty = info.getRenderedProperty();
		storeDependencies = info.isStoreDependencies();
	}

	public SimpleRenderInfo(RenderType renderType, RenderInfo info) {
		this.editMode = info.getEditMode();
		this.evaluate = info.doEvaluate();
		this.handleDependencies = info.doHandleDependencies();
		this.urlFactory = info.getRenderUrlFactory();
		this.preferences = info.getPreferences();
		this.parameters = new HashMap<String, Object>(info.getParameters());
		this.defaultRenderer = info.getDefaultRenderer();
		this.rendererList = new ArrayList<String>(info.getRendererList());
		this.ml = info.getMarkupLanguage();
		this.levelResolvable = info.getLevelResolvable();
		this.renderType = renderType;
		this.renderContentmap = info.isRenderContentmap();
		this.renderedProperty = info.getRenderedProperty();
		this.storeDependencies = info.isStoreDependencies();
	}

	public SimpleRenderInfo(RenderType renderType, StackResolvable resolvable) {
		this.renderType = renderType;
		this.editMode = RenderType.EM_PUBLISH;
		this.evaluate = true;
		this.handleDependencies = true;
		this.urlFactory = null;
		this.preferences = null;
		this.parameters = new HashMap<String, Object>();
		this.defaultRenderer = "";
		this.rendererList = new ArrayList<String>(RendererFactory.getRendererKeys());
		this.ml = null;
		this.levelResolvable = resolvable;
		this.renderContentmap = false;
		this.storeDependencies = true;
	}

	public RenderType getRenderType() {
		return renderType;
	}

	public StackResolvable getLevelResolvable() {
		return levelResolvable;
	}

	public RenderUrlFactory getRenderUrlFactory() {
		return urlFactory;
	}

	public RenderUrl getRenderUrl(Class<? extends NodeObject> targetObjClass, Integer targetObjId) throws NodeException {
		if (urlFactory == null) {
			return null;
		}
		return urlFactory.createRenderUrl(targetObjClass, targetObjId);
	}

	public int getEditMode() {
		return editMode;
	}

	public boolean doEvaluate() {
		return evaluate;
	}

	public boolean doHandleDependencies() {
		return handleDependencies;
	}

	public String getMarkupLanguage() {
		return ml;
	}

	public NodePreferences getPreferences() {
		return preferences;
	}

	public Object getParameter(String name, Object defaultValue) {
		Object obj = parameters.get(name);

		return obj != null ? obj : defaultValue;
	}

	public Object getParameter(String name) {
		return getParameter(name, null);
	}

	public String getDefaultRenderer() {
		return defaultRenderer;
	}

	public List<String> getRendererList() {
		return Collections.unmodifiableList(rendererList);
	}

	public boolean useRenderer(String rendererTag) {
		return rendererList.contains(rendererTag);
	}

	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	public void clear() {
		clearParameters();
		clearRendererList();

		// TODO reset other vars
	}

	public void clearParameters() {
		parameters.clear();
	}

	public void clearRendererList() {
		rendererList.clear();
	}

	public Object setParameter(String name, Object value) {
		return parameters.put(name, value);
	}

	public Object unsetParameter(String name) {
		return parameters.remove(name);
	}

	public void setEditMode(int editMode) {
		this.editMode = editMode;
	}

	public void setEvaluate(boolean evaluate) {
		this.evaluate = evaluate;
	}

	public void setHandleDependencies(boolean handleDependencies) {
		this.handleDependencies = handleDependencies;
	}

	public void setPreferences(NodePreferences preferences) {
		this.preferences = preferences;
	}

	public void setRenderUrlFactory(RenderUrlFactory urlFactory) {
		this.urlFactory = urlFactory;
	}

	public void setDefaultRenderer(String defaultRenderer) {
		this.defaultRenderer = defaultRenderer;
	}

	public void addRenderer(String renderer) {
		rendererList.add(renderer);
	}

	public void addRenderer(int index, String renderer) {
		rendererList.add(index, renderer);
	}

	public void removeRenderer(String renderer) {
		rendererList.remove(renderer);
	}

	public void setMarkupLanguage(String ml) {
		this.ml = ml;
	}

	public void setRenderContentmap(boolean renderContentmap) {
		this.renderContentmap = renderContentmap;
	}

	public boolean isRenderContentmap() {
		return renderContentmap;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#setStoreDependencies(boolean)
	 */
	public void setStoreDependencies(boolean storeDependencies) {
		this.storeDependencies = storeDependencies;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#isStoreDependencies()
	 */
	public boolean isStoreDependencies() {
		return storeDependencies;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#setRenderedProperty(java.lang.String)
	 */
	public void setRenderedProperty(String renderedProperty) {
		this.renderedProperty = renderedProperty;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.RenderInfo#getRenderedProperty()
	 */
	public String getRenderedProperty() {
		return renderedProperty;
	}
}
