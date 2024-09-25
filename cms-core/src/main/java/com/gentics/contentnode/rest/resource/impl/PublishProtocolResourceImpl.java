package com.gentics.contentnode.rest.resource.impl;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.protocol.PublishLogEntry;
import com.gentics.contentnode.publish.protocol.PublishProtocolUtil;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.PublishLogDto;
import com.gentics.contentnode.rest.model.response.GenericItemList;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PublishProtocolResource;
import com.gentics.contentnode.rest.resource.parameter.FilterPublishableObjectBean;
import com.gentics.contentnode.rest.resource.parameter.FilterPublishableObjectBean.PublishTypeDto;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.log.NodeLogger;
import java.util.Arrays;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

@Authenticated
@Produces({MediaType.APPLICATION_JSON})
@Path("/publish/state")
public class PublishProtocolResourceImpl implements PublishProtocolResource {

	public final static Function<PublishLogEntry, PublishLogDto> MAP2REST = (publishLogEntry) -> {
		var nodeUser = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, publishLogEntry.getUser());
		var userDto = ModelBuilder.getUser(nodeUser);

		return new PublishLogDto(
				publishLogEntry.getObjId(),
				publishLogEntry.getType(),
				publishLogEntry.getState() == 1 ? "ONLINE" : "OFFLINE",
				userDto,
				publishLogEntry.getDate().toString()
		);
	};

	private static final NodeLogger logger = NodeLogger.getNodeLogger(
			PublishProtocolResourceImpl.class);

	@Override
	@GET
	@Path("/{type}/{objId}")
	public PublishLogDto get(@PathParam("type") String type, @PathParam("objId") Integer objId)
			throws NodeException {
		validateRequest(type);
		try (Trx trx = ContentNodeHelper.trx()) {
			var publishLogEntry = PublishProtocolUtil.getPublishLogEntryByObjectId(type, objId);

			if (!canView(publishLogEntry)) {
				throw new RestMappedException(I18NHelper.get("rest.permission.required"))
						.setResponseCode(ResponseCode.PERMISSION)
						.setStatus(Status.FORBIDDEN);
			}

			return MAP2REST.apply(publishLogEntry);
		}
	}

	@Override
	@GET
	@Path("/")
	public GenericItemList<PublishLogDto> list(
			@BeanParam PagingParameterBean paging,
			@BeanParam FilterPublishableObjectBean filter) throws NodeException {
		validateRequest(filter.type);

		try (Trx trx = ContentNodeHelper.trx()) {
			var publishLogEntries = PublishProtocolUtil.getPublishLogEntries();

			return ListBuilder.from(publishLogEntries, MAP2REST)
					.filter(this::canView)
					.filter(entry -> (filter.objId == null || filter.objId.equals(entry.getObjId())) &&
							(filter.type == null || filter.type.equalsIgnoreCase(entry.getType())))
					.page(paging)
					.to(new GenericItemList<>());
		}
	}

	/**
	 * Validates the request filter
	 * @param typeFilter
	 * @throws NodeException
	 */
	private void validateRequest(String typeFilter) throws NodeException {
		try {
			PublishTypeDto.fromString(typeFilter);
		} catch (IllegalArgumentException e) {
			throw new RestMappedException(
					"Specified type does not exist. Valid values for the type filter are: "
							+ Arrays.toString(PublishTypeDto.values()));
		}
	}


	/**
	 * Checks if the current user can view the specified publish log entry.
	 *
	 * @param publishLogEntry the publish log entry to check
	 * @return {@code true} if the user can view the publish log entry, {@code false} otherwise
	 */
	private boolean canView(PublishLogEntry publishLogEntry) {
		NodeObject nodeObject;
		try {
			Transaction transaction = TransactionManager.getCurrentTransaction();
			switch (publishLogEntry.getType().toUpperCase()) {
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
