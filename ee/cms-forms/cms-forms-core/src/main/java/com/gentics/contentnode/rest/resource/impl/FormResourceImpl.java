package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.NEW_FIELD_CHECKER;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBodyWithFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.load;
import static com.gentics.contentnode.rest.util.MiscUtils.require;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.TriFunction;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.AnyChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Folder.FormSearch;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.FormTimeManagement;
import com.gentics.contentnode.rest.model.form.FormListResponse;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.form.FormResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationListResponse;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationResponse;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ItemVersionListResponse;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;
import com.gentics.contentnode.rest.model.response.TotalUsageInfo;
import com.gentics.contentnode.rest.model.response.TotalUsageResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.AuthenticatedContentNodeResource.PageUsage;
import com.gentics.contentnode.rest.resource.parameter.FilterFormDataParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterFormParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterFormTypeConfigurationsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FormDataExportParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageModelParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.staging.StagingUtil;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Resource implementation for forms
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("form")
@RequiredFeature(Feature.FORMS)
public class FormResourceImpl extends AbstractContentNodeResource implements FormResource {
	/**
	 * Transform the node object into its REST model.
	 */
	public static final BiFunction<Form, FormModel, FormModel> NODE2REST = (form, model) -> {
		model.setId(form.getId());
		model.setGlobalId(form.getGlobalId().toString());
		model.setName(form.getName());
		model.setDescription(form.getDescription());
		model.setFormType(form.getFormType());

		model.setFolderId(form.getFolderId());
		model.setSuccessPageId(form.getSuccessPageId());
		model.setSuccessNodeId(form.getSuccessNodeId());

		model.setLanguages(form.getLanguages());
		model.setData(form.getData());

		model.setCdate(form.getCDate().getIntTimestamp());
		model.setCreator(ModelBuilder.getUser(form.getCreator()));

		model.setEdate(form.getEDate().getIntTimestamp());
		model.setEditor(ModelBuilder.getUser(form.getEditor()));

		model.setPdate(form.getPDate().getIntTimestamp());
		SystemUser publisher = form.getPublisher();
		if (publisher != null) {
			model.setPublisher(ModelBuilder.getUser(publisher));
		}
		SystemUser unpublisher = form.getUnpublisher();
		if (unpublisher != null) {
			model.setUnpublisher(ModelBuilder.getUser(unpublisher));
		}

		model.setUnpublishedDate(form.getUnpublishedDate().getIntTimestamp());
		model.setDeleted(ModelBuilder.getDeleteInfo(form));
		model.setModified(form.isModified());
		model.setOnline(form.isOnline());

		model.setLocked(form.isLocked());
		SystemUser lockedBy = form.getLockedBy();
		if (lockedBy != null) {
			model.setLockedBy(ModelBuilder.getUser(lockedBy));
		}
		ContentNodeDate lockedSince = form.getLockedSince();
		if (lockedSince != null) {
			model.setLockedSince(lockedSince.getIntTimestamp());
		}
		model.setVersion(NodeObjectVersion.TRANSFORM2REST.apply(form.getVersion()));

		SystemUser futurePublisher = form.getFutureUnpublisher();
		SystemUser futureUnpublisher = form.getFutureUnpublisher();

		model.setTimeManagement(
				new FormTimeManagement()
						.setAt(form.getTimePub().getTimestamp())
						.setVersion(NodeObjectVersion.TRANSFORM2REST.apply(form.getTimePubVersion()))
						.setOfflineAt(form.getTimeOff().getTimestamp())
						.setFuturePublisher(ModelBuilder.getUser(futurePublisher))
						.setFutureUnpublisher(ModelBuilder.getUser(futureUnpublisher))
		);
		model.setPlanned(form.isPlanned());

		return model;
	};

	/**
	 * Transform the node object into its rest model
	 */
	public final static Function<Form, FormModel> TRANSFORM2REST = form -> {
		return NODE2REST.apply(form, new FormModel());
	};

	/**
	 * Function that transforms the rest model into the given node model
	 */
	public final static BiFunction<FormModel, Form, Form> REST2NODE = (restModel, form) -> {
		if (!StringUtils.isBlank(restModel.getName())) {
			form.setName(restModel.getName());
		}
		if (!StringUtils.isBlank(restModel.getDescription())) {
			form.setDescription(restModel.getDescription());
		}

		if (restModel.getSuccessPageId() != null) {
			form.setSuccessPageId(restModel.getSuccessPageId());
		}

		if (restModel.getSuccessNodeId() != null) {
			form.setSuccessNodeId(restModel.getSuccessNodeId());
		}

		if (restModel.getLanguages() != null) {
			form.setLanguages(restModel.getLanguages());
		}
		if (restModel.getData() != null) {
			form.setData(restModel.getData());
		}
		if (restModel.getFormType() != null) {
			form.setFormType(restModel.getFormType());
		}

		return form;
	};

