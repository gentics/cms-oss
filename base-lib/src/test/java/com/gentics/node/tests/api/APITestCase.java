/*
 * @author herbert
 * @date 19.07.2006
 * @version $Id: APITestCase.java,v 1.2 2010-09-28 17:08:13 norbert Exp $
 */
package com.gentics.node.tests.api;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.gentics.contentnode.tests.category.BaseLibTest;
import junit.framework.TestCase;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.lib.datasource.simple.SimpleAttribute;
import com.gentics.lib.datasource.simple.SimpleObject;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.ws.datasource.SimpleWSAttribute;
import com.gentics.lib.ws.datasource.SimpleWSObject;
import com.gentics.node.tests.api.beans.ClassBean;
import com.gentics.testutils.junit.StoredAssertion;
import org.junit.experimental.categories.Category;

/**
 * A simple test case which iterates through the API to check if the method
 * signatures don't change. Switch the compareResults flag and run the test, to
 * generate new reference results, and override the old one. Don't forget to
 * update your directory to see the changes.
 * @author herbert
 */
@Category(BaseLibTest.class)
public class APITestCase extends TestCase {

	/**
	 * This attributes defines if the code should compare the api to the stored
	 * results, or create a reference result.
	 */
	private boolean compareResults = true;

	private static NodeLogger logger = NodeLogger.getNodeLogger(APITestCase.class);

	public void setUp() {
		StoredAssertion.clearStoredAsserts();
		StoredAssertion.setTestName(this.getName());
	}

	public void tearDown() {
		StoredAssertion.doStoredAssertReport();
	}

	public void testPublicAPI() throws Exception {

		String jarLocation = System.getProperty("com.gentics.tests.api.jarfile");

		if (jarLocation == null) {
			// File basePath = TestEnvironment.getPortalNodeMetaRoot();
			// TODO Reimplement the loading of api and fix this test
			throw new NotImplementedException("Api jar test not yet usable in maven! Please fix me");
			// File basePath = new File(".");
			// File apiJarFile = new File(basePath, "dev.dist/build/tmp/gentics-portalnode-api.jar");
			// jarLocation = apiJarFile.getAbsolutePath();
		}
		JarFile jarFile = new JarFile(jarLocation);
		Enumeration entries = jarFile.entries();

		List allClasses = null;

		if (compareResults) {
			allClasses = loadReference();
		} else {
			allClasses = new ArrayList();
		}

		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			String name = entry.getName();

			if (name.endsWith(".class")) {
				name = name.replace('/', '.').replaceFirst("\\.class$", "");
				Class clazz = this.getClass().getClassLoader().loadClass(name);
				ClassBean classBean = null;

				if (compareResults) {
					classBean = findAndRemoveClass(allClasses, clazz.getName());
					StoredAssertion.storedAssertNotNull("Asserting class is in reference {" + clazz.getName() + "}", classBean);
				} else {
					classBean = new ClassBean();
					allClasses.add(classBean);
					classBean.setName(clazz.getName());
				}
				if (classBean != null) {
					Constructor[] constructors = clazz.getConstructors();

					if (compareResults) {
						classBean.verifyConstructors(constructors);
					} else {
						classBean.addConstructors(constructors);
					}

					Method[] methods = clazz.getMethods();

					if (compareResults) {
						classBean.verifyMethods(methods);
					} else {
						classBean.addMethods(methods);
					}
					Field[] fields = clazz.getFields();

					fields = verifyFieldUniqueness(fields);
					if (compareResults) {
						classBean.verifyFields(fields);
					} else {
						classBean.addFields(fields);
					}
				}

				// check the consistency of the class
				assertAPIClassConsistency(clazz, getKnownInconsistency());
			}
		}

