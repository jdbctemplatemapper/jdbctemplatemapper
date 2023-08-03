CREATE TABLE jdbctemplatemapper.orders (
	order_id NUMBER GENERATED ALWAYS AS IDENTITY,
	order_date timestamp NULL,
	customer_id NUMBER NULL,
	status varchar(100) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	non_model_column varchar(100),
	CONSTRAINT order_pk PRIMARY KEY (order_id)
);

CREATE TABLE jdbctemplatemapper.order_line (
	order_line_id NUMBER GENERATED ALWAYS AS IDENTITY,
	order_id NUMBER NOT NULL,
	product_id NUMBER NOT NULL,
	num_of_units NUMBER NULL,
	non_model_column varchar(100),
	CONSTRAINT order_line_pk PRIMARY KEY (order_line_id)
);

CREATE TABLE jdbctemplatemapper.customer (
	customer_id NUMBER GENERATED ALWAYS AS IDENTITY,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	non_model_column varchar(100),
	CONSTRAINT customer_pk PRIMARY KEY (customer_id)
);

CREATE TABLE jdbctemplatemapper.product (
	product_id integer NOT NULL,
	name varchar(100) NOT NULL,
	cost NUMBER(10,3) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	non_model_column varchar(100),
	CONSTRAINT product_pk PRIMARY KEY (product_id)
);


CREATE TABLE jdbctemplatemapper.person (
	person_id varchar(100) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	non_model_column varchar(100),
	CONSTRAINT person_pk PRIMARY KEY (person_id)
);


CREATE TABLE jdbctemplatemapper.no_id_object (
	something varchar(100)
);

CREATE TABLE jdbctemplatemapper.type_check (
   id NUMBER GENERATED ALWAYS AS IDENTITY,
   local_date_data date,
   java_util_date_data date,
   local_date_time_data timestamp,
   java_util_date_ts_data timestamp,
   big_decimal_data number(10,2),
   non_model_column varchar(100)
   /* oracle has no boolean field */
);











