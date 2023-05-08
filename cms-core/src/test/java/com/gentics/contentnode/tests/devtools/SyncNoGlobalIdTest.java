package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for synchronizing objects from the filesystem, that do not have globalId's set
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class SyncNoGlobalIdTest {
	public final static String CONSTRUCT_UUID = "1111.d625ab5c-bd23-11ea-a785-482ae36fb1c5";

	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected PackageSynchronizer pack;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		operate(() -> create(Construct.class, construct -> {
			construct.setAutoEnable(true);
			construct.setIconName("icon");
			construct.setKeyword("construct");
			construct.setName("Construct", 1);
			construct.setGlobalId(new GlobalId(CONSTRUCT_UUID));

			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(1);
				part.setHidden(false);
				part.setKeyname("text");
				part.setName("Text", 1);
				part.setPartTypeId(getPartTypeId(ShortTextPartType.class));
				part.setDefaultValue(create(Value.class, v -> {
				}, false));
			}, false));
		}));
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.disable();
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	@Test
	public void testConstruct() throws NodeException, IOException, URISyntaxException {
		// prepare package
		File packageRootDir = pack.getPackagePath().toFile();
		FileUtils.copyDirectory(new File(getClass().getResource("packages/no_globalid/construct").toURI()), packageRootDir);

		// list objects in package
		supply(() -> new PackageResourceImpl().listConstructs(pack.getName(), null, null, null, null, null));

		// get IDs of constructs before synchronizing
		Set<Integer> oldIds = supply(() -> DBUtils.select("SELECT id FROM construct", DBUtils.IDS));

		// synchronize from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(Construct.class))).isEqualTo(3);

		// get IDs of constructs after synchronizing
		Set<Integer> newIds = supply(() -> DBUtils.select("SELECT id FROM construct", DBUtils.IDS));

		Set<Integer> diff = new HashSet<>(newIds);
		diff.removeAll(oldIds);

		assertThat(diff).as("IDs of new constructs").hasSize(3);
	}

	@Test
	public void testDatasource() throws NodeException, IOException, URISyntaxException {
		// prepare package
		File packageRootDir = pack.getPackagePath().toFile();
		FileUtils.copyDirectory(new File(getClass().getResource("packages/no_globalid/datasource").toURI()), packageRootDir);

		// list objects in package
		supply(() -> new PackageResourceImpl().listDatasources(pack.getName(), null, null, null, null));

		// get IDs of datasources before synchronizing
		Set<Integer> oldIds = supply(() -> DBUtils.select("SELECT id FROM datasource", DBUtils.IDS));

		// synchronize from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(Datasource.class))).isEqualTo(3);

		// get IDs of datasources after synchronizing
		Set<Integer> newIds = supply(() -> DBUtils.select("SELECT id FROM datasource", DBUtils.IDS));

		Set<Integer> diff = new HashSet<>(newIds);
		diff.removeAll(oldIds);

		assertThat(diff).as("IDs of new datasources").hasSize(3);
	}

	@Test
	public void testObjectTagDefinition() throws NodeException, IOException, URISyntaxException {
		// prepare package
		File packageRootDir = pack.getPackagePath().toFile();
		FileUtils.copyDirectory(new File(getClass().getResource("packages/no_globalid/objprop").toURI()), packageRootDir);

		// list objects in package
		supply(() -> new PackageResourceImpl().listObjectProperties(pack.getName(), null, null, null, new EmbedParameterBean(), null));

		// get IDs of object property definitions before synchronizing
		Set<Integer> oldIds = supply(() -> DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS));

		// synchronize from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(ObjectTagDefinition.class))).isEqualTo(3);

		// get IDs of object property definitions after synchronizing
		Set<Integer> newIds = supply(() -> DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS));

		Set<Integer> diff = new HashSet<>(newIds);
		diff.removeAll(oldIds);

		assertThat(diff).as("IDs of new object property definitions").hasSize(3);
	}

	@Test
	public void testTemplate() throws NodeException, IOException, URISyntaxException {
		// prepare package
		File packageRootDir = pack.getPackagePath().toFile();
		FileUtils.copyDirectory(new File(getClass().getResource("packages/no_globalid/template").toURI()), packageRootDir);

		// list objects in package
		supply(() -> new PackageResourceImpl().listTemplates(pack.getName(), null, null, null, null));

		// get IDs of templates before synchronizing
		Set<Integer> oldIds = supply(() -> DBUtils.select("SELECT id FROM template", DBUtils.IDS));

		// synchronize from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(Template.class))).isEqualTo(3);

		// get IDs of templates after synchronizing
		Set<Integer> newIds = supply(() -> DBUtils.select("SELECT id FROM template", DBUtils.IDS));

		Set<Integer> diff = new HashSet<>(newIds);
		diff.removeAll(oldIds);

		assertThat(diff).as("IDs of new templates").hasSize(3);
	}

	@Test
	public void testCrFragment() throws NodeException, IOException, URISyntaxException {
		// prepare package
		File packageRootDir = pack.getPackagePath().toFile();
		FileUtils.copyDirectory(new File(getClass().getResource("packages/no_globalid/crfragment").toURI()), packageRootDir);

		// list objects in package
		supply(() -> new PackageResourceImpl().listCrFragments(pack.getName(), null, null, null, null, null));

		// get IDs of cr fragments before synchronizing
		Set<Integer> oldIds = supply(() -> DBUtils.select("SELECT id FROM cr_fragment", DBUtils.IDS));

		// synchronize from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(CrFragment.class))).isEqualTo(3);

		// get IDs of cr fragments after synchronizing
		Set<Integer> newIds = supply(() -> DBUtils.select("SELECT id FROM cr_fragment", DBUtils.IDS));

		Set<Integer> diff = new HashSet<>(newIds);
		diff.removeAll(oldIds);

		assertThat(diff).as("IDs of new cr fragments").hasSize(3);
	}

	@Test
	public void testContentRepository() throws NodeException, IOException, URISyntaxException {
		// prepare package
		File packageRootDir = pack.getPackagePath().toFile();
		FileUtils.copyDirectory(new File(getClass().getResource("packages/no_globalid/cr").toURI()), packageRootDir);

		// list objects in package
		supply(() -> new PackageResourceImpl().listContentRepositories(pack.getName(), null, null, null, null, null));

		// get IDs of crs before synchronizing
		Set<Integer> oldIds = supply(() -> DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS));

		// synchronize from FS
		assertThat(supply(() -> pack.syncAllFromFilesystem(ContentRepository.class))).isEqualTo(3);

		// get IDs of crs after synchronizing
		Set<Integer> newIds = supply(() -> DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS));

		Set<Integer> diff = new HashSet<>(newIds);
		diff.removeAll(oldIds);

		assertThat(diff).as("IDs of new crs").hasSize(3);
	}
}
