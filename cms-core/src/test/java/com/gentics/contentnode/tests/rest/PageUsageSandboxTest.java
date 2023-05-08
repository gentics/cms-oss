package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.dirting.AbstractPageDirtingTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;

public class PageUsageSandboxTest extends AbstractPageDirtingTest {

	/**
	 * Valid page ID
	 */
	private static final Integer PAGE_ID = 45;

	@Test
	public void testTotalCount() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		PageResourceImpl resource = new PageResourceImpl();
		resource.setTransaction(t);

		Page page = t.getObject(Page.class, PAGE_ID, true);
		TotalUsageResponse response = resource.getTotalPageUsage(Arrays.asList(PAGE_ID), page.getNode().getId());
		TotalUsageInfo firstInfo = response.getInfos().values().iterator().next();
		ContentNodeTestUtils.assertResponseCodeOk(response);
		assertEquals("The page should not be used/referenced by any other elements.", 0, firstInfo.getTotal());

		Page pageVariant = page.createVariant();
		pageVariant.setFilename("someExtra.html");
		pageVariant.save();

		response = resource.getTotalPageUsage(Arrays.asList(PAGE_ID), page.getNode
				().getId());
		ContentNodeTestUtils.assertResponseCodeOk(response);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The page should not be used/referenced the created variant.", 1, firstInfo.getTotal());

		response = resource.getTotalPageUsage(Arrays.asList(targetPageId), node.getId());
		ContentNodeTestUtils.assertResponseCodeOk(response);
		firstInfo = response.getInfos().values().iterator().next();
		assertEquals("The page should be used/referenced by three other elements.", 3, firstInfo.getTotal());
		assertEquals("The page should be used/referenced by three pages.", 3, firstInfo.getPages().intValue());

	}
}
