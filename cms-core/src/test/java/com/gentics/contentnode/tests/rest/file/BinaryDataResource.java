package com.gentics.contentnode.tests.rest.file;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Binary data provider resource
 */
@Path("/binary")
public final class BinaryDataResource {
	@GET
	public byte[] data() {
		return "testcontent".getBytes();
	}
}