	@Override
	@GET
	public FormListResponse list(@BeanParam InFolderParameterBean inFolder, @BeanParam FilterFormParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam WastebinParameterBean wastebin, @BeanParam PublishableParameterBean publishable)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			checkBodyWithFunction(inFolder, NEW_FIELD_CHECKER, f -> Pair.of(I18NHelper.get("folder"), f.folderId));

			Folder folder = null;
			if (NodeConfigRuntimeConfiguration.isFeature(Feature.WASTEBIN)
					&& (wastebin.wastebinSearch == WastebinSearch.include
							|| wastebin.wastebinSearch == WastebinSearch.only)) {
				try (WastebinFilter wbf = new WastebinFilter(Wastebin.INCLUDE)) {
					folder = load(Folder.class, inFolder.folderId);
				}
			} else {
				folder = load(Folder.class, inFolder.folderId);
			}
			require(Feature.FORMS, folder.getOwningNode());

			// switch to the master folder
			folder = folder.getMaster();

			FormSearch search = FormSearch.create().setRecursive(inFolder.recursive).setFormType(filter.formType).setSearchString(filter.query)
					.set(publishable);

			if (NodeConfigRuntimeConfiguration.isFeature(Feature.WASTEBIN)
					&& t.getPermHandler().canWastebin(folder.getOwningNode())) {
				search.setWastebin(wastebin.wastebinSearch);
			}
			List<Form> forms = folder.getForms(search);
			FormListResponse response = ListBuilder.from(forms, TRANSFORM2REST)
				.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
				.sort(ResolvableComparator.get(sorting, "id", "name", "description", "cdate", "edate", "pdate"))
				.page(paging)
				.to(new FormListResponse());
			response.setStagingStatus(StagingUtil.checkStagingStatus(forms, inFolder.stagingPackageName, o -> o.getGlobalId().toString()));
			trx.success();

