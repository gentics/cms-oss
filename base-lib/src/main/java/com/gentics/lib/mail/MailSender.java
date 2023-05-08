package com.gentics.lib.mail;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;
import com.sun.mail.smtp.SMTPMessage;

/**
 * Mail Sender
 */
public class MailSender {

	private static NodeLogger logger = NodeLogger.getNodeLogger(MailSender.class);

	private String host;

	private Integer port;

	private String to;

	private String cc;

	private String from;

	private String envelopeFrom;

	private String subject;

	private String bodyText;

	private String bodyPart;

	private String mimeType;

	private String username;

	private String password;

	private Boolean startTLS;

	private boolean debug = false;

	/**
	 * Set the mail server host
	 * @param host host
	 * @return fluent API
	 */
	public MailSender setHost(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Set the mail server port
	 * @param port port
	 * @return fluent API
	 */
	public MailSender setPort(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Set the addressee
	 * @param to addressee
	 * @return fluent API
	 */
	public MailSender setTo(String to) {
		this.to = to;
		return this;
	}

	/**
	 * Set CC addressees
	 * @param cc addressees
	 * @return fluent API
	 */
	public MailSender setCc(String cc) {
		this.cc = cc;
		return this;
	}

	/**
	 * Set the from address
	 * @param from from address
	 * @return fluent API
	 */
	public MailSender setFrom(String from) {
		this.from = from;
		return this;
	}

	/**
	 * Set the envelope from address
	 * @param envelopeFrom envelope from address
	 * @return fluent API
	 */
	public MailSender setEnvelopeFrom(String envelopeFrom) {
		this.envelopeFrom = envelopeFrom;
		return this;
	}

	/**
	 * Set the mail subject
	 * @param subject mail subject
	 * @return fluent API
	 */
	public MailSender setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	/**
	 * Set the mail body as plain text
	 * @param bodyText mail body as plain text
	 * @return fluent API
	 */
	public MailSender setBodyText(String bodyText) {
		this.bodyText = bodyText;
		return this;
	}

	/**
	 * Set the mail body as HTML
	 * @param bodyHTML mail body as HTML
	 * @return fluent API
	 */
	public MailSender setBodyHTML(String bodyHTML) {
		return setBodyPart(bodyHTML, "text/html");
	}

	/**
	 * Set the mail body with mimetype
	 * @param bodyPart body part
	 * @param mimetype mime type
	 * @return fluent API
	 */
	public MailSender setBodyPart(String bodyPart, String mimetype) {
		this.bodyPart = bodyPart;
		this.mimeType = mimetype;
		return this;
	}

	/**
	 * Set the authentication
	 * @param username username
	 * @param password password
	 * @return fluent API
	 */
	public MailSender setAuth(String username, String password) {
		this.username = username;
		this.password = password;
		return this;
	}

	/**
	 * Set whether STARTTLS shall be used
	 * @param startTLS flag
	 * @return fluent API
	 */
	public MailSender setStartTLS(boolean startTLS) {
		this.startTLS = startTLS;
		return this;
	}

	/**
	 * Set the debug flag
	 * @param debug flag
	 * @return fluent API
	 */
	public MailSender setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	/**
	 * Send the message
	 * @throws NodeException
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public void send() throws NodeException, AddressException, MessagingException {
		if (StringUtils.isBlank(host)) {
			throw new NodeException("MailSender.send host must be defined");
		}
		if (StringUtils.isBlank(to)) {
			throw new NodeException("MailSender.send to must be defined");
		}
		if (StringUtils.isBlank(from)) {
			throw new NodeException("MailSender.send from must be defined");
		}

		Properties props = new Properties(System.getProperties());
		props.put("mail.smtp.host", host);
		if (port != null) {
			props.put("mail.smtp.port", port.toString());
		}
		if (startTLS != null) {
			props.put("mail.smtp.starttls.enable", startTLS.toString());
		}
		if (debug) {
			props.put("mail.smtp.debug", "true");
		}
		Authenticator auth = null;
		if (!StringUtils.isBlank(username)) {
			props.put("mail.smtp.auth", "true");
			auth = new Authenticator() {
				//override the getPasswordAuthentication method
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			};
		}

		Session session = Session.getDefaultInstance(props, auth);
		if (debug) {
			session.setDebug(true);
		}

		try {
			SMTPMessage message = new SMTPMessage(session);

			if (!StringUtils.isBlank(envelopeFrom)) {
				message.setEnvelopeFrom(envelopeFrom);
			}
			if (!StringUtils.isBlank(from)) {
				try {
					message.setFrom(from);
				} catch (AddressException e) {
					logger.warn("Invalid from mail address {" + from + "}", e);
				}
			}

			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

			if (!StringUtils.isBlank(cc)) {
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
			}

			message.setSubject(subject, "UTF-8");

			if (!StringUtils.isEmpty(bodyPart) && !StringUtils.isEmpty(bodyText)) {
				BodyPart textPart = new MimeBodyPart();
				textPart.setText(bodyText);

				BodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(bodyPart, mimeType + "; charset=UTF-8");

				Multipart multipart = new MimeMultipart(textPart, htmlPart);

				message.setContent(multipart);
			} else if (!StringUtils.isEmpty(bodyPart)) {
				message.setContent(bodyPart, mimeType + "; charset=UTF-8");
			} else if (!StringUtils.isEmpty(bodyText)) {
				message.setText(bodyText, "UTF-8");
			}

			Transport.send(message);
		} catch (AddressException e) {
			NodeLogger.getLogger(MailSender.class).error("Error while sending mail - host[" + host + "] mailTo[" + to
					+ "] mailCC[" + cc + "] mailFrom[" + from + "] - AdressException: " + e);
			throw e;
		} catch (MessagingException e) {
			NodeLogger.getLogger(MailSender.class).error("Error while sending mail - host[" + host + "] mailTo[" + to
					+ "] mailCC[" + cc + "] mailFrom[" + from + "] - MessagingException: " + e);
			throw e;
		}
	}

	/**
	 * Sends an email-message
	 * @param host - Name of the mailserver that should send the mail.
	 * @param mailTo- Mailaddress of the recipient.
	 * @param mailCC - Mailaddress where to send a (carbon)copy to.
	 * @param mailFrom - Mailaddress of the sender of the email.
	 * @param subject - Subject of the email.
	 * @param body - Body (text) of the email.
	 * @throws TechnicalException if
	 *         <ul>
	 *         <li>host is not defined
	 *         <li>mailTo is not defined
	 *         <li>mailFrom is not defined
	 *         <li>one of the given mailaddresses is not valid
	 *         </ul>
	 */
	public static void send(String host, String mailTo, String mailCC, String mailFrom, String subject, String body) throws Exception {
		send(host, mailTo, mailCC, mailFrom, subject, body, "text/html");
	}

	/**
	 * Sends an email-message in plain format. Should behave very similar to the php mail() method.
	 * @param host - Name of the mailserver that should send the mail.
	 * @param mailTo- Mailaddress of the recipient.
	 * @param mailCC - Mailaddress where to send a (carbon)copy to.
	 * @param mailFrom - Mailaddress of the sender of the email.
	 * @param subject - Subject of the email.
	 * @param body - Body (text) of the email.
	 * @throws TechnicalException if
	 *         <ul>
	 *         <li>host is not defined
	 *         <li>mailTo is not defined
	 *         <li>mailFrom is not defined
	 *         <li>one of the given mailaddresses is not valid
	 *         </ul>
	 */
	public static void sendPlain(String host, String mailTo, String mailCC, String mailFrom, String subject, String body) throws NodeException, AddressException, MessagingException {
		sendPlain(host, mailTo, mailCC, mailFrom, subject, body, null);
	}

	/**
	 * Sends an email-message in plain format. Should behave very similar to the php mail() method.
	 * @param host - Name of the mailserver that should send the mail.
	 * @param mailTo- Mailaddress of the recipient.
	 * @param mailCC - Mailaddress where to send a (carbon)copy to.
	 * @param mailFrom - Mailaddress of the sender of the email.
	 * @param subject - Subject of the email.
	 * @param body - Body (text) of the email.
	 * @param returnPath - optional return-path address
	 * @throws TechnicalException if
	 *         <ul>
	 *         <li>host is not defined
	 *         <li>mailTo is not defined
	 *         <li>mailFrom is not defined
	 *         <li>one of the given mailaddresses is not valid
	 *         </ul>
	 */
	public static void sendPlain(String host, String mailTo, String mailCC, String mailFrom, String subject, String body, String returnPath) throws NodeException, AddressException, MessagingException {
		new MailSender().setHost(host).setTo(mailTo).setCc(mailCC).setFrom(mailFrom).setSubject(subject).setBodyText(body).setEnvelopeFrom(returnPath).send();
	}

	/**
	 * Sends an email-message
	 * @param host - Name of the mailserver that should send the mail.
	 * @param mailTo- Mailaddress of the recipient.
	 * @param mailCC - Mailaddress where to send a (carbon)copy to.
	 * @param mailFrom - Mailaddress of the sender of the email.
	 * @param subject - Subject of the email.
	 * @param body - Body (text) of the email.
	 * @param mimetype - the mimetye of the body part
	 * @throws TechnicalException if
	 *         <ul>
	 *         <li>host is not defined
	 *         <li>mailTo is not defined
	 *         <li>mailFrom is not defined
	 *         <li>one of the given mailaddresses is not valid
	 *         </ul>
	 */
	public static void send(String host, String mailTo, String mailCC, String mailFrom, String subject, String body, String mimetype) throws Exception {
		new MailSender().setHost(host).setTo(mailTo).setCc(mailCC).setFrom(mailFrom).setSubject(subject).setBodyPart(body, mimetype).send();
	}

	/**
	 * compose and send a multipart/alternative email message. <br>
	 * the message is composed such that mime compliant mail clients should be
	 * able to show either the html or (if rendering of html is not possible)
	 * the text part of the email message.
	 * @param host mail host to use for delivering the mail
	 * @param mailTo email address to send the mail to
	 * @param mailCC additional email address to send the mail to as carbon copy
	 * @param mailFrom email address to use in the "from" mail header
	 * @param subject subject of the mail
	 * @param bodyText text/plain part of the email message
	 * @param bodyHTML text/html part of the email message
	 */
	public static void sendTextAndHTMLMail(String host, String mailTo, String mailCC,
			String mailFrom, String subject, String bodyText, String bodyHTML) {
		try {
			new MailSender().setHost(host).setTo(mailTo).setCc(mailCC).setFrom(mailFrom).setSubject(subject).setBodyText(bodyText).setBodyHTML(bodyHTML).send();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * compose and send a mime-message with a single body part (text/html)
	 * @param host mail host to use for delivering the mail
	 * @param mailTo email address to send the mail to
	 * @param mailCC additional email address to send the mail to as carbon copy
	 * @param mailFrom email address to use in the "from" mail header
	 * @param subject subject of the mail
	 * @param body text/html part of the email message
	 * @throws Exception
	 */
	public static void sendSimpleMail(String host, String mailTo, String mailCC,
			String mailFrom, String subject, String body) throws Exception {
		new MailSender().setHost(host).setTo(mailTo).setCc(mailCC).setFrom(mailFrom).setSubject(subject).setBodyHTML(body).send();
	}

	/**
	 * static boolean isValidEMail( String s )<br>
	 * check if email adress is valid
	 * @param s
	 * @return
	 */
	public static boolean isValidEMail(String s) {

		s = s.trim();

		int at, dot, len = s.length();

		// s nicht angegeben (oder nur Whitespaces), oder kein @ bzw .

		if ((len == 0) || ((at = s.indexOf('@')) == -1) || ((dot = s.lastIndexOf('.')) == -1)) {
			return false;
		}

		// keine EMailadresse vor @ Zeichen oder . vor &

		if ((at == 0) || (dot < at)) {
			return false;
		}

		// Mindestens zwei Zeichen fuer die Endung

		return dot + 2 < len;
	}
}
