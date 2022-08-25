# JdbcTemplateMapper #

Spring's JdbcTemplate provides full data access control using SQL for a relational database. It is a better option for complex enterprise applications over ORM. ORM magic/nuances get in the way when used for large and complex applications. Even though JdbcTemplate abstracts away a lot of the boiler plate code needed by JDBC, it is verbose.

JdbcTemplateMapper tries to mitigate the verboseness. It is a helper utility for JdbcTemplate (NOT a replacement). It provides simple CRUD one liners and less verbose ways to query relationships. Your project code will be a mix of
JdbcTemplate and JdbcTemplateMapper. Use JdbcTemplateMapper's more concise features where appropriate and JdbcTemplate for others.

**Features:** 
 1. Simple CRUD one liners
 2. Methods to retrieve relationships (toOne(), toMany() etc)
 3. Can be configured for
    1. auto assign created on and updated on fields during insert/updates
    2. auto assign created by and updated by fields using an implementation of IRecordOperatorResolver.
 4. optimistic locking functionality for updates by configuring a version property.
 5. Thread safe. Just need to configure a singleton bean.
 
Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with other relational databases.

 **JdbcTemplateMapper is opinionated:** 
 
 Projects have to meet the following 2 criteria to use it:
 1. Models should have a property exactly named 'id' (or 'ID') which has to be of type Integer or Long.
 2. Camel case object property names are mapped to snake case database column names. Properties of a model like 'firstName' will be mapped to corresponding database column 'last__name' in the table. 
 
 **Examples of simple CRUD:** 
 
 ```java
 public class Product { // By default this maps to product table. Use @Table(name="some_other_tablename") to override default table name
    private Integer id; // 'id' property is needed for all models and has to be of type Integer or Long
    private String productName;
    private Double price;
    private LocalDateTime availableDate;
    
    // for insert/update/find.. properties which do not have a corresponding snake case column in database table will be ignored
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
 
 **Example of toOne, toMany relationships **
 
 ```
   public class Order{
     Integer id;
     LocalDateTime orderDate;
     Integer customerId; 
     Customer customer; // the toOne relationship
     List<OrderLine> orderLines; // toMany relationship
     
     // getters and setters ...
   }
    
    public class Customer{
      Integer id;
      String firstName;
      String lastName;
      String address;
      
      // getters and setters ...
    }
    
   public class OrderLine{
     Integer id;
     Integer orderId; 
     Integer productId;
     Integer quantity;
     Double price;
     
     // getters and setters ...
  }
    
   Order order = jdbcTemplateMapper.findById(orderId, Order.class);
   // Populate the order's toOne customer property. This will issue an sql query
   jdbcTemplateMapper.toOne(order,"customer", "customerId");'
   
   // Populate the order's toMany orderLines property This will issue an sql query
   jdbcTemplateMapper.toMany(order, "orderLines", "orderId");
   
   // For a list of orders populate their customer property
   List<Order> orders = jdbcTemplateMapper.findAll(Order.class);
   jdbcTemplateMapper.toOne(orders,"customer", "customerId");
   
   // For a list of orders populate their orderLines property. 
   jdbcTemplateMapper.toMany(orders, "orderLines", "orderId");
   
 ```
 
 **Installation:** 
 
 Requires Java8 or above and dependencies are the same as that for Spring's JdbcTemplate
 
 For a spring boot application:
 
 ```
 <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-jdbc</artifactId>
 </dependency>
 ```
 
 **Spring bean configuration for JdbcTemplateMapper:** 
 
 ```
 @Bean
 public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
   return new JdbcTemplateMapper(jdbcTemplate);   
  //if the database setup needs a schema name, pass it as argument.
  //return new JdbcTemplateMapper(jdbcTemplate, "your_database_schema_name");   
  }
  ```