package com.gentics.contentnode.rest.model.response.log;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of action log entries
 */
@XmlRootElement
public class ActionLogEntryList extends AbstractListResponse<ActionLogEntry> {
	/**
	 * Serial Version UId
	 */
	private static final long serialVersionUID = 6690012877836880544L;
}
