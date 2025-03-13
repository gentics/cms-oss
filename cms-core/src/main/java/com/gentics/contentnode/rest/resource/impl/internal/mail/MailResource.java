package com.gentics.contentnode.rest.resource.impl.internal.mail;

import jakarta.mail.MessagingException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.impl.internal.InternalResource;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.mail.MailSender;

/**
 * Resource for sending emails
 */
@Path("/internal/mail")
public class MailResource extends InternalResource {
	@POST
	public GenericResponse send(SendMailRequest request) throws NodeException {
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getPreferences();

		String mailhost = prefs.getProperty(MessageSender.MAILHOST_PARAM);
		int mailPort = ObjectTransformer.getInt(prefs.getProperty(MessageSender.MAILPORT_PARAM), -1);
		String mailUsername = prefs.getProperty(MessageSender.MAILUSERNAME_PARAM);
		String mailPassword = prefs.getProperty(MessageSender.MAILPASSWORD_PARAM);
		String mailStartTls = prefs.getProperty(MessageSender.MAILSTARTTLS_PARAM);
		String returnPath = prefs.getProperty(MessageSender.MAILRETURNPATH_PARAM);

		if (mailhost == null) {
			mailhost = "localhost";
		}
		if (ObjectTransformer.isEmpty(returnPath)) {
			returnPath = null;
		}

		MailSender sender = new MailSender()
				.setHost(mailhost)
				.setTo(request.getTo())
				.setFrom(request.getFrom() != null ? request.getFrom() : returnPath)
				.setSubject(request.getSubject())
				.setBodyText(request.getBody())
				.setEnvelopeFrom(returnPath);

		if (mailPort > 0) {
			sender.setPort(mailPort);
		}
		if (!StringUtils.isEmpty(mailStartTls)) {
			sender.setStartTLS(Boolean.parseBoolean(mailStartTls));
		}
		if (!StringUtils.isEmpty(mailUsername)) {
			sender.setAuth(mailUsername, mailPassword);
		}
		try {
			sender.send();
		} catch (MessagingException e) {
			throw new NodeException(String.format("Error while sending email with subject %s to %s",
					request.getSubject(), request.getTo()), e);
		}

		return new GenericResponse(null, ResponseInfo.ok("Successfully sent mail"));
	}
}
