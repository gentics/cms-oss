package com.gentics.node.tests.lib.mail;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.mail.MailSender;
import org.junit.experimental.categories.Category;

/**
 * Just a small test that tries to send a mail using the MailSender API
 * @author johannes2
 *
 */
@Ignore
@Category(BaseLibTest.class)
public class MailSenderTest {

	@Test
	public void testSendPlain() throws AddressException, NodeException, MessagingException {

		MailSender.sendPlain("localhost", "j.schueth@gentics.com", null, "", " sdgasdg ", "lalala");
	}

}
