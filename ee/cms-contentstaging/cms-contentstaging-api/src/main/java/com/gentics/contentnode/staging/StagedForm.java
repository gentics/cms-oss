package com.gentics.contentnode.staging;

import java.util.List;
import java.util.Optional;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * REST entity for the form, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedForm extends StagingBase {

	public static final String TYPE_NAME = "form";

	private static final long serialVersionUID = 9012361259459603486L;

	/**
	 * Form type
	 */
	private String formType;

	/**
	 * Description of the form
	 */
	private String description;

	/**
	 * Languages
	 */
	private List<String> languages;

	/**
	 * ID of the success page
	 */
	private String successPageId;

	/**
	 * ID of the success form
	 */
	private String successNodeId;

	/**
	 * Form data
	 */
	private JsonNode data;

	/**
	 * Version data
	 */
	private StagedObjectVersion version = new StagedObjectVersion();

	/**
	 * Form type
	 * @return form type
	 */
	public String getFormType() {
		return formType;
	}

	/**
	 * Set the form type
	 * @param formType form type
	 */
	public void setFormType(String formType) {
		this.formType = formType;
	}

	/**
	 * Form description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the form description
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Form languages
	 * @return form languages
	 */
	public List<String> getLanguages() {
		return languages;
	}

	/**
	 * Set the form languages
	 * @param languages form languages
	 */
	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	/**
	 * ID of the success page
	 * @return success page ID
	 */
	public String getSuccessPageId() {
		return successPageId;
	}

	/**
	 * Set the success page ID
	 * @param successPageId success page ID
	 */
	public void setSuccessPageId(String successPageId) {
		this.successPageId = successPageId;
	}

	/**
	 * ID of the success node
	 * @return success node ID
	 */
	public String getSuccessNodeId() {
		return successNodeId;
	}

	/**
	 * Set the success node ID
	 * @param successNodeId success node ID
	 */
	public void setSuccessNodeId(String successNodeId) {
		this.successNodeId = successNodeId;
	}

	/**
	 * Form data in JSON format
	 * @return form data
	 */
	public JsonNode getData() {
		return data;
	}

	/**
	 * Set the form data
	 * @param data form data
	 */
	public void setData(JsonNode data) {
		this.data = data;
	}

	/**
	 * Clone the given origin into this entity.
	 * 
	 * @param origin
	 */
	@XmlTransient
	@JsonIgnore
    public void clone(StagedForm origin) {
		super.clone(origin);
		this.description = origin.description;
		this.languages = origin.languages;
		this.data = origin.data;
		if (origin.version != null) {
			this.version.clone(origin.version);
		}
	}

	@Override
	@XmlTransient
	@JsonIgnore
    public String getTypeName() {
		return TYPE_NAME;
	}

	@XmlTransient
	@JsonIgnore
    @Override
    public Optional<StagedObjectVersion> maybeVersioned() {
		return Optional.of(this.version);
	}

	/**
	 * Get object version.
	 * 
	 * @return
	 */
	public StagedObjectVersion getVersion() {
		return version;
	}

	/**
	 * Set object version.
	 * 
	 * @param version
	 */
	public void setVersion(StagedObjectVersion version) {
		this.version = version;
	}
}
