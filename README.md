# JdbcTemplateMapper #
 
 A simple library to make the usage of Spring JdbcTemplate less verbose for features like CRUD and relationship queries.
 Use it where appropriate and for other functionality keep using JdbcTemplate as you normally would.
 
 [Javadoc](https://jdbctemplatemapper.github.io/jdbctemplatemapper/javadoc/javadoc.html) 
 
## Features

  1. One liners for CRUD.
  2. Fluent style queries for relationships hasOne, hasMany, hasMany through (many to many).
  3. Auto assign properties for models:
      * auto assign created on, updated on.
      * auto assign created by, updated by using an implementation of interface IRecordOperatorResolver.
      * optimistic locking feature for updates.
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
     // For non auto increment id use @Id. In this case you will have to manually set the id value before invoking insert().
 
     @Id(type=IdType.AUTO_INCREMENT)
     private Integer id; 
             
     @Column(name="product_name")            // Please read documentation of this annotation.
     private String name;                    // will map to product_name column in table.
     
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
 jdbcTemplateMapper.insert(product); // because id type is auto increment, id value will be set on insert.

 product = jdbcTemplateMapper.findById(Product.class, product.getId());
 product.setPrice(11.50);
 jdbcTemplateMapper.update(product);
 
 List<Product> products = jdbcTemplateMapper.findAll(Product.class);
 
 List<Product> list = jdbcTemplateMapper.findByProperty(Product.class, "name", "some product name");

 jdbcTemplateMapper.delete(product);
 jdbcTemplateMapper.delete(Product.class, 5); // delete using id
 
 // for methods which help make querying relationships less verbose see section 'Querying relationships' further below
 
 ```
 
## Maven coordinates

 ``` 
  <dependency>
    <groupId>io.github.jdbctemplatemapper</groupId>
    <artifactId>jdbctemplatemapper</artifactId>
    <version>1.3.4</version>
 </dependency>
 ```
 
## Spring bean configuration for JdbcTemplateMapper

 1. Configure the JdbcTemplate bean as per Spring documentation
 2. Configure the JdbcTemplateMapper bean:

 **Note: An instance of JdbcTemplateMapper is thread safe**
 
 ```java
@Bean
public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
  return new JdbcTemplateMapper(jdbcTemplate);
  
 // new JdbcTemplateMapper(jdbcTemplate, schemaName);
 // new JdbcTemplateMapper(jdbcTemplate, schemaName, catalogName);
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

In this case you will have to manually set the id value before invoking insert()

**@Column**

Properties that need be persisted to the database will need @Column annotation unless the property is already annotated with one of the other annotations (@Id, @Version, @CreatedOn @CreatedBy @UpdatedOn @UpdatedBy). @Column can be used with the other annotations to map to a different column name.

The two ways to use it:

@Column  
This will map property to a column using the default naming convention of camel case to underscore name.

@Column(name="some_column_name")  
This will map the property to the column specified by name attribute.   
Note that this will impact using "SELECT * " with Spring BeanPropertyRowMapper in custom queries. The mismatch of column and property names will cause BeanPropertyRowMapper to ignore these properties. Use "SELECT " + jdbcTemplateMapper.getColumnsSql(Class) which will create column aliases to match property names so will work with BeanPropertyRowMapper.

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
        .withRecordOperatorResolver(new YourImplementationOfIRecordOperatorResolver());
    return jdbcTemplateMapper;
}
```
 
## Querying relationships
The library provides multiple ways to query relationships. Keep in mind that that you can always use Spring JdbcTemplate if JdbcTemplateMapper lacks features you need.   
Fluent style queries allow querying of hasOne, hasMany and hasMany through (many to many using an associative table).
The IDE will provide suggestions to help chain the methods. Turn logging on (see logging section) to see the generated queries.  
The QueryMerge class allows the results of a previous query to be merged with results of a new query.

### fluent style queries

```
// Gets all orders. This is equivalent to jdbcTemplateMapper.findAll(Order.class);
List<Order> orders = Query.type(Order.class)
                          .execute(jdbcTemplateMapper);

// with where and orderBy
// orderBy() is strictly validated and is protected against SQL injection.
// make sure the whereClause is parameterized to prevent SQL injection.
List<Order> orders = 
  Query.type(Order.class)
       .where("orders.status = ?", "IN PROCESS") // always prefix where() columns with table
       .orderBy("orders.id")  // always prefix orderBy() columns with table
       .execute(jdbcTemplateMapper); // execute with jdbcTemplateMapper

// hasOne relationship         
List<Order> orders = 
  Query.type(Order.class) // owning class
       .where("orders.status = ?", "IN PROCESS") // always prefix where() columns with table
       .orderBy("orders.id")  // always prefix orderBy() columns with table
       .hasOne(Customer.class) // related Class
       .joinColumnOwningSide("customer_id") // hasOne() join column is on owning side. No prefixes.
       .populateProperty("customer") // property on owning class to populate
       .execute(jdbcTemplateMapper); // execute with jdbcTemplateMapper
              
