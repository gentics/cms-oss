package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.exceptions.DuplicateEntityException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.devtools.PackageListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependencyList;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.infrastructure.TestEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test cases for the package resource
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class PackageResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	/**
	 * Test adding a package
	 * @throws Exception
	 */
	@Test
	public void testAddPackage() throws Exception {
		String packageName = "testpackage";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add(packageName);

			assertThat(new File(syncContext.getPackagesRoot(), packageName)).exists().isDirectory();
			trx.success();
		}
	}

	/**
	 * Test adding a package that already exists
	 * @throws Exception
	 */
	@Test(expected = DuplicateEntityException.class)
	public void testAddDuplicatePackage() throws Exception {
		String packageName = "duplicate";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add(packageName);
			resource.add(packageName);
			trx.success();
		}
	}

	/**
	 * Test removing a package
	 * @throws Exception
	 */
	@Test
	public void testRemovePackage() throws Exception {
		String packageName = "testpackage";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add(packageName);
			trx.success();
		}

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.delete(packageName);
			assertThat(new File(syncContext.getPackagesRoot(), packageName)).doesNotExist();
			trx.success();
		}
	}

	/**
	 * Test removing a package that does not exist
	 * @throws Exception
	 */
	@Test(expected = EntityNotFoundException.class)
	public void testRemoveInexistentPackage() throws Exception {
		String packageName = "doesnotexist";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.delete(packageName);
			trx.success();
		}
	}

	/**
	 * Test reading a package
	 * @throws Exception
	 */
	@Test
	public void getPackage() throws Exception {
		String packageName = "testpackage";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add(packageName);
			assertThat(resource.get(packageName)).as("Package").hasName(packageName).hasDescription(null).hasConstructs(0).hasDatasources(0)
					.hasObjectProperties(0).hasTemplates(0);
			trx.success();
		}
	}

	/**
	 * Test reading an inexistent package
	 * @throws Exception
	 */
	@Test(expected = EntityNotFoundException.class)
	public void getInexistentPackage() throws Exception {
		String packageName = "doesnotexist";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.get(packageName);
			trx.success();
		}
	}

	/**
	 * Test reading a package list
	 * @throws Exception
	 */
	@Test
	public void testPackageList() throws Exception {
		Set<String> packageNames = addRandomPackages(50);

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			PackageListResponse packages = resource.list(new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean());
			ContentNodeRESTUtils.assertResponseOK(packages);

			assertThat(packages.getItems().stream().map(p -> p.getName()).collect(Collectors.toList())).as("Package list").containsOnlyElementsOf(packageNames);

			trx.success();
		}
	}

	/**
	 * Test reading a sorted package list
	 * @throws Exception
	 */
	@Test
	public void testPackageListSorting() throws Exception {
		List<String> sortedPackageNames = new ArrayList<>(addRandomPackages(50));

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();

			// sort ascending
			SortParameterBean sort = new SortParameterBean();
			sort.sort = "+name";
			PackageListResponse packages = resource.list(new FilterParameterBean(), sort, new PagingParameterBean());
			ContentNodeRESTUtils.assertResponseOK(packages);
			Collections.sort(sortedPackageNames);
			assertThat(packages.getItems().stream().map(p -> p.getName()).collect(Collectors.toList())).as("Package list").containsExactlyElementsOf(
					sortedPackageNames);

			// sort descending
			sort.sort = "-name";
			packages = resource.list(new FilterParameterBean(), sort, new PagingParameterBean());
			ContentNodeRESTUtils.assertResponseOK(packages);
			Collections.reverse(sortedPackageNames);
			assertThat(packages.getItems().stream().map(p -> p.getName()).collect(Collectors.toList())).as("Package list").containsExactlyElementsOf(
					sortedPackageNames);
		}
	}

	/**
	 * Test reading a paged package list
	 * @throws Exception
	 */
	@Test
	public void testPackageListPaging() throws Exception {
		List<String> sortedPackageNames = new ArrayList<>(addRandomPackages(50));
		Collections.sort(sortedPackageNames);

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			SortParameterBean sorting = new SortParameterBean();
			sorting.sort = "+name";
			PagingParameterBean paging = new PagingParameterBean();
			paging.pageSize = 10;

			for (int page = 1; page <= 5; page++) {
				paging.page = page;

				PackageListResponse packages = resource.list(new FilterParameterBean(), sorting, paging);
				ContentNodeRESTUtils.assertResponseOK(packages);

				assertThat(packages.getItems().stream().map(p -> p.getName()).collect(Collectors.toList())).as("Page " + page).containsExactlyElementsOf(
						sortedPackageNames.subList(paging.pageSize * (paging.page - 1), paging.pageSize * paging.page));
			}
		}
	}

	/**
	 * Test reading a filtered package list
	 * @throws Exception
	 */
	@Test
	public void testPackageListFiltering() throws Exception {
		Set<String> packageNames = addRandomPackages(50);

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			FilterParameterBean filtering = new FilterParameterBean();
			for (String name : packageNames) {
				filtering.query = name;

				PackageListResponse packages = resource.list(filtering, new SortParameterBean(), new PagingParameterBean());
				ContentNodeRESTUtils.assertResponseOK(packages);

				assertThat(packages.getItems().stream().map(p -> p.getName()).collect(Collectors.toList())).as("Filtered list").containsOnly(name);
			}
		}
	}


	@Test
	public void givenPackageShouldListDependencies() throws Exception {
		final String PACKAGE_NAME = "manual";

		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.add(PACKAGE_NAME);

			PackageResource packageResource = new PackageResourceImpl();
			PackageDependencyList packageConsistencyResponse = (PackageDependencyList) packageResource.performPackageConsistencyCheck(PACKAGE_NAME, null, null);

			assertThat(packageConsistencyResponse.getResponseInfo().getResponseCode()).isEqualTo(ResponseCode.OK);
			assertThat(packageConsistencyResponse.checkCompleteness()).isTrue();
		}
	}


	/**
	 * Add the given number of packages with random names
	 * @param numPackages number of packages to add
	 * @return set of package names
	 * @throws Exception
	 */
	protected Set<String> addRandomPackages(int numPackages) throws Exception {
		Set<String> packageNames = new HashSet<>();
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			for (int i = 0; i < numPackages; i++) {
				String name = TestEnvironment.getRandomHash(10);
				resource.add(name);
				packageNames.add(name);
			}
			trx.success();
		}
		return packageNames;
	}
}
