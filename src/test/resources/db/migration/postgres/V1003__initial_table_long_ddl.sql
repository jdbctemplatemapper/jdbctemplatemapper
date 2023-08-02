
CREATE TABLE jdbctemplatemapper.order_long (
	order_id bigserial NOT NULL,
	order_date timestamp NULL,
	customer_long_id bigint NULL,
	status varchar(100) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version int4 NULL,
	CONSTRAINT order_long_pk PRIMARY KEY (order_id)
);

CREATE TABLE jdbctemplatemapper.order_line_long (
	order_line_id bigserial NOT NULL,
	order_long_id bigint NOT NULL,
	product_long_id bigint NOT NULL,
	num_of_units int4 NULL,
	CONSTRAINT order_line_long_pk PRIMARY KEY (order_line_id)
);

CREATE TABLE jdbctemplatemapper.customer_long (
	customer_id bigserial NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_long_pk PRIMARY KEY (customer_id)
);

CREATE TABLE jdbctemplatemapper.product_long (
	product_id bigint NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on timestamp NULL,
	created_by varchar(100) NULL,
	updated_on timestamp NULL,
	updated_by varchar(100) NULL,
	version int4 NULL,
	CONSTRAINT product_long_pk PRIMARY KEY (product_id)
);
