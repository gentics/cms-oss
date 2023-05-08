/*
 * @author Erwin Mascher
 * @date 11.12.2003
 * @version $Id: NodeMessage.java,v 1.8 2007-04-10 14:53:56 herbert Exp $
 */
package com.gentics.contentnode.msg;

import org.apache.logging.log4j.Level;

import com.gentics.contentnode.object.Icon;

/**
 * NodeMessage is a generic message interface which is used for any message
 * which should be passed to the user.
 *
 * Each message must contain at least a short message, a level and a type.
 */
public interface NodeMessage {

	/**
	 * get the short message of this message.
	 * TODO use I18nString
	 * @return the short message,
	 */
	String getMessage();

	/**
	 * get an icon for this message.
	 * @return an icon associated with this message, or null if not set.
	 */
	Icon getIcon();

	/**
	 * get detailed version of this message.
	 * TODO use I18nString
	 * @return a detailed information text of the message, or null if not set.
	 */
	String getDetails();

	/**
	 * get a 'classification' of the message, like the general type of error, or the
	 * module which created the message. This can be either a string, or a FQ class name.
	 * TODO use I18nString
	 * @return the type or causing module of the message.
	 */
	String getType();

	/**
	 * get the severity level of the message.
	 * @return the level of this message.
	 */
	Level getLevel();

	Throwable getThrowable();
}
