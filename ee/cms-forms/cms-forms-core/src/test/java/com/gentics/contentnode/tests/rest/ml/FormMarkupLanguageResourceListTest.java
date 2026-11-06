package com.gentics.contentnode.tests.rest.ml;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.rest.model.MarkupLanguage;
import com.gentics.contentnode.rest.resource.MarkupLanguageResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link MarkupLanguageResource#list(SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
@GCNFeature(set = {Feature.FORMS})
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormMarkupLanguageResourceListTest extends MarkupLanguageResourceListTest {
	@Override
	protected void fillItemsList(List<? super MarkupLanguage> items) throws NodeException {
		operate(t ->  {
			List<com.gentics.contentnode.object.MarkupLanguage> mls = t.getObjects(
					com.gentics.contentnode.object.MarkupLanguage.class,
					DBUtils.select("SELECT id FROM ml", DBUtils.IDS));
			for (com.gentics.contentnode.object.MarkupLanguage ml : mls) {
				items.add(com.gentics.contentnode.object.MarkupLanguage.TRANSFORM2REST.apply(ml));
			}
		});
	}
}
