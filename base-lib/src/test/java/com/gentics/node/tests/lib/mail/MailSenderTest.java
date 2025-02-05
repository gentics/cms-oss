package com.gentics.node.tests.lib.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.lib.mail.MailSender;
import com.icegreen.greenmail.junit4.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;

/**
 * Just a small test that tries to send a mail using the MailSender API
 */
public class MailSenderTest {
	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetup.SMTP.dynamicPort());

	/**
	 * Get the sorted list of addresses separated by , (comma)
	 * If the list is null or empty, return empty string
	 * @param addresses list of addresses
	 * @return list as comma separated values
	 */
	protected static String getAddressList(Address[] addresses) {
		if (addresses == null) {
			return "";
		}

		return Stream.of(addresses).map(Address::toString).sorted().collect(Collectors.joining(","));
	}

	/**
	 * Get the properties from the given message as map
	 * @param message message
	 * @return map containing the properties
	 */
	protected static Map<String, String> messageProperties(MimeMessage message) {
		try {
			return Map.of(
					"to", getAddressList(message.getRecipients(RecipientType.TO)),
					"cc", getAddressList(message.getRecipients(RecipientType.CC)),
					"from", getAddressList(message.getFrom()),
					"return-path", message.getHeader("return-path", ""),
					"subject", message.getSubject(),
					"text", message.getContent().toString()
					);
		} catch (MessagingException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testSendPlain() throws Exception {
		new MailSender()
			.setHost(greenMail.getSmtp().getBindTo())
			.setPort(greenMail.getSmtp().getPort())
			.setTo("you@gentics.com")
			.setCc("someone@gentics.com")
			.setFrom("me@gentics.com")
			.setEnvelopeFrom("admin@gentics.com")
			.setSubject("About the test")
			.setBodyText("This is the tested mail text.")
			.send();

		MimeMessage[] mails = greenMail.getReceivedMessages();
		assertThat(Stream.of(mails).map(MailSenderTest::messageProperties))
			.containsOnly(Map.of(
					"to", "you@gentics.com",
					"from", "me@gentics.com",
					"cc", "someone@gentics.com",
					"return-path", "<admin@gentics.com>",
					"subject", "About the test",
					"text", "This is the tested mail text."));
	}
}
