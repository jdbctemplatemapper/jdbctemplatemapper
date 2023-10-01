
INSERT INTO SCHEMA1.orders
(order_date, customer_id, status, created_on, created_by, updated_on, updated_by, version)
VALUES(to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'),  1, 'IN PROCESS', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.orders
(order_date, customer_id, status, created_on, created_by, updated_on, updated_by,  version)
VALUES(to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'),  2, 'IN PROCESS',to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.orders
(order_date, created_on, created_by, updated_on, updated_by,  version)
VALUES(to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.order_line
(order_id, product_id, num_of_units)
VALUES(1, 1, 10);

INSERT INTO SCHEMA1.order_line
(order_id, product_id, num_of_units)
VALUES(1, 2, 5);

INSERT INTO SCHEMA1.order_line
(order_id, product_id, num_of_units)
VALUES(2, 3, 1);


INSERT INTO SCHEMA1.customer
(first_name, last_name)
VALUES( 'tony', 'joe');
INSERT INTO SCHEMA1.customer
(first_name, last_name)
VALUES('jane', 'doe');

INSERT INTO SCHEMA1.customer
(first_name, last_name)
VALUES('customer 3 test for property update', 'customer 3 last name');

INSERT INTO SCHEMA1.customer
(first_name, last_name)
VALUES('customer 4 test for no updateInfo and no version test', 'customer 4 last name');

INSERT INTO SCHEMA1.customer
(first_name, last_name)
VALUES(null, 'customer 5 last name whose first name is null');

INSERT INTO SCHEMA1.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(1, 'shoes', 95.00,to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(2, 'socks', 4.55,to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(3, 'laces', 1.25,to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(4, 'product4 for delete test', 1.25,to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);

INSERT INTO SCHEMA1.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(5, 'product5 for delete test', 1.25,to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);


INSERT INTO SCHEMA1.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(6, 'product 6 update test', 2.45,to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', to_timestamp('2020-06-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'system', 1);


INSERT INTO SCHEMA1.person
( person_id, first_name, last_name)
VALUES( 'person101', 'mike', 'smith');




