package com.gentics.contentnode.tests.rest.file;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Alternative binary data provider resource returning text content.
 */
@Path("/binaryalt")
public final class BinaryAltDataResource {

	public static final String CONTENT = "alternativetestcontent_äÖßÜ";

	@GET
	public byte[] data() {
		return CONTENT.getBytes();
	}
}
