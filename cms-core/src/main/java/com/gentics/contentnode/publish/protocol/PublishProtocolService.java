package com.gentics.contentnode.publish.protocol;

import static com.gentics.contentnode.object.Form.TYPE_FORM;
import static com.gentics.contentnode.object.Page.TYPE_PAGE;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.lib.log.NodeLogger;
import java.util.ArrayList;
import java.util.List;

public class PublishProtocolService {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(PublishProtocolService.class);


	public PublishProtocolService() {
	}


	public <T extends PublishableNodeObject> void logPublishState(T object, int status, int user)
			throws NodeException {
		var publishLogEntry = new PublishLogEntry(object.getId(), getType(object.getTType()), status,
				user);

		logger.info(
				String.format("Storing publish state information of '%s' with id '%s' with state '%s'.",
						publishLogEntry.getType(), publishLogEntry.getObjId(), publishLogEntry.getState()));
		publishLogEntry.save();
	}

	public PublishLogEntry getPublishLogEntries(int id) throws NodeException {
		return new PublishLogEntry().load(id);
	}

	public List<PublishLogEntry> getPublishLogEntries() throws NodeException {
		try {
			return new PublishLogEntry().loadAll();
		}
		catch (Exception e) {
			logger.error("Something went wrong while retrieving the publish protocol", e);
			return new ArrayList<>();
		}
	}

	public String getType(int ttype) {
		return switch (ttype) {
			case TYPE_PAGE -> PublishType.PAGE.toString();
			case TYPE_FORM -> PublishType.FORM.toString();
			default -> PublishType.OTHER.toString();
		};
	}

}
