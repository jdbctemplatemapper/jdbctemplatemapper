# JdbcTemplateMapper #
 
 The goal of JdbcTemplateMapper is to make usage of Spring's JdbcTemplate less verbose for features like CRUD and relationship queries.
 Use it where appropriate and for other features keep using JdbcTemplate as you normally would.
 
 [Javadoc](https://jdbctemplatemapper.github.io/jdbctemplatemapper/javadoc/) 
 
## Features

  1. One liners for CRUD.
  2. Features that help make querying of relationships less verbose.
  3. Can be configured for the following (optional):
      * auto assign created on, updated on.
      * auto assign created by, updated by using an implementation of interface IRecordOperatorResolver.
      * optimistic locking functionality for updates by configuring a version property.
  4. Thread safe so just needs a single instance (similar to JdbcTemplate).
  5. For transaction management use Spring transactions since the library uses JdbcTemplate behind the scenes.
  6. To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section.
  7. Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with other relational databases. 


## Project criteria for usage 
  1. The JdbcTemplateMapper has no concept of relationships. When an insert/update is issued, only that model gets inserted/updated.  
  2. Camel case model properties are mapped to underscore case columns in database table.
  
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
 jdbcTemplateMapper.insert(product); // because id type is auto increment id value will be set after insert.

 product = jdbcTemplateMapper.findById(product.getId(), Product.class);
 product.setPrice(11.50);
 jdbcTemplateMapper.update(product);
 
 List<Product> products = jdbcTemplateMapper.findAll(Product.class);

 jdbcTemplateMapper.delete(product);
 jdbcTemplateMapper.delete(5, Product.class); // deleting just using id
 
 // for methods which help make querying relationships less verbose see section 'Querying relationships' below
 
 ```
 
## Maven coordinates

 ``` 
  <dependency>
    <groupId>io.github.jdbctemplatemapper</groupId>
    <artifactId>jdbctemplatemapper</artifactId>
    <version>1.1.3</version>
 </dependency>
 ```
 
## Spring bean configuration for JdbcTemplateMapper

 1. Configure the JdbcTemplate bean as per Spring documentation
 2. Configure the JdbcTemplateMapper bean:
 
 ```java
@Bean
public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
  return new JdbcTemplateMapper(jdbcTemplate);
  
 // new JdbcTemplateMapper(jdbcTemplate, schemaName);
 // new JdbcTemplateMapper(jdbcTemplate, schemaName, catalogName);
 // see javadocs for all constructors
}
  
  ```
## Annotations

**@Table**

Required class level annotation. It can be any name and should match a table in the database

```java
@Table(name="product")
class Product {
  ...
}
```

**@Id**

Required field level annotation. There are 2 forms of usage for this.

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
 
 Auto configuration is optional and each property configuration is optional.
 
 Once configured matching properties of models will get auto assigned (Models don't need to have these properties but
 if they do they will get auto assigned).
 
```java
@Bean
public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate);
    jdbcTemplateMapper
        .withRecordOperatorResolver(new ConcreteImplementationOfIRecordOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");
}
```
Example model:

```java
@Table(name="product")
class Product {
 @Id(type=IdType.AUTO_INCREMENT)
 private Integer productId;
 ...
  private LocalDateTime createdOn; // type should be LocalDateTime
  private String createdBy;        // type should match return value of implementation of IRecordOperatorResolver.
  
  private LocalDateTime updatedOn; // type should be LocalDateTime
  private String updatedBy;        // type should match return value of implementation of IRecordOperatorResolver.
  
