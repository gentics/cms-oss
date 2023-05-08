package com.gentics.contentnode.publish.wrapper;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * Wrapper for the REST Model of a markup language.
 * Instances of this class will be used when versioned publishing is active and multithreaded publishing is used.
 * See {@link PublishablePage} for details.
 */
public class PublishableMarkupLanguage extends MarkupLanguage {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1913139043137250056L;

	/**
	 * Wrapped NodeObject
	 */
	protected com.gentics.contentnode.rest.model.MarkupLanguage wrappedMarkupLanguage;

	/**
	 * Create an instance
	 * @param template owning template
	 */
	public PublishableMarkupLanguage(PublishableTemplate template) {
		super(template.wrappedTemplate.getMarkupLanguage().getId(), null);
		this.wrappedMarkupLanguage = template.wrappedTemplate.getMarkupLanguage();
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		if (info == null) {
			info = new PublishableNodeObjectInfo(MarkupLanguage.class, -1);
		}

		return info;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#copy()
	 */
	public NodeObject copy() throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public String getName() {
		return wrappedMarkupLanguage.getName();
	}

	@Override
	public String getExtension() {
		return wrappedMarkupLanguage.getExtension();
	}

	@Override
	public String getContentType() {
		return wrappedMarkupLanguage.getContentType();
	}

	@Override
	public Feature getFeature() {
		return Feature.getByName(wrappedMarkupLanguage.getFeature());
	}

	@Override
	public boolean isExcludeFromPublishing() {
		return wrappedMarkupLanguage.isExcludeFromPublishing();
	}
}
