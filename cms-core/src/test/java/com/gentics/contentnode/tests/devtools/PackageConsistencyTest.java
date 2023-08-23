package com.gentics.contentnode.tests.devtools;


import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
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
  @Rule
  public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

  protected PackageSynchronizer synchronizer;
  private PackageDependencyChecker dependencyChecker;

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
    dependencyChecker = new PackageDependencyChecker(PACKAGE_NAME);
    synchronizer = Synchronizer.getPackage(PACKAGE_NAME);

    assertThat(synchronizer).as("package synchronizer").isNotNull();
  }

  @Test
  public void givenPackageWithDependenciesShouldBeComplete() throws NodeException {
    givenSynchronizedPackage();
    List<PackageDependency> dependencies = dependencyChecker.collectDependencies();

    assertThat(dependencyChecker.isPackageComplete(dependencies)).isTrue();
  }

  @Test
  public void givenPackageWithDependenciesShouldContainAllExpectedObjects() throws NodeException {
    givenSynchronizedPackage();
    List<PackageDependency> dependencies = dependencyChecker.collectDependencies();

    //todo: assert uuid also
    assertThat(dependencies.get(0)).hasFieldOrPropertyWithValue("dependencyType", Type.CONSTRUCT);
    assertThat(dependencies.get(1)).hasFieldOrPropertyWithValue("dependencyType", Type.TEMPLATE);
    assertThat(dependencies.get(1).getReferencedDependencies().get(0))
        .hasFieldOrPropertyWithValue("dependencyType", Type.TEMPLATE_TAG);
  }


  @Test
  public void givenConstructWithReferencedDatasourceShouldDetectDeletedDatasource()
      throws NodeException {
    List<SynchronizableNodeObject> packageObjects = givenSynchronizedPackage();
    List<PackageDependency> dependencies = dependencyChecker.collectDependencies();

    operate(() -> {
      PackageObject<Datasource> datasourceInPackage = synchronizer.getObjects(Datasource.class).get(0);
      synchronizer.remove(datasourceInPackage.getObject(), true);
    });

    // todo: needed?
    operate(() ->
        assertThat(synchronizer.syncAllFromFilesystem(Construct.class)).isPositive()
    );

    // datasource that is referenced by a construct is missing and should be detected
    List<PackageDependency> derangedDependencies = dependencyChecker.collectDependencies();
    assertThat(dependencyChecker.isPackageComplete(derangedDependencies)).isFalse();
  }

  @Test
  public void givenPackageWithDependenciesShouldIdentifyMissingObject() throws NodeException {
    List<SynchronizableNodeObject> packageObjects = givenSynchronizedPackage();

    // remove one object that should be part of the package
    Construct construct = (Construct) packageObjects.stream()
        .filter(object -> object instanceof Construct).findFirst().get();
    synchronizer.remove(construct, true);

/*    operate(()->
      assertThat(packageSynchronizer.syncAllFromFilesystem(Construct.class)).isPositive()
    );*/
    List<PackageDependency> dependencies = dependencyChecker.collectDependencies();

    // construct that is referenced by a template is missing and should be detected
    assertThat(dependencyChecker.isPackageComplete(dependencies)).isFalse();
    assertThat(PackageDependencyChecker.filterMissingDependencies(dependencies)).isNotEmpty();
  }


  private List<SynchronizableNodeObject> givenSynchronizedPackage() throws NodeException {
    List<SynchronizableNodeObject> packageObjects = new ArrayList<>();
    Construct construct = givenConstructWithDatasource();
    packageObjects.add(construct);
    packageObjects.add(givenTemplateWithConstruct(construct));
    packageObjects.add(givenDataSource());
    packageObjects.add(givenObjectPropertyWithConstruct(construct));

      operate(() -> {
      for (SynchronizableNodeObject objectToAdd : packageObjects) {
        synchronizer.synchronize(objectToAdd, true);
      }
    });
    return packageObjects;
  }


  private Construct givenConstructWithDatasource() throws NodeException {
    Datasource datasource = givenDataSource();

    return Builder.create(Construct.class, (c) -> {
      c.setAutoEnable(true);
      c.setKeyword("construct_with_ds");
      c.setName("construct_with_ds", 1);
      c.setIconName("icon");

      c.getParts().add(create(Part.class, p -> {
        p.setKeyname("select_part_ds");
        p.setPartTypeId(getPartTypeId(SingleSelectPartType.class));
        p.setPartoptionId(datasource.getId()); // todo: check me
        p.setName("DatasourcePart", 1);
      }).doNotSave().build());
    }).build();
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
    }).build();
  }

  private ObjectTagDefinition givenObjectPropertyWithConstruct(Construct construct) throws NodeException {
    return Builder.create(ObjectTagDefinition.class, oe -> {
        oe.setTargetType(Folder.TYPE_FOLDER);
        oe.setName("First Object Property", 1);
        ObjectTag objectTag = oe.getObjectTag();

        objectTag.setConstructId(construct.getConstructId()); //todo: checkme
        objectTag.setEnabled(true);
        objectTag.setName("object.first");
        objectTag.setObjType(Folder.TYPE_FOLDER);
    }).build();
  }

    private PackageDependency extractPackageDependencyOfType(List<PackageDependency> dependencies,
      Type type) {
    return dependencies.stream()
        .filter(packageDependency -> type == packageDependency.getDependencyType())
        .findFirst().get();
  }

  private SynchronizableNodeObject extractPackageObjectOfClass(
      List<SynchronizableNodeObject> packageObjects, Class<SynchronizableNodeObject> clazz) {
    return packageObjects.stream()
        .filter(clazz::isInstance).findFirst().get();
  }


  private List<PackageDependency> mockPackageWithDependencies() throws NodeException {
    List<PackageDependency> dependencies = new ArrayList<>();
    PackageDependency dependency = new PackageDependency();
    dependency.setName("[Manual] Contentpage");
    dependency.setDependencyType(Type.TEMPLATE);

    PackageDependency referencedDependency = new PackageDependency.Builder()
        .withName("construct")
        .withGlobalId("1234")
        .build();

    dependency.setReferencedDependencies(Collections.singletonList(referencedDependency));
    dependencies.add(dependency);

    return dependencies;
  }

}