  private Integer version;         // type should be Integer for optimistic locking version
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
 
## Querying relationships

SelectMapper allows generating the select columns string for the model and population of the model from a ResultSet.

selectMapper.getColumnsSql() will provide a string of columns which can be used in the sql select statement.

selectMapper.buildModel(resultSet) will return a model populated from the resultSet data.

Makes the code for writing and retrieving relationships less verbose.

An example for querying the following relationship: An 'Order' has many 'OrderLine' and each 'OrderLine' has one product
using Spring's ResultSetExtractor  

```java
 @Table(name = "orders")
  public class Order {
    @Id(type = IdType.AUTO_INCREMENT)
    private Long orderId;
    private LocalDateTime orderDate;
    private String customerName;
    
    List<OrderLine> orderLines = new ArrayList<>(); 
  }
  
  @Table(name="order_line")
  public class OrderLine {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer orderLineId;

    private Integer orderId;
  
    private Integer productId;
    private Product product;
    
    private Integer numOfUnits;
  }
  
  @Table(name="product")
  public class Product {
    @Id 
    private Integer productId; 
    private String name;
    private Double cost;
  }

 // The second argument to getSelectMapper() is the table alias used in the query.
 // For the query below the 'orders' table alias is 'o', the 'order_line' table alias is 'ol' and the product
 // table alias is 'p'.
 SelectMapper<Order> orderSelectMapper = jdbcTemplateMapper.getSelectMapper(Order.class, "o");
 SelectMapper<OrderLine> orderLineSelectMapper = jdbcTemplateMapper.getSelectMapper(OrderLine.class, "ol");
 SelectMapper<Product> productSelectMapper = jdbcTemplateMapper.getSelectMapper(Product.class, "p");

 // no need to type all those column names so we can concentrate on where and join clauses
 String sql = "select" 
               + orderSelectMapper.getColumnsSql() 
               + ","
               + orderLineSelectMapper.getColumnsSql() 
               + "," 
               + productSelectMapper.getColumnsSql()
               + " from orders o" 
               + " left join order_line ol on o.order_id = ol.order_id"
               + " join product p on p.product_id = ol.product_id"
               + " order by o.order_id, ol.order_line_id";
               
 // Using Spring's ResultSetExtractor 		
 ResultSetExtractor<List<Order>> rsExtractor = new ResultSetExtractor<List<Order>>() {
     @Override
     public List<Order> extractData(ResultSet rs) throws SQLException, DataAccessException {	
     
       Map<Long, Order> orderByIdMap = new LinkedHashMap<>(); // LinkedHashMap to retain result order	
       Map<Integer, Product> productByIdMap = new HashMap<>();
 		
       while (rs.next()) {				
         // selectMapper.buildModel(rs) will return the model populated from the resultSet
         // Everything below is just logic to populate the relationships
         // Doing some checks to make sure unwanted objects are not created.
         // In this use case Order has many OrderLine and an OrderLine has one product
 					
         // orderSelectMapper.getResultSetModelIdColumnName() returns the id column alias which is 'o_order_id'
         // for the sql above. 
         Long orderId = rs.getLong(orderSelectMapper.getResultSetModelIdColumnName());						
         Order order = orderByIdMap.get(orderId);
         if (order == null) {
           order = orderSelectMapper.buildModel(rs);
           orderByIdMap.put(order.getOrderId(), order);
         }
 				    
         // productSelectMapper.getResultSetModelIdColumnName() returns the id column alias which is 'p_product_id'
         // for the sql above.
         Integer productId = rs.getInt(productSelectMapper.getResultSetModelIdColumnName());
         Product product = productByIdMap.get(productId);
         if (product == null) {
           product = productSelectMapper.buildModel(rs);
           productByIdMap.put(product.getProductId(), product);
         }
 				    
         OrderLine orderLine = orderLineSelectMapper.buildModel(rs);	
         if(orderLine != null) {
           orderLine.setProduct(product);
           order.getOrderLines().add(orderLine);
         }			
      }
      return new ArrayList<Order>(orderByIdMap.values());
    }
  };
 
  // execute the JdbcTemplae query	
  List<Order> orders = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor);
    
```


## Logging
 
Uses the same logging configurations as JdbcTemplate to log the SQL. In applications.properties:
 
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
 3. Models will need a no argument constructor so it can be instantiated and properties set.
 
## TroubleShooting
Make sure you can connect to your database and issue a simple query using Spring's JdbcTemplate without the JdbcTemplateMapper.

## Known issues
1. For Oracle/SqlServer no support for blob/clob. Use JdbcTemplate directly for this with recommended custom code
  
 