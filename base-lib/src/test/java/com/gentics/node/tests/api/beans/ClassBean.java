/*
 * @author herbert
 * @date 19.07.2006
 * @version $Id: ClassBean.java,v 1.2 2010-09-28 17:08:13 norbert Exp $
 */
package com.gentics.node.tests.api.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.gentics.testutils.junit.StoredAssertion;

/**
 * a simple bean which holds information of an API class. used for
 * serialization.
 * @author herbert
 */
public class ClassBean {
	private String name;
    
	private ConstructorBean[] publicConstructors;

	private MethodBean[] publicMethods;

	public FieldBean[] publicFields;

	public FieldBean[] getPublicFields() {
		return publicFields;
	}

	public void setPublicFields(FieldBean[] publicFields) {
		this.publicFields = publicFields;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ConstructorBean[] getPublicConstructors() {
		return publicConstructors;
	}

	public void setPublicConstructors(ConstructorBean[] publicConstructors) {
		this.publicConstructors = publicConstructors;
	}

	public MethodBean[] getPublicMethods() {
		return publicMethods;
	}

	public void setPublicMethods(MethodBean[] publicMethods) {
		this.publicMethods = publicMethods;
	}

	public void addConstructors(Constructor[] constructors) {
		List constructorBeans = new ArrayList();

		for (int i = 0; i < constructors.length; i++) {
			ConstructorBean constructorBean = new ConstructorBean();

			constructorBeans.add(constructorBean);

			constructorBean.setName(constructors[i].getName());
			constructorBean.setModifiers(constructors[i].getModifiers());
			constructorBean.addExceptions(constructors[i].getExceptionTypes());
			constructorBean.addParameterTypes(constructors[i].getParameterTypes());
		}

		publicConstructors = (ConstructorBean[]) constructorBeans.toArray(new ConstructorBean[constructorBeans.size()]);
	}
    
	public void verifyConstructors(Constructor[] constructors) {
		StoredAssertion.storedAssertEquals("Making sure we've got the same number of Constructors for {" + name + "}", publicConstructors.length,
				constructors.length);
		for (int i = 0; i < constructors.length; i++) {
			ConstructorBean constructorBean = findConstructorBean(constructors[i]);

			StoredAssertion.storedAssertNotNull("Making sure constructor exists {" + constructors[i].toString() + "}", constructorBean);
			StoredAssertion.storedAssertTrue("Validating that exception types are equal. {" + constructors[i].toString() + "}",
					compareTypesWithNames(constructors[i].getExceptionTypes(), constructorBean.getExceptions()));
		}
	}

	private ConstructorBean findConstructorBean(Constructor constructor) {
		for (int i = 0; i < publicConstructors.length; i++) {
			Class[] parameters = constructor.getParameterTypes();

			if (compareTypesWithNames(parameters, publicConstructors[i].getParameters())) {
				return publicConstructors[i];
			}
		}
		return null;
	}

	/**
	 * Compares the names of a given array of classes with the given name of
	 * strings.
	 * @param parameters Classes to compare with name.
	 * @param stringParameters String parameters.
	 * @return true if classes compare.
	 */
	private boolean compareTypesWithNames(Class[] parameters, String[] stringParameters) {
		if (parameters.length != stringParameters.length) {
			return false;
		}
		for (int i = 0; i < parameters.length; i++) {
			if (!parameters[i].getName().equals(stringParameters[i])) {
				return false;
			}
		}
		return true;
	}
    
	public void verifyMethods(Method[] methods) {
		StoredAssertion.storedAssertEquals("Verifying that the same number of methods exists for class {" + name + "}.", publicMethods.length, methods.length);
		for (int i = 0; i < publicMethods.length; i++) {
			MethodBean methodBean = findMethodBean(methods[i]);

			StoredAssertion.storedAssertNotNull("Verifying that we found method {" + methods[i].toString() + "}", methodBean);
			if (methodBean != null) {
				StoredAssertion.storedAssertTrue("Validating that exception types are equal for method {" + methods[i].toString() + "}",
						compareTypesWithNames(methods[i].getExceptionTypes(), methodBean.getExceptions()));
				StoredAssertion.storedAssertEquals("Validating that the returnType is equal for method {" + methods[i].toString() + "}",
						methodBean.getReturnType(), methods[i].getReturnType() != null ? methods[i].getReturnType().getName() : "void");
			}
		}
	}

	private MethodBean findMethodBean(Method method) {
		for (int i = 0; i < publicMethods.length; i++) {
			if (!publicMethods[i].getName().equals(method.getName())) {
				continue;
			}
			if (compareTypesWithNames(method.getParameterTypes(), publicMethods[i].getParameters())) {
				return publicMethods[i];
			}
		}
		return null;
	}

	public void addMethods(Method[] methods) {
		List methodBeans = new ArrayList();

		for (int i = 0; i < methods.length; i++) {
			MethodBean methodBean = new MethodBean();

			methodBeans.add(methodBean);

			methodBean.setName(methods[i].getName());
			methodBean.setModifiers(methods[i].getModifiers());
			methodBean.addExceptions(methods[i].getExceptionTypes());
			methodBean.addParameterTypes(methods[i].getParameterTypes());
			methodBean.setReturnType(methods[i].getReturnType() != null ? methods[i].getReturnType().getName() : "void");
		}

		publicMethods = (MethodBean[]) methodBeans.toArray(new MethodBean[methodBeans.size()]);
	}

	public void verifyFields(Field[] fields) {
		StoredAssertion.storedAssertEquals("Verifying same number of fields for class {" + name + "}", publicFields.length, fields.length);
		for (int i = 0; i < fields.length; i++) {
			FieldBean bean = findFieldBean(fields[i]);

			StoredAssertion.storedAssertNotNull("Verifying that we found field {" + fields[i].toString() + "}", bean);
			StoredAssertion.storedAssertEquals("Verifying that field has same type {" + fields[i].toString() + "}", bean.getType(),
					fields[i].getType().getName());
			StoredAssertion.storedAssertEquals("Verifying that field has same modifiers {" + fields[i].toString() + "}", bean.getModifiers(),
					fields[i].getModifiers());
		}
	}

	private FieldBean findFieldBean(Field field) {
		for (int i = 0; i < publicFields.length; i++) {
			if (publicFields[i].getName().equals(field.getName())) {
				return publicFields[i];
			}
		}
		return null;
	}

	public void addFields(Field[] fields) {
		publicFields = new FieldBean[fields.length];
		for (int i = 0; i < fields.length; i++) {
			publicFields[i] = new FieldBean(fields[i]);
		}
	}
}
