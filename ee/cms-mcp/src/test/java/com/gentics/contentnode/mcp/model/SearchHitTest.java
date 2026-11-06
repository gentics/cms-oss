package com.gentics.contentnode.mcp.model;

import org.junit.Test;

/**
 * Unit test for {@link SearchHit}.
 */
public class SearchHitTest {
	@Test
	public void testSchemaMatchesRecord() {
		ModelJsonSchemaTest.assertMatches(SearchHit.class, SearchHit.jsonSchema());
	}
}
