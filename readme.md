# JdbcTemplateMapper #

Spring's JdbcTemplate gives full control of data access using SQL. It is a better option for complex enterprise applications than an ORM (ORM magic/nuances get in the way for large and complex applications). Even though JdbcTemplate abstracts away a lot of the boiler plate code needed by JDBC, it still remains verbose.
 
JdbcTemplateMapper makes CRUD with JdbcTemplate simpler (Its constructor take JdbcTemplate as an argument). Use it for one line CRUD operations and for other query stuff use JdbcTemplate as you normally would.

**Features:** 

  1. One liners for CRUD. To keep the library as simple possible it only has 2 annotations.
  2. Can be configured for:
      * auto assign created on, updated on.
      * auto assign created by, updated by using an implementation of IRecordOperatorResolver.
      * optimistic locking functionality for updates by configuring a version property.
  3. Thread safe so just needs a single instance (similar to JdbcTemplate)
  4. To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section.
  5. Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with other relational databases. 

 **JdbcTemplateMapper is opinionated:**
 
 Projects have to meet the following 2 criteria to use it:
 
  1. Camel case object property names are mapped to snake case table column names. Properties of a model like 'firstName', 'lastName' will be mapped to corresponding columns 'first\_name' and 'last\_name' in the database table (If for a model property a column match is not found, those properties are ignored during CRUD operations).
  
  2. The table columns map to object properties and have no concept of relationships. So foreign keys in tables will need a corresponding **extra** property in the model. For example if an 'Order' is tied to a 'Customer', to match the 'customer\_id' column in the 'order' table you will need to have the 'customerId' property in the 'Order' model. 
 
 **Example usage code:** 
 
  ```java 
 // Product class below maps to 'product' table by default.
 // Use annotation @Table(name="some_tablename") to override the default
 
 public class Product {
     // @Id annotation is required.
     // For a auto increment database id use @Id(type=IdType.AUTO_INCREMENT)
     // For a non auto increment id use @Id. In this case you will have to manually set id value before insert.
 
     @Id(type=IdType.AUTO_INCREMENT)
     private Integer id;
     private String productName;
     private Double price;
     private LocalDateTime availableDate;
 
     // insert/update/find.. methods will ignore properties which do not
     // have a corresponding snake case columns in database table
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
 ```
 
 **Maven coordinates:** 
 TODO
 
 **Spring bean configuration for JdbcTemplateMapper:** 

 1. Configure JdbcTemplate bean as per Spring documentation
 2. Configure the JdbcTemplateMapper bean:
 
 ```
@Bean
public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
  return new JdbcTemplateMapper(jdbcTemplate);
  
  // JdbcTemplateMapper needs to get database metadata to generate the SQL statements.
  // Databases may differ on what criteria is needed to retrieve this information. JdbcTemplateMapper
  // has multiple constructors so use the appropriate one. In most cases 'new JdbcTemplateMapper(jdbcTemplate);'
  // will do the job.
}
  
  ```
**Annotations**

**@Table**

This is a class level annotation. Use it when the class does have a corresponding table. (ie when the camel case class name does not have a corresponding snake case table name in the database) 

For example if you want to map 'Product' to the 'products' table (note plural) use

```java
@Table(name="products")
class Product {
  ...
}
```

If you had not used the annotation JdbcTemplateMapper will try to map to a 'product' table using the default naming convention.

**@Id**

This is a required annotation. There are 2 forms of usage for this.

* **auto incremented id usage**

```java
class Product {
 @Id(type=IdType.AUTO_INCREMENT)
 private Integer productId;
  ...
}

```
After a successful insert() operation the productId property will be populated with the new id.

* **NON auto incremented id usage**

```java
class Customer {
 @Id
 private Integer id;
  ...
}

```

In this case you will have to manually set the id value before calling insert()


**Logging:**
 
Uses the same logging configurations as JdbcTemplate for logging the SQL.
 
 ```
 # log the SQL
 logging.level.org.springframework.jdbc.core.JdbcTemplate=TRACE

 # need this to log the INSERT statements
 logging.level.org.springframework.jdbc.core.simple.SimpleJdbcInsert=TRACE

 # log the parameters of SQL statement
 logging.level.org.springframework.jdbc.core.StatementCreatorUtils=TRACE
 
 ```
 
 **Notes:**
 1. If insert/update fails do not reuse the object since it could be in an inconsistent state.
 2. Database changes will need a restart of the application since JdbcTemplateMapper caches table metadata.
  
 