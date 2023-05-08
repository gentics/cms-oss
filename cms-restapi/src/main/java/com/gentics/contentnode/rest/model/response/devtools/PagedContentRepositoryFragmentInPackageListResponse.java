package com.gentics.contentnode.rest.model.response.devtools;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.ContentRepositoryFragmentInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of ContentRepository Fragments in packages
 */
@XmlRootElement
public class PagedContentRepositoryFragmentInPackageListResponse extends AbstractListResponse<ContentRepositoryFragmentInPackage> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 750538472118675537L;
}
