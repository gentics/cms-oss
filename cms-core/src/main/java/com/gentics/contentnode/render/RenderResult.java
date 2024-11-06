/*
 * @author Stefan Hepp
 * @date 21.01.2006
 * @version $Id: RenderResult.java,v 1.9 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.contentnode.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.lib.log.NodeLogger;

/**
 * RenderResult is a container for logmessages and other feedback generated during rendering.
 * The logmessages are also passed to the internal logger.
 *
 * TODO use 'fatal', 'error'.. returncodes to allow to check how fatal the errors where.
 * TODO implement some logger interface?
 */
public class RenderResult {

	/**
	 * Returncode for status 'ok'. This is used if no errors or fatals occured.
	 */
	public final static int RETURNCODE_OK = 1;

	/**
	 * Returncode for status 'failed'. This is used if any errors or fatals occured.
	 **/
	private final static int RETURNCODE_FAILED = 2;

	private Map<String, String[]> params;

	// list of messages collected while rendering
	protected List<NodeMessage> messages;

	protected int returnCode;

	/**
	 * Main public constructor, to create a new, empty renderresult.
	 * The default-status is 'OK'.
	 */
	public RenderResult() {
		params = new LinkedHashMap<String, String[]>();
		messages = new ArrayList<NodeMessage>();
		returnCode = RETURNCODE_OK;
	}

	/**
	 * get the current returncode as String. This returns 'OK' if no errors or fatals
	 * occured, else 'FAIL'.
	 *
	 * @return the current returncode.
	 */
	public String getReturnCode() {
		String code;

		switch (returnCode) {
		case RETURNCODE_OK:
			code = "OK";
			break;

		case RETURNCODE_FAILED:
			code = "FAIL";
			break;

		default:
			code = "";
			break;
		}
		return code;
	}

	/**
	 * set a new parameter to the result and overwrite its previous value.
	 *
	 * @param key the name of the parameter.
	 * @param values a list of strings of values for the parameter.
	 * @return the previously stored parameter values, or null if it was not set.
	 */
	public String[] setParameter(String key, String[] values) {
		return params.put(key, values);
	}

	/**
	 * Set a parameter with a single value, overwriting its previous value. The value is used
	 * as its first and only parameter value, as every parameter is stored as a list of values.
	 *
	 * @param key name of the parameter.
	 * @param value the new value of the parameter.
	 * @return the previously stored parameter values, or null if it was not set.
	 */
	public String[] setParameter(String key, String value) {
		return params.put(key, new String[] { value});
	}

	/**
	 * Set a paramater with values, overwriting its previous values. The objects
	 * in the collection are converted to strings by {@link Object#toString()}.
	 * The values are stored in the order they are returned by the collections
	 * iterator.
	 *
	 * @param key name of the parameter.
	 * @param values a collection of values.
	 * @return the previously stored parameter values, or null if it was not set.
	 */
	public String[] setParameter(String key, Collection<?> values) {
		String[] lValues = new String[values.size()];
		int i = 0;

		for (Iterator<?> it = values.iterator(); it.hasNext();) {
			Object o = it.next();

			lValues[i] = o != null ? o.toString() : null;
			i++;
		}
		return params.put(key, lValues);
	}

	/**
	 * Add a parameter value to an existing parameter. If the parameter does not yet exist,
	 * a new one is created.
	 *
	 * @param key name of the parameter.
	 * @param value the value to add.
	 */
	public void addParameter(String key, String value) {
		String[] values = getParameterResized(key, 1);

		values[values.length - 1] = value;
		params.put(key, values);
	}

	/**
	 * Add a list of values to a parameter. If the parameter does not exist, a new one
	 * is created.
	 * @param key name of the parameter.
	 * @param values a list of values to be added.
	 */
	public void addParameters(String key, String[] values) {
		if (values == null) {
			return;
		}
		String[] newValues = getParameterResized(key, values.length);
		int oldSize = newValues.length - values.length;

		for (int i = 0; i < values.length; i++) {
			newValues[oldSize + i] = values[i];
		}

		params.put(key, newValues);
	}

