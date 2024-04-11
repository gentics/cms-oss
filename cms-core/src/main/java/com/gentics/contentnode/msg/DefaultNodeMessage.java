package com.gentics.contentnode.msg;

import java.io.Serializable;

import org.apache.logging.log4j.Level;

/**
 * Default NodeMessage implementation
 */
public class DefaultNodeMessage implements NodeMessage, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9081951479453446947L;

	private Level level;
	private String details;
	private String type;
	private String message;

	private Throwable throwable;

	public DefaultNodeMessage(Level level, Class<?> clazz, String message) {
		this.level = level;
		this.details = null;
		this.type = clazz.getName();
		this.message = message;
	}

	public DefaultNodeMessage(Level level, String type, String message, String details) {
		this.level = level;
		this.details = details;
		this.type = type;
		this.message = message;
	}

	public DefaultNodeMessage(Level level, Class<?> clazz, String message, String details) {
		this.level = level;
		this.details = details;
		this.type = clazz.getName();
		this.message = message;
	}

	public DefaultNodeMessage(Level error, Class<?> clazz, String message, Throwable throwable) {
		this(error, clazz, message);
		this.throwable = throwable;
		if (throwable != null) {
			this.details = throwable.getLocalizedMessage();
		}
	}

	public String getDetails() {
		return details;
	}

	public String getType() {
		return type;
	}

	public Level getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public String toString() {
		String msg = this.message;

		if (details != null) {
			message += "<br>\n" + this.details;
		}
		return msg;
	}
}
