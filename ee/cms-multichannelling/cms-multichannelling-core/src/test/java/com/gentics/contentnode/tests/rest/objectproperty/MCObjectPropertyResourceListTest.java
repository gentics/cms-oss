package com.gentics.contentnode.tests.rest.objectproperty;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.rest.resource.ObjectPropertyResource;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ObjectPropertyParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link ObjectPropertyResource#list(SortParameterBean, FilterParameterBean, PagingParameterBean, ObjectPropertyParameterBean, EmbedParameterBean)}
 */
@GCNFeature(set = {Feature.OBJTAG_SYNC, Feature.MULTICHANNELLING})
public class MCObjectPropertyResourceListTest extends ObjectPropertyResourceListTest {
}
