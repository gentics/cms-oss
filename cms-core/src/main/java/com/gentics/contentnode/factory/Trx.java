package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.etc.TriConsumer;
import com.gentics.contentnode.object.SystemUser;

/**
 * AutoClosable implementation that creates a new transaction and closes it upon {@link #close()}.
 * It is important to call {@link #success()} before the transaction is closed, so that it is committed (otherwise it will be rolled back).
 */
public class Trx implements AutoCloseable {
	/**
	 * Transaction to commit/roll back
	 */
	private Transaction t;

	/**
	 * Previous transaction (may be null)
	 */
	private Transaction previous;

	/**
	 * Previously set session
	 */
	private Session previousSession;

	/**
	 * True if session in {@link ContentNodeHelper} was changed and needs to be reset to {@link #previousSession} in {@link Trx#close()}
	 */
	private boolean resetSession = false;

	/**
	 * Flag for marking success
	 */
	private boolean success = false;

	/**
	 * Execute the operation in a new transaction
	 * @param operator operator
	 * @throws NodeException
	 */
	public static void operate(Operator operator) throws NodeException {
		try (Trx trx = new Trx()) {
			operator.operate();
			trx.success();
		}
	}

	/**
	 * Execute the operation in a new transaction for the user
	 * @param user user
	 * @param operator operator
	 * @throws NodeException
	 */
	public static void operate(SystemUser user, Operator operator) throws NodeException {
		try (Trx trx = new Trx(user)) {
			operator.operate();
			trx.success();
		}
	}

	/**
	 * Execute in a new transaction
	 * @param consumer consumer that will get the transaction
	 * @throws NodeException
	 */
	public static void operate(Consumer<Transaction> consumer) throws NodeException {
		try (Trx trx = new Trx()) {
			consumer.accept(trx.getTransaction());
			trx.success();
		}
	}

	/**
	 * Execute in a new transaction for the user
	 * @param user user
	 * @param consumer consumer that will get the transaction
	 * @throws NodeException
	 */
	public static void operate(SystemUser user, Consumer<Transaction> consumer) throws NodeException {
		try (Trx trx = new Trx(user)) {
			consumer.accept(trx.getTransaction());
			trx.success();
		}
	}

	/**
	 * Execute the given supplier and return the supplied result in a new transaction
	 * @param supplier supplier
	 * @return supplied object
	 * @throws NodeException
	 */
	public static <R> R supply(Supplier<R> supplier) throws NodeException {
		R result;
		try (Trx trx = new Trx()) {
			result = supplier.supply();
			trx.success();
		}
		return result;
	}

	/**
	 * Execute the given supplier and return the supplied result in a new transaction for the user
	 * @param user user
	 * @param supplier supplier
	 * @return supplied object
	 * @throws NodeException
	 */
	public static <R> R supply(SystemUser user, Supplier<R> supplier) throws NodeException {
		R result;
		try (Trx trx = new Trx(user)) {
			result = supplier.supply();
			trx.success();
		}
		return result;
	}

	/**
	 * Execute the given supplier and return the supplied result in a new transaction
	 * @param supplier supplier
	 * @return supplied object
	 * @throws NodeException
	 */
	public static <R> R supply(Function<Transaction, R> supplier) throws NodeException {
		R result;
		try (Trx trx = new Trx()) {
			result = supplier.apply(trx.getTransaction());
			trx.success();
		}
		return result;
	}

	/**
	 * Execute the given supplier and return the supplied result in a new transaction for the user
	 * @param user user
	 * @param supplier supplier
	 * @return supplied object
	 * @throws NodeException
	 */
	public static <R> R supply(SystemUser user, Function<Transaction, R> supplier) throws NodeException {
		R result;
		try (Trx trx = new Trx(user)) {
			result = supplier.apply(trx.getTransaction());
			trx.success();
		}
		return result;
	}

	/**
	 * Execute the given consumer, passing t to it in a new transaction
	 * @param consumer consumer
	 * @param t consumed parameter
	 * @throws NodeException
	 */
	public static <T> void consume(Consumer<T> consumer, T t) throws NodeException {
		try (Trx trx = new Trx()) {
			consumer.accept(t);
			trx.success();
		}
	}

	/**
	 * Execute the given consumer, passing t to it in a new transaction for the user
	 * @param user user
	 * @param consumer consumer
	 * @param t consumed parameter
	 * @throws NodeException
	 */
	public static <T> void consume(SystemUser user, Consumer<T> consumer, T t) throws NodeException {
		try (Trx trx = new Trx(user)) {
			consumer.accept(t);
			trx.success();
		}
	}

	/**
	 * Execute the given consumer, passing t and u in a new transaction
	 * @param consumer consumer
	 * @param t first parameter
	 * @param u second parameter
	 * @throws NodeException
	 */
	public static <T, U> void consume(BiConsumer<T, U> consumer, T t, U u) throws NodeException {
		try (Trx trx = new Trx()) {
			consumer.accept(t, u);
			trx.success();
		}
	}

