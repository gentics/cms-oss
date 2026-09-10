package com.gentics.api;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.render.RenderUtils;

/**
 * Loader helper, which should be used in Groovy Scripts to load forms
 */
public class FormLoader {
	/**
	 * Private constructor
	 */
	private FormLoader() {
	}

	/**
	 * Load form
	 * @param id form id
	 * @return wrapped form
	 * @throws NodeException
	 */
	public static Resolvable form(int id) throws NodeException {
		return RenderUtils.wrap(TransactionManager.getCurrentTransaction().getObject(Form.class, id));
	}

	/**
	 * Load form
	 * @param id form id
	 * @return wrapped form
	 * @throws NodeException
	 */
	public static Resolvable form(String id) throws NodeException {
		return RenderUtils.wrap(TransactionManager.getCurrentTransaction().getObject(Form.class, id));
	}
}
