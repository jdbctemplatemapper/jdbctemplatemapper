CREATE TABLE schema1.orders (
	order_id bigint NOT NULL AUTO_INCREMENT,
	order_date timestamp NULL,
	customer_id integer NULL,
	status varchar(100) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version integer NULL,
	non_model_column varchar(100),
	CONSTRAINT order_pk PRIMARY KEY (order_id)
);

CREATE TABLE schema1.order_line (
	order_line_id integer NOT NULL AUTO_INCREMENT,
	order_id bigint NOT NULL,
	product_id integer NOT NULL,
	num_of_units integer NULL,
	non_model_column varchar(100),
	CONSTRAINT order_line_pk PRIMARY KEY (order_line_id)
);

CREATE TABLE schema1.customer (
	customer_id integer NOT NULL AUTO_INCREMENT,
	first_name varchar(100),
	last_name varchar(100) NOT NULL,
	non_model_column varchar(100),
	CONSTRAINT customer_pk PRIMARY KEY (customer_id)
);

CREATE TABLE schema1.product (
	product_id integer NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version integer NULL,
	non_model_column varchar(100),
	CONSTRAINT product_pk PRIMARY KEY (product_id)
);


CREATE TABLE schema1.person (
	person_id varchar(100) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version integer NULL,
	non_model_column varchar(100),
	CONSTRAINT person_pk PRIMARY KEY (person_id)
);

CREATE VIEW schema1.person_view AS
SELECT person_id, first_name, last_name
FROM schema1.person;

CREATE TABLE schema1.type_check (
   id integer NOT NULL AUTO_INCREMENT,
   local_date_data date,
   java_util_date_data date,
   local_date_time_data timestamp,
   java_util_date_ts_data timestamp,
   big_decimal_data numeric(10,2),
   boolean_val BOOLEAN,
   image blob,
   offset_date_time_data timestamp,
   int_enum integer,
   string_enum varchar(100),
   non_model_column varchar(100),
   CONSTRAINT type_check_pk PRIMARY KEY (id)
);

CREATE TABLE schema1.annotation_check (
   id integer NOT NULL,  
   id2 integer,
   something varchar(100),
   created_on1 timestamp,
   created_on2 timestamp,
   created_by1 varchar(100),
   created_by2 varchar(100),
   updated_on1 timestamp,
   updated_on2 timestamp,
   updated_by1 varchar(100),
   updated_by2 varchar(100),
   version1 integer,
   version2 integer
);

CREATE TABLE schema1.no_id_object (
   something varchar(100)
);


CREATE TABLE schema1.employee (
	id integer NOT NULL AUTO_INCREMENT,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT employee_pk PRIMARY KEY (id)
);

CREATE TABLE schema1.skill (
	id integer NOT NULL AUTO_INCREMENT,
	name varchar(100) NOT NULL,
	CONSTRAINT skill_pk PRIMARY KEY (id)
);

CREATE TABLE schema1.employee_skill (
	id integer NOT NULL AUTO_INCREMENT,
	employee_id integer,
	skill_id integer,
	CONSTRAINT employee_skill_pk PRIMARY KEY (id)
);












