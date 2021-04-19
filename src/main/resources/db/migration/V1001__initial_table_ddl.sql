CREATE TABLE jdbctemplatemapper.order (
	id serial NOT NULL,
	order_date timestamp NULL,
	customer_id int4 NULL,
	status varchar(100) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version int4 NULL,
	CONSTRAINT order_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.order_line (
	id serial NOT NULL,
	order_id int4 NOT NULL,
	product_id int4 NOT NULL,
	num_of_units int4 NULL,
	CONSTRAINT order_line_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.customer (
	id serial NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.product (
	id serial NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	CONSTRAINT product_pk PRIMARY KEY (id)
);










