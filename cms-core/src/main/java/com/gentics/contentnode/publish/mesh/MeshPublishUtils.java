package com.gentics.contentnode.publish.mesh;

import java.util.function.Supplier;

import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Utilities for using the {@link MeshRestClient}
 */
public final class MeshPublishUtils {
	/**
	 * Private constructor to avoid instantiation
	 */
	private MeshPublishUtils() {
	}

	/**
	 * Handle instances of {@link MeshRestClientMessageException} containing status {@link HttpResponseStatus#NOT_FOUND}
	 * by calling the handler. Otherwise, a {@link RuntimeException} is thrown (with the throwable as cause)
	 * @param t throwable
	 * @param notFoundHandler handler for {@link HttpResponseStatus#NOT_FOUND}
	 * @return return value of the handler
	 */
	public static <T> T ifNotFound(Throwable t, Supplier<T> notFoundHandler) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			if (meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
				return notFoundHandler.get();
			}
		}
		throw new RuntimeException(t);
	}

	/**
	 * Check whether the thrown error is recoverable
	 * @param t throwable
	 * @return true for recoverable error
	 */
	public static boolean isRecoverable(Throwable t) {
		return isConflict(t);
	}

	/**
	 * Check whether the throwable is a {@link MeshRestClientMessageException} with {@link MeshRestClientMessageException#getStatusCode()} {@link HttpResponseStatus#CONFLICT}.
	 * @param t throwable
	 * @return true for conflict errors, false otherwise
	 */
	public static boolean isConflict(Throwable t) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			return meshException.getStatusCode() == HttpResponseStatus.CONFLICT.code();
		} else if (t.getCause() != null && t.getCause() != t) {
			// check the cause
			return isConflict(t.getCause());
		} else {
			return false;
		}
	}
}