	/**
	 * Add a list of values to a parameter. If the parameter does not exist, a new one is created.
	 * The objects in the collection are converted to strings using {@link Object#toString()}.
	 * The objects are added to the values in the order they are returned by the collections
	 * iterator.
	 *
	 * @param key name of the parameter.
	 * @param values a collection of values.
	 */
	public void addParameters(String key, Collection<?> values) {
		if (values == null) {
			return;
		}
		String[] newValues = getParameterResized(key, values.size());
		int pos = newValues.length - values.size();

		for (Iterator<?> it = values.iterator(); it.hasNext();) {
			final Object o = it.next();

			newValues[pos] = o != null ? o.toString() : null;
			pos++;
		}

		params.put(key, newValues);
	}

	/**
	 * Remove a parameter with all its values.
	 * @param key name of the parameter to be removed.
	 * @return the old value, or null, if it was not set.
	 */
	public String[] removeParameter(String key) {
		return params.remove(key);
	}

	/**
	 * Add a message. The message is handled and logged depending on its level.
	 * @param message message to be added.
	 */
	public void addMessage(NodeMessage message, boolean logMessage) {
		messages.add(message);
		if (logMessage) {
			logMessage(message);
		}
	}
    
	/**
	 * Adds a message and logs it.
	 * @param message
	 */
	public void addMessage(NodeMessage message) {
		addMessage(message, true);
	}

	/**
	 * Log a message with info level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 */
	public void info(String type, String message) {
		log(Level.INFO, type, message, null);
	}

	/**
	 * Log a message with info level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 * @param details some more details for the message.
	 */
	public void info(String type, String message, String details) {
		log(Level.INFO, type, message, details);
	}

	/**
	 * Log a message with info level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged for.
	 * @param message a short message to log.
	 */
	public void info(Class<?> clazz, String message) throws NodeException {
		log(Level.INFO, clazz, message, (String) null);
	}

	/**
	 * Log a message with info level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged for.
	 * @param message a short message to log.
	 * @param details some more details for the message.
	 */
	public void info(Class<?> clazz, String message, String details) {
		log(Level.INFO, clazz, message, details);
	}

	/**
	 * Log a message with debug level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 */
	public void debug(String type, String message) {
		log(Level.DEBUG, type, message, null);
	}

	/**
	 * Log a message with debug level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 * @param details some more details to the message.
	 */
	public void debug(String type, String message, String details) {
		log(Level.DEBUG, type, message, details);
	}

	/**
	 * Log a message with debug level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged.
	 * @param message a short message to log.
	 */
	public void debug(Class<?> clazz, String message) {
		log(Level.DEBUG, clazz, message, (String) null);
	}

	/**
	 * Log a message with debug level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged.
	 * @param message a short message to log.
	 * @param details some more details of the message.
	 */
	public void debug(Class<?> clazz, String message, String details) {
		log(Level.DEBUG, clazz, message, details);
	}

	/**
	 * Log a message with warn level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 */
	public void warn(String type, String message) {
		log(Level.WARN, type, message, null);
	}

	/**
	 * Log a message with warn level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 * @param details some more details for the message.
	 */
	public void warn(String type, String message, String details) {
		log(Level.WARN, type, message, details);
	}

	/**
	 * Log a message with warn level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged.
	 * @param message a short message to log.
	 */
	public void warn(Class<?> clazz, String message) {
		log(Level.WARN, clazz, message, (String) null);
	}

	/**
	 * Log a message with warn level.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged.
	 * @param message a short message to log.
	 * @param details some details for the messsage.
	 */
	public void warn(Class<?> clazz, String message, String details) {
		log(Level.WARN, clazz, message, details);
	}

	/**
	 * Log a message with fatal level. This sets the returncode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 */
	public void fatal(String type, String message) {
		log(Level.FATAL, type, message, null);
	}

	/**
	 * Log a message with fatal level. This sets the returncode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 * @param details some more details for the message.
	 */
	public void fatal(String type, String message, String details) {
		log(Level.FATAL, type, message, details);
	}

	/**
	 * Log a message with fatal level. This sets the returncode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged.
	 * @param message a short message to log.
	 */
	public void fatal(Class<?> clazz, String message) {
		log(Level.FATAL, clazz, message, (String) null);
	}

	/**
	 * Log a message with fatal level. This sets the resultcode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz a class for which this message should be logged.
	 * @param message a short message to log.
	 * @param details some more details to the message.
	 */
	public void fatal(Class<?> clazz, String message, String details) {
		log(Level.FATAL, clazz, message, details);
	}

