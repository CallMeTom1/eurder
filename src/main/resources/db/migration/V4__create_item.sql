create table item
(
    id          uuid           primary key,
    name        varchar(255)   not null,
    description varchar(1000)  not null,
    price       numeric(10, 2) not null,
    stock       integer        not null
);
