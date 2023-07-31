package com.gentics.contentnode.rest.model.response.devtools;


import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
public class PackageDependency {

  /**
   * The list of package dependencies that are referenced by this object.
   */
  private final List<PackageDependency> referencedDependencies = new ArrayList<>();
  /**
   * The name of the package.
   */
  private String name;
  /**
   * The globally-unique identifier for the package.
   */
  private String globalId;
  /**
   * The type of the package.
   */
  private Type dependencyTyp;
  /**
   * A flag indicating whether the package is in the current package.
   */
  private boolean isInPackage;
  /**
   * A flag indicating whether the package is located in a different package.
   */
  private boolean isInOtherPackage;

  public PackageDependency() {

  }

  /**
   * Gets the name of the package.
   *
   * @return The name of the package.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the package.
   *
   * @param name The new name of the package.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the globally-unique identifier for the package.
   *
   * @return The global identifier for the package.
   */
  public String getGlobalId() {
    return globalId;
  }

  /**
   * Sets the globally-unique identifier for the package.
   *
   * @param globalId The new global identifier for the package.
   */
  public void setGlobalId(String globalId) {
    this.globalId = globalId;
  }

  /**
   * Gets the type of the package.
   *
   * @return The type of the package.
   */
  public Type getDependencyTyp() {
    return dependencyTyp;
  }

  /**
   * Sets the type of the package.
   *
   * @param dependencyTyp The new type of the package.
   */
  public void setDependencyTyp(Type dependencyTyp) {
    this.dependencyTyp = dependencyTyp;
  }

  /**
   * Returns true if the package is in the current package, false otherwise.
   *
   * @return Whether the package is in the current package.
   */
  public boolean isInPackage() {
    return isInPackage;
  }

  /**
   * Sets whether the package is in the current package.
   *
   * @param isInPackage Whether the package is in the current package.
   */
  public void setInPackage(boolean isInPackage) {
    this.isInPackage = isInPackage;
  }

  /**
   * Gets whether the package is located in a different package.
   *
   * @return True if the package is located in a different package, false otherwise.
   */
  public boolean isInOtherPackage() {
    return isInOtherPackage;
  }

  /**
   * Sets whether the package is located in a different package.
   *
   * @param isInOtherPackage True if the package is located in a different package, false
   *                         otherwise.
   */
  public void setInOtherPackage(boolean isInOtherPackage) {
    this.isInOtherPackage = isInOtherPackage;
  }

  /**
   * Gets the list of dependencies that are referenced by this object.
   *
   * @return The list of package dependencies.
   */
  public List<PackageDependency> getReferencedDependencies() {
    return referencedDependencies;
  }


  /**
   * Set the list of dependencies that are referenced by this object.
   */
  public void setReferencedDependencies(List<PackageDependency> referencedDependencies) {
    this.setReferencedDependencies(referencedDependencies);
  }

  @XmlType(name = "Type")
  public enum Type {
    CONSTRUCT,
    TEMPLATE,
    DATASOURCE,
    OBJECT_PROPERTY,
    CONTENT_REPOSITORY
  }


  public static class Builder {

    PackageDependency dependency;

    public Builder() {
      this.dependency = new PackageDependency();
    }

    public PackageDependency build() {
      return dependency;
    }

    public Builder withGlobalId(String globalId) {
      dependency.setGlobalId(globalId);
      return this;
    }

    public Builder withName(String name) {
      dependency.setName(name);
      return this;
    }

    public Builder withDependencies(List<PackageDependency> dependencies) {
      dependency.setReferencedDependencies(dependencies);
      return this;
    }

  }


}
