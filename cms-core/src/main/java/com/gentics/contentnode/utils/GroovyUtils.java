package com.gentics.contentnode.utils;

import org.codehaus.groovy.control.CompilationUnit;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.parttype.CMSResolver;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.ResolvableMapWrapper;

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

	/**
	 * Get the current compilation unit from the {@link RenderType}
	 * @return current compilation unit
	 * @throws NodeException
	 */
	public static CompilationUnit getCurrentCompilationUnit() throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		CMSResolver cmsResolver = renderType.getCMSResolver();
		Node node = ObjectTransformer.get(Node.class, cmsResolver.get("node")).getMaster();

		return renderType.getCompilationUnit(node);
	}

	/**
	 * Inject the current {@link CMSResolver} (wrapped into a {@link ResolvableMapWrapper}) into the script (as "cms")
	 * @param script script to get the CMSResolver injected
	 * @throws NodeException
	 */
	public static void injectCmsResolver(Script script) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		CMSResolver cmsResolver = renderType.getCMSResolver();
		script.setProperty("cms", new ResolvableMapWrapper(cmsResolver));
	}
}
