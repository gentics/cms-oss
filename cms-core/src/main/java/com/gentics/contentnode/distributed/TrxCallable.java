package com.gentics.contentnode.distributed;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;

/**
 * Abstract base class for Callable implementations running with in a transaction
 *
 * @param <T> return type
 */
public abstract class TrxCallable <T extends Serializable> implements Callable<T>, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5237518706154928620L;

	/**
	 * Optionally set session
	 */
	protected Session session;

	/**
	 * Language ID
	 */
	protected int languageId;

	/**
	 * Set the current language ID
	 * @return fluent API
	 * @throws NodeException
	 */
	public TrxCallable<T> setLanguageId() throws NodeException {
		this.languageId = ContentNodeHelper.getLanguageId();
		return this;
	}

	/**
	 * Set session
	 * @param session session
	 * @return fluent API
	 */
	public TrxCallable<T> setSession(Session session) {
		this.session = session;
		return this;
	}

	@Override
	public final T call() throws Exception {
		try (Trx trx = ContentNodeHelper.trx(session)) {
			if (languageId != 0) {
				ContentNodeHelper.setLanguageId(languageId);
			}
			return callWithTrx();
		}
	}

	/**
	 * Method handler that will be called with a running transaction
	 * @return return value
	 * @throws NodeException
	 */
	protected abstract T callWithTrx() throws NodeException;
}
