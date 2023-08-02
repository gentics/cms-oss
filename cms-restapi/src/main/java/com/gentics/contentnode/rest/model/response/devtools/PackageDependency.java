package com.gentics.contentnode.rest.model.response.devtools;


import com.gentics.contentnode.rest.model.AbstractModel;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
public class PackageDependency extends AbstractModel {

  /**
   * The keyword of the package object
   */
  private String keyword;

  /**
   * The name of the package object.
   */
  private String name;

  /**
   * The type of the package.
   */
  private Type dependencyTyp;
  /**
   * A flag indicating whether the object is in the current package.
   */
  private Boolean isInPackage;
  /**
   * A flag indicating whether the object is located in a different package.
   */
  private Boolean isInOtherPackage;

  /**
   * The list of package dependencies that are referenced by this object.
   */
  private List<PackageDependency> referencedDependencies;

  public PackageDependency() {

  }

  /**
   * Gets the name of the package object.
   *
   * @return The name of the object.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the package object.
   *
   * @param name The new name of the object.
   */
  public void setName(String name) {
    this.name = name;
  }


  /**
   * Gets the keyword of the package object.
   *
   * @return The name of the object.
   */
  public String getKeyword() {
    return keyword;
  }


  /**
   * Gets the type of the package object.
   *
   * @return The type of the object.
   */
  public Type getDependencyTyp() {
    return dependencyTyp;
  }

  /**
   * Sets the type of the package object.
   *
   * @param dependencyTyp The new type of the object.
   */
  public void setDependencyTyp(Type dependencyTyp) {
    this.dependencyTyp = dependencyTyp;
  }

  /**
   * Returns true if the object is in the current package, false otherwise.
   *
   * @return Whether the object is in the current package.
   */
  public Boolean isInPackage() {
    return isInPackage;
  }

  /**
   * Sets whether the object is in the current package.
   *
   * @param isInPackage Whether the object is in the current package.
   */
  public void setInPackage(Boolean isInPackage) {
    this.isInPackage = isInPackage;
  }

  /**
   * Gets whether the object is located in a different package.
   *
   * @return True if the object is located in a different package, false otherwise.
   */
  public Boolean isInOtherPackage() {
    return isInOtherPackage;
  }

  /**
   * Sets whether the object is located in a different package.
   *
   * @param isInOtherPackage True if the object is located in a different package, false
   * otherwise.
   */
  public void setInOtherPackage(Boolean isInOtherPackage) {
    this.isInOtherPackage = isInOtherPackage;
  }

  /**
   * Gets the list of dependencies that are referenced by this object.
   *
   * @return The list of object dependencies.
   */
  public List<PackageDependency> getReferencedDependencies() {
    return referencedDependencies;
  }


  /**
   * Set the list of dependencies that are referenced by this object.
   */
  public void setReferencedDependencies(List<PackageDependency> referencedDependencies) {
    this.referencedDependencies = referencedDependencies;
  }

  @XmlType(name = "Type")
  public enum Type {
    CONSTRUCT,
    TEMPLATE,
    DATASOURCE,
    OBJECT_PROPERTY,
    CONTENT_REPOSITORY,

    TEMPLATE_TAG,
    OBJECT_TAG_DEFINITION,

    UNKNOWN;
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

    public Builder withKeyword(String keyword) {
      dependency.keyword = keyword;
      return this;
    }

    public Builder withType(Type type) {
      dependency.setDependencyTyp(type);
      return this;
    }

    public Builder withDependencies(List<PackageDependency> dependencies) {
      dependency.setReferencedDependencies(dependencies);
      return this;
    }

    public Builder withIsInPackage(boolean isInPackage) {
      dependency.setInPackage(isInPackage);
      return this;
    }

    public Builder withIsInOtherPackage(boolean isInOtherPackage) {
      dependency.setInOtherPackage(isInOtherPackage);
      return this;
    }

  }


}
