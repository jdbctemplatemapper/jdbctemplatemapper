
INSERT INTO jdbctemplatemapper.order
(id, order_date, customer_id, version)
VALUES(1, '2020-06-20 00:00:00.000', 1, 1);

INSERT INTO jdbctemplatemapper.order
(id, order_date, customer_id, version)
VALUES(2, '2020-06-2` 00:00:00.000', 2, 1);

INSERT INTO jdbctemplatemapper.order_line
(id, order_id, product_id, num_of_units)
VALUES(1, 1, 1, 10);

INSERT INTO jdbctemplatemapper.order_line
(id, order_id, product_id, num_of_units)
VALUES(2, 1, 2, 5);

INSERT INTO jdbctemplatemapper.order_line
(id, order_id, product_id, num_of_units)
VALUES(3, 2, 3, 1);


INSERT INTO jdbctemplatemapper.customer
(id, first_name, last_name)
VALUES(1, 'tony', 'joe');
INSERT INTO jdbctemplatemapper.customer
(id, first_name, last_name)
VALUES(2, 'jane', 'doe');


INSERT INTO jdbctemplatemapper.product
(id, name, cost)
VALUES(1, 'shoes', 95.00);

INSERT INTO jdbctemplatemapper.product
(id, name, cost)
VALUES(2, 'socks', 4.55);

INSERT INTO jdbctemplatemapper.product
(id, name, cost)
VALUES(3, 'laces', 1.25);






