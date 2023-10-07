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
 * An implementation of this interface should return the value used to populate properties annotated
 * with {@literal @}CreatedBy and {@literal @}UpdatedBy.
 *
 * @author ajoseph
 */
public interface IRecordOperatorResolver {
  /**
   * The implementation should return the value (name/id etc) which is used to populate the created
   * by and updated by properties of the object while inserting/updating
   *
   * @return Object - The value ie name/id etc of user who operated on the record.
   */
  public Object getRecordOperator();
}
