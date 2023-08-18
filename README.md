# JdbcTemplateMapper #
 
 The goal of JdbcTemplateMapper is to make the usage of Spring's JdbcTemplate less verbose for features like CRUD and relationship queries.
 Use it where appropriate and for other features keep using JdbcTemplate as you normally would.
 
 [Javadoc](https://jdbctemplatemapper.github.io/jdbctemplatemapper/javadoc/) 
 
## Features

  1. One liners for CRUD.
  2. Features that help make querying of relationships less verbose.
  3. Auto assign properties for models:
      * auto assign created on, updated on.
      * auto assign created by, updated by using an implementation of interface IRecordOperatorResolver.
      * optimistic locking functionality for updates by configuring a version property.
  4. For transaction management use Spring transactions since the library uses JdbcTemplate behind the scenes.
  5. To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section.
  6. Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with other relational databases. 
  
## Example code
 
  ```java 
 //@Table annotation is required
 @Table(name="product")
 public class Product {
     // @Id annotation is required.
     // For auto increment database id use @Id(type=IdType.AUTO_INCREMENT)
     // For non auto increment id use @Id. In this case you will have to manually set id value before invoking insert().
 
     @Id(type=IdType.AUTO_INCREMENT)
     private Integer id;   
       
     @Column(name="product_name")   
     private String name;                    // will map to product_name column in table
     
     @Column
     private LocalDateTime availableDate;    // will map to column available_date by default 
       
     @Column
     private Double price;                   // will map to price column
     
     private String someNonDatabaseProperty; // No annotations so excluded from inserts/updates/queries
 
     ...
 }
 
 Product product = new Product();
 product.setName("some product name");
 product.setPrice(10.25);
 product.setAvailableDate(LocalDateTime.now());
 jdbcTemplateMapper.insert(product); // because id type is auto increment, id value will be set after insert.

 product = jdbcTemplateMapper.findById(Product.class,product.getId());
 product.setPrice(11.50);
 jdbcTemplateMapper.update(product);
 
 List<Product> products = jdbcTemplateMapper.findAll(Product.class);

 jdbcTemplateMapper.delete(product);
 jdbcTemplateMapper.delete(Product.class, 5); // deleting just using id
 
 // for methods which help make querying relationships less verbose see section 'Querying relationships' below
 
 ```
 
## Maven coordinates

 ``` 
  <dependency>
    <groupId>io.github.jdbctemplatemapper</groupId>
    <artifactId>jdbctemplatemapper</artifactId>
    <version>1.3.1</version>
 </dependency>
 ```
 
## Spring bean configuration for JdbcTemplateMapper

 1. Configure the JdbcTemplate bean as per Spring documentation
 2. Configure the JdbcTemplateMapper bean:

 **Note: An instance JdbcTemplateMapper is thread safe**
 
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

There are 2 forms of usage for this.

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

**@Column**

Properties that need be persisted to the database will need @Column annotation unless the property is already annotated with one of the other annotations (@Id, @Version, @CreatedOn @CreatedBy @UpdatedOn @UpdatedBy). @Column can be used with the other annotations to map to a different column name.

The two ways to use it:

@Column  
This will map property to a column using the default naming convention of camel case to underscore name.

@Column(name="some_colum_name")  
This will map the property to the column specified by name attribute. 

```java
@Table(name="customer")
class Customer {
 @Id
 @Column(name = "customer_id")   
 private Integer id;           // this will map id property to customer_id in database table.
 
 @Column
 private String customerName;  // will map to custmer_name column by default
 
 @Column(name="type")
 private String customerType;  // will map to type column in database
 
 private String something;     // This will not be persisted because it has no annotations
 
}
```


**@Version**

This annotation is used for optimistic locking. It has to be of type Integer.
Will be set to 1 when record is created and will incremented on updates. On updates if the version is stale an OptimisticLockingException will be thrown.  @Column annotation can be used with this to map to a different column name.

**@CreatedOn**

When record is created the property will be set. It has to be of type LocalDateTime. @Column annotation can be used with this to map to a different column name.

**@UpdatedOn**

On updates  the property will be set. It has to be of type LocalDateTime. @Column annotation can be used with this to map to a different column name.

**@CreatedBy**

If IRecordOperatorResolver is implemented and configured with JdbcTemplateMapper the value will be set to value returned by implementation when the record is created. Without configuration no values will be set. The type returned should match the type of the property. @Column annotation can be used with this to map to a different column name.

**@UpdatedBy**

If IRecordOperatorResolver is implemented and configured with JdbcTemplateMapper the value will be set to value returned by implementation when the record is updated. Without configuration no values will be set. The type returned should match the type of the property. @Column annotation can be used with this to map to a different column name.
  

Example of the annotations:

```java
@Table(name="product")
class Product {

 @Id(type=IdType.AUTO_INCREMENT)
 private Integer productId; 
 
 @Column(name="product_name")
 private String name;              // maps to product_name column
 
 @Column
 private String productDescription // defaults to column product_description 
 
 @Column(name="created_timestamp")
 @CreatedOn 
 private LocalDateTime createdAt;  // maps to column created_timestamp. Property type should be LocalDateTime 
  
 @CreatedBy
 private String createdByUser;     // defaults to column created_by_user. 
                                   // Property type should match return value of implementation of IRecordOperatorResolver.  
  
 @UpdatedOn
 private LocalDateTime updatedAt;  // defaults to column updated_at. Property type should be LocalDateTime  
 
 @Column(name="last_update_user")
 @UpdatedBy
 private String updatedBy;         // maps to column last_update_user. 
                                   // Property type should match return value of implementation of IRecordOperatorResolver.
   
 @Version
 private Integer version;          // defaults to column version, 
                                   // Property type should be Integer. Used for optimistic locking
  
}
```

## Configuration for auto assigning @CreatedBy and @UpdateBy
 
```java
@Bean
public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate);
    jdbcTemplateMapper
        .withRecordOperatorResolver(new ConcreteImplementationOfIRecordOperatorResolver())
}
```
 
## Querying relationships

For querying complex relationships using SelectMapper with Spring's ResultSetExtractor
works well.

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
    @Column
    private LocalDateTime orderDate;
    @Column
    private String customerName;
    List<OrderLine> orderLines = new ArrayList<>(); // No mapping annotations for relationships
  }
  
  @Table(name="order_line")
  public class OrderLine {
    @Id(type = IdType.AUTO_INCREMENT)
    
    @Column
    private Integer orderLineId;

    @Column
    private Integer orderId;
  
    @Column
    private Integer productId;
    
    private Product product; // no mapping annotations for relationships
    
    @Column
    private Integer numOfUnits;
  }
  
  @Table(name="product")
  public class Product {
    @Id 
    private Integer productId; 
    
    @Column
    private String name;
    
    @Column
    private Double price;
  }

 // The second argument to getSelectMapper() below is the table alias used in the query.
 // For the query below the 'orders' table alias is 'o', the 'order_line' table alias is 'ol' and the product
 // table alias is 'p'. SelectMapper.getColumnsSql() aliases the columns with the prefix; table alias + "_" so that there are 
 // no conflicts in sql when different models have same property names like for example id. SelectMapper.buildModel(rs) uses 
 // the column alias prefix to identify and populate the pertinent models from the ResultSet.
 
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
     
       Map<Long, Order> idOrderMap = new LinkedHashMap<>(); // LinkedHashMap to retain result order	
       Map<Integer, Product> IdProductMap = new HashMap<>();
 		
       while (rs.next()) {				
         // selectMapper.buildModel(rs) will return the model populated from the resultSet
         // Everything below is just logic to populate the relationships
         // Doing some checks to make sure unwanted objects are not created.
         // In this use case Order has many OrderLine and an OrderLine has one product
 					
         // orderSelectMapper.getResultSetModelIdColumnLabel() returns the id column alias which is 'o_order_id'
         // for the sql above. 
         Long orderId = rs.getLong(orderSelectMapper.getResultSetModelIdColumnLabel());						
         Order order = idOrderMap.get(orderId);
         if (order == null) {
           order = orderSelectMapper.buildModel(rs); // populates Order model from resultSet
           idOrderMap.put(order.getOrderId(), order);
         }
 				    
         // productSelectMapper.getResultSetModelIdColumnName() returns the id column alias which is 'p_product_id'
         // for the sql above.
         Integer productId = rs.getInt(productSelectMapper.getResultSetModelIdColumnLabel());
         Product product = IdProductMap.get(productId);
         if (product == null) {
           product = productSelectMapper.buildModel(rs); // populates Product model from resultSet
           IdProductMap.put(product.getProductId(), product);
         }
 				    
         OrderLine orderLine = orderLineSelectMapper.buildModel(rs); // populated OrderLine model from resultSet
         if(orderLine != null) {
           // wire up the relationships
           orderLine.setProduct(product); 
           order.getOrderLines().add(orderLine);
         }			
      }
      return new ArrayList<Order>(idOrderMap.values());
    }
  };
 
  // execute the JdbcTemplate query	
  List<Order> orders = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor);
    
```

## Logging
 
Uses the same logging configurations as Spring's JdbcTemplate to log the SQL. In applications.properties:
 
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

## New features/bugs
Please open a [issues ticket](https://github.com/jdbctemplatemapper/jdbctemplatemapper/issues) 
  
 
