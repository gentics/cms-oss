package com.gentics.contentnode.rest.resource.parameter;

import java.util.Set;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;

/**
 * Parameter bean for link checker filter parameters
 */
public class LinkCheckerFilterParameterBean {
	/**
	 * Optional filtering for validity status. Defaults to no filtering (i.e. returning all pages with external links)
	 */
	@QueryParam("status")
	public Set<ExternalLinkStatus> status;

	/**
	 * Optional node ID for getting pages of a single node/channel only
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * Flag to restrict returned pages to those, that are editable by the user
	 */
	@QueryParam("editable")
	@DefaultValue("false")
	public boolean editable = false;

	/**
	 * (optional) true when only the pages created by the user shall be returned
	 */
	@QueryParam("iscreator")
	@DefaultValue("false")
	public boolean creator = false;

	/**
	 * (optional) true when only the pages last edited by the user shall be returned
	 */
	@QueryParam("iseditor")
	@DefaultValue("false")
	public boolean editor = false;

	/**
	 * Optional filtering for page languages. Defaults to no filtering (i.e. returning pages of all languages).
	 * Filtering can be done by language code or ID.
	 */
	@QueryParam("language")
	public Set<String> language;

	/**
	 * (optional) true to restrict to online pages, false to restrict to offline pages
	 */
	@QueryParam("online")
	public Boolean online;
}
