package com.gentics.contentnode.tests.rest.file;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Binary data provider resource returning text content.
 */
@Path("/binary")
public final class BinaryDataResource {
	@GET
	public byte[] data() {
		return "testcontent".getBytes();
	}
}
