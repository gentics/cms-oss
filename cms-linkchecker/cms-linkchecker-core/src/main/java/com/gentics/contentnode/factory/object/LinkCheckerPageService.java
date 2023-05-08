package com.gentics.contentnode.factory.object;

import java.util.Collection;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.page.linkchecker.ExternalLinkDiff;
import com.gentics.contentnode.object.page.linkchecker.LinkChecker;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Service that scans the content for external links, when a page is saved and the link checker feature is activated
 */
public class LinkCheckerPageService implements PageService {
	@Override
	public void onSave(Page object, boolean isNew, boolean contentModified, int editorId) throws NodeException {
		if (contentModified) {
			boolean linkChecker = false;
			int contentId = object.getContent().getId();
			for (Page page : object.getContent().getPages()) {
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.LINK_CHECKER, page.getOwningNode())) {
					linkChecker = true;
					break;
				}
			}
			if (linkChecker) {
				Collection<ExternalLink> existing = LinkChecker.load(contentId);
				ExternalLinkDiff diff = LinkChecker.getDiff(existing, object.getContent());
				LinkChecker.persist(contentId, diff);
			}
		}
	}
}
