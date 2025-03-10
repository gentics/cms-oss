package com.gentics.contentnode.tests.devtools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.TemplateSynchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedTemplateInPackageListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling Templates
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceTemplatesTest extends AbstractPackageResourceItemsTest<Template, com.gentics.contentnode.rest.model.Template> {
	/**
	 * Map of possible info functions
	 */
	protected static Map<String, Function<Template, String>> INFO_FUNCTIONS = new HashMap<>();

	static {
		INFO_FUNCTIONS.put("localId", t -> String.valueOf(t.getId()));
		INFO_FUNCTIONS.put("globalId", t -> t.getGlobalId().toString());
		INFO_FUNCTIONS.put("name", t -> t.getName());
	}

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: by {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String info : INFO_FUNCTIONS.keySet()) {
			data.add(new Object[] { info });
		}
		return data;
	}

	@Parameter(0)
	public String info;

	/**
	 * Create test instance
	 */
	public PackageResourceTemplatesTest() {
		super(Template.class);
	}

	@Override
	protected Supplier<Template> objectSupplier() {
		return () -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Collection<Integer> templateIds = DBUtils.select("SELECT id FROM template WHERE is_master = 1", DBUtils.IDS);
			assertThat(templateIds).as("List of template IDs").isNotEmpty();
			return t.getObject(Template.class, templateIds.iterator().next());
		};
	}

	@Override
	protected Function<Template, String> infoFunction() {
		return INFO_FUNCTIONS.get(info);
	}

	@Override
	protected Response add(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.addTemplate(PACKAGE_NAME, info);
		}
	}

	@Override
	protected com.gentics.contentnode.rest.model.Template get(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			TemplateLoadResponse response = resource.getTemplate(PACKAGE_NAME, info);
			return response.getTemplate();
		}
	}

	@Override
	protected List<? extends com.gentics.contentnode.rest.model.Template> list() throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			PagedTemplateInPackageListResponse response = resource.listTemplates(PACKAGE_NAME, new FilterParameterBean(), new SortParameterBean(),
					new PagingParameterBean(), null);
			return response.getItems();
		}
	}

	@Override
	protected Response remove(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.removeTemplate(PACKAGE_NAME, info);
		}
	}

	@Override
	protected com.gentics.contentnode.rest.model.Template transform(Template object, String packageName) throws Exception {
		return TemplateSynchronizer.TRANSFORM2REST.apply(new PackageObject<>(object, packageName));
	}
}
