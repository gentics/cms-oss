package com.gentics.contentnode.tests.feature.niceurl;


import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test filtering objects by nice URL
 */
@GCNFeature(set = { Feature.NICE_URLS, Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class MCNiceURLFilterTest extends NiceURLFilterTest {
}
