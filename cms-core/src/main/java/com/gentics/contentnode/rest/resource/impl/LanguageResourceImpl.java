package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.NEW_FIELD_CHECKER;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBody;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBodyWithFunction;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.DuplicateValueException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.ContentLanguageResponse;
import com.gentics.contentnode.rest.model.response.LanguageList;
import com.gentics.contentnode.rest.model.response.LanguageListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.LanguageResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.lib.db.IntegerColumnRetriever;

/**
 * Implementation of a language resource
 */
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Authenticated
@Path("language")
public class LanguageResourceImpl implements LanguageResource {
	@GET
	@Override
	public LanguageList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<com.gentics.contentnode.object.ContentLanguage> languages = trx.getTransaction().getObjects(com.gentics.contentnode.object.ContentLanguage.class,
					DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS));

			LanguageList response = ListBuilder.from(languages, com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST)
				.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
				.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "code"))
				.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "code"))
				.page(paging).to(new LanguageList());

			trx.success();
			return response;
		}
	}

	@POST
	@Override
	public ContentLanguageResponse create(ContentLanguage language) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canCreate(null, com.gentics.contentnode.object.ContentLanguage.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("contentgroup.nopermission"), null, null,
						com.gentics.contentnode.object.ContentLanguage.TYPE_CONTENTGROUP, 0, PermType.create);
			}

			checkBodyWithFunction(language, NEW_FIELD_CHECKER, d -> Pair.of(I18NHelper.get("name"), d.getName()), d -> Pair.of(I18NHelper.get("code"), d.getCode()));

			// check for uniqueness of code
			if (UniquifyHelper.findConflictingValues(language.getCode(), "SELECT code FROM contentgroup WHERE code = ?")) {
				throw new DuplicateValueException("code", language.getCode());
			}

			// check for uniqueness of name
			if (UniquifyHelper.findConflictingValues(language.getName(), "SELECT name FROM contentgroup WHERE name = ?")) {
				throw new DuplicateValueException("name", language.getName());
			}

			com.gentics.contentnode.object.ContentLanguage nodeLanguage = com.gentics.contentnode.object.ContentLanguage.REST2NODE
					.apply(language, t.createObject(com.gentics.contentnode.object.ContentLanguage.class));
			nodeLanguage.save();

			ContentLanguageResponse response = new ContentLanguageResponse(
					ResponseInfo.ok("Successfully created datasource"),
					com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(t.getObject(nodeLanguage)));

			trx.success();
			return response;
		}
	}

	@GET
	@Path("/{id}")
	@Override
	public ContentLanguageResponse get(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.ContentLanguage language = MiscUtils
					.load(com.gentics.contentnode.object.ContentLanguage.class, id);
			ContentLanguageResponse response = new ContentLanguageResponse(
					ResponseInfo.ok("Successfully loaded content language"),
					com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(language));
			trx.success();
			return response;
		}
	}

	@PUT
	@Path("/{id}")
	@Override
	public ContentLanguageResponse update(@PathParam("id") String id, ContentLanguage language) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBody(language);

			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.ContentLanguage nodeLanguage = MiscUtils
					.load(com.gentics.contentnode.object.ContentLanguage.class, id, ObjectPermission.edit);

			// check for uniqueness of code
			if (!StringUtils.isBlank(language.getCode()) && UniquifyHelper.findConflictingValues(language.getCode(),
					"SELECT code FROM contentgroup WHERE id != ? AND code = ?", nodeLanguage.getId())) {
				throw new DuplicateValueException("code", language.getCode());
			}

			// check for uniqueness of name
			if (!StringUtils.isBlank(language.getName()) && UniquifyHelper.findConflictingValues(language.getName(),
					"SELECT name FROM contentgroup WHERE id != ? AND name = ?", nodeLanguage.getId())) {
				throw new DuplicateValueException("name", language.getName());
			}

			nodeLanguage = com.gentics.contentnode.object.ContentLanguage.REST2NODE.apply(language,
					t.getObject(nodeLanguage, true));
			nodeLanguage.save();

			ContentLanguageResponse response = new ContentLanguageResponse(
					ResponseInfo.ok("Successfully updated datasource"),
					com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(t.getObject(nodeLanguage)));

			trx.success();
			return response;
		}
	}

	@DELETE
	@Path("/{id}")
	@Override
	public Response delete(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.ContentLanguage nodeLanguage = MiscUtils
					.load(com.gentics.contentnode.object.ContentLanguage.class, id, ObjectPermission.delete);

			// check whether language is still in use
			Pair<Integer, Integer> usageCounts = DBUtils.select("SELECT COUNT(*) c, IF(deleted > 0, 1, 0) d FROM page WHERE contentgroup_id = ? GROUP BY d", stmt -> {
				stmt.setInt(1, nodeLanguage.getId());
			}, rs -> {
				int using = 0;
				int usingInWastebin = 0;
				while (rs.next()) {
					using += rs.getInt("c");
					if (rs.getInt("d") == 1) {
						usingInWastebin += rs.getInt("c");
					}
				}
				return Pair.of(using, usingInWastebin);
			});

			if (usageCounts.getLeft() > 0) {
				String msg = null;
				if (usageCounts.getRight() > 0) {
					msg = I18NHelper.get("contentgroup.delete.used.wastebin", Integer.toString(usageCounts.getLeft()),
							Integer.toString(usageCounts.getRight()));
				} else {
					msg = I18NHelper.get("contentgroup.delete.used", Integer.toString(usageCounts.getLeft()));
				}
				throw new RestMappedException(msg).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			t.getObject(nodeLanguage, true).delete();

			trx.success();
			return Response.noContent().build();
		}
	}

	@GET
	@Path("list")
	@Override
	public LanguageListResponse list() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			IntegerColumnRetriever ids = new IntegerColumnRetriever("id");
			DBUtils.executeStatement("SELECT id FROM contentgroup", ids);
			List<com.gentics.contentnode.object.ContentLanguage> languages = t.getObjects(com.gentics.contentnode.object.ContentLanguage.class, ids.getValues());
			LanguageListResponse response = new LanguageListResponse(null, new ResponseInfo(ResponseCode.OK, ""));
			List<ContentLanguage> restLanguages = new ArrayList<ContentLanguage>(languages.size());
			for (com.gentics.contentnode.object.ContentLanguage language : languages) {
				restLanguages.add(com.gentics.contentnode.object.ContentLanguage.TRANSFORM2REST.apply(language));
			}
			response.setLanguages(restLanguages);
			trx.success();
			return response;
		}
	}
}
