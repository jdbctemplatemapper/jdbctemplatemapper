/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jdbctemplatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Identifier.
 *
 * <pre>
 * For auto increment id:
 * {@literal @}Table(name="products")
 *  class Product {
 *   {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *    private Integer productId;
 *    ...
 *  }
 * After a successful insert() operation the productId property will be populated with the new id.
 *
 * For NON auto increment id:
 * {@literal @}Table(name="customer")
 *  class Customer {
 *   {@literal @}Id
 *    private Integer id;
 *    ...
 *  }
 * In this case you will have to manually set the id value before calling insert()
 * </pre>
 *
 * @author ajoseph
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
  IdType type() default IdType.MANUAL;
}
