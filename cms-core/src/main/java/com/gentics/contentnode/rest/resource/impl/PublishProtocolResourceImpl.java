package com.gentics.contentnode.rest.resource.impl;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.protocol.PublishLogEntry;
import com.gentics.contentnode.publish.protocol.PublishProtocolService;
import com.gentics.contentnode.rest.model.PublishLogDto;
import com.gentics.contentnode.rest.model.response.GenericItemList;
import com.gentics.contentnode.rest.resource.PublishProtocolResource;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.lib.log.NodeLogger;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Produces({MediaType.APPLICATION_JSON})
@Path("/publish/state")
public class PublishProtocolResourceImpl implements PublishProtocolResource {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(
			PublishProtocolResourceImpl.class);

	private final PublishProtocolService publishProtocolService = new PublishProtocolService();

	private final Function<PublishLogEntry, PublishLogDto> MAP2REST = (publishLogEntry) -> new PublishLogDto(
			publishLogEntry.getObjId(),
			publishLogEntry.getType(),
			publishLogEntry.getState() == 1 ? "ONLINE" : "OFFLINE",
			publishLogEntry.getUser(),
			publishLogEntry.getDate().toString()
	);

	@Override
	@GET
	@Path("/{objId}")
	public PublishLogDto get(@PathParam("objId") Integer objId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			var publishLogEntry = this.publishProtocolService.getPublishLogEntryByObjectId(objId);

			if (!canView(publishLogEntry, trx.getTransaction())) {
				throw new NodeException(I18NHelper.get("rest.permission.required"));
			}

			return MAP2REST.apply(publishLogEntry);
		}
	}

	@Override
	@GET
	@Path("/")
	public GenericItemList<PublishLogDto> list(
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			var publishLogEntries = publishProtocolService.getPublishLogEntries();

			publishLogEntries = publishLogEntries.stream()
					.filter(entry -> canView(entry, trx.getTransaction())).toList();

			return ListBuilder.from(publishLogEntries, MAP2REST)
					.page(paging)
					.to(new GenericItemList<>());
		}
	}


	private boolean canView(PublishLogEntry publishLogEntry, Transaction transaction) {
		NodeObject nodeObject;
		try {
			switch (publishLogEntry.getType()) {
				case "PAGE" -> nodeObject = transaction.getObject(Page.class, publishLogEntry.getObjId());
				case "FORM" -> nodeObject = transaction.getObject(Form.class, publishLogEntry.getObjId());
				default -> {
					logger.error("Unsupported publish log entry type: " + publishLogEntry.getType());
					return false;
				}
			}
			return PermHandler.ObjectPermission.view.checkObject(nodeObject);
		} catch (NodeException e) {
			logger.error("Something went while checking the node object permission.", e);
			return false;
		}
	}

}
