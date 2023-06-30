package com.gentics.contentnode.rest.resource.impl;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.response.MarkupLanguageListResponse;
import com.gentics.contentnode.rest.resource.MarkupLanguageResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Implementation of the markupLanguage resource
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/markupLanguage")
public class MarkupLanguageResourceImpl implements MarkupLanguageResource {

	@Override
	@GET
	public MarkupLanguageListResponse list(@BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter, @BeanParam PagingParameterBean paging)
			throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<MarkupLanguage> mls = trx.getTransaction().getObjects(MarkupLanguage.class, DBUtils.select("SELECT id FROM ml", DBUtils.IDS));

			MarkupLanguageListResponse response = ListBuilder.from(mls, MarkupLanguage.TRANSFORM2REST)
					.filter(ml -> {
						Feature feature = ml.getFeature();
						if (feature != null) {
							return NodeConfigRuntimeConfiguration.isFeature(feature);
						} else {
							return true;
						}
					})
					.filter(ResolvableFilter.get(filter, "id", "name", "extension", "contenttype", "feature"))
					.sort(ResolvableComparator.get(sort, "id", "name", "extension", "contenttype", "feature", "excludeFromPublishing"))
					.page(paging)
					.to(new MarkupLanguageListResponse());
			trx.success();
			return response;
		}
	}
}
