package com.gentics.contentnode.mcp.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Error payload of a failed tool call, returned as {@code structuredContent.problem} next to the
 * error's text block, shaped like the {@code Problem} of the GenAIx tool contract (RFC 9457). Fields
 * that are not set are omitted on serialization.
 * @param type problem type URI, {@link #TYPE_PREFIX} plus a slug like {@code permission-denied}
 * @param title short title of the problem type
 * @param status status of the problem type (not necessarily the HTTP status the CMS REST API uses)
 * @param detail one sentence on what failed, the CMS message where there is one (not localized:
 *        the session binding, and with it the language, is gone when a failure is mapped)
 * @param tool name of the failed tool
 * @param retryable whether retrying the same call may succeed
 * @param cms CMS response code and messages, if the CMS supplied them
 */
@JsonInclude(Include.NON_NULL)
public record Problem(String type, String title, int status, String detail, String tool, boolean retryable, Cms cms) {
	/**
	 * Prefix of every problem type URI
	 */
	public static final String TYPE_PREFIX = "https://cms.gentics.com/problems/";

	/**
	 * Suffix of the message keys of a {@link ReadOnlyException} thrown because another user holds
	 * the edit lock, e.g. {@code page.readonly.locked}
	 */
	public static final String LOCKED_MESSAGE_KEY_SUFFIX = ".readonly.locked";

	/**
	 * Build the problem for a failed tool call. The cause chain is searched for the most specific
	 * known failure: a lock held by another user, another read-only object, missing privileges, a
	 * missing object, a CMS response with a response code, a rejected tool argument, in this order.
	 * Anything else is a CMS failure.
	 * @param tool name of the failed tool
	 * @param cause failure
	 * @return problem
	 */
	public static Problem of(String tool, Throwable cause) {
		ReadOnlyException readOnly = find(cause, ReadOnlyException.class);
		if (readOnly != null) {
			if (readOnly.getMessageKey() != null && readOnly.getMessageKey().endsWith(LOCKED_MESSAGE_KEY_SUFFIX)) {
				return create(Kind.LOCKED, tool, readOnly.getMessage(), null);
			}
			return create(Kind.PERMISSION, tool, readOnly.getMessage(), new Cms(ResponseCode.PERMISSION, null));
		}

		InsufficientPrivilegesException privileges = find(cause, InsufficientPrivilegesException.class);
		if (privileges != null) {
			return create(Kind.PERMISSION, tool, privileges.getMessage(),
					new Cms(ResponseCode.PERMISSION, null));
		}

		EntityNotFoundException notFound = find(cause, EntityNotFoundException.class);
		if (notFound != null) {
			return create(Kind.NOT_FOUND, tool, notFound.getMessage(), new Cms(ResponseCode.NOTFOUND, null));
		}

		RestMappedException restMapped = find(cause, RestMappedException.class);
		if (restMapped != null) {
			GenericResponse response = restMapped.getRestResponse();
			ResponseCode code = response.getResponseInfo() != null ? response.getResponseInfo().getResponseCode()
					: null;
			List<CmsMessage> messages = response.getMessages() == null ? null
					: response.getMessages().stream().map(CmsMessage::of).toList();
			return create(Kind.of(code), tool, detail(response), new Cms(code, messages));
		}

		IllegalArgumentException invalid = find(cause, IllegalArgumentException.class);
		if (invalid != null) {
			return create(Kind.INVALID_DATA, tool, invalid.getMessage(), null);
		}

		return create(Kind.FAILURE, tool, cause != null ? cause.getMessage() : null, null);
	}

	/**
	 * Find the first throwable of the given class in the cause chain
	 * @param cause start of the chain, may be null
	 * @param clazz class to look for
	 * @return throwable or null
	 */
	private static <T extends Throwable> T find(Throwable cause, Class<T> clazz) {
		for (Throwable t = cause; t != null; t = t.getCause() == t ? null : t.getCause()) {
			if (clazz.isInstance(t)) {
				return clazz.cast(t);
			}
		}
		return null;
	}

	/**
	 * Get the detail of a CMS response: its messages, or its response message if there are none
	 * @param response response
	 * @return detail, may be null
	 */
	private static String detail(GenericResponse response) {
		String messages = response.getMessages() == null ? ""
				: response.getMessages().stream().map(Message::getMessage).filter(Objects::nonNull)
						.collect(Collectors.joining(" "));
		if (!messages.isBlank()) {
			return messages;
		}
		return response.getResponseInfo() != null ? response.getResponseInfo().getResponseMessage() : null;
	}

	/**
	 * Create the problem of the given kind
	 * @param kind kind
	 * @param tool tool name
	 * @param detail detail, the kind's title is used if null or blank
	 * @param cms CMS part, may be null
	 * @return problem
	 */
	private static Problem create(Kind kind, String tool, String detail, Cms cms) {
		return new Problem(TYPE_PREFIX + kind.slug, kind.title, kind.status,
				detail != null && !detail.isBlank() ? detail : kind.title, tool, kind.retryable, cms);
	}

	/**
	 * CMS part of a problem
	 * @param responseCode CMS response code, verbatim
	 * @param messages CMS messages
	 */
	@JsonInclude(Include.NON_NULL)
	public record Cms(ResponseCode responseCode, List<CmsMessage> messages) {
	}

	/**
	 * CMS message of a problem
	 * @param type message type
	 * @param fieldName name of the offending field, if the CMS names one
	 * @param message message text
	 */
	@JsonInclude(Include.NON_NULL)
	public record CmsMessage(Message.Type type, String fieldName, String message) {
		/**
		 * Map the REST message
		 * @param message REST message
		 * @return CMS message
		 */
		public static CmsMessage of(Message message) {
			return new CmsMessage(message.getType(), message.getFieldName(), message.getMessage());
		}
	}

	/**
	 * Problem types of the contract's error mapping
	 */
	enum Kind {
		PERMISSION("permission-denied", "Permission denied", 403, false),
		NOT_FOUND("not-found", "Not found", 404, false),
		INVALID_DATA("invalid-data", "Invalid data", 422, false),
		LOCKED("object-locked", "Object locked", 423, true),
		MAINTENANCE_MODE("maintenance-mode", "Maintenance mode", 503, true),
		NOT_LICENSED("feature-not-licensed", "Feature not licensed", 403, false),
		FAILURE("cms-failure", "CMS failure", 500, true);

		private final String slug;

		private final String title;

		private final int status;

		private final boolean retryable;

		Kind(String slug, String title, int status, boolean retryable) {
			this.slug = slug;
			this.title = title;
			this.status = status;
			this.retryable = retryable;
		}

		/**
		 * Get the kind for a CMS response code
		 * @param code response code, may be null
		 * @return kind, {@link #FAILURE} for null and for codes the error mapping does not name
		 */
		static Kind of(ResponseCode code) {
			if (code == null) {
				return FAILURE;
			}
			switch (code) {
			case PERMISSION:
				return PERMISSION;
			case NOTFOUND:
				return NOT_FOUND;
			case INVALIDDATA:
				return INVALID_DATA;
			case LOCKED:
				return LOCKED;
			case MAINTENANCEMODE:
				return MAINTENANCE_MODE;
			case NOTLICENSED:
				return NOT_LICENSED;
			default:
				return FAILURE;
			}
		}
	}
}
