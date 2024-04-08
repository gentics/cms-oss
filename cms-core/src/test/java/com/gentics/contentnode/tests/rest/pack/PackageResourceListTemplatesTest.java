package com.gentics.contentnode.tests.rest.pack;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.devtools.TemplateInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PackageResource#listTemplates(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class PackageResourceListTemplatesTest extends AbstractListSortAndFilterTest<TemplateInPackage> {
	protected static String packageName;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();
		Synchronizer.start();

		packageName = randomStringGenerator.generate(5, 10);
		operate(() -> Synchronizer.addPackage(packageName));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<TemplateInPackage, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", TemplateInPackage::getName),
				Pair.of("description", TemplateInPackage::getDescription)
		);
		List<Pair<String, Function<TemplateInPackage, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", TemplateInPackage::getName),
				Pair.of("description", TemplateInPackage::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected TemplateInPackage createItem() throws NodeException {
		Template item = Builder.create(Template.class, t -> {
			t.setName(randomStringGenerator.generate(5, 10));
			t.setDescription(randomStringGenerator.generate(10, 20));
		}).build();

		consume(i -> Synchronizer.getPackage(packageName).synchronize(i, true), item);

		return execute(c -> {
			TemplateInPackage ip = new TemplateInPackage();
			ip.setPackageName(packageName);
			Template.NODE2REST.apply(item, ip);
			return ip;
		}, item);
	}

	@Override
	protected AbstractListResponse<TemplateInPackage> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new PackageResourceImpl().listTemplates(packageName, filter, sort, paging, new PermsParameterBean());
	}
}
