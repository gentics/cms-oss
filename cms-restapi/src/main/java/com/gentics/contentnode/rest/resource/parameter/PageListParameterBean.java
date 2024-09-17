package com.gentics.contentnode.rest.resource.parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.request.Permission;

/**
 * Parameter bean for getting pages of folder
 */
public class PageListParameterBean {

	/**
	 * node id of the channel when used in multichannelling
	 */
	@QueryParam("nodeId")
	public Integer nodeId;

	/**
	 * true when the template information should be added to the pages
	 */
	@QueryParam("template")
	@DefaultValue("false")
	public boolean template = false;

	/**
	 * true when the folder information should be added to the pages
	 */
	@QueryParam("folder")
	@DefaultValue("false")
	public boolean folder = false;

	/**
	 * true when the language variants should be added to the pages
	 */
	@QueryParam("langvars")
	@DefaultValue("false")
	public boolean languageVariants = false;

	/**
	 * code of the language in which the pages shall be fetched.
	 */
	@QueryParam("language")
	public String language;

	/**
	 * true if the language fallback shall be done when getting pages in a
	 * language, false if not. If a page is not present in the given language
	 * and langFallback is true, the language variant with highest priority in
	 * the node is used instead, otherwise the page will not be present in the
	 * list
	 */
	@QueryParam("langfallback")
	@DefaultValue("true")
	public boolean langFallback = true;

	/**
	 * true if the contenttags shall be attached to all returned pages. Default
	 * is false
	 */
	@QueryParam("contenttags")
	@DefaultValue("false")
	public boolean contentTags = false;

	/**
	 * true if the objecttags shall be attached to all returned pages. Default
	 * is false
	 */
	@QueryParam("objecttags")
	@DefaultValue("false")
	public boolean objectTags = false;

	/**
	 * (optional) true, if also the content shall be searched, false if not
	 */
	@QueryParam("searchcontent")
	@DefaultValue("false")
	public boolean searchContent = false;

	/**
	 * (optional) search string for filenames (may be empty)
	 */
	@QueryParam("filename")
	public String filename;

	/**
	 * (optional) difference in seconds for searching pages, that will change
	 * their status due to timemanagement within the given timespan. When set to
	 * 0 (default), the timemanagement will not be considered.
	 */
	@QueryParam("timedue")
	@DefaultValue("0")
	public int timeDue = 0;

	/**
	 * (optional) true to restrict to pages owned by the user in a workflow.
	 * Defaults to false.
	 */
	@QueryParam("wfown")
	@DefaultValue("false")
	public boolean workflowOwn = false;

	/**
	 * (optional) true to restrict to pages watched by the user in a workflow.
	 * Defaults to false.
	 */
	@QueryParam("wfwatch")
	@DefaultValue("false")
	public boolean workflowWatch = false;

	/**
	 * (optional) {@link Boolean#TRUE} to restrict to pages that are currently
	 * in sync with their translation masters, {@link Boolean#FALSE} to restrict
	 * to pages that are currently not in sync with their translation masters,
	 * and NULL to not consider the translation status information at all.
	 * Setting this flag (to either true or false) will also add the translation
	 * status information.
	 */
	@QueryParam("insync")
	public Boolean inSync;

	/**
	 * true if the translationstatus information shall be added for every page,
	 * false if not.
	 */
	@QueryParam("translationstatus")
	@DefaultValue("false")
	public boolean translationStatus = false;

	/**
	 * (optional) true to restrict to planned pages, false to restrict to unplanned pages
	 */
	@QueryParam("planned")
	public Boolean planned;

	/**
	 * (optional) true to restrict to queued pages, false to restrict to unqueued pages
	 */
	@QueryParam("queued")
	public Boolean queued;

	/**
	 * List of folder permissions which must be granted for folders in order to include their pages in the result
	 */
	@QueryParam("permission")
	public List<Permission> permission = Collections.emptyList();

	/**
	 * priority of the page
	 */
	@QueryParam("priority")
	@DefaultValue("0")
	public int priority = 0;

	/**
	 * list of template ids
	 */
	@QueryParam("template_id")
	public List<Integer> templateIds = Collections.emptyList();

	/**
	 * true to only return inherited pages in the given node, false to only get local/localized pages, null to get local and inherited pages
	 */
	@QueryParam("inherited")
	public Boolean inherited;

