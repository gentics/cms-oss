package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.DirtQueueSummaryEntry;

/**
 * REST Model containing a list of dirt queue summary entries
 */
@XmlRootElement
public class DirtQueueSummaryResponse extends AbstractListResponse<DirtQueueSummaryEntry> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 552858038212317819L;
}
