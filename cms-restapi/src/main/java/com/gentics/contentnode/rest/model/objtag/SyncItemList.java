package com.gentics.contentnode.rest.model.objtag;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing a list of tags out of sync
 */
@XmlRootElement
public class SyncItemList extends AbstractListResponse<SyncItem> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7693544631341044585L;

	public SyncItemList() {
		super();
	}

	public SyncItemList(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
