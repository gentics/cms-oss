package com.gentics.contentnode.rest.resource.impl.devtools;

import com.gentics.api.lib.etc.ObjectTransformer;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class ConstructResolver extends AbstractDependencyResolver {

  // todo: is this correct? only filter for ds? check other field as well?
  public final static List<Integer> DEPENDENCIES = Arrays.asList(
      Part.DATASOURCE, Part.SELECTSINGLE, Part.SELECTMULTIPLE);


  @Override
  public List<PackageDependency> resolve(PackageSynchronizer packageSynchronizer)
      throws NodeException {
    List<PackageObject<Construct>> packageObjects = packageSynchronizer.getObjects(Construct.class);
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
          .withIsInPackage(isInPackage(packageSynchronizer,
              part.getInfoInt())) //todo: is this mapping ok  part.info_int  to datasource.id ?
          .withType(
              Type.DATASOURCE) //todo: check me (is a multi select also a datasource in this context) ? // map all to DS ok?
          .build();

      referencedDependencies.add(referencedDependency);
    }

    return referencedDependencies;
  }


  /**
   * Get the datasource for the given local ID, global ID or name from the package
   *
   * @param synchronizer package
   * @param datasourceId local ID, global ID or name
   * @return true if datasource is in package
   * @throws NodeException
   */
  private boolean isInPackage(PackageSynchronizer synchronizer, int datasourceId)
      throws NodeException {
    Optional<String> resolvedUuidOpt = resolveUuid(datasourceId);
    if(!resolvedUuidOpt.isPresent()){
      return false;
    }

    String resolvedUuid = resolvedUuidOpt.get();

    return synchronizer.getObjects(Datasource.class).stream().anyMatch(d ->
        resolvedUuid.equals(ObjectTransformer.getString(d.getObject().getId(), null))
            || resolvedUuid.equals(ObjectTransformer.getString(d.getObject().getGlobalId(), null))
            || resolvedUuid.equals(d.getObject().getName()));
  }

  /**
   * Utility method to map datasourceId to the uuid of a datasource
   * @param datasourceId numeric id that should be mapped to an uuid
   * @return the mapped uuid
   * @throws NodeException
   */
  private Optional<String> resolveUuid(int datasourceId) throws NodeException {
    if (datasourceId == 0) {
      return Optional.empty();
    }

    return DBUtils.select("SELECT `uuid` FROM `datasource` WHERE `id` = ?", (ps) -> {
      ps.setInt(1, datasourceId);
    }, (resultSet) -> {
      if (resultSet.next()) {
        return Optional.of(resultSet.getString("uuid"));
      }
      return Optional.empty();
    });
  }


}
