package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class representing a diff request sent to DiffResource. Encapsulates request data.
 */
@XmlRootElement
public class DiffRequest {

	/**
	 * Default template for changes
	 */
	public final static String DEFAULT_CHANGE_TEMPLATE = "<del class='diff modified gtx-diff'>$remove</del><ins class='diff modified gtx-diff'>$insert</ins>";

	/**
	 * Default template for inserts
	 */
	public final static String DEFAULT_INSERT_TEMPLATE = "<ins class='diff modified gtx-diff'>$insert</ins>";

	/**
	 * Default template for removals
	 */
	public final static String DEFAULT_REMOVE_TEMPALTE = "<del class='diff modified gtx-diff'>$remove</del>";

	/**
	 * Default number of words before the change to be shown
	 */
	public final static int DEFAULT_WORDS_BEFORE = 10;

	/**
	 * Default number of words after the change to be shown
	 */
	public final static int DEFAULT_WORDS_AFTER = 10;

	/**
	 * First content to be diff'ed
	 */
	protected String content1;

	/**
	 * Second content to be diff'ed
	 */
	protected String content2;

	/**
	 * Regex of content to be ignored while diffing
	 */
	protected String ignoreRegex;

	/**
	 * Template for rendering changes
	 */
	protected String changeTemplate;

	/**
	 * Template for rendering inserts
	 */
	protected String insertTemplate;

	/**
	 * Template for rendering removals
	 */
	protected String removeTemplate;

	/**
	 * Number of words to be shown before the diff
	 */
	protected Integer wordsBefore;

	/**
	 * Number of words to be shown after the diff
	 */
	protected Integer wordsAfter;

	/**
	 * Create an empty instance (Used by JAXB)
	 */
	public DiffRequest() {}

	/**
	 * Return the content1.
	 * 
	 * @return the content1
	 */
	public String getContent1() {
		return content1;
	}

	/**
	 * Set the content1.
	 * 
	 * @param content1 the content1 to set
	 */
	public void setContent1(String content1) {
		this.content1 = content1;
	}

	/**
	 * Return the content2.
	 * 
	 * @return the content2
	 */
	public String getContent2() {
		return content2;
	}

	/**
	 * Set the content2.
	 * 
	 * @param content2 the content2 to set
	 */
	public void setContent2(String content2) {
		this.content2 = content2;
	}

	/**
	 * Return the ignore regex that will be used to sanitize the content before the diff is invoked.
	 * 
	 * @return the ignoreRegex
	 */
	public String getIgnoreRegex() {
		return ignoreRegex;
	}

	/**
	 * Set the ignore regex that will be used to sanitize the content before the diff is invoked.
	 * 
	 * @param ignoreRegex the ignoreRegex to set
	 */
	public void setIgnoreRegex(String ignoreRegex) {
		this.ignoreRegex = ignoreRegex;
	}

	/**
	 * Return the change template.
	 * 
	 * <p>
	 * <pre>
	 * {@code
	 * The following default template will be used when no template has been set:
	 * <del class='diff modified gtx-diff'>$remove</del><ins class='diff modified gtx-diff'>$insert</ins>
	 * }
	 * </pre>
	 * 
	 * @return the changeTemplate
	 */
	public String getChangeTemplate() {
		return changeTemplate;
	}

	/**
	 * Set the change template.
	 * 
	 * <p>
	 * The following default template will be used when no template has been set:
	 * <pre>
	 * {@code
	 * <del class='diff modified gtx-diff'>$remove</del><ins class='diff modified gtx-diff'>$insert</ins>
	 * }
	 * </pre>
	 * 
	 * @param changeTemplate the template to set
	 */
	public void setChangeTemplate(String changeTemplate) {
		this.changeTemplate = changeTemplate;
	}

	/**
	 * Return the insert template.
	 * 
	 * <p>
	 * <pre>
	 * {@code
	 * The following default template will be used when no template has been set:
	 * <ins class='diff modified gtx-diff'>$insert</ins>
	 * }
	 * </pre>
	 * 
	 * @return the insertTemplate
	 */
	public String getInsertTemplate() {
		return insertTemplate;
	}

	/**
	 * Set the insert template for the diff request.
	 * 
	 * <p>
	 * The following default template will be used when no template has been set:
	 * <pre>
	 * {@code
	 * <ins class='diff modified gtx-diff'>$insert</ins>
	 * }
	 * </pre>
	 * 
	 * @param insertTemplate the template to set
	 */
	public void setInsertTemplate(String insertTemplate) {
		this.insertTemplate = insertTemplate;
	}

	/**
	 * Return the remove template.
	 * 
	 * <p>
	 * The following default template will be used when no template has been set:
	 * <pre>
	 * {@code
	 * <del class='diff modified gtx-diff'>$remove</del>
	 * }
	 * </pre>
	 * 
	 * @return the removeTemplate
	 */
	public String getRemoveTemplate() {
		return removeTemplate;
	}

	/**
	 * Set the remove template.
	 * 
	 * <p>
	 * The following default template will be used when no template has been set:
	 * <pre>
	 * {@code
	 * <del class='diff modified gtx-diff'>$remove</del>
	 * }
	 * </pre>
	 * 
	 * @param removeTemplate the removeTemplate to set
	 */
	public void setRemoveTemplate(String removeTemplate) {
		this.removeTemplate = removeTemplate;
	}

	/**
	 * Return the number of words before the change to be shown.
	 * Default value: {@value #DEFAULT_WORDS_BEFORE}
	 * 
	 * @return the wordsBefore
	 */
	public Integer getWordsBefore() {
		return wordsBefore;
	}

	/**
	 * Set the amount of words that should be displayed before the actual diff.
	 * Default value: {@value #DEFAULT_WORDS_BEFORE}
	 *  
	 * @param wordsBefore the wordsBefore to set
	 */
	public void setWordsBefore(Integer wordsBefore) {
		this.wordsBefore = wordsBefore;
	}

	/**
	 * Return the amount of words that should be displayed after the actual diff.
	 * Default value: {@value #DEFAULT_WORDS_AFTER}
	 * 
	 * @return the wordsAfter
	 */
	public Integer getWordsAfter() {
		return wordsAfter;
	}

	/**
	 * Set the amount of words that should be displayed after the actual diff.
	 * Default value: {@value #DEFAULT_WORDS_AFTER}
	 * 
	 * @param wordsAfter the wordsAfter to set
	 */
	public void setWordsAfter(Integer wordsAfter) {
		this.wordsAfter = wordsAfter;
	}
}
