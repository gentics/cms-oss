package com.gentics.contentnode.task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskCreator;
import com.gentics.contentnode.rest.model.task.TaskObject;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

/**
 * Virtual tasks for the pages in the publish queue
 */
public class PublishQueueTaskProvider implements TaskProviderService {
	public final static String TYPE_APPROVE_PUBLISH = "approvePublish";

	public final static String TYPE_APPROVE_OFFLINE = "approveOffline";

	@Override
	public TaskSource source() {
		return TaskSource.publishQueue;
	}

	@Override
	public List<ResolvableUserTaskModel> tasks() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ResolvableUserTaskModel> tasks = new ArrayList<>();
		LegacyPageListResponse queue = new PageResourceImpl().pubqueue(0, -1, null, "name", "asc");
		for (com.gentics.contentnode.rest.model.Page queued : queue.getPages()) {
			Page page = t.getObject(Page.class, queued.getId());
			if (page == null) {
				continue;
			}
			try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
				tasks.add(toTask(t, page));
			}
		}
		return tasks;
	}

	private ResolvableUserTaskModel toTask(Transaction t, Page page) throws NodeException {
		boolean offline = page.getOffQueueUser() != null;
		SystemUser requestedBy = offline ? page.getOffQueueUser() : page.getPubQueueUser();
		int at = offline ? page.getTimeOffQueue().getIntTimestamp() : page.getTimePubQueue().getIntTimestamp();
		Node node = page.getChannel() != null ? page.getChannel() : page.getOwningNode();

		Map<String, Object> details = new LinkedHashMap<>();
		details.put("action", offline ? "offline" : "publish");
		// 0 means "as soon as approved"
		details.put("at", at > 0 ? at : null);
		if (requestedBy != null) {
			Map<String, Object> user = new LinkedHashMap<>();
			user.put("id", requestedBy.getId());
			user.put("name", requestedBy.getFullName());
			details.put("requestedBy", user);
		}
		if (!offline) {
			NodeObjectVersion version = page.getTimePubVersionQueue();
			if (version != null) {
				details.put("version", version.getNumber());
			}
		}

		List<TaskAction> actions = new ArrayList<>(List.of(TaskAction.open, TaskAction.approve));
		// handing back requires the edit permission (POST /page/assign)
		if (t.canEdit(page)) {
			actions.add(TaskAction.assignBack);
		}
		actions.add(TaskAction.startSession);

		ResolvableUserTaskModel task = new ResolvableUserTaskModel();
		task.setId("%s:page:%d".formatted(TaskSource.publishQueue.name(), page.getId()))
			.setSource(TaskSource.publishQueue)
			.setType(offline ? TYPE_APPROVE_OFFLINE : TYPE_APPROVE_PUBLISH)
			.setStatus(TaskStatus.open)
			.setDueAt(at > 0 ? at : null)
			.setCreator(new TaskCreator().setType(TaskCreator.Type.system).setName("publishQueue"))
			.setObjects(List.of(new TaskObject().setType("page").setId(page.getId())
					.setNodeId(node != null ? node.getId() : null).setName(page.getName())))
			.setDetails(details)
			.setActions(actions);
		return task;
	}
}
