package com.gentics.contentnode.tests.feature.niceurl;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Uniqueness tests for pages (conflicting with files)
 */
@GCNFeature(set = { Feature.NICE_URLS, Feature.PUBLISH_CACHE })
@RunWith(value = Parameterized.class)
public class PageFileNiceURLUniquenessTest extends AbstractNiceURLUniquenessTest {
	/**
	 * Create test instance
	 */
	public PageFileNiceURLUniquenessTest() {
		testedType = TestedType.page;
		conflictType = TestedType.file;
	}
}
