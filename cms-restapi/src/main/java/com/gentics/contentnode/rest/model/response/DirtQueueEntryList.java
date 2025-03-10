package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.DirtQueueEntry;

/**
 * REST Model containing a list of dirt queue entries
 */
@XmlRootElement
public class DirtQueueEntryList extends AbstractListResponse<DirtQueueEntry> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7308718086731134009L;
}
