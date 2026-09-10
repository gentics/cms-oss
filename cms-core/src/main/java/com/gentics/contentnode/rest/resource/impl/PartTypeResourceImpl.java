package com.gentics.contentnode.rest.resource.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.PartTypeFactory;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.resolving.ResolvableWrapper;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.PartType;
import com.gentics.contentnode.rest.model.response.PartTypeListResponse;
import com.gentics.contentnode.rest.resource.PartTypeResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PartTypeListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.Filter;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/parttype")
public class PartTypeResourceImpl implements PartTypeResource {

	@GET
	public PartTypeListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,  @BeanParam PartTypeListParameterBean partTypeFilter) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PartTypeFactory factory = PartTypeFactory.getInstance();
			List<ResolvableWrapper<PartType>> partTypes = DBUtils.select("SELECT * FROM `type`", rs -> {
				List<ResolvableWrapper<PartType>> types = new ArrayList<>();
				while (rs.next()) {
					PartType partType = new PartType()
						.setId(rs.getInt("id"))
						.setName(rs.getString("name"))
						.setDescription(rs.getString("description"))
						.setJavaClass(rs.getString("javaclass"))
						.setDeprecated(rs.getBoolean("deprecated"));

					if (factory.isAvailable(partType.getId())) {
						types.add(new ResolvableWrapper<>(partType));
					}

				}
				return types;
			});

			trx.success();

			Filter<ResolvableWrapper<PartType>> deprecatedFilter = Optional.ofNullable(partTypeFilter)
					.map(f -> f.deprecated).map(deprecated -> (Filter<ResolvableWrapper<PartType>>) object -> object
							.unwrap().isDeprecated() == deprecated)
					.orElse(null);

			return ListBuilder.from(partTypes, ResolvableWrapper::unwrap)
					.filter(deprecatedFilter)
					.filter(ResolvableFilter.get(filter, "name", "description", "javaClass"))
					.sort(ResolvableComparator.get(sorting, "id", "name", "javaClass", "deprecated"))
					.page(paging)
					.to(new PartTypeListResponse());
		}
	}
}
