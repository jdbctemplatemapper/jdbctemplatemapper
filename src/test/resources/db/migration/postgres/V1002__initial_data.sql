
INSERT INTO jdbctemplatemapper.orders
(order_date, customer_id, created_on, created_by, updated_on, updated_by, version)
VALUES('2020-06-20 00:00:00.000', 1, '2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.orders
(order_date, customer_id, created_on, created_by, updated_on, updated_by,  version)
VALUES('2020-06-20 00:00:00.000', 2, '2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.orders
(order_date, created_on, created_by, updated_on, updated_by,  version)
VALUES('2020-06-20 00:00:00.000', '2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.order_line
(order_id, product_id, num_of_units)
VALUES(1, 1, 10);

INSERT INTO jdbctemplatemapper.order_line
(order_id, product_id, num_of_units)
VALUES(1, 2, 5);

INSERT INTO jdbctemplatemapper.order_line
(order_id, product_id, num_of_units)
VALUES(2, 3, 1);


INSERT INTO jdbctemplatemapper.customer
(first_name, last_name)
VALUES( 'tony', 'joe');
INSERT INTO jdbctemplatemapper.customer
(first_name, last_name)
VALUES('jane', 'doe');

INSERT INTO jdbctemplatemapper.customer
(first_name, last_name)
VALUES('customer 3 test for property update', 'customer 3 last name');

INSERT INTO jdbctemplatemapper.customer
(first_name, last_name)
VALUES('customer 4 test for no updateInfo and no version test', 'customer 4 last name');

INSERT INTO jdbctemplatemapper.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(1, 'shoes', 95.00,'2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(2, 'socks', 4.55,'2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(3, 'laces', 1.25,'2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(4, 'product4 for delete test', 1.25,'2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);

INSERT INTO jdbctemplatemapper.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(5, 'product5 for delete test', 1.25,'2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);


INSERT INTO jdbctemplatemapper.product
(product_id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(6, 'product 6 update test', 2.45,'2020-06-20 00:00:00.000', 'system', '2020-06-20 00:00:00.000', 'system', 1);


INSERT INTO jdbctemplatemapper.person
( first_name, last_name)
VALUES( 'mike', 'smith');


