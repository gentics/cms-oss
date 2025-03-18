package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.request.WastebinSearch;

/**
 * Parameter bean for search requests
 */
public class SearchParameterBean {
	/**
	 * true to add the template to pages
	 */
	@QueryParam("template")
	@DefaultValue("false")
	public boolean template;

	/**
	 * true to add the folder to search hits
	 */
	@QueryParam("folder")
	@DefaultValue("false")
	public boolean folder;

	/**
	 * true to add language variants to pages
	 */
	@QueryParam("langvars")
	@DefaultValue("false")
	public boolean languageVariants;

	/**
	 * true to add the translation status to pages
	 */
	@QueryParam("translationstatus")
	@DefaultValue("false")
	public boolean translationStatus;

	/**
	 * true to add content tags to pages
	 */
	@QueryParam("contenttags")
	@DefaultValue("false")
	public boolean contentTags;

	/**
	 * true to add object tags to search hits
	 */
	@QueryParam("objecttags")
	@DefaultValue("false")
	public boolean objectTags;

	/**
	 * true to add privilege information to folders
	 */
	@QueryParam("privileges")
	@DefaultValue("false")
	public boolean addPrivileges;

	/**
	 * true to add privilege maps to folders
	 */
	@QueryParam("privilegeMap")
	@DefaultValue("false")
	public boolean privilegeMap;

	/**
	 * optional nodeId to restrict search to a specific node
	 */
	@QueryParam("nodeId")
	@DefaultValue("0")
	public int nodeId;

	/**
	 * optional folderId to restrict search to specific folder(s)
	 */
	@QueryParam("folderId")
	public List<Integer> folderId;

	/**
	 * flag for restricting search to given folderIds and their subfolders
	 */
	@QueryParam("recursive")
	@DefaultValue("false")
	public boolean recursive;

	/**
	 * optional list of language codes to restrict search
	 */
	@QueryParam("language")
	public List<String> languages;

	/**
	 * exclude (default) to exclude deleted objects, include to include deleted
	 * objects, only to return only deleted objects
	 */
	@QueryParam("wastebin")
	@DefaultValue("exclude")
	public WastebinSearch wastebinSearch = WastebinSearch.exclude;

	/**
	 * optional name for a Content Staging package to check upon the object inclusion status
	 */
	@QueryParam("package")
	public String stagingPackageName;
}
