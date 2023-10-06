CREATE TABLE SCHEMA2.company (
	id NUMBER GENERATED ALWAYS AS IDENTITY,
	name varchar2(100),
	CONSTRAINT company_pk PRIMARY KEY (id)
);


CREATE TABLE SCHEMA2.office (
	id NUMBER GENERATED ALWAYS AS IDENTITY,
	company_id integer,
	address varchar2(100),
	CONSTRAINT office_pk PRIMARY KEY (id)
);

CREATE synonym testsynonym FOR SCHEMA1.testsynonym;
