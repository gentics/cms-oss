package com.gentics.contentnode.rest.resource.impl.devtools;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import com.gentics.contentnode.rest.model.response.devtools.PackageDependencyList;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.ChangeWatchService;
import com.gentics.contentnode.devtools.ConstructSynchronizer;
import com.gentics.contentnode.devtools.ContentRepositoryFragmentSynchronizer;
import com.gentics.contentnode.devtools.ContentRepositorySynchronizer;
import com.gentics.contentnode.devtools.DatasourceSynchronizer;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.ObjectTagDefinitionSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.Synchronizer.Status;
import com.gentics.contentnode.devtools.TemplateSynchronizer;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.distributed.DistributedWebApplicationException;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.distributed.TrxCallable;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.CannotModifySubpackageException;
import com.gentics.contentnode.rest.exceptions.DuplicateEntityException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.devtools.AutocompleteItem;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.model.devtools.PackageListResponse;
import com.gentics.contentnode.rest.model.devtools.SyncInfo;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ObjectPropertyLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedConstructInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedContentRepositoryFragmentInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedContentRepositoryInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedDatasourceInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedObjectPropertyInPackageListResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedTemplateInPackageListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.util.FileUtil;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/devtools")
@Authenticated
@RequiredFeature(Feature.DEVTOOLS)
@RequiredPerm(type=PermHandler.TYPE_ADMIN, bit=PermHandler.PERM_VIEW)
@RequiredPerm(type=PermHandler.TYPE_CONADMIN, bit=PermHandler.PERM_VIEW)
@RequiredPerm(type=PermHandler.TYPE_DEVTOOLS_PACKAGES, bit=PermHandler.PERM_VIEW)
public class PackageResourceImpl implements PackageResource {
	@Override
	@GET
	@Path("/packages")
	public PackageListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return getSortedPagedList(Synchronizer.getPackages(), filter, sorting, paging);
		}
	}

	@Override
	@GET
	@Path("/packages/{name}")
	public Package get(@PathParam("name") String name) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			MainPackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();
			return MainPackageSynchronizer.TRANSFORM2REST.apply(packageSynchronizer);
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}")
	public Response add(@PathParam("name") String name) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!Synchronizer.allowedPackageName(name)) {
				throw new NodeException(I18NHelper.get("devtools_package.invalidname", name));
			}
			if (Synchronizer.getPackage(name) != null) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.new.conflict", name));
			}
			Synchronizer.addPackage(name);
			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}")
	public Response delete(@PathParam("name") String name) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			getPackage(name);
			Synchronizer.removePackage(name);
			return Response.noContent().build();
		}
	}


	@Override
	@GET
	@Path("/packages/{name}/check")
	public PackageDependencyList performPackageConsistencyCheck(@PathParam("name") String packageName) throws NodeException {
		operate(()-> getPackage(packageName));

		PackageDependencyChecker dependencyChecker = new PackageDependencyChecker(packageName);
		PackageDependencyList dependencyListResponse = new PackageDependencyList();
		dependencyListResponse.setItems(dependencyChecker.collectDependencies());

		return dependencyListResponse;
	}


	@Override
	@PUT
	@Path("/packages/{name}/cms2fs")
	public GenericResponse synchronizeToFS(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			CNI18nString description = new CNI18nString("devtools_packages.action.cms2fs");
			description.addParameter(name);
			return Operator.executeLocked(description.toString(), waitMs, null, () -> {
				PackageSynchronizer packageSynchronizer = getPackage(name);
				Map<Class<? extends SynchronizableNodeObject>, Integer> counts = new HashMap<>();
				for (Class<? extends SynchronizableNodeObject> clazz : Synchronizer.CLASSES) {
					counts.put(clazz, packageSynchronizer.syncAllToFilesystem(clazz));
				}

				CNI18nString message = new CNI18nString("devtools_packages.action.cms2fs.result");
				message.addParameter(name);
				for (Class<? extends SynchronizableNodeObject> clazz : Synchronizer.CLASSES) {
					message.addParameter(Integer.toString(counts.get(clazz)));
				}
				return new GenericResponse(new Message(Message.Type.SUCCESS, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
			}, e -> new WebApplicationException(e.getLocalizedMessage()));
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/fs2cms")
	public GenericResponse synchronizeFromFS(@PathParam("name") String name, @QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			CNI18nString description = new CNI18nString("devtools_packages.action.fs2cms");
			description.addParameter(name);
			return Operator.executeLocked(description.toString(), waitMs, null, () -> {
				PackageSynchronizer packageSynchronizer = getPackage(name);
				Map<Class<? extends SynchronizableNodeObject>, Integer> counts = new HashMap<>();
				for (Class<? extends SynchronizableNodeObject> clazz : Synchronizer.CLASSES) {
					counts.put(clazz, packageSynchronizer.syncAllFromFilesystem(clazz));
				}

				CNI18nString message = new CNI18nString("devtools_packages.action.fs2cms.result");
				message.addParameter(name);
				for (Class<? extends SynchronizableNodeObject> clazz : Synchronizer.CLASSES) {
					message.addParameter(Integer.toString(counts.get(clazz)));
				}
				return new GenericResponse(new Message(Message.Type.SUCCESS, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
			}, e -> new WebApplicationException(e.getLocalizedMessage()));
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/constructs")
	public PagedConstructInPackageListResponse listConstructs(
			@PathParam("name") String name,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();

			return ListBuilder.from(packageSynchronizer.getObjects(Construct.class), ConstructSynchronizer.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o.getObject()))
					.filter(ResolvableFilter.get(filter, "keyword", "name", "packageName", "description"))
					.perms(permFunction(perms, PackageObject::getObject, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "keyword", "name", "packageName", "description"))
					.embed(embed, "category", ConstructSynchronizer.EMBED_CATEGORY)
					.page(paging).to(new PagedConstructInPackageListResponse());
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/constructs/{construct}")
	public ConstructLoadResponse getConstruct(@PathParam("name") String name, @PathParam("construct") String construct) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<Construct> constructInPackage = getConstruct(packageSynchronizer, construct);
			ConstructLoadResponse response = new ConstructLoadResponse(null, new ResponseInfo(ResponseCode.OK, ""));
			response.setConstruct(ConstructSynchronizer.TRANSFORM2REST.apply(constructInPackage));
			return response;
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/constructs/{construct}")
	public Response addConstruct(@PathParam("name") String name, @PathParam("construct") String construct)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			Construct nodeObject = getConstruct(construct);
			// check for duplicates
			if (packageSynchronizer.getObjects(Construct.class).contains(new PackageObject<>(nodeObject))) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add_construct.duplicate", nodeObject.getName().toString(), name));
			}
			packageSynchronizer.synchronize(nodeObject, true);

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}/constructs/{construct}")
	public Response removeConstruct(@PathParam("name") String name, @PathParam("construct") String construct) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<Construct> constructInPackage = getConstruct(packageSynchronizer, construct);
			if (!ObjectTransformer.isEmpty(constructInPackage.getPackageName())) {
				throw new CannotModifySubpackageException(I18NHelper.get("devtools_package.remove.subpackage"));
			}
			packageSynchronizer.remove(constructInPackage.getObject(), true);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/templates")
	public PagedTemplateInPackageListResponse listTemplates(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();
			return ListBuilder.from(packageSynchronizer.getObjects(Template.class), TemplateSynchronizer.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o.getObject()))
					.filter(ResolvableFilter.get(filter, "name", "packageName", "description"))
					.perms(permFunction(perms, PackageObject::getObject, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "name", "packageName", "description"))
					.page(paging).to(new PagedTemplateInPackageListResponse());
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/templates/{template}")
	public TemplateLoadResponse getTemplate(@PathParam("name") String name, @PathParam("template") String template) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<Template> templateInPackage = getTemplate(packageSynchronizer, template);

			TemplateLoadResponse response = new TemplateLoadResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			response.setTemplate(TemplateSynchronizer.TRANSFORM2REST.apply(templateInPackage));
			return response;
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/templates/{template}")
	public Response addTemplate(@PathParam("name") String name, @PathParam("template") String template) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			Template nodeObject = getTemplate(template);

			// check for localized copies
			if (!nodeObject.isMaster()) {
				throw new WebApplicationException(I18NHelper.get("devtools_package.add_template.localized", nodeObject.toString(), name),
						Response.Status.BAD_REQUEST);
			}

			// check for duplicates
			if (packageSynchronizer.getObjects(Template.class).contains(new PackageObject<>(nodeObject))) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add_template.duplicate", nodeObject.getName(), name));
			}
			packageSynchronizer.synchronize(nodeObject, true);

			// also synchronize all localized copies
			try (NoMcTrx noMcTrx = new NoMcTrx()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				for (Integer id : nodeObject.getChannelSet().values()) {
					// omit the master (already synchronized)
					if (id.equals(nodeObject.getId())) {
						continue;
					}

					Template localizedCopy = t.getObject(Template.class, id);
					if (localizedCopy != null) {
						packageSynchronizer.synchronize(localizedCopy, true);
					}
				}
			}

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}/templates/{template}")
	public Response removeTemplate(@PathParam("name") String name, @PathParam("template") String template) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<Template> templateInPackage = getTemplate(packageSynchronizer, template);
			if (!ObjectTransformer.isEmpty(templateInPackage.getPackageName())) {
				throw new CannotModifySubpackageException(I18NHelper.get("devtools_package.remove.subpackage"));
			}
			packageSynchronizer.remove(templateInPackage.getObject(), true);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/datasources")
	public PagedDatasourceInPackageListResponse listDatasources(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();
			return ListBuilder.from(packageSynchronizer.getObjects(Datasource.class), DatasourceSynchronizer.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o.getObject()))
					.filter(ResolvableFilter.get(filter, "name", "packageName"))
					.perms(permFunction(perms, PackageObject::getObject, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "name", "packageName"))
					.page(paging).to(new PagedDatasourceInPackageListResponse());
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/datasources/{datasource}")
	public DatasourceLoadResponse getDatasource(@PathParam("name") String name, @PathParam("datasource") String datasource) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<Datasource> datasourceInPackage = getDatasource(packageSynchronizer, datasource);

			DatasourceLoadResponse response = new DatasourceLoadResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			response.setDatasource(DatasourceSynchronizer.TRANSFORM2REST.apply(datasourceInPackage));
			return response;
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/datasources/{datasource}")
	public Response addDatasource(@PathParam("name") String name, @PathParam("datasource") String datasource) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			Datasource nodeObject = getDatasource(datasource);
			// check for duplicates
			if (packageSynchronizer.getObjects(Datasource.class).contains(new PackageObject<>(nodeObject))) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add_datasource.duplicate", nodeObject.getName(), name));
			}
			packageSynchronizer.synchronize(nodeObject, true);

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}/datasources/{datasource}")
	public Response removeDatasource(@PathParam("name") String name, @PathParam("datasource") String datasource) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<Datasource> datasourceInPackage = getDatasource(packageSynchronizer, datasource);
			if (!ObjectTransformer.isEmpty(datasourceInPackage.getPackageName())) {
				throw new CannotModifySubpackageException(I18NHelper.get("devtools_package.remove.subpackage"));
			}
			packageSynchronizer.remove(datasourceInPackage.getObject(), true);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/objectproperties")
	public PagedObjectPropertyInPackageListResponse listObjectProperties(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed,
			@BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();

			return ListBuilder.from(packageSynchronizer.getObjects(ObjectTagDefinition.class), ObjectTagDefinitionSynchronizer.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o.getObject()))
					.filter(ResolvableFilter.get(filter, "name", "packageName", "description", "keyword"))
					.perms(permFunction(perms, PackageObject::getObject, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "name", "packageName", "description", "keyword", "type", "required", "inheritable", "construct.name"))
					.page(paging)
					.embed(embed, "construct", ObjectTagDefinitionSynchronizer.EMBED_CONSTRUCT)
					.embed(embed, "category", ObjectTagDefinitionSynchronizer.EMBED_CATEGORY)
					.to(new PagedObjectPropertyInPackageListResponse());
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/objectproperties/{objectproperty}")
	public ObjectPropertyLoadResponse getObjectProperty(@PathParam("name") String name, @PathParam("objectproperty") String objectproperty) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<ObjectTagDefinition> objectPropertyInPackage = getObjectProperty(packageSynchronizer, objectproperty);

			ObjectPropertyLoadResponse response = new ObjectPropertyLoadResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
			response.setObjectProperty(ObjectTagDefinitionSynchronizer.TRANSFORM2REST.apply(objectPropertyInPackage));
			return response;
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/objectproperties/{objectproperty}")
	public Response addObjectProperty(@PathParam("name") String name, @PathParam("objectproperty") String objectproperty) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			ObjectTagDefinition nodeObject = getObjectProperty(objectproperty);
			// check for duplicates
			if (packageSynchronizer.getObjects(ObjectTagDefinition.class).contains(new PackageObject<>(nodeObject))) {
				String objectName = nodeObject.getName();
				if (StringUtils.isEmpty(objectName)) {
					objectName = nodeObject.getObjectTag().getName();
				}
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add_objectproperty.duplicate", objectName, name));
			}
			packageSynchronizer.synchronize(nodeObject, true);

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}/objectproperties/{objectproperty}")
	public Response removeObjectProperty(@PathParam("name") String name, @PathParam("objectproperty") String objectproperty) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<ObjectTagDefinition> objectPropertyInPackage = getObjectProperty(packageSynchronizer, objectproperty);
			if (!ObjectTransformer.isEmpty(objectPropertyInPackage.getPackageName())) {
				throw new CannotModifySubpackageException(I18NHelper.get("devtools_package.remove.subpackage"));
			}
			packageSynchronizer.remove(objectPropertyInPackage.getObject(), true);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/cr_fragments")
	public PagedContentRepositoryFragmentInPackageListResponse listCrFragments(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed,
			@BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();
			return ListBuilder.from(packageSynchronizer.getObjects(CrFragment.class), ContentRepositoryFragmentSynchronizer.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o.getObject()))
					.filter(ResolvableFilter.get(filter, "name", "crType", "packageName"))
					.perms(permFunction(perms, PackageObject::getObject, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "name", "crType", "packageName"))
					.page(paging).to(new PagedContentRepositoryFragmentInPackageListResponse());
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/cr_fragments/{cr_fragment}")
	public ContentRepositoryFragmentResponse getCrFragment(@PathParam("name") String name, @PathParam("cr_fragment") String crFragment) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<CrFragment> crFragmentInPackage = getCrFragment(packageSynchronizer, crFragment);
			ContentRepositoryFragmentResponse response = new ContentRepositoryFragmentResponse(crFragmentInPackage.getObject().getModel(),
					new ResponseInfo(ResponseCode.OK, ""));
			return response;
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/cr_fragments/{cr_fragment}")
	public Response addCrFragment(@PathParam("name") String name, @PathParam("cr_fragment") String crFragment) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			CrFragment nodeObject = getCrFragment(crFragment);
			// check for duplicates
			if (packageSynchronizer.getObjects(CrFragment.class).contains(new PackageObject<>(nodeObject))) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add_cr_fragment.duplicate", nodeObject.getName().toString(), name));
			}
			packageSynchronizer.synchronize(nodeObject, true);

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}/cr_fragments/{cr_fragment}")
	public Response removeCrFragment(@PathParam("name") String name, @PathParam("cr_fragment") String crFragment) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<CrFragment> crFragmentInPackage = getCrFragment(packageSynchronizer, crFragment);
			if (!ObjectTransformer.isEmpty(crFragmentInPackage.getPackageName())) {
				throw new CannotModifySubpackageException(I18NHelper.get("devtools_package.remove.subpackage"));
			}
			packageSynchronizer.remove(crFragmentInPackage.getObject(), true);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/contentrepositories")
	public PagedContentRepositoryInPackageListResponse listContentRepositories(@PathParam("name") String name, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed,
			@BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);
			trx.success();
			return ListBuilder.from(packageSynchronizer.getObjects(ContentRepository.class), ContentRepositorySynchronizer.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o.getObject()))
					.filter(ResolvableFilter.get(filter, "name", "crType", "packageName"))
					.perms(permFunction(perms, PackageObject::getObject, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "name", "crType", "packageName"))
					.page(paging).to(new PagedContentRepositoryInPackageListResponse());
		}
	}

	@Override
	@GET
	@Path("/packages/{name}/contentrepositories/{contentrepository}")
	public ContentRepositoryResponse getContentRepository(@PathParam("name") String name, @PathParam("contentrepository") String contentrepository)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<ContentRepository> contentRepositoryInPackage = getContentRepository(packageSynchronizer, contentrepository);
			ContentRepositoryResponse response = new ContentRepositoryResponse(
					ContentRepository.NODE2REST.apply(contentRepositoryInPackage.getObject(), new ContentRepositoryModel()),
					new ResponseInfo(ResponseCode.OK, ""));
			return response;
		}
	}

	@Override
	@PUT
	@Path("/packages/{name}/contentrepositories/{contentrepository}")
	public Response addContentRepository(@PathParam("name") String name, @PathParam("contentrepository") String contentrepository) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			ContentRepository nodeObject = getContentRepository(contentrepository);
			// check for duplicates
			if (packageSynchronizer.getObjects(ContentRepository.class).contains(new PackageObject<>(nodeObject))) {
				throw new DuplicateEntityException(I18NHelper.get("devtools_package.add_contentrepository.duplicate", nodeObject.getName().toString(), name));
			}
			packageSynchronizer.synchronize(nodeObject, true);

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/packages/{name}/contentrepositories/{contentrepository}")
	public Response removeContentRepository(@PathParam("name") String name, @PathParam("contentrepository") String contentrepository) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PackageSynchronizer packageSynchronizer = getPackage(name);

			PackageObject<ContentRepository> contentRepositoryInPackage = getContentRepository(packageSynchronizer, contentrepository);
			if (!ObjectTransformer.isEmpty(contentRepositoryInPackage.getPackageName())) {
				throw new CannotModifySubpackageException(I18NHelper.get("devtools_package.remove.subpackage"));
			}
			packageSynchronizer.remove(contentRepositoryInPackage.getObject(), true);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/nodes/{nodeId}/packages")
	public PackageListResponse listNodePackages(@PathParam("nodeId") String nodeId, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, false);
			trx.success();
			return getSortedPagedList(Synchronizer.getPackages(node), filter, sorting, paging);
		}
	}

	@Override
	@DELETE
	@Path("/nodes/{nodeId}/packages/{packageName}")
	public Response removePackage(@PathParam("nodeId") String nodeId, @PathParam("packageName") String packageName) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, false);
			Synchronizer.removePackage(node, packageName);
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@PUT
	@Path("/nodes/{nodeId}/packages/{packageName}")
	public Response addPackage(@PathParam("nodeId") String nodeId, @PathParam("packageName") String packageName) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, false);
			Synchronizer.addPackage(node, packageName);
			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@PUT
	@Path("/nodes/{nodeId}/packages")
	public Response addPackage(@PathParam("nodeId") String nodeId, Package addedPackage) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Node node = getNode(nodeId, false);

			// TODO check completeness of data (package name)

			Synchronizer.addPackage(node, addedPackage.getName());

			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@GET
	@Path("/listen/{uuid}")
	@Produces(SseFeature.SERVER_SENT_EVENTS)
	public EventOutput listenPageChange(@PathParam("uuid") String uuid) throws NodeException {
		return ChangeWatchService.getEventOutput(UUID.fromString(uuid));
	}

	@Override
	@POST
	@Path("/stoplisten/{uuid}")
	public Response removeListener(@PathParam("uuid") String uuid) {
		ChangeWatchService.unregister(UUID.fromString(uuid));
		return Response.noContent().build();
	}

	@Override
	@GET
	@Path("/preview/{uuid}")
	@Produces(MediaType.TEXT_HTML)
	public String preview(@PathParam("uuid") String uuid) throws NodeException {
		return ChangeWatchService.render(UUID.fromString(uuid));
	}

	@Override
	@GET
	@Path("/preview/page/{id}")
	@Produces(MediaType.TEXT_HTML)
	public String renderPage(@PathParam("id") String id, @QueryParam("nodeId") String nodeId) throws Exception {
		nodeId = ObjectTransformer.getString(nodeId, "");
		try (Trx trx = ContentNodeHelper.trx(); InputStream in = getClass().getResourceAsStream("page_preview.html")) {
			UUID uuid = ChangeWatchService.register(nodeId, id);

			Transaction t = TransactionManager.getCurrentTransaction();
			return FileUtil.stream2String(in, "UTF-8").replaceAll("\\{\\{uuid\\}\\}", uuid.toString()).replaceAll("\\{\\{sid\\}\\}", t.getSessionId())
					.replaceAll("\\{\\{time\\}\\}", Long.toString(System.currentTimeMillis()));
		}
	}

	@Override
	@GET
	@Path("/autocomplete/constructs")
	public List<AutocompleteItem> autocompleteConstructs(@QueryParam("term") String term) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String lowerCaseTerm = ObjectTransformer.getString(term, "").toLowerCase();
			// get all construct IDs
			List<Integer> constructIds = DBUtils.select("SELECT id FROM construct", rs -> {
				List<Integer> ids = new ArrayList<>();
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
				return ids;
			});

			Transaction t = TransactionManager.getCurrentTransaction();
			return constructIds.stream().map(id -> {
				try {
					return t.getObject(Construct.class, id);
				} catch (NodeException e) {
					return null;
				}
			}).filter(o -> {
				try {
					return o != null && t.canView(o)
							&& (o.getName().toString().toLowerCase().contains(lowerCaseTerm) || o.getKeyword().toLowerCase().contains(lowerCaseTerm));
				} catch (NodeException e) {
					return false;
				}
			}).limit(10).map(o -> new AutocompleteItem(String.format("%s (%s)", o.getName().toString(), o.getKeyword()), Integer.toString(o.getId()))).collect(Collectors.toList());
		}
	}

	@Override
	@GET
	@Path("/autocomplete/templates")
	public List<AutocompleteItem> autocompleteTemplates(@QueryParam("term") String term) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String lowerCaseTerm = ObjectTransformer.getString(term, "").toLowerCase();

			// get all template IDs
			List<Integer> templateIds = DBUtils.select("SELECT id FROM template WHERE is_master = 1", rs -> {
				List<Integer> ids = new ArrayList<>();
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
				return ids;
			});

			Transaction t = TransactionManager.getCurrentTransaction();
			return templateIds.stream().map(id -> {
				try {
					return t.getObject(Template.class, id);
				} catch (NodeException e) {
					return null;
				}
			}).filter(o -> {
				try {
					return o != null && t.canView(o) && o.getName().toLowerCase().contains(lowerCaseTerm);
				} catch (NodeException e) {
					return false;
				}
			}).limit(10).map(o -> new AutocompleteItem(String.format("%s (%s)", o.getName(), o.getId()), Integer.toString(o.getId()))).collect(Collectors.toList());
		}
	}

	@Override
	@GET
	@Path("/autocomplete/datasources")
	public List<AutocompleteItem> autocompleteDatasources(@QueryParam("term") String term) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String lowerCaseTerm = ObjectTransformer.getString(term, "").toLowerCase();

			// get all datasource IDs
			List<Integer> datasourceIds = DBUtils.select("SELECT id FROM datasource WHERE name IS NOT NULL", rs -> {
				List<Integer> ids = new ArrayList<>();
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
				return ids;
			});

			Transaction t = TransactionManager.getCurrentTransaction();
			return datasourceIds.stream().map(id -> {
				try {
					return t.getObject(Datasource.class, id);
				} catch (NodeException e) {
					return null;
				}
			}).filter(o -> {
				try {
					return o != null && t.canView(o) && o.getName().toLowerCase().contains(lowerCaseTerm);
				} catch (NodeException e) {
					return false;
				}
			}).limit(10).map(o -> new AutocompleteItem(String.format("%s (%s)", o.getName(), o.getId()), Integer.toString(o.getId()))).collect(Collectors.toList());
		}
	}

	@Override
	@GET
	@Path("/autocomplete/objectproperties")
	public List<AutocompleteItem> autocompleteObjectProperties(@QueryParam("term") String term) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String lowerCaseTerm = ObjectTransformer.getString(term, "").toLowerCase();

			// get all objprop IDs
			List<Integer> objectPropertyIds = DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", rs -> {
				List<Integer> ids = new ArrayList<>();
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
				return ids;
			});

			Transaction t = TransactionManager.getCurrentTransaction();
			return objectPropertyIds.stream().map(id -> {
				try {
					return t.getObject(ObjectTagDefinition.class, id);
				} catch (NodeException e) {
					return null;
				}
			}).filter(o -> {
				try {
					return o != null && t.canView(o)
							&& (o.getName().toLowerCase().contains(lowerCaseTerm) || o.getObjectTag().getName().toLowerCase().contains(lowerCaseTerm));
				} catch (NodeException e) {
					return false;
				}
			}).limit(10).map(o -> {
				String name = "";
				try {
					name = String.format("%s/%s (%s)", ObjectTagDefinition.TYPENAME.apply(o), o.getName(), o.getObjectTag().getName());
				} catch (Exception e) {
				}
				return new AutocompleteItem(name, Integer.toString(o.getId()));
			}).collect(Collectors.toList());
		}
	}

	@Override
	@GET
	@Path("/autocomplete/cr_fragments")
	public List<AutocompleteItem> autocompleteCrFragments(@QueryParam("term") String term) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String lowerCaseTerm = ObjectTransformer.getString(term, "").toLowerCase();
			// get all construct IDs
			Set<Integer> crFragmentIds = DBUtils.select("SELECT id FROM cr_fragment", DBUtils.IDS);

			Transaction t = TransactionManager.getCurrentTransaction();
			return crFragmentIds.stream().map(id -> {
				try {
					return t.getObject(CrFragment.class, id);
				} catch (NodeException e) {
					return null;
				}
			}).filter(o -> {
				try {
					return o != null && t.canView(o)
							&& (o.getName().toString().toLowerCase().contains(lowerCaseTerm));
				} catch (NodeException e) {
					return false;
				}
			}).limit(10).map(o -> new AutocompleteItem(o.getName(), Integer.toString(o.getId()))).collect(Collectors.toList());
		}
	}

	@Override
	@GET
	@Path("/autocomplete/contentrepositories")
	public List<AutocompleteItem> autocompleteContentRepositories(@QueryParam("term") String term) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String lowerCaseTerm = ObjectTransformer.getString(term, "").toLowerCase();
			// get all IDs
			Set<Integer> contentRepositoryIds = DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS);

			Transaction t = TransactionManager.getCurrentTransaction();
			return contentRepositoryIds.stream().map(id -> {
				try {
					return t.getObject(ContentRepository.class, id);
				} catch (NodeException e) {
					return null;
				}
			}).filter(o -> {
				try {
					return o != null && t.canView(o)
							&& (o.getName().toString().toLowerCase().contains(lowerCaseTerm));
				} catch (NodeException e) {
					return false;
				}
			}).limit(10).map(o -> new AutocompleteItem(o.getName(), Integer.toString(o.getId()))).collect(Collectors.toList());
		}
	}

	@Override
	@GET
	@Path("/sync")
	public SyncInfo getSyncInfo() throws Exception {
		return DistributionUtil.call(new GetSyncUserTask().setSession(ContentNodeHelper.getSession()));
	}

	@Override
	@PUT
	@Path("/sync")
	public SyncInfo startSync() throws Exception {
		return DistributionUtil.call(new StartSyncTask().setSession(ContentNodeHelper.getSession()));
	}

	@Override
	@DELETE
	@Path("/sync")
	public Response stopSync() throws Exception {
		DistributionUtil.call(new StopSyncTask().setSession(ContentNodeHelper.getSession()));
		return Response.noContent().build();
	}

	/**
	 * Get the named package and return {@link EntityNotFoundException} if not found
	 * @param name package name
	 * @return found package
	 * @throws EntityNotFoundException if the package could not be found
	 */
	protected MainPackageSynchronizer getPackage(String name) throws EntityNotFoundException {
		MainPackageSynchronizer packageSynchronizer = Synchronizer.getPackage(name);
		if (packageSynchronizer == null) {
			throw new EntityNotFoundException(I18NHelper.get("package.notfound", name));
		}
		return packageSynchronizer;
	}

	/**
	 * Get the node with given ID, check for existence and permissions
	 * @param nodeId node ID
	 * @param allowChannels true if channels are allowed, false if not
	 * @return node
	 * @throws NodeException if node not found, no permission or other error
	 */
	protected Node getNode(String nodeId, boolean allowChannels) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.getObject(Node.class, nodeId);

		if (node == null) {
			throw new EntityNotFoundException(I18NHelper.get("rest.node.notfound", nodeId));
		}

		if (!allowChannels && node.isChannel()) {
			throw new EntityNotFoundException(I18NHelper.get("rest.node.notfound", nodeId));
		}

		// TODO check permissions

		return node;
	}

	/**
	 * Get the construct with either local ID or global ID or keyword
	 * @param construct local ID or global ID or keyword
	 * @return construct
	 * @throws NodeException
	 */
	protected Construct getConstruct(String construct) throws NodeException {
		Construct nodeObject = TransactionManager.getCurrentTransaction().getObject(Construct.class, construct);
		if (nodeObject != null) {
			return nodeObject;
		}
		int constructId = DBUtils.select("SELECT id FROM construct WHERE keyword = ?", pst -> {
			pst.setString(1, construct);
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				return 0;
			}
		});
		nodeObject = TransactionManager.getCurrentTransaction().getObject(Construct.class, constructId);
		if (nodeObject == null) {
			throw new EntityNotFoundException(I18NHelper.get("construct.notfound", construct));
		}
		return nodeObject;
	}

	/**
	 * Get the construct with either local ID or global ID or keyword from the package
	 * @param synchronizer package synchronizer
	 * @param construct local ID or global ID or keyword
	 * @return construct
	 * @throws NodeException
	 */
	protected PackageObject<Construct> getConstruct(PackageSynchronizer synchronizer, String construct) throws NodeException {
		return synchronizer.getObjects(Construct.class).stream().filter(c -> {
			return construct.equals(ObjectTransformer.getString(c.getObject().getId(), null))
					|| construct.equals(ObjectTransformer.getString(c.getObject().getGlobalId(), null))
					|| construct.equals(c.getObject().getKeyword());
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("construct.notfound", construct)));
	}

	/**
	 * Get the template with either local ID or global ID or name (if the name is unique)
	 * @param template local ID or global ID or name
	 * @return template or null
	 * @throws NodeException
	 */
	protected Template getTemplate(String template) throws NodeException {
		Template nodeObject = TransactionManager.getCurrentTransaction().getObject(Template.class, template);
		if (nodeObject != null) {
			return nodeObject;
		}

		int templateId = DBUtils.select("SELECT id FROM template WHERE name = ?", pst -> {
			pst.setString(1, template);
		}, rs -> {
			if (rs.next()) {
				int id = rs.getInt("id");
				if (rs.next()) {
					return 0;
				} else {
					return id;
				}
			} else {
				return 0;
			}
		});
		nodeObject = TransactionManager.getCurrentTransaction().getObject(Template.class, templateId);
		if (nodeObject == null) {
			throw new EntityNotFoundException(I18NHelper.get("template.notfound", template));
		}
		return nodeObject;
	}

	/**
	 * Get the template with either local ID or global ID or name from the package
	 * @param synchronizer package synchronizer
	 * @param template local ID or global ID or name
	 * @return template
	 * @throws NodeException
	 */
	protected PackageObject<Template> getTemplate(PackageSynchronizer synchronizer, String template) throws NodeException {
		return synchronizer.getObjects(Template.class).stream().filter(t -> {
			return template.equals(ObjectTransformer.getString(t.getObject().getId(), null))
					|| template.equals(ObjectTransformer.getString(t.getObject().getGlobalId(), null))
					|| template.equals(t.getObject().getName());
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("template.notfound", template)));
	}

	/**
	 * Get the object property definition for the given name (in the form: [type].object.[name]) or local ID or global ID
	 * @param objectProperty name, local ID or global ID
	 * @return object property definition or null if not found
	 * @throws NodeException
	 */
	protected ObjectTagDefinition getObjectProperty(String objectProperty) throws NodeException {
		ObjectTagDefinition nodeObject = TransactionManager.getCurrentTransaction().getObject(ObjectTagDefinition.class, objectProperty);
		if (nodeObject != null) {
			return nodeObject;
		}
		Matcher matcher = ObjectTagDefinitionSynchronizer.FOLDER_NAME_PATTERN.matcher(objectProperty);
		if (!matcher.matches()) {
			throw new EntityNotFoundException(I18NHelper.get("objectproperty.notfound", objectProperty));
		}
		ObjectTagDefinitionTypeModel type = ObjectTagDefinitionTypeModel.valueOf(matcher.group("type"));
		String tagName = "object." + matcher.group("name");

		int objectTagId = DBUtils.select("SELECT id FROM objtag where obj_type = ? AND obj_id = ? AND name = ?", pst -> {
			pst.setInt(1, type.getTypeValue());
			pst.setInt(2, 0);
			pst.setString(3, tagName);
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				return 0;
			}
		});

		nodeObject = TransactionManager.getCurrentTransaction().getObject(ObjectTagDefinition.class, objectTagId);
		if (nodeObject == null) {
			throw new EntityNotFoundException(I18NHelper.get("objectproperty.notfound", objectProperty));
		}
		return nodeObject;
	}

	/**
	 * Get the object property definition for the given name (in the form: [type].object.[name]) or local ID or global ID from the package
	 * @param synchronizer package
	 * @param objectProperty name, local ID or global ID
	 * @return object property definition or null if not found
	 * @throws NodeException
	 */
	protected PackageObject<ObjectTagDefinition> getObjectProperty(PackageSynchronizer synchronizer, String objectProperty) throws NodeException {
		return synchronizer.getObjects(ObjectTagDefinition.class).stream().filter(d -> {
			try {
				return objectProperty.equals(ObjectTransformer.getString(d.getId(), null))
						|| objectProperty.equals(ObjectTransformer.getString(d.getGlobalId(), null))
						|| objectProperty.equals(ObjectTagDefinitionTypeModel.fromValue(d.getObject().getTargetType()) + "." + d.getObject().getObjectTag().getName());
			} catch (NodeException e) {
				return false;
			}
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("objectproperty.notfound", objectProperty)));
	}

	/**
	 * Get the datasource for the given local ID, global ID or name
	 * @param datasource local ID, global ID or name
	 * @return datasource or null
	 * @throws NodeException
	 */
	protected Datasource getDatasource(String datasource) throws NodeException {
		Datasource nodeObject = TransactionManager.getCurrentTransaction().getObject(Datasource.class, datasource);
		if (nodeObject != null) {
			return nodeObject;
		}
		int datasourceId = DBUtils.select("SELECT id FROM datasource WHERE name = ?", pst -> {
			pst.setString(1, datasource);
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				return 0;
			}
		});
		nodeObject = TransactionManager.getCurrentTransaction().getObject(Datasource.class, datasourceId);
		if (nodeObject == null) {
			throw new EntityNotFoundException(I18NHelper.get("datasource.notfound", datasource));
		}
		return nodeObject;
	}

	/**
	 * Get the datasource for the given local ID, global ID or name from the package
	 * @param synchronizer package
	 * @param datasource local ID, global ID or name
	 * @return datasource or null
	 * @throws NodeException
	 */
	protected PackageObject<Datasource> getDatasource(PackageSynchronizer synchronizer, String datasource) throws NodeException {
		return synchronizer.getObjects(Datasource.class).stream().filter(d -> {
			return datasource.equals(ObjectTransformer.getString(d.getObject().getId(), null))
					|| datasource.equals(ObjectTransformer.getString(d.getObject().getGlobalId(), null))
					|| datasource.equals(d.getObject().getName());
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("datasource.notfound", datasource)));
	}

	/**
	 * Get the CR Fragment with either local ID or global ID or name
	 * @param crFragment local ID or global ID or name
	 * @return CR Fragment
	 * @throws NodeException
	 */
	protected CrFragment getCrFragment(String crFragment) throws NodeException {
		CrFragment nodeObject = TransactionManager.getCurrentTransaction().getObject(CrFragment.class, crFragment);
		if (nodeObject != null) {
			return nodeObject;
		}
		int crFragmentId = DBUtils.select("SELECT id FROM cr_fragment WHERE name = ?", pst -> {
			pst.setString(1, crFragment);
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				return 0;
			}
		});
		nodeObject = TransactionManager.getCurrentTransaction().getObject(CrFragment.class, crFragmentId);
		if (nodeObject == null) {
			throw new EntityNotFoundException(I18NHelper.get("cr_fragment.notfound", crFragment));
		}
		return nodeObject;
	}

	/**
	 * Get the CR Fragment with either local ID or global ID or name from the package
	 * @param synchronizer package synchronizer
	 * @param crFragment local ID or global ID or name
	 * @return CR Fragment
	 * @throws NodeException
	 */
	protected PackageObject<CrFragment> getCrFragment(PackageSynchronizer synchronizer, String crFragment) throws NodeException {
		return synchronizer.getObjects(CrFragment.class).stream().filter(c -> {
			return crFragment.equals(ObjectTransformer.getString(c.getObject().getId(), null))
					|| crFragment.equals(ObjectTransformer.getString(c.getObject().getGlobalId(), null))
					|| crFragment.equals(c.getObject().getName());
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("cr_fragment.notfound", crFragment)));
	}

	/**
	 * Get the ContentRepository with either local ID or global ID or name
	 * @param contentRepository local ID or global ID or name
	 * @return ContentRepository
	 * @throws NodeException
	 */
	protected ContentRepository getContentRepository(String contentRepository) throws NodeException {
		ContentRepository nodeObject = TransactionManager.getCurrentTransaction().getObject(ContentRepository.class, contentRepository);
		if (nodeObject != null) {
			return nodeObject;
		}
		int contentRepositoryId = DBUtils.select("SELECT id FROM contentrepository WHERE name = ?", pst -> {
			pst.setString(1, contentRepository);
		}, rs -> {
			if (rs.next()) {
				return rs.getInt("id");
			} else {
				return 0;
			}
		});
		nodeObject = TransactionManager.getCurrentTransaction().getObject(ContentRepository.class, contentRepositoryId);
		if (nodeObject == null) {
			throw new EntityNotFoundException(I18NHelper.get("contentrepository.notfound", contentRepository));
		}
		return nodeObject;
	}

	/**
	 * Get the ContentRepository with either local ID or global ID or name from the package
	 * @param synchronizer package synchronizer
	 * @param contentRepository local ID or global ID or name
	 * @return ContentRepository
	 * @throws NodeException
	 */
	protected PackageObject<ContentRepository> getContentRepository(PackageSynchronizer synchronizer, String contentRepository) throws NodeException {
		return synchronizer.getObjects(ContentRepository.class).stream().filter(c -> {
			return contentRepository.equals(ObjectTransformer.getString(c.getObject().getId(), null))
					|| contentRepository.equals(ObjectTransformer.getString(c.getObject().getGlobalId(), null))
					|| contentRepository.equals(c.getObject().getName());
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("contentrepository.notfound", contentRepository)));
	}

	/**
	 * Get sorted paged list of packages
	 * @param packageNames set of package names
	 * @param query optional query for filtering
	 * @param sort optional sorting info
	 * @param page returned page
	 * @param pageSize page size
	 * @return list response
	 * @throws NodeException in case of errors
	 */
	protected PackageListResponse getSortedPagedList(Set<String> packageNames, FilterParameterBean filter, SortParameterBean sorting,
			PagingParameterBean paging) throws NodeException {
		// transform the set of package names into a set of Resolvable packages
		Set<ResolvablePackage> packageSet = new HashSet<>();
		for (String name : packageNames) {
			packageSet.add(new ResolvablePackage(MainPackageSynchronizer.TRANSFORM2REST.apply(Synchronizer.getPackage(name))));
		}

		return ListBuilder.from(packageSet, p -> p.pack).filter(ResolvableFilter.get(filter, "name")).sort(
				ResolvableComparator.get(sorting, "name", "constructs", "datasources", "templates", "objectProperties", "crFragments", "contentRepositories"))
				.page(paging).to(new PackageListResponse());
	}

	/**
	 * Resolvable wrapper around package REST Models
	 */
	protected static class ResolvablePackage implements Resolvable {
		/**
		 * Wrapped package
		 */
		protected Package pack;

		/**
		 * Create an instance
		 * @param pack package
		 */
		public ResolvablePackage(Package pack) {
			this.pack = pack;
		}

		@Override
		public Object getProperty(String key) {
			return get(key);
		}

		@Override
		public Object get(String key) {
			if ("name".equals(key)) {
				return pack.getName();
			} else if ("constructs".equals(key)) {
				return pack.getConstructs();
			} else if ("templates".equals(key)) {
				return pack.getTemplates();
			} else if ("datasources".equals(key)) {
				return pack.getDatasources();
			} else if ("objectProperties".equals(key)) {
				return pack.getObjectProperties();
			} else if ("crFragments".equals(key)) {
				return pack.getCrFragments();
			} else if ("contentRepositories".equals(key)) {
				return pack.getContentRepositories();
			}
			return null;
		}

		@Override
		public boolean canResolve() {
			return true;
		}
	}

	/**
	 * Task to get the sync user ID
	 */
	public static class GetSyncUserTask extends TrxCallable<SyncInfo> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 4037098053025380059L;

		@Override
		protected SyncInfo callWithTrx() throws NodeException {
			int userId = 0;
			if (Synchronizer.getStatus() == Status.UP && (userId = Synchronizer.getUserId()) != 0) {
				Transaction t = TransactionManager.getCurrentTransaction();
				SystemUser user = t.getObject(SystemUser.class, userId);
				return new SyncInfo(true, SystemUser.TRANSFORM2REST.apply(user));
			} else {
				return new SyncInfo(false, null);
			}
		}
	}

	/**
	 * Task to start the sync
	 */
	public static class StartSyncTask extends TrxCallable<SyncInfo> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 6049652725202337141L;

		@Override
		protected SyncInfo callWithTrx() throws NodeException {
			if (Synchronizer.getStatus() == Status.UP && !Synchronizer.isEnabled()) {
				Transaction t = TransactionManager.getCurrentTransaction();
				Synchronizer.enable(t.getUserId());
				SystemUser user = t.getObject(SystemUser.class, Synchronizer.getUserId());
				return new SyncInfo(true, SystemUser.TRANSFORM2REST.apply(user));
			} else {
				throw new DistributedWebApplicationException(I18NHelper.get("devtools.autosync.error.enabled"), Response.Status.CONFLICT.getStatusCode());
			}
		}
	}

	/**
	 * Task to stop the sync
	 */
	public static class StopSyncTask extends TrxCallable<Boolean> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -6707465826357269077L;

		@Override
		protected Boolean callWithTrx() throws NodeException {
			if (Synchronizer.getStatus() == Status.UP && Synchronizer.isEnabled()) {
				Synchronizer.disable();
				return true;
			} else {
				throw new DistributedWebApplicationException(I18NHelper.get("devtools.autosync.error.disabled"), Response.Status.CONFLICT.getStatusCode());
			}
		}
	}
}
