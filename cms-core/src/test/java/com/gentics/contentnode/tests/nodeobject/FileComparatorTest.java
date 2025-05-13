package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.utility.FileComparator;

/**
 * Test cases for sorting {@link File}s with the {@link FileComparator}
 */
public class FileComparatorTest extends AbstractComparatorTest<File> {

	@Override
	protected String getName(File object) {
		return object.getName();
	}

	@Override
	protected List<File> getObjects() throws NodeException {
		return supply(t -> t.getObjects(File.class, DBUtils.select("SELECT id FROM contentfile", DBUtils.IDS)));
	}

	@Override
	protected void sortAscending() throws NodeException {
		Collections.sort(objects, new FileComparator("name", "asc"));
	}

	@Override
	protected void sortDescending() throws NodeException {
		Collections.sort(objects, new FileComparator("name", "desc"));
	}
}
