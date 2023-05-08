package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * Parameter bean for page model parameters
 */
public class PageModelParameterBean {
	/**
	 * True for adding template info to the pages. Default is false.
	 */
	@QueryParam("template")
	@DefaultValue("false")
	public boolean template;

	/**
	 * True for adding folder info to the pages. Default is false.
	 */
	@QueryParam("folder")
	@DefaultValue("false")
	public boolean folder;

	/**
	 * True for adding the language variants to the pages. Default is false.
	 */
	@QueryParam("langvars")
	@DefaultValue("false")
	public boolean languageVariants;

	/**
	 * True if the contenttags shall be attached to all returned pages. Default
	 * is false
	 */
	@QueryParam("contenttags")
	@DefaultValue("false")
	public boolean contentTags;

	/**
	 * True if the objecttags shall be attached to all returned pages. Default
	 * is false.
	 */
	@QueryParam("objecttags")
	@DefaultValue("false")
	public boolean objectTags;

	/**
	 * True if the translationstatus information shall be added for every page.
	 * Default is false.
	 */
	@QueryParam("translationstatus")
	@DefaultValue("false")
	public boolean translationStatus;
}
