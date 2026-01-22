package com.gentics.contentnode.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.lib.db.DBQuery;
import com.gentics.lib.db.DBQueryHandler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * {@link AutoCloseable} implementation of {@link DBQueryHandler} which will count the number of SQL statements,
 * which are executed between creation and call to {@link #close()} and will assert that the total count does not exceed
 * the given number
 */
public class QueryCountAsserter implements DBQueryHandler, AutoCloseable {
	/**
	 * Meter registry
	 */
	protected final MeterRegistry registry = new SimpleMeterRegistry();

	/**
	 * SQL Statement timer meters
	 */
	protected final Map<String, Timer> sqlTimers = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Total timer mete
	 */
	protected final Timer totalTimer = Timer.builder("total").register(registry);

	/**
	 * Maximum allowed number of SQL Statements
	 */
	protected final long maxSqlStatements;

	/**
	 * Registry uuid
	 */
	private String uuid;

	/**
	 * Create an instance that will allow the given number of statements
	 * @param maxSqlStatements allowed number of statements
	 * @return instance
	 */
	public final static QueryCountAsserter allow(long maxSqlStatements) {
		return new QueryCountAsserter(maxSqlStatements);
	}

	/**
	 * Create an instance
	 * @param maxSqlStatements allowed number of statements
	 */
	private QueryCountAsserter(long maxSqlStatements) {
		this.maxSqlStatements = maxSqlStatements;
		uuid = DBQuery.register(this);
	}

	@Override
	public void query(String sql, long durationMs) {
		totalTimer.record(durationMs, TimeUnit.MILLISECONDS);
		sqlTimers.computeIfAbsent(sql, k -> {
			return Timer.builder(k).register(registry);
		}).record(durationMs, TimeUnit.MILLISECONDS);
	}

	@Override
	public void close() {
		try {
			StringBuilder log = new StringBuilder();
			List<Pair<String, Long>> statementCounts = new ArrayList<>();
			sqlTimers.entrySet().forEach(entry -> {
				String sql = entry.getKey();
				long count = entry.getValue().count();
				statementCounts.add(Pair.of(sql, count));
			});
			statementCounts.sort((p1, p2) -> {
				return -Long.compare(p1.getRight(), p2.getRight());
			});

			statementCounts.forEach(p -> {
				log.append("\n%d: %s".formatted(p.getRight(), p.getLeft()));
			});

			assertThat(totalTimer.count()).as("Total number of sql statements:%s".formatted(log.toString())).isLessThanOrEqualTo(maxSqlStatements);
		} finally {
			registry.clear();
			DBQuery.unregister(uuid);
		}
	}
}
