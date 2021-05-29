
CREATE TABLE jdbctemplatemapper.orders(
    id int IDENTITY(1,1) NOT NULL,
	order_date datetime NULL,
	customer_id int NULL,
	status varchar(100) NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	CONSTRAINT order_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.order_line (
	id int IDENTITY(1,1) NOT NULL,
	order_id int NOT NULL,
	product_id int NOT NULL,
	num_of_units int NULL,
	CONSTRAINT order_line_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.customer (
	id int IDENTITY(1,1) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	CONSTRAINT customer_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.product (
	id int NOT NULL,
	name varchar(100) NOT NULL,
	cost numeric(10,3) NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	CONSTRAINT product_pk PRIMARY KEY (id)
);


CREATE TABLE jdbctemplatemapper.person (
	id int IDENTITY(1,1) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	created_on datetime NULL,
	created_by varchar(100) NULL,
	updated_on datetime NULL,
	updated_by varchar(100) NULL,
	version int NULL,
	CONSTRAINT person_pk PRIMARY KEY (id)
);

CREATE TABLE jdbctemplatemapper.no_id_object (
	something varchar(100)
);