	/**
	 * Log a message with fatal level. This sets the resultcode to 'failed'.
	 * @param clazz
	 * @param message
	 * @param e
	 * @throws Throwable
	 */
	public void fatal(Class<?> clazz, String message, Throwable e) throws NodeException {
		log(Level.FATAL, clazz, message, e);
	}

	/**
	 * Log a message with error level. This sets the resultcode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 */
	public void error(String type, String message) {
		log(Level.ERROR, type, message, null);
	}

	/**
	 * Log a message with error level. This sets the resultcode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param type a classification of the message.
	 * @param message a short message to log.
	 * @param details some more details for the message to log.
	 */
	public void error(String type, String message, String details) {
		log(Level.ERROR, type, message, details);
	}

	/**
	 * Log an error message
	 * 
	 * @param clazz logging class
	 * @param message message
	 * @param e throwable
	 * @throws NodeException
	 */
	public void error(Class<?> clazz, String message, Throwable e) throws NodeException {
		log(Level.ERROR, clazz, message, e);
	}

	/**
	 * Log a message with error level. This sets the resultcode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz the class for which this message should be logged.
	 * @param message a short message.
	 */
	public void error(Class<?> clazz, String message) {
		log(Level.ERROR, clazz, message, (String) null);
	}

	/**
	 * Log a message with error level. This sets the resultcode to 'failed'.
	 *
	 * TODO i18n
	 * @see NodeMessage
	 * @param clazz the class for which this message should be logged.
	 * @param message a short message to log.
	 * @param details some more details for the message.
	 */
	public void error(Class<?> clazz, String message, String details) {
		log(Level.ERROR, clazz, message, details);
	}

	/**
	 * get all parametes set as return-params.
	 * @return a map of String (key) to String[] (values).
	 */
	public Map<String, String[]> getParameters() {
		return params;
	}

	/**
	 * get list of all generated messages.
	 *
	 * @return a collection of NodeMessages
	 * @see com.gentics.contentnode.msg.NodeMessage
	 */
	public Collection<NodeMessage> getMessages() {
		return messages;
	}

	private String[] getParameterResized(String key, int addEntries) {
		String[] oldValue = params.get(key);
		int newSize = oldValue != null ? oldValue.length + addEntries : addEntries;

		String[] newValue = new String[newSize];

		if (oldValue != null) {
			for (int i = 0; i < oldValue.length; i++) {
				newValue[i] = oldValue[i];
			}
		}

		return newValue;
	}

	private void logMessage(NodeMessage message) {
		NodeLogger.getLogger(message.getType()).log(message.getLevel(), message.toString(), message.getThrowable());
	}

	/**
	 * Log the given message
	 * @param level log level
	 * @param type message type
	 * @param message message
	 * @param details message details
	 */
	private void log(Level level, String type, String message, String details) {
		if (level == Level.ERROR || level == Level.FATAL) {
			returnCode = RETURNCODE_FAILED;
		}
		addMessage(new DefaultNodeMessage(level, type, enhanceMessage(level, message), details), true);
	}

	/**
	 * Log the given message
	 * @param level log level
	 * @param clazz logging class
	 * @param message message
	 * @param details message details
	 */
	private void log(Level level, Class<?> clazz, String message, String details) {
		if (level == Level.ERROR || level == Level.FATAL) {
			returnCode = RETURNCODE_FAILED;
		}
		addMessage(new DefaultNodeMessage(level, clazz, enhanceMessage(level, message), details), true);
	}

	/**
	 * Log the given message
	 * @param level log level
	 * @param clazz logging class
	 * @param message message
	 * @param e throwable
	 */
	private void log(Level level, Class<?> clazz, String message, Throwable e) {
		if (level == Level.ERROR || level == Level.FATAL) {
			returnCode = RETURNCODE_FAILED;
		}
		addMessage(new DefaultNodeMessage(level, clazz, enhanceMessage(level, message), e), true);
	}

	/**
	 * Enhance the message with info about the currently rendered object
	 * @param level log level
	 * @param message message to enhance
	 * @return enhanced message
	 */
	private String enhanceMessage(Level level, String message) {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			RenderType renderType = t.getRenderType();
			if (renderType != null) {
				return String.format("%s while rendering %s: %s", level, renderType.getReadableStack(), message);
			}
		}
		return message;
	}
}
