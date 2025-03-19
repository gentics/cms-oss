package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrlFactory;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * Autoclosable for setting a specific rendertype to the current transaction
 */
public class RenderTypeTrx implements AutoCloseable {
	/**
	 * Old rendertype
	 */
	protected RenderType oldRenderType;

	/**
	 * New rendertype
	 */
	protected RenderType renderType;

	protected NodeObject object;

	protected boolean publishDataSet;

	/**
	 * Create an instance for the {@link RenderType#EM_PUBLISH}
	 * @return instance
	 * @throws NodeException
	 */
	public static RenderTypeTrx publish() throws NodeException {
		return new RenderTypeTrx(RenderType.EM_PUBLISH, null, false, true, false);
	}

	/**
	 * Create an instance for publishing the object
	 * @param object published object
	 * @return instance
	 * @throws NodeException
	 */
	public static RenderTypeTrx publish(NodeObject object) throws NodeException {
		return new RenderTypeTrx(RenderType.EM_PUBLISH, object, true, true, false);
	}

	/**
	 * Set the rendertype to the given editmode. If editmode is
	 * {@link RenderType#EM_PUBLISH}, the {@link StaticUrlFactory} will be set
	 * (with linkway AUTO), otherwise the {@link DynamicUrlFactory}
	 *
	 * @param editMode edit mode
	 * @throws NodeException
	 */
	public RenderTypeTrx(int editMode) throws NodeException {
		this(editMode, null, true, true, false);
	}

	/**
	 * Set the rendertype to the given editmode. If editmode is
	 * {@link RenderType#EM_PUBLISH}, the {@link StaticUrlFactory} will be set
	 * (with linkway AUTO), otherwise the {@link DynamicUrlFactory}
	 *
	 * @param editMode edit mode
	 * @param object rendered object
	 * @param handleDependencies true if dependencies shall be handled (when publishing)
	 * @param storeDependencies true if dependencies shall be stored (when publishing)
	 * @param isPublishProcess true if the transaction is used during a publish process (and not during for example instant publishing)
	 * @throws NodeException
	 */
	public RenderTypeTrx(int editMode, NodeObject object, boolean handleDependencies, boolean storeDependencies, boolean isPublishProcess) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		oldRenderType = t.getRenderType();
		RenderUrlFactory oldRenderUrlFactory = oldRenderType != null ? oldRenderType.getRenderUrlFactory() : null;

		renderType = RenderType.getDefaultRenderType(t.getNodeConfig().getDefaultPreferences(), editMode, t.getSessionId(), -1);
		if (!handleDependencies) {
			renderType.setHandleDependencies(false);
		}
		if (!storeDependencies) {
			renderType.setStoreDependencies(false);
		}
		t.setRenderType(renderType);
		// set the url factory
		switch (editMode) {
		case RenderType.EM_PUBLISH:
			renderType.setRenderUrlFactory(new StaticUrlFactory(RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway")),
					RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway_file")), ""));
			if (isPublishProcess && t.getPublishData() == null) {
				PublishData publishData = PublishController.getPublishData();
				if (publishData != null) {
					t.setPublishData(publishData);
					publishDataSet = true;
				}
			}
			break;
		default:
			// if the previous rendertype had a dynamicurlfactory set, we reuse it
			if (oldRenderUrlFactory instanceof DynamicUrlFactory) {
				renderType.setRenderUrlFactory(oldRenderUrlFactory);
			} else {
				renderType.setRenderUrlFactory(new DynamicUrlFactory(t.getSessionId()));
			}
			break;
		}

		if (object != null) {
			this.object = object;

			if (renderType.doHandleDependencies()) {
				DependencyObject depObject = new DependencyObject(object);
				renderType.pushRootDependentObject(depObject);
			}

			renderType.push((StackResolvable)object);

			if (object instanceof Page) {
				renderType.setLanguage(((Page) object).getLanguage());
			}
		}
	}

	/**
	 * Get the rendertype
	 * @return rendertype
	 */
	public RenderType get() {
		return renderType;
	}

	@Override
	public void close() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (object != null) {
			RenderType renderType = t.getRenderType();
			renderType.pop((StackResolvable)object);

			if (renderType.doHandleDependencies()) {
				renderType.popDependentObject();
				renderType.storeDependencies();
			}
		}
		t.setRenderType(oldRenderType);
		if (publishDataSet) {
			t.setPublishData(null);
		}
	}
}
