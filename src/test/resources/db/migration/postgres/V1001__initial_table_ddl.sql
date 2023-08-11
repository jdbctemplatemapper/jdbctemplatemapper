CREATE TABLE jdbctemplatemapper.orders (
	order_id bigserial NOT NULL,
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

CREATE TABLE jdbctemplatemapper.order_line (
	order_line_id serial NOT NULL,
	order_id bigint NOT NULL,
	product_id integer NOT NULL,
	num_of_units integer NULL,
	non_model_column varchar(100),
	CONSTRAINT order_line_pk PRIMARY KEY (order_line_id)
);

CREATE TABLE jdbctemplatemapper.customer (
	customer_id serial NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	non_model_column varchar(100),
	CONSTRAINT customer_pk PRIMARY KEY (customer_id)
);

CREATE TABLE jdbctemplatemapper.product (
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


CREATE TABLE jdbctemplatemapper.person (
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

CREATE TABLE jdbctemplatemapper.no_id_object (
	something varchar(100)
);

CREATE TABLE jdbctemplatemapper.type_check (
   id serial NOT NULL,
   local_date_data date,
   java_util_date_data date,
   local_date_time_data timestamp,
   java_util_date_ts_data timestamp,
   big_decimal_data numeric(10,2),
   boolean_val boolean,
   image bytea,
   offset_date_time_data timestamptz,
   instant_data timestamptz,
   non_model_column varchar(100),
   non_model_offset_date_time timestamptz,
   CONSTRAINT type_check_pk PRIMARY KEY (id)
);














