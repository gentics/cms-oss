package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency;
import com.gentics.contentnode.rest.model.response.devtools.PackageDependency.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConstructResolver extends AbstractDependencyResolver {

  private final Class<Construct> CLAZZ = Construct.class;


  @Override
  public List<PackageDependency> resolve(PackageSynchronizer packageSynchronizer)
      throws NodeException {
    List<PackageObject<Construct>> packageObjects = packageSynchronizer.getObjects(CLAZZ);
    List<PackageDependency> resolvedDependencyList = new ArrayList<>();

    for (PackageObject<Construct> packageObject : packageObjects) {
      Construct construct = packageObject.getObject();
      List<PackageDependency> references = resolveReferences(packageSynchronizer, construct);

      PackageDependency dependency = new PackageDependency.Builder()
          .withGlobalId(construct.getGlobalId().toString())
          .withName(construct.getName().toString())
          .withKeyword(construct.getKeyword())
          .withType(Type.CONSTRUCT)
          .withDependencies(references)
          .build();

      resolvedDependencyList.add(dependency);
    }

    return resolvedDependencyList;
  }

  private List<PackageDependency> resolveReferences(PackageSynchronizer packageSynchronizer,
      Construct construct) throws NodeException {
    final List<Integer> DEPENDENCIES = Arrays.asList(Part.SELECTSINGLE, Part.SELECTMULTIPLE);
    List<PackageDependency> referencedDependencies = new ArrayList<>();

    List<Part> datasourceParts = construct.getParts().stream().filter(
            part -> DEPENDENCIES.stream()
                .anyMatch(type -> type == part.getPartTypeId())) //todo: check me
        .collect(Collectors.toList());

    for (Part part : datasourceParts) {
      PackageDependency referencedDependency = new PackageDependency.Builder()
          .withGlobalId(part.getGlobalId().toString())
          .withKeyword(part.getKeyname())
          .withName(part.getName().toString())
          .withIsInPackage(
              isInPackage(packageSynchronizer, Datasource.class, resolveUuid(part.getInfoInt())))
          //todo: is this mapping ok  part.info_int to datasource.id ?
          .withType(
              Type.DATASOURCE)
          .build();

      referencedDependencies.add(referencedDependency);
    }

    return referencedDependencies;
  }


  /**
   * Utility method to map datasourceId to the uuid of a datasource
   *
   * @param datasourceId numeric id that should be mapped to an uuid
   * @return the mapped uuid
   * @throws NodeException
   */
  private String resolveUuid(int datasourceId) throws NodeException {
    if (datasourceId == 0) {
      return "";
    }

    return DBUtils.select("SELECT `uuid` FROM `datasource` WHERE `id` = ?", (ps) -> {
      ps.setInt(1, datasourceId);
    }, (resultSet) -> {
      if (resultSet.next()) {
        return resultSet.getString("uuid");
      }
      return "";
    });
  }


}
