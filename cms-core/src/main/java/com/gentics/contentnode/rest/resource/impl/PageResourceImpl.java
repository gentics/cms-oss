/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: PageResource.java,v 1.16.2.1.2.2.2.21 2011-03-29 15:33:37 norbert Exp $
 */
package com.gentics.contentnode.rest.resource.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.HandleSelectResultSet;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionLockManager;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.ReturnValueExecutable;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.job.MultiPagePublishJob;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.PublishWorkflowStep;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.object.page.PageCopyOpResultInfo;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.utility.FolderComparator;
import com.gentics.contentnode.object.utility.PageComparator;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.InstantPublisher.Result;
import com.gentics.contentnode.publish.InstantPublisher.ResultStatus;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.render.renderer.MetaEditableRenderer;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.ContentTagCreateRequest;
import com.gentics.contentnode.rest.model.request.DiffRequest;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.MultiObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.MultiPageAssignRequest;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiPagePublishRequest;
import com.gentics.contentnode.rest.model.request.MultiPubqueueApproveRequest;
import com.gentics.contentnode.rest.model.request.MultiTagCreateRequest;
import com.gentics.contentnode.rest.model.request.ObjectMoveRequest;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageIdSetRequest;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePreviewRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TagCreateRequest;
import com.gentics.contentnode.rest.model.request.TagSortAttribute;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.request.WorkflowRequest;
import com.gentics.contentnode.rest.model.request.page.CreatedTag;
import com.gentics.contentnode.rest.model.request.page.PageCopyRequest;
import com.gentics.contentnode.rest.model.request.page.PageFilenameSuggestRequest;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;
import com.gentics.contentnode.rest.model.response.DiffResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.MultiPageLoadResponse;
import com.gentics.contentnode.rest.model.response.MultiTagCreateResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.PagePreviewResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.model.response.PageRenderResponse.Editable;
import com.gentics.contentnode.rest.model.response.PageRenderResponse.MetaEditable;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.ReferencedFilesListResponse;
import com.gentics.contentnode.rest.model.response.ReferencedPagesListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TagCreateResponse;
import com.gentics.contentnode.rest.model.response.TagListResponse;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResponse;
import com.gentics.contentnode.rest.model.response.page.PageCopyResultInfo;
import com.gentics.contentnode.rest.model.response.page.PageFilenameSuggestResponse;
import com.gentics.contentnode.rest.resource.DiffResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.NodeObjectFilter;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.PageValidator;
import com.gentics.contentnode.rest.util.PageValidatorAndErrorCollector;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.StringFilter;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.staging.StagingUtil;
import com.gentics.contentnode.translation.LanguageVariantService;
import com.gentics.contentnode.validation.map.inputchannels.UserMessageInputChannel;
import com.gentics.contentnode.validation.util.ValidationUtils;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.util.FileUtil;
import de.jkeylockmanager.manager.ReturnValueLockCallback;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

import static com.gentics.contentnode.rest.util.MiscUtils.getItemList;
import static com.gentics.contentnode.rest.util.MiscUtils.getMatchingSystemUsers;
import static com.gentics.contentnode.rest.util.MiscUtils.getRequestedContentLanguage;
import static com.gentics.contentnode.rest.util.MiscUtils.getUrlDuplicationMessage;
import static com.gentics.contentnode.rest.util.MiscUtils.reduceList;

/**
 * Resource used for loading, saving and manipulating GCN pages.
 *
 * @author floriangutmann
 */
@Path("/page")
public class PageResourceImpl extends AuthenticatedContentNodeResource implements PageResource {

	/**
	 * Keyed lock to synchronize translation calls for the same contentset
	 */
	private static final TransactionLockManager<PageLoadResponse> translateLock = new TransactionLockManager<>();

	/**
	 * Create an instance
	 */
	public PageResourceImpl() {
		super();
	}

	/**
	 * Create an instance using the given transaction
	 *
	 * @param t transaction to use
	 */
	public PageResourceImpl(Transaction t) {
		super(t);
	}

	/**
	 * Helper method to transform tag references (like <node bla>)
	 *
	 * @param value             value possibly containing tag references
	 * @param transformationMap map of old tagnames -> new tagnames
	 * @return value with tag references replaced
	 */
	protected static String transformTagReferences(String value,
			Map<String, String> transformationMap) {
		for (Map.Entry<String, String> entry : transformationMap.entrySet()) {
			value = value.replaceAll("(<node (" + entry.getKey() + "))(>|:)",
					"<node_gtx " + entry.getValue() + "$3");
		}
		value = value.replaceAll("<node_gtx ", "<node ");
		return value;
	}

	/**
	 * Get the page with given id, check whether the page exists. Check for given permissions for the
	 * current user. Additionally, the page is locked (or a ReadOnlyException is thrown if locking is
	 * not possible)
	 *
	 * @param id    id of the page
	 * @param perms permissions to check
	 * @return page
	 * @throws NodeException                   when loading the page fails due to underlying error
	 * @throws EntityNotFoundException         when the page was not found
	 * @throws InsufficientPrivilegesException when the user doesn't have a requested permission on
	 *                                         the page
	 * @throws ReadOnlyException               when the page should be locked, but is locked by
	 *                                         another user
	 */
	public static Page getLockedPage(String id, PermHandler.ObjectPermission... perms)
			throws EntityNotFoundException, InsufficientPrivilegesException, ReadOnlyException,
			NodeException {
		// first get the page (checking for existance, privileges and other problems)
		Page page = getPage(id, perms);

		// now get the page again and lock it this time
		Transaction t = TransactionManager.getCurrentTransaction();
		page = t.getObject(Page.class, id, true);

		return page;
	}

	/**
	 * Variant of method {@link #getLockedPage(String, ObjectPermission...)}, that will try to lock
	 * all language variants
	 *
	 * @param id    id of the page. This can either be a local or globalid
	 * @param perms permissions to check
	 * @return list of locked pages
	 * @throws EntityNotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public static List<Page> getLockedLanguageVariants(String id,
			PermHandler.ObjectPermission... perms)
			throws EntityNotFoundException, InsufficientPrivilegesException, ReadOnlyException,
			NodeException {
		Page page = getLockedPage(id, perms);
		List<Page> langVariants = page.getLanguageVariants(false);
		List<Page> lockedLangVariants = new Vector<Page>(langVariants.size());
		Transaction t = TransactionManager.getCurrentTransaction();
		try {
			for (Page langVariant : langVariants) {

				lockedLangVariants.add(t.getObject(Page.class,
						langVariant.getId(), true));
			}
		} catch (ReadOnlyException e) {
			// one of the language variants could not be locked, so we unlock all previously locked
			for (Page langVariant : lockedLangVariants) {
				langVariant.unlock();
			}

			// throw the exception
			throw e;
		}

		return lockedLangVariants;
	}

	/**
	 * Get the page with given id, check whether the page exists. Check for given permissions for the
	 * current user.
	 *
	 * @param id    id of the page
	 * @param perms permissions to check
	 * @return page
	 * @throws NodeException                   when loading the page fails due to underlying error
	 * @throws EntityNotFoundException         when the page was not found
	 * @throws InsufficientPrivilegesException when the user doesn't have a requested permission on
	 *                                         the page
	 */
	public static Page getPage(String id, PermHandler.ObjectPermission... perms)
			throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page page = t.getObject(Page.class, id);
		if (page == null) {
			I18nString message = new CNI18nString("page.notfound");
			message.setParameter("0", id.toString());
			throw new EntityNotFoundException(message.toString());
		}

