
CREATE TABLE jdbctemplatemapper.orders(
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

CREATE TABLE jdbctemplatemapper.order_line (
	order_line_id int IDENTITY(1,1) NOT NULL,
	order_id int NOT NULL,
	product_id int NOT NULL,
	num_of_units int NULL,
	non_model_column varchar(100),
	CONSTRAINT order_line_pk PRIMARY KEY (order_line_id)
);

CREATE TABLE jdbctemplatemapper.customer (
	customer_id int IDENTITY(1,1) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	non_model_column varchar(100),
	CONSTRAINT customer_pk PRIMARY KEY (customer_id)
);

CREATE TABLE jdbctemplatemapper.product (
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


CREATE TABLE jdbctemplatemapper.person (
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

CREATE TABLE jdbctemplatemapper.no_id_object (
	something varchar(100)
);

CREATE TABLE jdbctemplatemapper.type_check (
   	id int IDENTITY(1,1) NOT NULL,
   local_date_data date,
   java_util_date_data date,
   local_date_time_data datetime,
   java_util_date_dt_data datetime,
   big_decimal_data numeric(10,2),
   non_model_column varchar(100)
);