	/**
	 * Execute the given consumer, passing t, u and v in a new transaction
	 * @param consumer consumer
	 * @param t first parameter
	 * @param u second parameter
	 * @param v third parameter
	 * @throws NodeException
	 */
	public static <T, U, V> void consume(TriConsumer<T, U, V> consumer, T t, U u, V v) throws NodeException {
		try (Trx trx = new Trx()) {
			consumer.accept(t, u, v);
			trx.success();
		}
	}

	/**
	 * Execute the given consumer, passing t and u in a new transaction for the user
	 * @param user user
	 * @param consumer consumer
	 * @param t first parameter
	 * @param u second parameter
	 * @throws NodeException
	 */
	public static <T, U> void consume(SystemUser user, BiConsumer<T, U> consumer, T t, U u) throws NodeException {
		try (Trx trx = new Trx(user)) {
			consumer.accept(t, u);
			trx.success();
		}
	}

	/**
	 * Execute the given function in a new transaction
	 * @param function function
	 * @param t consumed parameter
	 * @return returned result
	 * @throws NodeException
	 */
	public static <T, R> R execute(Function<T, R> function, T t) throws NodeException {
		R result;
		try (Trx trx = new Trx()) {
			result = function.apply(t);
			trx.success();
		}
		return result;
	}

	/**
	 * Execute the given function in a new transaction for the user
	 * @param user user
	 * @param function function
	 * @param t consumed parameter
	 * @return returned result
	 * @throws NodeException
	 */
	public static <T, R> R execute(SystemUser user, Function<T, R> function, T t) throws NodeException {
		R result;
		try (Trx trx = new Trx(user)) {
			result = function.apply(t);
			trx.success();
		}
		return result;
	}

	/**
	 * Create a session for the user in a new transaction
	 * @param user user
	 * @return session
	 * @throws NodeException
	 */
	protected static Session createSession(SystemUser user) throws NodeException {
		try (Trx trx = new Trx()) {
			Session session = new Session(user, "", "", null, 0);
			trx.success();
			return session;
		}
	}

	/**
	 * Create a new transaction without session id or user id
	 * @throws NodeException
	 */
	public Trx() throws NodeException {
		this(null, 1, true);
	}

	/**
	 * Create a new transaction with given session id and user id
	 * @param sessionId session id
	 * @param userId user id
	 * @throws NodeException
	 */
	public Trx(String sessionId, Integer userId) throws NodeException {
		this(sessionId, userId, true);
	}

	/**
	 * Create a new transaction with given session id and user id, with or without using the db connection pool
	 * @param sessionId session id
	 * @param userId user id
	 * @param connectionPool true to get a db connection from the pool, false to create a new one
	 * @throws NodeException
	 */
	public Trx(String sessionId, Integer userId, boolean connectionPool) throws NodeException {
		previous = TransactionManager.getCurrentTransactionOrNull();
		t = getContentNodeFactory().startTransaction(sessionId, userId, connectionPool);
		previousSession = ContentNodeHelper.getSession();
		ContentNodeHelper.setSession(t.getSession());
		resetSession = true;
	}

	/**
	 * Create a new transaction with the given session
	 * @param session The session
	 * @param connectionPool true to get a db connection from the pool, false to create a new one
	 * @throws NodeException
	 */
	public Trx(Session session, boolean connectionPool) throws NodeException {
		previous = TransactionManager.getCurrentTransactionOrNull();
		t = getContentNodeFactory().startTransaction(Integer.toString(session.getSessionId()), session.getUserId(), connectionPool);
		previousSession = ContentNodeHelper.getSession();
		ContentNodeHelper.setSession(session);
		resetSession = true;
	}

	/**
	 * Create a new transaction with a new session for the given user
	 * @param user user
	 * @throws NodeException
	 */
	public Trx(SystemUser user) throws NodeException {
		this(user, true);
	}

	/**
	 * Create a new transaction with a new session for the given user
	 * @param user user
	 * @param connectionPool true to get a db connection from the pool, false to create a new one
	 * @throws NodeException
	 */
	public Trx(SystemUser user, boolean connectionPool) throws NodeException {
		this(createSession(user), connectionPool);
	}

	/**
	 * Get the default ContentNodeFactory
	 * @return The default ContentNodeFactory
	 */
	protected ContentNodeFactory getContentNodeFactory() {
		return ContentNodeFactory.getInstance();
	}

	/**
	 * Set success. The transaction will be committed, if this method has been called, or rolled back otherwise
	 */
	public void success() {
		this.success = true;
	}

	@Override
	public void close() throws NodeException {
		if (success) {
			t.commit(true);
		} else {
			t.rollback(true);
		}

		// set previous session
		if (resetSession) {
			ContentNodeHelper.setSession(previousSession);
		}

		// set the previous transaction
		TransactionManager.setCurrentTransaction(previous);
	}

	/**
	 * Get the transaction
	 * @return The transaction
	 */
	public Transaction getTransaction() {
		return t;
	}

	/**
	 * Set the transaction timestamp
	 * @param timestamp unix timestamp
	 * @return fluent API
	 */
	public Trx at(int timestamp) {
		t.setTimestamp(timestamp * 1000L);
		return this;
	}
}
