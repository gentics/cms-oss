package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.utility.PageComparator;

/**
 * Test cases for sorting {@link Page}s with the {@link PageComparator}
 */
public class PageComparatorTest extends AbstractComparatorTest<Page> {

	@Override
	protected String getName(Page object) {
		return object.getName();
	}

	@Override
	protected List<Page> getObjects() throws NodeException {
		return supply(t -> t.getObjects(Page.class, DBUtils.select("SELECT id FROM page", DBUtils.IDS)));
	}

	@Override
	protected void sortAscending() throws NodeException {
		Collections.sort(objects, new PageComparator("name", "asc"));
	}

	@Override
	protected void sortDescending() throws NodeException {
		Collections.sort(objects, new PageComparator("name", "desc"));
	}
}
