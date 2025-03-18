package com.gentics.contentnode.devtools.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class EnhancedCRFModel extends ContentRepositoryFragmentModel {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8803904031271216494L;

	private List<ContentRepositoryFragmentEntryModel> entries;

	/**
	 * Entries
	 * @return entries
	 */
	public List<ContentRepositoryFragmentEntryModel> getEntries() {
		return entries;
	}

	/**
	 * Set the entries
	 * @param entries entries
	 */
	public void setEntries(List<ContentRepositoryFragmentEntryModel> entries) {
		this.entries = entries;
	}
}
