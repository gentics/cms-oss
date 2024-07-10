/*
 * @author herbert
 * @date 05.04.2007
 * @version $Id: RenderResultWrapper.java,v 1.2 2007-04-20 08:57:38 herbert Exp $
 */
package com.gentics.contentnode.render;

import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.msg.NodeMessage;

public class RenderResultWrapper extends RenderResult {
    
	private RenderResult wrapped;

	public RenderResultWrapper(RenderResult wrapped) {
		this.wrapped = wrapped;
	}

	public void addMessage(NodeMessage message) {
		wrapped.addMessage(message);
	}

	public void addParameter(String key, String value) {
		wrapped.addParameter(key, value);
	}

	public void addParameters(String key, Collection values) {
		wrapped.addParameters(key, values);
	}

	public void addParameters(String key, String[] values) {
		wrapped.addParameters(key, values);
	}

	public void debug(Class clazz, String message, String details) {
		wrapped.debug(clazz, message, details);
	}

	public void debug(Class clazz, String message) {
		wrapped.debug(clazz, message);
	}

	public void debug(String type, String message, String details) {
		wrapped.debug(type, message, details);
	}

	public void debug(String type, String message) {
		wrapped.debug(type, message);
	}

	public void error(Class clazz, String message, String details) {
		wrapped.error(clazz, message, details);
	}

	public void error(Class clazz, String message) {
		wrapped.error(clazz, message);
	}

	public void error(String type, String message, String details) {
		wrapped.error(type, message, details);
	}

	public void error(String type, String message) {
		wrapped.error(type, message);
	}

	public void fatal(Class clazz, String message, String details) {
		wrapped.fatal(clazz, message, details);
	}

	public void fatal(Class clazz, String message) {
		wrapped.fatal(clazz, message);
	}

	public void fatal(String type, String message, String details) {
		wrapped.fatal(type, message, details);
	}

	public void fatal(String type, String message) {
		wrapped.fatal(type, message);
	}

	public Collection getMessages() {
		return wrapped.getMessages();
	}

	public Map getParameters() {
		return wrapped.getParameters();
	}

	public String getReturnCode() {
		return wrapped.getReturnCode();
	}

	public void info(Class clazz, String message, String details) {
		wrapped.info(clazz, message, details);
	}

	public void info(Class clazz, String message) throws NodeException {
		wrapped.info(clazz, message);
	}

	public void info(String type, String message, String details) {
		wrapped.info(type, message, details);
	}

	public void info(String type, String message) {
		wrapped.info(type, message);
	}

	public String[] removeParameter(String key) {
		return wrapped.removeParameter(key);
	}

	public String[] setParameter(String key, Collection values) {
		return wrapped.setParameter(key, values);
	}

	public String[] setParameter(String key, String value) {
		return wrapped.setParameter(key, value);
	}

	public String[] setParameter(String key, String[] values) {
		return wrapped.setParameter(key, values);
	}

	public void warn(Class clazz, String message, String details) {
		wrapped.warn(clazz, message, details);
	}

	public void warn(Class clazz, String message) {
		wrapped.warn(clazz, message);
	}

	public void warn(String type, String message, String details) {
		wrapped.warn(type, message, details);
	}

	public void warn(String type, String message) {
		wrapped.warn(type, message);
	}

	public void addMessage(NodeMessage message, boolean logMessage) {
		wrapped.addMessage(message, logMessage);
	}

	public void error(Class clazz, String message, Throwable e) throws NodeException {
		wrapped.error(clazz, message, e);
	}

	public void fatal(Class clazz, String message, Throwable e) throws NodeException {
		wrapped.fatal(clazz, message, e);
	}

}
