package com.gentics.contentnode.rest.model.response.log;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of logged errors
 */
@XmlRootElement
public class ErrorLogEntryList extends AbstractListResponse<ErrorLogEntry> {
	/**
	 * Serial version UId
	 */
	private static final long serialVersionUID = 2578213240913828104L;
}
