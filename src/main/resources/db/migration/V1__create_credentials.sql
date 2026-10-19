create table credentials
(
    id            uuid         primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(20)  not null
);
