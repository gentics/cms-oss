package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Transactional;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for transactionals
 */
public class TransactionalTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test committing a transaction with a transactional, that tries to add another transactional
	 * @throws NodeException
	 */
	@Test
	public void testTransactionalCommit() throws NodeException {
		TestTransactional inner = new TestTransactional();
		TestTransactional outer = new TestTransactional(inner);

		try (Trx trx = new Trx()) {
			trx.getTransaction().addTransactional(outer);
			trx.success();
		}

		// we assert that the outer transactional was committed
		assertThat(outer.dbCommitCount.get()).as("Outer db commit count").isEqualTo(1);
		assertThat(outer.transactionCommitCount.get()).as("Outer transaction commit count").isEqualTo(1);
		assertThat(outer.transactionRollbackCount.get()).as("Outer transaction rollback count").isEqualTo(0);

		// we assert that the inner transactional was not used, because it is not allowed to add transactionals from other transactionals
		assertThat(inner.dbCommitCount.get()).as("Inner db commit count").isEqualTo(0);
		assertThat(inner.transactionCommitCount.get()).as("Inner transaction commit count").isEqualTo(0);
		assertThat(inner.transactionRollbackCount.get()).as("Inner transaction rollback count").isEqualTo(0);
	}

	/**
	 * Test rolling back a transaction with a transactional, that tries to add another transactional
	 * @throws NodeException
	 */
	@Test
	public void testTransactionalRollback() throws NodeException {
		TestTransactional inner = new TestTransactional();
		TestTransactional outer = new TestTransactional(inner);

		try (Trx trx = new Trx()) {
			trx.getTransaction().addTransactional(outer);
		}

		// we assert that the outer transactional was rolled back
		assertThat(outer.dbCommitCount.get()).as("Outer db commit count").isEqualTo(0);
		assertThat(outer.transactionCommitCount.get()).as("Outer transaction commit count").isEqualTo(0);
		assertThat(outer.transactionRollbackCount.get()).as("Outer transaction rollback count").isEqualTo(1);

		// we assert that the inner transactional was not used, because it is not allowed to add transactionals from other transactionals
		assertThat(inner.dbCommitCount.get()).as("Inner db commit count").isEqualTo(0);
		assertThat(inner.transactionCommitCount.get()).as("Inner transaction commit count").isEqualTo(0);
		assertThat(inner.transactionRollbackCount.get()).as("Inner transaction rollback count").isEqualTo(0);
	}

	/**
	 * Test transactional
	 */
	public static class TestTransactional extends AbstractTransactional {
		/**
		 * Counter for calls to {@link #onDBCommit(Transaction)}
		 */
		protected AtomicInteger dbCommitCount = new AtomicInteger();

		/**
		 * Counter for calls to {@link #onTransactionCommit(Transaction)}
		 */
		protected AtomicInteger transactionCommitCount = new AtomicInteger();

		/**
		 * Counter for calls to {@link #onTransactionRollback(Transaction)}
		 */
		protected AtomicInteger transactionRollbackCount = new AtomicInteger();

		/**
		 * Optional inner transaction
		 */
		protected Transactional inner;

		/**
		 * Create a transactional without inner transactional
		 */
		public TestTransactional() {
			this(null);
		}

		/**
		 * Create a transactional with the given inner transactional
		 * @param inner inner transactional
		 */
		public TestTransactional(Transactional inner) {
			this.inner = inner;
		}

		@Override
		public void onDBCommit(Transaction t) throws NodeException {
			dbCommitCount.incrementAndGet();
			if (inner != null) {
				t.addTransactional(inner);
			}
		}

		@Override
		public boolean onTransactionCommit(Transaction t) {
			transactionCommitCount.incrementAndGet();
			return false;
		}

		@Override
		public void onTransactionRollback(Transaction t) {
			transactionRollbackCount.incrementAndGet();
			if (inner != null) {
				t.addTransactional(inner);
			}
		}
	}
}