	/**
	 * optional regular expression to get pages with a nice URL.
	 */
	@QueryParam("niceurl")
	public String niceUrl;

	/**
	 * List of markup language IDs for restricting to pages that have templates with one of the given markup languages
	 */
	@QueryParam("includeMlId")
	public List<Integer> includeMlIds = Collections.emptyList();

	/**
	 * List of markup language IDs for restricting to pages that have templates with none of the given markup languages
	 */
	@QueryParam("excludeMlId")
	public List<Integer> excludeMlIds = Collections.emptyList();


	public PageListParameterBean setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public PageListParameterBean setTemplate(boolean template) {
		this.template = template;
		return this;
	}

	public PageListParameterBean setFolder(boolean folder) {
		this.folder = folder;
		return this;
	}

	public PageListParameterBean setLanguageVariants(boolean languageVariants) {
		this.languageVariants = languageVariants;
		return this;
	}

	public PageListParameterBean setLanguage(String language) {
		this.language = language;
		return this;
	}

	public PageListParameterBean setLangFallback(boolean langFallback) {
		this.langFallback = langFallback;
		return this;
	}

	public PageListParameterBean setContentTags(boolean contentTags) {
		this.contentTags = contentTags;
		return this;
	}

	public PageListParameterBean setObjectTags(boolean objectTags) {
		this.objectTags = objectTags;
		return this;
	}

	public PageListParameterBean setSearchContent(boolean searchContent) {
		this.searchContent = searchContent;
		return this;
	}

	public PageListParameterBean setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	public PageListParameterBean setTimeDue(int timeDue) {
		this.timeDue = timeDue;
		return this;
	}

	public PageListParameterBean setWorkflowOwn(boolean workflowOwn) {
		this.workflowOwn = workflowOwn;
		return this;
	}

	public PageListParameterBean setWorkflowWatch(boolean workflowWatch) {
		this.workflowWatch = workflowWatch;
		return this;
	}

	public PageListParameterBean setInSync(Boolean inSync) {
		this.inSync = inSync;
		return this;
	}

	public PageListParameterBean setTranslationStatus(boolean translationStatus) {
		this.translationStatus = translationStatus;
		return this;
	}

	public PageListParameterBean setPlanned(Boolean planned) {
		this.planned = planned;
		return this;
	}

	public PageListParameterBean setQueued(Boolean queued) {
		this.queued = queued;
		return this;
	}

	public PageListParameterBean setPermission(List<Permission> permission) {
		this.permission = permission;
		return this;
	}

	public PageListParameterBean setPriority(int priority) {
		this.priority = priority;
		return this;
	}

	public PageListParameterBean setTemplateIds(List<Integer> templateIds) {
		this.templateIds = templateIds;
		return this;
	}

	public PageListParameterBean setInherited(Boolean inherited) {
		this.inherited = inherited;
		return this;
	}

	public PageListParameterBean setNiceUrl(String niceUrl) {
		this.niceUrl = niceUrl;
		return this;
	}

	public PageListParameterBean setIncludeMlIds(List<Integer> includeMlIds) {
		this.includeMlIds = includeMlIds;
		return this;
	}

	public PageListParameterBean setExcludeMlIds(List<Integer> excludeMlIds) {
		this.excludeMlIds = excludeMlIds;
		return this;
	}

	/**
	 * Create a clone of this parameter bean.
	 * @return A clone of this parameter bean
	 */
	public PageListParameterBean clone() {
		return new PageListParameterBean()
			.setNodeId(nodeId)
			.setTemplate(template)
			.setFolder(folder)
			.setLanguageVariants(languageVariants)
			.setLanguage(language)
			.setLangFallback(langFallback)
			.setContentTags(contentTags)
			.setObjectTags(objectTags)
			.setSearchContent(searchContent)
			.setFilename(filename)
			.setTimeDue(timeDue)
			.setWorkflowOwn(workflowOwn)
			.setWorkflowWatch(workflowWatch)
			.setInSync(inSync)
			.setTranslationStatus(translationStatus)
			.setPlanned(planned)
			.setQueued(queued)
			.setPermission(new ArrayList<>(permission))
			.setPriority(priority)
			.setTemplateIds(new ArrayList<>(templateIds))
			.setInherited(inherited)
			.setNiceUrl(niceUrl)
			.setIncludeMlIds(includeMlIds)
			.setExcludeMlIds(excludeMlIds);
	}
}
