package com.gentics.contentnode.tests.rest.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("bigbinary")
public class BigFileResource {

	@GET
	public byte[] data() {
		return get();
	}

	public static final byte[] get() {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		try {
			IOUtils.copy(BigFileResource.class.getResourceAsStream("www_gentics_com.pdf"), o);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return o.toByteArray();
	}
}
