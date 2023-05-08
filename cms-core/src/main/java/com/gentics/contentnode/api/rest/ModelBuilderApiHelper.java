package com.gentics.contentnode.api.rest;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.util.ModelBuilder;

/**
 * The {@link ModelBuilderApiHelper} provides some utility methods that can be used to deal with rest models.
 * 
 * @author johannes2
 * 
 */
public class ModelBuilderApiHelper {

	/**
	 * Renders the page using the given template in the live preview mode and returns the rendered content.
	 * 
	 * @param restPage
	 * @param template
	 * @return
	 * @throws NodeException
	 */
	public static String renderPage(Page restPage, String template) throws NodeException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();

			com.gentics.contentnode.object.Page page = ModelBuilder.getPage(restPage, true);

			NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();
			RenderType renderType = RenderType.getDefaultRenderType(nodePreferences, RenderType.EM_ALOHA_READONLY, t.getSessionId(), 0);
			renderType.setLanguage(page.getLanguage());
			t.setRenderType(renderType);
			// push the page onto the rendertype stack
			renderType.push(page);
			RenderResult renderResult = new RenderResult();

			TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());
			return renderer.render(renderResult, template);
		} catch (Exception e) {
			throw new NodeException("Error while rendering page.", e);
		}
	}

	/**
	 * Renders the given page and returns the rendered content
	 * 
	 * @param restPage
	 * @return
	 * @throws NodeException
	 */
	public static String renderPage(Page restPage) throws NodeException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.Page page = ModelBuilder.getPage(restPage, true);
			RenderResult renderResult = new RenderResult();
			t.setRenderResult(renderResult);
			return page.render(renderResult);
		} catch (Exception e) {
			throw new NodeException("Error while rendering page.", e);
		}

	}
}
