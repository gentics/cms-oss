package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.jsonToFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.model.PackageModel;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract base class for test of the packages REST API for items
 *
 * @param <T> type of the items to handle
 * @param <U> type of the REST model
 */
public abstract class AbstractPackageResourceItemsTest<T extends SynchronizableNodeObject, U> {
	/**
	 * Name of the package
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	/**
	 * Type class
	 */
	protected Class<T> typeClass;

	/**
	 * Create an instance for the type class
	 * @param typeClass type class
	 */
	protected AbstractPackageResourceItemsTest(Class<T> typeClass) {
		this.typeClass = typeClass;
	}

	@Before
	public void setup() throws Exception {
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add(PACKAGE_NAME);
			trx.success();
		}
	}

	/**
	 * Test adding an object
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAdd() throws Exception {
		T object = Trx.supply(objectSupplier());
		try (Trx trx = new Trx()) {
			Response response = add(infoFunction().apply(object));
			assertThat(response.getStatus()).as("response status").isEqualTo(Status.CREATED.getStatusCode());
			assertThat(Synchronizer.getPackage(PACKAGE_NAME).getObjects(typeClass)).as("Items in package").containsExactly(new PackageObject<>(object));
			trx.success();
		}
	}

	/**
	 * Test removing an object
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testRemove() throws Exception {
		T object = Trx.supply(objectSupplier());
		try (Trx trx = new Trx()) {
			Synchronizer.getPackage(PACKAGE_NAME).synchronize(object, true);
			assertThat(Synchronizer.getPackage(PACKAGE_NAME).getObjects(typeClass)).as("Items in package").containsExactly(new PackageObject<>(object));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			Response response = remove(infoFunction().apply(object));
			assertThat(response.getStatus()).as("response status").isEqualTo(Status.NO_CONTENT.getStatusCode());
			assertThat(Synchronizer.getPackage(PACKAGE_NAME).getObjects(typeClass)).as("Items in package").isEmpty();
			trx.success();
		}
	}

	/**
	 * Test getting an object
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGet() throws Exception {
		T object = Trx.supply(objectSupplier());
		try (Trx trx = new Trx()) {
			Synchronizer.getPackage(PACKAGE_NAME).synchronize(object, true);
			assertThat(Synchronizer.getPackage(PACKAGE_NAME).getObjects(typeClass)).as("Items in package").containsExactly(new PackageObject<>(object));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			U response = get(infoFunction().apply(object));
			assertThat(response).isNotNull();
		}
	}

	/**
	 * Test listing objects
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testList() throws Exception {
		T object = Trx.supply(objectSupplier());
		try (Trx trx = new Trx()) {
			Synchronizer.getPackage(PACKAGE_NAME).synchronize(object, true);
			assertThat(Synchronizer.getPackage(PACKAGE_NAME).getObjects(typeClass)).as("Items in package").containsExactly(new PackageObject<>(object));
			trx.success();
		}

		try (Trx trx = new Trx()) {
			List<U> response = new ArrayList<>(list());
			assertThat(response).usingElementComparatorOnFields("globalId", "packageName").containsOnly(transform(object));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testListSub() throws Exception {
		String subPackageName = "subPackage";

		// prepare package for subpackages
		Synchronizer.stop();
		PackageModel model = new PackageModel();
		model.setSubpackages("node_modules");
		File packageDir = new File(syncContext.getPackagesRoot(), PACKAGE_NAME);
		jsonToFile(model, new File(packageDir, MainPackageSynchronizer.GENTICS_PACKAGE_JSON));
		File subPackageRoot = new File(new File(packageDir, "node_modules"), subPackageName);
		subPackageRoot.mkdirs();
		Synchronizer.start();

		T object = Trx.supply(objectSupplier());
		try (Trx trx = new Trx()) {
			Synchronizer.getPackage(PACKAGE_NAME).getSubPackageContainer().getPackage(subPackageName).synchronize(object, true);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			List<U> response = new ArrayList<>(list());
			assertThat(response).usingElementComparatorOnFields("globalId", "packageName").containsOnly(transform(object, subPackageName));
		}
	}

	/**
	 * Get the object supplier
	 * @return object supplier
	 */
	protected abstract Supplier<T> objectSupplier();

	/**
	 * Get the info function
	 * @return info function
	 */
	protected abstract Function<T, String> infoFunction();

	/**
	 * Add the object with given info to the test package
	 * @param info object info
	 * @return response
	 * @throws Exception
	 */
	protected abstract Response add(String info) throws Exception;

	/**
	 * Get the object with given info from the test package
	 * @param info object info
	 * @return response
	 * @throws Exception
	 */
	protected abstract U get(String info) throws Exception;

	/**
	 * Get the list of objects from the test package
	 * @return list of objects
	 * @throws Exception
	 */
	protected abstract List<? extends U> list() throws Exception;

	/**
	 * Remove the object with given info from the test package
	 * @param info object info
	 * @return response
	 * @throws Exception
	 */
	protected abstract Response remove(String info) throws Exception;

	/**
	 * Transform the object into its REST model
	 * @param object object to transform
	 * @return REST model
	 * @throws Exception
	 */
	protected U transform(T object) throws Exception {
		return transform(object, null);
	}

	/**
	 * Transform the object into its REST model
	 * @param object object to transform
	 * @param packageName name of the package (may be null)
	 * @return REST model
	 * @throws Exception
	 */
	protected abstract U transform(T object, String packageName) throws Exception;
}
