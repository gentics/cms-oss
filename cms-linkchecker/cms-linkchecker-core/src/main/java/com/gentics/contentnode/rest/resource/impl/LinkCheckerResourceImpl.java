package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.load;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.require;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeSetup;
import com.gentics.contentnode.etc.NodeSetupValuePair;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.page.linkchecker.LinkChecker;
import com.gentics.contentnode.object.utility.PageComparator;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.model.linkchecker.CheckExternalLinkRequest;
import com.gentics.contentnode.rest.model.linkchecker.CheckResponse;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLink;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkList;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkResponse;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatistics;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatus;
import com.gentics.contentnode.rest.model.linkchecker.PageList;
import com.gentics.contentnode.rest.model.linkchecker.PageWithExternalLinks;
import com.gentics.contentnode.rest.model.linkchecker.ReplaceExternalLinkRequest;
import com.gentics.contentnode.rest.model.linkchecker.UpdateExternalLinkStatusRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.LinkCheckerResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LinkCheckerFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.NodeObjectFilter;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.PermissionFilter;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Implementation of {@link LinkCheckerResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Authenticated
@RequiredFeature(Feature.LINK_CHECKER)
@Path("/linkChecker")
public class LinkCheckerResourceImpl implements LinkCheckerResource {
	@Override
	@GET
	@Path("/stats")
	public ExternalLinkStatistics getStats(@QueryParam("nodeId") Integer nodeId, @QueryParam("online") Boolean online) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Node node = null;
			Node channel = null;
			if (nodeId != null) {
				node = load(Node.class, Integer.toString(nodeId));
				if (node.isChannel()) {
					channel = node;
					node = channel.getMaster();
				}
				require(Feature.LINK_CHECKER, node);
			}

			StringBuilder sql = new StringBuilder("SELECT page.folder_id, external_link.last_status, count(*) c FROM external_link, page, folder");
			sql.append(" WHERE external_link.content_id = page.content_id AND page.deleted = 0 AND page.folder_id = folder.id");

			// filter by node/channel
			if (node != null) {
				sql.append(" AND folder.node_id = ? AND page.channel_id = ?");
			}

			// filter by online status
			if (online != null) {
				sql.append(" AND page.online = ?");
			}

			sql.append(" GROUP BY page.folder_id, external_link.last_status");

			final Node finalNode = node;
			final Node finalChannel = channel;
			List<Triple<Integer, ExternalLinkStatus, Integer>> counts = DBUtils.select(sql.toString(), stmt -> {
				int index = 1;
				if (finalNode != null) {
					stmt.setInt(index++, finalNode.getId()); // folder.node_id = 
					stmt.setInt(index++, finalChannel != null ? finalChannel.getId() : 0); // page.channel_id = ?
				}
				if (online != null) {
					stmt.setBoolean(index++, online.booleanValue()); // page.online = ?
				}
			}, rs -> {
				List<Triple<Integer, ExternalLinkStatus, Integer>> result = new ArrayList<>();
				while (rs.next()) {
					result.add(Triple.of(rs.getInt("folder_id"), ExternalLinkStatus.getForCode(rs.getInt("last_status")), rs.getInt("c")));
				}
				return result;
			});

			// filter by permission and accumulate
			int valid = 0;
			int invalid = 0;
			int unchecked = 0;
			for (Triple<Integer, ExternalLinkStatus, Integer> triple : counts) {
				int folderId = triple.getLeft();
				ExternalLinkStatus status = triple.getMiddle();
				int count = triple.getRight();

				// filter folder permission
				Folder folder = t.getObject(Folder.class, folderId);
				if (!t.canView(folder, Page.class, null)) {
					continue;
				}
				switch (status) {
				case valid:
					valid += count;
					break;
				case invalid:
					invalid += count;
					break;
				case unchecked:
					unchecked += count;
					break;
				}
			}

			ExternalLinkStatistics response = new ExternalLinkStatistics().setValid(valid).setInvalid(invalid).setUnchecked(unchecked);
			NodeSetupValuePair linkcheckerInfo = NodeSetup.getKeyValue(LinkChecker.NODESETUP_KEY);
			if (linkcheckerInfo != null) {
				response.setLastRun(linkcheckerInfo.getIntValue());
			}
			response.setResponseInfo(ResponseInfo.ok(""));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/pages")
	public PageList getPages(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam LinkCheckerFilterParameterBean linkFilter, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			boolean filterLastStatus = linkFilter != null && !ObjectTransformer.isEmpty(linkFilter.status);
			boolean filterNode = linkFilter != null && linkFilter.nodeId != null;
			boolean filterEditable = linkFilter != null && linkFilter.editable;
			boolean filterCreator = linkFilter != null && linkFilter.creator;
			boolean filterEditor = linkFilter != null && linkFilter.editor;
			boolean filterLanguage = linkFilter != null && !ObjectTransformer.isEmpty(linkFilter.language);
			boolean filterOnline = linkFilter != null && linkFilter.online != null;
			boolean search = filter != null && !ObjectTransformer.isEmpty(filter.query);
			Set<Integer> filteredContentGroupIds = new HashSet<>();

			Node node = null;
			Node channel = null;
			if (filterNode) {
				node = load(Node.class, Integer.toString(linkFilter.nodeId));
				if (node.isChannel()) {
					channel = node;
					node = channel.getMaster();
				}
				require(Feature.LINK_CHECKER, node);
			}

			StringBuilder sql = new StringBuilder("SELECT DISTINCT page.id FROM external_link, page");
			if (filterNode) {
				sql.append(", folder");
			}
			sql.append(" WHERE external_link.content_id = page.content_id");

			// filter by node/channel
			if (filterNode) {
				sql.append(" AND page.folder_id = folder.id AND folder.node_id = ? AND page.channel_id = ?");
			}

			// filter by external link status
			if (filterLastStatus) {
				sql.append(" AND last_status IN (");
				sql.append(StringUtils.repeat("?", ",", linkFilter.status.size()));
				sql.append(")");
			}

			// filter for pages created by current user
			if (filterCreator) {
				sql.append(" AND page.creator = ?");
			}

			// filter for pages edited by current user
			if (filterEditor) {
				sql.append(" AND page.editor = ?");
			}

			// filter for pages in specific languages
			if (filterLanguage) {
				Set<Integer> contentGroupIds = DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS);
				filteredContentGroupIds.addAll(trx.getTransaction().getObjects(ContentLanguage.class, contentGroupIds).stream().filter(lang -> {
					return linkFilter.language.contains(lang.getId().toString()) || linkFilter.language.contains(lang.getCode());
				}).map(ContentLanguage::getId).collect(Collectors.toSet()));

				// if the filter contained "0", we add this to the IDs (this will filter for pages without language)
				if (linkFilter.language.contains("0")) {
					filteredContentGroupIds.add(0);
				}

				// if no language matched the given codes/ids, we add -1 to make the sql statement valid (but returning no pages)
				if (filteredContentGroupIds.isEmpty()) {
					filteredContentGroupIds.add(-1);
				}

				sql.append(" AND contentgroup_id IN (");
				sql.append(StringUtils.repeat("?", ",", filteredContentGroupIds.size()));
				sql.append(")");
			}

			// filter for online status
			if (filterOnline) {
				sql.append(" AND page.online = ?");
			}

			// add search clause
			if (search) {
				sql.append(" AND (");
				sql.append("page.id = ?");
				sql.append(" OR LOWER(page.name) LIKE ?");
				sql.append(" OR LOWER(page.filename) LIKE ?");
				sql.append(" OR LOWER(page.description) LIKE ?");
				sql.append(" OR LOWER(page.nice_url) LIKE ?");
				sql.append(" OR LOWER(external_link.url) LIKE ?");
				sql.append(" OR LOWER(external_link.text) LIKE ?");
				sql.append(")");
			}

			final Node finalNode = node;
			final Node finalChannel = channel;
			Set<Integer> pageIds = DBUtils.select(sql.toString(), stmt -> {
				int index = 1;
				if (filterNode) {
					stmt.setInt(index++, finalNode.getId());
					stmt.setInt(index++, finalChannel != null ? finalChannel.getId() : 0);
				}
				if (filterLastStatus) {
					for (ExternalLinkStatus status : linkFilter.status) {
						stmt.setInt(index++, status.getCode());
					}
				}
				if (filterCreator) {
					stmt.setInt(index++, trx.getTransaction().getUserId());
				}
				if (filterEditor) {
					stmt.setInt(index++, trx.getTransaction().getUserId());
				}
				if (filterLanguage) {
					for (Integer id : filteredContentGroupIds) {
						stmt.setInt(index++, id);
					}
				}
				if (filterOnline) {
					stmt.setBoolean(index++, linkFilter.online.booleanValue());
				}

				if (search) {
					String patternLike = "%" + filter.query + "%";
					stmt.setObject(index++, filter.query); // page.id
					stmt.setObject(index++, patternLike);  // page.name
					stmt.setObject(index++, patternLike);  // page.filename
					stmt.setObject(index++, patternLike);  // page.description
					stmt.setObject(index++, patternLike);  // page.nice_url
					stmt.setObject(index++, patternLike);  // external_link.url
					stmt.setObject(index++, patternLike);  // external_link.text
				}
			}, DBUtils.IDS);

			List<Page> pages = trx.getTransaction().getObjects(Page.class, pageIds);
			NodeObjectFilter permFilter = PermissionFilter.get(filterEditable ? ObjectPermission.edit : ObjectPermission.view);
			NodeObjectFilter editableFilter = PermissionFilter.get(ObjectPermission.edit);

			PageList response = ListBuilder.from(pages, p -> {
				Collection<ExternalLink> links = LinkChecker.load(p.getContent().getId());
				if (linkFilter != null && !ObjectTransformer.isEmpty(linkFilter.status)) {
					links = links.stream().filter(link -> linkFilter.status.contains(link.getLastStatus())).collect(Collectors.toList());
				}
				LinkChecker.attachStatusHistory(links);
				return new PageWithExternalLinks().setPage(ModelBuilder.getPage(p)).setLinks(links).setEditable(editableFilter.matches(p));
			})
					.filter(o -> permFilter.matches(o))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete, ObjectPermission.publish))
					.sort(PageComparator.get(sorting))
					.page(paging)
					.to(new PageList());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/pages/{id}")
	public ExternalLinkList getLinks(@PathParam("id") String pageId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Page page = load(Page.class, pageId);
			require(Feature.LINK_CHECKER, page.getOwningNode());

			Collection<ExternalLink> links = LinkChecker.load(page.getContent().getId());
			LinkChecker.attachStatusHistory(links);
			ExternalLinkList response = ListBuilder.from(links, o -> o).to(new ExternalLinkList());
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/pages/{id}")
	public GenericResponse updateLinkStatus(@PathParam("id") String pageId, UpdateExternalLinkStatusRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Page page = load(Page.class, pageId, ObjectPermission.edit);
			require(Feature.LINK_CHECKER, page.getOwningNode());

			if (request == null || ObjectTransformer.isEmpty(request.getItems())) {
				throw new MissingFieldException("items");
			}

			Set<Integer> idSet = LinkChecker.load(page.getContent().getId()).stream().map(ExternalLink::getId).collect(Collectors.toSet());
			List<ExternalLink> updated = request.getItems().stream().filter(link -> idSet.contains(link.getId())
					&& (link.getLastStatus() == ExternalLinkStatus.valid || link.getLastStatus() == ExternalLinkStatus.invalid)).collect(Collectors.toList());
			LinkChecker.updateStatus(updated);

			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully updated external link status"));
		}
	}

	@Override
	@POST
	@Path("/check")
	public CheckResponse checkLink(CheckExternalLinkRequest request) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			require(Feature.LINK_CHECKER);

			if (request == null || StringUtils.isEmpty(request.getUrl())) {
				throw new MissingFieldException("url");
			}

			CheckResponse response = new CheckResponse();
			if (LinkChecker.isChecked(request.getUrl())) {
				Pair<ExternalLinkStatus, String> result = LinkChecker.check(request.getUrl());
				Boolean valid = null;
				switch (result.getLeft()) {
				case invalid:
					valid = false;
					break;
				case valid:
					valid = true;
					break;
				case unchecked:
				default:
					break;
				}
				response.setValid(valid).setReason(result.getRight());
			}
			response.setResponseInfo(ResponseInfo.ok(""));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/pages/{pageId}/links/{id}")
	public ExternalLinkResponse getLink(@PathParam("pageId") String pageId, @PathParam("id") int linkId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			// load page and check view permission
			Page page = load(Page.class, pageId);
			require(Feature.LINK_CHECKER);
			ExternalLink link = LinkChecker.load(page.getContent().getId(), linkId);
			if (link == null) {
				throw new EntityNotFoundException(I18NHelper.get("linkchecker.link.notfound", Integer.toString(linkId)));
			}

			// collect occurrence information
			int nodeId = page.getNode().getId();
			Node channel = page.getChannel();
			if (channel != null) {
				nodeId = channel.getId();
			}
			Triple<Integer,Integer,Integer> occurrences = LinkChecker.findOccurrences(link.getUrl(), page.getId(), nodeId);

			ExternalLinkResponse response = new ExternalLinkResponse();
			response.setResponseInfo(ResponseInfo.ok(""));
			response.setLink(link);
			response.setPage(occurrences.getLeft());
			response.setNode(occurrences.getMiddle());
			response.setGlobal(occurrences.getRight());

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/pages/{pageId}/links/{id}")
	public GenericResponse updateLink(@PathParam("pageId") String pageId, @PathParam("id") int linkId,
			@QueryParam("wait") @DefaultValue("0") long waitMs, ReplaceExternalLinkRequest request)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			GenericResponse resp = Operator.executeRethrowing(I18NHelper.get("linkchecker.link.update"), waitMs, () -> {
				// load page and check view and edit permission
				Page page = load(Page.class, pageId, ObjectPermission.edit);
				require(Feature.LINK_CHECKER);
				ExternalLink link = LinkChecker.load(page.getContent().getId(), linkId);
				if (link == null) {
					throw new EntityNotFoundException(I18NHelper.get("linkchecker.link.notfound", Integer.toString(linkId)));
				}

				int nodeId = page.getNode().getId();
				if (page.getChannel() != null) {
					nodeId = page.getChannel().getId();
				}
				UpdatePagesResult opResult = LinkChecker.updateOccurrences(link.getUrl(), link.getId(), request.getUrl(), page.getId(), nodeId, request.getScope());
				GenericResponse response = new GenericResponse();

				StringBuilder msg = new StringBuilder();
				if (opResult.getNumPagesUpdated() == 1) {
					msg.append(I18NHelper.get("linkchecker.link.replace.changed.singular", Integer.toString(opResult.getNumPagesUpdated())));
				} else {
					msg.append(I18NHelper.get("linkchecker.link.replace.changed", Integer.toString(opResult.getNumPagesUpdated())));
				}
				if (opResult.getNoPermission() > 0) {
					if (opResult.getNoPermission() == 1) {
						msg.append("\n").append(I18NHelper.get("linkchecker.link.replace.nopermission.singular", Integer.toString(opResult.getNoPermission())));
					} else {
						msg.append("\n").append(I18NHelper.get("linkchecker.link.replace.nopermission", Integer.toString(opResult.getNoPermission())));
					}
				}
				if (opResult.getLocked() > 0) {
					if (opResult.getLocked() == 1) {
						msg.append("\n").append(I18NHelper.get("linkchecker.link.replace.locked.singular", Integer.toString(opResult.getLocked())));
					} else {
						msg.append("\n").append(I18NHelper.get("linkchecker.link.replace.locked", Integer.toString(opResult.getLocked())));
					}
				}
				response.addMessage(new Message(Message.Type.INFO, msg.toString()));

				if (opResult.isOK()) {
					response.setResponseInfo(ResponseInfo.ok(""));
				} else {
					response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, ""));
				}

				ModelBuilder.addMessagesFromOpResult(opResult, response);
				return response;
			}, Function.identity());

			trx.success();
			return resp;
		}
	}
}
