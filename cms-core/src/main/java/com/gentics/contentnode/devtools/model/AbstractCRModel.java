package com.gentics.contentnode.devtools.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.TagmapEntryModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class AbstractCRModel extends ContentRepositoryModel {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2651450921696433405L;

	private List<TagmapEntryModel> entries;

	@Override
	public Integer getId() {
		return null;
	}

	@Override
	public void setId(Integer id) {
	}

	@Override
	public Integer getCheckDate() {
		return null;
	}

	@Override
	public void setCheckDate(Integer checkDate) {
	}

	@Override
	public String getCheckResult() {
		return null;
	}

	@Override
	public void setCheckResult(String checkResult) {
	}

	@Override
	public Status getCheckStatus() {
		return null;
	}

	@Override
	public void setCheckStatus(Status checkStatus) {
	}

	@Override
	public String getDataCheckResult() {
		return null;
	}

	@Override
	public void setDataCheckResult(String dataCheckResult) {
	}

	@Override
	public Status getDataStatus() {
		return null;
	}

	@Override
	public void setDataStatus(Status dataStatus) {
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public void setPassword(String password) {
	}

	@Override
	public Integer getStatusDate() {
		return null;
	}

	@Override
	public void setStatusDate(Integer statusDate) {
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Override
	public void setUrl(String url) {
	}

	@Override
	public Boolean getUsePassword() {
		return null;
	}

	@Override
	public void setUsePassword(Boolean usePassword) {
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public void setUsername(String username) {
	}

	/**
	 * Entries
	 * @return entries
	 */
	public List<TagmapEntryModel> getEntries() {
		return entries;
	}

	/**
	 * Set the entries
	 * @param entries entries
	 */
	public void setEntries(List<TagmapEntryModel> entries) {
		this.entries = entries;
	}
}
