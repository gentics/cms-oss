package com.gentics.contentnode.rest.model.response;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * REST response that indicates whether the requested job has been pushed into the background.
 */
public class BackgroundJobResponse extends GenericResponse {

	@Serial
	private static final long serialVersionUID = -2330508315703987654L;

	private boolean inBackground;

	/**
	 * Default constructor with the {@link #inBackground} set to {@code false}.
	 */
	public BackgroundJobResponse() {
		super();

		this.inBackground = false;
	}

	/**
	 * Create instance with message and response info and {@link #inBackground} set to {@code false}.
	 * @param message The message that should be displayed to the user.
	 * @param responseInfo The response info.
	 */
	public BackgroundJobResponse(Message message, ResponseInfo responseInfo) {
		this(message, responseInfo, false);
	}

	/**
	 * Create instance from a {@link GenericResponse} and {@link #inBackground} set to {@code false}.
	 * @param response The generic response.
	 */
	public BackgroundJobResponse(GenericResponse response) {
		this(response.getMessages(), response.getResponseInfo(), false);
	}

	/**
	 * Create instance from a {@link GenericResponse} and {@code inBackground}.
	 * @param response The generic response.
	 * @param inBackground Whether the job was pushed into the background.
	 */
	public BackgroundJobResponse(GenericResponse response, boolean inBackground) {
		this(response.getMessages(), response.getResponseInfo(), inBackground);
	}

	/**
	 * Create instance with messages and response info and {@code inBackground}.
	 * @param message The message that should be displayed to the user.
	 * @param responseInfo The response info.
	 * @param inBackground Whether the job was pushed into the background.
	 */
	public BackgroundJobResponse(Message message, ResponseInfo responseInfo, boolean inBackground) {
		this(Collections.singletonList(message), responseInfo, inBackground);
	}

	/**
	 * Create instance from messages, response info and {@code inBackground}.
	 * @param messages The messages that should be displayed to the user.
	 * @param responseInfo The response info.
	 * @param inBackground Whether the job was pushed into the background.
	 */
	public BackgroundJobResponse(List<Message> messages, ResponseInfo responseInfo, boolean inBackground) {
		super(null, responseInfo);

		if (messages != null) {
			messages.stream().filter(Objects::nonNull).forEach(this::addMessage);
		}

		this.inBackground = inBackground;
	}

	/**
	 * Whether the job was pushed into the background.
	 * @return Whether the job was pushed into the background.
	 */
	public boolean isInBackground() {
		return inBackground;
	}

	/**
	 * Set whether the job was pushed into the background.
	 * @param inBackground Whether the job was pushed into the background.
	 * @return Fluent API
	 */
	public BackgroundJobResponse setInBackground(boolean inBackground) {
		this.inBackground = inBackground;

		return this;
	}
}
