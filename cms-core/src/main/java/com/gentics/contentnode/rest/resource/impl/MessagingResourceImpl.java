/**
 * 
 */
package com.gentics.contentnode.rest.resource.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.request.MessageSendRequest;
import com.gentics.contentnode.rest.model.request.MessagesReadRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.MessagingResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Messaging resource to access the inbox (list, view, mark read, delete) and
 * send messages TODO: make ObjectFactory for messages
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/msg")
public class MessagingResourceImpl implements MessagingResource {
	@Override
	@DELETE
	@Path("/{id}")
	public Response delete(@PathParam("id") int id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Set<Integer> ids = DBUtils.select("SELECT id FROM msg WHERE id = ? AND to_user_id = ?", ps -> {
				ps.setInt(1, id);
				ps.setInt(2, t.getUserId());
			}, DBUtils.IDS);

			if (ids.isEmpty()) {
				throw new EntityNotFoundException(I18NHelper.get("msg.notfound", Integer.toString(id)));
			}

			DBUtils.update("DELETE FROM msg WHERE id = ? AND to_user_id = ?", new Object[] {id, t.getUserId()});
			trx.success();

			return Response.noContent().build();
		}
	}

	@Override
	@POST
	@Path("/send")
	public GenericResponse send(MessageSendRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<SystemUser> adresseeList = new Vector<SystemUser>();
			List<Integer> userIdList = request.getToUserId();
			List<Integer> groupIdList = request.getToGroupId();

			// add all users
			if (userIdList != null) {
				for (Integer userId : userIdList) {
					SystemUser adressee = t.getObject(SystemUser.class, userId);

					if (!adresseeList.contains(adressee)) {
						adresseeList.add(adressee);
					}
				}
			}

			// add members of the group
			if (groupIdList != null) {
				for (Integer groupId : groupIdList) {
					UserGroup group = t.getObject(UserGroup.class, groupId);
					List<SystemUser> members = group.getMembers();

					for (SystemUser adressee : members) {
						if (!adresseeList.contains(adressee)) {
							adresseeList.add(adressee);
						}
					}
				}
			}

			if (adresseeList.isEmpty()) {
				trx.success();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA, "No adressee found to send the message to"));
			} else {
				MessageSender messageSender = new MessageSender();

				for (SystemUser adressee : adresseeList) {
					try (LangTrx lTrx = new LangTrx(adressee)) {
						String msgKey = request.getMessage();
						if (!ObjectTransformer.isEmpty(request.getTranslations())) {
							msgKey = ObjectTransformer.getString(request.getTranslations().get(lTrx.getCode()), msgKey);
						}
						CNI18nString message = new CNI18nString(msgKey);
						if (!ObjectTransformer.isEmpty(request.getParameters())) {
							message.setParameters(request.getParameters());
						}
						// now compose the message and add to the message sender
						Message msg = new Message(t.getUserId(), ObjectTransformer.getInt(adressee.getId(), 0),
								message.toString(), request.getInstantTimeMinutes());

						messageSender.sendMessage(msg);
					}
				}

				// add the message sender to the transaction (messages will be
				// sent, when transaction is committed)
				t.addTransactional(messageSender);
				trx.success();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully sent messages"));
			}
		}
	}

	@Override
	@GET
	@Path("/list")
	public GenericResponse list(
			@QueryParam("unread") @DefaultValue("false") boolean unread) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			GenericResponse response = new GenericResponse();

			response.setMessages(DBUtils.select(unread ? "SELECT * FROM msg WHERE to_user_id = ? AND oldmsg = 0 ORDER BY timestamp DESC"
					: "SELECT * FROM msg WHERE to_user_id = ? ORDER BY timestamp DESC", ps -> {
						ps.setInt(1, t.getUserId());
					}, res -> {
						List<com.gentics.contentnode.rest.model.response.Message> list = new ArrayList<>();
						while (res.next()) {
							Message nodeMsg = new Message(res.getInt("from_user_id"), res.getInt("to_user_id"), res.getString("msg"));
							com.gentics.contentnode.rest.model.response.Message msg = new com.gentics.contentnode.rest.model.response.Message();
							msg.setId(res.getInt("id"));
							msg.setMessage(nodeMsg.getParsedMessage());
							msg.setSender(ModelBuilder.getUser(t.getObject(SystemUser.class, res.getInt("from_user_id"))));
							msg.setTimestamp(res.getLong("timestamp"));
							msg.setInstantMessage(!msg.isExpired(res.getInt("instanttime")));
							msg.setType(Type.INFO);
							list.add(msg);
						}
						return list;
					}));

			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully fetched messages"));
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/read")
	public GenericResponse read(MessagesReadRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			int updateCount = 0;

			// get the list of messages
			List<Integer> messages = request.getMessages();

			if (ObjectTransformer.isEmpty(messages)) {
				// nothing to do
				trx.success();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Nothing to do"));
			}

			StringBuffer sql = new StringBuffer("UPDATE msg SET oldmsg = 1 WHERE to_user_id = ? AND id IN (");

			sql.append(StringUtils.repeat("?", messages.size(), ","));
			sql.append(")");

			List<Object> params = new ArrayList<>();
			params.add(t.getUserId());
			params.addAll(messages);

			updateCount = DBUtils.update(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully set " + updateCount + " messages to 'read'"));
		}
	}
}
