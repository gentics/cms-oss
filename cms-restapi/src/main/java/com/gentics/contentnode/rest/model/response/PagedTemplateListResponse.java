package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Template;

/**
 * Paged list of templates
 */
@XmlRootElement
public class PagedTemplateListResponse extends AbstractListResponse<Template> {
}
