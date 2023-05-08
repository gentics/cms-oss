/*
 * @author Erwin Mascher (e.mascher@gentics.com)
 * @date 21.07.2004
 * @version $Id: ActionEvent.java,v 1.1 2006-01-13 15:25:40 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.portalnode.event;

import java.util.Map;

/**
 * Interface for an action event that may be triggered and handled inside the
 * portal.
 */
public interface ActionEvent {

	/**
	 * Get the action command of the event
	 * @return action command string
	 */
	String getActionCommand();

	String getObjectPath();

	void setObjectPath(String path);

	/**
	 * Get the action parameter with given name
	 * @param name name of the action parameter
	 * @return the value of the parameter or null if the parameter is not set
	 */
	Object getParameter(String name);

	/**
	 * Get the action parameter with given name or the default value if the
	 * parameter is not set
	 * @param name name of the action parameter
	 * @param defaultValue default value to be returned when the parameter is
	 *        not set
	 * @return the value of the parameter or the default value when the
	 *         parameter is not set
	 */
	Object getParameter(String name, Object defaultValue);

	/**
	 * Get a map of all event parameters
	 * @return Map where the keys are the parameter names and values are the
	 *         parameter values
	 */
	Map getParameterMap();

	/**
	 * Get the embedded event
	 * @return the embedded event or null
	 */
	ActionEvent getEmbeddedEvent();

	/**
	 * Set the embedded event
	 * @param evt an event this event shall wrap
	 */
	void setEmbeddedEvent(ActionEvent evt);

	/**
	 * Set the parameter with given name
	 * @param name name of the parameter
	 * @param value new value of the parameter
	 */
	void setParameter(String name, Object value);
}
