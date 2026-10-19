create table customer
(
    id             uuid         primary key,
    credentials_id uuid         not null unique references credentials (id),
    first_name     varchar(255) not null,
    last_name      varchar(255) not null,
    street         varchar(255) not null,
    house_number   varchar(20)  not null,
    postal_code    varchar(20)  not null,
    city           varchar(255) not null,
    phone_number   varchar(50)  not null
);
