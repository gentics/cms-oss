package com.gentics.contentnode.rest.resource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link StreamingOutput} to be used for binary downloads.
 *
 * <p>
 *     <em>Note:</em> Accessing the {@link #inputStream()} is only intended for testing when
 *     the {@link javax.ws.rs.core.Response} object from the binary download endpoint is
 *     used directly instead of being sent via HTTP to a client.
 * </p>
 * @param inputStream The input stream to read data from for the response.
 */
public record BinaryOutput(InputStream inputStream) implements StreamingOutput {

    @Override
    public void write(OutputStream outputStream) throws WebApplicationException {
        try {
            byte[] buf = new byte[1024];
            int len;

            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
}
