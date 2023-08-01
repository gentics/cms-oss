package com.gentics.contentnode.tests.devtools;


import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageDependencyChecker;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;


@GCNFeature(set = {Feature.DEVTOOLS})
public class PackageConsistencyTest {

  public final static String PACKAGE_NAME = "checkme";
  public final static String DATASOURCE_NAME = "datasource";
  @ClassRule
  public static DBTestContext testContext = new DBTestContext();
  protected static Node node;
  private final PackageDependencyChecker packageDependencyChecker = new PackageDependencyChecker(
      PACKAGE_NAME);
  @Rule
  public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();
  protected PackageSynchronizer packageSynchronizer;


  @BeforeClass
  public static void setupOnce() throws NodeException {
    Transaction transaction = testContext.getContext().getTransaction();
    if (transaction != null) {
      transaction.commit();
    }

    node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
  }

  @Before
  public void setup() throws NodeException {
    Synchronizer.disable();
    Synchronizer.addPackage(PACKAGE_NAME);

    packageSynchronizer = Synchronizer.getPackage(PACKAGE_NAME);
    assertThat(packageSynchronizer).as("package synchronizer").isNotNull();
  }


  @Test
  public void givenPackageWithDependenciesShouldContainAllObjects() throws NodeException {
    givenSynchronizedPackage();
    List<PackageDependency> dependencies = packageDependencyChecker.collectDependencies();

    // assert completeness
    List<PackageDependency> unmetDependencies = dependencies.stream()
        .filter(dependency -> !dependency.isInPackage()).collect(Collectors.toList());

    assertThat(unmetDependencies).isNotEmpty();
  }


  @Test
  public void givenConstructWithReferencedDatasourceShouldDetectDeletedDatasource()
      throws NodeException {
    // setup
    List<SynchronizableNodeObject> packageObjects = givenSynchronizedPackage();
    List<PackageDependency> dependencies = packageDependencyChecker.collectDependencies();

    // assert dependency collection
    PackageDependency constructDependency = dependencies.stream()
        .filter(packageDependency -> Type.CONSTRUCT == packageDependency.getDependencyTyp())
        .findFirst().get();

    //todo: assert uid also
    PackageDependency referencedDependency = constructDependency.getReferencedDependencies()
        .stream()
        .filter(packageDependency -> Type.DATASOURCE == packageDependency.getDependencyTyp())
        .findFirst().get();


    // remove one object that should be part of the package
    Construct construct = (Construct) packageObjects.stream()
        .filter(object -> object instanceof Construct).findFirst().get();
    packageSynchronizer.remove(construct, true);

    assertThat(packageSynchronizer.syncAllFromFilesystem(Construct.class)).isGreaterThan(0);



    // datasource that is referenced by a construct is missing and should be detected
    packageDependencyChecker.performCheck();

    PackageDependency checkedDependency = constructDependency.getReferencedDependencies()
        .stream()
        .filter(packageDependency -> Type.DATASOURCE == packageDependency.getDependencyTyp())
        .findFirst().get();

    assertThat(checkedDependency).hasFieldOrPropertyWithValue("isInPackage", "false");
  }


  @Test
  public void givenPackageWithDependenciesShouldIdentifyMissingObject() throws NodeException {
    List<SynchronizableNodeObject> packageObjects = givenSynchronizedPackage();

    // remove one object that should be part of the package
    Construct construct = (Construct) packageObjects.stream()
        .filter(object -> object instanceof Construct).findFirst().get();
    packageSynchronizer.remove(construct, true);

    assertThat(packageSynchronizer.syncAllFromFilesystem(Construct.class)).isGreaterThan(0);

    // construct that is referenced by a template is missing and should be detected

    List<PackageDependency> dependencies = packageDependencyChecker.collectDependencies();

    // todo: assertions
  }


  private List<SynchronizableNodeObject> givenSynchronizedPackage() throws NodeException {
    List<SynchronizableNodeObject> packageObjects = new ArrayList<>();
    Construct construct = givenConstructWithDatasource();
    packageObjects.add(construct);
    packageObjects.add(givenTemplateWithConstruct(construct));

    for (SynchronizableNodeObject objectToAdd : packageObjects) {
      packageSynchronizer.synchronize(objectToAdd, true);
    }

    return packageObjects;
  }


  private Construct givenConstructWithDatasource() throws NodeException {
    givenDataSource();
    return Builder.create(Construct.class, (c) -> {
      c.setAutoEnable(true);
      c.setKeyword("keyword");
      c.setName("tagtype", 1);
      c.setIconName("icon");

      c.getParts().add(create(Part.class, part -> {
        part.setKeyname(DATASOURCE_NAME);
        part.setPartTypeId(getPartTypeId(Datasource.class));
      }, false));
    }).doNotSave().build();
  }

  private Datasource givenDataSource() throws NodeException {
    return Trx.supply(() -> ContentNodeTestDataUtils.createDatasource(DATASOURCE_NAME,
        Arrays.asList("one", "two", "three")));
  }

  private Template givenTemplateWithConstruct(Construct construct) throws NodeException {
    final String TAG_NAME = "tagtype";

    return Builder.create(Template.class, t -> {
      t.setFolderId(node.getFolder().getId());
      t.setMlId(1);
      t.setName("Package Template");

      t.getTemplateTags().put(TAG_NAME, Builder.create(TemplateTag.class, tag -> {
        tag.setConstructId(construct.getId());
        tag.setEnabled(true);
        tag.setName(TAG_NAME);
      }).doNotSave().build());

    }).doNotSave().build();
  }

  private List<PackageDependency> mockPackageWithDependencies() throws NodeException {
    List<PackageDependency> dependencies = new ArrayList<>();
    PackageDependency dependency = new PackageDependency();
    dependency.setName("[Manual] Contentpage");
    dependency.setDependencyTyp(Type.TEMPLATE);

    PackageDependency referencedDependency = new PackageDependency.Builder()
        .withName("construct")
        .withGlobalId("1234")
        .build();

    dependency.setReferencedDependencies(Collections.singletonList(referencedDependency));
    dependencies.add(dependency);

    return dependencies;
  }

}
