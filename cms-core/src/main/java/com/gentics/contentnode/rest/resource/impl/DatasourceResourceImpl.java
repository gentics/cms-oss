package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.NEW_FIELD_CHECKER;
import static com.gentics.contentnode.rest.util.MiscUtils.UPDATE_FIELD_CHECKER;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBody;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBodyWithFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.checkFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
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
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.Datasource;
import com.gentics.contentnode.rest.model.DatasourceEntryModel;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.ConstructList;
import com.gentics.contentnode.rest.model.response.DatasourceEntryListResponse;
import com.gentics.contentnode.rest.model.response.DatasourceEntryResponse;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.model.response.PagedDatasourceListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.DatasourceResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;

@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/datasource")
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = com.gentics.contentnode.object.Datasource.TYPE_DATASOURCE, bit = PermHandler.PERM_VIEW)
public class DatasourceResourceImpl implements DatasourceResource {
	@GET
	@Override
	public PagedDatasourceListResponse list(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Set<Integer> ids = DBUtils.select("SELECT id FROM datasource WHERE name != ''", DBUtils.IDS);
			trx.success();
			return ListBuilder.from(trx.getTransaction().getObjects(com.gentics.contentnode.object.Datasource.class, ids), com.gentics.contentnode.object.Datasource.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "type"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "type"))
					.page(paging).to(new PagedDatasourceListResponse());
		}
	}

	@POST
	@Override
	public DatasourceLoadResponse create(Datasource datasource) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canCreate(null, com.gentics.contentnode.object.Datasource.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("datasource.nopermission"), null, null,
						com.gentics.contentnode.object.Datasource.TYPE_DATASOURCE, 0, PermType.create);
			}

			checkBodyWithFunction(datasource, NEW_FIELD_CHECKER, d -> Pair.of(I18NHelper.get("name"), d.getName()), d -> Pair.of(I18NHelper.get("type"), d.getType()));

			// check for uniqueness of name
			if (UniquifyHelper.findConflictingValues(datasource.getName(), "SELECT name FROM datasource WHERE name = ?")) {
				throw new DuplicateValueException("name", datasource.getName());
			}

			com.gentics.contentnode.object.Datasource nodeDatasource = com.gentics.contentnode.object.Datasource.REST2NODE
					.apply(datasource, t.createObject(com.gentics.contentnode.object.Datasource.class));
			nodeDatasource.save();

			DatasourceLoadResponse response = new DatasourceLoadResponse(
					ResponseInfo.ok("Successfully created datasource"),
					com.gentics.contentnode.object.Datasource.TRANSFORM2REST.apply(t.getObject(nodeDatasource)));

			trx.success();
			return response;
		}
	}

	@GET
	@Path("/{id}")
	@Override
	public DatasourceLoadResponse get(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.Datasource datasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id);

			DatasourceLoadResponse response = new DatasourceLoadResponse(
					ResponseInfo.ok("Successfully loaded datasource"),
					com.gentics.contentnode.object.Datasource.TRANSFORM2REST.apply(datasource));
			trx.success();
			return response;
		}
	}

	@PUT
	@Path("/{id}")
	@Override
	public DatasourceLoadResponse update(@PathParam("id") String id, Datasource datasource) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBody(datasource);

			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.Datasource nodeDatasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id, ObjectPermission.edit);

			// check for uniqueness of name
			if (!StringUtils.isBlank(datasource.getName()) && UniquifyHelper.findConflictingValues(datasource.getName(),
					"SELECT name FROM datasource WHERE id != ? AND name = ?", nodeDatasource.getId())) {
				throw new DuplicateValueException("name", datasource.getName());
			}

			nodeDatasource = com.gentics.contentnode.object.Datasource.REST2NODE.apply(datasource,
					t.getObject(nodeDatasource, true));
			nodeDatasource.save();

			DatasourceLoadResponse response = new DatasourceLoadResponse(
					ResponseInfo.ok("Successfully updated datasource"),
					com.gentics.contentnode.object.Datasource.TRANSFORM2REST.apply(t.getObject(nodeDatasource)));

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
			com.gentics.contentnode.object.Datasource nodeDatasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id, ObjectPermission.delete);

			// check whether datasource is still in use
			int usageCount = DBUtils.select(
					"SELECT COUNT(*) c FROM construct c INNER JOIN part p ON p.construct_id = c.id WHERE p.type_id IN (29,30) AND p.info_int = ?",
					stmt -> {
						stmt.setInt(1, nodeDatasource.getId());
					}, DBUtils.firstInt("c"));

			if (usageCount > 0) {
				throw new RestMappedException(I18NHelper.get("datasource_delete_error"))
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			t.getObject(nodeDatasource, true).delete();

			trx.success();
			return Response.noContent().build();
		}
	}

	@GET
	@Path("/{id}/constructs")
	@Override
	public ConstructList constructs(@PathParam("id") String id, @BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.Datasource datasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id);

			Set<Integer> constructIds = DBUtils.select(
					"SELECT c.id FROM construct c INNER JOIN part p ON p.construct_id = c.id WHERE p.type_id IN (29,30) AND p.info_int = ?",
					stmt -> {
						stmt.setInt(1, datasource.getId());
					}, DBUtils.IDS);
			List<Construct> constructs = trx.getTransaction().getObjects(Construct.class, constructIds);

			ConstructList response = ListBuilder.from(constructs, Construct.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "keyword", "description", "category"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "keyword", "description", "category"))
					.page(paging).to(new ConstructList());

			trx.success();
			return response;
		}
	}

	@GET
	@Path("/{id}/entries")
	@Override
	public DatasourceEntryListResponse listEntries(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.Datasource datasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id);

			DatasourceEntryListResponse response = ListBuilder.from(datasource.getEntries(), DatasourceEntry.TRANSFORM2REST)
					.to(new DatasourceEntryListResponse());

			trx.success();
			return response;
		}
	}

	@POST
	@Path("/{id}/entries")
	@Override
	public DatasourceEntryResponse addEntry(@PathParam("id") String id, DatasourceEntryModel item) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBodyWithFunction(item, NEW_FIELD_CHECKER, i -> Pair.of(I18NHelper.get("key"), i.getKey()), i -> Pair.of(I18NHelper.get("value"), i.getValue()));

			com.gentics.contentnode.object.Datasource datasource = MiscUtils.load(com.gentics.contentnode.object.Datasource.class, id, ObjectPermission.edit);

			// check uniqueness of dsid (if given), key and value
			if (UniquifyHelper.findConflictingValues(item.getKey(),
					"SELECT dskey FROM datasource_value WHERE datasource_id = ? AND dskey = ?", datasource.getId())) {
				throw new DuplicateValueException("key", item.getKey());
			}
			if (UniquifyHelper.findConflictingValues(item.getValue(),
					"SELECT value FROM datasource_value WHERE datasource_id = ? AND value = ?", datasource.getId())) {
				throw new DuplicateValueException("value", item.getValue());
			}
			if (item.getDsId() != null && UniquifyHelper.findConflictingValues(item.getDsId(),
					"SELECT dsid FROM datasource_value WHERE datasource_id = ? AND dsid = ?", datasource.getId())) {
				throw new DuplicateValueException("dsId", Integer.toString(item.getDsId()));
			}

			// determine current maximum dsid
			AtomicInteger maxDsId = new AtomicInteger(datasource.getEntries().stream().mapToInt(DatasourceEntry::getDsid).max().orElse(0));

			datasource = trx.getTransaction().getObject(datasource, true);

			DatasourceEntry entry = DatasourceEntry.REST2NODE.apply(item,
					trx.getTransaction().createObject(DatasourceEntry.class));

			// if no dsid given, set it to the next free value
			if (item.getDsId() == null) {
				entry.setDsid(maxDsId.incrementAndGet());
			}

			datasource.getEntries().add(entry);
			datasource.save();
			trx.success();
			return new DatasourceEntryResponse(new ResponseInfo(ResponseCode.OK, "Successfully created datasource entry"),
					DatasourceEntry.TRANSFORM2REST.apply(entry));
		}
	}

	@GET
	@Path("/{id}/entries/{entryId}")
	@Override
	public DatasourceEntryResponse getEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.Datasource datasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id);

			DatasourceEntry entry = MiscUtils.get(DatasourceEntry.class, datasource.getEntries(), entryId);
			DatasourceEntryResponse response = new DatasourceEntryResponse(
					new ResponseInfo(ResponseCode.OK, "Successfully fetched datasource entry"),
					DatasourceEntry.TRANSFORM2REST.apply(entry));
			trx.success();

			return response;
		}
	}

	@PUT
	@Path("/{id}/entries/{entryId}")
	@Override
	public DatasourceEntryResponse updateEntry(@PathParam("id") String id, @PathParam("entryId") String entryId, DatasourceEntryModel item) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBodyWithFunction(item, UPDATE_FIELD_CHECKER, i -> Pair.of(I18NHelper.get("key"), i.getKey()), i -> Pair.of(I18NHelper.get("value"), i.getValue()));

			com.gentics.contentnode.object.Datasource datasource = MiscUtils.load(com.gentics.contentnode.object.Datasource.class, id, ObjectPermission.edit);
			DatasourceEntry entry = MiscUtils.get(DatasourceEntry.class, datasource.getEntries(), entryId);

			// check uniqueness
			if (!StringUtils.isBlank(item.getKey()) && UniquifyHelper.findConflictingValues(item.getKey(),
					"SELECT dskey FROM datasource_value WHERE datasource_id = ? AND id != ? AND dskey = ?",
					datasource.getId(), entry.getId())) {
				throw new DuplicateValueException("key", item.getKey());
			}
			if (!StringUtils.isBlank(item.getValue()) && UniquifyHelper.findConflictingValues(item.getValue(),
					"SELECT value FROM datasource_value WHERE datasource_id = ? AND id != ? AND value = ?",
					datasource.getId(), entry.getId())) {
				throw new DuplicateValueException("value", item.getValue());
			}
			// dsid cannot be changed
			item.setDsId(null);

			// get editable copy and do the update
			datasource = trx.getTransaction().getObject(datasource, true);
			entry = MiscUtils.get(DatasourceEntry.class, datasource.getEntries(), entryId);

			entry = DatasourceEntry.REST2NODE.apply(item, entry);
			datasource.save();
			trx.success();

			return new DatasourceEntryResponse(new ResponseInfo(ResponseCode.OK, "Successfully updated datasource entry"),
					DatasourceEntry.TRANSFORM2REST.apply(entry));
		}
	}

	@DELETE
	@Path("/{id}/entries/{entryId}")
	@Override
	public Response deleteEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			com.gentics.contentnode.object.Datasource datasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id, ObjectPermission.edit);
			datasource = trx.getTransaction().getObject(datasource, true);
			DatasourceEntry entry = MiscUtils.get(DatasourceEntry.class, datasource.getEntries(), entryId);

			datasource.getEntries().remove(entry);
			datasource.save();
			trx.success();

			return Response.noContent().build();
		}
	}

	@PUT
	@Path("/{id}/entries")
	@Override
	public DatasourceEntryListResponse updateEntryList(@PathParam("id") String id, List<DatasourceEntryModel> items) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBody(items);

			com.gentics.contentnode.object.Datasource datasource = MiscUtils
					.load(com.gentics.contentnode.object.Datasource.class, id, ObjectPermission.edit);
			datasource = trx.getTransaction().getObject(datasource, true);

			// determine current maximum dsid
			AtomicInteger maxDsId = new AtomicInteger(datasource.getEntries().stream().mapToInt(DatasourceEntry::getDsid).max().orElse(0));

			// get the current entries as Map id -> entry, in order to find entries, which are updated with the request
			Map<Integer, DatasourceEntry> entryMap = new HashMap<>(datasource.getEntries().stream().collect(Collectors.toMap(e -> e.getId(), e -> e)));

			// prepare new list
			List<DatasourceEntry> newEntries = new ArrayList<>();

			for (DatasourceEntryModel item : items) {
				DatasourceEntry entry = null;
				// check whether entry already exists, and needs to be updated
				if (item.getId() != null) {
					entry = entryMap.get(item.getId());
				}

				// if entry did not exist, we need to create a new one
				if (entry == null) {
					entry = trx.getTransaction().createObject(DatasourceEntry.class);

					// for new entries, sufficient data needs to be provided
					checkFields(NEW_FIELD_CHECKER, () -> Pair.of(I18NHelper.get("key"), item.getKey()), () -> Pair.of(I18NHelper.get("value"), item.getValue()));

					// if no dsid was given, create a new one
					if (item.getDsId() == null) {
						item.setDsId(maxDsId.incrementAndGet());
					}
				} else {
					// for updating, we check that nothing is set to ""
					checkFields(UPDATE_FIELD_CHECKER, () -> Pair.of(I18NHelper.get("key"), item.getKey()), () -> Pair.of(I18NHelper.get("value"), item.getValue()));
				}

				// update the entry with the given data
				entry = DatasourceEntry.REST2NODE.apply(item, entry);

				// add the entry to the new list
				newEntries.add(entry);
			}

			// check uniqueness
			Set<String> keys = new HashSet<>();
			Set<String> values = new HashSet<>();
			Set<Integer> dsIds = new HashSet<>();
			for (DatasourceEntry entry : newEntries) {
				if (!keys.add(entry.getKey())) {
					throw new DuplicateValueException("key", entry.getKey());
				}
				if (!values.add(entry.getValue())) {
					throw new DuplicateValueException("value", entry.getValue());
				}
				if (!dsIds.add(entry.getDsid())) {
					throw new DuplicateValueException("dsId", Integer.toString(entry.getDsid()));
				}
			}

			// clear the original list and add entries from the new list (in order)
			List<DatasourceEntry> entries = datasource.getEntries();
			entries.clear();
			entries.addAll(newEntries);

			// save datasource and reload
			datasource.save();
			// we need to commit the transaction (without closing), in order to have entries
			// deleted (which is done at commit time). Otherwise, deleted entries would still be contained in the result
			trx.getTransaction().commit(false);
			datasource = datasource.reload();

			DatasourceEntryListResponse response = ListBuilder.from(datasource.getEntries(), DatasourceEntry.TRANSFORM2REST)
					.to(new DatasourceEntryListResponse());

			trx.success();
			return response;
		}
	}
}