// hasMany relationship         
List<Order> orders = 
  Query.type(Order.class) // owning class
       .where("orders.status = ?", "IN PROCESS") // always prefix where() columns with table
       .orderBy("orders.id, order_line.id")  // owning and related table columns can be used
       .hasMany(OrderLine.class) // related class
       .joinColumnManySide("order_id") // hasMany() join column will be on the many side. No prefixes
       .populateProperty("orderLines") // the property to populate on the owning class
       .execute(jdbcTemplateMapper); // execute with jdbcTemplateMapper   
       
 // employees hasMany skills through associated table 'employee_skill'
 List<Employee> employees =  
   Query.type(Employee.class) // owning class
        .hasMany(Skill.class) // related class
        .throughJoinTable("employee_skill") // the associated table
        .throughJoinColumns("employee_id", "skill_id")  // note order of join columns. owning join column is first
        .populateProperty("skills") // property on owning class to populate
        .execute(jdbcTemplateMapper);                  
         
```

### fluent queries with QueryMerge

Query api allows only one relationship to be queried. If multiple relationships need to be queried and its okay to issue multiple queries use QueryMerge. It merges the results from a query with results from another query.

Example: Order hasOne Customer, Order hasMany OrderLine, OrderLine hasOne Product

```java

 @Table(name = "orders")
  public class Order {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;                             // column id    
    @Column
    private LocalDateTime orderDate;                // column order_date    
    @Column
    private String status;                          // column status  
    @Column
    private Integer customerId;                     // column customer_id. foreign key
    
    private Customer customer;                      // no mapping for relationships
    List<OrderLine> orderLines = new ArrayList<>(); // No mapping annotations for relationships
                                                    // Important: collections have to be initialized
                                                    // to that the queries can populate them.
  }
  
  @Table(name="order_line")
  public class OrderLine {
    @Id(type = IdType.AUTO_INCREMENT)   
    private Integer id; // column id   
    @Column
    private Integer orderId; // column order_id. foreign key   
    @Column
    private Integer numOfUnits; // column num_of_units   
    @Column
    private Integer productId; // column product_id. foreign key

    private Product product; // no mapping annotations for relationships
  }
  
  @Table(name="product")
  public class Product {
    @Id 
    private Integer id;           // column id    
    @Column
    private String name;          // column name    
    @Column
    private Double price;         // column price
  }
  
  @Table(name="customer")
  public class Customer {
    @Id 
    private Integer id;           // column id      
    @Column
    private String name;          // column name
  }
  
```
 
 1) Below query will return a list of orders with their 'orderLines' populated.
 
 ```
 List<Order> order = 
    Query.type(Order.class) // owning class
         .where(orders.status = ?, "IN PROCESS") // always prefix columns with table name.
         .orderBy("orders.order_date DESC, order_line.id") // always prefix columns with table name.
         .hasMany(OrderLine.class) // the related class. Order hasMany OrderLine
         .joinColumnManySide("order_id") // the join column will be on the many side. No prefixes
         .populateProperty("orderLines") // the property to populate on the owning class
         .execute(jdbcTemplateMapper); // execute with the jdbcTemplateMapper
 
 ```
   
 2) Populate the Order hasOne customer relationship for the above orders.
    For this use QueryMerge. It merges the results of a query with the orders list from previous query. 
    QueryMerge issues an 'IN' sql clause.
    
  ``` 
   QueryMerge.type(Order.class) // owning class
             .hasOne(Customer.class) // related class
             .joinColumnOwningSide("customer_id") // the join column in on the owning side table. No prefixes
             .populateProperty("customer") // the property to populate on the owning class
             .execute(jdbcTemplateMapper, orders); // merges the query results with orders list
     
  ```
             
 Now we have Order and its orderLines (1st query) and orders with its Customer (second query merge)   
   
 3) Finally we need to populate the product for each OrderLine.
    Each order could have multiple orderLines so it is a list of lists.
    To get all the orderLines as a list we need to flatten the list of lists.
 
  ```       
   List<OrderLine> allOrderLines = orders.stream()
                                         .map(o -> o.getOrderLines())
                                         .flatMap(list -> list.stream())
                                         .collect(Collectors.toList());
                 
   QueryMerge.type(OrderLine.class) // owning class
             .hasOne(Product.class) // related class
             .joinColumnOwningSide("product_id") // the join column in on the  owning side table. No prefixes
             .populateProperty("product") // the property to populate on the owning class
             .execute(jdbcTemplateMapper, allOrderLines); // merges the query results with orderLines
             
 ```
 
### hasMany through (many to many)
This allows querying of hasMany relationship through an associated table (many to many)

```
@Table(name = "employee")
public class Employee {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;   
    @Column
    private String lastName;
    @Column
    private String firstName;
        
    private List<Skill> skills = new ArrayList<>();
    ...
}

@Table(name = "skill")
public class Skill {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;
    @Column
    private String name;
    
