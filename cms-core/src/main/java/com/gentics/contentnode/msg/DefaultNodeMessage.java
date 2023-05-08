package com.gentics.contentnode.msg;

import java.io.Serializable;

import org.apache.logging.log4j.Level;

import com.gentics.contentnode.object.Icon;

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

	private Icon icon;
	private Throwable throwable;
	private static final Level DEFAULT_LEVEL = Level.INFO;

	public DefaultNodeMessage(String type, String message, String details, Icon icon) {
		this.details = details;
		this.type = type;
		this.message = message;
		this.icon = icon;
		this.level = DEFAULT_LEVEL;
	}

	public DefaultNodeMessage(Level level, String type, String message) {
		this.level = level;
		this.details = null;
		this.type = type;
		this.message = message;
		this.icon = null;
	}

	public DefaultNodeMessage(Level level, Class<?> clazz, String message) {
		this.level = level;
		this.details = null;
		this.type = clazz.getName();
		this.message = message;
		this.icon = null;
	}

	public DefaultNodeMessage(String level, String type, String message, String details, Icon icon) {
		this.level = Level.toLevel(level);
		this.details = details;
		this.type = type;
		this.message = message;
		this.icon = icon;
	}

	public DefaultNodeMessage(Level level, String type, String message, String details, Icon icon) {
		this.level = level;
		this.details = details;
		this.type = type;
		this.message = message;
		this.icon = icon;
	}

	public DefaultNodeMessage(Level level, Class<?> clazz, String message, String details, Icon icon) {
		this.level = level;
		this.details = details;
		this.type = clazz.getName();
		this.message = message;
		this.icon = icon;
	}

	public DefaultNodeMessage(Level error, Class<?> clazz, String message, Throwable throwable) {
		this(error, clazz, message);
		this.throwable = throwable;
		if (throwable != null) {
			this.details = throwable.getLocalizedMessage();
		}
	}

	public static Icon getIcon(Level level) {
		return null;
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

	public Icon getIcon() {
		return icon;
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
