package com.gentics.contentnode.tests.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.AuthenticationResource;
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
	public static ConstructResourceImpl getConstructResource() throws NodeException {
		return new ConstructResourceImpl();
	}

	/**
	 * Get a page resource for the current transaction
	 * @return page resource
	 * @throws NodeException
	 */
	public static PageResourceImpl getPageResource() throws NodeException {
		return new PageResourceImpl();
	}

	/**
	 * Get a file resource for the current transaction
	 * @return file resource
	 * @throws NodeException
	 */
	public static FileResourceImpl getFileResource() throws NodeException {
		return new FileResourceImpl();
	}

	/**
	 * Get a user resource for the current transaction
	 * @return user resource
	 * @throws NodeException
	 */
	public static UserResourceImpl getUserResource() throws NodeException {
		return new UserResourceImpl();
	}

	/**
	 * Get a image resource for the current transaction
	 * @return image resource
	 * @throws NodeException
	 */
	public static ImageResourceImpl getImageResource() throws NodeException {
		return new ImageResourceImpl();
	}

	/**
	 * Get an auth resource for the current transaction
	 * @return auth resource
	 * @throws NodeException
	 */
	public static AuthenticationResourceImpl getAuthResource() throws NodeException {
		return new AuthenticationResourceImpl();
	}

	/**
	 * Get a folder resource
	 * @return folder resource
	 * @throws Exception
	 */
	public static FolderResourceImpl getFolderResource() throws NodeException {
		return new FolderResourceImpl();
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
	public static TemplateResourceImpl getTemplateResource() throws NodeException {
		return new TemplateResourceImpl();
	}

	/**
	 * Perform login for the given luser credentials
	 * @param login login
	 * @param password password
	 * @return session
	 * @throws NodeException
	 */
	public static Session login(String login, String password) throws NodeException {
		AuthenticationResource resource = getAuthResource();
		LoginRequest request = new LoginRequest();
		request.setLogin(login);
		request.setPassword(password);
		LoginResponse response = resource.login(request, "0");
		assertResponseOK(response);

		int sid = Integer.parseInt(response.getSid());
		return new DBSession(sid, TransactionManager.getCurrentTransaction());
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
			if (StringUtils.isNotBlank(expectedResponseMessage)) {
				assertThat(response.getResponseInfo().getResponseMessage()).as("Checkresponse message").endsWith(expectedResponseMessage);
			}
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
