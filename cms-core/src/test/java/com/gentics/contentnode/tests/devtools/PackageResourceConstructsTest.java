package com.gentics.contentnode.tests.devtools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.ConstructSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedConstructInPackageListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling constructs
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceConstructsTest extends AbstractPackageResourceItemsTest<Construct, com.gentics.contentnode.rest.model.Construct> {
	/**
	 * Map of possible info functions
	 */
	protected static Map<String, Function<Construct, String>> INFO_FUNCTIONS = new HashMap<>();

	static {
		INFO_FUNCTIONS.put("localId", c -> String.valueOf(c.getId()));
		INFO_FUNCTIONS.put("globalId", c -> c.getGlobalId().toString());
		INFO_FUNCTIONS.put("keyword", c -> c.getKeyword());
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
	public PackageResourceConstructsTest() {
		super(Construct.class);
	}

	@Override
	protected Response add(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.addConstruct(PACKAGE_NAME, info);
		}
	}

	@Override
	protected com.gentics.contentnode.rest.model.Construct get(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			ConstructLoadResponse response = resource.getConstruct(PACKAGE_NAME, info);
			return response.getConstruct();
		}
	}

	@Override
	protected List<? extends com.gentics.contentnode.rest.model.Construct> list() throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			PagedConstructInPackageListResponse response = resource.listConstructs(PACKAGE_NAME, new FilterParameterBean(), new SortParameterBean(),
					new PagingParameterBean(), null, null);
			return response.getItems();
		}
	}

	@Override
	protected Response remove(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.removeConstruct(PACKAGE_NAME, info);
		}
	}

	@Override
	protected Function<Construct, String> infoFunction() {
		return INFO_FUNCTIONS.get(info);
	}

	@Override
	protected Supplier<Construct> objectSupplier() {
		return () -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Collection<Integer> constructIds = DBUtils.select("SELECT id FROM construct", DBUtils.IDS);
			assertThat(constructIds).as("List of construct IDs").isNotEmpty();
			return t.getObject(Construct.class, constructIds.iterator().next());
		};
	}

	@Override
	protected com.gentics.contentnode.rest.model.Construct transform(Construct object, String packageName) throws Exception {
		return ConstructSynchronizer.TRANSFORM2REST.apply(new PackageObject<>(object, packageName));
	}
}
