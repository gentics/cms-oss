/*
 * @author herbert
 * @date 19.07.2006
 * @version $Id: MethodBean.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.api.beans;

public class MethodBean {
	private int modifiers;

	private String name;

	private String[] parameters;

	private String[] exceptions;

	private String returnType;

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getParameters() {
		return parameters;
	}

	public void setParameters(String[] parameterTypes) {
		this.parameters = parameterTypes;
	}

	public String[] getExceptions() {
		return exceptions;
	}

	public void setExceptions(String[] exceptions) {
		this.exceptions = exceptions;
	}

	public void addExceptions(Class[] exceptionTypes) {
		exceptions = new String[exceptionTypes.length];
		for (int i = 0; i < exceptionTypes.length; i++) {
			exceptions[i] = exceptionTypes[i].getName();
		}
	}

	public void addParameterTypes(Class[] parameterTypes) {
		parameters = new String[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			parameters[i] = parameterTypes[i].getName();
		}
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
}
