package com.gentics.contentnode.testutils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.resolving.ResolvableMapWrappable;
import com.gentics.contentnode.resolving.ResolvableMapWrapper;
import com.github.jknack.handlebars.helper.HelperFunction;

/**
 * Helper Source for some loader helpers
 */
public class LoaderHelperSource {
	/**
	 * Load a folder with given id
	 * @param id folder id
	 * @return folder
	 * @throws NodeException
	 */
	@HelperFunction("gtx_test_folder")
	public static Object getFolder(int id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		return wrap(t.getObject(Folder.class, id));
	}

	/**
	 * Load a page with given id
	 * @param id page id
	 * @return page
	 * @throws NodeException
	 */
	@HelperFunction("gtx_test_page")
	public static Object getPage(int id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		return wrap(t.getObject(Page.class, id));
	}

	/**
	 * Wrap the returned object
	 * @param object object to be wrapped
	 * @return wrapped object
	 */
	protected static Object wrap(NodeObject object) {
		if (object == null) {
			return null;
		} else if (object instanceof ResolvableMapWrappable wrappable) {
			return new ResolvableMapWrapper(wrappable);
		} else {
			return object;
		}
	}
}
