package com.gentics.contentnode.utils;

import java.util.function.Consumer;

import com.gentics.api.lib.exception.NodeException;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

/**
 * Utilities for calling groovy scripts
 */
public final class GroovyUtils {
	/**
	 * Private constructor
	 */
	private GroovyUtils() {
	}

	/**
	 * Load the class from the class loader and if it is a {@link Script}, create an instance, prepare it with the given handler and run it 
	 * @param gcl groovy class loader
	 * @param className class name
	 * @param prepareScript consumer to prepare the script (add properties)
	 * @return script return value
	 * @throws NodeException
	 */
	public static Object call(GroovyClassLoader gcl, String className, Consumer<Script> prepareScript) throws NodeException {
		try {
			Class<?> scriptClass = gcl.loadClass(className);
			if (Script.class.isAssignableFrom(scriptClass)) {
				Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();

				if (prepareScript != null) {
					prepareScript.accept(script);
				}

				return script.run();
			} else {
				throw new NodeException("%s is not a Script".formatted(className));
			}
		} catch (ClassNotFoundException e) {
			throw new NodeException("Script %s not found".formatted(className));
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}
}
