package com.gentics.contentnode.tests.rest.file;

import com.gentics.testutils.GenericTestUtils;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;

/**
 * Binary data provider resource returning a JPG image.
 */
@Path("/binary")
public final class BinaryDataImageResource {

	public static String FILENAME = "blume.jpg";

	@GET
	public byte[] data() throws IOException {
		return IOUtils.toByteArray(GenericTestUtils.getPictureResource(FILENAME));
	}
}
