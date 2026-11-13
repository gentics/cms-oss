package com.gentics.contentnode.tests.devtools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.Response;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PortalImplementationSynchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedPortalImplementationInPackageListResponse;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Test cases for handling Portal Implementations in packages via the REST API
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourcePortalImplementationsTest extends AbstractPackageResourceItemsTest<PortalImplementation, PortalImplementationModel> {
	/**
	 * Map of possible info functions
	 */
	protected static Map<String, Function<PortalImplementation, String>> INFO_FUNCTIONS = new HashMap<>();

	/**
	 * Counter for unique keywords and names
	 */
	protected static AtomicInteger counter = new AtomicInteger();

	static {
		INFO_FUNCTIONS.put("localId", impl -> String.valueOf(impl.getId()));
		INFO_FUNCTIONS.put("globalId", impl -> impl.getGlobalId().toString());
		INFO_FUNCTIONS.put("keyword", impl -> impl.getKeyword());
	}

	/**
	 * Get the test parameters
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
	public PackageResourcePortalImplementationsTest() {
		super(PortalImplementation.class);
	}

	@Override
	protected Response add(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.addPortalImplementation(PACKAGE_NAME, info);
		}
	}

	@Override
	protected PortalImplementationModel get(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			PortalImplementationResponse response = resource.getPortalImplementation(PACKAGE_NAME, info);
			return response.getPortalImplementation();
		}
	}

	@Override
	protected List<? extends PortalImplementationModel> list() throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			PagedPortalImplementationInPackageListResponse response = resource.listPortalImplementations(PACKAGE_NAME, new FilterParameterBean(),
					new SortParameterBean(), new PagingParameterBean(), new EmbedParameterBean(), null);
			return response.getItems();
		}
	}

	@Override
	protected Response remove(String info) throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			trx.success();
			return resource.removePortalImplementation(PACKAGE_NAME, info);
		}
	}

	@Override
	protected Function<PortalImplementation, String> infoFunction() {
		return INFO_FUNCTIONS.get(info);
	}

	@Override
	protected Supplier<PortalImplementation> objectSupplier() {
		return () -> {
			int index = counter.incrementAndGet();
			return Builder.create(PortalImplementation.class, impl -> {
				impl.setKeyword("portal" + index);
				impl.setName("Portal " + index);
				impl.setType(PortalImplementationModel.Type.PHP);
			}).build();
		};
	}

	@Override
	protected PortalImplementationModel transform(PortalImplementation object, String packageName) throws Exception {
		return PortalImplementationSynchronizer.TRANSFORM2REST.apply(new PackageObject<>(object, packageName));
	}
}
