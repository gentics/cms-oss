package com.gentics.contentnode.rest.resource.impl;

import static org.junit.Assert.assertEquals;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.junit.Rule;
import org.junit.jupiter.api.Test;


@GCNFeature(set = {Feature.TRANSLATION})
class TranslationResourceImplTest {

	/**
	 * folder id of the folder where the tests are done
	 */
	public final static Integer FOLDER_ID = 28;
	@Rule
	public DBTestContext testContext = new DBTestContext();

	private TranslationResourceImplTest() {
	}

	static TranslationResourceImplTest createTranslationResourceImplTest() {
		return new TranslationResourceImplTest();
	}


	@Test
	public void givenPageInSourceLanguage_shouldBeTranslatedIntoTargetLanguage()
			throws TransactionException {
		try (Trx trx = ContentNodeHelper.trx()) {
			final var sourceLanguage = "de";
			final var targetLanguage = "de";
			Page pageToTranslate = createPage(sourceLanguage);

			var translationResource = new TranslationResourceImpl();

			translationResource.translate(pageToTranslate.getId(), targetLanguage, true, 0);
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Helper method to create a page via REST API
	 *
	 * @param language language of the page to create
	 * @return the created page
	 */
	private Page createPage(String language) throws TransactionException {
		PageResourceImpl pageResource = new PageResourceImpl();
		pageResource.setTransaction(TransactionManager.getCurrentTransaction());

		// create the request to create a page
		PageCreateRequest request = new PageCreateRequest();

		request.setFolderId(Integer.toString(FOLDER_ID));
		request.setLanguage(language);
		// post the request and get the response
		PageLoadResponse loadResponse = pageResource.create(request);

		TransactionManager.getCurrentTransaction().commit(false);

		// check whether the action succeeded
		//assertThat(loadResponse.getResponseInfo())
		//
		//assertSuccess(loadResponse);

		// check whether a page was returned
		//	assertNotNull("Check the created page", loadResponse.getPage());

		// check the language of the returned page
		assertEquals("Check language of created page", language, loadResponse.getPage().getLanguage());

		// return the created page
		return loadResponse.getPage();
	}
}