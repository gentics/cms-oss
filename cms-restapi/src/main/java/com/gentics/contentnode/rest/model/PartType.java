package com.gentics.contentnode.rest.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PartType {

  private int id;
  private String name;
  private String description;
  private boolean auto;
  private String javaClass;
  private boolean deprecated;

  /**
   * Method to create an instance from a ResultSet
   * @return fluent API
   */
  public PartType buildFromResultSet(ResultSet rs) throws SQLException {
    this.id = rs.getInt("id");
    this.name = rs.getString("name");
    this.description = rs.getString("description");
    this.auto = rs.getBoolean("auto");
    this.javaClass = rs.getString("javaclass");
    this.deprecated = rs.getBoolean("deprecated");
    return this;
  }

  /**
   * The name of the part type
   * @return name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set the name of the part type
   * @return fluent API
   */
  public PartType setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get the id of the part type
   * @return the part type id
   */
  public int getId() {
    return id;
  }


  /**
   * Set the id of the part type
   * @return fluent API
   */
  public PartType setId(int id) {
    this.id = id;
    return this;
  }

  /**
   * Get the description of the part type
   * @return the description of the part type
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the description of the part type
   * @return fluent API
   */
  public PartType setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * The auto flag
   * @return true if auto is set
   */
  public boolean isAuto() {
    return auto;
  }

  /**
   * The auto flag
   * @return fluent API
   */
  public PartType setAuto(boolean auto) {
    this.auto = auto;
    return this;
  }

  /**
   * Get the java class (implementation) of the part type
   * @return the implementation class
   */
  public String getJavaClass() {
    return javaClass;
  }

  /**
   * Set the java class (implementation) of the part type
   * @return fluent API
   */
  public PartType setJavaClass(String javaClass) {
    this.javaClass = javaClass;
    return this;
  }

  /**
   * The deprecation flag
   * @return ture if the part type is deprecated
   */
  public boolean isDeprecated() {
    return deprecated;
  }

  /**
   * Set the java class (implementation) of the part type
   * @return fluent API
   */
  public PartType setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
    return this;
  }

}