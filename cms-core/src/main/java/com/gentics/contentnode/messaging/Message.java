/*
 * @author floriangutmann
 * @date Nov 18, 2009
 * @version $Id: Message.java,v 1.2 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.messaging;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Represents a message that can be sent by a MessageSender.
 * @author floriangutmann
 */
public class Message {

	/**
	 * Object type inbox message
	 */
	public static final int TYPE_INBOX_MESSAGE = 31;

	/**
	 * Pattern to find user placeholders in messages
	 */
	protected static final Pattern USER_PATTERN = Pattern.compile("<userid ([0-9]+)>");

	/**
	 * Pattern to find page placeholders in messages
	 */
	protected static final Pattern PAGE_PATTERN = Pattern.compile("<pageid ([0-9]+)>");

	/**
	 * Pattern to find language placeholders in messages
	 */
	protected static final Pattern LANGUAGE_PATTERN = Pattern.compile("<langid ([a-zA-Z0-9_]+)>");

	/**
	 * Pattern to find task placeholders in messages
	 */
	protected static final Pattern TASK_PATTERN = Pattern.compile("<task ([0-9]+)>");

	/**
	 * userId of the sender
	 */
	private int fromId;

	/**
	 * userId of the recipient
	 */
	private int toId;

	/**
	 * Text of the message
	 */
	private String message;
    
	/**
	 * Message is an instant message if:
	 * current timestamp + instantTime is in the future
	 */
	private int instantTime = 0;

	/**
	 * Time the message was created
	 */
	private int creationTime = 0;

	/**
	 * Default constructor with all neccessary fields.
	 * @param fromId id of the sender
	 * @param toId id of the recipient
	 * @param message message to send
	 */
	public Message(int fromId, int toId, String message) {
		super();
		this.fromId = fromId;
		this.toId = toId;
		this.message = message;
	}

	/**
	 * Default constructor with all neccessary fields.
	 * @param fromId id of the sender
	 * @param toId id of the recipient
	 * @param message message to send
	 * @param instantTime specifies how many seconds from now the message should be delivered as an instant message
	 */
	public Message(int fromId, int toId, String message, int instantTime) {
		super();
		this.fromId = fromId;
		this.toId = toId;
		this.message = message;
		this.instantTime = instantTime;
	}
    
	public int getFromId() {
		return fromId;
	}

	public void setFromId(int fromId) {
		this.fromId = fromId;
	}

	public int getToId() {
		return toId;
	}

	public void setToId(int toId) {
		this.toId = toId;
	}

	public String getMessage() {
		return message;
	}

	public String getParsedMessage() throws NodeException {
		if (!ObjectTransformer.isEmpty(message)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean multichannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
			String msg = message;

			// parse <userid [id]>
			msg = parse(msg, USER_PATTERN, (matcher, parsedMessage) -> {
				SystemUser user = t.getObject(SystemUser.class, ObjectTransformer.getInteger(matcher.group(1), null));

				if (user != null) {
					parsedMessage.append(user.getLastname()).append(" ").append(user.getFirstname());
				} else {
					parsedMessage.append("--");
				}

			});

			// parse <pageid [id]>
			msg = parse(msg, PAGE_PATTERN, (matcher, parsedMessage) -> {
				Page page = t.getObject(Page.class, ObjectTransformer.getInteger(matcher.group(1), null));

				if (page != null) {
					Node channel = null;

					if (multichannelling) {
						channel = page.getChannel();
						if (channel != null) {
							t.setChannelId(channel.getId());
						}
					}
					try {
						com.gentics.contentnode.rest.model.Page restPage = ModelBuilder.getPage(page, Collections.singleton(Reference.FOLDER));
						String path = ObjectTransformer.getString(restPage.getPath(), "");

						if (path.startsWith("/")) {
							parsedMessage.append(path.substring(1));
						} else {
							parsedMessage.append(path);
						}
						parsedMessage.append(page.getName()).append(" (").append(page.getId()).append(")");
					} finally {
						if (channel != null) {
							t.resetChannel();
						}
					}
				} else {
					parsedMessage.append("--");
				}
			});

			// parse <langid [id|code]>
			IntegerColumnRetriever languageIds = new IntegerColumnRetriever("id");
			DBUtils.executeStatement("SELECT id FROM contentgroup", languageIds);
			List<ContentLanguage> languages = t.getObjects(ContentLanguage.class, languageIds.getValues());
			msg = parse(msg, LANGUAGE_PATTERN, (matcher, parsedMessage) -> {
				String langId = matcher.group(1);
				Optional<ContentLanguage> matchingLanguage = languages.stream().filter(l -> langId.equals(l.getId()) || langId.equals(l.getCode())).findFirst();
				if (matchingLanguage.isPresent()) {
					parsedMessage.append(matchingLanguage.get().getName());
				} else {
					parsedMessage.append("--");
				}
			});

			// parse <task [id]>
			msg = parse(msg, TASK_PATTERN, (matcher, parsedMessage) -> {
			});

			return msg;
		} else {
			return message;
		}
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getInstantTime() {
		return instantTime;
	}

	public void setInstantTime(int instantTime) {
		this.instantTime = instantTime;
	}

	public int getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(int creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * Parse the message with the given pattern and use the replace to replace matches
	 * @param msg message to parse
	 * @param pattern pattern to match
	 * @param replacer replacer
	 * @return parsed and replaced message
	 * @throws NodeException
	 */
	protected String parse(String msg, Pattern pattern, Replacer replacer) throws NodeException {
		StringBuilder parsedMessage = new StringBuilder();
		int pos = 0;
		Matcher matcher = pattern.matcher(msg);

		while (matcher.find(pos)) {
			if (matcher.start() > pos) {
				parsedMessage.append(msg.substring(pos, matcher.start()));
			}
			replacer.append(matcher, parsedMessage);

			pos = matcher.end();
		}
		if (pos < msg.length()) {
			parsedMessage.append(msg.substring(pos));
		}
		return parsedMessage.toString();
	}

	/**
	 * Interface for a replacer
	 */
	@FunctionalInterface
	protected static interface Replacer {
		/**
		 * Append the replacement for the match
		 * @param matcher matcher
		 * @param builder
		 * @throws NodeException
		 */
		void append(Matcher matcher, StringBuilder builder) throws NodeException;
	}
}
