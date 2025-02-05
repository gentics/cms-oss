package com.gentics.contentnode.tests.rest.file.fum;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.rest.model.fum.FUMRequest;
import com.gentics.contentnode.rest.model.fum.FUMResult;
import com.gentics.contentnode.rest.model.fum.FUMResponseStatus;

/**
 * Resource for a fake FUM
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path("fum")
public class FUMResource {
	public final static String DENY_MSG = "This is the deny message";

	public final static String ACCEPT_MSG = "This is the accept message";

	public final static String FILENAME = "fum_was_here.txt";

	public static Consumer<FUMRequest> requestConsumer;

	@POST
	@Path("accept")
	public FUMResult accept(FUMRequest request) throws NodeException {
		if (requestConsumer != null) {
			requestConsumer.accept(request);
		}
		return new FUMResult().setStatus(FUMResponseStatus.ACCEPTED);
	}

	@POST
	@Path("accept/msg")
	public FUMResult acceptMsg(FUMRequest request) throws NodeException {
		if (requestConsumer != null) {
			requestConsumer.accept(request);
		}
		return new FUMResult().setStatus(FUMResponseStatus.ACCEPTED).setMsg(ACCEPT_MSG);
	}

	@POST
	@Path("deny")
	public FUMResult deny(FUMRequest request) throws NodeException {
		if (requestConsumer != null) {
			requestConsumer.accept(request);
		}
		return new FUMResult().setStatus(FUMResponseStatus.DENIED).setMsg(DENY_MSG);
	}

	@POST
	@Path("postpone")
	public FUMResult postpone(FUMRequest request) throws NodeException {
		if (requestConsumer != null) {
			requestConsumer.accept(request);
		}
		return new FUMResult().setStatus(FUMResponseStatus.POSTPONED);
	}

	@POST
	@Path("change/filename")
	public FUMResult changeFilename(FUMRequest request) throws NodeException {
		if (requestConsumer != null) {
			requestConsumer.accept(request);
		}
		return new FUMResult().setStatus(FUMResponseStatus.ACCEPTED).setFilename(FILENAME);
	}
}
