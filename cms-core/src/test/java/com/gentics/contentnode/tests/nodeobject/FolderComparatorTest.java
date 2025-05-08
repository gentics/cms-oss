package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.utility.FolderComparator;

/**
 * Test cases for sorting {@link Folder}s with the {@link FolderComparator}
 */
public class FolderComparatorTest extends AbstractComparatorTest<Folder> {

	@Override
	protected String getName(Folder object) {
		return object.getName();
	}

	@Override
	protected List<Folder> getObjects() throws NodeException {
		return supply(t -> t.getObjects(Folder.class, DBUtils.select("SELECT id FROM folder", DBUtils.IDS)));
	}

	@Override
	protected void sortAscending() throws NodeException {
		Collections.sort(objects, new FolderComparator("name", "asc"));
	}

	@Override
	protected void sortDescending() throws NodeException {
		Collections.sort(objects, new FolderComparator("name", "desc"));
	}
}
