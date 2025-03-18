package com.gentics.contentnode.tests.rest.file;

import com.gentics.testutils.GenericTestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.io.IOException;

/**
 * Binary data provider resource returning a JPG image.
 */
@Path("/binary")
public final class BinaryDataImageResource {

	public enum ImageType {
		JPG("blume.jpg", true),
		ANIMATED_GIF("rotation.gif", true),
		SVG("test.svg", false);

		private final String filename;
		private final boolean canConvert;

		ImageType(String filename, boolean canConvert) {
			this.filename = filename;
			this.canConvert = canConvert;
		}

		public String filename() {
			return filename;
		}

		public boolean canConvert() {
			return canConvert;
		}
	}

	@GET
	public byte[] data() throws IOException {
		return IOUtils.toByteArray(GenericTestUtils.getPictureResource(ImageType.JPG.filename()));
	}

	@GET
	@Path("/{type}")
	public byte[] data(@PathParam("type") String type) throws IOException {
		var imageType = EnumUtils.getEnumIgnoreCase(ImageType.class, type);

		if (imageType == null) {
			return data();
		}

		return IOUtils.toByteArray(GenericTestUtils.getPictureResource(imageType.filename()));
	}
}
