
CREATE TABLE jdbctemplatemapper.order_long (
	id bigint IDENTITY(1,1) NOT NULL,
	order_date datetime NULL,
	customer_long_id bigint NULL,
	status varchar(100) NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	CONSTRAINT order_long_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.order_line_long (
	id bigint IDENTITY(1,1) NOT NULL,
	order_long_id bigint NOT NULL,
	product_long_id bigint NOT NULL,
	num_of_units int NULL,
	CONSTRAINT order_line_long_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.customer_long (
	id bigint IDENTITY(1,1) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_long_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.product_long (
	id bigint NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	CONSTRAINT product_long_pk PRIMARY KEY (id)
);
