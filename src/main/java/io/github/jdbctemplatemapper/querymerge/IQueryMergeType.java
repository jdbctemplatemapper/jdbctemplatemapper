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
package io.github.jdbctemplatemapper.querymerge;

/**
 * interface with the next methods in the chain.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryMergeType<T> {
  IQueryMergeHasMany<T> hasMany(Class<?> relatedType);

  IQueryMergeHasMany<T> hasMany(Class<?> relatedType, String tableAlias);

  IQueryMergeHasOne<T> hasOne(Class<?> relatedType);

  IQueryMergeHasOne<T> hasOne(Class<?> relatedType, String tableAlias);
}
