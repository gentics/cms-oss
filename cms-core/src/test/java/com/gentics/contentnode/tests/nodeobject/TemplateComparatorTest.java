package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.utility.TemplateComparator;
import com.gentics.contentnode.rest.model.Template;

/**
 * Test cases for sorting {@link Template}s with the {@link TemplateComparator}
 */
public class TemplateComparatorTest extends AbstractComparatorTest<Template> {

	@Override
	protected String getName(Template object) {
		return object.getName();
	}

	@Override
	protected List<Template> getObjects() throws NodeException {
		List<com.gentics.contentnode.object.Template> list = supply(t -> t.getObjects(com.gentics.contentnode.object.Template.class,
				DBUtils.select("SELECT id FROM template", DBUtils.IDS)));
		List<Template> result = new ArrayList<>();
		for (com.gentics.contentnode.object.Template template : list) {
			result.add(com.gentics.contentnode.object.Template.TRANSFORM2REST.apply(template));
		}
		return result;
	}

	@Override
	protected void sortAscending() throws NodeException {
		Collections.sort(objects, new TemplateComparator("name", "asc"));
	}

	@Override
	protected void sortDescending() throws NodeException {
		Collections.sort(objects, new TemplateComparator("name", "desc"));
	}
}
