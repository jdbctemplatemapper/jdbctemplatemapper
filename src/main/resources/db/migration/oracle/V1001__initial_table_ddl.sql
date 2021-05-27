CREATE TABLE jdbctemplatemapper.orders (
	id NUMBER GENERATED ALWAYS AS IDENTITY,
	order_date date NULL,
	customer_id NUMBER NULL,
	status varchar(100) NULL,
	created_on date NULL,
	created_by varchar(100) NULL,
	updated_on date NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	CONSTRAINT order_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.order_line (
	id NUMBER GENERATED ALWAYS AS IDENTITY,
	order_id NUMBER NOT NULL,
	product_id NUMBER NOT NULL,
	num_of_units NUMBER NULL,
	CONSTRAINT order_line_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.customer (
	id NUMBER GENERATED ALWAYS AS IDENTITY,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.product (
	id NUMBER NOT NULL,
	name varchar(100) NOT NULL,
	cost NUMBER(10,3) NULL,
	created_on date NULL,
	created_by varchar(100) NULL,
	updated_on date NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	CONSTRAINT product_pk PRIMARY KEY (id)
);


CREATE TABLE jdbctemplatemapper.person (
	id NUMBER GENERATED ALWAYS AS IDENTITY,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	created_on date NULL,
	created_by varchar(100) NULL,
	updated_on date NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	CONSTRAINT person_pk PRIMARY KEY (id)
);












