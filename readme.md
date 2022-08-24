Spring's JdbcTemplate provides full data access control using SQL to a relational database. Its a is better option for complex enterprise applications than an ORM (ORM magic/nuances get in the way when used for large and complex applications). Even though JdbcTemplate abstracts away a lot of the boiler plate code needed by JDBC, it is verbose.

JdbcTemplateMapper tries to mitigate the verboseness. It is a helper utility for JdbcTemplate (NOT a replacement). It provides simple CRUD one liners and less verbose ways to query relationships. Your project code will be a mix of
JdbcTemplate and JdbcTemplateMapper. Use JdbcTemplateMapper's more concise features where appropriate.

** Features:**
 1. Simple CRUD one liners
 2. Methods to retrieve relationships (toOne(), toMany() etc)
 3. Can be configured for
    1. auto assign created by and updated by fields during insert/updates
    2. auto assign created by, updated by using an implementation of IRecordOperatorResolver.
 4. optimistic locking functionality for updates by configuring a version property.
 5. Thread safe
Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with all other relational databases.

 ** JdbcTemplateMapper is opinionated **
 Projects have to meet the following 2 criteria to use it:
 1. Models should have a property exactly named 'id' (or 'ID') which has to be of type Integer or Long.
 2. Camel case object property names are mapped to snake case database column names. Properties of a model like 'firstName', 'lastName' will be mapped to corresponding database columns first_name/FIRST_NAME and last_name/LAST_NAME in the database table. If you are using a case sensitive database installation and have mixed case database column names like 'Last_Name' you could run into problems.
 
 Examples of simple CRUD:
 Product class below maps to product/PRODUCT table by default.
 Use annotation @Table(name="some_other_tablename") to override the default
  public class Product {
    private Integer id; // 'id' property is needed for all models and has to be of type Integer or Long
    private String productName;
     private Double price;
     private LocalDateTime availableDate;
 
 for insert/update/find.. methods will ignore properties which do not
 have a corresponding snake case columns in database table
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
 See methods toOne() and  toMany() for relationship retrieval.
 Installation:
 Requires Java8 or above and dependencies are the same as that for Spring's JdbcTemplate
 pom.xml dependencies
 For a spring boot application:
 <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-jdbc</artifactId>
 </dependency>
 
  For a regular spring application:
 <dependency>
   <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>YourVersionOfSpringJdbc</version>
   </dependency>
 
 Spring bean configuration for JdbcTemplateMapper will look something like below:
 (Assuming that org.springframework.jdbc.core.JdbcTemplate is configured as per Spring instructions)
 
 @Bean
 public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
   return new JdbcTemplateMapper(jdbcTemplate);   
  //if the database setup needs a schema name, pass it as argument.
  //return new JdbcTemplateMapper(jdbcTemplate, "your_database_schema_name");   