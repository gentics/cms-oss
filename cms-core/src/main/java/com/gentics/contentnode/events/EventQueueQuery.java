package com.gentics.contentnode.events;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.rest.resource.parameter.DirtQueueParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;

/**
 * Query for getting dirt queue entries
 */
public class EventQueueQuery {
	private Boolean failed;

	private Integer startTimestamp;

	private Integer endTimestamp;

	private int start = 0;

	private int pageSize = -1;

	/**
	 * Set query parameters
	 * @param param parameter bean
	 * @return fluent API
	 */
	public EventQueueQuery query(DirtQueueParameterBean param) {
		if (param != null) {
			failed = param.failed;
			startTimestamp = param.start;
			endTimestamp = param.end;
		}
		return this;
	}

	/**
	 * Set paging
	 * @param paging paging bean
	 * @return fluent API
	 */
	public EventQueueQuery page(PagingParameterBean paging) {
		if (paging != null) {
			start = (Math.max(paging.page, 1) - 1) * paging.pageSize;
			pageSize = paging.pageSize;
		}
		return this;
	}

	/**
	 * Get the filtered, paged queue entry list
	 * @return list
	 * @throws NodeException
	 */
	public List<QueueEntry> get() throws NodeException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT *");
		sql.append(getFromClause());
		sql.append(getWhereClause());

		sql.append(" ORDER BY timestamp ASC");
		if (pageSize > 0) {
			sql.append(String.format(" LIMIT %d, %d", start, pageSize));
		}

		return DBUtils.select(sql.toString(), params(), rs -> {
			List<QueueEntry> list = new ArrayList<>();
			while (rs.next()) {
				list.add(new QueueEntry(rs));
			}
			return list;
		});
	}

	/**
	 * Delete queue entries
	 * return number of deleted entries
	 * @throws NodeException
	 */
	public int delete() throws NodeException {
		String sql = "DELETE"
				+ getFromClause()
				+ getWhereClause();

		return DBUtils.update(sql, args());
	}

	private Object[] args() {
		List<Object> args = new ArrayList<>();

		if (failed != null) {
			args.add(failed);
		}
		if (startTimestamp != null) {
			args.add(startTimestamp);
		}
		if (endTimestamp != null) {
			args.add(endTimestamp);
		}

		return args.toArray();
	}

	/**
	 * Count the number of filtered queue entries
	 * @return count
	 * @throws NodeException
	 */
	public int count() throws NodeException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT count(*) c");
		sql.append(getFromClause());
		sql.append(getWhereClause());

		return DBUtils.select(sql.toString(), params(), DBUtils.firstInt("c"));
	}

	/**
	 * Check whether there are more items (if paging is used)
	 * @param totalCount total count
	 * @return true if there are more items
	 */
	public boolean hasMore(int totalCount) {
		if (pageSize <= 0) {
			return false;
		} else {
			return totalCount > start + pageSize;
		}
	}

	/**
	 * Get parameter preparator
	 * @return preparator
	 */
	protected PrepareStatement params() {
		return stmt -> {
			int paramCounter = 0;
			if (failed != null) {
				stmt.setBoolean(++paramCounter, failed);
			}
			if (startTimestamp != null) {
				stmt.setInt(++paramCounter, startTimestamp);
			}
			if (endTimestamp != null) {
				stmt.setInt(++paramCounter, endTimestamp);
			}
		};
	}

	/**
	 * Get the FROM clause
	 * @return clause
	 * @throws NodeException
	 */
	protected String getFromClause() throws NodeException {
		StringBuilder sql = new StringBuilder();
		sql.append(" FROM dirtqueue");

		return sql.toString();
	}

	/**
	 * Get the WHERE clause
	 * @return clause
	 * @throws NodeException
	 */
	protected String getWhereClause() throws NodeException {
		if (failed == null && startTimestamp == null && endTimestamp == null) {
			return "";
		}

		List<String> clauses = new ArrayList<>();
		if (failed != null) {
			clauses.add("failed = ?");
		}

		if (startTimestamp != null) {
			clauses.add("timestamp >= ?");
		}

		if (endTimestamp != null) {
			clauses.add("timestamp <= ?");
		}

		return " WHERE " + clauses.stream().collect(Collectors.joining(" AND "));
	}
}