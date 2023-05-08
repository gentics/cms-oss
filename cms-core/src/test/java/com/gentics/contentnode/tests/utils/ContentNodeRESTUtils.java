package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.AuthenticationResource;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.AuthenticationResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;

/**
 * Static helper class for using the REST API in Tests
 */
public class ContentNodeRESTUtils {

	/**
	 * Get a construct resource for the current transaction
	 * @return construct resource
	 * @throws NodeException
	 */
	public static ConstructResource getConstructResource() throws NodeException {
		return new ConstructResourceImpl();
	}

	/**
	 * Get a page resource for the current transaction
	 * @return page resource
	 * @throws NodeException
	 */
	public static PageResource getPageResource() throws NodeException {
		PageResourceImpl resource = new PageResourceImpl();
		resource.setTransaction(TransactionManager.getCurrentTransaction());
		return resource;
	}

	/**
	 * Get a file resource for the current transaction
	 * @return file resource
	 * @throws NodeException
	 */
	public static FileResource getFileResource() throws NodeException {
		FileResourceImpl resource = new FileResourceImpl();

		resource.setTransaction(TransactionManager.getCurrentTransaction());

		return resource;
	}

	/**
	 * Get a user resource for the current transaction
	 * @return user resource
	 * @throws NodeException
	 */
	public static UserResource getUserResource() throws NodeException {
		UserResource resource = new UserResourceImpl();
		return resource;
	}

	/**
	 * Get a image resource for the current transaction
	 * @return image resource
	 * @throws NodeException
	 */
	public static ImageResource getImageResource() throws NodeException {
		ImageResourceImpl resource = new ImageResourceImpl();

		resource.setTransaction(TransactionManager.getCurrentTransaction());

		return resource;
	}

	/**
	 * Get an auth resource for the current transaction
	 * @return auth resource
	 * @throws NodeException
	 */
	public static AuthenticationResource getAuthResource() throws NodeException {
		AuthenticationResourceImpl resource = new AuthenticationResourceImpl();
		resource.setTransaction(TransactionManager.getCurrentTransaction());
		return resource;
	}

	/**
	 * Get a folder resource
	 * @return folder resource
	 * @throws Exception
	 */
	public static FolderResource getFolderResource() throws NodeException {
		FolderResourceImpl res = new FolderResourceImpl();

		res.setTransaction(TransactionManager.getCurrentTransaction());
		return res;
	}

	/**
	 * Get a node resource
	 * @return node resource
	 * @throws NodeException
	 */
	public static NodeResourceImpl getNodeResource() throws NodeException {
		return new NodeResourceImpl();
	}

	/**
	 * Get a template resource
	 * @return template resource
	 * @throws NodeException
	 */
	public static TemplateResource getTemplateResource() throws NodeException {
		return new TemplateResourceImpl();
	}

	/**
	 * Perform login for the given luser credentials
	 * @param login login
	 * @param password password
	 * @return sid
	 * @throws NodeException
	 */
	public static String login(String login, String password) throws NodeException {
		AuthenticationResource resource = getAuthResource();
		LoginRequest request = new LoginRequest();
		request.setLogin(login);
		request.setPassword(password);
		LoginResponse response = resource.login(request, "0");
		assertResponseOK(response);
		return response.getSid();
	}

	/**
	 * Assert that the response code is {@link ResponseCode.OK}
	 * @param response response
	 * @throws Exception
	 */
	public static void assertResponseOK(GenericResponse response) {
		assertResponse(response, ResponseCode.OK);
	}

	/**
	 * Assert that the response code is as expected
	 * @param response response
	 * @param expected expected response code
	 */
	public static void assertResponse(GenericResponse response, ResponseCode expected) {
		assertNotNull("Response must not be null", response);
		String firstMessage = "";
		if (response.getMessages() != null && !response.getMessages().isEmpty()) {
			firstMessage = response.getMessages().get(0).getMessage();
		}
		assertEquals("Check response code (" + response.getResponseInfo().getResponseMessage() + ") Msg: (" + firstMessage + ")", expected,
				response.getResponseInfo().getResponseCode());
	}

	/**
	 * Assert that the response has code and message as expected. If optional messages are given, they are also asserted (all of them in the same order)
	 * @param response response
	 * @param expectedCode expected response code
	 * @param expectedResponseMessage expected response message
	 * @param expectedMessages optional expected messages
	 */
	public static void assertResponse(GenericResponse response, ResponseCode expectedCode, String expectedResponseMessage, Message... expectedMessages) {
		assertNotNull("Response must not be null", response);
		if (expectedCode != null) {
			assertNotNull("Response must contain a response info", response.getResponseInfo());
			assertEquals("Check response code", expectedCode, response.getResponseInfo().getResponseCode());
			assertEquals("Check response message", expectedResponseMessage, response.getResponseInfo().getResponseMessage());
		}

		if (!ObjectTransformer.isEmpty(expectedMessages)) {
			assertEquals("Check # of messages", expectedMessages.length, response.getMessages().size());
			for (int i = 0; i < expectedMessages.length; i++) {
				assertEquals("Check message type #" + i, expectedMessages[i].getType(), response.getMessages().get(i).getType());
				assertEquals("Check message #" + i, expectedMessages[i].getMessage(), response.getMessages().get(i).getMessage());
			}
		}
	}
}
