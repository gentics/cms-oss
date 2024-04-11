package com.gentics.contentnode.factory.url;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;

/**
 * This factory gerenates urls which work for the Content.Node Frontend.
 */
public class DynamicUrlFactory extends AbstractRenderUrlFactory {

	private static final int DO_FILE = 16000;
	private static final int DO_PAGE = 14001;
	private static final int DO_REAL = 14012;

	public final static String STAG_PREFIX_PARAM = "stag_prefix";
    
	/**
	 * Configuration parameter name that is used to prefix the Portlet application
	 */
	public final static String PORTLETAPP_PREFIX_PARAM = "portletapp_prefix";

	/**
	 * Configuration parameter name that is used to store the proxy prefix setting
	 */
	public final static String PROXY_PREFIX_PARAM = "proxy_prefix";
    
	private String cnSessionId;

	/**
	 * This is the factory implementation of the renderurl.
	 */
	private class MyUrl extends AbstractContentRenderUrl {

		private MyUrl(Class targetClass, Integer targetId) {
			super(targetClass, targetId);
		}

		protected String renderFolder(Folder folder) throws NodeException {
			return generateFolderUrl(folder, LINK_HOST);
		}

		protected String renderFile(File file) throws NodeException {
			boolean dontAppendFilename = TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getFeature(
					"disablepreviewurlappendfilename");

			return String.format("/rest/file/content/load/%d?sid=%s&nodeId=%d%s", file.getId(),
					cnSessionId, file.getNode().getId(), dontAppendFilename ? "" : "&fingerprint=" + file.getMd5());
		}

		protected String renderPage(Page page) throws NodeException {

			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			int editMode = renderType.getEditMode();
            
			StringBuffer url = new StringBuffer(100);

			String extension = null;

			// I am sure this can't be null .. but.. i'll better check it anyway ..
			if (page != null && page.getTemplate() != null && page.getTemplate().getMarkupLanguage() != null) {
				extension = page.getTemplate().getMarkupLanguage().getExtension();
			}
			String realViewMode = "newview";
			if ((editMode == RenderType.EM_ALOHA || editMode == RenderType.EM_ALOHA_READONLY
					|| editMode == RenderType.EM_PREVIEW)
					&& !"css".equalsIgnoreCase(extension)
					&& !"js".equalsIgnoreCase(extension)) {

				if (editMode == RenderType.EM_ALOHA || editMode == RenderType.EM_ALOHA_READONLY) {
					url.append(renderType.getPreferences().getProperty(PORTLETAPP_PREFIX_PARAM));
					url.append("alohapage");
					url.append("?nodeid=");
					url.append(page.getFolder().getNode().getId());
					url.append("&language=");
					url.append(page.getLanguageId());
					url.append("&sid=");
					url.append(cnSessionId);
					url.append("&real=").append(realViewMode);
					url.append("&realid=");
				} else {
					// when in aloha or normal edit mode or preview (and the linked page is neither a css, nor a js),
					// the links will go to normal preview
					url.append(renderType.getPreferences().getProperty(PORTLETAPP_PREFIX_PARAM));
					url.append("rest/page/render/content/");
					url.append(page.getId());
					url.append("?nodeId=");
					url.append(page.getFolder().getNode().getId());
					url.append("&sid=");
					url.append(cnSessionId);
					return url.toString();
				}
			} else {
				// all other cases: links will go to live preview. Especially for css and js pages, because we must not render
				// javascript into css or js pages
				url.append(renderType.getPreferences().getProperty(PORTLETAPP_PREFIX_PARAM));
				url.append("rest/page/render/content/");
				url.append(page.getId());
				url.append("?nodeId=");
				url.append(page.getFolder().getNode().getId());
				url.append("&sid=");
				url.append(cnSessionId);
				return url.toString();
			}

			url.append(page.getId());

			return url.toString();
		}

		protected String renderContentTag(ContentTag contentTag) throws NodeException {
			return renderContentOrTemplateTag(contentTag, "page");
		}
        
