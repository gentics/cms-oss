package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.utility.ConstructComparator;
import com.gentics.contentnode.rest.model.request.ConstructSortAttribute;
import com.gentics.contentnode.rest.model.request.SortOrder;

/**
 * Test cases for sorting {@link Construct}s with the {@link ConstructComparator}
 */
public class ConstructComparatorTest extends AbstractComparatorTest<Construct> {

	@Override
	protected String getName(Construct object) {
		return object.getName().toString();
	}

	@Override
	protected List<Construct> getObjects() throws NodeException {
		return supply(t -> t.getObjects(Construct.class, DBUtils.select("SELECT id FROM construct", DBUtils.IDS)));
	}

	@Override
	protected void sortAscending() throws NodeException {
		Collections.sort(objects, new ConstructComparator(ConstructSortAttribute.name, SortOrder.asc));
	}

	@Override
	protected void sortDescending() throws NodeException {
		Collections.sort(objects, new ConstructComparator(ConstructSortAttribute.name, SortOrder.desc));
	}
}
