/*
 * @author herbert
 * @date 19.07.2006
 * @version $Id: FieldBean.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.api.beans;

import java.lang.reflect.Field;

public class FieldBean {
	private String name;

	private String type;

	private int modifiers;

	public FieldBean() {}

	public FieldBean(Field field) {
		setName(field.getName());
		setModifiers(field.getModifiers());
		setType(field.getType().getName());
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