			return response;
		}
	}

	@Override
	@POST
	public FormResponse create(FormModel form) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			checkBodyWithFunction(form, NEW_FIELD_CHECKER, fm -> Pair.of(I18NHelper.get("name"), fm.getName()),
					fm -> Pair.of(I18NHelper.get("folder"), fm.getFolderId()),
					fm -> Pair.of(I18NHelper.get("language"), fm.getLanguages()),
					fm -> Pair.of(I18NHelper.get("formType"), fm.getFormType()));

			Folder folder = load(Folder.class, Integer.toString(form.getFolderId())).getMaster();
			Node node = folder.getOwningNode();
			require(Feature.FORMS, node);

			if (!t.getPermHandler().canCreate(folder, Form.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("form.nopermission"), null, null,
						Form.TYPE_FORM, 0, PermType.create);
			}

			// check whether the folder belongs to a channel
			if (folder.getChannel() != null) {
				throw new RestMappedException(I18NHelper.get("form.create.channel", I18NHelper.getPath(folder)))
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}

			// check whether the node has languages
			if (node.getLanguages().isEmpty()) {
				throw new RestMappedException(
						I18NHelper.get("form.create.nolanguages", I18NHelper.getName(node.getFolder())))
								.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}

			// check whether the requested formtype is assigned to the node
			checkFormType(node, form.getFormType());

			// check whether the languages are all activated for the node
			checkLanguages(node, form.getLanguages());

			Form nodeForm = REST2NODE.apply(form, t.createObject(Form.class));
			nodeForm.setFolderId(folder.getId());
			nodeForm.save();

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(nodeForm.reload()),
					ResponseInfo.ok("Successfully created form"));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("{id}")
	public FormResponse get(@PathParam("id") String formId, @QueryParam("package") String stagingPackageName) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			require(Feature.FORMS, form.getFolder().getOwningNode());

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(form),
					ResponseInfo.ok("Successfully loaded form"));
			response.setStagingStatus(StagingUtil.checkStagingStatus(form, stagingPackageName));
			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("{id}")
	public FormResponse update(@PathParam("id") String formId, FormModel form) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			Form update = load(Form.class, formId, ObjectPermission.edit);
			Node node = update.getFolder().getOwningNode();
			require(Feature.FORMS, node);

			if (form.getLanguages() != null) {
				// check whether the languages are all activated for the node
				checkLanguages(node, form.getLanguages());
			}

			form.setFormType(null);
			update = REST2NODE.apply(form, t.getObject(update, true));
			update.save();

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(update.reload()),
					ResponseInfo.ok("Successfully updated form"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("{id}")
	public GenericResponse delete(@PathParam("id") String formId, @QueryParam("disableInstantDelete") Boolean disableInstantDelete) throws NodeException {
		boolean syncCr = Optional.ofNullable(disableInstantDelete).map(BooleanUtils::negate).orElse(true);
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx(); InstantPublishingTrx ip = new InstantPublishingTrx(syncCr)) {
			Transaction t = trx.getTransaction();
			Form toDelete = load(Form.class, formId, ObjectPermission.delete);
			require(Feature.FORMS, toDelete.getFolder().getOwningNode());

			t.getObject(toDelete, true).delete();

			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully deleted form"));
		}
	}

	@Override
	@PUT
	@Path("{id}/online")
	public FormResponse publish(@PathParam("id") String formId, @QueryParam("at") @DefaultValue("0") int at,
			@QueryParam("keepVersion") @DefaultValue("false") boolean keepVersion) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			Form toPublish = load(Form.class, formId, ObjectPermission.publish);
			require(Feature.FORMS, toPublish.getFolder().getOwningNode());

			t.getObject(toPublish, true).publish(at, keepVersion);

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(toPublish.reload()),
					ResponseInfo.ok("Successfully published form"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("{id}/online")
	public FormResponse takeOffline(@PathParam("id") String formId, @QueryParam("at") @DefaultValue("0") int at)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			Form toTakeOffline = load(Form.class, formId, ObjectPermission.publish);
			require(Feature.FORMS, toTakeOffline.getFolder().getOwningNode());

			t.getObject(toTakeOffline, true).takeOffline(at);

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(toTakeOffline.reload()),
					ResponseInfo.ok("Successfully taken form offline"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("{id}/publish/at")
	public FormResponse removePublishAt(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form toChangeTimemanagement = load(Form.class, formId, ObjectPermission.publish);
			require(Feature.FORMS, toChangeTimemanagement.getFolder().getOwningNode());

			toChangeTimemanagement.clearTimePub();

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(toChangeTimemanagement.reload()),
					ResponseInfo.ok("Successfully taken form offline"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("{id}/offline/at")
	public FormResponse removeOfflineAt(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form toChangeTimemanagement = load(Form.class, formId, ObjectPermission.publish);
			require(Feature.FORMS, toChangeTimemanagement.getFolder().getOwningNode());

			toChangeTimemanagement.clearTimeOff();

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(toChangeTimemanagement.reload()),
					ResponseInfo.ok("Successfully taken form offline"));

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("{id}/lock")
	public FormResponse lock(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			Form toLock = load(Form.class, formId, ObjectPermission.edit);
			require(Feature.FORMS, toLock.getFolder().getOwningNode());

			// get an editable copy, which will lock the form (if possible)
			t.getObject(toLock, true);

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(toLock.reload()),
					ResponseInfo.ok("Successfully locked form"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("{id}/lock")
	public FormResponse unlock(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form toUnlock = load(Form.class, formId, ObjectPermission.edit);
			require(Feature.FORMS, toUnlock.getFolder().getOwningNode());

			toUnlock.unlock();

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(toUnlock.reload()),
					ResponseInfo.ok("Successfully unlocked form"));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("{id}/version")
	public ItemVersionListResponse listVersions(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			require(Feature.FORMS, form.getFolder().getOwningNode());

			ItemVersionListResponse response = ListBuilder
					.from(Arrays.asList(form.getVersions()), NodeObjectVersion.TRANSFORM2REST)
					.to(new ItemVersionListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("{id}/version/{version}")
	public FormResponse getVersion(@PathParam("id") String formId, @PathParam("version") String version)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			require(Feature.FORMS, form.getFolder().getOwningNode());

			NodeObjectVersion requestedVersion = form.getVersion(version);
			if (requestedVersion == null) {
				throw new EntityNotFoundException(I18NHelper.get("form.version.notfound", formId, version));
			}

			Form versionedForm = trx.getTransaction().getObject(Form.class, form.getId(),
					requestedVersion.getDate().getIntTimestamp());
			FormResponse response = new FormResponse(TRANSFORM2REST.apply(versionedForm),
					ResponseInfo.ok("Successfully loaded form version"));

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("{id}/version/{version}/restore")
	public FormResponse restoreVersion(@PathParam("id") String formId, @PathParam("version") String version)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			Form form = load(Form.class, formId, ObjectPermission.edit);
			require(Feature.FORMS, form.getFolder().getOwningNode());

			NodeObjectVersion requestedVersion = form.getVersion(version);
			if (requestedVersion == null) {
				throw new EntityNotFoundException(I18NHelper.get("form.version.notfound", formId, version));
			}

			Form editable = t.getObject(form, true);
			editable.restoreVersion(requestedVersion);

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(editable.reload()),
					ResponseInfo.ok("Successfully restored form version"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("wastebin/{id}")
	public GenericResponse removeFromWastebin(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx(); WastebinFilter filter = Wastebin.INCLUDE.set()) {
			Form form = load(Form.class, formId, ObjectPermission.wastebin);
			require(Feature.FORMS, form.getFolder().getOwningNode());

			if (!form.isDeleted()) {
				throw new EntityNotFoundException(I18NHelper.get("form.notfound", formId));
			}

			trx.getTransaction().getObject(form, true).delete(true);
			trx.success();

			return new GenericResponse(null, ResponseInfo.ok("Successfully removed form from wastebin"));
		}
	}

	@Override
	@POST
	@Path("wastebin/{id}/restore")
	public FormResponse restoreFromWastebin(@PathParam("id") String formId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx(); WastebinFilter filter = Wastebin.INCLUDE.set()) {
			Form form = load(Form.class, formId, ObjectPermission.wastebin);
			require(Feature.FORMS, form.getFolder().getOwningNode());

			if (!form.isDeleted()) {
				throw new EntityNotFoundException(I18NHelper.get("form.notfound", formId));
			}

			trx.getTransaction().getObject(form, true).restore();
			FormResponse response = new FormResponse(TRANSFORM2REST.apply(trx.getTransaction().getObject(form)),
					ResponseInfo.ok("Successfully restored form from wastebin"));
			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("{id}/folder/{folderId}")
	public FormResponse move(@PathParam("id") String formId, @PathParam("folderId") String folderId)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			Form form = load(Form.class, formId,
					NodeConfigRuntimeConfiguration.isFeature(Feature.MOVE_PERM_WITH_EDIT) ? ObjectPermission.edit
							: ObjectPermission.delete);
			Node sourceNode = form.getFolder().getOwningNode();
			require(Feature.FORMS, sourceNode);

			Folder targetFolder = load(Folder.class, folderId);
			Node targetNode = targetFolder.getOwningNode();
			require(Feature.FORMS, targetNode);
			if (!t.getPermHandler().canCreate(targetFolder, Form.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("form.nopermission"), null, null,
						Form.TYPE_FORM, 0, PermType.create);
			}

			// check whether the targetFolder belongs to a channel
			if (targetFolder.getChannel() != null) {
				throw new RestMappedException(
						I18NHelper.get("form.move.channel", I18NHelper.getName(form), I18NHelper.getPath(targetFolder)))
								.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}

			// check whether the targetNode has the required languages and formtype
			if (!Objects.equals(sourceNode, targetNode)) {
				if (targetNode.getLanguages().isEmpty()) {
					throw new RestMappedException(I18NHelper.get("form.move.nolanguages", I18NHelper.getName(form),
							I18NHelper.getName(targetNode.getFolder()))).setResponseCode(ResponseCode.INVALIDDATA)
									.setStatus(Status.CONFLICT);
				}

				// check whether the languages are all activated for the node
				checkLanguages(targetNode, form.getLanguages());

				// check whether the form type is activated for the node
				checkFormType(targetNode, form.getFormType());
			}

			// check for uniqueness of form names in the target folder
			if (UniquifyHelper.findConflictingValues(form.getName(),
					"SELECT name FROM form WHERE id != ? AND folder_id = ? AND name = ?", form.getId(),
					targetFolder.getId())) {
				throw new RestMappedException(
						I18NHelper.get("form.move.name", I18NHelper.getName(form), I18NHelper.getPath(targetFolder)))
								.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			Form update = t.getObject(form, true);
			update.setFolderId(targetFolder.getId());
			update.save();
			update.unlock();

			FormResponse response = new FormResponse(TRANSFORM2REST.apply(form.reload()),
					ResponseInfo.ok("Successfully moved form"));

			if (!Objects.equals(sourceNode, targetNode)) {
				String[] oldNodeProps = new String[] {Integer.toString(sourceNode.getId())};
				t.addTransactional(new TransactionalTriggerEvent(Form.class, form.getId(), oldNodeProps, Events.MOVE));
			}

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("{id}/preview/{language}")
	@Produces({ MediaType.TEXT_HTML })
	public String preview(@PathParam("id") String formId, @PathParam("language") String language) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			ContentRepository cr = getMeshCrForPreview(form);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				return mp.render(form, language);
			}
		}
	}

	@Override
	@POST
	@Path("{id}/preview/{language}")
	@Produces({ MediaType.TEXT_HTML })
	public String preview(@PathParam("id") String formId, @PathParam("language") String language, FormModel formModel)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			ContentRepository cr = getMeshCrForPreview(form);

			Form copy = (Form) form.copy();
			copy.setGlobalId(form.getGlobalId());
			formModel.setFormType(null);
			copy = REST2NODE.apply(formModel, copy);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				return mp.render(copy, language);
			}
		}
	}

	@Override
	@GET
	@Path("{id}/data")
	public Response listData(@PathParam("id") String formId, @BeanParam FilterFormDataParameterBean filter, @BeanParam PagingParameterBean paging)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			checkReportingPermission(form);
			ContentRepository cr = getMeshCrForPreview(form);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				String projectName = MeshPublisher.getMeshProjectName(form.getOwningNode());
				String uuid = MeshPublisher.getMeshUuid(form);

				return mp.get(uriBuilder -> {
					uriBuilder.segment(projectName, "plugins", "forms", "forms", uuid, "data");
					if (paging != null) {
						uriBuilder.queryParam("perPage", paging.pageSize);
						uriBuilder.queryParam("page", paging.page);
					}

					Optional<FilterFormDataParameterBean> filterParam = Optional.ofNullable(filter);

					filterParam.map(f -> f.query).ifPresent(q -> uriBuilder.queryParam("q", filter.query));

					// By default, only list published (i.e. completed) form data.
					uriBuilder.queryParam(
						"publishedOnly",
						filterParam.map(f -> f.publishedOnly).orElse(true));
				});
			}
		}
	}

	@Override
	@GET
	@Path("{id}/data/{dataId}")
	public Response getData(@PathParam("id") String formId, @PathParam("dataId") String dataId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			checkReportingPermission(form);
			ContentRepository cr = getMeshCrForPreview(form);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				String projectName = MeshPublisher.getMeshProjectName(form.getOwningNode());
				String uuid = MeshPublisher.getMeshUuid(form);

				return mp.get(uriBuilder -> {
					uriBuilder.segment(projectName, "plugins", "forms", "forms", uuid, "data", dataId);
				});
			}
		}
	}

	@Override
	@DELETE
	@Path("{id}/data/{dataId}")
	public Response deleteData(@PathParam("id") String formId, @PathParam("dataId") String dataId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			checkReportingPermission(form);
			ContentRepository cr = getMeshCrForPreview(form);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				String projectName = MeshPublisher.getMeshProjectName(form.getOwningNode());
				String uuid = MeshPublisher.getMeshUuid(form);

				return mp.delete(uriBuilder -> {
					uriBuilder.segment(projectName, "plugins", "forms", "forms", uuid, "data", dataId);
				});
			}
		}
	}

	@Override
	@GET
	@Path("{id}/data/{dataId}/binary/{field}")
	@Produces({ "*/*" })
	public Response downloadDataBinaryField(@PathParam("id") String formId, @PathParam("dataId") String dataId, @PathParam("field") String field) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			checkReportingPermission(form);
			ContentRepository cr = getMeshCrForPreview(form);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				String projectName = MeshPublisher.getMeshProjectName(form.getOwningNode());
				String uuid = MeshPublisher.getMeshUuid(form);

				return mp.get(uriBuilder -> {
					uriBuilder.segment(projectName, "plugins", "forms", "forms", uuid, "data", dataId, "binary", field);
				}, requestBuilder -> {
					requestBuilder.addHeader("Accept", "application/zip");
				});
			}
		}
	}

	@Override
	@GET
	@Path("{id}/export")
	public Response exportData(@PathParam("id") String formId, @BeanParam FormDataExportParameterBean exportFormat, @BeanParam FilterFormDataParameterBean filter) throws NodeException {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) -> mp.get(getPluginUriBuilder(projectName, uuid, exportFormat, filter, "export")));
	}

	@Override
	@GET
	@Path("{id}/export/status")
	public Response exportStatus(@PathParam("id") String formId, @BeanParam FormDataExportParameterBean exportFormat, @BeanParam FilterFormDataParameterBean filter) throws NodeException {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) ->
				mp.get(getPluginUriBuilder(projectName, uuid, exportFormat, filter, "download", "export", "status")));
	}

	@Override
	@POST
	@Path("{id}/export")
	public Response requestExport(@PathParam("id") String formId, @BeanParam FormDataExportParameterBean exportFormat, @BeanParam FilterFormDataParameterBean filter) throws Exception {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) -> mp.post(getPluginUriBuilder(projectName, uuid, exportFormat, filter, "export")));
	}

	@Override
	@GET
	@Path("{id}/binaries")
	@Produces({ "application/zip", MediaType.APPLICATION_JSON })
	public Response downloadDataBinaries(@PathParam("id") String formId) throws NodeException {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) -> mp.get(
				uriBuilder -> uriBuilder.segment(projectName, "plugins", "forms", "forms", uuid, "binaries"),
				requestBuilder -> requestBuilder.addHeader("Accept", "application/zip")));
	}

	@Override
	@GET
	@Path("{id}/binaries/status")
	public Response dataBinariesStatus(@PathParam("id") String formId) throws Exception {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) -> mp.get(getPluginUriBuilder(projectName, uuid, "download", "binaries", "status")));
	}

	@Override
	@POST
	@Path("{id}/binaries")
	public Response requestDataBinaries(@PathParam("id") String formId) throws Exception {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) -> mp.post(getPluginUriBuilder(projectName, uuid, "binaries")));
	}

	@Override
	@GET
	@Path("{id}/download/{exportUuid}")
	public Response downloadFile(@PathParam("id") String formId, @PathParam("exportUuid") String downloadUuid) throws Exception {
		return performPluginRequest(
			formId,
			(mp, projectName, uuid) -> mp.get(getPluginUriBuilder(projectName, uuid,  "download", downloadUuid)));
	}

	@Override
	@GET
	@Path("/usage/total")
	public TotalUsageResponse getTotalUsage(@QueryParam("id") List<Integer> formId,
			@QueryParam("nodeId") Integer nodeId) throws NodeException {
		if (ObjectTransformer.isEmpty(formId)) {
			return new TotalUsageResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched total usage information using 0 forms"));
		}
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			TotalUsageResponse response = new TotalUsageResponse();
			for (Integer id : formId) {
				TotalUsageInfo info = new TotalUsageInfo();
				Set<Integer> referencingPageIds = MiscUtils.getPageUsageIds(Arrays.asList(id), Form.TYPE_FORM,
						PageUsage.GENERAL, nodeId);
				Set<Integer> usingTemplateIds = MiscUtils.getTemplateUsageIds(Arrays.asList(id), Form.TYPE_FORM,
						nodeId);
				info.setTotal(referencingPageIds.size() + usingTemplateIds.size());
				info.setPages(referencingPageIds.size());
				info.setTemplates(usingTemplateIds.size());
				response.getInfos().put(id, info);
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully fetched total usage information"));
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/usage/page")
	public PageUsageListResponse getPageUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> formId,
			@QueryParam("nodeId") Integer nodeId, @QueryParam("pages") @DefaultValue("true") boolean returnPages,
			@BeanParam PageModelParameterBean pageModel) throws NodeException {
		if (ObjectTransformer.isEmpty(formId)) {
			return new PageUsageListResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched pages using 0 forms"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			PageUsageListResponse response = MiscUtils.getPageUsage(skipCount, maxItems, sortBy, sortOrder,
					Form.TYPE_FORM, formId, PageUsage.GENERAL, nodeId, returnPages, pageModel);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/usage/template")
	public TemplateUsageListResponse getTemplateUsageInfo(@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			@QueryParam("sortby") @DefaultValue("name") String sortBy,
			@QueryParam("sortorder") @DefaultValue("asc") String sortOrder, @QueryParam("id") List<Integer> formId,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("templates") @DefaultValue("true") boolean returnTemplates) throws NodeException {
		if (ObjectTransformer.isEmpty(formId)) {
			return new TemplateUsageListResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully fetched templates using 0 forms"), null, 0, 0);
		}

		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			TemplateUsageListResponse response = MiscUtils.getTemplateUsage(skipCount, maxItems, sortBy, sortOrder,
					Form.TYPE_FORM, formId, nodeId, returnTemplates);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/types")
	public FormTypeConfigurationListResponse listFormTypeConfigurations(
			@BeanParam FilterFormTypeConfigurationsParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {

		try (Trx trx = ContentNodeHelper.trx()) {
			ListBuilder<Entry<String, FormTypeConfiguration>, FormTypeConfigurationModel> listBuilder = ListBuilder
					.from(FormConfigProvider.getConfig().entrySet(), FormConfigProvider.TRANSFORM2REST);

			// Add optional nodeId filter
			if (filter != null) {
				if (!StringUtils.isBlank(filter.nodeId)) {
					Node node = MiscUtils.load(Node.class, filter.nodeId);
					Set<String> formTypesForNode = FormFactory.getFormTypeAssignment(node.getId());
					listBuilder.filter(entry -> formTypesForNode.contains(entry.getKey()));
				}

				// Add optional external flag filter
				if (filter.external != null) {
					listBuilder.filter(entry -> entry.getValue().external == filter.external);
				}

				// Add optional submitOnce flag filter
				if (filter.submitOnce != null) {
					listBuilder.filter(entry -> entry.getValue().submitOnce == filter.submitOnce);
				}

				listBuilder.filter(MiscUtils.filter(filter, FormConfigProvider.RESOLVER, "type", "name", "description"));
			}

			listBuilder.sort(MiscUtils.comparator(sorting, FormConfigProvider.RESOLVER, "type", "name", "description", "external", "submitOnce"));
			listBuilder.page(paging);

			return listBuilder.to(new FormTypeConfigurationListResponse());
		}
	}

	@Override
	@GET
	@Path("/types/{type}")
	public FormTypeConfigurationResponse getFormTypeconfiguration(@PathParam("type") String type) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			FormTypeConfiguration config = getFormTypeConfiguration(type);

			return new FormTypeConfigurationResponse(FormConfigProvider.TRANSFORM2REST.apply(Map.entry(type, config)),
					ResponseInfo.ok("Successfully loaded form type configuration"));
		}
	}

	@Override
	@DELETE
	@Path("/nodes/{nodeId}/types/{type}")
	public Response removeFormTypeConfigurationsFromNode(@PathParam("nodeId") String nodeId, @PathParam("type") String type) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = MiscUtils.load(Node.class, nodeId, ObjectPermission.edit);
			getFormTypeConfiguration(type);

			if (FormFactory.getFormTypeUsage(node.getId()).contains(type)) {
				throw new RestMappedException(I18NHelper.get("form.form_type_configuration.used", type))
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			FormFactory.removeFormTypeFromNode(type, node.getId());

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@PUT
	@Path("/nodes/{nodeId}/types/{type}")
	public Response addFormTypeConfigurationsToNode(@PathParam("nodeId") String nodeId, @PathParam("type") String type) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = MiscUtils.load(Node.class, nodeId, ObjectPermission.edit);
			getFormTypeConfiguration(type);

			FormFactory.addFormTypeToNode(type, node.getId());
			trx.success();
			return Response.created(null).build();
		}
	}

	/**
	 * Check whether the requested form type exists and is assigned to the node
	 * @param node node
	 * @param formType form type
	 * @throws NodeException
	 */
	protected void checkFormType(Node node, String formType) throws NodeException {
		if (!FormConfigProvider.getConfig().containsKey(formType)) {
			throw new RestMappedException(I18NHelper.get("form.form_type_configuration.notfound", formType))
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}
		if (!FormFactory.getFormTypeAssignment(node.getId()).contains(formType)) {
			throw new RestMappedException(I18NHelper.get("form.form_type_configuration.notactive", formType, I18NHelper.getName(node)))
					.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}
	}

	/**
	 * Check whether the requested languages exist and are assigned to the node
	 *
	 * @param node      node
	 * @param languages requested languages
	 * @throws NodeException
	 */
	protected void checkLanguages(Node node, List<String> languages) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ContentLanguage> all = t.getObjects(ContentLanguage.class,
				DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS));
		Map<String, ContentLanguage> allMap = all.stream().collect(Collectors.toMap(ContentLanguage::getCode, f -> f));
		List<ContentLanguage> nodeLanguages = node.getLanguages();
		for (String code : languages) {
			if (!allMap.containsKey(code)) {
				throw new RestMappedException(I18NHelper.get("form.language.notfound", code))
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			ContentLanguage language = allMap.get(code);
			if (!nodeLanguages.contains(language)) {
				throw new RestMappedException(I18NHelper.get("form.language.notactive", language.getName(),
						I18NHelper.getName(node.getFolder()))).setResponseCode(ResponseCode.INVALIDDATA)
								.setStatus(Status.CONFLICT);
			}
		}
	}

	/**
	 * Get the Mesh CR to be used for preview of the form. This will also check,
	 * whether the feature is enabled for the node and whether an appropriate CR is
	 * assigned to the node.
	 *
	 * @param form form to be previewed
	 * @return Mesh Content.Repository (never null)
	 * @throws NodeException
	 */
	protected ContentRepository getMeshCrForPreview(Form form) throws NodeException {
		Node owningNode = form.getFolder().getOwningNode();
		require(Feature.FORMS, owningNode);

		ContentRepository cr = owningNode.getContentRepository();
		if (cr == null || cr.getCrType() != Type.mesh) {
			String message = cr == null
					? I18NHelper.get("form.preview.nocr", I18NHelper.getName(form),
							I18NHelper.getName(owningNode.getFolder()))
					: I18NHelper.get("form.preview.nomeshcr", I18NHelper.getName(form), I18NHelper.getName(cr));
			throw new RestMappedException(message).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}

		return cr;
	}

	/**
	 * Check whether the current user has permission to view reports for the given
	 * form
	 *
	 * @param form form to check
	 * @throws InsufficientPrivilegesException if the user does not have permission
	 * @throws NodeException                   in case of other errors
	 */
	protected void checkReportingPermission(Form form) throws InsufficientPrivilegesException, NodeException {
		if (!TransactionManager.getCurrentTransaction().getPermHandler().checkPermissionBit(Folder.TYPE_FOLDER,
				form.getFolderId(), PermType.formreport.getBit())) {
			throw new InsufficientPrivilegesException(
					I18NHelper.get("form.nopermission", Integer.toString(form.getId())), form, PermType.formreport);
		}
	}

	/**
	 * Convenience override for calling {@link #getPluginUriBuilder(String, String, FormDataExportParameterBean, FilterFormDataParameterBean, String...)}
	 * without parameter beans.
	 *
	 * @param projectName The Mesh project name.
	 * @param uuid The Mesh form UUID.
	 * @param extraSegments
	 * @param extraSegments Extra segments to append after the base path.
	 * @return  URI builder for a request to the Mesh forms plugin.
	 */
	private Consumer<UriBuilder> getPluginUriBuilder(String projectName, String uuid, String ...extraSegments) {
		return getPluginUriBuilder(projectName, uuid, null, null, extraSegments);
	}

	/**
	 * Creates a builder for a forms plugin URL using the given parameters.
	 *
	 * <p>
	 *     The path will be created up to {@code /api/v1/:projectName/plugins/forms/forms/:formUuid and then the}
	 *     {@code extraSegments} will be added.
	 * </p>
	 *
	 * <p>
	 *     Query parameters for export options (e.g. language, export format, only published data, ...) will be
	 *     extracted form the parameter beans.
	 * </p>
	 *
	 * @param projectName The Mesh project name.
	 * @param uuid The Mesh form UUID.
	 * @param exportFormat The form export options.
	 * @param filter The form filter options.
	 * @param extraSegments Extra segments to append after the base path.
	 * @return  URI builder for a request to the Mesh forms plugin.
	 */
	private Consumer<UriBuilder> getPluginUriBuilder(String projectName, String uuid, FormDataExportParameterBean exportFormat, FilterFormDataParameterBean filter, String ...extraSegments) {
		return uriBuilder -> {
			uriBuilder.segment(projectName, "plugins", "forms", "forms", uuid)
				.segment(extraSegments);

			if (exportFormat != null) {
				if (exportFormat.format != null) {
					uriBuilder.queryParam("format", exportFormat.format);
				}
				if (exportFormat.options != null) {
					uriBuilder.queryParam("options", exportFormat.options);
				}
				if (exportFormat.languages != null) {
					Object[] languages = exportFormat.languages.toArray(new String[exportFormat.languages.size()]);
					uriBuilder.queryParam("lang", languages);
				}
			}

			// By default export only published (i.e. completed) form data nodes.
			uriBuilder.queryParam(
				"publishedOnly",
				Optional.ofNullable(filter).map(f -> f.publishedOnly).orElse(true));
		};
	}

	/**
	 * Perform a request to the Mesh forms plugin for the given form.
	 *
	 * <p>
	 *     This method will setup a transaction and create a {@link MeshPublisher}
	 *     which can be used for the actual request.
	 * </p>
	 *
	 * <p>
	 *     The actual request must be supplied via the {@code request} function,
	 *     which accepts a {@code MeshPublisher}, the {@code projectName} and
	 *     {@code uuid} of the form and must return the response from the
	 *     Mesh forms plugin.
	 * </p>
	 *
	 * @param formId The forms ID.
	 * @param request The actual request to make.
	 * @return The response from the Mesh forms plugin.
	 */
	private Response performPluginRequest(String formId, TriFunction<MeshPublisher, String, String, Response> request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Form form = load(Form.class, formId);
			checkReportingPermission(form);
			ContentRepository cr = getMeshCrForPreview(form);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				String projectName = MeshPublisher.getMeshProjectName(form.getOwningNode());
				String uuid = MeshPublisher.getMeshUuid(form);

				return request.apply(mp, projectName, uuid);
			}
		}
	}

	/**
	 * Get the form type configuration with given type or throw an {@link EntityNotFoundException} if is does not exist
	 * @param type type
	 * @return configuration
	 * @throws EntityNotFoundException
	 */
	protected FormTypeConfiguration getFormTypeConfiguration(String type) throws EntityNotFoundException {
		FormTypeConfiguration config = FormConfigProvider.getConfig().get(type);

		if (config == null) {
			throw new EntityNotFoundException(I18NHelper.get("form_type_configuration.notfound", type));
		}

		return config;
	}
}
