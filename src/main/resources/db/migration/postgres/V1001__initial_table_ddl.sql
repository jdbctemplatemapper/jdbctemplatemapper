CREATE TABLE jdbctemplatemapper.orders (
	id serial NOT NULL,
	order_date timestamp NULL,
	customer_id integer NULL,
	status varchar(100) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version integer NULL,
	CONSTRAINT order_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.order_line (
	id serial NOT NULL,
	order_id integer NOT NULL,
	product_id integer NOT NULL,
	num_of_units integer NULL,
	CONSTRAINT order_line_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.customer (
	id serial NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.product (
	id integer NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version integer NULL,
	CONSTRAINT product_pk PRIMARY KEY (id)
);


CREATE TABLE jdbctemplatemapper.person (
	id serial NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version integer NULL,
	CONSTRAINT person_pk PRIMARY KEY (id)
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
   big_decimal_data numeric(10,2)
);














