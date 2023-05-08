package com.gentics.lib.content;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.db.DBHandle;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 06.11.2003
 */
public class GenticsContentResultHelper {
	public static GenticsContentResult restrictResult(GenticsContentResult result, int limit) throws NodeIllegalArgumentException {
		if (!(result instanceof GenticsContentResultImpl)) {
			throw new NodeIllegalArgumentException("Object " + result.getClass() + " not yet supported!");
		}
		return new GenticsContentResultImpl((GenticsContentResultImpl) result, limit);
	}

	public static GenticsContentResult restrictResult(GenticsContentResult result, int start,
			int count) throws NodeIllegalArgumentException {
		if (!(result instanceof GenticsContentResultImpl)) {
			throw new NodeIllegalArgumentException("Object " + result.getClass() + " not yet supported!");
		}
		return new GenticsContentResultImpl((GenticsContentResultImpl) result, start, count);
	}

	public static GenticsContentResult mergeResults(DBHandle handle,
			GenticsContentResult[] results, String sortAttributeName, int sortway) throws CMSUnavailableException, SQLException, NodeIllegalArgumentException {
		HashMap map = new HashMap(10);

		for (int i = 0; i < results.length; i++) {
			if (results[i] != null) {
				GenticsContentObject obj;

				while ((obj = results[i].getNextObject()) != null) {
					// add to a map to automagically eliminate double
					// content-ids
					map.put(obj.getContentId(), obj);
				}
			}
		}

		Vector v = new Vector(map.values());
		GenticsAttributeComparator c = new GenticsAttributeComparator(sortAttributeName);

		Collections.sort(v, c);

		if (sortway == GenticsContentSearch.SORT_ORDER_ASC) {
			return new GenticsContentResultImpl(v);
		}
		Collections.reverse(v);
		return new GenticsContentResultImpl(v);
	}
}
