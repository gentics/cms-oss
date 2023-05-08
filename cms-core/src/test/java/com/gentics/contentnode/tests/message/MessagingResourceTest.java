package com.gentics.contentnode.tests.message;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.MessageSendRequest;
import com.gentics.contentnode.rest.model.request.MessagesReadRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.resource.impl.MessagingResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the messaging resource
 */
public class MessagingResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature("inbox_to_email", false);

		user = Trx.supply(t -> {
			List<Integer> ids = DBUtils.select("SELECT id FROM systemuser WHERE login = ?", ps -> {
				ps.setString(1, "editor");
			}, DBUtils.IDLIST);

			assertThat(ids).as("User IDs").isNotEmpty();
			return t.getObject(SystemUser.class, ids.get(0));
		});
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> update("DELETE FROM msg"));
	}

	/**
	 * Test sending a message
	 * @throws NodeException
	 */
	@Test
	public void testSend() throws NodeException {
		operate(user, () -> {
			MessageSendRequest request = new MessageSendRequest();
			request.setToUserId(Arrays.asList(user.getId()));
			request.setMessage("Test Message");
			assertResponseOK(new MessagingResourceImpl().send(request));
		});

		operate(user, () -> {
			GenericResponse response = new MessagingResourceImpl().list(false);
			assertResponseOK(response);

			assertThat(response.getMessages()).as("Messages").usingElementComparatorOnFields("type", "message").containsOnly(new Message(Type.INFO, "Test Message"));
		});
	}

	/**
	 * Test marking a message "read"
	 * @throws NodeException
	 */
	@Test
	public void testMarkRead() throws NodeException {
		operate(user, () -> {
			MessageSendRequest request = new MessageSendRequest();
			request.setToUserId(Arrays.asList(user.getId()));
			request.setMessage("Test Message");
			assertResponseOK(new MessagingResourceImpl().send(request));
		});

		Message message = supply(user, () -> {
			GenericResponse response = new MessagingResourceImpl().list(true);
			assertResponseOK(response);

			assertThat(response.getMessages()).as("Messages").usingElementComparatorOnFields("type", "message").containsOnly(new Message(Type.INFO, "Test Message"));
			return response.getMessages().get(0);
		});

		operate(user, () -> {
			MessagesReadRequest request = new MessagesReadRequest();
			request.setMessages(Arrays.asList(message.getId()));
			assertResponseOK(new MessagingResourceImpl().read(request));
		});

		operate(user, () -> {
			GenericResponse response = new MessagingResourceImpl().list(true);
			assertResponseOK(response);

			assertThat(response.getMessages()).as("Messages").isEmpty();

			response = new MessagingResourceImpl().list(false);
			assertResponseOK(response);

			assertThat(response.getMessages()).as("Messages").isNotEmpty();

		});
	}

	/**
	 * Test deleting a message
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		operate(user, () -> {
			MessageSendRequest request = new MessageSendRequest();
			request.setToUserId(Arrays.asList(user.getId()));
			request.setMessage("Test Message");
			assertResponseOK(new MessagingResourceImpl().send(request));
		});

		Message message = supply(user, () -> {
			GenericResponse response = new MessagingResourceImpl().list(true);
			assertResponseOK(response);

			assertThat(response.getMessages()).as("Messages").usingElementComparatorOnFields("type", "message").containsOnly(new Message(Type.INFO, "Test Message"));
			return response.getMessages().get(0);
		});

		operate(user, () -> {
			new MessagingResourceImpl().delete(message.getId());

			GenericResponse response = new MessagingResourceImpl().list(false);
			assertResponseOK(response);

			assertThat(response.getMessages()).as("Messages").isEmpty();
		});
	}
}
