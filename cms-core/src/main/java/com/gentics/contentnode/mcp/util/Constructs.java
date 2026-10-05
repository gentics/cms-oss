package com.gentics.contentnode.mcp.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;

/**
 * Looks up constructs by keyword, which {@code PageResourceImpl} only does privately and reports as a plain
 * {@link NodeException}
 */
public final class Constructs {
	private Constructs() {
	}

	/**
	 * Get the construct with the given keyword. Must be called in a transaction.
	 * @param keyword construct keyword
	 * @return construct
	 * @throws EntityNotFoundException if no construct has the keyword
	 * @throws NodeException
	 */
	public static Construct byKeyword(String keyword) throws NodeException {
		int id = DBUtils.select("SELECT id FROM construct WHERE keyword = ?", pst -> pst.setString(1, keyword),
				rs -> rs.next() ? rs.getInt("id") : 0);
		Construct construct = id > 0 ? TransactionManager.getCurrentTransaction().getObject(Construct.class, id) : null;
		if (construct == null) {
			throw new EntityNotFoundException(
					"No construct has the keyword '%s', see list_constructs".formatted(keyword));
		}
		return construct;
	}
}
