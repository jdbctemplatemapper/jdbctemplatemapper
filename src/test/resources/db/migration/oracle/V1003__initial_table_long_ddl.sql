
CREATE TABLE jdbctemplatemapper.order_long (
	id  NUMBER GENERATED ALWAYS AS IDENTITY,
	order_date  timestamp NULL,
	customer_long_id NUMBER NULL,
	status varchar(100) NULL,
	created_on  timestamp NULL,
	created_by varchar(100) NULL,
	updated_on  timestamp NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	CONSTRAINT order_long_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.order_line_long (
	id  NUMBER GENERATED ALWAYS AS IDENTITY,
	order_long_id NUMBER NOT NULL,
	product_long_id NUMBER NOT NULL,
	num_of_units NUMBER NULL,
	CONSTRAINT order_line_long_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.customer_long (
	id  NUMBER GENERATED ALWAYS AS IDENTITY,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_long_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.product_long (
	id NUMBER NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on  timestamp NULL,
	created_by varchar(100) NULL,
	updated_on  timestamp NULL,
	updated_by varchar(100) NULL,
	version NUMBER NULL,
	CONSTRAINT product_long_pk PRIMARY KEY (id)
);