    private List<Employee> employees = new ArrayList<>();
    ...
}    

@Table(name = "employee_skill")
public class EmployeeSkill {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;
    @Column
    private Integer employeeId;
    @Column
    private Integer skillId;
    ...
 }   
    
 // employees with skills populated.
 List<Employee> employees =  
   Query.type(Employee.class) // owning class
        .hasMany(Skill.class) // related class
        .throughJoinTable("employee_skill") // the associated table
        .throughJoinColumns("employee_id", "skill_id")  // note order of join columns. owning join column is first
        .populateProperty("skills") // property on owning class to populate
        .execute(jdbcTemplateMapper);
 
 // Skills with employees populated
 List<Skill> skills = 
   Query.type(Skill.class) // owning class
        .hasMany(Employee.class) // related class
        .throughJoinTable("employee_skill") //the associated table
        .throughJoinColumns("skill_id", "employee_id") // note order of join columns. owning join column is first
        .populateProperty("employees") // property on owning class to populate
        .execute(jdbcTemplateMapper);
 
```
 
### Querying multiple relationships with a single query

For querying multiple relationships with a single query use SelectMapper with Spring ResultSetExtractor.  
SelectMapper allows generating the select columns string for the model and population of the model from a ResultSet.  
An example for querying the following relationship: An Order has many OrderLine and each OrderLine has one Product.  

```
 // The second argument to getSelectMapper() below is the table alias used in the query.
 // For the query below the 'orders' table alias is 'o', the 'order_line' table alias 
 // is 'ol' and the product table alias is 'p'. SelectMapper.getColumnsSql() aliases 
 // the columns with the prefix; table alias + "_" so that there are no conflicts in sql
 // when different models have same property names like for example id.
 // SelectMapper.buildModel(rs) uses the column alias prefix to populate the pertinent
 // model from the ResultSet.
 
 SelectMapper<Order> orderSelectMapper = jdbcTemplateMapper.getSelectMapper(Order.class, "o");
 SelectMapper<OrderLine> orderLineSelectMapper = jdbcTemplateMapper.getSelectMapper(OrderLine.class, "ol");
 SelectMapper<Product> productSelectMapper = jdbcTemplateMapper.getSelectMapper(Product.class, "p");

 // no need to type in column names and aliases so we can concentrate on join and where clauses
 String sql = "select" 
               + orderSelectMapper.getColumnsSql() 
               + ","
               + orderLineSelectMapper.getColumnsSql() 
               + "," 
               + productSelectMapper.getColumnsSql()
               + " from orders o" 
               + " left join order_line ol on o.id = ol.order_id"
               + " join product p on p.id = ol.product_id"
               + " order by o.id, ol.id";
                                        	
 ResultSetExtractor<List<Order>> rsExtractor = new ResultSetExtractor<List<Order>>() {
     @Override
     public List<Order> extractData(ResultSet rs) throws SQLException, DataAccessException {
       // below logic is specific to this use case. Your logic will be different.
       // The thing to note is the model gets populated by selectMapper.build().
       Map<Long, Order> idOrderMap = new LinkedHashMap<>(); // LinkedHashMap to retain record order
       Map<Integer, Product> idProductMap = new HashMap<>();
       while (rs.next()) {				 					
         // orderSelectMapper.getResultSetModelIdColumnLabel() returns the id column alias 
         // which is 'o_id' for the sql above. 
         Long orderId = rs.getLong(orderSelectMapper.getResultSetModelIdColumnLabel());
         Order order = idOrderMap.get(orderId);
         if (order == null) {
           order = orderSelectMapper.buildModel(rs);
           idOrderMap.put(order.getId(), order);
         }
         // productSelectMapper.getResultSetModelIdColumnName() returns the id column alias 
         // which is 'p_id'for the sql above.
         Integer productId = rs.getInt(productSelectMapper.getResultSetModelIdColumnLabel());
         Product product = idProductMap.get(productId);
         if (product == null) {
           product = productSelectMapper.buildModel(rs); 
           idProductMap.put(product.getId(), product);
         }
         OrderLine orderLine = orderLineSelectMapper.buildModel(rs);
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
...

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
 3. When using @Column(name="some_column_name") to map a property to a non default column it will impact using "SELECT * " with Spring BeanPropertyRowMapper in custom queries. The mismatch of column and property names will cause BeanPropertyRowMapper to ignore these properties. Use "SELECT " + jdbcTemplateMapper.getColumnsSql(Class) which will create column aliases to match property names so will work with BeanPropertyRowMapper.
 4. Models should have a no argument constructor so they can be instantiated and properties set.
 5. For Oracle/SqlServer no support for blob/clob. Use JdbcTemplate directly for this with recommended custom code
 
## TroubleShooting
Make sure you can connect to your database and issue a simple query using Spring JdbcTemplate without the JdbcTemplateMapper.

## New features/bugs
Please open a [issues ticket](https://github.com/jdbctemplatemapper/jdbctemplatemapper/issues) 
  
 
