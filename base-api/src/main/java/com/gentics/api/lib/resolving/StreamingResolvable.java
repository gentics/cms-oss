package com.gentics.api.lib.resolving;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Interface for {@link Resolvable} instances, that support streaming of properties
 */
public interface StreamingResolvable {

	/**
	 * Get the names of the properties that can be streamed
	 * @return collection of property names
	 */
	Collection<String> getStreamableProperties();

	/**
	 * Check whether the property with given name can be streamed.
	 * @param name property name
	 * @return true if the property can be streamed, false if not
	 */
	boolean isStreamable(String name);

	/**
	 * Get the number of streams available for the property with given name.
	 * @param name property name
	 * @return number of streams available (0 if the property cannot be streamed)
	 */
	int getNumStreams(String name);

	/**
	 * Get an input stream for reading the n<sup>th</sup> property value, if the
	 * property can be streamed and 0 &lt;= n &lt; {@link #getNumStreams(String)}
	 * @param name name of the property
	 * @param n index of the input stream (starting with 0)
	 * @return input stream
	 * @throws IOException if the property cannot be streamed
	 * @throws ArrayIndexOutOfBoundsException if n is set inappropriately
	 */
	InputStream getInputStream(String name, int n) throws IOException,
				ArrayIndexOutOfBoundsException;
}
