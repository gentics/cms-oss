package com.gentics.portalconnector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.portalnode.connector.PLinkInformation;
import com.gentics.api.portalnode.connector.PLinkReplacer;
import com.gentics.api.portalnode.connector.PortalConnectorHelper;
import org.junit.experimental.categories.Category;

/**
 * Simple test that tests the plink replacer
 * @author johannes2
 *
 */
@Category(BaseLibTest.class)
public class PortalConnectorHelperTest {

	@Test
	public void testPLinkReplacer() {
		String toreplace = "just testing <h1>test</h1> " + "<plink id=\"10008.661\"> ... " + "<plink id=\"10008.662\" /> ... "
				+ "<plink id=\"10008.663\" att=\"test\" obj_type=\"10007\" />";

		System.out.println("string: " + toreplace);
		final PLinkInformation[] plinks = new PLinkInformation[3];
		final int[] i = new int[] { 0 };
		PLinkReplacer replacer = new PLinkReplacer() {
			public String replacePLink(PLinkInformation plink) {
				plinks[i[0]++] = plink;
				System.out.println("contentid: " + plink.getContentId());
				return plink.getContentId();
			}
		};
		String result = PortalConnectorHelper.replacePLinks(toreplace, replacer);

		assertNotNull(result);
		assertEquals("10008.661", plinks[0].getContentId());
		assertEquals("10008.662", plinks[1].getContentId());
		assertEquals("10008.663", plinks[2].getContentId());
		assertEquals("test", plinks[2].getAttributes().get("att"));
		assertEquals("10007", plinks[2].getAttributes().get("obj_type"));
	}
}
