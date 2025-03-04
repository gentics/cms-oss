package com.gentics.contentnode.tests.devtools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.DatasourceSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedDatasourceInPackageListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling datasources
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceDatasourcesTest extends AbstractPackageResourceItemsTest<Datasource, com.gentics.contentnode.rest.model.Datasource> {
	/**
	 * Map of possible info functions
	 */
	protected static Map<String, Function<Datasource, String>> INFO_FUNCTIONS = new HashMap<>();

	static {
		INFO_FUNCTIONS.put("localId", c -> String.valueOf(c.getId()));
		INFO_FUNCTIONS.put("globalId", c -> c.getGlobalId().toString());
		INFO_FUNCTIONS.put("name", c -> c.getName());
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

	@BeforeClass
	public static void setupOnce() throws Exception {
		Trx.operate(() -> ContentNodeTestDataUtils.createDatasource("Test Datasource", Arrays.asList("one", "two", "three")));
	}

	@Parameter(0)
	public String info;

	/**
	 * Create a test instance
	 */
	public PackageResourceDatasourcesTest() {
		super(Datasource.class);
	}

	@Override
	protected Supplier<Datasource> objectSupplier() {
		return () -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Collection<Integer> datasourceIds = DBUtils.select("SELECT id FROM datasource WHERE name IS NOT NULL AND name != ''", DBUtils.IDS);
			assertThat(datasourceIds).as("List of datasource IDs").isNotEmpty();
			return t.getObject(Datasource.class, datasourceIds.iterator().next());
		};
	}

	@Override
	protected Function<Datasource, String> infoFunction() {
		return INFO_FUNCTIONS.get(info);
	}

	@Override
	protected Response add(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.addDatasource(PACKAGE_NAME, info);
		}
	}

	@Override
	protected com.gentics.contentnode.rest.model.Datasource get(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			DatasourceLoadResponse response = resource.getDatasource(PACKAGE_NAME, info);
			return response.getDatasource();
		}
	}

	@Override
	protected List<? extends com.gentics.contentnode.rest.model.Datasource> list() throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			PagedDatasourceInPackageListResponse response = resource.listDatasources(PACKAGE_NAME, new FilterParameterBean(), new SortParameterBean(),
					new PagingParameterBean(), null);
			return response.getItems();
		}
	}

	@Override
	protected Response remove(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.removeDatasource(PACKAGE_NAME, info);
		}
	}

	@Override
	protected com.gentics.contentnode.rest.model.Datasource transform(Datasource object, String packageName) throws Exception {
		return DatasourceSynchronizer.TRANSFORM2REST.apply(new PackageObject<>(object, packageName));
	}
}
