package com.gentics.contentnode.publish.mesh;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
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
		return isConflict(t) || isNotFound(t);
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

	/**
	 * Get the optional message response from the throwable
	 * @param t throwable
	 * @return optional message response
	 */
	public static Optional<GenericMessageResponse> getResponse(Throwable t) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			return Optional.of(meshException.getResponseMessage());
		} else if (t.getCause() != null && t.getCause() != t) {
			return getResponse(t.getCause());
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Get the optional conflicting node as pair of uuid and language, if the throwable is a conflict
	 * @param t throwable
	 * @return optional conflicting node
	 */
	public static Optional<Pair<String, String>> getConflictingNode(Throwable t) {
		if (isConflict(t)) {
			return getResponse(t).flatMap(resp -> {
				Object conflictingUuid = resp.getProperty("conflictingUuid");
				Object conflictingLanguage = resp.getProperty("conflictingLanguage");
				if (conflictingUuid == null) {
					return Optional.empty();
				} else {
					return Optional.of(Pair.of(conflictingUuid.toString(), conflictingLanguage != null ? conflictingLanguage.toString() : null));
				}
			});
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Check whether the throwable is a {@link MeshRestClientMessageException} with {@link MeshRestClientMessageException#getStatusCode()} {@link HttpResponseStatus#NOT_FOUND}.
	 * @param t throwable
	 * @return true for not_found errors, false otherwise
	 */
	public static boolean isNotFound(Throwable t) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			return meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code();
		} else if (t.getCause() != null && t.getCause() != t) {
			// check the cause
			return isNotFound(t.getCause());
		} else {
			return false;
		}
	}
}