		// check permission bits
		for (PermHandler.ObjectPermission p : perms) {
			if (!p.checkObject(page)) {
				I18nString message = new CNI18nString("page.nopermission");
				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), page, p.getPermType());
			}

			// delete permissions for master pages must be checked for all channels containing localized copies
			if (page.isMaster() && p == ObjectPermission.delete) {
				for (int channelSetNodeId : page.getChannelSet().keySet()) {
					if (channelSetNodeId == 0) {
						continue;
					}
					Node channel = t.getObject(Node.class, channelSetNodeId);
					if (!ObjectPermission.delete.checkObject(page, channel)) {
						I18nString message = new CNI18nString("page.nopermission");
						message.setParameter("0", id.toString());
						throw new InsufficientPrivilegesException(message.toString(), page, PermType.delete);
					}
				}
			}
		}

		return page;
	}

	/**
	 * Helper method to get the tags stored in the given render result in the reverse order in which
	 * they were rendered
	 *
	 * @param renderResult render result
	 * @return list of tags
	 * @throws NodeException
	 */
	public static List<PageRenderResponse.Tag> getTags(RenderResult renderResult)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<String, PageRenderResponse.Tag> tags = new LinkedHashMap<String, PageRenderResponse.Tag>();

		String[] blockHtmlIds = (String[]) renderResult.getParameters().get(
				AlohaRenderer.PARAM_BLOCK_HTML_IDS);
		String[] blockTagIds = (String[]) renderResult.getParameters().get(
				AlohaRenderer.PARAM_BLOCK_TAG_IDS);

		String[] editableHtmlIds = (String[]) renderResult.getParameters().get(
				AlohaRenderer.PARAM_EDITABLE_HTML_IDS);
		String[] editableValueIds = (String[]) renderResult.getParameters().get(
				AlohaRenderer.PARAM_EDITABLE_VALUE_IDS);

		Collection<?> readonlies;
		if (renderResult.getParameters().containsKey(MetaEditableRenderer.READONLIES_KEY)) {
			readonlies = ObjectTransformer.getCollection(renderResult.getParameters().get(
					MetaEditableRenderer.READONLIES_KEY), Collections.EMPTY_LIST);
		} else {
			readonlies = Collections.EMPTY_LIST;
		}

		// iterate over blocks and make tags out of them
		if (blockTagIds != null) {
			for (int i = 0; i < blockTagIds.length; i++) {
				// get the tag
				ContentTag cnTag = (ContentTag) t.getObject(ContentTag.class,
						ObjectTransformer.getInteger(blockTagIds[i], null));
				PageRenderResponse.Tag tag = new PageRenderResponse.Tag(
						blockHtmlIds[i], cnTag.getName());

				// determine, whether all editable parts in the tag are inline editable
				if (cnTag.getConstruct().isEditable()) {
					boolean onlyEditables = true;
					List<Part> parts = cnTag.getConstruct().getParts();
					for (Part part : parts) {
						if (part.isEditable() && !part.isInlineEditable()) {
							onlyEditables = false;
							break;
						}
					}
					tag.setOnlyeditables(onlyEditables);
				} else {
					tag.setOnlyeditables(false);
				}

				tags.put(cnTag.getName(), tag);
			}
		}

		// iterate over editables and make tags out of them
		if (editableHtmlIds != null) {
			for (int i = 0; i < editableHtmlIds.length; i++) {
				// get the Value
				Value value = (Value) t.getObject(Value.class, ObjectTransformer
						.getInteger(editableValueIds[i], null));
				// get the tag name
				String tagName = value.getContainer().get("name").toString();

				// check whether the tag was already found
				PageRenderResponse.Tag tag = tags.get(tagName);
				if (tag == null) {
					// tag not found, so create it
					tag = new PageRenderResponse.Tag(editableHtmlIds[i], tagName);
					tag.setOnlyeditables(true);
					tags.put(tagName, tag);
				}
				// check whether the tag already contains a list of editables
				List<Editable> editables = tag.getEditables();
				if (editables == null) {
					// create a new list of editables
					editables = new Vector<PageRenderResponse.Editable>();
					tag.setEditables(editables);
				}

				// add the editable (if not already added)
				boolean editableFound = false;
				for (Editable editable : editables) {
					if (StringUtils.isEqual(editable.getPartname(), value.getPart().getKeyname())) {
						editableFound = true;
						break;
					}
				}
				if (!editableFound) {
					editables.add(new Editable(editableHtmlIds[i], value.getPart()
							.getKeyname(), readonlies.contains(tagName)));
				}
			}
		}

		List<PageRenderResponse.Tag> tagValues = new ArrayList<>(tags.values());
		Collections.reverse(tagValues);
		return tagValues;
	}

	/**
	 * Helper method to get the list of meta editables
	 *
	 * @param renderResult render result
	 * @return list of meta editables
	 * @throws NodeException
	 */
	public static List<MetaEditable> getMetaEditables(RenderResult renderResult)
			throws NodeException {
		List<MetaEditable> metaEditables = new Vector<PageRenderResponse.MetaEditable>();

		// now add meta editables, which where added by the MetaEditableRenderer
		if (renderResult.getParameters().containsKey(MetaEditableRenderer.METAEDITABLES_KEY)) {
			Collection<?> me = ObjectTransformer.getCollection(renderResult.getParameters().get(
					MetaEditableRenderer.METAEDITABLES_KEY), null);
			String id;
			for (Iterator<?> iterator = me.iterator(); iterator.hasNext(); ) {
				id = ObjectTransformer.getString(iterator.next(), null);
				// unfortunately this has to be done here, as jQuery would
				// interprete "."-chars as the start of a css class name

				// TODO this is extremely ugly, due to RenderResult not supporting
				// Maps in addParameters. Review this with NOP
				MetaEditable metaEditable = new MetaEditable(
						MetaEditableRenderer.EDITABLE_PREFIX + id.replaceAll("\\.", "_"), id);
				metaEditables.add(metaEditable);
			}
		}

		return metaEditables;
	}

	@Override
	public PageListResponse list(
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam PageListParameterBean pageListParams,
			@BeanParam FilterParameterBean filterParams,
			@BeanParam SortParameterBean sortingParams,
			@BeanParam PagingParameterBean pagingParams,
			@BeanParam PublishableParameterBean publishParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		try {
			Transaction t = getTransaction();
			FolderResourceImpl folderResource = new FolderResourceImpl();
			boolean channelIdSet = false;
			boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only)
					.contains(wastebinParams.wastebinSearch);

			folderResource.setTransaction(t);

			try {
				channelIdSet = setChannelToTransaction(pageListParams.nodeId);

				try (WastebinFilter filter = folderResource.getWastebinFilter(includeWastebin,
						inFolder.folderId)) {
					// load the folder
					com.gentics.contentnode.object.Folder f = folderResource.getFolder(inFolder.folderId,
							false);

					// if a language was set, we need to do language fallback here
					ContentLanguage lang = null;
					Node node = f.getNode();

					if (!ObjectTransformer.isEmpty(pageListParams.language)) {
						List<ContentLanguage> languages = node.getLanguages();

						for (ContentLanguage contentLanguage : languages) {
							if (pageListParams.language.equals(contentLanguage.getCode())
									|| pageListParams.language.equals(
									ObjectTransformer.getString(contentLanguage.getId(), null))) {
								lang = contentLanguage;
								break;
							}
						}

						// when the language was NOT found and language fallback is disabled, we will return an empty list
						if (lang == null && !pageListParams.langFallback && !"0".equals(
								pageListParams.language)) {
							PageListResponse response = new PageListResponse();

							response.setHasMoreItems(false);
							response.setNumItems(0);
							response.setResponseInfo(
									new ResponseInfo(ResponseCode.OK, "Successfully loaded pages"));

							return response;
						} else if (lang == null && pageListParams.langFallback) {
							// language fallback is activated, but the requested
							// language was not found, so get the first language for the
							// node (if any)
							if (languages.size() > 0) {
								lang = languages.get(0);
							}
						}
					}

					// Check which references shall be added
					Collection<Reference> fillRefs = new ArrayList<>();

					if (pageListParams.template) {
						fillRefs.add(Reference.TEMPLATE);
					}
					if (pageListParams.folder) {
						fillRefs.add(Reference.FOLDER);
					}
					if (pageListParams.languageVariants) {
						fillRefs.add(Reference.LANGUAGEVARIANTS);
					}
					if (pageListParams.workflowOwn || pageListParams.workflowWatch) {
						fillRefs.add(Reference.WORKFLOW);
					}
					if (pageListParams.translationStatus || pageListParams.inSync != null) {
						fillRefs.add(Reference.TRANSLATIONSTATUS);
					}
					if (pageListParams.contentTags) {
						fillRefs.add(Reference.CONTENT_TAGS);
					}
					if (pageListParams.objectTags) {
						fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
					}

					// if restricting with templateIds, make sure we use the IDs of the master objects
					if (!ObjectTransformer.isEmpty(pageListParams.templateIds) && t.getNodeConfig()
							.getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
						List<Template> templates = t.getObjects(Template.class, pageListParams.templateIds);
						List<Integer> templateMasterIds = new ArrayList<>(templates.size());
						for (Template tmpl : templates) {
							templateMasterIds.add(tmpl.getMaster().getId());
						}
						pageListParams.templateIds = templateMasterIds;
					}

					// get the pages
					List<Page> pages = folderResource.getPagesFromFolder(f,
							Folder.PageSearch.create().setSearchString(filterParams.query)
									.setSearchContent(pageListParams.searchContent)
									.setFileNameSearch(pageListParams.filename)
									.setNiceUrlSearch(pageListParams.niceUrl)
									.setWorkflowOwn(pageListParams.workflowOwn)
									.setWorkflowWatch(pageListParams.workflowWatch)
									.setEditor(publishParams.isEditor).setCreator(publishParams.isCreator)
									.setPublisher(publishParams.isPublisher)
									.setOnline(publishParams.online).setModified(publishParams.modified)
									.setPlanned(pageListParams.planned).setQueued(pageListParams.queued)
									.setPriority(pageListParams.priority).setTemplateIds(pageListParams.templateIds)
									.setPermissions(pageListParams.permission)
									.setEditors(getMatchingSystemUsers(publishParams.editor, publishParams.editorIds))
									.setCreators(
											getMatchingSystemUsers(publishParams.creator, publishParams.creatorIds))
									.setPublishers(
											getMatchingSystemUsers(publishParams.publisher, publishParams.publisherIds))
									.setEditedBefore(publishParams.editedBefore)
									.setEditedSince(publishParams.editedSince)
									.setCreatedBefore(publishParams.createdBefore)
									.setCreatedSince(publishParams.createdSince)
									.setPublishedBefore(publishParams.publishedBefore)
									.setPublishedSince(publishParams.publishedSince)
									.setRecursive(inFolder.recursive).setInherited(pageListParams.inherited)
									.setWastebin(wastebinParams.wastebinSearch == WastebinSearch.only)
									.setIncludeMlIds(pageListParams.includeMlIds)
									.setExcludeMlIds(pageListParams.excludeMlIds),
							pageListParams.timeDue,
							pageListParams.inSync);

					if (wastebinParams.wastebinSearch == WastebinSearch.only) {
						Wastebin.ONLY.filter(pages);
					}

					// check view permission on the pages
					for (Iterator<Page> iter = pages.iterator(); iter.hasNext(); ) {
						Page page = iter.next();
						if (!PermHandler.ObjectPermission.view.checkObject(page)) {
							iter.remove();
						}
					}

					// if a language was set, we need to filter out pages of the wrong language
					if (lang != null) {
						if (pageListParams.langFallback) {
							// if language fallback shall be used, we need to do it
							// now
							PageLanguageFallbackList fallbackList = new PageLanguageFallbackList(lang, node);

							for (Page page : pages) {
								fallbackList.addPage(page);
							}
							pages = fallbackList.getPages();
						} else {
							// otherwise we simply ignore languages of the wrong
							// language
							for (Iterator<Page> iPages = pages.iterator(); iPages.hasNext(); ) {
								Page page = iPages.next();

								if (!lang.equals(page.getLanguage())) {
									iPages.remove();
								}
							}
						}
					} else if ("0".equals(pageListParams.language)) {
						// "0" was set as language, so we only return pages WITHOUT language
						for (Iterator<Page> iPages = pages.iterator(); iPages.hasNext(); ) {
							Page page = iPages.next();

							if (page.getLanguage() != null) {
								iPages.remove();
							}
						}
					} else {
						// do the fallback with no specific language
						PageLanguageFallbackList fallbackList = new PageLanguageFallbackList(null, node);

						fallbackList.setCheckViewPermission(true);

						for (Page page : pages) {
							fallbackList.addPage(page);
						}
						pages = fallbackList.getPages();
					}

					Wastebin wastebin;

					switch (wastebinParams.wastebinSearch) {
						case include:
							wastebin = Wastebin.INCLUDE;
							break;
						case only:
							wastebin = Wastebin.ONLY;
							break;
						case exclude:
						default:
							wastebin = Wastebin.EXCLUDE;
							break;
					}

					Map<String, String> fieldMap = new HashMap<>();
					fieldMap.put("niceUrl", "nice_url");
					fieldMap.put("alternateUrls", "alternate_urls");
					fieldMap.put("fileName", "filename");

					ResolvableComparator<Page> comparator = ResolvableComparator.get(
							sortingParams,
							fieldMap,
							// From AbstractContentObject
							"id", "ttype", "ispage", "isfolder", "isfile", "isimage", "istag",
							// From AbstractPage
							"seite", "page", "url", "template", "template_id", "ml_id", "name", "nice_url",
							"niceUrl",
							"alternate_urls", "alternateUrls", "filename", "fileName", "description",
							"beschreibung", "priority", "folder_id",
							"node_id", "tags", "publishtimestamp", "veroeffentlichungsdatum", "creationtimestamp",
							"erstellungstimestamp", "edittimestamp", "bearbeitungstimestamp", "editdate",
							"bearbeitungsdatum",
							"expiredate", "expiretimestamp", "ordner", "folder", "node", "languageset",
							"ersteller",
							"creator", "bearbeiter", "editor", "veroeffentlicher", "publisher", "sprach_id",
							"language_id", "language", "sprache", "languagecode", "sprach_code",
							"languagevariants",
							"sprachvarianten", "object", "versions", "version", "pagevariants", "ismaster",
							"inherited",
							"edittime", "createtime", "createtimestamp", "timepub", "content_id", "code",
							"online",
							"sprachen", "contentset_id");

					PageListResponse response = ListBuilder.from(pages,
									p -> ModelBuilder.getPage(p, fillRefs, wastebin))
							.sort(comparator)
							.page(pagingParams)
							.to(new PageListResponse());

					response.setStagingStatus(
							StagingUtil.checkStagingStatus(pages, inFolder.stagingPackageName,
									o -> o.getGlobalId().toString(), pageListParams.languageVariants));
					return response;
				}
			} finally {
				if (channelIdSet) {
					// render type channel wieder auf null setzen
					t.resetChannel();
				}
			}
		} catch (EntityNotFoundException e) {
			return new PageListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting pages", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new PageListResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, e.getMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#create(com.gentics.contentnode
	 * .rest.model.request.PageCreateRequest)
	 */
	@POST
	@Path("/create")
	public PageLoadResponse create(PageCreateRequest request) {
		Transaction t = getTransaction();
		// this will be the new page
		Page page = null;

		// this is the mother folder of the new page
		Folder folder = null;

		// this will be the template of the new page
		Template template = null;

		// whether a page variant is created
		boolean createVariant = ObjectTransformer.getInt(request.getVariantId(), 0) != 0;

		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// If set nodeId, create the page as local in the channel, corresponding
		// to the provided nodeId.
		final Integer channelId = request.getNodeId();

		Integer srcChannel = request.getVariantChannelId();

		try (AutoCommit ac = new AutoCommit(); ChannelTrx ct = new ChannelTrx(srcChannel)) {
			// first get the folder
			Node targetChannel;
			try (NoMcTrx nmt = new NoMcTrx()) {
				folder = t.getObject(Folder.class, request.getFolderId());
				targetChannel = t.getObject(Node.class, channelId);
			}
			if (folder == null) {
				I18nString message = new CNI18nString("folder.notfound");
				return new PageLoadResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, "Folder with id "
								+ request.getFolderId() + " does not exist"), null);
			}
			folder = folder.getMaster();

			// transform the requested language code into a language and set
			// it to the page
			ContentLanguage language = MiscUtils.getRequestedContentLanguage(folder,
					request.getLanguage());

			// now check the permission to create the page
			try (ChannelTrx ct1 = new ChannelTrx(request.getNodeId())) {
				if (!t.canCreate(folder, Page.class, language)) {
					I18nString message = new CNI18nString("page.nopermission");
					return new PageLoadResponse(new Message(Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.PERMISSION,
									"Insufficient permission to create pages in folder " + request.getFolderId()),
							null);
				}
			}

			// if a template id was requested, try to get that template

			if (request.getTemplateId() != null) {

				Template requestedTemplate = t.getObject(Template.class, request.getTemplateId());
				if (requestedTemplate != null) {
					for (Folder f : requestedTemplate.getFolders()) {
						if (f.getId().equals(folder.getId())) {
							template = requestedTemplate;
						}
					}
				}

				// Throw an error if the given template could not found in
				// the folder
				if (template == null && !createVariant) {
					I18nString message = new CNI18nString("rest.general.insufficientdata");
					return new PageLoadResponse(new Message(Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.FAILURE,
									"Could not find a template to create page"), null);
				}
			} else if (!createVariant) {
				// now get the templates linked to the folder
				List<Template> templates = folder.getTemplates();

				// if no template id requested get the first template from the
				// list
				if (template == null && templates.size() > 0) {
					template = templates.iterator().next();
				}

				// if still no template found, we have a failure
			}
			if (!createVariant && template == null) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");
				return new PageLoadResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
								"Could not find a template to create page"), null);
			}

			// check whether the page shall be a variant of another page
			if (createVariant) {
				try {
					Page origin;
					if (prefs.getFeature("always_allow_page_create_variant")) {
						origin = getPage(request.getVariantId(), ObjectPermission.view);
					} else {
						origin = getPage(request.getVariantId(), ObjectPermission.edit);
					}
					page = origin.createVariant(folder, targetChannel);
					template = origin.getTemplate();
				} catch (EntityNotFoundException e) {
					return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
							new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()),
							null);
				} catch (InsufficientPrivilegesException e) {
					InsufficientPrivilegesMapper.log(e);
					return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
							new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()),
							null);
				}
			} else {
				page = (Page) t.createObject(Page.class);
				page.setFolderId(folder.getId());
				page.setTemplateId(template.getId());
			}

			boolean checkDuplicateName = false;
			boolean checkDuplicateFilename = false;
			if (!StringUtils.isEmpty(request.getPageName())) {
				page.setName(request.getPageName());
				if (ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false)) {
					checkDuplicateName = true;
				}
			}
			if (!StringUtils.isEmpty(request.getFileName())) {
				if (!request.isForceExtension() && !StringUtils.isEmpty(
						template.getMarkupLanguage().getExtension()) && !folder.getOwningNode()
						.isOmitPageExtension()) {
					String expectedExtension = String.format(".%s",
							template.getMarkupLanguage().getExtension());
					if (!request.getFileName().toLowerCase().endsWith(expectedExtension)) {
						request.setFileName(String.format("%s%s", request.getFileName(), expectedExtension));
					}
				}

				page.setFilename(request.getFileName());
				if (ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false)) {
					checkDuplicateFilename = true;
				}
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!StringUtils.isEmpty(request.getNiceUrl())) {
					page.setNiceUrl(request.getNiceUrl());
				}
				if (request.getAlternateUrls() != null) {
					page.setAlternateUrls(request.getAlternateUrls());
				}
			}

			if (!StringUtils.isEmpty(request.getDescription())) {
				page.setDescription(request.getDescription());
			}

			if (request.getPriority() != null) {
				page.setPriority(request.getPriority());
			}

			if (language != null) {
				page.setLanguage(language);
			}

			// If multichannelling is active and nodeId is provided start
			// preparing the page for local channel creation.
			if (channelId != null && prefs.isFeature(Feature.MULTICHANNELLING)) {
				Node folderNode = folder.getNode().getMaster();
				Node channel = t.getObject(Node.class, channelId);

				if (!folderNode.equals(channel)) {
					if (channel == null || !channel.isChannel()) {
						logger.error(
								"Error while creating new page: there is no channel for the specified nodeId.");
						I18nString m = new CNI18nString("rest.general.error");
						return new PageLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
								new ResponseInfo(ResponseCode.FAILURE,
										"Error while creating new page: there is no channel for the specified nodeId."),
								null);
					}

					// Check whether page's folder node is one of the channel
					// master nodes (check if the folder
					// of the page is visible from the channel).
					if (!channel.getMasterNodes().contains(folderNode)) {
						logger.error(
								"Error while creating new page: page's folder is not in a master node of the specified channel.");
						I18nString m = new CNI18nString("rest.general.error");
						return new PageLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
								new ResponseInfo(ResponseCode.FAILURE,
										"Error while creating new page: page's folder is not in a master node of the specified channel."),
								null);
					}
					if (!createVariant) {
						page.setChannelInfo(channel.getId(), page.getChannelSetId());
					}
				}
			}

			// check for duplicates now
			if (checkDuplicateName) {
				NodeObject conflictingObject = PageFactory.isNameAvailable(page);
				if (conflictingObject != null) {
					CNI18nString message = new CNI18nString("page.duplicatename");
					message.addParameter(I18NHelper.getPath(conflictingObject.getParentObject()));
					message.addParameter(page.getName());
					return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.INVALIDDATA,
									"Error while creating page: " + message.toString(), "name"), null);
				}
			}
			if (checkDuplicateFilename) {
				NodeObject conflictingObject = PageFactory.isFilenameAvailable(page);
				if (conflictingObject != null) {
					CNI18nString message = new CNI18nString("a page with this filename already exists");
					message.addParameter(I18NHelper.getPath(conflictingObject));
					return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.INVALIDDATA,
									"Error while creating page: " + message.toString(), "fileName"), null);
				}
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!StringUtils.isEmpty(page.getNiceUrl())) {
					NodeObject conflictingObject = PageFactory.isNiceUrlAvailable(page, page.getNiceUrl());
					if (conflictingObject != null) {
						I18nString message = getUrlDuplicationMessage(page.getNiceUrl(), conflictingObject);
						return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
								new ResponseInfo(ResponseCode.INVALIDDATA,
										"Error while creating page: " + message.toString(), "niceUrl"), null);
					}
				}

				if (!page.getAlternateUrls().isEmpty()) {
					List<Message> messages = new ArrayList<>();
					for (String url : page.getAlternateUrls()) {
						NodeObject conflictingObject = PageFactory.isNiceUrlAvailable(page, url);
						if (conflictingObject != null) {
							I18nString message = getUrlDuplicationMessage(url, conflictingObject);
							messages.add(new Message(Message.Type.CRITICAL, message.toString()));
						}
					}
					if (!messages.isEmpty()) {
						PageLoadResponse response = new PageLoadResponse();
						response.setResponseInfo(
								new ResponseInfo(ResponseCode.INVALIDDATA, "Error while creating page.",
										"alternateUrls"));
						response.setMessages(messages);
						return response;
					}
				}
			}

			page.save();

			// when a page variant was created, unlock the page (all language variants)
			if (createVariant) {
				for (Page languageVariant : page.getLanguageVariants(true)) {
					languageVariant.unlock();
				}
				page.unlock();
			}

			ac.success();
		} catch (NodeException e) {
			logger.error("Error while creating new page", e);
			I18nString m = new CNI18nString("rest.general.error");
			return new PageLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while creating new page :" + e.getLocalizedMessage()), null);
		}

		// after creating the page, load it and return it
		String id = Integer.toString(ObjectTransformer.getInteger(page.getId(), null));
		PageLoadResponse response = load(id, !createVariant, false, false, false, false, false, false,
				false, false, false, channelId, null);
		if (response.getResponseInfo().getResponseCode() == ResponseCode.OK) {
			response.getResponseInfo().setResponseMessage("Successfully created page");
		}
		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#save(java.lang.String, com.gentics.contentnode.rest.model.request.PageSaveRequest)
	 */
	@POST
	@Path("/save/{id}")
	public GenericResponse save(@PathParam("id") String id, PageSaveRequest request) {
		Transaction t = getTransaction();
		Page page = null;

		try (AutoCommit cmt = new AutoCommit()) {
			// first get the page and lock it (for checking the permission to
			// save)
			if (request.isClearOfflineAt() || request.isClearPublishAt()) {
				// clearing offlineAt or publishAt currently requires the publish permission (since that cannot be queued)
				page = getLockedPage(id, PermHandler.ObjectPermission.edit,
						PermHandler.ObjectPermission.publish);
			} else {
				page = getLockedPage(id, PermHandler.ObjectPermission.edit);
			}

			// Transform the rest PageSaveRequest into a rest page
			com.gentics.contentnode.rest.model.Page restPage = request.getPage();

			// The page ID is probably not in the request, so we set it here
			restPage.setId(Integer.parseInt(id));

			List<String> tagsToDelete = request.getDelete();

			// Validate the given rest page properties and tags
			sanitizeAndValidateRestPage(restPage, page);

			// fix an incorrect extension
			if (!StringUtils.isEmpty(restPage.getFileName()) && !StringUtils.isEmpty(
					page.getTemplate().getMarkupLanguage().getExtension()) && !page.getOwningNode()
					.isOmitPageExtension()) {
				String expectedExtension = String.format(".%s",
						page.getTemplate().getMarkupLanguage().getExtension());
				if (!restPage.getFileName().toLowerCase().endsWith(expectedExtension)) {
					restPage.setFileName(String.format("%s%s", restPage.getFileName(), expectedExtension));
				}
			}

			// check whether requested name is available
			boolean checkDuplicateName = ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false)
					&& !ObjectTransformer.isEmpty(restPage.getName())
					&& !StringUtils.isEqual(restPage.getName(), page.getName());
			boolean checkDuplicateFilename =
					ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false)
							&& !ObjectTransformer.isEmpty(restPage.getFileName())
							&& !StringUtils.isEqual(restPage.getFileName(), page.getFilename());

			// transform the page from the request into a page object
			page = ModelBuilder.getPage(request.getPage(), false);

			boolean deriveFilename = ObjectTransformer.getBoolean(request.getDeriveFileName(), false)
					&& StringUtils.isEmpty(request.getPage().getFileName());

			// An empty filename will be sanitized during page.save().
			if (deriveFilename) {
				page.setFilename("");
			}

			if (checkDuplicateName) {
				NodeObject conflictingObject = PageFactory.isNameAvailable(page);
				if (conflictingObject != null) {
					CNI18nString message = new CNI18nString("page.duplicatename");
					message.addParameter(I18NHelper.getPath(conflictingObject.getParentObject()));
					message.addParameter(page.getName());
					return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.INVALIDDATA,
									"Error while saving page " + id + ": " + message.toString(), "name"));
				}
			}
			if (checkDuplicateFilename && !deriveFilename) {
				NodeObject conflictingObject = PageFactory.isFilenameAvailable(page);
				if (conflictingObject != null) {
					CNI18nString message = new CNI18nString("a page with this filename already exists");
					message.addParameter(I18NHelper.getPath(conflictingObject));
					return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.INVALIDDATA,
									"Error while saving page " + id + ": " + message.toString(), "fileName"));
				}
			}
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
				if (!StringUtils.isEmpty(page.getNiceUrl())) {
					NodeObject conflictingObject = PageFactory.isNiceUrlAvailable(page, page.getNiceUrl());
					if (conflictingObject != null) {
						I18nString message = getUrlDuplicationMessage(page.getNiceUrl(), conflictingObject);
						return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
								new ResponseInfo(ResponseCode.INVALIDDATA,
										"Error while saving page: " + message.toString(), "niceUrl"), null);
					}
				}
				if (!page.getAlternateUrls().isEmpty()) {
					List<Message> messages = new ArrayList<>();
					for (String url : page.getAlternateUrls()) {
						NodeObject conflictingObject = PageFactory.isNiceUrlAvailable(page, url);
						if (conflictingObject != null) {
							I18nString message = getUrlDuplicationMessage(url, conflictingObject);
							messages.add(new Message(Message.Type.CRITICAL, message.toString()));
						}
					}
					if (!messages.isEmpty()) {
						PageLoadResponse response = new PageLoadResponse();
						response.setResponseInfo(
								new ResponseInfo(ResponseCode.INVALIDDATA, "Error while saving page.",
										"alternateUrls"));
						response.setMessages(messages);
						return response;
					}
				}
			}

			Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restPage.getTags();
			Map<String, ObjectTag> objectTags = page.getObjectTags();
			if (objectTags != null) {
				if (restTags != null) {
					// Throw an error if the user doesn't have permission
					// to update or delete all the object properties
					MiscUtils.checkObjectTagEditPermissions(restTags, objectTags, true);
				}

				if (tagsToDelete != null) {
					// Since there is no permission bit for deletion for
					// object properties, we just check for the edit permission.
					MiscUtils.checkObjectTagEditPermissions(tagsToDelete, objectTags, false);
				}
			}

			// delete the tags, which are requested to be deleted
			ModelBuilder.deleteTags(tagsToDelete, page);

			// save the page
			page.save(request.isCreateVersion());

			if (request.isClearPublishAt()) {
				page.clearTimePub();
			}
			if (request.isClearOfflineAt()) {
				page.clearTimeOff();
			}

			// unlock the page if requested
			if (request.isUnlock()) {
				page.unlock();
			}

			cmt.success();

			I18nString message = new CNI18nString("page.save.success");
			return new GenericResponse(new Message(Message.Type.SUCCESS, message.toString()),
					new ResponseInfo(ResponseCode.OK, "saved page with id: "
							+ page.getId()));
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			try {
				t.rollback(false);
			} catch (TransactionException ignored) {
			}
			logger.error("Error while saving page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while saving page " + id + ": " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#load(java.lang.String, boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean, java.lang.Integer)
	 */
	@GET
	@Path("/load/{id}")
	public PageLoadResponse load(
			@PathParam("id") String id,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@QueryParam("template") @DefaultValue("false") boolean template,
			@QueryParam("folder") @DefaultValue("false") boolean folder,
			@QueryParam("langvars") @DefaultValue("false") boolean languageVariants,
			@QueryParam("pagevars") @DefaultValue("false") boolean pageVariants,
			@QueryParam("workflow") @DefaultValue("false") boolean workflow,
			@QueryParam("translationstatus") @DefaultValue("false") boolean translationStatus,
			@QueryParam("versioninfo") @DefaultValue("false") boolean versionInfo,
			@QueryParam("disinherited") @DefaultValue("false") boolean disinherited,
			@QueryParam("construct") @DefaultValue("false") boolean construct,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("package") String stagingPackageName) {

		Transaction t = getTransaction();
		boolean channelIdset = false;
		try {
			String responseMessage = null;

			// Load the Page from GCN
			Page page;

			// set the nodeId, if provided
			channelIdset = setChannelToTransaction(nodeId);

			boolean readOnly = !update;
			PageLoadResponse response = new PageLoadResponse();

			// Check which references shall be added
			Collection<Reference> fillRefs = new Vector<Reference>();

			if (disinherited) {
				fillRefs.add(Reference.DISINHERITED_CHANNELS);
			}

			fillRefs.add(Reference.CONTENT_TAGS);
			fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
			if (template) {
				fillRefs.add(Reference.TEMPLATE);
			}
			if (folder) {
				fillRefs.add(Reference.FOLDER);
			}
			if (languageVariants) {
				fillRefs.add(Reference.LANGUAGEVARIANTS);
			}
			if (pageVariants) {
				fillRefs.add(Reference.PAGEVARIANTS);
			}
			if (workflow) {
				fillRefs.add(Reference.WORKFLOW);
			}
			if (translationStatus) {
				fillRefs.add(Reference.TRANSLATIONSTATUS);
			}
			if (versionInfo) {
				fillRefs.add(Reference.VERSIONS);
			}
			if (construct) {
				fillRefs.add(Reference.TAG_EDIT_DATA);
			}

			try {
				if (update) {
					page = getLockedPage(id, PermHandler.ObjectPermission.edit);
				} else {
					page = getPage(id, ObjectPermission.view);
				}
			} catch (ReadOnlyException e) {
				//TODO Why don't we use getPage(id) here?
				page = t.getObject(Page.class, id);
				responseMessage = e.getMessage();
				readOnly = true;
				// show a warning message that the page could not be locked
				response.addMessage(new Message(Type.WARNING, e.getLocalizedMessage()));
			}

			// transform the page into a REST object
			com.gentics.contentnode.rest.model.Page restPage = getPage(page, fillRefs, readOnly);

			response.setPage(restPage);
			if (responseMessage == null) {
				responseMessage = "Loaded page with id { " + id + " } successfully";
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, responseMessage));
			response.setStagingStatus(StagingUtil.checkStagingStatus(page, stagingPackageName));
			return response;
		} catch (EntityNotFoundException e) {
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (NodeException e) {
			logger.error("Error while loading page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while loading page " + id
							+ ": " + e.getLocalizedMessage()), null);
		} finally {
			if (channelIdset) {
				// reset channel
				t.resetChannel();
			}
		}
	}

	@Override
	@POST
	@Path("/load")
	public MultiPageLoadResponse load(MultiPageLoadRequest request,
			@QueryParam("fillWithNulls") @DefaultValue("false") boolean fillWithNulls) {
		Transaction t = getTransaction();
		boolean forUpdate = ObjectTransformer.getBoolean(request.isForUpdate(), false);
		Set<Reference> references = new HashSet<>();

		references.add(Reference.CONTENT_TAGS);
		references.add(Reference.OBJECT_TAGS_VISIBLE);

		if (ObjectTransformer.getBoolean(request.isDisinherited(), false)) {
			references.add(Reference.DISINHERITED_CHANNELS);
		}
		if (ObjectTransformer.getBoolean(request.isTemplate(), false)) {
			references.add(Reference.TEMPLATE);
		}
		if (ObjectTransformer.getBoolean(request.isFolder(), false)) {
			references.add(Reference.FOLDER);
		}
		if (ObjectTransformer.getBoolean(request.isLanguageVariants(), false)) {
			references.add(Reference.LANGUAGEVARIANTS);
		}
		if (ObjectTransformer.getBoolean(request.isPageVariants(), false)) {
			references.add(Reference.PAGEVARIANTS);
		}
		if (ObjectTransformer.getBoolean(request.isWorkflow(), false)) {
			references.add(Reference.WORKFLOW);
		}
		if (ObjectTransformer.getBoolean(request.isTranslationStatus(), false)) {
			references.add(Reference.TRANSLATIONSTATUS);
		}
		if (ObjectTransformer.getBoolean(request.isVersionInfo(), false)) {
			references.add(Reference.VERSIONS);
		}

		try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
			List<Page> allPages = t.getObjects(Page.class, request.getIds());

			List<com.gentics.contentnode.rest.model.Page> returnedPages = getItemList(request.getIds(),
					allPages, page -> {
						Set<Integer> ids = new HashSet<>();
						ids.add(page.getId());
						ids.addAll(page.getChannelSet().values());
						return ids;
					}, page -> {
						if (forUpdate) {
							page = t.getObject(page, true);
						}
						return getPage(page, references, !forUpdate);
					}, page -> {
						return ObjectPermission.view.checkObject(page)
								&& (!forUpdate || ObjectPermission.edit.checkObject(page));
					}, fillWithNulls);

			return new MultiPageLoadResponse(returnedPages);
		} catch (NodeException e) {
			return new MultiPageLoadResponse(
					new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.FAILURE, "Could not load pages"));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#preview(com.gentics.contentnode.rest.model.request.PagePreviewRequest)
	 */
	@POST
	@Path("/preview")
	public PagePreviewResponse preview(PagePreviewRequest request) {
		Transaction t = getTransaction();
		boolean channelIdset = false;
		try {
			// TODO should we check permissions here?

			PagePreviewResponse response = new PagePreviewResponse();

			if (request.getPage() == null) {
				response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE,
						"No page data was provided in the preview request."));
				return response;
			}
			channelIdset = setChannelToTransaction(request.getNodeId());
			Page page = ModelBuilder.getPage(request.getPage(), true);
			ContentNodeFactory factory = ContentNodeFactory.getInstance();

			NodePreferences preferences = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig()
					.getDefaultPreferences();
			RenderType renderType = RenderType.getDefaultRenderType(preferences,
					RenderType.EM_LIVEPREVIEW, t.getSessionId(), 0);
			renderType.setRenderUrlFactory(new StaticUrlFactory(
					RenderUrl.LINKWAY_PORTAL, RenderUrl.LINKWAY_PORTAL, null));
			renderType.setFrontEnd(true);
			t.setRenderType(renderType);
			RenderResult renderResult = new RenderResult();
			t.setRenderResult(renderResult);

			response.setPreview(page.render(renderResult));

			if ("OK".equals(renderResult.getReturnCode())) {
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK,
						"Rendering the page preview succeeded"));
			} else {
				response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE,
						"Rendering the page preview failed"));
			}
			// TODO add the renderresult messages
			return response;
		} catch (NodeException e) {
			logger.error("Error while rendering preview", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PagePreviewResponse(new Message(Message.Type.CRITICAL, message
					.toString()), new ResponseInfo(ResponseCode.FAILURE,
					"Error while rendering preview" + e.getLocalizedMessage()), null);
		} finally {
			if (channelIdset) {
				t.resetChannel();
			}
		}
	}

	/**
	 * Prepare the assignment message which has usually two parts. One system message and a
	 * customizable user message part.
	 *
	 * @param pageIds
	 * @param userMessage User message
	 * @return
	 */
	private String prepareAssignmentMessage(List<String> pageIds, String userMessage) {

		I18nString systemMessage = null;
		if (pageIds.size() == 1) {
			systemMessage = new CNI18nString("pubqueue.assign.message.single");
			systemMessage.setParameter("0", pageIds.get(0));
		} else {
			StringBuffer pageIdList = new StringBuffer();
			for (String pageId : pageIds) {
				pageIdList.append("<pageid ");
				pageIdList.append(pageId);
				pageIdList.append(">\n");
			}
			systemMessage = new CNI18nString("pubqueue.assign.message.multiple");
			systemMessage.setParameter("0", pageIdList.toString());
		}

		// Append the user message if it has been set
		StringBuffer wholeMessage = new StringBuffer();
		if (!StringUtils.isEmpty(userMessage)) {
			wholeMessage.append(systemMessage.toString().trim());
			wholeMessage.append("\n\n--\n\n");
			wholeMessage.append(userMessage.toString());
		} else {
			wholeMessage.append(systemMessage.toString());
		}

		return wholeMessage.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#assign(com.gentics.contentnode.rest.model.request.MultiPageAssignRequest)
	 */
	@POST
	@Path("/assign")
	@Override
	public GenericResponse assign(MultiPageAssignRequest request) {

		String userMessage = request.getMessage();
		List<Integer> userIds = request.getUserIds();
		if (userIds == null || userIds.size() == 0) {
			return new GenericResponse(null,
					new ResponseInfo(ResponseCode.INVALIDDATA, "No userIds could not be found."));
		}
		List<String> pageIds = request.getPageIds();
		if (pageIds == null || pageIds.size() == 0) {
			return new GenericResponse(null,
					new ResponseInfo(ResponseCode.INVALIDDATA, "User pageIds could not be found."));
		}
		Transaction t = getTransaction();
		try {
			if (!StringUtils.isEmpty(userMessage) && t.getNodeConfig().getDefaultPreferences()
					.getFeature("validation")) {
				ValidationResult result = ValidationUtils.validate(new UserMessageInputChannel(),
						userMessage);
				if (result.hasErrors()) {
					return new GenericResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA,
							"Validation of given user message failed."));
				}
			}

			// Check edit permission for given pages (The perm is needed to put
			// pages in "in_work" status)
			List<Page> pages = new ArrayList<>();
			for (String pageId : pageIds) {
				Page page = getPage(pageId, ObjectPermission.edit);
				page.unlock();
				pages.add(page);
			}

			MessageSender messageSender = new MessageSender();
			String message = prepareAssignmentMessage(pageIds, userMessage);

			// Iterate over users and send message
			for (Integer userId : userIds) {
				SystemUser user = t.getObject(SystemUser.class, userId);
				if (user == null) {
					return new GenericResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA,
							"User with id {" + userId + "} could not be found."));
				} else {
					com.gentics.contentnode.messaging.Message msg = new com.gentics.contentnode.messaging.Message(
							t.getUserId(), user.getId(), message);
					messageSender.sendMessage(msg);
				}
			}

			// Iterate over pages and set the page status
			for (Page page : pages) {
				page.clearQueue();
			}
			GenericResponse response = new GenericResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, message));
			t.addTransactional(messageSender);
			return response;
		} catch (NodeException e) {
			logger.error("Error assigning pages to users.", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while assigning pages to users: " + e.getLocalizedMessage()));
		}

	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#publish(java.lang.Integer, com.gentics.contentnode.rest.model.request.MultiPagePublishRequest)
	 */
	@POST
	@Path("/publish")
	public GenericResponse publish(@QueryParam("nodeId") Integer nodeId,
			MultiPagePublishRequest request) {
		LinkedList<String> ids = new LinkedList<String>();
		ids.addAll(request.getIds());
		MultiPagePublishJob job = new MultiPagePublishJob()
				.setIds(ids)
				.setAlllang(request.isAlllang())
				.setAt(request.getAt())
				.setMessage(request.getMessage())
				.setKeepPublishAt(request.isKeepPublishAt())
				.setKeepVersion(request.isKeepVersion())
				.setNodeId(nodeId);
		try {
			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		} catch (NodeException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.FAILURE, e.getMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#publish(java.lang.String, java.lang.Integer,
	 * com.gentics.contentnode.rest.model.request.PagePublishRequest)
	 */
	@POST
	@Path("/publish/{id}")
	public GenericResponse publish(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			PagePublishRequest request) {
		boolean channelIdSet = false;
		Transaction t = null;
		String restError = new CNI18nString("rest.general.error").toString();

		GenericResponse response = new GenericResponse();
		try {
			t = TransactionManager.getCurrentTransaction();
			channelIdSet = setChannelToTransaction(nodeId);
			List<String> feedback = new ArrayList<String>(1);

			Page page = MiscUtils.load(Page.class, id);

			MultiPagePublishJob.PublishSuccessState state =
					MultiPagePublishJob.publishPage(id,
							request.isAlllang(),
							request.getAt(),
							request.getMessage(),
							false,
							request.isKeepVersion(), feedback);

			String info = null;
			Type messageType = Type.SUCCESS;
			ResponseCode responseCode = ResponseCode.OK;

			// commit the transaction now to handle instant publishing
			t.commit(false);
			Result instantPublishingResult = t.getInstantPublishingResult(Page.TYPE_PAGE, page.getId());

			List<String> messages = new ArrayList<String>(1);

			switch (state) {
			case PUBLISHED:
				if (instantPublishingResult != null && instantPublishingResult.status() == ResultStatus.success) {
					messages.add(I18NHelper.get("page.instantpublish.success", I18NHelper.getName(page)));
				} else {
					messages.add(I18NHelper.get("page.publish.success", I18NHelper.getName(page)));
				}

				info = "Page " + id + " was successfully published";
				break;
			case PUBLISHAT:
				messages.add(I18NHelper.get("page.publishat.success", I18NHelper.getName(page)));
				info = "Publish at was set for page " + id;
				break;
			case WORKFLOW:
				if (request.getAt() > 0) {
					messages.add(I18NHelper.get("page.publishat.workflow", I18NHelper.getName(page)));
				} else {
					messages.add(I18NHelper.get("page.publish.workflow", I18NHelper.getName(page)));
				}
				info = "Page " + id
					 + " was successfully put into a publish workflow";
				break;
			case WORKFLOW_STEP:
				messages.add(I18NHelper.get("page.publish.workflow", I18NHelper.getName(page)));
				info = "Page " + id
					 + " was successfully pushed a step further in the publish"
					 + "workflow";
				break;
			case SKIPPED:
				// it is important to return this messages with type CRITICAL, otherwise they would not be shown to the user in the frontend
				messageType = Type.CRITICAL;
				responseCode = ResponseCode.INVALIDDATA;
				messages.addAll(feedback);
				StringBuilder infoStr = new StringBuilder();
				for (String str : feedback) {
					infoStr.append(str);
				}
				info = infoStr.toString();
				break;
			case INHERITED:
				messageType = Type.CRITICAL;
				responseCode = ResponseCode.INVALIDDATA;
				CNI18nString message = new CNI18nString("multipagepublishjob.inheritedpage");
				messages.add(message.toString());
				break;
			}

			response.setResponseInfo(new ResponseInfo(responseCode, info));

			for (String message : messages) {
				response.addMessage(new Message(messageType, message));
			}

			MiscUtils.addMessage(instantPublishingResult, response);

			return response;
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while publishing page " + id, e);
			return new GenericResponse(new Message(Message.Type.CRITICAL,
					restError), new ResponseInfo(ResponseCode.FAILURE,
					"Error while publishing page " + id + ": "
							+ e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				t.resetChannel();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#delete(java.lang.String)
	 */
	@POST
	@Path("/delete/{id}")
	public GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("noSync") Boolean noCrSync) {
		boolean syncCr = Optional.ofNullable(noCrSync).map(BooleanUtils::negate).orElse(true);
		try (InstantPublishingTrx ip = new InstantPublishingTrx(syncCr)) {
			// set the channel ID if given
			boolean isChannelIdset = setChannelToTransaction(nodeId);

			// get the page (this will check for existence and delete permission)
			Page page = getLockedPage(id, PermHandler.ObjectPermission.delete);

			Node node = page.getChannel();

			int nodeIdOfPage = -1;
			if (node != null) {
				nodeIdOfPage = ObjectTransformer.getInteger(node.getId(), -1);
			}

			if (nodeId == null) {
				nodeId = 0;
			}

			if (isChannelIdset && page.isInherited()) {
				throw new NodeException(
						"Can't delete an inherated page, the page has to be deleted in the master node.");
			}

			if (nodeId > 0 && nodeIdOfPage > 0 && nodeIdOfPage != nodeId) {
				throw new EntityNotFoundException(
						"The specified page exists, but is not part of the node you specified.");
			}

			int channelSetId = ObjectTransformer.getInteger(page.getChannelSetId(), 0);
			if (channelSetId > 0 && !page.isMaster()) {
				throw new NodeException(
						"Deletion of localized pages is currently not implemented, you maybe want to unlocalize it instead.");
			}

			final int pageId = page.getId();
			TransactionManager.getCurrentTransaction().commit(false);

			return Operator.executeLocked(new CNI18nString("page.delete.job").toString(), 0,
					Operator.lock(LockType.channelSet, channelSetId),
					new Callable<GenericResponse>() {
						@Override
						public GenericResponse call() throws Exception {
							Page page = getLockedPage(String.valueOf(pageId));

							// get all (other) language variants, which are visible in the node
							ArrayList<Page> nodeVariants = new ArrayList<>(page.getLanguageVariants(true));
							nodeVariants.remove(page);
							// get all (other) language variants, not considering the node languages
							ArrayList<Page> allVariants = new ArrayList<>(page.getLanguageVariants(false));
							allVariants.remove(page);

							// if we would delete the last node specific page variant, but there would be
							// other language variants (not visible in the node)
							// we delete them all
							if (nodeVariants.isEmpty() && !allVariants.isEmpty()) {
								for (Page p : allVariants) {
									p.delete();
								}
							}

							// now delete the page
							page.unlock();
							page.delete();

							I18nString message = new CNI18nString("page.delete.success");
							return new GenericResponse(new Message(Type.INFO, message.toString()),
									new ResponseInfo(ResponseCode.OK, message.toString()));
						}
					});
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while deleting page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message
					.toString()), new ResponseInfo(ResponseCode.FAILURE,
					"Error while deleting page " + id + ": " + e.getLocalizedMessage()));
		}
	}

	@Override
	@POST
	@Path("/wastebin/delete/{id}")
	public GenericResponse deleteFromWastebin(@PathParam("id") String id,
			@QueryParam("wait") @DefaultValue("0") long waitMs) {
		return deleteFromWastebin(new PageIdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/delete")
	public GenericResponse deleteFromWastebin(PageIdSetRequest request,
			@QueryParam("wait") @DefaultValue("0") long waitMs) {
		List<String> ids = request.getIds();
		if (ObjectTransformer.isEmpty(ids)) {
			I18nString message = new CNI18nString("rest.general.insufficientdata");
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
		}

		I18nString description = null;
		if (ids.size() == 1) {
			description = new CNI18nString("page.delete.wastebin");
			description.setParameter("0", ids.iterator().next());
		} else {
			description = new CNI18nString("pages.delete.wastebin");
			description.setParameter("0", ids.size());
		}
		return Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
			@Override
			public GenericResponse call() throws Exception {
				try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
					List<Page> pages = new ArrayList<Page>();

					for (String id : ids) {
						Page page = getPage(id);

						try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
							page = getPage(id, ObjectPermission.view, ObjectPermission.wastebin);
						}

						if (!page.isDeleted()) {
							I18nString message = new CNI18nString("page.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						}
						pages.add(page);
					}

					String pagePaths = I18NHelper.getPaths(pages, 5);
					for (Page page : pages) {
						page.delete(true);

						if (request.isAlllangs()) {
							List<Page> variants = new ArrayList<Page>();
							try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
								variants.addAll(page.getLanguageVariants(false));
								for (Iterator<Page> i = variants.iterator(); i.hasNext(); ) {
									Page variant = i.next();
									if (variant.equals(page) || !variant.isDeleted() || variant.isInherited()) {
										i.remove();
									}
								}
							}

							for (Page variant : variants) {
								variant.delete(true);
							}
						}
					}

					trx.success();
					// generate the response
					I18nString message = new CNI18nString(
							ids.size() == 1 ? "page.delete.wastebin.success" : "pages.delete.wastebin.success");
					message.setParameter("0", pagePaths);
					return new GenericResponse(new Message(Type.INFO, message.toString()),
							new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			}
		});
	}

	@Override
	@POST
	@Path("/wastebin/restore/{id}")
	public GenericResponse restoreFromWastebin(@PathParam("id") String id,
			@QueryParam("wait") @DefaultValue("0") long waitMs) {
		return restoreFromWastebin(new PageIdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/restore")
	public GenericResponse restoreFromWastebin(PageIdSetRequest request,
			@QueryParam("wait") @DefaultValue("0") long waitMs) {
		List<String> ids = request.getIds();
		if (ObjectTransformer.isEmpty(ids)) {
			I18nString message = new CNI18nString("rest.general.insufficientdata");
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
		}

		I18nString description = null;
		if (ids.size() == 1) {
			description = new CNI18nString("page.restore.wastebin");
			description.setParameter("0", ids.iterator().next());
		} else {
			description = new CNI18nString("pages.restore.wastebin");
			description.setParameter("0", ids.size());
		}
		return Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
			@Override
			public GenericResponse call() throws Exception {
				try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
					List<Page> pages = new ArrayList<Page>();

					for (String id : ids) {
						Page page = getPage(id);

						try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
							page = getPage(id, ObjectPermission.view, ObjectPermission.wastebin);
						}

						if (!page.isDeleted()) {
							I18nString message = new CNI18nString("page.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						}

						checkImplicitRestorePermissions(page);
						pages.add(page);
					}

					String pagePaths = I18NHelper.getPaths(pages, 5);
					for (Page page : pages) {
						page.restore();
						if (request.isAlllangs()) {
							List<Page> variants = new ArrayList<Page>();
							try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
								variants.addAll(page.getLanguageVariants(false));
								for (Iterator<Page> i = variants.iterator(); i.hasNext(); ) {
									Page variant = i.next();
									if (variant.equals(page) || !variant.isDeleted() || variant.isInherited()) {
										i.remove();
									}
								}
							}

							for (Page variant : variants) {
								variant.restore();
							}
						}
					}

					trx.success();
					// generate the response
					I18nString message = new CNI18nString(
							ids.size() == 1 ? "page.restore.wastebin.success" : "pages.restore.wastebin.success");
					message.setParameter("0", pagePaths);
					return new GenericResponse(new Message(Type.INFO, message.toString()),
							new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#cancel(java.lang.Integer, java.lang.Integer)
	 */
	@POST
	@Path("/cancel/{id}")
	public GenericResponse cancel(@PathParam("id") Integer id, @QueryParam("nodeId") Integer nodeId) {
		Transaction t = getTransaction();
		Page page = null;
		boolean isChannelIdset = false;

		try {
			// set the channel ID if given
			isChannelIdset = setChannelToTransaction(nodeId);

			page = getPage(id, ObjectPermission.view);

			// get the last version of the page
			NodeObjectVersion[] pageVersions = page.getVersions();
			if (!ObjectTransformer.isEmpty(pageVersions)) {
				page = getLockedPage(Integer.toString(id), ObjectPermission.edit);
				// restore the last page version
				page.restoreVersion(pageVersions[0], true);
			}

			page.unlock();
			t.commit(false);

			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK,
					"Cancelled editing of page : " + page.getId());
			return new GenericResponse(null, responseInfo);
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while cancelling page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message
					.toString()), new ResponseInfo(ResponseCode.FAILURE,
					"Error while canelling page " + id + ": " + e.getLocalizedMessage()));
		} finally {
			if (isChannelIdset) {
				t.resetChannel();
			}
		}
	}

	@POST
	@Path("/render")
	@Override
	public PageRenderResponse render(@QueryParam("nodeId") Integer nodeId,
			@QueryParam("template") String template,
			@QueryParam("edit") @DefaultValue("false") boolean editMode,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType,
			@QueryParam("tagmap") @DefaultValue("false") boolean tagmap,
			@QueryParam("inherited") @DefaultValue("false") boolean inherited,
			@QueryParam("publish") @DefaultValue("false") boolean publish,
			com.gentics.contentnode.rest.model.Page page) {
		return render(String.format("Preview of %d", page.getId()), nodeId, template, editMode,
				proxyprefix, linksType, tagmap, inherited, publish, editable -> {
					return ModelBuilder.getPage(page, !editable);
				});
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#render(java.lang.String, java.lang.Integer, java.lang.String, boolean, java.lang.String, com.gentics.contentnode.rest.model.request.LinksType, boolean, boolean, boolean, boolean)
	 */
	@GET
	@Path("/render/{id}")
	public PageRenderResponse render(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("template") String template,
			@QueryParam("edit") @DefaultValue("false") boolean editMode,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType,
			@QueryParam("tagmap") @DefaultValue("false") boolean tagmap,
			@QueryParam("inherited") @DefaultValue("false") boolean inherited,
			@QueryParam("publish") @DefaultValue("false") boolean publish,
			@QueryParam("version") Integer versionTimestamp) {
		int version = ObjectTransformer.getInt(versionTimestamp, 0);
		return render(id, nodeId, template, editMode, proxyprefix, linksType, tagmap, inherited,
				publish, editable -> {
					if (editable) {
						return getLockedPage(id, ObjectPermission.edit);
					} else if (version > 0) {
						Page page = getPage(id, ObjectPermission.view);
						Page versionedPage = TransactionManager.getCurrentTransaction()
								.getObject(Page.class, page.getId(), version);

						if (versionedPage == null) {
							I18nString message = new CNI18nString("page.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						} else {
							return versionedPage;
						}
					} else {
						return getPage(id, ObjectPermission.view);
					}
				});
	}

	@Override
	@GET
	@Path("/render/content/{id}")
	public Response renderContent(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("version") Integer versionTimestamp) {
		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			Page page = getPage(id, ObjectPermission.view);

			int version = ObjectTransformer.getInt(versionTimestamp, 0);
			if (version > 0) {
				page = TransactionManager.getCurrentTransaction()
						.getObject(Page.class, page.getId(), version);

				if (page == null) {
					I18nString message = new CNI18nString("page.notfound");
					message.setParameter("0", id.toString());
					throw new EntityNotFoundException(message.toString());
				}
			}

			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW, null, false, false,
					false)) {
				String content = page.render(RenderUtils.getPreviewTemplate(page, RenderType.EM_PREVIEW),
						new RenderResult(), null, null, null, null);
				return Response.status(Status.OK)
						.type(page.getTemplate().getMarkupLanguage().getContentType()).encoding("UTF-8")
						.entity(content).build();
			}
		} catch (EntityNotFoundException e) {
			return Response.status(Status.NOT_FOUND).build();
		} catch (InsufficientPrivilegesException e) {
			return Response.status(Status.FORBIDDEN).build();
		} catch (NodeException e) {
			logger.error(String.format("Error while rendering content of page %s", id), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Override
	@GET
	@Path("/diff/versions/{id}")
	public Response diffVersions(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("old") @DefaultValue("0") int oldVersion,
			@QueryParam("new") @DefaultValue("0") int newVersion,
			@QueryParam("source") @DefaultValue("false") boolean source) {
		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page page = getPage(id, ObjectPermission.view);

			String diff = renderDiff(source, () -> {
				Page p = t.getObject(Page.class, page.getId(), oldVersion);
				if (p == null) {
					I18nString message = new CNI18nString("page.notfound");
					message.setParameter("0", id.toString());
					throw new EntityNotFoundException(message.toString());
				} else {
					return p;
				}
			}, () -> {
				Page p = t.getObject(Page.class, page.getId(), newVersion);
				if (p == null) {
					I18nString message = new CNI18nString("page.notfound");
					message.setParameter("0", id.toString());
					throw new EntityNotFoundException(message.toString());
				} else {
					return p;
				}
			});
			return Response.status(Status.OK)
					.type(page.getTemplate().getMarkupLanguage().getContentType()).encoding("UTF-8")
					.entity(diff).build();
		} catch (EntityNotFoundException e) {
			return Response.status(Status.NOT_FOUND).build();
		} catch (InsufficientPrivilegesException e) {
			return Response.status(Status.FORBIDDEN).build();
		} catch (NodeException e) {
			logger.error(String.format("Error while rendering version diff for page %s", id), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Override
	@GET
	@Path("/diff/{id}")
	public Response diffWithOtherPage(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("otherPageId") @DefaultValue("0") int otherPageId,
			@QueryParam("source") @DefaultValue("false") boolean source) {
		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			Page page = getPage(id, ObjectPermission.view);
			Page otherPage = getPage(ObjectTransformer.getString(otherPageId, null),
					ObjectPermission.view);

			String diff = renderDiff(source, () -> {
				return page;
			}, () -> {
				return otherPage;
			});
			return Response.status(Status.OK)
					.type(page.getTemplate().getMarkupLanguage().getContentType()).encoding("UTF-8")
					.entity(diff).build();
		} catch (EntityNotFoundException e) {
			return Response.status(Status.NOT_FOUND).build();
		} catch (InsufficientPrivilegesException e) {
			return Response.status(Status.FORBIDDEN).build();
		} catch (NodeException e) {
			logger.error(String.format("Error while rendering version diff for page %s", id), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Helper method to render the diff between two pages
	 *
	 * @param source             true to show diff in source code
	 * @param firstPageSupplier  supplier for the first page
	 * @param secondPageSupplier supplier for the second page
	 * @return diff
	 * @throws NodeException
	 */
	protected String renderDiff(boolean source, Supplier<Page> firstPageSupplier,
			Supplier<Page> secondPageSupplier) throws NodeException {
		Page firstPage = firstPageSupplier.supply();
		Page secondPage = secondPageSupplier.supply();

		try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PREVIEW, null, false, false, false)) {
			String firstContent = firstPage.render(
					RenderUtils.getPreviewTemplate(firstPage, RenderType.EM_PREVIEW), new RenderResult(),
					null, null, null, null);
			String secondContent = secondPage.render(
					RenderUtils.getPreviewTemplate(secondPage, RenderType.EM_PREVIEW), new RenderResult(),
					null, null, null, null);

			DiffResource diffResource = new DiffResourceImpl();
			DiffRequest request = new DiffRequest();
			request.setContent1(firstContent);
			request.setContent2(secondContent);

			DiffResponse response = null;
			if (source) {
				response = diffResource.diffSource(request);
			} else {
				response = diffResource.diffHTML(request);
			}

			return response.getDiff();
		}
	}

	@Override
	@POST
	@Path("/renderTag/{tag}")
	public PageRenderResponse renderTag(@PathParam("tag") String tag,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType,
			com.gentics.contentnode.rest.model.Page page) {
		return renderTag(tag, nodeId, proxyprefix, linksType, () -> ModelBuilder.getPage(page, false));
	}

	@Override
	@GET
	@Path("/renderTag/{id}/{tag}")
	public PageRenderResponse renderTag(@PathParam("id") String id,
			@PathParam("tag") String tag,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("proxyprefix") String proxyprefix,
			@QueryParam("links") @DefaultValue("backend") LinksType linksType) {
		return renderTag(tag, nodeId, proxyprefix, linksType,
				() -> getLockedPage(id, ObjectPermission.edit));
	}

	/**
	 * Create the properties that should be rendered if parameter tagmap is true
	 *
	 * @param page Page to get the properties from
	 * @return properties that should be rendered
	 * @throws NodeException
	 */
	private Map<TagmapEntryRenderer, Object> createPropsToRender(Page page) throws NodeException {
		Map<TagmapEntryRenderer, Object> propsToRender = new HashMap<>();

		Node node = page.getFolder().getNode();
		ContentMap contentMap = node.getContentMap();

		if (contentMap != null) {
			List<TagmapEntryRenderer> tagmapEntries = contentMap.getTagmapEntries(Page.TYPE_PAGE);
			for (TagmapEntryRenderer entry : tagmapEntries) {
				// only add the "real" tagmap entries (which have a tagname)
				if (!StringUtils.isEmpty(entry.getTagname())) {
					propsToRender.put(entry, null);
				}
			}
		}
		return propsToRender;
	}

	/**
	 * Create a Render TYpe
	 *
	 * @param linksType       Type link
	 * @param editMode        One of the RenderType constants
	 * @param sessionId       Transaction session id
	 * @param nodePreferences Preferences for the node
	 * @param readOnly        should be read only
	 * @return Render Type
	 * @throws NodeException
	 */
	private RenderType createRenderType(LinksType linksType, int editMode,
			String sessionId, NodePreferences nodePreferences, boolean readOnly) throws NodeException {
		RenderType renderType = RenderType.getDefaultRenderType(nodePreferences, editMode, sessionId,
				0);

		switch (linksType) {
			case backend:
				renderType.setRenderUrlFactory(new DynamicUrlFactory(sessionId));
				renderType.setParameter(AlohaRenderer.LINKS_TYPE, "backend");
				break;
			case frontend:
				int linkWay = RenderType.parseLinkWay(nodePreferences.getProperty("contentnode.linkway"));
				int fileLinkWay = RenderType.parseLinkWay(
						nodePreferences.getProperty("contentnode.linkway_file"));
				// TODO Find out if checking for LINKWAY_AUTO should be done inside StaticUrlFactory.renderFile instead
				fileLinkWay =
						fileLinkWay == RenderUrl.LINKWAY_AUTO ? RenderUrl.LINKWAY_PORTAL : fileLinkWay;
				String fileLinkPrefix = nodePreferences.getProperty("contentnode.linkway_file_path");

				renderType.setRenderUrlFactory(new StaticUrlFactory(linkWay, fileLinkWay, fileLinkPrefix));
				renderType.setParameter(AlohaRenderer.LINKS_TYPE, "frontend");
				renderType.setFrontEnd(true);
				break;
		}

		if (editMode == RenderType.EM_PUBLISH) {
			renderType.setHandleDependencies(false);
			renderType.setDefaultRenderer(ContentRenderer.RENDERER_CONTENT);
			renderType.addRenderer(ContentRenderer.RENDERER_CONTENT);
			renderType.addRenderer(ContentRenderer.RENDERER_TAG);
			renderType.addRenderer(ContentRenderer.RENDERER_ALOHA);
		} else {
			renderType.addRenderer("aloha");
			renderType.setParameter(AlohaRenderer.ADD_SCRIPT_INCLUDES, false);
		}
		return renderType;
	}

	/**
	 * Create properties for set into the response
	 *
	 * @param propsToRender properties render
	 * @return Properties to be set into the response
	 */
	private Map<String, String> createResponseProperties(
			Map<TagmapEntryRenderer, Object> propsToRender) {
		Map<String, String> properties = new HashMap<>();
		for (Map.Entry<TagmapEntryRenderer, Object> prop : propsToRender.entrySet()) {
			properties.put(prop.getKey().getMapname(),
					ObjectTransformer.getString(prop.getValue(), null));
		}
		return properties;
	}

	/**
	 * Creates content to be set in the response content
	 *
	 * @param template      Template to be rendered in case it has been set
	 * @param page          The page
	 * @param renderResult
	 * @param renderType
	 * @param propsToRender Properties to be rendered
	 * @return Content to be set in the response
	 * @throws NodeException
	 */
	private String createResponseContent(String template, Page page, RenderResult renderResult,
			RenderType renderType, Map<TagmapEntryRenderer, Object> propsToRender)
			throws NodeException {
		String content = "";
		// render the page and set the content into the response object
		if (StringUtils.isEmpty(template)) {
			content = page.render(renderResult, propsToRender, CnMapPublisher.LINKTRANSFORMER);
		} else {
			renderType.setLanguage(page.getLanguage());
			TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());

			// push the page onto the rendertype stack
			renderType.push(page);
			try {
				content = renderer.render(renderResult, template);
			} finally {
				renderType.pop();
			}
		}
		return content;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#getTags(java.lang.Integer, java.lang.Integer, java.lang.Integer, com.gentics.contentnode.rest.model.request.TagSortAttribute, com.gentics.contentnode.rest.model.request.SortOrder, java.lang.String)
	 */
	@GET
	@Path("/getTags/{id}")
	public TagListResponse getTags(@PathParam("id") Integer id,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			final @QueryParam("sortby") @DefaultValue("name") TagSortAttribute sortBy,
			final @QueryParam("sortorder") @DefaultValue("asc") SortOrder sortOrder,
			@QueryParam("search") String search
	) {
		Page page = null;

		try {
			page = getPage(id, ObjectPermission.view);
			List<Tag> tags = new Vector<Tag>(page.getTags().values());

			// search tags
			if (!ObjectTransformer.isEmpty(search)) {
				NodeObjectFilter filter = new StringFilter("%" + search + "%", Tag.class
						.getMethod("getName"), true,
						StringFilter.Case.INSENSITIVE);
				filter.filter(tags);
			}

			// sort tags
			if (sortBy != null) {
				Collections.sort(tags, new Comparator<Tag>() {
					public int compare(Tag tag1, Tag tag2) {
						String value1 = null;
						String value2 = null;
						switch (sortBy) {
							case name:
								value1 = ObjectTransformer.getString(tag1
										.getName(), "");
								value2 = ObjectTransformer.getString(tag2
										.getName(), "");
								break;
							default:
								return 0;
						}

						return (sortOrder == SortOrder.asc || sortOrder == SortOrder.ASC) ? value1
								.compareToIgnoreCase(value2) : value2
								.compareToIgnoreCase(value1);
					}
				});
			}

			// return tag list
			TagListResponse response = new TagListResponse(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully fetched tags of page " + id));

			// do paging
			response.setNumItems(tags.size());
			response.setHasMoreItems(maxItems >= 0 && (tags.size() > skipCount + maxItems));
			reduceList(tags, skipCount, maxItems);

			List<com.gentics.contentnode.rest.model.Tag> restTags = new Vector<com.gentics.contentnode.rest.model.Tag>(
					tags.size());
			for (Tag tag : tags) {
				restTags.add(ModelBuilder.getTag(tag, false));
			}
			response.setTags(restTags);
			return response;
		} catch (EntityNotFoundException e) {
			return new TagListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new TagListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (Exception e) {
			logger.error("Error while cancelling page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new TagListResponse(new Message(Message.Type.CRITICAL, message
					.toString()), new ResponseInfo(ResponseCode.FAILURE,
					"Error while cancelling page " + id + ": " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#getPrivileges(java.lang.Integer)
	 */
	@GET
	@Path("/privileges/{id}")
	public PrivilegesResponse getPrivileges(@PathParam("id") Integer id) {
		throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
	}

	/**
	 * Create a new tag based on the given construct in the page with given id
	 *
	 * @param id          id of the page
	 * @param constructId id of the construct
	 * @param magicValue  optional magic value
	 * @return tag create response
	 * @throws NodeException
	 */
	protected TagCreateResponse createTagFromConstruct(String id, Integer constructId,
			String magicValue) throws NodeException {
		Transaction t = getTransaction();

		// get the page
		Page page = getLockedPage(id, PermHandler.ObjectPermission.edit);

		// add a contenttag
		ContentTag newTag = page.getContent().addContentTag(constructId);

		// enable the tag TODO should we really enable the tag?
		newTag.setEnabled(true);

		// honor the magic value, if the feature is activated
		if (!ObjectTransformer.isEmpty(magicValue) && t.getNodeConfig().getDefaultPreferences()
				.getFeature("magic_part_value")) {
			@SuppressWarnings("unchecked")
			Collection<String> magicPartNames = ObjectTransformer.getCollection(
					t.getNodeConfig().getDefaultPreferences().getPropertyObject("magic_part_names"),
					Collections.EMPTY_LIST);
			ValueList values = newTag.getValues();
			for (Value value : values) {
				if (magicPartNames.contains(value.getPart().getKeyname())) {
					value.setValueText(magicValue);
				}
			}
		}

		// save the page
		page.save(false);

		ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK, "created new tag {"
				+ newTag.getName() + "} in page with id: " + page.getId());

		return new TagCreateResponse(null, responseInfo, ModelBuilder.getTag(newTag, false));
	}

	/**
	 * Copy the tag with name tagname from the page sourceId (including all "embedded" tags) into the
	 * page with given id
	 *
	 * @param id       id of the page
	 * @param sourceId id of the source page
	 * @param tagname  tagname of the source tag
	 * @return tag create response
	 * @throws NodeException
	 */
	protected TagCreateResponse copyTag(String id, String sourceId, String tagname)
			throws NodeException {
		// get the source page
		Page sourcePage = getPage(sourceId, PermHandler.ObjectPermission.view);

		// get the page
		Page page = getLockedPage(id, PermHandler.ObjectPermission.edit);

		// render the tag to collect all "embedded" tags
		PageRenderResponse renderResponse = render(sourceId, null, "<node " + tagname + ">", true, null,
				LinksType.backend, false, false, false, 0);
		if (ResponseCode.OK.equals(renderResponse.getResponseInfo()
				.getResponseCode())) {
			Map<String, String> nameTranslation = new HashMap<String, String>();
			List<com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag> tags = renderResponse
					.getTags();
			for (com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag tag : tags) {
				ContentTag sourceTag = sourcePage.getContentTag(tag
						.getTagname());
				ContentTag newTag = page.getContent().addContentTag(
						ObjectTransformer.getInt(sourceTag.getConstruct()
								.getId(), -1));
				newTag.setEnabled(true);
				nameTranslation.put(sourceTag.getName(), newTag.getName());
			}
			// iterate again, copy the values (and transform tag references)
			for (com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag tag : tags) {
				String oldName = tag.getTagname();
				String newName = nameTranslation.get(oldName);
				ContentTag sourceTag = sourcePage.getContentTag(oldName);
				ContentTag newTag = page.getContentTag(newName);
				ValueList sourceValues = sourceTag.getValues();
				ValueList newValues = newTag.getValues();
				for (Value newValue : newValues) {
					Value sourceValue = sourceValues.getByPartId(newValue.getPartId());
					if (sourceValue != null) {
						newValue.copyFrom(sourceValue);
						String valueText = newValue.getValueText();
						if (!ObjectTransformer.isEmpty(valueText)) {
							newValue.setValueText(transformTagReferences(valueText, nameTranslation));
						}
					}
				}
			}

			// save the page
			page.save(false);

			// reload page (to get readonly instance)
			page = TransactionManager.getCurrentTransaction().getObject(page);

			Tag newTag = page.getContentTag(nameTranslation.get(tagname));
			return new TagCreateResponse(null, new ResponseInfo(ResponseCode.OK, "created new tag {"
					+ newTag.getName() + "} in page with id: " + page.getId()),
					ModelBuilder.getTag(newTag, false));
		} else {
			throw new NodeException(renderResponse.getResponseInfo().getResponseMessage());
		}
	}

	/**
	 * Get the construct ID from either the request or the given values
	 *
	 * @param request     tag create request
	 * @param constructId cosntruct ID
	 * @param keyword     keyword
	 * @return construct ID (never null)
	 * @throws NodeException if constructID could not be determined or conflicting data were given
	 */
	protected Integer getConstructId(TagCreateRequest request, Integer constructId, String keyword)
			throws NodeException {
		if (request.getConstructId() != null) {
			constructId = request.getConstructId();
		}
		if (request.getKeyword() != null) {
			keyword = request.getKeyword();
		}

		// If no constructId has been specified we try to get it by the passed
		// keyword
		if (constructId == null) {
			if (keyword == null) {
				throw new NodeException("Either a constructId or a keyword must be passed");
			} else {
				constructId = getConstructIdByKeyword(keyword);
			}
		} else if (keyword != null) {
			// If constructId and keyword passed throw an error
			throw new NodeException("Both constructId and keyword can not be passed");
		}

		return constructId;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#createTag(java.lang.String,
	 * java.lang.Integer, java.lang.String,
	 * com.gentics.contentnode.rest.model.request.ContentTagCreateRequest)
	 */
	@POST
	@Path("/newtag/{id}")
	public TagCreateResponse createTag(@PathParam("id") String id,
			@QueryParam("constructId") Integer constructId, @QueryParam("keyword") String keyword,
			ContentTagCreateRequest request) {
		try {
			if (request.getCopyPageId() == null && request.getCopyTagname() == null) {
				constructId = getConstructId(request, constructId, keyword);

				return createTagFromConstruct(id, constructId, request.getMagicValue());
			} else {
				// copy the given tag
				return copyTag(id, request.getCopyPageId(), request.getCopyTagname());
			}
		} catch (EntityNotFoundException e) {
			return new TagCreateResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new TagCreateResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (ReadOnlyException e) {
			return new TagCreateResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (NodeException e) {
			logger.error("Error while creating tag in page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new TagCreateResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while creating tag in page " + id + ": " + e.getLocalizedMessage()), null);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.PageResource#createTags(java.lang.String, com.gentics.contentnode.rest.model.request.MultiTagCreateRequest)
	 */
	@Override
	@POST
	@Path("/newtags/{id}")
	public MultiTagCreateResponse createTags(@PathParam("id") String id,
			MultiTagCreateRequest request) {
		try (AutoCommit ac = new AutoCommit()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Map<String, CreatedTag> created = new HashMap<String, CreatedTag>();

			if (ObjectTransformer.isEmpty(request.getCreate())) {
				return new MultiTagCreateResponse(null,
						new ResponseInfo(ResponseCode.INVALIDDATA, "Cannot create tags without data"));
			}

			@SuppressWarnings("unchecked")
			Collection<String> magicPartNames = ObjectTransformer.getCollection(
					t.getNodeConfig().getDefaultPreferences().getPropertyObject("magic_part_names"),
					Collections.emptyList());

			// get the page
			Page page = getLockedPage(id, PermHandler.ObjectPermission.edit);

			StringBuilder template = new StringBuilder();
			for (Map.Entry<String, TagCreateRequest> entry : request.getCreate().entrySet()) {
				Integer constructId = getConstructId(entry.getValue(), null, null);
				String magicValue = entry.getValue().getMagicValue();

				// add a contenttag
				ContentTag newTag = page.getContent().addContentTag(constructId);

				// enable the tag
				newTag.setEnabled(true);

				// honor the magic value, if the feature is activated
				if (!ObjectTransformer.isEmpty(magicValue) && t.getNodeConfig().getDefaultPreferences()
						.getFeature("magic_part_value")) {
					ValueList values = newTag.getValues();
					for (Value value : values) {
						if (magicPartNames.contains(value.getPart().getKeyname())) {
							value.setValueText(magicValue);
						}
					}
				}

				// add to created tag
				created.put(entry.getKey(), new CreatedTag(ModelBuilder.getTag(newTag, false), ""));

				// add a reference to the tag, enclosed with unique markers for start and end, so we can later find the rendered tag in the output
				template.append("@@GtxTagStart-").append(entry.getKey()).append("@@<node ")
						.append(newTag.getName()).append(">@@GtxTagEnd-")
						.append(entry.getKey()).append("@@");
			}

			page.save(false);

			// render all tags
			RenderType renderType = createRenderType(LinksType.backend, RenderType.EM_ALOHA,
					t.getSessionId(), t.getNodeConfig().getDefaultPreferences(), false);
			t.setRenderType(renderType);
			RenderResult renderResult = new RenderResult();
			t.setRenderResult(renderResult);
			String renderedAll = createResponseContent(template.toString(), page, renderResult,
					renderType, null);

			// for every tag, find the rendered content between the unique markers
			for (Map.Entry<String, CreatedTag> entry : created.entrySet()) {
				String startPattern = "@@GtxTagStart-" + entry.getKey() + "@@";
				String endPattern = "@@GtxTagEnd-" + entry.getKey() + "@@";
				int startPos = renderedAll.indexOf(startPattern);
				int endPos = renderedAll.indexOf(endPattern);
				if (startPos < 0 || endPos < 0) {
					throw new NodeException("Could not find rendered tag");
				}
				CreatedTag createdTag = entry.getValue();
				createdTag.setHtml(renderedAll.substring(startPos + startPattern.length(), endPos));
				ContentTag tag = page.getContentTag(createdTag.getTag().getName());
				// also set the tag ID (was not set before, because the page was not yet saved)
				createdTag.getTag().setId(ObjectTransformer.getInt(tag.getId(), 0));
			}

			ac.success();

			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK,
					"created new tags in page with id: " + page.getId());

			return new MultiTagCreateResponse(null, responseInfo, created, getTags(renderResult));
		} catch (EntityNotFoundException e) {
			return new MultiTagCreateResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new MultiTagCreateResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new MultiTagCreateResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while creating tags in page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new MultiTagCreateResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while creating tag in page " + id + ": " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#restoreVersion(java.lang.String, java.lang.Integer)
	 */
	@POST
	@Path("/restore/{id}")
	public PageLoadResponse restoreVersion(@PathParam("id") String id,
			@QueryParam("version") Integer versionTimestamp) {
		Transaction t = getTransaction();
		Page page = null;

		// get the page
		try {
			page = getLockedPage(id, PermHandler.ObjectPermission.edit);

			NodeObjectVersion[] versions = page.getVersions();
			NodeObjectVersion toRestore = null;
			for (int i = 0; i < versions.length; i++) {
				if (versions[i].getDate().getTimestamp().equals(versionTimestamp)) {
					toRestore = versions[i];
				}
			}

			// version to be restored was not found
			if (toRestore == null) {
				CNI18nString message = new CNI18nString("rest.general.insufficientdata");
				message.addParameters(new String[]{ObjectTransformer.getString(id, null),
						ObjectTransformer.getString(versionTimestamp, null)});
				return new PageLoadResponse(new Message(Type.WARNING, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
								"Could not find specified version {" + versionTimestamp
										+ "} of page " + id), null);
			}

			// restore the page (in the database)
			page.restoreVersion(toRestore, false);

			// commit the transaction
			t.commit(false);

			// transform the page into a rest page
			com.gentics.contentnode.rest.model.Page restPage = getPage(page,
					Arrays.asList(Reference.CONTENT_TAGS, Reference.OBJECT_TAGS), true);

			I18nString message = new CNI18nString("page.restore.success");

			return new PageLoadResponse(new Message(Type.SUCCESS, message.toString()),
					new ResponseInfo(ResponseCode.OK, "Successfully restored version {"
							+ versionTimestamp + "} of page with id { " + id + " }"), restPage);
		} catch (EntityNotFoundException e) {
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (ReadOnlyException e) {
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (NodeException e) {
			logger.error("Error while restoring version of page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageLoadResponse(
					new Message(Message.Type.CRITICAL, message.toString()), new ResponseInfo(
					ResponseCode.FAILURE, "Error while restoring version of page " + id
					+ ": " + e.getLocalizedMessage()), null);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#restore(java.lang.String,
	 * java.lang.String, java.lang.Integer)
	 */
	@POST
	@Path("/restore/{pageid}/{tag}")
	public TagListResponse restoreTag(@PathParam("pageid") String pageId,
			@PathParam("tag") String tag, @QueryParam("version") Integer versionTimestamp) {
		Transaction t = getTransaction();
		Page page = null;

		// get the page
		try {
			page = getLockedPage(pageId, PermHandler.ObjectPermission.edit);

			// get tag
			ContentTag tagToRestore = page.getContentTag(tag);
			Integer tagId = ObjectTransformer.getInteger(tag, null);
			if (tagToRestore == null && tagId != null) {
				Map<String, ? extends Tag> tags = page.getContent().getTags();
				for (Tag tempTag : tags.values()) {
					if (!(tempTag instanceof ContentTag)) {
						continue;
					}
					if (ObjectTransformer.equals(tagId, tempTag.getId())) {
						tagToRestore = (ContentTag) tempTag;
						break;
					}
				}
			}

			if (tagToRestore == null) {
				throw new EntityNotFoundException("Could not find tag " + tag + " in " + page,
						"page.tag.notfound", Arrays.asList(tag,
						ObjectTransformer.getString(page.getId(), "")));
			}

			// restore
			page.restoreTagVersion(tagToRestore, versionTimestamp);

			// commit the transaction
			t.commit(false);

			// build response
			tagToRestore = t.getObject(ContentTag.class, tagToRestore.getId());
			I18nString message = new CNI18nString("page.restore.success");
			TagListResponse response = new TagListResponse(new Message(Type.SUCCESS, message.toString()),
					new ResponseInfo(ResponseCode.OK,
							"Successfully restored version {" + versionTimestamp + "} of tag {" + tag
									+ "} in page { " + pageId + " }"));

			response.setTags(Arrays.asList(ModelBuilder.getTag(tagToRestore, false)));
			return response;
		} catch (EntityNotFoundException e) {
			return new TagListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new TagListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new TagListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while restoring version of page " + pageId, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new TagListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while restoring version of page " + pageId + ": " + e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#translate(java.lang.Integer, java.lang.String, boolean)
	 */
	@POST
	@Path("/translate/{id}")
	public PageLoadResponse translate(final @PathParam("id") Integer id,
			final @QueryParam("language") String languageCode,
			final @QueryParam("locked") @DefaultValue("true") boolean locked,
			final @QueryParam("channelId") @DefaultValue("0") Integer channelId) {
		try {
			// get the page in a new transaction
			Page page = TransactionManager.execute(new ReturnValueExecutable<Page>() {
				public Page execute() throws NodeException {
					return getPage(Integer.toString(id));
				}
			});
			Integer lockKey = ObjectTransformer.getInteger(page.getContentsetId(), 0);

			// create the translation (synchronized for the contentset_id and in a new transaction)
			return translateLock.execute(lockKey, new ReturnValueLockCallback<PageLoadResponse>() {
				/* (non-Javadoc)
				 * @see de.jkeylockmanager.manager.ReturnValueLockCallback#doInLock()
				 */
				public PageLoadResponse doInLock() {
					Transaction oldTransaction = getTransaction();

					try {
						// The TransactionLockManager creates a copy of the current transaction.
						// In order to use this new transaction, we have to set it here
						transaction = TransactionManager.getCurrentTransaction();

						return translate(id, languageCode, locked, channelId, true);
					} catch (TransactionException e) {
						throw new WebApplicationException(new Exception("Could not get transaction."));
					} finally {
						transaction = oldTransaction;
					}
				}
			});
		} catch (EntityNotFoundException e) {
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (Exception e) {
			logger.error("Error while translating page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while translating page " + id + ": " + e.getLocalizedMessage()), null);
		}
	}

	/**
	 * Translate the page into the given language. When the language variant of the page exists, it is
	 * just locked and returned, otherwise the page is copied into the language variant and returned.
	 * This method fails, if the requested language is not available for the node of the page or the
	 * user has no permission to create/edit the given language variant
	 *
	 * @param id                id of the page to translate
	 * @param languageCode      code of the language into which the page shall be translated
	 * @param locked            true if the translation shall be locked, false if not
	 * @param channelId         for multichannelling, specify channel in which to create page (can be
	 *                          0 or equal to node ID to be ignored)
	 * @param requirePermission if false, the permission check on the page to translate is skipped
	 * @return page load response
	 */
	private PageLoadResponse translate(Integer id, String languageCode, boolean locked,
			Integer channelId, boolean requirePermission) throws TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean readOnly = !locked;

		if (channelId == null) {
			channelId = 0;
		}

		try {
			var translationService = new LanguageVariantService();
			var languageVariant = translationService.translate(id, languageCode, locked, channelId,
					requirePermission);

			// return the language variant
			com.gentics.contentnode.rest.model.Page restPage = getPage(languageVariant,
					Arrays.asList(Reference.CONTENT_TAGS, Reference.OBJECT_TAGS_VISIBLE), readOnly);

			PageLoadResponse response = new PageLoadResponse();
			response.setPage(restPage);

			var responseMessage = "Translated page with id { " + id + " } successfully";
			if (languageVariant.getChannel() != null) {
				responseMessage += " in channel {" + languageVariant.getChannel().getId() + "}";
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, responseMessage));

			return response;

		} catch (EntityNotFoundException e) {
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()), null);
		} catch (Exception e) {
			logger.error("Error while translating page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageLoadResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while translating page " + id + ": " + e.getLocalizedMessage()), null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#workflowRevoke(java.lang.Integer)
	 */
	@POST
	@Path("/workflow/revoke/{id}")
	public GenericResponse workflowRevoke(@PathParam("id") Integer id) {
		Transaction t = getTransaction();
		Page page = null;

		try {
			SystemUser user = (SystemUser) t.getObject(SystemUser.class, t.getUserId());

			// get the page and check whether it is currently locked
			page = getPage(id, ObjectPermission.view);
			if (page.getContent().isLocked()) {
				throw new ReadOnlyException(
						"Could revoke page from workflow, since it is locked for another user",
						"page.readonly.locked", I18NHelper.getName(page));
			}

			// get the workflow
			PublishWorkflow workflow = page.getWorkflow();
			if (workflow == null) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");
				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND,
								"Could not find workflow of page " + id));
			}

			// get the current step of the workflow
			PublishWorkflowStep currentStep = workflow.getCurrentStep();

			// check if the page was modified while being in this step
			if (currentStep.isPageModified() || !currentStep.getCreator().equals(user)) {
				I18nString message = new CNI18nString("page.workflow.irrevokable");
				return new GenericResponse(
						new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.PERMISSION,
								"Could not revoke last workflow step, because the page was modified"));
			}

			// if there is only one step, remove the workflow, otherwise remove the current step
			if (workflow.getSteps().size() == 1) {
				workflow.delete();
			} else {
				workflow.revokeStep();
				workflow.save();
			}

			I18nString message = new CNI18nString("page.workflow.revoke.success");
			return new GenericResponse(new Message(Type.SUCCESS, message.toString()),
					new ResponseInfo(ResponseCode.OK, "Successfully revoked last step of page "
							+ id));
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.FAILURE, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while revoking workflow of page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while revoking workflow of page " + id + ": "
									+ e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#workflowDecline(java.lang.String, com.gentics.contentnode.rest.model.request.WorkflowRequest)
	 */
	@POST
	@Path("/workflow/decline/{id}")
	public GenericResponse workflowDecline(@PathParam("id") String id, WorkflowRequest request) {
		Transaction t = getTransaction();
		Page page = null;

		try {
			// get the page and lock it TODO what kind of permissions do we need?
			page = getLockedPage(id, ObjectPermission.view);

			// get the workflow
			PublishWorkflow workflow = page.getWorkflow();
			if (workflow == null) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");
				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND,
								"Could not find workflow of page " + id));
			}

			if (request.isDelete()) {
				// TODO inform all users watching
				workflow.delete();
				I18nString message = new CNI18nString("page.workflow.delete.success");
				return new GenericResponse(new Message(Type.SUCCESS, message.toString()),
						new ResponseInfo(ResponseCode.OK,
								"Successfully removed the workflow of page " + id));
			} else {
				// get the group
				UserGroup group = (UserGroup) t.getObject(UserGroup.class, request.getGroup());

				if (group == null) {
					logger.error("Error while changing workflow of page: invalid group id {"
							+ request.getGroup() + "} given.");
					I18nString message = new CNI18nString("rest.general.error");
					return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.INVALIDDATA,
									"Error while changing workflow of page: invalid group id {"
											+ request.getGroup() + "} given."));
				}

				// TODO check whether the group is visible to the user

				// add a step and assign to the given group
				workflow.addStep(request.getMessage(), group);
				workflow.save();

				I18nString message = new CNI18nString("page.workflow.decline.success");
				return new GenericResponse(new Message(Type.SUCCESS, message.toString()),
						new ResponseInfo(ResponseCode.OK,
								"Successfully updated the workflow of page " + id));
			}
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.FAILURE, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while modifying workflow of page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while modifying workflow of page " + id + ": "
									+ e.getLocalizedMessage()));
		} finally {
			if (page != null) {
				try {
					page.unlock();
				} catch (NodeException e) {
					logger.error("Error while unlocking " + page);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.gentics.contentnode.rest.api.PageResource#getTotalPageUsage(java.util
	 * .List, java.lang.Integer)
	 */
	@GET
	@Path("/usage/total")
	@Override
	public TotalUsageResponse getTotalPageUsage(@QueryParam("id") List<Integer> pageIds,
			@QueryParam("nodeId") Integer nodeId) {
		if (ObjectTransformer.isEmpty(pageIds)) {
			return new TotalUsageResponse(null, new ResponseInfo(ResponseCode.OK,
					"Successfully fetched total usage information using 0 pages"));
		}
		try {
			TotalUsageResponse response = new TotalUsageResponse();
			Map<Integer, Integer> masterMap = mapMasterPageIds(pageIds);
			for (Entry<Integer, Integer> entry : masterMap.entrySet()) {
				int masterPageId = entry.getKey();
				int originalPageId = entry.getValue();

				TotalUsageInfo info = new TotalUsageInfo();
				Set<Integer> referencingPageIds = MiscUtils.getPageUsageIds(Arrays.asList(masterPageId),
						Page.TYPE_PAGE, PageUsage.GENERAL, nodeId);
				Set<Integer> pageVariantIds = MiscUtils.getPageUsageIds(Arrays.asList(masterPageId),
						Page.TYPE_PAGE, PageUsage.VARIANT, nodeId);
				Set<Integer> pageTagIds = MiscUtils.getPageUsageIds(Arrays.asList(masterPageId),
						Page.TYPE_PAGE, PageUsage.TAG, nodeId);
				Set<Integer> usingTemplateIds = MiscUtils.getTemplateUsageIds(Arrays.asList(masterPageId),
						Page.TYPE_PAGE, nodeId);
				info.setTotal(referencingPageIds.size() + pageVariantIds.size() + pageTagIds.size()
						+ usingTemplateIds.size());
				info.setPages(info.getTotal());
				response.getInfos().put(originalPageId, info);
			}
			response.setResponseInfo(
					new ResponseInfo(ResponseCode.OK, "Successfully fetched total usage information"));
			return response;
		} catch (Exception e) {
			logger.error("Error while getting total usage info for " + pageIds.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new TotalUsageResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting total usage info for " + pageIds.size() + " pages"
									+ e.getLocalizedMessage()));
		}
	}

	/**
	 * For every page in the list, get the id of the master page (or the page itself, if it is a
	 * master page or multichannelling is not activated)
	 *
	 * @param pageId list of page ids
	 * @return map of master page ids to the given ids
	 * @throws NodeException
	 */
	protected Map<Integer, Integer> mapMasterPageIds(List<Integer> pageId) throws NodeException {
		Transaction t = getTransaction();
		if (!t.getNodeConfig().getDefaultPreferences().isFeature(
				Feature.MULTICHANNELLING)) {
			return pageId.stream().collect(Collectors.toMap(java.util.function.Function.identity(),
					java.util.function.Function.identity()));
		}
		List<Page> pages = t.getObjects(Page.class, pageId);
		Map<Integer, Integer> masterMap = new HashMap<>(pageId.size());
		for (Page page : pages) {
			Integer id = ObjectTransformer.getInteger(page.getMaster().getId(), null);
			if (id != null && !masterMap.containsKey(id)) {
				masterMap.put(id, page.getId());
			}
		}
		return masterMap;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#getPagetagUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean, boolean, boolean, boolean)
	 */
	@GET
	@Path("/usage/tag")
	public PageUsageListResponse getPagetagUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new PageUsageListResponse(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully fetched pages using 0 pages"), null, 0, 0);
		}
		try {
			pageId = getMasterPageIds(pageId);
			return MiscUtils.getPageUsage(skipCount, maxItems, sortBy, sortOrder, Page.TYPE_PAGE, pageId,
					PageUsage.TAG, nodeId, returnPages, pageModel);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#getVariantsUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean, boolean, boolean, boolean)
	 */
	@GET
	@Path("/usage/variant")
	public PageUsageListResponse getVariantsUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new PageUsageListResponse(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully fetched pages using 0 pages"), null, 0, 0);
		}
		try {
			pageId = getMasterPageIds(pageId);
			return MiscUtils.getPageUsage(skipCount, maxItems, sortBy, sortOrder, Page.TYPE_PAGE, pageId,
					PageUsage.VARIANT, nodeId, returnPages, pageModel);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#getPageUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean, boolean, boolean, boolean)
	 */
	@GET
	@Path("/usage/page")
	public PageUsageListResponse getPageUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new PageUsageListResponse(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully fetched pages using 0 pages"), null, 0, 0);
		}

		try {
			pageId = getMasterPageIds(pageId);
			return MiscUtils.getPageUsage(skipCount, maxItems, sortBy, sortOrder, Page.TYPE_PAGE, pageId,
					PageUsage.GENERAL, nodeId, returnPages, pageModel);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#getTemplateUsageInfo(java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, java.util.List, java.lang.Integer, boolean)
	 */
	@GET
	@Path("/usage/template")
	public TemplateUsageListResponse getTemplateUsageInfo(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("templates") @DefaultValue("true") boolean returnTemplates) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new TemplateUsageListResponse(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully fetched templates using 0 pages"), null, 0, 0);
		}

		try {
			pageId = getMasterPageIds(pageId);
			return MiscUtils.getTemplateUsage(skipCount, maxItems, sortBy, sortOrder, Page.TYPE_PAGE,
					pageId, nodeId, returnTemplates);
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new TemplateUsageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0, 0);
		}
	}

	@GET
	@Path("/usage/linkedPage")
	public ReferencedPagesListResponse getLinkedPages(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new ReferencedPagesListResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched objects linked by 0 pages"), null,
					0, 0);
		}
		try {
			Set<Page> pages = new HashSet<>();
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				for (Page page : t.getObjects(Page.class, pageId)) {
					for (ContentTag tag : page.getContent().getContentTags().values()) {
						for (Value value : tag.getValues()) {
							PartType partType = value.getPartType();
							if (partType instanceof PageURLPartType) {
								Page target = ((PageURLPartType) partType).getTargetPage();
								if (target != null) {
									pages.add(target);
								}
							}
						}
					}
				}

				// filter by permission
				int withoutPermission = 0;
				List<com.gentics.contentnode.rest.model.Page> items = new ArrayList<>();

				for (Iterator<Page> i = pages.iterator(); i.hasNext(); ) {
					Page page = i.next();
					if (PermHandler.ObjectPermission.view.checkObject(page)) {
						items.add(ModelBuilder.getPage(page, Arrays.asList(Reference.TEMPLATE)));
					} else {
						withoutPermission++;
					}
				}

				if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)
						&& !ObjectTransformer.isEmpty(items)) {
					Collections.sort(items, new ItemComparator(sortBy, sortOrder));
				}

				// create the response
				ReferencedPagesListResponse response = new ReferencedPagesListResponse(null,
						new ResponseInfo(ResponseCode.OK, ""), null, items.size(), withoutPermission);

				response.setNumItems(items.size());
				response.setHasMoreItems(maxItems >= 0 && (items.size() > skipCount + maxItems));
				reduceList(items, skipCount, maxItems);
				response.setPages(items);

				return response;
			}
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new ReferencedPagesListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0,
					0);
		}
	}

	@GET
	@Path("/usage/linkedFile")
	public ReferencedFilesListResponse getLinkedFiles(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new ReferencedFilesListResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched objects linked by 0 pages"), null,
					0, 0);
		}
		try {
			Set<File> files = new HashSet<>();
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				for (Page page : t.getObjects(Page.class, pageId)) {
					for (ContentTag tag : page.getContent().getContentTags().values()) {
						for (Value value : tag.getValues()) {
							PartType partType = value.getPartType();
							if (partType instanceof FileURLPartType) {
								File target = ((FileURLPartType) partType).getTargetFile();
								if (target != null) {
									files.add(target);
								}
							}
						}
					}
				}

				// filter by permission
				int withoutPermission = 0;
				List<com.gentics.contentnode.rest.model.File> items = new ArrayList<>();

				for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
					File file = i.next();
					if (PermHandler.ObjectPermission.view.checkObject(file)) {
						items.add(ModelBuilder.getFile(file, Collections.emptyList()));
					} else {
						withoutPermission++;
					}
				}

				if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)
						&& !ObjectTransformer.isEmpty(items)) {
					Collections.sort(items, new ItemComparator(sortBy, sortOrder));
				}

				// create the response
				ReferencedFilesListResponse response = new ReferencedFilesListResponse(null,
						new ResponseInfo(ResponseCode.OK, ""), null, items.size(), withoutPermission);

				response.setNumItems(items.size());
				response.setHasMoreItems(maxItems >= 0 && (items.size() > skipCount + maxItems));
				reduceList(items, skipCount, maxItems);
				response.setFiles(items);

				return response;
			}
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new ReferencedFilesListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0,
					0);
		}
	}

	@GET
	@Path("/usage/linkedImage")
	public ReferencedFilesListResponse getLinkedImages(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder,
			@QueryParam("id") List<Integer> pageId,
			@QueryParam("nodeId") Integer nodeId) {
		if (ObjectTransformer.isEmpty(pageId)) {
			return new ReferencedFilesListResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched objects linked by 0 pages"), null,
					0, 0);
		}
		try {
			Set<ImageFile> images = new HashSet<>();
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				Transaction t = TransactionManager.getCurrentTransaction();
				for (Page page : t.getObjects(Page.class, pageId)) {
					for (ContentTag tag : page.getContent().getContentTags().values()) {
						for (Value value : tag.getValues()) {
							PartType partType = value.getPartType();
							if (partType instanceof ImageURLPartType) {
								ImageFile target = ((ImageURLPartType) partType).getTargetImage();
								if (target != null) {
									images.add(target);
								}
							}
						}
					}
				}

				// filter by permission
				int withoutPermission = 0;
				List<com.gentics.contentnode.rest.model.File> items = new ArrayList<>();

				for (Iterator<ImageFile> i = images.iterator(); i.hasNext(); ) {
					ImageFile image = i.next();
					if (PermHandler.ObjectPermission.view.checkObject(image)) {
						items.add(ModelBuilder.getImage(image, (Collection<Reference>) null));
					} else {
						withoutPermission++;
					}
				}

				if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)
						&& !ObjectTransformer.isEmpty(items)) {
					Collections.sort(items, new ItemComparator(sortBy, sortOrder));
				}

				// create the response
				ReferencedFilesListResponse response = new ReferencedFilesListResponse(null,
						new ResponseInfo(ResponseCode.OK, ""), null, items.size(), withoutPermission);

				response.setNumItems(items.size());
				response.setHasMoreItems(maxItems >= 0 && (items.size() > skipCount + maxItems));
				reduceList(items, skipCount, maxItems);
				response.setFiles(items);

				return response;
			}
		} catch (Exception e) {
			logger.error("Error while getting usage info for " + pageId.size() + " pages", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new ReferencedFilesListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while getting usage info for " + pageId.size() + " pages"
									+ e.getLocalizedMessage()), null, 0,
					0);
		}
	}

	@Override
	@POST
	@Path("/takeOffline/{id}")
	public GenericResponse takeOffline(@PathParam("id") String id, PageOfflineRequest request) {
		try (AutoCommit ac = new AutoCommit()) {
			Page page = getPage(id);

			Node node = page.getChannel();
			if (node == null) {
				node = page.getOwningNode();
			}

			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				page = getPage(id, ObjectPermission.view, ObjectPermission.edit);
			}

			PageOfflineResult result = null;

			// Actually take the page offline
			if (request != null) {
				if (request.isAlllang()) {
					List<Page> variants = null;
					try (ChannelTrx cTrx = new ChannelTrx(node)) {
						variants = new ArrayList<>(page.getLanguages());
						PermFilter.get(ObjectPermission.edit).filter(variants);
					}
					if (!ObjectTransformer.isEmpty(variants)) {
						for (Page variant : variants) {
							PageOfflineResult variantResult = takeOfflineOrQueue(variant, request.getAt());
							if (variant.equals(page)) {
								result = variantResult;
							}
						}
					} else {
						result = takeOfflineOrQueue(page, request.getAt());
					}
				} else {
					result = takeOfflineOrQueue(page, request.getAt());
				}
			} else {
				result = takeOfflineOrQueue(page, 0);
			}
			page.unlock();

			// Commit the transaction now to handle instant publishing
			Transaction t = TransactionManager.getCurrentTransaction();
			t.commit(false);
			Result instantPublishingResult = t.getInstantPublishingResult(Page.TYPE_PAGE, page.getId());

			GenericResponse response = new GenericResponse();
			Message message = null;
			switch (result) {
			case OFFLINE:
				if (instantPublishingResult != null && instantPublishingResult.status() == ResultStatus.success) {
					message = new Message(Type.SUCCESS, I18NHelper.get("page.instantoffline.success", I18NHelper.getName(page)));
				} else {
					message = new Message(Type.SUCCESS, I18NHelper.get("page.offline.success", I18NHelper.getName(page)));
				}
				break;
			case OFFLINE_AT:
				message = new Message(Type.SUCCESS, I18NHelper.get("page.offlineat.success", I18NHelper.getName(page)));
				break;
			case QUEUED:
				message = new Message(Type.SUCCESS, I18NHelper.get("page.offline.workflow", I18NHelper.getName(page)));
				break;
			case QUEUED_AT:
				message = new Message(Type.SUCCESS, I18NHelper.get("page.offlineat.workflow", I18NHelper.getName(page)));
				break;
			}

			MiscUtils.addMessage(instantPublishingResult, response);

			ac.success();

			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK, "The following page has been taken offline : " + page.getId());
			response.addMessage(message);
			response.setResponseInfo(responseInfo);
			return response;
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.PERMISSION, e.getMessage()));
		} catch (ReadOnlyException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e
					.getLocalizedMessage()), new ResponseInfo(
					ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while taking offline page " + id, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL,
					message.toString()), new ResponseInfo(ResponseCode.FAILURE,
					"Error while taking offline page " + id + ": "
							+ e.getLocalizedMessage()));
		}
	}

	@GET
	@Path("/search")
	@Override
	public PageLoadResponse search(@DefaultValue("false") @QueryParam("update") boolean update,
			@QueryParam("template") @DefaultValue("false") boolean template,
			@QueryParam("folder") @DefaultValue("false") boolean folder,
			@QueryParam("langvars") @DefaultValue("false") boolean languageVariants,
			@QueryParam("pagevars") @DefaultValue("false") boolean pageVariants,
			@QueryParam("workflow") @DefaultValue("false") boolean workflow,
			@QueryParam("translationstatus") @DefaultValue("false") boolean translationStatus,
			@QueryParam("versioninfo") @DefaultValue("false") boolean versionInfo,
			@QueryParam("disinherited") @DefaultValue("false") boolean disinherited,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("liveUrl") String liveUrl) {

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("select page_id, node_id from publish where CONCAT(path, filename) = ?");
		if (nodeId != null) {
			queryBuilder.append(" and node_id = ?");
		}
		try {
			Pair<Integer, Integer> hit = DBUtils.select(queryBuilder.toString(),
					new DBUtils.PrepareStatement() {
						@Override
						public void prepare(PreparedStatement stmt) throws SQLException, NodeException {
							stmt.setString(1, liveUrl);
							if (nodeId != null) {
								stmt.setInt(2, nodeId);
							}
						}
					}, new HandleSelectResultSet<Pair<Integer, Integer>>() {
						@Override
						public Pair<Integer, Integer> handle(ResultSet res) throws SQLException, NodeException {
							if (res.next()) {
								return Pair.of(res.getInt("page_id"), res.getInt("node_id"));
							}
							return Pair.of(null, null);
						}
					});

			Integer pageId = hit.getLeft();

			if (pageId != null) {
				Integer foundNodeId = hit.getRight();
				return load(Integer.toString(pageId), update, template, folder, languageVariants,
						pageVariants, workflow, translationStatus, versionInfo, disinherited, false,
						foundNodeId, null);
			} else {
				I18nString message = new CNI18nString("page.notfound");
				message.setParameter("0", liveUrl);
				throw new EntityNotFoundException(message.toString());
			}

		} catch (EntityNotFoundException e) {
			return new PageLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()), null);
		} catch (NodeException e) {
			logger.error("Error while searching for page", e);
			I18nString m = new CNI18nString("rest.general.error");
			return new PageLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while searching for page: " + e.getLocalizedMessage()), null);
		}
	}

	/**
	 * For every page in the list, get the id of the master page (or the page itself, if it is a
	 * master page or multichannelling is not activated)
	 *
	 * @param pageId list of page ids
	 * @return list of master page ids
	 * @throws NodeException
	 */
	protected List<Integer> getMasterPageIds(List<Integer> pageId) throws NodeException {
		Transaction t = getTransaction();
		if (!t.getNodeConfig().getDefaultPreferences().isFeature(
				Feature.MULTICHANNELLING)) {
			return pageId;
		}
		List<Page> pages = t.getObjects(Page.class, pageId);
		List<Integer> newPageId = new Vector<Integer>(pageId.size());
		for (Page page : pages) {
			Integer id = ObjectTransformer.getInteger(page.getMaster().getId(), null);
			if (id != null && !newPageId.contains(id)) {
				newPageId.add(id);
			}
		}
		return newPageId;
	}

	/**
	 * Helper method to transform the given GCN Page into a REST Page
	 *
	 * @param gcnPage  GCN Page object
	 * @param fillRefs list of references to be filled (may be null)
	 * @param readOnly true when the REST Page shall be readonly, false if not
	 * @return REST Page or null if gcnPage was null
	 * @throws NodeException
	 */
	private com.gentics.contentnode.rest.model.Page getPage(Page gcnPage,
			Collection<Reference> fillRefs, boolean readOnly) throws NodeException {
		if (gcnPage == null) {
			return null;
		}
		// create the REST Page object
		com.gentics.contentnode.rest.model.Page restPage = ModelBuilder.getPage(gcnPage, fillRefs);
		// set the readonly flag
		restPage.setReadOnly(readOnly);

		return restPage;
	}

	/**
	 * Helper method to get the Construct id of a keyword
	 *
	 * @param keyword keyword of the construct
	 * @return Construct id
	 * @throws NodeException
	 */
	private Integer getConstructIdByKeyword(String keyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;
		Integer id = null;

		try {
			pst = t.prepareStatement("SELECT id FROM construct WHERE keyword = ? LIMIT 1");
			pst.setString(1, keyword);
			res = pst.executeQuery();

			if (!res.next()) {
				throw new NodeException("Couldn't find a matching Construct for the keyword" + keyword);
			}

			id = res.getInt("id");
		} catch (SQLException e) {
			throw new NodeException("Unable to get the Construct id for keyword " + keyword, e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}

		return id;
	}

	/**
	 * Get the page with given id, check whether the page exists. Check for given permissions for the
	 * current user.
	 *
	 * @param id    id of the page
	 * @param perms permissions to check
	 * @return page
	 * @throws NodeException                   when loading the page fails due to underlying error
	 * @throws EntityNotFoundException         when the page was not found
	 * @throws InsufficientPrivilegesException when the user doesn't have a requested permission on
	 *                                         the page
	 */
	@Deprecated
	protected Page getPage(Integer id, PermHandler.ObjectPermission... perms)
			throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		return getPage(Integer.toString(id), perms);
	}

	/**
	 * Prefix the given page with the given prefix. Make sure that no double slashes are created.
	 *
	 * @param prefix Prefix
	 * @param path   Path
	 * @return prefixed path
	 */
	protected String addPathPrefix(String prefix, String path) {
		if (prefix == null) {
			return path;
		} else if (path == null) {
			return prefix;
		}
		StringBuffer prefixedPath = new StringBuffer(prefix.length() + path.length());
		prefixedPath.append(prefix);
		if (prefix.endsWith("/") && path.startsWith("/")) {
			prefixedPath.append(path.substring(1));
		} else {
			prefixedPath.append(path);
		}

		return prefixedPath.toString();
	}

	/**
	 * Validates all properties of a rest page and its tags
	 *
	 * @param restPage Rest   page
	 * @param page     Node page - this is needed as reference as the rest page doesn't contain all
	 *                 required data
	 * @throws NodeException
	 */
	void sanitizeAndValidateRestPage(
			com.gentics.contentnode.rest.model.Page restPage, Page page) throws NodeException {
		if (!ValidationUtils.isValidationEnabled()) {
			// Return when validation is not enabled
			return;
		}

		// Make sure that all rest tags have a valid construct ID set (important for validation)
		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restPage.getTags();
		Map<String, ContentTag> contentTags = page.getContent().getContentTags();
		Map<String, ObjectTag> objectTags = page.getObjectTags();

		if (restTags != null) {
			for (Map.Entry<String, com.gentics.contentnode.rest.model.Tag> restTagEntry : restTags.entrySet()) {
				com.gentics.contentnode.rest.model.Tag restTag = restTagEntry.getValue();

				if (restTag.getConstructId() != null) {
					continue;
				}

				if (restTag.getName().length() == 0) {
					continue;
				}

				Tag referenceTag = null;
				if (restTag.getName().startsWith("object.")) {
					// Object property
					String realName = restTag.getName().substring(7);
					referenceTag = objectTags.get(realName);
				} else {
					// Content tag
					referenceTag = contentTags.get(restTag.getName());
				}

				if (referenceTag == null) {
					// ContentTag or object property not found
					continue;
				}

				restTag.setConstructId(referenceTag.getConstruct().getId());
			}
		}

		// And finally: Validate
		PageValidator validator = new PageValidatorAndErrorCollector(page.getFolder().getNode());
		validator.processRestPage(restPage, getTransaction());
		for (ValidationResult result : validator.getResults()) {
			if (result.hasErrors()) {
				throw new NodeException(ValidationUtils.formatValidationError(result));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.PageResource#autocomplete(String, int)
	 */
	@GET
	@Path("/autocomplete")
	@Produces(MediaType.TEXT_HTML)
	public String autocomplete(@QueryParam("q") String q,
			final @QueryParam("limit") @DefaultValue("15") int limit) {
		// no input data, no output
		if (ObjectTransformer.isEmpty(q)) {
			return "";
		}
		final String search = q.length() < 3 ? "" : ObjectTransformer.getString(q, "");
		final Integer searchId = ObjectTransformer.getInteger(q, null);

		// the input must either be at least 3 characters long, or must be a number
		if (ObjectTransformer.isEmpty(search) && searchId == null) {
			return "";
		}

		final Map<Integer, Map<Integer, Set<Integer>>> pageIdMap = new HashMap<Integer, Map<Integer, Set<Integer>>>();

		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			final PermHandler permHandler = t.getPermHandler();
			StringBuilder sql = new StringBuilder();
			sql.append(
					"SELECT p.id page_id, p.folder_id, f.node_id FROM page p, folder f WHERE p.deleted = 0 AND p.folder_id = f.id AND (");
			if (!ObjectTransformer.isEmpty(search)) {
				sql.append("p.name LIKE ?");
				if (searchId != null) {
					sql.append(" OR p.id = ?");
				}
			} else if (searchId != null) {
				sql.append("p.id = ?");
			}
			sql.append(")");
			if (logger.isDebugEnabled()) {
				logger.debug("Searching with " + sql.toString());
			}
			DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					int pCounter = 1;
					if (!ObjectTransformer.isEmpty(search)) {
						stmt.setString(pCounter++, "%" + search + "%"); // name LIKE ?
					}
					if (searchId != null) {
						stmt.setInt(pCounter++, searchId); // id = ?
					}
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						int nodeId = rs.getInt("node_id");
						int folderId = rs.getInt("folder_id");
						int pageId = rs.getInt("page_id");

						Map<Integer, Set<Integer>> folderMap = pageIdMap.get(nodeId);
						if (folderMap == null) {
							folderMap = new HashMap<Integer, Set<Integer>>();
							pageIdMap.put(nodeId, folderMap);
						}

						Set<Integer> pageIds = folderMap.get(folderId);
						if (pageIds == null) {
							pageIds = new HashSet<Integer>();
							folderMap.put(folderId, pageIds);
						}
						pageIds.add(pageId);
					}
				}
			});

			if (logger.isDebugEnabled()) {
				logger.debug("Found " + pageIdMap);
			}

			List<Node> nodes = t.getObjects(Node.class, pageIdMap.keySet());
			List<Node> allNodes = new ArrayList<Node>();
			for (Node node : nodes) {
				allNodes.add(node);
				allNodes.addAll(node.getAllChannels());
			}
			// sort by name
			Collections.sort(allNodes, new Comparator<Node>() {
				public int compare(Node o1, Node o2) {
					try {
						return StringUtils.mysqlLikeCompare(o1.getFolder().getName(), o2.getFolder().getName());
					} catch (NodeException e) {
						return 0;
					}
				}
			});

			if (logger.isDebugEnabled()) {
				logger.debug("Nodelist: " + allNodes);
			}

			StringBuilder out = new StringBuilder();

			int pageCounter = 0;
			for (Node node : allNodes) {
				try {
					// determine languages activated for the node and the default language (which may be null)
					List<ContentLanguage> nodeLanguages = node.getLanguages();
					ContentLanguage defaultLanguage = nodeLanguages.isEmpty() ? null : nodeLanguages.get(0);
					boolean newNode = true;
					t.setChannelId(node.getId());

					Map<Integer, Set<Integer>> folderMap = pageIdMap.get(node.getMaster().getId());
					List<Folder> folders = new ArrayList<Folder>(
							t.getObjects(Folder.class, folderMap.keySet()));
					Collections.sort(folders, new FolderComparator("name", "asc"));

					for (Folder folder : folders) {
						boolean newFolder = true;
						if (!permHandler.canView(folder, Page.class, null)) {
							continue;
						}
						Set<Integer> pageIds = folderMap.get(folder.getMaster().getId());
						List<Page> pages = new ArrayList<Page>(
								new HashSet<Page>(t.getObjects(Page.class, pageIds)));
						Collections.sort(pages, new PageComparator("name", "asc"));

						for (Page page : pages) {
							if (pageCounter >= limit) {
								break;
							}

							// when the page uses a language, that is not activated for its node, we check the language fallback
							ContentLanguage language = page.getLanguage();
							if (language != null) {
								if (!nodeLanguages.contains(language)) {
									// do language fallback for the default language of the node
									PageLanguageFallbackList fb = new PageLanguageFallbackList(defaultLanguage, node);
									// add all language variants of the page
									for (Page variant : page.getLanguageVariants(false)) {
										fb.addPage(variant);
									}

									// if the current page is not the fallback page, omit it
									if (!fb.getPages().contains(page)) {
										continue;
									}
								}
							}

							// check whether the page really matches (it may be
							// that the master page matched, but the channel
							// specific page does not match any more)
							boolean pageMatches = false;
							if (searchId != null && page.getId().equals(searchId)) {
								pageMatches = true;
							}
							if (!pageMatches && !ObjectTransformer.isEmpty(search) && page.getName().toLowerCase()
									.contains(search.toLowerCase())) {
								pageMatches = true;
							}

							// omit non matching pages
							if (!pageMatches) {
								continue;
							}

							if (newNode) {
								out.append("<div class=\"ac_node\">").append(node.getFolder().getName())
										.append("</div>");
								newNode = false;
							}
							if (newFolder) {
								out.append("<div class=\"ac_folder\">").append(folder.getName()).append("</div>");
								newFolder = false;
							}
							out.append("<div class=\"ac_page\" page_id=\"").append(page.getId())
									.append("\" node_id=\"").append(node.getId()).append("\">")
									.append(page.getName()).append("</div>\n");

							pageCounter++;
						}

						if (pageCounter >= limit) {
							break;
						}
					}
				} finally {
					t.resetChannel();
				}

				if (pageCounter >= limit) {
					break;
				}
			}

			return out.toString();
		} catch (Exception e) {
			logger.error("Error while doing autocomplete for " + q, e);
			return "";
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.gentics.contentnode.rest.api.PageResource#copy(PageCopyRequest, long)
	 */
	@POST
	@Path("/copy")
	public PageCopyResponse copy(final PageCopyRequest request,
			@QueryParam("wait") @DefaultValue("0") long waitMs) {
		GenericResponse response = Operator.execute(new CNI18nString("nodecopy_pages").toString(),
				waitMs, new Callable<GenericResponse>() {
					@Override
					public PageCopyResponse call() throws Exception {
						Transaction t = TransactionManager.getCurrentTransaction();

						boolean channelSet = setChannelToTransaction(request.getNodeId());
						try {
							PageCopyResponse response = new PageCopyResponse(null,
									new ResponseInfo(ResponseCode.OK, "Copied pages"));

							if (request.getTargetFolders().size() == 0) {
								CNI18nString message = new CNI18nString("page_copy.no_target_folder");
								return new PageCopyResponse(new Message(Message.Type.CRITICAL, message.toString()),
										new ResponseInfo(ResponseCode.FAILURE,
												"Error while copying pages: No target folder was specified."));
							}
							if (request.getSourcePageIds().size() == 0) {
								CNI18nString message = new CNI18nString("page_copy.no_source_pages");
								return new PageCopyResponse(new Message(Message.Type.CRITICAL, message.toString()),
										new ResponseInfo(ResponseCode.FAILURE,
												"Error while copying pages: No source pages were specified."));
							}
							// Copy each set of source pages to all target folders
							for (Integer sourcePageId : request.getSourcePageIds()) {
								Page nodeSourcePage = t.getObject(Page.class, sourcePageId);
								if (nodeSourcePage == null) {
									logger.error("Could not load page with id {" + sourcePageId + "}");
									CNI18nString message = new CNI18nString("page_copy.sourcepage_not_found");
									return new PageCopyResponse(
											new Message(Message.Type.CRITICAL, message.toString()),
											new ResponseInfo(ResponseCode.FAILURE, "Source page could not be loaded."));
								}

								// Now iterate over each target folder and copy the source page into
								// the folder
								for (TargetFolder targetFolder : request.getTargetFolders()) {

									CNI18nString genericMessage = new CNI18nString(
											"page_copy.generic_copy_not_possible");
									genericMessage.addParameter(I18NHelper.getLocation(nodeSourcePage));
									genericMessage.addParameter(I18NHelper.getLocation(targetFolder));

									// Load the target folder and invoke copy
									Folder currentTargetFolder = t.getObject(Folder.class, targetFolder.getId(), -1,
											false);
									if (currentTargetFolder == null) {
										CNI18nString message = new CNI18nString("page_copy.target_folder_not_found");
										rollbackTransaction();
										return new PageCopyResponse(new Message(Message.Type.CRITICAL,
												genericMessage.toString() + message.toString()),
												new ResponseInfo(ResponseCode.FAILURE,
														"Target folder could not be loaded."));
									}

									Integer targetChannelId = targetFolder.getChannelId();
									PageCopyOpResult result = nodeSourcePage.copyTo(request.getNodeId(),
											currentTargetFolder, request.isCreateCopy(), null, targetChannelId);
									if (!result.isOK()) {
										PageCopyResponse copyResponse = new PageCopyResponse(null,
												new ResponseInfo(ResponseCode.FAILURE,
														"Error during copy process. Please check the messages."));
										t.rollback(false);
										ModelBuilder.addMessagesFromOpResult(result, copyResponse);
										return copyResponse;
									}

									// Add info about copied pages to rest response
									for (PageCopyOpResultInfo copyInfo : result.getCopyInfos()) {
										try (ChannelTrx cTrx = new ChannelTrx(copyInfo.getTargetChannelId())) {
											response.getPages().add(ModelBuilder.getPage(copyInfo.getCreatedPageCopy(),
													(Collection<Reference>) null));
										}
										PageCopyResultInfo restCopyInfo = new PageCopyResultInfo();
										restCopyInfo.setNewPageId(
												ObjectTransformer.getInteger(copyInfo.getCreatedPageCopy().getId(), -1));
										restCopyInfo.setTargetFolderChannelId(
												ObjectTransformer.getInteger(copyInfo.getTargetChannelId(), -1));
										restCopyInfo.setTargetFolderId(
												ObjectTransformer.getInteger(copyInfo.getTargetFolder().getId(), -1));
										restCopyInfo.setNewPageId(
												ObjectTransformer.getInteger(copyInfo.getCreatedPageCopy().getId(), -1));
										response.getPageCopyMappings().add(restCopyInfo);
									}
								}
							}
							t.commit(false);
							return response;
						} finally {
							if (channelSet) {
								t.resetChannel();
							}
						}
					}
				});

		if (response instanceof PageCopyResponse) {
			return (PageCopyResponse) response;
		} else {
			PageCopyResponse pageCopyResponse = new PageCopyResponse();
			pageCopyResponse.setResponseInfo(response.getResponseInfo());
			pageCopyResponse.setMessages(response.getMessages());
			return pageCopyResponse;
		}
	}

	/**
	 * Move the given page to another folder
	 *
	 * @param id      page id
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move/{id}")
	public GenericResponse move(@PathParam("id") String id, ObjectMoveRequest request) {
		MultiObjectMoveRequest multiRequest = new MultiObjectMoveRequest();
		multiRequest.setFolderId(request.getFolderId());
		multiRequest.setNodeId(request.getNodeId());
		multiRequest.setIds(Arrays.asList(id));
		return move(multiRequest);
	}

	/**
	 * Move multiple pages to another folder
	 *
	 * @param request request
	 * @return generic response
	 */
	@POST
	@Path("/move")
	public GenericResponse move(MultiObjectMoveRequest request) {
		try (AutoCommit trx = new AutoCommit()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder target = t.getObject(Folder.class, request.getFolderId());

			for (String id : request.getIds()) {
				Page toMove = getPage(id);
				OpResult result = toMove.move(target, ObjectTransformer.getInt(request.getNodeId(), 0),
						ObjectTransformer.getBoolean(request.isAllLanguages(), true));
				switch (result.getStatus()) {
					case FAILURE:
						GenericResponse response = new GenericResponse();
						for (NodeMessage msg : result.getMessages()) {
							response.addMessage(ModelBuilder.getMessage(msg));
						}
						response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Error"));
						return response;
					case OK:
						// Nothing to be done.
				}
			}
			trx.success();
			return new GenericResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully moved pages"));
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			I18nString message = new CNI18nString("rest.general.error");
			return new GenericResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while moving pages: " + e.getLocalizedMessage()));
		}
	}

	@GET
	@Path("/pubqueue")
	public LegacyPageListResponse pubqueue(
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("search") String search,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder) {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<Page> pages = null;

			if (ObjectTransformer.isEmpty(search)) {
				// get all pages in queue
				pages = new ArrayList<>(t.getObjects(Page.class,
						DBUtils.select("SELECT id FROM page WHERE pub_queue != 0 OR off_queue != 0",
								DBUtils.IDS)));
			} else {
				// get pages in queue, filtered by search string
				String searchPattern = String.format("%%%s%%", search.toLowerCase());
				pages = new ArrayList<>(t.getObjects(Page.class,
						DBUtils.select(
								"SELECT id FROM page WHERE (pub_queue != 0 OR off_queue != 0) AND (page.id = ? OR LOWER(page.name) LIKE ? OR LOWER(page.filename) LIKE ? OR LOWER page.description LIKE ?)",
								new DBUtils.PrepareStatement() {
									@Override
									public void prepare(PreparedStatement stmt) throws SQLException, NodeException {
										stmt.setInt(1, ObjectTransformer.getInt(search, 0));
										stmt.setString(2, searchPattern);
										stmt.setString(3, searchPattern);
										stmt.setString(4, searchPattern);
									}
								}, DBUtils.IDS)));
			}

			// Filter by permission
			for (Iterator<? extends Page> i = pages.iterator(); i.hasNext(); ) {
				Page page = i.next();

				try (ChannelTrx channelTrx = new ChannelTrx(page.getChannel())) {
					if (!t.canPublish(page)) {
						i.remove();
					}
				}
			}

			// optionally sort the list
			if (!ObjectTransformer.isEmpty(sortBy) && !ObjectTransformer.isEmpty(sortOrder)) {
				Collections.sort(pages, new PageComparator(sortBy, sortOrder));
			}

			// create the response
			LegacyPageListResponse response = new LegacyPageListResponse();

			response.setHasMoreItems(maxItems >= 0 && (pages.size() > skipCount + maxItems));
			response.setNumItems(pages.size());
			reduceList(pages, skipCount, maxItems);

			List<com.gentics.contentnode.rest.model.Page> restPages = new ArrayList<com.gentics.contentnode.rest.model.Page>(
					pages.size());
			for (Page page : pages) {
				try {
					restPages.add(ModelBuilder.getPage(page, (Collection<Reference>) null));
				} catch (InconsistentDataException e) {
					logger.error("Error while fetching page {" + page.getId() + "}", e);
				}
			}
			response.setPages(restPages);

			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully loaded pages"));
			return response;

		} catch (NodeException e) {
			logger.error("Error while loading publish queue", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new LegacyPageListResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while loading publish queue: " + e.getLocalizedMessage()));
		}
	}

	@Override
	@POST
	@Path("/pubqueue/approve")
	public GenericResponse pubqueueApprove(MultiPubqueueApproveRequest request,
			@QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		return Operator.execute(new CNI18nString("approve_publication").toString(), waitMs, () -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			MessageSender messageSender = new MessageSender();
			t.addTransactional(messageSender);

			List<Page> pages = new ArrayList<>();

			try {
				for (int id : request.getIds()) {
					Page page = getPage(String.valueOf(id));
					try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
						page = getLockedPage(String.valueOf(id), ObjectPermission.publish);
					}

					pages.add(page);

					if (!page.isQueued()) {
						continue;
					}

					// check whether page was queued to be published
					if (page.getPubQueueUser() != null) {
						SystemUser pubQueueUser = page.getPubQueueUser();
						int at = page.getTimePubQueue().getIntTimestamp();
						NodeObjectVersion publishAtVersion = page.getTimePubVersionQueue();

						CNI18nString messageTextI18n = null;
						if (at > 0) {
							messageTextI18n = new CNI18nString(
									"publishing of page <pageid {0}> at {1} has been approved.");
							messageTextI18n.setParameter("0", ObjectTransformer.getString(page.getId(), null));
							messageTextI18n.setParameter("1", new ContentNodeDate(at).getFullFormat());
						} else {
							messageTextI18n = new CNI18nString("the page <pageid {0}> has been published.");
							messageTextI18n.setParameter("0", ObjectTransformer.getString(page.getId(), null));
						}

						try (LangTrx lTrx = new LangTrx(pubQueueUser)) {
							messageSender.sendMessage(
									new com.gentics.contentnode.messaging.Message(t.getUserId(), pubQueueUser.getId(),
											messageTextI18n.toString()));
						}
						page.publish(at, publishAtVersion);
					}

					// check whether page was queued to be taken offline
					if (page.getOffQueueUser() != null) {
						SystemUser offQueueUser = page.getOffQueueUser();
						int at = page.getTimeOffQueue().getIntTimestamp();

						if (at > 0) {
							// send message
							CNI18nString msg = new CNI18nString("message.offlineat.queue.approve");
							msg.setParameters(Arrays.asList(String.valueOf(page.getId()),
									page.getTimeOffQueue().getFullFormat()));
							try (LangTrx lTrx = new LangTrx(offQueueUser)) {
								messageSender.sendMessage(
										new com.gentics.contentnode.messaging.Message(t.getUserId(),
												offQueueUser.getId(), msg.toString()));
							}

							// plan taking offline
							page.takeOffline(at);
						} else {
							// send message
							CNI18nString msg = new CNI18nString("message.offline.queue.approve");
							msg.setParameters(Arrays.asList(String.valueOf(page.getId())));
							try (LangTrx lTrx = new LangTrx(offQueueUser)) {
								messageSender.sendMessage(
										new com.gentics.contentnode.messaging.Message(t.getUserId(),
												offQueueUser.getId(), msg.toString()));
							}

							// take offline
							page.takeOffline();
						}
					}
				}
			} finally {
				for (Page page : pages) {
					page.unlock();
				}
			}

			return new GenericResponse(null, ResponseInfo.ok(""));
		});
	}

	@Override
	@POST
	@Path("/suggest/filename")
	public PageFilenameSuggestResponse suggestFilename(PageFilenameSuggestRequest request)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		try (ChannelTrx cTrx = new ChannelTrx(request.getNodeId())) {
			Folder folder = MiscUtils.load(Folder.class, Integer.toString(request.getFolderId()))
					.getMaster();

			Template template = t.getObject(Template.class, request.getTemplateId());
			if (template == null) {
				throw new EntityNotFoundException(
						I18NHelper.get("template.notfound", Integer.toString(request.getTemplateId())));
			}

			String suggestedFilename = null;
			Page page = t.createObject(Page.class);
			page.setFolderId(folder.getId());
			page.setTemplateId(request.getTemplateId());
			page.setLanguage(getRequestedContentLanguage(folder, request.getLanguage()));
			page.setName(request.getPageName());
			if (!org.apache.commons.lang3.StringUtils.isBlank(request.getFileName())) {
				String defaultExtension = PageFactory.getDefaultPageFileNameExtension(page,
						request.getFileName());
				NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig()
						.getDefaultPreferences();
				@SuppressWarnings("unchecked")
				Map<String, String> sanitizeCharacters = prefs.getPropertyMap("sanitize_character");
				String replacementChararacter = prefs.getProperty("sanitize_replacement_character");
				String[] preservedCharacters = prefs.getProperties("sanitize_allowed_characters");
				suggestedFilename = FileUtil.sanitizeName(request.getFileName(), defaultExtension,
						sanitizeCharacters, replacementChararacter, preservedCharacters);
			} else {
				suggestedFilename = PageFactory.suggestFilename(page);
			}

			return new PageFilenameSuggestResponse().setFileName(suggestedFilename);
		}
	}

	/**
	 * Render the tag for the page
	 *
	 * @param tag          tagname
	 * @param nodeId       optional node ID
	 * @param proxyprefix  optional proxyprefix
	 * @param linksType    optional links type
	 * @param pageSupplier page supplier
	 * @return response
	 */
	protected PageRenderResponse renderTag(String tag, Integer nodeId, String proxyprefix,
			LinksType linksType, Supplier<Page> pageSupplier) {
		Transaction t = getTransaction();
		NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();
		boolean prefixSet = false;

		try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			String responseMessage = null;

			PageRenderResponse response = new PageRenderResponse();

			// set the prefix, if one was passed in a parameter
			if (!StringUtils.isEmpty(proxyprefix)) {
				prefixSet = true;
				nodePreferences.setProperty(DynamicUrlFactory.STAG_PREFIX_PARAM,
						addPathPrefix(proxyprefix,
								nodePreferences.getProperty(DynamicUrlFactory.STAG_PREFIX_PARAM)));
				nodePreferences.setProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM,
						addPathPrefix(proxyprefix,
								nodePreferences.getProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM)));
			}

			// render the page and return the result
			RenderResult renderResult = new RenderResult();

			// set the RR for the current transaction, so that parameters are not reset
			t.setRenderResult(renderResult);

			RenderType renderType = createRenderType(linksType, RenderType.EM_ALOHA, t.getSessionId(),
					nodePreferences, false);
			t.setRenderType(renderType);

			Page page = pageSupplier.supply();

			long start = System.currentTimeMillis();

			// try to render with Mesh Portal URL
			String template = RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA, tag);
			if (template == null) {
				// Mesh Portal URL not set or not working: fall back to normal rendering
				response.setContent(
						createResponseContent("<node " + tag + ">", page, renderResult, renderType, null));
			} else {
				String beginMark = String.format("<gtxtag %s>", tag);
				String endMark = String.format("</gtxtag %s>", tag);
				String content = createResponseContent(template, page, renderResult, renderType, null);

				// The tag was enclosed by markers, so cut out the rendered tag from the response
				int beginIndex = content.indexOf(beginMark);
				int endIndex = content.indexOf(endMark);
				if (beginIndex >= 0 && endIndex >= 0) {
					// found the markers, cut out tag
					content = content.substring(beginIndex + beginMark.length(), endIndex);
				} else {
					// did not find markers, fall backt to normal rendering
					content = createResponseContent("<node " + tag + ">", page, renderResult, renderType,
							null);
				}

				response.setContent(content);
			}

			long duration = System.currentTimeMillis() - start;

			// set the tags and meta editables
			response.setTags(getTags(renderResult));
			response.setMetaeditables(getMetaEditables(renderResult));
			response.setTime(duration);

			if (responseMessage == null) {
				responseMessage = "Rendered tag { " + tag + " } successfully";
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, responseMessage));

			return response;
		} catch (EntityNotFoundException e) {
			return new PageRenderResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageRenderResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while rendering tag {" + tag + "}", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageRenderResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE,
							"Error while rendering tag {" + tag + "}: " + e.getLocalizedMessage()));
		} finally {
			if (prefixSet) {
				nodePreferences.unsetProperty(DynamicUrlFactory.STAG_PREFIX_PARAM);
				nodePreferences.unsetProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM);
			}
		}
	}

	/**
	 * Render the page (supplied by the pageSupplier)
	 *
	 * @param identifier   page identifier (used for logging and in message)
	 * @param nodeId       optional node ID
	 * @param template     rendered template (if empty, the page's template will be used)
	 * @param editMode     edit mode
	 * @param proxyprefix  optional proxy prefix for links
	 * @param linksType    rendering type for links
	 * @param tagmap       flag to render tagmap entries
	 * @param inherited    flag to render the inherited page, if the page itself is localized
	 * @param publish      flag to render in publish mode
	 * @param pageSupplier function that supplies the page (boolean parameter is true for editable
	 *                     page, false for readonly page)
	 * @return render response
	 */
	protected PageRenderResponse render(String identifier, Integer nodeId, String template,
			boolean editMode, String proxyprefix, LinksType linksType,
			boolean tagmap, boolean inherited, boolean publish, Function<Boolean, Page> pageSupplier) {
		Transaction t = getTransaction();
		NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();
		boolean prefixSet = false;
		boolean channelIdSet = false;
		try {
			String responseMessage = null;

			// set the nodeId, if provided
			channelIdSet = setChannelToTransaction(nodeId);

			boolean readOnly = !editMode;
			PageRenderResponse response = new PageRenderResponse();

			// set the prefix, if one was passed in a parameter
			if (!StringUtils.isEmpty(proxyprefix)) {
				prefixSet = true;
				nodePreferences.setProperty(DynamicUrlFactory.STAG_PREFIX_PARAM,
						addPathPrefix(proxyprefix,
								nodePreferences.getProperty(DynamicUrlFactory.STAG_PREFIX_PARAM)));
				nodePreferences.setProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM,
						addPathPrefix(proxyprefix,
								nodePreferences.getProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM)));
			}

			// render the page and return the result
			RenderResult renderResult = new RenderResult();

			// set the RR for the current transaction, so that parameters are not reset
			t.setRenderResult(renderResult);

			int renderTypeValue = 0;
			if (publish) {
				renderTypeValue = RenderType.EM_PUBLISH;
				readOnly = true;
				linksType = LinksType.frontend;
			} else {
				renderTypeValue = readOnly ? RenderType.EM_ALOHA_READONLY : RenderType.EM_ALOHA;
			}

			RenderType renderType = createRenderType(linksType, renderTypeValue, t.getSessionId(),
					nodePreferences, readOnly);
			t.setRenderType(renderType);

			Page page;
			if (publish) {
				// Needs to be done after the render type is set
				t.preparePublishData();

				page = pageSupplier.apply(false);
			} else {
				try {
					if (editMode) {
						page = pageSupplier.apply(true);
					} else {
						page = pageSupplier.apply(false);
					}
				} catch (ReadOnlyException e) {
					page = pageSupplier.apply(false);
					responseMessage = e.getMessage();
					readOnly = true;
					// show a warning message that the page could not be locked
					response.addMessage(new Message(Type.WARNING, e.getLocalizedMessage()));

					// we need to reset the rendertype, since editmode is different now
					renderTypeValue = readOnly ? RenderType.EM_ALOHA_READONLY : RenderType.EM_ALOHA;
					renderType = createRenderType(linksType, renderTypeValue, t.getSessionId(),
							nodePreferences, readOnly);
					t.setRenderType(renderType);
				}
			}

			long start = System.currentTimeMillis();
			Map<TagmapEntryRenderer, Object> propsToRender = null;

			if (tagmap) {
				propsToRender = createPropsToRender(page);
				response.setContent(
						createResponseContent(template, page, renderResult, renderType, propsToRender));
				response.setProperties(createResponseProperties(propsToRender));
			} else {
				response.setContent(
						createResponseContent(template, page, renderResult, renderType, propsToRender));
			}

			if (inherited && page.isLocalized()) {
				RenderResult renderResultInherit = new RenderResult();
				RenderType renderTypeInherit = createRenderType(linksType, renderTypeValue,
						t.getSessionId(), nodePreferences, true);

				t.setRenderResult(renderResultInherit);
				t.setRenderType(renderTypeInherit);

				Page nextHigherObject = page.getNextHigherObject();
				Map<TagmapEntryRenderer, Object> propsToRenderInherit =
						(tagmap) ? createPropsToRender(nextHigherObject) : null;

				response.setInheritedContent(
						createResponseContent(template, nextHigherObject, renderResultInherit,
								renderTypeInherit, propsToRenderInherit));
				response.setInheritedProperties(
						(tagmap) ? createResponseProperties(propsToRenderInherit) : null);

				t.setRenderResult(renderResult);
				t.setRenderType(renderType);
			}

			long duration = System.currentTimeMillis() - start;

			// set the tags and meta editables
			response.setTags(getTags(renderResult));
			response.setMetaeditables(getMetaEditables(renderResult));
			response.setTime(duration);

			if (responseMessage == null) {
				responseMessage = "Rendered page with id { " + identifier + " } successfully";
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, responseMessage));

			return response;
		} catch (EntityNotFoundException e) {
			return new PageRenderResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PageRenderResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while loading page " + identifier, e);
			I18nString message = new CNI18nString("rest.general.error");
			return new PageRenderResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while rendering page " + identifier
							+ ": " + e.getLocalizedMessage()));
		} finally {
			if (prefixSet) {
				nodePreferences.unsetProperty(DynamicUrlFactory.STAG_PREFIX_PARAM);
				nodePreferences.unsetProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM);
			}
			if (channelIdSet) {
				// reset channel
				t.resetChannel();
			}
		}
	}

	/**
	 * Take the page offline (set offline at, if timestamp > 0) or queue taking offline
	 *
	 * @param page      page
	 * @param timestamp timestamp
	 * @return result
	 * @throws NodeException
	 */
	protected PageOfflineResult takeOfflineOrQueue(Page page, int timestamp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser user = t.getObject(SystemUser.class, t.getUserId());

		Node node = page.getChannel();
		if (node == null) {
			node = page.getOwningNode();
		}

		try (ChannelTrx cTrx = new ChannelTrx(node)) {
			if (ObjectPermission.publish.checkObject(page)) {
				// publish permission, so take offline (or set offlineAt)
				page.takeOffline(timestamp);

				return timestamp > 0 ? PageOfflineResult.OFFLINE_AT : PageOfflineResult.OFFLINE;
			} else {
				// no publish permission, so queue taking offline
				page.queueOffline(user, timestamp);

				Map<SystemUser, PermHandler> parentUsers = PageFactory.getPublishers(page);

				MessageSender messageSender = new MessageSender();
				t.addTransactional(messageSender);

				// inform the group members
				CNI18nString messageTextI18n = null;

				if (timestamp > 0) {
					messageTextI18n = new CNI18nString("message.offlineat.queue");
					messageTextI18n.setParameter("0", user.getFirstname() + " " + user.getLastname());
					messageTextI18n.setParameter("1", ObjectTransformer.getString(page.getId(), null));
					messageTextI18n.setParameter("2", new ContentNodeDate(timestamp).getFullFormat());
				} else {
					messageTextI18n = new CNI18nString("message.offline.queue");
					messageTextI18n.setParameter("0", user.getFirstname() + " " + user.getLastname());
					messageTextI18n.setParameter("1", ObjectTransformer.getString(page.getId(), null));
				}

				for (Map.Entry<SystemUser, PermHandler> entry : parentUsers.entrySet()) {
					SystemUser parentUser = entry.getKey();
					PermHandler permHandler = entry.getValue();
					// we need to check the publish permission for every user, since this will
					// consider node restrictions
					if (permHandler.canPublish(page)) {
						try (LangTrx lTrx = new LangTrx(parentUser)) {
							messageSender.sendMessage(new com.gentics.contentnode.messaging.Message(t.getUserId(),
									ObjectTransformer.getInt(
											parentUser.getId(), -1), messageTextI18n.toString()));
						}
					}
				}

				return timestamp > 0 ? PageOfflineResult.QUEUED_AT : PageOfflineResult.QUEUED;
			}
		}
	}

	/**
	 * Enum for possible results of {@link PageResourceImpl#takeOfflineOrQueue(Page, int)}
	 */
	protected static enum PageOfflineResult {
		/**
		 * Page has been taken offline
		 */
		OFFLINE,

		/**
		 * Offline at has been set for the page
		 */
		OFFLINE_AT,

		/**
		 * Taking offline has been queued
		 */
		QUEUED,

		/**
		 * Setting offline at has been queued
		 */
		QUEUED_AT
	}
}
