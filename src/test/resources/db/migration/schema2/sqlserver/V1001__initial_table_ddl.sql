CREATE TABLE schema2.company (
	id int IDENTITY(1,1) NOT NULL,
	name varchar(100),
	CONSTRAINT company_pk PRIMARY KEY (id)
);


CREATE TABLE schema2.office (
	id int IDENTITY(1,1) NOT NULL,
	company_id integer,
	address varchar(100),
	CONSTRAINT office_pk PRIMARY KEY (id)
);