		protected String renderContentOrTemplateTag(Tag tag, String urlType) throws NodeException {
			// determine the correct do
			int theDo = tag.containsOverviewPart() ? 17001 : 10008;
			StringBuffer url = new StringBuffer(100);

			prepareUrlWithTimestamp(url);
			// the empty parameters (back, backparam) are just there for
			// compatibility with php
			url.append("&do=").append(theDo).append("&id=").append(tag.getId()).append("&type=" + urlType + "&keepsid=1&back=&backparam=");

			return url.toString();            
		}
        
		protected String renderTemplateTag(TemplateTag templateTag) throws NodeException {
			return renderContentOrTemplateTag(templateTag, "template");
		}
        
		/**
		 * Prepares an Url that calls the specified servlet with some necessary parameters.
		 * 
		 * @param url StringBuffer that will be filled with the URL
		 * @param servletName Path of the servlet to call
		 * @param setLanguage Specifies if the current language should be rendered as "language" parameter into the url.
		 * @return Generated Url
		 * @throws NodeException If ContentNodeHelper doesn't have a language set or no current transaction is set
		 */
		protected StringBuffer prepareJavaUrl(StringBuffer url, String servletName, boolean setLanguage) throws NodeException {
			url.append(getPortletappPrefix());
			url.append(servletName);
			url.append("?sid=");
			url.append(cnSessionId);
			if (setLanguage) {
				url.append("&language=");
				url.append(ContentNodeHelper.getLanguageId());
			}
			url.append("&time=");
			url.append(TransactionManager.getCurrentTransaction().getTimestamp());
			return url;
		}
        
		protected StringBuffer prepareUrl(StringBuffer url) throws NodeException {
			url.append(getStagPrefix());
			url.append("?sid=");
			url.append(cnSessionId);
			return url;
		}
        
		protected StringBuffer prepareUrlWithFingerprint(StringBuffer url, String fingerprint) throws NodeException {
			url = prepareUrl(url);
			url.append("&fingerprint=").append(fingerprint);
			return url;
		}
        
		protected StringBuffer prepareUrlWithTimestamp(StringBuffer url) throws NodeException {
			url = prepareUrl(url);
			url.append("&time=");
			url.append(TransactionManager.getCurrentTransaction().getTimestamp());
			return url;
		}
        
		protected String renderOfflinePage(Page page) throws NodeException {
			return "#";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.render.RenderUrl#getLinkManagement()
		 */
		public LinkManagement getLinkManagement() {
			return DynamicUrlFactory.this.getLinkManagement();
		}
	}

	private class MyEmptyURL implements RenderUrl {

		public void setMode(int mode) {}

		public void setSourceObject(NodeObject source) throws NodeException {}

		public int getMode() {
			return 0;
		}

		public Class getTargetClass() {
			return null;
		}

		public Integer getTargetId() {
			return null;
		}

		public NodeObject getSourceObject() {
			return null;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			try {
				StringBuffer url = new StringBuffer();

				url.append(getStagPrefix());
				url.append("?sid=");
				url.append(cnSessionId);
				url.append("&time=");
				url.append(TransactionManager.getCurrentTransaction().getTimestamp());
				return url.toString();
			} catch (NodeException e) {
				return "";
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.render.RenderUrl#getLinkManagement()
		 */
		public LinkManagement getLinkManagement() {
			return DynamicUrlFactory.this.getLinkManagement();
		}
	}

	/**
	 * Get the URL prefix for the Portletapp
	 * @return The URL prefix for the Portletapp 
	 * @throws NodeException
	 */
	private String getPortletappPrefix() throws NodeException {
		return TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getProperty(PORTLETAPP_PREFIX_PARAM);
	}
    
	private String getStagPrefix() throws NodeException {
		return TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getProperty(STAG_PREFIX_PARAM);
	}

	/**
	 * create a new urlfactory, which creates url valid for a given session-id.
	 *
	 * @param cnSessionId the session-id of the contentnode-frontend.
	 */
	public DynamicUrlFactory(String cnSessionId) {
		this.cnSessionId = cnSessionId;
	}

	public RenderUrl createRenderUrl(Class targetObjClass, Integer targetObjId) {
		if (targetObjClass == null && targetObjId == null) {
			return new MyEmptyURL();
		} else {
			return new MyUrl(targetObjClass, targetObjId);
		}
	}
}
