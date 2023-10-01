

CREATE TABLE schema2.company (
	id integer NOT NULL AUTO_INCREMENT,
	name varchar(100),
	CONSTRAINT company_pk PRIMARY KEY (id)
);


CREATE TABLE schema2.office (
	id integer NOT NULL AUTO_INCREMENT,
	company_id integer,
	address varchar(100),
	CONSTRAINT office_pk PRIMARY KEY (id)
);