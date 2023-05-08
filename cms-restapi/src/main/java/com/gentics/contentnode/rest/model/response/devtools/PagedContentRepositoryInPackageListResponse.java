package com.gentics.contentnode.rest.model.response.devtools;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.ContentRepositoryInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of ContentRepositories in packages
 */
@XmlRootElement
public class PagedContentRepositoryInPackageListResponse extends AbstractListResponse<ContentRepositoryInPackage> {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = -4940180200988271776L;
}
