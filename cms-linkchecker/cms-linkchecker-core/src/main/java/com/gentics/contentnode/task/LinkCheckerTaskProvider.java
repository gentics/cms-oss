package com.gentics.contentnode.task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;
import com.gentics.contentnode.rest.model.linkchecker.PageWithExternalLinks;
import com.gentics.contentnode.rest.model.task.TaskAction;
import com.gentics.contentnode.rest.model.task.TaskCreator;
import com.gentics.contentnode.rest.model.task.TaskObject;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.resource.impl.LinkCheckerResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.LinkCheckerFilterParameterBean;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Virtual tasks for pages with broken links
 */
public class LinkCheckerTaskProvider implements TaskProviderService {
	public final static String TYPE_FIX_BROKEN_LINKS = "fixBrokenLinks";

	@Override
	public TaskSource source() {
		return TaskSource.linkChecker;
	}

	@Override
	public List<ResolvableUserTaskModel> tasks() throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.LINK_CHECKER)) {
			return List.of();
		}

		// the resource joins "iscreator" and "iseditor" with AND, the task list wants pages the user created OR last edited
		Map<Integer, PageWithExternalLinks> pages = new LinkedHashMap<>();
		for (boolean creator : new boolean[] { true, false }) {
			LinkCheckerFilterParameterBean filter = new LinkCheckerFilterParameterBean();
			filter.status = Set.of(ExternalLinkStatus.invalid);
			filter.editable = true;
			filter.creator = creator;
			filter.editor = !creator;
			for (PageWithExternalLinks page : new LinkCheckerResourceImpl().getPages(null, null, null, filter, null).getItems()) {
				pages.putIfAbsent(page.getPage().getId(), page);
			}
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		List<ResolvableUserTaskModel> tasks = new ArrayList<>();
		for (PageWithExternalLinks entry : pages.values()) {
			Page page = t.getObject(Page.class, entry.getPage().getId());
			if (page == null) {
				continue;
			}
			try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
				Node node = page.getChannel() != null ? page.getChannel() : page.getOwningNode();
				if (node == null || !NodeConfigRuntimeConfiguration.isFeature(Feature.LINK_CHECKER, node)) {
					continue;
				}
				tasks.add(toTask(entry, node));
			}
		}
		return tasks;
	}

	private ResolvableUserTaskModel toTask(PageWithExternalLinks entry, Node node) throws NodeException {
		List<Map<String, Object>> links = new ArrayList<>();
		for (ExternalLink link : entry.getLinks()) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("id", link.getId());
			data.put("url", link.getUrl());
			data.put("lastCheck", link.getLastCheckTimestamp() > 0 ? link.getLastCheckTimestamp() : null);
			links.add(data);
		}

		boolean online = entry.getPage().isOnline();
		String liveUrl = entry.getPage().getLiveUrl();
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("brokenCount", links.size());
		details.put("links", links);
		details.put("online", online);
		details.put("liveUrl", liveUrl);

		List<TaskAction> actions = new ArrayList<>(List.of(TaskAction.startSession));
		if (online && !StringUtils.isEmpty(liveUrl)) {
			actions.add(TaskAction.view);
		}

		int pageId = entry.getPage().getId();
		ResolvableUserTaskModel task = new ResolvableUserTaskModel();
		task.setId("%s:page:%d".formatted(TaskSource.linkChecker.name(), pageId))
			.setSource(TaskSource.linkChecker)
			.setType(TYPE_FIX_BROKEN_LINKS)
			.setStatus(TaskStatus.open)
			.setCreator(new TaskCreator().setType(TaskCreator.Type.system).setName("linkChecker"))
			.setObjects(List.of(new TaskObject().setType("page").setId(pageId).setNodeId(node.getId())
					.setName(entry.getPage().getName())))
			.setDetails(details)
			.setActions(actions);
		return task;
	}
}
