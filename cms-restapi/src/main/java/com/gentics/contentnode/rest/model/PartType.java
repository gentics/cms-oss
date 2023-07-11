package com.gentics.contentnode.rest.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PartType {

  private int id;
  private String name;
  private String description;
  private boolean auto;
  private String javaClass;
  private boolean deprecated;

  public PartType() {
  }

  public PartType buildFromResultSet(ResultSet rs) throws SQLException {
    this.id = rs.getInt("id");
    this.name = rs.getString("name");
    this.description = rs.getString("description");
    this.auto = rs.getBoolean("auto");
    this.javaClass = rs.getString("javaclass");
    this.deprecated = rs.getBoolean("deprecated");
    return this;
  }

  public String getName() {
    return this.name;
  }

  public PartType setName(String name) {
    this.name = name;
    return this;
  }

  public int getId() {
    return id;
  }

  public PartType setId(int id) {
    this.id = id;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public PartType setDescription(String description) {
    this.description = description;
    return this;
  }

  public boolean isAuto() {
    return auto;
  }

  public PartType setAuto(boolean auto) {
    this.auto = auto;
    return this;
  }

  public String getJavaClass() {
    return javaClass;
  }

  public PartType setJavaClass(String javaClass) {
    this.javaClass = javaClass;
    return this;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public PartType setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
    return this;
  }

}