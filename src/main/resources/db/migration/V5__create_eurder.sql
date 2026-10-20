create table eurder
(
    id          uuid primary key,
    customer_id uuid not null references customer (id)
);

create table item_group
(
    id            uuid           primary key,
    eurder_id     uuid           not null references eurder (id),
    line_number   integer        not null,
    item_id       uuid           not null references item (id),
    item_name     varchar(255)   not null,
    item_price    numeric(10, 2) not null,
    amount        integer        not null,
    shipping_date date           not null,
    unique (eurder_id, line_number)
);
