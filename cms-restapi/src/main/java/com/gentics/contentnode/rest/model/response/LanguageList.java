package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentLanguage;

/**
 * Model of language list
 */
@XmlRootElement
public class LanguageList extends AbstractListResponse<ContentLanguage> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;
}
