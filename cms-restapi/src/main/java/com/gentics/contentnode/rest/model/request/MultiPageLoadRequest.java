package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to load multiple pages.
 */
@XmlRootElement
public class MultiPageLoadRequest extends MultiObjectLoadRequest {

	/**
	 * Whether information about the template should be included in
	 * the response.
	 */
	private Boolean template = false;

	/**
	 * Whether information about the folder should be included in
	 * the response.
	 */
	private Boolean folder = false;

	/**
	 * Whether information about language variants should be included in
	 * the response.
	 */
	private Boolean languageVariants = false;

	/**
	 * Whether information about variants should be included in
	 * the response.
	 */
	private Boolean pageVariants = false;

	/**
	 * Whether information about the workflow should be included in
	 * the response.
	 */
	private Boolean workflow = false;

	/**
	 * Whether information about the translation status should be included in
	 * the response.
	 */
	private Boolean translationStatus = false;

	/**
	 * Whether version information should be included in the response.
	 */
	private Boolean versionInfo = false;

	/**
	 * Whether disinheriting information about should be included in
	 * the response.
	 */
	private Boolean disinherited = false;

	/**
	 * Indicates whether information about the template should be
	 * included in the response.
	 *
	 * @return <code>true</code> if information about the template
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isTemplate() {
		return template;
	}

	/**
	 * Set whether information about the template should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if information about
	 *		the template should be included in the response.
	 */
	public void setTemplate(Boolean template) {
		this.template = template;
	}

	/**
	 * Indicates whether information about the folder should be
	 * included in the response.
	 *
	 * @return <code>true</code> if information about the folder
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isFolder() {
		return folder;
	}

	/**
	 * Set whether information about the folder should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if information about
	 *		the folder should be included in the response.
	 */
	public void setFolder(Boolean folder) {
		this.folder = folder;
	}

	/**
	 * Indicates whether information about language variants should be
	 * included in the response.
	 *
	 * @return <code>true</code> if information about language variants
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isLanguageVariants() {
		return languageVariants;
	}

	/**
	 * Set whether information about the language variants should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if information about
	 *		language variants template should be included in the response.
	 */
	public void setLanguageVariants(Boolean languageVariants) {
		this.languageVariants = languageVariants;
	}

	/**
	 * Indicates whether information about variants should be
	 * included in the response.
	 *
	 * @return <code>true</code> if information about variants
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isPageVariants() {
		return pageVariants;
	}

	/**
	 * Set whether information about variants should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if information about
	 *		variants should be included in the response.
	 */
	public void setPageVariants(Boolean pageVariants) {
		this.pageVariants = pageVariants;
	}

	/**
	 * Indicates whether information about the workflow should be
	 * included in the response.
	 *
	 * @return <code>true</code> if information about the workflow
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isWorkflow() {
		return workflow;
	}

	/**
	 * Set whether information about the workflow should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if information about
	 *		the workflow should be included in the response.
	 */
	public void setWorkflow(Boolean workflow) {
		this.workflow = workflow;
	}

	/**
	 * Indicates whether information about the translation status
	 * should be included in the response.
	 *
	 * @return <code>true</code> if information about the translation
	 *		status should be included in the response,
	 *		<code>false</code> otherwise.
	 */
	public Boolean isTranslationStatus() {
		return translationStatus;
	}

	/**
	 * Set whether information about the translation status should be
	 * included in the response.
	 *
	 * @param template Set to <code>true</code> if information about
	 *		the translation status should be included in the response.
	 */
	public void setTranslationStatus(Boolean translationStatus) {
		this.translationStatus = translationStatus;
	}

	/**
	 * Indicates whether version information should be
	 * included in the response.
	 *
	 * @return <code>true</code> if version information
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isVersionInfo() {
		return versionInfo;
	}

	/**
	 * Set whether version information should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if version information
	 *		should be included in the response.
	 */
	public void setVersionInfo(Boolean versionInfo) {
		this.versionInfo = versionInfo;
	}

	/**
	 * Indicates whether disinheriting information should be
	 * included in the response.
	 *
	 * @return <code>true</code> if disinheriting information
	 *		should be included in the response, <code>false</code>
	 *		otherwise.
	 */
	public Boolean isDisinherited() {
		return disinherited;
	}

	/**
	 * Set whether disinheriting information should be included
	 * in the response.
	 *
	 * @param template Set to <code>true</code> if disinheriting information
	 *		should be included in the response.
	 */
	public void setDisinherited(Boolean disinherited) {
		this.disinherited = disinherited;
	}
}
