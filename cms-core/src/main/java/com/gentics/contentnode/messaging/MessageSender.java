/*

 * @author floriangutmann
 * @date Nov 18, 2009
 * @version $Id: MessageSender.java,v 1.5 2010-10-19 11:19:48 norbert Exp $
 */
package com.gentics.contentnode.messaging;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.AbstractTransactional;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Transactional;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.mail.MailSender;
import org.apache.commons.lang3.tuple.Pair;

import jakarta.mail.MessagingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * The MessageSender sends messages in a transactional way.
 * It has to be registered at a Transaction ({@link Transaction#addTransactional(Transactional)}) otherwise it will never send the messages.
 * When committing the transaction the messages are written to the database and mails are sent.
 *
 * @author floriangutmann
 */
public class MessageSender extends AbstractTransactional {

	public static final String MAILHOST_PARAM = "mailhost";
	public static final String MAILPORT_PARAM = "mailport";
	public static final String MAILRETURNPATH_PARAM = "mailreturnpath";
	public static final String MAILSTARTTLS_PARAM = "mailstarttls";
	public static final String MAILUSERNAME_PARAM = "mailusername";
	public static final String MAILPASSWORD_PARAM = "mailpassword";

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(MessageSender.class);

	/**
	 * Collection of messages that must be sent
	 */
	private Collection<Message> messages = new Vector<Message>();

	/**
	 * Feature inbox_to_email
	 */
	private boolean inboxToEmail = false;

	/**
	 * Feature inbox_to_email_optional
	 */
	private boolean inboxToEmailOpt = false;

	/**
	 * Last insertId
	 */
	private int lastInsertId;

	/**
	 * Stores all user (recipient and sender) information.
	 * int userId => User user
	 */
	private HashMap<Integer, SystemUser> users = new HashMap<Integer, SystemUser>();

	/**
	 * Sends a message to
	 * @param message
	 */
	public void sendMessage(Message message) {
		messages.add(message);
	}

	/**
	 * Inserts the messages in the database.
	 * @see Transactional#onDBCommit(Transaction)
	 */
	public void onDBCommit(final Transaction t) throws NodeException {
		if (messages.isEmpty()) {
			return;
		}

		// Insert messages
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		String sql = "INSERT INTO msg (to_user_id, from_user_id, msg, oldmsg, timestamp, instanttime) VALUES (?, ?, ?, ?, ?, ?)";

		for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
			final Message message = it.next();

			DBUtils.executeStatement(sql, new SQLExecutor() {
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, message.getToId());
					stmt.setInt(2, message.getFromId());
					stmt.setString(3, message.getMessage());
					stmt.setInt(4, 0);
					stmt.setInt(5, t.getUnixTimestamp());
					stmt.setInt(6, message.getInstantTime());
				}

				public void handleStatment(PreparedStatement stmt) throws SQLException, NodeException {
					ResultSet rs = stmt.getGeneratedKeys();

					if (!rs.next()) {
						throw new NodeException("Could not get messageId of inserted message");
					}
					lastInsertId = rs.getInt(1);
				}
			}, Transaction.INSERT_STATEMENT);

			if (prefs.getFeature("log_inbox_create")) {
				ActionLogger.logCmd(ActionLogger.INBOXCREATE, Message.TYPE_INBOX_MESSAGE, new Integer(lastInsertId), null, "MessageSender.onDBCommit()");
			}
		}

		// Fetch user information for all messages
		inboxToEmailOpt = prefs.getFeature("inbox_to_email_optional");
		inboxToEmail = prefs.getFeature("inbox_to_email");

		if (inboxToEmailOpt || inboxToEmail) {
			HashSet<Integer> userIds = new HashSet<Integer>();

			for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
				Message message = it.next();

				userIds.add(message.getToId());
				userIds.add(message.getFromId());
			}
			List<SystemUser> systemUsers = t.getObjects(SystemUser.class, userIds);

			for (Iterator<SystemUser> it = systemUsers.iterator(); it.hasNext();) {
				SystemUser user = it.next();

				users.put(ObjectTransformer.getInteger(user.getId(), null), user);
			}
		}
	}

	/**
	 * Sends the mails after the database transaction was committed successfully.<br />
	 * Be aware that the database connection has already been committed at the time this function is called.
	 * @see Transactional#onTransactionCommit(Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		if (messages.isEmpty()) {
			return false;
		}

		if (inboxToEmailOpt || inboxToEmail) {
			String subject = new CNI18nString("message_from").toString();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
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

			String signature = new CNI18nString("mailsignature").toString();

			for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
				Message message = it.next();
				SystemUser toUser = (SystemUser) users.get(new Integer(message.getToId()));
				SystemUser fromUser = (SystemUser) users.get(new Integer(message.getFromId()));

				if (toUser.getActive() == 0) {
					// Don't send e-mails to deleted users
					continue;
				}

				if ((inboxToEmail || toUser.isInboxToEmail()) && !StringUtils.isEmpty(toUser.getEmail())) {
					Pair<String, String> fromUserInfo = getFromUserInfo(fromUser, returnPath);

					if (fromUserInfo == null) {
						logger.error("Cannot send email because no from address could be determined. Did you configure a return path?");
						continue;
					}

					try {
						MailSender sender = new MailSender()
							.setHost(mailhost)
							.setTo(toUser.getEmail())
							.setSubject(subject + " " + fromUserInfo.getRight())
							.setBodyHTML(message.getParsedMessage() + "\n--\n" + signature)
							.setEnvelopeFrom(returnPath);

						if (mailPort > 0) {
							sender.setPort(mailPort);
						}
						if (!StringUtils.isEmpty(fromUserInfo.getLeft())) {
							sender.setFrom(fromUserInfo.getLeft());
						}
						if (!StringUtils.isEmpty(mailStartTls)) {
							sender.setStartTLS(Boolean.parseBoolean(mailStartTls));
						}
						if (!StringUtils.isEmpty(mailUsername)) {
							sender.setAuth(mailUsername, mailPassword);
						}
						sender.send();
					} catch (MessagingException me) {
						try {
							Transaction currentTransaction = TransactionManager.getCurrentTransactionOrNull();

							// Get a new (uncommitted) transaction and log the error
							ContentNodeFactory factory = ContentNodeFactory.getInstance();
							Transaction logTransaction = factory.startTransaction(true);

							Object[] args = new Object[5];

							args[0] = t.getSessionId();
							if (args[0] == null) {
								args[0] = "";
							}
							args[1] = new Integer(t.getUserId());
							args[2] = new Integer(42);
							args[3] = "";
							args[4] = me.getMessage();

							DBUtils.executeUpdate(
									"INSERT INTO logerror (sid, user_id, halt_id, request, timestamp, detail) VALUES (?, ?, ?, ?, unix_timestamp(), ?)", args);

							logTransaction.commit();

							// Restore old current transaction
							TransactionManager.setCurrentTransaction(currentTransaction);
						} catch (Exception e) {
							logger.error("Could not log mail send error", e);
						}
					} catch (Exception e) {
						logger.error("Could not send inbox mail to " + toUser.getEmail(), e);
					}
				}
			}
		}

		return false;
	}

	/**
	 * Get full name and email address for the <em>from</em> field of the email.
	 *
	 * <p>
	 *     The name (left field of the result) will be determined by
	 *     <ul>
	 *         <li>the last- and firstname of the user object</li>
	 *         <li>the email address of the user object</li>
	 *         <li>the return path email address</li>
	 *     </ul>
	 *     whichever gives a non-empty result first.
	 * </p>
	 *
	 * <p>
	 *     The email address (right field of the result) will be determined by
	 *     <ul>
	 *         <li>the user objects email address</li>
	 *         <li>the return path address</li>
	 *     </ul>
	 *     whichever gives a non-empty result first.
	 * </p>
	 *
	 * <p>
	 *     <strong>Important:</strong> When neither the users email nor the
	 *     return path are available, we cannot send a mail at all, and
	 *     therefore this method will return {@code null}.
	 * </p>
	 *
	 * @param fromUser The system user sending the message (may be {@code null}
	 * 		or empty)
	 * @param returnPath The email return path (may be {@code null} or empty)
	 * @return A {@code Pair} where the left part is the determined email
	 * 		address and the right part is the full name to use in the
	 * 		<em>From</em> field
	 */
	private Pair<String, String> getFromUserInfo(SystemUser fromUser, String returnPath) {
		boolean returnPathMissing = StringUtils.isEmpty(returnPath);

		if (fromUser == null) {
			if (returnPathMissing) {
				// No user, and no return path. Cannot determine from address.
				return null;
			}

			return Pair.of(returnPath, returnPath);
		}

		String email = fromUser.getEmail();

		if (StringUtils.isEmpty(email)) {
			if (returnPathMissing) {
				// No user email and no return path. Cannot determine from address.
				return null;
			}

			// Fallback to return path as from address.
			email = returnPath;
		}

		String name = fromUser.getLastname() + " " + fromUser.getFirstname();

		if (name.trim().isEmpty()) {
			// Fallback to email address as name.
			name = email;
		}

		return Pair.of(email, name);
	}
}
