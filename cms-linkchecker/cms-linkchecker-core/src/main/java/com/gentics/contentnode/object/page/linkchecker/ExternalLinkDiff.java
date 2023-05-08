package com.gentics.contentnode.object.page.linkchecker;

import java.util.ArrayList;
import java.util.Collection;

import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;

/**
 * Class containing the diff for external links contained in a page
 */
public class ExternalLinkDiff {
	/**
	 * Collection of new links (to be inserted)
	 */
	private Collection<ExternalLink> newLinks = new ArrayList<>();

	/**
	 * Collection of modified links (to be updated), for which the URL was not changed
	 */
	private Collection<ExternalLink> modifiedLinks = new ArrayList<>();

	/**
	 * Collection of modified links (to be updated), for which the URL was also changed
	 */
	private Collection<ExternalLink> modifiedUrlLinks = new ArrayList<>();

	/**
	 * Collection of removed links (to be deleted)
	 */
	private Collection<ExternalLink> removedLinks = new ArrayList<>();

	/**
	 * Get links to be inserted
	 * @return collection of new links
	 */
	public Collection<ExternalLink> getNewLinks() {
		return newLinks;
	}

	/**
	 * Get links to be updated
	 * @return collection of modified links
	 */
	public Collection<ExternalLink> getModifiedLinks() {
		return modifiedLinks;
	}

	/**
	 * Get links to be updated
	 * @return collection of modified links
	 */
	public Collection<ExternalLink> getModifiedUrlLinks() {
		return modifiedUrlLinks;
	}

	/**
	 * Get links to be deleted
	 * @return collection of removed links
	 */
	public Collection<ExternalLink> getRemovedLinks() {
		return removedLinks;
	}

	@Override
	public String toString() {
		return String.format("new: %s, modified: %s, removed: %s", newLinks, modifiedLinks, removedLinks);
	}
}
