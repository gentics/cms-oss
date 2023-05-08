package com.gentics.contentnode.tests.dirting;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


/**
 * Test dirting of changes in the languageset in a multichannelling context, using a channel tree with a master node and 5 channels
 * using multichannelling content repositories.
 * @author escitalopram
 *
 */
@RunWith(Parameterized.class)
public class MultichannellingLanguagesetDirtingTestMCCR extends MultichannellingLanguagesetDirtingTestNoMCCR {

	public MultichannellingLanguagesetDirtingTestMCCR(FixedChannelSubset fixed, Integer otherLangLocalization) {
		super(fixed, otherLangLocalization);
	}

	@BeforeClass
	public static void setupClass() throws Exception {
		setupBaseObjects(true);
	}

}