		if (compareResults) {
			StoredAssertion.storedAssertEquals("There were API classes within reference that could not be found within api jar.", 0, allClasses.size());
			Iterator it = allClasses.iterator();

			while (it.hasNext()) {
				ClassBean bean = (ClassBean) it.next();

				logger.info("Found class within reference that was not within api: " + bean.getName());
			}
		} else {
			saveReference(allClasses);
		}
	}

	private Field[] verifyFieldUniqueness(Field[] fields) {
		List fieldList = new ArrayList(fields.length);

		for (int i = 0; i < fields.length; i++) {
			if (!fieldList.contains(fields[i])) {
				fieldList.add(fields[i]);
			} else {
				logger.info("Ignoring duplicate field {" + fields[i].getName() + "}");
			}
		}
		return (Field[]) fieldList.toArray(new Field[fieldList.size()]);
	}

	private ClassBean findAndRemoveClass(List allClasses, String name) {
		Iterator i = allClasses.iterator();

		while (i.hasNext()) {
			ClassBean classBean = (ClassBean) i.next();

			if (classBean.getName().equals(name)) {
				i.remove();
				return classBean;
			}
		}
		return null;
	}

	private void saveReference(List allClasses) throws IOException {
		FileOutputStream outputStream = new FileOutputStream("junit/src/com/gentics/tests/api/api_reference_results.txt"); // url.getFile());
		XMLEncoder e = new XMLEncoder(outputStream);

		e.writeObject(allClasses);
		e.close();
		outputStream.flush();
		outputStream.close();
	}

	private List loadReference() {
		InputStream inputStream = this.getClass().getResourceAsStream("api_reference_results.txt");
		XMLDecoder d = new XMLDecoder(inputStream);
		List object = (List) d.readObject();

		d.close();
		return object;
	}

	/**
	 * Check whether the class from the api is consistent:<br>
	 * All extended classes or implemented interfaces must be API safe<br>
	 * All parameters and result values of public methods must be API safe<br>
	 * All public fields must be API safe
	 * @param clazz class to test
	 * @param knownInconsistency TODO
	 */
	public void assertAPIClassConsistency(Class clazz, List knownInconsistency) {
		String baseMessage = "Checking API consistency of " + clazz;

		// first check the super class
		StoredAssertion.storedAssertClassIsInAPI(compareResults, baseMessage + ", superclass", clazz.getSuperclass(), null);

		// check interfaces
		Class[] interfaces = clazz.getInterfaces();

		for (int i = 0; i < interfaces.length; ++i) {
			StoredAssertion.storedAssertClassIsInAPI(compareResults, baseMessage + ", implemented interface", interfaces[i], null);
		}

		// check public methods
		Method[] methods = clazz.getDeclaredMethods();

		for (int m = 0; m < methods.length; ++m) {
			if (Modifier.isPublic(methods[m].getModifiers())) {
				// check whether the method is known to be inconsistent and shall not be checked
				if (knownInconsistency.contains(methods[m].toString())) {
					continue;
				}

				String methodMessage = baseMessage + ", method " + methods[m];

				// check the return value
				StoredAssertion.storedAssertClassIsInAPI(compareResults, methodMessage + ", return type", methods[m].getReturnType(), methods[m]);

				// check all parameter types
				Class[] parameterTypes = methods[m].getParameterTypes();

				for (int p = 0; p < parameterTypes.length; p++) {
					StoredAssertion.storedAssertClassIsInAPI(compareResults, methodMessage + ", parameter type", parameterTypes[p], methods[m]);
				}

				// check thrown exceptions
				Class[] exceptionTypes = methods[m].getExceptionTypes();

				for (int e = 0; e < exceptionTypes.length; e++) {
					StoredAssertion.storedAssertClassIsInAPI(compareResults, methodMessage + ", exception", exceptionTypes[e], methods[m]);
				}
			}
		}

		// check public fields
		Field[] fields = clazz.getFields();

		for (int f = 0; f < fields.length; f++) {
			// check whether the field is known to be inconsistent and shall not be checked
			if (knownInconsistency.contains(fields[f].toString())) {
				continue;
			}
			StoredAssertion.storedAssertClassIsInAPI(compareResults, baseMessage + ", field {" + fields[f].getName() + "}", fields[f].getType(), fields[f]);
		}
	}

	/**
	 * Get the known inconsistency as List of Strings. Each strings contains the
	 * .toString() of a Method or Field that is known to be API inconsistent and
	 * shall not produce an error.
	 * @return List of Strings
	 * @throws IOException
	 */
	private List getKnownInconsistency() throws IOException {
		List knownInconsistency = new Vector();
		BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("known_api_inconsistency.txt")));
		String readLine = null;

		while ((readLine = reader.readLine()) != null) {
			if (readLine.length() > 0 && !readLine.startsWith("#")) {
				knownInconsistency.add(readLine);
			}
		}
		reader.close();

		return knownInconsistency;
	}

	/**
	 * Check whether the interfaces SimpleObject and SimpleAttribute correlate
	 * with the webservice implementations SimpleWSObject and SimpleWSAttribute.
	 * @throws Exception
	 */
	public void testSimpleDatasourceAPI() throws Exception {
		compareInterfaceWithClass(SimpleObject.class, SimpleWSObject.class);
		compareInterfaceWithClass(SimpleAttribute.class, SimpleWSAttribute.class);
	}

	/**
	 * Check whether the given classClass implements all public methods of the
	 * interfaceClass
	 * @param interfaceClass interface class
	 * @param classClass class class
	 * @throws Exception
	 */
	private void compareInterfaceWithClass(Class interfaceClass, Class classClass) throws Exception {
		// get all interface methods
		Method[] interfaceMethods = interfaceClass.getDeclaredMethods();

		for (int i = 0; i < interfaceMethods.length; i++) {
			if (!Modifier.isPublic(interfaceMethods[i].getModifiers())) {
				// omit non-public methods (although an interface is unlikely to
				// contain non-public methods)
				continue;
			}
			// get the class counterpart
			Method classMethod = classClass.getMethod(interfaceMethods[i].getName(), interfaceMethods[i].getParameterTypes());

			// check that the method is also public
			StoredAssertion.storedAssertTrue("Method " + classMethod.getName() + " must be public", Modifier.isPublic(classMethod.getModifiers()));
		}
	}
}

