package com.gentics.contentnode.rest.resource.impl.search;

import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.object.search.SearchIndex;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.resolving.ResolvableWrapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.IndexList;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.search.IndexModel;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.search.SearchIndexResource;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;

/**
 * Resource implementation
 */
@Produces({ MediaType.APPLICATION_JSON })
@Path("/index")
@Authenticated
@RequiredFeature(Feature.ELASTICSEARCH)
@RequiredPerm(type=PermHandler.TYPE_ADMIN, bit=PermHandler.PERM_VIEW)
@RequiredPerm(type=PermHandler.TYPE_CONADMIN, bit=PermHandler.PERM_VIEW)
@RequiredPerm(type=Indexer.TYPE_SEARCH_MAINTENANCE, bit=PermHandler.PERM_VIEW)
public class SearchIndexResourceImpl implements SearchIndexResource {
	@Override
	@GET
	public IndexList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Indexer.updateIndexStats();
			List<ResolvableWrapper<IndexModel>> indices = Indexer.flowableIndices().map(index -> SearchIndex.TRANSFORM2REST.apply(index))
					.map(index -> new ResolvableWrapper<>(index)).toList().blockingGet();

			IndexList response = ListBuilder.from(indices, ResolvableWrapper::unwrap)
					.filter(ResolvableFilter.get(filter, "name"))
					.sort(ResolvableComparator.get(sorting, "name", "found", "settingsValid", "mappingValid", "indexed", "objects"))
					.page(paging)
					.to(new IndexList());
			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{name}/rebuild")
	public GenericResponse rebuild(@PathParam("name") String name, @QueryParam("drop") @DefaultValue("false") boolean drop) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			SearchIndex searchIndex = Indexer.flowableIndices().filter(index -> StringUtils.equals(index.getName(), name)).blockingFirst(null);
			if (searchIndex == null) {
				throw new EntityNotFoundException(I18NHelper.get("index.notfound", name));
			}
			Indexer.reIndex(searchIndex, drop);
			trx.success();
			return new GenericResponse(null, ResponseInfo.ok(""));
		}
	}
}
