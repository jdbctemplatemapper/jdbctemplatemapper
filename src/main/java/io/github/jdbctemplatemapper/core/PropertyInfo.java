/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jdbctemplatemapper.core;

/**
 * Property info.
 *
 * @author ajoseph
 */
class PropertyInfo {
  private String propertyName;
  private Class<?> propertyType;

  private boolean idAnnotation = false;
  private boolean createdOnAnnotation = false;
  private boolean updatedOnAnnotation = false;
  private boolean versionAnnotation = false;
  private boolean createdByAnnotation = false;
  private boolean updatedByAnnotation = false;

  public PropertyInfo(String propertyName, Class<?> propertyType) {
    this.propertyName = propertyName;
    this.propertyType = propertyType;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public Class<?> getPropertyType() {
    return propertyType;
  }

  public boolean isIdAnnotation() {
    return idAnnotation;
  }

  public void setIdAnnotation(boolean idAnnotation) {
    this.idAnnotation = idAnnotation;
  }

  public boolean isCreatedOnAnnotation() {
    return createdOnAnnotation;
  }

  public void setCreatedOnAnnotation(boolean createdOnAnnotation) {
    this.createdOnAnnotation = createdOnAnnotation;
  }

  public boolean isUpdatedOnAnnotation() {
    return updatedOnAnnotation;
  }

  public void setUpdatedOnAnnotation(boolean updatedOnAnnotation) {
    this.updatedOnAnnotation = updatedOnAnnotation;
  }

  public boolean isVersionAnnotation() {
    return versionAnnotation;
  }

  public void setVersionAnnotation(boolean versionAnnotation) {
    this.versionAnnotation = versionAnnotation;
  }

  public boolean isCreatedByAnnotation() {
    return createdByAnnotation;
  }

  public void setCreatedByAnnotation(boolean createdByAnnotation) {
    this.createdByAnnotation = createdByAnnotation;
  }

  public boolean isUpdatedByAnnotation() {
    return updatedByAnnotation;
  }

  public void setUpdatedByAnnotation(boolean updatedByAnnotation) {
    this.updatedByAnnotation = updatedByAnnotation;
  }
}
