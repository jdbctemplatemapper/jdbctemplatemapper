
INSERT INTO jdbctemplatemapper.order_long
(order_date, customer_long_id, created_on, created_by, updated_on, updated_by, version)
VALUES( '20200620 00:00:00', 1, '20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);

INSERT INTO jdbctemplatemapper.order_long
( order_date, customer_long_id, created_on, created_by, updated_on, updated_by,  version)
VALUES( '20200620 00:00:00', 2, '20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);

INSERT INTO jdbctemplatemapper.order_line_long
( order_long_id, product_long_id, num_of_units)
VALUES( 1, 1, 10);

INSERT INTO jdbctemplatemapper.order_line_long
(order_long_id, product_long_id, num_of_units)
VALUES( 1, 2, 5);

INSERT INTO jdbctemplatemapper.order_line_long
( order_long_id, product_long_id, num_of_units)
VALUES( 2, 3, 1);


INSERT INTO jdbctemplatemapper.customer_long
(first_name, last_name)
VALUES( 'tony', 'joe');
INSERT INTO jdbctemplatemapper.customer_long
( first_name, last_name)
VALUES( 'jane', 'doe');


INSERT INTO jdbctemplatemapper.product_long
(id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(1, 'shoes', 95.00,'20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);

INSERT INTO jdbctemplatemapper.product_long
(id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(2, 'socks', 4.55,'20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);

INSERT INTO jdbctemplatemapper.product_long
(id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(3, 'laces', 1.25,'20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);

INSERT INTO jdbctemplatemapper.product_long
(id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(4, 'product4 for delete test', 1.25,'20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);

INSERT INTO jdbctemplatemapper.product_long
(id, name, cost, created_on, created_by, updated_on, updated_by, version)
VALUES(5, 'product5 for delete test', 1.25,'20200620 00:00:00', 'system', '20200620 00:00:00', 'system', 1);






