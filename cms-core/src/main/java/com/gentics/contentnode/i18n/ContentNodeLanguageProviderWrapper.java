package com.gentics.contentnode.i18n;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.i18n.LanguageProvider;
import com.gentics.lib.i18n.LanguageProviderWrapper;
import com.gentics.lib.log.NodeLogger;

/**
 * Portal.Node specific language provider wrapper.
 * 
 * @author johannes2
 * 
 */
public class ContentNodeLanguageProviderWrapper implements LanguageProviderWrapper {

	private static NodeLogger logger = NodeLogger.getNodeLogger(ContentNodeLanguageProviderWrapper.class);

	/**
	 * Provider that always provides english
	 */
	private static StaticLanguageProvider ENGLISH_PROVIDER = new StaticLanguageProvider(CNDictionary.ENGLISH);

	@Override
	public LanguageProvider getCurrentProvider() {
		// get the current transaction, or the provider that will always return english
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		Session session = ContentNodeHelper.getSession();
		if (t != null) {
			return t;
		} else if (session != null) {
			return session;
		} else {
			return ENGLISH_PROVIDER;
		}
	}

	@Override
	public String getCurrentLanguageCode() {
		// try to get the currently rendered page (if any)
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			StackResolvable rootObject = t.getRenderType().getRenderedRootObject();

			if (rootObject instanceof Page) {
				return ((Page) rootObject).getLanguage().getCode();
			}
		} catch (Exception e) {
			logger.debug("Could not determine language code.", e);
		}
		return null;
	}

}
