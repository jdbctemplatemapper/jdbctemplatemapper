
CREATE TABLE schema1.orders(
    order_id bigint IDENTITY(1,1) NOT NULL,
	order_date datetime NULL,
	customer_id int NULL,
	status varchar(100) NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	non_model_column varchar(100),
	CONSTRAINT order_pk PRIMARY KEY (order_id)
);

CREATE TABLE schema1.order_line (
	order_line_id int IDENTITY(1,1) NOT NULL,
	order_id int NOT NULL,
	product_id int NOT NULL,
	num_of_units int NULL,
	non_model_column varchar(100),
	CONSTRAINT order_line_pk PRIMARY KEY (order_line_id)
);

CREATE TABLE schema1.customer (
	customer_id int IDENTITY(1,1) NOT NULL,
	first_name varchar(100),
	last_name varchar(100) NOT NULL,
	non_model_column varchar(100),
	CONSTRAINT customer_pk PRIMARY KEY (customer_id)
);

CREATE TABLE schema1.product (
	product_id int NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	non_model_column varchar(100),
	CONSTRAINT product_pk PRIMARY KEY (product_id)
);


CREATE TABLE schema1.person (
	person_id varchar(100) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	non_model_column varchar(100),
	CONSTRAINT person_pk PRIMARY KEY (person_id)
);


CREATE TABLE schema1.type_check (
   	id int IDENTITY(1,1) NOT NULL,
   local_date_data date,
   java_util_date_data date,
   local_date_time_data datetime,
   java_util_date_dt_data datetime,
   big_decimal_data numeric(10,2),
   int_enum int,
   string_enum varchar(100),
   non_model_column varchar(100),
   offset_date_time_data varchar(100)
);

CREATE TABLE schema1.annotation_check (
   id int NOT NULL,  
   id2 int,
   something varchar(100),
   created_on1 datetime,
   created_on2 datetime,
   created_by1 varchar(100),
   created_by2 varchar(100),
   updated_on1 datetime,
   updated_on2 datetime,
   updated_by1 varchar(100),
   updated_by2 varchar(100),
   version1 int,
   version2 int,

   CONSTRAINT annotation_check_pk PRIMARY KEY (id)
);

CREATE TABLE schema1.no_id_object (
   something varchar(100)
);

CREATE TABLE schema1.employee (
	id int IDENTITY(1,1) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT employee_pk PRIMARY KEY (id)
);

CREATE TABLE schema1.skill (
	id int IDENTITY(1,1) NOT NULL,
	name varchar(100) NOT NULL,
	CONSTRAINT skill_pk PRIMARY KEY (id)
);

CREATE TABLE schema1.employee_skill (
	id int IDENTITY(1,1) NOT NULL,
	employee_id integer,
	skill_id integer,
	CONSTRAINT employee_skill_pk PRIMARY KEY (id)
);












