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
import com.gentics.contentnode.devtools.ContentRepositorySynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedContentRepositoryInPackageListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for handling contentrepositories
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceCRsTest extends AbstractPackageResourceItemsTest<ContentRepository, ContentRepositoryModel> {
	/**
	 * Map of possible info functions
	 */
	protected static Map<String, Function<ContentRepository, String>> INFO_FUNCTIONS = new HashMap<>();

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

	@Parameter(0)
	public String info;

	/**
	 * Create test instance
	 */
	public PackageResourceCRsTest() {
		super(ContentRepository.class);
	}

	@Override
	protected Response add(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.addContentRepository(PACKAGE_NAME, info);
		}
	}

	@Override
	protected ContentRepositoryModel get(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			ContentRepositoryResponse response = resource.getContentRepository(PACKAGE_NAME, info);
			return response.getContentRepository();
		}
	}

	@Override
	protected List<? extends ContentRepositoryModel> list() throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			PagedContentRepositoryInPackageListResponse response = resource.listContentRepositories(PACKAGE_NAME, new FilterParameterBean(),
					new SortParameterBean(), new PagingParameterBean(), new EmbedParameterBean(), null);
			return response.getItems();
		}
	}

	@Override
	protected Response remove(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.removeContentRepository(PACKAGE_NAME, info);
		}
	}

	@Override
	protected Function<ContentRepository, String> infoFunction() {
		return INFO_FUNCTIONS.get(info);
	}

	@Override
	protected Supplier<ContentRepository> objectSupplier() {
		return () -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Collection<Integer> constructIds = DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS);
			assertThat(constructIds).as("List of construct IDs").isNotEmpty();
			return t.getObject(ContentRepository.class, constructIds.iterator().next());
		};
	}

	@Override
	protected ContentRepositoryModel transform(ContentRepository object, String packageName) throws Exception {
		return ContentRepositorySynchronizer.TRANSFORM2REST.apply(new PackageObject<>(object, packageName));
	}
}
