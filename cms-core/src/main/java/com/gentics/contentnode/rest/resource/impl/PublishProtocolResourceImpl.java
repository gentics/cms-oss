package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.factory.Trx.supply;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.publish.protocol.PublishLogEntry;
import com.gentics.contentnode.publish.protocol.PublishProtocolService;
import com.gentics.contentnode.rest.model.PublishLogDto;
import com.gentics.contentnode.rest.model.response.GenericItemList;
import com.gentics.contentnode.rest.resource.PublishProtocolResource;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces({MediaType.APPLICATION_JSON})
@Path("/publish/state")
public class PublishProtocolResourceImpl implements PublishProtocolResource {

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
			return MAP2REST.apply(this.publishProtocolService.getPublishLogEntryByObjectId(objId));
		}
	}

	@Override
	@GET
	@Path("/")
	public GenericItemList<PublishLogDto> list(
			@BeanParam PagingParameterBean paging) throws NodeException {
		var publishLogEntries = supply(publishProtocolService::getPublishLogEntries);

		return ListBuilder.from(publishLogEntries, MAP2REST)
				.page(paging)
				.to(new GenericItemList<>());
	}

}
