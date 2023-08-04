# JdbcTemplateMapper #
 
 Spring's JdbcTemplate provides data access using SQL/JDBC for relational databases. 
 JdbcTemplate is a good option for complex enterprise applications where an ORMs magic/nuances become challenging.
 Even though JdbcTemplate simplifies the use of JDBC, it still remains verbose.
 
 JdbcTemplateMapper makes CRUD with Spring's JdbcTemplate simpler. It provides one liners for CRUD.
 
 [Javadocs](https://jdbctemplatemapper.github.io/jdbctemplatemapper/javadoc/) 
 
## Features

  1. One liners for CRUD. To keep the library as simple possible it only has 2 annotations.
  2. Can be configured for the following (optional):
      * auto assign created on, updated on.
      * auto assign created by, updated by using an implementation of interface IRecordOperatorResolver.
      * optimistic locking functionality for updates by configuring a version property.
  3. Thread safe so just needs a single instance (similar to JdbcTemplate)
  4. To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section.
  5. Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with other relational databases. 


## JbcTemplateMapper is opinionated
 
 Projects have to meet the following criteria for use:
 
  1. Camel case object property names are mapped to underscore case table column names. Properties of a model like 'firstName', 'lastName' will be mapped to corresponding columns 'first\_name' and 'last\_name' in the database table. Properties which don't have a column match will be ignored during CRUD operations
  2. The model properties map to table columns and have no concept of relationships. Foreign keys in tables will need a corresponding property in the model. For example if an 'Order' is tied to a 'Customer', to match the 'customer\_id' column in the 'order' table there should be a 'customerId' property in the 'Order' model. 
 
## Example code
 
  ```java 
 //@Table annotation is required and should match a table name in database
 
 @Table(name="product")
 public class Product {
     // @Id annotation is required.
     // For auto increment database id use @Id(type=IdType.AUTO_INCREMENT)
     // For non auto increment id use @Id. In this case you will have to manually set id value before invoking insert().
 
     @Id(type=IdType.AUTO_INCREMENT)
     private Integer id;
     private String productName;
     private Double price;
     private LocalDateTime availableDate;
 
     // insert/update/find.. methods will ignore properties which do not
     // have a corresponding underscore case columns in database table
     private String someNonDatabaseProperty;
 
     // getters and setters ...
 }
 
 Product product = new Product();
 product.setProductName("some product name");
 product.setPrice(10.25);
 product.setAvailableDate(LocalDateTime.now());
 jdbcTemplateMapper.insert(product);

 product = jdbcTemplateMapper.findById(1, Product.class);
 product.setPrice(11.50);
 jdbcTemplateMapper.update(product);
 
 List<Product> products = jdbcTemplateMapper.findAll(Product.class);

 jdbcTemplateMapper.delete(product);
 jdbcTemplateMapper.delete(1, Product.class); // deleting just using id
 ```
 
## Maven coordinates

 ``` 
  <dependency>
    <groupId>io.github.jdbctemplatemapper</groupId>
    <artifactId>jdbctemplatemapper</artifactId>
    <version>1.0.1</version>
 </dependency>
 ```
 
## Spring bean configuration for JdbcTemplateMapper

 1. Configure the JdbcTemplate bean as per Spring documentation
 2. Configure the JdbcTemplateMapper bean:
 
 ```
@Bean
public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
  return new JdbcTemplateMapper(jdbcTemplate);
  
  // JdbcTemplateMapper needs to get database metadata to generate the SQL statements.
  // Databases may differ on what criteria is needed to retrieve this information. JdbcTemplateMapper
  // has multiple constructors so use the appropriate one. For example if you are using oracle and tables
  // are not aliased the SQL will need schemaName.tableName to access the table. In this case 
  // use the constructor new JdbcTemplateMapper(jdbcTemplate, schemaName);
}
  
  ```
## Annotations

**@Table**

This is a class level annotation and is required. It can be any name and should match a table in the database

```java
@Table(name="product")
class Product {
  ...
}
```

**@Id**

This is a required annotation. There are 2 forms of usage for this.

* **auto incremented id usage**

```java
@Table(name="product")
class Product {
 @Id(type=IdType.AUTO_INCREMENT)
 private Integer productId;
  ...
}

```
After a successful insert() operation the productId property will be populated with the new id.

* **NON auto incremented id usage**

```java
@Table(name="customer")
class Customer {
 @Id
 private Integer id;
  ...
}

```

In this case you will have to manually set the id value before calling insert()

## Configuration to auto assign created on, created by, updated on, updated by, version (optimistic locking)
 
 All these auto assign configurations are optional.

    JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate);
    jdbcTemplateMapper
        .withRecordOperatorResolver(new ConcreteImplementationOfIRecordOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");
        
 Example model
 
 ```java
@Table(name="product")
class Product {
 @Id(type=IdType.AUTO_INCREMENT)
 private Integer productId;
 ...
  private LocalDateTime createdOn;
  private String createdBy;
  
  private LocalDateTime updatedOn;
  private String updatedBy;
  
  private Integer version;
}
```

The following will be the effect of the configuration:

* created on:
For insert the matching property value on the model will be set to the current datetime. Property should be of type LocalDateTime
* update on:
For update the matching property value on the model will be set to the current datetime. Property should be of type LocalDateTime
* created by:
For insert the matching property value on the model will be set to value returned by implementation of IRecordOperatorResolver
* updated by:
For update the matching property value on the model will be set to value returned by implementation of IRecordOperatorResolver
* version:
For update the matching property value on the model will be incremented if successful. If version is stale, an OptimisticLockingException will be thrown. For an insert this value will be set to 1. The version property should be of type Integer.
 

## Logging
 
Uses the same logging configurations as JdbcTemplate to log the SQL.
 
 ```
 # log the SQL
 logging.level.org.springframework.jdbc.core.JdbcTemplate=TRACE

 # need this to log the INSERT statements
 logging.level.org.springframework.jdbc.core.simple.SimpleJdbcInsert=TRACE

 # log the parameters of SQL statement
 logging.level.org.springframework.jdbc.core.StatementCreatorUtils=TRACE
 
 ```
 
## Notes
 1. If insert/update fails do not reuse the object since it could be in an inconsistent state.
 2. Database changes will require a restart of the application since JdbcTemplateMapper caches table metadata.
 
## TroubleShooting
Make sure you can connect to your database and issue a simple query using JdbcTemplate without the JdbcTemplateMapper.
  
 